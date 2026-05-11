package zmaster587.advancedRocketry.test.unit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Ignore;
import org.junit.Test;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.api.satellite.SatelliteProperties;
import zmaster587.advancedRocketry.network.PacketAtmSync;
import zmaster587.advancedRocketry.network.PacketOxygenState;
import zmaster587.advancedRocketry.network.PacketStellarInfo;
import zmaster587.advancedRocketry.network.PacketSyncKnownPlanets;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * §6.9 Network packet round-trip — write/readClient symmetry.
 *
 * Each test:
 *   1. constructs a representative packet,
 *   2. writes it into a Netty {@link ByteBuf},
 *   3. reads into a fresh instance via the no-arg ctor + readClient,
 *   4. asserts every field is preserved.
 *
 * Production code dispatches read vs readClient based on side. Most AR packets are
 * server→client only (no executable {@code read} on server). We exercise the
 * client-bound path (write → readClient) here.
 *
 * Packets that pull state from {@code DimensionManager} / {@code SpaceObjectManager}
 * during executeClient are NOT exercised end-to-end here; that lives in the §7
 * scenario suite.
 */
public class PacketSerializationTest {

    private static ByteBuf newBuffer() {
        return Unpooled.buffer();
    }

    /**
     * Reflection helper — sets a private field on a packet instance so we can
     * exercise the round-trip without invoking constructors that touch global
     * registries (e.g. {@code PacketSyncKnownPlanets} pulls from
     * {@code DimensionManager.getInstance().knownPlanets}).
     */
    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void packetAtmSyncRoundTrip() {
        PacketAtmSync sent = new PacketAtmSync("ar:test_atm", 850);

        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketAtmSync received = new PacketAtmSync();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        assertEquals("ar:test_atm", PacketSerializationTest.<String>field(received, "type"));
        assertEquals(850, (int) PacketSerializationTest.<Integer>field(received, "pressure"));
    }

    @Test
    public void packetOxygenStateRoundTrip() {
        // PacketOxygenState carries no payload — write() must produce zero bytes
        // and readClient() must complete without throwing.
        PacketOxygenState sent = new PacketOxygenState();
        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        assertEquals("PacketOxygenState carries no payload", 0, buffer.readableBytes());

        // Readability test: a fresh instance should accept the empty stream silently.
        // We deliberately do NOT call sent.readClient — that path uses
        // Minecraft.getMinecraft() which requires a running game.
    }

    @Test
    public void packetStellarInfoRoundTrip() throws Exception {
        StellarBody star = new StellarBody();
        star.setName("TestStar");
        star.setTemperature(80);
        star.setSize(1.5f);
        star.setBlackHole(false);
        star.setId(7);

        PacketStellarInfo sent = new PacketStellarInfo(7, star);
        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketStellarInfo received = new PacketStellarInfo();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        assertEquals(7, (int) PacketSerializationTest.<Integer>field(received, "starId"));
        assertEquals(false, (boolean) PacketSerializationTest.<Boolean>field(received, "removeStar"));

        // The packet stores the inner NBT and only re-hydrates the star inside
        // executeClient (which mutates DimensionManager). Round-trip the NBT to
        // verify it survived the wire.
        net.minecraft.nbt.NBTTagCompound nbt = field(received, "nbt");
        assertNotNull(nbt);

        StellarBody restored = new StellarBody();
        restored.readFromNBT(nbt);
        assertEquals("TestStar", restored.getName());
        assertEquals(80, restored.getTemperature());
        assertEquals(1.5f, restored.getSize(), 1e-6);
        assertEquals(7, restored.getId());
    }

    @Test
    public void packetStellarInfoRoundTripRemoveStar() throws Exception {
        // Setting star=null signals removal — the wire format must encode just the
        // id + removeStar=true and no NBT block.
        PacketStellarInfo sent = new PacketStellarInfo(99, null);
        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketStellarInfo received = new PacketStellarInfo();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        assertEquals(99, (int) PacketSerializationTest.<Integer>field(received, "starId"));
        assertTrue("star=null on send must round-trip as removeStar=true",
                PacketSerializationTest.<Boolean>field(received, "removeStar"));
    }

    @Test
    public void packetSyncKnownPlanetsRoundTrip() throws Exception {
        // The 2-arg ctor pulls DimensionManager.getInstance().knownPlanets into the
        // payload — bypass it via no-arg ctor + reflection so we don't depend on
        // global state.
        PacketSyncKnownPlanets sent = new PacketSyncKnownPlanets();
        sent.stationId = 42;
        Set<Integer> planets = new HashSet<>();
        planets.add(2);
        planets.add(7);
        planets.add(11);
        setField(sent, "knownPlanets", planets);

        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketSyncKnownPlanets received = new PacketSyncKnownPlanets();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        assertEquals(42, received.stationId);

        Set<Integer> recvPlanets = field(received, "knownPlanets");
        assertNotNull(recvPlanets);
        assertEquals(planets, recvPlanets);
    }

    @Test
    public void packetSyncKnownPlanetsRoundTripEmpty() throws Exception {
        PacketSyncKnownPlanets sent = new PacketSyncKnownPlanets();
        sent.stationId = 1;
        setField(sent, "knownPlanets", new HashSet<Integer>());

        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketSyncKnownPlanets received = new PacketSyncKnownPlanets();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        Set<Integer> recvPlanets = field(received, "knownPlanets");
        assertEquals(0, recvPlanets.size());
    }

    @Test
    public void satellitePropertiesNbtSurvivesPacketBufferTransport() {
        // Satellite-bearing packets (PacketSatellite) ultimately serialize
        // SatelliteProperties via writeCompoundTag. Test the inner serialization is
        // wire-stable independent of the surrounding packet machinery.
        SatelliteProperties original = new SatelliteProperties(40, 800, "ar:test", 256, 1.5f);
        original.setId(0xFEEDL);

        net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
        original.writeToNBT(nbt);

        ByteBuf buffer = newBuffer();
        net.minecraft.network.PacketBuffer packetBuffer = new net.minecraft.network.PacketBuffer(buffer);
        packetBuffer.writeCompoundTag(nbt);

        net.minecraft.nbt.NBTTagCompound received;
        try {
            received = packetBuffer.readCompoundTag();
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }

        SatelliteProperties restored = new SatelliteProperties();
        restored.readFromNBT(received);

        assertEquals(40, restored.getPowerGeneration());
        assertEquals(800, restored.getPowerStorage());
        assertEquals("ar:test", restored.getSatelliteType());
        assertEquals(256, restored.getMaxDataStorage());
        assertEquals(1.5f, restored.getWeight(), 1e-6);
        assertEquals(0xFEEDL, restored.getId());
    }

    @Test @Ignore("PacketDimInfo round-trip needs DimensionManager-bound DimensionProperties — covered in §7.4 scenario tests")
    public void packetDimInfoRoundTrip() {}

    @Test @Ignore("PacketSatellite needs SatelliteRegistry resolved + DimensionManager — covered in §7.12")
    public void packetSatelliteRoundTrip() {}

    @Test @Ignore("PacketStationUpdate needs ISpaceObject fixture — covered in §7.11")
    public void packetStationUpdateRoundTrip() {}

    @Test @Ignore("PacketConfigSync needs full ARConfiguration loadPreInit — covered in §7.1")
    public void packetConfigSyncRoundTrip() {}

    // Convenience to keep callsites clean without leaking the throws clause.
    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new AssertionError("Reflection failed reading field " + name, e);
        }
    }
}
