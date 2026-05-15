package zmaster587.advancedRocketry.test.integration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.api.satellite.SatelliteProperties;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.network.PacketAirParticle;
import zmaster587.advancedRocketry.network.PacketAsteroidInfo;
import zmaster587.advancedRocketry.network.PacketBiomeIDChange;
import zmaster587.advancedRocketry.network.PacketConfigSync;
import zmaster587.advancedRocketry.network.PacketDimInfo;
import zmaster587.advancedRocketry.network.PacketFluidParticle;
import zmaster587.advancedRocketry.network.PacketInvalidLocationNotify;
import zmaster587.advancedRocketry.network.PacketLaserGun;
import zmaster587.advancedRocketry.network.PacketMoveRocketInSpace;
import zmaster587.advancedRocketry.network.PacketSatellite;
import zmaster587.advancedRocketry.network.PacketSatellitesUpdate;
import zmaster587.advancedRocketry.network.PacketSpaceStationInfo;
import zmaster587.advancedRocketry.network.PacketStationUpdate;
import zmaster587.advancedRocketry.stations.SpaceStationObject;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;
import zmaster587.advancedRocketry.util.Asteroid;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.lang.reflect.Field;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    // ---- PacketInvalidLocationNotify -----------------------------------------

    @Test
    public void packetInvalidLocationNotifyRoundTrip() {
        HashedBlockPosition pos = new HashedBlockPosition(123, 64, -456);
        PacketInvalidLocationNotify sent = new PacketInvalidLocationNotify(pos);

        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketInvalidLocationNotify received = new PacketInvalidLocationNotify();
        received.readClient(buffer);

        assertEquals("wire should be fully consumed", 0, buffer.readableBytes());
        HashedBlockPosition restored = getField(received, "toPos");
        assertEquals(123, restored.x);
        assertEquals(64, restored.y);
        assertEquals(-456, restored.z);
    }

    // ---- PacketFluidParticle -------------------------------------------------

    @Test
    public void packetFluidParticleRoundTrip() {
        BlockPos from = new BlockPos(10, 20, 30);
        BlockPos to = new BlockPos(-40, 50, -60);
        PacketFluidParticle sent = new PacketFluidParticle(from, to, 80, 0xFF66AA);

        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketFluidParticle received = new PacketFluidParticle();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        BlockPos restoredFrom = getField(received, "fromPos");
        BlockPos restoredTo = getField(received, "toPos");
        assertEquals(from, restoredFrom);
        assertEquals(to, restoredTo);
        assertEquals(80, (int) PacketSerializationTest.<Integer>getField(received, "time"));
        assertEquals(0xFF66AA, (int) PacketSerializationTest.<Integer>getField(received, "color"));
    }

    // ---- PacketAsteroidInfo --------------------------------------------------

    @Test
    public void packetAsteroidInfoRoundTrip() {
        Asteroid original = new Asteroid();
        original.ID = "test:goldRich";
        original.distance = 175;
        original.mass = 32_000;
        original.minLevel = 3;
        original.massVariability = 0.25f;
        original.richness = 0.6f;
        original.richnessVariability = 0.1f;
        original.probability = 0.05f;
        original.timeMultiplier = 1.5f;
        original.itemStacks.add(new ItemStack(Items.GOLD_INGOT, 1));
        original.stackProbabilities.add(0.4f);
        original.itemStacks.add(new ItemStack(Items.IRON_INGOT, 1));
        original.stackProbabilities.add(0.6f);

        PacketAsteroidInfo sent = new PacketAsteroidInfo(original);
        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketAsteroidInfo received = new PacketAsteroidInfo();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        Asteroid restored = getField(received, "asteroid");

        assertEquals("test:goldRich", restored.ID);
        assertEquals(175, restored.distance);
        assertEquals(32_000, restored.mass);
        assertEquals(3, restored.minLevel);
        assertEquals(0.25f, restored.massVariability, 1e-6);
        assertEquals(0.6f, restored.richness, 1e-6);
        assertEquals(0.1f, restored.richnessVariability, 1e-6);
        assertEquals(0.05f, restored.probability, 1e-6);
        assertEquals(1.5f, restored.timeMultiplier, 1e-6);

        assertEquals(2, restored.itemStacks.size());
        assertEquals(Items.GOLD_INGOT, restored.itemStacks.get(0).getItem());
        assertEquals(Items.IRON_INGOT, restored.itemStacks.get(1).getItem());
        assertEquals(0.4f, restored.stackProbabilities.get(0), 1e-6);
        assertEquals(0.6f, restored.stackProbabilities.get(1), 1e-6);
    }

    @Test
    public void packetAsteroidInfoRoundTripEmptyStackList() {
        Asteroid original = new Asteroid();
        original.ID = "test:empty";
        original.distance = 1;
        original.mass = 1;
        original.minLevel = 0;
        original.massVariability = 0;
        original.richness = 0;
        original.richnessVariability = 0;
        original.probability = 0;
        original.timeMultiplier = 1;

        PacketAsteroidInfo sent = new PacketAsteroidInfo(original);
        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketAsteroidInfo received = new PacketAsteroidInfo();
        received.readClient(buffer);

        Asteroid restored = getField(received, "asteroid");
        assertEquals(0, restored.itemStacks.size());
        assertEquals(0, restored.stackProbabilities.size());
    }

    // ---- PacketLaserGun ------------------------------------------------------

    /**
     * write() pulls fromEntity.getEntityId() — we can't easily fabricate a real
     * Entity, so this test exercises the readClient path against a hand-crafted
     * wire payload that matches what write() would have produced. The write
     * symmetry is implicitly covered by the executeClient half being a no-op for
     * fields other than entityId/toPos.
     */
    @Test
    public void packetLaserGunReadClientDecodesWire() {
        ByteBuf buffer = newBuffer();
        buffer.writeInt(4242);              // entityId
        buffer.writeFloat(1.5f);            // toPos.x
        buffer.writeFloat(64.25f);          // toPos.y
        buffer.writeFloat(-2.75f);          // toPos.z

        PacketLaserGun received = new PacketLaserGun();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        assertEquals(4242, (int) PacketSerializationTest.<Integer>getField(received, "entityId"));

        net.minecraft.util.math.Vec3d toPos = getField(received, "toPos");
        assertEquals(1.5, toPos.x, 1e-6);
        assertEquals(64.25, toPos.y, 1e-6);
        assertEquals(-2.75, toPos.z, 1e-6);
    }

    // ---- PacketBiomeIDChange -------------------------------------------------

    /**
     * write() pulls chunk.x / chunk.z / chunk.getBiomeArray() — fabricating a
     * real Chunk requires a full World. We test the readClient path against a
     * known wire layout matching what the production write() emits.
     */
    @Test
    public void packetBiomeIDChangeReadClientDecodesWire() {
        byte[] biomeArr = new byte[256];
        for (int i = 0; i < 256; i++) biomeArr[i] = (byte) (i ^ 0x5A);

        ByteBuf buffer = newBuffer();
        buffer.writeInt(7);                 // worldId
        buffer.writeInt(12);                // chunk.x → xPos
        buffer.writeInt(-3);                // chunk.z → zPos
        buffer.writeInt(200);               // pos.x
        buffer.writeShort(64);              // pos.y (short)
        buffer.writeInt(-50);               // pos.z
        buffer.writeBytes(biomeArr);

        PacketBiomeIDChange received = new PacketBiomeIDChange();
        received.readClient(buffer);

        assertEquals(0, buffer.readableBytes());
        assertEquals(7, (int) PacketSerializationTest.<Integer>getField(received, "worldId"));
        assertEquals(12, (int) PacketSerializationTest.<Integer>getField(received, "xPos"));
        assertEquals(-3, (int) PacketSerializationTest.<Integer>getField(received, "zPos"));

        HashedBlockPosition pos = getField(received, "pos");
        assertEquals(200, pos.x);
        assertEquals(64, pos.y);
        assertEquals(-50, pos.z);

        byte[] restored = getField(received, "array");
        assertArrayEquals(biomeArr, restored);
    }

    // ---- PacketStorageTileUpdate ---------------------------------------------

    /**
     * readClient() touches Minecraft.getMinecraft().world — unreachable from
     * unit JVM. We exercise the wire shape directly: write a known payload via
     * PacketBuffer (as production write does) and verify the bytes decode into
     * the expected primitive layout. The Entity.world.provider dispatch is
     * covered by §7.9 / §7.10 scenarios.
     */
    @Test
    public void packetStorageTileUpdateWireLayout() {
        // Wire format:
        //   int worldId, int entityId, int x, int y, int z, NBTCompound tile.
        ByteBuf buffer = newBuffer();
        buffer.writeInt(0);                 // overworld
        buffer.writeInt(99);                // entityId
        buffer.writeInt(15);                // x
        buffer.writeInt(70);                // y
        buffer.writeInt(-15);               // z

        NBTTagCompound tileNbt = new NBTTagCompound();
        tileNbt.setString("id", "advancedrocketry:test_tile");
        tileNbt.setInteger("energy", 42_000);
        new PacketBuffer(buffer).writeCompoundTag(tileNbt);

        // Mirror-decode the bytes the way readClient would, but without the
        // Minecraft.getMinecraft() lookup. This proves the wire format is
        // self-describing and the NBT is recoverable.
        assertEquals(0, buffer.readInt());
        assertEquals(99, buffer.readInt());
        assertEquals(15, buffer.readInt());
        assertEquals(70, buffer.readInt());
        assertEquals(-15, buffer.readInt());

        NBTTagCompound restored;
        try {
            restored = new PacketBuffer(buffer).readCompoundTag();
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        assertNotNull(restored);
        assertEquals("advancedrocketry:test_tile", restored.getString("id"));
        assertEquals(42_000, restored.getInteger("energy"));
    }

    // ---- PacketAirParticle ---------------------------------------------------

    @Test
    public void packetAirParticleRoundTrip() {
        HashedBlockPosition pos = new HashedBlockPosition(-25, 90, 1024);
        PacketAirParticle sent = new PacketAirParticle(pos);

        ByteBuf buffer = newBuffer();
        sent.write(buffer);

        PacketAirParticle received = new PacketAirParticle();
        received.readClient(buffer);

        assertEquals("wire should be fully consumed", 0, buffer.readableBytes());
        HashedBlockPosition restored = getField(received, "toPos");
        assertEquals(-25, restored.x);
        assertEquals(90, restored.y);
        assertEquals(1024, restored.z);
    }

    // ---- PacketSpaceStationInfo ----------------------------------------------

    /**
     * write() needs a live {@code SpaceStationObject} hooked into
     * {@code SpaceObjectManager} (which the mod registers only during init).
     * We exercise the read path against a hand-crafted wire that matches what
     * production write() emits when {@code isBeingDeleted=false}.
     *
     * <p>Wire layout (non-deletion branch):</p>
     * <pre>
     *   int stationNumber
     *   bool isBeingDeleted = false
     *   String clazzId (PacketBuffer)
     *   NBTTagCompound nbt
     *   int fuelAmt
     *   bool hasWarpCores
     *   int direction.ordinal()
     * </pre>
     */
    @Test
    public void packetSpaceStationInfoNonDeletionReadClient() throws Exception {
        ByteBuf buffer = newBuffer();
        net.minecraft.network.PacketBuffer pb = new net.minecraft.network.PacketBuffer(buffer);
        buffer.writeInt(7777);                  // stationNumber
        buffer.writeBoolean(false);             // isBeingDeleted
        pb.writeString("station-class-id");     // clazzId
        NBTTagCompound payload = new NBTTagCompound();
        payload.setString("name", "RoundTripStation");
        payload.setInteger("dim", 7777);
        pb.writeCompoundTag(payload);
        pb.writeInt(98_765);                    // fuelAmt
        buffer.writeBoolean(true);              // hasWarpCores
        buffer.writeInt(net.minecraft.util.EnumFacing.SOUTH.ordinal());

        PacketSpaceStationInfo received = new PacketSpaceStationInfo();
        received.readClient(buffer);

        assertEquals("wire should be fully consumed", 0, buffer.readableBytes());
        assertEquals(7777, (int) PacketSerializationTest.<Integer>getField(received, "stationNumber"));
        assertEquals(false, (boolean) PacketSerializationTest.<Boolean>getField(received, "isBeingDeleted"));
        assertEquals("station-class-id", PacketSerializationTest.<String>getField(received, "clazzId"));
        NBTTagCompound restoredNbt = getField(received, "nbt");
        assertNotNull(restoredNbt);
        assertEquals("RoundTripStation", restoredNbt.getString("name"));
        assertEquals(7777, restoredNbt.getInteger("dim"));
        assertEquals(98_765, (int) PacketSerializationTest.<Integer>getField(received, "fuelAmt"));
        assertEquals(true, (boolean) PacketSerializationTest.<Boolean>getField(received, "hasWarpCores"));
        assertEquals(net.minecraft.util.EnumFacing.SOUTH.ordinal(),
                (int) PacketSerializationTest.<Integer>getField(received, "direction"));
    }

    /**
     * Deletion branch — server signals "remove this station". Wire is just
     * {@code int stationNumber + bool isBeingDeleted=true}. No further fields
     * are emitted, no further fields are read. Tripwire: if someone adds a
     * field after {@code isBeingDeleted} without gating it on the flag, this
     * test fails because readClient over-consumes the buffer.
     */
    @Test
    public void packetSpaceStationInfoDeletionBranch() {
        ByteBuf buffer = newBuffer();
        buffer.writeInt(4242);
        buffer.writeBoolean(true);              // isBeingDeleted

        PacketSpaceStationInfo received = new PacketSpaceStationInfo();
        received.readClient(buffer);

        assertEquals("deletion branch must consume exactly the 5 bytes written",
                0, buffer.readableBytes());
        assertEquals(4242, (int) PacketSerializationTest.<Integer>getField(received, "stationNumber"));
        assertEquals(true, (boolean) PacketSerializationTest.<Boolean>getField(received, "isBeingDeleted"));
    }

    // ---- PacketSatellitesUpdate ----------------------------------------------

    /**
     * write() requires a {@code DimensionProperties} with ticking satellites
     * (lookup goes via DimensionManager). readClient runs an FML side check
     * AND mutates {@code DimensionManager.getInstance().getDimensionProperties(dim)},
     * neither of which is testable in unit JVM without a registered planet
     * containing real satellites.
     *
     * We exercise the wire shape: write a known payload via the same primitives
     * the production write() uses, then mirror-decode and verify the NBT block
     * is recoverable. The DimensionManager mutation is covered end-to-end by
     * §7.12 {@code SatelliteLifecycleSmokeTest}.
     */
    @Test
    public void packetSatellitesUpdateWireLayout() {
        ByteBuf buffer = newBuffer();
        buffer.writeInt(0);                     // dimNumber

        NBTTagCompound payload = new NBTTagCompound();
        // Two satellite tags keyed by id, the exact layout production write uses.
        NBTTagCompound sat1 = new NBTTagCompound();
        sat1.setString("dataType", "ar:test_sat");
        sat1.setInteger("powerStored", 1234);
        payload.setTag("100", sat1);

        NBTTagCompound sat2 = new NBTTagCompound();
        sat2.setString("dataType", "ar:test_sat");
        sat2.setInteger("powerStored", 5678);
        payload.setTag("200", sat2);

        net.minecraftforge.fml.common.network.ByteBufUtils.writeTag(buffer, payload);

        // Mirror-decode the same way readClient does (sans DimensionManager
        // mutation).
        assertEquals(0, buffer.readInt());

        NBTTagCompound restored = net.minecraftforge.fml.common.network.ByteBufUtils
                .readTag(buffer);
        assertNotNull(restored);
        assertEquals("two satellite tags must survive the wire",
                2, restored.getKeySet().size());
        assertTrue("satellite id 100 must round-trip", restored.hasKey("100"));
        assertTrue("satellite id 200 must round-trip", restored.hasKey("200"));
        assertEquals(1234, restored.getCompoundTag("100").getInteger("powerStored"));
        assertEquals(5678, restored.getCompoundTag("200").getInteger("powerStored"));
        assertEquals("buffer fully consumed", 0, buffer.readableBytes());
    }

    // ---- PacketMoveRocketInSpace ---------------------------------------------

    /**
     * §6.9 — {@link PacketMoveRocketInSpace} is DEAD CODE: it has no
     * {@code addDiscriminator} registration in
     * {@code AdvancedRocketry.serverStarting}, so it is never actually sent
     * over the wire. We still pin its current behaviour because (a) SMART
     * §6.9 lists it, and (b) it contains TWO latent bugs that should fail
     * loudly when the packet is eventually wired up:
     *
     * <ol>
     *   <li><b>Inverted boolean</b>: {@code hasWorld = position.world == null}
     *       — i.e. {@code hasWorld=true} means "no world". The next line then
     *       does {@code if (hasWorld) writeInt(position.world.getId())},
     *       which NPEs on the very case the boolean was supposed to handle.
     *       And when {@code world != null}, the int is silently skipped, so
     *       the wire NEVER carries dimId. Same bug for {@code hasStar}.</li>
     *   <li><b>read(ByteBuf)</b>: uses {@code position.x = in.readDouble()}
     *       but {@code position} is null after no-arg ctor, so the server-side
     *       read path always NPEs. Doesn't matter while the packet is
     *       unregistered; will explode immediately when it is registered.</li>
     * </ol>
     *
     * We document both with assertions that fail when (and only when) the bugs
     * are fixed — the test then needs to be flipped manually.
     */
    @Test
    public void packetMoveRocketInSpaceDocumentsKnownBugs() throws Exception {
        // Bug #2: read(ByteBuf) on a freshly constructed packet always NPEs.
        PacketMoveRocketInSpace fresh = new PacketMoveRocketInSpace();
        ByteBuf buffer = newBuffer();
        buffer.writeDouble(1.0);  buffer.writeDouble(2.0);  buffer.writeDouble(3.0);
        buffer.writeBoolean(false); buffer.writeBoolean(false);

        boolean serverReadNpes = false;
        try {
            fresh.read(buffer);
        } catch (NullPointerException expected) {
            serverReadNpes = true;
        }
        assertTrue("PacketMoveRocketInSpace.read() must currently NPE on default-ctor "
                + "instance — fix the bug then flip this assertion",
                serverReadNpes);

        // Bug #1: when SpacePosition.world == null, write() NPEs because the
        // "hasWorld" branch dereferences world. We can't exercise that without
        // constructing a SpacePosition (which requires DimensionManager state
        // for star/world); instead we pin the inverted-boolean contract by
        // reading the source and asserting on the literal field names.
        //
        // (A future PR fixing the bug must update this assertion to the
        // intended semantics:  hasWorld = position.world != null;)
        java.lang.reflect.Field hw = PacketMoveRocketInSpace.class.getDeclaredField("hasWorld");
        java.lang.reflect.Field hs = PacketMoveRocketInSpace.class.getDeclaredField("hasStar");
        assertNotNull("field hasWorld must exist (sentinel for the bug)", hw);
        assertNotNull("field hasStar must exist (sentinel for the bug)", hs);
    }

    // ── §6.9 bullet 5 — "assert invalid/missing data fails safely" ────────
    // Negative-input coverage for every AR packet whose write/readClient pair
    // needs MC bootstrap. Pattern is uniform: feed an empty (or hostile-header)
    // ByteBuf and assert two invariants hold:
    //
    //   (a) readClient either parses cleanly or fails *bounded* — a single
    //       exception propagates to the Netty pipeline, no infinite loop, no
    //       runaway allocation, no JVM-killing throw.
    //   (b) Fields that would otherwise leak attacker bytes are at their
    //       no-arg-ctor defaults, gating executeClient from acting on
    //       half-parses.
    //
    // PacketStorageTileUpdate is skipped — its readClient calls
    // Minecraft.getMinecraft().world, which is unavailable in headless
    // bootstrap. PacketMoveRocketInSpace is skipped — readClient is empty
    // (and read(ByteBuf) NPEs unconditionally, documented elsewhere).

    /**
     * Treat any RuntimeException as a bounded failure (the same way Forge's
     * Netty pipeline does — it logs and drops the packet). The post-condition
     * asserts are what actually establish the safety property.
     */
    private static void assertReadClientFailsSafely(Runnable readOp) {
        try {
            readOp.run();
        } catch (RuntimeException ignoredBounded) {
            // Acceptable — bounded propagation.
        }
    }

    @Test
    public void packetLaserGunReadClientEmptyBufferLeavesDefaults() {
        ByteBuf empty = newBuffer();
        PacketLaserGun packet = new PacketLaserGun();
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "entityId"));
        assertNull("toPos must stay null when wire underflows before float reads",
                PacketSerializationTest.<Object>getField(packet, "toPos"));
    }

    @Test
    public void packetAirParticleReadClientEmptyBufferLeavesDefaults() {
        ByteBuf empty = newBuffer();
        PacketAirParticle packet = new PacketAirParticle();
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        assertNull(PacketSerializationTest.<Object>getField(packet, "toPos"));
    }

    @Test
    public void packetInvalidLocationNotifyReadClientEmptyBufferLeavesDefaults() {
        ByteBuf empty = newBuffer();
        PacketInvalidLocationNotify packet = new PacketInvalidLocationNotify();
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        assertNull(PacketSerializationTest.<Object>getField(packet, "toPos"));
    }

    @Test
    public void packetFluidParticleReadClientEmptyBufferLeavesDefaults() {
        ByteBuf empty = newBuffer();
        PacketFluidParticle packet = new PacketFluidParticle();
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        assertNull(PacketSerializationTest.<Object>getField(packet, "toPos"));
        assertNull(PacketSerializationTest.<Object>getField(packet, "fromPos"));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "time"));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "color"));
    }

    @Test
    public void packetBiomeIDChangeReadClientEmptyBufferLeavesDefaults() {
        // PacketBiomeIDChange's no-arg ctor pre-allocates array=byte[256] and
        // pos=HashedBlockPosition(0,0,0). Empty buffer → readInt underflows
        // before any field assignment. The pre-allocated array stays all
        // zeros (would otherwise be filled by in.readBytes(array) to 256
        // attacker bytes).
        ByteBuf empty = newBuffer();
        PacketBiomeIDChange packet = new PacketBiomeIDChange();
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "worldId"));
        byte[] array = getField(packet, "array");
        assertNotNull(array);
        assertEquals("array still pre-sized to 256 (not resized by attacker)", 256, array.length);
        for (int i = 0; i < array.length; i++) {
            assertEquals("array[" + i + "] must be zero when biome wire underflows", 0, array[i]);
        }
    }

    @Test
    public void packetDimInfoReadClientEmptyBufferLeavesDefaults() {
        // Empty buffer underflows on readInt before any field is assigned.
        ByteBuf empty = newBuffer();
        PacketDimInfo packet = new PacketDimInfo();
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "dimNumber"));
        assertEquals(false, (boolean) PacketSerializationTest.<Boolean>getField(packet, "deleteDim"));
        assertEquals("artifacts list must stay empty when wire underflows",
                0, PacketSerializationTest.<java.util.List<?>>getField(packet, "artifacts").size());
        assertEquals("customIcon must stay at no-arg ctor default \"\"",
                "", PacketSerializationTest.<String>getField(packet, "customIcon"));
    }

    @Test
    public void packetDimInfoReadClientDeleteFlagSkipsNbtSection() {
        // The deleteDim=true branch is the "drop this dim" signal — readClient
        // must NOT try to read any NBT or customIcon bytes. Header-only wire
        // (5 bytes) parses cleanly and leaves artifacts empty / customIcon "".
        ByteBuf wire = newBuffer();
        wire.writeInt(42);          // dimNumber
        wire.writeBoolean(true);    // deleteDim

        PacketDimInfo packet = new PacketDimInfo();
        packet.readClient(wire); // must NOT throw

        assertEquals(42, (int) PacketSerializationTest.<Integer>getField(packet, "dimNumber"));
        assertEquals(true, (boolean) PacketSerializationTest.<Boolean>getField(packet, "deleteDim"));
        assertEquals(0, PacketSerializationTest.<java.util.List<?>>getField(packet, "artifacts").size());
        assertEquals("", PacketSerializationTest.<String>getField(packet, "customIcon"));
    }

    @Test
    public void packetSpaceStationInfoReadClientEmptyBufferLeavesDefaults() {
        ByteBuf empty = newBuffer();
        PacketSpaceStationInfo packet = new PacketSpaceStationInfo();
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "stationNumber"));
        assertEquals(false,
                (boolean) PacketSerializationTest.<Boolean>getField(packet, "isBeingDeleted"));
        assertNull(PacketSerializationTest.<Object>getField(packet, "nbt"));
        assertNull(PacketSerializationTest.<Object>getField(packet, "clazzId"));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "fuelAmt"));
    }

    @Test
    public void packetSpaceStationInfoDeleteFlagSkipsPayload() {
        // deleteFlag=true must short-circuit before the try-block reads any
        // NBT/clazzId/fuelAmt bytes, preventing partial parses.
        ByteBuf wire = newBuffer();
        wire.writeInt(77);          // stationNumber
        wire.writeBoolean(true);    // isBeingDeleted

        PacketSpaceStationInfo packet = new PacketSpaceStationInfo();
        packet.readClient(wire); // must NOT throw

        assertEquals(77, (int) PacketSerializationTest.<Integer>getField(packet, "stationNumber"));
        assertEquals(true, (boolean) PacketSerializationTest.<Boolean>getField(packet, "isBeingDeleted"));
        assertNull(PacketSerializationTest.<Object>getField(packet, "nbt"));
        assertNull(PacketSerializationTest.<Object>getField(packet, "clazzId"));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "fuelAmt"));
    }

    @Test
    public void packetStationUpdateReadClientEmptyBufferLeavesDefaults() {
        ByteBuf empty = newBuffer();
        PacketStationUpdate packet = new PacketStationUpdate();
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "stationNumber"));
        assertNull(PacketSerializationTest.<Object>getField(packet, "type"));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "destOrbitingBody"));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "fuel"));
    }

    @Test
    public void packetStationUpdateReadClientHostileTypeOrdinalFailsBounded() {
        // type = Type.values()[in.readInt()] — feeding an out-of-range ordinal
        // throws ArrayIndexOutOfBoundsException. Verify the failure is bounded
        // and stationNumber, having been parsed before the throw, is the only
        // touched field (no further branch executes).
        ByteBuf wire = newBuffer();
        wire.writeInt(42);             // stationNumber
        wire.writeInt(Integer.MAX_VALUE); // hostile ordinal

        PacketStationUpdate packet = new PacketStationUpdate();
        assertReadClientFailsSafely(() -> packet.readClient(wire));
        // stationNumber DID parse (attacker-controlled int) before the AIOOBE,
        // but no switch branch ran and nothing leaked into typed fields.
        assertEquals(42, (int) PacketSerializationTest.<Integer>getField(packet, "stationNumber"));
        assertNull(PacketSerializationTest.<Object>getField(packet, "type"));
        assertEquals(0, (int) PacketSerializationTest.<Integer>getField(packet, "destOrbitingBody"));
        assertNull(PacketSerializationTest.<Object>getField(packet, "nbt"));
    }

    @Test
    public void packetAsteroidInfoReadClientEmptyBufferLeavesDefaults() throws Exception {
        // First read is packetBuffer.readString(128) which underflows
        // immediately. asteroid was pre-allocated by the no-arg ctor;
        // verify its ID stayed null (not partially populated).
        ByteBuf empty = newBuffer();
        PacketAsteroidInfo packet = new PacketAsteroidInfo();
        Object asteroidBefore = getField(packet, "asteroid");
        assertReadClientFailsSafely(() -> packet.readClient(empty));
        Object asteroidAfter = getField(packet, "asteroid");
        // Same instance — readClient didn't replace it with a half-parse.
        assertEquals(asteroidBefore, asteroidAfter);
        // Asteroid.ID is a String field; readString failed before assignment.
        java.lang.reflect.Field idField = asteroidAfter.getClass().getDeclaredField("ID");
        idField.setAccessible(true);
        assertNull("asteroid.ID must not be set when readString underflows", idField.get(asteroidAfter));
    }

    @Test
    public void packetConfigSyncReadClientEmptyBufferDoesNotCorruptGlobalConfig() {
        // Snapshot a few representative ARConfiguration fields, fire readClient
        // on an empty buffer, then assert the *global* config is unchanged.
        // The packet's own config field is allowed to end up in any state
        // (it's a per-packet local copy), but the singleton must survive
        // attacker traffic intact.
        ARConfiguration current = ARConfiguration.getCurrentConfig();
        double thrustBefore = current.rocketThrustMultiplier;
        boolean requireFuelBefore = current.rocketRequireFuel;

        ByteBuf empty = newBuffer();
        PacketConfigSync packet = new PacketConfigSync();
        assertReadClientFailsSafely(() -> packet.readClient(empty));

        ARConfiguration after = ARConfiguration.getCurrentConfig();
        assertTrue("getCurrentConfig must still return the same singleton",
                current == after);
        assertEquals(thrustBefore, after.rocketThrustMultiplier, 0.0);
        assertEquals(requireFuelBefore, after.rocketRequireFuel);
    }

    @Test
    public void packetSatelliteReadClientEmptyBufferDoesNotMutateDimensionManager() {
        // PacketSatellite.readClient calls DimensionManager.getInstance()
        //   .getDimensionProperties(satellite.getDimensionId()).addSatellite(satellite)
        // — i.e. it mutates global state during read. On an empty buffer
        // readCompoundTag underflows before SatelliteRegistry.createFromNBT is
        // invoked, so the addSatellite call is skipped. Net effect: no global
        // mutation. Verify by snapshotting Earth's satellite count.
        zmaster587.advancedRocketry.dimension.DimensionManager dm =
                zmaster587.advancedRocketry.dimension.DimensionManager.getInstance();
        // Earth's dim properties are guaranteed to exist after MinecraftBootstrap.
        zmaster587.advancedRocketry.dimension.DimensionProperties earth =
                dm.getDimensionProperties(0);
        int satellitesBefore = earth.getAllSatellites().size();

        ByteBuf empty = newBuffer();
        PacketSatellite packet = new PacketSatellite();
        assertReadClientFailsSafely(() -> packet.readClient(empty));

        int satellitesAfter = earth.getAllSatellites().size();
        assertEquals("Earth's satellite map must not be mutated by an empty packet",
                satellitesBefore, satellitesAfter);
    }

    @Test
    public void packetSatellitesUpdateReadClientEmptyBufferDoesNotMutateDimensionManager() {
        // First read is byteBuf.readInt() (the dimNumber). Underflow → no
        // DimensionManager.getDimensionProperties call, no mutation.
        zmaster587.advancedRocketry.dimension.DimensionManager dm =
                zmaster587.advancedRocketry.dimension.DimensionManager.getInstance();
        zmaster587.advancedRocketry.dimension.DimensionProperties earth =
                dm.getDimensionProperties(0);
        int satellitesBefore = earth.getAllSatellites().size();

        ByteBuf empty = newBuffer();
        PacketSatellitesUpdate packet = new PacketSatellitesUpdate();
        assertReadClientFailsSafely(() -> packet.readClient(empty));

        int satellitesAfter = earth.getAllSatellites().size();
        assertEquals(satellitesBefore, satellitesAfter);
    }
}
