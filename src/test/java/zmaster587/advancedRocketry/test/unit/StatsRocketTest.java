package zmaster587.advancedRocketry.test.unit;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.StatsRocket;
import zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * §6.4 StatsRocket NBT round-trip and fuel arithmetic.
 *
 * Tests intentionally manipulate per-field state directly (not via getThrust /
 * getFuelRate which apply ARConfiguration multipliers — those would mask field
 * round-trip with config defaults). The thrust multiplier is fixed at 1.0 here so
 * `getThrust()` returns the underlying field value.
 */
public class StatsRocketTest {

    @BeforeClass
    public static void primeConfig() {
        // Ensure the multiplier-applying getters are observable. ARConfiguration's
        // public field defaults to 0 (no @ConfigProperty default) which would make
        // getThrust() always return 0, which is correct in production but masks our
        // per-field assertions.
        ARConfiguration.getCurrentConfig().rocketThrustMultiplier = 1.0;
        ARConfiguration.getCurrentConfig().rocketRequireFuel = true;
    }

    private static StatsRocket sample() {
        StatsRocket stats = new StatsRocket();
        stats.setThrust(12345);
        stats.setWeight(987.5f);
        stats.setDrillingPower(0.75f);
        stats.setFuelFluid("ar:test_fuel");
        stats.setOxidizerFluid("ar:test_ox");
        stats.setWorkingFluid("ar:test_working");
        for (FuelType type : FuelType.values()) {
            int seed = type.ordinal() + 1;
            stats.setFuelCapacity(type, seed * 1000);
            stats.setFuelAmount(type, seed * 100);
            stats.setFuelRate(type, seed * 5);
            stats.setBaseFuelRate(type, seed * 7);
        }
        stats.setSeatLocation(3, 4, 5);
        stats.addPassengerSeat(6, 7, 8);
        stats.addEngineLocation(1.5f, 2.5f, 3.5f);
        return stats;
    }

    @Test
    public void statsRocketNbtRoundTrip() {
        StatsRocket original = sample();
        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        // Use the canonical production path: readFromNBT(outer). createFromNBT is
        // currently broken (double-unwrap of TAGNAME) — see
        // createFromNbtIsBrokenDueToDoubleUnwrap below.
        StatsRocket restored = new StatsRocket();
        restored.readFromNBT(nbt);

        assertEquals(original.getThrust(), restored.getThrust());
        assertEquals(original.getWeight_NoFuel(), restored.getWeight_NoFuel(), 1e-6);
        assertEquals(original.getDrillingPower(), restored.getDrillingPower(), 1e-6);
        assertEquals(original.getFuelFluid(), restored.getFuelFluid());
        assertEquals(original.getOxidizerFluid(), restored.getOxidizerFluid());
        assertEquals(original.getWorkingFluid(), restored.getWorkingFluid());
        for (FuelType type : FuelType.values()) {
            assertEquals("amount " + type, original.getFuelAmount(type), restored.getFuelAmount(type));
            assertEquals("capacity " + type, original.getFuelCapacity(type), restored.getFuelCapacity(type));
            assertEquals("rate " + type, original.getFuelRate(type), restored.getFuelRate(type));
            assertEquals("baseRate " + type, original.getBaseFuelRate(type), restored.getBaseFuelRate(type));
        }
        assertTrue(restored.hasSeat());
        assertEquals(3, restored.getSeatX());
        assertEquals(4, restored.getSeatY());
        assertEquals(5, restored.getSeatZ());
        // sample() does setSeatLocation(3,4,5) (pilot only) + addPassengerSeat(6,7,8)
        // (one passenger). setSeatLocation does not register a passenger seat.
        assertEquals(1, restored.getNumPassengerSeats());
    }

    @Test
    public void fuelCannotExceedCapacity() {
        StatsRocket stats = new StatsRocket();
        stats.setFuelCapacity(FuelType.LIQUID_MONOPROPELLANT, 1000);

        int added = stats.addFuelAmount(FuelType.LIQUID_MONOPROPELLANT, 5_000);
        assertEquals("addFuelAmount must clamp to remaining capacity", 1000, added);
        assertEquals(1000, stats.getFuelAmount(FuelType.LIQUID_MONOPROPELLANT));

        int addedAgain = stats.addFuelAmount(FuelType.LIQUID_MONOPROPELLANT, 1);
        assertEquals("once full, addFuelAmount must return 0", 0, addedAgain);
    }

    @Test
    public void fuelCanGoNegativeViaSetButNotViaAdd() {
        // Document the actual behavior: setFuelAmount is a raw setter (no clamp),
        // addFuelAmount clamps to capacity. If clamping is desired in setFuelAmount,
        // that's a behavior change and should land in a separate PR.
        StatsRocket stats = new StatsRocket();
        stats.setFuelCapacity(FuelType.LIQUID_BIPROPELLANT, 100);

        stats.setFuelAmount(FuelType.LIQUID_BIPROPELLANT, -50);
        assertEquals(-50, stats.getFuelAmount(FuelType.LIQUID_BIPROPELLANT));

        stats.setFuelAmount(FuelType.LIQUID_BIPROPELLANT, 0);
        int added = stats.addFuelAmount(FuelType.LIQUID_BIPROPELLANT, 200);
        assertEquals("addFuelAmount clamps to capacity", 100, added);
    }

    @Test
    public void bipropellantOxidizerHandledSeparately() {
        StatsRocket stats = new StatsRocket();
        stats.setFuelCapacity(FuelType.LIQUID_BIPROPELLANT, 500);
        stats.setFuelCapacity(FuelType.LIQUID_OXIDIZER, 250);

        stats.addFuelAmount(FuelType.LIQUID_BIPROPELLANT, 500);
        stats.addFuelAmount(FuelType.LIQUID_OXIDIZER, 250);

        assertEquals(500, stats.getFuelAmount(FuelType.LIQUID_BIPROPELLANT));
        assertEquals(250, stats.getFuelAmount(FuelType.LIQUID_OXIDIZER));
        // Each fuel type maintains its own capacity envelope.
        assertEquals(0, stats.addFuelAmount(FuelType.LIQUID_BIPROPELLANT, 1));
        assertEquals(0, stats.addFuelAmount(FuelType.LIQUID_OXIDIZER, 1));
    }

    @Test
    public void thrustMultiplierApplied() {
        try {
            StatsRocket stats = new StatsRocket();
            stats.setThrust(100);

            ARConfiguration.getCurrentConfig().rocketThrustMultiplier = 1.0;
            assertEquals(100, stats.getThrust());

            ARConfiguration.getCurrentConfig().rocketThrustMultiplier = 2.5;
            assertEquals("getThrust must scale by ARConfiguration.rocketThrustMultiplier",
                    250, stats.getThrust());
        } finally {
            ARConfiguration.getCurrentConfig().rocketThrustMultiplier = 1.0;
        }
    }

    @Test
    public void seatCountPreserved() {
        StatsRocket stats = new StatsRocket();
        stats.addPassengerSeat(0, 0, 0);
        stats.addPassengerSeat(1, 2, 3);
        stats.addPassengerSeat(4, 5, 6);

        NBTTagCompound nbt = new NBTTagCompound();
        stats.writeToNBT(nbt);
        StatsRocket restored = new StatsRocket();
        restored.readFromNBT(nbt);

        assertEquals(stats.getNumPassengerSeats(), restored.getNumPassengerSeats());
        assertTrue(restored.hasSeat());
    }

    @Test
    public void emptyStatsRocketNbtRoundTrip() {
        StatsRocket original = new StatsRocket();
        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        StatsRocket restored = new StatsRocket();
        restored.readFromNBT(nbt);

        assertFalse(restored.hasSeat());
        assertEquals(0, restored.getNumPassengerSeats());
        for (FuelType type : FuelType.values()) {
            assertEquals(0, restored.getFuelAmount(type));
            assertEquals(0, restored.getFuelCapacity(type));
        }
    }

    @Test
    public void createFromNbtWithoutTagReturnsDefaults() {
        StatsRocket fresh = StatsRocket.createFromNBT(new NBTTagCompound());

        assertFalse(fresh.hasSeat());
        for (FuelType type : FuelType.values()) {
            assertEquals(0, fresh.getFuelAmount(type));
        }
    }

    /**
     * Documents an existing latent bug (do NOT fix in this PR — see SMART §3).
     *
     * {@code createFromNBT(outer)} extracts the inner {@code rocketStats} compound
     * and passes it to {@code readFromNBT(inner)}, but {@code readFromNBT} also
     * tries to unwrap the same tag — so when called via {@code createFromNBT},
     * the inner check fails and no fields are restored. Production code never
     * triggers this because every caller uses {@code stats.readFromNBT(outer)}
     * directly (e.g. {@code TileRocketAssemblingMachine:690},
     * {@code EntityRocket:1997}). Marked as expected behavior here so a future
     * fix to the contract surfaces this test as a tripwire.
     */
    @Test
    public void createFromNbtCurrentlyLosesAllFields_documented() {
        StatsRocket original = sample();
        NBTTagCompound nbt = new NBTTagCompound();
        original.writeToNBT(nbt);

        StatsRocket restored = StatsRocket.createFromNBT(nbt);

        // Restored ends up with reset() defaults — thrust=0, no seat, no fuel.
        assertEquals("BUG: createFromNBT loses thrust", 0, restored.getThrust());
        assertFalse("BUG: createFromNBT loses seat", restored.hasSeat());
        assertEquals("BUG: createFromNBT loses passenger seats", 0, restored.getNumPassengerSeats());
    }

    @Test
    public void fuelTypeSelectionPrefersExpectedFuelType() {
        // Each FuelType has its own independent backing storage. Setting one
        // type must not bleed into another — this guards against any future
        // refactor that consolidates the per-type fields into a shared map and
        // accidentally collapses keys.
        StatsRocket stats = new StatsRocket();
        stats.setFuelCapacity(FuelType.LIQUID_MONOPROPELLANT, 1000);
        stats.setFuelCapacity(FuelType.LIQUID_BIPROPELLANT, 2000);
        stats.setFuelCapacity(FuelType.LIQUID_OXIDIZER, 500);

        stats.setFuelAmount(FuelType.LIQUID_MONOPROPELLANT, 100);
        stats.setFuelAmount(FuelType.LIQUID_BIPROPELLANT, 200);
        stats.setFuelAmount(FuelType.LIQUID_OXIDIZER, 300);

        assertEquals(100, stats.getFuelAmount(FuelType.LIQUID_MONOPROPELLANT));
        assertEquals(200, stats.getFuelAmount(FuelType.LIQUID_BIPROPELLANT));
        assertEquals(300, stats.getFuelAmount(FuelType.LIQUID_OXIDIZER));
        // Other types remain at default (0) — proves storage is truly per-type.
        assertEquals(0, stats.getFuelAmount(FuelType.WARP));
        assertEquals(0, stats.getFuelAmount(FuelType.IMPULSE));
        assertEquals(0, stats.getFuelAmount(FuelType.ION));
        assertEquals(0, stats.getFuelAmount(FuelType.NUCLEAR_WORKING_FLUID));
    }

    /**
     * SMART §6.4 — {@code rocketStatsBackwardCompatibleWithOldNbt}.
     *
     * Synthesizes a minimal NBT shaped like an older save (only `thrust`/`weight`
     * + a few fuel keys, no per-type rate/capacity). Asserts {@code readFromNBT}
     * tolerates missing keys (defaults to zero) without throwing — saves from
     * earlier AR versions must not crash on load.
     */
    @Test
    public void rocketStatsBackwardCompatibleWithOldNbt() {
        NBTTagCompound stats = new NBTTagCompound();
        stats.setInteger("thrust", 999);
        stats.setFloat("weight", 12.5f);
        stats.setString("fuelFluid", "ar:legacy_fuel");
        stats.setInteger("fuelMonopropellant", 250);
        // Intentionally omit: bipropellant / oxidizer / nuclear / ion / warp /
        // impulse fields, all *Capacity* keys, *Rate* keys, dynStats, engineLoc,
        // passengerSeats, playerXPos/YPos/ZPos. A pre-2.x save would lack these.

        NBTTagCompound outer = new NBTTagCompound();
        outer.setTag("rocketStats", stats);

        StatsRocket restored = new StatsRocket();
        restored.readFromNBT(outer);

        assertEquals(999, restored.getThrust());
        assertEquals(12.5f, restored.getWeight_NoFuel(), 1e-6);
        assertEquals("ar:legacy_fuel", restored.getFuelFluid());
        assertEquals(250, restored.getFuelAmount(FuelType.LIQUID_MONOPROPELLANT));
        // Missing keys default to zero — no NPE, no exception.
        for (FuelType type : new FuelType[] {
                FuelType.LIQUID_BIPROPELLANT, FuelType.LIQUID_OXIDIZER,
                FuelType.NUCLEAR_WORKING_FLUID, FuelType.ION, FuelType.WARP, FuelType.IMPULSE
        }) {
            assertEquals("missing fuel amount key for " + type + " must default to 0",
                    0, restored.getFuelAmount(type));
            assertEquals("missing capacity key for " + type + " must default to 0",
                    0, restored.getFuelCapacity(type));
        }
        // KNOWN ISSUE (do NOT fix per SMART §3, just document):
        // readFromNBT does `pilotSeatPos.x = stats.getInteger("playerXPos")`, which
        // returns 0 when the key is absent rather than the INVALID_SEAT sentinel
        // (Integer.MIN_VALUE) initialized by reset(). Result: legacy saves without
        // seat keys load as "seat at (0,0,0)" with hasSeat()=true. In practice
        // production saves always write the seat keys (writeToNBT is unconditional),
        // so the bug is only reachable via NBT crafted by hand or by very old saves.
        assertTrue("legacy NBT without seat keys: hasSeat() returns true because the "
                + "missing-key default (0) collides with a valid seat coordinate",
                restored.hasSeat());
        assertEquals(0, restored.getSeatX());
        assertEquals(0, restored.getNumPassengerSeats()); // passenger list still empty
    }

    @Test
    public void copyProducesIndependentInstance() {
        StatsRocket original = new StatsRocket();
        original.setThrust(500);
        original.setWeight(50f);
        original.setFuelCapacity(FuelType.LIQUID_MONOPROPELLANT, 1000);
        original.setFuelAmount(FuelType.LIQUID_MONOPROPELLANT, 800);

        StatsRocket copy = original.copy();

        // Mutating original must not leak into the copy.
        original.setFuelAmount(FuelType.LIQUID_MONOPROPELLANT, 100);
        original.setWeight(999f);

        assertEquals(800, copy.getFuelAmount(FuelType.LIQUID_MONOPROPELLANT));
        assertEquals(50f, copy.getWeight_NoFuel(), 1e-6);
    }
}
