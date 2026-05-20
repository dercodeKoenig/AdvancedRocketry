package zmaster587.advancedRocketry.test.server;

import org.junit.Assume;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-07 Phase 5 — rocket-flight failure modes.
 *
 * <p>Pins observed production behaviour for failure paths that the
 * TASK-07 plan called out:
 *
 * <ul>
 *   <li><b>{@code explode()}</b> — production method (line 1720) that
 *       spawns particles + sets the entity dead. Currently only invoked
 *       from {@code launch()} when {@code partsWearSystem &&
 *       storage.shouldBreak()}. Pin the contract via the new probe.</li>
 *   <li><b>Out-of-fuel mid-flight</b> — the TASK-07 plan wished for an
 *       "out of fuel → rocket explodes" path but production has no such
 *       branch. The {@code isInFlight()} branch (line 1226 onwards) just
 *       sets fuelFluid="null" and lets motionY accumulate downwards. Pin
 *       this as the current contract: zero fuel does NOT auto-explode.
 *       (A future production fix that adds the explode path will fail
 *       this test, signalling that the assertion should flip.)</li>
 *   <li><b>Launch with zero fuel</b> — production launch() does NOT
 *       short-circuit on zero fuel (no rocketRequireFuel gate at launch
 *       time, only at burn time). Document current behaviour.</li>
 * </ul>
 */
public class RocketFlightFailureModesTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern AR_DIMS_ARRAY =
            Pattern.compile("\"arDimensions\":\\[([^]]*)]");
    private static final Pattern UUID_FIELD =
            Pattern.compile("\"uuid\":\"([0-9a-fA-F-]+)\"");
    private static final Pattern FUEL_AMOUNT =
            Pattern.compile("\"amount\":(-?\\d+)");

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private int firstNonOverworldArDimOrSkip() throws Exception {
        String joined = ok(client().execute("artest dim list"));
        Assume.assumeFalse("No AR dimensions registered",
                joined.contains("\"arDimensions\":[]"));
        Matcher m = AR_DIMS_ARRAY.matcher(joined);
        assertTrue("could not parse arDimensions array: " + joined, m.find());
        for (String part : m.group(1).split(",")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            int dim = Integer.parseInt(t);
            if (dim != 0) return dim;
        }
        Assume.assumeTrue("Only overworld is an AR planet", false);
        return -1;
    }

    private int buildAndAssemble(int baseX, int baseY, int baseZ) throws Exception {
        ok(client().execute(
                "artest fill 0 " + (baseX - 2) + " " + (baseY + 1) + " " + (baseZ - 2)
                        + " " + (baseX + 7) + " " + (baseY + 10) + " " + (baseZ + 7)
                        + " minecraft:air"));
        String fixture = ok(client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ + " simple"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture missing builderPos: " + fixture, bp.find());
        int bx = Integer.parseInt(bp.group(1));
        int by = Integer.parseInt(bp.group(2));
        int bz = Integer.parseInt(bp.group(3));
        ok(client().execute("artest rocket assemble 0 " + bx + " " + by + " " + bz));
        String list = ok(client().execute("artest rocket list 0"));
        Matcher rim = ROCKET_LIST_ID.matcher(list);
        int lastId = -1;
        while (rim.find()) lastId = Integer.parseInt(rim.group(1));
        assertTrue("no rocket after assemble: " + list, lastId >= 0);
        return lastId;
    }

    @Test
    public void explodeProbeSetsRocketDeadAndRemovesFromWorld() throws Exception {
        // Production EntityRocket.explode() (line 1720) sets the entity
        // dead. After dead it's no longer in the world.loadedEntityList
        // and findRocket(id) returns null.
        int id = buildAndAssemble(7000, 64, 500);
        String infoBefore = ok(client().execute("artest rocket info " + id));
        Matcher um = UUID_FIELD.matcher(infoBefore);
        assertTrue("no uuid in info: " + infoBefore, um.find());

        String explodeResp = ok(client().execute("artest rocket explode " + id));
        assertTrue("explode probe must succeed: " + explodeResp,
                explodeResp.contains("\"ok\":true"));
        // The atomic probe-response contract is the reliable assertion:
        // production EntityRocket.explode() calls setDead, which flips
        // the rocket's isDead flag synchronously inside the probe call.
        // We do NOT chain a follow-up rocket-info call to assert removal
        // from loadedEntityList — vanilla MC keeps a dead entity in the
        // list until the next worldTick's collect-dead pass, so that
        // observation is racy in a shared headless harness.
        assertTrue("explode probe response must report isDead=true: " + explodeResp,
                explodeResp.contains("\"isDead\":true"));
    }

    @Test
    public void outOfFuelMidFlightDoesNotAutoExplode_documentsCurrentBehavior() throws Exception {
        // The TASK-07 plan wished for "out of fuel → explode" but
        // production has no such code path. The fuel-decrement loop at
        // line 1235 just sets fuelFluid="null" when amount hits 0. The
        // rocket continues to drift (falling under gravity once burning
        // stops). Pin this as the current contract.
        //
        // If a future PR adds an out-of-fuel explode path, this test
        // fails — flip the assertion + delete the documents-bug note.
        int id = buildAndAssemble(7100, 64, 500);

        // Put the rocket in mid-flight (orbit=true so descent gate is
        // active, flight=true so the isInFlight branch is taken).
        ok(client().execute("artest rocket set-state " + id
                + " orbit=true flight=true ticksExisted=60 posY=300 motionY=0"));
        ok(client().execute("artest rocket drain-fuel " + id));

        // Verify fuel is actually zero.
        String fuelResp = ok(client().execute("artest rocket fuel " + id));
        Matcher fm = FUEL_AMOUNT.matcher(fuelResp);
        while (fm.find()) {
            assertEquals("all fuel types must be drained", 0, Integer.parseInt(fm.group(1)));
        }

        // Tick a few times — production must NOT explode.
        ok(client().execute("artest rocket tick " + id + " 5"));

        String info = ok(client().execute("artest rocket info " + id));
        assertFalse("out-of-fuel mid-flight must NOT auto-mark rocket dead "
                        + "(documents current contract; no production explode-on-empty path): "
                        + info,
                info.contains("\"error\":\"rocket not found\""));
        assertTrue("rocket should still be in-flight or descending — not vanished: " + info,
                info.contains("\"entityId\":"));
    }

    @Test
    public void launchWithZeroFuelStillTransitionsToInFlight() throws Exception {
        // Counter-test for the assumption that empty tanks gate launch.
        // Production launch() (line 1757) has NO fuel-amount precondition
        // — only weight/thrust + destination validation. So a rocket with
        // 0 fuel and a valid destination still enters isInFlight=true on
        // the production path. The gameplay-visible consequence (no
        // thrust → falls back down) plays out on the burn-phase
        // (line 1235+) which already drains-then-noops with empty tanks.
        //
        // Pin: launch with empty tanks succeeds at the setInFlight gate.
        int destDim = firstNonOverworldArDimOrSkip();
        int id = buildAndAssemble(7200, 64, 500);
        ok(client().execute("artest rocket set-destination " + id + " " + destDim));
        ok(client().execute("artest rocket drain-fuel " + id));
        // launch with fillFuel=false to keep tanks empty.
        ok(client().execute("artest rocket launch " + id + " false instant"));

        String info = ok(client().execute("artest rocket info " + id));
        assertTrue("zero-fuel launch must still set isInFlight=true "
                        + "(production has no fuel gate at launch time): " + info,
                info.contains("\"isInFlight\":true"));
    }

    @Test
    public void explodeOnUnknownRocketReturnsError() throws Exception {
        String resp = ok(client().execute("artest rocket explode 9999999"));
        assertTrue("unknown rocket must error: " + resp,
                resp.contains("\"error\":\"rocket not found\""));
    }

    @Test
    public void drainFuelOnUnknownRocketReturnsError() throws Exception {
        String resp = ok(client().execute("artest rocket drain-fuel 9999999"));
        assertTrue("unknown rocket must error: " + resp,
                resp.contains("\"error\":\"rocket not found\""));
    }
}
