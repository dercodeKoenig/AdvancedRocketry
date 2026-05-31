package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-44 (audit Gap B) — Orbital Laser Drill MINING-mode dispatch.
 *
 * <p>Production: {@link
 * zmaster587.advancedRocketry.tile.multiblock.orbitallaserdrill.MiningDrill#performOperation}
 * breaks the opaque blocks in the laser's 3×3 column, collects their
 * {@code Block.getDrops} output, and clears them to air (then descends the
 * column). The drops feed the drill's output hatches → adjacent inventory.</p>
 *
 * <p>Pinned (player-visible): a MINING-mode drill fired over a solid ore
 * block removes that block and yields its drop. This is the mode-dispatch
 * contract — MINING produces resource drops, distinct from the terraforming
 * mode (which replaces blocks). Driven via the {@code /artest infra
 * laserdrill-mine} probe, which exercises the real {@code MiningDrill
 * .performOperation} with a laser node positioned on the target block,
 * bypassing only the multiblock-assembly + energy + spiral-walk fixture
 * (none of which is the contract under test here).</p>
 *
 * <p>Band/end-state pins only: "&gt;0 drops produced" + "target removed" +
 * "drop item matches the mined block" — NOT an exact drop count or the
 * spiral order (impl details).</p>
 */
public class OrbitalLaserDrillModeDispatchTest extends AbstractSharedServerTest {

    // High above terrain so the descend-loop walks air and the only mined
    // block is the one we place.
    private static final int X = 6400;
    private static final int Y = 150;
    private static final int Z = 6400;

    @Test
    public void miningModeBreaksTargetBlockAndYieldsItsDrop() throws Exception {
        String resp = exec("artest infra laserdrill-mine 0 "
                + X + " " + Y + " " + Z + " minecraft:iron_ore");

        assertTrue("probe must succeed: " + resp, resp.contains("\"ok\":true"));

        // The mined iron_ore block drops itself in 1.12 (BlockOre for
        // iron/gold drops the block item, not an ingot). Contract: the drill
        // produced the block's drop.
        assertTrue("mining drill must yield the mined block's drop "
                        + "(player-visible: drill produces resources from the "
                        + "column); resp=" + resp,
                resp.contains("minecraft:iron_ore"));

        // Contract: the target block was removed from the world.
        assertTrue("mining drill must remove the target block (set to air); "
                        + "resp=" + resp,
                resp.contains("\"centerRemoved\":true"));

        // Band-pin: strictly more than zero items produced.
        assertTrue("drop count must be > 0; resp=" + resp,
                !resp.contains("\"dropCount\":0"));
    }

    private String exec(String cmd) throws Exception {
        return String.join("\n", client().execute(cmd));
    }
}
