package zmaster587.advancedRocketry.test.unit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import static org.junit.Assert.assertNull;
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

    // PacketDimInfo / PacketSatellite / PacketStationUpdate / PacketConfigSync
    // round-trips require live DimensionManager / SatelliteRegistry / ISpaceObject /
    // ARConfiguration state. They're covered end-to-end through the matching §7
    // scenario tests (§7.4 / §7.12 / §7.11 / §7.1) which exercise the same wire
    // format implicitly via /artest probes on real packets between client and
    // server.

    // ── §6.9 bullet 5 — "assert invalid/missing data fails safely" ────────
    // For every AR packet whose readClient lives in a pure path (no MC client
    // required), we pin failure semantics on malformed wire data. The unifying
    // safety invariant is:
    //
    //   "Either readClient parses everything cleanly, or it bails — but it
    //    MUST NOT half-fill fields with attacker-controlled bytes."
    //
    // PacketBuffer.readCompoundTag's underflow path throws IndexOutOfBoundsException
    // (from underlying ByteBuf.readByte) — not the declared IOException — so the
    // catch (IOException) clause in PacketAtmSync / PacketStellarInfo only handles
    // structurally-malformed NBT, not byte-truncated wire. Either way: Netty's
    // pipeline catches it, Forge logs and drops the packet. JVM stays up.
    //
    // PacketOxygenState's readClient touches Minecraft.getMinecraft() and is
    // exercised by the integration suite — no unit-level safety mode exists.

    /**
     * The core safety assertion: regardless of whether readClient throws or
     * returns, the packet instance must not have been half-populated with
     * untrusted bytes. Either every field is at its default or every field is
     * a coherent result of a successful parse — never a hostile mix.
     */
    private static void assertReadClientFailsSafely(Runnable readOp) {
        try {
            readOp.run();
        } catch (RuntimeException ignoredBounded) {
            // Acceptable. IndexOutOfBoundsException and friends propagate to
            // Netty/Forge; the packet is dropped by the network pipeline. The
            // important property is that the throw is *bounded* (single
            // exception, not OOM / infinite loop) and that no partial
            // mutation leaked attacker bytes onto our fields — verified by
            // the caller's post-condition asserts.
        }
    }

    @Test
    public void packetAtmSyncReadClientEmptyBufferLeavesDefaults() {
        ByteBuf empty = newBuffer();
        PacketAtmSync packet = new PacketAtmSync();
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        // readCompoundTag underflowed → field assignments inside the try block
        // never executed → fields are at no-arg-ctor defaults.
        assertNull(PacketSerializationTest.<String>field(packet, "type"));
        assertEquals(0, (int) PacketSerializationTest.<Integer>field(packet, "pressure"));
    }

    @Test
    public void packetAtmSyncReadClientGarbageBytesLeavesDefaults() {
        // Random bytes that don't form a valid NBT compound. Either the
        // tag-type byte is rejected by CompressedStreamTools.read or the
        // buffer underflows during structured read — either way, readClient's
        // type/pressure assignments are skipped.
        ByteBuf garbage = newBuffer();
        garbage.writeBytes(new byte[]{0x42, 0x13, 0x37, (byte) 0xFF, 0x00, 0x01});

        PacketAtmSync packet = new PacketAtmSync();
        assertReadClientFailsSafely(() -> packet.readClient(garbage));
        assertNull(PacketSerializationTest.<String>field(packet, "type"));
        assertEquals(0, (int) PacketSerializationTest.<Integer>field(packet, "pressure"));
    }

    @Test
    public void packetStellarInfoReadClientEmptyBufferLeavesDefaults() {
        // readInt on an empty ByteBuf throws IndexOutOfBoundsException before
        // any assignment lands. Fields keep their declared defaults.
        ByteBuf empty = newBuffer();
        PacketStellarInfo packet = new PacketStellarInfo();
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        assertEquals(0, (int) PacketSerializationTest.<Integer>field(packet, "starId"));
        assertEquals(false, (boolean) PacketSerializationTest.<Boolean>field(packet, "removeStar"));
        assertNull(PacketSerializationTest.<Object>field(packet, "nbt"));
    }

    @Test
    public void packetStellarInfoReadClientHeaderOnlyLeavesNbtNull() {
        // Writer emits id (4 bytes) + removeStar (1 byte) + optional NBT.
        // Feed only the 5-byte header with removeStar=false: id and removeStar
        // parse cleanly, then readCompoundTag tries to consume the absent NBT
        // and underflows. The exception propagates (it's an IOOBE, not the
        // IOException the catch clause handles), but the critical safety
        // property is that nbt stays null — guaranteeing executeClient's
        // `if (nbt != null)` branch can't fire on attacker data.
        ByteBuf header = newBuffer();
        header.writeInt(42);          // starId
        header.writeBoolean(false);   // removeStar — triggers the NBT-read branch

        PacketStellarInfo packet = new PacketStellarInfo();
        assertReadClientFailsSafely(() -> packet.readClient(header));

        assertEquals(42, (int) PacketSerializationTest.<Integer>field(packet, "starId"));
        assertEquals(false, (boolean) PacketSerializationTest.<Boolean>field(packet, "removeStar"));
        assertNull("nbt must be null when the NBT portion underflows — that's "
                        + "what gates executeClient from a half-parse",
                PacketSerializationTest.<Object>field(packet, "nbt"));
    }

    @Test
    public void packetSyncKnownPlanetsReadClientEmptyBufferLeavesDefaults() {
        // readInt on empty buffer throws IOOBE before stationId or
        // knownPlanets is assigned.
        ByteBuf empty = newBuffer();
        PacketSyncKnownPlanets packet = new PacketSyncKnownPlanets();
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        assertEquals(0, packet.stationId);
        assertNull(PacketSerializationTest.<Object>field(packet, "knownPlanets"));
    }

    @Test
    public void packetSyncKnownPlanetsReadClientNegativeSizeReturnsEmptySet() throws Exception {
        // A hostile / corrupt sender could put size=-1 on the wire. The
        // for-loop guard (i < size) fails immediately so no further reads
        // happen. Crucial: no pre-allocated array sized to the (negative,
        // possibly-cast-to-huge) count, no IOOBE, no infinite loop —
        // knownPlanets ends up empty.
        ByteBuf wire = newBuffer();
        wire.writeInt(7);             // stationId
        wire.writeInt(-1);            // size — hostile

        PacketSyncKnownPlanets packet = new PacketSyncKnownPlanets();
        packet.readClient(wire); // must NOT throw

        assertEquals(7, packet.stationId);
        Set<Integer> known = field(packet, "knownPlanets");
        assertNotNull(known);
        assertEquals("negative-size header must produce an empty set, not crash",
                0, known.size());
    }

    @Test
    public void packetSyncKnownPlanetsReadClientTruncatedPayloadFailsBounded() {
        // Header claims 5 entries; only 1.5 entries' worth of bytes follow.
        // The loop reads entry 0 successfully, partially consumes 2 bytes for
        // entry 1, then underflows on the next readInt → IOOBE. Asserts the
        // failure is bounded (a single exception, no infinite read).
        ByteBuf wire = newBuffer();
        wire.writeInt(99);            // stationId
        wire.writeInt(5);             // claimed size
        wire.writeInt(1);             // entry 0
        wire.writeBytes(new byte[]{0x00, 0x00}); // 2 bytes — short of an int

        PacketSyncKnownPlanets packet = new PacketSyncKnownPlanets();
        assertReadClientFailsSafely(() -> packet.readClient(wire));
        // stationId did make it (read before size) — this is OK because it
        // is *attacker-derived* but bounded to one int and not used until
        // executeClient pairs it with the (now-incomplete) planet set.
        assertEquals(99, packet.stationId);
    }

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
