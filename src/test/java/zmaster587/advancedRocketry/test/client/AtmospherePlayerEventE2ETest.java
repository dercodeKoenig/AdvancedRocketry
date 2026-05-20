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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10b Phase 1 — atmosphere player-event behaviour pins (AR-side).
 *
 * <h2>Scope</h2>
 *
 * <p>Pins production
 * {@link zmaster587.advancedRocketry.atmosphere.AtmosphereHandler}
 * hooks that touch the EntityPlayerMP lifecycle directly. The
 * <em>damage application</em> itself lives in libVulpes (a binary
 * dependency — {@code ItemAirWrapper.protectsFromSubstance} drains
 * the suit's O2 buffer and applies fall-back damage when empty),
 * so the tests here intentionally do NOT exercise that path. Instead
 * they pin the AR-owned bookkeeping that surrounds the libVulpes
 * call: cross-dim cache invalidation, per-dim atmosphere selection,
 * and the {@code PacketAtmSync} that pushes the dim's atmosphere
 * type to the client.</p>
 *
 * <p>Stages two AR planets via XML: a vacuum dim ({@link #DIM_VAC},
 * atmosphereDensity=0) and a breathable dim ({@link #DIM_AIR},
 * atmosphereDensity=100). Drives a real client through {@code /artest
 * tp} between them and asserts the production
 * {@link zmaster587.advancedRocketry.atmosphere.AtmosphereHandler#onPlayerChangeDim}
 * and per-dim {@code prevAtmosphere} bookkeeping behave correctly.</p>
 *
 * <h2>Pinned behaviours</h2>
 *
 * <ul>
 *   <li>{@link #aArDimWithoutVisitDoesNotCacheAtmosphereForPlayer} —
 *       baseline: cache is empty until the player ticks in an AR
 *       dim with an {@link zmaster587.advancedRocketry.atmosphere.AtmosphereHandler}
 *       registered.</li>
 *   <li>{@link #bArDimTickPopulatesPerPlayerCache} —
 *       after a player ticks in an AR dim, the per-player
 *       {@code prevAtmosphere} entry is populated with that dim's
 *       atmosphere name (the {@code != prevAtmosphere.get(entity)}
 *       branch fired and stored).</li>
 *   <li>{@link #cDimChangeClearsAtmosphereCacheForPlayer} —
 *       {@code onPlayerChangeDim} drops the cache so the new dim's
 *       atmosphere takes effect on the next onTick (not via stale
 *       cache lag).</li>
 * </ul>
 *
 * <p>Follows the manual server+client harness pattern from
 * {@link WeatherClientSyncE2ETest} (extending {@code AbstractClientE2ETest}
 * forces an empty workdir with no AR planet XML, which we need
 * controlled here).</p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AtmospherePlayerEventE2ETest {

    private static final int DIM_VAC = 9401;
    private static final int DIM_AIR = 9402;

    private static final Pattern CACHED_ATMOS =
            Pattern.compile("\"cachedAtmosphere\":\"([^\"]*)\"");
    private static final Pattern HAS_CACHED =
            Pattern.compile("\"hasCachedAtmosphere\":(true|false)");

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

        workDir = Files.createTempDirectory("forge-client-atmos-pin-");
        Path arConfigDir = workDir.resolve("config").resolve("advRocketry");
        Files.createDirectories(arConfigDir);
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<galaxy>\n"
                + "    <star name=\"Sol\" temp=\"100\" x=\"0\" y=\"0\" size=\"1.0\" "
                + "          isBlackHole=\"false\" diskAngle=\"70\" "
                + "          numPlanets=\"2\" numGasGiants=\"0\">\n"
                + planetXml("VacuumPlanet", DIM_VAC, 0)
                + planetXml("AirPlanet", DIM_AIR, 100)
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

    private String stringField(Pattern p, String src) {
        Matcher m = p.matcher(src);
        return m.find() ? m.group(1) : "";
    }

    /** Block until the client reports the expected dim id or budget elapses. */
    private void waitForClientDim(int dim) throws Exception {
        for (int i = 0; i < 200; i++) {
            JsonObject w = clientHarness.bot().reportWeather();
            if (w != null && w.has("dim") && w.get("dim").getAsInt() == dim) return;
            clientHarness.bot().waitTicks(2);
        }
    }

    /**
     * Baseline: with the player in the overworld (no AR atmosphere
     * subscription fires for overworld in default config), the
     * per-player cache must be empty. Guards against a regression
     * where AtmosphereHandler.onTick spuriously fires for vanilla
     * dims and pollutes the map.
     */
    @Test
    public void aArDimWithoutVisitDoesNotCacheAtmosphereForPlayer() throws Exception {
        clientHarness.bot().waitForWorld();
        // The bot starts in the overworld (dim 0).
        String cache = exec("artest atmosphere cached-for-player");
        String has = stringField(HAS_CACHED, cache);
        // Either no cache entry OR an entry that's empty/blank — both
        // acceptable; what we're ruling out is "vacuum atmosphere
        // somehow cached for player while still in overworld".
        String atmos = stringField(CACHED_ATMOS, cache);
        assertTrue("overworld baseline: cache must be empty or non-AR; "
                + "hasCached=" + has + " atmos=" + atmos + " " + cache,
                "false".equals(has) || atmos.isEmpty() || !atmos.contains("vacuum"));
    }

    /**
     * Pin: after the player ticks in an AR dim, the AtmosphereHandler
     * for that dim populates the per-player cache with the dim's
     * atmosphere name. Exercises the
     * {@code atmosType != prevAtmosphere.get(entity)} branch in
     * {@code AtmosphereHandler.onTick} (line 217) — i.e. proves the
     * subscription fired AND the put() happened.
     */
    @Test
    public void bArDimTickPopulatesPerPlayerCache() throws Exception {
        clientHarness.bot().waitForWorld();

        exec("artest tp " + DIM_VAC);
        waitForClientDim(DIM_VAC);
        // 40 ticks easily covers the first onTick dispatch for the
        // newly arrived player (LivingUpdateEvent fires every tick).
        clientHarness.bot().waitTicks(40);

        String cache = exec("artest atmosphere cached-for-player");
        String has = stringField(HAS_CACHED, cache);
        String atmos = stringField(CACHED_ATMOS, cache);
        assertEquals("after >=1 tick in an AR dim the per-player cache "
                + "MUST be populated (AtmosphereHandler.onTick must have "
                + "fired for the EntityPlayerMP); cache=" + cache,
                "true", has);
        assertFalse("cached atmosphere name must be non-empty: " + cache,
                atmos.isEmpty());
    }

    /**
     * Pin: changing dims clears the per-player cache via
     * {@code AtmosphereHandler.onPlayerChangeDim}; the new dim's
     * AtmosphereHandler then repopulates with its own atmosphere.
     * The two dims here have opposite atmosphereDensity (0 vacuum vs
     * 100 breathable) so the post-teleport cache name MUST differ
     * from the pre-teleport one.
     */
    @Test
    public void cDimChangeClearsAtmosphereCacheForPlayer() throws Exception {
        clientHarness.bot().waitForWorld();

        exec("artest tp " + DIM_VAC);
        waitForClientDim(DIM_VAC);
        clientHarness.bot().waitTicks(40);

        String cacheVac = exec("artest atmosphere cached-for-player");
        String atmoVac = stringField(CACHED_ATMOS, cacheVac);
        assertFalse("vacuum-dim cache must populate before the second tp: "
                + cacheVac, atmoVac.isEmpty());

        exec("artest tp " + DIM_AIR);
        waitForClientDim(DIM_AIR);
        clientHarness.bot().waitTicks(40);

        String cacheAir = exec("artest atmosphere cached-for-player");
        String atmoAir = stringField(CACHED_ATMOS, cacheAir);
        assertFalse("breathable-dim cache must repopulate after dim change: "
                + cacheAir, atmoAir.isEmpty());
        assertFalse("the vacuum-dim atmosphere name must NOT carry over "
                + "into the breathable dim's cache slot (onPlayerChangeDim "
                + "must have cleared the per-player entry); vacuumAtmos="
                + atmoVac + " breathableAtmos=" + atmoAir,
                atmoVac.equals(atmoAir));
    }
}
