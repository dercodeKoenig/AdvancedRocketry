package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-04 — Area Gravity Controller multiblock validation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.TileAreaGravityController}
 * — smallest AR multiblock: 2×3×3 with only 6 non-null cells (controller +
 * 4 advStructure cross + 1 power-input plug below the controller).</p>
 *
 * <p>Position-isolated at x=5500.</p>
 */
public class AreaGravityControllerMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 5500;
    private static final int CY = 64;
    private static final int CZ = 5500;

    @Test
    public void gravityControllerMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        String fixture = join(client().execute(
                "artest fixture multiblock gravity-controller 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock gravity-controller failed: " + fixture,
                fixture.contains("\"ok\":true"));

        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileAreaGravityController tile at controller pos: " + info,
                info.contains("TileAreaGravityController"));

        String tryComplete = join(client().execute(
                "artest machine try-complete 0 " + CX + " " + CY + " " + CZ));
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("gravity-controller multiblock didn't validate (isComplete=false): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void gravityControllerMultiblockInvalidatesWhenPlugRemoved() throws Exception {
        int cx = CX + 30, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock gravity-controller 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // Power-input plug directly under controller → globalY = cy - 1, globalX = cx, globalZ = cz.
        String breakPlug = join(client().execute(
                "artest place 0 " + cx + " " + (cy - 1) + " " + cz + " minecraft:stone"));
        assertTrue("could not replace plug: " + breakPlug,
                breakPlug.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure stayed complete after plug removal — "
                        + "validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    @Test
    public void gravityControllerMultiblockInvalidatesWhenAdvStructureRemoved() throws Exception {
        int cx = CX + 60, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock gravity-controller 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // advStructure at (cx+1, cy-1, cz) — east arm of the cross.
        String breakArm = join(client().execute(
                "artest place 0 " + (cx + 1) + " " + (cy - 1) + " " + cz + " minecraft:stone"));
        assertTrue("could not break arm: " + breakArm,
                breakArm.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure stayed complete after arm removal — "
                        + "validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
