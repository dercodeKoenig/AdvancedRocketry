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
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 8 — multi-boot harness for station landing-pad
 * persistence.
 *
 * Companion to {@link PersistenceRestartSmokeTest} (which covers station
 * id + orbiting body + satellite + atmosphere density across restart).
 * This test focuses on the LANDING-PAD state: a station's pad set, each
 * pad's occupied flag, each pad's auto-land allow-list — all of which
 * are NBT-serialised in {@code SpaceStationObject.writeToNBT}'s
 * spawnLocations branch.
 *
 * Sequence:
 *
 * <ol>
 *   <li>Boot 1: create station, add 3 pads (A, B, C), enable auto-land on
 *       B only, dock once (must claim B and mark it occupied).</li>
 *   <li>Boot 2 (same workDir): verify all 3 pads survived, B is
 *       still occupied + auto-land=true, A and C are still free +
 *       auto-land=false. Then undock B and dock again — must reclaim B.</li>
 * </ol>
 *
 * Why this matters: without per-pad occupied flags surviving save/load,
 * a server restart would lose the dock state of every in-orbit rocket
 * — modpack players would log back in to find their docked rockets
 * had vanished from their station's tracking even though the rocket
 * entity itself persists in the world.
 */
public class SpaceStationPadPersistenceTest {

    private static final Pattern STATION_ID =
            Pattern.compile("\"id\":(-?\\d+),\"orbitingBody\":");

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    @Before
    public void prepareWorkDir() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -Dforge.test.harness.enabled=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        workDir = Files.createTempDirectory("forge-server-pad-persistence-");
    }

    @After
    public void closeAll() throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
    }

    @Test
    public void padSetAndPerPadStateSurviveRestart() throws Exception {
        long stationId;

        firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);

        // --- Boot 1: create station with three pads, lock auto-land + dock B
        String createStation = String.join("\n",
                firstBoot.client().execute("artest station create 0"));
        Matcher sm = STATION_ID.matcher(createStation);
        assertTrue("could not extract station id: " + createStation, sm.find());
        stationId = Long.parseLong(sm.group(1));

        ok(firstBoot, "artest station add-pad " + stationId + " 100 100 padA");
        ok(firstBoot, "artest station add-pad " + stationId + " 200 200 padB");
        ok(firstBoot, "artest station add-pad " + stationId + " 300 300 padC");
        ok(firstBoot, "artest station set-autoland " + stationId + " 200 200 true");

        // Dock must consume B (the only auto-land pad).
        String dock = String.join("\n",
                firstBoot.client().execute("artest station dock " + stationId));
        assertTrue("boot1 dock must claim padB: " + dock,
                dock.contains("\"ok\":true") && dock.contains("\"x\":200"));

        // Sanity dump before restart.
        String padsBefore = String.join("\n",
                firstBoot.client().execute("artest station pads " + stationId));
        assertTrue("padA must be in boot1 dump: " + padsBefore,
                padsBefore.contains("\"x\":100"));
        assertTrue("padB must be in boot1 dump: " + padsBefore,
                padsBefore.contains("\"x\":200"));
        assertTrue("padC must be in boot1 dump: " + padsBefore,
                padsBefore.contains("\"x\":300"));

        // /save-all to force the world to flush before close — same as the
        // existing PersistenceRestartSmokeTest pattern.
        firstBoot.client().execute("save-all flush");
        firstBoot.close();
        firstBoot = null;

        // --- Boot 2 on the same workDir — every pad-level state must restore.
        secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        String stations = String.join("\n",
                secondBoot.client().execute("artest station list"));
        assertTrue("station " + stationId + " did NOT survive restart: " + stations,
                stations.contains("\"id\":" + stationId));

        String padsAfter = String.join("\n",
                secondBoot.client().execute("artest station pads " + stationId));
        assertTrue("padA must survive restart: " + padsAfter,
                padsAfter.contains("\"x\":100"));
        assertTrue("padB must survive restart: " + padsAfter,
                padsAfter.contains("\"x\":200"));
        assertTrue("padC must survive restart: " + padsAfter,
                padsAfter.contains("\"x\":300"));

        // Per-pad state assertions are extracted via substring isolation
        // (pads is a flat array of LinkedList-ordered objects).
        String padAObj = extractObjectContaining(padsAfter, "\"x\":100");
        String padBObj = extractObjectContaining(padsAfter, "\"x\":200");
        String padCObj = extractObjectContaining(padsAfter, "\"x\":300");

        // occupied: padB is the only one that should be true (we docked it
        // pre-restart). A and C stay free.
        assertTrue("padB's occupied=true must survive restart: " + padBObj,
                padBObj.contains("\"occupied\":true"));
        assertTrue("padA must restore to occupied=false: " + padAObj,
                padAObj.contains("\"occupied\":false"));
        assertTrue("padC must restore to occupied=false: " + padCObj,
                padCObj.contains("\"occupied\":false"));

        // pad name field — writeToNBT.setString("name", …) + readFromNbt
        // reads it back via tag.getString("name"). All three names must
        // survive verbatim.
        assertTrue("padA name must survive restart (\"padA\"): " + padAObj,
                padAObj.contains("\"name\":\"padA\""));
        assertTrue("padB name must survive restart (\"padB\"): " + padBObj,
                padBObj.contains("\"name\":\"padB\""));
        assertTrue("padC name must survive restart (\"padC\"): " + padCObj,
                padCObj.contains("\"name\":\"padC\""));

        // -- allowAutoLand: surface the known bug at
        //    SpaceStationObject.java:801. The write side correctly writes
        //    `tag.setBoolean("autoLand", pos.getAllowedForAutoLand())`,
        //    but the read side reads from the WRONG KEY:
        //      loc.setAllowedForAutoLand(!tag.hasKey("occupied")
        //                                  || tag.getBoolean("occupied"));
        //    This collapses allowAutoLand to "is the pad occupied?" plus
        //    a weird hasKey defaults-to-true fallback. The result:
        //    - padB (occupied=true) → allowAutoLand reads as true (lucky)
        //    - padA / padC (occupied=false) → allowAutoLand reads as
        //                                     false ALWAYS, regardless of
        //                                     what was written.
        // Our boot1 set padB autoLand=true and padA/C never opted in
        // (default false), so the OBSERVED outcomes happen to all match
        // what we want — but ONLY because of the collision between the
        // semantic of occupied-on-padB and the read-key bug. If the
        // boot1 sequence opted padA into autoLand WITHOUT docking it,
        // the bug would surface. Pin both observations explicitly so a
        // future read-side fix is forced to update this test.
        assertTrue("padB allowAutoLand reads true after restart (lucky path "
                        + "— SpaceStationObject:801 reads from \"occupied\" "
                        + "key, and padB IS occupied): " + padBObj,
                padBObj.contains("\"allowAutoLand\":true"));
        assertTrue("padA allowAutoLand reads FALSE after restart (whatever "
                        + "the original write was — read side ignores the "
                        + "\"autoLand\" key, SpaceStationObject:801 bug): "
                        + padAObj,
                padAObj.contains("\"allowAutoLand\":false"));

        // Behavioural check: undock B → next dock must reclaim B again.
        String undock = String.join("\n", secondBoot.client().execute(
                "artest station undock " + stationId + " 200 200"));
        assertTrue("post-restart undock must succeed: " + undock,
                undock.contains("\"ok\":true"));
        String dock2 = String.join("\n", secondBoot.client().execute(
                "artest station dock " + stationId));
        assertTrue("post-restart dock must reclaim padB: " + dock2,
                dock2.contains("\"ok\":true") && dock2.contains("\"x\":200"));
    }

    /**
     * <em>DOCUMENTS KNOWN PRODUCTION BUG</em> at
     * {@code SpaceStationObject.java:801}:
     *
     * <pre>
     * tag.setBoolean("autoLand", pos.getAllowedForAutoLand());  // write
     * ...
     * loc.setAllowedForAutoLand(
     *     !tag.hasKey("occupied") || tag.getBoolean("occupied"));  // read
     * </pre>
     *
     * Fixed in TASK-12 (bug #4): the read now uses the "autoLand" key
     * that the write side writes. allowAutoLand survives restart even
     * for pads that weren't docked at save time.
     */
    @Test
    public void autoLandFlagWithoutDockSurvivesRestart() throws Exception {
        firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);
        String createStation = String.join("\n",
                firstBoot.client().execute("artest station create 0"));
        Matcher sm = STATION_ID.matcher(createStation);
        assertTrue("could not extract station id: " + createStation, sm.find());
        long stationId = Long.parseLong(sm.group(1));

        // Add ONE pad and enable auto-land — but DO NOT dock it. occupied
        // stays false; the read-side bug forces allowAutoLand to false too.
        ok(firstBoot, "artest station add-pad " + stationId + " 999 999 lonely");
        ok(firstBoot, "artest station set-autoland " + stationId + " 999 999 true");

        // Sanity in boot1: the in-memory state correctly reports both flags.
        String padsBefore = String.join("\n",
                firstBoot.client().execute("artest station pads " + stationId));
        assertTrue("boot1 padA must report allowAutoLand=true in memory: " + padsBefore,
                padsBefore.contains("\"allowAutoLand\":true"));
        assertTrue("boot1 padA must report occupied=false: " + padsBefore,
                padsBefore.contains("\"occupied\":false"));

        firstBoot.client().execute("save-all flush");
        firstBoot.close();
        firstBoot = null;

        secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);
        String padsAfter = String.join("\n",
                secondBoot.client().execute("artest station pads " + stationId));
        assertTrue("padA allowAutoLand must be true after restart — "
                        + "SpaceStationObject:801 now reads from the same "
                        + "\"autoLand\" key the write side writes. pads dump: "
                        + padsAfter,
                padsAfter.contains("\"allowAutoLand\":true"));
    }

    /**
     * Extract the JSON object that contains the given marker from a flat
     * JSON array of objects. Used to assert per-pad fields when the array
     * has multiple peer objects with different `x` values.
     */
    private static String extractObjectContaining(String json, String marker) {
        int markerIdx = json.indexOf(marker);
        assertTrue("marker not found: " + marker + " in " + json, markerIdx >= 0);
        // Walk back to the opening `{`.
        int start = markerIdx;
        int depth = 0;
        while (start >= 0) {
            char c = json.charAt(start);
            if (c == '}') depth++;
            else if (c == '{') {
                if (depth == 0) break;
                depth--;
            }
            start--;
        }
        // Walk forward to the matching closing `}`.
        int end = markerIdx;
        depth = 0;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) { end++; break; }
            }
            end++;
        }
        return json.substring(start, Math.min(end, json.length()));
    }

    private static void ok(RealDedicatedServerHarness harness, String cmd) throws Exception {
        String resp = String.join("\n", harness.client().execute(cmd));
        assertEquals("probe " + cmd + " did not return ok: " + resp,
                true, resp.contains("\"ok\":true"));
    }
}
