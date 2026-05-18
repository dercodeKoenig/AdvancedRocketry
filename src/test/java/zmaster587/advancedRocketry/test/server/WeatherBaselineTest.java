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
import static org.junit.Assert.fail;

/**
 * SMART §7.5 — weather baseline / B1 regression.
 *
 * Pre-writes a 2-planet fixture XML, sets rain on the overworld, observes both
 * AR planets. After the B1 Mixin weather wrapper landed, per-dimension weather
 * is the only supported behaviour: rain on the overworld must NOT propagate to
 * AR planets, and each AR planet's {@code WorldInfo} must be the
 * {@code ARWeatherWorldInfo} wrapper.
 */
public class WeatherBaselineTest {

    private static final int FIXTURE_DIM_A = 9101;
    private static final int FIXTURE_DIM_B = 9102;

    private Path workDir;
    private RealDedicatedServerHarness harness;

    @Before
    public void writeTwoPlanetFixture() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -Dforge.test.harness.enabled=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));

        workDir = Files.createTempDirectory("forge-server-weather-baseline-");
        Path arConfigDir = workDir.resolve("config").resolve("advRocketry");
        Files.createDirectories(arConfigDir);

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
    public void weatherPropagationMatchesExpectedMode() throws Exception {
        harness = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        String dimList = String.join("\n", harness.client().execute("artest dim list"));
        assertTrue("fixture dim A not registered: " + dimList,
                dimList.contains(String.valueOf(FIXTURE_DIM_A)));
        assertTrue("fixture dim B not registered: " + dimList,
                dimList.contains(String.valueOf(FIXTURE_DIM_B)));

        harness.client().execute("artest weather set 0 clear 12000");
        String setOver = String.join("\n", harness.client().execute("artest weather set 0 rain 12000"));
        assertTrue("weather set on overworld failed: " + setOver, setOver.contains("\"ok\":true"));

        String w0 = String.join("\n", harness.client().execute("artest weather get 0"));
        String wA = String.join("\n", harness.client().execute("artest weather get " + FIXTURE_DIM_A));
        String wB = String.join("\n", harness.client().execute("artest weather get " + FIXTURE_DIM_B));
        boolean overRaining = w0.contains("\"isRaining\":true");
        boolean aRaining = wA.contains("\"isRaining\":true");
        boolean bRaining = wB.contains("\"isRaining\":true");

        assertTrue("overworld failed to start raining after set: " + w0, overRaining);

        if (aRaining || bRaining) {
            fail("expected per-dimension isolation but AR dim followed overworld\n"
                    + "  overworld=" + w0 + "\n  A=" + wA + "\n  B=" + wB);
        }
        // AR planet WorldInfo MUST be the B1 wrapper. If it isn't, the
        // isolation assertion above passed for the wrong reason (e.g. server
        // tick simply didn't propagate weather yet), and we'd ship a regression.
        assertTrue("planet A is NOT wrapped: " + wA, wA.contains("ARWeatherWorldInfo"));
        assertTrue("planet B is NOT wrapped: " + wB, wB.contains("ARWeatherWorldInfo"));
    }
}
