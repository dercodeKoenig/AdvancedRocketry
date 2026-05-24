package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-04 — Observatory multiblock validation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.TileObservatory} is a
 * 5×5×5 sparse structure: 3×3 {@code blockStructureBlock} cap with glass-lens
 * cells, a hollow inner chamber with a {@code Blocks.AIR}-required centre at
 * y=2, an IRON_BLOCK wildcard outer ring on the controller layer, and a
 * {@code blockStructureTower} base with a {@code libvulpes:motor} in the
 * centre.</p>
 *
 * <p>Pins the validator through the new
 * {@code /artest fixture multiblock observatory} probe. Verifies:</p>
 * <ol>
 *   <li>fixture-built layout passes {@code attemptCompleteStructure};</li>
 *   <li>structure invalidates when the central lens at y=1 is broken — pins
 *       the {@code Block[]} (lens / glass) check;</li>
 *   <li>structure invalidates when the central motor in the base is removed —
 *       pins the {@code libVulpesBlocks.motors} cell;</li>
 *   <li>structure invalidates when a {@code Blocks.AIR}-required cell at y=2
 *       (the hollow chamber) is filled with stone — pins the strict
 *       air-block check at libVulpes
 *       {@code TileMultiBlock.completeStructure}.</li>
 * </ol>
 *
 * <p>Position-isolated at x=4000 (no collision with BHG x=3000 or Beacon
 * x=3500 fixtures).</p>
 */
public class ObservatoryMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 4000;
    private static final int CY = 64;
    private static final int CZ = 4000;

    @Test
    public void observatoryMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        String fixture = join(client().execute(
                "artest fixture multiblock observatory 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock observatory failed: " + fixture,
                fixture.contains("\"ok\":true"));

        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileObservatory tile at controller pos: " + info,
                info.contains("TileObservatory"));

        String tryComplete = MachineRecipeEndToEndKit.tryCompleteWithRetry(
                client(), 0, CX, CY, CZ);
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("observatory multiblock didn't validate (isComplete=false): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void observatoryMultiblockInvalidatesWhenCentralLensIsRemoved() throws Exception {
        int cx = CX + 30, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock observatory 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = MachineRecipeEndToEndKit.tryCompleteWithRetry(client(), 0, cx, cy, cz);
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // Central lens at y=1 of the structure → globalY = cy + 2, globalX = cx,
        // globalZ = cz + 2 (per handleFixtureObservatory). Replace it with stone.
        String breakLens = join(client().execute(
                "artest place 0 " + cx + " " + (cy + 2) + " " + (cz + 2) + " minecraft:stone"));
        assertTrue("could not replace lens: " + breakLens,
                breakLens.contains("\"ok\":true"));

        String broken = MachineRecipeEndToEndKit.tryCompleteWithRetry(client(), 0, cx, cy, cz);
        assertTrue("structure stayed complete after central lens removal — "
                        + "validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    @Test
    public void observatoryMultiblockInvalidatesWhenMotorIsRemoved() throws Exception {
        int cx = CX + 60, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock observatory 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = MachineRecipeEndToEndKit.tryCompleteWithRetry(client(), 0, cx, cy, cz);
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // Motor at base layer → globalY = cy - 1, globalX = cx, globalZ = cz + 2
        // (per handleFixtureObservatory motorPos).
        String breakMotor = join(client().execute(
                "artest place 0 " + cx + " " + (cy - 1) + " " + (cz + 2) + " minecraft:stone"));
        assertTrue("could not replace motor: " + breakMotor,
                breakMotor.contains("\"ok\":true"));

        String broken = MachineRecipeEndToEndKit.tryCompleteWithRetry(client(), 0, cx, cy, cz);
        assertTrue("structure stayed complete after motor removal — "
                        + "validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    @Test
    public void observatoryMultiblockInvalidatesWhenAirChamberIsFilled() throws Exception {
        int cx = CX + 90, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock observatory 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        String first = MachineRecipeEndToEndKit.tryCompleteWithRetry(client(), 0, cx, cy, cz);
        assertTrue("baseline must validate: " + first,
                first.contains("\"isComplete\":true"));

        // y=2 hollow chamber centre — must be air. Fill it with stone to break.
        // globalY = cy + 1, globalX = cx, globalZ = cz + 1 (interior air cell,
        // not the lens — that's at globalZ = cz + 3).
        String fillAir = join(client().execute(
                "artest place 0 " + cx + " " + (cy + 1) + " " + (cz + 1) + " minecraft:stone"));
        assertTrue("could not fill air chamber: " + fillAir,
                fillAir.contains("\"ok\":true"));

        String broken = MachineRecipeEndToEndKit.tryCompleteWithRetry(client(), 0, cx, cy, cz);
        assertTrue("structure stayed complete after air-chamber fill — "
                        + "Blocks.AIR-cell validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
