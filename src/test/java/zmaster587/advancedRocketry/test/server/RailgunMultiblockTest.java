package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-04 — Railgun multiblock validation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.TileRailgun} is the
 * tallest (and sparsest) AR multiblock — 11 layers × 9×9. Layers 0–8 are
 * pure {@code coilCopper} cross-sections around a {@code blockStructureBlock}
 * core column; layer 9 is a {@code blockSteel}-capped {@code blockTitanium}
 * plus-sign with {@code blockAdvStructureBlock} corners; layer 10 is the
 * full circular dish containing the controller, item-input / item-output
 * hatches, an advanced motor and three power-input plugs.</p>
 *
 * <p>The structure references {@code coilCopper}, {@code blockSteel},
 * {@code blockTitanium} and {@code slab} through the OreDictionary — these
 * are dynamically registered by libVulpes' {@code MaterialRegistry}, so the
 * fixture probe looks them up at runtime via
 * {@code firstOreDictBlockState} rather than hard-coding registry names.
 * If a referenced OreDictionary entry is missing in the test environment,
 * the fixture probe returns an explicit error JSON — failing fast instead
 * of placing the wrong block.</p>
 *
 * <p>Pins three contracts:</p>
 * <ol>
 *   <li>fixture-built layout passes {@code attemptCompleteStructure};</li>
 *   <li>structure invalidates when the core {@code blockStructureBlock}
 *       column is broken at the top — pins the simple-layer pattern;</li>
 *   <li>structure invalidates when the central {@code blockTitanium} in the
 *       y=9 transition layer is removed — pins the special transition layer
 *       (separate code path from the simple-layer loop).</li>
 * </ol>
 *
 * <p>Position-isolated at x=4500 (no collision with BHG x=3000, Beacon
 * x=3500 or Observatory x=4000 fixtures). Each test uses a fresh column to
 * avoid stale-block contamination from prior fixtures in the same test run.</p>
 */
public class RailgunMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 4500;
    private static final int CY = 64;
    private static final int CZ = 4500;

    @Test
    public void railgunMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        String fixture = join(client().execute(
                "artest fixture multiblock railgun 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock railgun failed: " + fixture,
                fixture.contains("\"ok\":true"));

        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileRailgun tile at controller pos: " + info,
                info.contains("TileRailgun"));

        String tryComplete = join(client().execute(
                "artest machine try-complete 0 " + CX + " " + CY + " " + CZ));
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("railgun multiblock didn't validate (isComplete=false): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void railgunMultiblockInvalidatesWhenCoreColumnBroken() throws Exception {
        int cx = CX + 30, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock railgun 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // Top of the core column (y=0 layer struct cell at globalY = cy + 10,
        // globalX = cx, globalZ = cz + 3). Replace with stone.
        String breakCore = join(client().execute(
                "artest place 0 " + cx + " " + (cy + 10) + " " + (cz + 3) + " minecraft:stone"));
        assertTrue("could not break core column: " + breakCore,
                breakCore.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure stayed complete after core column removal — "
                        + "validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    @Test
    public void railgunMultiblockInvalidatesWhenTransitionLayerBroken() throws Exception {
        int cx = CX + 60, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock railgun 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // Centre of the y=9 transition layer (titanium centre at globalY = cy+1,
        // globalX = cx, globalZ = cz + 3). Replace with stone.
        String breakTitanium = join(client().execute(
                "artest place 0 " + cx + " " + (cy + 1) + " " + (cz + 3) + " minecraft:stone"));
        assertTrue("could not break titanium centre: " + breakTitanium,
                breakTitanium.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure stayed complete after transition-layer titanium removal — "
                        + "validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
