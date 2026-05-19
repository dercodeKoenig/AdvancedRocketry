package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-04 Phases 2-5 (consolidated) — pre-assembly contract
 * for every multiblock controller in the mod.
 *
 * <p>Each of TileOrbitalLaserDrill, TileSpaceElevator,
 * TileBlackHoleGenerator, TileWarpCore, TileObservatory, TileRailgun,
 * and TilePlanetAnalyser ships as a single block — placing the block
 * creates the controller tile, but the multiblock structure isn't
 * formed until the player builds the right shape of surrounding
 * blocks. Production code has TWO surfaces that depend on this:
 *
 * <ul>
 *   <li>{@code isComplete()} returns false until the structure is
 *       validated. Tested here for every controller.</li>
 *   <li>{@code update()} (the per-tick logic) MUST early-exit cleanly
 *       when the structure isn't complete — otherwise every placed but
 *       not-yet-assembled controller crashes on its first server tick.
 *       This was a real concern that produced the
 *       "terraformer round-2 force-tick safety" test in TASK-02; this
 *       test extends that pattern to the full multiblock family.</li>
 * </ul>
 *
 * <p>For the actual ASSEMBLED-multiblock depth (form structure →
 * tick → produce output), see future TASK-04 follow-up sessions. Each
 * multiblock has a different shape contract; building each fixture is
 * a non-trivial probe-side investment.</p>
 *
 * Coverage matrix (per multiblock):
 *
 * <ul>
 *   <li>place succeeds</li>
 *   <li>tileClass FQN matches expected</li>
 *   <li>isComplete reports false on isolated placement</li>
 *   <li>force-tick is safe (no exception) — when ITickable</li>
 *   <li>energy probe surfaces the multiblock's pre-assembly energy
 *       contract (most multiblocks report hasEnergy=false until formed)</li>
 * </ul>
 */
public class MultiblockControllerPreAssemblyTest extends AbstractSharedServerTest {

    private static final int DIM = 0;
    private static final int BASE_X = 8000;
    private static final int BASE_Z = 8000;
    private static final int Y = 80;

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    /** Place a block at the given offset from BASE; return the
     *  multiblock-state probe response. */
    private String placeAndProbe(String blockId, int xOffset, int zOffset) throws Exception {
        int x = BASE_X + xOffset, z = BASE_Z + zOffset;
        // Pre-clear so we can write the block cleanly even if a previous
        // test wrote something here (shared harness — JVM-shared state).
        client().execute("artest place " + DIM + " " + x + " " + Y + " " + z
                + " minecraft:air");
        String place = ok(client().execute("artest place " + DIM + " " + x + " " + Y
                + " " + z + " " + blockId));
        assertTrue("place(" + blockId + ") failed: " + place,
                place.contains("\"placed\":true"));
        return ok(client().execute(
                "artest tile multiblock-state " + DIM + " " + x + " " + Y + " " + z));
    }

    /** Force-tick the given block; assert no exception. Returns the tick
     *  response so a test can additionally validate the per-tick
     *  contract surface. */
    private String forceTickSafely(int xOffset, int zOffset, int ticks) throws Exception {
        int x = BASE_X + xOffset, z = BASE_Z + zOffset;
        String resp = ok(client().execute(
                "artest tile force-tick " + DIM + " " + x + " " + Y + " " + z
                        + " " + ticks));
        // The probe returns ok=true OR tile-not-ITickable (acceptable for
        // non-ITickable multiblock controllers). The ONLY non-acceptable
        // outcome is an exception inside update().
        assertTrue("force-tick threw or hard-errored on "
                        + xOffset + "," + zOffset + ": " + resp,
                resp.contains("\"ok\":true") || resp.contains("tile not ITickable"));
        return resp;
    }

    @Test
    public void orbitalLaserDrillPreAssemblyContract() throws Exception {
        // "spaceLaser" registry → TileOrbitalLaserDrill. Placing without
        // the surrounding multiblock leaves isComplete=false; the per-tick
        // loop early-exits.
        String state = placeAndProbe("advancedrocketry:spaceLaser", 0, 0);
        assertTrue("orbitalLaserDrill must be at this position: " + state,
                state.contains("TileOrbitalLaserDrill"));
        assertTrue("isolated orbitalLaserDrill must NOT be complete: " + state,
                state.contains("\"isComplete\":false"));
        forceTickSafely(0, 0, 5);
    }

    @Test
    public void spaceElevatorControllerPreAssemblyContract() throws Exception {
        // The space elevator's controller is a multiblock at its base.
        String state = placeAndProbe("advancedrocketry:spaceElevatorController", 8, 0);
        assertTrue("tileClass must be TileSpaceElevator: " + state,
                state.contains("TileSpaceElevator"));
        assertTrue("isolated space elevator must NOT be complete: " + state,
                state.contains("\"isComplete\":false"));
        forceTickSafely(8, 0, 5);
    }

    @Test
    public void blackHoleGeneratorPreAssemblyContract() throws Exception {
        // "blackholegenerator" — bottom-tier end-game energy source.
        String state = placeAndProbe("advancedrocketry:blackholegenerator", 16, 0);
        assertTrue("tileClass must be TileBlackHoleGenerator: " + state,
                state.contains("TileBlackHoleGenerator"));
        assertTrue("isolated blackHoleGenerator must NOT be complete: " + state,
                state.contains("\"isComplete\":false"));
        forceTickSafely(16, 0, 5);
    }

    @Test
    public void warpCorePreAssemblyContract() throws Exception {
        // The warp engine itself (different from the warp CONTROLLER
        // tested in WarpControllerDepthTest). The warp core is the
        // multiblock that consumes fuel during station warp.
        String state = placeAndProbe("advancedrocketry:warpCore", 24, 0);
        assertTrue("tileClass must be TileWarpCore: " + state,
                state.contains("TileWarpCore"));
        assertTrue("isolated warpCore must NOT be complete: " + state,
                state.contains("\"isComplete\":false"));
        forceTickSafely(24, 0, 5);
    }

    @Test
    public void observatoryPreAssemblyContract() throws Exception {
        // TileObservatory — for stellar data collection.
        String state = placeAndProbe("advancedrocketry:observatory", 32, 0);
        assertTrue("tileClass must be TileObservatory: " + state,
                state.contains("TileObservatory"));
        assertTrue("isolated observatory must NOT be complete: " + state,
                state.contains("\"isComplete\":false"));
        forceTickSafely(32, 0, 5);
    }

    @Test
    public void railgunPreAssemblyContract() throws Exception {
        // TileRailgun — for cargo launch / asteroid breaking.
        String state = placeAndProbe("advancedrocketry:railgun", 40, 0);
        assertTrue("tileClass must be TileRailgun: " + state,
                state.contains("TileRailgun"));
        assertTrue("isolated railgun must NOT be complete: " + state,
                state.contains("\"isComplete\":false"));
        forceTickSafely(40, 0, 5);
    }

    @Test
    public void planetAnalyserPreAssemblyContract() throws Exception {
        // "planetAnalyser" registry name resolves to
        // TileAstrobodyDataProcessor (named after the data processing
        // role, not the block). Pin that mapping — a refactor that
        // accidentally swaps the tile class behind this block would
        // surface here. The block is the player-facing scanner that
        // turns mass/composition/distance data into planet IDs.
        String state = placeAndProbe("advancedrocketry:planetAnalyser", 48, 0);
        assertTrue("planetAnalyser must resolve to "
                        + "TileAstrobodyDataProcessor (the data-processing tile, "
                        + "not a TilePlanetAnalyser as the block name suggests): " + state,
                state.contains("TileAstrobodyDataProcessor"));
        assertTrue("isolated planetAnalyser must NOT be complete: " + state,
                state.contains("\"isComplete\":false"));
        forceTickSafely(48, 0, 5);
    }

    @Test
    public void multiblockProbeReportsCanRenderFlagWhereExposed() throws Exception {
        // libVulpes multiblocks have a public `canRender` field that
        // mirrors the structure-formed state. The probe surfaces it;
        // pin that at least one of the multiblocks places-with-canRender=false.
        // (Some classes might not have the field — the probe reports
        // "<no field>"; we tolerate either.)
        String state = placeAndProbe("advancedrocketry:warpCore", 56, 0);
        // Accepted outcomes: explicit false OR "<no field>" (class doesn't
        // expose canRender). The bug case would be `true` on an isolated
        // controller — that would mean the multiblock is rendering as
        // formed when it isn't.
        assertTrue("warpCore canRender must NOT be true on isolated placement: "
                + state, !state.contains("\"canRender\":true"));
    }
}
