package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-06 Phase 3 + 4 deferred — multi-boot persistence smoke for
 * MissionGasCollection and MissionOreMining.
 *
 * <p>A mission, once started, is registered on its target
 * {@link zmaster587.advancedRocketry.dimension.DimensionProperties} as
 * a tickable {@link zmaster587.advancedRocketry.api.satellite.SatelliteBase},
 * which is serialised through the dim's NBT save (same path as the
 * station / satellite persistence already covered by
 * {@link PersistenceRestartSmokeTest}).</p>
 *
 * <p>The contract under test:</p>
 * <ul>
 *   <li>{@code writeToNBT} captures: {@code startWorldTime, duration,
 *       worldId, launchDimension, x/y/z, rocketStats, rocketStorage,
 *       persist, infrastructure} (all in {@link
 *       zmaster587.advancedRocketry.mission.MissionResourceCollection#writeToNBT}).</li>
 *   <li>{@code MissionGasCollection} adds the {@code "gas"} key with
 *       the fluid registry name.</li>
 *   <li>{@code readFromNBT} restores the mission so its
 *       {@code getMissionId()}, {@code duration}, gas-fluid type, and
 *       type-distinguishing class are recoverable on the second boot.</li>
 * </ul>
 *
 * <p>This is the "gold-standard" reboot roundtrip — the completion
 * tests already exercise the in-memory mission state, but only a real
 * shutdown + boot proves the NBT path survives.</p>
 *
 * <p>Does NOT use {@link AbstractSharedServerTest} because it needs a
 * fresh workDir per test and an explicit two-boot lifecycle (same
 * reason {@link PersistenceRestartSmokeTest} stays on its own
 * harness).</p>
 */
public class MissionPersistenceRestartTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern MISSION_ID = Pattern.compile("\"missionId\":(-?\\d+)");
    private static final Pattern DURATION = Pattern.compile("\"duration\":(-?\\d+)");

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    @Before
    public void prepareWorkDir() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -Dforge.test.harness.enabled=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        workDir = Files.createTempDirectory("forge-server-mission-persistence-");
    }

    @After
    public void closeAll() throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
    }

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private int buildAndAssembleRocket(RealDedicatedServerHarness boot, int baseX) throws Exception {
        int baseY = 64;
        int baseZ = 600;
        ok(boot.client().execute(
                "artest fill 0 " + (baseX - 2) + " " + (baseY + 1) + " " + (baseZ - 2)
                        + " " + (baseX + 7) + " " + (baseY + 10) + " " + (baseZ + 7)
                        + " minecraft:air"));
        String fixture = ok(boot.client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ + " simple"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture missing builderPos: " + fixture, bp.find());
        int bx = Integer.parseInt(bp.group(1));
        int by = Integer.parseInt(bp.group(2));
        int bz = Integer.parseInt(bp.group(3));
        ok(boot.client().execute("artest rocket assemble 0 " + bx + " " + by + " " + bz));
        String list = ok(boot.client().execute("artest rocket list 0"));
        Matcher rim = ROCKET_LIST_ID.matcher(list);
        int lastId = -1;
        while (rim.find()) lastId = Integer.parseInt(rim.group(1));
        assertTrue("no rocket after assemble: " + list, lastId >= 0);
        return lastId;
    }

    @Test
    public void gasMissionSurvivesServerRestart() throws Exception {
        long missionId;
        long expectedDuration = 5000;
        firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);

        int rid = buildAndAssembleRocket(firstBoot, 9500);
        String start = ok(firstBoot.client().execute(
                "artest mission start-gas 0 " + rid + " " + expectedDuration + " oxygen 10"));
        assertFalse("start-gas failed in boot1: " + start, start.contains("\"error\""));
        Matcher mm = MISSION_ID.matcher(start);
        assertTrue("missing missionId in start response: " + start, mm.find());
        missionId = Long.parseLong(mm.group(1));

        firstBoot.close();
        firstBoot = null;

        secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        String state = ok(secondBoot.client().execute("artest mission state " + missionId));
        assertFalse("state probe failed after reboot — mission lost: " + state,
                state.contains("\"error\""));
        assertTrue("mission type must be gas after reboot: " + state,
                state.contains("\"type\":\"gas\""));
        Matcher dm = DURATION.matcher(state);
        assertTrue("missing duration in restored state: " + state, dm.find());
        // MissionGasCollection ctor multiplies duration by gasCollectionMult
        // (config default 1.0 in test env). Pin against the value the mission
        // actually stored — pull it via state probe from boot 1 was already
        // computed; here we just assert it's nonzero and stable across reboot.
        long restoredDuration = Long.parseLong(dm.group(1));
        assertTrue("restored duration must be > 0: " + state, restoredDuration > 0);
        assertEquals("restored duration must equal configured (gasCollectionMult=1 in test env)",
                expectedDuration, restoredDuration);
        assertTrue("mission must not be dead after reboot: " + state,
                state.contains("\"isDead\":false"));
    }

    @Test
    public void oreMissionSurvivesServerRestart() throws Exception {
        long missionId;
        long expectedDuration = 5000;
        firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);

        int rid = buildAndAssembleRocket(firstBoot, 9600);
        String start = ok(firstBoot.client().execute(
                "artest mission start-ore 0 " + rid + " " + expectedDuration + " 1.0"));
        assertFalse("start-ore failed in boot1: " + start, start.contains("\"error\""));
        Matcher mm = MISSION_ID.matcher(start);
        assertTrue("missing missionId in start response: " + start, mm.find());
        missionId = Long.parseLong(mm.group(1));

        firstBoot.close();
        firstBoot = null;

        secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        String state = ok(secondBoot.client().execute("artest mission state " + missionId));
        assertFalse("state probe failed after reboot — mission lost: " + state,
                state.contains("\"error\""));
        assertTrue("mission type must be ore after reboot: " + state,
                state.contains("\"type\":\"ore\""));
        Matcher dm = DURATION.matcher(state);
        assertTrue("missing duration in restored state: " + state, dm.find());
        assertEquals("restored ore duration must equal configured",
                expectedDuration, Long.parseLong(dm.group(1)));
        assertTrue("mission must not be dead after reboot: " + state,
                state.contains("\"isDead\":false"));
    }
}
