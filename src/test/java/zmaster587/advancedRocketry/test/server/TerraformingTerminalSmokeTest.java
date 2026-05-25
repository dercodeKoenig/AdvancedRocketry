package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * Coverage-audit gap (Tier 2 #10) — {@code TileTerraformingTerminal}
 * smoke contracts.
 *
 * <p>The terraforming terminal is the player-facing block that takes
 * a BiomeChanger satellite chip in its inventory slot 0 and (under
 * redstone power) drives biome mutation on the current dim. Existing
 * coverage:</p>
 *
 * <ul>
 *   <li>{@code TileMachineDepthTest} pins the block's registry name +
 *       tile-class identity.</li>
 *   <li>{@code SatelliteTypeBehaviourTest} pins the biome-changer
 *       satellite's tickEntity / queue behaviour.</li>
 * </ul>
 *
 * <p>The gap was the terminal's <b>intermediary role</b>: place the
 * block → wire to satellite + redstone → drive biome mutation.</p>
 *
 * <p>This test pins the SMOKE surface that doesn't require a real
 * BiomeChanger chip fixture:</p>
 *
 * <ul>
 *   <li>Block places + reports the right tile class.</li>
 *   <li>Tick without inventory contents (no chip) → no crash + tile
 *       remains queryable.</li>
 *   <li>Tick with redstone but no chip → still no crash (the
 *       {@code hasValidBiomeChanger} guard short-circuits the
 *       satellite-energy-drain branch).</li>
 * </ul>
 *
 * <p>The deeper "chip-in-slot + redstone + power → biome actually
 * changes" contract needs a BiomeChanger satellite chip with NBT-
 * embedded satellite-id pointing at a registered satellite with a
 * loaded battery. That fixture would duplicate
 * {@code SatelliteTypeBehaviourTest}'s setup; deferred to a follow-up
 * if a regression in the bridge-layer ever motivates the deeper pin.</p>
 */
public class TerraformingTerminalSmokeTest extends AbstractSharedServerTest {

    private static final int CY = 64;
    private static final int CZ = 9500;
    private static final int CX_BASIC    = 9500;
    private static final int CX_REDSTONE = 9700;

    @Test
    public void terminalPlacesAndTicksWithEmptyInventoryWithoutCrash() throws Exception {
        String place = exec("artest place 0 " + CX_BASIC + " " + CY + " " + CZ
                + " advancedrocketry:terraformingTerminal");
        assertTrue("terminal must place: " + place,
                place.contains("\"placed\":true"));

        String info = exec("artest machine info 0 " + CX_BASIC + " " + CY + " " + CZ);
        assertTrue("block must produce TileTerraformingTerminal: " + info,
                info.contains("TileTerraformingTerminal"));

        // 40 force-ticks — drives the natural update() loop through
        // hasValidBiomeChanger=false branch repeatedly. No NPE if the
        // null-chip guards in update():135 and 141 hold.
        String tick = exec("artest tile force-tick 0 " + CX_BASIC + " " + CY + " "
                + CZ + " 40");
        assertTrue("force-tick on empty terminal must succeed: " + tick,
                tick.contains("\"ok\":true"));

        String postInfo = exec("artest machine info 0 " + CX_BASIC + " " + CY + " "
                + CZ);
        assertTrue("tile must remain TileTerraformingTerminal after ticking: "
                        + postInfo,
                postInfo.contains("TileTerraformingTerminal"));
    }

    @Test
    public void terminalAdjacentToRedstoneTicksWithoutCrash() throws Exception {
        String place = exec("artest place 0 " + CX_REDSTONE + " " + CY + " " + CZ
                + " advancedrocketry:terraformingTerminal");
        assertTrue("terminal must place: " + place,
                place.contains("\"placed\":true"));

        // Place a redstone block adjacent so isBlockIndirectlyGettingPowered
        // returns true. This pushes the terminal into the
        // "hasValidBiomeChanger && has_redstone" branch — guard must fail
        // (no chip in slot 0) and not crash.
        exec("artest place 0 " + (CX_REDSTONE + 1) + " " + CY + " " + CZ
                + " minecraft:redstone_block");

        String tick = exec("artest tile force-tick 0 " + CX_REDSTONE + " " + CY + " "
                + CZ + " 40");
        assertTrue("force-tick on redstone-powered empty terminal must succeed: "
                        + tick,
                tick.contains("\"ok\":true"));

        String postInfo = exec("artest machine info 0 " + CX_REDSTONE + " " + CY + " "
                + CZ);
        assertTrue("tile must survive redstone-powered tick burst: " + postInfo,
                postInfo.contains("TileTerraformingTerminal"));
    }
}
