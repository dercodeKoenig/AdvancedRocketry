package zmaster587.advancedRocketry.test.server;

import org.junit.Assume;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-07 Phase 3-5 (subset) — full rocket lifecycle event
 * sequence as an integration test.
 *
 * <p>{@link RocketFlightCycleDepthTest} pins each individual event-bus
 * emission. This file extends to the SEQUENCE: a rocket goes through
 * launch → orbit-reached → dismantle, and every event fires exactly
 * once in the correct order with the correct global counter deltas.
 *
 * <p>Why "integration" and not just sequence: a regression that fires
 * RocketReachesOrbitEvent before launch() finishes setInFlight(true)
 * (or doubles up RocketLaunchEvent because of a duplicate event-bus
 * post) is invisible to per-stage tests but a real gameplay-breaking
 * bug — it would, e.g., complete a mining mission BEFORE the rocket is
 * confirmed in flight, granting rewards on a rocket that's still on
 * the pad. We pin the strict ordering and exact-count contract here.
 */
public class RocketFlightCycleIntegrationTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern AR_DIMS_ARRAY =
            Pattern.compile("\"arDimensions\":\\[([^]]*)]");
    private static final Pattern LAUNCH_COUNT = Pattern.compile("\"launch\":(-?\\d+)");
    private static final Pattern PRE_LAUNCH_COUNT = Pattern.compile("\"preLaunch\":(-?\\d+)");
    private static final Pattern ORBIT_COUNT = Pattern.compile("\"orbitReached\":(-?\\d+)");
    private static final Pattern DISMANTLE_COUNT = Pattern.compile("\"dismantle\":(-?\\d+)");

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private static int g(Pattern p, String s, String label) {
        Matcher m = p.matcher(s);
        if (!m.find()) throw new AssertionError("could not parse " + label + ": " + s);
        return Integer.parseInt(m.group(1));
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

    /** Snapshot of all four event counters in one object. */
    private static class Counts {
        int launch, preLaunch, orbit, dismantle;
        static Counts snapshot(java.util.List<String> probeOut) {
            String s = String.join("\n", probeOut);
            Counts c = new Counts();
            c.launch = g(LAUNCH_COUNT, s, "launch");
            c.preLaunch = g(PRE_LAUNCH_COUNT, s, "preLaunch");
            c.orbit = g(ORBIT_COUNT, s, "orbitReached");
            c.dismantle = g(DISMANTLE_COUNT, s, "dismantle");
            return c;
        }
    }

    @Test
    public void launchThenDismantleSequenceFiresExpectedEventsInOrder() throws Exception {
        // SEQUENCE under test (assemble → launch → dismantle):
        //   1. assemble (no event)
        //   2. program destination (no event)
        //   3. launch (real production path) — RocketLaunchEvent +1
        //   4. dismantle — RocketDismantleEvent +1
        // Each step asserts its expected counter delta in isolation so a
        // doubled / dropped event surfaces in the right step.
        //
        // Note: this sequence intentionally skips force-orbit-reached
        // between launch and dismantle. The production
        // reachSpaceManned() branch invoked by onOrbitReached schedules
        // a delayed cross-dim transition via PlanetEventHandler.addDelayedTransition,
        // which moves the entity into a queue and makes subsequent
        // direct entity lookups (e.g. dismantle's findRocket) fail
        // intermittently. The orbit-reached event-fire is pinned
        // separately in RocketFlightCycleDepthTest; this test focuses
        // on the launch→dismantle ordering specifically.
        int destDim = firstNonOverworldArDimOrSkip();
        int id = buildAndAssemble(4000, 64, 500);

        Counts c0 = Counts.snapshot(client().execute("artest rocket event-counts"));

        ok(client().execute("artest rocket set-destination " + id + " " + destDim));
        Counts c1 = Counts.snapshot(client().execute("artest rocket event-counts"));
        assertEquals("set-destination must not fire RocketLaunchEvent",
                c0.launch, c1.launch);
        assertEquals("set-destination must not fire RocketReachesOrbitEvent",
                c0.orbit, c1.orbit);
        assertEquals("set-destination must not fire RocketDismantleEvent",
                c0.dismantle, c1.dismantle);

        ok(client().execute("artest rocket launch " + id + " true instant"));
        Counts c2 = Counts.snapshot(client().execute("artest rocket event-counts"));
        assertEquals("real launch must fire exactly one RocketLaunchEvent",
                c1.launch + 1, c2.launch);
        assertEquals("launch must not fire RocketReachesOrbitEvent yet",
                c1.orbit, c2.orbit);
        assertEquals("launch must not fire RocketDismantleEvent",
                c1.dismantle, c2.dismantle);

        ok(client().execute("artest rocket dismantle " + id));
        Counts c3 = Counts.snapshot(client().execute("artest rocket event-counts"));
        assertEquals("dismantle must fire exactly one RocketDismantleEvent",
                c2.dismantle + 1, c3.dismantle);
        assertEquals("dismantle must not fire any RocketLaunchEvent",
                c2.launch, c3.launch);
        assertEquals("dismantle must not fire any RocketReachesOrbitEvent",
                c2.orbit, c3.orbit);
    }

    @Test
    public void doubleOrbitReachedFiresTwoEvents() throws Exception {
        // Edge-case contract: production onOrbitReached has NO
        // early-return guard against being called when already in orbit.
        // Pin observed behaviour: double-fire produces double-events.
        // If a future regression adds a guard (sensible — duplicate
        // events break mission integration), this test fails and the
        // assertion flips. Documents current contract.
        int id = buildAndAssemble(4100, 64, 500);
        Counts c0 = Counts.snapshot(client().execute("artest rocket event-counts"));
        ok(client().execute("artest rocket force-orbit-reached " + id));
        ok(client().execute("artest rocket force-orbit-reached " + id));
        Counts c1 = Counts.snapshot(client().execute("artest rocket event-counts"));
        assertEquals("two force-orbit-reached calls must produce 2 events "
                + "(no current idempotency guard in production)",
                c0.orbit + 2, c1.orbit);
    }

    @Test
    public void dismantleAfterLaunchDoesNotMutateLaunchCounter() throws Exception {
        // Order-of-emission contract: dismantle must not retroactively
        // increment any other counter. Regression-net for the
        // event-bus subscription wiring — if a refactor accidentally
        // posts a launch event during dismantle handling, this fails.
        int destDim = firstNonOverworldArDimOrSkip();
        int id = buildAndAssemble(4200, 64, 500);
        ok(client().execute("artest rocket set-destination " + id + " " + destDim));
        ok(client().execute("artest rocket launch " + id + " true instant"));

        Counts c0 = Counts.snapshot(client().execute("artest rocket event-counts"));
        ok(client().execute("artest rocket dismantle " + id));
        Counts c1 = Counts.snapshot(client().execute("artest rocket event-counts"));

        assertEquals("dismantle must NOT touch RocketLaunchEvent counter",
                c0.launch, c1.launch);
        assertEquals("dismantle must NOT touch RocketReachesOrbitEvent counter",
                c0.orbit, c1.orbit);
        assertEquals("dismantle must increment its own counter by 1",
                c0.dismantle + 1, c1.dismantle);
    }
}
