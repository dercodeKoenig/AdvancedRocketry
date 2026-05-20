package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-04 — Solar Array multiblock validation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.energy.TileSolarArray}
 * — 22-row × 3-wide single-layer structure. The wildcard '*' accepts the
 * solar-array-panel block OR {@code Blocks.AIR}, so the minimal valid
 * fixture places only the controller + two power-output plugs and leaves
 * the rest air.</p>
 *
 * <p>Pins the wildcard-accepts-AIR contract: a regression that tightens
 * the validator (or drops {@code Blocks.AIR} from the wildcard list)
 * would break this layout.</p>
 *
 * <p>Position-isolated at x=7500.</p>
 */
public class SolarArrayMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 7500;
    private static final int CY = 64;
    private static final int CZ = 7500;

    @Test
    public void solarArrayMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        String fixture = join(client().execute(
                "artest fixture multiblock solar-array 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock solar-array failed: " + fixture,
                fixture.contains("\"ok\":true"));

        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileSolarArray tile at controller pos: " + info,
                info.contains("TileSolarArray"));

        String tryComplete = join(client().execute(
                "artest machine try-complete 0 " + CX + " " + CY + " " + CZ));
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("solar-array multiblock didn't validate (isComplete=false): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void solarArrayMultiblockInvalidatesWhenFlankingPlugRemoved() throws Exception {
        int cx = CX + 30, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock solar-array 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        // Right plug flanking controller — globalY = cy, globalX = cx + 1, globalZ = cz.
        // Replacing with stone (NOT removing the plug TE — stone doesn't match 'p',
        // so the validator fails, and the plug TE's invalidate is bypassed since
        // we never invoked try-complete first).
        String breakPlug = join(client().execute(
                "artest place 0 " + (cx + 1) + " " + cy + " " + cz + " minecraft:stone"));
        assertTrue("could not replace plug: " + breakPlug,
                breakPlug.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure validated despite missing 'p' plug: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    @Test
    public void solarArrayMultiblockInvalidatesWhenWildcardCellFilledWithStone() throws Exception {
        int cx = CX + 60, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock solar-array 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        // The '*' wildcard accepts solarArrayPanel OR Blocks.AIR — but NOT
        // stone. Replace a mid-array panel with stone and the validator
        // should reject. Row z=10 (mid-array), x=1 centre — globalY = cy,
        // globalX = cx, globalZ = cz + 10.
        String breakCell = join(client().execute(
                "artest place 0 " + cx + " " + cy + " " + (cz + 10) + " minecraft:stone"));
        assertTrue("could not replace panel: " + breakCell,
                breakCell.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure validated despite stone in '*' wildcard cell: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
