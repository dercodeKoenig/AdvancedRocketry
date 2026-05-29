package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-40d (audit Gap L) — TileForceFieldProjector projects + retracts
 * blockForceField blocks along its facing direction.
 *
 * <p>Production:
 * {@link zmaster587.advancedRocketry.tile.TileForceFieldProjector#onIntermittentUpdate}
 * — when {@code world.isBlockPowered(getPos())} fires, each call extends
 * the field by one cell along the projector's facing, placing
 * {@code blockForceField}; when un-powered, each call retracts by one
 * cell, reverting to air. Player-visible: the force field appears/disappears
 * in front of the projector when redstone is toggled.</p>
 *
 * <p>Pinned (loose-bound):</p>
 * <ul>
 *   <li>Powered + 1 tick → block in front of projector is
 *       {@code advancedrocketry:forcefield} (replaces previous air).</li>
 *   <li>After un-power + 1 retract tick → same cell reverts to
 *       {@code minecraft:air}.</li>
 * </ul>
 *
 * <p>NOT pinned (impl per SOP): the exact range constant
 * ({@code MAX_RANGE = 32}); the {@code worldTime % 5 == 0} natural-tick
 * gate (probe bypasses it via direct call to the public
 * {@code onIntermittentUpdate}).</p>
 */
public class ForceFieldProjectorProjectsAndRetractsTest extends AbstractSharedServerTest {

    private static final int PX = 6500;
    private static final int PY = 65;
    private static final int PZ = 6500;

    @Test
    public void poweredProjectorPlacesForceFieldThenRetractsOnUnpower() throws Exception {
        int x = PX, y = PY, z = PZ;
        // Pre-clear the projection target cell so the field placement
        // condition (isReplaceable) is satisfied.
        ok("artest place 0 " + x + " " + y + " " + (z - 1) + " minecraft:air");

        // Place projector at (x, y, z) facing NORTH (meta 2 → EnumFacing.NORTH
        // per BlockFullyRotatable.getStateFromMeta).
        ok("artest place 0 " + x + " " + y + " " + z
                + " advancedrocketry:forcefieldProjector 2");

        // Place a redstone block adjacent (east face) — that's a strong
        // power source, so world.isBlockPowered(projectorPos) returns true.
        ok("artest place 0 " + (x + 1) + " " + y + " " + z
                + " minecraft:redstone_block");

        // Drive one extension cycle via the probe (bypasses the
        // worldTime % 5 == 0 natural-tick gate).
        ok("artest infra forcefield-tick 0 " + x + " " + y + " " + z + " 1");

        // The projector facing NORTH places at pos.offset(NORTH, 1) =
        // (x, y, z-1). Block must now be the AR force-field.
        String poweredCell = exec("artest block at 0 "
                + x + " " + y + " " + (z - 1));
        assertTrue("force field must appear at one cell in front of "
                        + "projector when powered + ticked; cell="
                        + poweredCell,
                poweredCell.contains("\"block\":\"advancedrocketry:forcefield\""));

        // Un-power by replacing the redstone block with air.
        ok("artest place 0 " + (x + 1) + " " + y + " " + z + " minecraft:air");

        // Drive retraction ticks. After an extension cycle, internal
        // extensionRange = 2 (incremented after placing at distance 1).
        // The first retraction tick checks distance 3 then 2 (both air)
        // and only decrements extensionRange to 1. The second tick
        // checks distance 2 (air) then 1 (the placed field) and clears
        // it. Use 3 ticks for safety margin.
        ok("artest infra forcefield-tick 0 " + x + " " + y + " " + z + " 3");

        String unpoweredCell = exec("artest block at 0 "
                + x + " " + y + " " + (z - 1));
        assertTrue("force field must retract (back to air) when "
                        + "projector unpowers + ticks; cell="
                        + unpoweredCell,
                unpoweredCell.contains("\"isAir\":true"));
    }

    private String exec(String cmd) throws Exception {
        return String.join("\n", client().execute(cmd));
    }

    private void ok(String cmd) throws Exception {
        String resp = exec(cmd);
        assertTrue("probe must succeed: cmd='" + cmd + "' resp=" + resp,
                resp.contains("\"ok\":true"));
    }
}
