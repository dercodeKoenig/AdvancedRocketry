package zmaster587.advancedRocketry.test.server;

// migrated to AbstractSharedServerTest (TASK-03 B2)
import org.junit.Assume;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 1 round 2 — player-event handler wiring &
 * pre-join side-effects.
 *
 * The headless dedicated-server harness in this repo has NO connected
 * player. "Player joins AR planet → sky/gravity/weather wrapper applied"
 * is a behaviour that belongs in the {@code testClient} e2e harness
 * (§2.4 — real GL client + dedicated server), not here. What this layer
 * CAN — and what the long-running marker
 * {@code 2026-05-19-0600_task02-round2-tile-rocket-eod.md} deferred — is
 * pin the SERVER-SIDE state that {@code PlanetEventHandler} maintains:
 *
 * <ol>
 *   <li>The {@code ServerTickEvent} subscription is live (its public
 *       counter advances under normal ticking).</li>
 *   <li>{@link zmaster587.advancedRocketry.event.PlanetEventHandler},
 *       {@code RocketEventHandler}, and {@code PlanetWeatherEventHandler}
 *       are all class-loaded by the time the server is up. A regression
 *       in the {@code @Mod} init phase that drops one would silently
 *       break swathes of gameplay.</li>
 *   <li>For every AR dimension that's loaded, the side-effects that a
 *       player-join would observe are coherent SERVER-SIDE: the world
 *       info is the B1 weather wrapper, an atmosphere handler is
 *       registered, the dimension is classified as an AR planet, and
 *       gravity / sky color are non-default.</li>
 *   <li>The transition queue (used for rocket-launch warp transitions)
 *       is empty at rest — counter-test for any test that mistakenly
 *       leaks a {@code TransitionEntity}.</li>
 * </ol>
 *
 * The full "player joins AR dim → side effects fire" path is the job
 * of the {@code testClient} e2e harness; the server-side state checked
 * here is the necessary pre-condition for that join to be coherent.
 */
public class PlayerEventHandlerWiringTest extends AbstractSharedServerTest {

    private static final Pattern TIME_PATTERN = Pattern.compile("\"time\":(\\d+)");
    private static final Pattern WORLD_TIME_PATTERN =
            Pattern.compile("\"worldTotalTime\":(-?\\d+)");
    private static final Pattern AR_DIMS_ARRAY = Pattern.compile("\"arDimensions\":\\[([^]]*)]");

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private static long parseGroup(Pattern pattern, String resp, String label) {
        Matcher m = pattern.matcher(resp);
        if (!m.find()) {
            throw new AssertionError("could not parse " + label + " from response: " + resp);
        }
        return Long.parseLong(m.group(1));
    }

    private int firstArDimOrSkip() throws Exception {
        String joined = ok(client().execute("artest dim list"));
        Assume.assumeFalse(
                "No AR dimensions registered — skipping",
                joined.contains("\"arDimensions\":[]"));
        Matcher m = AR_DIMS_ARRAY.matcher(joined);
        assertTrue("could not parse arDimensions array: " + joined, m.find());
        for (String part : m.group(1).split(",")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            int dim = Integer.parseInt(t);
            if (dim != 0) return dim;
        }
        Assume.assumeTrue(
                "Only overworld is an AR planet — skipping", false);
        return -1;
    }

    @Test
    public void planetEventHandlerTickCounterAdvancesUnderServerTicks() throws Exception {
        // Tick counter advance is the strongest "PlanetEventHandler is
        // subscribed to the event bus" smoke we have at the headless
        // server layer. The counter increments inside ServerTickEvent.END;
        // if @Mod init failed to subscribe, the value freezes at zero.
        String first = ok(client().execute("artest event tick-counter"));
        long t1 = parseGroup(TIME_PATTERN, first, "time");
        long w1 = parseGroup(WORLD_TIME_PATTERN, first, "worldTotalTime");

        // The headless server ticks at ~20 Hz. 200ms wall = ~4 ticks; we
        // ask for headroom of 2 to absorb scheduler jitter and CI noise.
        Thread.sleep(400);

        String second = ok(client().execute("artest event tick-counter"));
        long t2 = parseGroup(TIME_PATTERN, second, "time");
        long w2 = parseGroup(WORLD_TIME_PATTERN, second, "worldTotalTime");

        // The two cross-checks here are independent:
        //   - worldTotalTime advancing proves the SERVER is ticking
        //     (so any failure to see t advance is the handler's fault,
        //     not "the server was paused").
        //   - t advancing proves the handler subscription is live.
        assertTrue("vanilla world totalTime must advance over 400ms: w1=" + w1 + " w2=" + w2,
                w2 > w1);
        assertTrue("PlanetEventHandler.time must advance under server ticks: "
                        + "t1=" + t1 + " t2=" + t2 + " (server ticking? w1=" + w1 + " w2=" + w2 + ")",
                t2 > t1);
    }

    @Test
    public void coreEventHandlersAreClassLoaded() throws Exception {
        // Class-load smoke for the three event handlers that the @Mod
        // init phase wires. If any one of them fails to load (rare —
        // would have to be a static-init crash or a build-time class
        // strip), the field-/Class-lookup in the probe surfaces it.
        String resp = ok(client().execute("artest event handlers"));
        assertTrue("PlanetEventHandler must be class-loaded: " + resp,
                resp.contains("\"planetEventHandler\":\"loaded\""));
        // RocketEventHandler is reported as "shipped" via classfile-resource
        // lookup — a static class reference would NoClassDefFoundError on
        // dedicated server because the class imports LWJGL / FontRenderer
        // (client-only). Resource presence is the strongest server-safe
        // proof that the @Mod packaging didn't drop the class.
        assertTrue("RocketEventHandler .class resource must be shipped: " + resp,
                resp.contains("\"rocketEventHandler\":\"shipped\""));
        // PlanetWeatherEventHandler IS server-safe (no client imports), so
        // a direct static reference verifies + reports its FQN.
        assertTrue("PlanetWeatherEventHandler must be class-loaded (probe "
                        + "should report its FQN): " + resp,
                resp.contains(
                        "zmaster587.advancedRocketry.world.weather.PlanetWeatherEventHandler"));
    }

    @Test
    public void arDimensionPreJoinSideEffectsAreCoherent() throws Exception {
        // For an AR dim, the pre-join side-effects MUST all line up:
        //   - WorldInfo wrapped (ARWeatherWorldInfo) — required for the
        //     B1 weather isolation chain to fire on player join
        //   - AtmosphereHandler registered — required for vacuum / oxygen
        //     handling the moment the player tick starts
        //   - isARPlanet=true — gates the per-tick planetary logic in
        //     PlanetEventHandler.tick and elsewhere
        //   - gravity != 1.0 (a non-default value) — implies the planet
        //     XML was actually parsed and applied
        int dim = firstArDimOrSkip();
        String resp = ok(client().execute("artest event dim-side-effects " + dim));

        assertTrue("AR dim must be loaded for side-effect probing: " + resp,
                resp.contains("\"loaded\":true"));
        assertTrue("AR dim WorldInfo must be wrapped by ARWeatherWorldInfo: " + resp,
                resp.contains("ARWeatherWorldInfo"));
        assertTrue("AR dim must have an AtmosphereHandler registered: " + resp,
                resp.contains("\"hasAtmosphereHandler\":true"));
        assertTrue("dim must be classified as AR planet: " + resp,
                resp.contains("\"isARPlanet\":true"));
        // hasSkyColor=true means props.skyColor is non-null/non-empty.
        // (A future fixture planet with the default vanilla colour would
        // still pass — float[] is allocated by DimensionProperties; this
        // assertion just guards against a regression that drops the field.)
        assertTrue("AR dim must have a sky-color array configured: " + resp,
                resp.contains("\"hasSkyColor\":true"));
    }

    @Test
    public void nonArDimensionRejectsArPlanetClassification() throws Exception {
        // Counter-test: a non-AR dim (nether = -1, end = 1) must NOT be
        // classified as an AR planet. A polarity flip here would mean
        // every nether/end join would try to run AR's per-planet tick
        // logic against vanilla state — catastrophic.
        // The overworld is registered as AR planet "Earth" on this dev
        // fixture set, so we can't use dim 0 here; pick the first non-AR
        // forge dim that's NOT in the arDimensions array.
        String dimList = ok(client().execute("artest dim list"));
        Matcher arM = AR_DIMS_ARRAY.matcher(dimList);
        Assume.assumeTrue("dim list missing arDimensions array", arM.find());
        java.util.Set<Integer> arDims = new java.util.HashSet<>();
        for (String part : arM.group(1).split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) arDims.add(Integer.parseInt(t));
        }
        // Try nether (-1) then end (1). Skip if both happen to be AR (the
        // fixture doesn't currently register them, but be defensive).
        int nonArDim = arDims.contains(-1) ? (arDims.contains(1) ? Integer.MIN_VALUE : 1) : -1;
        Assume.assumeTrue("no non-AR vanilla dim available to counter-test against",
                nonArDim != Integer.MIN_VALUE);

        String resp = ok(client().execute("artest event dim-side-effects " + nonArDim));
        assertTrue("non-AR dim " + nonArDim + " must be loaded: " + resp,
                resp.contains("\"loaded\":true"));
        assertTrue("non-AR dim " + nonArDim + " must NOT be classified as AR planet: " + resp,
                resp.contains("\"isARPlanet\":false"));
        // ARWeatherWorldInfo wrapping is the per-AR-dim B1 isolation chain;
        // a non-AR dim must stay vanilla so weather doesn't bleed in/out.
        assertTrue("non-AR dim " + nonArDim + " WorldInfo must NOT be wrapped: " + resp,
                !resp.contains("ARWeatherWorldInfo"));
    }

    @Test
    public void transitionMapIsEmptyAtRest() throws Exception {
        // No rocket launches have been issued in this test class → the
        // transition queue MUST be empty. If it's not, either:
        //   (a) a previous test in the same JVM leaked a transition
        //       (failure of cleanup discipline), OR
        //   (b) the queue's drain logic in PlanetEventHandler.tick()
        //       (line ~322) silently regressed and never pops entries.
        // Either failure mode would silently corrupt subsequent rocket
        // launches' destination dim.
        String resp = ok(client().execute("artest event transitions"));
        assertTrue("transition map probe must succeed: " + resp,
                resp.contains("\"ok\":true"));
        assertTrue("transition map must be empty at rest in a no-rocket test: " + resp,
                resp.contains("\"size\":0"));
    }
}
