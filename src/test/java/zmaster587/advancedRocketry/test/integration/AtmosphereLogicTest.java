package zmaster587.advancedRocketry.test.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * §6.8 Atmosphere — pure-logic checks on AtmosphereType subtypes.
 *
 * Loading {@code AtmosphereType} runs its static initializer which registers
 * atmospheres into {@code AtmosphereRegister}. We trigger MC bootstrap defensively
 * because some atmosphere subclasses reference vanilla blocks transitively.
 */
public class AtmosphereLogicTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    public void airIsBreathable() {
        assertTrue(AtmosphereType.AIR.isBreathable());
        assertTrue("normal air must allow combustion (torches burn)", AtmosphereType.AIR.allowsCombustion());
    }

    @Test
    public void pressurizedAirIsBreathable() {
        assertTrue(AtmosphereType.PRESSURIZEDAIR.isBreathable());
    }

    @Test
    public void vacuumIsNotBreathable() {
        assertFalse(AtmosphereType.VACUUM.isBreathable());
        assertFalse("vacuum must not support combustion", AtmosphereType.VACUUM.allowsCombustion());
    }

    @Test
    public void noOxygenAtmospheresAreNotBreathable() {
        assertFalse(AtmosphereType.NOO2.isBreathable());
        assertFalse(AtmosphereType.HIGHPRESSURENOO2.isBreathable());
        assertFalse(AtmosphereType.SUPERHIGHPRESSURENOO2.isBreathable());
        assertFalse(AtmosphereType.VERYHOTNOO2.isBreathable());
        assertFalse(AtmosphereType.SUPERHEATEDNOO2.isBreathable());
    }

    @Test
    public void hostileAtmospheresHaveTickingEnabled() {
        // Atmospheres that damage / affect entities every tick must report canTick.
        assertTrue("vacuum ticks for suffocation damage", AtmosphereType.VACUUM.canTick());
        assertTrue("LowO2 ticks for nausea/damage", AtmosphereType.LOWOXYGEN.canTick());
        assertTrue("HighPressure ticks", AtmosphereType.HIGHPRESSURE.canTick());
        assertTrue("VeryHot ticks", AtmosphereType.VERYHOT.canTick());
    }

    @Test
    public void breathableAtmospheresDoNotTick() {
        assertFalse("Breathable AIR is not expected to tick effects", AtmosphereType.AIR.canTick());
        assertFalse("PressurizedAir does not tick", AtmosphereType.PRESSURIZEDAIR.canTick());
    }

    @Test
    public void atmosphereNamesArePreservedFromConstructor() {
        assertEquals("air", AtmosphereType.AIR.getUnlocalizedName());
        assertEquals("PressurizedAir", AtmosphereType.PRESSURIZEDAIR.getUnlocalizedName());
        assertEquals("lowO2", AtmosphereType.LOWOXYGEN.getUnlocalizedName());
        assertEquals("NoO2", AtmosphereType.NOO2.getUnlocalizedName());
    }

    @Test
    public void breathableSetterFlipsBreathableFlag() {
        // Local instance — DO NOT mutate the singleton AIR / VACUUM, that would leak
        // into other tests.
        AtmosphereType local = new AtmosphereType(false, true, "ar.test.local." + System.nanoTime());
        assertTrue(local.isBreathable());

        local.setIsBreathable(false);
        assertFalse(local.isBreathable());
    }

    @Test
    public void allowsCombustionSetterIsIndependentOfBreathable() {
        AtmosphereType local = new AtmosphereType(false, false, true, "ar.test.combust." + System.nanoTime());
        assertFalse(local.isBreathable());
        assertTrue("constructor must keep combustion flag distinct from breathable", local.allowsCombustion());

        local.setAllowsCombustion(false);
        assertFalse(local.allowsCombustion());
    }
}
