package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-04 — Planet Analyser (TileAstrobodyDataProcessor) multiblock validation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.TileAstrobodyDataProcessor}
 * — 2×2×3 small structure: top row of slabs with controller, bottom row with
 * power input + item I/O + three data hatches.</p>
 *
 * <p>Pins the {@code 'D'} char-mapping (AR-registered, not libVulpes) — a
 * regression that drops the {@code AdvancedRocketry.addMapping('D', ...)}
 * call would silently break this validator.</p>
 *
 * <p>Position-isolated at x=6000.</p>
 */
public class PlanetAnalyserMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 6000;
    private static final int CY = 64;
    private static final int CZ = 6000;

    @Test
    public void planetAnalyserMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        String fixture = join(client().execute(
                "artest fixture multiblock planet-analyser 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock planet-analyser failed: " + fixture,
                fixture.contains("\"ok\":true"));

        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileAstrobodyDataProcessor tile at controller pos: " + info,
                info.contains("TileAstrobodyDataProcessor"));

        String tryComplete = join(client().execute(
                "artest machine try-complete 0 " + CX + " " + CY + " " + CZ));
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("planet-analyser multiblock didn't validate (isComplete=false): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void planetAnalyserMultiblockInvalidatesWhenDataHatchRemoved() throws Exception {
        int cx = CX + 30, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock planet-analyser 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // Centre data hatch at globalY = cy - 1, globalX = cx, globalZ = cz + 1.
        String breakData = join(client().execute(
                "artest place 0 " + cx + " " + (cy - 1) + " " + (cz + 1) + " minecraft:stone"));
        assertTrue("could not replace data hatch: " + breakData,
                breakData.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure stayed complete after data-hatch removal — "
                        + "'D' char mapping broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    @Test
    public void planetAnalyserMultiblockInvalidatesWhenSlabRemoved() throws Exception {
        int cx = CX + 60, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock planet-analyser 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // Slab next to controller at globalY = cy, globalX = cx + 1, globalZ = cz.
        String breakSlab = join(client().execute(
                "artest place 0 " + (cx + 1) + " " + cy + " " + cz + " minecraft:stone"));
        assertTrue("could not replace slab: " + breakSlab,
                breakSlab.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure stayed complete after slab removal — "
                        + "slab OreDict lookup broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
