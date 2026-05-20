package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-07 Phase 4 — descent + landing under the REAL server
 * tick loop.
 *
 * <p>Earlier drafts of this suite drove {@code rocket.onUpdate()} via a
 * synthetic {@code /artest rocket tick} probe. That worked for the
 * state-machine gates but skirted the production environment: real
 * collision data depends on neighbour chunks being loaded, real
 * motion-integration happens on the server tick thread, and the
 * landing-detection branch (line 1284 of {@code EntityRocket.onUpdate})
 * relies on {@code move()} consulting the chunk's collision shapes.
 *
 * <p>The reliable substitute is a Forge chunk-loading ticket
 * (registered via {@code WorldEvents} mod-side, dispensed by the new
 * {@code /artest chunk forceload} probe). Holding the chunk hot lets
 * the headless dedicated server tick the rocket entity through its
 * production code paths exactly as a real game session would. The
 * {@code /artest server wait <dim> <ticks>} probe blocks the test
 * thread until {@code worldserver.getTotalWorldTime()} has advanced by
 * the requested number of ticks.
 *
 * <p>Test method names suffixed {@code _realTick} to make it explicit
 * which path is exercised.
 */
public class RocketDescentLandingTest extends AbstractSharedServerTest {

    private static final int DESCENT_TIMER = 40; // mirrors EntityRocket.DESCENT_TIMER

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern TICKS_EXISTED = Pattern.compile("\"ticksExisted\":(-?\\d+)");
    private static final Pattern LANDED_COUNT = Pattern.compile("\"landed\":(-?\\d+)");
    private static final Pattern POS_Y_FIELD = Pattern.compile("\"posY\":(-?[0-9.E]+)");

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private static int gi(Pattern p, String s, String label) {
        Matcher m = p.matcher(s);
        if (!m.find()) throw new AssertionError("could not parse " + label + ": " + s);
        return Integer.parseInt(m.group(1));
    }

    // No per-test cleanup of chunk tickets: releasing a Forge chunk
    // ticket on an inhabited chunk has been observed to stall the
    // shared dedicated-server harness for >30 s (likely chunk-unload
    // bookkeeping over entities still in those chunks). We let the
    // tickets leak for the duration of the class — they're freed
    // implicitly when the harness shuts down at @AfterClass. Each test
    // picks a position-disjoint chunk so leaked tickets do not bleed
    // into other tests.

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

    /** Force-load a 3×3 grid of chunks centered on (worldX, worldZ) in
     *  dim {@code dim}. Three chunks each side covers any rocket descent
     *  within ~16 blocks of the center — generous given the rocket sits
     *  in a single chunk. */
    private void forceLoadChunksAround(int dim, int worldX, int worldZ) throws Exception {
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        for (int dxc = -1; dxc <= 1; dxc++) {
            for (int dzc = -1; dzc <= 1; dzc++) {
                ok(client().execute(
                        "artest chunk forceload " + dim + " " + (cx + dxc) + " " + (cz + dzc)));
            }
        }
    }

    @Test
    public void rocketTickProbeReportsTicksExistedInResponse() throws Exception {
        // Probe-surface sanity: /artest rocket tick must succeed and
        // expose ticksExisted in the response. Used by the explicit
        // synthetic-tick path in Phase 5 (failure-mode tests).
        int id = buildAndAssemble(6000, 64, 500);
        String tickResp = ok(client().execute("artest rocket tick " + id + " 5"));
        assertTrue("tick probe must succeed: " + tickResp,
                tickResp.contains("\"ok\":true"));
        assertTrue("tick probe response must expose ticksExisted: " + tickResp,
                tickResp.contains("\"ticksExisted\":"));
        int t = gi(TICKS_EXISTED, tickResp, "ticksExisted from tick response");
        assertTrue("ticksExisted must be non-negative: " + t, t >= 0);
    }

    @Test
    public void descentTimerGateFlipsInFlightUnderRealTicks_realTick() throws Exception {
        // Production gate (EntityRocket.onUpdate line 1047):
        //   if (ticksExisted > DESCENT_TIMER && isInOrbit() && !isInFlight())
        //       setInFlight(true);
        //
        // Setup under REAL server ticking:
        //   - assemble + force-load the rocket's chunk
        //   - state: orbit=true, flight=false, ticksExisted=DESCENT_TIMER+1
        //   - server wait 5 ticks → onUpdate runs at least once →
        //     gate fires → isInFlight flips to true.
        int baseX = 6100;
        int baseZ = 500;
        int id = buildAndAssemble(baseX, 64, baseZ);
        forceLoadChunksAround(0, baseX, baseZ);

        ok(client().execute("artest rocket set-state " + id
                + " orbit=true flight=false ticksExisted=" + (DESCENT_TIMER + 1)
                + " posY=300 motionY=0"));

        ok(client().execute("artest server wait 0 5"));

        String info = ok(client().execute("artest rocket info " + id));
        assertTrue("descent gate must flip isInFlight under real ticking: " + info,
                info.contains("\"isInFlight\":true"));
    }

    @Test
    public void tickBeforeDescentTimerKeepsFlightOff_realTick() throws Exception {
        // Counter-test under real ticking: with ticksExisted well below
        // DESCENT_TIMER, even a few real server ticks must NOT flip the
        // gate. Pins that the gate is correctly conditional on the timer.
        int baseX = 6200;
        int baseZ = 500;
        int id = buildAndAssemble(baseX, 64, baseZ);
        forceLoadChunksAround(0, baseX, baseZ);

        ok(client().execute("artest rocket set-state " + id
                + " orbit=true flight=false ticksExisted=5 posY=300 motionY=0"));

        ok(client().execute("artest server wait 0 5"));

        String info = ok(client().execute("artest rocket info " + id));
        // ticksExisted will have advanced by up to ~5 under real ticking;
        // the gate threshold (DESCENT_TIMER=40) is still not crossed, so
        // isInFlight remains false.
        int t = gi(TICKS_EXISTED, info, "ticksExisted after");
        assertTrue("ticksExisted should remain below the descent timer "
                + "(have " + t + ", DESCENT_TIMER=" + DESCENT_TIMER + ")", t <= DESCENT_TIMER);
        assertTrue("isInFlight must NOT be set before descent timer expires: " + info,
                info.contains("\"isInFlight\":false"));
    }

    @Test
    public void inFlightDescentApplesGravityUnderRealTicks_realTick() throws Exception {
        // Production line 1260: when isInOrbit AND descending (motionY
        // negative or burning false), motionY decreases on every tick.
        // After 5 real ticks the rocket's posY must have dropped below
        // its starting altitude. Pin: gravity actually integrates.
        int baseX = 6400;
        int baseZ = 500;
        int id = buildAndAssemble(baseX, 64, baseZ);
        forceLoadChunksAround(0, baseX, baseZ);

        ok(client().execute("artest rocket set-state " + id
                + " orbit=true flight=true ticksExisted=" + (DESCENT_TIMER + 5)
                + " posY=300 motionY=0"));

        ok(client().execute("artest server wait 0 5"));

        String info = ok(client().execute("artest rocket info " + id));
        Matcher m = POS_Y_FIELD.matcher(info);
        assertTrue("info must expose posY: " + info, m.find());
        double posYAfter = Double.parseDouble(m.group(1));
        assertTrue("gravity must have pulled the rocket downwards under "
                + "real ticking (posY=" + posYAfter + ", started at 300)",
                posYAfter < 300.0);
    }

    @Test
    public void landedEventFiresOnGroundCollisionUnderRealTicks_realTick() throws Exception {
        // Drive the line-1284 landed branch via REAL ticking with the
        // rocket's chunk force-loaded:
        //   - 5×5 stone floor at y=64
        //   - orbit=true, flight=true, posY=66, motionY=-10
        //   - wait 6 ticks → move() collides with stone → RocketLandedEvent.
        int baseX = 6300;
        int baseY = 64;
        int baseZ = 500;
        int id = buildAndAssemble(baseX, baseY, baseZ);
        forceLoadChunksAround(0, baseX, baseZ);

        ok(client().execute("artest fill 0 " + (baseX - 2) + " " + baseY + " " + (baseZ - 2)
                + " " + (baseX + 2) + " " + baseY + " " + (baseZ + 2) + " minecraft:stone"));

        String countsBefore = ok(client().execute("artest rocket event-counts-full"));
        int landedBefore = gi(LANDED_COUNT, countsBefore, "landed before");

        ok(client().execute("artest rocket set-state " + id
                + " orbit=true flight=true ticksExisted=" + (DESCENT_TIMER + 5)
                + " posY=" + (baseY + 2) + " motionY=-10"));

        ok(client().execute("artest server wait 0 6"));

        String countsAfter = ok(client().execute("artest rocket event-counts-full"));
        int landedAfter = gi(LANDED_COUNT, countsAfter, "landed after");
        assertTrue("RocketLandedEvent must fire on ground collision under real ticks: "
                + landedBefore + " → " + landedAfter, landedAfter > landedBefore);

        String info = ok(client().execute("artest rocket info " + id));
        assertTrue("production must clear isInFlight on landing: " + info,
                info.contains("\"isInFlight\":false"));
        assertTrue("production must clear isInOrbit on landing: " + info,
                info.contains("\"isInOrbit\":false"));
    }

    @Test
    public void dismantleAfterAssemblePastesBlocksBackAtRocketFootprint() throws Exception {
        // EntityRocket.deconstructRocket (line 1898) calls
        //   storage.pasteInWorld(world, posX - sizeX/2, posY, posZ - sizeZ/2)
        // Verify the storage chunk's contents land back in the world.
        // (This test doesn't need real ticking — dismantle is synchronous —
        // but it does need the destination chunk loaded, which the fill
        // probe pulls in automatically.)
        int id = buildAndAssemble(6500, 64, 500);

        String info = ok(client().execute("artest rocket info " + id));
        Matcher mY = POS_Y_FIELD.matcher(info);
        assertTrue("info must expose posY: " + info, mY.find());
        int posY = (int) Double.parseDouble(mY.group(1));

        String dismantleResp = ok(client().execute("artest rocket dismantle " + id));
        assertTrue("dismantle must succeed: " + dismantleResp,
                dismantleResp.contains("\"ok\":true"));

        boolean foundNonAir = false;
        outer:
        for (int dx = -2; dx <= 2 && !foundNonAir; dx++) {
            for (int dz = -2; dz <= 2 && !foundNonAir; dz++) {
                for (int dy = 0; dy <= 4 && !foundNonAir; dy++) {
                    String blockResp = ok(client().execute(
                            "artest block at 0 " + (6500 + dx) + " " + (posY + dy)
                                    + " " + (500 + dz)));
                    if (!blockResp.contains("\"isAir\":true")) {
                        foundNonAir = true;
                        break outer;
                    }
                }
            }
        }
        assertTrue("dismantle must paste at least one non-air block back",
                foundNonAir);
    }

    @Test
    public void chunkAnchorProbeRoundTrips() throws Exception {
        // Probe-surface sanity: forceload + release for a single chunk
        // must succeed and return ok=true. The list endpoint reflects
        // the active ticket set. release-all clears them.
        String fl = ok(client().execute("artest chunk forceload 0 100 100"));
        assertTrue("forceload must succeed: " + fl, fl.contains("\"ok\":true"));

        String list = ok(client().execute("artest chunk list"));
        assertTrue("list must include the ticket key: " + list,
                list.contains("0:100:100"));

        String rel = ok(client().execute("artest chunk release 0 100 100"));
        assertTrue("release must succeed: " + rel, rel.contains("\"ok\":true"));

        String listAfter = ok(client().execute("artest chunk list"));
        assertFalse("list must not include released ticket: " + listAfter,
                listAfter.contains("0:100:100"));
    }
}
