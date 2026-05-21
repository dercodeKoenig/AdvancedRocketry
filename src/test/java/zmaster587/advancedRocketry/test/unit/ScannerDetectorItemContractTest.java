package zmaster587.advancedRocketry.test.unit;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.item.ItemBeaconFinder;
import zmaster587.advancedRocketry.item.ItemOreScanner;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TASK-05 Phase 3 — scanner / detector item contracts (unit tier).
 *
 * <p>Pins the pure-NBT and pure-function surface of the scanner /
 * detector items. The EntityPlayer-driven {@code onItemRightClick} /
 * {@code onItemUse} paths (which call into a real
 * {@link zmaster587.advancedRocketry.atmosphere.AtmosphereHandler},
 * {@link zmaster587.advancedRocketry.util.SealableBlockHandler}, or
 * GUI subsystem) are defer-allocated to the testClient e2e harness
 * (TASK-10b) per the TASK-05 plan §"Technical Decisions". Do NOT
 * introduce a FakePlayer here.</p>
 *
 * <p>Scope:</p>
 * <ul>
 *   <li>{@link ItemBeaconFinder} — IArmorComponent slot gate +
 *       install no-throw contract.</li>
 *   <li>{@link ItemOreScanner} — satellite-id NBT round-trip +
 *       IModularInventory metadata.</li>
 * </ul>
 *
 * <p>{@link zmaster587.advancedRocketry.item.ItemAtmosphereAnalzer} is
 * intentionally absent at unit tier — its {@code <clinit>} dereferences
 * {@code LibVulpes.proxy.getLocalizedString(...)} into static fields,
 * which NPEs because the proxy isn't injected until full Forge boot.
 * Defer its IArmorComponent surface to the testServer / testClient
 * tier.</p>
 *
 * <p>{@link zmaster587.advancedRocketry.item.ItemSealDetector} is also
 * absent — its only non-trivial method ({@code onItemUse}) requires
 * a real World + EntityPlayer to invoke the
 * {@link zmaster587.advancedRocketry.util.SealableBlockHandler} chain;
 * that surface lives in testServer / testClient.</p>
 */
public class ScannerDetectorItemContractTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    // ───────────────────── ItemBeaconFinder ──────────────────────────────

    @Test
    public void beaconFinderAcceptsHeadSlotOnly() {
        ItemBeaconFinder item = new ItemBeaconFinder();
        ItemStack stack = new ItemStack(item, 1);

        assertTrue("beacon finder must allow HEAD slot",
                item.isAllowedInSlot(stack, EntityEquipmentSlot.HEAD));
        for (EntityEquipmentSlot slot : new EntityEquipmentSlot[]{
                EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS,
                EntityEquipmentSlot.FEET}) {
            assertFalse("beacon finder must reject slot " + slot,
                    item.isAllowedInSlot(stack, slot));
        }
    }

    @Test
    public void beaconFinderOnComponentAddedAlwaysSucceeds() {
        ItemBeaconFinder item = new ItemBeaconFinder();
        ItemStack armor = new ItemStack(item, 1);
        assertTrue("onComponentAdded must always report success",
                item.onComponentAdded(null, armor));
    }

    // ───────────────────── ItemOreScanner ────────────────────────────────

    @Test
    public void oreScannerEmptyStackReturnsSentinelSatelliteId() {
        // Production contract (getSatelliteID, lines 66-73): a stack with
        // no NBT returns -1, NOT 0. -1 is the sentinel "unprogrammed
        // scanner" value that the addInformation tooltip path (line 40-41)
        // uses to display "msg.unprogrammed".
        ItemOreScanner scanner = new ItemOreScanner();
        ItemStack s = new ItemStack(scanner, 1);
        assertEquals("fresh scanner stack must report -1 (unprogrammed)",
                -1L, scanner.getSatelliteID(s));
    }

    @Test
    public void oreScannerSatelliteIdRoundTrips() {
        ItemOreScanner scanner = new ItemOreScanner();
        ItemStack s = new ItemStack(scanner, 1);
        scanner.setSatelliteID(s, 0xABCD_EF01L);
        assertTrue("setSatelliteID must attach NBT", s.hasTagCompound());
        assertEquals(0xABCD_EF01L, scanner.getSatelliteID(s));
    }

    @Test
    public void oreScannerSatelliteIdOverwriteIsLossless() {
        ItemOreScanner scanner = new ItemOreScanner();
        ItemStack s = new ItemStack(scanner, 1);
        scanner.setSatelliteID(s, 1L);
        scanner.setSatelliteID(s, 42L);
        assertEquals("subsequent setSatelliteID must overwrite, not duplicate",
                42L, scanner.getSatelliteID(s));
    }

    @Test
    public void oreScannerSatelliteIdSurvivesItemStackCopy() {
        ItemOreScanner scanner = new ItemOreScanner();
        ItemStack a = new ItemStack(scanner, 1);
        scanner.setSatelliteID(a, 7777L);
        ItemStack b = a.copy();
        assertEquals("ItemStack.copy() must preserve the scanner's satellite ID",
                7777L, scanner.getSatelliteID(b));
    }

    @Test
    public void oreScannerSatelliteIdReadDirectlyFromKnownNbtKey() {
        // Pin the NBT key shape — production reads "id" as a long. A
        // future refactor that renames this key silently un-programs
        // every player's pre-existing scanner on world load.
        ItemOreScanner scanner = new ItemOreScanner();
        ItemStack s = new ItemStack(scanner, 1);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong("id", 13L);
        s.setTagCompound(nbt);
        assertEquals("scanner must read satellite id from the NBT key 'id'",
                13L, scanner.getSatelliteID(s));
    }

    @Test
    public void oreScannerModularInventoryHookIsAlwaysOpenable() {
        // canInteractWithContainer returns true unconditionally — the
        // observable contract is "the scanner's GUI is always reachable
        // regardless of player state". A regression that gates this on,
        // say, a programmed-satellite check would silently lock players
        // out of the unprogramming workflow.
        ItemOreScanner scanner = new ItemOreScanner();
        assertTrue("scanner GUI must always be openable",
                scanner.canInteractWithContainer(null));
    }

    @Test
    public void oreScannerExposesEmptyModuleListForNow() {
        // getModules currently returns an empty list (the OreMapper
        // module is commented out in production). Pinning "empty list,
        // non-null, no throw" guards the future-restoration path: when
        // production re-enables the module, this test will fail and the
        // author will update both production and test together.
        ItemOreScanner scanner = new ItemOreScanner();
        assertNotNull("getModules must not return null",
                scanner.getModules(0, null));
        assertTrue("getModules currently has zero entries (OreMapper module "
                + "commented out at ItemOreScanner.java:121); if this fires "
                + "production has restored a module — update the test along "
                + "with whatever was re-enabled",
                scanner.getModules(0, null).isEmpty());
    }
}
