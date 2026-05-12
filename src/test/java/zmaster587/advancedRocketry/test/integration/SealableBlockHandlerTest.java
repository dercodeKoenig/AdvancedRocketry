package zmaster587.advancedRocketry.test.integration;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;
import zmaster587.advancedRocketry.util.SealableBlockHandler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * §6.8 SealableBlockHandler — pure list-management logic.
 *
 * The world-level seal check ({@code isBlockSealed}) needs a real World and is
 * exercised in scenario tests under §7.13. Here we only cover allow/ban list
 * mutation and {@code loadDefaultData}.
 *
 * <p>NOTE: SealableBlockHandler.INSTANCE is process-wide singleton state shared
 * with production code. We snapshot the lists in @BeforeClass so test order does
 * not contaminate other tests in the same JVM.</p>
 */
public class SealableBlockHandlerTest {

    private static java.util.List<Block> snapshotBlocks;
    private static java.util.List<Block> snapshotAllow;
    private static java.util.List<Material> snapshotMaterialBan;

    @BeforeClass
    public static void bootstrap() throws Exception {
        MinecraftBootstrap.ensure();
        // Capture singleton state before mutation so we can restore it afterwards.
        snapshotBlocks = new java.util.ArrayList<>(reflect("blockBanList"));
        snapshotAllow = new java.util.ArrayList<>(reflect("blockAllowList"));
        snapshotMaterialBan = new java.util.ArrayList<>(reflectMaterials("materialBanList"));
    }

    @AfterClass
    public static void restore() throws Exception {
        java.util.List<Block> banList = reflect("blockBanList");
        banList.clear();
        banList.addAll(snapshotBlocks);
        java.util.List<Block> allowList = reflect("blockAllowList");
        allowList.clear();
        allowList.addAll(snapshotAllow);
        java.util.List<Material> matBan = reflectMaterials("materialBanList");
        matBan.clear();
        matBan.addAll(snapshotMaterialBan);
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<Block> reflect(String fieldName) throws Exception {
        java.lang.reflect.Field f = SealableBlockHandler.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (java.util.List<Block>) f.get(SealableBlockHandler.INSTANCE);
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<Material> reflectMaterials(String fieldName) throws Exception {
        java.lang.reflect.Field f = SealableBlockHandler.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (java.util.List<Material>) f.get(SealableBlockHandler.INSTANCE);
    }

    @Test
    public void defaultSealableBlocksLoaded() throws Exception {
        // Reset and load defaults.
        reflectMaterials("materialBanList").clear();

        SealableBlockHandler.INSTANCE.loadDefaultData();

        // The set explicitly enumerated in loadDefaultData() must end up on the ban list.
        assertTrue(SealableBlockHandler.INSTANCE.isMaterialBanned(Material.AIR));
        assertTrue(SealableBlockHandler.INSTANCE.isMaterialBanned(Material.FIRE));
        assertTrue(SealableBlockHandler.INSTANCE.isMaterialBanned(Material.LEAVES));
        assertTrue(SealableBlockHandler.INSTANCE.isMaterialBanned(Material.WEB));
        assertTrue(SealableBlockHandler.INSTANCE.isMaterialBanned(Material.PLANTS));
        assertTrue(SealableBlockHandler.INSTANCE.isMaterialBanned(Material.CACTUS));
        assertTrue(SealableBlockHandler.INSTANCE.isMaterialBanned(Material.PORTAL));
        assertTrue(SealableBlockHandler.INSTANCE.isMaterialBanned(Material.VINE));
        assertTrue(SealableBlockHandler.INSTANCE.isMaterialBanned(Material.SPONGE));
        assertTrue(SealableBlockHandler.INSTANCE.isMaterialBanned(Material.SAND));
        // Stone is sealable, must NOT be banned by default.
        assertFalse(SealableBlockHandler.INSTANCE.isMaterialBanned(Material.ROCK));
    }

    @Test
    public void whitelistOverridesDetection() throws Exception {
        Block target = Blocks.LEAVES;

        // First put it on the ban list…
        SealableBlockHandler.INSTANCE.addUnsealableBlock(target);
        assertTrue(SealableBlockHandler.INSTANCE.isBlockBanned(target));

        // …then overriding via addSealableBlock must remove it from the ban list.
        SealableBlockHandler.INSTANCE.addSealableBlock(target);
        assertFalse("addSealableBlock must remove the block from the ban list",
                SealableBlockHandler.INSTANCE.isBlockBanned(target));
        assertTrue("addSealableBlock must put the block onto the allow list",
                SealableBlockHandler.INSTANCE.getOverriddenSealableBlocks().contains(target));
    }

    @Test
    public void blacklistOverridesDetection() throws Exception {
        Block target = Blocks.STONE;

        SealableBlockHandler.INSTANCE.addSealableBlock(target);
        assertTrue(SealableBlockHandler.INSTANCE.getOverriddenSealableBlocks().contains(target));

        SealableBlockHandler.INSTANCE.addUnsealableBlock(target);
        assertTrue(SealableBlockHandler.INSTANCE.isBlockBanned(target));
        assertFalse("addUnsealableBlock must remove the block from the allow list",
                SealableBlockHandler.INSTANCE.getOverriddenSealableBlocks().contains(target));
    }

    @Test
    public void addingSameBlockTwiceDoesNotDuplicate() throws Exception {
        Block target = Blocks.GRAVEL;

        SealableBlockHandler.INSTANCE.addSealableBlock(target);
        SealableBlockHandler.INSTANCE.addSealableBlock(target);

        // Allow list contains the block exactly once.
        long count = SealableBlockHandler.INSTANCE.getOverriddenSealableBlocks().stream()
                .filter(b -> b == target)
                .count();
        // assertEquals(long, long) is unambiguous, so explicit cast.
        org.junit.Assert.assertEquals(1L, count);
    }
}
