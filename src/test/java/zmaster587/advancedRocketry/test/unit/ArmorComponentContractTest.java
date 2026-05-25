package zmaster587.advancedRocketry.test.unit;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.item.components.ItemJetpack;
import zmaster587.advancedRocketry.item.components.ItemPressureTank;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Coverage-audit gap (Tier 3 #11) — unit-tier contract for the
 * {@code IArmorComponent} surface that ItemJetpack and ItemPressureTank
 * implement.
 *
 * <p>These two component items live in {@code ItemSpaceChest}'s embedded
 * sub-inventory (slots 0-1: pressure tank, slot 2+: misc upgrades).
 * Pinning their contract at unit tier guards against:</p>
 *
 * <ul>
 *   <li><b>Slot-eligibility regression</b> — both must return true ONLY
 *       for {@code EntityEquipmentSlot.CHEST}. A regression that broadens
 *       this to ARMOR slots silently makes the components placeable in
 *       helmet/legs/boots → undefined behaviour.</li>
 *   <li><b>onComponentAdded contract</b> — must return true so
 *       {@code ItemSpaceArmor.addArmorComponent} actually inserts.
 *       A regression to false silently makes the component non-installable.</li>
 *   <li><b>ItemPressureTank capacity scaling</b> — capacity is
 *       {@code baseCapacity * 2^itemDamage} (tier 0/1/2 = 1×/2×/4×).
 *       Player-visible: a tier-2 tank holds 4× the oxygen of tier-0.</li>
 *   <li><b>ItemJetpack enabled-state NBT round-trip</b> — toggle persists
 *       across stack copy (which Minecraft does for IO + GUI).</li>
 * </ul>
 *
 * <p>Each test instantiates the item class directly — no Bootstrap or
 * mod-init required. Tracks the {@code IArmorComponent} interface as
 * fully as is meaningful at unit tier (the {@code onTick} side-effect
 * code path needs a real player and is left to player-tier tests).</p>
 */
public class ArmorComponentContractTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    public void jetpackIsAllowedOnlyInChestSlot() {
        ItemJetpack jetpack = new ItemJetpack();
        ItemStack stack = new ItemStack(jetpack);
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            boolean expected = slot == EntityEquipmentSlot.CHEST;
            assertEquals("jetpack slot eligibility for " + slot
                            + " — only CHEST is valid",
                    expected, jetpack.isAllowedInSlot(stack, slot));
        }
    }

    @Test
    public void pressureTankIsAllowedOnlyInChestSlot() {
        ItemPressureTank tank = new ItemPressureTank(1, 8000);
        ItemStack stack = new ItemStack(tank);
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            boolean expected = slot == EntityEquipmentSlot.CHEST;
            assertEquals("pressure-tank slot eligibility for " + slot
                            + " — only CHEST is valid",
                    expected, tank.isAllowedInSlot(stack, slot));
        }
    }

    @Test
    public void jetpackOnComponentAddedReturnsTrue() {
        // Production ItemSpaceArmor.addArmorComponent only inserts when
        // onComponentAdded returns true. A regression to false would
        // silently make jetpacks un-installable via the Suit Workstation.
        ItemJetpack jetpack = new ItemJetpack();
        ItemStack armor = new ItemStack(jetpack);  // any stack — unused by jetpack's impl
        assertTrue("ItemJetpack.onComponentAdded must return true so the "
                        + "chest sub-inventory accepts it",
                jetpack.onComponentAdded(null, armor));
    }

    @Test
    public void pressureTankOnComponentAddedReturnsTrue() {
        ItemPressureTank tank = new ItemPressureTank(1, 8000);
        ItemStack armor = new ItemStack(tank);
        assertTrue("ItemPressureTank.onComponentAdded must return true",
                tank.onComponentAdded(null, armor));
    }

    @Test
    public void pressureTankCapacityScalesAsPowerOfTwoWithItemDamage() {
        // capacity formula: baseCapacity * 2^itemDamage
        // — see ItemPressureTank.getCapacity(stack):75-77
        ItemPressureTank tank = new ItemPressureTank(1, 8000);

        ItemStack tier0 = new ItemStack(tank, 1, 0);
        ItemStack tier1 = new ItemStack(tank, 1, 1);
        ItemStack tier2 = new ItemStack(tank, 1, 2);

        assertEquals("tier 0 tank capacity = base", 8000, tank.getCapacity(tier0));
        assertEquals("tier 1 tank capacity = 2× base", 16000, tank.getCapacity(tier1));
        assertEquals("tier 2 tank capacity = 4× base", 32000, tank.getCapacity(tier2));
    }

    @Test
    public void jetpackEnabledStateToggleStoresAndClearsNbtFlag() {
        // Production toggle stored under NBT key "enabled" via
        // setEnabledState(stack, boolean). Pin the round-trip at the
        // tag level: setEnabledState writes the key, isEnabled reads it.
        // Full ItemStack envelope round-trip (with item registry id)
        // requires a registered Item, which unit-tier doesn't reach;
        // the contract being tested is "the flag persists across stack
        // mutations" — covered by reading back through isEnabled.
        ItemJetpack jetpack = new ItemJetpack();
        ItemStack stack = new ItemStack(jetpack);
        assertFalse("default jetpack must be disabled (no NBT)",
                jetpack.isEnabled(stack));

        jetpack.setEnabledState(stack, true);
        assertTrue("after setEnabledState(true), isEnabled must be true",
                jetpack.isEnabled(stack));
        // Direct NBT key inspection — proves the flag is in the stack's
        // own NBT (not transient state on the Item).
        NBTTagCompound tag = stack.getTagCompound();
        assertTrue("setEnabledState(true) must write the 'enabled' NBT key: " + tag,
                tag != null && tag.getBoolean("enabled"));

        jetpack.setEnabledState(stack, false);
        assertFalse("setEnabledState(false) must clear the enabled flag",
                jetpack.isEnabled(stack));
    }

    @Test
    public void jetpackOnArmorDamagedIsNoOp() {
        // Production wires component-tick → onArmorDamaged broadcasts to
        // every component. The jetpack's no-op contract is intentional —
        // a regression that adds damage-amount logic would (a) crash on
        // null-checks or (b) silently consume jetpack durability that
        // players don't expect.
        ItemJetpack jetpack = new ItemJetpack();
        ItemStack armor = new ItemStack(jetpack);
        ItemStack component = new ItemStack(jetpack);
        // Should not throw — null entity/source is the easiest no-op proof.
        jetpack.onArmorDamaged(null, armor, component, null, 99);
    }
}
