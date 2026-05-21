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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10b Phase 2 — space-dim "outside-any-station" teleport guard.
 *
 * <p>Production:
 * {@link zmaster587.advancedRocketry.event.PlanetEventHandler#playerTick}
 * lines 210-232. Every server tick, if the player is in
 * {@code ARConfiguration.spaceDimId}, NOT inside any registered
 * station's bounds, and NOT riding a rocket, the handler:</p>
 *
 * <ul>
 *   <li>If at least one {@link zmaster587.advancedRocketry.api.stations.ISpaceObject}
 *       station is registered, teleports the player to the
 *       <strong>furthest</strong> station's
 *       {@code getSpawnLocation()}.</li>
 *   <li>Otherwise transfers the player to dim 0 via
 *       {@code PlayerList.transferPlayerToDimension} with a
 *       {@code TeleporterNoPortal}.</li>
 * </ul>
 *
 * <p>This pin asserts both branches behave correctly. Reproduces the
 * inline server+client harness lifecycle from
 * {@link AtmospherePlayerEventE2ETest} / {@link WeatherClientSyncE2ETest}
 * because the no-station branch needs a workdir without persisted
 * station NBT.</p>
 */
public class SpaceDimGuardE2ETest {

    private static final Pattern DIM = Pattern.compile("\"dim\":(-?\\d+)");
    private static final Pattern POS_X = Pattern.compile("\"posX\":(-?[0-9.eE+-]+)");
    private static final Pattern POS_Y = Pattern.compile("\"posY\":(-?[0-9.eE+-]+)");
    private static final Pattern POS_Z = Pattern.compile("\"posZ\":(-?[0-9.eE+-]+)");
    private static final Pattern SPAWN_X = Pattern.compile("\"spawnX\":(-?\\d+)");
    private static final Pattern SPAWN_Y = Pattern.compile("\"spawnY\":(-?\\d+)");
    private static final Pattern SPAWN_Z = Pattern.compile("\"spawnZ\":(-?\\d+)");
    private static final Pattern STATION_ID = Pattern.compile("\"id\":(-?\\d+)");

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

        workDir = Files.createTempDirectory("forge-client-space-guard-");
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

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverHarness.client().execute(cmd));
    }

    private int intField(Pattern p, String src, String name) {
        Matcher m = p.matcher(src);
        assertTrue("field " + name + " missing in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }

    private double doubleField(Pattern p, String src, String name) {
        Matcher m = p.matcher(src);
        assertTrue("field " + name + " missing in: " + src, m.find());
        return Double.parseDouble(m.group(1));
    }

    /**
     * Pin: with NO registered station, a player who lands in the space
     * dim gets kicked back to the overworld on the next server tick.
     */
    @Test
    public void noStationFallbackTeleportsPlayerToOverworld() throws Exception {
        clientHarness.bot().waitForWorld();

        // Sanity: must start in overworld.
        String pre = exec("artest player health");
        assertEquals("baseline must be overworld dim 0; " + pre,
                0, intField(DIM, pre, "dim"));

        // Sanity: no stations exist (a fresh workdir has none).
        String list = exec("artest station list");
        assertTrue("no stations expected at test start: " + list,
                list.contains("\"stations\":[]"));

        // Teleport to spaceDimId (-2 default). Without any station, the
        // playerTick guard MUST kick the player back to dim 0 on the
        // very next server tick.
        exec("artest tp -2");
        // Wait a few ticks for one full server tick cycle so the
        // playerTick guard fires reliably (LivingUpdateEvent runs every
        // tick).
        clientHarness.bot().waitTicks(40);

        String after = exec("artest player health");
        int dim = intField(DIM, after, "dim");
        assertEquals("no-station fallback must transfer player back to "
                + "overworld; player is in dim " + dim + " — " + after,
                0, dim);
    }

    /**
     * Pin: with a registered station, a player who lands in the space
     * dim outside the station's bounds gets teleported to the station's
     * spawn location.
     */
    @Test
    public void registeredStationTeleportTargetsStationSpawn() throws Exception {
        clientHarness.bot().waitForWorld();

        // Create a station orbiting the overworld (dim 0).
        String createResp = exec("artest station create 0");
        assertFalse("station create must succeed: " + createResp,
                createResp.contains("\"error\""));
        int stationId = intField(STATION_ID, createResp, "station id");

        // Read the station's spawn position to derive a "definitely
        // outside the station" probe target (well past 1024 blocks from
        // spawn — vanilla 1.12 space-station bounds are much smaller).
        String info = exec("artest station info " + stationId);
        int spawnX = intField(SPAWN_X, info, "spawnX");
        int spawnY = intField(SPAWN_Y, info, "spawnY");
        int spawnZ = intField(SPAWN_Z, info, "spawnZ");

        // Teleport player into the space dim. The default spawn lands
        // in station-id-1's slot (the spiral indexing puts the first
        // station near origin), which would make the playerTick guard
        // skip teleporting (player is "in" the station's slot). So
        // immediately /tp the player far away (50_000 blocks) — that
        // resolves to a station slot index our station doesn't occupy,
        // so SpaceObjectManager.getSpaceStationFromBlockCoords returns
        // null and the guard fires.
        exec("artest tp -2");
        clientHarness.bot().waitTicks(20);
        // Vanilla /tp works inside the same dim.
        exec("tp @a 50000 100 50000");
        // Only 5 ticks: the guard fires every tick, so a single tick is
        // already enough — extra ticks just let gravity drag the player
        // away from spawnY (there's no platform at the freshly-created
        // station's spawn), inflating the posY epsilon for no gain.
        clientHarness.bot().waitTicks(5);

        String after = exec("artest player health");
        int dim = intField(DIM, after, "dim");
        // The player MUST still be in the space dim (the guard
        // teleported them to a station INSIDE space dim, not back to
        // overworld) AND their position must match the station spawn.
        assertEquals("player must remain in space dim — they should be "
                + "teleported to the station's spawn, not back to "
                + "overworld; dim=" + dim + " " + after,
                -2, dim);

        double posX = doubleField(POS_X, after, "posX");
        double posY = doubleField(POS_Y, after, "posY");
        double posZ = doubleField(POS_Z, after, "posZ");
        // The handler uses setPositionAndUpdate(spawn.x, spawn.y, spawn.z)
        // exactly. X/Z motion in vacuum is zero (no input), so a tight
        // 2.0 epsilon holds. Y drifts down: spawn has no platform, so
        // gravity pulls the player ~1 block/tick after a few ticks of
        // accumulation. A 6.0 epsilon covers the 5-tick free-fall window
        // while still pinning "teleported to spawn area, not overworld".
        assertEquals("player posX must match station spawnX after "
                + "guard fires; spawn=(" + spawnX + "," + spawnY + "," + spawnZ
                + ") player=(" + posX + "," + posY + "," + posZ + ")",
                spawnX, posX, 2.0);
        assertEquals("player posY must match station spawnY (within free-fall window)",
                spawnY, posY, 6.0);
        assertEquals("player posZ must match station spawnZ",
                spawnZ, posZ, 2.0);
        // Pin the "not overworld" invariant explicitly for readability.
        assertNotEquals("player must NOT be in overworld (station exists "
                + "→ teleport-to-station branch, not fallback): " + after,
                0, dim);
    }
}
