package zmaster587.advancedRocketry.test.integration;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldInfo;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;
import zmaster587.advancedRocketry.world.weather.ARWeatherWorldInfo;
import zmaster587.advancedRocketry.world.weather.PlanetWeatherState;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * SMART §6.10 (4-7) — {@link ARWeatherWorldInfo} delegation contract.
 *
 * <ul>
 *   <li>(4) non-weather getters route to the delegate;</li>
 *   <li>(5) weather setters route only to the state, not the delegate;</li>
 *   <li>(6) {@code getWorldTime} stays on the delegate (day/night must not
 *       diverge between planet and overworld in this iteration);</li>
 *   <li>(7) weather mutations fire the dirty callback.</li>
 * </ul>
 *
 * Lives in the integration layer because constructing a vanilla {@link WorldInfo}
 * touches {@code GameRules} which requires {@code Bootstrap.register()}.
 */
public class ARWeatherWorldInfoTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    private static WorldInfo seededDelegate() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong("RandomSeed", 4242L);
        nbt.setString("LevelName", "DelegateLevel");
        nbt.setLong("Time", 17000L);
        nbt.setLong("DayTime", 17000L);
        nbt.setInteger("SpawnX", 11);
        nbt.setInteger("SpawnY", 64);
        nbt.setInteger("SpawnZ", 22);
        return new WorldInfo(nbt);
    }

    private static ARWeatherWorldInfo wrap(WorldInfo delegate, PlanetWeatherState state, Runnable dirty) {
        return new ARWeatherWorldInfo(delegate, state, dirty);
    }

    @Test
    public void arWeatherWorldInfoDelegatesNonWeatherFields() {
        WorldInfo delegate = seededDelegate();
        PlanetWeatherState state = new PlanetWeatherState();
        ARWeatherWorldInfo wrapper = wrap(delegate, state, () -> {});

        assertEquals("seed must come from delegate", 4242L, wrapper.getSeed());
        assertEquals("worldName must come from delegate", "DelegateLevel", wrapper.getWorldName());
        assertEquals("spawnX delegated", 11, wrapper.getSpawnX());
        assertEquals("spawnY delegated", 64, wrapper.getSpawnY());
        assertEquals("spawnZ delegated", 22, wrapper.getSpawnZ());
        assertNotNull("game rules delegated", wrapper.getGameRulesInstance());
        assertSame("same game rules instance as delegate (no fresh GameRules)",
                delegate.getGameRulesInstance(), wrapper.getGameRulesInstance());
    }

    @Test
    public void arWeatherWorldInfoOverridesOnlyWeatherFields() {
        WorldInfo delegate = seededDelegate();
        PlanetWeatherState state = new PlanetWeatherState();
        ARWeatherWorldInfo wrapper = wrap(delegate, state, () -> {});

        // Pre-seed delegate weather to a DIFFERENT value than the wrapper —
        // proves the wrapper reads state, not delegate.
        delegate.setRaining(true);
        delegate.setRainTime(99999);
        delegate.setThundering(true);
        delegate.setThunderTime(99999);
        delegate.setCleanWeatherTime(99999);

        state.setRaining(false);
        state.setRainTime(123);
        state.setThundering(false);
        state.setThunderTime(456);
        state.setCleanWeatherTime(789);

        assertFalse("wrapper.isRaining reads state, not delegate", wrapper.isRaining());
        assertEquals(123, wrapper.getRainTime());
        assertFalse("wrapper.isThundering reads state, not delegate", wrapper.isThundering());
        assertEquals(456, wrapper.getThunderTime());
        assertEquals(789, wrapper.getCleanWeatherTime());

        // Writes through the wrapper must update state, not the delegate.
        wrapper.setRaining(true);
        wrapper.setRainTime(42);
        wrapper.setThundering(true);
        wrapper.setThunderTime(43);
        wrapper.setCleanWeatherTime(44);

        assertTrue("state.raining after wrapper.setRaining(true)", state.isRaining());
        assertEquals(42, state.getRainTime());
        assertTrue(state.isThundering());
        assertEquals(43, state.getThunderTime());
        assertEquals(44, state.getCleanWeatherTime());

        // Delegate's weather is untouched (still the pre-seeded values).
        assertTrue("delegate.raining unchanged by wrapper write", delegate.isRaining());
        assertEquals(99999, delegate.getRainTime());
        assertTrue(delegate.isThundering());
        assertEquals(99999, delegate.getThunderTime());
        assertEquals(99999, delegate.getCleanWeatherTime());
    }

    @Test
    public void arWeatherWorldInfoDoesNotOverrideWorldTime() {
        WorldInfo delegate = seededDelegate();
        ARWeatherWorldInfo wrapper = wrap(delegate, new PlanetWeatherState(), () -> {});

        // Day/night currently must NOT diverge between planet and overworld
        // (SMART §10) — getWorldTime / getWorldTotalTime stay on delegate.
        assertEquals("worldTime stays on delegate", delegate.getWorldTime(), wrapper.getWorldTime());
        assertEquals("worldTotalTime stays on delegate",
                delegate.getWorldTotalTime(), wrapper.getWorldTotalTime());

        delegate.setWorldTotalTime(50_000L);
        assertEquals("delegate worldTotalTime change visible through wrapper",
                50_000L, wrapper.getWorldTotalTime());
    }

    @Test
    public void arWeatherWorldInfoMarksDirtyOnWeatherMutation() {
        WorldInfo delegate = seededDelegate();
        PlanetWeatherState state = new PlanetWeatherState();
        AtomicInteger dirtyHits = new AtomicInteger();
        ARWeatherWorldInfo wrapper = wrap(delegate, state, dirtyHits::incrementAndGet);

        wrapper.setRaining(true);
        wrapper.setRainTime(1);
        wrapper.setThundering(true);
        wrapper.setThunderTime(1);
        wrapper.setCleanWeatherTime(1);

        assertEquals("every weather setter must trigger the dirty callback exactly once",
                5, dirtyHits.get());
    }

    @Test
    public void arWeatherWorldInfoDoesNotFireDirtyOnNonWeatherCalls() {
        // Non-weather setters are no-op / pass-through and must not pretend
        // anything happened from the weather subsystem's POV.
        WorldInfo delegate = seededDelegate();
        AtomicInteger dirtyHits = new AtomicInteger();
        ARWeatherWorldInfo wrapper = wrap(delegate, new PlanetWeatherState(), dirtyHits::incrementAndGet);

        wrapper.setWorldName("ignored");
        wrapper.setSaveVersion(7);
        wrapper.setWorldTotalTime(100L);

        assertEquals("non-weather mutations must NOT mark weather saved-data dirty",
                0, dirtyHits.get());
    }

    @Test
    public void getDelegateExposesUnderlyingForUnwrap() {
        WorldInfo delegate = seededDelegate();
        ARWeatherWorldInfo wrapper = wrap(delegate, new PlanetWeatherState(), () -> {});
        assertSame(delegate, wrapper.getDelegate());
    }
}
