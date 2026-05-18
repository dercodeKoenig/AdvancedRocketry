package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.client.RealClientHarness;
import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import com.google.gson.JsonObject;
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
 * SMART §7.20 — multi-planet weather isolation as exercised through a real
 * Minecraft client.
 *
 * <p><b>What this validates end-to-end.</b> Two AR planets are pre-staged via
 * XML into the harness workdir (deterministic dim ids 9301 and 9302), the
 * harness server boots and registers both, weather is set to OPPOSITE values
 * on the two dims, the real client gets cross-dim teleported via the
 * test-only {@code /artest tp <dim>} probe (which calls
 * {@code PlayerList.transferPlayerToDimension} just like the production
 * {@code /advancedrocketry goto} command would, firing
 * {@code PlayerChangedDimensionEvent} → {@code PlanetWeatherEventHandler
 * .syncToPlayer} → vanilla {@code SPacketChangeGameState}), and the
 * <em>client-side rendered</em> weather state is observed via the framework's
 * {@code report_weather} probe (forge-test-framework 0.4.1+) to match the
 * dim the player is currently in. That's the full server→packet→client→render
 * loop covered.</p>
 *
 * <p>The class does NOT extend {@link AbstractClientE2ETest} because that base
 * class's {@code @Before final} creates a fresh workdir with no AR planet
 * XML, and we need deterministic dim ids the test can target. The lifecycle
 * is reproduced inline ({@link #startBoth()} / {@link #stopBoth()}).</p>
 */
public class WeatherClientSyncE2ETest {

    private static final int DIM_A = 9301;
    private static final int DIM_B = 9302;

    private Path workDir;
    private RealDedicatedServerHarness serverHarness;
    private RealClientHarness clientHarness;

    @Before
    public void startBoth() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -D" + AbstractHeadlessServerTest.PROP_HARNESS_ENABLED + "=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        Assume.assumeTrue(
                "Client harness disabled — set -D" + AbstractClientE2ETest.PROP_CLIENT_ENABLED + "=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractClientE2ETest.PROP_CLIENT_ENABLED, "false")));

        workDir = Files.createTempDirectory("forge-client-weather-sync-");
        Path arConfigDir = workDir.resolve("config").resolve("advRocketry");
        Files.createDirectories(arConfigDir);
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<galaxy>\n"
                + "    <star name=\"Sol\" temp=\"100\" x=\"0\" y=\"0\" size=\"1.0\" "
                + "          isBlackHole=\"false\" diskAngle=\"70\" "
                + "          numPlanets=\"2\" numGasGiants=\"0\">\n"
                + planetXml("ClientPlanetA", DIM_A)
                + planetXml("ClientPlanetB", DIM_B)
                + "    </star>\n"
                + "</galaxy>\n";
        Files.write(arConfigDir.resolve("planetDefs.xml"), xml.getBytes(StandardCharsets.UTF_8));

        serverHarness = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);
        try {
            clientHarness = RealClientHarness.start(serverHarness);
        } catch (Exception startupException) {
            try {
                serverHarness.close();
            } catch (Exception cleanup) {
                startupException.addSuppressed(cleanup);
            }
            serverHarness = null;
            throw startupException;
        }
    }

    @After
    public void stopBoth() throws Exception {
        Exception deferred = null;
        if (clientHarness != null) {
            try {
                clientHarness.close();
            } catch (Exception e) {
                deferred = e;
            }
            clientHarness = null;
        }
        if (serverHarness != null) {
            try {
                serverHarness.close();
            } catch (Exception e) {
                if (deferred == null) deferred = e;
                else deferred.addSuppressed(e);
            }
            serverHarness = null;
        }
        if (deferred != null) throw deferred;
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

    @Test
    public void weatherIsolatedAcrossDimsThroughRealClient() throws Exception {
        clientHarness.bot().waitForWorld();

        // Seed deterministic, opposite weather on the two planets. /artest
        // weather set goes through world.getWorldInfo().setRaining(...), which
        // on AR planets is our ARWeatherWorldInfo wrapper.
        String setA = String.join("\n", serverHarness.client().execute(
                "artest weather set " + DIM_A + " rain 12000"));
        assertTrue("set rain on dim A failed: " + setA, setA.contains("\"ok\":true"));
        String setB = String.join("\n", serverHarness.client().execute(
                "artest weather set " + DIM_B + " clear 12000"));
        assertTrue("set clear on dim B failed: " + setB, setB.contains("\"ok\":true"));

        // Confirm the wrapper is in place on BOTH AR dims — without this the
        // isolation assertion below could pass for the wrong reason (e.g.
        // vanilla shared weather happened to differ on the two dims this
        // sample tick).
        String getA = String.join("\n",
                serverHarness.client().execute("artest weather get " + DIM_A));
        String getB = String.join("\n",
                serverHarness.client().execute("artest weather get " + DIM_B));
        assertTrue("dim A WorldInfo class should be ARWeatherWorldInfo: " + getA,
                getA.contains("ARWeatherWorldInfo"));
        assertTrue("dim B WorldInfo class should be ARWeatherWorldInfo: " + getB,
                getB.contains("ARWeatherWorldInfo"));
        assertTrue("dim A should be raining after explicit set: " + getA,
                getA.contains("\"isRaining\":true"));
        assertFalse("dim B should NOT be raining after explicit clear: " + getB,
                getB.contains("\"isRaining\":true"));

        // Teleport the client to dim A. Vanilla 1.12 /tp doesn't cross dims,
        // and /advancedrocketry goto needs an Entity sender (unreachable from
        // the harness server console). /artest tp picks the connected player
        // and calls PlayerList.transferPlayerToDimension directly — same path
        // commandGoto uses internally, but driveable from the console.
        serverHarness.client().execute("artest tp " + DIM_A);
        waitForClientDim(DIM_A);

        // The client now SEES dim A's wrapped weather. rainStrength is
        // server-driven via SPacketChangeGameState (begin/end raining +
        // strength edges), so it ramps up over a handful of ticks before
        // settling near 1.0. Poll a short window.
        JsonObject onA = waitForClientRainStrengthAtLeast(0.05f);
        assertTrue("client should be in dim A after goto: " + onA,
                onA.has("dim") && onA.get("dim").getAsInt() == DIM_A);
        assertTrue("client-visible isRaining must be true on dim A: " + onA,
                onA.get("isRaining").getAsBoolean());
        assertTrue("client rainStrength must climb above 0 on dim A: " + onA,
                onA.get("rainStrength").getAsFloat() > 0f);

        // Teleport to dim B. This is the path that fires
        // PlayerChangedDimensionEvent → PlanetWeatherEventHandler.syncToPlayer,
        // pushing the new dim's weather via SPacketChangeGameState. The
        // explicit end-raining packet should drop client-visible rain
        // immediately.
        serverHarness.client().execute("artest tp " + DIM_B);
        waitForClientDim(DIM_B);

        JsonObject onB = clientHarness.bot().reportWeather();
        assertTrue("client should be in dim B after goto: " + onB,
                onB.has("dim") && onB.get("dim").getAsInt() == DIM_B);
        assertFalse("client-visible isRaining must be FALSE on dim B (isolation across "
                        + "teleport — A→B must not carry A's rain): " + onB,
                onB.get("isRaining").getAsBoolean());

        // Server-side wrapper guarantees on dim B persist too.
        String getBAgain = String.join("\n",
                serverHarness.client().execute("artest weather get " + DIM_B));
        assertTrue("dim B wrapper must persist across teleports: " + getBAgain,
                getBAgain.contains("ARWeatherWorldInfo"));
        assertFalse("server-side dim B must remain clear: " + getBAgain,
                getBAgain.contains("\"isRaining\":true"));
    }

    /**
     * Polls until {@code bot.reportWeather().dim} matches the expected dim,
     * capped at ~10 seconds. On a successful goto the client briefly
     * disconnects from the source dim and re-spawns into the target — once
     * {@code mc.world.provider.getDimension()} == expected, the client is
     * settled.
     */
    private void waitForClientDim(int expectedDim) throws Exception {
        for (int waited = 0; waited < 200; waited += 10) {
            clientHarness.bot().waitTicks(10);
            JsonObject w = clientHarness.bot().reportWeather();
            if (w != null && w.has("dim") && w.get("dim").getAsInt() == expectedDim) {
                return;
            }
        }
        throw new AssertionError("client never reached dim " + expectedDim
                + " (last weather report: " + clientHarness.bot().reportWeather() + ")");
    }

    /**
     * After SPacketChangeGameState (begin raining + rain strength) is
     * received, {@code World.rainingStrength} starts lerping toward 1.0 at
     * +0.01/tick. Poll briefly so the test isn't flaky on the exact tick of
     * the snapshot — settling above {@code minStrength} confirms the rain
     * packet actually reached and is being applied client-side.
     */
    private JsonObject waitForClientRainStrengthAtLeast(float minStrength) throws Exception {
        JsonObject latest = clientHarness.bot().reportWeather();
        for (int waited = 0; waited < 200; waited += 10) {
            if (latest.has("rainStrength") && latest.get("rainStrength").getAsFloat() >= minStrength) {
                return latest;
            }
            clientHarness.bot().waitTicks(10);
            latest = clientHarness.bot().reportWeather();
        }
        return latest; // let the caller decide; this is a soft wait
    }
}
