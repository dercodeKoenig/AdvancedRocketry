package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-06 Phase 3 — MissionGasCollection.onMissionComplete contract.
 *
 * <p>Pins the player-visible cause-effect of completing a gas-collection
 * mission:</p>
 * <ul>
 *   <li>Fluid tiles in the respawned rocket are filled with 64000 mB of
 *       the configured {@code gasFluid} type.</li>
 *   <li>The respawned entity is {@code EntityStationDeployedRocket},
 *       NOT a plain {@code EntityRocket} — distinguishes the gas
 *       completion path from the ore path.</li>
 *   <li>Production guard {@code (int)getStatTag("intakePower") > 0}
 *       short-circuits the fluid fill — no intakePower set → no fill,
 *       even if the rocket otherwise has fluid tiles.</li>
 * </ul>
 *
 * <p>The respawned rocket's exact position depends on the fixture
 * rocket's forwardDirection (offset by 64 in that axis). Tests scan a
 * 128-block cube around the launch coords to find it — the
 * {@code rocket-cargo} probe handles this lookup.</p>
 */
public class MissionGasCompletionTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern MISSION_ID = Pattern.compile("\"missionId\":(-?\\d+)");

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private int buildAndAssembleRocket(int baseX) throws Exception {
        return buildAndAssembleRocket(baseX, "simple");
    }

    private int buildAndAssembleRocket(int baseX, String variant) throws Exception {
        int baseY = 64;
        int baseZ = 600;
        ok(client().execute(
                "artest fill 0 " + (baseX - 2) + " " + (baseY + 1) + " " + (baseZ - 2)
                        + " " + (baseX + 7) + " " + (baseY + 10) + " " + (baseZ + 7)
                        + " minecraft:air"));
        String fixture = ok(client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ + " " + variant));
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

    private long startGasMission(int rocketId, long duration, String fluid, int intakePower) throws Exception {
        String start = ok(client().execute(
                "artest mission start-gas 0 " + rocketId + " " + duration + " " + fluid
                        + " " + intakePower));
        assertFalse("start-gas must not error: " + start, start.contains("\"error\""));
        Matcher mm = MISSION_ID.matcher(start);
        assertTrue("missing missionId in start response: " + start, mm.find());
        return Long.parseLong(mm.group(1));
    }

    /** With intakePower > 0 the gas mission completes WITHOUT crashing
     *  even when the rocket's storage chunk has no fluid-tile entities
     *  (BlockFuelTank in the `simple` fixture is a pure block — no
     *  TileEntity → not added to StorageChunk.liquidTiles → the fill
     *  loop iterates zero times). The strong "64000 mB of oxygen
     *  appears in cargo" assertion needs a fluid-cargo rocket fixture
     *  variant that doesn't exist yet — tracked as a TASK-06 follow-up.
     *  This test pins the no-crash safety contract on the intake>0
     *  branch as a regression net against e.g. a future NPE on
     *  null-fluid or empty-tile-list. */
    @Test
    public void gasCompletionWithIntakeAboveZeroCompletesWithoutCrash() throws Exception {
        int rid = buildAndAssembleRocket(8000);
        long mid = startGasMission(rid, 1000, "oxygen", 10);
        String cargo = ok(client().execute("artest mission complete-now " + mid));
        assertFalse("complete-now must not error: " + cargo, cargo.contains("\"error\""));
        assertTrue("completion must mark mission dead: " + cargo,
                cargo.contains("\"isDeadAfter\":true"));
        assertTrue("completion must report fired (wasDeadBefore=false): " + cargo,
                cargo.contains("\"wasDeadBefore\":false") && cargo.contains("\"completed\":true"));
    }

    /** Production gate: if `(int)stats.getStatTag("intakePower") > 0`
     *  is false, the fluid-fill loop is skipped (MissionGasCollection
     *  line 46). Counter-test pinning the gate. */
    @Test
    public void gasCompletionDoesNotFillFluidWhenIntakePowerZero() throws Exception {
        int rid = buildAndAssembleRocket(8100);
        long mid = startGasMission(rid, 1000, "water", 0);
        String cargo = ok(client().execute("artest mission complete-now " + mid));
        assertFalse("complete-now must not error: " + cargo, cargo.contains("\"error\""));
        // Either no rocket re-spawned, or no fluid entries — both
        // represent the no-fill branch (production also spawns the
        // rocket entity in this path; we pin the empty-fluid invariant).
        assertTrue("intakePower=0 → no fluid entries: " + cargo,
                cargo.contains("\"fluidEntries\":0"));
    }

    /** The gas completion path constructs an EntityStationDeployedRocket
     *  (MissionGasCollection line 60), distinguishing it from the ore
     *  path (which spawns a plain EntityRocket). Pin via a presence
     *  check in the dim's entity list after completion. */
    @Test
    public void gasCompletionRespawnsRocketInLaunchDim() throws Exception {
        int rid = buildAndAssembleRocket(8200);
        long mid = startGasMission(rid, 1000, "water", 10);
        String cargo = ok(client().execute("artest mission complete-now " + mid));
        assertFalse("complete-now must not error: " + cargo, cargo.contains("\"error\""));
        // rocketCount > 0 confirms at least one rocket entity is in
        // the launch dim near the launch coords post-completion. The
        // type discrimination (StationDeployed vs plain) is checked
        // via the ore counter-test in MissionOreCompletionTest.
        assertTrue("at least one rocket entity must exist near launch coords after gas completion: "
                        + cargo,
                cargo.contains("\"rocketCount\":") && !cargo.contains("\"rocketCount\":0"));
    }

    /** Strong contract: with intakePower>0 AND a rocket carrying fluid
     *  tiles (TileFluidTank, exposing FLUID_HANDLER capability) the
     *  gas completion fills each fluid tile with exactly 64000 mB of
     *  the configured fluid (MissionGasCollection line 50:
     *  {@code fill(new FluidStack(type, 64000), true)}).
     *  Uses the `with-fluid-cargo` fixture variant that swaps 2 of 6
     *  fuel tanks for liquidTank blocks so StorageChunk.liquidTiles is
     *  non-empty. */
    @Test
    public void gasCompletionFillsRocketFluidTilesWithConfiguredFluid() throws Exception {
        int rid = buildAndAssembleRocket(8300, "with-fluid-cargo");
        long mid = startGasMission(rid, 1000, "oxygen", 10);
        String cargo = ok(client().execute("artest mission complete-now " + mid));
        assertFalse("complete-now must not error: " + cargo, cargo.contains("\"error\""));
        // fluidEntries > 0 — fill loop ran on at least one TE. Exact
        // count depends on whether the original EntityRocket still
        // lingers next to the freshly spawned StationDeployedRocket
        // (both share the same StorageChunk via reference). Loose pin
        // avoids that ambiguity.
        assertFalse("fluidEntries must be > 0 (production filled fluid tiles): " + cargo,
                cargo.contains("\"fluidEntries\":0"));
        // Each filled tile holds 64000 mB of oxygen — production literal
        // at MissionGasCollection.java:50 (FluidStack(type, 64000)).
        assertTrue("fluid contents must include oxygen 64000 mB: " + cargo,
                cargo.contains("\"type\":\"oxygen\"") && cargo.contains("\"amount\":64000"));
    }
}
