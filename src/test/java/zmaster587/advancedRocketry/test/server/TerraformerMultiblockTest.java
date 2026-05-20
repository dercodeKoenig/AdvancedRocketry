package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-04 — Atmosphere Terraformer multiblock validation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.TileAtmosphereTerraformer}
 * — the largest AR multiblock by footprint: a 17×17 sphere-like shape over
 * ~10 layers, mixing {@code blockAdvStructureBlock}, {@code blockOxygenVent},
 * {@code blockConcrete}, {@code blockFuelTank}, {@code Blocks.CLAY} and the
 * {@code 'P'} / {@code 'L'} hatches at the base.</p>
 *
 * <p>Built through the new reflection-backed generic fixture probe
 * {@code /artest fixture multiblock terraformer} — reads the production
 * {@code structure} array directly, so the test stays in sync with the
 * production layout automatically.</p>
 *
 * <p>Position-isolated at x=8000 (well clear of x=7500 SolarArray and
 * predecessors). The 17×17 footprint is much larger than other multiblocks,
 * so successive test methods step by 60 blocks to avoid overlap.</p>
 */
public class TerraformerMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 8000;
    private static final int CY = 64;
    private static final int CZ = 8000;

    @Test
    public void terraformerMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        String fixture = join(client().execute(
                "artest fixture multiblock terraformer 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock terraformer failed: " + fixture,
                fixture.contains("\"ok\":true"));
        assertTrue("fixture didn't place any blocks: " + fixture,
                fixture.contains("\"placed\":") && !fixture.contains("\"placed\":0"));

        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileAtmosphereTerraformer tile at controller pos: " + info,
                info.contains("TileAtmosphereTerraformer"));

        String tryComplete = join(client().execute(
                "artest machine try-complete 0 " + CX + " " + CY + " " + CZ));
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("terraformer multiblock didn't validate (isComplete=false): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void terraformerMultiblockInvalidatesWhenAdjacentAdvStructureRemoved() throws Exception {
        int cx = CX + 60, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock terraformer 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        // The controller sits in the equator ring. An advStructureBlock cell
        // directly adjacent at globalX = cx + 1 (one block east of the
        // controller, same row) is part of the structure — replacing it with
        // stone fails validation. Break BEFORE first try-complete (no-baseline
        // pattern — once hidden, oxygenVent / hatch TE breakBlocks can NPE).
        String breakAdj = join(client().execute(
                "artest place 0 " + (cx + 1) + " " + cy + " " + cz + " minecraft:stone"));
        assertTrue("could not replace neighbour: " + breakAdj,
                breakAdj.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("terraformer validated despite missing neighbour: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
