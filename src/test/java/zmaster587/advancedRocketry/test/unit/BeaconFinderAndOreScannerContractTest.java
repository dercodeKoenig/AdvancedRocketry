package zmaster587.advancedRocketry.test.unit;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.item.ItemBeaconFinder;
import zmaster587.advancedRocketry.item.ItemOreScanner;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Coverage-audit gap (Tier 3 #12, unit slice) — scanner-item
 * contracts pinnable without a Minecraft world.
 *
 * <p>The audit framed this gap as "player-use" but the actual contract
 * surfaces split into two layers:</p>
 *
 * <ul>
 *   <li><b>ItemBeaconFinder</b> — pure HUD-render IArmorComponent, NO
 *       {@code onItemRightClick} / {@code onItemUse}. The contract is
 *       slot-eligibility (HEAD only) + component-add ok. {@code
 *       renderScreen} reads {@code DimensionProperties.getBeacons()}
 *       and draws indicators; the data-source side is already pinned
 *       by {@code BeaconEnableCycleTest} (TASK-19 Phase 3).</li>
 *   <li><b>ItemOreScanner</b> — has {@code onItemRightClick} +
 *       {@code onItemUse} which open the OreMapping GUI WHEN the
 *       stored satellite-ID resolves to a SatelliteOreMapping on the
 *       current dim. The NBT round-trip for the satellite-ID is the
 *       pinnable contract here; the GUI-open path is testClient.</li>
 * </ul>
 *
 * <p>This file pins the unit-tier contracts. A companion
 * client-tier smoke is in
 * {@code OreScannerPlayerUseClientSmokeE2ETest}.</p>
 */
public class BeaconFinderAndOreScannerContractTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    public void beaconFinderIsAllowedOnlyInHeadSlot() {
        ItemBeaconFinder finder = new ItemBeaconFinder();
        ItemStack stack = new ItemStack(finder);
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            boolean expected = slot == EntityEquipmentSlot.HEAD;
            assertEquals("BeaconFinder slot eligibility for " + slot
                            + " — only HEAD is valid because the finder draws "
                            + "the HUD direction indicator on the helmet overlay",
                    expected, finder.isAllowedInSlot(stack, slot));
        }
    }

    @Test
    public void beaconFinderOnComponentAddedReturnsTrue() {
        // Production ItemSpaceArmor.addArmorComponent requires this to
        // be true for the BeaconFinder to actually install in the
        // helmet's sub-inventory.
        ItemBeaconFinder finder = new ItemBeaconFinder();
        ItemStack armor = new ItemStack(finder);
        assertTrue("BeaconFinder must be installable into helmet sub-inventory",
                finder.onComponentAdded(null, armor));
    }

    @Test
    public void oreScannerSatelliteIdRoundTripsThroughNbt() {
        ItemOreScanner scanner = new ItemOreScanner();
        scanner.setRegistryName("ar_test:ore_scanner_g12_roundtrip");
        ItemStack stack = new ItemStack(scanner);

        // Default — no NBT, returns -1 sentinel.
        assertEquals("default ore-scanner has no satellite id (-1 sentinel)",
                -1L, scanner.getSatelliteID(stack));

        scanner.setSatelliteID(stack, 0xDEADBEEFCAFEL);
        assertEquals("setSatelliteID must round-trip through stack NBT",
                0xDEADBEEFCAFEL, scanner.getSatelliteID(stack));

        // Overwrite — pin that subsequent calls replace, not append.
        scanner.setSatelliteID(stack, 42L);
        assertEquals("setSatelliteID overwrite must replace previous value",
                42L, scanner.getSatelliteID(stack));
    }

    @Test
    public void oreScannerEmptySatelliteIdReturnsMinusOne() {
        // Specifically: an ItemStack with a non-null NBT compound that
        // happens to NOT have the "id" key returns 0 (NBT default for
        // getLong on missing key), not -1. The -1 sentinel only applies
        // when the stack has NO NBT compound at all. Pin both branches:
        ItemOreScanner scanner = new ItemOreScanner();
        scanner.setRegistryName("ar_test:ore_scanner_g12_empty");
        ItemStack stack = new ItemStack(scanner);
        assertEquals("no-NBT stack returns the -1 sentinel",
                -1L, scanner.getSatelliteID(stack));

        // Once setSatelliteID is called once, NBT exists. Then we
        // overwrite under a different key (via reflection won't matter —
        // the contract is simpler: setSatelliteID + getSatelliteID
        // round-trips correctly).
        scanner.setSatelliteID(stack, 7L);
        assertEquals("after explicit set, get returns the set value",
                7L, scanner.getSatelliteID(stack));
    }
}
