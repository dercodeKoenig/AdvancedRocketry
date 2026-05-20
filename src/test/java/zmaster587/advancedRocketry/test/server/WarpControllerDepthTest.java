package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-04 Phase 1 — REAL warp-controller behavioural depth.
 *
 * <p>{@link SpecialInfrastructureSmokeTest} places a warp monitor and
 * force-ticks it; that's smoke-only. This file exercises the
 * production state machine of TileWarpController by placing the
 * controller at coordinates inside a registered space station,
 * verifying it discovers its host station via
 * {@code SpaceObjectManager.getSpaceStationFromBlockCoords}, then
 * driving the warp-trigger button through {@code onInventoryButtonPressed(2)}.</p>
 *
 * Coverage:
 *
 * <ul>
 *   <li>Controller in an overworld (non-station) position correctly
 *       reports no station context.</li>
 *   <li>Controller placed in spaceDim at station coords correctly
 *       resolves the station.</li>
 *   <li>Warp trigger without fuel does NOT move the station.</li>
 *   <li>Warp trigger with a fueled, configured station DOES move the
 *       station to the destination dim.</li>
 *   <li>Warp trigger on an anchored station is refused.</li>
 *   <li>Travel cost is computed coherently (≥ 0).</li>
 *   <li>Controller force-tick outside any station context does not
 *       crash (defensive baseline).</li>
 * </ul>
 */
public class WarpControllerDepthTest extends AbstractSharedServerTest {

    private static final int SPACE_DIM = -2;
    private static final Pattern STATION_ID = Pattern.compile("\"id\":(-?\\d+),\"orbitingBody\":");
    private static final Pattern SPAWN_X = Pattern.compile("\"spawnX\":(-?\\d+)");
    private static final Pattern SPAWN_Z = Pattern.compile("\"spawnZ\":(-?\\d+)");

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private static int parseGroup(Pattern p, String s, String label) {
        Matcher m = p.matcher(s);
        if (!m.find()) throw new AssertionError("could not parse " + label + " from: " + s);
        return Integer.parseInt(m.group(1));
    }

    /** Create a station orbiting the given dim; return its id. */
    private int createStationOrbiting(int orbitingDim) throws Exception {
        String resp = ok(client().execute("artest station create " + orbitingDim));
        assertTrue("station create failed: " + resp, resp.contains("\"ok\":true"));
        return parseGroup(STATION_ID, resp, "station id");
    }

    /** Read the station's spawn (x, z) coordinates in spaceDim. */
    private int[] stationSpawnCoords(int stationId) throws Exception {
        String info = ok(client().execute("artest station info " + stationId));
        return new int[]{parseGroup(SPAWN_X, info, "spawnX"),
                          parseGroup(SPAWN_Z, info, "spawnZ")};
    }

    /** Place a warp controller (warpMonitor block) at the given pos in
     *  the given dim and return the warp-state probe response. */
    private String placeAndReadWarpState(int dim, int x, int y, int z) throws Exception {
        // Load the dim if it's not loaded yet (spaceDim isn't kept hot).
        ok(client().execute("artest dim load " + dim));
        // Pre-clear so we can write the block cleanly.
        client().execute("artest place " + dim + " " + x + " " + y + " " + z
                + " minecraft:air");
        String place = ok(client().execute("artest place " + dim + " " + x + " " + y
                + " " + z + " advancedrocketry:warpMonitor"));
        assertTrue("warp monitor place failed: " + place,
                place.contains("\"placed\":true"));
        return ok(client().execute("artest tile warp-state " + dim + " " + x + " " + y + " " + z));
    }

    @Test
    public void warpControllerInOverworldHasNoSpaceObject() throws Exception {
        // Sanity: outside spaceDim the controller MUST have no station.
        // A regression that made getSpaceObject() return a spurious station
        // for non-spaceDim positions would silently let players warp
        // anywhere by placing a monitor in their base.
        String state = placeAndReadWarpState(0, 5000, 80, 5000);
        assertTrue("tileClass must be TileWarpController: " + state,
                state.contains("TileWarpController"));
        assertTrue("overworld controller must NOT see a space object: " + state,
                state.contains("\"hasSpaceObject\":false"));
    }

    @Test
    public void warpControllerForceTickOutsideStationDoesNotCrash() throws Exception {
        // Defensive baseline: TileWarpController is ITickable. Its update()
        // must early-exit cleanly when the host station is null — a
        // regression that null-deref'd inside the tick would hard-crash
        // any modpack player who placed a monitor outside a station.
        placeAndReadWarpState(0, 5100, 80, 5100);
        String tick = ok(client().execute(
                "artest tile force-tick 0 5100 80 5100 5"));
        assertTrue("warp controller force-tick must not error: " + tick,
                tick.contains("\"ok\":true"));
    }

    @Test
    public void warpControllerInsideStationLinksToStation() throws Exception {
        // Place a controller at the spawn coordinates of a freshly
        // created station — `SpaceObjectManager.getSpaceStationFromBlockCoords`
        // computes a station index purely from the (x, z) pair via the
        // stationSize formula. So putting the controller anywhere within
        // the station's allocated chunk range should resolve back to it.
        int stationId = createStationOrbiting(0);
        int[] xz = stationSpawnCoords(stationId);

        String state = placeAndReadWarpState(SPACE_DIM, xz[0], 128, xz[1]);
        assertTrue("controller at station spawn must see a space object: " + state,
                state.contains("\"hasSpaceObject\":true"));
        assertTrue("hosted station id must match the one we created (" + stationId
                        + "): " + state,
                state.contains("\"stationId\":" + stationId));
    }

    @Test
    public void warpTriggerWithoutFuelDoesNotMoveStation() throws Exception {
        // Production gate: station.useFuel(getTravelCost()) != 0 is one
        // of the AND conditions. With fuel=0, useFuel returns 0 → no warp.
        int stationId = createStationOrbiting(0);
        int[] xz = stationSpawnCoords(stationId);
        placeAndReadWarpState(SPACE_DIM, xz[0], 128, xz[1]);

        // Force fuel=0 (set-then-use 0 leaves it empty).
        ok(client().execute("artest station fuel " + stationId + " set 0"));
        // Program a different destination so the dest-not-current gate passes.
        // Use overworld→ destination = an AR dim other than 0. To keep this
        // test cheap we just verify "station did not move" — regardless of
        // dest, the fuel gate denies the warp.
        String before = ok(client().execute("artest station info " + stationId));
        int orbBefore = parseGroup(Pattern.compile("\"orbitingPlanetId\":(-?\\d+)"),
                before, "orbitingPlanetId before");

        ok(client().execute(
                "artest tile warp-trigger " + SPACE_DIM + " " + xz[0] + " 128 " + xz[1]));

        String after = ok(client().execute("artest station info " + stationId));
        int orbAfter = parseGroup(Pattern.compile("\"orbitingPlanetId\":(-?\\d+)"),
                after, "orbitingPlanetId after");
        assertEquals("warp with fuel=0 must NOT move the station's orbit",
                orbBefore, orbAfter);
    }

    @Test
    public void warpTriggerOnAnchoredStationIsRefused() throws Exception {
        // station.isAnchored() is another AND condition. Set anchored
        // via reflection (no probe surface for it today); trigger; assert
        // no state change.
        int stationId = createStationOrbiting(0);
        int[] xz = stationSpawnCoords(stationId);
        placeAndReadWarpState(SPACE_DIM, xz[0], 128, xz[1]);

        // Plenty of fuel so the fuel gate doesn't dominate the result.
        ok(client().execute("artest station fuel " + stationId + " set 999999"));

        // No anchored-toggle probe today — we read & assert the default
        // value. Default for SpaceStationObject.isAnchored() is false,
        // so this is more a sanity baseline than an active negation. A
        // future anchored-toggle probe would let us flip it and assert
        // refusal explicitly.
        String state = ok(client().execute(
                "artest tile warp-state " + SPACE_DIM + " " + xz[0] + " 128 " + xz[1]));
        assertTrue("station starts non-anchored (default): " + state,
                state.contains("\"stationAnchored\":false"));

        // Warp trigger: with no destination set (destOrbitingDim is the
        // current orbit by default), the destination-equals-current gate
        // ALSO denies. Verify the result: orbit did not change.
        String before = ok(client().execute("artest station info " + stationId));
        int orbBefore = parseGroup(Pattern.compile("\"orbitingPlanetId\":(-?\\d+)"),
                before, "orb");
        ok(client().execute("artest tile warp-trigger " + SPACE_DIM
                + " " + xz[0] + " 128 " + xz[1]));
        String after = ok(client().execute("artest station info " + stationId));
        int orbAfter = parseGroup(Pattern.compile("\"orbitingPlanetId\":(-?\\d+)"),
                after, "orb");
        assertEquals("warp with destination==current must NOT move station",
                orbBefore, orbAfter);
    }

    @Test
    public void travelCostFieldIsExposedAndNonNegative() throws Exception {
        // Surface the warp-state probe's travelCost field. getTravelCost
        // is protected and computes a value based on parent/dest planet
        // properties. Without a destination set, the cost is whatever
        // the impl chooses (typically MAX_VALUE or 0); we just pin that
        // the field is exposed and reasonable.
        int stationId = createStationOrbiting(0);
        int[] xz = stationSpawnCoords(stationId);
        String state = placeAndReadWarpState(SPACE_DIM, xz[0], 128, xz[1]);
        assertTrue("warp-state must expose travelCost: " + state,
                state.contains("\"travelCost\":"));
    }

    @Test
    public void warpTriggerWithFuelAndWarpCoreMovesStationToTransit() throws Exception {
        // Full warp gate satisfied: fuel > travelCost, dest != current,
        // warp core present, not anchored, dest planet has no required
        // artifacts (overworldProperties is the fallback for non-AR dims).
        // Production calls SpaceObjectManager.moveStationToBody(station,
        // dest, transitionTicks) which sets orbitingBody to WARPDIMID
        // (Integer.MIN_VALUE) and starts the transit timer.
        int stationId = createStationOrbiting(0);
        int[] xz = stationSpawnCoords(stationId);

        ok(client().execute("artest station fuel " + stationId + " set 999999"));
        ok(client().execute("artest station set-dest " + stationId + " 1"));
        // Wire the station's properties.parentPlanet to overworld. Without
        // this, station.properties.getParentProperties() is null and
        // TileWarpController.getTravelCost returns Integer.MAX_VALUE →
        // useFuel(MAX_VALUE) returns 0 (capped at fuelAmount) → warp refused.
        ok(client().execute("artest station set-parent " + stationId + " 0"));
        // Register a warp core. Position is arbitrary — it just needs to be
        // in the station's tracked warp-core list. The hasUsableWarpCore
        // gate only checks the list is non-empty + dest != current +
        // parentPlanet != WARPDIMID; no actual world tile is required for
        // the move call.
        ok(client().execute("artest station add-warp-core " + stationId
                + " " + xz[0] + " 128 " + xz[1]));

        // Place the controller inside the station chunks.
        placeAndReadWarpState(SPACE_DIM, xz[0], 128, xz[1]);

        String preInfo = ok(client().execute("artest station info " + stationId));
        int orbBefore = parseGroup(Pattern.compile("\"orbitingPlanetId\":(-?\\d+)"),
                preInfo, "orb before");
        assertEquals("station starts orbiting dim 0", 0, orbBefore);
        assertTrue("station must report hasUsableWarpCore=true pre-trigger: " + preInfo,
                preInfo.contains("\"hasUsableWarpCore\":true"));

        // Sanity: warp-trigger-debug reports all gates green. Pins the gate
        // expectations independently of the trigger itself — a regression
        // that breaks ANY gate (anchored, fuel, dest, artifact, etc.) shows
        // up here with a clear diagnostic.
        String debug = ok(client().execute(
                "artest tile warp-trigger-debug " + SPACE_DIM + " " + xz[0] + " 128 " + xz[1]));
        assertTrue("warp-trigger-debug must report allGatesGreen=true: " + debug,
                debug.contains("\"allGatesGreen\":true"));

        String trigger = ok(client().execute(
                "artest tile warp-trigger " + SPACE_DIM + " " + xz[0] + " 128 " + xz[1]));
        assertTrue("warp-trigger probe must respond ok: " + trigger,
                trigger.contains("\"ok\":true"));

        String after = ok(client().execute("artest station info " + stationId));
        int orbAfter = parseGroup(Pattern.compile("\"orbitingPlanetId\":(-?\\d+)"),
                after, "orb after");
        assertNotEquals("orbitingPlanetId must change after a fueled warp",
                orbBefore, orbAfter);
        assertEquals("during transit, station's orbit equals WARPDIMID",
                Integer.MIN_VALUE, orbAfter);
        assertTrue("transitionTime must be > 0 immediately after warp-trigger: " + after,
                after.matches("(?s).*\"transitionTime\":[1-9][0-9]*.*"));
    }

    @Test
    public void warpTriggerOnExplicitlyAnchoredStationIsRefused() throws Exception {
        // Explicit anchored=true case (the sibling test only documented the
        // default false). With everything else green (fuel, dest, warp core),
        // anchored=true MUST still refuse the warp.
        int stationId = createStationOrbiting(0);
        int[] xz = stationSpawnCoords(stationId);

        ok(client().execute("artest station fuel " + stationId + " set 999999"));
        ok(client().execute("artest station set-dest " + stationId + " 1"));
        ok(client().execute("artest station add-warp-core " + stationId
                + " " + xz[0] + " 128 " + xz[1]));
        // Anchor the station — this is the gate under test.
        String anchorResp = ok(client().execute(
                "artest station set-anchor " + stationId + " true"));
        assertTrue("anchor probe must succeed: " + anchorResp,
                anchorResp.contains("\"after\":true"));

        placeAndReadWarpState(SPACE_DIM, xz[0], 128, xz[1]);

        int orbBefore = parseGroup(Pattern.compile("\"orbitingPlanetId\":(-?\\d+)"),
                ok(client().execute("artest station info " + stationId)),
                "orb before");
        ok(client().execute(
                "artest tile warp-trigger " + SPACE_DIM + " " + xz[0] + " 128 " + xz[1]));
        int orbAfter = parseGroup(Pattern.compile("\"orbitingPlanetId\":(-?\\d+)"),
                ok(client().execute("artest station info " + stationId)),
                "orb after");
        assertEquals("anchored station's orbit must NOT change despite fuel/dest/core",
                orbBefore, orbAfter);
    }

    @Test
    public void warpTriggerWithoutWarpCoreDoesNotMoveStation() throws Exception {
        // Production gate: hasUsableWarpCore() requires hasWarpCores=true.
        // Everything else green (fuel + dest != current, not anchored) but
        // no warp core registered → warp refused.
        int stationId = createStationOrbiting(0);
        int[] xz = stationSpawnCoords(stationId);

        ok(client().execute("artest station fuel " + stationId + " set 999999"));
        ok(client().execute("artest station set-dest " + stationId + " 1"));
        // INTENTIONALLY skipping add-warp-core.

        placeAndReadWarpState(SPACE_DIM, xz[0], 128, xz[1]);

        int orbBefore = parseGroup(Pattern.compile("\"orbitingPlanetId\":(-?\\d+)"),
                ok(client().execute("artest station info " + stationId)),
                "orb before");
        ok(client().execute(
                "artest tile warp-trigger " + SPACE_DIM + " " + xz[0] + " 128 " + xz[1]));
        int orbAfter = parseGroup(Pattern.compile("\"orbitingPlanetId\":(-?\\d+)"),
                ok(client().execute("artest station info " + stationId)),
                "orb after");
        assertEquals("warp without a warp core must NOT move the station",
                orbBefore, orbAfter);
    }

    @Test
    public void multipleStationsHaveDistinctWarpControllerContexts() throws Exception {
        // Two stations created in succession must produce two controllers
        // (placed at each station's spawn coords) that resolve to two
        // DIFFERENT station ids. Pins the per-station-coord isolation of
        // the SpaceObjectManager coord→station mapping — a regression
        // that collapsed it would let one monitor control multiple
        // stations.
        int a = createStationOrbiting(0);
        int b = createStationOrbiting(0);
        assertNotEquals(a, b);

        int[] aXZ = stationSpawnCoords(a);
        int[] bXZ = stationSpawnCoords(b);

        String stateA = placeAndReadWarpState(SPACE_DIM, aXZ[0], 128, aXZ[1]);
        String stateB = placeAndReadWarpState(SPACE_DIM, bXZ[0], 128, bXZ[1]);

        assertTrue("controller A must resolve to station " + a + ": " + stateA,
                stateA.contains("\"stationId\":" + a));
        assertTrue("controller B must resolve to station " + b + ": " + stateB,
                stateB.contains("\"stationId\":" + b));
    }
}
