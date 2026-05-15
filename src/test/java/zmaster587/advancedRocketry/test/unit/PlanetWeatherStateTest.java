package zmaster587.advancedRocketry.test.unit;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import zmaster587.advancedRocketry.world.weather.PlanetWeatherState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * SMART §6.10 (1, 2) — pure-Java unit tests for the per-dimension weather state
 * model. Deliberately stays in the {@code unit} layer: no Minecraft bootstrap,
 * no registries — only {@link NBTTagCompound} which is a plain in-memory data
 * carrier.
 */
public class PlanetWeatherStateTest {

    @Test
    public void planetWeatherStateDefaultsStable() {
        PlanetWeatherState state = new PlanetWeatherState();
        assertEquals("fresh state: cleanWeatherTime", 0, state.getCleanWeatherTime());
        assertEquals("fresh state: rainTime", 0, state.getRainTime());
        assertEquals("fresh state: thunderTime", 0, state.getThunderTime());
        assertFalse("fresh state: raining", state.isRaining());
        assertFalse("fresh state: thundering", state.isThundering());
        assertFalse("fresh state: lastSyncedRaining", state.wasLastSyncedRaining());
        assertFalse("fresh state: lastSyncedThundering", state.wasLastSyncedThundering());
    }

    @Test
    public void planetWeatherStateNbtRoundTrip() {
        PlanetWeatherState source = new PlanetWeatherState();
        source.setCleanWeatherTime(1234);
        source.setRainTime(5678);
        source.setThunderTime(9012);
        source.setRaining(true);
        source.setThundering(true);

        NBTTagCompound tag = new NBTTagCompound();
        source.writeToNBT(tag);

        PlanetWeatherState round = new PlanetWeatherState();
        round.readFromNBT(tag);

        assertEquals(1234, round.getCleanWeatherTime());
        assertEquals(5678, round.getRainTime());
        assertEquals(9012, round.getThunderTime());
        assertEquals(true, round.isRaining());
        assertEquals(true, round.isThundering());
    }

    @Test
    public void planetWeatherStateNbtRoundTripPreservesClearWeather() {
        // Distinct from raining round-trip — guards against the trivial impl
        // bug where false booleans are serialised as missing keys.
        PlanetWeatherState source = new PlanetWeatherState();
        source.setRaining(false);
        source.setThundering(false);
        source.setCleanWeatherTime(20000);

        NBTTagCompound tag = new NBTTagCompound();
        source.writeToNBT(tag);

        PlanetWeatherState round = new PlanetWeatherState();
        round.readFromNBT(tag);

        assertFalse(round.isRaining());
        assertFalse(round.isThundering());
        assertEquals(20000, round.getCleanWeatherTime());
    }

    @Test
    public void lastSyncedFlagsAreSettable() {
        // lastSynced* are transient (not in NBT) and only used by the manager
        // to detect edge transitions for explicit client packets — but the
        // setter/getter pair must still behave like a plain flag pair.
        PlanetWeatherState state = new PlanetWeatherState();
        state.markSyncedRaining(true);
        state.markSyncedThundering(true);

        assertEquals(true, state.wasLastSyncedRaining());
        assertEquals(true, state.wasLastSyncedThundering());

        state.markSyncedRaining(false);
        assertFalse(state.wasLastSyncedRaining());
    }
}
