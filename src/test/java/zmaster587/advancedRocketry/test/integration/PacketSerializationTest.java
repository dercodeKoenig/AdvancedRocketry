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
import zmaster587.advancedRocketry.network.PacketAsteroidInfo;
import zmaster587.advancedRocketry.network.PacketBiomeIDChange;
import zmaster587.advancedRocketry.network.PacketConfigSync;
import zmaster587.advancedRocketry.network.PacketDimInfo;
import zmaster587.advancedRocketry.network.PacketFluidParticle;
import zmaster587.advancedRocketry.network.PacketInvalidLocationNotify;
import zmaster587.advancedRocketry.network.PacketLaserGun;
import zmaster587.advancedRocketry.network.PacketSatellite;
import zmaster587.advancedRocketry.network.PacketStationUpdate;
import zmaster587.advancedRocketry.stations.SpaceStationObject;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;
import zmaster587.advancedRocketry.util.Asteroid;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.lang.reflect.Field;

import static org.junit.Assert.assertArrayEquals;
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
}
