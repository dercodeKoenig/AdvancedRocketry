package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-04 — Microwave Receiver multiblock validation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.energy.TileMicrowaveReciever}
 * — single layer 5×5 with {@code blockSolarPanel} ring (and wildcards that
 * accept the same panel) around the controller centre.</p>
 *
 * <p>Promotes the existing smoke-level Microwave coverage in
 * {@code SpecialInfrastructureSmokeTest} to behavioural-depth.</p>
 *
 * <p>Position-isolated at x=7000.</p>
 */
public class MicrowaveReceiverMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 7000;
    private static final int CY = 64;
    private static final int CZ = 7000;

    @Test
    public void microwaveReceiverMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        String fixture = join(client().execute(
                "artest fixture multiblock microwave-receiver 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock microwave-receiver failed: " + fixture,
                fixture.contains("\"ok\":true"));

        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileMicrowaveReciever tile at controller pos: " + info,
                info.contains("TileMicrowaveReciever"));

        String tryComplete = join(client().execute(
                "artest machine try-complete 0 " + CX + " " + CY + " " + CZ));
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("microwave-receiver multiblock didn't validate (isComplete=false): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void microwaveReceiverMultiblockInvalidatesWhenCornerPanelRemoved() throws Exception {
        int cx = CX + 30, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock microwave-receiver 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        // Break BEFORE first try-complete (no-baseline pattern — solar panels
        // are TE-aware in hidden-multiblock state).
        // NW corner — globalY = cy, globalX = cx + 2, globalZ = cz - 2.
        String breakCorner = join(client().execute(
                "artest place 0 " + (cx + 2) + " " + cy + " " + (cz - 2) + " minecraft:stone"));
        assertTrue("could not replace corner: " + breakCorner,
                breakCorner.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure validated despite missing corner panel: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    @Test
    public void microwaveReceiverMultiblockInvalidatesWhenAdjacentPanelRemoved() throws Exception {
        int cx = CX + 60, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock microwave-receiver 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        // Cell immediately east of controller — globalY = cy, globalX = cx + 1, globalZ = cz.
        String breakAdj = join(client().execute(
                "artest place 0 " + (cx + 1) + " " + cy + " " + cz + " minecraft:stone"));
        assertTrue("could not replace adjacent panel: " + breakAdj,
                breakAdj.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("structure validated despite missing adjacent panel: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
