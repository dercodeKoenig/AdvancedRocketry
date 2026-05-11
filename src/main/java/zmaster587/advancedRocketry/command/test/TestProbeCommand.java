package zmaster587.advancedRocketry.command.test;

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.fuel.FuelRegistry;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.api.stations.ISpaceObject;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.entity.EntityRocket;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStationObject;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only {@code /artest} command tree (SMART §5).
 *
 * <p>Registered ONLY when system property {@code advancedrocketry.tests=true}
 * is present (see {@code AdvancedRocketry#serverStarting} dispatch). Commands
 * exposed by this class must NOT be available in normal gameplay — they exist
 * to give scenario tests deterministic, side-effect-free observability into
 * server-side state.</p>
 *
 * <p>Output format is a stable, parseable single-line JSON-like blob so that
 * {@code TestClient.execute(...)} can capture it via the standard "say marker"
 * protocol of the reusable test framework.</p>
 */
public class TestProbeCommand extends CommandBase {

    @Override
    @Nonnull
    public String getName() {
        return "artest";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/artest <registry|dim|planet|weather|atmosphere|oxygen|rocket|station|satellite|machine|terraforming|worldgen|commands|energy|infra|place|fill>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            send(sender, "{\"error\":\"missing subcommand\",\"usage\":\"" + getUsage(sender) + "\"}");
            return;
        }
        try {
            switch (args[0].toLowerCase()) {
                case "registry":
                    handleRegistry(sender, tail(args));
                    break;
                case "dim":
                    handleDim(sender, tail(args));
                    break;
                case "planet":
                    handlePlanet(sender, tail(args));
                    break;
                case "weather":
                    handleWeather(server, sender, tail(args));
                    break;
                case "rocket":
                    handleRocket(server, sender, tail(args));
                    break;
                case "station":
                    handleStation(sender, tail(args));
                    break;
                case "satellite":
                    handleSatellite(sender, tail(args));
                    break;
                case "atmosphere":
                    handleAtmosphere(sender, tail(args));
                    break;
                case "oxygen":
                    handleOxygen(server, sender, tail(args));
                    break;
                case "machine":
                    handleMachine(server, sender, tail(args));
                    break;
                case "terraforming":
                    handleTerraforming(sender, tail(args));
                    break;
                case "worldgen":
                    handleWorldgen(server, sender, tail(args));
                    break;
                case "commands":
                    handleCommands(server, sender, tail(args));
                    break;
                case "energy":
                    handleEnergy(server, sender, tail(args));
                    break;
                case "infra":
                    handleInfra(server, sender, tail(args));
                    break;
                case "place":
                    handlePlace(server, sender, tail(args));
                    break;
                case "fill":
                    handleFill(server, sender, tail(args));
                    break;
                default:
                    send(sender, "{\"error\":\"unknown subcommand\",\"sub\":\"" + args[0] + "\"}");
            }
        } catch (RuntimeException e) {
            send(sender, "{\"error\":\"" + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage()) + "\"}");
        }
    }

    // §5.1 Registry probes -----------------------------------------------------

    private void handleRegistry(ICommandSender sender, String[] args) {
        if (args.length == 0 || "summary".equalsIgnoreCase(args[0])) {
            Map<String, Long> counts = new LinkedHashMap<>();
            counts.put("blocks", count(ForgeRegistries.BLOCKS));
            counts.put("items", count(ForgeRegistries.ITEMS));
            counts.put("entities", count(ForgeRegistries.ENTITIES));
            counts.put("biomes", count(ForgeRegistries.BIOMES));
            counts.put("enchantments", count(ForgeRegistries.ENCHANTMENTS));
            counts.put("recipes", count(ForgeRegistries.RECIPES));
            counts.put("fluids", (long) FluidRegistry.getRegisteredFluids().size());
            send(sender, jsonMap(counts));
            return;
        }
        send(sender, "{\"error\":\"unknown registry subcommand\",\"sub\":\"" + args[0] + "\"}");
    }

    private static long count(net.minecraftforge.registries.IForgeRegistry<?> registry) {
        return registry == null ? -1L : registry.getKeys().size();
    }

    // §5.2 Dimension probes ----------------------------------------------------

    private void handleDim(ICommandSender sender, String[] args) {
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            Integer[] arDims = DimensionManager.getInstance().getRegisteredDimensions();
            Integer[] forgeDims = net.minecraftforge.common.DimensionManager.getStaticDimensionIDs();

            StringBuilder builder = new StringBuilder("{");
            builder.append("\"arDimensions\":[");
            for (int i = 0; i < arDims.length; i++) {
                if (i > 0) builder.append(',');
                builder.append(arDims[i]);
            }
            builder.append("],\"forgeDimensions\":[");
            for (int i = 0; i < forgeDims.length; i++) {
                if (i > 0) builder.append(',');
                builder.append(forgeDims[i]);
            }
            builder.append("]}");
            send(sender, builder.toString());
            return;
        }
        if ("info".equalsIgnoreCase(args[0]) && args.length >= 2) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            if (dim == Integer.MIN_VALUE) {
                send(sender, "{\"error\":\"invalid dim id\",\"value\":\"" + args[1] + "\"}");
                return;
            }
            net.minecraft.world.WorldServer world = net.minecraftforge.common.DimensionManager.getWorld(dim);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("dim", dim);
            info.put("loaded", world != null);
            info.put("providerClass", world != null ? world.provider.getClass().getName() : "null");
            info.put("isARPlanet", DimensionManager.getInstance().isDimensionCreated(dim));
            if (props != null) {
                info.put("name", props.getName());
                info.put("rotationalPeriod", props.rotationalPeriod);
                info.put("atmosphereDensity", props.getAtmosphereDensity());
                info.put("gravity", props.getGravitationalMultiplier());
                info.put("orbitalDistance", props.orbitalDist);
            }
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown dim subcommand\"}");
    }

    // §5.3 Planet/weather probes ----------------------------------------------

    private void handlePlanet(ICommandSender sender, String[] args) {
        if (args.length >= 2 && "info".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"unknown planet\",\"dim\":" + dim + "}");
                return;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("dim", dim);
            info.put("name", props.getName());
            info.put("starId", props.getStarId());
            info.put("parent", props.getParentPlanet());
            info.put("atmosphereDensity", props.getAtmosphereDensity());
            info.put("gravity", props.getGravitationalMultiplier());
            info.put("orbitalDistance", props.orbitalDist);
            info.put("rotationalPeriod", props.rotationalPeriod);
            info.put("hasRings", props.hasRings);
            info.put("hasOxygen", props.hasOxygen);
            info.put("seaLevel", props.getSeaLevel());
            info.put("rainStartLength", props.rainStartLength);
            info.put("thunderStartLength", props.thunderStartLength);
            info.put("rainMarker", props.getRainMarker());
            info.put("thunderMarker", props.getThunderMarker());
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown planet subcommand\"}");
    }

    private void handleWeather(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 2 && "get".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            net.minecraft.world.storage.WorldInfo info = world.getWorldInfo();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("dim", dim);
            map.put("worldInfoClass", info.getClass().getName());
            map.put("isRaining", info.isRaining());
            map.put("isThundering", info.isThundering());
            map.put("rainTime", info.getRainTime());
            map.put("thunderTime", info.getThunderTime());
            map.put("cleanWeatherTime", info.getCleanWeatherTime());
            map.put("rainStrength", world.getRainStrength(1.0f));
            map.put("thunderStrength", world.getThunderStrength(1.0f));
            send(sender, jsonMap(map));
            return;
        }
        if (args.length >= 4 && "set".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            String mode = args[2].toLowerCase();
            int ticks = parseIntOr(args[3], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            net.minecraft.world.storage.WorldInfo info = world.getWorldInfo();
            switch (mode) {
                case "clear":
                    info.setRaining(false);
                    info.setThundering(false);
                    info.setCleanWeatherTime(ticks);
                    break;
                case "rain":
                    info.setRaining(true);
                    info.setThundering(false);
                    info.setRainTime(ticks);
                    break;
                case "thunder":
                    info.setRaining(true);
                    info.setThundering(true);
                    info.setThunderTime(ticks);
                    break;
                default:
                    send(sender, "{\"error\":\"unknown weather mode\",\"mode\":\"" + mode + "\"}");
                    return;
            }
            send(sender, "{\"ok\":true,\"dim\":" + dim + ",\"mode\":\"" + mode + "\",\"ticks\":" + ticks + "}");
            return;
        }
        send(sender, "{\"error\":\"unknown weather subcommand\"}");
    }

    // §5.5 Rocket probes ------------------------------------------------------

    private void handleRocket(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            // Dimension argument optional — defaults to all loaded dimensions.
            int dimFilter = args.length >= 2 ? parseIntOr(args[1], Integer.MIN_VALUE) : Integer.MIN_VALUE;
            StringBuilder builder = new StringBuilder("{\"rockets\":[");
            boolean first = true;
            for (WorldServer world : server.worlds) {
                if (dimFilter != Integer.MIN_VALUE && world.provider.getDimension() != dimFilter) continue;
                for (Entity entity : world.loadedEntityList) {
                    if (!(entity instanceof EntityRocket)) continue;
                    if (!first) builder.append(',');
                    first = false;
                    builder.append("{\"id\":").append(entity.getEntityId())
                            .append(",\"dim\":").append(world.provider.getDimension())
                            .append(",\"pos\":[").append(entity.posX).append(',').append(entity.posY).append(',').append(entity.posZ).append("]}");
                }
            }
            builder.append("]}");
            send(sender, builder.toString());
            return;
        }
        if ("info".equalsIgnoreCase(args[0]) && args.length >= 2) {
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("entityId", rocket.getEntityId());
            info.put("dim", rocket.world.provider.getDimension());
            info.put("posX", rocket.posX);
            info.put("posY", rocket.posY);
            info.put("posZ", rocket.posZ);
            info.put("isInFlight", rocket.isInFlight());
            info.put("isInOrbit", rocket.isInOrbit());
            info.put("destinationDim", reflectInt(rocket, "destinationDimId"));
            info.put("hasStorage", rocket.storage != null);
            info.put("numPassengers", rocket.getPassengers().size());
            // Fuel snapshot per fuel type — using the public StatsRocket API.
            Map<String, Object> fuel = new LinkedHashMap<>();
            for (FuelRegistry.FuelType type : FuelRegistry.FuelType.values()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("amount", rocket.stats.getFuelAmount(type));
                entry.put("capacity", rocket.stats.getFuelCapacity(type));
                fuel.put(type.name(), entry);
            }
            info.put("fuel", fuel);
            info.put("thrust", rocket.stats.getThrust());
            info.put("weight_no_fuel", rocket.stats.getWeight_NoFuel());
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown rocket subcommand — try list|info <id>\"}");
    }

    private static EntityRocket findRocket(MinecraftServer server, int entityId) {
        for (WorldServer world : server.worlds) {
            Entity e = world.getEntityByID(entityId);
            if (e instanceof EntityRocket) {
                return (EntityRocket) e;
            }
        }
        return null;
    }

    // §5.6 Station probes -----------------------------------------------------

    private void handleStation(ICommandSender sender, String[] args) {
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            StringBuilder builder = new StringBuilder("{\"stations\":[");
            boolean first = true;
            for (ISpaceObject station : SpaceObjectManager.getSpaceManager().getSpaceObjects()) {
                if (!first) builder.append(',');
                first = false;
                builder.append("{\"id\":").append(station.getId())
                        .append(",\"orbiting\":").append(station.getOrbitingPlanetId()).append('}');
            }
            builder.append("]}");
            send(sender, builder.toString());
            return;
        }
        if ("info".equalsIgnoreCase(args[0]) && args.length >= 2) {
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            ISpaceObject station = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (station == null) {
                send(sender, "{\"error\":\"station not found\",\"id\":" + id + "}");
                return;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id", station.getId());
            info.put("orbitingPlanetId", station.getOrbitingPlanetId());
            info.put("destOrbitingBody", station.getDestOrbitingBody());
            info.put("orbitalDistance", station.getOrbitalDistance());
            info.put("isAnchored", station.isAnchored());
            info.put("transitionTime", station.getTransitionTime());
            zmaster587.libVulpes.util.HashedBlockPosition spawn = station.getSpawnLocation();
            if (spawn != null) {
                info.put("spawnX", spawn.x);
                info.put("spawnY", spawn.y);
                info.put("spawnZ", spawn.z);
            }
            if (station instanceof SpaceStationObject) {
                info.put("fuelAmount", ((SpaceStationObject) station).getFuelAmount());
            }
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown station subcommand — try list|info <id>\"}");
    }

    // §5.6 Satellite probes ---------------------------------------------------

    private void handleSatellite(ICommandSender sender, String[] args) {
        if ("list".equalsIgnoreCase(args[0]) && args.length >= 2) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                return;
            }
            // satellites is a private HashMap<Long, SatelliteBase> — expose ids via reflection.
            try {
                java.lang.reflect.Field f = DimensionProperties.class.getDeclaredField("satellites");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Long, SatelliteBase> satMap = (Map<Long, SatelliteBase>) f.get(props);
                StringBuilder builder = new StringBuilder("{\"dim\":").append(dim).append(",\"satellites\":[");
                boolean first = true;
                for (Map.Entry<Long, SatelliteBase> entry : satMap.entrySet()) {
                    if (!first) builder.append(',');
                    first = false;
                    SatelliteBase sat = entry.getValue();
                    builder.append("{\"id\":").append(entry.getKey())
                            .append(",\"type\":\"").append(escapeJson(sat.getProperties().getSatelliteType()))
                            .append("\",\"powerGen\":").append(sat.getProperties().getPowerGeneration())
                            .append('}');
                }
                builder.append("]}");
                send(sender, builder.toString());
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if ("info".equalsIgnoreCase(args[0]) && args.length >= 3) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                return;
            }
            SatelliteBase sat = props.getSatellite(satId);
            if (sat == null) {
                send(sender, "{\"error\":\"satellite not found\",\"dim\":" + dim + ",\"id\":" + satId + "}");
                return;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id", sat.getId());
            info.put("dim", sat.getDimensionId());
            info.put("type", sat.getProperties().getSatelliteType());
            info.put("powerGen", sat.getProperties().getPowerGeneration());
            info.put("powerStorage", sat.getProperties().getPowerStorage());
            info.put("maxData", sat.getProperties().getMaxDataStorage());
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown satellite subcommand — try list <dim>|info <dim> <id>\"}");
    }

    // §5.7 Atmosphere probe ---------------------------------------------------

    private void handleAtmosphere(ICommandSender sender, String[] args) {
        if (args.length >= 5 && "get".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            AtmosphereHandler handler = AtmosphereHandler.getOxygenHandler(dim);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("dim", dim);
            info.put("pos", new int[]{x, y, z});
            if (handler == null) {
                // No per-dim handler → fall back to the planet's default atmosphere.
                DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
                if (props == null) {
                    send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                    return;
                }
                IAtmosphere atm = props.getAtmosphere();
                info.put("source", "dimension-default");
                info.put("type", atm.getUnlocalizedName());
                info.put("breathable", atm.isBreathable());
            } else {
                IAtmosphere atm = handler.getAtmosphereType(new BlockPos(x, y, z));
                info.put("source", "block-handler");
                info.put("type", atm.getUnlocalizedName());
                info.put("breathable", atm.isBreathable());
            }
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown atmosphere subcommand — try get <dim> <x> <y> <z>\"}");
    }

    // §5.7 Oxygen probe -------------------------------------------------------

    private void handleOxygen(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 2 && "player".equalsIgnoreCase(args[0])) {
            String name = args[1];
            EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(name);
            if (player == null) {
                send(sender, "{\"error\":\"player not found\",\"name\":\"" + escapeJson(name) + "\"}");
                return;
            }
            AtmosphereHandler handler = AtmosphereHandler.getOxygenHandler(player.world.provider.getDimension());
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", name);
            info.put("dim", player.world.provider.getDimension());
            info.put("posX", player.posX);
            info.put("posY", player.posY);
            info.put("posZ", player.posZ);
            if (handler != null) {
                IAtmosphere atm = handler.getAtmosphereType(player);
                info.put("atmosphere", atm.getUnlocalizedName());
                info.put("breathable", atm.isBreathable());
                info.put("pressure", handler.getAtmospherePressure(player));
            } else {
                info.put("atmosphere", "no-handler");
            }
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown oxygen subcommand — try player <name>\"}");
    }

    // §5.4 Machine probes -----------------------------------------------------

    private void handleMachine(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 6 && "tick-until".equalsIgnoreCase(args[0])) {
            handleMachineTickUntil(server, sender, args);
            return;
        }
        if (args.length >= 4 && "info".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], sender.getEntityWorld().provider.getDimension());
            int x = parseIntOr(args[1], 0); // legacy fallback if dim omitted
            // Accept either: machine info <x> <y> <z>  (current dim)
            //              : machine info <dim> <x> <y> <z>
            int posX, posY, posZ;
            World world;
            if (args.length == 4) {
                world = sender.getEntityWorld();
                posX = parseIntOr(args[1], 0);
                posY = parseIntOr(args[2], 0);
                posZ = parseIntOr(args[3], 0);
            } else {
                world = server.getWorld(dim);
                posX = parseIntOr(args[2], 0);
                posY = parseIntOr(args[3], 0);
                posZ = parseIntOr(args[4], 0);
            }
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }

            BlockPos pos = new BlockPos(posX, posY, posZ);
            TileEntity tile = world.getTileEntity(pos);
            if (tile == null) {
                send(sender, "{\"error\":\"no tile entity\",\"pos\":[" + posX + "," + posY + "," + posZ + "]}");
                return;
            }

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("dim", world.provider.getDimension());
            info.put("posX", posX);
            info.put("posY", posY);
            info.put("posZ", posZ);
            info.put("tileClass", tile.getClass().getName());

            // libVulpes TileMultiBlock public API: isComplete()
            try {
                java.lang.reflect.Method m = tile.getClass().getMethod("isComplete");
                info.put("isComplete", m.invoke(tile));
            } catch (NoSuchMethodException ignored) {
                info.put("isComplete", "n/a");
            } catch (ReflectiveOperationException e) {
                info.put("isCompleteError", e.getMessage());
            }
            // TileMultiPowerConsumer adds isRunning + getMachineEnabled.
            for (String name : new String[] {"isRunning", "getMachineEnabled"}) {
                try {
                    java.lang.reflect.Method m = tile.getClass().getMethod(name);
                    info.put(name, m.invoke(tile));
                } catch (NoSuchMethodException ignored) {
                    // skip — tile is not a power consumer
                } catch (ReflectiveOperationException e) {
                    info.put(name + "Error", e.getMessage());
                }
            }
            // Progress (slot 0) — most multiblock recipes report current/total here.
            try {
                java.lang.reflect.Method get = tile.getClass().getMethod("getProgress", int.class);
                java.lang.reflect.Method total = tile.getClass().getMethod("getTotalProgress", int.class);
                info.put("progress", get.invoke(tile, 0));
                info.put("totalProgress", total.invoke(tile, 0));
            } catch (NoSuchMethodException ignored) {
                // skip — tile has no progress bar
            } catch (ReflectiveOperationException e) {
                info.put("progressError", e.getMessage());
            }

            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown machine subcommand — try info [dim] <x> <y> <z>\"}");
    }

    /**
     * {@code /artest machine tick-until <dim> <x> <y> <z> <condition> <timeoutTicks>}
     *
     * <p>Polls the tile at the given position once per server tick (via
     * {@link MinecraftServer#getCurrentTime()}-based wait) until either
     * {@code condition} matches or {@code timeoutTicks} elapses. Conditions:</p>
     * <ul>
     *   <li>{@code complete} — {@code isComplete()} returns true</li>
     *   <li>{@code running} — {@code isRunning()} returns true</li>
     *   <li>{@code idle} — {@code isRunning()} returns false (machine done)</li>
     *   <li>{@code progress=N} — {@code getProgress(0)} reaches at least N</li>
     * </ul>
     *
     * <p>Returns {@code {"matched":true, "ticks":N}} on success or
     * {@code {"matched":false, "ticks":timeout, "lastSeen":...}} on timeout.</p>
     *
     * <p>NOTE: this probe blocks the server's main thread for up to
     * {@code timeoutTicks * 50ms} via Thread.sleep — fine for short waits but
     * keep the timeout below ~1200 ticks (1 minute) to avoid harness deadline
     * issues.</p>
     */
    private void handleMachineTickUntil(MinecraftServer server, ICommandSender sender, String[] args) {
        // tick-until <dim> <x> <y> <z> <condition> <timeoutTicks>
        int dim = parseIntOr(args[1], Integer.MIN_VALUE);
        int x = parseIntOr(args[2], 0), y = parseIntOr(args[3], 0), z = parseIntOr(args[4], 0);
        String condition = args[5].toLowerCase();
        int timeoutTicks = args.length >= 7 ? parseIntOr(args[6], 100) : 100;
        if (timeoutTicks > 1200) {
            send(sender, "{\"error\":\"timeoutTicks > 1200 — refuse to block server thread that long\"}");
            return;
        }

        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);

        int progressTarget = -1;
        if (condition.startsWith("progress=")) {
            progressTarget = parseIntOr(condition.substring("progress=".length()), -1);
            condition = "progress";
        }

        Object lastSeen = "n/a";
        for (int tick = 0; tick < timeoutTicks; tick++) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile == null) {
                send(sender, "{\"error\":\"no tile entity\",\"pos\":[" + x + "," + y + "," + z + "],\"ticks\":" + tick + "}");
                return;
            }
            try {
                switch (condition) {
                    case "complete": {
                        Object v = tile.getClass().getMethod("isComplete").invoke(tile);
                        lastSeen = v;
                        if (Boolean.TRUE.equals(v)) {
                            send(sender, "{\"matched\":true,\"ticks\":" + tick + ",\"condition\":\"complete\"}");
                            return;
                        }
                        break;
                    }
                    case "running": {
                        Object v = tile.getClass().getMethod("isRunning").invoke(tile);
                        lastSeen = v;
                        if (Boolean.TRUE.equals(v)) {
                            send(sender, "{\"matched\":true,\"ticks\":" + tick + ",\"condition\":\"running\"}");
                            return;
                        }
                        break;
                    }
                    case "idle": {
                        Object v = tile.getClass().getMethod("isRunning").invoke(tile);
                        lastSeen = v;
                        if (Boolean.FALSE.equals(v)) {
                            send(sender, "{\"matched\":true,\"ticks\":" + tick + ",\"condition\":\"idle\"}");
                            return;
                        }
                        break;
                    }
                    case "progress": {
                        Object v = tile.getClass().getMethod("getProgress", int.class).invoke(tile, 0);
                        lastSeen = v;
                        if (v instanceof Integer && (Integer) v >= progressTarget) {
                            send(sender, "{\"matched\":true,\"ticks\":" + tick + ",\"progress\":" + v + "}");
                            return;
                        }
                        break;
                    }
                    default:
                        send(sender, "{\"error\":\"unknown condition\",\"value\":\"" + escapeJson(condition) + "\"}");
                        return;
                }
            } catch (NoSuchMethodException e) {
                send(sender, "{\"error\":\"tile lacks " + e.getMessage() + "\"}");
                return;
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\"" + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        send(sender, "{\"matched\":false,\"ticks\":" + timeoutTicks + ",\"lastSeen\":\""
                + escapeJson(String.valueOf(lastSeen)) + "\"}");
    }

    // §5.8 Terraforming probe -------------------------------------------------

    private void handleTerraforming(ICommandSender sender, String[] args) {
        if (args.length >= 2 && "info".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                return;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("dim", dim);
            info.put("name", props.getName());
            info.put("originalAtmosphere", reflectInt(props, "originalAtmosphereDensity"));
            info.put("currentAtmosphere", props.getAtmosphereDensity());
            // Safe access to terraforming proxy state — these methods may NPE if
            // proxylists hasn't been initialized for the dim yet.
            try {
                boolean inited = DimensionProperties.proxylists.isinitialized(dim);
                info.put("proxyInitialized", inited);
                if (inited) {
                    info.put("protectingBlockCount",
                            DimensionProperties.proxylists.getProtectingBlocksForDimension(dim).size());
                    info.put("chunksFullyTerraformed",
                            DimensionProperties.proxylists.getChunksFullyTerraformed(dim).size());
                    info.put("chunksFullyBiomeChanged",
                            DimensionProperties.proxylists.getChunksFullyBiomeChanged(dim).size());
                    info.put("helperPresent", DimensionProperties.proxylists.gethelper(dim) != null);
                }
            } catch (Exception e) {
                info.put("proxyError", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown terraforming subcommand — try info <dim>\"}");
    }

    // §5.8 Worldgen probe -----------------------------------------------------

    private void handleWorldgen(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 4 && "sample".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int chunkX = parseIntOr(args[2], 0);
            int chunkZ = parseIntOr(args[3], 0);
            WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            // getChunk(int, int) force-loads + populates if needed.
            Chunk chunk = world.getChunkProvider().provideChunk(chunkX, chunkZ);
            if (chunk == null || !chunk.isLoaded()) {
                send(sender, "{\"error\":\"chunk failed to load\",\"chunk\":[" + chunkX + "," + chunkZ + "]}");
                return;
            }

            // Sample center of chunk: top non-air block + biome.
            int worldX = (chunkX << 4) + 8;
            int worldZ = (chunkZ << 4) + 8;
            int topY = chunk.getHeightValue(worldX & 15, worldZ & 15);
            BlockPos topPos = new BlockPos(worldX, Math.max(0, topY - 1), worldZ);
            IBlockState topBlock = world.getBlockState(topPos);
            Biome biome = world.getBiome(topPos);

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("dim", dim);
            info.put("chunkX", chunkX);
            info.put("chunkZ", chunkZ);
            info.put("centerWorldX", worldX);
            info.put("centerWorldZ", worldZ);
            info.put("topY", topY);
            info.put("topBlock", topBlock.getBlock().getRegistryName() == null
                    ? "minecraft:air" : topBlock.getBlock().getRegistryName().toString());
            info.put("biome", biome.getRegistryName() == null
                    ? "unknown" : biome.getRegistryName().toString());
            info.put("biomeId", Biome.getIdForBiome(biome));
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown worldgen subcommand — try sample <dim> <chunkX> <chunkZ>\"}");
    }

    // §5 Commands probe -------------------------------------------------------

    private void handleCommands(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            // Use a TreeSet for stable / sorted output so test diffs stay readable.
            java.util.Set<String> sortedNames = new java.util.TreeSet<>();
            for (ICommand cmd : server.getCommandManager().getCommands().values()) {
                sortedNames.add(cmd.getName());
            }
            StringBuilder builder = new StringBuilder("{\"commands\":[");
            boolean first = true;
            for (String name : sortedNames) {
                if (!first) builder.append(',');
                first = false;
                builder.append('"').append(escapeJson(name)).append('"');
            }
            builder.append("]}");
            send(sender, builder.toString());
            return;
        }
        send(sender, "{\"error\":\"unknown commands subcommand — try list\"}");
    }

    // §5.16 Energy probe -------------------------------------------------------

    private void handleEnergy(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 4 && "stored".equalsIgnoreCase(args[0])) {
            // energy stored [dim] <x> <y> <z>  — single signature: dim required for clarity.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = args.length >= 5 ? parseIntOr(args[4], 0) : 0;
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            BlockPos pos = new BlockPos(x, y, z);
            TileEntity tile = world.getTileEntity(pos);
            if (tile == null) {
                send(sender, "{\"error\":\"no tile entity\",\"pos\":[" + x + "," + y + "," + z + "]}");
                return;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("dim", dim);
            info.put("posX", x);
            info.put("posY", y);
            info.put("posZ", z);
            info.put("tileClass", tile.getClass().getName());

            // Try Forge's energy capability on every face. Reports the first face
            // that exposes IEnergyStorage and its current/max values.
            net.minecraftforge.energy.IEnergyStorage es = null;
            String face = "null";
            for (net.minecraft.util.EnumFacing dir : net.minecraft.util.EnumFacing.values()) {
                if (tile.hasCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, dir)) {
                    es = tile.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, dir);
                    face = dir.name();
                    break;
                }
            }
            if (es == null && tile.hasCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null)) {
                es = tile.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
                face = "null";
            }
            if (es == null) {
                info.put("hasEnergy", false);
            } else {
                info.put("hasEnergy", true);
                info.put("energyFace", face);
                info.put("energyStored", es.getEnergyStored());
                info.put("energyMax", es.getMaxEnergyStored());
                info.put("canExtract", es.canExtract());
                info.put("canReceive", es.canReceive());
            }
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown energy subcommand — try stored <dim> <x> <y> <z>\"}");
    }

    // §5.10 Rocket infrastructure probe ---------------------------------------

    private void handleInfra(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 4 && "info".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = args.length >= 5 ? parseIntOr(args[4], 0) : 0;
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (tile == null) {
                send(sender, "{\"error\":\"no tile entity\",\"pos\":[" + x + "," + y + "," + z + "]}");
                return;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("dim", dim);
            info.put("posX", x); info.put("posY", y); info.put("posZ", z);
            info.put("tileClass", tile.getClass().getName());
            if (tile instanceof zmaster587.advancedRocketry.api.IInfrastructure) {
                zmaster587.advancedRocketry.api.IInfrastructure infra =
                        (zmaster587.advancedRocketry.api.IInfrastructure) tile;
                info.put("isInfrastructure", true);
                info.put("maxLinkDistance", infra.getMaxLinkDistance());
                info.put("disconnectOnLiftOff", infra.disconnectOnLiftOff());
            } else {
                info.put("isInfrastructure", false);
            }
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown infra subcommand — try info <dim> <x> <y> <z>\"}");
    }

    // §9.2 Fixture-building primitives -----------------------------------------

    private void handlePlace(MinecraftServer server, ICommandSender sender, String[] args) {
        // place <dim> <x> <y> <z> <block-id> [meta]
        if (args.length < 5) {
            send(sender, "{\"error\":\"usage: /artest place <dim> <x> <y> <z> <block-id> [meta]\"}");
            return;
        }
        int dim = parseIntOr(args[0], Integer.MIN_VALUE);
        int x = parseIntOr(args[1], 0);
        int y = parseIntOr(args[2], 0);
        int z = parseIntOr(args[3], 0);
        String blockId = args[4];
        int meta = args.length >= 6 ? parseIntOr(args[5], 0) : 0;

        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        net.minecraft.block.Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
        if (block == null) {
            send(sender, "{\"error\":\"unknown block id\",\"id\":\"" + escapeJson(blockId) + "\"}");
            return;
        }

        @SuppressWarnings("deprecation")
        IBlockState state = block.getStateFromMeta(meta);
        boolean placed = world.setBlockState(new BlockPos(x, y, z), state);
        send(sender, "{\"ok\":true,\"placed\":" + placed + ",\"block\":\"" + escapeJson(blockId)
                + "\",\"pos\":[" + x + "," + y + "," + z + "]}");
    }

    private void handleFill(MinecraftServer server, ICommandSender sender, String[] args) {
        // fill <dim> <x1> <y1> <z1> <x2> <y2> <z2> <block-id> [meta]
        if (args.length < 8) {
            send(sender, "{\"error\":\"usage: /artest fill <dim> <x1> <y1> <z1> <x2> <y2> <z2> <block-id> [meta]\"}");
            return;
        }
        int dim = parseIntOr(args[0], Integer.MIN_VALUE);
        int x1 = parseIntOr(args[1], 0); int y1 = parseIntOr(args[2], 0); int z1 = parseIntOr(args[3], 0);
        int x2 = parseIntOr(args[4], 0); int y2 = parseIntOr(args[5], 0); int z2 = parseIntOr(args[6], 0);
        String blockId = args[7];
        int meta = args.length >= 9 ? parseIntOr(args[8], 0) : 0;

        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        net.minecraft.block.Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
        if (block == null) {
            send(sender, "{\"error\":\"unknown block id\",\"id\":\"" + escapeJson(blockId) + "\"}");
            return;
        }
        @SuppressWarnings("deprecation")
        IBlockState state = block.getStateFromMeta(meta);

        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        // Soft cap to keep tests deterministic and quick — refuse pathological fills.
        if (volume > 32_768) {
            send(sender, "{\"error\":\"fill volume too large\",\"volume\":" + volume + ",\"cap\":32768}");
            return;
        }

        int placed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.setBlockState(new BlockPos(x, y, z), state)) {
                        placed++;
                    }
                }
            }
        }
        send(sender, "{\"ok\":true,\"placed\":" + placed + ",\"block\":\"" + escapeJson(blockId)
                + "\",\"volume\":" + volume + "}");
    }

    // ---- helpers -------------------------------------------------------------

    @Override
    @Nonnull
    public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender,
                                          @Nonnull String[] args, @javax.annotation.Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args,
                    "registry", "dim", "planet", "weather", "rocket", "station", "satellite",
                    "atmosphere", "oxygen", "machine", "terraforming", "worldgen", "commands",
                    "energy", "infra", "place", "fill");
        }
        return Collections.emptyList();
    }

    private static String[] tail(String[] args) {
        return args.length <= 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
    }

    private static int parseIntOr(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }

    private static long parseLongOr(String s, long fallback) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return fallback; }
    }

    /** Reads a private int field of an arbitrary object (used for EntityRocket.destinationDimId etc.). */
    private static int reflectInt(Object target, String fieldName) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.getInt(target);
        } catch (ReflectiveOperationException e) {
            return Integer.MIN_VALUE;
        }
    }

    private static void send(ICommandSender sender, String text) {
        sender.sendMessage(new TextComponentString(text));
    }

    private static String jsonMap(Map<String, ?> map) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (!first) builder.append(',');
            first = false;
            builder.append('"').append(escapeJson(entry.getKey())).append("\":");
            Object v = entry.getValue();
            if (v == null) {
                builder.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                builder.append(v);
            } else {
                builder.append('"').append(escapeJson(v.toString())).append('"');
            }
        }
        builder.append('}');
        return builder.toString();
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
