package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.client.RealClientHarness;
import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10b Phase 3 — advancement triggers fired by player-event gameplay.
 *
 * <p>Pins the
 * {@link zmaster587.advancedRocketry.event.PlanetEventHandler}
 * passive trigger at lines 203-208: when a player ticks in a dim
 * named {@code "Luna"} within distanceSq &lt; 512 of (2347, 80, 67),
 * the {@code WENT_TO_THE_MOON} custom-trigger fires every 20 server
 * ticks.</p>
 *
 * <p>Two gates are exercised:</p>
 * <ul>
 *   <li><b>Name gate</b> — the dim's
 *       {@link zmaster587.advancedRocketry.dimension.DimensionProperties#getName()}
 *       must equal {@code "Luna"}. {@link #cNonLunaArDimDoesNotFireWentToTheMoon}
 *       pins the negative.</li>
 *   <li><b>Distance gate</b> — player must stand within ~22 blocks of
 *       the lander coords. {@link #dFarFromLanderCoordsOnLunaDoesNotFire}
 *       pins the negative.</li>
 * </ul>
 *
 * <p>The {@code MOON_LANDING} trigger is intentionally NOT pinned here
 * — it fires only from {@link zmaster587.advancedRocketry.entity.EntityRocket}'s
 * deorbit branch (with a human passenger), which is the rocket
 * flight-cycle suite's domain (TASK-07), not player-event handler's.</p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdvancementsE2ETest {

    private static final int DIM_LUNA = 9501;
    private static final int DIM_OTHER = 9502;

    private static final String ADV_WENT = "advancedrocketry:normal/wenttothemoon";

    private static final Pattern IS_DONE = Pattern.compile("\"isDone\":(true|false)");

    private Path workDir;
    private RealDedicatedServerHarness serverHarness;
    private RealClientHarness clientHarness;

    @Before
    public void startBoth() throws Exception {
        Assume.assumeTrue("Server harness disabled",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        Assume.assumeTrue("Client harness disabled",
                Boolean.parseBoolean(System.getProperty(
                        AbstractClientE2ETest.PROP_CLIENT_ENABLED, "false")));

        workDir = Files.createTempDirectory("forge-client-adv-pin-");
        Path arConfigDir = workDir.resolve("config").resolve("advRocketry");
        Files.createDirectories(arConfigDir);
        // The PlanetEventHandler.WENT_TO_THE_MOON gate is keyed on the
        // dim name string "Luna", NOT on ARConfiguration.MoonId. So we
        // explicitly name one custom dim "Luna" and a second one
        // "AlsoNotLuna" for the name-gate counter-test.
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<galaxy>\n"
                + "    <star name=\"Sol\" temp=\"100\" x=\"0\" y=\"0\" size=\"1.0\" "
                + "          isBlackHole=\"false\" diskAngle=\"70\" "
                + "          numPlanets=\"2\" numGasGiants=\"0\">\n"
                + planetXml("Luna", DIM_LUNA, 0)
                + planetXml("AlsoNotLuna", DIM_OTHER, 0)
                + "    </star>\n"
                + "</galaxy>\n";
        Files.write(arConfigDir.resolve("planetDefs.xml"), xml.getBytes(StandardCharsets.UTF_8));

        serverHarness = RealDedicatedServerHarness.startWith(workDir, false);
        try {
            clientHarness = RealClientHarness.start(serverHarness);
        } catch (Exception ex) {
            try { serverHarness.close(); } catch (Exception cleanup) { ex.addSuppressed(cleanup); }
            serverHarness = null;
            throw ex;
        }
    }

    @After
    public void stopBoth() throws Exception {
        Exception deferred = null;
        if (clientHarness != null) {
            try { clientHarness.close(); } catch (Exception e) { deferred = e; }
            clientHarness = null;
        }
        if (serverHarness != null) {
            try { serverHarness.close(); }
            catch (Exception e) { if (deferred == null) deferred = e; else deferred.addSuppressed(e); }
            serverHarness = null;
        }
        if (deferred != null) throw deferred;
    }

    private static String planetXml(String name, int dim, int atmosDensity) {
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
                + "            <atmosphereDensity>" + atmosDensity + "</atmosphereDensity>\n"
                + "            <generateCraters>false</generateCraters>\n"
                + "            <generateCaves>true</generateCaves>\n"
                + "            <generateVolcanos>false</generateVolcanos>\n"
                + "        </planet>\n";
    }

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverHarness.client().execute(cmd));
    }

    private boolean isDone(String src) {
        Matcher m = IS_DONE.matcher(src);
        assertTrue("isDone field missing in: " + src, m.find());
        return "true".equals(m.group(1));
    }

    /** Block until the client reports the expected dim id or budget elapses. */
    private void waitForClientDim(int dim) throws Exception {
        for (int i = 0; i < 200; i++) {
            JsonObject w = clientHarness.bot().reportWeather();
            if (w != null && w.has("dim") && w.get("dim").getAsInt() == dim) return;
            clientHarness.bot().waitTicks(2);
        }
    }

    /** Baseline: a freshly-spawned player in the overworld has the
     *  WENT_TO_THE_MOON advancement NOT done — guards against state
     *  bleed-through between test classes (each gets a fresh workdir
     *  but the assertion locks the precondition explicitly). */
    @Test
    public void aBaselineWentToTheMoonNotDoneInOverworld() throws Exception {
        clientHarness.bot().waitForWorld();
        String resp = exec("artest player advancement " + ADV_WENT);
        assertEquals("baseline: WENT_TO_THE_MOON must not be granted yet; " + resp,
                false, isDone(resp));
    }

    /** Pin: standing on a Luna-named AR dim within ~22 blocks of the
     *  lander coords (2347, 80, 67) causes
     *  {@code PlanetEventHandler.fallEvent} (the LivingUpdateEvent
     *  branch wrapped at lines 203-208) to call
     *  {@code WENT_TO_THE_MOON.trigger(player)} within one
     *  {@code worldTime % 20 == 0} window. */
    @Test
    public void bStandingNearLanderOnLunaFiresWentToTheMoon() throws Exception {
        clientHarness.bot().waitForWorld();

        exec("artest tp " + DIM_LUNA);
        waitForClientDim(DIM_LUNA);
        // Move to a safe y above the magic spot but still well within
        // distanceSq < 512 (sqrt(512) ≈ 22.6). Δy=15 → distSq=225 — clear
        // of any moon terrain block at y=80 and inside the gate.
        exec("tp @a 2347 95 67");
        // The trigger gate runs only when worldTime % 20 == 0. Wait
        // 40 ticks ≥ 2 cycles to make hitting the gate effectively
        // certain, plus a small buffer for the trigger criterion to
        // propagate through AdvancementManager.
        clientHarness.bot().waitTicks(50);

        String resp = exec("artest player advancement " + ADV_WENT);
        assertEquals("standing near (2347,80,67) on Luna must grant "
                + "WENT_TO_THE_MOON within 1-2 trigger cycles; " + resp,
                true, isDone(resp));
    }

    /** Counter-test: AR dim that is NOT named "Luna" never fires the
     *  trigger regardless of player coords — pins the name-gate at
     *  line 204 ({@code getName().equals("Luna")}). */
    @Test
    public void cNonLunaArDimDoesNotFireWentToTheMoon() throws Exception {
        clientHarness.bot().waitForWorld();

        exec("artest tp " + DIM_OTHER);
        waitForClientDim(DIM_OTHER);
        // Same coords as the positive test — only the dim name differs,
        // so any failure here is a name-gate regression (the trigger
        // started firing for non-moon AR dims).
        exec("tp @a 2347 95 67");
        clientHarness.bot().waitTicks(50);

        String resp = exec("artest player advancement " + ADV_WENT);
        assertEquals("non-Luna AR dim must NOT fire WENT_TO_THE_MOON "
                + "even at the magic coords; " + resp,
                false, isDone(resp));
    }

    /** Counter-test: standing on Luna but OUTSIDE the distance gate
     *  (distanceSq ≥ 512) doesn't fire — pins the distance gate at
     *  line 205. */
    @Test
    public void dFarFromLanderCoordsOnLunaDoesNotFire() throws Exception {
        clientHarness.bot().waitForWorld();

        exec("artest tp " + DIM_LUNA);
        waitForClientDim(DIM_LUNA);
        // 100 blocks in z from (2347, 80, 67) → distSq=10000 > 512 ✗
        exec("tp @a 2347 95 167");
        clientHarness.bot().waitTicks(50);

        String resp = exec("artest player advancement " + ADV_WENT);
        assertEquals("standing far from lander coords on Luna must NOT "
                + "grant WENT_TO_THE_MOON; " + resp,
                false, isDone(resp));
    }
}
