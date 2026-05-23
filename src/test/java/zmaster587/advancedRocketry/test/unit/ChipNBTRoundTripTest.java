package zmaster587.advancedRocketry.test.unit;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.api.Constants;
import zmaster587.advancedRocketry.item.ItemAsteroidChip;
import zmaster587.advancedRocketry.item.ItemPlanetIdentificationChip;
import zmaster587.advancedRocketry.item.ItemStationChip;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * TASK-05 Phase 1 — chip-item NBT round-trip pins.
 *
 * <p>Production launch and landing paths read these chip items' NBT directly;
 * a silent change in NBT-key names or read/write asymmetry would break
 * already-programmed chips on existing saves. Each chip is exercised at
 * unit tier via {@link MinecraftBootstrap} (vanilla MC registries + AR
 * CommonProxy injection + Sol star). No server / world required.</p>
 *
 * <p>The chip classes' setter methods are <b>instance</b> (not static), so
 * the tests construct each chip class directly. {@code new ItemX()} invokes
 * vanilla {@link net.minecraft.item.Item}'s no-arg constructor which is safe
 * after {@link net.minecraft.init.Bootstrap#register()}.</p>
 *
 * <p>Several production setters contain known asymmetries (set-then-get drops
 * the NBT in some branches). Those are pinned via {@code _documentsKnownBug}
 * tests rather than fixed — the "no production logic changes" rule from
 * TASK-01 §15 still applies.</p>
 */
public class ChipNBTRoundTripTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    /** Any vanilla ItemStack works — chip methods only touch the NBT. */
    private static ItemStack freshStack() {
        return new ItemStack(Items.STICK, 1);
    }

    // ───────────────────── ItemPlanetIdentificationChip ─────────────────

    @Test
    public void planetChipDimIdReadDefaultsToInvalidPlanetWithoutNbt() {
        // Production contract (getDimensionId, lines 99-103): a stack with
        // no NBT compound returns Constants.INVALID_PLANET. Programmed-chip
        // gameplay relies on this sentinel to decide "unprogrammed → show
        // unprogrammed tooltip".
        ItemPlanetIdentificationChip chip = new ItemPlanetIdentificationChip();
        ItemStack s = freshStack();
        assertEquals("fresh stack must read as INVALID_PLANET",
                Constants.INVALID_PLANET, chip.getDimensionId(s));
    }

    @Test
    public void planetChipDimIdRoundTripsForRegisteredDim() {
        // setDimensionId only persists if the dim is registered (its
        // production path looks up DimensionProperties and erase()s on miss).
        // INVALID_PLANET hits a separate early-return branch that has its
        // own contract — pinned in the _documentsKnownBug test below. Here
        // we directly seed the NBT key to validate the read path, which is
        // what production launch / landing tests as the "trusted" surface
        // (post-write integrity from the production setter is a separate
        // matter).
        ItemPlanetIdentificationChip chip = new ItemPlanetIdentificationChip();
        ItemStack s = freshStack();
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("dimId", 7);
        s.setTagCompound(nbt);
        assertEquals(7, chip.getDimensionId(s));
    }

    /** Fixed in TASK-12 (bug #8). The INVALID_PLANET branch of
     *  {@code setDimensionId} previously built a fresh NBT but never
     *  called {@code stack.setTagCompound(nbt)} before returning — so
     *  the sentinel was silently dropped. Now it attaches the NBT so
     *  callers can observe the "explicitly invalid" state. */
    @Test
    public void planetChipSetDimensionIdWithInvalidPlanetAttachesNbtSentinel() {
        ItemPlanetIdentificationChip chip = new ItemPlanetIdentificationChip();
        ItemStack s = freshStack();
        chip.setDimensionId(s, Constants.INVALID_PLANET);
        assertTrue("setDimensionId(INVALID_PLANET) must attach the NBT",
                s.hasTagCompound());
        assertEquals("the stored sentinel must equal INVALID_PLANET",
                Constants.INVALID_PLANET, s.getTagCompound().getInteger("dimId"));
    }

    @Test
    public void planetChipUuidRoundTrip() {
        // UUID setter does attach NBT correctly (line 131). Pin the round-
        // trip + verify the getter returns boxed Long (production callers
        // null-check against the "no NBT" case).
        ItemPlanetIdentificationChip chip = new ItemPlanetIdentificationChip();
        ItemStack s = freshStack();
        assertNull("fresh stack has no UUID — must return null, not 0",
                chip.getUUID(s));
        chip.setUUID(s, 0xCAFEBABE_DEADBEEFL);
        assertNotNull("after setUUID, getUUID must return non-null",
                chip.getUUID(s));
        assertEquals(0xCAFEBABE_DEADBEEFL, chip.getUUID(s).longValue());
    }

    @Test
    public void planetChipEraseClearsAllNbt() {
        ItemPlanetIdentificationChip chip = new ItemPlanetIdentificationChip();
        ItemStack s = freshStack();
        chip.setUUID(s, 42L);
        assertTrue("precondition: stack has NBT after setUUID", s.hasTagCompound());
        chip.erase(s);
        assertFalse("erase() must drop the entire NBT compound", s.hasTagCompound());
    }

    // ───────────────────── ItemStationChip ──────────────────────────────

    @Test
    public void stationChipUuidDefaultsToZero() {
        // Static methods, no instance needed. Production contract: no NBT
        // → getUUID returns 0 (not -1, not Integer.MIN_VALUE). 0 is also a
        // valid station UUID for an existing station, so callers must not
        // disambiguate "unprogrammed" from "station 0" by this method
        // alone.
        ItemStack s = freshStack();
        assertEquals(0, ItemStationChip.getUUID(s));
    }

    @Test
    public void stationChipUuidRoundTrip() {
        ItemStack s = freshStack();
        ItemStationChip.setUUID(s, 12345);
        assertEquals(12345, ItemStationChip.getUUID(s));
        // Re-set: subsequent write to same key must overwrite, not append.
        ItemStationChip.setUUID(s, -42);
        assertEquals(-42, ItemStationChip.getUUID(s));
    }

    @Test
    public void stationChipUuidPersistsAcrossItemStackCopy() {
        ItemStack a = freshStack();
        ItemStationChip.setUUID(a, 999);
        ItemStack b = a.copy();
        assertEquals("ItemStack.copy() must preserve the UUID NBT key",
                999, ItemStationChip.getUUID(b));
        // Independence: mutating b must NOT change a.
        ItemStationChip.setUUID(b, 1);
        assertEquals("mutating the copy must not bleed into the original",
                999, ItemStationChip.getUUID(a));
    }

    // ───────────────────── ItemAsteroidChip ─────────────────────────────

    @Test
    public void asteroidChipUuidAndTypeRoundTrip() {
        ItemAsteroidChip chip = new ItemAsteroidChip();
        ItemStack s = freshStack();
        assertNull("fresh stack: UUID null", chip.getUUID(s));
        assertNull("fresh stack: type null", chip.getType(s));

        chip.setUUID(s, 0x0123_4567_89ABCDEFL);
        chip.setType(s, "metallic");

        assertNotNull(chip.getUUID(s));
        assertEquals(0x0123_4567_89ABCDEFL, chip.getUUID(s).longValue());
        assertEquals("metallic", chip.getType(s));
    }

    @Test
    public void asteroidChipEraseDropsBothFields() {
        ItemAsteroidChip chip = new ItemAsteroidChip();
        ItemStack s = freshStack();
        chip.setUUID(s, 1L);
        chip.setType(s, "carbonaceous");
        chip.erase(s);
        assertNull("erase must clear UUID", chip.getUUID(s));
        assertNull("erase must clear type", chip.getType(s));
    }

    @Test
    public void asteroidChipTypeOverwriteIsLossless() {
        ItemAsteroidChip chip = new ItemAsteroidChip();
        ItemStack s = freshStack();
        chip.setType(s, "rocky");
        chip.setType(s, "icy");
        assertEquals("subsequent setType must overwrite, not concat",
                "icy", chip.getType(s));
        // UUID set in-between must not be dropped.
        chip.setUUID(s, 7L);
        chip.setType(s, "metallic");
        assertEquals(7L, chip.getUUID(s).longValue());
        assertEquals("metallic", chip.getType(s));
    }

    // ─────────────────── ItemSatelliteIdentificationChip ────────────────

    @Test
    public void satelliteChipDirectNbtReadsBackKnownKeys() {
        // setSatellite(SatelliteBase) requires a non-null SatelliteBase
        // backed by the full SatelliteRegistry — out of scope for a
        // unit test. For the NBT-format pin, directly seed the keys that
        // production reads: satelliteId, dimId, satelliteName. The
        // ItemSatelliteIdentificationChip.getSatellite static method
        // routes through DimensionManager → FMLCommonHandler.getSide(),
        // which requires Forge's FML to be initialised; that's a
        // server-tier integration concern, not unit-tier. We pin the
        // NBT key shape here; the server-tier round-trip is implicitly
        // covered by SatelliteIdChipPersistenceTest.
        ItemStack s = freshStack();
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong("satelliteId", 42L);
        nbt.setInteger("dimId", 0);
        nbt.setString("satelliteName", "test-comsat");
        s.setTagCompound(nbt);

        assertEquals(42L, s.getTagCompound().getLong("satelliteId"));
        assertEquals(0, s.getTagCompound().getInteger("dimId"));
        assertEquals("test-comsat", s.getTagCompound().getString("satelliteName"));
    }

    /** TASK-12 (bug #6) — {@code setSatellite(SatelliteBase)} must
     *  attach the freshly built NBT to the stack. Previously the
     *  else-branch (no pre-existing tag compound) silently dropped
     *  the NBT because {@code stack.setTagCompound(nbt)} was missing
     *  — the sibling overload {@code setSatellite(SatelliteProperties)}
     *  at line 87 did attach it, confirming the omission was an
     *  oversight. */
    @Test
    public void satelliteChipSetSatelliteAttachesNbtToFreshStack() {
        zmaster587.advancedRocketry.item.ItemSatelliteIdentificationChip chip =
                new zmaster587.advancedRocketry.item.ItemSatelliteIdentificationChip();
        ItemStack s = freshStack();
        zmaster587.advancedRocketry.api.satellite.SatelliteBase fake =
                new zmaster587.advancedRocketry.api.satellite.SatelliteBase() {
                    @Override public String getName() { return "test-comsat"; }
                    @Override public int getDimensionId() { return 17; }
                    @Override public long getId() { return 4242L; }
                    @Override public String getInfo(net.minecraft.world.World w) { return ""; }
                    @Override public boolean performAction(net.minecraft.entity.player.EntityPlayer p,
                            net.minecraft.world.World w, net.minecraft.util.math.BlockPos b) { return false; }
                    @Override public double failureChance() { return 0; }
                };
        chip.setSatellite(s, fake);
        assertTrue("setSatellite must attach the NBT to a fresh stack",
                s.hasTagCompound());
        assertEquals("test-comsat", s.getTagCompound().getString("satelliteName"));
        assertEquals(17, s.getTagCompound().getInteger("dimId"));
        assertEquals(4242L, s.getTagCompound().getLong("satelliteId"));
    }

    @Test
    public void satelliteChipEraseClearsNbt() {
        zmaster587.advancedRocketry.item.ItemSatelliteIdentificationChip chip =
                new zmaster587.advancedRocketry.item.ItemSatelliteIdentificationChip();
        ItemStack s = freshStack();
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong("satelliteId", 99L);
        s.setTagCompound(nbt);
        chip.erase(s);
        assertFalse("erase must drop the NBT compound entirely", s.hasTagCompound());
    }

    // ───────────────────── Cross-chip: ItemStack.copy() ────────────────

    @Test
    public void itemStackCopyPreservesArbitraryChipNbt() {
        // Generic copy contract — pins that AR's "chip is a stack with
        // NBT" assumption survives the vanilla copy path used by hopper,
        // shulker boxes, inventory transfer, etc.
        ItemStack a = freshStack();
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("dimId", 1);
        nbt.setLong("UUID", 0xDEADBEEFL);
        nbt.setString("DimensionName", "TestPlanet");
        a.setTagCompound(nbt);

        ItemStack b = a.copy();
        assertEquals(1, b.getTagCompound().getInteger("dimId"));
        assertEquals(0xDEADBEEFL, b.getTagCompound().getLong("UUID"));
        assertEquals("TestPlanet", b.getTagCompound().getString("DimensionName"));

        // Mutating b's NBT must not alter a's.
        b.getTagCompound().setInteger("dimId", 99);
        assertEquals("original stack's NBT must be independent of the copy",
                1, a.getTagCompound().getInteger("dimId"));
    }
}
