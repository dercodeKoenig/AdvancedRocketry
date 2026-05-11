package zmaster587.advancedRocketry.test.unit;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import org.junit.Test;
import zmaster587.advancedRocketry.api.fuel.FuelRegistry;
import zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * §6.5 FuelRegistry.
 *
 * Pure logic — uses raw {@link Fluid} instances (no FluidRegistry / item registry
 * required). The registry holds entries inside enum constants, so each test must
 * choose a unique fuel instance to avoid leaking state between tests in the same
 * JVM (the registry is a process-wide singleton via {@code FuelType.fuels}).
 */
public class FuelRegistryTest {

    private static final ResourceLocation STILL = new ResourceLocation("advancedrocketry", "test_still");
    private static final ResourceLocation FLOW = new ResourceLocation("advancedrocketry", "test_flow");

    private static Fluid newFluid(String name) {
        // Fluid(String, ResourceLocation, ResourceLocation) — pure data, no MC bootstrap.
        return new Fluid(name, STILL, FLOW);
    }

    @Test
    public void registerMonopropellantFuel() {
        Fluid fluid = newFluid("ar.test.monoprop." + System.nanoTime());
        boolean wasNew = FuelRegistry.instance.registerFuel(FuelType.LIQUID_MONOPROPELLANT, fluid, 1.0f);

        // FuelRegistry.registerFuel returns the inverted set.add result; new entry → false.
        assertFalse("first registration of a fluid must succeed (return inverted-set-add)", wasNew);
        assertTrue(FuelRegistry.instance.isFuel(FuelType.LIQUID_MONOPROPELLANT, fluid));
        assertEquals(1.0f, FuelRegistry.instance.getMultiplier(FuelType.LIQUID_MONOPROPELLANT, fluid), 1e-6);
    }

    @Test
    public void registerBipropellantFuelAndOxidizer() {
        Fluid fuel = newFluid("ar.test.biprop." + System.nanoTime());
        Fluid oxidizer = newFluid("ar.test.ox." + System.nanoTime());

        FuelRegistry.instance.registerFuel(FuelType.LIQUID_BIPROPELLANT, fuel, 1.0f);
        FuelRegistry.instance.registerFuel(FuelType.LIQUID_OXIDIZER, oxidizer, 1.0f);

        assertTrue(FuelRegistry.instance.isFuel(FuelType.LIQUID_BIPROPELLANT, fuel));
        assertTrue(FuelRegistry.instance.isFuel(FuelType.LIQUID_OXIDIZER, oxidizer));
        // Categories must stay distinct.
        assertFalse(FuelRegistry.instance.isFuel(FuelType.LIQUID_OXIDIZER, fuel));
        assertFalse(FuelRegistry.instance.isFuel(FuelType.LIQUID_BIPROPELLANT, oxidizer));
    }

    @Test
    public void registerNuclearWorkingFluid() {
        Fluid coolant = newFluid("ar.test.nuke." + System.nanoTime());
        FuelRegistry.instance.registerFuel(FuelType.NUCLEAR_WORKING_FLUID, coolant, 2.5f);

        assertTrue(FuelRegistry.instance.isFuel(FuelType.NUCLEAR_WORKING_FLUID, coolant));
        assertEquals(2.5f, FuelRegistry.instance.getMultiplier(FuelType.NUCLEAR_WORKING_FLUID, coolant), 1e-6);
    }

    @Test
    public void unknownFluidIsNotFuel() {
        Fluid unknown = newFluid("ar.test.unknown." + System.nanoTime());

        for (FuelType type : FuelType.values()) {
            assertFalse("freshly minted fluid must not be a fuel of " + type,
                    FuelRegistry.instance.isFuel(type, unknown));
            assertEquals("multiplier of unregistered fluid must be 0",
                    0f, FuelRegistry.instance.getMultiplier(type, unknown), 0f);
        }
    }

    @Test
    public void nullFuelTypeIsNeverFuel() {
        Fluid anyFluid = newFluid("ar.test.any." + System.nanoTime());
        assertFalse(FuelRegistry.instance.isFuel((FuelType) null, anyFluid));
        assertEquals(0f, FuelRegistry.instance.getMultiplier((FuelType) null, anyFluid), 0f);
    }

    @Test
    public void fuelMultiplierDefaultAndOverride() {
        Fluid fluid = newFluid("ar.test.mult." + System.nanoTime());
        FuelRegistry.instance.registerFuel(FuelType.IMPULSE, fluid, 3.5f);

        assertEquals(3.5f, FuelRegistry.instance.getMultiplier(FuelType.IMPULSE, fluid), 1e-6);
    }
}
