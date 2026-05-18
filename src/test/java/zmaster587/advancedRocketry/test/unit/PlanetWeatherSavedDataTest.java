package zmaster587.advancedRocketry.test.unit;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import zmaster587.advancedRocketry.world.weather.PlanetWeatherSavedData;
import zmaster587.advancedRocketry.world.weather.PlanetWeatherState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * SMART §6.10 (3) — {@link PlanetWeatherSavedData} stores state by dimension id
 * and round-trips its full map through NBT. Pure-NBT test; no MC bootstrap.
 *
 * Also pins the storage key — changing it would silently lose all existing
 * players' weather state, so it lives behind a regression test.
 */
public class PlanetWeatherSavedDataTest {

    @Test
    public void storageKeyIsStable() {
        // Save files use this string verbatim. Renaming = silent data loss.
        assertEquals("advancedrocketry_planet_weather", PlanetWeatherSavedData.STORAGE_KEY);
    }

    @Test
    public void getOrCreateInsertsFreshStateAndIsIdempotent() {
        PlanetWeatherSavedData data = new PlanetWeatherSavedData();
        PlanetWeatherState first = data.getOrCreate(42);
        assertNotNull(first);

        PlanetWeatherState second = data.getOrCreate(42);
        assertSame("second call must return the same instance, not a fresh one",
                first, second);
        assertTrue("inserting fresh state must mark dirty", data.isDirty());
    }

    @Test
    public void getOrCreateIsolatesDimensions() {
        PlanetWeatherSavedData data = new PlanetWeatherSavedData();
        PlanetWeatherState a = data.getOrCreate(101);
        PlanetWeatherState b = data.getOrCreate(102);

        assertNotSame("different dim ids must yield different state instances", a, b);
        a.setRaining(true);
        assertFalse("mutation on dim A must not leak to dim B", b.isRaining());
    }

    @Test
    public void getIfPresentDoesNotInsert() {
        PlanetWeatherSavedData data = new PlanetWeatherSavedData();
        assertNull("getIfPresent must not auto-create", data.getIfPresent(999));
    }

    @Test
    public void planetWeatherSavedDataStoresByDimensionId() {
        PlanetWeatherSavedData source = new PlanetWeatherSavedData();
        PlanetWeatherState a = source.getOrCreate(2);
        a.setRaining(true);
        a.setRainTime(11111);
        a.setCleanWeatherTime(0);

        PlanetWeatherState b = source.getOrCreate(7);
        b.setThundering(true);
        b.setThunderTime(22222);

        NBTTagCompound tag = new NBTTagCompound();
        source.writeToNBT(tag);

        PlanetWeatherSavedData round = new PlanetWeatherSavedData();
        round.readFromNBT(tag);

        PlanetWeatherState a2 = round.getIfPresent(2);
        PlanetWeatherState b2 = round.getIfPresent(7);
        assertNotNull("dim 2 must round-trip", a2);
        assertNotNull("dim 7 must round-trip", b2);
        assertTrue("dim 2 raining", a2.isRaining());
        assertEquals(11111, a2.getRainTime());
        assertTrue("dim 7 thundering", b2.isThundering());
        assertEquals(22222, b2.getThunderTime());

        // Untouched dim must remain absent — guard against the bug where the
        // map writer silently injects every (dim, default-state) entry it
        // sees.
        assertNull("untouched dim must not appear after restore", round.getIfPresent(99));
    }

    @Test
    public void emptySavedDataRoundTripsToEmpty() {
        PlanetWeatherSavedData source = new PlanetWeatherSavedData();
        NBTTagCompound tag = new NBTTagCompound();
        source.writeToNBT(tag);

        PlanetWeatherSavedData round = new PlanetWeatherSavedData();
        round.readFromNBT(tag);
        assertNull(round.getIfPresent(0));
        assertNull(round.getIfPresent(7));
    }
}
