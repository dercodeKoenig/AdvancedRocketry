package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-04 — Orbital Laser Drill multiblock validation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.orbitallaserdrill.TileOrbitalLaserDrill}
 * — a 3-layer 11×9 sparse structure mixing {@code blockAdvStructureBlock},
 * {@code blockStructureBlock}, {@code blockVacuumLaser}, {@code blockLens},
 * the controller and {@code 'O'} item-output / {@code 'P'} power-input
 * hatches.</p>
 *
 * <p>Built through the reflection-backed generic fixture probe
 * {@code /artest fixture multiblock orbital-laser-drill}.</p>
 *
 * <p>Position-isolated at x=8500.</p>
 */
public class OrbitalLaserDrillMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 8500;
    private static final int CY = 64;
    private static final int CZ = 8500;

    @Test
    public void orbitalLaserDrillMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        String fixture = join(client().execute(
                "artest fixture multiblock orbital-laser-drill 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock orbital-laser-drill failed: " + fixture,
                fixture.contains("\"ok\":true"));
        assertTrue("fixture didn't place any blocks: " + fixture,
                fixture.contains("\"placed\":") && !fixture.contains("\"placed\":0"));

        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileOrbitalLaserDrill tile at controller pos: " + info,
                info.contains("TileOrbitalLaserDrill"));

        String tryComplete = join(client().execute(
                "artest machine try-complete 0 " + CX + " " + CY + " " + CZ));
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("orbital-laser-drill multiblock didn't validate (isComplete=false): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void orbitalLaserDrillMultiblockInvalidatesWhenLensCellRemoved() throws Exception {
        int cx = CX + 40, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock orbital-laser-drill 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        // No baseline try-complete — break BEFORE validation. A lens cell on
        // the controller layer at structure[2][4][4] (NORTH-facing globalX =
        // cx + 5, globalY = cy, globalZ = cz + 2 relative to controller).
        // Exact offset coordinates depend on the controller offset 'c' at
        // structure[2][2][1]; the lens cell at structure[2][4][4] resolves to
        // global (cx - 3, cy, cz + 2).
        String breakLens = join(client().execute(
                "artest place 0 " + (cx - 3) + " " + cy + " " + (cz + 2) + " minecraft:stone"));
        assertTrue("could not replace lens cell: " + breakLens,
                breakLens.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("orbital-laser-drill validated despite missing lens cell: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
