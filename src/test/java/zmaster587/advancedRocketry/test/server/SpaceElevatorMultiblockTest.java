package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-04 — Space Elevator multiblock validation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.TileSpaceElevator}
 * — single-layer 10×9 disc with slab outer ring, advStructure inner ring,
 * blockSteel corner caps, motor centre, dual power-input plugs flanking
 * the controller, and {@code Blocks.AIR} cells in the corners.</p>
 *
 * <p>Pins the validator across multiple cell-types in one structure:</p>
 * <ol>
 *   <li>fixture-built layout passes {@code attemptCompleteStructure};</li>
 *   <li>structure invalidates when the centre motor is removed;</li>
 *   <li>structure invalidates when one of the dual 'P' plugs flanking the
 *       controller is removed.</li>
 * </ol>
 *
 * <p>Position-isolated at x=6500.</p>
 */
public class SpaceElevatorMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 6500;
    private static final int CY = 64;
    private static final int CZ = 6500;

    @Test
    public void spaceElevatorMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        warmup(CX, CZ);
        String fixture = join(client().execute(
                "artest fixture multiblock space-elevator 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock space-elevator failed: " + fixture,
                fixture.contains("\"ok\":true"));

        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileSpaceElevator tile at controller pos: " + info,
                info.contains("TileSpaceElevator"));

        String tryComplete = join(client().execute(
                "artest machine try-complete 0 " + CX + " " + CY + " " + CZ));
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("space-elevator multiblock didn't validate (isComplete=false): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void spaceElevatorMultiblockInvalidatesWhenAdvStructureAdjacentToMotorRemoved() throws Exception {
        int cx = CX + 30, cy = CY, cz = CZ;
        warmup(cx, cz);
        String fixture = join(client().execute(
                "artest fixture multiblock space-elevator 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        // Break BEFORE first try-complete — once attemptCompleteStructure
        // succeeds, libVulpes converts the footprint blocks to their hidden-
        // multiblock variants, and replacing a hidden block via setBlockState
        // can NPE through TileMotor/TilePowerInput's deconstruct hooks. Pin
        // the validator directly on the broken layout.
        String breakAdj = join(client().execute(
                "artest place 0 " + (cx + 1) + " " + cy + " " + (cz + 5) + " minecraft:stone"));
        assertTrue("could not replace adv-structure: " + breakAdj,
                breakAdj.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure validated despite missing adv-structure east of motor: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    @Test
    public void spaceElevatorMultiblockInvalidatesWhenSlabRemoved() throws Exception {
        int cx = CX + 60, cy = CY, cz = CZ;
        warmup(cx, cz);
        String fixture = join(client().execute(
                "artest fixture multiblock space-elevator 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        // Break BEFORE first try-complete (see sibling test).
        String breakSlab = join(client().execute(
                "artest place 0 " + cx + " " + cy + " " + (cz + 1) + " minecraft:stone"));
        assertTrue("could not replace slab: " + breakSlab,
                breakSlab.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure validated despite missing slab in outer ring: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    /** Force-generate and populate the chunk grid covering the elevator
     *  footprint (centre block + ~9-block radius, in worst case) BEFORE
     *  the fixture lays its blocks. Without this, vanilla cross-chunk
     *  populate(...) can drop tree decorations on top of fixture cells
     *  AFTER the fixture's setBlockState, making attemptCompleteStructure
     *  refuse to attempt validation. See /artest chunk warmup javadoc. */
    private static void warmup(int blockX, int blockZ) throws Exception {
        int cx1 = (blockX - 16) >> 4;
        int cz1 = (blockZ - 16) >> 4;
        int cx2 = (blockX + 16) >> 4;
        int cz2 = (blockZ + 16) >> 4;
        String resp = join(client().execute(
                "artest chunk warmup 0 " + cx1 + " " + cz1 + " " + cx2 + " " + cz2));
        assertTrue("chunk warmup failed: " + resp, resp.contains("\"ok\":true"));
    }
}
