package zmaster587.advancedRocketry.test.server;

// migrated to AbstractSharedServerTest (TASK-03 B2)
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 8 — dock / undock contract on
 * {@code SpaceStationObject}.
 *
 * The smoke / depth tests in {@link SpaceStationLifecycleSmokeTest} and
 * {@link SpaceStationDepthTest} cover id allocation, fuel accounting,
 * and registry persistence. They do NOT exercise the LANDING-PAD
 * lifecycle: addLandingPad → setLandingPadAutoLandStatus →
 * getNextLandingPad → setPadStatus. That state is what every rocket
 * landing on a station and every rocket lifting off a pad mutates;
 * a subtle regression in this state machine would silently break
 * inter-dim travel for modpack players.
 *
 * What's pinned here:
 *
 * <ul>
 *   <li>{@code add-pad} grows the landing-pad list by 1 and the new
 *       pad starts <em>occupied=false, allowAutoLand=false</em>
 *       (default-state contract — easy to flip in a refactor).</li>
 *   <li>{@code dock} (== getNextLandingPad(true)) returns <em>no pad
 *       available</em> until a pad has been explicitly opted in to
 *       auto-landing. This is a non-obvious gate — a refactor that
 *       defaults pads to auto-land would silently land rockets on pads
 *       the station owner hadn't authorized.</li>
 *   <li>After enabling auto-land, dock returns the pad and marks it
 *       occupied; a second dock for the same pad fails with "no free pad".</li>
 *   <li>{@code undock(<x>,<z>)} frees the pad so the next dock returns
 *       it again.</li>
 *   <li>{@code remove-pad} shrinks the list and the removed pad's pos
 *       is no longer reported by {@code pads}.</li>
 *   <li>Two pads at the same (x,z) — addLandingPad must de-dupe (the
 *       prod code uses {@code !spawnLocations.contains(pos)} via
 *       StationLandingLocation.equals → BlockPos equality).</li>
 *   <li>{@code dock(commit=false)} reads next-free without consuming.</li>
 * </ul>
 */
public class SpaceStationDockUndockTest extends AbstractSharedServerTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(-?\\d+)");

    private int createStation() throws Exception {
        String resp = String.join("\n", client().execute("artest station create 0"));
        assertTrue("station create failed: " + resp, resp.contains("\"ok\":true"));
        Matcher m = ID_PATTERN.matcher(resp);
        assertTrue("could not parse station id: " + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    @Test
    public void addPadGrowsListWithExpectedDefaults() throws Exception {
        int id = createStation();
        String add = ok(client().execute("artest station add-pad " + id + " 10 20 alpha"));
        assertTrue("add-pad must succeed: " + add, add.contains("\"ok\":true"));
        assertTrue("padCount should be 1 after first add: " + add,
                add.contains("\"padCount\":1"));

        String pads = ok(client().execute("artest station pads " + id));
        assertTrue("pads probe must list the new pad: " + pads,
                pads.contains("\"x\":10") && pads.contains("\"z\":20"));
        // Default state contract — pad starts unoccupied AND not opted into
        // auto-landing. A refactor that flips either default would silently
        // change the dock-allocation semantics.
        assertTrue("new pad must start occupied=false: " + pads,
                pads.contains("\"occupied\":false"));
        assertTrue("new pad must start allowAutoLand=false: " + pads,
                pads.contains("\"allowAutoLand\":false"));
        assertTrue("new pad must carry the supplied name: " + pads,
                pads.contains("\"name\":\"alpha\""));
    }

    @Test
    public void dockRejectsPadWithoutAutoLandOptIn() throws Exception {
        // Critical: getNextLandingPad gates on BOTH not-occupied AND
        // allowedForAutoLanding. A pad just added via addLandingPad starts
        // with allowAutoLand=false — dock must NOT silently consume it.
        int id = createStation();
        ok(client().execute("artest station add-pad " + id + " 10 20 alpha"));
        String dock = ok(client().execute("artest station dock " + id));
        assertTrue("dock must refuse a pad that hasn't opted into auto-land: " + dock,
                dock.contains("\"ok\":false")
                        && dock.contains("\"reason\":\"no free landing pad\""));
    }

    @Test
    public void dockClaimsAutoLandPadAndMarksOccupied() throws Exception {
        int id = createStation();
        ok(client().execute("artest station add-pad " + id + " 30 40 beta"));
        ok(client().execute("artest station set-autoland " + id + " 30 40 true"));

        String dock = ok(client().execute("artest station dock " + id));
        assertTrue("dock must succeed once pad is auto-land enabled: " + dock,
                dock.contains("\"ok\":true"));
        assertTrue("dock response must echo the chosen pad coords: " + dock,
                dock.contains("\"x\":30") && dock.contains("\"z\":40"));

        // After dock with commit=true, the pad's occupied flag must flip.
        String pads = ok(client().execute("artest station pads " + id));
        assertTrue("dock must mark the pad occupied=true: " + pads,
                pads.contains("\"occupied\":true"));

        // A second dock against the only pad MUST return no-free-pad.
        String dock2 = ok(client().execute("artest station dock " + id));
        assertTrue("second dock with no other free pad must fail: " + dock2,
                dock2.contains("\"ok\":false"));
    }

    @Test
    public void undockReturnsPadToFreePool() throws Exception {
        int id = createStation();
        ok(client().execute("artest station add-pad " + id + " 50 60 gamma"));
        ok(client().execute("artest station set-autoland " + id + " 50 60 true"));
        ok(client().execute("artest station dock " + id));  // consume

        // Pre-undock: the pad reports occupied=true.
        String pre = ok(client().execute("artest station pads " + id));
        assertTrue("pre-undock pad must read occupied=true: " + pre,
                pre.contains("\"occupied\":true"));

        String undock = ok(client().execute("artest station undock " + id + " 50 60"));
        assertTrue("undock must succeed: " + undock, undock.contains("\"ok\":true"));

        // Post-undock: pad is free again.
        String post = ok(client().execute("artest station pads " + id));
        assertTrue("post-undock pad must read occupied=false: " + post,
                post.contains("\"occupied\":false"));

        // And the next dock call must successfully reclaim it.
        String reclaim = ok(client().execute("artest station dock " + id));
        assertTrue("post-undock dock must reclaim the just-freed pad: " + reclaim,
                reclaim.contains("\"ok\":true") && reclaim.contains("\"x\":50"));
    }

    @Test
    public void dockWithCommitFalseDoesNotConsumePad() throws Exception {
        // The probe forwards commit=false to getNextLandingPad — production
        // path used for "preview which pad WOULD I land on?" checks. The
        // pad must NOT flip to occupied.
        int id = createStation();
        ok(client().execute("artest station add-pad " + id + " 70 80 delta"));
        ok(client().execute("artest station set-autoland " + id + " 70 80 true"));

        String preview = ok(client().execute("artest station dock " + id + " false"));
        assertTrue("preview dock must report ok and the pad coords: " + preview,
                preview.contains("\"ok\":true") && preview.contains("\"x\":70"));

        String pads = ok(client().execute("artest station pads " + id));
        assertTrue("preview dock must NOT mark the pad occupied: " + pads,
                pads.contains("\"occupied\":false"));
    }

    @Test
    public void addPadIsIdempotentForSamePosition() throws Exception {
        // Production gate: spawnLocations.contains(pos) check uses
        // StationLandingLocation.equals which compares by position. Two
        // adds at the same (x,z) MUST collapse to one entry.
        int id = createStation();
        ok(client().execute("artest station add-pad " + id + " 90 90 first"));
        String second = ok(client().execute(
                "artest station add-pad " + id + " 90 90 second"));
        // padCount stays 1 even after the duplicate add.
        assertTrue("duplicate add at same (x,z) must NOT grow padCount: " + second,
                second.contains("\"padCount\":1"));
    }

    @Test
    public void removePadShrinksList() throws Exception {
        int id = createStation();
        ok(client().execute("artest station add-pad " + id + " 100 100 toremove"));
        ok(client().execute("artest station add-pad " + id + " 110 110 keep"));

        String remove = ok(client().execute("artest station remove-pad " + id + " 100 100"));
        assertTrue("remove-pad must succeed and report removed=1: " + remove,
                remove.contains("\"ok\":true") && remove.contains("\"removed\":1"));
        assertTrue("padCount must drop to 1 after remove: " + remove,
                remove.contains("\"padCount\":1"));

        // The remaining pad's coords must still be reachable.
        String pads = ok(client().execute("artest station pads " + id));
        assertTrue("remaining pad must still be listed: " + pads,
                pads.contains("\"x\":110") && pads.contains("\"z\":110"));
        assertTrue("removed pad must be gone from list: " + pads,
                !(pads.contains("\"x\":100") && pads.contains("\"z\":100")));
    }

    @Test
    public void multipleStationsTrackPadsIndependently() throws Exception {
        // Per-station pad state must not bleed across stations. A regression
        // that consolidated landing pads into a global registry would
        // silently route rockets to the wrong station's pads.
        int a = createStation();
        int b = createStation();
        assertNotEquals("station ids must be unique", a, b);

        ok(client().execute("artest station add-pad " + a + " 200 200 a1"));
        ok(client().execute("artest station add-pad " + b + " 300 300 b1"));

        String padsA = ok(client().execute("artest station pads " + a));
        String padsB = ok(client().execute("artest station pads " + b));
        assertTrue("station A must have its pad: " + padsA,
                padsA.contains("\"x\":200"));
        assertTrue("station A must NOT have station B's pad: " + padsA,
                !padsA.contains("\"x\":300"));
        assertTrue("station B must have its pad: " + padsB,
                padsB.contains("\"x\":300"));
        assertTrue("station B must NOT have station A's pad: " + padsB,
                !padsB.contains("\"x\":200"));
    }

    @Test
    public void infoExposesPadCountAndFreePadFlag() throws Exception {
        // Pin the info probe's pad-related fields; downstream tooling
        // (rocket launch UI, station-finder satellite) reads these.
        int id = createStation();
        String empty = ok(client().execute("artest station info " + id));
        assertEquals("empty station: padCount=0", true,
                empty.contains("\"padCount\":0"));
        assertEquals("empty station: hasFreePad=false (no pads at all)", true,
                empty.contains("\"hasFreePad\":false"));

        ok(client().execute("artest station add-pad " + id + " 400 400 p1"));
        String oneOccupied = ok(client().execute("artest station info " + id));
        assertTrue("after add: padCount=1: " + oneOccupied,
                oneOccupied.contains("\"padCount\":1"));
        // hasFreeLandingPad checks for ANY pad with occupied=false, NOT
        // gating on auto-land. So even a non-auto-land pad reports
        // hasFreePad=true. Pin this contract — it's a separate axis from
        // dock-allocation.
        assertTrue("pad just added (not occupied) → hasFreePad=true: "
                + oneOccupied, oneOccupied.contains("\"hasFreePad\":true"));
    }
}
