package zmaster587.advancedRocketry.test.unit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.item.ItemBiomeChanger;
import zmaster587.advancedRocketry.item.ItemThermite;
import zmaster587.advancedRocketry.item.ItemWeatherController;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-05 Phase 5 — special-purpose item contracts (unit tier).
 *
 * <p>Pins the surface that doesn't require a real EntityPlayer / world /
 * satellite. {@code onItemRightClick} and {@code useNetworkData} (which
 * call into a real satellite's {@code performAction}) live in the
 * testClient e2e harness (TASK-10b) per the TASK-05 plan §"Technical
 * Decisions".</p>
 *
 * <p>Scope:</p>
 * <ul>
 *   <li>{@link ItemThermite} — furnace burn-time contract.</li>
 *   <li>{@link ItemBiomeChanger} — IModularInventory metadata +
 *       wire→NBT round-trip for the satellite-modification packet.</li>
 *   <li>{@link ItemWeatherController} — IModularInventory metadata +
 *       wire→NBT round-trip for the weather-state packet (3 fields).</li>
 * </ul>
 *
 * <p>{@link zmaster587.advancedRocketry.item.ItemData} and
 * {@link zmaster587.advancedRocketry.item.ItemMultiData} are covered in
 * the dedicated {@code ItemDataCarrierNBTRoundTripTest}.</p>
 */
public class SpecialPurposeItemContractTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    // ───────────────────── ItemThermite ──────────────────────────────────

    @Test
    public void thermiteBurnTimeMatchesFurnaceContract() {
        // Thermite's vanilla-Forge furnace fuel value is 6000 ticks. This
        // is externally observable — modpack recipes / autoclave logic
        // depend on it for crafting outcomes. The value is the contract;
        // the constant 6000 IS the API surface (in the same sense as a
        // registry name or recipe ID).
        ItemThermite item = new ItemThermite();
        ItemStack stack = new ItemStack(item, 1);
        assertEquals("ItemThermite must report 6000-tick furnace burn time",
                6000, item.getItemBurnTime(stack));
    }

    @Test
    public void thermiteBurnTimeIsStackInsensitive() {
        // Burn time must not depend on stack size or NBT — vanilla furnace
        // takes one item at a time and burns the configured time.
        ItemThermite item = new ItemThermite();
        ItemStack single = new ItemStack(item, 1);
        ItemStack many = new ItemStack(item, 64);
        ItemStack tagged = new ItemStack(item, 1);
        tagged.setTagCompound(new NBTTagCompound());

        assertEquals(6000, item.getItemBurnTime(single));
        assertEquals(6000, item.getItemBurnTime(many));
        assertEquals(6000, item.getItemBurnTime(tagged));
    }

    // ───────────────────── ItemBiomeChanger ──────────────────────────────

    @Test
    public void biomeChangerModularInventoryNameIsI18nKey() {
        // Production contract: the GUI uses this string as the i18n
        // lookup key for the chest title bar. Renaming it silently swaps
        // the GUI title (vanilla I18n returns the key verbatim when no
        // match). Pin the exact key.
        ItemBiomeChanger item = new ItemBiomeChanger();
        assertEquals("item.biomeChanger.name", item.getModularInventoryName());
    }

    @Test
    public void biomeChangerContainerAlwaysOpenable() {
        ItemBiomeChanger item = new ItemBiomeChanger();
        assertTrue("biome-changer GUI must always be openable",
                item.canInteractWithContainer(null));
    }

    @Test
    public void biomeChangerReadDataFromNetworkPersistsBiomeIdToNbt() {
        // Wire-format contract: packet id 0 carries a single int payload
        // → write into NBT key "biome". This pins both the packet schema
        // AND the NBT key name used by the production useNetworkData
        // path (line 180: `nbt.getInteger("biome")`).
        ItemBiomeChanger item = new ItemBiomeChanger();
        ItemStack stack = new ItemStack(item, 1);
        NBTTagCompound nbt = new NBTTagCompound();
        ByteBuf wire = Unpooled.buffer().writeInt(7);
        item.readDataFromNetwork(wire, (byte) 0, nbt, stack);
        assertEquals("packet id 0 must persist payload int into NBT key 'biome'",
                7, nbt.getInteger("biome"));
    }

    @Test
    public void biomeChangerReadDataFromNetworkOtherPacketIdIsNoOp() {
        // Production gates on packetId==0; other ids must NOT mutate NBT.
        // Contract: forward-compatibility for future packet ids — an
        // unknown packetId silently does nothing rather than corrupting
        // existing state.
        ItemBiomeChanger item = new ItemBiomeChanger();
        ItemStack stack = new ItemStack(item, 1);
        NBTTagCompound nbt = new NBTTagCompound();
        ByteBuf wire = Unpooled.buffer().writeInt(42);
        item.readDataFromNetwork(wire, (byte) 99, nbt, stack);
        assertTrue("unknown packet id must leave NBT untouched (no 'biome' key)",
                !nbt.hasKey("biome"));
    }

    // ───────────────────── ItemWeatherController ─────────────────────────

    @Test
    public void weatherControllerModularInventoryNameIsI18nKey() {
        ItemWeatherController item = new ItemWeatherController();
        assertEquals("item.weatherController.name",
                item.getModularInventoryName());
    }

    @Test
    public void weatherControllerContainerAlwaysOpenable() {
        ItemWeatherController item = new ItemWeatherController();
        assertTrue("weather-controller GUI must always be openable",
                item.canInteractWithContainer(null));
    }

    @Test
    public void weatherControllerReadDataFromNetworkPersistsAllThreeFieldsToNbt() {
        // Wire-format contract: every packet carries 3 ints in fixed
        // order — mode_id, floodlevel, last_mode_id — written into NBT
        // keys of matching names (production useNetworkData reads back
        // by exactly these keys at lines 171-173). Pin both the order
        // and the NBT key names.
        ItemWeatherController item = new ItemWeatherController();
        ItemStack stack = new ItemStack(item, 1);
        NBTTagCompound nbt = new NBTTagCompound();
        ByteBuf wire = Unpooled.buffer()
                .writeInt(2)    // mode_id
                .writeInt(63)   // floodlevel
                .writeInt(1);   // last_mode_id

        item.readDataFromNetwork(wire, (byte) 0, nbt, stack);

        assertEquals("first int → NBT 'mode_id'",
                2, nbt.getInteger("mode_id"));
        assertEquals("second int → NBT 'floodlevel'",
                63, nbt.getInteger("floodlevel"));
        assertEquals("third int → NBT 'last_mode_id'",
                1, nbt.getInteger("last_mode_id"));
    }
}
