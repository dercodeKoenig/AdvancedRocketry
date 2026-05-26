package zmaster587.advancedRocketry.test.unit;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.satellite.SatelliteMicrowaveEnergy;
import zmaster587.advancedRocketry.satellite.SatelliteWeatherController;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Coverage-audit gap (2026-05-26 Tier 2 #4 + Tier 3 #8) —
 * persistent state on two satellite subtypes.
 *
 * <p>The two classes pinned here serialize state that drives
 * player-visible behaviour but had no round-trip coverage:</p>
 *
 * <ul>
 *   <li>{@link SatelliteWeatherController} — {@code mode_id},
 *       {@code last_mode_id}, {@code floodlevel} drive which of the
 *       three weather modes the satellite is running (rain / drain /
 *       flood) and at what y-level. Save/load contract: a player who
 *       configured the satellite to flood-mode at y=80 expects that
 *       configuration to survive server restart.</li>
 *   <li>{@link SatelliteMicrowaveEnergy} — {@code teir} byte is the
 *       opaque tier field that future tier mechanics will read. If
 *       it doesn't round-trip, a future feature reading it silently
 *       sees 0 on every reload.</li>
 * </ul>
 *
 * <p>Both NBT round-trips are tested at testUnit tier because the
 * production {@code writeToNBT} / {@code readFromNBT} are pure NBT
 * manipulators — no world, no server, no Forge lifecycle needed.</p>
 */
public class SatelliteWeatherAndMicrowaveNbtTest {

    @BeforeClass
    public static void bootstrap() {
        // SatelliteBase.readFromNBT touches ItemStack which transitively
        // initialises net.minecraft.init.Items — needs MC bootstrap.
        MinecraftBootstrap.ensure();
    }

    // ── SatelliteWeatherController ──────────────────────────────────────

    @Test
    public void weatherControllerNbtRoundTripPreservesModeIdLastModeIdAndFloodlevel() {
        SatelliteWeatherController src = new SatelliteWeatherController();
        src.mode_id = 2;          // flood mode
        src.last_mode_id = 1;     // last was drain mode
        src.floodlevel = 80;      // player-set flood y-level

        NBTTagCompound nbt = new NBTTagCompound();
        src.writeToNBT(nbt);

        // Save-format contract: all three keys are present after a
        // write. Their literal names are part of the save schema —
        // changing them silently breaks existing saves.
        assertEquals("mode_id key must be written",
                2, nbt.getInteger("mode_id"));
        assertEquals("last_mode_id key must be written",
                1, nbt.getInteger("last_mode_id"));
        assertEquals("floodlevel key must be written",
                80, nbt.getInteger("floodlevel"));

        SatelliteWeatherController peer = new SatelliteWeatherController();
        peer.readFromNBT(nbt);

        assertEquals("mode_id must round-trip", 2, peer.mode_id);
        assertEquals("last_mode_id must round-trip", 1, peer.last_mode_id);
        assertEquals("floodlevel must round-trip", 80, peer.floodlevel);
    }

    @Test
    public void weatherControllerNbtRoundTripPreservesFreshDefaults() {
        // A fresh satellite has mode_id=0, last_mode_id=0, floodlevel=-1
        // (the lazy sea-level fallback sentinel). The round-trip MUST
        // preserve -1 — if it accidentally normalised to 0, the
        // getFloodlevel() lazy fallback would never fire and flood-mode
        // would use the wrong y-level after a reload.
        SatelliteWeatherController src = new SatelliteWeatherController();
        assertEquals("ctor sets floodlevel=-1 sentinel",
                -1, src.floodlevel);

        NBTTagCompound nbt = new NBTTagCompound();
        src.writeToNBT(nbt);

        SatelliteWeatherController peer = new SatelliteWeatherController();
        peer.readFromNBT(nbt);

        assertEquals("default mode_id (=0) must round-trip",
                0, peer.mode_id);
        assertEquals("default last_mode_id (=0) must round-trip",
                0, peer.last_mode_id);
        assertEquals("floodlevel sentinel (-1) must round-trip — the lazy "
                        + "getFloodlevel() fallback depends on this sentinel "
                        + "surviving save/load",
                -1, peer.floodlevel);
    }

    // ── SatelliteMicrowaveEnergy ────────────────────────────────────────

    @Test
    public void microwaveEnergyTeirByteRoundTripsAcrossNbt() throws Exception {
        SatelliteMicrowaveEnergy src = new SatelliteMicrowaveEnergy();
        // The teir field is package-private — use reflection to set
        // it so we don't have to leak a public setter into production
        // just for the test.
        Field teirField = SatelliteMicrowaveEnergy.class.getDeclaredField("teir");
        teirField.setAccessible(true);
        teirField.setByte(src, (byte) 3);

        NBTTagCompound nbt = new NBTTagCompound();
        src.writeToNBT(nbt);
        assertEquals("teir key must be written",
                (byte) 3, nbt.getByte("teir"));

        SatelliteMicrowaveEnergy peer = new SatelliteMicrowaveEnergy();
        peer.readFromNBT(nbt);
        assertEquals("teir byte must round-trip",
                (byte) 3, teirField.getByte(peer));
    }

    @Test
    public void microwaveEnergyTeirDefaultsToZeroAndRoundTrips() throws Exception {
        // A fresh satellite has teir=0. The round-trip must preserve
        // that — if the reader accidentally read a different key, a
        // freshly-constructed peer would still observe 0 (because
        // NBTTagCompound.getByte returns 0 on missing keys), which
        // would mask the bug. Pin by comparing with a positive
        // non-default in a separate test (above) and the default
        // here.
        SatelliteMicrowaveEnergy src = new SatelliteMicrowaveEnergy();
        Field teirField = SatelliteMicrowaveEnergy.class.getDeclaredField("teir");
        teirField.setAccessible(true);
        assertEquals("ctor sets teir=0", (byte) 0, teirField.getByte(src));

        NBTTagCompound nbt = new NBTTagCompound();
        src.writeToNBT(nbt);

        SatelliteMicrowaveEnergy peer = new SatelliteMicrowaveEnergy();
        peer.readFromNBT(nbt);
        assertEquals("default teir (=0) must round-trip",
                (byte) 0, teirField.getByte(peer));

        // Sanity counter: a positive teir produces a positive byte in
        // the NBT — proves we aren't accidentally reading from a
        // different key than the one we wrote.
        teirField.setByte(src, (byte) 7);
        NBTTagCompound nbt2 = new NBTTagCompound();
        src.writeToNBT(nbt2);
        assertNotEquals("non-default teir must NOT be 0 in NBT",
                (byte) 0, nbt2.getByte("teir"));
    }
}
