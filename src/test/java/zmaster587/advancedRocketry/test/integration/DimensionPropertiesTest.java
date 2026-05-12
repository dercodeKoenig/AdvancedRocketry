package zmaster587.advancedRocketry.test.integration;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * §6.2 DimensionProperties domain logic — defaults, NBT round-trip, hierarchy.
 *
 * Tests stay clear of biome / ocean-block / filler-block round-trip because those
 * pull from {@code Block.REGISTRY} / {@code AdvancedRocketryBiomes.instance} which
 * require the AR registry pipeline. Those branches are exercised in §7.4.
 */
public class DimensionPropertiesTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    /** starId / parentPlanet are package-private; tests use reflection. */
    private static void setIntField(Object target, String name, int value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.setInt(target, value);
        } catch (Exception e) {
            throw new AssertionError("Reflection failed setting " + name, e);
        }
    }

    private static int getIntField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(target);
        } catch (Exception e) {
            throw new AssertionError("Reflection failed reading " + name, e);
        }
    }

    private static DimensionProperties earthLike() {
        DimensionProperties props = new DimensionProperties(9001, "TestEarthLike");
        props.gravitationalMultiplier = 1.0f;
        props.orbitalDist = 100;
        props.rotationalPeriod = 24000;
        props.setAtmosphereDensityDirect(100);
        props.skyColor = new float[]{0.5f, 0.7f, 1.0f};
        props.fogColor = new float[]{0.6f, 0.6f, 0.6f};
        props.hasOxygen = true;
        return props;
    }

    @Test
    public void dimensionPropertiesDefaultsAreStable() {
        DimensionProperties props = new DimensionProperties(42);

        assertEquals("Temp", props.getName());
        assertEquals(1.0f, props.getGravitationalMultiplier(), 1e-6);
        assertEquals(100, props.orbitalDist);
        assertEquals(24000, props.rotationalPeriod);
        assertEquals(63, props.getSeaLevel());
        assertTrue(props.hasOxygen);
        assertTrue(props.isNativeDimension);
        assertFalse(props.hasRings);
        assertFalse(props.isGasGiant());

        // Default colors are non-null per resetProperties.
        assertNotNull(props.fogColor);
        assertNotNull(props.skyColor);
        assertNotNull(props.ringColor);
        assertNotNull(props.sunriseSunsetColors);
    }

    @Test
    public void nbtRoundTripPreservesPlanetIdentity() {
        DimensionProperties original = earthLike();
        // starId=0 (Sol) is the only star MinecraftBootstrap registers. Production
        // saves only refer to stars that DimensionManager already knows about, so
        // this matches the in-game contract.
        setIntField(original, "starId", 0);
        setIntField(original, "parentPlanet", -1);

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        DimensionProperties restored = DimensionProperties.createFromNBT(9001, nbt);

        assertEquals(original.getId(), restored.getId());
        assertEquals("TestEarthLike", restored.getName());
        assertEquals(0, restored.getStarId());
        assertEquals(original.getGravitationalMultiplier(), restored.getGravitationalMultiplier(), 1e-6);
        assertEquals(original.orbitalDist, restored.orbitalDist);
        assertEquals(original.rotationalPeriod, restored.rotationalPeriod);
        assertEquals(original.getAtmosphereDensity(), restored.getAtmosphereDensity());
    }

    @Test
    public void nbtRoundTripPreservesWeatherConfig() {
        DimensionProperties original = new DimensionProperties(9002, "WeatherWorld");
        original.rainStartLength = 50_000;
        original.thunderStartLength = 80_000;
        original.rainProlongationLength = 2_500;
        original.thunderProlongationLength = 4_000;
        original.setRainMarker(1);
        original.setThunderMarker(-1);

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        DimensionProperties restored = DimensionProperties.createFromNBT(9002, nbt);

        assertEquals(50_000, restored.rainStartLength);
        assertEquals(80_000, restored.thunderStartLength);
        assertEquals(2_500, restored.rainProlongationLength);
        assertEquals(4_000, restored.thunderProlongationLength);
        assertEquals(1, restored.getRainMarker());
        assertEquals(-1, restored.getThunderMarker());
    }

    @Test
    public void nbtRoundTripPreservesGenerationFlags() {
        DimensionProperties original = new DimensionProperties(9003, "GenWorld");
        original.setGenerateCraters(false);
        original.setGenerateGeodes(false);
        original.setGenerateStructures(false);
        original.setGenerateVolcanos(true);
        original.setGenerateCaves(false);
        original.hasRivers = false;
        original.setCraterMultiplier(0.25f);
        original.setVolcanoMultiplier(3.0f);
        original.setGeodeMultiplier(1.75f);

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        DimensionProperties restored = DimensionProperties.createFromNBT(9003, nbt);

        assertFalse(restored.canGenerateCraters());
        assertFalse(restored.canGenerateGeodes());
        assertFalse(restored.canGenerateStructures());
        assertTrue(restored.canGenerateVolcanos());
        assertFalse(restored.canGenerateCaves());
        assertFalse(restored.hasRivers);
        assertEquals(0.25f, restored.getCraterMultiplier(), 1e-6);
        assertEquals(3.0f, restored.getVolcanoMultiplier(), 1e-6);
        // Asserting the FIELD via reflection (NBT round-trip is correct for the
        // wire). getGeodeMultiplier() is buggy — see
        // getGeodeMultiplierReturnsVolcanoMultiplier_documented below.
        try {
            java.lang.reflect.Field f = restored.getClass().getDeclaredField("geodeFrequencyMultiplier");
            f.setAccessible(true);
            assertEquals(1.75f, f.getFloat(restored), 1e-6);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Documents an existing latent bug (do NOT fix in this PR — see SMART §3).
     *
     * {@code getGeodeMultiplier()} returns {@code volcanoFrequencyMultiplier}
     * instead of {@code geodeFrequencyMultiplier} (copy-paste error at
     * DimensionProperties.java:2153). The on-wire field is correct, only the
     * accessor is wrong. Production callers that read geode multiplier through
     * this getter will silently observe the volcano value.
     */
    @Test
    public void getGeodeMultiplierReturnsVolcanoMultiplier_documented() {
        DimensionProperties props = new DimensionProperties(8888, "BuggyGetter");
        props.setGeodeMultiplier(2.0f);
        props.setVolcanoMultiplier(7.0f);

        // Bug: the geode getter ignores the geode field and returns the volcano field.
        assertEquals("BUG: getGeodeMultiplier returns volcanoFrequencyMultiplier",
                7.0f, props.getGeodeMultiplier(), 1e-6);
    }

    @Test
    public void nbtRoundTripPreservesRings() {
        DimensionProperties original = new DimensionProperties(9004, "RingedPlanet");
        original.hasRings = true;
        original.ringAngle = 45;
        original.ringColor = new float[]{0.9f, 0.1f, 0.2f};

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        DimensionProperties restored = DimensionProperties.createFromNBT(9004, nbt);

        assertTrue(restored.hasRings);
        assertEquals(45, restored.ringAngle);
        assertEquals(0.9f, restored.ringColor[0], 1e-6);
        assertEquals(0.1f, restored.ringColor[1], 1e-6);
        assertEquals(0.2f, restored.ringColor[2], 1e-6);
    }

    @Test
    public void nbtRoundTripPreservesSkyAndFogColors() {
        DimensionProperties original = new DimensionProperties(9005, "ColorWorld");
        original.skyColor = new float[]{0.1f, 0.2f, 0.3f};
        original.fogColor = new float[]{0.4f, 0.5f, 0.6f};
        original.sunriseSunsetColors = new float[]{0.7f, 0.8f, 0.9f, 1.0f};

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        DimensionProperties restored = DimensionProperties.createFromNBT(9005, nbt);

        assertEquals(0.1f, restored.skyColor[0], 1e-6);
        assertEquals(0.2f, restored.skyColor[1], 1e-6);
        assertEquals(0.3f, restored.skyColor[2], 1e-6);
        assertEquals(0.4f, restored.fogColor[0], 1e-6);
        assertEquals(0.7f, restored.sunriseSunsetColors[0], 1e-6);
    }

    @Test
    public void setAtmosphereDensityDirectDoesNotCorruptIdOrHierarchy() {
        DimensionProperties props = new DimensionProperties(123, "Mars");
        setIntField(props, "starId", 5);
        setIntField(props, "parentPlanet", -1);

        props.setAtmosphereDensityDirect(42);

        // Identity invariants survive density mutation.
        assertEquals(123, props.getId());
        assertEquals("Mars", props.getName());
        assertEquals(5, props.getStarId());
        assertEquals(-1, getIntField(props, "parentPlanet"));
        assertEquals(42, props.getAtmosphereDensity());
    }

    @Test
    public void emptyNbtRoundTripUsesPostConstructorDefaults() {
        DimensionProperties original = new DimensionProperties(9999);

        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        DimensionProperties restored = DimensionProperties.createFromNBT(9999, nbt);

        // Default name written by constructor = "Temp".
        assertEquals("Temp", restored.getName());
        // Default density from resetProperties = 100.
        assertEquals(100, restored.getAtmosphereDensity());
        assertEquals(24000, restored.rotationalPeriod);
    }
}
