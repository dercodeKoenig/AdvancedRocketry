package zmaster587.advancedRocketry.test.unit;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;
import zmaster587.advancedRocketry.util.ItemAirUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 3 (continuation, unit slice).
 *
 * {@link ItemAirUtils} stores the suit's remaining air as an NBT integer
 * on the stack. Pin the read/write/decrement/increment math:
 *
 *   - decrement clamps at 0 and returns the actual amount extracted
 *   - increment clamps at getMaxAir() and returns the actual amount added
 *   - setAirRemaining does NOT clamp (documented as such in the production
 *     javadoc — surface that contract here so a future "tighten this up"
 *     refactor doesn't silently break callers that intentionally over-fill)
 */
public class ItemAirUtilsTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Before
    public void ensureOxygenBufferConfigured() {
        // ARConfiguration is normally populated by Forge config-load in
        // production. In unit tests it defaults to 0, which makes
        // getMaxAir() return 0 and the increment/decrement clamps trivial.
        // Set a realistic value (30 mins, matching production default) so
        // the boundary math has room to breathe.
        if (ARConfiguration.getCurrentConfig().spaceSuitOxygenTime <= 0) {
            ARConfiguration.getCurrentConfig().spaceSuitOxygenTime = 30;
        }
    }

    private static ItemStack stack() {
        // Any item is fine — ItemAirUtils reads/writes the "air" NBT key on
        // whatever stack you pass in; it doesn't care about item identity.
        return new ItemStack(Items.IRON_HELMET, 1);
    }

    @Test
    public void setAirThenGetAirRoundTrips() {
        ItemStack s = stack();
        ItemAirUtils.INSTANCE.setAirRemaining(s, 1234);
        assertEquals(1234, ItemAirUtils.INSTANCE.getAirRemaining(s));
    }

    @Test
    public void decrementClampsAtZeroAndReportsExtractedAmount() {
        ItemStack s = stack();
        ItemAirUtils.INSTANCE.setAirRemaining(s, 100);
        int extracted = ItemAirUtils.INSTANCE.decrementAir(s, 30);
        assertEquals("decrement should report exactly the amount extracted", 30, extracted);
        assertEquals(70, ItemAirUtils.INSTANCE.getAirRemaining(s));

        int overshoot = ItemAirUtils.INSTANCE.decrementAir(s, 999);
        assertEquals("decrement past zero must report only the remaining amount",
                70, overshoot);
        assertEquals("decrement must clamp the stored value at zero",
                0, ItemAirUtils.INSTANCE.getAirRemaining(s));
    }

    @Test
    public void incrementClampsAtMaxAndReportsInsertedAmount() {
        ItemStack s = stack();
        int max = ItemAirUtils.INSTANCE.getMaxAir(s);
        assertTrue("max air must be positive — config sentinel: spaceSuitOxygenTime > 0",
                max > 0);

        ItemAirUtils.INSTANCE.setAirRemaining(s, 0);
        int added = ItemAirUtils.INSTANCE.increment(s, 50);
        assertEquals(50, added);
        assertEquals(50, ItemAirUtils.INSTANCE.getAirRemaining(s));

        // Overshoot — increment far beyond max — must clamp and report only
        // what fit.
        int addedOverflow = ItemAirUtils.INSTANCE.increment(s, max * 2);
        assertEquals("increment overshoot must report only the amount that fit",
                max - 50, addedOverflow);
        assertEquals("storage must clamp at max",
                max, ItemAirUtils.INSTANCE.getAirRemaining(s));
    }

    @Test
    public void setAirRemainingDoesNotClamp() {
        // The setAirRemaining javadoc says "DOES NOT BOUNDS CHECK!". This
        // is a contract for callers who intentionally bypass the increment
        // limit. Pin it so a future refactor doesn't silently start clamping.
        ItemStack s = stack();
        int max = ItemAirUtils.INSTANCE.getMaxAir(s);
        ItemAirUtils.INSTANCE.setAirRemaining(s, max * 3);
        assertEquals("setAirRemaining must NOT clamp — docstring says no bounds check",
                max * 3, ItemAirUtils.INSTANCE.getAirRemaining(s));

        ItemAirUtils.INSTANCE.setAirRemaining(s, -100);
        assertEquals("setAirRemaining accepts negative — no clamp",
                -100, ItemAirUtils.INSTANCE.getAirRemaining(s));
    }

    @Test
    public void freshStackAirReadFillsToMax() {
        // Documented oddity: getAirRemaining on a stack with NO tag compound
        // creates the tag, stores 0, then returns getMaxAir(stack). This
        // implements "freshly-crafted suit has a full tank". Pin it — the
        // null-NBT branch is easy to refactor away.
        ItemStack s = stack();
        assertFalse(s.hasTagCompound());
        int firstRead = ItemAirUtils.INSTANCE.getAirRemaining(s);
        assertEquals("fresh stack must read as 'full tank' on first getAirRemaining",
                ItemAirUtils.INSTANCE.getMaxAir(s), firstRead);
        assertTrue("after first read, tag compound must exist",
                s.hasTagCompound());
        NBTTagCompound tag = s.getTagCompound();
        assertEquals("tag's stored air must be 0 (counter to the returned value)",
                0, tag.getInteger("air"));
        // Second read picks up the stored zero — quirky but documented behaviour.
        assertEquals("second read returns the stored zero",
                0, ItemAirUtils.INSTANCE.getAirRemaining(s));
    }
}
