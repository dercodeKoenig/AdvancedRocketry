package zmaster587.advancedRocketry.test.integration;

import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.item.ItemPackedStructure;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertNull;

/**
 * Coverage-audit gap (Tier 3 #15) — {@code ItemPackedStructure} null-state
 * contract.
 *
 * <p>The audit framed this gap as "deploy contract" but the class is
 * actually a serialization wrapper for {@link
 * zmaster587.advancedRocketry.util.StorageChunk} with two methods —
 * {@code setStructure} and {@code getStructure}. There's no deploy
 * logic on the item; the StorageChunk-to-world flow happens in
 * downstream consumers (station-deploy events, rocket assembly).</p>
 *
 * <p>What we CAN unit/integration-pin: {@code getStructure} returns
 * null when no NBT is present. This is the load-bearing sentinel
 * downstream "is this packed?" checks rely on. A regression that
 * returns an empty-but-not-null StorageChunk would cause every
 * caller to think the stack contains a valid (but empty) chunk.</p>
 *
 * <p>The {@code setStructure}/{@code getStructure} round-trip cannot
 * be pinned at integration tier — {@code StorageChunk}'s constructor
 * eagerly calls {@code AdvancedRocketry.proxy.getProfiler()} which
 * NPEs without a running {@code MinecraftServer}. A future
 * server-tier probe-driven test could close that gap by going
 * through the existing rocket fixture → assemble → pack flow, but
 * it would duplicate {@code RocketAssemblySmokeTest}'s coverage.</p>
 */
public class ItemPackedStructureNbtTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    public void emptyStackHasNullStructure() {
        ItemPackedStructure item = new ItemPackedStructure();
        item.setRegistryName("ar_test:packed_structure_g15_emptyStack");
        ItemStack stack = new ItemStack(item);
        assertNull("freshly-created PackedStructure stack must report a "
                        + "null StorageChunk — the null sentinel is what "
                        + "downstream code uses to detect 'not yet packed'",
                item.getStructure(stack));
    }
}
