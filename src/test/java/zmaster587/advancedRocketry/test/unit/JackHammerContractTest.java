package zmaster587.advancedRocketry.test.unit;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.item.ItemJackHammer;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-05 Phase 4 — ItemJackHammer pure-function contracts.
 *
 * <p>The jackhammer is the player's pickaxe-tier mining tool for rock,
 * iron and ore materials. Pins:</p>
 *
 * <ul>
 *   <li>{@link ItemJackHammer#getDestroySpeed} returns the elevated
 *       speed for {@link net.minecraft.block.material.Material#ROCK} and
 *       {@link net.minecraft.block.material.Material#IRON} blocks; falls
 *       through to vanilla {@link net.minecraft.item.ItemTool} speed for
 *       irrelevant materials.</li>
 *   <li>{@link ItemJackHammer#canHarvestBlock} returns {@code true}
 *       unconditionally — the production contract is "the jackhammer
 *       drops resources for every block it can hit", with the
 *       speed-vs-tier check handled by getDestroySpeed.</li>
 * </ul>
 *
 * <p>The {@code onBlockStartBreak} / break-event path requires an
 * {@link net.minecraft.entity.player.EntityPlayer}; that surface is
 * deferred to the testClient e2e harness (TASK-10b) per the TASK-05
 * plan §"Technical Decisions".</p>
 */
public class JackHammerContractTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    private static ItemJackHammer hammer() {
        // The toolMaterial choice only affects super's harvestLevel /
        // attack damage; getDestroySpeed and canHarvestBlock don't read
        // it. Use vanilla IRON for a stable baseline.
        return new ItemJackHammer(Item.ToolMaterial.IRON);
    }

    private static IBlockState stateOf(Block block) {
        return block.getDefaultState();
    }

    // ───────────────────── getDestroySpeed: elevated cases ───────────────

    @Test
    public void destroySpeedIsElevatedForRockMaterial() {
        ItemJackHammer h = hammer();
        ItemStack stack = new ItemStack(h, 1);
        float speed = h.getDestroySpeed(stack, stateOf(Blocks.STONE));
        // Pin the contract shape: "rock material → significantly faster
        // than vanilla pickaxe baseline". Vanilla iron pickaxe on stone
        // returns 6.0f; the jackhammer must noticeably exceed that.
        assertTrue("jackhammer must mine ROCK noticeably faster than "
                + "vanilla iron pick (vanilla=6.0f); got " + speed,
                speed > 10.0f);
    }

    @Test
    public void destroySpeedIsElevatedForIronMaterial() {
        ItemJackHammer h = hammer();
        ItemStack stack = new ItemStack(h, 1);
        float speed = h.getDestroySpeed(stack, stateOf(Blocks.IRON_BLOCK));
        assertTrue("jackhammer must mine IRON material noticeably faster "
                + "than vanilla iron pick (vanilla=6.0f); got " + speed,
                speed > 10.0f);
    }

    @Test
    public void destroySpeedElevatedForVariousRockBlocks() {
        // Pin the matrix across several ROCK-material blocks in the
        // production "effective on" set. All must report the same
        // elevated speed because getDestroySpeed gates only on the
        // material, not the specific block.
        ItemJackHammer h = hammer();
        ItemStack stack = new ItemStack(h, 1);
        Block[] rockBlocks = {
                Blocks.COBBLESTONE, Blocks.STONE, Blocks.SANDSTONE,
                Blocks.MOSSY_COBBLESTONE, Blocks.NETHERRACK,
                Blocks.IRON_ORE, Blocks.COAL_ORE, Blocks.DIAMOND_ORE,
                Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE,
                Blocks.DIAMOND_BLOCK, Blocks.GOLD_BLOCK, Blocks.LAPIS_BLOCK
        };
        float reference = h.getDestroySpeed(stack, stateOf(Blocks.STONE));
        for (Block b : rockBlocks) {
            float s = h.getDestroySpeed(stack, stateOf(b));
            assertEquals("jackhammer destroy speed must be uniform across "
                            + "ROCK-material blocks; mismatch on " + b.getRegistryName(),
                    reference, s, 0.0001f);
        }
    }

    // ───────────────────── getDestroySpeed: fall-through cases ───────────

    @Test
    public void destroySpeedFallsThroughForWoodMaterial() {
        // WOOD material is not in the (IRON || ROCK || GEODE) gate, so
        // getDestroySpeed must fall through to super.getDestroySpeed,
        // which returns 1.0f for non-effective materials on a pickaxe-
        // style ItemTool. Contract: "jackhammer is NOT a wood-mining tool".
        ItemJackHammer h = hammer();
        ItemStack stack = new ItemStack(h, 1);
        float speed = h.getDestroySpeed(stack, stateOf(Blocks.LOG));
        float rockSpeed = h.getDestroySpeed(stack, stateOf(Blocks.STONE));
        assertTrue("jackhammer must NOT be elevated on WOOD material; "
                + "got wood=" + speed + " vs rock=" + rockSpeed,
                speed < rockSpeed);
    }

    @Test
    public void destroySpeedFallsThroughForDirtMaterial() {
        ItemJackHammer h = hammer();
        ItemStack stack = new ItemStack(h, 1);
        float dirtSpeed = h.getDestroySpeed(stack, stateOf(Blocks.DIRT));
        float rockSpeed = h.getDestroySpeed(stack, stateOf(Blocks.STONE));
        assertTrue("jackhammer must NOT be elevated on GROUND/dirt; "
                + "got dirt=" + dirtSpeed + " vs rock=" + rockSpeed,
                dirtSpeed < rockSpeed);
    }

    // ───────────────────── canHarvestBlock ───────────────────────────────

    @Test
    public void canHarvestBlockIsTrueForEveryBlockState() {
        // Production contract: canHarvestBlock returns true for every
        // input. Vanilla ItemTool's default would gate on tool tier /
        // harvest level; the jackhammer bypasses that, letting drops
        // appear even for blocks normally above its tier. Sampling
        // across rock / iron / wood / dirt / gold / obsidian validates
        // the unconditional behaviour.
        ItemJackHammer h = hammer();
        Block[] sample = {
                Blocks.STONE, Blocks.IRON_BLOCK, Blocks.LOG,
                Blocks.DIRT, Blocks.GOLD_BLOCK, Blocks.OBSIDIAN,
                Blocks.BEDROCK, Blocks.AIR, Blocks.GLASS
        };
        for (Block b : sample) {
            assertTrue("canHarvestBlock must return true for "
                    + b.getRegistryName(),
                    h.canHarvestBlock(stateOf(b)));
        }
    }
}
