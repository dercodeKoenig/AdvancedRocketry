package zmaster587.advancedRocketry.test.unit;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.armor.ItemSpaceArmor;
import zmaster587.advancedRocketry.enchant.EnchantmentSpaceBreathing;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 3.
 *
 * Contract for {@link EnchantmentSpaceBreathing}: cannot be reached via
 * the vanilla enchanting table (treasure-tier), only applies to armor,
 * single-level, never lands on books.
 */
public class SpaceBreathingEnchantmentContractTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    public void appliesToArmorItems() {
        Enchantment ench = new EnchantmentSpaceBreathing();
        // Any vanilla ItemArmor works; pick the simplest.
        ItemStack armor = new ItemStack(Items.LEATHER_HELMET, 1);
        assertTrue("space-breathing must be applicable to a vanilla ItemArmor",
                ench.canApply(armor));
    }

    @Test
    public void appliesToARSpaceArmor() {
        Enchantment ench = new EnchantmentSpaceBreathing();
        ItemSpaceArmor suit = new ItemSpaceArmor(
                ItemArmor.ArmorMaterial.LEATHER, EntityEquipmentSlot.CHEST, 4);
        ItemStack stack = new ItemStack(suit, 1);
        // Stronger guarantee: the entire purpose of the enchant is to make
        // existing space suits also bypass low-O2 damage. If this regressed
        // to false the enchantment would be silently inert on AR's own gear.
        assertTrue("space-breathing must apply to AR's own ItemSpaceArmor",
                ench.canApply(stack));
    }

    @Test
    public void rejectsNonArmorItems() {
        Enchantment ench = new EnchantmentSpaceBreathing();
        ItemStack notArmor = new ItemStack(Items.STICK, 1);
        assertFalse("space-breathing must not apply to non-armor items",
                ench.canApply(notArmor));
    }

    @Test
    public void rejectsEmptyStack() {
        Enchantment ench = new EnchantmentSpaceBreathing();
        assertFalse("canApply must return false for empty stack",
                ench.canApply(ItemStack.EMPTY));
    }

    @Test
    public void notReachableViaEnchantingTable() {
        // Treasure-tier: only obtainable via villager trading / loot, never
        // through random enchantment table rolls. Drop this guarantee and
        // every enchantable book will start surfacing it.
        Enchantment ench = new EnchantmentSpaceBreathing();
        ItemStack armor = new ItemStack(Items.LEATHER_HELMET, 1);
        assertFalse("space-breathing must NOT be available at the enchanting table",
                ench.canApplyAtEnchantingTable(armor));
    }

    @Test
    public void notAllowedOnBooks() {
        Enchantment ench = new EnchantmentSpaceBreathing();
        assertFalse("space-breathing must NOT land on books (would defeat treasure-tier intent)",
                ench.isAllowedOnBooks());
    }

    @Test
    public void singleLevelMax() {
        Enchantment ench = new EnchantmentSpaceBreathing();
        assertEquals("space-breathing is binary on/off — max level must stay 1",
                1, ench.getMaxLevel());
    }
}
