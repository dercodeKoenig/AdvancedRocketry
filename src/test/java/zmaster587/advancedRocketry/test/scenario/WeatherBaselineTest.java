package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.HeadlessGameTest;
import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import zmaster587.advancedRocketry.test.AdvancedRocketryTestConstants;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * SMART §7.5 — weather baseline + future B1 regression.
 *
 * Pre-writes a 2-planet fixture XML, then sets rain on planet A and observes
 * planet B. Behavior depends on the {@code expectedWeatherMode} flag:
 *
 * <ul>
 *   <li>{@code shared} (pre-B1 default): rain on A → rain visible on B (the
 *       current implementation shares the world-time / weather state across all
 *       dimensions).</li>
 *   <li>{@code per_dimension} (post-B1): rain on A → B remains clear.</li>
 * </ul>
 *
 * <p>This test asserts the configured baseline matches reality so that a B1
 * refactor flipping behavior is immediately visible: change the
 * {@code -Pweather=...} arg AFTER the refactor and the same suite re-runs the
 * post-B1 contract.</p>
 */
public class WeatherBaselineTest implements HeadlessGameTest {

    private static final int FIXTURE_DIM_A = 9101;
    private static final int FIXTURE_DIM_B = 9102;

    private Path workDir;
    private RealDedicatedServerHarness harness;

    @Override public String id() { return "ar.scenario.weather_baseline"; }
    @Override public String category() { return "P0/weather"; }
    @Override public boolean required() { return true; }
    @Override public int timeoutTicks() { return 1; }

    @Override
    public void setUp(TestContext context) throws Exception {
        if (!HarnessBoundScenario.isHarnessEnabled()) {
            context.note("server harness disabled — skip");
            return;
        }
        workDir = Files.createTempDirectory("forge-server-weather-baseline-");
        Path arConfigDir = workDir.resolve("config").resolve("advRocketry");
        Files.createDirectories(arConfigDir);

        // Two AR planets at distinct dimensions, both orbiting Sol. Identical
        // parameters except DIMID so the only difference observable from
        // /artest weather is per-dim isolation behavior.
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<galaxy>\n"
                + "    <star name=\"Sol\" temp=\"100\" x=\"0\" y=\"0\" size=\"1.0\" "
                + "          isBlackHole=\"false\" diskAngle=\"70\" "
                + "          numPlanets=\"2\" numGasGiants=\"0\">\n"
                + planetXml("WeatherPlanetA", FIXTURE_DIM_A)
                + planetXml("WeatherPlanetB", FIXTURE_DIM_B)
                + "    </star>\n"
                + "</galaxy>\n";
        Files.write(arConfigDir.resolve("planetDefs.xml"), xml.getBytes(StandardCharsets.UTF_8));
        context.note("wrote 2-planet weather baseline fixture");
    }

    private static String planetXml(String name, int dim) {
        return "        <planet name=\"" + name + "\" DIMID=\"" + dim + "\">\n"
                + "            <isKnown>true</isKnown>\n"
                + "            <fogColor>0.5,0.5,0.5</fogColor>\n"
                + "            <skyColor>0.4,0.6,0.9</skyColor>\n"
                + "            <gravitationalMultiplier>100</gravitationalMultiplier>\n"
                + "            <orbitalDistance>100</orbitalDistance>\n"
                + "            <orbitalTheta>0</orbitalTheta>\n"
                + "            <orbitalPhi>0</orbitalPhi>\n"
                + "            <retrograde>false</retrograde>\n"
                + "            <averageTemperature>250</averageTemperature>\n"
                + "            <rotationalPeriod>24000</rotationalPeriod>\n"
                + "            <atmosphereDensity>100</atmosphereDensity>\n"
                + "            <generateCraters>false</generateCraters>\n"
                + "            <generateCaves>true</generateCaves>\n"
                + "            <generateVolcanos>false</generateVolcanos>\n"
                + "        </planet>\n";
    }

    @Override
    public TestStatus tick(TestContext context) throws Exception {
        if (workDir == null) return TestStatus.SKIPPED;

        String mode = AdvancedRocketryTestConstants.expectedWeatherMode();
        context.note("expected mode: " + mode);

        try {
            harness = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);
        } catch (Throwable t) {
            context.note("harness start failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return TestStatus.SKIPPED;
        }

        // 1. Make sure both fixture dims are registered.
        String dimList = String.join("\n", harness.client().execute("artest dim list"));
        if (!dimList.contains(String.valueOf(FIXTURE_DIM_A)) || !dimList.contains(String.valueOf(FIXTURE_DIM_B))) {
            context.note("fixture dims not registered: " + dimList);
            return TestStatus.FAILED;
        }

        // AR planets currently use DerivedWorldInfo backed by the overworld
        // (dim=0). To exercise the shared-vs-isolated invariant, we set weather
        // on the overworld and observe what propagates to the AR planets.
        // Pre-B1 (shared): rain on overworld → both AR planets observe rain.
        // Post-B1 (per_dimension): rain on overworld → AR planets unaffected
        // (each has its own WorldInfo).
        harness.client().execute("artest weather set 0 clear 12000");
        String setOver = String.join("\n", harness.client().execute("artest weather set 0 rain 12000"));
        if (!setOver.contains("\"ok\":true")) {
            context.note("weather set on overworld failed: " + setOver);
            return TestStatus.FAILED;
        }

        // Observe overworld (sanity check) + both AR planets.
        String w0 = String.join("\n", harness.client().execute("artest weather get 0"));
        String wA = String.join("\n", harness.client().execute("artest weather get " + FIXTURE_DIM_A));
        String wB = String.join("\n", harness.client().execute("artest weather get " + FIXTURE_DIM_B));
        boolean overRaining = w0.contains("\"isRaining\":true");
        boolean aRaining = wA.contains("\"isRaining\":true");
        boolean bRaining = wB.contains("\"isRaining\":true");

        if (!overRaining) {
            context.note("overworld failed to start raining after set: " + w0);
            return TestStatus.FAILED;
        }

        if (AdvancedRocketryTestConstants.WEATHER_MODE_SHARED.equals(mode)) {
            // Pre-B1: derived-info means AR dims see the overworld weather state.
            if (!aRaining || !bRaining) {
                context.note("expected 'shared' baseline but AR dims didn't follow overworld"
                        + "\n  overworld=" + w0 + "\n  A=" + wA + "\n  B=" + wB);
                return TestStatus.FAILED;
            }
            context.note("shared baseline confirmed: overworld + both AR planets report rain");
            return TestStatus.PASSED;
        } else {
            // Post-B1: per-dimension. Overworld raining; AR dims independent (clear).
            if (aRaining || bRaining) {
                context.note("expected 'per_dimension' isolation but AR dim followed overworld"
                        + "\n  overworld=" + w0 + "\n  A=" + wA + "\n  B=" + wB);
                return TestStatus.FAILED;
            }
            context.note("per-dimension isolation confirmed: overworld=rain, AR planets=clear");
            return TestStatus.PASSED;
        }
    }

    @Override
    public void tearDown(TestContext context) throws Exception {
        if (harness != null) harness.close();
    }
}
