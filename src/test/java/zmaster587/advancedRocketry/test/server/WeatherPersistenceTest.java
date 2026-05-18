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

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.6 + §12 (DoD #9) — weather state persists across a server restart
 * for AR planets (saved-data on the overworld MapStorage, keyed by dim id).
 *
 * Previously this test exercised the overworld (dim 0), which is intentionally
 * NOT wrapped by B1 — so it was actually a vanilla persistence test in disguise.
 * Rewritten to write rain into an AR planet (where {@code ARWeatherWorldInfo}
 * is installed and {@code PlanetWeatherSavedData} is the actual persistence
 * target), then verify it survives a clean stop/start cycle on the same
 * workdir.
 *
 * Manages two harness lifecycles directly against the same workDir — not a
 * fit for {@link AbstractHeadlessServerTest} (which auto-manages a single
 * fresh-dir harness).
 */
public class WeatherPersistenceTest {

    private static final int FIXTURE_DIM = 9301;

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    @Before
    public void prepareWorkDir() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -Dforge.test.harness.enabled=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        workDir = Files.createTempDirectory("forge-server-weather-persistence-");

        // Stage the planet XML BEFORE the first boot so the dim id is stable
        // across both lifecycles.
        Path arConfigDir = workDir.resolve("config").resolve("advRocketry");
        Files.createDirectories(arConfigDir);
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<galaxy>\n"
                + "    <star name=\"Sol\" temp=\"100\" x=\"0\" y=\"0\" size=\"1.0\" "
                + "          isBlackHole=\"false\" diskAngle=\"70\" "
                + "          numPlanets=\"1\" numGasGiants=\"0\">\n"
                + "        <planet name=\"PersistencePlanet\" DIMID=\"" + FIXTURE_DIM + "\">\n"
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
                + "        </planet>\n"
                + "    </star>\n"
                + "</galaxy>\n";
        Files.write(arConfigDir.resolve("planetDefs.xml"), xml.getBytes(StandardCharsets.UTF_8));
    }

    @After
    public void closeAll() throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
    }

    @Test
    public void planetRainSurvivesRestartOnSameWorkDir() throws Exception {
        // First boot: set rain on the planet, verify the wrapper is in place.
        firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);
        firstBoot.client().execute("artest weather set " + FIXTURE_DIM + " rain 12000");
        String beforeStop = String.join("\n",
                firstBoot.client().execute("artest weather get " + FIXTURE_DIM));
        assertTrue("rain didn't take effect on first boot: " + beforeStop,
                beforeStop.contains("\"isRaining\":true"));
        assertTrue("wrapper not installed on first boot: " + beforeStop,
                beforeStop.contains("ARWeatherWorldInfo"));

        // Stop cleanly — saved-data must flush via vanilla MapStorage save.
        firstBoot.close();
        firstBoot = null;

        // Second boot on the same workdir. The planet weather wrapper
        // re-reads state from advancedrocketry_planet_weather saved-data on
        // the overworld MapStorage.
        secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);
        String after = String.join("\n",
                secondBoot.client().execute("artest weather get " + FIXTURE_DIM));
        assertTrue("planet rain DID NOT persist across restart: " + after,
                after.contains("\"isRaining\":true"));
        assertTrue("wrapper should still be installed after restart: " + after,
                after.contains("ARWeatherWorldInfo"));
    }
}
