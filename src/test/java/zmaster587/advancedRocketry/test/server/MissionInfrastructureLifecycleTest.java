package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-06 Phase 5 — mission ↔ infrastructure lifecycle contract.
 *
 * <p>Pins the player-visible cause-effect of starting and completing
 * a mission that has linked-infrastructure tiles (e.g. a Rocket
 * Monitoring Station the player connected to the rocket pre-launch):</p>
 * <ul>
 *   <li>At start time the tile's {@code mission} field is set to the
 *       mission instance (so its GUI / comparator output / progress
 *       readouts reflect the live mission). Pinned via the production
 *       {@link zmaster587.advancedRocketry.api.IInfrastructure#linkMission}
 *       contract — the probe mirrors what
 *       {@code EntityRocket.createMission} does after the mission ctor.</li>
 *   <li>At completion the mission iterates {@code infrastructureCoords},
 *       calls {@code unlinkMission()} on each live tile (the tile's
 *       {@code mission} field becomes null), and re-links each tile to
 *       the freshly respawned rocket via {@code rocket.linkInfrastructure}.
 *       Post-condition: the rocket's {@code infrastructureCoords}
 *       collection contains the tile's coord. This is the "your
 *       monitoring station now follows the returned rocket" UX.</li>
 * </ul>
 *
 * <p>Uses {@code monitoringStation} as the fixture infra-tile
 * (registry: {@code advancedrocketry:monitoringStation}) — it's the
 * simplest IInfrastructure implementor that actually stores a
 * non-null mission ref on {@code linkMission} and clears it on
 * {@code unlinkMission}. Counter-example: {@code TileGuidanceComputerAccessHatch}
 * always returns false from linkMission (it's a chip-eject passthrough),
 * so picking the right tile here matters.</p>
 *
 * <p>Position-isolated per {@link AbstractSharedServerTest} contract:
 * each test method uses a unique {@code BASE_X} far from
 * {@code MissionGasCompletionTest} (which uses 8000+).</p>
 */
public class MissionInfrastructureLifecycleTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern MISSION_ID = Pattern.compile("\"missionId\":(-?\\d+)");

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private int buildAndAssembleRocket(int baseX) throws Exception {
        int baseY = 64;
        int baseZ = 600;
        ok(client().execute(
                "artest fill 0 " + (baseX - 2) + " " + (baseY + 1) + " " + (baseZ - 2)
                        + " " + (baseX + 7) + " " + (baseY + 10) + " " + (baseZ + 7)
                        + " minecraft:air"));
        String fixture = ok(client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ + " simple"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture missing builderPos: " + fixture, bp.find());
        int bx = Integer.parseInt(bp.group(1));
        int by = Integer.parseInt(bp.group(2));
        int bz = Integer.parseInt(bp.group(3));
        ok(client().execute("artest rocket assemble 0 " + bx + " " + by + " " + bz));
        String list = ok(client().execute("artest rocket list 0"));
        Matcher rim = ROCKET_LIST_ID.matcher(list);
        int lastId = -1;
        while (rim.find()) lastId = Integer.parseInt(rim.group(1));
        assertTrue("no rocket after assemble: " + list, lastId >= 0);
        return lastId;
    }

    private long startGasMission(int rocketId, long duration) throws Exception {
        String start = ok(client().execute(
                "artest mission start-gas 0 " + rocketId + " " + duration + " oxygen 10"));
        assertFalse("start-gas must not error: " + start, start.contains("\"error\""));
        Matcher mm = MISSION_ID.matcher(start);
        assertTrue("missing missionId: " + start, mm.find());
        return Long.parseLong(mm.group(1));
    }

    /** Places a monitoringStation block in the SAME chunk as the rocket
     *  so the chunk stays reliably loaded between probe commands.
     *  Production's {@code onMissionComplete} calls
     *  {@code world.getTileEntity(coord)} which returns null if the chunk
     *  has been unloaded — placing far away (different chunk) makes the
     *  re-link race unloads. Position is inside the fixture's
     *  air-cleared bbox at the column above the launchpad (baseX, baseY+2,
     *  baseZ) — chunk (baseX>>4, baseZ>>4) is the rocket's chunk. */
    private int[] placeMonitoringStation(int baseX, int baseZ) throws Exception {
        int ix = baseX;
        int iy = 66;
        int iz = baseZ;
        ok(client().execute("artest place 0 " + ix + " " + iy + " " + iz
                + " advancedrocketry:monitoringStation"));
        return new int[]{ix, iy, iz};
    }

    /** After link-infra the tile's {@code mission} field points back to
     *  the just-started mission. Pins the link half of the lifecycle. */
    @Test
    public void startLinksInfrastructureToMission() throws Exception {
        int baseX = 9000;
        int rid = buildAndAssembleRocket(baseX);
        long mid = startGasMission(rid, 1000);
        int[] ipos = placeMonitoringStation(baseX, 600);
        String link = ok(client().execute("artest mission link-infra " + mid
                + " 0 " + ipos[0] + " " + ipos[1] + " " + ipos[2]));
        assertFalse("link-infra must not error: " + link, link.contains("\"error\""));
        assertTrue("link-infra must report linked=true: " + link,
                link.contains("\"linked\":true"));

        String state = ok(client().execute("artest mission infra-state 0 "
                + ipos[0] + " " + ipos[1] + " " + ipos[2]));
        assertFalse("infra-state must not error: " + state, state.contains("\"error\""));
        assertTrue("infra must report hasMission=true after link: " + state,
                state.contains("\"hasMission\":true"));
        assertTrue("infra must report this mission's id: " + state,
                state.contains("\"missionId\":" + mid));
    }

    /** After complete-now the production loop in MissionGasCollection
     *  iterates infrastructureCoords and calls {@code unlinkMission()} on
     *  the live tile (MissionGasCollection.java:80-86). Post-condition:
     *  tile.mission becomes null — the player-visible effect is that the
     *  monitoring station GUI stops showing the mission progress.
     *
     *  <p>The rocket-side half of the lifecycle (production also calls
     *  {@code rocket.linkInfrastructure} on the freshly spawned
     *  EntityStationDeployedRocket) is pinned by
     *  {@link #completionLinksInfrastructureToRespawnedRocket} via the
     *  {@code rocket-relink-state} probe. */
    @Test
    public void completionUnlinksInfrastructureFromMission() throws Exception {
        int baseX = 9100;
        int rid = buildAndAssembleRocket(baseX);
        long mid = startGasMission(rid, 1000);
        int[] ipos = placeMonitoringStation(baseX, 600);
        String link = ok(client().execute("artest mission link-infra " + mid
                + " 0 " + ipos[0] + " " + ipos[1] + " " + ipos[2]));
        assertTrue("setup link-infra must succeed: " + link, link.contains("\"linked\":true"));

        // Sanity: pre-completion tile reports the mission.
        String preState = ok(client().execute("artest mission infra-state 0 "
                + ipos[0] + " " + ipos[1] + " " + ipos[2]));
        assertTrue("pre-completion infra must report hasMission=true: " + preState,
                preState.contains("\"hasMission\":true"));

        String cargo = ok(client().execute("artest mission complete-now " + mid));
        assertFalse("complete-now must not error: " + cargo, cargo.contains("\"error\""));
        assertTrue("completion must fire: " + cargo, cargo.contains("\"completed\":true"));

        // Post-completion tile.mission cleared by production's unlinkMission().
        String postState = ok(client().execute("artest mission infra-state 0 "
                + ipos[0] + " " + ipos[1] + " " + ipos[2]));
        assertFalse("infra-state must not error: " + postState,
                postState.contains("\"error\""));
        assertTrue("infra must report hasMission=false after completion: " + postState,
                postState.contains("\"hasMission\":false"));
    }

    /** Rocket-side half of the lifecycle (MissionGasCollection.java:80-86):
     *  for each entry in {@code infrastructureCoords} the gas-completion
     *  loop calls {@code rocket.linkInfrastructure(tile)} on the freshly
     *  spawned {@code EntityStationDeployedRocket}. Pins the post-condition
     *  that the new rocket's {@code infrastructureCoords} set contains
     *  the linked infra tile's coord, i.e. the "your monitoring station
     *  now follows the returned rocket" UX.
     *
     *  <p>This test cannot reuse the bbox-restricted {@code rocket-cargo}
     *  probe — with a vanilla EntityRocket fixture the
     *  {@code writeMissionPersistentNBT} call inside the
     *  MissionResourceCollection ctor is a no-op, so the new rocket's
     *  {@code launchLocation} restored from empty NBT defaults to
     *  (0,0,0). The rocket therefore spawns at world origin, outside
     *  the {@code rocket-cargo} bbox around the original launch coords.
     *  The {@code rocket-relink-state} probe is class-filtered (scans
     *  the whole launch dim for EntityStationDeployedRocket instances)
     *  and finds the rocket regardless of position. */
    @Test
    public void completionLinksInfrastructureToRespawnedRocket() throws Exception {
        int baseX = 9200;
        int rid = buildAndAssembleRocket(baseX);
        long mid = startGasMission(rid, 1000);
        int[] ipos = placeMonitoringStation(baseX, 600);
        String link = ok(client().execute("artest mission link-infra " + mid
                + " 0 " + ipos[0] + " " + ipos[1] + " " + ipos[2]));
        assertTrue("setup link-infra must succeed: " + link, link.contains("\"linked\":true"));

        String cargo = ok(client().execute("artest mission complete-now " + mid));
        assertTrue("completion must fire: " + cargo, cargo.contains("\"completed\":true"));

        String relink = ok(client().execute("artest mission rocket-relink-state 0"));
        assertFalse("rocket-relink-state must not error: " + relink,
                relink.contains("\"error\""));
        // At least one EntityStationDeployedRocket exists in launch dim
        // post-completion — production's onMissionComplete spawned it.
        assertFalse("deployedCount must be > 0 after gas completion: " + relink,
                relink.contains("\"deployedCount\":0"));
        // Production looped infrastructureCoords and called
        // rocket.linkInfrastructure for each entry. The placed monitoring
        // station coord must appear in some StationDeployedRocket's
        // infrastructureCoords list. Test for the exact triple as JSON
        // array to avoid matching a coincidental coord-with-shared-axis.
        String expected = "[" + ipos[0] + "," + ipos[1] + "," + ipos[2] + "]";
        assertTrue("rocket infrastructureCoords must contain "
                        + expected + ": " + relink,
                relink.contains(expected));
    }
}
