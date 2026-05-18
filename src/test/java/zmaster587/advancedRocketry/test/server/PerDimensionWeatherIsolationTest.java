package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.5 + §12 (per_dimension definition of done) — direct cross-dim
 * isolation: rain set on planet A must NOT leak to planet B or the overworld,
 * and vice versa.
 *
 * Complementary to {@link WeatherBaselineTest}, which only checks the
 * "overworld → planets" direction. This test exercises the "planet ↔ planet"
 * and "planet → overworld" directions, which the B1 wrapper has to handle
 * symmetrically.
 *
 * Setup pattern mirrors {@code WeatherBaselineTest}: pre-stage a 2-planet
 * fixture XML in the harness workdir, start the harness, drive it via /artest.
 */
public class PerDimensionWeatherIsolationTest {

    private static final int FIXTURE_DIM_A = 9201;
    private static final int FIXTURE_DIM_B = 9202;

    private Path workDir;
    private RealDedicatedServerHarness harness;

    @Before
    public void writeFixture() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -Dforge.test.harness.enabled=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));

        workDir = Files.createTempDirectory("forge-server-perdim-weather-");
        Path arConfigDir = workDir.resolve("config").resolve("advRocketry");
        Files.createDirectories(arConfigDir);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<galaxy>\n"
                + "    <star name=\"Sol\" temp=\"100\" x=\"0\" y=\"0\" size=\"1.0\" "
                + "          isBlackHole=\"false\" diskAngle=\"70\" "
                + "          numPlanets=\"2\" numGasGiants=\"0\">\n"
                + planetXml("PerDimPlanetA", FIXTURE_DIM_A)
                + planetXml("PerDimPlanetB", FIXTURE_DIM_B)
                + "    </star>\n"
                + "</galaxy>\n";
        Files.write(arConfigDir.resolve("planetDefs.xml"), xml.getBytes(StandardCharsets.UTF_8));
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

    @After
    public void stopHarness() throws Exception {
        if (harness != null) harness.close();
    }

    @Test
    public void rainOnPlanetADoesNotLeakToBOrOverworld() throws Exception {
        harness = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        // Clear everywhere first so the test starts from a known baseline.
        harness.client().execute("artest weather set 0 clear 12000");
        harness.client().execute("artest weather set " + FIXTURE_DIM_A + " clear 12000");
        harness.client().execute("artest weather set " + FIXTURE_DIM_B + " clear 12000");

        // Rain on A only.
        String setA = String.join("\n",
                harness.client().execute("artest weather set " + FIXTURE_DIM_A + " rain 12000"));
        assertTrue("set rain on A failed: " + setA, setA.contains("\"ok\":true"));

        String wA = String.join("\n", harness.client().execute("artest weather get " + FIXTURE_DIM_A));
        String wB = String.join("\n", harness.client().execute("artest weather get " + FIXTURE_DIM_B));
        String w0 = String.join("\n", harness.client().execute("artest weather get 0"));

        assertTrue("planet A should be raining after explicit set: " + wA,
                wA.contains("\"isRaining\":true"));
        assertFalse("planet B must NOT be raining (rain set on A only): " + wB,
                wB.contains("\"isRaining\":true"));
        assertFalse("overworld must NOT be raining (rain set on planet A only): " + w0,
                w0.contains("\"isRaining\":true"));
        // Wrapper must actually be installed — otherwise the isolation above
        // could pass for the wrong reason (no propagation simply because we
        // changed nothing on the other dims yet).
        assertTrue("planet A WorldInfo class should be ARWeatherWorldInfo: " + wA,
                wA.contains("ARWeatherWorldInfo"));
        assertTrue("planet B WorldInfo class should be ARWeatherWorldInfo: " + wB,
                wB.contains("ARWeatherWorldInfo"));
    }

    @Test
    public void rainOnPlanetBDoesNotLeakToAOrOverworld() throws Exception {
        // The reverse direction — guards against a one-way leak bug where A
        // is properly wrapped but B silently writes to the overworld.
        harness = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        harness.client().execute("artest weather set 0 clear 12000");
        harness.client().execute("artest weather set " + FIXTURE_DIM_A + " clear 12000");
        harness.client().execute("artest weather set " + FIXTURE_DIM_B + " clear 12000");

        harness.client().execute("artest weather set " + FIXTURE_DIM_B + " rain 12000");

        String wA = String.join("\n", harness.client().execute("artest weather get " + FIXTURE_DIM_A));
        String wB = String.join("\n", harness.client().execute("artest weather get " + FIXTURE_DIM_B));
        String w0 = String.join("\n", harness.client().execute("artest weather get 0"));

        assertTrue("planet B should be raining after explicit set: " + wB,
                wB.contains("\"isRaining\":true"));
        assertFalse("planet A must NOT be raining (rain set on B only): " + wA,
                wA.contains("\"isRaining\":true"));
        assertFalse("overworld must NOT be raining (rain set on planet B only): " + w0,
                w0.contains("\"isRaining\":true"));
    }

    @Test
    public void clearOnPlanetADoesNotClearB() throws Exception {
        // Symmetric to the rain test — clearing one planet must not clear the
        // other. Without the wrapper, /weather clear would propagate.
        harness = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        // Rain on BOTH first.
        harness.client().execute("artest weather set " + FIXTURE_DIM_A + " rain 12000");
        harness.client().execute("artest weather set " + FIXTURE_DIM_B + " rain 12000");

        String beforeA = String.join("\n",
                harness.client().execute("artest weather get " + FIXTURE_DIM_A));
        String beforeB = String.join("\n",
                harness.client().execute("artest weather get " + FIXTURE_DIM_B));
        assertTrue("planet A must be raining as precondition: " + beforeA,
                beforeA.contains("\"isRaining\":true"));
        assertTrue("planet B must be raining as precondition: " + beforeB,
                beforeB.contains("\"isRaining\":true"));

        // Clear only A.
        harness.client().execute("artest weather set " + FIXTURE_DIM_A + " clear 12000");

        String afterA = String.join("\n",
                harness.client().execute("artest weather get " + FIXTURE_DIM_A));
        String afterB = String.join("\n",
                harness.client().execute("artest weather get " + FIXTURE_DIM_B));

        assertFalse("planet A should be clear after explicit clear: " + afterA,
                afterA.contains("\"isRaining\":true"));
        assertTrue("planet B must remain raining (clear set on A only): " + afterB,
                afterB.contains("\"isRaining\":true"));
    }
}
