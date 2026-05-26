package zmaster587.advancedRocketry.test.unit;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.item.ItemPackedStructure;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * TASK-32 3a — {@link ItemPackedStructure} unit-tier contract pins.
 *
 * <p>{@code ItemPackedStructure} is the storage shell for assembler-built
 * structures (the {@code itemSpaceStation} item is an instance of this
 * class). The full setStructure → getStructure round-trip pin requires
 * a runtime profiler — {@link zmaster587.advancedRocketry.util.StorageChunk}'s
 * constructor reaches {@code FMLCommonHandler.getMinecraftServerInstance().profiler}
 * via {@link zmaster587.advancedRocketry.common.CommonProxy#getProfiler}.
 * That live in the testServer tier where a real server is up.</p>
 *
 * <p>At unit tier we pin the two contracts that DON'T require a
 * StorageChunk allocation:</p>
 *
 * <ul>
 *   <li><b>null-gate</b>: {@code getStructure} on a stack with no NBT
 *       compound returns {@code null}. Consumers
 *       ({@link zmaster587.advancedRocketry.tile.hatch.TileSatelliteHatch},
 *       {@link zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine})
 *       iterate player inventories and use this gate to skip blank
 *       items before reading content.</li>
 *   <li><b>subtype-flag</b>: the constructor sets
 *       {@code hasSubtypes=true} — required for the per-meta variant
 *       rendering used by {@code itemSpaceStation}'s station-type
 *       variants.</li>
 * </ul>
 *
 * <p>The capture path (player → assembler → ItemPackedStructure) is
 * out of scope per the TASK ticket — that's tested by the rocket /
 * station assembler suites.</p>
 */
public class ItemPackedStructureNbtRoundTripTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    /**
     * Pin: {@code getStructure} on a stack with no NBT compound returns
     * {@code null}. Consumers iterate inventories and use this null
     * gate to skip blank items before reading content. Any regression
     * that returned a default empty {@code StorageChunk} or threw would
     * break those scans.
     */
    @Test
    public void getStructureOnStackWithoutNbtReturnsNull() {
        ItemPackedStructure item = new ItemPackedStructure();
        ItemStack stack = new ItemStack(Items.STICK);
        assertFalse("test setup: fresh stack has no NBT compound",
                stack.hasTagCompound());
        assertNull("getStructure on stack with no NBT compound must return "
                        + "null — consumers iterate inventories and use this "
                        + "gate to skip blank items before reading content",
                item.getStructure(stack));
    }

    /**
     * Pin: {@code ItemPackedStructure} declares {@code hasSubtypes=true}.
     * Set in the constructor (line 13); needed for per-meta variant
     * rendering — {@code itemSpaceStation} ships multiple metas, each
     * one a different station-type, and the vanilla item-mesh system
     * relies on {@code hasSubtypes()} to dispatch the right model per
     * meta. A regression here would render every station the same.
     */
    @Test
    public void itemPackedStructureDeclaresHasSubtypes() {
        ItemPackedStructure item = new ItemPackedStructure();
        assertTrue("ItemPackedStructure must declare hasSubtypes=true — "
                        + "itemSpaceStation depends on per-meta variant "
                        + "rendering for its station-type display, and the "
                        + "vanilla item-mesh system gates on getHasSubtypes()",
                item.getHasSubtypes());
    }
}
