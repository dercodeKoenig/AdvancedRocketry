package zmaster587.advancedRocketry.test.unit;

import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.api.DataStorage;
import zmaster587.advancedRocketry.item.ItemData;
import zmaster587.advancedRocketry.item.ItemMultiData;
import zmaster587.advancedRocketry.item.ItemSpaceElevatorChip;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;
import zmaster587.advancedRocketry.util.DimensionBlockPosition;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TASK-05 Phase 1/5 — data-carrying items NBT round-trip pins.
 *
 * <p>Three items in {@code item/} are pure NBT carriers that production
 * reads/writes directly:</p>
 *
 * <ul>
 *   <li>{@link ItemSpaceElevatorChip} — list of {@link DimensionBlockPosition}
 *       entries (player binds an elevator to one or more station landing
 *       coords).</li>
 *   <li>{@link ItemData} — single-type data-stick from satellite collection
 *       (mass, composition, atmospheric density, etc.).</li>
 *   <li>{@link ItemMultiData} — multi-type data carrier used by the
 *       composition satellite to aggregate multiple data types into one
 *       stick.</li>
 * </ul>
 *
 * <p>Contracts pinned: round-trip read-after-write, default-on-empty,
 * stack independence after {@link ItemStack#copy()}. One known production
 * bug in {@link ItemSpaceElevatorChip#setBlockPositions} is pinned via
 * {@code _documentsKnownBug} per the TASK-01 §15 "no production logic
 * changes" rule.</p>
 *
 * <p>All tests run at unit tier via {@link MinecraftBootstrap} — no
 * world / server needed since these items are pure NBT manipulators.</p>
 */
public class ItemDataCarrierNBTRoundTripTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    // ────────────────────── ItemSpaceElevatorChip ───────────────────────

    @Test
    public void elevatorChipEmptyStackReturnsEmptyPositionList() {
        ItemSpaceElevatorChip chip = new ItemSpaceElevatorChip();
        ItemStack s = new ItemStack(chip, 1);
        List<DimensionBlockPosition> positions = chip.getBlockPositions(s);
        assertNotNull("getBlockPositions must not return null on empty stack",
                positions);
        assertTrue("fresh stack must yield empty position list",
                positions.isEmpty());
    }

    @Test
    public void elevatorChipPositionsRoundTripAcrossWriteRead() {
        ItemSpaceElevatorChip chip = new ItemSpaceElevatorChip();
        ItemStack s = new ItemStack(chip, 1);

        List<DimensionBlockPosition> input = Arrays.asList(
                new DimensionBlockPosition(0, new HashedBlockPosition(10, 64, -5)),
                new DimensionBlockPosition(-1, new HashedBlockPosition(0, 200, 0)));
        chip.setBlockPositions(s, input);

        assertTrue("setBlockPositions(non-empty) must attach NBT",
                s.hasTagCompound());
        List<DimensionBlockPosition> out = chip.getBlockPositions(s);
        assertEquals("round-trip must preserve element count",
                input.size(), out.size());
        // Compare via equals (DimensionBlockPosition implements equals by
        // dim + pos), order is the wire-contract preserved by NBTTagList.
        for (int i = 0; i < input.size(); i++) {
            assertEquals("round-trip position [" + i + "] must equal input",
                    input.get(i), out.get(i));
        }
    }

    @Test
    public void elevatorChipSetEmptyOnFreshStackDoesNotAttachNbt() {
        ItemSpaceElevatorChip chip = new ItemSpaceElevatorChip();
        ItemStack s = new ItemStack(chip, 1);
        chip.setBlockPositions(s, new ArrayList<DimensionBlockPosition>());
        assertFalse("setBlockPositions([]) on a stack with no NBT must not "
                + "create a tag compound (production lines 46-51 gate on "
                + "!isEmpty for the attach branch)", s.hasTagCompound());
    }

    @Test
    public void elevatorChipPositionsSurviveItemStackCopy() {
        ItemSpaceElevatorChip chip = new ItemSpaceElevatorChip();
        ItemStack a = new ItemStack(chip, 1);
        chip.setBlockPositions(a, Arrays.asList(
                new DimensionBlockPosition(2, new HashedBlockPosition(1, 2, 3))));

        ItemStack b = a.copy();
        List<DimensionBlockPosition> out = chip.getBlockPositions(b);
        assertEquals("copy must preserve elevator-chip position list size",
                1, out.size());
        assertEquals("copy must preserve elevator-chip position content",
                new DimensionBlockPosition(2, new HashedBlockPosition(1, 2, 3)),
                out.get(0));
    }

    /** Fixed in TASK-12 (bug #5). The empty-input clear branch
     *  previously called {@code removeTag("positions")} but the data
     *  lived under {@code "list"} per
     *  {@code NBTStorableListList.writeToNBT}, so the clear was a no-op.
     *  Now the key matches and the list is actually cleared. */
    @Test
    public void elevatorChipSetEmptyAfterNonEmptyClearsList() {
        ItemSpaceElevatorChip chip = new ItemSpaceElevatorChip();
        ItemStack s = new ItemStack(chip, 1);
        chip.setBlockPositions(s, Arrays.asList(
                new DimensionBlockPosition(0, new HashedBlockPosition(7, 8, 9))));
        assertEquals("precondition: one position attached",
                1, chip.getBlockPositions(s).size());

        chip.setBlockPositions(s, new ArrayList<DimensionBlockPosition>());

        assertEquals("setBlockPositions with an empty list must clear the "
                        + "stored list (NBT key \"list\" removed)",
                0, chip.getBlockPositions(s).size());
    }

    // ────────────────────── ItemData ────────────────────────────────────

    @Test
    public void dataStickEmptyStackReportsZeroData() {
        ItemData item = new ItemData();
        ItemStack s = new ItemStack(item, 1);
        assertEquals("fresh data stick must report 0 data",
                0, item.getData(s));
        assertEquals("fresh data stick's type must default to UNDEFINED",
                DataStorage.DataType.UNDEFINED, item.getDataType(s));
    }

    @Test
    public void dataStickAddDataPersistsAcrossReads() {
        ItemData item = new ItemData();
        ItemStack s = new ItemStack(item, 1);
        int added = item.addData(s, 42, DataStorage.DataType.MASS);
        assertTrue("addData must report some amount stored (non-negative)",
                added >= 0);
        assertEquals("getData after addData must echo the stored amount",
                added, item.getData(s));
        assertEquals("getDataType after addData(MASS) must report MASS",
                DataStorage.DataType.MASS, item.getDataType(s));
        assertTrue("addData must attach NBT", s.hasTagCompound());
    }

    @Test
    public void dataStickRemoveDataDecrementsStoredAmount() {
        ItemData item = new ItemData();
        ItemStack s = new ItemStack(item, 1);
        item.addData(s, 100, DataStorage.DataType.COMPOSITION);
        int before = item.getData(s);
        int removed = item.removeData(s, 30, DataStorage.DataType.COMPOSITION);
        assertTrue("removeData must report a non-negative amount removed",
                removed >= 0);
        assertEquals("getData after removeData must reflect the decrement",
                before - removed, item.getData(s));
    }

    @Test
    public void dataStickSetDataOverridesPreviousValue() {
        ItemData item = new ItemData();
        ItemStack s = new ItemStack(item, 1);
        item.addData(s, 50, DataStorage.DataType.DISTANCE);
        item.setData(s, 7, DataStorage.DataType.DISTANCE);
        assertEquals("setData must overwrite the previous stored value",
                7, item.getData(s));
    }

    @Test
    public void dataStickNeverStacksPastOne() {
        // Production contract: ItemData() ctor calls setMaxStackSize(1), so
        // the data-stick item ALWAYS reports a stack-limit of 1 regardless
        // of the stored-data branch in getItemStackLimit (the data==0 ?
        // super : 1 ternary is functionally dead because super also
        // returns 1). The observable contract is "data sticks do not stack
        // — each one is its own inventory entry".
        ItemData item = new ItemData();
        ItemStack empty = new ItemStack(item, 1);
        ItemStack programmed = new ItemStack(item, 1);
        item.addData(programmed, 10, DataStorage.DataType.MASS);

        assertEquals("empty data stick must not stack past 1",
                1, item.getItemStackLimit(empty));
        assertEquals("programmed data stick must not stack past 1",
                1, item.getItemStackLimit(programmed));
    }

    @Test
    public void dataStickMaxDataIsZeroForNonZeroDamage() {
        // ItemData.getMaxData(damage) returns 1000 only for damage 0;
        // every other damage value yields 0. Production uses this to gate
        // "this stick variant carries data" vs "this is the inert variant".
        ItemData item = new ItemData();
        assertEquals("damage 0 → max data 1000",
                1000, item.getMaxData(0));
        assertEquals("damage 1 → max data 0 (inert variant)",
                0, item.getMaxData(1));
        assertEquals("any non-zero damage → max data 0",
                0, item.getMaxData(99));
    }

    // ────────────────────── ItemMultiData ───────────────────────────────

    @Test
    public void multiDataEmptyStackReportsZeroForEveryRealType() {
        // Contract: every "real" DataType (every non-UNDEFINED enum value)
        // defaults to 0 on a fresh stack. UNDEFINED is the sentinel — both
        // MultiData.reset() (line 23-24) and ItemMultiData.addInformation
        // (line 121) explicitly skip it, so querying UNDEFINED is not part
        // of the public contract.
        ItemMultiData item = new ItemMultiData();
        ItemStack s = new ItemStack(item, 1);
        for (DataStorage.DataType t : DataStorage.DataType.values()) {
            if (t == DataStorage.DataType.UNDEFINED) continue;
            assertEquals("fresh multi-data stick must report 0 for type " + t,
                    0, item.getData(s, t));
        }
    }

    @Test
    public void multiDataAddDataAccumulatesPerTypeIndependently() {
        ItemMultiData item = new ItemMultiData();
        ItemStack s = new ItemStack(item, 1);
        item.setMaxData(s, 1000);

        item.addData(s, 50, DataStorage.DataType.MASS);
        item.addData(s, 20, DataStorage.DataType.HUMIDITY);

        // Each type's data is independent; adding to MASS must not leak
        // into HUMIDITY and vice-versa.
        int mass = item.getData(s, DataStorage.DataType.MASS);
        int hum = item.getData(s, DataStorage.DataType.HUMIDITY);
        assertTrue("MASS must register a non-zero amount after addData",
                mass > 0);
        assertTrue("HUMIDITY must register a non-zero amount after addData",
                hum > 0);
        assertEquals("TEMPERATURE bucket must remain at 0 — types do not "
                + "bleed into each other",
                0, item.getData(s, DataStorage.DataType.TEMPERATURE));
    }

    @Test
    public void multiDataSetMaxPersistsAcrossReads() {
        ItemMultiData item = new ItemMultiData();
        ItemStack s = new ItemStack(item, 1);
        item.setMaxData(s, 250);
        assertEquals("setMaxData must persist to subsequent getMaxData",
                250, item.getMaxData(s));
    }

    @Test
    public void multiDataIsFullWhenDataReachesMax() {
        ItemMultiData item = new ItemMultiData();
        ItemStack s = new ItemStack(item, 1);
        item.setMaxData(s, 10);
        item.setData(s, 10, DataStorage.DataType.MASS);
        assertTrue("isFull(MASS) must report true when data == max",
                item.isFull(s, DataStorage.DataType.MASS));
        item.setData(s, 5, DataStorage.DataType.MASS);
        assertFalse("isFull(MASS) must report false when data < max",
                item.isFull(s, DataStorage.DataType.MASS));
    }

    @Test
    public void multiDataRemoveDataDecrementsAddressedTypeOnly() {
        ItemMultiData item = new ItemMultiData();
        ItemStack s = new ItemStack(item, 1);
        item.setMaxData(s, 1000);
        item.addData(s, 100, DataStorage.DataType.MASS);
        item.addData(s, 100, DataStorage.DataType.HUMIDITY);

        int massBefore = item.getData(s, DataStorage.DataType.MASS);
        int humBefore = item.getData(s, DataStorage.DataType.HUMIDITY);

        item.removeData(s, 40, DataStorage.DataType.MASS);

        assertTrue("MASS amount must decrease after removeData(MASS)",
                item.getData(s, DataStorage.DataType.MASS) < massBefore);
        assertEquals("HUMIDITY must NOT be touched by removeData(MASS)",
                humBefore, item.getData(s, DataStorage.DataType.HUMIDITY));
    }

    @Test
    public void multiDataSurvivesItemStackCopy() {
        ItemMultiData item = new ItemMultiData();
        ItemStack a = new ItemStack(item, 1);
        item.setMaxData(a, 500);
        item.addData(a, 77, DataStorage.DataType.COMPOSITION);

        ItemStack b = a.copy();
        assertEquals("copy preserves maxData", 500, item.getMaxData(b));
        assertEquals("copy preserves per-type data",
                item.getData(a, DataStorage.DataType.COMPOSITION),
                item.getData(b, DataStorage.DataType.COMPOSITION));

        // Independence: mutating b must not bleed into a.
        item.setData(b, 0, DataStorage.DataType.COMPOSITION);
        assertTrue("mutating the copy must not change the original",
                item.getData(a, DataStorage.DataType.COMPOSITION) > 0);
    }
}
