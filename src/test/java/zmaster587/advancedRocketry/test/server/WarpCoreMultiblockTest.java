package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-04 — Warp Core multiblock validation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.TileWarpCore} —
 * 3×3×3 structure: top rim cap with item-input hatch, middle cross of
 * {@code blockStructureBlock} around a {@code blockWarpCoreCore} centre,
 * bottom controller layer with rim ring + core centre.</p>
 *
 * <p>Both {@code blockWarpCoreRim} (→ Titanium block) and
 * {@code blockWarpCoreCore} (→ Gold block) are AR-registered OreDictionary
 * entries; the fixture resolves them at runtime via
 * {@code firstOreDictBlockState}.</p>
 *
 * <p>Position-isolated at x=5000.</p>
 */
public class WarpCoreMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 5000;
    private static final int CY = 64;
    private static final int CZ = 5000;

    @Test
    public void warpCoreMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        String fixture = join(client().execute(
                "artest fixture multiblock warp-core 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock warp-core failed: " + fixture,
                fixture.contains("\"ok\":true"));

        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileWarpCore tile at controller pos: " + info,
                info.contains("TileWarpCore"));

        String tryComplete = join(client().execute(
                "artest machine try-complete 0 " + CX + " " + CY + " " + CZ));
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("warp-core multiblock didn't validate (isComplete=false): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void warpCoreMultiblockInvalidatesWhenCoreCentreRemoved() throws Exception {
        int cx = CX + 30, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock warp-core 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // Core centre at middle layer → globalY = cy + 1, globalX = cx, globalZ = cz + 1.
        String breakCore = join(client().execute(
                "artest place 0 " + cx + " " + (cy + 1) + " " + (cz + 1) + " minecraft:stone"));
        assertTrue("could not replace core centre: " + breakCore,
                breakCore.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure stayed complete after core centre removal — "
                        + "validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    @Test
    public void warpCoreMultiblockInvalidatesWhenInputHatchRemoved() throws Exception {
        int cx = CX + 60, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock warp-core 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // Input hatch at top → globalY = cy + 2, globalX = cx, globalZ = cz + 1.
        String breakHatch = join(client().execute(
                "artest place 0 " + cx + " " + (cy + 2) + " " + (cz + 1) + " minecraft:stone"));
        assertTrue("could not replace input hatch: " + breakHatch,
                breakHatch.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure stayed complete after input-hatch removal — "
                        + "validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
