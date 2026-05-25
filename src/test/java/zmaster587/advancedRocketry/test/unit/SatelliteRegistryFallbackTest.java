package zmaster587.advancedRocketry.test.unit;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.Test;
import zmaster587.advancedRocketry.api.SatelliteRegistry;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Coverage-audit gap (post-TASK-26): save-compatibility fallback for an
 * unregistered satellite type.
 *
 * <p>{@link SatelliteRegistry#getNewSatellite(String)} is the dispatch
 * point for "load a satellite from NBT" (called by
 * {@link SatelliteRegistry#createFromNBT(NBTTagCompound)}). Its javadoc
 * promises a {@link zmaster587.advancedRocketry.satellite.SatelliteDefunct}
 * fallback for unknown type ids — that's the documented save-compatibility
 * contract: a save containing a satellite whose type was registered by a
 * companion mod that's been removed from the modpack must still load,
 * producing an inert "Offline Satellite" placeholder.</p>
 *
 * <p><b>Current production behaviour (≠ javadoc)</b>: the method
 * returns {@code null} for an unknown type, and
 * {@code createFromNBT} immediately dereferences {@code null} →
 * {@code NullPointerException}. The shipping save-load path
 * ({@link zmaster587.advancedRocketry.dimension.DimensionProperties#readFromNBT})
 * catches the NPE in a {@code try / catch(NullPointerException)} and
 * silently drops the satellite — so the save still loads — but other
 * callers ({@link zmaster587.advancedRocketry.network.PacketSatellite#readClient}
 * and {@link zmaster587.advancedRocketry.entity.EntityRocket#readEntityFromNBT})
 * lack that catch and will propagate the NPE to their callers.</p>
 *
 * <p>This test pins the <b>current (buggy) contract</b> so a future fix
 * (return {@code SatelliteDefunct} from {@code getNewSatellite}, or add
 * a null-guard in {@code createFromNBT}) flips the assertion and forces
 * a re-evaluation. Ledgered as a known bug — see
 * {@code .agent/history/known-bugs-ledger.md} Batch #2.</p>
 *
 * <p><b>Why log this bug</b>: the player-visible scenario is "join a
 * server using a different mod set than the save was created with" →
 * NPE on packet handler → client disconnect or crash. Low-probability
 * (modpack-author hygiene usually prevents this) but real.</p>
 */
public class SatelliteRegistryFallbackTest {

    /** Minimal SatelliteBase stand-in for the positive control. The real
     *  satellite classes (SatelliteBiomeChanger, etc.) hit
     *  {@code Biome.getBiome(0)} in their no-arg ctor which requires the
     *  Minecraft Bootstrap to have run — fine in TASK-09's mod-init
     *  context, not fine in pure unit-tier. Mirrors the
     *  {@code TestSatellite} class in SatellitePropertiesTest. */
    public static class TestStandInSatellite extends SatelliteBase {
        @Override public String getInfo(World world) { return "test"; }
        @Override public String getName() { return "test_satellite"; }
        @Override public boolean performAction(EntityPlayer player, World world, BlockPos pos) { return false; }
        @Override public double failureChance() { return 0.0d; }
    }

    private static final String KNOWN_TYPE_KEY =
            "ar:gap4_known_type_for_positive_control";

    /** Documents the bug: unknown type name returns null, contradicting
     *  the {@code getNewSatellite} javadoc that promises SatelliteDefunct. */
    @Test
    public void unknownSatelliteTypeReturnsNullInsteadOfDefunct_documentsKnownBug() {
        SatelliteBase result = SatelliteRegistry.getNewSatellite(
                "advancedrocketry:nonexistent.satellite.type.for.gap4.test");
        // Production-currently: null. Expected per javadoc: SatelliteDefunct.
        // When the bug is fixed, this assertion fires and the test must be
        // updated to assertNotNull + class check.
        assertNull("getNewSatellite javadoc promises SatelliteDefunct fallback "
                        + "but production returns null. Fix candidate: "
                        + "SatelliteRegistry.java:97 — replace `return null` "
                        + "with `return new SatelliteDefunct()`.",
                result);
    }

    /** Documents the downstream consequence: createFromNBT NPEs on
     *  unregistered type because it doesn't guard against the null
     *  returned by getNewSatellite. */
    @Test
    public void createFromNBTWithUnknownTypeThrowsNPE_documentsKnownBug() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("dataType",
                "advancedrocketry:nonexistent.satellite.type.for.gap4.test");
        try {
            SatelliteRegistry.createFromNBT(nbt);
            org.junit.Assert.fail("createFromNBT should fall back to "
                    + "SatelliteDefunct per getNewSatellite's javadoc; "
                    + "instead it NPEs on the null returned from the "
                    + "registry. Fix candidate: SatelliteRegistry.java:84 — "
                    + "guard against null before satellite.readFromNBT(nbt).");
        } catch (NullPointerException expected) {
            // Current production behaviour — pinning until the bug is fixed.
        }
    }

    /** Positive control: a KNOWN satellite type produces a real instance —
     *  pins that the registry dispatch works for the happy path so the
     *  two _documentsKnownBug tests can't pass by registry-wide breakage.
     *  Uses a unit-tier-friendly stand-in (no Bootstrap dependency). */
    @Test
    public void knownSatelliteTypeProducesNonNullInstance() {
        SatelliteRegistry.registerSatellite(KNOWN_TYPE_KEY, TestStandInSatellite.class);
        SatelliteBase result = SatelliteRegistry.getNewSatellite(KNOWN_TYPE_KEY);
        assertNotNull("registered type must resolve via SatelliteRegistry — "
                        + "if this fails the registry itself is broken "
                        + "(independent of the SatelliteDefunct gap)",
                result);
    }
}
