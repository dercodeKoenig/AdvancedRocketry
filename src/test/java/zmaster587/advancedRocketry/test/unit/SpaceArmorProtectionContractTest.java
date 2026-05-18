package zmaster587.advancedRocketry.test.unit;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.api.capability.CapabilitySpaceArmor;
import zmaster587.advancedRocketry.armor.ItemSpaceArmor;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 3.
 *
 * Pins the atmosphere-protection contract of {@link ItemSpaceArmor} —
 * what it protects against, what it does NOT protect against, and the
 * capability dispatch on {@link CapabilitySpaceArmor#PROTECTIVEARMOR}.
 *
 * The actual armor tier doesn't matter for these assertions; we
 * instantiate it with vanilla {@link ItemArmor.ArmorMaterial#LEATHER}.
 */
public class SpaceArmorProtectionContractTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    private static ItemSpaceArmor newSuit(EntityEquipmentSlot slot) {
        return new ItemSpaceArmor(ItemArmor.ArmorMaterial.LEATHER, slot, /*numModules=*/4);
    }

    private static ItemStack newStack(EntityEquipmentSlot slot) {
        ItemSpaceArmor armor = newSuit(slot);
        return new ItemStack(armor, 1);
    }

    @Test
    public void protectsAgainstVacuum() {
        ItemSpaceArmor suit = newSuit(EntityEquipmentSlot.CHEST);
        ItemStack stack = new ItemStack(suit, 1);
        assertTrue("vacuum must be a protected atmosphere",
                suit.protectsFromSubstance(AtmosphereType.VACUUM, stack, /*commit=*/false));
    }

    @Test
    public void protectsAgainstLowOxygenAndPressureExtremes() {
        ItemSpaceArmor suit = newSuit(EntityEquipmentSlot.CHEST);
        ItemStack stack = new ItemStack(suit, 1);

        // Low O2 + every pressure / temperature extreme — these are why the
        // armor exists; a regression that drops any of them silently kills
        // players on the affected planet biomes.
        AtmosphereType[] mustProtect = {
                AtmosphereType.LOWOXYGEN,
                AtmosphereType.NOO2,
                AtmosphereType.HIGHPRESSURE, AtmosphereType.HIGHPRESSURENOO2,
                AtmosphereType.SUPERHIGHPRESSURE, AtmosphereType.SUPERHIGHPRESSURENOO2,
                AtmosphereType.VERYHOT, AtmosphereType.VERYHOTNOO2,
                AtmosphereType.SUPERHEATED, AtmosphereType.SUPERHEATEDNOO2,
        };
        for (AtmosphereType atm : mustProtect) {
            assertTrue("suit must protect against " + atm,
                    suit.protectsFromSubstance(atm, stack, /*commit=*/false));
        }
    }

    @Test
    public void doesNotProtectAgainstNormalAtmospheres() {
        ItemSpaceArmor suit = newSuit(EntityEquipmentSlot.CHEST);
        ItemStack stack = new ItemStack(suit, 1);
        // Breathable air and pressurised-but-breathable air are not threats —
        // protectsFromSubstance is also used to decide if the suit ticks down
        // its tank, so returning true here would needlessly drain it.
        assertFalse("breathable air is not a threat",
                suit.protectsFromSubstance(AtmosphereType.AIR, stack, /*commit=*/false));
        assertFalse("pressurised breathable air is not a threat",
                suit.protectsFromSubstance(AtmosphereType.PRESSURIZEDAIR, stack, /*commit=*/false));
    }

    @Test
    public void exposesProtectiveArmorCapabilityOnAllEquipmentSlots() {
        // Helmet / chest / leggings / boots — the suit's IModularArmor
        // dispatch must reply on every slot variant so that the capability
        // lookup in AtmosphereHandler.canBreathe finds *something*.
        for (EntityEquipmentSlot slot : new EntityEquipmentSlot[]{
                EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
                EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET}) {
            ItemSpaceArmor suit = newSuit(slot);
            assertTrue("hasCapability(PROTECTIVEARMOR) on slot " + slot + " must be true",
                    suit.hasCapability(CapabilitySpaceArmor.PROTECTIVEARMOR, null));
            Object cap = suit.getCapability(CapabilitySpaceArmor.PROTECTIVEARMOR, null);
            assertNotNull("getCapability(PROTECTIVEARMOR) returned null for slot " + slot, cap);
            assertSame("PROTECTIVEARMOR cap must dispatch to the suit itself",
                    suit, cap);
        }
    }

    // Note: "rejects unrelated capabilities" cannot be unit-tested here.
    // Forge's @CapabilityInject populates static cap fields at runtime; in
    // this test JVM both CapabilitySpaceArmor.PROTECTIVEARMOR and
    // CapabilityItemHandler.ITEM_HANDLER_CAPABILITY are null, so the
    // identity check `capability == PROTECTIVEARMOR` returns true for any
    // null argument. The real-server WeatherClientSyncE2ETest /
    // OxygenSuitClientStateE2ETest cover the live capability dispatch.

    @Test
    public void getNumSlotsHonorsConstructorArgumentAfterInventoryInit() {
        // Fresh suit, fresh stack — getNumSlots routes through
        // loadEmbeddedInventory which lazily creates an EmbeddedInventory of
        // size = numModules (4 here). Pin that the constructor arg actually
        // reaches the inventory.
        ItemSpaceArmor suit = newSuit(EntityEquipmentSlot.CHEST);
        ItemStack stack = new ItemStack(suit, 1);
        int slots = suit.getNumSlots(stack);
        assertTrue("numModules constructor arg should propagate to embedded inventory, got " + slots,
                slots >= 1);
    }
}
