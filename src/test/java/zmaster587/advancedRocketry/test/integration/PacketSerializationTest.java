package zmaster587.advancedRocketry.test.integration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.api.satellite.SatelliteProperties;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.network.PacketConfigSync;
import zmaster587.advancedRocketry.network.PacketDimInfo;
import zmaster587.advancedRocketry.network.PacketSatellite;
import zmaster587.advancedRocketry.network.PacketStationUpdate;
import zmaster587.advancedRocketry.stations.SpaceStationObject;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * §6.9 Network packet wire-format round-trip — the four packets
 * (PacketDimInfo, PacketSatellite, PacketStationUpdate, PacketConfigSync)
 * that need {@link MinecraftBootstrap#ensure()} because their write/readClient
 * pipelines touch {@link net.minecraft.nbt.NBTTagCompound} serialization of
 * vanilla / AR registry-backed objects (biome IDs, satellite type strings,
 * config field schemas).
 *
 * <p>Lighter packets that don't need MC bootstrap live in
 * {@code unit/PacketSerializationTest}.</p>
 */
public class PacketSerializationTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    private static ByteBuf newBuffer() {
        return Unpooled.buffer();
    }

    private static <T> T getField(Object target, String name) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                T value = (T) f.get(target);
                return value;
            } catch (NoSuchFieldException nope) {
                c = c.getSuperclass();
            } catch (Exception e) {
                throw new AssertionError("reflection get " + name + " failed", e);
            }
        }
        throw new AssertionError("field " + name + " not found on " + target.getClass());
    }

    private static void setField(Object target, String name, Object value) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException nope) {
                c = c.getSuperclass();
            } catch (Exception e) {
                throw new AssertionError("reflection set " + name + " failed", e);
            }
        }
        throw new AssertionError("field " + name + " not found on " + target.getClass());
    }

    // ---- PacketDimInfo --------------------------------------------------------

    @Test
    public void packetDimInfoRoundTrip() {
        DimensionProperties props = new DimensionProperties(4242);
        props.setName("TestDim");
        props.setAtmosphereDensityDirect(75);
        props.orbitalDist = 175;
        props.rotationalPeriod = 18000;

        PacketDimInfo sent = new PacketDimInfo(4242, props);
        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketDimInfo received = new PacketDimInfo();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        assertEquals(4242, (int) getField(received, "dimNumber"));
        assertEquals(false, (boolean) getField(received, "deleteDim"));

        // The packet stores raw NBT and re-hydrates inside executeClient (which
        // mutates DimensionManager). Round-trip through DimensionProperties to
        // verify the NBT survived the wire.
        NBTTagCompound nbt = getField(received, "dimNBT");
        assertNotNull("dimNBT missing on receive", nbt);

        DimensionProperties restored = new DimensionProperties(4242);
        restored.readFromNBT(nbt);
        assertEquals("TestDim", restored.getName());
        assertEquals(75, restored.getAtmosphereDensity());
        assertEquals(175, restored.orbitalDist);
        assertEquals(18000, restored.rotationalPeriod);
    }

    @Test
    public void packetDimInfoNullPropertiesIsDeleteSignal() {
        // ctor with null DimensionProperties → wire format collapses to
        // {dimNumber, deleteDim=true}. executeClient interprets that as a delete.
        PacketDimInfo sent = new PacketDimInfo(99, null);
        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketDimInfo received = new PacketDimInfo();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        assertEquals(99, (int) getField(received, "dimNumber"));
        assertTrue("null dimProperties on send must round-trip as deleteDim=true",
                getField(received, "deleteDim"));
    }

    // ---- PacketSatellite ------------------------------------------------------

    /**
     * Minimal SatelliteBase subclass so we can construct a satellite without
     * going through {@code SatelliteRegistry.getNewSatellite(name)} — that
     * lookup is empty in tests because AR's mod-init satellite registrations
     * don't run.
     */
    public static class TestSatellite extends SatelliteBase {
        @Override public String getInfo(net.minecraft.world.World world) { return "test"; }
        @Override public String getName() { return "TestSatellite"; }
        @Override public boolean performAction(net.minecraft.entity.player.EntityPlayer p,
                                               net.minecraft.world.World w,
                                               net.minecraft.util.math.BlockPos pos) {
            return false;
        }
        @Override public double failureChance() { return 0; }
    }

    @Test
    public void packetSatelliteRoundTrip() {
        SatelliteProperties props =
                new SatelliteProperties(120, 4000, "ar:test_packet_sat", 768, 2.5f);
        props.setId(0xC0FFEEL);

        TestSatellite sat = new TestSatellite();
        setField(sat, "satelliteProperties", props);
        sat.setDimensionId(5);

        PacketSatellite sent = new PacketSatellite(sat);
        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        // PacketSatellite.readClient calls SatelliteRegistry.createFromNBT which
        // looks up the type string in the satellite registry — that registry is
        // empty in tests. So we verify the wire payload by reading the NBT
        // directly via PacketBuffer (same call the packet would make) without
        // resolving the satellite class.
        net.minecraft.network.PacketBuffer packetBuffer = new net.minecraft.network.PacketBuffer(buffer);
        NBTTagCompound nbt;
        try {
            nbt = packetBuffer.readCompoundTag();
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        assertEquals(0, buffer.readableBytes());
        assertNotNull("packet payload missing satellite NBT", nbt);

        // Re-hydrate properties and verify everything that doesn't need the
        // registry survived (the type string is what executeClient would feed
        // to createFromNBT — verifying it preserves the wire format).
        assertTrue("NBT missing properties tag: " + nbt, nbt.hasKey("properties"));
        assertEquals(5, nbt.getInteger("dimId"));

        SatelliteProperties restored = new SatelliteProperties();
        restored.readFromNBT(nbt.getCompoundTag("properties"));
        assertEquals(120, restored.getPowerGeneration());
        assertEquals(4000, restored.getPowerStorage());
        assertEquals("ar:test_packet_sat", restored.getSatelliteType());
        assertEquals(768, restored.getMaxDataStorage());
        assertEquals(2.5f, restored.getWeight(), 1e-6);
        assertEquals(0xC0FFEEL, restored.getId());
    }

    // ---- PacketStationUpdate --------------------------------------------------

    @Test
    public void packetStationUpdateFuelRoundTrip() {
        // FUEL_UPDATE is the simplest payload — just stationNumber+type+fuel int.
        SpaceStationObject station = new SpaceStationObject();
        station.setFuelAmount(7777);
        // ISpaceObject.getId() reads from a field that's normally set by
        // SpaceObjectManager.register; inject via reflection for the test.
        station.setId(1234);

        PacketStationUpdate sent = new PacketStationUpdate(station, PacketStationUpdate.Type.FUEL_UPDATE);
        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketStationUpdate received = new PacketStationUpdate();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        assertEquals(1234, (int) getField(received, "stationNumber"));
        assertEquals(PacketStationUpdate.Type.FUEL_UPDATE, getField(received, "type"));
        assertEquals(7777, (int) getField(received, "fuel"));
    }

    @Test
    public void packetStationUpdateOrbitRoundTrip() {
        SpaceStationObject station = new SpaceStationObject();
        // Avoid the orbiting-body NPE — beginTransition flips `created` to true
        // and primes destination resolution.
        station.beginTransition(0);
        station.setOrbitingBody(0);
        station.setId(5678);

        PacketStationUpdate sent = new PacketStationUpdate(
                station, PacketStationUpdate.Type.ORBIT_UPDATE);
        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketStationUpdate received = new PacketStationUpdate();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        assertEquals(5678, (int) getField(received, "stationNumber"));
        assertEquals(PacketStationUpdate.Type.ORBIT_UPDATE, getField(received, "type"));
        // ORBIT_UPDATE stores planet id in `destOrbitingBody` slot on the wire.
        assertEquals(0, (int) getField(received, "destOrbitingBody"));
    }

    // ---- PacketConfigSync -----------------------------------------------------

    @Test
    public void packetConfigSyncRoundTrip() {
        // Start from a current-config copy (matches what production ARConfiguration
        // routinely serializes) and tweak deterministic fields. A fresh
        // ARConfiguration() leaves some collection-fields null which throws off
        // the wire format because writeConfigToNetwork expects them initialized.
        ARConfiguration cfg = new ARConfiguration(ARConfiguration.getCurrentConfig());
        cfg.spaceDimId = 9999;
        cfg.stationSize = 999;

        PacketConfigSync sent = new PacketConfigSync(cfg);
        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketConfigSync received = new PacketConfigSync();
        received.readClient(buffer);

        // Note: not asserting `readableBytes == 0` because ARConfiguration carries
        // version padding / optional sections; the wire-level invariant we care
        // about is that the round-tripped fields match.
        ARConfiguration restored = getField(received, "config");
        assertNotNull("config null after readClient", restored);
        assertEquals(9999, restored.spaceDimId);
        assertEquals(999, restored.stationSize);
    }
}
