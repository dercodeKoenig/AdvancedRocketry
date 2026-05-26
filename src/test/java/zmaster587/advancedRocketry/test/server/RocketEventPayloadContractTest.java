package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * Coverage-audit gap (Tier 2 #6) + TASK-31 — RocketEvent payload contract
 * for external subscribers.
 *
 * <p>The pre-existing event-counts probe + {@code RocketEventRecorder}
 * proves an event was POSTED, but doesn't prove what payload the
 * subscriber received. Companion mods subscribing to AR events expect
 * {@code event.getEntity()} to return the rocket and {@code event.world}
 * to be the rocket's current world.</p>
 *
 * <p>Pins — all six {@link zmaster587.advancedRocketry.api.RocketEvent}
 * subtypes are now covered for entity-id + dim payload:</p>
 * <ul>
 *   <li>{@code RocketDismantleEvent} — via {@code rocket dismantle} probe.</li>
 *   <li>{@code RocketPreLaunchEvent} — via {@code rocket launch ... prepare}.</li>
 *   <li>{@code RocketLaunchEvent} — implicitly via TASK-07 launch tests.</li>
 *   <li>{@code RocketLandedEvent} (TASK-31) — via real-tick ground
 *       collision under a force-loaded chunk + stone floor.</li>
 *   <li>{@code RocketDeOrbitingEvent} (TASK-31) — via the in-flight
 *       {@code ticksExisted == 20} branch in {@code EntityRocket.onUpdate}.</li>
 *   <li>{@code RocketReachesOrbitEvent} (TASK-31) — via the
 *       {@code force-orbit-reached} probe.</li>
 * </ul>
 *
 * <p>Together these guarantee {@code event.getEntity()} returns the
 * rocket and {@code event.world.provider.getDimension()} reports the
 * rocket's actual dim across the entire lifecycle event surface — the
 * companion-mod-facing API contract.</p>
 */
public class RocketEventPayloadContractTest extends AbstractSharedServerTest {

    private static final int DESCENT_TIMER = 40; // mirrors EntityRocket.DESCENT_TIMER

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENTITY_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern PRELAUNCH_ID = Pattern.compile("\"preLaunchEntityId\":(-?\\d+)");
    private static final Pattern PRELAUNCH_DIM = Pattern.compile("\"preLaunchDim\":(-?\\d+)");
    private static final Pattern DISMANTLE_ID = Pattern.compile("\"dismantleEntityId\":(-?\\d+)");
    private static final Pattern DISMANTLE_DIM = Pattern.compile("\"dismantleDim\":(-?\\d+)");
    private static final Pattern LANDED_ID = Pattern.compile("\"landedEntityId\":(-?\\d+)");
    private static final Pattern LANDED_DIM = Pattern.compile("\"landedDim\":(-?\\d+)");
    private static final Pattern LANDED_COUNT = Pattern.compile("\"landed\":(-?\\d+)");
    private static final Pattern DEORBIT_ID = Pattern.compile("\"deOrbitingEntityId\":(-?\\d+)");
    private static final Pattern DEORBIT_DIM = Pattern.compile("\"deOrbitingDim\":(-?\\d+)");
    private static final Pattern DEORBIT_COUNT = Pattern.compile("\"deOrbiting\":(-?\\d+)");
    private static final Pattern ORBIT_REACHED_ID = Pattern.compile("\"orbitReachedEntityId\":(-?\\d+)");
    private static final Pattern ORBIT_REACHED_DIM = Pattern.compile("\"orbitReachedDim\":(-?\\d+)");
    private static final Pattern ORBIT_REACHED_COUNT = Pattern.compile("\"orbitReached\":(-?\\d+)");

    private static final int CY = 64;
    private static final int CZ = 8000;
    private static final int CX_DISMANTLE = 8000;
    private static final int CX_PRELAUNCH = 8400;
    // TASK-31 — disjoint x offsets to avoid colliding with the existing
    // dismantle / prelaunch fixtures in the shared-harness world.
    private static final int CX_LANDED = 8800;
    private static final int CX_DEORBIT = 9200;
    private static final int CX_ORBIT_REACHED = 9600;

    @Test
    public void rocketDismantleEventCarriesRocketEntityAndWorld() throws Exception {
        int rocketId = buildAndAssemble(CX_DISMANTLE);
        // Disarm any leaked PreLaunch cancellation from sibling tests.
        exec("artest rocket disarm-prelaunch-cancel");

        // Trigger dismantle — fires RocketDismantleEvent synchronously.
        String dismantle = exec("artest rocket dismantle " + rocketId);
        assertTrue("dismantle probe must succeed: " + dismantle,
                dismantle.contains("\"ok\":true"));

        String payloads = exec("artest rocket event-payloads");
        assertEquals("RocketDismantleEvent.getEntity().getEntityId() must equal "
                        + "the dismantled rocket's id: " + payloads,
                rocketId, extract(payloads, DISMANTLE_ID));
        assertEquals("RocketDismantleEvent.world.provider.getDimension() must "
                        + "equal the rocket's current dim (overworld=0): " + payloads,
                0, extract(payloads, DISMANTLE_DIM));
    }

    @Test
    public void rocketPreLaunchEventCarriesRocketEntityAndWorld() throws Exception {
        int rocketId = buildAndAssemble(CX_PRELAUNCH);
        exec("artest rocket disarm-prelaunch-cancel");

        // Call prepareLaunch — fires RocketPreLaunchEvent.
        String launch = exec("artest rocket launch " + rocketId + " true prepare");
        assertTrue("rocket launch (prepare) must succeed: " + launch,
                launch.contains("\"ok\":true") || launch.contains("\"entityId\":"));

        String payloads = exec("artest rocket event-payloads");
        assertEquals("RocketPreLaunchEvent.getEntity().getEntityId() must equal "
                        + "the rocket's id: " + payloads,
                rocketId, extract(payloads, PRELAUNCH_ID));
        assertEquals("RocketPreLaunchEvent.world.provider.getDimension() must "
                        + "equal the rocket's current dim: " + payloads,
                0, extract(payloads, PRELAUNCH_DIM));
    }

    /**
     * TASK-31 — pin: {@code RocketLandedEvent.getEntity()} returns the
     * landing rocket and {@code event.world.provider.getDimension()}
     * reports the rocket's current dim.
     *
     * <p>Driven by the same real-tick descent + ground-collision pattern
     * {@link RocketDescentLandingTest#landedEventFiresOnGroundCollisionUnderRealTicks_realTick}
     * uses to pin event firing — extended here to also assert the
     * payload identity. The companion-mod surface (e.g. "first landing
     * on planet X" achievements) requires both id + dim to be correct;
     * counter pin alone wouldn't catch a regression that swapped entity
     * references.</p>
     */
    @Test
    public void rocketLandedEventCarriesRocketEntityAndWorld() throws Exception {
        int rocketId = buildAndAssemble(CX_LANDED);
        // Force-load the rocket's chunk so the real server tick can
        // run move() against actual collision shapes.
        forceLoadChunksAround(0, CX_LANDED, CZ);

        // 5x1x5 stone floor at y=CY so move() reports a collision under
        // the falling rocket. CY+2 is the rocket's start posY.
        exec("artest fill 0 " + (CX_LANDED - 2) + " " + CY + " " + (CZ - 2)
                + " " + (CX_LANDED + 2) + " " + CY + " " + (CZ + 2) + " minecraft:stone");

        // Reset PreLaunch cancel armor inherited from sibling tests.
        exec("artest rocket disarm-prelaunch-cancel");

        String countsBefore = exec("artest rocket event-counts-full");
        int landedBefore = extract(countsBefore, LANDED_COUNT);

        // orbit+flight gate enters the line-1284 landed branch on the
        // first real tick that move() resolves a downward collision.
        exec("artest rocket set-state " + rocketId
                + " orbit=true flight=true ticksExisted=" + (DESCENT_TIMER + 5)
                + " posY=" + (CY + 2) + " motionY=-10");
        exec("artest server wait 0 6");

        String countsAfter = exec("artest rocket event-counts-full");
        int landedAfter = extract(countsAfter, LANDED_COUNT);
        assertTrue("RocketLandedEvent must fire under the descent+collision "
                        + "pattern (counter pin guards the payload assertion "
                        + "below from passing on a stale recorder); "
                        + landedBefore + " → " + landedAfter,
                landedAfter > landedBefore);

        String payloads = exec("artest rocket event-payloads");
        assertEquals("RocketLandedEvent.getEntity().getEntityId() must equal "
                        + "the landed rocket's id: " + payloads,
                rocketId, extract(payloads, LANDED_ID));
        assertEquals("RocketLandedEvent.world.provider.getDimension() must "
                        + "equal the rocket's current dim (overworld=0): " + payloads,
                0, extract(payloads, LANDED_DIM));
    }

    /**
     * TASK-31 — pin: {@code RocketDeOrbitingEvent.getEntity()} returns
     * the rocket and {@code event.world.provider.getDimension()} reports
     * its dim.
     *
     * <p>Triggered by the {@code ticksExisted == 20} branch in
     * {@code EntityRocket.onUpdate} (line 1052): the event fires
     * exactly once per rocket, on the tick where {@code ticksExisted}
     * first becomes 20 while the rocket is in flight or orbit.</p>
     *
     * <p>Setup: assemble rocket, set {@code ticksExisted=18} (one tick
     * before the gate so a short wait fires it deterministically),
     * mark {@code orbit=true}, then wait 3 real ticks — {@code super.onUpdate()}
     * increments {@code ticksExisted} to 19, 20 (event fires), 21
     * across those ticks.</p>
     */
    @Test
    public void rocketDeOrbitingEventCarriesRocketEntityAndWorld() throws Exception {
        int rocketId = buildAndAssemble(CX_DEORBIT);
        forceLoadChunksAround(0, CX_DEORBIT, CZ);
        exec("artest rocket disarm-prelaunch-cancel");

        String countsBefore = exec("artest rocket event-counts-full");
        int deOrbitBefore = extract(countsBefore, DEORBIT_COUNT);

        // ticksExisted=18 + 3 real ticks → super.onUpdate() advances to
        // 19, 20 (gate fires here), 21. The gate runs INSIDE the same
        // onUpdate as the increment (super first, then body) so the
        // tick that bumps the counter to 20 is the one that posts the
        // event.
        exec("artest rocket set-state " + rocketId
                + " orbit=true flight=false ticksExisted=18 posY=300 motionY=0");
        exec("artest server wait 0 3");

        String countsAfter = exec("artest rocket event-counts-full");
        int deOrbitAfter = extract(countsAfter, DEORBIT_COUNT);
        assertTrue("RocketDeOrbitingEvent must fire on the tick "
                        + "ticksExisted == 20 with orbit=true: "
                        + deOrbitBefore + " → " + deOrbitAfter,
                deOrbitAfter > deOrbitBefore);

        String payloads = exec("artest rocket event-payloads");
        assertEquals("RocketDeOrbitingEvent.getEntity().getEntityId() must "
                        + "equal the rocket's id: " + payloads,
                rocketId, extract(payloads, DEORBIT_ID));
        assertEquals("RocketDeOrbitingEvent.world.provider.getDimension() must "
                        + "equal the rocket's current dim (overworld=0): " + payloads,
                0, extract(payloads, DEORBIT_DIM));
    }

    /**
     * TASK-31 — pin: {@code RocketReachesOrbitEvent.getEntity()} returns
     * the rocket and {@code event.world.provider.getDimension()} reports
     * its dim.
     *
     * <p>Driven via the {@code force-orbit-reached} probe which directly
     * invokes the production {@code EntityRocket.onOrbitReached()} method.
     * This is the same production codepath the natural flight loop hits
     * when {@code posY > stats.orbitHeight}; the probe just removes the
     * need to spin a full ascent.</p>
     *
     * <p>Rounds out payload coverage to all six {@link
     * zmaster587.advancedRocketry.api.RocketEvent} subtypes — the
     * remaining gap after TASK-31's primary Landed+DeOrbit pins.</p>
     */
    @Test
    public void rocketReachesOrbitEventCarriesRocketEntityAndWorld() throws Exception {
        int rocketId = buildAndAssemble(CX_ORBIT_REACHED);
        exec("artest rocket disarm-prelaunch-cancel");

        String countsBefore = exec("artest rocket event-counts-full");
        int orbitBefore = extract(countsBefore, ORBIT_REACHED_COUNT);

        String orbitResp = exec("artest rocket force-orbit-reached " + rocketId);
        assertTrue("force-orbit-reached probe must succeed: " + orbitResp,
                orbitResp.contains("\"ok\":true"));

        String countsAfter = exec("artest rocket event-counts-full");
        int orbitAfter = extract(countsAfter, ORBIT_REACHED_COUNT);
        assertTrue("force-orbit-reached must advance the orbitReached "
                        + "counter (sanity gate for the payload pin): "
                        + orbitBefore + " → " + orbitAfter,
                orbitAfter > orbitBefore);

        String payloads = exec("artest rocket event-payloads");
        assertEquals("RocketReachesOrbitEvent.getEntity().getEntityId() must "
                        + "equal the rocket's id: " + payloads,
                rocketId, extract(payloads, ORBIT_REACHED_ID));
        assertEquals("RocketReachesOrbitEvent.world.provider.getDimension() "
                        + "must equal the rocket's current dim (overworld=0): "
                        + payloads,
                0, extract(payloads, ORBIT_REACHED_DIM));
    }

    // ─── helpers ───────────────────────────────────────────────────────

    /** Force-load a 3×3 grid of chunks centered on {@code (worldX, worldZ)}
     *  in dim {@code dim}. Required for real-tick paths so the rocket's
     *  chunk stays loaded while the server ticks it. Mirrors the helper
     *  in {@link RocketDescentLandingTest}; we don't release tickets
     *  per-test because release-on-inhabited-chunks has been observed to
     *  stall the shared dedicated-server harness — each test picks a
     *  position-disjoint chunk via its own {@code CX_*} constant. */
    private static void forceLoadChunksAround(int dim, int worldX, int worldZ) throws Exception {
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        for (int dxc = -1; dxc <= 1; dxc++) {
            for (int dzc = -1; dzc <= 1; dzc++) {
                exec("artest chunk forceload " + dim + " " + (cx + dxc) + " " + (cz + dzc));
            }
        }
    }

    private int buildAndAssemble(int baseX) throws Exception {
        int cx1 = (baseX - 2) >> 4, cz1 = (CZ - 2) >> 4;
        int cx2 = (baseX + 7) >> 4, cz2 = (CZ + 7) >> 4;
        exec("artest chunk warmup 0 " + cx1 + " " + cz1 + " " + cx2 + " " + cz2);
        exec("artest fill 0 " + (baseX - 2) + " " + (CY + 1) + " " + (CZ - 2)
                + " " + (baseX + 7) + " " + (CY + 10) + " " + (CZ + 7)
                + " minecraft:air");
        String fixture = exec("artest fixture rocket 0 " + baseX + " " + CY + " " + CZ
                + " simple");
        assertTrue("fixture build failed: " + fixture, fixture.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("no builderPos: " + fixture, bp.find());
        String assemble = exec("artest rocket assemble 0 "
                + bp.group(1) + " " + bp.group(2) + " " + bp.group(3));
        assertTrue("assemble must succeed: " + assemble,
                assemble.contains("\"ok\":true"));
        Matcher eim = ENTITY_ID.matcher(assemble);
        assertTrue("no entityId: " + assemble, eim.find());
        return Integer.parseInt(eim.group(1));
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }
}
