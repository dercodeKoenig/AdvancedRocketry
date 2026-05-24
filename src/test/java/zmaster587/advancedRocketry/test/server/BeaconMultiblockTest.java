package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-04 — Beacon multiblock validation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.TileBeacon} has the
 * smallest libVulpes-based structure in AR: a 5×3×3 pillar with a
 * {@code REDSTONE_BLOCK} tip + 4 {@code blockStructureBlock} shaft levels +
 * a 5-block base ring at the controller layer.</p>
 *
 * <p>Pins the validator end-to-end through the new
 * {@code /artest fixture multiblock beacon} probe (second multiblock fixture
 * after BHG). Verifies <strong>three</strong> contracts:</p>
 * <ol>
 *   <li>fixture-built layout passes {@code attemptCompleteStructure};</li>
 *   <li>structure invalidates when the redstone tip is broken — the
 *       Blocks.AIR-required cells around the tip are what makes this
 *       structure interesting: a regression that ignores the "must be air"
 *       constraint would let the structure validate even with debris on
 *       top of it;</li>
 *   <li>structure invalidates when the structure shaft is broken — the
 *       inverse case of (2).</li>
 * </ol>
 *
 * <p>Position-isolated at x=3500 (no collision with BHG fixtures at
 * x=3000..3090).</p>
 */
public class BeaconMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 3500;
    private static final int CY = 64;
    private static final int CZ = 3500;

    @Test
    public void beaconMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        String fixture = join(client().execute(
                "artest fixture multiblock beacon 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock beacon failed: " + fixture,
                fixture.contains("\"ok\":true"));

        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileBeacon tile at controller pos: " + info,
                info.contains("TileBeacon"));

        String tryComplete = MachineRecipeEndToEndKit.tryCompleteWithRetry(
                client(), 0, CX, CY, CZ);
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("beacon multiblock didn't validate (isComplete=false): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void beaconMultiblockInvalidatesWhenRedstoneTipIsRemoved() throws Exception {
        int cx = CX + 30, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock beacon 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = MachineRecipeEndToEndKit.tryCompleteWithRetry(client(), 0, cx, cy, cz);
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // Replace the redstone tip with a stone block (any non-air, non-
        // redstone block fails the structure check).
        String breakTip = join(client().execute(
                "artest place 0 " + cx + " " + (cy + 4) + " " + (cz + 1) + " minecraft:stone"));
        assertTrue("could not replace redstone tip: " + breakTip,
                breakTip.contains("\"ok\":true"));

        String broken = MachineRecipeEndToEndKit.tryCompleteWithRetry(client(), 0, cx, cy, cz);
        assertTrue("structure stayed complete after redstone tip removal — "
                        + "validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    @Test
    public void beaconMultiblockInvalidatesWhenShaftBlockIsRemoved() throws Exception {
        int cx = CX + 60, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock beacon 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = MachineRecipeEndToEndKit.tryCompleteWithRetry(client(), 0, cx, cy, cz);
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // Replace a middle shaft block (y=cy+2 layer) with air. The
        // structure has blockStructureBlock at this position — replacing
        // it with air fails (air-only positions are different cells).
        String breakShaft = join(client().execute(
                "artest place 0 " + cx + " " + (cy + 2) + " " + (cz + 1) + " minecraft:air"));
        assertTrue("could not break shaft block: " + breakShaft,
                breakShaft.contains("\"ok\":true"));

        String broken = MachineRecipeEndToEndKit.tryCompleteWithRetry(client(), 0, cx, cy, cz);
        assertTrue("structure stayed complete after shaft removal — "
                        + "validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
