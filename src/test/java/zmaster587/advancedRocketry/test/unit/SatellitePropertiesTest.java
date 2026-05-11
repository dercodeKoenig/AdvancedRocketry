package zmaster587.advancedRocketry.test.unit;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import zmaster587.advancedRocketry.api.satellite.SatelliteProperties;
import zmaster587.advancedRocketry.api.satellite.SatelliteProperties.Property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * §6.6 Satellite domain logic — SatelliteProperties NBT round-trip + property flags.
 */
public class SatellitePropertiesTest {

    @Test
    public void satellitePropertiesNbtRoundTrip() {
        SatelliteProperties original = new SatelliteProperties(160, 5000, "ar:test_sat", 1024, 5.5f);
        original.setId(0xDEADBEEFL);

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        SatelliteProperties restored = new SatelliteProperties();
        restored.readFromNBT(nbt);

        assertEquals(original.getPowerGeneration(), restored.getPowerGeneration());
        assertEquals(original.getPowerStorage(), restored.getPowerStorage());
        assertEquals(original.getMaxDataStorage(), restored.getMaxDataStorage());
        assertEquals(original.getSatelliteType(), restored.getSatelliteType());
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getWeight(), restored.getWeight(), 1e-6);
    }

    @Test
    public void satelliteIdChipStoresAndReadsId() {
        SatelliteProperties props = new SatelliteProperties();
        assertEquals(-1, props.getId());

        boolean assigned = props.setId(42L);
        assertTrue("first setId on a fresh property must succeed", assigned);
        assertEquals(42L, props.getId());

        // setId is one-shot: subsequent assignments are rejected.
        boolean reassigned = props.setId(99L);
        assertFalse("setId must reject when an ID is already present", reassigned);
        assertEquals(42L, props.getId());
    }

    @Test
    public void propertyFlagsReflectConfiguredFields() {
        // No type, no power, no data → only zero-valued fields.
        SatelliteProperties empty = new SatelliteProperties();
        int emptyFlag = empty.getPropertyFlag();
        assertFalse(Property.MAIN.isOfType(emptyFlag));
        assertFalse(Property.POWER_GEN.isOfType(emptyFlag));
        assertFalse(Property.BATTERY.isOfType(emptyFlag));
        assertFalse(Property.DATA.isOfType(emptyFlag));

        SatelliteProperties full = new SatelliteProperties(50, 1000, "ar:full", 256, 1.0f);
        int flag = full.getPropertyFlag();
        assertTrue(Property.MAIN.isOfType(flag));
        assertTrue(Property.POWER_GEN.isOfType(flag));
        assertTrue(Property.BATTERY.isOfType(flag));
        assertTrue(Property.DATA.isOfType(flag));
    }

    @Test
    public void propertyFlagsAreDistinctBits() {
        // The flag enum uses 1 << ordinal() — verify they don't collide.
        int seen = 0;
        for (Property p : Property.values()) {
            int flag = p.getFlag();
            assertTrue("flag must be a single non-zero bit: " + p, flag > 0 && (flag & (flag - 1)) == 0);
            assertTrue("flags must be distinct: " + p, (seen & flag) == 0);
            seen |= flag;
        }
    }

    @Test
    public void emptyNbtReadProducesDefaults() {
        SatelliteProperties props = new SatelliteProperties();
        props.readFromNBT(new NBTTagCompound());

        assertEquals(0, props.getPowerGeneration());
        assertEquals(0, props.getPowerStorage());
        assertEquals(0, props.getMaxDataStorage());
        assertEquals(0L, props.getId());     // empty NBT → readLong default is 0
        assertEquals("", props.getSatelliteType());
        assertEquals(0f, props.getWeight(), 0f);
    }
}
