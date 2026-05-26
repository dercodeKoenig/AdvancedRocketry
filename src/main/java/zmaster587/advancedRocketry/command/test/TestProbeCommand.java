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
        return "/artest <registry|dim|planet|weather|atmosphere|oxygen|rocket|station|satellite|machine|terraforming|worldgen|commands|energy|infra|place|fill|fixture|tile|hatch|selector>";
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
                case "assembler":
                    handleAssembler(server, sender, tail(args));
                    break;
                case "station":
                    handleStation(sender, tail(args));
                    break;
                case "satellite":
                    handleSatellite(server, sender, tail(args));
                    break;
                case "satellite-builder":
                    handleSatelliteBuilder(server, sender, tail(args));
                    break;
                case "atmosphere":
                    handleAtmosphere(server, sender, tail(args));
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
                case "fixture":
                    handleFixture(server, sender, tail(args));
                    break;
                case "tile":
                    handleTile(server, sender, tail(args));
                    break;
                case "hatch":
                    handleHatch(server, sender, tail(args));
                    break;
                case "selector":
                    handleSelector(server, sender, tail(args));
                    break;
                case "fluid":
                    handleFluid(server, sender, tail(args));
                    break;
                case "vent":
                    handleVent(server, sender, tail(args));
                    break;
                case "item":
                    handleItem(server, sender, tail(args));
                    break;
                case "enchant":
                    handleEnchant(server, sender, tail(args));
                    break;
                case "beacon":
                    handleBeacon(server, sender, tail(args));
                    break;
                case "entity":
                    handleEntity(server, sender, tail(args));
                    break;
                case "block":
                    handleBlock(server, sender, tail(args));
                    break;
                case "field":
                    handleField(server, sender, tail(args));
                    break;
                case "scrubber":
                    handleScrubber(server, sender, tail(args));
                    break;
                case "gascharge":
                    handleGasCharge(server, sender, tail(args));
                    break;
                case "pipe":
                    handlePipe(server, sender, tail(args));
                    break;
                case "tp":
                    handleTp(server, sender, tail(args));
                    break;
                case "event":
                    handleEvent(server, sender, tail(args));
                    break;
                case "chunk":
                    handleChunk(server, sender, tail(args));
                    break;
                case "server":
                    handleServer(server, sender, tail(args));
                    break;
                case "player":
                    handlePlayer(server, sender, tail(args));
                    break;
                case "seal-detector":
                    handleSealDetector(server, sender, tail(args));
                    break;
                case "mission":
                    handleMission(server, sender, tail(args));
                    break;
                case "config":
                    handleConfig(sender, tail(args));
                    break;
                case "star":
                    handleStar(sender, tail(args));
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
            info.put("biomeProviderClass", (world != null && world.getBiomeProvider() != null)
                    ? world.getBiomeProvider().getClass().getName() : "null");
            info.put("chunkGeneratorClass", chunkGeneratorClassOf(world));
            info.put("saveDir", (world != null && world.provider.getSaveFolder() != null)
                    ? world.provider.getSaveFolder() : "null");
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
        if ("celestial-angle".equalsIgnoreCase(args[0]) && args.length >= 3) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            if (dim == Integer.MIN_VALUE) {
                send(sender, "{\"error\":\"invalid dim id\",\"value\":\"" + args[1] + "\"}");
                return;
            }
            long worldTime = parseLongOr(args[2], Long.MIN_VALUE);
            if (worldTime == Long.MIN_VALUE) {
                send(sender, "{\"error\":\"invalid worldTime\",\"value\":\"" + args[2] + "\"}");
                return;
            }
            net.minecraft.world.WorldServer world = net.minecraftforge.common.DimensionManager.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            // Pure computation — provider math is read-only at this entry point,
            // so callers can probe the same (dim, worldTime) twice and rely on
            // bit-for-bit identical results.
            float angle = world.provider.calculateCelestialAngle(worldTime, 0.0f);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("dim", dim);
            info.put("worldTime", worldTime);
            info.put("partialTicks", 0.0f);
            info.put("angle", angle);
            send(sender, jsonMap(info));
            return;
        }
        if ("load".equalsIgnoreCase(args[0]) && args.length >= 2) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            if (dim == Integer.MIN_VALUE) {
                send(sender, "{\"error\":\"invalid dim id\",\"value\":\"" + args[1] + "\"}");
                return;
            }
            // Mirror the keepDimensionLoaded + initDimension idiom used by the
            // weather/worldgen probes — pin the dim so AR's per-tick unload
            // doesn't drop it again immediately after load.
            net.minecraftforge.common.DimensionManager.keepDimensionLoaded(dim, true);
            if (net.minecraftforge.common.DimensionManager.getWorld(dim) == null) {
                net.minecraftforge.common.DimensionManager.initDimension(dim);
            }
            net.minecraft.world.WorldServer world = net.minecraftforge.common.DimensionManager.getWorld(dim);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("dim", dim);
            info.put("loaded", world != null);
            info.put("providerClass", world != null ? world.provider.getClass().getName() : "null");
            info.put("isARPlanet", DimensionManager.getInstance().isDimensionCreated(dim));
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
            info.put("averageTemperature", props.averageTemperature);
            info.put("genType", props.getGenType());
            IBlockState ocean = props.getOceanBlock();
            // null is meaningful — vanilla water fallback — so emit explicitly.
            info.put("oceanBlock",
                    ocean == null ? null : ocean.getBlock().getRegistryName().toString());
            info.put("skyColor", floatArrayToList(props.skyColor));
            info.put("sunriseSunsetColors", floatArrayToList(props.sunriseSunsetColors));
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown planet subcommand\"}");
    }

    private static List<Double> floatArrayToList(float[] arr) {
        if (arr == null) return null;
        List<Double> out = new java.util.ArrayList<>(arr.length);
        for (float f : arr) out.add((double) f);
        return out;
    }

    private void handleWeather(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 2 && "get".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            // Same pinning as `weather set` — ensures we observe the same
            // WorldServer instance that previous /artest weather set wrote to.
            net.minecraftforge.common.DimensionManager.keepDimensionLoaded(dim, true);
            if (net.minecraftforge.common.DimensionManager.getWorld(dim) == null) {
                net.minecraftforge.common.DimensionManager.initDimension(dim);
            }
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
            // Pin the dim loaded so AR's per-tick unload doesn't drop our
            // weather state before the test reads it back.
            net.minecraftforge.common.DimensionManager.keepDimensionLoaded(dim, true);
            if (net.minecraftforge.common.DimensionManager.getWorld(dim) == null) {
                net.minecraftforge.common.DimensionManager.initDimension(dim);
            }
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
                    // Resetting cleanWeatherTime is mandatory — otherwise the
                    // server's updateWeatherBody() forces isRaining=false next
                    // tick (cleanWeatherTime > 0 ⇒ forced clear).
                    info.setCleanWeatherTime(0);
                    info.setRaining(true);
                    info.setThundering(false);
                    info.setRainTime(ticks);
                    break;
                case "thunder":
                    info.setCleanWeatherTime(0);
                    info.setRaining(true);
                    info.setThundering(true);
                    info.setRainTime(ticks);
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
                            .append(",\"uuid\":\"").append(entity.getPersistentID().toString()).append("\"")
                            .append(",\"dim\":").append(world.provider.getDimension())
                            .append(",\"pos\":[").append(entity.posX).append(',').append(entity.posY).append(',').append(entity.posZ).append("]}");
                }
            }
            builder.append("]}");
            send(sender, builder.toString());
            return;
        }
        if ("assemble".equalsIgnoreCase(args[0]) && args.length >= 4) {
            handleRocketAssemble(server, sender, args);
            return;
        }
        if ("launch".equalsIgnoreCase(args[0]) && args.length >= 2) {
            handleRocketLaunch(server, sender, args);
            return;
        }
        if ("fuel".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // /artest rocket fuel <entityId> — exposes stats.getFuelAmount /
            // getFuelCapacity per FuelType + primary rocket fuel type.
            // Consumers: TileFuelingStation cause-effect tests that need to
            // assert "rocket received fuel" without poking the rocket's
            // dataManager directly.
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType primary = rocket.getRocketFuelType();
            StringBuilder builder = new StringBuilder("{\"entityId\":").append(entityId)
                    .append(",\"primaryFuelType\":\"")
                    .append(primary == null ? "null" : primary.name())
                    .append("\",\"fuels\":{");
            boolean first = true;
            for (zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType ft :
                    zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType.values()) {
                if (!first) builder.append(',');
                first = false;
                int amount = rocket.getFuelAmount(ft);
                int capacity = rocket.getFuelCapacity(ft);
                builder.append("\"").append(ft.name()).append("\":{\"amount\":")
                        .append(amount).append(",\"capacity\":").append(capacity).append("}");
            }
            builder.append("}}");
            send(sender, builder.toString());
            return;
        }
        if ("override-landing".equalsIgnoreCase(args[0]) && args.length >= 3) {
            // /artest rocket override-landing <rocketId> <stationId> —
            // production cause-effect: TileGuidanceComputer.overrideLandingStation
            // → getStationLocation(commit=true) → either marks an existing
            // chosen pad as occupied OR calls getNextLandingPad(true). Used
            // by the A5 dock cause-effect tests: assert that this production
            // method's side effect actually reaches station-side state.
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            int stationId = parseIntOr(args[2], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            zmaster587.advancedRocketry.tile.TileGuidanceComputer gc =
                    rocket.storage == null ? null : rocket.storage.getGuidanceComputer();
            if (gc == null) {
                send(sender, "{\"error\":\"rocket has no guidance computer\",\"entityId\":"
                        + entityId + "}");
                return;
            }
            zmaster587.advancedRocketry.api.stations.ISpaceObject station =
                    SpaceObjectManager.getSpaceManager().getSpaceStation(stationId);
            if (station == null) {
                send(sender, "{\"error\":\"station not found\",\"id\":" + stationId + "}");
                return;
            }
            gc.overrideLandingStation(station);
            send(sender, "{\"ok\":true,\"entityId\":" + entityId
                    + ",\"stationId\":" + stationId + "}");
            return;
        }
        if ("set-destination".equalsIgnoreCase(args[0]) && args.length >= 3) {
            // /artest rocket set-destination <entityId> <dimId> — programs
            // the rocket's guidance computer chip so production launch()
            // can route to the destination. Needed for the rocket-launch
            // depth tests (TASK-03 A1): without a programmed destination,
            // rocket.launch() bails with "error.rocket.cannotGetThere"
            // and isInFlight stays false.
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            int dimId = parseIntOr(args[2], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            zmaster587.advancedRocketry.tile.TileGuidanceComputer gc =
                    rocket.storage == null ? null : rocket.storage.getGuidanceComputer();
            if (gc == null) {
                send(sender, "{\"error\":\"rocket has no guidance computer\",\"entityId\":"
                        + entityId + "}");
                return;
            }
            net.minecraft.item.Item chipItem = ForgeRegistries.ITEMS.getValue(
                    new ResourceLocation("advancedrocketry", "planetIdChip"));
            if (!(chipItem instanceof zmaster587.advancedRocketry.item.ItemPlanetIdentificationChip)) {
                send(sender, "{\"error\":\"ItemPlanetIdentificationChip not registered\"}");
                return;
            }
            zmaster587.advancedRocketry.item.ItemPlanetIdentificationChip chip =
                    (zmaster587.advancedRocketry.item.ItemPlanetIdentificationChip) chipItem;
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(chip);
            chip.setDimensionId(stack, dimId);
            gc.setInventorySlotContents(0, stack);
            send(sender, "{\"ok\":true,\"entityId\":" + entityId + ",\"dim\":" + dimId
                    + ",\"chipDim\":" + chip.getDimensionId(stack) + "}");
            return;
        }
        if ("force-orbit-reached".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // /artest rocket force-orbit-reached <entityId> — invokes the
            // production EntityRocketBase.onOrbitReached. TASK-07 A2 cause-
            // effect: this fires RocketReachesOrbitEvent and (if rocket is
            // in spaceDim on a station pad) calls station.setPadStatus(false).
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            int eventCountBefore = RocketEventRecorder.orbitReachedCount;
            try {
                rocket.onOrbitReached();
            } catch (RuntimeException e) {
                send(sender, "{\"error\":\"onOrbitReached threw: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage())
                        + "\"}");
                return;
            }
            send(sender, "{\"ok\":true,\"entityId\":" + entityId
                    + ",\"isInOrbit\":" + rocket.isInOrbit()
                    + ",\"orbitReachedEventDelta\":"
                    + (RocketEventRecorder.orbitReachedCount - eventCountBefore) + "}");
            return;
        }
        if ("dismantle".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // /artest rocket dismantle <entityId> — invokes production
            // EntityRocketBase.deconstructRocket. Fires RocketDismantleEvent.
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            int eventCountBefore = RocketEventRecorder.dismantleCount;
            try {
                rocket.deconstructRocket();
            } catch (RuntimeException e) {
                send(sender, "{\"error\":\"deconstructRocket threw: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage())
                        + "\"}");
                return;
            }
            send(sender, "{\"ok\":true,\"entityId\":" + entityId
                    + ",\"dismantleEventDelta\":"
                    + (RocketEventRecorder.dismantleCount - eventCountBefore) + "}");
            return;
        }
        if ("event-counts".equalsIgnoreCase(args[0])) {
            // /artest rocket event-counts — dump global counters for the
            // 4 RocketEvent types. The recorder is registered once
            // statically (see RocketEventRecorder.ensureRegistered).
            RocketEventRecorder.ensureRegistered();
            send(sender, "{\"launch\":" + RocketEventRecorder.launchCount
                    + ",\"preLaunch\":" + RocketEventRecorder.preLaunchCount
                    + ",\"orbitReached\":" + RocketEventRecorder.orbitReachedCount
                    + ",\"dismantle\":" + RocketEventRecorder.dismantleCount + "}");
            return;
        }
        if ("event-payloads".equalsIgnoreCase(args[0])) {
            // Gap #6 — dump last-observed entity id + dim per event type.
            RocketEventRecorder.ensureRegistered();
            StringBuilder out = new StringBuilder("{");
            out.append("\"launchEntityId\":").append(RocketEventRecorder.lastLaunchEntityId)
                    .append(",\"launchDim\":").append(RocketEventRecorder.lastLaunchDim);
            out.append(",\"preLaunchEntityId\":").append(RocketEventRecorder.lastPreLaunchEntityId)
                    .append(",\"preLaunchDim\":").append(RocketEventRecorder.lastPreLaunchDim);
            out.append(",\"orbitReachedEntityId\":").append(RocketEventRecorder.lastOrbitReachedEntityId)
                    .append(",\"orbitReachedDim\":").append(RocketEventRecorder.lastOrbitReachedDim);
            out.append(",\"dismantleEntityId\":").append(RocketEventRecorder.lastDismantleEntityId)
                    .append(",\"dismantleDim\":").append(RocketEventRecorder.lastDismantleDim);
            out.append(",\"landedEntityId\":").append(RocketEventRecorder.lastLandedEntityId)
                    .append(",\"landedDim\":").append(RocketEventRecorder.lastLandedDim);
            out.append(",\"deOrbitingEntityId\":").append(RocketEventRecorder.lastDeOrbitingEntityId)
                    .append(",\"deOrbitingDim\":").append(RocketEventRecorder.lastDeOrbitingDim);
            out.append('}');
            send(sender, out.toString());
            return;
        }
        if ("arm-prelaunch-cancel".equalsIgnoreCase(args[0])) {
            // Gap 1 — arm the test-only RocketPreLaunchEvent canceller.
            // Subsequent prepareLaunch() calls fire the event, which is
            // then cancelled, preventing LAUNCH_COUNTER from being set
            // to 200. Tests MUST disarm in @After.
            ensurePreLaunchCancellerRegistered();
            preLaunchObservedCount = 0;
            preLaunchCancelledCount = 0;
            cancelNextPreLaunch = true;
            send(sender, "{\"ok\":true,\"armed\":true}");
            return;
        }
        if ("disarm-prelaunch-cancel".equalsIgnoreCase(args[0])) {
            cancelNextPreLaunch = false;
            send(sender, "{\"ok\":true,\"armed\":false"
                    + ",\"observedSinceArm\":" + preLaunchObservedCount
                    + ",\"cancelledSinceArm\":" + preLaunchCancelledCount + "}");
            return;
        }
        if ("prelaunch-cancel-counts".equalsIgnoreCase(args[0])) {
            ensurePreLaunchCancellerRegistered();
            send(sender, "{\"ok\":true,\"armed\":" + cancelNextPreLaunch
                    + ",\"observed\":" + preLaunchObservedCount
                    + ",\"cancelled\":" + preLaunchCancelledCount + "}");
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
            info.put("uuid", rocket.getPersistentID().toString());
            // TASK-22 — exact entity class FQN, used to distinguish
            // EntityRocket (rocket-assembler output) from
            // EntityStationDeployedRocket (UV-assembler output). Both
            // are valid types in this probe surface because the latter
            // extends the former.
            info.put("entityClass", rocket.getClass().getName());
            // Gap 1 (RocketPreLaunchEvent cancellation) — countdown
            // value set by prepareLaunch() to 200 when the event isn't
            // cancelled. Stays at default (-1) when cancelled.
            // LAUNCH_COUNTER is private static final; reach in via reflection.
            try {
                java.lang.reflect.Field counterField =
                        zmaster587.advancedRocketry.entity.EntityRocket.class
                                .getDeclaredField("LAUNCH_COUNTER");
                counterField.setAccessible(true);
                @SuppressWarnings("unchecked")
                net.minecraft.network.datasync.DataParameter<Integer> param =
                        (net.minecraft.network.datasync.DataParameter<Integer>) counterField.get(null);
                info.put("launchCounter", rocket.getDataManager().get(param));
            } catch (ReflectiveOperationException e) {
                info.put("launchCounter", -999);
            }
            info.put("dim", rocket.world.provider.getDimension());
            info.put("posX", rocket.posX);
            info.put("posY", rocket.posY);
            info.put("posZ", rocket.posZ);
            info.put("isInFlight", rocket.isInFlight());
            info.put("isInOrbit", rocket.isInOrbit());
            info.put("ticksExisted", rocket.ticksExisted);
            info.put("destinationDim", reflectInt(rocket, "destinationDimId"));
            // errorStr is private + set by setError(...) when launch() bails
            // on a precondition. Without surfacing it, A1 launch-depth tests
            // can't discriminate "launched successfully" from "silently
            // bailed before setInFlight". Empty string = no error reported.
            try {
                java.lang.reflect.Field errF =
                        EntityRocket.class.getDeclaredField("errorStr");
                errF.setAccessible(true);
                Object v = errF.get(rocket);
                info.put("errorMessage", v == null ? "" : v.toString());
            } catch (ReflectiveOperationException e) {
                info.put("errorMessage", "<reflection failed: " + e.getClass().getSimpleName() + ">");
            }
            info.put("hasStorage", rocket.storage != null);
            info.put("numPassengers", rocket.getPassengers().size());
            // Storage chunk geometry — null-safe.
            if (rocket.storage != null) {
                int sx = rocket.storage.getSizeX();
                int sy = rocket.storage.getSizeY();
                int sz = rocket.storage.getSizeZ();
                info.put("storageSizeX", sx);
                info.put("storageSizeY", sy);
                info.put("storageSizeZ", sz);
                info.put("storageChunkSize", sx * sy * sz);
                // Count fuel-tank blocks — StatsRocket caches engineCount and
                // seatCount, but tank counting requires a per-block scan. In
                // AR, IFuelTank is implemented on the Block (not the
                // TileEntity), so we walk the storage chunk's IBlockState
                // grid rather than its tile-entity list.
                int fuelTankCount = 0;
                for (int sxi = 0; sxi < sx; sxi++) {
                    for (int syi = 0; syi < sy; syi++) {
                        for (int szi = 0; szi < sz; szi++) {
                            net.minecraft.block.state.IBlockState bs =
                                    rocket.storage.getBlockState(new BlockPos(sxi, syi, szi));
                            if (bs.getBlock() instanceof zmaster587.advancedRocketry.api.IFuelTank) {
                                fuelTankCount++;
                            }
                        }
                    }
                }
                info.put("fuelTankCount", fuelTankCount);
                // Guidance-computer slot: present iff the storage chunk has a
                // TileGuidanceComputer AND its slot 0 (the chip slot) is non-empty.
                zmaster587.advancedRocketry.tile.TileGuidanceComputer gc =
                        rocket.storage.getGuidanceComputer();
                boolean gcPresent = gc != null;
                boolean chipPresent = gcPresent && !gc.getStackInSlot(0).isEmpty();
                info.put("guidanceComputerPresent", gcPresent);
                info.put("guidanceComputerSlotOccupied", chipPresent);
            } else {
                info.put("storageChunkSize", -1);
                info.put("fuelTankCount", -1);
                info.put("guidanceComputerPresent", false);
                info.put("guidanceComputerSlotOccupied", false);
            }
            // Component counts from StatsRocket (cached during scan).
            info.put("seatCount", rocket.stats.getNumPassengerSeats());
            info.put("engineCount", rocket.stats.getEngineLocations().size());
            // Fuel snapshot per fuel type — using the public StatsRocket API.
            Map<String, Object> fuel = new LinkedHashMap<>();
            for (FuelRegistry.FuelType type : FuelRegistry.FuelType.values()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("amount", rocket.stats.getFuelAmount(type));
                entry.put("capacity", rocket.stats.getFuelCapacity(type));
                entry.put("rate", rocket.stats.getFuelRate(type));
                fuel.put(type.name(), entry);
            }
            info.put("fuel", fuel);
            info.put("thrust", rocket.stats.getThrust());
            info.put("weight_no_fuel", rocket.stats.getWeight_NoFuel());
            send(sender, jsonMap(info));
            return;
        }
        if ("storage-inventory".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // rocket storage-inventory <entityId> — flat dump of every item
            // stack across every IInventory tile inside the rocket's storage
            // chunk. Used by §7.10 loader/unloader tests to verify the
            // transfer ended up in the rocket's cargo hatches.
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            if (rocket.storage == null) {
                send(sender, "{\"error\":\"rocket has no storage\",\"entityId\":" + entityId + "}");
                return;
            }
            StringBuilder builder = new StringBuilder("{\"entityId\":")
                    .append(entityId).append(",\"items\":[");
            boolean first = true;
            int tileCount = 0;
            for (TileEntity te : rocket.storage.getInventoryTiles()) {
                tileCount++;
                if (!(te instanceof net.minecraft.inventory.IInventory)) continue;
                net.minecraft.inventory.IInventory inv = (net.minecraft.inventory.IInventory) te;
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    net.minecraft.item.ItemStack s = inv.getStackInSlot(i);
                    if (s.isEmpty()) continue;
                    if (!first) builder.append(',');
                    first = false;
                    ResourceLocation rn = s.getItem().getRegistryName();
                    builder.append("{\"slot\":").append(i)
                            .append(",\"item\":\"").append(rn == null ? "null" : rn.toString())
                            .append("\",\"count\":").append(s.getCount()).append('}');
                }
            }
            builder.append("],\"inventoryTileCount\":").append(tileCount).append('}');
            send(sender, builder.toString());
            return;
        }
        if ("storage-fluid".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // rocket storage-fluid <entityId> — flat dump of every fluid
            // stack across every fluid-handler tile inside storage. Used by
            // §7.10 fluid loader/unloader tests.
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            if (rocket.storage == null) {
                send(sender, "{\"error\":\"rocket has no storage\",\"entityId\":" + entityId + "}");
                return;
            }
            StringBuilder builder = new StringBuilder("{\"entityId\":")
                    .append(entityId).append(",\"tanks\":[");
            boolean first = true;
            int totalAmount = 0;
            for (TileEntity te : rocket.storage.getFluidTiles()) {
                net.minecraftforge.fluids.capability.IFluidHandler h =
                        te.getCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler
                                .FLUID_HANDLER_CAPABILITY, null);
                if (h == null) continue;
                for (net.minecraftforge.fluids.capability.IFluidTankProperties p : h.getTankProperties()) {
                    if (!first) builder.append(',');
                    first = false;
                    net.minecraftforge.fluids.FluidStack contents = p.getContents();
                    builder.append("{\"capacity\":").append(p.getCapacity());
                    if (contents == null || contents.amount == 0) {
                        builder.append(",\"fluid\":null,\"amount\":0}");
                    } else {
                        builder.append(",\"fluid\":\"").append(escapeJson(contents.getFluid().getName()))
                                .append("\",\"amount\":").append(contents.amount).append('}');
                        totalAmount += contents.amount;
                    }
                }
            }
            builder.append("],\"totalAmount\":").append(totalAmount).append('}');
            send(sender, builder.toString());
            return;
        }
        if ("find-by-uuid".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // TASK-07 Phase 3: find a rocket by its persistent UUID across all
            // loaded dimensions. Needed after EntityRocket.changeDimension()
            // because that respawns the entity in the destination world with
            // a NEW entityId, but UUID is preserved (Forge Entity contract).
            java.util.UUID uuid;
            try {
                uuid = java.util.UUID.fromString(args[1]);
            } catch (IllegalArgumentException e) {
                send(sender, "{\"error\":\"invalid uuid\",\"raw\":\"" + escapeJson(args[1]) + "\"}");
                return;
            }
            // Prefer the LIVE copy. Forge's Entity.changeDimension leaves
            // the source-dim entity in the old world's tracking map until
            // the next collect-dead tick (isDead=true). A naive iteration
            // could return that stale copy and report the old entityId
            // even though the rocket has already transitioned. Two-pass:
            // first look for a non-dead match, then fall back to ANY match.
            Entity liveMatch = null;
            Entity anyMatch = null;
            int liveDim = 0;
            int anyDim = 0;
            for (WorldServer world : server.worlds) {
                Entity ent = world.getEntityFromUuid(uuid);
                if (ent instanceof EntityRocket) {
                    if (!ent.isDead && liveMatch == null) {
                        liveMatch = ent;
                        liveDim = world.provider.getDimension();
                    } else if (anyMatch == null) {
                        anyMatch = ent;
                        anyDim = world.provider.getDimension();
                    }
                }
            }
            Entity ent = liveMatch != null ? liveMatch : anyMatch;
            int dimResult = liveMatch != null ? liveDim : anyDim;
            if (ent instanceof EntityRocket) {
                EntityRocket r = (EntityRocket) ent;
                    int sx = r.storage == null ? -1 : r.storage.getSizeX();
                    int sy = r.storage == null ? -1 : r.storage.getSizeY();
                    int sz = r.storage == null ? -1 : r.storage.getSizeZ();
                int engineCount = r.storage == null ? -1
                        : r.stats.getEngineLocations().size();
                send(sender, "{\"ok\":true,\"entityId\":" + ent.getEntityId()
                        + ",\"uuid\":\"" + r.getPersistentID().toString() + "\""
                        + ",\"dim\":" + dimResult
                        + ",\"posX\":" + ent.posX
                        + ",\"posY\":" + ent.posY
                        + ",\"posZ\":" + ent.posZ
                        + ",\"isDead\":" + ent.isDead
                        + ",\"isInFlight\":" + r.isInFlight()
                        + ",\"isInOrbit\":" + r.isInOrbit()
                        + ",\"storageSizeX\":" + sx
                        + ",\"storageSizeY\":" + sy
                        + ",\"storageSizeZ\":" + sz
                        + ",\"engineCount\":" + engineCount + "}");
                return;
            }
            send(sender, "{\"error\":\"rocket not found by uuid\",\"uuid\":\""
                    + uuid + "\"}");
            return;
        }
        if ("force-dest-dim".equalsIgnoreCase(args[0]) && args.length >= 3) {
            // TASK-07 Phase 3: directly mutate EntityRocket.destinationDimId
            // via reflection, bypassing launch()'s canTravelTo validation.
            // Required for the invalid-dim test — we need a rocket with a
            // bogus destination so onOrbitReached -> reachSpaceManned ->
            // changeDimension hits the !canTravelTo guard at line 1943.
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            int dimId = parseIntOr(args[2], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            try {
                java.lang.reflect.Field f = EntityRocket.class.getDeclaredField("destinationDimId");
                f.setAccessible(true);
                f.setInt(rocket, dimId);
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed: " + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            send(sender, "{\"ok\":true,\"entityId\":" + entityId
                    + ",\"destinationDim\":" + reflectInt(rocket, "destinationDimId") + "}");
            return;
        }
        if ("tick".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // TASK-07 Phase 4: directly call EntityRocket.onUpdate() N times.
            // The headless test server only ticks chunks that hold a player;
            // without a chunk anchor the rocket entity sits frozen. Calling
            // onUpdate() explicitly drives the descent-timer gate, motion
            // integration, and the landed-on-ground / orbit-reached checks.
            // Optional 2nd arg = N (default 1).
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            int times = args.length >= 3 ? Math.max(1, parseIntOr(args[2], 1)) : 1;
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            try {
                for (int i = 0; i < times; i++) {
                    if (rocket.isDead) break;
                    rocket.onUpdate();
                }
            } catch (RuntimeException e) {
                send(sender, "{\"error\":\"onUpdate threw: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage()) + "\"}");
                return;
            }
            send(sender, "{\"ok\":true,\"entityId\":" + entityId + ",\"ticks\":" + times
                    + ",\"isDead\":" + rocket.isDead
                    + ",\"isInFlight\":" + (rocket.isDead ? false : rocket.isInFlight())
                    + ",\"isInOrbit\":" + (rocket.isDead ? false : rocket.isInOrbit())
                    + ",\"ticksExisted\":" + (rocket.isDead ? -1 : rocket.ticksExisted)
                    + ",\"posY\":" + (rocket.isDead ? Double.NaN : rocket.posY) + "}");
            return;
        }
        if ("set-state".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // TASK-07 Phase 4: direct state mutation. Accepts key=value pairs:
            //   orbit=true|false   -> setInOrbit
            //   flight=true|false  -> setInFlight
            //   ticksExisted=<n>   -> set rocket.ticksExisted directly
            //   posY=<n>           -> setPosition(posX, posY, posZ)
            //   motionY=<n>        -> rocket.motionY = n
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            for (int i = 2; i < args.length; i++) {
                String kv = args[i];
                int eq = kv.indexOf('=');
                if (eq <= 0) continue;
                String k = kv.substring(0, eq);
                String v = kv.substring(eq + 1);
                try {
                    switch (k) {
                        case "orbit":    rocket.setInOrbit(Boolean.parseBoolean(v)); break;
                        case "flight":   rocket.setInFlight(Boolean.parseBoolean(v)); break;
                        case "ticksExisted":
                            java.lang.reflect.Field tf = Entity.class.getDeclaredField("ticksExisted");
                            tf.setAccessible(true);
                            tf.setInt(rocket, Integer.parseInt(v));
                            break;
                        case "posY":
                            rocket.setPosition(rocket.posX, Double.parseDouble(v), rocket.posZ);
                            break;
                        case "motionY":
                            rocket.motionY = Double.parseDouble(v);
                            break;
                        default:
                            send(sender, "{\"error\":\"unknown set-state key\",\"key\":\"" + k + "\"}");
                            return;
                    }
                } catch (ReflectiveOperationException | NumberFormatException e) {
                    send(sender, "{\"error\":\"set-state failed: " + escapeJson(e.getMessage()) + "\"}");
                    return;
                }
            }
            send(sender, "{\"ok\":true,\"entityId\":" + entityId
                    + ",\"isInFlight\":" + rocket.isInFlight()
                    + ",\"isInOrbit\":" + rocket.isInOrbit()
                    + ",\"ticksExisted\":" + rocket.ticksExisted
                    + ",\"posY\":" + rocket.posY
                    + ",\"motionY\":" + rocket.motionY + "}");
            return;
        }
        if ("explode".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // TASK-07 Phase 5: invoke production EntityRocket.explode().
            // The current production code calls explode() from launch() iff
            // partsWearSystem && storage.shouldBreak(). Tests pin: the
            // method sets the entity dead.
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            try {
                rocket.explode();
            } catch (RuntimeException e) {
                send(sender, "{\"error\":\"explode threw: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage()) + "\"}");
                return;
            }
            send(sender, "{\"ok\":true,\"entityId\":" + entityId + ",\"isDead\":" + rocket.isDead + "}");
            return;
        }
        if ("drain-fuel".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // TASK-07 Phase 5: zero out every fuel type on the rocket.
            // Companion to the (already existing) rocket fuel probe which
            // reads amounts; this is the write side.
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            for (zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType ft :
                    zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType.values()) {
                rocket.setFuelAmount(ft, 0);
            }
            send(sender, "{\"ok\":true,\"entityId\":" + entityId + "}");
            return;
        }
        if ("event-counts-full".equalsIgnoreCase(args[0])) {
            // TASK-07 Phase 4: extended counter dump including landed + deOrbiting.
            RocketEventRecorder.ensureRegistered();
            send(sender, "{\"launch\":" + RocketEventRecorder.launchCount
                    + ",\"preLaunch\":" + RocketEventRecorder.preLaunchCount
                    + ",\"orbitReached\":" + RocketEventRecorder.orbitReachedCount
                    + ",\"dismantle\":" + RocketEventRecorder.dismantleCount
                    + ",\"landed\":" + RocketEventRecorder.landedCount
                    + ",\"deOrbiting\":" + RocketEventRecorder.deOrbitingCount + "}");
            return;
        }
        send(sender, "{\"error\":\"unknown rocket subcommand — try list|info <id> | storage-inventory <id> | storage-fluid <id> | find-by-uuid <uuid> | force-dest-dim <id> <dim> | tick <id> [n] | set-state <id> k=v... | explode <id> | drain-fuel <id> | event-counts-full\"}");
    }

    /** {@code /artest rocket assemble <dim> <x> <y> <z>} — synchronously assembles
     *  a rocket at the {@link zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine}
     *  position, bypassing the tick/power scan loop. Steps:
     *  <ol>
     *    <li>{@code getRocketPadBounds(world, pos)} → BB (or null if pad/tower invalid).</li>
     *    <li>Inject the BB into the tile's protected {@code bbCache} field via reflection.</li>
     *    <li>{@code scanRocket(world, pos, bbCache)} — populates {@code stats} +
     *        sets {@code status} to {@code SUCCESS} or an error code.</li>
     *    <li>If {@code SUCCESS}: {@code assembleRocket()} → spawns the
     *        {@link EntityRocket} immediately.</li>
     *    <li>Find the spawned rocket in the BB and return its entity id.</li>
     *  </ol>
     *  This is the test-only equivalent of clicking the "Build" button after the
     *  scanner has finished — but synchronous and independent of energy supply,
     *  so it works on bare fixtures without a creative input plug.
     */
    /**
     * {@code /artest assembler pad-bounds <dim> <x> <y> <z>} — invokes
     * {@code TileRocketAssemblingMachine.getRocketPadBounds()} on the
     * controller at the given pos and returns the resulting BB's
     * dimensions. Polymorphic — fires UV's override on
     * {@code TileUnmannedVehicleAssembler}, parent's on
     * {@code TileRocketAssemblingMachine}.
     *
     * <p>{@code /artest assembler max-y} — reports
     * {@code TileRocketAssemblingMachine.MAX_SIZE_Y} and
     * {@code TileUnmannedVehicleAssembler.MAX_SIZE_Y} via reflection,
     * one shared probe call. The two private-static-final constants
     * are the contract for "how tall a rocket can each assembler scan";
     * pinning their relative magnitude (rocket > UV) catches a regression
     * that swaps or unifies the caps.</p>
     *
     * <p>Used by TASK-22 to observe the {@code MAX_SIZE_Y} delta:
     * rocket assembler caps at 128, UV caps at 17.</p>
     */
    /**
     * Gap 2 — service station state observability.
     * {@code /artest infra service-state <dim> <x> <y> <z>} — reads
     * {@link zmaster587.advancedRocketry.tile.infrastructure.TileRocketServiceStation}'s
     * package-private state via reflection: linkedRocket entity id (or -1
     * if unlinked), partsToRepair count, and assemblers count.
     */
    private void handleInfraServiceState(MinecraftServer server,
                                         ICommandSender sender,
                                         int dim, int x, int y, int z) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!(tile instanceof zmaster587.advancedRocketry.tile.infrastructure
                .TileRocketServiceStation)) {
            send(sender, "{\"error\":\"not a TileRocketServiceStation\",\"tile\":\""
                    + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
            return;
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("tileClass", tile.getClass().getName());
        try {
            java.lang.reflect.Field linkedF = tile.getClass().getDeclaredField("linkedRocket");
            linkedF.setAccessible(true);
            Object linkedRocket = linkedF.get(tile);
            if (linkedRocket instanceof net.minecraft.entity.Entity) {
                info.put("linkedRocketId",
                        ((net.minecraft.entity.Entity) linkedRocket).getEntityId());
            } else {
                info.put("linkedRocketId", -1);
            }
            java.lang.reflect.Field partsF = tile.getClass().getDeclaredField("partsToRepair");
            partsF.setAccessible(true);
            Object partsList = partsF.get(tile);
            int partsCount = (partsList instanceof java.util.Collection<?>)
                    ? ((java.util.Collection<?>) partsList).size() : -1;
            info.put("partsToRepairCount", partsCount);
            java.lang.reflect.Field initF = tile.getClass().getDeclaredField("initialPartToRepairCount");
            initF.setAccessible(true);
            info.put("initialPartToRepairCount", initF.get(tile));
            java.lang.reflect.Field asmF = tile.getClass().getDeclaredField("assemblers");
            asmF.setAccessible(true);
            Object asmList = asmF.get(tile);
            int asmCount = (asmList instanceof java.util.Collection<?>)
                    ? ((java.util.Collection<?>) asmList).size() : -1;
            info.put("assemblersCount", asmCount);
        } catch (ReflectiveOperationException e) {
            info.put("reflectionError",
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        send(sender, jsonMap(info));
    }

    private void handleAssembler(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 1 && "max-y".equalsIgnoreCase(args[0])) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("rocketAssemblerMaxY", readPrivateIntStatic(
                    zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine.class,
                    "MAX_SIZE_Y"));
            info.put("uvAssemblerMaxY", readPrivateIntStatic(
                    zmaster587.advancedRocketry.tile.TileUnmannedVehicleAssembler.class,
                    "MAX_SIZE_Y"));
            info.put("rocketAssemblerMaxXZ", readPrivateIntStatic(
                    zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine.class,
                    "MAX_SIZE"));
            info.put("uvAssemblerMaxXZ", readPrivateIntStatic(
                    zmaster587.advancedRocketry.tile.TileUnmannedVehicleAssembler.class,
                    "MAX_SIZE"));
            send(sender, jsonMap(info));
            return;
        }
        if (args.length >= 5 && "pad-bounds".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0), y = parseIntOr(args[3], 0), z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            BlockPos pos = new BlockPos(x, y, z);
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine)) {
                send(sender, "{\"error\":\"not a rocket assembling machine\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine builder =
                    (zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine) tile;
            net.minecraft.util.math.AxisAlignedBB bb = builder.getRocketPadBounds(world, pos);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("tileClass", tile.getClass().getName());
            if (bb == null) {
                info.put("bbNull", true);
            } else {
                info.put("bbNull", false);
                int sx = (int) (bb.maxX - bb.minX + 1);
                int sy = (int) (bb.maxY - bb.minY + 1);
                int sz = (int) (bb.maxZ - bb.minZ + 1);
                info.put("sizeX", sx);
                info.put("sizeY", sy);
                info.put("sizeZ", sz);
                info.put("minX", (int) bb.minX);
                info.put("minY", (int) bb.minY);
                info.put("minZ", (int) bb.minZ);
                info.put("maxX", (int) bb.maxX);
                info.put("maxY", (int) bb.maxY);
                info.put("maxZ", (int) bb.maxZ);
            }
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown assembler subcommand — try pad-bounds <dim> <x> <y> <z>\"}");
    }

    /**
     * Gap 1 (RocketPreLaunchEvent cancellation contract) — test-only
     * subscriber that conditionally cancels the {@code RocketPreLaunchEvent}.
     * Registered lazily the first time {@code arm-prelaunch-cancel} is
     * called. The toggle is volatile because the listener fires on the
     * server thread while the probe runs on the command-handler thread.
     *
     * <p>Tests MUST {@code disarm-prelaunch-cancel} in {@code @After} —
     * leaving the flag armed would silently break every subsequent rocket
     * test in the shared harness.</p>
     */
    private static volatile boolean cancelNextPreLaunch = false;
    private static volatile boolean preLaunchCancellerRegistered = false;
    private static volatile int preLaunchObservedCount = 0;
    private static volatile int preLaunchCancelledCount = 0;

    private static synchronized void ensurePreLaunchCancellerRegistered() {
        if (preLaunchCancellerRegistered) return;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Object() {
            @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
            public void onPreLaunch(
                    zmaster587.advancedRocketry.api.RocketEvent.RocketPreLaunchEvent event) {
                preLaunchObservedCount++;
                if (cancelNextPreLaunch) {
                    event.setCanceled(true);
                    preLaunchCancelledCount++;
                }
            }
        });
        preLaunchCancellerRegistered = true;
    }

    private void handleRocketAssemble(MinecraftServer server, ICommandSender sender, String[] args) {
        int dim = parseIntOr(args[1], Integer.MIN_VALUE);
        int x = parseIntOr(args[2], 0), y = parseIntOr(args[3], 0), z = parseIntOr(args[4], 0);
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        BlockPos builderPos = new BlockPos(x, y, z);
        TileEntity tile = world.getTileEntity(builderPos);
        if (!(tile instanceof zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine)) {
            send(sender, "{\"error\":\"not a rocket assembling machine\",\"tile\":\""
                    + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
            return;
        }
        zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine builder =
                (zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine) tile;
        try {
            // 1. Resolve pad bounds.
            net.minecraft.util.math.AxisAlignedBB bb = builder.getRocketPadBounds(world, builderPos);
            if (bb == null) {
                send(sender, "{\"error\":\"getRocketPadBounds returned null — pad < 3x3 OR no >= 4-block structure tower on perimeter\"}");
                return;
            }
            // 2. Inject bbCache (protected field).
            java.lang.reflect.Field bbField =
                    zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine.class.getDeclaredField("bbCache");
            bbField.setAccessible(true);
            bbField.set(builder, bb);
            // 3. Scan rocket → sets status. (UNSCANNED → SUCCESS or specific error.)
            //    ErrorCodes is a protected nested enum, so getStatus() can't be
            //    assigned to a typed variable here — reflectively read .name().
            builder.scanRocket(world, builderPos, bb);
            java.lang.reflect.Method getStatusMethod = builder.getClass().getMethod("getStatus");
            String statusName = ((Enum<?>) getStatusMethod.invoke(builder)).name();
            if (!"SUCCESS".equals(statusName)) {
                send(sender, "{\"error\":\"scan status not SUCCESS\",\"status\":\"" + statusName + "\"}");
                return;
            }
            // 4. Assemble. assembleRocket() re-runs scanRocket internally; if the
            //    second scan changes status, abort there too.
            builder.assembleRocket();
            String postStatusName = ((Enum<?>) getStatusMethod.invoke(builder)).name();
            // 5. Find the spawned rocket inside the pad BB.
            java.util.List<zmaster587.advancedRocketry.entity.EntityRocket> rockets =
                    world.getEntitiesWithinAABB(zmaster587.advancedRocketry.entity.EntityRocket.class, bb);
            int entityId = rockets.isEmpty() ? -1 : rockets.get(0).getEntityId();
            send(sender, "{\"ok\":true,\"status\":\"" + postStatusName
                    + "\",\"entityId\":" + entityId + ",\"rocketCount\":" + rockets.size() + "}");
        } catch (ReflectiveOperationException e) {
            send(sender, "{\"error\":\"reflection failed: " + escapeJson(e.getMessage()) + "\"}");
        } catch (RuntimeException e) {
            send(sender, "{\"error\":\"" + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage()) + "\"}");
        }
    }

    /** {@code /artest rocket launch <entityId> [fillFuel] [mode]}.
     *  <ul>
     *    <li>{@code fillFuel=true} (default): fill all fuel types to capacity.</li>
     *    <li>{@code mode=prepare} (default): call {@link EntityRocket#prepareLaunch()},
     *        which schedules a 200-tick countdown (matches the in-game button).
     *        In a headless test without a player, the chunk often unloads before
     *        the countdown ticks down — use {@code mode=instant} or {@code mode=force}.</li>
     *    <li>{@code mode=instant}: call {@link EntityRocket#launch()} synchronously,
     *        skipping the countdown. Still requires a valid destination via the
     *        guidance computer; without one the launch path errors out and
     *        {@code isInFlight} stays {@code false}.</li>
     *    <li>{@code mode=force}: skip {@code launch()} entirely and set
     *        {@code isInFlight=true} directly via {@link EntityRocket#setInFlight(boolean)}.
     *        For tests that only want to verify the flight-state transition itself,
     *        independent of guidance-computer / destination-validity logic.</li>
     *  </ul>
     */
    private void handleRocketLaunch(MinecraftServer server, ICommandSender sender, String[] args) {
        int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
        boolean fillFuel = args.length >= 3 ? Boolean.parseBoolean(args[2]) : true;
        String mode = args.length >= 4 ? args[3].toLowerCase(java.util.Locale.ROOT) : "prepare";
        // Backward compat: "true" / "false" used to mean instant / prepare.
        if ("true".equals(mode)) mode = "instant";
        else if ("false".equals(mode)) mode = "prepare";

        EntityRocket rocket = findRocket(server, entityId);
        if (rocket == null) {
            send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
            return;
        }
        if (fillFuel) {
            for (FuelRegistry.FuelType type : FuelRegistry.FuelType.values()) {
                int cap = rocket.stats.getFuelCapacity(type);
                if (cap > 0) {
                    rocket.setFuelAmount(type, cap);
                }
            }
        }
        try {
            switch (mode) {
                case "instant":
                    rocket.launch();
                    break;
                case "force":
                    rocket.setInFlight(true);
                    break;
                case "prepare":
                default:
                    rocket.prepareLaunch();
            }
            send(sender, "{\"ok\":true,\"entityId\":" + entityId + ",\"fuelFilled\":" + fillFuel
                    + ",\"mode\":\"" + mode + "\""
                    + ",\"isInFlight\":" + rocket.isInFlight()
                    + ",\"isInOrbit\":" + rocket.isInOrbit() + "}");
        } catch (RuntimeException e) {
            send(sender, "{\"error\":\"" + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage()) + "\"}");
        }
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
        if (args.length >= 2 && "create".equalsIgnoreCase(args[0])) {
            int orbitingDim = parseIntOr(args[1], Integer.MIN_VALUE);
            int stationDim = args.length >= 3 ? parseIntOr(args[2], Integer.MIN_VALUE) : Integer.MIN_VALUE;
            SpaceStationObject station = new SpaceStationObject();
            station.setOrbitingBody(orbitingDim);
            // SpaceStationObject.getOrbitingPlanetId() returns INVALID_PLANET until
            // `created=true`. setOrbitingBody alone doesn't flip that — production
            // code does so via beginTransition() / station-assembler success path.
            // Force the flag here so test-created stations are immediately
            // queryable by /artest station info.
            try {
                java.lang.reflect.Field createdField = SpaceStationObject.class.getDeclaredField("created");
                createdField.setAccessible(true);
                createdField.setBoolean(station, true);
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"could not flip created flag\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            if (stationDim == Integer.MIN_VALUE) {
                SpaceObjectManager.getSpaceManager().registerSpaceObject(station, orbitingDim);
            } else {
                SpaceObjectManager.getSpaceManager().registerSpaceObject(station, orbitingDim, stationDim);
            }
            send(sender, "{\"ok\":true,\"id\":" + station.getId()
                    + ",\"orbitingBody\":" + station.getOrbitingPlanetId() + "}");
            return;
        }
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
        if ("fuel".equalsIgnoreCase(args[0]) && args.length >= 4) {
            // /artest station fuel <id> {set|add|use} <amount>
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            String op = args[2];
            int amount = parseIntOr(args[3], 0);
            ISpaceObject station = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (!(station instanceof SpaceStationObject)) {
                send(sender, "{\"error\":\"station not found or wrong type\",\"id\":" + id + "}");
                return;
            }
            SpaceStationObject sso = (SpaceStationObject) station;
            int before = sso.getFuelAmount();
            int returned;
            if ("set".equalsIgnoreCase(op)) {
                sso.setFuelAmount(amount);
                returned = amount;
            } else if ("add".equalsIgnoreCase(op)) {
                returned = sso.addFuel(amount);
            } else if ("use".equalsIgnoreCase(op)) {
                returned = sso.useFuel(amount);
            } else {
                send(sender, "{\"error\":\"unknown fuel op — try set|add|use\",\"op\":\"" + escapeJson(op) + "\"}");
                return;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("id", id);
            out.put("op", op);
            out.put("requested", amount);
            out.put("returned", returned);
            out.put("before", before);
            out.put("after", sso.getFuelAmount());
            out.put("max", sso.getMaxFuelAmount());
            send(sender, jsonMap(out));
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
                SpaceStationObject sso = (SpaceStationObject) station;
                info.put("fuelAmount", sso.getFuelAmount());
                info.put("fuelMax", sso.getMaxFuelAmount());
                info.put("padCount", sso.getLandingPads().size());
                info.put("hasFreePad", sso.hasFreeLandingPad());
                info.put("hasWarpCores", sso.hasWarpCores);
                info.put("hasUsableWarpCore", sso.hasUsableWarpCore());
            }
            send(sender, jsonMap(info));
            return;
        }
        if ("set-dest".equalsIgnoreCase(args[0]) && args.length >= 3) {
            // /artest station set-dest <id> <destDimId>
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            int destDim = parseIntOr(args[2], Integer.MIN_VALUE);
            ISpaceObject station = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (station == null) {
                send(sender, "{\"error\":\"station not found\",\"id\":" + id + "}");
                return;
            }
            int before = station.getDestOrbitingBody();
            station.setDestOrbitingBody(destDim);
            send(sender, "{\"ok\":true,\"id\":" + id + ",\"before\":" + before
                    + ",\"after\":" + station.getDestOrbitingBody() + "}");
            return;
        }
        if ("set-anchor".equalsIgnoreCase(args[0]) && args.length >= 3) {
            // /artest station set-anchor <id> <true|false>
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            boolean anchored = Boolean.parseBoolean(args[2]);
            ISpaceObject station = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (station == null) {
                send(sender, "{\"error\":\"station not found\",\"id\":" + id + "}");
                return;
            }
            boolean before = station.isAnchored();
            station.setIsAnchored(anchored);
            send(sender, "{\"ok\":true,\"id\":" + id + ",\"before\":" + before
                    + ",\"after\":" + station.isAnchored() + "}");
            return;
        }
        if ("set-parent".equalsIgnoreCase(args[0]) && args.length >= 3) {
            // /artest station set-parent <id> <parentDimId>
            // Wires the station's DimensionProperties parent so that
            // travel-cost calculations have a non-null reference frame.
            // Fresh stations from /artest station create start with
            // parentPlanet = INVALID_PLANET (clone of
            // defaultSpaceDimensionProperties), which makes
            // TileWarpController.getTravelCost return Integer.MAX_VALUE
            // and useFuel(...) return 0 → warp refused.
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            int parentDim = parseIntOr(args[2], 0);
            ISpaceObject station = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (station == null) {
                send(sender, "{\"error\":\"station not found\",\"id\":" + id + "}");
                return;
            }
            zmaster587.advancedRocketry.dimension.DimensionProperties parentProps =
                    zmaster587.advancedRocketry.dimension.DimensionManager.getInstance()
                            .getDimensionProperties(parentDim);
            if (parentProps == null) {
                send(sender, "{\"error\":\"unknown parent dim\",\"dim\":" + parentDim + "}");
                return;
            }
            zmaster587.advancedRocketry.dimension.DimensionProperties stationProps =
                    (zmaster587.advancedRocketry.dimension.DimensionProperties) station.getProperties();
            stationProps.setParentPlanet(parentProps, false);
            send(sender, "{\"ok\":true,\"id\":" + id + ",\"parentDim\":" + parentDim + "}");
            return;
        }
        if ("add-warp-core".equalsIgnoreCase(args[0]) && args.length >= 5) {
            // /artest station add-warp-core <id> <x> <y> <z>
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            ISpaceObject station = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (!(station instanceof SpaceStationObject)) {
                send(sender, "{\"error\":\"station not found or wrong type\",\"id\":" + id + "}");
                return;
            }
            SpaceStationObject sso = (SpaceStationObject) station;
            sso.addWarpCore(new zmaster587.libVulpes.util.HashedBlockPosition(x, y, z));
            send(sender, "{\"ok\":true,\"id\":" + id + ",\"pos\":[" + x + "," + y + "," + z
                    + "],\"hasWarpCores\":" + sso.hasWarpCores
                    + ",\"hasUsableWarpCore\":" + sso.hasUsableWarpCore() + "}");
            return;
        }
        if ("add-pad".equalsIgnoreCase(args[0]) && args.length >= 4) {
            // /artest station add-pad <id> <x> <z> [name]
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int z = parseIntOr(args[3], 0);
            String name = args.length >= 5 ? args[4] : "pad-" + x + "-" + z;
            ISpaceObject st = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (!(st instanceof SpaceStationObject)) {
                send(sender, "{\"error\":\"station not found or wrong type\",\"id\":" + id + "}");
                return;
            }
            SpaceStationObject sso = (SpaceStationObject) st;
            sso.addLandingPad(x, z, name);
            send(sender, "{\"ok\":true,\"id\":" + id + ",\"x\":" + x + ",\"z\":" + z
                    + ",\"name\":\"" + escapeJson(name) + "\",\"padCount\":"
                    + sso.getLandingPads().size() + "}");
            return;
        }
        if ("remove-pad".equalsIgnoreCase(args[0]) && args.length >= 4) {
            // /artest station remove-pad <id> <x> <z>
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int z = parseIntOr(args[3], 0);
            ISpaceObject st = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (!(st instanceof SpaceStationObject)) {
                send(sender, "{\"error\":\"station not found or wrong type\",\"id\":" + id + "}");
                return;
            }
            SpaceStationObject sso = (SpaceStationObject) st;
            int before = sso.getLandingPads().size();
            sso.removeLandingPad(x, z);
            int after = sso.getLandingPads().size();
            send(sender, "{\"ok\":true,\"id\":" + id + ",\"removed\":"
                    + (before - after) + ",\"padCount\":" + after + "}");
            return;
        }
        if ("pads".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // /artest station pads <id> — dump all landing pads
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            ISpaceObject st = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (!(st instanceof SpaceStationObject)) {
                send(sender, "{\"error\":\"station not found or wrong type\",\"id\":" + id + "}");
                return;
            }
            SpaceStationObject sso = (SpaceStationObject) st;
            StringBuilder builder = new StringBuilder("{\"id\":");
            builder.append(id).append(",\"pads\":[");
            boolean first = true;
            for (zmaster587.advancedRocketry.util.StationLandingLocation pad : sso.getLandingPads()) {
                if (!first) builder.append(',');
                first = false;
                zmaster587.libVulpes.util.HashedBlockPosition pos = pad.getPos();
                builder.append("{\"x\":").append(pos.x)
                        .append(",\"z\":").append(pos.z)
                        .append(",\"occupied\":").append(pad.getOccupied())
                        .append(",\"allowAutoLand\":").append(pad.getAllowedForAutoLand());
                if (pad.getName() != null) {
                    builder.append(",\"name\":\"")
                            .append(escapeJson(pad.getName())).append('"');
                }
                builder.append('}');
            }
            builder.append("]}");
            send(sender, builder.toString());
            return;
        }
        if ("dock".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // /artest station dock <id> [commit] — mirror production
            // getNextLandingPad(true): find the next free auto-land pad
            // and mark it occupied. Returns the chosen pad's pos or an
            // error if no free pad was available.
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            boolean commit = args.length < 3 || Boolean.parseBoolean(args[2]);
            ISpaceObject st = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (!(st instanceof SpaceStationObject)) {
                send(sender, "{\"error\":\"station not found or wrong type\",\"id\":" + id + "}");
                return;
            }
            SpaceStationObject sso = (SpaceStationObject) st;
            zmaster587.libVulpes.util.HashedBlockPosition pad = sso.getNextLandingPad(commit);
            if (pad == null) {
                send(sender, "{\"ok\":false,\"reason\":\"no free landing pad\",\"id\":" + id
                        + ",\"padCount\":" + sso.getLandingPads().size() + "}");
                return;
            }
            send(sender, "{\"ok\":true,\"id\":" + id + ",\"x\":" + pad.x
                    + ",\"z\":" + pad.z + ",\"commit\":" + commit + "}");
            return;
        }
        if ("undock".equalsIgnoreCase(args[0]) && args.length >= 4) {
            // /artest station undock <id> <x> <z> — set the named pad free
            // (setPadStatus(x, z, false) — production calls this when a
            // rocket lifts off from a station pad).
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int z = parseIntOr(args[3], 0);
            ISpaceObject st = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (!(st instanceof SpaceStationObject)) {
                send(sender, "{\"error\":\"station not found or wrong type\",\"id\":" + id + "}");
                return;
            }
            SpaceStationObject sso = (SpaceStationObject) st;
            sso.setPadStatus(x, z, false);
            send(sender, "{\"ok\":true,\"id\":" + id + ",\"x\":" + x + ",\"z\":" + z + "}");
            return;
        }
        if ("set-autoland".equalsIgnoreCase(args[0]) && args.length >= 5) {
            // /artest station set-autoland <id> <x> <z> <true|false>
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int z = parseIntOr(args[3], 0);
            boolean allowed = Boolean.parseBoolean(args[4]);
            ISpaceObject st = SpaceObjectManager.getSpaceManager().getSpaceStation(id);
            if (!(st instanceof SpaceStationObject)) {
                send(sender, "{\"error\":\"station not found or wrong type\",\"id\":" + id + "}");
                return;
            }
            SpaceStationObject sso = (SpaceStationObject) st;
            sso.setLandingPadAutoLandStatus(x, z, allowed);
            send(sender, "{\"ok\":true,\"id\":" + id + ",\"x\":" + x + ",\"z\":" + z
                    + ",\"allowAutoLand\":" + allowed + "}");
            return;
        }
        send(sender, "{\"error\":\"unknown station subcommand — try list|info <id>|"
                + "fuel <id> set|add|use <amount>|add-pad <id> <x> <z> [name]|"
                + "remove-pad <id> <x> <z>|pads <id>|dock <id> [commit]|"
                + "undock <id> <x> <z>|set-autoland <id> <x> <z> <bool>\"}");
    }

    // §5.6 Satellite probes ---------------------------------------------------

    private void handleSatellite(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 3 && "create".equalsIgnoreCase(args[0])) {
            // satellite create <dim> <typeId> [powerGen] [powerStorage] [maxData] [weight]
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            String typeId = args[2];
            int powerGen = args.length >= 4 ? parseIntOr(args[3], 100) : 100;
            int powerStorage = args.length >= 5 ? parseIntOr(args[4], 1000) : 1000;
            int maxData = args.length >= 6 ? parseIntOr(args[5], 1000) : 1000;
            float weight = args.length >= 7 ? Float.parseFloat(args[6]) : 1.0f;

            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                return;
            }
            SatelliteBase sat = zmaster587.advancedRocketry.api.SatelliteRegistry.getNewSatellite(typeId);
            if (sat == null) {
                send(sender, "{\"error\":\"unknown satellite type\",\"type\":\"" + escapeJson(typeId) + "\"}");
                return;
            }
            zmaster587.advancedRocketry.api.satellite.SatelliteProperties sp =
                    new zmaster587.advancedRocketry.api.satellite.SatelliteProperties(
                            powerGen, powerStorage, typeId, maxData, weight);
            long satId = System.nanoTime() & 0x7fffffffffffffffL;
            sp.setId(satId);
            // SatelliteBase.setProperties only accepts ItemStack; inject the
            // properties object directly into the private field via reflection.
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.api.satellite.SatelliteBase
                        .class.getDeclaredField("satelliteProperties");
                f.setAccessible(true);
                f.set(sat, sp);
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"failed to inject satelliteProperties\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            sat.setDimensionId(dim);
            // SatelliteBase's constructor sizes the battery off the freshly-
            // built (empty) satelliteProperties and SatelliteData's
            // constructor builds DataStorage with no maxData — neither
            // re-syncs when satelliteProperties is later swapped in via
            // reflection. Mirror what setProperties(ItemStack) would do
            // so the synthetic satellite behaves like a builder-assembled
            // one when tested.
            try {
                java.lang.reflect.Field bf = SatelliteBase.class.getDeclaredField("battery");
                bf.setAccessible(true);
                zmaster587.libVulpes.util.UniversalBattery batt =
                        (zmaster587.libVulpes.util.UniversalBattery) bf.get(sat);
                batt.setMaxEnergyStored(powerStorage);
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"failed to size battery\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            if (sat instanceof zmaster587.advancedRocketry.satellite.SatelliteData) {
                zmaster587.advancedRocketry.satellite.SatelliteData sd =
                        (zmaster587.advancedRocketry.satellite.SatelliteData) sat;
                sd.data.setMaxData(maxData);
                // SatelliteData's constructor pre-computes powerConsumption +
                // collectionTime off the empty satelliteProperties (powerGen=0
                // → collectionTime = 200/sqrt(0) = Integer.MAX_VALUE on int
                // cast). Mirror what setProperties(ItemStack) does so the
                // worldTime % collectionTime data gate fires within a
                // reasonable tick budget.
                try {
                    java.lang.reflect.Field pcf = zmaster587.advancedRocketry.satellite.SatelliteData
                            .class.getDeclaredField("powerConsumption");
                    pcf.setAccessible(true);
                    pcf.setInt(sd, powerGen);
                    java.lang.reflect.Field ctf = zmaster587.advancedRocketry.satellite.SatelliteData
                            .class.getDeclaredField("collectionTime");
                    ctf.setAccessible(true);
                    int collectionTime = (int) (200.0 / Math.sqrt(0.1 * powerGen));
                    if (collectionTime <= 0) collectionTime = 200;
                    ctf.setInt(sd, collectionTime);
                } catch (ReflectiveOperationException e) {
                    send(sender, "{\"error\":\"failed to init SatelliteData fields\",\"msg\":\""
                            + escapeJson(e.getMessage()) + "\"}");
                    return;
                }
            }
            initMissionPersistentNbtIfNeeded(sat);
            props.addSatellite(sat, dim, false);
            send(sender, "{\"ok\":true,\"id\":" + satId + ",\"type\":\"" + escapeJson(typeId)
                    + "\",\"dim\":" + dim + ",\"powerGen\":" + powerGen + "}");
            return;
        }
        if ("types".equalsIgnoreCase(args[0])) {
            // Reflect SatelliteRegistry.registry (private static HashMap<String, Class>)
            // and return the registered satellite type names.
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.api.SatelliteRegistry
                        .class.getDeclaredField("registry");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Class<?>> registry = (Map<String, Class<?>>) f.get(null);
                java.util.Set<String> sorted = new java.util.TreeSet<>(registry.keySet());
                StringBuilder builder = new StringBuilder("{\"satelliteTypes\":[");
                boolean first = true;
                for (String type : sorted) {
                    if (!first) builder.append(',');
                    first = false;
                    builder.append('"').append(escapeJson(type)).append('"');
                }
                builder.append("]}");
                send(sender, builder.toString());
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
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
        if ("imprint-terminal".equalsIgnoreCase(args[0]) && args.length >= 6) {
            // imprint-terminal <dim> <x> <y> <z> <satId>
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            long satId = parseLongOr(args[5], Long.MIN_VALUE);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.satellite.TileSatelliteTerminal)) {
                send(sender, "{\"error\":\"tile not TileSatelliteTerminal\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            SatelliteBase sat = props == null ? null : props.getSatellite(satId);
            if (sat == null) {
                send(sender, "{\"error\":\"satellite not registered\",\"dim\":" + dim
                        + ",\"id\":" + satId + "}");
                return;
            }
            net.minecraft.item.ItemStack chip = new net.minecraft.item.ItemStack(
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemSatelliteIdChip);
            // ItemSatelliteIdentificationChip.setSatellite mutates a NBT
            // reference but does NOT call stack.setTagCompound when the stack
            // is freshly created with no tag — the writes get discarded.
            // Pre-attach an empty NBT so setSatellite's writes stick.
            chip.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
            ((zmaster587.advancedRocketry.item.ItemSatelliteIdentificationChip)
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemSatelliteIdChip)
                    .setSatellite(chip, sat);
            ((net.minecraft.inventory.IInventory) tile).setInventorySlotContents(0, chip);
            send(sender, "{\"ok\":true,\"chipSlot\":0,\"satId\":" + satId + "}");
            return;
        }
        if ("terminal-info".equalsIgnoreCase(args[0]) && args.length >= 5) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.satellite.TileSatelliteTerminal)) {
                send(sender, "{\"error\":\"tile not TileSatelliteTerminal\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            zmaster587.advancedRocketry.tile.satellite.TileSatelliteTerminal terminal =
                    (zmaster587.advancedRocketry.tile.satellite.TileSatelliteTerminal) tile;
            SatelliteBase linked = terminal.getSatelliteFromSlot(0);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("hasChip", !((net.minecraft.inventory.IInventory) tile).getStackInSlot(0).isEmpty());
            if (linked == null) {
                info.put("linkedSatelliteId", -1);
                info.put("linkedType", "null");
            } else {
                info.put("linkedSatelliteId", linked.getId());
                info.put("linkedType", linked.getProperties().getSatelliteType());
                info.put("linkedDim", linked.getDimensionId());
            }
            send(sender, jsonMap(info));
            return;
        }
        if ("tick".equalsIgnoreCase(args[0]) && args.length >= 4) {
            // /artest satellite tick <dim> <satId> <ticks>
            //
            // Directly invokes SatelliteBase.tickEntity() N times on the
            // satellite, bypassing the world tick scheduler. Each call
            // also advances the overworld's totalWorldTime by 1 — this
            // is what SatelliteData subclasses query through
            // AdvancedRocketry.proxy.getWorldTimeUniversal(0) for their
            // % collectionTime == 0 data-gate. Without the bump, the
            // gate either always-fires or never-fires across the whole
            // batch depending on starting worldTime, which makes
            // SatelliteData accumulation tests non-deterministic.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            int ticks = parseIntOr(args[3], 1);
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
            net.minecraft.world.WorldServer overworld = server.getWorld(0);
            long startTime = overworld == null ? -1 : overworld.getTotalWorldTime();
            // Capture pre-tick battery/data snapshots BEFORE the loop, then
            // post-tick AFTER, both on the same server thread call. Tests
            // can assert on the delta (preStored→postStored, preData→postData)
            // to nail down the per-tick contract without contamination from
            // background DimensionManager.tickDimensions ticks that fire
            // between probe invocations.
            zmaster587.libVulpes.util.UniversalBattery batt = null;
            try {
                java.lang.reflect.Field bf = zmaster587.advancedRocketry.api.satellite.SatelliteBase
                        .class.getDeclaredField("battery");
                bf.setAccessible(true);
                batt = (zmaster587.libVulpes.util.UniversalBattery) bf.get(sat);
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"battery reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            long preStored = batt.getUniversalEnergyStored();
            long preData = -1L;
            if (sat instanceof zmaster587.advancedRocketry.satellite.SatelliteData) {
                preData = ((zmaster587.advancedRocketry.satellite.SatelliteData) sat).data.getData();
            }
            int actualTicked = 0;
            try {
                for (int i = 0; i < ticks; i++) {
                    if (overworld != null) {
                        overworld.getWorldInfo().setWorldTotalTime(startTime + i + 1);
                    }
                    sat.tickEntity();
                    actualTicked++;
                }
            } catch (RuntimeException e) {
                send(sender, "{\"error\":\"tickEntity threw after " + actualTicked + " ticks: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage()) + "\"}");
                return;
            } finally {
                if (overworld != null) overworld.getWorldInfo().setWorldTotalTime(startTime);
            }
            long postStored = batt.getUniversalEnergyStored();
            long postData = -1L;
            if (sat instanceof zmaster587.advancedRocketry.satellite.SatelliteData) {
                postData = ((zmaster587.advancedRocketry.satellite.SatelliteData) sat).data.getData();
            }
            send(sender, "{\"ok\":true,\"id\":" + satId + ",\"dim\":" + dim
                    + ",\"ticked\":" + actualTicked
                    + ",\"preStored\":" + preStored
                    + ",\"postStored\":" + postStored
                    + ",\"preData\":" + preData
                    + ",\"postData\":" + postData
                    + ",\"satClass\":\"" + sat.getClass().getName() + "\"}");
            return;
        }
        if ("battery".equalsIgnoreCase(args[0]) && args.length >= 3) {
            // /artest satellite battery <dim> <satId>
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
            // SatelliteBase.battery is protected — reach it via reflection so
            // future probe additions don't need a getter on the public API.
            try {
                java.lang.reflect.Field bf = zmaster587.advancedRocketry.api.satellite.SatelliteBase
                        .class.getDeclaredField("battery");
                bf.setAccessible(true);
                zmaster587.libVulpes.util.UniversalBattery batt =
                        (zmaster587.libVulpes.util.UniversalBattery) bf.get(sat);
                send(sender, "{\"ok\":true,\"id\":" + satId
                        + ",\"stored\":" + batt.getUniversalEnergyStored()
                        + ",\"max\":" + batt.getMaxEnergyStored() + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if ("data".equalsIgnoreCase(args[0]) && args.length >= 3) {
            // /artest satellite data <dim> <satId>
            //
            // SatelliteData family only — exposes the DataStorage state
            // (current data points, max, data type). Errors out cleanly
            // for non-SatelliteData satellites so tests can use this as
            // a class-family probe too.
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
            if (!(sat instanceof zmaster587.advancedRocketry.satellite.SatelliteData)) {
                send(sender, "{\"error\":\"not a SatelliteData subclass\",\"satClass\":\""
                        + sat.getClass().getName() + "\"}");
                return;
            }
            zmaster587.advancedRocketry.satellite.SatelliteData sd =
                    (zmaster587.advancedRocketry.satellite.SatelliteData) sat;
            zmaster587.advancedRocketry.api.DataStorage ds = sd.data;
            send(sender, "{\"ok\":true,\"id\":" + satId
                    + ",\"data\":" + ds.getData()
                    + ",\"maxData\":" + ds.getMaxData()
                    + ",\"dataType\":\"" + ds.getDataType() + "\"}");
            return;
        }
        if ("markers".equalsIgnoreCase(args[0]) && args.length >= 3) {
            // /artest satellite markers <dim> <satId> — exposes marker
            // interfaces relevant for per-type contract tests
            // (IUniversalEnergyTransmitter, IUniversalEnergy, etc.).
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
            send(sender, "{\"ok\":true,\"id\":" + satId
                    + ",\"satClass\":\"" + sat.getClass().getName() + "\""
                    + ",\"canTick\":" + sat.canTick()
                    + ",\"isUniversalEnergyTransmitter\":"
                    + (sat instanceof zmaster587.libVulpes.api.IUniversalEnergyTransmitter)
                    + ",\"isUniversalEnergy\":"
                    + (sat instanceof zmaster587.libVulpes.api.IUniversalEnergy)
                    + ",\"isSatelliteData\":"
                    + (sat instanceof zmaster587.advancedRocketry.satellite.SatelliteData) + "}");
            return;
        }
        if ("force-charge".equalsIgnoreCase(args[0]) && args.length >= 4) {
            // /artest satellite force-charge <dim> <satId> <amount> —
            // injects energy directly into the battery (battery.acceptEnergy
            // with simulate=false). Used to pre-charge the BiomeChanger /
            // WeatherController above their per-action threshold without
            // having to spin many ticks.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            int amount = parseIntOr(args[3], 0);
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
            try {
                java.lang.reflect.Field bf = zmaster587.advancedRocketry.api.satellite.SatelliteBase
                        .class.getDeclaredField("battery");
                bf.setAccessible(true);
                zmaster587.libVulpes.util.UniversalBattery batt =
                        (zmaster587.libVulpes.util.UniversalBattery) bf.get(sat);
                int accepted = batt.acceptEnergy(amount, false);
                send(sender, "{\"ok\":true,\"id\":" + satId + ",\"accepted\":" + accepted
                        + ",\"stored\":" + batt.getUniversalEnergyStored() + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if ("biome-add-pos".equalsIgnoreCase(args[0]) && args.length >= 6) {
            // /artest satellite biome-add-pos <dim> <satId> <x> <y> <z>
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            int x = parseIntOr(args[3], 0);
            int y = parseIntOr(args[4], 0);
            int z = parseIntOr(args[5], 0);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                return;
            }
            SatelliteBase sat = props.getSatellite(satId);
            if (!(sat instanceof zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger)) {
                send(sender, "{\"error\":\"not a SatelliteBiomeChanger\",\"satClass\":\""
                        + (sat == null ? "null" : sat.getClass().getName()) + "\"}");
                return;
            }
            ((zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger) sat).addBlockToList(
                    new zmaster587.libVulpes.util.HashedBlockPosition(x, y, z));
            send(sender, "{\"ok\":true,\"id\":" + satId + ",\"added\":[" + x + "," + y + "," + z + "]}");
            return;
        }
        if ("biome-set".equalsIgnoreCase(args[0]) && args.length >= 4) {
            // /artest satellite biome-set <dim> <satId> <biomeId>
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            int biomeIdInt = parseIntOr(args[3], -1);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                return;
            }
            SatelliteBase sat = props.getSatellite(satId);
            if (!(sat instanceof zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger)) {
                send(sender, "{\"error\":\"not a SatelliteBiomeChanger\"}");
                return;
            }
            net.minecraft.world.biome.Biome b = net.minecraft.world.biome.Biome.getBiome(biomeIdInt);
            if (b == null) {
                send(sender, "{\"error\":\"unknown biome id\",\"id\":" + biomeIdInt + "}");
                return;
            }
            ((zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger) sat).setBiome(b);
            send(sender, "{\"ok\":true,\"id\":" + satId + ",\"biomeId\":" + biomeIdInt
                    + ",\"biomeName\":\"" + escapeJson(b.getRegistryName().toString()) + "\"}");
            return;
        }
        if ("biome-list-size".equalsIgnoreCase(args[0]) && args.length >= 3) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            SatelliteBase sat = props == null ? null : props.getSatellite(satId);
            if (!(sat instanceof zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger)) {
                send(sender, "{\"error\":\"not a SatelliteBiomeChanger\"}");
                return;
            }
            try {
                java.lang.reflect.Field lf = zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger
                        .class.getDeclaredField("toChangeList");
                lf.setAccessible(true);
                java.util.List<?> list = (java.util.List<?>) lf.get(sat);
                send(sender, "{\"ok\":true,\"id\":" + satId + ",\"listSize\":" + list.size() + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if ("weather-add-pos".equalsIgnoreCase(args[0]) && args.length >= 6) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            int x = parseIntOr(args[3], 0);
            int y = parseIntOr(args[4], 0);
            int z = parseIntOr(args[5], 0);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            SatelliteBase sat = props == null ? null : props.getSatellite(satId);
            if (!(sat instanceof zmaster587.advancedRocketry.satellite.SatelliteWeatherController)) {
                send(sender, "{\"error\":\"not a SatelliteWeatherController\"}");
                return;
            }
            try {
                java.lang.reflect.Field vf = zmaster587.advancedRocketry.satellite.SatelliteWeatherController
                        .class.getDeclaredField("viable_positions");
                vf.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<BlockPos> list = (java.util.List<BlockPos>) vf.get(sat);
                list.add(new BlockPos(x, y, z));
                send(sender, "{\"ok\":true,\"id\":" + satId + ",\"added\":[" + x + "," + y + "," + z
                        + "],\"listSize\":" + list.size() + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if ("weather-mode".equalsIgnoreCase(args[0]) && args.length >= 4) {
            // /artest satellite weather-mode <dim> <satId> <mode> [update-last]
            //
            // update-last defaults to true → also bumps last_mode_id so the
            // next tick does NOT enter the "mode changed, clear list"
            // branch. Set to false when the test wants to pin exactly
            // that branch.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            int mode = parseIntOr(args[3], 0);
            boolean updateLast = args.length < 5 || Boolean.parseBoolean(args[4]);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            SatelliteBase sat = props == null ? null : props.getSatellite(satId);
            if (!(sat instanceof zmaster587.advancedRocketry.satellite.SatelliteWeatherController)) {
                send(sender, "{\"error\":\"not a SatelliteWeatherController\"}");
                return;
            }
            zmaster587.advancedRocketry.satellite.SatelliteWeatherController wc =
                    (zmaster587.advancedRocketry.satellite.SatelliteWeatherController) sat;
            wc.mode_id = mode;
            if (updateLast) wc.last_mode_id = mode;
            send(sender, "{\"ok\":true,\"id\":" + satId + ",\"mode_id\":" + mode
                    + ",\"last_mode_id\":" + wc.last_mode_id + "}");
            return;
        }
        if ("weather-discard-test".equalsIgnoreCase(args[0]) && args.length >= 8) {
            // /artest satellite weather-discard-test <dim> <satId>
            //                                         <newMode> <baseX> <y> <z> <numPositions>
            //
            // Atomic compound probe — all four operations run on the
            // server thread within ONE command dispatch, so no
            // DimensionManager background tick can interleave:
            //   1. set mode_id = last_mode_id = 0 (synced baseline)
            //   2. add N AIR-targeting positions to viable_positions
            //   3. set mode_id = newMode (now last_mode_id (0) != mode_id)
            //   4. invoke sat.tickEntity() once — the mismatch fires
            //      the clear-on-mode-change branch BEFORE either old
            //      or new mode runs against the queue
            //
            // Used to pin the contract "mode change between queue-build
            // and tick discards queued work" — the visible-block-state
            // assertion lives in the test; this probe just guarantees
            // the race-free server-thread atomicity.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            int newMode = parseIntOr(args[3], 0);
            int baseX = parseIntOr(args[4], 0);
            int y = parseIntOr(args[5], 0);
            int z = parseIntOr(args[6], 0);
            int n = parseIntOr(args[7], 1);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            SatelliteBase sat = props == null ? null : props.getSatellite(satId);
            if (!(sat instanceof zmaster587.advancedRocketry.satellite.SatelliteWeatherController)) {
                send(sender, "{\"error\":\"not a SatelliteWeatherController\"}");
                return;
            }
            zmaster587.advancedRocketry.satellite.SatelliteWeatherController wc =
                    (zmaster587.advancedRocketry.satellite.SatelliteWeatherController) sat;
            try {
                java.lang.reflect.Field vf = zmaster587.advancedRocketry.satellite.SatelliteWeatherController
                        .class.getDeclaredField("viable_positions");
                vf.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<BlockPos> list = (java.util.List<BlockPos>) vf.get(wc);
                list.clear();
                wc.mode_id = 0;
                wc.last_mode_id = 0;
                for (int i = 0; i < n; i++) {
                    list.add(new BlockPos(baseX + i, y, z));
                }
                wc.mode_id = newMode;
                // Single atomic tickEntity — the mismatch branch fires
                // inside it.
                wc.tickEntity();
                send(sender, "{\"ok\":true,\"id\":" + satId + ",\"mode_id\":"
                        + wc.mode_id + ",\"last_mode_id\":" + wc.last_mode_id + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if ("weather-list-size".equalsIgnoreCase(args[0]) && args.length >= 3) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            SatelliteBase sat = props == null ? null : props.getSatellite(satId);
            if (!(sat instanceof zmaster587.advancedRocketry.satellite.SatelliteWeatherController)) {
                send(sender, "{\"error\":\"not a SatelliteWeatherController\"}");
                return;
            }
            try {
                java.lang.reflect.Field vf = zmaster587.advancedRocketry.satellite.SatelliteWeatherController
                        .class.getDeclaredField("viable_positions");
                vf.setAccessible(true);
                java.util.List<?> list = (java.util.List<?>) vf.get(sat);
                send(sender, "{\"ok\":true,\"id\":" + satId + ",\"listSize\":" + list.size() + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if ("biome-null".equalsIgnoreCase(args[0]) && args.length >= 3) {
            // /artest satellite biome-null <dim> <satId> — sets the
            // BiomeChanger's biomeId to null via reflection. Pins the
            // BiomeHandler.terraform null-guard ("if (biomeId == null) return;").
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            SatelliteBase sat = props == null ? null : props.getSatellite(satId);
            if (!(sat instanceof zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger)) {
                send(sender, "{\"error\":\"not a SatelliteBiomeChanger\"}");
                return;
            }
            try {
                java.lang.reflect.Field bf = zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger
                        .class.getDeclaredField("biomeId");
                bf.setAccessible(true);
                bf.set(sat, null);
                send(sender, "{\"ok\":true,\"id\":" + satId + ",\"biomeId\":null}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if ("ticking-list".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // /artest satellite ticking-list <dim> — exposes the
            // DimensionProperties.tickingSatellites map (satellites that
            // canTick=true at register-time). Anything in `satellites`
            // map but NOT here pins the canTick-gates-registration
            // contract.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                return;
            }
            try {
                java.lang.reflect.Field f = DimensionProperties.class.getDeclaredField("tickingSatellites");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Long, SatelliteBase> map = (Map<Long, SatelliteBase>) f.get(props);
                StringBuilder builder = new StringBuilder("{\"dim\":").append(dim)
                        .append(",\"size\":").append(map.size())
                        .append(",\"ids\":[");
                boolean first = true;
                for (Long id : map.keySet()) {
                    if (!first) builder.append(',');
                    first = false;
                    builder.append(id);
                }
                builder.append("]}");
                send(sender, builder.toString());
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if ("set-dead".equalsIgnoreCase(args[0]) && args.length >= 3) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            long satId = parseLongOr(args[2], Long.MIN_VALUE);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            SatelliteBase sat = props == null ? null : props.getSatellite(satId);
            if (sat == null) {
                send(sender, "{\"error\":\"satellite not found\",\"dim\":" + dim + ",\"id\":" + satId + "}");
                return;
            }
            sat.setDead();
            send(sender, "{\"ok\":true,\"id\":" + satId + ",\"isDead\":" + sat.isDead() + "}");
            return;
        }
        if ("force-tick-dim".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // /artest satellite force-tick-dim <dim> — invokes
            // DimensionProperties.tick() directly. Used to drive the
            // isDead-removal branch deterministically (instead of
            // waiting for the natural DimensionManager.tickDimensions
            // background tick).
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                return;
            }
            props.tick();
            send(sender, "{\"ok\":true,\"dim\":" + dim + "}");
            return;
        }
        if ("create-spy-telescope".equalsIgnoreCase(args[0]) && args.length >= 2) {
            // /artest satellite create-spy-telescope <dim> — registers a
            // SatelliteSpyTelescope (an orphan class — not in the
            // public SatelliteRegistry registry but instantiable).
            // SpyTelescope.canTick() returns false; this probe is the
            // only way to drop one into a dim for the
            // "canTick=false-gates-registration" pin.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                return;
            }
            zmaster587.advancedRocketry.satellite.SatelliteSpyTelescope spy =
                    new zmaster587.advancedRocketry.satellite.SatelliteSpyTelescope();
            zmaster587.advancedRocketry.api.satellite.SatelliteProperties sp =
                    new zmaster587.advancedRocketry.api.satellite.SatelliteProperties(
                            100, 1000, "spyTelescope", 100, 1.0f);
            long satId = System.nanoTime() & 0x7fffffffffffffffL;
            sp.setId(satId);
            try {
                java.lang.reflect.Field f = SatelliteBase.class.getDeclaredField("satelliteProperties");
                f.setAccessible(true);
                f.set(spy, sp);
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            spy.setDimensionId(dim);
            props.addSatellite(spy, dim, false);
            send(sender, "{\"ok\":true,\"id\":" + satId + ",\"canTick\":" + spy.canTick() + "}");
            return;
        }
        if ("can-tick".equalsIgnoreCase(args[0]) && args.length >= 3) {
            // /artest satellite can-tick <dim> <satId> — pins
            // SatelliteBase.canTick() per-type contract (e.g. SpyTelescope
            // returns false).
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
            send(sender, "{\"ok\":true,\"id\":" + satId
                    + ",\"satClass\":\"" + sat.getClass().getName() + "\""
                    + ",\"canTick\":" + sat.canTick() + "}");
            return;
        }
        send(sender, "{\"error\":\"unknown satellite subcommand — try list <dim> | info <dim> <id> | create <dim> <type> [...] | types | imprint-terminal <dim> <x> <y> <z> <satId> | terminal-info <dim> <x> <y> <z> | tick <dim> <id> <ticks> | battery <dim> <id> | data <dim> <id> | can-tick <dim> <id>\"}");
    }

    /**
     * §7.12 — satellite-builder synthesis.
     *
     * <p>{@code /artest satellite-builder build <dim> <typeId>} — mirrors
     * {@link zmaster587.advancedRocketry.tile.satellite.TileSatelliteBuilder#assembleSatellite}'s
     * per-slot aggregation against synthetic component ItemStacks for the
     * requested satellite type. Uses {@link
     * zmaster587.advancedRocketry.api.SatelliteRegistry#getSatelliteProperty}
     * for each input (same lookup the production builder runs against player-
     * inserted chips, generators, batteries), then registers the resulting
     * satellite in the dim — bypassing the multiblock-validation requirement
     * that headless harness can't satisfy.</p>
     */
    private void handleSatelliteBuilder(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 3 || !"build".equalsIgnoreCase(args[0])) {
            send(sender, "{\"error\":\"unknown satellite-builder subcommand — try build <dim> <typeId>\"}");
            return;
        }
        int dim = parseIntOr(args[1], Integer.MIN_VALUE);
        String typeId = args[2];
        DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
        if (props == null) {
            send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
            return;
        }
        // Resolve primary-function chip meta by scanning the registry: each
        // itemSatellitePrimaryFunction meta is registered as a property whose
        // SatelliteType matches one of the known type ids.
        net.minecraft.item.Item primaryItem =
                zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemSatellitePrimaryFunction;
        if (primaryItem == null) {
            send(sender, "{\"error\":\"itemSatellitePrimaryFunction not initialised\"}");
            return;
        }
        int primaryMeta = -1;
        for (int meta = 0; meta < 16; meta++) {
            net.minecraft.item.ItemStack candidate = new net.minecraft.item.ItemStack(primaryItem, 1, meta);
            zmaster587.advancedRocketry.api.satellite.SatelliteProperties sp =
                    zmaster587.advancedRocketry.api.SatelliteRegistry.getSatelliteProperty(candidate);
            if (sp != null && typeId.equalsIgnoreCase(sp.getSatelliteType())) {
                primaryMeta = meta;
                break;
            }
        }
        if (primaryMeta < 0) {
            send(sender, "{\"error\":\"no primary-function chip meta maps to type\",\"type\":\""
                    + escapeJson(typeId) + "\"}");
            return;
        }
        // Aggregate properties the way assembleSatellite does. We use the
        // strongest stock power source (meta 1) for a non-trivial
        // generation reading, and a single itemBattery for storage.
        net.minecraft.item.ItemStack primary = new net.minecraft.item.ItemStack(primaryItem, 1, primaryMeta);
        net.minecraft.item.ItemStack powerSrc = new net.minecraft.item.ItemStack(
                zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemSatellitePowerSource, 1, 1);
        net.minecraft.item.ItemStack battery = new net.minecraft.item.ItemStack(
                zmaster587.libVulpes.api.LibVulpesItems.itemBattery, 1, 0);
        int powerGeneration = 0, powerStorage = 0, maxData = 0;
        float weight = 0;
        for (net.minecraft.item.ItemStack stack : new net.minecraft.item.ItemStack[]{primary, powerSrc, battery}) {
            zmaster587.advancedRocketry.api.satellite.SatelliteProperties sp =
                    zmaster587.advancedRocketry.api.SatelliteRegistry.getSatelliteProperty(stack);
            if (sp == null) continue;
            int flag = sp.getPropertyFlag();
            if (flag == zmaster587.advancedRocketry.api.satellite.SatelliteProperties.Property.POWER_GEN.getFlag())
                powerGeneration += sp.getPowerGeneration();
            if (flag == zmaster587.advancedRocketry.api.satellite.SatelliteProperties.Property.BATTERY.getFlag())
                powerStorage += sp.getPowerStorage();
            if (flag == zmaster587.advancedRocketry.api.satellite.SatelliteProperties.Property.DATA.getFlag())
                maxData += sp.getMaxDataStorage();
            weight += zmaster587.advancedRocketry.util.WeightEngine.INSTANCE.getWeight(stack);
        }
        zmaster587.advancedRocketry.api.satellite.SatelliteProperties finalProps =
                new zmaster587.advancedRocketry.api.satellite.SatelliteProperties(
                        powerGeneration, powerStorage + 720, typeId, maxData, weight);
        long satId = DimensionManager.getInstance().getNextSatelliteId();
        finalProps.setId(satId);
        SatelliteBase sat = zmaster587.advancedRocketry.api.SatelliteRegistry.getNewSatellite(typeId);
        if (sat == null) {
            send(sender, "{\"error\":\"unknown satellite type\",\"type\":\""
                    + escapeJson(typeId) + "\"}");
            return;
        }
        try {
            java.lang.reflect.Field f = zmaster587.advancedRocketry.api.satellite.SatelliteBase
                    .class.getDeclaredField("satelliteProperties");
            f.setAccessible(true);
            f.set(sat, finalProps);
        } catch (ReflectiveOperationException e) {
            send(sender, "{\"error\":\"failed to inject satelliteProperties\",\"msg\":\""
                    + escapeJson(e.getMessage()) + "\"}");
            return;
        }
        sat.setDimensionId(dim);
        initMissionPersistentNbtIfNeeded(sat);
        props.addSatellite(sat, dim, false);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("id", satId);
        info.put("type", typeId);
        info.put("primaryMeta", primaryMeta);
        info.put("powerGen", powerGeneration);
        info.put("powerStorage", powerStorage + 720);
        info.put("maxData", maxData);
        send(sender, jsonMap(info));
    }

    /**
     * MissionResourceCollection subclasses (asteroidMiner, gasMining) keep a
     * {@code missionPersistantNBT} field that's normally populated when the
     * mission is launched by a real player. The no-arg constructor leaves it
     * null, which crashes the world-save NBT path. Pre-attach an empty NBT
     * so the satellite can be registered + saved without a real launch.
     */
    private static void initMissionPersistentNbtIfNeeded(SatelliteBase sat) {
        if (!(sat instanceof zmaster587.advancedRocketry.mission.MissionResourceCollection)) {
            return;
        }
        // MissionResourceCollection's no-arg constructor leaves several
        // fields null, all of which would NPE in writeToNBT during a world
        // save. Production normally populates them via the launched-rocket
        // ctor; the test harness can't launch a real rocket, so we seed safe
        // defaults so a persisted mission satellite survives a level save.
        try {
            initFieldIfNull(sat, "missionPersistantNBT", new net.minecraft.nbt.NBTTagCompound());
            initFieldIfNull(sat, "rocketStats", new zmaster587.advancedRocketry.api.StatsRocket());
            initFieldIfNull(sat, "rocketStorage", new zmaster587.advancedRocketry.util.StorageChunk());
            initFieldIfNull(sat, "infrastructureCoords", new java.util.LinkedList<>());
            // tickEntity fires onMissionComplete when getProgress() ≥ 1.
            // Default duration=0 + non-zero worldTime → progress=+inf →
            // mission instantly "completes" and crashes (the synthetic
            // mission has no real rocket to land). Push duration into the
            // far future so the tick gate stays closed for the test run.
            setLongField(sat, "duration", Long.MAX_VALUE / 4);
        } catch (RuntimeException ignored) {
            // Defensive — never fail probe registration on the helper's behalf.
        }
    }

    private static void setLongField(Object target, String name, long value) {
        try {
            java.lang.reflect.Field f = zmaster587.advancedRocketry.mission
                    .MissionResourceCollection.class.getDeclaredField(name);
            f.setAccessible(true);
            f.setLong(target, value);
        } catch (ReflectiveOperationException ignored) {
            // Field renamed in a fork — silently skip.
        }
    }

    private static void initFieldIfNull(Object target, String name, Object value) {
        if (value == null) return;
        try {
            java.lang.reflect.Field f = zmaster587.advancedRocketry.mission
                    .MissionResourceCollection.class.getDeclaredField(name);
            f.setAccessible(true);
            if (f.get(target) == null) {
                f.set(target, value);
            }
        } catch (ReflectiveOperationException ignored) {
            // Field renamed or removed in a fork — silently skip; if the
            // missing field is actually load-bearing the save will surface
            // the NPE clearly.
        }
    }

    /**
     * §7.17 — wireless transceiver probes.
     *
     * <p>{@code /artest pipe wireless-pair <dim> <x1> <y1> <z1> <x2> <y2> <z2>}
     * — drives the same network-merge logic
     * {@link zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever#onLinkComplete}
     * runs when a player completes a linker-item handshake between two
     * transceivers, but without needing a player or linker item. Returns
     * the resulting shared {@code networkID} so tests can confirm both
     * tiles end up on the same dataNetwork.</p>
     *
     * <p>{@code /artest pipe wireless-info <dim> <x> <y> <z>} — reads the
     * tile's current {@code networkID}, {@code mode}
     * (extract/inject), {@code enabled}.</p>
     *
     * <p>{@code /artest pipe wireless-set-mode <dim> <x> <y> <z> <extract|inject>}
     * — mirrors the GUI toggle: writes {@code extractMode}, calls
     * {@code removeFromAll} on the network, re-registers as source or
     * sink.</p>
     *
     * <p>{@code /artest pipe wireless-set-enabled <dim> <x> <y> <z> <true|false>}
     * — writes the {@code enabled} field + {@code markDirty}.</p>
     *
     * <p>{@code /artest pipe wireless-role-on-network <dim> <x> <y> <z>}
     * — reads back the observed role of this tile in its
     * {@code dataNetwork}: {@code "isSource"} / {@code "isSink"}. The
     * tile's {@code extractMode} field is one thing; its actual
     * registration is the contract.</p>
     */
    private void handlePipe(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 8 && "wireless-pair".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x1 = parseIntOr(args[2], 0);
            int y1 = parseIntOr(args[3], 0);
            int z1 = parseIntOr(args[4], 0);
            int x2 = parseIntOr(args[5], 0);
            int y2 = parseIntOr(args[6], 0);
            int z2 = parseIntOr(args[7], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile1 = world.getTileEntity(new BlockPos(x1, y1, z1));
            TileEntity tile2 = world.getTileEntity(new BlockPos(x2, y2, z2));
            if (!(tile1 instanceof zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever)
                    || !(tile2 instanceof zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever)) {
                send(sender, "{\"error\":\"one or both tiles not TileWirelessTransciever\","
                        + "\"tile1\":\"" + (tile1 == null ? "null" : tile1.getClass().getName())
                        + "\",\"tile2\":\"" + (tile2 == null ? "null" : tile2.getClass().getName())
                        + "\"}");
                return;
            }
            zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever t1 =
                    (zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever) tile1;
            zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever t2 =
                    (zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever) tile2;
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.tile.cables
                        .TileWirelessTransciever.class.getDeclaredField("networkID");
                f.setAccessible(true);
                int id1 = f.getInt(t1);
                int id2 = f.getInt(t2);
                // Mirror onLinkComplete's branch logic exactly.
                int shared;
                if (id1 == -1 && id2 == -1) {
                    shared = zmaster587.advancedRocketry.cable.NetworkRegistry.dataNetwork.getNewNetworkID();
                    f.setInt(t1, shared);
                    f.setInt(t2, shared);
                } else if (id1 == -1) {
                    shared = id2;
                    f.setInt(t1, shared);
                } else if (id2 == -1) {
                    shared = id1;
                    f.setInt(t2, shared);
                } else if (id1 == id2) {
                    shared = id1;
                } else {
                    shared = zmaster587.advancedRocketry.cable.NetworkRegistry
                            .dataNetwork.mergeNetworks(id1, id2);
                    f.setInt(t1, shared);
                    f.setInt(t2, shared);
                }
                // Mirror onLinkComplete's addToNetwork() postlude — invoke
                // the same private method so the network actually registers
                // both endpoints as connected nodes.
                try {
                    java.lang.reflect.Method m = zmaster587.advancedRocketry.tile.cables
                            .TileWirelessTransciever.class.getDeclaredMethod("addToNetwork");
                    m.setAccessible(true);
                    m.invoke(t1);
                    m.invoke(t2);
                } catch (NoSuchMethodException ignored) {
                    // Method renamed in a fork — leave the network in the
                    // ID-merge state; tests can still verify shared id.
                }
                send(sender, "{\"ok\":true,\"id1Before\":" + id1
                        + ",\"id2Before\":" + id2
                        + ",\"sharedNetworkId\":" + shared + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if (args.length >= 5 && "wireless-info".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever)) {
                send(sender, "{\"error\":\"tile not TileWirelessTransciever\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            try {
                Class<?> cls = zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever.class;
                java.lang.reflect.Field fId = cls.getDeclaredField("networkID");
                java.lang.reflect.Field fMode = cls.getDeclaredField("extractMode");
                java.lang.reflect.Field fEnabled = cls.getDeclaredField("enabled");
                fId.setAccessible(true);
                fMode.setAccessible(true);
                fEnabled.setAccessible(true);
                int id = fId.getInt(tile);
                boolean extractMode = fMode.getBoolean(tile);
                boolean enabled = fEnabled.getBoolean(tile);
                send(sender, "{\"ok\":true,\"networkID\":" + id
                        + ",\"mode\":\"" + (extractMode ? "extract" : "inject") + "\""
                        + ",\"enabled\":" + enabled + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if (args.length >= 6 && "wireless-set-mode".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            String modeArg = args[5];
            boolean extract;
            if ("extract".equalsIgnoreCase(modeArg)) {
                extract = true;
            } else if ("inject".equalsIgnoreCase(modeArg)) {
                extract = false;
            } else {
                send(sender, "{\"error\":\"mode must be extract|inject\",\"got\":\""
                        + escapeJson(modeArg) + "\"}");
                return;
            }
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever)) {
                send(sender, "{\"error\":\"tile not TileWirelessTransciever\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            try {
                Class<?> cls = zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever.class;
                java.lang.reflect.Field fMode = cls.getDeclaredField("extractMode");
                java.lang.reflect.Field fId = cls.getDeclaredField("networkID");
                fMode.setAccessible(true);
                fId.setAccessible(true);
                fMode.setBoolean(tile, extract);
                int netId = fId.getInt(tile);
                // Mirror useNetworkData's role-update side: remove + re-add
                // under the new mode, only if the network actually exists.
                zmaster587.advancedRocketry.cable.HandlerDataNetwork handler =
                        zmaster587.advancedRocketry.cable.NetworkRegistry.dataNetwork;
                if (handler.doesNetworkExist(netId)) {
                    zmaster587.advancedRocketry.cable.CableNetwork cableNet = handler.getNetwork(netId);
                    cableNet.removeFromAll(tile);
                    if (extract) {
                        cableNet.addSource(tile, net.minecraft.util.EnumFacing.UP);
                    } else {
                        cableNet.addSink(tile, net.minecraft.util.EnumFacing.UP);
                    }
                }
                tile.markDirty();
                send(sender, "{\"ok\":true,\"mode\":\""
                        + (extract ? "extract" : "inject") + "\"}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if (args.length >= 6 && "wireless-set-enabled".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            boolean enabled = Boolean.parseBoolean(args[5]);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever)) {
                send(sender, "{\"error\":\"tile not TileWirelessTransciever\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            try {
                java.lang.reflect.Field fEnabled = zmaster587.advancedRocketry.tile.cables
                        .TileWirelessTransciever.class.getDeclaredField("enabled");
                fEnabled.setAccessible(true);
                fEnabled.setBoolean(tile, enabled);
                tile.markDirty();
                send(sender, "{\"ok\":true,\"enabled\":" + enabled + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if (args.length >= 5 && "wireless-role-on-network".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.cables.TileWirelessTransciever)) {
                send(sender, "{\"error\":\"tile not TileWirelessTransciever\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            try {
                java.lang.reflect.Field fId = zmaster587.advancedRocketry.tile.cables
                        .TileWirelessTransciever.class.getDeclaredField("networkID");
                fId.setAccessible(true);
                int netId = fId.getInt(tile);
                zmaster587.advancedRocketry.cable.HandlerDataNetwork handler =
                        zmaster587.advancedRocketry.cable.NetworkRegistry.dataNetwork;
                boolean isSource = false;
                boolean isSink = false;
                boolean networkExists = handler.doesNetworkExist(netId);
                if (networkExists) {
                    zmaster587.advancedRocketry.cable.CableNetwork cableNet = handler.getNetwork(netId);
                    BlockPos selfPos = tile.getPos();
                    for (java.util.Map.Entry<TileEntity, net.minecraft.util.EnumFacing> e : cableNet.getSources()) {
                        if (e.getKey() != null && selfPos.equals(e.getKey().getPos())) {
                            isSource = true;
                            break;
                        }
                    }
                    for (java.util.Map.Entry<TileEntity, net.minecraft.util.EnumFacing> e : cableNet.getSinks()) {
                        if (e.getKey() != null && selfPos.equals(e.getKey().getPos())) {
                            isSink = true;
                            break;
                        }
                    }
                }
                send(sender, "{\"ok\":true,\"networkID\":" + netId
                        + ",\"networkExists\":" + networkExists
                        + ",\"isSource\":" + isSource
                        + ",\"isSink\":" + isSink + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        send(sender, "{\"error\":\"unknown pipe subcommand — try wireless-pair <dim> <x1> <y1> <z1> <x2> <y2> <z2> | wireless-info <dim> <x> <y> <z> | wireless-set-mode <dim> <x> <y> <z> <extract|inject> | wireless-set-enabled <dim> <x> <y> <z> <true|false> | wireless-role-on-network <dim> <x> <y> <z>\"}");
    }

    // §5.7 Atmosphere probe ---------------------------------------------------

    private void handleAtmosphere(MinecraftServer server, ICommandSender sender, String[] args) {
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
        if (args.length >= 3 && "set-density".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int density = parseIntOr(args[2], -1);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                return;
            }
            int oldDensity = props.getAtmosphereDensity();
            props.setAtmosphereDensity(density);
            send(sender, "{\"ok\":true,\"dim\":" + dim
                    + ",\"oldDensity\":" + oldDensity
                    + ",\"newDensity\":" + props.getAtmosphereDensity() + "}");
            return;
        }
        if (args.length >= 5 && "detector-output".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            boolean isDetector = state.getBlock() instanceof zmaster587.advancedRocketry.block.BlockRedstoneEmitter;
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("isDetector", isDetector);
            info.put("block", state.getBlock().getRegistryName() == null ? "null" : state.getBlock().getRegistryName().toString());
            if (isDetector) {
                boolean powered = state.getValue(zmaster587.advancedRocketry.block.BlockRedstoneEmitter.POWERED);
                info.put("powered", powered);
                info.put("strongPower", state.getBlock().getStrongPower(state, world, pos, net.minecraft.util.EnumFacing.UP));
                TileEntity tile = world.getTileEntity(pos);
                if (tile instanceof zmaster587.advancedRocketry.tile.atmosphere.TileAtmosphereDetector) {
                    try {
                        java.lang.reflect.Field f = zmaster587.advancedRocketry.tile.atmosphere
                                .TileAtmosphereDetector.class.getDeclaredField("atmosphereToDetect");
                        f.setAccessible(true);
                        zmaster587.advancedRocketry.api.IAtmosphere mode =
                                (zmaster587.advancedRocketry.api.IAtmosphere) f.get(tile);
                        info.put("detectorMode", mode == null ? "null" : mode.getUnlocalizedName());
                    } catch (ReflectiveOperationException ignored) {
                        info.put("detectorMode", "reflect-failed");
                    }
                }
            }
            send(sender, jsonMap(info));
            return;
        }
        if ("cached-for-player".equalsIgnoreCase(args[0])) {
            // TASK-10b — read AtmosphereHandler.prevAtmosphere via reflection
            // so tests can assert dim-change cache invalidation. The map
            // is private static HashMap<EntityPlayer, IAtmosphere>, keyed
            // by reference; we report the current cached IAtmosphere
            // (or null) for the first connected player.
            java.util.List<net.minecraft.entity.player.EntityPlayerMP> ps =
                    server.getPlayerList().getPlayers();
            if (ps.isEmpty()) {
                send(sender, "{\"error\":\"no players connected\"}");
                return;
            }
            net.minecraft.entity.player.EntityPlayerMP player = ps.get(0);
            try {
                java.lang.reflect.Field f =
                        zmaster587.advancedRocketry.atmosphere.AtmosphereHandler
                                .class.getDeclaredField("prevAtmosphere");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.HashMap<net.minecraft.entity.player.EntityPlayer,
                        zmaster587.advancedRocketry.api.IAtmosphere> map =
                        (java.util.HashMap<net.minecraft.entity.player.EntityPlayer,
                                zmaster587.advancedRocketry.api.IAtmosphere>) f.get(null);
                zmaster587.advancedRocketry.api.IAtmosphere cached = map.get(player);
                send(sender, "{\"ok\":true,\"player\":\""
                        + escapeJson(player.getName()) + "\""
                        + ",\"hasCachedAtmosphere\":" + (cached != null)
                        + ",\"cachedAtmosphere\":\""
                        + escapeJson(cached == null ? "" : cached.getUnlocalizedName())
                        + "\"}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"could not read prevAtmosphere: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage())
                        + "\"}");
            }
            return;
        }
        if (args.length >= 5 && "detector-force-sample".equalsIgnoreCase(args[0])) {
            // Bypasses TileAtmosphereDetector.update()'s
            // world.getWorldTime() % 10 == 0 gate so headless tests don't
            // depend on the server's world-time being a multiple of 10 at the
            // moment the command runs. Runs the same sample loop + setState
            // call as production.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof zmaster587.advancedRocketry.block.BlockRedstoneEmitter)) {
                send(sender, "{\"error\":\"block not BlockRedstoneEmitter\",\"block\":\""
                        + (state.getBlock().getRegistryName() == null ? "null" : state.getBlock().getRegistryName().toString())
                        + "\"}");
                return;
            }
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof zmaster587.advancedRocketry.tile.atmosphere.TileAtmosphereDetector)) {
                send(sender, "{\"error\":\"tile not TileAtmosphereDetector\"}");
                return;
            }
            zmaster587.advancedRocketry.api.IAtmosphere mode;
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.tile.atmosphere
                        .TileAtmosphereDetector.class.getDeclaredField("atmosphereToDetect");
                f.setAccessible(true);
                mode = (zmaster587.advancedRocketry.api.IAtmosphere) f.get(tile);
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            zmaster587.advancedRocketry.atmosphere.AtmosphereHandler atmh =
                    zmaster587.advancedRocketry.atmosphere.AtmosphereHandler.getOxygenHandler(dim);
            boolean detected;
            if (atmh == null) {
                detected = mode == zmaster587.advancedRocketry.atmosphere.AtmosphereType.AIR;
            } else {
                detected = false;
                for (net.minecraft.util.EnumFacing dir : net.minecraft.util.EnumFacing.values()) {
                    if (!world.getBlockState(pos.offset(dir)).isOpaqueCube()
                            && mode == atmh.getAtmosphereType(pos.offset(dir))) {
                        detected = true;
                        break;
                    }
                }
            }
            zmaster587.advancedRocketry.block.BlockRedstoneEmitter emitter =
                    (zmaster587.advancedRocketry.block.BlockRedstoneEmitter) state.getBlock();
            boolean was = emitter.getState(world, state, pos);
            if (was != detected) {
                emitter.setState(world, state, pos, detected);
            }
            send(sender, "{\"ok\":true,\"detected\":" + detected
                    + ",\"wasPowered\":" + was
                    + ",\"isNowPowered\":" + detected + "}");
            return;
        }
        if (args.length >= 6 && "detector-set-mode".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            String atmName = args[5];
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.atmosphere.TileAtmosphereDetector)) {
                send(sender, "{\"error\":\"tile not TileAtmosphereDetector\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            zmaster587.advancedRocketry.api.IAtmosphere target =
                    zmaster587.advancedRocketry.api.atmosphere.AtmosphereRegister.getInstance().getAtmosphere(atmName);
            if (target == null) {
                send(sender, "{\"error\":\"unknown atmosphere name\",\"name\":\""
                        + escapeJson(atmName) + "\"}");
                return;
            }
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.tile.atmosphere
                        .TileAtmosphereDetector.class.getDeclaredField("atmosphereToDetect");
                f.setAccessible(true);
                f.set(tile, target);
                tile.markDirty();
                send(sender, "{\"ok\":true,\"detectorMode\":\"" + escapeJson(atmName) + "\"}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if (args.length >= 5 && "extinguish-at".equalsIgnoreCase(args[0])) {
            // Drives AtmosphereBlob.runEffectOnWorldBlocks's per-block branch
            // (vanilla TORCH → blockUnlitTorch; torchBlocks-listed block →
            // dropped as item + cleared to air) for a SINGLE position. Bypasses
            // the blob/flood-fill so tests can verify the conversion logic
            // deterministically without constructing a non-combustion dim.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            net.minecraft.block.Block before = state.getBlock();
            String action = "unchanged";
            if (before == net.minecraft.init.Blocks.TORCH) {
                world.setBlockState(pos, zmaster587.advancedRocketry.api.AdvancedRocketryBlocks.blockUnlitTorch
                        .getDefaultState().withProperty(net.minecraft.block.BlockTorch.FACING,
                                state.getValue(net.minecraft.block.BlockTorch.FACING)));
                action = "extinguished";
            } else if (zmaster587.advancedRocketry.api.ARConfiguration.getCurrentConfig().torchBlocks.contains(before)) {
                net.minecraft.entity.item.EntityItem item = new net.minecraft.entity.item.EntityItem(
                        world, x, y, z, new net.minecraft.item.ItemStack(before));
                world.setBlockToAir(pos);
                world.spawnEntity(item);
                action = "dropped";
            }
            IBlockState after = world.getBlockState(pos);
            net.minecraft.util.ResourceLocation beforeRn = before.getRegistryName();
            net.minecraft.util.ResourceLocation afterRn = after.getBlock().getRegistryName();
            send(sender, "{\"ok\":true,\"action\":\"" + action + "\","
                    + "\"before\":\"" + escapeJson(beforeRn == null ? "null" : beforeRn.toString()) + "\","
                    + "\"after\":\"" + escapeJson(afterRn == null ? "null" : afterRn.toString()) + "\"}");
            return;
        }
        if (args.length >= 2 && "torch-block-add".equalsIgnoreCase(args[0])) {
            String blockId = args[1];
            net.minecraft.block.Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
            if (block == null) {
                send(sender, "{\"error\":\"unknown block id\",\"id\":\"" + escapeJson(blockId) + "\"}");
                return;
            }
            java.util.LinkedList<net.minecraft.block.Block> list =
                    zmaster587.advancedRocketry.api.ARConfiguration.getCurrentConfig().torchBlocks;
            boolean alreadyPresent = list.contains(block);
            if (!alreadyPresent) list.add(block);
            send(sender, "{\"ok\":true,\"added\":" + (!alreadyPresent)
                    + ",\"size\":" + list.size() + "}");
            return;
        }
        if (args.length >= 1 && "torch-block-clear".equalsIgnoreCase(args[0])) {
            java.util.LinkedList<net.minecraft.block.Block> list =
                    zmaster587.advancedRocketry.api.ARConfiguration.getCurrentConfig().torchBlocks;
            int n = list.size();
            list.clear();
            send(sender, "{\"ok\":true,\"cleared\":" + n + "}");
            return;
        }
        send(sender, "{\"error\":\"unknown atmosphere subcommand — try get <dim> <x> <y> <z> | set-density <dim> <value> | detector-output <dim> <x> <y> <z> | detector-set-mode <dim> <x> <y> <z> <atmName> | extinguish-at <dim> <x> <y> <z> | torch-block-add <blockId> | torch-block-clear\"}");
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
        if (args.length >= 5 && "controller-state".equalsIgnoreCase(args[0])) {
            // controller-state <dim> <x> <y> <z> — reflective dump of libVulpes
            // multiblock controller internals: aggregated battery energy and
            // fluidInPorts count. Used by TASK-19 powered-cycle tests to
            // verify that integrateTile() actually wired up the structure's
            // P/L hatches (separate from whether `artest energy inject` /
            // `artest fluid inject` lands on the individual hatch tiles).
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0), y = parseIntOr(args[3], 0), z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (tile == null) {
                send(sender, "{\"error\":\"no tile entity\"}");
                return;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("tileClass", tile.getClass().getName());
            // Walk class hierarchy for the libVulpes TileMultiBlock fields.
            try {
                java.lang.reflect.Field bat = findFieldOrNull(tile.getClass(), "batteries");
                if (bat != null) {
                    bat.setAccessible(true);
                    Object multiBattery = bat.get(tile);
                    info.put("batteriesPresent", multiBattery != null);
                    if (multiBattery != null) {
                        java.lang.reflect.Method getStored = multiBattery.getClass()
                                .getMethod("getUniversalEnergyStored");
                        info.put("batteriesStored", getStored.invoke(multiBattery));
                        java.lang.reflect.Method getMax = multiBattery.getClass()
                                .getMethod("getMaxEnergyStored");
                        info.put("batteriesMax", getMax.invoke(multiBattery));
                        // Read internal LinkedList size to detect "empty aggregator"
                        // (i.e. integrateTile never added the P plugs).
                        java.lang.reflect.Field listField = findFieldOrNull(multiBattery.getClass(), "batteries");
                        if (listField != null) {
                            listField.setAccessible(true);
                            Object list = listField.get(multiBattery);
                            if (list instanceof java.util.Collection<?>) {
                                info.put("batteriesCount", ((java.util.Collection<?>) list).size());
                            }
                        }
                    }
                }
                java.lang.reflect.Field fluidIn = findFieldOrNull(tile.getClass(), "fluidInPorts");
                if (fluidIn != null) {
                    fluidIn.setAccessible(true);
                    Object portsList = fluidIn.get(tile);
                    if (portsList instanceof java.util.Collection<?>) {
                        info.put("fluidInPortsCount", ((java.util.Collection<?>) portsList).size());
                    }
                }
                java.lang.reflect.Field currentTime = findFieldOrNull(tile.getClass(), "currentTime");
                if (currentTime != null) {
                    currentTime.setAccessible(true);
                    info.put("currentTime", currentTime.get(tile));
                }
                java.lang.reflect.Field oof = findFieldOrNull(tile.getClass(), "outOfFluid");
                if (oof != null) {
                    oof.setAccessible(true);
                    info.put("outOfFluid", oof.get(tile));
                }
            } catch (ReflectiveOperationException e) {
                info.put("reflectionError",
                        e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            send(sender, jsonMap(info));
            return;
        }
        if (args.length >= 5 && "clear-batteries".equalsIgnoreCase(args[0])) {
            // clear-batteries <dim> <x> <y> <z> — empties the libVulpes
            // MultiBattery aggregator on the controller via reflection.
            // Used by TASK-19 counter-tests to disable the "infinite power"
            // that creative input plugs (default mapping for 'P') provide.
            // Plugs stay placed; only the controller-side aggregator is
            // cleared, so hasEnergy() returns false on subsequent ticks.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0), y = parseIntOr(args[3], 0), z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (tile == null) {
                send(sender, "{\"error\":\"no tile entity\"}");
                return;
            }
            try {
                java.lang.reflect.Field bat = findFieldOrNull(tile.getClass(), "batteries");
                if (bat == null) {
                    send(sender, "{\"error\":\"tile has no batteries field\"}");
                    return;
                }
                bat.setAccessible(true);
                Object multiBattery = bat.get(tile);
                if (multiBattery == null) {
                    send(sender, "{\"error\":\"batteries field is null\"}");
                    return;
                }
                java.lang.reflect.Method clear = multiBattery.getClass().getMethod("clear");
                clear.invoke(multiBattery);
                send(sender, "{\"ok\":true,\"cleared\":true}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if (args.length >= 6 && "set-enabled".equalsIgnoreCase(args[0])) {
            // set-enabled <dim> <x> <y> <z> <true|false>
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0), y = parseIntOr(args[3], 0), z = parseIntOr(args[4], 0);
            boolean value = Boolean.parseBoolean(args[5]);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (tile == null) {
                send(sender, "{\"error\":\"no tile entity\"}");
                return;
            }
            try {
                java.lang.reflect.Method m = tile.getClass().getMethod("setMachineEnabled", boolean.class);
                m.invoke(tile, value);
                java.lang.reflect.Method ge = tile.getClass().getMethod("getMachineEnabled");
                boolean readBack = (Boolean) ge.invoke(tile);
                send(sender, "{\"ok\":true,\"enabled\":" + readBack + "}");
            } catch (NoSuchMethodException e) {
                send(sender, "{\"error\":\"tile lacks setMachineEnabled\"}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if (args.length >= 5 && "try-complete".equalsIgnoreCase(args[0])) {
            // try-complete <dim> <x> <y> <z> — invoke libVulpes' attemptCompleteStructure
            // on the controller tile to trigger validation without needing a player +
            // hammer interaction.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0), y = parseIntOr(args[3], 0), z = parseIntOr(args[4], 0);
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
            try {
                java.lang.reflect.Method m = tile.getClass().getMethod(
                        "attemptCompleteStructure", net.minecraft.block.state.IBlockState.class);
                Object result = m.invoke(tile, world.getBlockState(pos));
                boolean attempted = result instanceof Boolean && (Boolean) result;
                // Re-read isComplete after the attempt.
                java.lang.reflect.Method ic = tile.getClass().getMethod("isComplete");
                boolean isComplete = (Boolean) ic.invoke(tile);
                send(sender, "{\"ok\":true,\"attempted\":" + attempted
                        + ",\"isComplete\":" + isComplete
                        + ",\"tileClass\":\"" + tile.getClass().getName() + "\"}");
            } catch (NoSuchMethodException e) {
                send(sender, "{\"error\":\"tile lacks attemptCompleteStructure/isComplete — not a libVulpes multiblock\",\"tileClass\":\""
                        + tile.getClass().getName() + "\"}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if (args.length >= 2 && "recipe-info".equalsIgnoreCase(args[0])) {
            // recipe-info <machineShortClassName> [recipeIndex]
            String shortName = args[1];
            int recipeIndex = args.length >= 3 ? parseIntOr(args[2], 0) : 0;
            try {
                Class<?> recipesMachineClass = Class.forName("zmaster587.libVulpes.recipe.RecipesMachine");
                Object instance = recipesMachineClass.getMethod("getInstance").invoke(null);
                java.lang.reflect.Method getRecipes = recipesMachineClass.getMethod("getRecipes", Class.class);
                Class<?> machineClass = Class.forName(
                        "zmaster587.advancedRocketry.tile.multiblock.machine." + shortName);
                java.util.List<?> recipes = (java.util.List<?>) getRecipes.invoke(instance, machineClass);
                if (recipes == null || recipes.isEmpty()) {
                    send(sender, "{\"error\":\"no recipes registered\",\"machine\":\""
                            + escapeJson(shortName) + "\"}");
                    return;
                }
                if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
                    send(sender, "{\"error\":\"recipeIndex out of range\",\"index\":" + recipeIndex
                            + ",\"size\":" + recipes.size() + "}");
                    return;
                }
                Object recipe = recipes.get(recipeIndex);
                Class<?> recipeClass = recipe.getClass();

                java.util.List<?> ingredients = (java.util.List<?>) recipeClass.getMethod("getIngredients").invoke(recipe);
                java.util.List<?> outputs = (java.util.List<?>) recipeClass.getMethod("getOutput").invoke(recipe);
                int time = (Integer) recipeClass.getMethod("getTime").invoke(recipe);
                int power = (Integer) recipeClass.getMethod("getPower").invoke(recipe);
                java.util.List<?> fluidIngredients;
                java.util.List<?> fluidOutputs;
                try {
                    fluidIngredients = (java.util.List<?>) recipeClass.getMethod("getFluidIngredients").invoke(recipe);
                } catch (NoSuchMethodException ne) {
                    fluidIngredients = java.util.Collections.emptyList();
                }
                try {
                    fluidOutputs = (java.util.List<?>) recipeClass.getMethod("getFluidOutputs").invoke(recipe);
                } catch (NoSuchMethodException ne) {
                    fluidOutputs = java.util.Collections.emptyList();
                }

                StringBuilder builder = new StringBuilder("{\"machine\":\"")
                        .append(escapeJson(shortName))
                        .append("\",\"recipeIndex\":").append(recipeIndex)
                        .append(",\"totalRecipes\":").append(recipes.size())
                        .append(",\"time\":").append(time)
                        .append(",\"power\":").append(power);

                // Each ingredient is a List<ItemStack> (oredict alternatives) — emit the first.
                builder.append(",\"ingredients\":[");
                for (int i = 0; i < ingredients.size(); i++) {
                    Object slot = ingredients.get(i);
                    if (!(slot instanceof java.util.List)) continue;
                    java.util.List<?> alts = (java.util.List<?>) slot;
                    if (alts.isEmpty()) continue;
                    Object first = alts.get(0);
                    if (!(first instanceof net.minecraft.item.ItemStack)) continue;
                    net.minecraft.item.ItemStack stack = (net.minecraft.item.ItemStack) first;
                    if (i > 0) builder.append(',');
                    appendItemStackJson(builder, stack, i);
                }
                builder.append("],\"outputs\":[");
                for (int i = 0; i < outputs.size(); i++) {
                    Object out = outputs.get(i);
                    if (!(out instanceof net.minecraft.item.ItemStack)) continue;
                    net.minecraft.item.ItemStack stack = (net.minecraft.item.ItemStack) out;
                    if (i > 0) builder.append(',');
                    appendItemStackJson(builder, stack, i);
                }
                builder.append("],\"fluidIngredients\":[");
                {
                    int emitted = 0;
                    for (Object fObj : fluidIngredients) {
                        if (!(fObj instanceof net.minecraftforge.fluids.FluidStack)) continue;
                        net.minecraftforge.fluids.FluidStack fs = (net.minecraftforge.fluids.FluidStack) fObj;
                        if (emitted > 0) builder.append(',');
                        builder.append("{\"fluid\":\"")
                                .append(escapeJson(fs.getFluid().getName()))
                                .append("\",\"amount\":").append(fs.amount).append('}');
                        emitted++;
                    }
                }
                builder.append("],\"fluidOutputs\":[");
                {
                    int emitted = 0;
                    for (Object fObj : fluidOutputs) {
                        if (!(fObj instanceof net.minecraftforge.fluids.FluidStack)) continue;
                        net.minecraftforge.fluids.FluidStack fs = (net.minecraftforge.fluids.FluidStack) fObj;
                        if (emitted > 0) builder.append(',');
                        builder.append("{\"fluid\":\"")
                                .append(escapeJson(fs.getFluid().getName()))
                                .append("\",\"amount\":").append(fs.amount).append('}');
                        emitted++;
                    }
                }
                builder.append("]}");
                send(sender, builder.toString());
            } catch (ClassNotFoundException missing) {
                send(sender, "{\"error\":\"machine class not found\",\"name\":\""
                        + escapeJson(shortName) + "\"}");
            } catch (ReflectiveOperationException re) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(re.getMessage()) + "\"}");
            }
            return;
        }
        // TASK-25 — same shape as `recipe-info` above but takes an arbitrary
        // class FQN. Used by classes outside `tile.multiblock.machine.*`
        // (notably `BlockSmallPlatePress`, whose recipes are registered
        // against its block class).
        if (args.length >= 2 && "recipe-info-block".equalsIgnoreCase(args[0])) {
            String fqn = args[1];
            int recipeIndex = args.length >= 3 ? parseIntOr(args[2], 0) : 0;
            try {
                Class<?> recipesMachineClass = Class.forName("zmaster587.libVulpes.recipe.RecipesMachine");
                Object instance = recipesMachineClass.getMethod("getInstance").invoke(null);
                java.lang.reflect.Method getRecipes = recipesMachineClass.getMethod("getRecipes", Class.class);
                Class<?> machineClass = Class.forName(fqn);
                java.util.List<?> recipes = (java.util.List<?>) getRecipes.invoke(instance, machineClass);
                if (recipes == null || recipes.isEmpty()) {
                    send(sender, "{\"error\":\"no recipes registered\",\"class\":\""
                            + escapeJson(fqn) + "\"}");
                    return;
                }
                if (recipeIndex < 0 || recipeIndex >= recipes.size()) {
                    send(sender, "{\"error\":\"recipeIndex out of range\",\"index\":" + recipeIndex
                            + ",\"size\":" + recipes.size() + "}");
                    return;
                }
                Object recipe = recipes.get(recipeIndex);
                Class<?> recipeClass = recipe.getClass();
                java.util.List<?> ingredients = (java.util.List<?>) recipeClass.getMethod("getIngredients").invoke(recipe);
                java.util.List<?> outputs = (java.util.List<?>) recipeClass.getMethod("getOutput").invoke(recipe);
                int time = (Integer) recipeClass.getMethod("getTime").invoke(recipe);
                int power = (Integer) recipeClass.getMethod("getPower").invoke(recipe);

                StringBuilder builder = new StringBuilder("{\"class\":\"")
                        .append(escapeJson(fqn))
                        .append("\",\"recipeIndex\":").append(recipeIndex)
                        .append(",\"totalRecipes\":").append(recipes.size())
                        .append(",\"time\":").append(time)
                        .append(",\"power\":").append(power)
                        .append(",\"ingredients\":[");
                for (int i = 0; i < ingredients.size(); i++) {
                    Object slot = ingredients.get(i);
                    if (!(slot instanceof java.util.List)) continue;
                    java.util.List<?> alts = (java.util.List<?>) slot;
                    if (alts.isEmpty()) continue;
                    Object first = alts.get(0);
                    if (!(first instanceof net.minecraft.item.ItemStack)) continue;
                    net.minecraft.item.ItemStack stack = (net.minecraft.item.ItemStack) first;
                    if (i > 0) builder.append(',');
                    appendItemStackJson(builder, stack, i);
                }
                builder.append("],\"outputs\":[");
                for (int i = 0; i < outputs.size(); i++) {
                    Object out = outputs.get(i);
                    if (!(out instanceof net.minecraft.item.ItemStack)) continue;
                    net.minecraft.item.ItemStack stack = (net.minecraft.item.ItemStack) out;
                    if (i > 0) builder.append(',');
                    appendItemStackJson(builder, stack, i);
                }
                builder.append("]}");
                send(sender, builder.toString());
            } catch (ClassNotFoundException missing) {
                send(sender, "{\"error\":\"class not found\",\"fqn\":\""
                        + escapeJson(fqn) + "\"}");
            } catch (ReflectiveOperationException re) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(re.getMessage()) + "\"}");
            }
            return;
        }
        if (args.length >= 1 && "recipes-summary".equalsIgnoreCase(args[0])) {
            // Report recipe counts for every canonical AR multiblock recipe machine
            // (SMART §7.7). Uses libVulpes' RecipesMachine singleton.
            String[] machines = {
                    "zmaster587.advancedRocketry.tile.multiblock.machine.TileCuttingMachine",
                    "zmaster587.advancedRocketry.tile.multiblock.machine.TilePrecisionAssembler",
                    "zmaster587.advancedRocketry.tile.multiblock.machine.TileChemicalReactor",
                    "zmaster587.advancedRocketry.tile.multiblock.machine.TileCrystallizer",
                    "zmaster587.advancedRocketry.tile.multiblock.machine.TileElectrolyser",
                    "zmaster587.advancedRocketry.tile.multiblock.machine.TileElectricArcFurnace",
                    "zmaster587.advancedRocketry.tile.multiblock.machine.TileLathe",
                    "zmaster587.advancedRocketry.tile.multiblock.machine.TileRollingMachine",
                    "zmaster587.advancedRocketry.tile.multiblock.machine.TileCentrifuge",
                    "zmaster587.advancedRocketry.tile.multiblock.machine.TilePrecisionLaserEtcher",
            };
            Map<String, Object> recipes = new LinkedHashMap<>();
            try {
                Class<?> recipesMachineClass = Class.forName("zmaster587.libVulpes.recipe.RecipesMachine");
                Object instance = recipesMachineClass.getMethod("getInstance").invoke(null);
                java.lang.reflect.Method getRecipes = recipesMachineClass.getMethod("getRecipes", Class.class);
                for (String fqn : machines) {
                    String shortName = fqn.substring(fqn.lastIndexOf('.') + 1);
                    try {
                        Class<?> machineClass = Class.forName(fqn);
                        Object listObj = getRecipes.invoke(instance, machineClass);
                        java.util.List<?> list = (java.util.List<?>) listObj;
                        recipes.put(shortName, list == null ? 0 : list.size());
                    } catch (ClassNotFoundException missing) {
                        recipes.put(shortName, "class-missing");
                    } catch (ReflectiveOperationException re) {
                        recipes.put(shortName, "error:" + re.getClass().getSimpleName());
                    }
                }
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"RecipesMachine reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            send(sender, jsonMap(recipes));
            return;
        }
        send(sender, "{\"error\":\"unknown machine subcommand — try info [dim] <x> <y> <z> | try-complete <dim> <x> <y> <z> | recipes-summary\"}");
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
     *   <li>{@code building} / {@code not-building} — {@code isBuilding()} state
     *       (TileRocketAssemblingMachine uses this instead of isRunning)</li>
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
                    case "building": {
                        Object v = tile.getClass().getMethod("isBuilding").invoke(tile);
                        lastSeen = v;
                        if (Boolean.TRUE.equals(v)) {
                            send(sender, "{\"matched\":true,\"ticks\":" + tick + ",\"condition\":\"building\"}");
                            return;
                        }
                        break;
                    }
                    case "not-building": {
                        Object v = tile.getClass().getMethod("isBuilding").invoke(tile);
                        lastSeen = v;
                        if (Boolean.FALSE.equals(v)) {
                            send(sender, "{\"matched\":true,\"ticks\":" + tick + ",\"condition\":\"not-building\"}");
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

    // §5.7b ARConfiguration set/get probe (TASK-19 Phase 1b) ----------------

    /**
     * Whitelist of mutable {@link zmaster587.advancedRocketry.api.ARConfiguration}
     * fields exposed via {@code /artest config set}. Keep it tight — this
     * verb writes directly to the live config instance and would otherwise
     * be a generic test-pollution vector. Each entry lists the field that
     * tests currently need to flip:
     *
     * <ul>
     *   <li>{@code allowTerraformNonAR} — TASK-19 Phase 1b, exercise the
     *       non-AR-planet branch of
     *       {@code TileAtmosphereTerraformer.processComplete}.</li>
     *   <li>{@code terraformRequiresFluid} — reserved for future
     *       fluid-bypass tests; not currently used.</li>
     * </ul>
     *
     * <p>Tests MUST restore the original value in {@code @After}, otherwise
     * subsequent tests on the shared harness inherit the flipped state.</p>
     */
    private static final java.util.Set<String> CONFIG_WHITELIST =
            new java.util.LinkedHashSet<>(java.util.Arrays.asList(
                    "allowTerraformNonAR",
                    "terraformRequiresFluid"));

    private void handleConfig(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            send(sender, "{\"error\":\"missing subcommand — try get <key> | set <key> <value>\","
                    + "\"whitelist\":" + jsonStringArray(CONFIG_WHITELIST) + "}");
            return;
        }
        if ("get".equalsIgnoreCase(args[0]) && args.length >= 2) {
            String key = args[1];
            if (!CONFIG_WHITELIST.contains(key)) {
                send(sender, "{\"error\":\"key not in whitelist\",\"key\":\""
                        + escapeJson(key) + "\",\"whitelist\":"
                        + jsonStringArray(CONFIG_WHITELIST) + "}");
                return;
            }
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.api.ARConfiguration.class
                        .getField(key);
                Object value = f.get(zmaster587.advancedRocketry.api.ARConfiguration.getCurrentConfig());
                send(sender, "{\"ok\":true,\"key\":\"" + escapeJson(key)
                        + "\",\"value\":" + jsonValue(value) + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if ("set".equalsIgnoreCase(args[0]) && args.length >= 3) {
            String key = args[1];
            String rawValue = args[2];
            if (!CONFIG_WHITELIST.contains(key)) {
                send(sender, "{\"error\":\"key not in whitelist\",\"key\":\""
                        + escapeJson(key) + "\",\"whitelist\":"
                        + jsonStringArray(CONFIG_WHITELIST) + "}");
                return;
            }
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.api.ARConfiguration.class
                        .getField(key);
                Object cfg = zmaster587.advancedRocketry.api.ARConfiguration.getCurrentConfig();
                Object oldValue = f.get(cfg);
                Object newValue = parseConfigValue(f.getType(), rawValue);
                if (newValue == null) {
                    send(sender, "{\"error\":\"unsupported field type\",\"type\":\""
                            + f.getType().getName() + "\"}");
                    return;
                }
                f.set(cfg, newValue);
                send(sender, "{\"ok\":true,\"key\":\"" + escapeJson(key)
                        + "\",\"oldValue\":" + jsonValue(oldValue)
                        + ",\"newValue\":" + jsonValue(newValue) + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        send(sender, "{\"error\":\"unknown subcommand — try get <key> | set <key> <value>\"}");
    }

    private static Object parseConfigValue(Class<?> type, String raw) {
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(raw);
        if (type == int.class || type == Integer.class) {
            try { return Integer.parseInt(raw); } catch (NumberFormatException e) { return null; }
        }
        if (type == double.class || type == Double.class) {
            try { return Double.parseDouble(raw); } catch (NumberFormatException e) { return null; }
        }
        if (type == float.class || type == Float.class) {
            try { return Float.parseFloat(raw); } catch (NumberFormatException e) { return null; }
        }
        if (type == String.class) return raw;
        return null;
    }

    private static String jsonValue(Object v) {
        if (v == null) return "null";
        if (v instanceof Boolean || v instanceof Number) return v.toString();
        return "\"" + escapeJson(v.toString()) + "\"";
    }

    private static String jsonStringArray(java.util.Collection<String> items) {
        StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (String s : items) {
            if (i++ > 0) sb.append(',');
            sb.append('"').append(escapeJson(s)).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    // §5.7c Star (StellarBody) probe (TASK-19 Phase 2) ----------------------

    /**
     * {@code /artest star <get|set-blackhole> <starId> [value]} — reads or
     * mutates a {@link zmaster587.advancedRocketry.api.dimension.solar.StellarBody}'s
     * black-hole flag via reflection. Used by TASK-19 Phase 2 to flip the
     * default Sol star (id 0) into a black hole so a station orbiting it
     * satisfies {@code TileBlackHoleGenerator.isAroundBlackHole()}.
     *
     * <p>Tests MUST restore the original flag in {@code @After} — otherwise
     * subsequent methods on the shared harness inherit a black-hole Sol
     * which corrupts unrelated sky-render and orbital-mechanics paths.</p>
     */
    private void handleStar(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            send(sender, "{\"error\":\"missing subcommand — try get <starId> | set-blackhole <starId> <true|false>\"}");
            return;
        }
        if ("get".equalsIgnoreCase(args[0]) && args.length >= 2) {
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            zmaster587.advancedRocketry.api.dimension.solar.StellarBody star =
                    DimensionManager.getInstance().getStar(id);
            if (star == null) {
                send(sender, "{\"error\":\"star not found\",\"id\":" + id + "}");
                return;
            }
            send(sender, "{\"ok\":true,\"id\":" + id
                    + ",\"isBlackHole\":" + star.isBlackHole()
                    + ",\"name\":\"" + escapeJson(String.valueOf(star.getName())) + "\"}");
            return;
        }
        if ("set-blackhole".equalsIgnoreCase(args[0]) && args.length >= 3) {
            int id = parseIntOr(args[1], Integer.MIN_VALUE);
            boolean value = Boolean.parseBoolean(args[2]);
            zmaster587.advancedRocketry.api.dimension.solar.StellarBody star =
                    DimensionManager.getInstance().getStar(id);
            if (star == null) {
                send(sender, "{\"error\":\"star not found\",\"id\":" + id + "}");
                return;
            }
            boolean before = star.isBlackHole();
            star.setBlackHole(value);
            send(sender, "{\"ok\":true,\"id\":" + id
                    + ",\"before\":" + before + ",\"after\":" + star.isBlackHole() + "}");
            return;
        }
        send(sender, "{\"error\":\"unknown subcommand — try get <starId> | set-blackhole <starId> <true|false>\"}");
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
        if (args.length >= 3 && "set-density".equalsIgnoreCase(args[0])) {
            // terraforming set-density <dim> <newDensity>
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int density = parseIntOr(args[2], -1);
            DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
                return;
            }
            int before = props.getAtmosphereDensity();
            props.setAtmosphereDensity(density);
            send(sender, "{\"ok\":true,\"dim\":" + dim
                    + ",\"oldDensity\":" + before
                    + ",\"newDensity\":" + props.getAtmosphereDensity() + "}");
            return;
        }
        send(sender, "{\"error\":\"unknown terraforming subcommand — try info <dim> | set-density <dim> <value>\"}");
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
            // getChunk(int, int) force-loads + populates if needed. Under
            // parallel-fork pressure the populate step occasionally lags so
            // adjacent-chunk decorations (trees, ores) haven't run yet,
            // collapsing the (topY, biome) signature of spaced chunks —
            // TASK-28 F7 (worldgen sampling race, promoted from TASK-16
            // shape #4 after 3 sightings). Poll up to 1 s for
            // {@code isTerrainPopulated()} before sampling, also pre-load
            // neighbour chunks so cross-chunk decorations finalize on this
            // chunk's column.
            ensureChunkAreaLoaded(world, (chunkX << 4) + 8, (chunkZ << 4) + 8, 1);
            Chunk chunk = world.getChunkProvider().provideChunk(chunkX, chunkZ);
            if (chunk == null || !chunk.isLoaded()) {
                send(sender, "{\"error\":\"chunk failed to load\",\"chunk\":[" + chunkX + "," + chunkZ + "]}");
                return;
            }
            for (int attempt = 0; attempt < 20 && !chunk.isTerrainPopulated(); attempt++) {
                try { Thread.sleep(50L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                chunk = world.getChunkProvider().provideChunk(chunkX, chunkZ);
                if (chunk == null) break;
            }
            if (chunk == null) {
                send(sender, "{\"error\":\"chunk became null during populate wait\",\"chunk\":[" + chunkX + "," + chunkZ + "]}");
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
        if (args.length >= 6 && "ore-stats".equalsIgnoreCase(args[0])) {
            // ore-stats <dim> <chunkX> <chunkZ> <radiusChunks> <blockId>
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int centerCX = parseIntOr(args[2], 0);
            int centerCZ = parseIntOr(args[3], 0);
            int radius = parseIntOr(args[4], 1);
            String blockId = args[5];
            WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            net.minecraft.block.Block target = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
            // Forge's GameRegistry returns AIR as the default for any missing
            // registry key (instead of null). Detect the fallback explicitly
            // so callers that supplied "foo:bar_typo" get a real error
            // rather than a 424k count of air blocks.
            boolean isAirRequested = blockId.equalsIgnoreCase("minecraft:air");
            if (target == null || (target == net.minecraft.init.Blocks.AIR && !isAirRequested)) {
                send(sender, "{\"error\":\"unknown block id\",\"id\":\"" + escapeJson(blockId) + "\"}");
                return;
            }
            // Soft cap: (2r+1)^2 chunks, 16x16x256 blocks each → (2r+1)^2 * 65536
            // Refuse r > 4 (9x9 = 81 chunks ~5.3M blocks scan).
            if (radius > 4) {
                send(sender, "{\"error\":\"radius too large\",\"radius\":" + radius + ",\"cap\":4}");
                return;
            }
            int chunksScanned = 0;
            long count = 0;
            for (int cx = centerCX - radius; cx <= centerCX + radius; cx++) {
                for (int cz = centerCZ - radius; cz <= centerCZ + radius; cz++) {
                    Chunk chunk = world.getChunkProvider().provideChunk(cx, cz);
                    if (chunk == null || !chunk.isLoaded()) continue;
                    chunksScanned++;
                    for (int y = 0; y < 256; y++) {
                        for (int lx = 0; lx < 16; lx++) {
                            for (int lz = 0; lz < 16; lz++) {
                                if (chunk.getBlockState(lx, y, lz).getBlock() == target) count++;
                            }
                        }
                    }
                }
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("dim", dim);
            info.put("centerChunk", new int[]{centerCX, centerCZ});
            info.put("radius", radius);
            info.put("block", blockId);
            info.put("chunksScanned", chunksScanned);
            info.put("count", count);
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown worldgen subcommand — try sample <dim> <chunkX> <chunkZ> | ore-stats <dim> <cx> <cz> <radius> <blockId>\"}");
    }

    // Inventory hatch probe ----------------------------------------------------

    /**
     * {@code /artest hatch fill <dim> <x> <y> <z> <slot> <itemId> [count] [meta]}
     * — sets a stack into an {@link net.minecraft.inventory.IInventory} slot
     * (typically a libVulpes input hatch).
     *
     * {@code /artest hatch read <dim> <x> <y> <z> [nbt]} — dumps every
     * non-empty slot as {@code {"slot":N,"item":"<id>","count":K,"meta":M}}.
     * Pass the literal {@code nbt} as the 6th arg to additionally include
     * {@code "nbt":"<Mojangson dump>"} per slot (the stack's
     * {@code getTagCompound().toString()}, JSON-escaped, or empty string
     * when the stack has no tag).
     */
    private void handleHatch(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 6 && "fill".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            int slot = parseIntOr(args[5], 0);
            String itemId = args.length >= 7 ? args[6] : "minecraft:stick";
            int count = args.length >= 8 ? parseIntOr(args[7], 1) : 1;
            int meta = args.length >= 9 ? parseIntOr(args[8], 0) : 0;
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof net.minecraft.inventory.IInventory)) {
                send(sender, "{\"error\":\"tile not IInventory\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            net.minecraft.item.Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            if (item == null) {
                send(sender, "{\"error\":\"unknown item id\",\"id\":\"" + escapeJson(itemId) + "\"}");
                return;
            }
            net.minecraft.inventory.IInventory inv = (net.minecraft.inventory.IInventory) tile;
            if (slot < 0 || slot >= inv.getSizeInventory()) {
                send(sender, "{\"error\":\"slot out of range\",\"slot\":" + slot
                        + ",\"size\":" + inv.getSizeInventory() + "}");
                return;
            }
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item, count, meta);
            inv.setInventorySlotContents(slot, stack);
            // libVulpes' hatches typically callback the host machine via onInventoryUpdate.
            // setInventorySlotContents alone is enough for the host's next-tick scan.
            send(sender, "{\"ok\":true,\"slot\":" + slot + ",\"item\":\"" + escapeJson(itemId)
                    + "\",\"count\":" + count + "}");
            return;
        }
        if (args.length >= 4 && "read".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof net.minecraft.inventory.IInventory)) {
                send(sender, "{\"error\":\"tile not IInventory\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            net.minecraft.inventory.IInventory inv = (net.minecraft.inventory.IInventory) tile;
            // Optional trailing "nbt" flag → include each slot's stack NBT
            // as a Mojangson string (NBTTagCompound.toString()). Used by
            // tests that need to verify a stack's tag-compound shape (e.g.
            // suit component lists) without adding a per-tile probe verb.
            boolean includeNbt = args.length >= 6
                    && "nbt".equalsIgnoreCase(args[5]);
            StringBuilder builder = new StringBuilder("{\"size\":")
                    .append(inv.getSizeInventory()).append(",\"slots\":[");
            boolean first = true;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                net.minecraft.item.ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (!first) builder.append(',');
                first = false;
                ResourceLocation regName = stack.getItem().getRegistryName();
                builder.append("{\"slot\":").append(i)
                        .append(",\"item\":\"").append(regName == null ? "null" : regName.toString())
                        .append("\",\"count\":").append(stack.getCount())
                        .append(",\"meta\":").append(stack.getMetadata());
                if (includeNbt) {
                    net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
                    builder.append(",\"nbt\":\"")
                            .append(tag == null ? "" : escapeJson(tag.toString()))
                            .append("\"");
                }
                builder.append('}');
            }
            builder.append("]}");
            send(sender, builder.toString());
            return;
        }
        send(sender, "{\"error\":\"unknown hatch subcommand — try fill <dim> <x> <y> <z> <slot> <itemId> [count] [meta] | read <dim> <x> <y> <z> [nbt]\"}");
    }

    // §5 Planet selector probe --------------------------------------------------

    /**
     * <ul>
     *   <li>{@code /artest selector info <dim> <x> <y> <z>} — reads server-side
     *       {@code TilePlanetSelector.dimCache} via reflection. Returns the
     *       cached planet's dim id + name, or {@code hasSelection=false}.</li>
     *   <li>{@code /artest selector simulate-click <dim> <x> <y> <z> <planetDim>}
     *       — sets {@code dimCache} on the tile to the resolved
     *       {@link zmaster587.advancedRocketry.dimension.DimensionProperties}.
     *       Mimics the end-state produced by the {@code PacketMachine} path
     *       (client GUI click → server {@code useNetworkData} → {@code selectSystem})
     *       without needing a client.</li>
     * </ul>
     */
    private void handleSelector(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 5 && "info".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0), y = parseIntOr(args[3], 0), z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.multiblock.TilePlanetSelector)) {
                send(sender, "{\"error\":\"tile not TilePlanetSelector\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.tile.multiblock.TilePlanetSelector
                        .class.getDeclaredField("dimCache");
                f.setAccessible(true);
                Object cached = f.get(tile);
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("tileClass", tile.getClass().getName());
                info.put("hasSelection", cached != null);
                if (cached instanceof DimensionProperties) {
                    DimensionProperties props = (DimensionProperties) cached;
                    info.put("selectedDim", props.getId());
                    info.put("selectedName", props.getName());
                }
                send(sender, jsonMap(info));
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if (args.length >= 6 && "simulate-click".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0), y = parseIntOr(args[3], 0), z = parseIntOr(args[4], 0);
            int planetDim = parseIntOr(args[5], Integer.MIN_VALUE);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.multiblock.TilePlanetSelector)) {
                send(sender, "{\"error\":\"tile not TilePlanetSelector\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            // getDimensionProperties falls back to overworldProperties for unknown
            // dims; use isDimensionCreated for an unambiguous registration check.
            // Special-case: vanilla overworld (0) IS valid even though it's not
            // in AR's dimensionList — production allows selecting it.
            if (planetDim != 0 && !DimensionManager.getInstance().isDimensionCreated(planetDim)) {
                send(sender, "{\"error\":\"planet dim not registered\",\"planetDim\":" + planetDim + "}");
                return;
            }
            DimensionProperties target = DimensionManager.getInstance().getDimensionProperties(planetDim);
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.tile.multiblock.TilePlanetSelector
                        .class.getDeclaredField("dimCache");
                f.setAccessible(true);
                f.set(tile, target);
                tile.markDirty();
                send(sender, "{\"ok\":true,\"planetDim\":" + planetDim
                        + ",\"name\":\"" + escapeJson(target.getName()) + "\"}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        send(sender, "{\"error\":\"unknown selector subcommand — try info <dim> <x> <y> <z> | simulate-click <dim> <x> <y> <z> <planetDim>\"}");
    }

    // Generic tile ticking probe ----------------------------------------------

    /**
     * {@code /artest tile force-tick <dim> <x> <y> <z> <ticks>} — directly invokes
     * {@link net.minecraft.util.ITickable#update()} on a tile entity N times in a
     * row, bypassing the world tick scheduler. Used by tests that need
     * deterministic, synchronous machine progress without waiting for the server
     * thread to schedule a world tick (which it can't during a command since
     * commands themselves run on the server thread).
     */
    private void handleTile(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 6 && "force-tick".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            int ticks = parseIntOr(args[5], 1);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            BlockPos pos = new BlockPos(x, y, z);
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof net.minecraft.util.ITickable)) {
                send(sender, "{\"error\":\"tile not ITickable\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            net.minecraft.util.ITickable tickable = (net.minecraft.util.ITickable) tile;
            int ticked = 0;
            try {
                for (int i = 0; i < ticks; i++) {
                    tickable.update();
                    ticked++;
                }
            } catch (RuntimeException e) {
                send(sender, "{\"error\":\"tile.update() threw after " + ticked + " ticks: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage()) + "\"}");
                return;
            }
            send(sender, "{\"ok\":true,\"ticked\":" + ticked
                    + ",\"tileClass\":\"" + tile.getClass().getName() + "\"}");
            return;
        }
        if (args.length >= 5 && "init-modules".equalsIgnoreCase(args[0])) {
            // /artest tile init-modules <dim> <x> <y> <z>
            // Calls getModules(0, null) on an IModularInventory tile to
            // populate any internal module/slot-array fields that
            // production code lazily initialises in the GUI-open path.
            // E.g. TileSuitWorkStation.slotArray is populated only inside
            // getModules(); its setInventorySlotContents(0, ...) NPEs on
            // a fresh server-side tile that hasn't seen a GUI open.
            // Swallows any NPE from player-using modules (e.g.
            // ModuleSlotArmor with a null player) — by the time those
            // construct, the slot-array fields have already been set.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.libVulpes.inventory.modules.IModularInventory)) {
                send(sender, "{\"error\":\"tile not IModularInventory\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            zmaster587.libVulpes.inventory.modules.IModularInventory imi =
                    (zmaster587.libVulpes.inventory.modules.IModularInventory) tile;
            String swallowed = null;
            try {
                imi.getModules(0, null);
            } catch (RuntimeException e) {
                swallowed = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            send(sender, "{\"ok\":true,\"tileClass\":\"" + tile.getClass().getName() + "\""
                    + (swallowed == null ? ""
                            : ",\"playerModuleSkipped\":\"" + escapeJson(swallowed) + "\"")
                    + "}");
            return;
        }
        if (args.length >= 5 && "warp-state".equalsIgnoreCase(args[0])) {
            // /artest tile warp-state <dim> <x> <y> <z> — dumps TileWarpController
            // state for TASK-04 Phase 1 tests. Returns:
            //   tileClass, hasSpaceObject, stationId, stationOrbitingDim,
            //   stationFuel, stationDest, travelCost (computed from station state).
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.station.TileWarpController)) {
                send(sender, "{\"error\":\"tile not TileWarpController\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            zmaster587.advancedRocketry.tile.station.TileWarpController controller =
                    (zmaster587.advancedRocketry.tile.station.TileWarpController) tile;
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("tileClass", tile.getClass().getName());
            // getSpaceObject() is private — use reflection.
            zmaster587.advancedRocketry.stations.SpaceStationObject station;
            try {
                java.lang.reflect.Method m =
                        zmaster587.advancedRocketry.tile.station.TileWarpController
                                .class.getDeclaredMethod("getSpaceObject");
                m.setAccessible(true);
                station = (zmaster587.advancedRocketry.stations.SpaceStationObject) m.invoke(controller);
            } catch (ReflectiveOperationException e) {
                station = null;
            }
            info.put("hasSpaceObject", station != null);
            if (station != null) {
                info.put("stationId", station.getId());
                info.put("stationOrbitingDim", station.getOrbitingPlanetId());
                info.put("stationDestDim", station.getDestOrbitingBody());
                info.put("stationFuel", station.getFuelAmount());
                info.put("stationFuelMax", station.getMaxFuelAmount());
                info.put("stationAnchored", station.isAnchored());
                info.put("hasUsableWarpCore", station.hasUsableWarpCore());
                // getTravelCost is protected → reflect.
                try {
                    java.lang.reflect.Method tc =
                            zmaster587.advancedRocketry.tile.station.TileWarpController
                                    .class.getDeclaredMethod("getTravelCost");
                    tc.setAccessible(true);
                    info.put("travelCost", tc.invoke(controller));
                } catch (ReflectiveOperationException e) {
                    info.put("travelCost", "<reflect failed>");
                }
            }
            send(sender, jsonMap(info));
            return;
        }
        if (args.length >= 5 && "multiblock-state".equalsIgnoreCase(args[0])) {
            // /artest tile multiblock-state <dim> <x> <y> <z> — dumps
            // libVulpes TileMultiBlock state via reflection on the
            // canonical `isComplete()` / `canRender` / `completeStructure`
            // methods. Used by TASK-04 Phase 2-5 multiblock controller
            // pre-assembly contract tests.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (tile == null) {
                send(sender, "{\"error\":\"no tile entity\"}");
                return;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("tileClass", tile.getClass().getName());
            // Call isComplete() if available. libVulpes TileMultiBlock
            // exposes it as `public boolean isComplete()`.
            try {
                java.lang.reflect.Method m = tile.getClass().getMethod("isComplete");
                info.put("isComplete", m.invoke(tile));
            } catch (NoSuchMethodException e) {
                info.put("isComplete", "<not a multiblock>");
            } catch (ReflectiveOperationException e) {
                info.put("isComplete", "<reflect failed: "
                        + e.getClass().getSimpleName() + ">");
            }
            // canRender — public boolean field on libVulpes multiblocks;
            // false when structure isn't formed.
            try {
                java.lang.reflect.Field f = tile.getClass().getField("canRender");
                info.put("canRender", f.get(tile));
            } catch (NoSuchFieldException e) {
                info.put("canRender", "<no field>");
            } catch (ReflectiveOperationException e) {
                info.put("canRender", "<reflect failed>");
            }
            // isITickable — handy for the test to know whether force-tick
            // will succeed.
            info.put("isITickable", tile instanceof net.minecraft.util.ITickable);
            send(sender, jsonMap(info));
            return;
        }
        if (args.length >= 5 && "warp-trigger".equalsIgnoreCase(args[0])) {
            // /artest tile warp-trigger <dim> <x> <y> <z> — invokes the
            // production button-id=2 handler (the warp-go button). Wraps
            // onInventoryButtonPressed(2). Does NOT bypass production
            // gating (fuel, anchored, warpCore, destination); failure
            // surfaces as "station did not move" — the test reads
            // warp-state again to confirm.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.station.TileWarpController)) {
                send(sender, "{\"error\":\"tile not TileWarpController\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            zmaster587.advancedRocketry.tile.station.TileWarpController controller =
                    (zmaster587.advancedRocketry.tile.station.TileWarpController) tile;
            try {
                // Production GUI flow: GUI button → PacketMachine(controller, (byte)2)
                // → server's useNetworkData(player=null on dedicated-test path,
                // Side.SERVER, packetId=2, empty nbt). onInventoryButtonPressed
                // is the CLIENT-side dispatcher and does NOT contain the warp
                // gate code — useNetworkData on the server does.
                controller.useNetworkData(null, net.minecraftforge.fml.relauncher.Side.SERVER,
                        (byte) 2, new net.minecraft.nbt.NBTTagCompound());
            } catch (RuntimeException e) {
                send(sender, "{\"error\":\"warp trigger threw: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage())
                        + "\"}");
                return;
            }
            send(sender, "{\"ok\":true}");
            return;
        }
        if (args.length >= 5 && "warp-trigger-debug".equalsIgnoreCase(args[0])) {
            // /artest tile warp-trigger-debug <dim> <x> <y> <z>
            // Reports per-gate state for the warp-trigger production
            // condition. Doesn't actually invoke the trigger — purely
            // diagnostic.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.station.TileWarpController)) {
                send(sender, "{\"error\":\"tile not TileWarpController\"}");
                return;
            }
            try {
                java.lang.reflect.Method gso = tile.getClass().getDeclaredMethod("getSpaceObject");
                gso.setAccessible(true);
                Object spaceObj = gso.invoke(tile);
                if (!(spaceObj instanceof SpaceStationObject)) {
                    send(sender, "{\"hasStation\":false,\"reason\":\""
                            + (spaceObj == null ? "null" : spaceObj.getClass().getName())
                            + "\"}");
                    return;
                }
                SpaceStationObject sso = (SpaceStationObject) spaceObj;
                java.lang.reflect.Method getCost = tile.getClass().getDeclaredMethod("getTravelCost");
                getCost.setAccessible(true);
                int cost = (Integer) getCost.invoke(tile);
                java.lang.reflect.Method meets = tile.getClass().getDeclaredMethod(
                        "meetsArtifactReq",
                        zmaster587.advancedRocketry.dimension.DimensionProperties.class);
                meets.setAccessible(true);
                zmaster587.advancedRocketry.dimension.DimensionProperties destProps =
                        zmaster587.advancedRocketry.dimension.DimensionManager.getInstance()
                                .getDimensionProperties(sso.getDestOrbitingBody());
                boolean meetsArtifact = (Boolean) meets.invoke(tile, destProps);
                Map<String, Object> debug = new LinkedHashMap<>();
                debug.put("hasStation", true);
                debug.put("isAnchored", sso.isAnchored());
                debug.put("hasUsableWarpCore", sso.hasUsableWarpCore());
                debug.put("hasWarpCores", sso.hasWarpCores);
                debug.put("orbitingPlanetId", sso.getOrbitingPlanetId());
                debug.put("destOrbitingBody", sso.getDestOrbitingBody());
                debug.put("fuelAmount", sso.getFuelAmount());
                debug.put("travelCost", cost);
                debug.put("meetsArtifactReq", meetsArtifact);
                debug.put("destPropsNull", destProps == null);
                debug.put("destRequiredArtifactsEmpty", destProps != null && destProps.getRequiredArtifacts().isEmpty());
                debug.put("wouldUseFuelReturn", cost > sso.getFuelAmount() ? 0 : cost);
                debug.put("allGatesGreen",
                        !sso.isAnchored() && sso.hasUsableWarpCore() && meetsArtifact
                        && (cost <= sso.getFuelAmount()) && cost > 0);
                send(sender, jsonMap(debug));
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage()) + "\"}");
            }
            return;
        }
        send(sender, "{\"error\":\"unknown tile subcommand — try force-tick | warp-state | warp-trigger | warp-trigger-debug | multiblock-state\"}");
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
        if (args.length >= 5 && "inject".equalsIgnoreCase(args[0])) {
            // energy inject <dim> <x> <y> <z> <amount> [simulate]
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            int amount = args.length >= 6 ? parseIntOr(args[5], 0) : 0;
            boolean simulate = args.length >= 7 && Boolean.parseBoolean(args[6]);
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
            net.minecraftforge.energy.IEnergyStorage es = null;
            for (net.minecraft.util.EnumFacing dir : net.minecraft.util.EnumFacing.values()) {
                if (tile.hasCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, dir)) {
                    es = tile.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, dir);
                    break;
                }
            }
            if (es == null && tile.hasCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null)) {
                es = tile.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
            }
            if (es == null) {
                send(sender, "{\"error\":\"tile has no IEnergyStorage capability\"}");
                return;
            }
            int accepted = es.receiveEnergy(amount, simulate);
            send(sender, "{\"ok\":true,\"accepted\":" + accepted
                    + ",\"stored\":" + es.getEnergyStored()
                    + ",\"max\":" + es.getMaxEnergyStored() + "}");
            return;
        }
        send(sender, "{\"error\":\"unknown energy subcommand — try stored <dim> <x> <y> <z> | inject <dim> <x> <y> <z> <amount>\"}");
    }

    // §5.10 Rocket infrastructure probe ---------------------------------------

    private void handleInfra(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 5 && "service-state".equalsIgnoreCase(args[0])) {
            handleInfraServiceState(server, sender,
                    parseIntOr(args[1], Integer.MIN_VALUE),
                    parseIntOr(args[2], 0),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 0));
            return;
        }
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
        if (args.length >= 6 && "link".equalsIgnoreCase(args[0])) {
            // infra link <dim> <x> <y> <z> <entityId>
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            int entityId = parseIntOr(args[5], Integer.MIN_VALUE);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.api.IInfrastructure)) {
                send(sender, "{\"error\":\"tile not IInfrastructure\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            zmaster587.advancedRocketry.api.IInfrastructure infra =
                    (zmaster587.advancedRocketry.api.IInfrastructure) tile;
            // EntityRocketBase.linkInfrastructure calls infra.linkRocket(this) and
            // appends to its protected connectedInfrastructure list on success.
            int before, after;
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.api.EntityRocketBase
                        .class.getDeclaredField("connectedInfrastructure");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.LinkedList<zmaster587.advancedRocketry.api.IInfrastructure> list =
                        (java.util.LinkedList<zmaster587.advancedRocketry.api.IInfrastructure>) f.get(rocket);
                before = list.size();
                rocket.linkInfrastructure(infra);
                after = list.size();
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"connectedInfrastructure access failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            send(sender, "{\"ok\":true,\"linked\":" + (after > before)
                    + ",\"connectedCount\":" + after
                    + ",\"maxDistance\":" + infra.getMaxLinkDistance() + "}");
            return;
        }
        if (args.length >= 6 && "unlink".equalsIgnoreCase(args[0])) {
            // infra unlink <dim> <x> <y> <z> <entityId>
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            int entityId = parseIntOr(args[5], Integer.MIN_VALUE);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.api.IInfrastructure)) {
                send(sender, "{\"error\":\"tile not IInfrastructure\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            EntityRocket rocket = findRocket(server, entityId);
            if (rocket == null) {
                send(sender, "{\"error\":\"rocket not found\",\"entityId\":" + entityId + "}");
                return;
            }
            zmaster587.advancedRocketry.api.IInfrastructure infra =
                    (zmaster587.advancedRocketry.api.IInfrastructure) tile;
            int before, after;
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.api.EntityRocketBase
                        .class.getDeclaredField("connectedInfrastructure");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.LinkedList<zmaster587.advancedRocketry.api.IInfrastructure> list =
                        (java.util.LinkedList<zmaster587.advancedRocketry.api.IInfrastructure>) f.get(rocket);
                before = list.size();
                rocket.unlinkInfrastructure(infra);
                after = list.size();
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"connectedInfrastructure access failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            send(sender, "{\"ok\":true,\"unlinked\":" + (after < before)
                    + ",\"connectedCount\":" + after + "}");
            return;
        }
        if (args.length >= 5 && "monitor-info".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation)) {
                send(sender, "{\"error\":\"tile not TileRocketMonitoringStation\",\"tile\":\""
                        + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
                return;
            }
            zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation monitor =
                    (zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation) tile;
            int linkedEntityId = -1;
            String linkedClass = "null";
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.tile.infrastructure
                        .TileRocketMonitoringStation.class.getDeclaredField("linkedRocket");
                f.setAccessible(true);
                Object linked = f.get(monitor);
                if (linked instanceof Entity) {
                    linkedEntityId = ((Entity) linked).getEntityId();
                    linkedClass = linked.getClass().getName();
                }
            } catch (ReflectiveOperationException ignored) {
                // Field renamed — surfaces as -1 / "null"; safer than failing.
            }
            send(sender, "{\"ok\":true,\"linkedEntityId\":" + linkedEntityId
                    + ",\"linkedClass\":\"" + escapeJson(linkedClass) + "\""
                    + ",\"maxLinkDistance\":" + monitor.getMaxLinkDistance() + "}");
            return;
        }
        send(sender, "{\"error\":\"unknown infra subcommand — try info <dim> <x> <y> <z> | link <dim> <x> <y> <z> <entityId> | unlink <dim> <x> <y> <z> <entityId> | monitor-info <dim> <x> <y> <z>\"}");
    }

    // §9.2 Fixture-building primitives -----------------------------------------

    /**
     * TASK-28 F1/F6/F7 — force chunk load before block-state mutation or
     * sampling. Under parallel-fork load the chunk containing the test
     * position can be unloaded between probe round-trips; subsequent
     * {@code setBlockState} / {@code attemptCompleteStructure} /
     * {@code getBiome} calls then race with the chunk reload.
     * {@code provideChunk} loads from disk OR generates if missing —
     * synchronous and cheap on the happy path (single map lookup).
     */
    private static void ensureChunkLoaded(net.minecraft.world.WorldServer world, int blockX, int blockZ) {
        if (world == null) return;
        world.getChunkProvider().provideChunk(blockX >> 4, blockZ >> 4);
    }

    /**
     * TASK-28 F1 — force chunk load for a square area centred at the
     * given block position. {@code radiusChunks=2} covers a 5×5 chunk
     * (80×80 block) area, sufficient for every existing fixture footprint.
     */
    private static void ensureChunkAreaLoaded(net.minecraft.world.WorldServer world,
                                              int centerBlockX, int centerBlockZ,
                                              int radiusChunks) {
        if (world == null) return;
        int ccx = centerBlockX >> 4;
        int ccz = centerBlockZ >> 4;
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                world.getChunkProvider().provideChunk(ccx + dx, ccz + dz);
            }
        }
    }

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
        // Force chunk load before setBlockState — mitigates TASK-28 F6
        // (Wireless tile=null race after place).
        ensureChunkLoaded(world, x, z);
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

        // Force every chunk in the fill rectangle to be loaded — mitigates
        // TASK-28 F1 chunk-load race for fill operations that cross chunk
        // boundaries (e.g. clearing airspace around a fixture).
        int cxMin = minX >> 4, cxMax = maxX >> 4;
        int czMin = minZ >> 4, czMax = maxZ >> 4;
        for (int cx = cxMin; cx <= cxMax; cx++) {
            for (int cz = czMin; cz <= czMax; cz++) {
                world.getChunkProvider().provideChunk(cx, cz);
            }
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

    /**
     * {@code /artest fixture rocket <dim> <x> <y> <z>} — builds the
     * BuildRocketTest geometry rooted at the given pad-center coordinates in a
     * single command (faster than 40+ individual /artest place calls):
     * <ul>
     *   <li>5×5 launchpad at y</li>
     *   <li>Structure tower 6 high on one corner</li>
     *   <li>RocketBuilder (assembler tile) facing NORTH at (x+2, y+1, z-1)</li>
     *   <li>Creative input plug above the builder</li>
     *   <li>Rocket structure at (x+3, y+1, z+3): 2 advRocketmotors + 6 fuel tanks +
     *       guidance computer + seat</li>
     * </ul>
     * Returns the absolute world coordinates of the builder for use with
     * {@code /artest rocket assemble}.
     */
    private void handleFixture(MinecraftServer server, ICommandSender sender, String[] args) {
        // TASK-28 F1 — pre-load a 3×3 chunk area around the fixture origin
        // so per-variant setBlockState below hits loaded chunks. ROCKET
        // FIXTURE IS DEDUCTED FROM THIS PATH: aggressive pre-load there
        // triggered a 2 s server-thread block on cold-start, and the
        // subsequent natural-tick burst race-cleared {@code isInFlight}
        // on rockets force-launched right after, breaking 3 launch tests
        // 100 % in the TASK-28 v6 10× rerun. Other fixture variants
        // (multiblock / machine) don't race the natural-tick burst —
        // their assertion windows are larger.
        if (args.length >= 6 && !"rocket".equalsIgnoreCase(args[0])) {
            int preloadDim = parseIntOr(args[2], Integer.MIN_VALUE);
            int preloadX = parseIntOr(args[3], 0);
            int preloadZ = parseIntOr(args[5], 0);
            net.minecraft.world.WorldServer preloadWorld = server.getWorld(preloadDim);
            if (preloadWorld != null) {
                ensureChunkAreaLoaded(preloadWorld, preloadX, preloadZ, 1);
            }
        }
        if (args.length >= 5 && "rocket".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int baseX = parseIntOr(args[2], 0);
            int baseY = parseIntOr(args[3], 64);
            int baseZ = parseIntOr(args[4], 0);
            // Optional variant — defaults to "simple" (full happy-path rocket).
            // Recognised variants:
            //   simple              — full rocket: 2 engines, 6 fuel tanks, guidance, seat
            //   invalid-no-engine   — same minus engines       → expects NOENGINES on scan
            //   invalid-no-fuel-tank — same minus fuel tanks   → expects NOFUEL on scan
            //   invalid-no-seat     — same minus seat          → assembles (seat not enforced;
            //                                                    documents production behaviour)
            //   invalid-no-guidance — same minus guidance comp → expects NOGUIDANCE on scan
            String variant = args.length >= 6 ? args[5].toLowerCase(java.util.Locale.ROOT) : "simple";
            boolean includeEngines = !"invalid-no-engine".equals(variant);
            boolean includeFuelTanks = !"invalid-no-fuel-tank".equals(variant);
            boolean includeSeat = !"invalid-no-seat".equals(variant);
            boolean includeGuidance = !"invalid-no-guidance".equals(variant);
            boolean includeCargo = "with-cargo".equals(variant);
            // with-fluid-cargo: same as simple but replaces 2 of the 6 BlockFuelTank
            // positions with BlockPressurizedFluidTank (registry "liquidTank") which
            // creates TileFluidTank — a TE exposing CapabilityFluidHandler. The
            // rocket's StorageChunk then populates `liquidTiles` with these TEs so
            // MissionGasCollection.onMissionComplete can fill them.
            // Production NOFUEL gate needs >=1 fuel tank; 4 remain.
            boolean includeFluidCargo = "with-fluid-cargo".equals(variant);

            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }

            net.minecraft.block.Block launchpad =
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "launchpad"));
            net.minecraft.block.Block structureTower =
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "structureTower"));
            net.minecraft.block.Block rocketBuilder =
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "rocketBuilder"));
            net.minecraft.block.Block advEngine =
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "advRocketmotor"));
            net.minecraft.block.Block fuelTank =
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "fuelTank"));
            net.minecraft.block.Block guidanceComputer =
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "guidanceComputer"));
            net.minecraft.block.Block seat =
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "seat"));
            net.minecraft.block.Block creativePlug =
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "advStructureMachine"));
            net.minecraft.block.Block liquidTank =
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "liquidTank"));

            if (launchpad == null || rocketBuilder == null || advEngine == null
                    || fuelTank == null || guidanceComputer == null || seat == null) {
                send(sender, "{\"error\":\"missing AR block(s) in registry\"}");
                return;
            }
            if (includeFluidCargo && liquidTank == null) {
                send(sender, "{\"error\":\"missing liquidTank block (advancedrocketry:liquidTank)\"}");
                return;
            }

            int padSize = 5;
            // Launchpad (5×5).
            for (int dx = 0; dx <= padSize; dx++) {
                for (int dz = 0; dz <= padSize; dz++) {
                    world.setBlockState(new BlockPos(baseX + dx, baseY, baseZ + dz),
                            launchpad.getDefaultState());
                }
            }
            // Structure tower.
            if (structureTower != null) {
                for (int dy = 0; dy <= 6; dy++) {
                    world.setBlockState(new BlockPos(baseX - 1, baseY + dy, baseZ + padSize / 2),
                            structureTower.getDefaultState());
                }
            }
            // Rocket builder MUST face NORTH for the launchpad to be detected
            // (TileRocketAssemblingMachine.getRocketPadBounds scans the area
            // OPPOSITE the builder's facing — north-facing builder finds the
            // south pad). Replicates BuildRocketTest's explicit FACING=NORTH.
            BlockPos builderPos = new BlockPos(baseX + padSize / 2, baseY + 1, baseZ - 1);
            net.minecraft.block.state.IBlockState builderState = rocketBuilder.getDefaultState();
            try {
                builderState = builderState.withProperty(
                        zmaster587.libVulpes.block.RotatableBlock.FACING,
                        net.minecraft.util.EnumFacing.NORTH);
            } catch (IllegalArgumentException ignored) {
                // Property absent on this block variant — fall back to default state.
            }
            world.setBlockState(builderPos, builderState);
            // Creative energy source above builder.
            if (creativePlug != null) {
                world.setBlockState(builderPos.up(), creativePlug.getDefaultState());
            }

            // Rocket structure (centered around baseX+3, y+1, baseZ+3).
            int rocketX = baseX + 3, rocketY = baseY + 1, rocketZ = baseZ + 3;
            if (includeEngines) {
                world.setBlockState(new BlockPos(rocketX - 1, rocketY, rocketZ), advEngine.getDefaultState());
                world.setBlockState(new BlockPos(rocketX + 1, rocketY, rocketZ), advEngine.getDefaultState());
            }
            if (includeFuelTanks) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = 1; dy <= 2; dy++) {
                        world.setBlockState(new BlockPos(rocketX + dx, rocketY + dy, rocketZ),
                                fuelTank.getDefaultState());
                    }
                }
            }
            if (includeFluidCargo) {
                // Swap 2 of the 6 fuel-tank slots for liquidTank (TileFluidTank).
                // Pos: dx=±1, dy=2 — outer columns, upper row.
                world.setBlockState(new BlockPos(rocketX - 1, rocketY + 2, rocketZ),
                        liquidTank.getDefaultState());
                world.setBlockState(new BlockPos(rocketX + 1, rocketY + 2, rocketZ),
                        liquidTank.getDefaultState());
            }
            if (includeGuidance) {
                world.setBlockState(new BlockPos(rocketX, rocketY + 3, rocketZ), guidanceComputer.getDefaultState());
            }
            if (includeSeat) {
                world.setBlockState(new BlockPos(rocketX, rocketY + 4, rocketZ), seat.getDefaultState());
            }
            if (includeCargo) {
                // Vanilla chest above the seat — gives the rocket an IInventory
                // tile in its storage chunk for rocket-loader / unloader
                // transfer tests. The block above the seat goes from "passable
                // air" to "solid chest" → scanRocket's "passable above" check
                // for seat detection fails, so the cargo variant reports
                // seatCount=0 in addition to engineCount=2.
                world.setBlockState(new BlockPos(rocketX, rocketY + 5, rocketZ),
                        net.minecraft.init.Blocks.CHEST.getDefaultState());
            }

            send(sender, "{\"ok\":true,\"variant\":\"" + variant + "\",\"builderPos\":[" + builderPos.getX() + ","
                    + builderPos.getY() + "," + builderPos.getZ() + "]}");
            return;
        }
        if (args.length >= 5 && "machine".equalsIgnoreCase(args[0])
                && "cutting".equalsIgnoreCase(args[1])) {
            handleFixtureCuttingMachine(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        if (args.length >= 5 && "uv-rocket".equalsIgnoreCase(args[0])) {
            handleFixtureUvRocket(server, sender,
                    parseIntOr(args[1], Integer.MIN_VALUE),
                    parseIntOr(args[2], 0),
                    parseIntOr(args[3], 64),
                    parseIntOr(args[4], 0));
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "blackhole-gen".equalsIgnoreCase(args[1])) {
            handleFixtureBlackHoleGenerator(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "beacon".equalsIgnoreCase(args[1])) {
            handleFixtureBeacon(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "observatory".equalsIgnoreCase(args[1])) {
            handleFixtureObservatory(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "railgun".equalsIgnoreCase(args[1])) {
            handleFixtureRailgun(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "warp-core".equalsIgnoreCase(args[1])) {
            handleFixtureWarpCore(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "gravity-controller".equalsIgnoreCase(args[1])) {
            handleFixtureGravityController(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "planet-analyser".equalsIgnoreCase(args[1])) {
            handleFixturePlanetAnalyser(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "space-elevator".equalsIgnoreCase(args[1])) {
            handleFixtureSpaceElevator(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "microwave-receiver".equalsIgnoreCase(args[1])) {
            handleFixtureMicrowaveReceiver(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "solar-array".equalsIgnoreCase(args[1])) {
            handleFixtureSolarArray(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "terraformer".equalsIgnoreCase(args[1])) {
            handleFixtureGenericFromStructure(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0),
                    "advancedrocketry", "terraformer",
                    "zmaster587.advancedRocketry.tile.multiblock.TileAtmosphereTerraformer",
                    "structure", null);
            return;
        }
        if (args.length >= 6 && "multiblock".equalsIgnoreCase(args[0])
                && "orbital-laser-drill".equalsIgnoreCase(args[1])) {
            handleFixtureGenericFromStructure(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0),
                    "advancedrocketry", "spaceLaser",
                    "zmaster587.advancedRocketry.tile.multiblock.orbitallaserdrill.TileOrbitalLaserDrill",
                    "structure", null);
            return;
        }
        // TASK-25 — PlatePress fixture. Different shape from the multiblock
        // industrial machines: a 3-block vertical stack (obsidian / ingredient
        // / press) with no hatches, no RF, redstone-triggered. The ingredient
        // block is resolved at fixture-build time from
        // RecipesMachine.getInstance().getRecipes(BlockSmallPlatePress.class)
        // — first recipe, first ingredient alternative.
        if (args.length >= 6 && "machine".equalsIgnoreCase(args[0])
                && "plate-press".equalsIgnoreCase(args[1])) {
            handleFixturePlatePress(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0));
            return;
        }
        // TASK-18 — multiblock industrial machines via generic structure
        // helper. Keys are kebab-case short names; lookup table resolves to
        // controller registry name + tile-class FQN. TASK-26 adds optional
        // hatch overlays for wildcard-structure machines.
        if (args.length >= 6 && "machine".equalsIgnoreCase(args[0])
                && !"cutting".equalsIgnoreCase(args[1])) {
            String[] spec = lookupMultiblockMachineSpec(args[1]);
            if (spec == null) {
                send(sender, "{\"error\":\"unknown machine type\",\"name\":\""
                        + escapeJson(args[1]) + "\"}");
                return;
            }
            WildcardConfig wildcardConfig = lookupWildcardMachineOverrides(args[1]);
            handleFixtureGenericFromStructure(server, sender,
                    parseIntOr(args[2], Integer.MIN_VALUE),
                    parseIntOr(args[3], 0),
                    parseIntOr(args[4], 64),
                    parseIntOr(args[5], 0),
                    spec[0], spec[1], spec[2], "structure", wildcardConfig);
            return;
        }
        send(sender, "{\"error\":\"unknown fixture subcommand — try rocket <dim> <x> <y> <z> | machine cutting|rolling-machine|lathe|precision-assembler|electrolyser|chemical-reactor|crystallizer|arc-furnace|centrifuge|precision-laser-etcher <dim> <x> <y> <z> | multiblock blackhole-gen|beacon|observatory|railgun|warp-core|gravity-controller|planet-analyser|space-elevator|microwave-receiver|solar-array|terraformer|orbital-laser-drill <dim> <x> <y> <z>\"}");
    }

    /**
     * Builds a complete beacon multiblock with controller at (cx, cy, cz)
     * NORTH-facing. Per {@code TileBeacon.structure} — a 5-layer 3×3 array
     * with the controller 'c' at structure[4][0][1] (offset x=1, y=4, z=0):
     * <pre>
     *   y=0: REDSTONE_BLOCK at centre, AIR around (tip)
     *   y=1..3: blockStructureBlock at centre, AIR around (pillar)
     *   y=4: controller in front-row centre, 3 structureBlocks in mid-row,
     *        1 structureBlock at z=2 centre (base)
     * </pre>
     *
     * <p>For NORTH-facing controller (frontZ=-1, frontX=0) the libVulpes
     * position formula simplifies to:</p>
     * <ul>
     *   <li>{@code globalX = cx - (x - 1)}</li>
     *   <li>{@code globalY = cy - y + 4}</li>
     *   <li>{@code globalZ = cz + z}</li>
     * </ul>
     *
     * <p>The structure requires every Blocks.AIR cell to be {@code isAirBlock}
     * at validation time, so the fixture pre-clears the full 5×3×3 footprint
     * to air before placing the non-air blocks.</p>
     */
    private void handleFixtureBeacon(MinecraftServer server, ICommandSender sender,
                                     int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "beacon"));
        net.minecraft.block.Block structure =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "structuremachine"));
        if (controller == null || structure == null) {
            send(sender, "{\"error\":\"missing block(s)\",\"controller\":" + (controller != null)
                    + ",\"structure\":" + (structure != null) + "}");
            return;
        }

        // Pre-clear the 5×3×3 footprint to air. Bounding box:
        //   x: cx-1 .. cx+1
        //   y: cy   .. cy+4
        //   z: cz   .. cz+2
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                for (int dz = 0; dz <= 2; dz++) {
                    world.setBlockToAir(new BlockPos(cx + dx, cy + dy, cz + dz));
                }
            }
        }

        // Controller NORTH-facing.
        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — fall back to default.
        }

        BlockPos controllerPos = new BlockPos(cx, cy, cz);
        net.minecraft.block.state.IBlockState structState = structure.getDefaultState();
        net.minecraft.block.state.IBlockState redstoneState = net.minecraft.init.Blocks.REDSTONE_BLOCK.getDefaultState();

        // Pillar tip — REDSTONE_BLOCK at (cx, cy+4, cz+1).
        world.setBlockState(new BlockPos(cx, cy + 4, cz + 1), redstoneState);
        // Pillar shaft — 3 blockStructureBlock at (cx, cy+1..3, cz+1).
        world.setBlockState(new BlockPos(cx, cy + 3, cz + 1), structState);
        world.setBlockState(new BlockPos(cx, cy + 2, cz + 1), structState);
        world.setBlockState(new BlockPos(cx, cy + 1, cz + 1), structState);
        // Controller (y=4 in structure, z=0).
        world.setBlockState(controllerPos, controllerState);
        // y=4 z=1 row — three blockStructureBlock at (cx+1, cy, cz+1),
        // (cx, cy, cz+1), (cx-1, cy, cz+1).
        world.setBlockState(new BlockPos(cx + 1, cy, cz + 1), structState);
        world.setBlockState(new BlockPos(cx,     cy, cz + 1), structState);
        world.setBlockState(new BlockPos(cx - 1, cy, cz + 1), structState);
        // y=4 z=2 — single blockStructureBlock at (cx, cy, cz+2).
        world.setBlockState(new BlockPos(cx,     cy, cz + 2), structState);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{cx, cy, cz});
        info.put("tipPos",        new int[]{cx, cy + 4, cz + 1});
        send(sender, jsonMap(info));
    }

    /**
     * Builds a complete observatory multiblock with controller at (cx, cy, cz)
     * NORTH-facing. Per {@code TileObservatory.structure} — a 5×5×5 array
     * iterated [y][z][x] with controller 'c' at structure[3][0][2] (offset
     * x=2, y=3, z=0). For a NORTH-facing controller (frontZ=-1, frontX=0)
     * the libVulpes position formula simplifies to:
     * <pre>
     *   globalX = cx + 2 - x
     *   globalY = cy - y + 3
     *   globalZ = cz + z
     * </pre>
     *
     * <p>Layout (top → bottom):</p>
     * <ul>
     *   <li>y=0 (globalY = cy+3): 3×3 cap of {@code blockStructureBlock}
     *       at z=1..3, x=1..3, with a {@code Blocks.GLASS} lens cell at z=1, x=2.</li>
     *   <li>y=1 (globalY = cy+2): same 3×3 ring, lens cell at z=2, x=2.</li>
     *   <li>y=2 (globalY = cy+1): hollow chamber — {@code blockStructureBlock}
     *       perimeter at z=0/z=4 (x=1..3) and x=0/x=4 (z=1..3), AIR inside,
     *       lens cell at z=3, x=2.</li>
     *   <li>y=3 (globalY = cy, controller layer): controller at z=0, x=2;
     *       wildcards ({@code IRON_BLOCK}, accepted via Observatory's
     *       {@code getAllowableWildCardBlocks}) at the outer ring; 3×3
     *       {@code blockStructureBlock} grid at z=1..3, x=1..3.</li>
     *   <li>y=4 (globalY = cy-1, base): outer ring of {@code IRON_BLOCK}
     *       wildcards; {@code blockStructureTower} 3×3 at z=1..3, x=1..3;
     *       {@code libvulpes:motor} at z=2, x=2 (motors slot).</li>
     * </ul>
     *
     * <p>Every {@code Blocks.AIR} cell in y=2 must be air at validation time,
     * so the full 5×5×5 footprint is pre-cleared to air before placement.</p>
     */
    private void handleFixtureObservatory(MinecraftServer server, ICommandSender sender,
                                           int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "observatory"));
        net.minecraft.block.Block structureBlock =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "structuremachine"));
        net.minecraft.block.Block structureTower =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "structureTower"));
        net.minecraft.block.Block motor =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "motor"));

        if (controller == null || structureBlock == null
                || structureTower == null || motor == null) {
            send(sender, "{\"error\":\"missing block(s)\",\"controller\":"
                    + (controller != null) + ",\"structureBlock\":" + (structureBlock != null)
                    + ",\"structureTower\":" + (structureTower != null)
                    + ",\"motor\":" + (motor != null) + "}");
            return;
        }

        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — fall back to default.
        }

        net.minecraft.block.state.IBlockState struct = structureBlock.getDefaultState();
        net.minecraft.block.state.IBlockState tower = structureTower.getDefaultState();
        net.minecraft.block.state.IBlockState motorState = motor.getDefaultState();
        net.minecraft.block.state.IBlockState iron = net.minecraft.init.Blocks.IRON_BLOCK.getDefaultState();
        net.minecraft.block.state.IBlockState glass = net.minecraft.init.Blocks.GLASS.getDefaultState();

        // Pre-clear the full 5×5×5 footprint to air.
        for (int gx = cx - 2; gx <= cx + 2; gx++) {
            for (int gy = cy - 1; gy <= cy + 3; gy++) {
                for (int gz = cz; gz <= cz + 4; gz++) {
                    world.setBlockToAir(new BlockPos(gx, gy, gz));
                }
            }
        }

        // y=0 cap, globalY = cy + 3. 3×3 of struct, lens at z=1 x=2.
        for (int z = 1; z <= 3; z++) {
            for (int x = 1; x <= 3; x++) {
                BlockPos p = new BlockPos(cx + 2 - x, cy + 3, cz + z);
                world.setBlockState(p, (z == 1 && x == 2) ? glass : struct);
            }
        }

        // y=1, globalY = cy + 2. 3×3 of struct, lens at z=2 x=2.
        for (int z = 1; z <= 3; z++) {
            for (int x = 1; x <= 3; x++) {
                BlockPos p = new BlockPos(cx + 2 - x, cy + 2, cz + z);
                world.setBlockState(p, (z == 2 && x == 2) ? glass : struct);
            }
        }

        // y=2, globalY = cy + 1. Hollow chamber + lens at z=3 x=2.
        for (int x = 1; x <= 3; x++) {
            world.setBlockState(new BlockPos(cx + 2 - x, cy + 1, cz), struct);
            world.setBlockState(new BlockPos(cx + 2 - x, cy + 1, cz + 4), struct);
        }
        for (int z = 1; z <= 3; z++) {
            world.setBlockState(new BlockPos(cx + 2,     cy + 1, cz + z), struct);
            world.setBlockState(new BlockPos(cx + 2 - 4, cy + 1, cz + z), struct);
        }
        world.setBlockState(new BlockPos(cx, cy + 1, cz + 3), glass);  // central lens

        // y=3 controller layer, globalY = cy.
        BlockPos controllerPos = new BlockPos(cx, cy, cz);
        world.setBlockState(controllerPos, controllerState);
        world.setBlockState(new BlockPos(cx + 2 - 1, cy, cz), iron);
        world.setBlockState(new BlockPos(cx + 2 - 3, cy, cz), iron);
        for (int z = 1; z <= 3; z++) {
            world.setBlockState(new BlockPos(cx + 2,     cy, cz + z), iron);
            world.setBlockState(new BlockPos(cx + 2 - 4, cy, cz + z), iron);
            for (int x = 1; x <= 3; x++) {
                world.setBlockState(new BlockPos(cx + 2 - x, cy, cz + z), struct);
            }
        }
        for (int x = 1; x <= 3; x++) {
            world.setBlockState(new BlockPos(cx + 2 - x, cy, cz + 4), iron);
        }

        // y=4 base, globalY = cy - 1.
        for (int x = 1; x <= 3; x++) {
            world.setBlockState(new BlockPos(cx + 2 - x, cy - 1, cz), iron);
            world.setBlockState(new BlockPos(cx + 2 - x, cy - 1, cz + 4), iron);
        }
        for (int z = 1; z <= 3; z++) {
            world.setBlockState(new BlockPos(cx + 2,     cy - 1, cz + z), iron);
            world.setBlockState(new BlockPos(cx + 2 - 4, cy - 1, cz + z), iron);
            for (int x = 1; x <= 3; x++) {
                BlockPos p = new BlockPos(cx + 2 - x, cy - 1, cz + z);
                world.setBlockState(p, (z == 2 && x == 2) ? motorState : tower);
            }
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{cx, cy, cz});
        info.put("lensCentre",    new int[]{cx, cy + 1, cz + 3});
        info.put("motorPos",      new int[]{cx, cy - 1, cz + 2});
        send(sender, jsonMap(info));
    }

    /**
     * Builds a complete railgun multiblock with controller at (cx, cy, cz)
     * NORTH-facing. Per {@code TileRailgun.structure} — an 11×9×9 array
     * iterated [y][z][x] with controller 'c' at structure[10][1][4] (offset
     * x=4, y=10, z=1). For a NORTH-facing controller the position formula
     * simplifies to:
     * <pre>
     *   globalX = cx + 4 - x
     *   globalY = cy - y + 10
     *   globalZ = cz - 1 + z
     * </pre>
     *
     * <p>The structure is mostly empty (sparse). The non-null cells are:</p>
     * <ul>
     *   <li>y=0..9 (globalY = cy+10..cy+1): a {@code coilCopper} cross
     *       around an {@code blockStructureBlock} core column at z=4±1, x=4±1
     *       (5 cells per layer × 10 layers).</li>
     *   <li>y=10 (globalY = cy, bottom slab): a full dish — blockSteel
     *       corner ring + {@code slab} (vanilla stone slab) outer ring +
     *       {@code blockAdvStructureBlock} middle ring + {@code blockTitanium}
     *       inner ring + {@code blockSteel} caps + a single
     *       {@code blockAdvancedMotor} (motors slot) at z=4, x=4 in inner ring;
     *       the controller at z=1, x=4 with input/output hatches at z=1
     *       x=3/x=5, power-input plugs at z=7, x=3/4/5.</li>
     * </ul>
     *
     * <p>The bottom layer is the only one that requires the AR / libVulpes
     * advStructure / blockSteel / blockTitanium / slab mix; the upper 10
     * layers are pure coilCopper + structureBlock columns.</p>
     */
    private void handleFixtureRailgun(MinecraftServer server, ICommandSender sender,
                                       int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "railgun"));
        net.minecraft.block.Block structureBlock =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "structuremachine"));
        net.minecraft.block.Block advStructure =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "advstructuremachine"));
        net.minecraft.block.Block motorAdvanced =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "advancedMotor"));
        net.minecraft.block.Block hatch =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "hatch"));
        net.minecraft.block.Block powerInput =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "forgepowerinput"));

        // The Railgun structure references several blocks through the OreDictionary
        // ("coilCopper", "blockSteel", "blockTitanium", "slab"). These are
        // registered dynamically by MaterialRegistry + AR's setup, so look them
        // up at runtime — the registry names of the underlying BlockOre tiles
        // ("metal0", "coil0", etc.) and their meta values depend on the order
        // materials are inserted into the registry.
        net.minecraft.block.state.IBlockState coil = firstOreDictBlockState("coilCopper");
        net.minecraft.block.state.IBlockState steel = firstOreDictBlockState("blockSteel");
        net.minecraft.block.state.IBlockState titanium = firstOreDictBlockState("blockTitanium");
        net.minecraft.block.state.IBlockState slab = firstOreDictBlockState("slab");

        if (controller == null || structureBlock == null
                || advStructure == null || motorAdvanced == null
                || hatch == null || powerInput == null
                || coil == null || steel == null || titanium == null || slab == null) {
            send(sender, "{\"error\":\"missing block(s)\""
                    + ",\"controller\":" + (controller != null)
                    + ",\"coilCopper\":" + (coil != null)
                    + ",\"structureBlock\":" + (structureBlock != null)
                    + ",\"advStructure\":" + (advStructure != null)
                    + ",\"blockSteel\":" + (steel != null)
                    + ",\"blockTitanium\":" + (titanium != null)
                    + ",\"slab\":" + (slab != null)
                    + ",\"motorAdvanced\":" + (motorAdvanced != null)
                    + ",\"hatch\":" + (hatch != null)
                    + ",\"powerInput\":" + (powerInput != null) + "}");
            return;
        }

        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — fall back to default.
        }

        net.minecraft.block.state.IBlockState struct = structureBlock.getDefaultState();
        net.minecraft.block.state.IBlockState advStruct = advStructure.getDefaultState();
        net.minecraft.block.state.IBlockState advMotor = motorAdvanced.getDefaultState();
        @SuppressWarnings("deprecation") net.minecraft.block.state.IBlockState inputState =
                hatch.getStateFromMeta(0);   // meta 0 = input hatch
        @SuppressWarnings("deprecation") net.minecraft.block.state.IBlockState outputState =
                hatch.getStateFromMeta(1);   // meta 1 = output hatch
        net.minecraft.block.state.IBlockState plug = powerInput.getDefaultState();

        // Pre-clear the full footprint to air: x [cx-4 .. cx+4],
        // y [cy .. cy+10], z [cz-1 .. cz+7].
        for (int gx = cx - 4; gx <= cx + 4; gx++) {
            for (int gy = cy; gy <= cy + 10; gy++) {
                for (int gz = cz - 1; gz <= cz + 7; gz++) {
                    world.setBlockToAir(new BlockPos(gx, gy, gz));
                }
            }
        }

        // y=0..8 — coil cross around structureBlock core (top 9 layers; y=9 is
        // a special transition layer, see below). structure[y][z=3..5][x=3..5]:
        //   z=3 → only x=4 is coilCopper
        //   z=4 → x=3 coil, x=4 STRUCT (core), x=5 coil
        //   z=5 → only x=4 is coilCopper
        for (int y = 0; y <= 8; y++) {
            int globalY = cy - y + 10;
            // z=3 (globalZ = cz - 1 + 3 = cz + 2)
            world.setBlockState(new BlockPos(cx, globalY, cz + 2), coil);
            // z=4 (globalZ = cz + 3) — coil/struct/coil
            world.setBlockState(new BlockPos(cx + 1, globalY, cz + 3), coil);
            world.setBlockState(new BlockPos(cx,     globalY, cz + 3), struct);
            world.setBlockState(new BlockPos(cx - 1, globalY, cz + 3), coil);
            // z=5 (globalZ = cz + 4)
            world.setBlockState(new BlockPos(cx, globalY, cz + 4), coil);
        }

        // y=9 transition layer (globalY = cy + 1): blockSteel caps + blockTitanium
        // plus-sign with advStructure corners.
        int gy9 = cy + 1;
        // z=2 (globalZ = cz + 1): blockSteel at x=4 (centre)
        world.setBlockState(new BlockPos(cx, gy9, cz + 1), steel);
        // z=3 (globalZ = cz + 2): advStruct(x=3), titanium(x=4), advStruct(x=5)
        world.setBlockState(new BlockPos(cx + 1, gy9, cz + 2), advStruct);
        world.setBlockState(new BlockPos(cx,     gy9, cz + 2), titanium);
        world.setBlockState(new BlockPos(cx - 1, gy9, cz + 2), advStruct);
        // z=4 (globalZ = cz + 3): steel(x=2), titanium(x=3..5), steel(x=6)
        world.setBlockState(new BlockPos(cx + 2, gy9, cz + 3), steel);
        world.setBlockState(new BlockPos(cx + 1, gy9, cz + 3), titanium);
        world.setBlockState(new BlockPos(cx,     gy9, cz + 3), titanium);
        world.setBlockState(new BlockPos(cx - 1, gy9, cz + 3), titanium);
        world.setBlockState(new BlockPos(cx - 2, gy9, cz + 3), steel);
        // z=5 (globalZ = cz + 4): advStruct(x=3), titanium(x=4), advStruct(x=5)
        world.setBlockState(new BlockPos(cx + 1, gy9, cz + 4), advStruct);
        world.setBlockState(new BlockPos(cx,     gy9, cz + 4), titanium);
        world.setBlockState(new BlockPos(cx - 1, gy9, cz + 4), advStruct);
        // z=6 (globalZ = cz + 5): blockSteel at x=4 (centre)
        world.setBlockState(new BlockPos(cx, gy9, cz + 5), steel);

        // y=10 (globalY = cy) bottom dish.
        // Row z=0 (globalZ = cz - 1): steel,null,null,slab,slab,slab,null,null,steel
        world.setBlockState(new BlockPos(cx + 4 - 0, cy, cz - 1), steel);
        world.setBlockState(new BlockPos(cx + 4 - 3, cy, cz - 1), slab);
        world.setBlockState(new BlockPos(cx + 4 - 4, cy, cz - 1), slab);
        world.setBlockState(new BlockPos(cx + 4 - 5, cy, cz - 1), slab);
        world.setBlockState(new BlockPos(cx + 4 - 8, cy, cz - 1), steel);
        // Row z=1 (globalZ = cz): null,advStruct,slab,'I','c','O',slab,advStruct,null
        world.setBlockState(new BlockPos(cx + 4 - 1, cy, cz), advStruct);
        world.setBlockState(new BlockPos(cx + 4 - 2, cy, cz), slab);
        world.setBlockState(new BlockPos(cx + 4 - 3, cy, cz), inputState);
        world.setBlockState(new BlockPos(cx,         cy, cz), controllerState);
        world.setBlockState(new BlockPos(cx + 4 - 5, cy, cz), outputState);
        world.setBlockState(new BlockPos(cx + 4 - 6, cy, cz), slab);
        world.setBlockState(new BlockPos(cx + 4 - 7, cy, cz), advStruct);
        // Row z=2 (globalZ = cz + 1): null,slab,advStruct×5,slab,null
        world.setBlockState(new BlockPos(cx + 4 - 1, cy, cz + 1), slab);
        for (int x = 2; x <= 6; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 1), advStruct);
        }
        world.setBlockState(new BlockPos(cx + 4 - 7, cy, cz + 1), slab);
        // Row z=3 (globalZ = cz + 2): slab,slab,advStruct×5,slab,slab
        for (int x = 0; x <= 1; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 2), slab);
        }
        for (int x = 2; x <= 6; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 2), advStruct);
        }
        for (int x = 7; x <= 8; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 2), slab);
        }
        // Row z=4 (globalZ = cz + 3): slab,slab,advStruct,advStruct,MOTOR,advStruct,advStruct,slab,slab
        for (int x = 0; x <= 1; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 3), slab);
        }
        world.setBlockState(new BlockPos(cx + 4 - 2, cy, cz + 3), advStruct);
        world.setBlockState(new BlockPos(cx + 4 - 3, cy, cz + 3), advStruct);
        world.setBlockState(new BlockPos(cx,         cy, cz + 3), advMotor);
        world.setBlockState(new BlockPos(cx + 4 - 5, cy, cz + 3), advStruct);
        world.setBlockState(new BlockPos(cx + 4 - 6, cy, cz + 3), advStruct);
        for (int x = 7; x <= 8; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 3), slab);
        }
        // Row z=5 (globalZ = cz + 4): slab,slab,advStruct×5,slab,slab
        for (int x = 0; x <= 1; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 4), slab);
        }
        for (int x = 2; x <= 6; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 4), advStruct);
        }
        for (int x = 7; x <= 8; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 4), slab);
        }
        // Row z=6 (globalZ = cz + 5): null,slab,advStruct×5,slab,null
        world.setBlockState(new BlockPos(cx + 4 - 1, cy, cz + 5), slab);
        for (int x = 2; x <= 6; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 5), advStruct);
        }
        world.setBlockState(new BlockPos(cx + 4 - 7, cy, cz + 5), slab);
        // Row z=7 (globalZ = cz + 6): null,advStruct,slab,'P','P','P',slab,advStruct,null
        world.setBlockState(new BlockPos(cx + 4 - 1, cy, cz + 6), advStruct);
        world.setBlockState(new BlockPos(cx + 4 - 2, cy, cz + 6), slab);
        world.setBlockState(new BlockPos(cx + 4 - 3, cy, cz + 6), plug);
        world.setBlockState(new BlockPos(cx,         cy, cz + 6), plug);
        world.setBlockState(new BlockPos(cx + 4 - 5, cy, cz + 6), plug);
        world.setBlockState(new BlockPos(cx + 4 - 6, cy, cz + 6), slab);
        world.setBlockState(new BlockPos(cx + 4 - 7, cy, cz + 6), advStruct);
        // Row z=8 (globalZ = cz + 7): steel,null,null,slab,slab,slab,null,null,steel
        world.setBlockState(new BlockPos(cx + 4 - 0, cy, cz + 7), steel);
        world.setBlockState(new BlockPos(cx + 4 - 3, cy, cz + 7), slab);
        world.setBlockState(new BlockPos(cx + 4 - 4, cy, cz + 7), slab);
        world.setBlockState(new BlockPos(cx + 4 - 5, cy, cz + 7), slab);
        world.setBlockState(new BlockPos(cx + 4 - 8, cy, cz + 7), steel);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{cx, cy, cz});
        info.put("motorPos",      new int[]{cx, cy, cz + 3});
        info.put("coreTopPos",    new int[]{cx, cy + 10, cz + 3});
        send(sender, jsonMap(info));
    }

    /**
     * Builds a complete warp-core multiblock with controller at (cx, cy, cz)
     * NORTH-facing. Per {@code TileWarpCore.structure} — a 3×3×3 array
     * iterated [y][z][x] with controller 'c' at structure[2][0][1] (offset
     * x=1, y=2, z=0). For a NORTH-facing controller the position formula
     * simplifies to:
     * <pre>
     *   globalX = cx + 1 - x
     *   globalY = cy + 2 - y
     *   globalZ = cz + z
     * </pre>
     *
     * <p>Layout:</p>
     * <ul>
     *   <li>y=0 (globalY = cy+2): 3×3 of {@code blockWarpCoreRim} with
     *       {@code 'I'} input hatch at z=1, x=1.</li>
     *   <li>y=1 (globalY = cy+1): cross of {@code blockStructureBlock}
     *       around {@code blockWarpCoreCore} centre at z=1, x=1, with
     *       null cells in the corners.</li>
     *   <li>y=2 (globalY = cy): {@code 'c'} controller at z=0, x=1;
     *       {@code blockWarpCoreCore} at z=1, x=1; remainder
     *       {@code blockWarpCoreRim}.</li>
     * </ul>
     *
     * <p>{@code blockWarpCoreRim} and {@code blockWarpCoreCore} are
     * OreDictionary entries (registered by AR's setup —
     * {@code AdvancedRocketry.preInit} lines 603-604, pointing to
     * Titanium block + Gold block respectively).</p>
     */
    private void handleFixtureWarpCore(MinecraftServer server, ICommandSender sender,
                                        int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "warpCore"));
        net.minecraft.block.Block structureBlock =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "structuremachine"));
        net.minecraft.block.Block hatch =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "hatch"));

        net.minecraft.block.state.IBlockState rim = firstOreDictBlockState("blockWarpCoreRim");
        net.minecraft.block.state.IBlockState core = firstOreDictBlockState("blockWarpCoreCore");

        if (controller == null || structureBlock == null || hatch == null
                || rim == null || core == null) {
            send(sender, "{\"error\":\"missing block(s)\""
                    + ",\"controller\":" + (controller != null)
                    + ",\"structureBlock\":" + (structureBlock != null)
                    + ",\"hatch\":" + (hatch != null)
                    + ",\"rim\":" + (rim != null)
                    + ",\"core\":" + (core != null) + "}");
            return;
        }

        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — fall back to default.
        }

        net.minecraft.block.state.IBlockState struct = structureBlock.getDefaultState();
        @SuppressWarnings("deprecation") net.minecraft.block.state.IBlockState inputHatchState =
                hatch.getStateFromMeta(0);

        // Pre-clear the 3×3×3 footprint to air.
        for (int gx = cx - 1; gx <= cx + 1; gx++) {
            for (int gy = cy; gy <= cy + 2; gy++) {
                for (int gz = cz; gz <= cz + 2; gz++) {
                    world.setBlockToAir(new BlockPos(gx, gy, gz));
                }
            }
        }

        // y=0 (top) globalY = cy + 2 — 3×3 rim with input hatch at z=1, x=1.
        for (int z = 0; z <= 2; z++) {
            for (int x = 0; x <= 2; x++) {
                BlockPos p = new BlockPos(cx + 1 - x, cy + 2, cz + z);
                world.setBlockState(p, (z == 1 && x == 1) ? inputHatchState : rim);
            }
        }

        // y=1 (middle) globalY = cy + 1 — cross of structureBlock + core centre.
        // Cells: (z=0,x=1), (z=1,x=0), (z=1,x=1 core), (z=1,x=2), (z=2,x=1)
        world.setBlockState(new BlockPos(cx,     cy + 1, cz),     struct);
        world.setBlockState(new BlockPos(cx + 1, cy + 1, cz + 1), struct);
        world.setBlockState(new BlockPos(cx,     cy + 1, cz + 1), core);
        world.setBlockState(new BlockPos(cx - 1, cy + 1, cz + 1), struct);
        world.setBlockState(new BlockPos(cx,     cy + 1, cz + 2), struct);

        // y=2 (bottom, controller layer) globalY = cy.
        // Row z=0: rim, 'c', rim
        world.setBlockState(new BlockPos(cx + 1, cy, cz),     rim);
        world.setBlockState(new BlockPos(cx,     cy, cz),     controllerState);
        world.setBlockState(new BlockPos(cx - 1, cy, cz),     rim);
        // Row z=1: rim, core, rim
        world.setBlockState(new BlockPos(cx + 1, cy, cz + 1), rim);
        world.setBlockState(new BlockPos(cx,     cy, cz + 1), core);
        world.setBlockState(new BlockPos(cx - 1, cy, cz + 1), rim);
        // Row z=2: rim, rim, rim
        for (int x = 0; x <= 2; x++) {
            world.setBlockState(new BlockPos(cx + 1 - x, cy, cz + 2), rim);
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{cx, cy, cz});
        info.put("coreCentre",    new int[]{cx, cy + 1, cz + 1});
        info.put("inputHatchPos", new int[]{cx, cy + 2, cz + 1});
        send(sender, jsonMap(info));
    }

    /**
     * Builds a complete area-gravity-controller multiblock with controller at
     * (cx, cy, cz) NORTH-facing. Per {@code TileAreaGravityController.structure}
     * — a 2×3×3 array iterated [y][z][x] with controller 'c' at
     * structure[0][1][1] (offset x=1, y=0, z=1). For a NORTH-facing
     * controller the position formula simplifies to:
     * <pre>
     *   globalX = cx + 1 - x
     *   globalY = cy - y
     *   globalZ = cz + z - 1
     * </pre>
     *
     * <p>Layout:</p>
     * <ul>
     *   <li>y=0 (globalY = cy, controller layer): just {@code 'c'}
     *       at (cx, cy, cz). Everything else is null (no constraint).</li>
     *   <li>y=1 (globalY = cy - 1): cross of {@code advStructureBlock}
     *       around a {@code 'P'} power-input plug at (cx, cy-1, cz).</li>
     * </ul>
     */
    private void handleFixtureGravityController(MinecraftServer server, ICommandSender sender,
                                                 int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "gravityMachine"));
        net.minecraft.block.Block advStructure =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "advstructuremachine"));
        net.minecraft.block.Block powerInput =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "forgepowerinput"));

        if (controller == null || advStructure == null || powerInput == null) {
            send(sender, "{\"error\":\"missing block(s)\""
                    + ",\"controller\":" + (controller != null)
                    + ",\"advStructure\":" + (advStructure != null)
                    + ",\"powerInput\":" + (powerInput != null) + "}");
            return;
        }

        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — fall back to default.
        }

        net.minecraft.block.state.IBlockState advStruct = advStructure.getDefaultState();
        net.minecraft.block.state.IBlockState plug = powerInput.getDefaultState();

        // Controller at top.
        world.setBlockState(new BlockPos(cx, cy, cz), controllerState);
        // Underside cross — advStruct N/E/S/W of plug + plug at centre below
        // controller.
        world.setBlockState(new BlockPos(cx,     cy - 1, cz - 1), advStruct);
        world.setBlockState(new BlockPos(cx + 1, cy - 1, cz),     advStruct);
        world.setBlockState(new BlockPos(cx,     cy - 1, cz),     plug);
        world.setBlockState(new BlockPos(cx - 1, cy - 1, cz),     advStruct);
        world.setBlockState(new BlockPos(cx,     cy - 1, cz + 1), advStruct);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{cx, cy, cz});
        info.put("plugPos",       new int[]{cx, cy - 1, cz});
        send(sender, jsonMap(info));
    }

    /**
     * Builds a complete planet-analyser (TileAstrobodyDataProcessor) multiblock
     * with controller at (cx, cy, cz) NORTH-facing. Per
     * {@code TileAstrobodyDataProcessor.structure} — a 2×2×3 array iterated
     * [y][z][x] with controller 'c' at structure[0][0][1] (offset x=1, y=0, z=0).
     * For a NORTH-facing controller the position formula simplifies to:
     * <pre>
     *   globalX = cx + 1 - x
     *   globalY = cy - y
     *   globalZ = cz + z
     * </pre>
     *
     * <p>Layout:</p>
     * <ul>
     *   <li>y=0 z=0 (globalY = cy, globalZ = cz): slab, 'c', slab</li>
     *   <li>y=0 z=1 (globalY = cy, globalZ = cz + 1): slab, slab, slab</li>
     *   <li>y=1 z=0 (globalY = cy - 1, globalZ = cz):
     *       'P' (power input), 'I' (item input), 'O' (item output)</li>
     *   <li>y=1 z=1 (globalY = cy - 1, globalZ = cz + 1): 'D', 'D', 'D' —
     *       three data hatches ({@code advancedrocketry:loader} meta 0).</li>
     * </ul>
     */
    private void handleFixturePlanetAnalyser(MinecraftServer server, ICommandSender sender,
                                              int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "planetAnalyser"));
        net.minecraft.block.Block hatch =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "hatch"));
        net.minecraft.block.Block powerInput =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "forgepowerinput"));
        net.minecraft.block.Block dataLoader =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "loader"));

        net.minecraft.block.state.IBlockState slab = firstOreDictBlockState("slab");

        if (controller == null || hatch == null || powerInput == null
                || dataLoader == null || slab == null) {
            send(sender, "{\"error\":\"missing block(s)\""
                    + ",\"controller\":" + (controller != null)
                    + ",\"hatch\":" + (hatch != null)
                    + ",\"powerInput\":" + (powerInput != null)
                    + ",\"dataLoader\":" + (dataLoader != null)
                    + ",\"slab\":" + (slab != null) + "}");
            return;
        }

        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — fall back to default.
        }

        @SuppressWarnings("deprecation") net.minecraft.block.state.IBlockState input =
                hatch.getStateFromMeta(0);
        @SuppressWarnings("deprecation") net.minecraft.block.state.IBlockState output =
                hatch.getStateFromMeta(1);
        @SuppressWarnings("deprecation") net.minecraft.block.state.IBlockState dataIn =
                dataLoader.getStateFromMeta(0);
        net.minecraft.block.state.IBlockState plug = powerInput.getDefaultState();

        // y=0 z=0 — slab, 'c', slab
        world.setBlockState(new BlockPos(cx + 1, cy, cz),     slab);
        world.setBlockState(new BlockPos(cx,     cy, cz),     controllerState);
        world.setBlockState(new BlockPos(cx - 1, cy, cz),     slab);
        // y=0 z=1 — slab×3
        world.setBlockState(new BlockPos(cx + 1, cy, cz + 1), slab);
        world.setBlockState(new BlockPos(cx,     cy, cz + 1), slab);
        world.setBlockState(new BlockPos(cx - 1, cy, cz + 1), slab);
        // y=1 z=0 — 'P', 'I', 'O'
        world.setBlockState(new BlockPos(cx + 1, cy - 1, cz),     plug);
        world.setBlockState(new BlockPos(cx,     cy - 1, cz),     input);
        world.setBlockState(new BlockPos(cx - 1, cy - 1, cz),     output);
        // y=1 z=1 — 'D'×3
        world.setBlockState(new BlockPos(cx + 1, cy - 1, cz + 1), dataIn);
        world.setBlockState(new BlockPos(cx,     cy - 1, cz + 1), dataIn);
        world.setBlockState(new BlockPos(cx - 1, cy - 1, cz + 1), dataIn);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{cx, cy, cz});
        info.put("plugPos",       new int[]{cx + 1, cy - 1, cz});
        info.put("dataHatchRow",  new int[]{cx, cy - 1, cz + 1});
        send(sender, jsonMap(info));
    }

    /**
     * Builds a complete space-elevator multiblock with controller at
     * (cx, cy, cz) NORTH-facing. Per {@code TileSpaceElevator.structure} — a
     * 1-layer 10×9 disc iterated [y=0][z][x] with controller 'c' at
     * structure[0][0][4] (offset x=4, y=0, z=0). For a NORTH-facing
     * controller the position formula simplifies to:
     * <pre>
     *   globalX = cx + 4 - x
     *   globalY = cy
     *   globalZ = cz + z
     * </pre>
     *
     * <p>Layout (single layer, z=0..9):</p>
     * <ul>
     *   <li>z=0 (controller row): AIR×3, 'P', 'c', 'P', AIR×3.</li>
     *   <li>z=1: blockSteel, AIR, AIR, slab, slab, slab, AIR, AIR, blockSteel.</li>
     *   <li>z=2: AIR, advStruct, slab, slab, slab, slab, slab, advStruct, AIR.</li>
     *   <li>z=3: AIR, slab, advStruct, slab, slab, slab, advStruct, slab, AIR.</li>
     *   <li>z=4: slab×3, advStruct×3, slab×3.</li>
     *   <li>z=5: slab×3, advStruct, motor, advStruct, slab×3. (centre motor)</li>
     *   <li>z=6: slab×3, advStruct×3, slab×3.</li>
     *   <li>z=7: AIR, slab, advStruct, slab×3, advStruct, slab, AIR.</li>
     *   <li>z=8: AIR, advStruct, slab×5, advStruct, AIR.</li>
     *   <li>z=9: blockSteel, AIR×2, slab×3, AIR×2, blockSteel.</li>
     * </ul>
     *
     * <p>Footprint pre-cleared to air before placement so that
     * {@code Blocks.AIR} cells satisfy the strict validator check.</p>
     */
    private void handleFixtureSpaceElevator(MinecraftServer server, ICommandSender sender,
                                             int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "spaceElevatorController"));
        net.minecraft.block.Block advStructure =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "advstructuremachine"));
        net.minecraft.block.Block motor =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "motor"));
        net.minecraft.block.Block powerInput =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "forgepowerinput"));

        net.minecraft.block.state.IBlockState slab = firstOreDictBlockState("slab");
        net.minecraft.block.state.IBlockState steel = firstOreDictBlockState("blockSteel");

        if (controller == null || advStructure == null || motor == null
                || powerInput == null || slab == null || steel == null) {
            send(sender, "{\"error\":\"missing block(s)\""
                    + ",\"controller\":" + (controller != null)
                    + ",\"advStructure\":" + (advStructure != null)
                    + ",\"motor\":" + (motor != null)
                    + ",\"powerInput\":" + (powerInput != null)
                    + ",\"slab\":" + (slab != null)
                    + ",\"blockSteel\":" + (steel != null) + "}");
            return;
        }

        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — fall back to default.
        }

        net.minecraft.block.state.IBlockState advStruct = advStructure.getDefaultState();
        net.minecraft.block.state.IBlockState motorState = motor.getDefaultState();
        net.minecraft.block.state.IBlockState plug = powerInput.getDefaultState();

        // Pre-clear the 9-wide × 10-deep footprint to air (single y layer).
        for (int gx = cx - 4; gx <= cx + 4; gx++) {
            for (int gz = cz; gz <= cz + 9; gz++) {
                world.setBlockToAir(new BlockPos(gx, cy, gz));
            }
        }

        // z=0 controller row: AIR(x=0..2), 'P'(x=3), 'c'(x=4), 'P'(x=5), AIR(x=6..8)
        world.setBlockState(new BlockPos(cx + 1, cy, cz), plug);
        world.setBlockState(new BlockPos(cx,     cy, cz), controllerState);
        world.setBlockState(new BlockPos(cx - 1, cy, cz), plug);

        // z=1: steel(x=0), AIR(x=1,2), slab(x=3,4,5), AIR(x=6,7), steel(x=8)
        world.setBlockState(new BlockPos(cx + 4 - 0, cy, cz + 1), steel);
        world.setBlockState(new BlockPos(cx + 4 - 3, cy, cz + 1), slab);
        world.setBlockState(new BlockPos(cx + 4 - 4, cy, cz + 1), slab);
        world.setBlockState(new BlockPos(cx + 4 - 5, cy, cz + 1), slab);
        world.setBlockState(new BlockPos(cx + 4 - 8, cy, cz + 1), steel);

        // z=2: AIR(x=0), advStruct(x=1), slab(x=2..6), advStruct(x=7), AIR(x=8)
        world.setBlockState(new BlockPos(cx + 4 - 1, cy, cz + 2), advStruct);
        for (int x = 2; x <= 6; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 2), slab);
        }
        world.setBlockState(new BlockPos(cx + 4 - 7, cy, cz + 2), advStruct);

        // z=3: AIR(x=0), slab(x=1), advStruct(x=2), slab(x=3..5), advStruct(x=6), slab(x=7), AIR(x=8)
        world.setBlockState(new BlockPos(cx + 4 - 1, cy, cz + 3), slab);
        world.setBlockState(new BlockPos(cx + 4 - 2, cy, cz + 3), advStruct);
        for (int x = 3; x <= 5; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 3), slab);
        }
        world.setBlockState(new BlockPos(cx + 4 - 6, cy, cz + 3), advStruct);
        world.setBlockState(new BlockPos(cx + 4 - 7, cy, cz + 3), slab);

        // z=4: slab(x=0..2), advStruct(x=3..5), slab(x=6..8)
        for (int x = 0; x <= 2; x++) world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 4), slab);
        for (int x = 3; x <= 5; x++) world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 4), advStruct);
        for (int x = 6; x <= 8; x++) world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 4), slab);

        // z=5: slab(x=0..2), advStruct(x=3), MOTOR(x=4), advStruct(x=5), slab(x=6..8)
        for (int x = 0; x <= 2; x++) world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 5), slab);
        world.setBlockState(new BlockPos(cx + 4 - 3, cy, cz + 5), advStruct);
        world.setBlockState(new BlockPos(cx,         cy, cz + 5), motorState);
        world.setBlockState(new BlockPos(cx + 4 - 5, cy, cz + 5), advStruct);
        for (int x = 6; x <= 8; x++) world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 5), slab);

        // z=6: slab(x=0..2), advStruct(x=3..5), slab(x=6..8)
        for (int x = 0; x <= 2; x++) world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 6), slab);
        for (int x = 3; x <= 5; x++) world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 6), advStruct);
        for (int x = 6; x <= 8; x++) world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 6), slab);

        // z=7: AIR(x=0), slab(x=1), advStruct(x=2), slab(x=3..5), advStruct(x=6), slab(x=7), AIR(x=8)
        world.setBlockState(new BlockPos(cx + 4 - 1, cy, cz + 7), slab);
        world.setBlockState(new BlockPos(cx + 4 - 2, cy, cz + 7), advStruct);
        for (int x = 3; x <= 5; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 7), slab);
        }
        world.setBlockState(new BlockPos(cx + 4 - 6, cy, cz + 7), advStruct);
        world.setBlockState(new BlockPos(cx + 4 - 7, cy, cz + 7), slab);

        // z=8: AIR(x=0), advStruct(x=1), slab(x=2..6), advStruct(x=7), AIR(x=8)
        world.setBlockState(new BlockPos(cx + 4 - 1, cy, cz + 8), advStruct);
        for (int x = 2; x <= 6; x++) {
            world.setBlockState(new BlockPos(cx + 4 - x, cy, cz + 8), slab);
        }
        world.setBlockState(new BlockPos(cx + 4 - 7, cy, cz + 8), advStruct);

        // z=9: steel(x=0), AIR(x=1,2), slab(x=3..5), AIR(x=6,7), steel(x=8)
        world.setBlockState(new BlockPos(cx + 4 - 0, cy, cz + 9), steel);
        world.setBlockState(new BlockPos(cx + 4 - 3, cy, cz + 9), slab);
        world.setBlockState(new BlockPos(cx + 4 - 4, cy, cz + 9), slab);
        world.setBlockState(new BlockPos(cx + 4 - 5, cy, cz + 9), slab);
        world.setBlockState(new BlockPos(cx + 4 - 8, cy, cz + 9), steel);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{cx, cy, cz});
        info.put("motorPos",      new int[]{cx, cy, cz + 5});
        send(sender, jsonMap(info));
    }

    /**
     * Builds a complete microwave-receiver multiblock with controller at
     * (cx, cy, cz) NORTH-facing. Per {@code TileMicrowaveReciever.structure}
     * — a single layer 5×5 with controller 'c' at structure[0][2][2]
     * (offset x=2, y=0, z=2). For a NORTH-facing controller the position
     * formula simplifies to:
     * <pre>
     *   globalX = cx + 2 - x
     *   globalY = cy
     *   globalZ = cz + z - 2
     * </pre>
     *
     * <p>The structure references {@code BlockMeta(blockSolarPanel)} at most
     * cells, with {@code '*'} wildcards on a few cells (Microwave's
     * {@code getAllowableWildCardBlocks} permits item-input hatches,
     * power-output plugs, and the solar-panel block itself at wildcards).
     * The fixture places {@code blockSolarPanel} at all non-controller cells
     * — this satisfies both the literal-block cells and the wildcard
     * (since solarPanel is in the wildcard list).</p>
     */
    private void handleFixtureMicrowaveReceiver(MinecraftServer server, ICommandSender sender,
                                                 int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "microwaveReciever"));
        net.minecraft.block.Block solarPanel =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "solarPanel"));

        if (controller == null || solarPanel == null) {
            send(sender, "{\"error\":\"missing block(s)\""
                    + ",\"controller\":" + (controller != null)
                    + ",\"solarPanel\":" + (solarPanel != null) + "}");
            return;
        }

        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — fall back to default.
        }

        net.minecraft.block.state.IBlockState panel = solarPanel.getDefaultState();

        // Fill 5×5 with solar panels, controller at the centre.
        for (int z = 0; z <= 4; z++) {
            for (int x = 0; x <= 4; x++) {
                BlockPos p = new BlockPos(cx + 2 - x, cy, cz + z - 2);
                world.setBlockState(p, (z == 2 && x == 2) ? controllerState : panel);
            }
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{cx, cy, cz});
        info.put("nwCornerPos",   new int[]{cx + 2, cy, cz - 2});
        send(sender, jsonMap(info));
    }

    /**
     * Builds a complete solar-array multiblock with controller at
     * (cx, cy, cz) NORTH-facing. Per {@code TileSolarArray.structure} — a
     * 22-row × 3-wide single-layer array with controller 'c' at
     * structure[0][0][1] (offset x=1, y=0, z=0). The wildcard '*' accepts
     * {@code blockSolarArrayPanel} OR {@code Blocks.AIR} (per Solar's
     * {@code getAllowableWildCardBlocks}), so pre-clearing the footprint
     * to air and placing only the controller + 2 power-output plugs
     * satisfies the validator.
     *
     * <p>For a NORTH-facing controller the position formula simplifies to:</p>
     * <pre>
     *   globalX = cx + 1 - x
     *   globalY = cy
     *   globalZ = cz + z
     * </pre>
     *
     * <p>Concrete placements (3 cells total):</p>
     * <ul>
     *   <li>z=0, x=0: 'p' (forge power output) at globalX = cx + 1</li>
     *   <li>z=0, x=1: 'c' controller at globalX = cx</li>
     *   <li>z=0, x=2: 'p' at globalX = cx - 1</li>
     *   <li>z=1..21: cleared to AIR (satisfies the '*' wildcard).</li>
     * </ul>
     */
    private void handleFixtureSolarArray(MinecraftServer server, ICommandSender sender,
                                          int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "solararray"));
        net.minecraft.block.Block powerOutput =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "forgepoweroutput"));
        net.minecraft.block.Block solarArrayPanel =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "solararraypanel"));

        if (controller == null || powerOutput == null || solarArrayPanel == null) {
            send(sender, "{\"error\":\"missing block(s)\""
                    + ",\"controller\":" + (controller != null)
                    + ",\"powerOutput\":" + (powerOutput != null)
                    + ",\"solarArrayPanel\":" + (solarArrayPanel != null) + "}");
            return;
        }

        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — fall back to default.
        }

        net.minecraft.block.state.IBlockState plug = powerOutput.getDefaultState();
        net.minecraft.block.state.IBlockState panel = solarArrayPanel.getDefaultState();

        // Pre-clear the 3-wide × 22-deep footprint to air, then place panels
        // in rows z=1..21 (the wildcard accepts panel OR air, but explicit
        // panels are immune to terrain interaction at sea level).
        for (int gx = cx - 1; gx <= cx + 1; gx++) {
            for (int gz = cz; gz <= cz + 21; gz++) {
                world.setBlockToAir(new BlockPos(gx, cy, gz));
            }
        }
        for (int gx = cx - 1; gx <= cx + 1; gx++) {
            for (int gz = cz + 1; gz <= cz + 21; gz++) {
                world.setBlockState(new BlockPos(gx, cy, gz), panel);
            }
        }

        // Row z=0: 'p', 'c', 'p'.
        world.setBlockState(new BlockPos(cx + 1, cy, cz), plug);
        world.setBlockState(new BlockPos(cx,     cy, cz), controllerState);
        world.setBlockState(new BlockPos(cx - 1, cy, cz), plug);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{cx, cy, cz});
        info.put("controllerBlock", controller.getRegistryName().toString());
        send(sender, jsonMap(info));
    }

    /**
     * Builds a minimal UV-assembler fixture satisfying
     * {@code TileUnmannedVehicleAssembler.getRocketPadBounds} (which uses a
     * different geometry from {@link
     * zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine#getRocketPadBounds}):
     *
     * <ul>
     *   <li>{@code deployableRocketBuilder} controller at (cx, cy, cz),
     *       NORTH-facing.</li>
     *   <li>{@code structureTower} column UP from builder, 6 tall →
     *       {@code yMax = 6} (well under UV's {@code MAX_SIZE_Y = 17}).</li>
     *   <li>{@code structureTower} row SOUTH at the top of the column
     *       (cx, cy+6, cz+1..cz+3) → {@code zSize = 3}.</li>
     *   <li>{@code structureTower} row WEST + EAST at builder Y
     *       (cx-2..cx-1, cy, cz) + (cx+1..cx+2, cy, cz) → {@code xSize = 5}.</li>
     *   <li>Rocket components (engines + fuel tanks + guidance + seat)
     *       placed inside the resulting BB
     *       (cx-2..cx+2, cy..cy+5, cz+1..cz+4).</li>
     * </ul>
     *
     * <p>Returns the builder pos so the test can call
     * {@code artest rocket assemble} on the controller (which polymorphically
     * fires UV's {@code assembleRocket} → spawns {@code EntityStationDeployedRocket}).</p>
     */
    private void handleFixtureUvRocket(MinecraftServer server, ICommandSender sender,
                                       int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        net.minecraft.block.Block uvBuilder = ForgeRegistries.BLOCKS
                .getValue(new ResourceLocation("advancedrocketry", "deployableRocketBuilder"));
        net.minecraft.block.Block structureTower = ForgeRegistries.BLOCKS
                .getValue(new ResourceLocation("advancedrocketry", "structureTower"));
        net.minecraft.block.Block advEngine = ForgeRegistries.BLOCKS
                .getValue(new ResourceLocation("advancedrocketry", "advRocketmotor"));
        net.minecraft.block.Block fuelTank = ForgeRegistries.BLOCKS
                .getValue(new ResourceLocation("advancedrocketry", "fuelTank"));
        net.minecraft.block.Block guidanceComputer = ForgeRegistries.BLOCKS
                .getValue(new ResourceLocation("advancedrocketry", "guidanceComputer"));
        net.minecraft.block.Block seat = ForgeRegistries.BLOCKS
                .getValue(new ResourceLocation("advancedrocketry", "seat"));
        if (uvBuilder == null || structureTower == null || advEngine == null
                || fuelTank == null || guidanceComputer == null || seat == null) {
            send(sender, "{\"error\":\"missing AR block(s) for UV fixture\"}");
            return;
        }
        ensureChunkAreaLoaded(world, cx, cz, 1);

        // Pre-clear the volume around the fixture (similar to rocket fixture
        // hygiene — terrain in the way would inflate scanRocket counts).
        for (int gx = cx - 3; gx <= cx + 3; gx++) {
            for (int gy = cy; gy <= cy + 7; gy++) {
                for (int gz = cz - 1; gz <= cz + 5; gz++) {
                    world.setBlockToAir(new BlockPos(gx, gy, gz));
                }
            }
        }

        // Builder NORTH-facing.
        net.minecraft.block.state.IBlockState builderState = uvBuilder.getDefaultState();
        try {
            builderState = builderState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — keep default state.
        }
        world.setBlockState(new BlockPos(cx, cy, cz), builderState);

        // structureTower column directly above builder (cy+1..cy+6).
        net.minecraft.block.state.IBlockState towerState = structureTower.getDefaultState();
        for (int dy = 1; dy <= 6; dy++) {
            world.setBlockState(new BlockPos(cx, cy + dy, cz), towerState);
        }
        // structureTower top-south row (cy+6, cz+1..cz+3).
        for (int dz = 1; dz <= 3; dz++) {
            world.setBlockState(new BlockPos(cx, cy + 6, cz + dz), towerState);
        }
        // structureTower west/east at builder Y.
        for (int dx = 1; dx <= 2; dx++) {
            world.setBlockState(new BlockPos(cx - dx, cy, cz), towerState);
            world.setBlockState(new BlockPos(cx + dx, cy, cz), towerState);
        }

        // Rocket components inside the BB (cx-2..cx+2, cy..cy+5, cz+1..cz+4).
        // Engines (bottom row, two of them on either side of the bb center).
        world.setBlockState(new BlockPos(cx - 1, cy + 1, cz + 1), advEngine.getDefaultState());
        world.setBlockState(new BlockPos(cx + 1, cy + 1, cz + 1), advEngine.getDefaultState());
        // Fuel tanks: 3 wide × 2 tall column inside the bb.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 2; dy <= 3; dy++) {
                world.setBlockState(new BlockPos(cx + dx, cy + dy, cz + 1),
                        fuelTank.getDefaultState());
            }
        }
        // Guidance computer.
        world.setBlockState(new BlockPos(cx, cy + 4, cz + 1), guidanceComputer.getDefaultState());
        // Seat.
        world.setBlockState(new BlockPos(cx, cy + 5, cz + 1), seat.getDefaultState());

        send(sender, "{\"ok\":true,\"builderPos\":["
                + cx + "," + cy + "," + cz + "]"
                + ",\"expectedBbMinX\":" + (cx - 2)
                + ",\"expectedBbMaxX\":" + (cx + 2)
                + ",\"expectedBbMinY\":" + cy
                + ",\"expectedBbMaxY\":" + (cy + 5)
                + ",\"expectedBbMinZ\":" + (cz + 1)
                + ",\"expectedBbMaxZ\":" + (cz + 4) + "}");
    }

    /**
     * Builds a complete black-hole-generator multiblock with controller at
     * (cx, cy, cz) NORTH-facing. The production {@code TileBlackHoleGenerator
     * .structure} is a 5×3×3 array iterated [y][z][x] with controller offset
     * (x=1, y=1, z=0). Translating to world coords for a NORTH-facing
     * controller (frontZ=-1) simplifies to:
     * <pre>
     *   globalX = cx - (x - 1)
     *   globalY = cy - y + 1
     *   globalZ = cz + z
     * </pre>
     *
     * <p>Concrete placements (10 cells — layer y=2 has TWO advStructure
     * blocks at z=0 AND z=1, not one):</p>
     * <ul>
     *   <li>{@code (cx, cy+1, cz+1)} — advStructureBlock (top cap, y=0)</li>
     *   <li>{@code (cx, cy,   cz)}   — controller (y=1, z=0)</li>
     *   <li>{@code (cx, cy,   cz+1)} — advStructureBlock (centre, y=1, z=1)</li>
     *   <li>{@code (cx+1, cy, cz+1)} — power-output plug ('*' at y=1, z=1, x=0;
     *       provides the energy-capability access point)</li>
     *   <li>{@code (cx-1, cy, cz+1)} — item-input hatch ('*' at y=1, z=1, x=2;
     *       BHG consumes "fuel" through I)</li>
     *   <li>{@code (cx, cy,   cz+2)} — advStructureBlock ('*' at y=1, z=2, x=1)</li>
     *   <li>{@code (cx, cy-1, cz)}   — advStructureBlock (lower-1 front, y=2, z=0) ← easy to miss</li>
     *   <li>{@code (cx, cy-1, cz+1)} — advStructureBlock (lower-1 mid, y=2, z=1)</li>
     *   <li>{@code (cx, cy-2, cz+1)} — advStructureBlock (lower-2, y=3, z=1)</li>
     *   <li>{@code (cx, cy-3, cz+1)} — advStructureBlock (lower-3, y=4, z=1)</li>
     * </ul>
     *
     * <p>BHG's {@code getAllowableWildCardBlocks} permits 'I' / 'p' / the
     * advStructureBlock itself at '*' positions, so the chosen mix forms
     * a valid structure that production {@code attemptCompleteStructure}
     * accepts.</p>
     */
    private void handleFixtureBlackHoleGenerator(MinecraftServer server, ICommandSender sender,
                                                  int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "blackholegenerator"));
        // libVulpes blocks. Registry names are derived from setUnlocalizedName
        // substring(5), so case follows the production unlocalized-name string.
        // libVulpes registry names are derived from setUnlocalizedName.substring(5)
        // and lowercased by Forge ResourceLocation in 1.12.2.
        net.minecraft.block.Block advStructure =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "advstructuremachine"));
        net.minecraft.block.Block hatch =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "hatch"));
        net.minecraft.block.Block powerOutput =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "forgepoweroutput"));

        if (controller == null || advStructure == null || hatch == null || powerOutput == null) {
            send(sender, "{\"error\":\"missing block(s)\",\"controller\":"
                    + (controller != null) + ",\"advStructure\":" + (advStructure != null)
                    + ",\"hatch\":" + (hatch != null) + ",\"powerOutput\":" + (powerOutput != null) + "}");
            return;
        }

        // Controller NORTH-facing.
        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — fall back to default state.
        }

        BlockPos controllerPos = new BlockPos(cx, cy, cz);
        BlockPos topCap        = new BlockPos(cx,     cy + 1, cz + 1);
        BlockPos centre        = new BlockPos(cx,     cy,     cz + 1);
        BlockPos lower1Front   = new BlockPos(cx,     cy - 1, cz);      // y=2 z=0
        BlockPos lower1Mid     = new BlockPos(cx,     cy - 1, cz + 1);  // y=2 z=1
        BlockPos lower2        = new BlockPos(cx,     cy - 2, cz + 1);
        BlockPos lower3        = new BlockPos(cx,     cy - 3, cz + 1);
        BlockPos powerOutPos   = new BlockPos(cx + 1, cy,     cz + 1);
        BlockPos itemInputPos  = new BlockPos(cx - 1, cy,     cz + 1);
        BlockPos backFiller    = new BlockPos(cx,     cy,     cz + 2);

        world.setBlockState(controllerPos, controllerState);
        world.setBlockState(topCap, advStructure.getDefaultState());
        world.setBlockState(centre, advStructure.getDefaultState());
        world.setBlockState(lower1Front, advStructure.getDefaultState());
        world.setBlockState(lower1Mid, advStructure.getDefaultState());
        world.setBlockState(lower2, advStructure.getDefaultState());
        world.setBlockState(lower3, advStructure.getDefaultState());
        @SuppressWarnings("deprecation") net.minecraft.block.state.IBlockState itemInputState =
                hatch.getStateFromMeta(0);  // meta 0 = TileInputHatch
        world.setBlockState(itemInputPos, itemInputState);
        world.setBlockState(powerOutPos, powerOutput.getDefaultState());
        world.setBlockState(backFiller, advStructure.getDefaultState());

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()});
        info.put("powerOutPos",   new int[]{powerOutPos.getX(),   powerOutPos.getY(),   powerOutPos.getZ()});
        info.put("itemInputPos",  new int[]{itemInputPos.getX(),  itemInputPos.getY(),  itemInputPos.getZ()});
        send(sender, jsonMap(info));
    }

    /**
     * Builds a complete cutting-machine multiblock at (x,y,z), controller
     * NORTH-facing. Per {@link zmaster587.advancedRocketry.tile.multiblock.machine.TileCuttingMachine#getStructure()}
     * the layout (relative to NORTH-facing controller) is:
     * <pre>
     *   z+0:  inputHatch  controller  outputHatch    (cx+1, cx, cx-1)
     *   z+1:  motor       sawBlade    powerHatch
     * </pre>
     * Returns positions of all six placed blocks so the test can probe them.
     */
    private void handleFixtureCuttingMachine(MinecraftServer server, ICommandSender sender,
                                              int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "cuttingMachine"));
        net.minecraft.block.Block sawBlade =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("advancedrocketry", "sawBlade"));
        net.minecraft.block.Block motor =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "motor"));
        net.minecraft.block.Block hatch =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "hatch"));
        net.minecraft.block.Block powerInput =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("libvulpes", "forgepowerinput"));

        if (controller == null || sawBlade == null || motor == null
                || hatch == null || powerInput == null) {
            send(sender, "{\"error\":\"missing block(s)\",\"controller\":"
                    + (controller != null) + ",\"sawBlade\":" + (sawBlade != null)
                    + ",\"motor\":" + (motor != null) + ",\"hatch\":" + (hatch != null)
                    + ",\"powerInput\":" + (powerInput != null) + "}");
            return;
        }

        // Controller NORTH-facing.
        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // Property absent — fall back to default state.
        }

        BlockPos controllerPos = new BlockPos(cx, cy, cz);
        BlockPos inputPos = new BlockPos(cx + 1, cy, cz);
        BlockPos outputPos = new BlockPos(cx - 1, cy, cz);
        BlockPos motorPos = new BlockPos(cx + 1, cy, cz + 1);
        BlockPos sawBladePos = new BlockPos(cx, cy, cz + 1);
        BlockPos powerPos = new BlockPos(cx - 1, cy, cz + 1);

        world.setBlockState(controllerPos, controllerState);
        @SuppressWarnings("deprecation") net.minecraft.block.state.IBlockState inputState =
                hatch.getStateFromMeta(0);   // meta 0 = TileInputHatch
        @SuppressWarnings("deprecation") net.minecraft.block.state.IBlockState outputState =
                hatch.getStateFromMeta(1);   // meta 1 = TileOutputHatch
        world.setBlockState(inputPos, inputState);
        world.setBlockState(outputPos, outputState);
        world.setBlockState(motorPos, motor.getDefaultState());
        world.setBlockState(sawBladePos, sawBlade.getDefaultState());
        world.setBlockState(powerPos, powerInput.getDefaultState());

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()});
        info.put("inputPos",      new int[]{inputPos.getX(),      inputPos.getY(),      inputPos.getZ()});
        info.put("outputPos",     new int[]{outputPos.getX(),     outputPos.getY(),     outputPos.getZ()});
        info.put("motorPos",      new int[]{motorPos.getX(),      motorPos.getY(),      motorPos.getZ()});
        info.put("sawBladePos",   new int[]{sawBladePos.getX(),   sawBladePos.getY(),   sawBladePos.getZ()});
        info.put("powerPos",      new int[]{powerPos.getX(),      powerPos.getY(),      powerPos.getZ()});
        send(sender, jsonMap(info));
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
                    "energy", "infra", "place", "fill", "fixture", "tile", "hatch", "selector");
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

    /**
     * Looks up the first non-empty OreDictionary entry registered under
     * {@code oreName} and returns the matching {@code IBlockState} (block +
     * meta). Used to resolve {@code "coilCopper"}, {@code "blockSteel"},
     * {@code "blockTitanium"}, {@code "slab"} etc. — names backing the
     * libVulpes structure validator's String entries that resolve via
     * {@link net.minecraftforge.oredict.OreDictionary}. Returns {@code null}
     * if no entry is registered (e.g. mod-compat dependency missing).
     */
    private static net.minecraft.block.state.IBlockState firstOreDictBlockState(String oreName) {
        java.util.List<net.minecraft.item.ItemStack> stacks =
                net.minecraftforge.oredict.OreDictionary.getOres(oreName);
        if (stacks == null || stacks.isEmpty()) return null;
        net.minecraft.item.ItemStack stack = stacks.get(0);
        if (stack.isEmpty()) return null;
        net.minecraft.block.Block block = net.minecraft.block.Block.getBlockFromItem(stack.getItem());
        if (block == null || block == net.minecraft.init.Blocks.AIR) return null;
        int meta = stack.getItem().getMetadata(stack.getItemDamage());
        @SuppressWarnings("deprecation")
        net.minecraft.block.state.IBlockState state = block.getStateFromMeta(meta);
        return state;
    }

    /**
     * Resolves a single structure-array cell to an {@code IBlockState} for
     * placement, mirroring libVulpes' {@code TileMultiBlock.getAllowableBlocks}
     * but choosing a concrete representative from each accepted set. Returns
     * {@code null} for an unresolved cell (e.g. unknown char mapping or an
     * empty OreDictionary lookup). Handles:
     * <ul>
     *   <li>{@code null} → {@code null} (caller skips).</li>
     *   <li>{@code Blocks.AIR} → AIR state (caller may pre-clear instead).</li>
     *   <li>{@code Block} instance → {@code getDefaultState}.</li>
     *   <li>{@code BlockMeta(block, meta)} → {@code block.getStateFromMeta(meta)}.</li>
     *   <li>{@code Block[]} → first element's default state.</li>
     *   <li>{@code String} → {@link #firstOreDictBlockState}.</li>
     *   <li>{@code Character 'c'} → caller-supplied {@code controllerState}.</li>
     *   <li>{@code Character} in libVulpes/AR charMapping
     *       ({@code 'I','O','P','p','L','l','D'}) → first {@code BlockMeta}
     *       from the mapping (which is the canonical Forge variant).</li>
     * </ul>
     */
    @SuppressWarnings("deprecation")
    /**
     * Lookup table: kebab-case machine key → {controller namespace,
     * controller registry path, tile-class FQN}. Used by
     * {@code /artest fixture machine <key>} dispatch. All 9 multiblock
     * industrial machines use the libVulpes character mappings
     * 'c'/'I'/'O'/'P'/'L'/'l' in their {@code structure} arrays, so the
     * shared {@link #handleFixtureGenericFromStructure} helper can
     * build the fixture for all of them — only the per-machine
     * controller block and tile-class identity differ.
     */
    private static String[] lookupMultiblockMachineSpec(String key) {
        switch (key.toLowerCase()) {
            case "rolling-machine":
                return new String[]{"advancedrocketry", "rollingMachine",
                        "zmaster587.advancedRocketry.tile.multiblock.machine.TileRollingMachine"};
            case "lathe":
                return new String[]{"advancedrocketry", "lathe",
                        "zmaster587.advancedRocketry.tile.multiblock.machine.TileLathe"};
            case "precision-assembler":
                return new String[]{"advancedrocketry", "precisionassemblingmachine",
                        "zmaster587.advancedRocketry.tile.multiblock.machine.TilePrecisionAssembler"};
            case "electrolyser":
                return new String[]{"advancedrocketry", "electrolyser",
                        "zmaster587.advancedRocketry.tile.multiblock.machine.TileElectrolyser"};
            case "chemical-reactor":
                return new String[]{"advancedrocketry", "chemicalReactor",
                        "zmaster587.advancedRocketry.tile.multiblock.machine.TileChemicalReactor"};
            case "crystallizer":
                return new String[]{"advancedrocketry", "crystallizer",
                        "zmaster587.advancedRocketry.tile.multiblock.machine.TileCrystallizer"};
            case "arc-furnace":
                return new String[]{"advancedrocketry", "arcfurnace",
                        "zmaster587.advancedRocketry.tile.multiblock.machine.TileElectricArcFurnace"};
            case "centrifuge":
                return new String[]{"advancedrocketry", "centrifuge",
                        "zmaster587.advancedRocketry.tile.multiblock.machine.TileCentrifuge"};
            case "precision-laser-etcher":
                return new String[]{"advancedrocketry", "precisionlaseretcher",
                        "zmaster587.advancedRocketry.tile.multiblock.machine.TilePrecisionLaserEtcher"};
            default:
                return null;
        }
    }

    /**
     * TASK-26 — per-machine hatch overlay for wildcard-structure machines.
     *
     * <p>{@link TileElectricArcFurnace} and {@link TilePrecisionAssembler}
     * declare their hatch slots via {@code '*'} wildcards instead of explicit
     * {@code 'I'}/{@code 'O'}/{@code 'P'} chars, so the generic fixture
     * helper's structure scan can't compute hatch positions.</p>
     *
     * <p>The returned {@link WildcardConfig} carries (a) the chosen hatch
     * overlays — each overlays a wildcard cell with a libVulpes hatch via
     * the {@link #resolveStructureCell} char mapping; and (b) a "filler"
     * block to place at any remaining wildcard cell that the machine's
     * {@code getAllowableWildCardBlocks()} accepts but which isn't a hatch
     * (otherwise the wildcard cell stays AIR and validation fails because
     * AIR is not in the allowable list).</p>
     *
     * <p>Returns {@code null} for machines whose structure has explicit
     * hatch chars — those go through the regular scan-based path.</p>
     */
    private static WildcardConfig lookupWildcardMachineOverrides(String key) {
        switch (key.toLowerCase()) {
            case "arc-furnace":
                // Structure has three explicit 'P' chars at y=0 already, so
                // only 'I' and 'O' need overlay. Both placed on the base
                // wildcard ring at y=3 z=4 (back row opposite controller).
                // Controller 'c' is at structure[3][0][2]. Filler =
                // blockBlastBrick (the structure block listed in
                // TileElectricArcFurnace.getAllowableWildCardBlocks).
                return new WildcardConfig(
                        zmaster587.advancedRocketry.api.AdvancedRocketryBlocks.blockBlastBrick,
                        new HatchOverride('I', 3, 4, 1),
                        new HatchOverride('O', 3, 4, 3));
            case "precision-assembler":
                // Structure has NO explicit hatch chars; overlay 'I'/'O'/'P'
                // onto the three front-row wildcards on the bottom layer
                // (structure[2][0][1..3]). Controller 'c' is at
                // structure[2][0][0]. Filler = libVulpes blockStructureBlock
                // (added with WILDCARD meta in
                // TilePrecisionAssembler.getAllowableWildCardBlocks).
                return new WildcardConfig(
                        zmaster587.libVulpes.api.LibVulpesBlocks.blockStructureBlock,
                        new HatchOverride('I', 2, 0, 1),
                        new HatchOverride('O', 2, 0, 2),
                        new HatchOverride('P', 2, 0, 3));
            default:
                return null;
        }
    }

    /** Override entry — pins a libVulpes hatch char
     *  ({@code 'I'}/{@code 'O'}/{@code 'P'}/...) to a specific structure-space
     *  cell ({@code y}, {@code z}, {@code x}). Block placement is via
     *  {@link #resolveStructureCell} on the char. */
    private static final class HatchOverride {
        final char role;
        final int y, z, x;
        HatchOverride(char role, int y, int z, int x) {
            this.role = role; this.y = y; this.z = z; this.x = x;
        }
    }

    /** Wildcard machine configuration — hatch overrides plus the structure-
     *  block to use as filler for every other {@code '*'} cell. */
    private static final class WildcardConfig {
        final HatchOverride[] hatches;
        final net.minecraft.block.Block filler;
        WildcardConfig(net.minecraft.block.Block filler, HatchOverride... hatches) {
            this.filler = filler;
            this.hatches = hatches;
        }
    }

    /** Pack a structure-space (y, z, x) cell into a 64-bit key for
     *  {@link java.util.HashMap}-based lookup. Each axis fits in 20 bits
     *  (max structure dim observed in this repo is ~30). */
    private static long packCell(int y, int z, int x) {
        return ((long)(y & 0xFFFFF) << 40) | ((long)(z & 0xFFFFF) << 20) | (x & 0xFFFFF);
    }

    /**
     * TASK-25 — builds the 3-block PlatePress stack at the requested press
     * position: obsidian at y-2, the first registered recipe's ingredient
     * block at y-1, PlatePress at y (FACING=DOWN, EXTENDED=false).
     *
     * <p>The press's activation contract requires obsidian as the BASE and
     * a recognised recipe-ingredient block in the MIDDLE
     * (see {@link zmaster587.advancedRocketry.block.BlockSmallPlatePress#getRecipe}).
     * If the first recipe's ingredient alternatives aren't a placeable
     * block, the response includes an error.</p>
     */
    private void handleFixturePlatePress(MinecraftServer server, ICommandSender sender,
            int dim, int cx, int cy, int cz) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        net.minecraft.block.Block pressBlock = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation("advancedrocketry", "platepress"));
        if (pressBlock == null) {
            send(sender, "{\"error\":\"missing block 'advancedrocketry:platepress'\"}");
            return;
        }

        // Resolve first recipe + first ingredient block.
        net.minecraft.item.ItemStack ingredientStack;
        net.minecraft.item.ItemStack outputStack;
        net.minecraft.block.Block ingredientBlock;
        try {
            Class<?> pressClass = Class.forName(
                    "zmaster587.advancedRocketry.block.BlockSmallPlatePress");
            Class<?> recipesMachineClass = Class.forName("zmaster587.libVulpes.recipe.RecipesMachine");
            Object instance = recipesMachineClass.getMethod("getInstance").invoke(null);
            java.util.List<?> recipes = (java.util.List<?>) recipesMachineClass
                    .getMethod("getRecipes", Class.class).invoke(instance, pressClass);
            if (recipes == null || recipes.isEmpty()) {
                send(sender, "{\"error\":\"no recipes registered for BlockSmallPlatePress\"}");
                return;
            }
            Object recipe = recipes.get(0);
            Class<?> recipeClass = recipe.getClass();
            java.util.List<?> ingredients = (java.util.List<?>) recipeClass.getMethod("getIngredients").invoke(recipe);
            java.util.List<?> outputs = (java.util.List<?>) recipeClass.getMethod("getOutput").invoke(recipe);
            if (ingredients.isEmpty()) {
                send(sender, "{\"error\":\"first recipe has no ingredients\"}");
                return;
            }
            java.util.List<?> alts = (java.util.List<?>) ingredients.get(0);
            if (alts == null || alts.isEmpty()) {
                send(sender, "{\"error\":\"first recipe ingredient has no alternatives\"}");
                return;
            }
            ingredientStack = (net.minecraft.item.ItemStack) alts.get(0);
            outputStack = outputs.isEmpty()
                    ? net.minecraft.item.ItemStack.EMPTY
                    : (net.minecraft.item.ItemStack) outputs.get(0);
            ingredientBlock = net.minecraft.block.Block.getBlockFromItem(ingredientStack.getItem());
            if (ingredientBlock == net.minecraft.init.Blocks.AIR) {
                send(sender, "{\"error\":\"first ingredient is not a placeable block\",\"item\":\""
                        + escapeJson(ingredientStack.getItem().getRegistryName().toString()) + "\"}");
                return;
            }
        } catch (ReflectiveOperationException re) {
            send(sender, "{\"error\":\"reflection failed loading PlatePress recipe\",\"msg\":\""
                    + escapeJson(re.getMessage()) + "\"}");
            return;
        }

        BlockPos pressPos      = new BlockPos(cx, cy, cz);
        BlockPos ingredientPos = new BlockPos(cx, cy - 1, cz);
        BlockPos obsidianPos   = new BlockPos(cx, cy - 2, cz);
        // Pre-clear a 3-block-tall column + a 1-block redstone slot around
        // the press so neighbouring leftovers from prior tests don't
        // pre-power the press or block the ingredient placement.
        for (int dy = -2; dy <= 1; dy++) {
            world.setBlockToAir(new BlockPos(cx, cy + dy, cz));
        }
        for (net.minecraft.util.EnumFacing dir : net.minecraft.util.EnumFacing.HORIZONTALS) {
            world.setBlockToAir(pressPos.offset(dir));
        }

        world.setBlockState(obsidianPos, net.minecraft.init.Blocks.OBSIDIAN.getDefaultState());
        @SuppressWarnings("deprecation")
        net.minecraft.block.state.IBlockState ingredientState =
                ingredientBlock.getStateFromMeta(ingredientStack.getMetadata());
        world.setBlockState(ingredientPos, ingredientState);
        net.minecraft.block.state.IBlockState pressState = pressBlock.getDefaultState();
        try {
            pressState = pressState
                    .withProperty(net.minecraft.block.BlockPistonBase.FACING,
                            net.minecraft.util.EnumFacing.DOWN)
                    .withProperty(net.minecraft.block.BlockPistonBase.EXTENDED,
                            Boolean.FALSE);
        } catch (IllegalArgumentException ignored) {
            // Unexpected — but if FACING/EXTENDED aren't on the state, fall
            // back to default. PlatePress declares both.
        }
        world.setBlockState(pressPos, pressState);

        net.minecraft.util.ResourceLocation outputId = outputStack.isEmpty()
                ? null : outputStack.getItem().getRegistryName();
        net.minecraft.util.ResourceLocation ingredientItemId = ingredientStack.getItem().getRegistryName();
        net.minecraft.util.ResourceLocation ingredientBlockId = ingredientBlock.getRegistryName();
        send(sender, "{\"ok\":true"
                + ",\"pressPos\":[" + cx + "," + cy + "," + cz + "]"
                + ",\"ingredientPos\":[" + cx + "," + (cy - 1) + "," + cz + "]"
                + ",\"obsidianPos\":[" + cx + "," + (cy - 2) + "," + cz + "]"
                + ",\"ingredientItem\":\"" + (ingredientItemId == null ? "null" : ingredientItemId.toString()) + "\""
                + ",\"ingredientBlock\":\"" + (ingredientBlockId == null ? "null" : ingredientBlockId.toString()) + "\""
                + ",\"ingredientMeta\":" + ingredientStack.getMetadata()
                + ",\"outputItem\":\"" + (outputId == null ? "null" : outputId.toString()) + "\""
                + ",\"outputCount\":" + outputStack.getCount()
                + ",\"outputMeta\":" + outputStack.getMetadata()
                + "}");
    }

    private static net.minecraft.block.state.IBlockState resolveStructureCell(Object cell,
            net.minecraft.block.state.IBlockState controllerState) {
        if (cell == null) return null;

        if (cell instanceof Character) {
            char c = (Character) cell;
            if (c == 'c') return controllerState;
            if (c == '*') return null;  // wildcard — caller's responsibility
            java.util.List<zmaster587.libVulpes.block.BlockMeta> mapping =
                    zmaster587.libVulpes.tile.multiblock.TileMultiBlock.getMapping(c);
            if (mapping == null || mapping.isEmpty()) return null;
            zmaster587.libVulpes.block.BlockMeta bm = mapping.get(0);
            net.minecraft.block.Block block = bm.getBlock();
            int meta = bm.getMeta();
            return block.getStateFromMeta(meta);
        }
        if (cell instanceof net.minecraft.block.Block) {
            net.minecraft.block.Block block = (net.minecraft.block.Block) cell;
            return block.getDefaultState();
        }
        if (cell instanceof zmaster587.libVulpes.block.BlockMeta) {
            zmaster587.libVulpes.block.BlockMeta bm = (zmaster587.libVulpes.block.BlockMeta) cell;
            int meta = bm.getMeta();
            return bm.getBlock().getStateFromMeta(meta);
        }
        if (cell instanceof net.minecraft.block.Block[]) {
            net.minecraft.block.Block[] arr = (net.minecraft.block.Block[]) cell;
            if (arr.length == 0 || arr[0] == null) return null;
            return arr[0].getDefaultState();
        }
        if (cell instanceof String) {
            return firstOreDictBlockState((String) cell);
        }
        return null;
    }

    /**
     * Generic fixture-builder backed by reflection into a tile class's
     * {@code structure} array. Use for multiblocks whose structure array is
     * large enough that hand-translating every cell is impractical (e.g.
     * {@code TileAtmosphereTerraformer} 17×17, {@code TileOrbitalLaserDrill}
     * sparse 11×9×3).
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Look up the controller block by registry name; assemble its
     *       NORTH-facing state.</li>
     *   <li>Reflectively read the structure array — static field or, if the
     *       field is non-static, construct a new instance via the tile
     *       class's no-arg constructor.</li>
     *   <li>Locate the {@code 'c'} character to derive the controller
     *       offset.</li>
     *   <li>Pre-clear the full bounding box to air, then iterate every cell
     *       and place a concrete representative via
     *       {@link #resolveStructureCell}. Wildcards ({@code '*'}) are left
     *       at air — fixtures using this helper must either have empty
     *       wildcards or accept AIR.</li>
     *   <li>If {@code overrides} is non-null, each entry overwrites a
     *       chosen wildcard cell with a concrete libVulpes hatch block
     *       and the resulting world position is added to the response's
     *       hatch-position lists. Used by wildcard-structure machines
     *       (TASK-26) — see {@link #lookupWildcardMachineOverrides}.</li>
     * </ol>
     */
    private void handleFixtureGenericFromStructure(MinecraftServer server, ICommandSender sender,
            int dim, int cx, int cy, int cz,
            String controllerNamespace, String controllerPath,
            String tileClassName, String structureFieldName,
            WildcardConfig wildcardConfig) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        // TASK-28 F1 — pre-load the controller chunk + its 8 neighbours so
        // attemptCompleteStructure's per-cell block-match scan doesn't race
        // chunk loading on multiblocks that straddle a chunk boundary
        // (PrecisionLaserEtcher / ArcFurnace observed flaking through the
        // existing 8×500 ms retry budget without this pre-load).
        ensureChunkAreaLoaded(world, cx, cz, 1);

        net.minecraft.block.Block controller =
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation(controllerNamespace, controllerPath));
        if (controller == null) {
            send(sender, "{\"error\":\"missing controller block\",\"id\":\""
                    + controllerNamespace + ":" + controllerPath + "\"}");
            return;
        }

        net.minecraft.block.state.IBlockState controllerState = controller.getDefaultState();
        try {
            controllerState = controllerState.withProperty(
                    zmaster587.libVulpes.block.RotatableBlock.FACING,
                    net.minecraft.util.EnumFacing.NORTH);
        } catch (IllegalArgumentException ignored) {
            // FACING absent (e.g. fully-rotatable variants) — keep default.
        }

        Object[][][] structure;
        try {
            Class<?> tileClass = Class.forName(tileClassName);
            java.lang.reflect.Field field = tileClass.getDeclaredField(structureFieldName);
            field.setAccessible(true);
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                structure = (Object[][][]) field.get(null);
            } else {
                Object instance = tileClass.getConstructor().newInstance();
                structure = (Object[][][]) field.get(instance);
            }
        } catch (ReflectiveOperationException e) {
            send(sender, "{\"error\":\"reflection failed loading structure\",\"msg\":\""
                    + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage()) + "\"}");
            return;
        }
        if (structure == null || structure.length == 0
                || structure[0].length == 0 || structure[0][0].length == 0) {
            send(sender, "{\"error\":\"empty structure array\"}");
            return;
        }

        // Locate controller offset.
        int ox = -1, oy = -1, oz = -1;
        for (int y = 0; y < structure.length && ox == -1; y++) {
            for (int z = 0; z < structure[0].length && ox == -1; z++) {
                for (int x = 0; x < structure[0][0].length; x++) {
                    Object cell = structure[y][z][x];
                    if (cell instanceof Character && (Character) cell == 'c') {
                        ox = x; oy = y; oz = z;
                        break;
                    }
                }
            }
        }
        if (ox == -1) {
            send(sender, "{\"error\":\"structure has no 'c' controller cell\"}");
            return;
        }

        int dimY = structure.length;
        int dimZ = structure[0].length;
        int dimX = structure[0][0].length;

        // NORTH-facing position formula (frontZ=-1, frontX=0):
        //   globalX = cx + (ox - x)
        //   globalY = cy - y + oy
        //   globalZ = cz + (z - oz)
        int minX = cx + ox - (dimX - 1), maxX = cx + ox;
        int minY = cy - (dimY - 1) + oy, maxY = cy + oy;
        int minZ = cz - oz,              maxZ = cz + (dimZ - 1) - oz;

        // Pre-clear the bounding box to air. Soft cap to keep tests cheap.
        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > 16_384) {
            send(sender, "{\"error\":\"footprint volume too large\",\"volume\":" + volume + ",\"cap\":16384}");
            return;
        }
        for (int gx = minX; gx <= maxX; gx++) {
            for (int gy = minY; gy <= maxY; gy++) {
                for (int gz = minZ; gz <= maxZ; gz++) {
                    world.setBlockToAir(new BlockPos(gx, gy, gz));
                }
            }
        }

        // Place each non-null cell.
        int placed = 0, skipped = 0, unresolved = 0;
        for (int y = 0; y < dimY; y++) {
            for (int z = 0; z < dimZ; z++) {
                for (int x = 0; x < dimX; x++) {
                    Object cell = structure[y][z][x];
                    if (cell == null) continue;
                    int gx = cx + (ox - x);
                    int gy = cy - y + oy;
                    int gz = cz + (z - oz);
                    BlockPos p = new BlockPos(gx, gy, gz);

                    if (cell instanceof net.minecraft.block.Block
                            && cell == net.minecraft.init.Blocks.AIR) {
                        // Already cleared.
                        skipped++;
                        continue;
                    }
                    if (cell instanceof Character && (Character) cell == '*') {
                        // Wildcard left as AIR — callers using this helper
                        // must ensure '*' accepts AIR for the multiblock.
                        skipped++;
                        continue;
                    }

                    net.minecraft.block.state.IBlockState state = resolveStructureCell(cell, controllerState);
                    if (state == null) {
                        unresolved++;
                        continue;
                    }
                    world.setBlockState(p, state);
                    placed++;
                }
            }
        }

        // TASK-26 — for wildcard-structure machines, overlay each '*' cell
        // with either a libVulpes hatch (where the test needs one) or the
        // machine's structure-block (filler) so the validator's
        // getAllowableWildCardBlocks list matches the world state. The
        // placement loop above leaves '*' cells as AIR, which the validator
        // rejects for these machines.
        Map<Character, java.util.List<int[]>> overrideHatchPositions = new LinkedHashMap<>();
        if (wildcardConfig != null) {
            // Build a quick lookup: structure-space cell → hatch role.
            Map<Long, Character> hatchByCell = new java.util.HashMap<>();
            for (HatchOverride ov : wildcardConfig.hatches) {
                hatchByCell.put(packCell(ov.y, ov.z, ov.x), ov.role);
            }

            for (int y = 0; y < dimY; y++) {
                for (int z = 0; z < dimZ; z++) {
                    for (int x = 0; x < dimX; x++) {
                        Object cell = structure[y][z][x];
                        if (!(cell instanceof Character) || (Character) cell != '*') continue;

                        int gx = cx + (ox - x);
                        int gy = cy - y + oy;
                        int gz = cz + (z - oz);
                        BlockPos cellPos = new BlockPos(gx, gy, gz);

                        Character role = hatchByCell.get(packCell(y, z, x));
                        if (role != null) {
                            net.minecraft.block.state.IBlockState hatchState =
                                    resolveStructureCell(role, controllerState);
                            if (hatchState == null) { unresolved++; continue; }
                            world.setBlockState(cellPos, hatchState);
                            overrideHatchPositions
                                    .computeIfAbsent(role, k -> new java.util.ArrayList<>())
                                    .add(new int[]{gx, gy, gz});
                        } else if (wildcardConfig.filler != null) {
                            world.setBlockState(cellPos, wildcardConfig.filler.getDefaultState());
                        } else {
                            // No filler — leave AIR. Validation will likely
                            // fail for this machine, but the caller asked.
                            continue;
                        }
                        placed++;
                    }
                }
            }
        }

        // Scan structure for libVulpes hatch chars and report ALL world
        // positions (some machines have multiple of the same hatch — e.g.
        // ChemicalReactor has two 'L' liquid inputs, ArcFurnace has three
        // 'P' power inputs). Same coord formula as the placement loop above.
        Map<Character, java.util.List<int[]>> hatchPositions = new LinkedHashMap<>();
        for (int y = 0; y < dimY; y++) {
            for (int z = 0; z < dimZ; z++) {
                for (int x = 0; x < dimX; x++) {
                    Object cell = structure[y][z][x];
                    if (!(cell instanceof Character)) continue;
                    char c = (Character) cell;
                    if (c != 'I' && c != 'O' && c != 'P' && c != 'p'
                            && c != 'L' && c != 'l') continue;
                    int gx = cx + (ox - x);
                    int gy = cy - y + oy;
                    int gz = cz + (z - oz);
                    hatchPositions.computeIfAbsent(c, k -> new java.util.ArrayList<>())
                            .add(new int[]{gx, gy, gz});
                }
            }
        }

        // Fold override positions into the response's hatch lists. The scan
        // above won't find these (wildcard cells aren't hatch chars in the
        // structure array), so the override placements have to be merged in
        // explicitly.
        for (Map.Entry<Character, java.util.List<int[]>> entry : overrideHatchPositions.entrySet()) {
            hatchPositions
                    .computeIfAbsent(entry.getKey(), k -> new java.util.ArrayList<>())
                    .addAll(entry.getValue());
        }

        StringBuilder out = new StringBuilder("{");
        out.append("\"ok\":true");
        out.append(",\"controllerPos\":").append(jsonArray(new int[]{cx, cy, cz}));
        out.append(",\"dimensions\":").append(jsonArray(new int[]{dimX, dimY, dimZ}));
        out.append(",\"offset\":").append(jsonArray(new int[]{ox, oy, oz}));
        out.append(",\"boundingBox\":").append(jsonArray(new int[]{minX, minY, minZ, maxX, maxY, maxZ}));
        out.append(",\"placed\":").append(placed);
        out.append(",\"skipped\":").append(skipped);
        out.append(",\"unresolved\":").append(unresolved);
        // Emit first-position aliases (back-compat) AND full position lists.
        appendHatchPositions(out, hatchPositions, 'I', "inputPos",        "inputPositions");
        appendHatchPositions(out, hatchPositions, 'O', "outputPos",       "outputPositions");
        appendHatchPositions(out, hatchPositions, 'P', "powerPos",        "powerPositions");
        appendHatchPositions(out, hatchPositions, 'p', "powerOutputPos",  "powerOutputPositions");
        appendHatchPositions(out, hatchPositions, 'L', "liquidInputPos",  "liquidInputPositions");
        appendHatchPositions(out, hatchPositions, 'l', "liquidOutputPos", "liquidOutputPositions");
        out.append('}');
        send(sender, out.toString());
    }

    private static void appendHatchPositions(StringBuilder out,
            Map<Character, java.util.List<int[]>> positions,
            char c, String firstKey, String listKey) {
        java.util.List<int[]> list = positions.get(c);
        if (list == null || list.isEmpty()) return;
        out.append(",\"").append(firstKey).append("\":").append(jsonArray(list.get(0)));
        out.append(",\"").append(listKey).append("\":[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) out.append(',');
            out.append(jsonArray(list.get(i)));
        }
        out.append(']');
    }

    private static String jsonArray(int[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Drills past the per-dimension wrapper to report the inner generator that
     * actually owns chunk generation. For a vanilla dedicated server the chunk
     * provider is {@code ChunkProviderServer} which delegates to an
     * {@code IChunkGenerator}; for AR planets that inner generator is the
     * informative one. Falls back to the wrapper's class name (or "null") when
     * the layout is unexpected.
     */
    private static String chunkGeneratorClassOf(net.minecraft.world.WorldServer world) {
        if (world == null) return "null";
        net.minecraft.world.chunk.IChunkProvider provider = world.getChunkProvider();
        if (provider instanceof net.minecraft.world.gen.ChunkProviderServer) {
            net.minecraft.world.gen.IChunkGenerator inner =
                    ((net.minecraft.world.gen.ChunkProviderServer) provider).chunkGenerator;
            if (inner != null) return inner.getClass().getName();
        }
        return provider != null ? provider.getClass().getName() : "null";
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

    private static void appendItemStackJson(StringBuilder out, net.minecraft.item.ItemStack stack, int slot) {
        ResourceLocation regName = stack.getItem().getRegistryName();
        out.append("{\"slot\":").append(slot)
                .append(",\"item\":\"").append(regName == null ? "null" : regName.toString())
                .append("\",\"count\":").append(stack.getCount())
                .append(",\"meta\":").append(stack.getMetadata())
                .append('}');
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
            } else if (v instanceof int[]) {
                int[] arr = (int[]) v;
                builder.append('[');
                for (int i = 0; i < arr.length; i++) {
                    if (i > 0) builder.append(',');
                    builder.append(arr[i]);
                }
                builder.append(']');
            } else if (v instanceof java.util.List) {
                builder.append('[');
                boolean firstItem = true;
                for (Object item : (java.util.List<?>) v) {
                    if (!firstItem) builder.append(',');
                    firstItem = false;
                    if (item == null) builder.append("null");
                    else if (item instanceof Number || item instanceof Boolean) builder.append(item);
                    else builder.append('"').append(escapeJson(item.toString())).append('"');
                }
                builder.append(']');
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

    // §5 / §7.13 — item / enchantment registry probes -------------------------

    /**
     * {@code /artest item check <item-id> [capability]} —
     *   * registry presence of the item;
     *   * its unlocalized name;
     *   * whether a freshly-created ItemStack exposes the named capability
     *     (currently supports "protective-armor", or omit to skip the cap check).
     */
    private void handleItem(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 2 || !"check".equalsIgnoreCase(args[0])) {
            send(sender, "{\"error\":\"unknown item subcommand — try check <item-id> [capability]\"}");
            return;
        }
        String itemId = args[1];
        net.minecraft.item.Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", itemId);
        info.put("registered", item != null);
        if (item == null) {
            send(sender, jsonMap(info));
            return;
        }
        info.put("itemClass", item.getClass().getName());
        info.put("unlocalizedName", item.getUnlocalizedName());

        if (args.length >= 3) {
            String capName = args[2];
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item);
            boolean has = false;
            if ("protective-armor".equalsIgnoreCase(capName)) {
                if (zmaster587.advancedRocketry.api.capability.CapabilitySpaceArmor.PROTECTIVEARMOR != null) {
                    has = stack.hasCapability(
                            zmaster587.advancedRocketry.api.capability.CapabilitySpaceArmor.PROTECTIVEARMOR,
                            null);
                }
            } else if ("fluid-handler".equalsIgnoreCase(capName)) {
                has = stack.hasCapability(
                        net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY,
                        null);
            } else {
                info.put("capability_error", "unknown capability \"" + capName + "\"");
            }
            info.put("capability", capName);
            info.put("hasCapability", has);
        }
        send(sender, jsonMap(info));
    }

    /**
     * {@code /artest enchant check <enchant-id>} — reports whether an
     * enchantment is registered. Used to verify the spacebreathing enchant lands
     * during AR init.
     */
    private void handleEnchant(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 2 && "check".equalsIgnoreCase(args[0])) {
            String id = args[1];
            net.minecraft.enchantment.Enchantment ench =
                    ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(id));
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id", id);
            info.put("registered", ench != null);
            if (ench != null) {
                info.put("name", ench.getName());
                info.put("maxLevel", ench.getMaxLevel());
                info.put("rarity", ench.getRarity().name());
            }
            send(sender, jsonMap(info));
            return;
        }
        if (args.length >= 2 && "validates-as-airsuit".equalsIgnoreCase(args[0])) {
            // Synthesises an ItemStack of the given item, optionally enchants it
            // with the AR space-protection enchant ("spacebreathing"), and
            // reports whether ItemAirUtils.isStackValidAirContainer accepts it.
            // The acceptance branch is the production gateway for vacuum-damage
            // bypass via AtmosphereNeedsSuit.protectsFrom → ItemAirWrapper.
            String itemId = args[1];
            boolean withEnchant = args.length >= 3 && Boolean.parseBoolean(args[2]);
            net.minecraft.item.Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("itemId", itemId);
            info.put("registered", item != null);
            info.put("withEnchant", withEnchant);
            if (item == null) {
                send(sender, jsonMap(info));
                return;
            }
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item);
            if (withEnchant) {
                if (zmaster587.advancedRocketry.api.AdvancedRocketryAPI.enchantmentSpaceProtection == null) {
                    info.put("error", "spaceProtection enchant not initialised");
                    send(sender, jsonMap(info));
                    return;
                }
                stack.addEnchantment(zmaster587.advancedRocketry.api.AdvancedRocketryAPI.enchantmentSpaceProtection, 1);
            }
            boolean isAirContainer = zmaster587.advancedRocketry.util.ItemAirUtils.INSTANCE
                    .isStackValidAirContainer(stack);
            info.put("isAirContainer", isAirContainer);
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown enchant subcommand — try check <id> | validates-as-airsuit <itemId> [withEnchant]\"}");
    }

    // §7.13 — CO2 scrubber probe ----------------------------------------------

    /**
     * {@code /artest scrubber consume <dim> <x> <y> <z>} — invokes
     * {@code TileCO2Scrubber.useCharge()} once. Returns whether a charge was
     * consumed and the cartridge's resulting durability damage; tests use
     * before/after diffs to lock down the per-call increment contract.
     */
    private void handleScrubber(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 4 || !"consume".equalsIgnoreCase(args[0])) {
            send(sender, "{\"error\":\"unknown scrubber subcommand — try consume <dim> <x> <y> <z>\"}");
            return;
        }
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
        if (!(tile instanceof zmaster587.advancedRocketry.tile.atmosphere.TileCO2Scrubber)) {
            send(sender, "{\"error\":\"tile not TileCO2Scrubber\",\"tile\":\""
                    + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
            return;
        }
        zmaster587.advancedRocketry.tile.atmosphere.TileCO2Scrubber scrubber =
                (zmaster587.advancedRocketry.tile.atmosphere.TileCO2Scrubber) tile;
        net.minecraft.item.ItemStack pre = scrubber.getStackInSlot(0);
        int damageBefore = pre.isEmpty() ? -1 : pre.getItemDamage();
        boolean consumed = scrubber.useCharge();
        net.minecraft.item.ItemStack post = scrubber.getStackInSlot(0);
        int damageAfter = post.isEmpty() ? -1 : post.getItemDamage();
        send(sender, "{\"ok\":true,\"consumed\":" + consumed
                + ",\"damageBefore\":" + damageBefore
                + ",\"damageAfter\":" + damageAfter
                + ",\"comparatorOverride\":" + scrubber.getComparatorOverride() + "}");
    }

    // §7.13 — gas charge pad probe --------------------------------------------

    /**
     * {@code /artest gascharge fill-suit <dim> <x> <y> <z>} — invokes the
     * same fluid-transfer code path that {@code TileGasChargePad.canPerformFunction}
     * runs against a player standing on the pad, but against a synthetic
     * {@code spaceChestplate} stack. Removes the need to spawn a real entity
     * for the headless harness while still pinning the contract:
     * <em>oxygen in pad tank ends up in suit air when the chestplate is
     * empty</em>.
     *
     * <p>Returns {@code {filled: <int>, airBefore: 0, airAfter: <int>,
     * tankBefore: <int>, tankAfter: <int>}}.</p>
     */
    private void handleGasCharge(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 4 || !"fill-suit".equalsIgnoreCase(args[0])) {
            send(sender, "{\"error\":\"unknown gascharge subcommand — try fill-suit <dim> <x> <y> <z> [itemId] [withSpaceEnchant]\"}");
            return;
        }
        int dim = parseIntOr(args[1], Integer.MIN_VALUE);
        int x = parseIntOr(args[2], 0);
        int y = parseIntOr(args[3], 0);
        int z = args.length >= 5 ? parseIntOr(args[4], 0) : 0;
        // Defaults: enchanted iron chestplate — exercises the ItemAirWrapper
        // branch of TileGasChargePad.canPerformFunction. (A bare
        // spaceChestplate has 0 max-air until oxygen tanks are inserted into
        // its modular inventory, so a fresh stack would no-op — the wrapper
        // path is the deterministic one.)
        String itemId = args.length >= 7 ? args[5] : "minecraft:iron_chestplate";
        boolean withEnchant = args.length < 7 || Boolean.parseBoolean(args[6]);
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!(tile instanceof zmaster587.advancedRocketry.tile.atmosphere.TileGasChargePad)) {
            send(sender, "{\"error\":\"tile not TileGasChargePad\",\"tile\":\""
                    + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
            return;
        }
        zmaster587.advancedRocketry.tile.atmosphere.TileGasChargePad pad =
                (zmaster587.advancedRocketry.tile.atmosphere.TileGasChargePad) tile;
        net.minecraft.item.Item chest = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (chest == null) {
            send(sender, "{\"error\":\"item not registered\",\"id\":\"" + escapeJson(itemId) + "\"}");
            return;
        }
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(chest);
        if (withEnchant) {
            if (zmaster587.advancedRocketry.api.AdvancedRocketryAPI.enchantmentSpaceProtection == null) {
                send(sender, "{\"error\":\"spaceProtection enchant not initialised\"}");
                return;
            }
            stack.addEnchantment(zmaster587.advancedRocketry.api.AdvancedRocketryAPI.enchantmentSpaceProtection, 1);
        }
        // Mirror TileGasChargePad.canPerformFunction's fillable resolution.
        zmaster587.advancedRocketry.api.armor.IFillableArmor fillable = null;
        if (stack.getItem() instanceof zmaster587.advancedRocketry.api.armor.IFillableArmor) {
            fillable = (zmaster587.advancedRocketry.api.armor.IFillableArmor) stack.getItem();
        } else if (zmaster587.advancedRocketry.util.ItemAirUtils.INSTANCE.isStackValidAirContainer(stack)) {
            fillable = new zmaster587.advancedRocketry.util.ItemAirUtils.ItemAirWrapper(stack);
        }
        if (fillable == null) {
            send(sender, "{\"error\":\"item not IFillableArmor and not valid air container\","
                    + "\"item\":\"" + escapeJson(itemId) + "\"}");
            return;
        }
        // Start the suit empty so any transfer is visible (production semantics:
        // pad fills the delta between current and max air).
        fillable.setAirRemaining(stack, 0);
        int airBefore = fillable.getAirRemaining(stack);
        int tankBefore = padTankAmount(pad);
        int amtFluid = fillable.getMaxAir(stack) - airBefore;
        net.minecraftforge.fluids.FluidStack drained = pad.drain(amtFluid, false);
        int filled = 0;
        if (amtFluid > 0 && drained != null
                && zmaster587.libVulpes.util.FluidUtils.areFluidsSameType(drained.getFluid(),
                        zmaster587.advancedRocketry.api.AdvancedRocketryFluids.fluidOxygen)
                && drained.amount > 0) {
            net.minecraftforge.fluids.FluidStack actual = pad.drain(amtFluid, true);
            filled = fillable.increment(stack, actual.amount);
        }
        int airAfter = fillable.getAirRemaining(stack);
        int tankAfter = padTankAmount(pad);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("filled", filled);
        info.put("airBefore", airBefore);
        info.put("airAfter", airAfter);
        info.put("tankBefore", tankBefore);
        info.put("tankAfter", tankAfter);
        send(sender, jsonMap(info));
    }

    private static int padTankAmount(zmaster587.advancedRocketry.tile.atmosphere.TileGasChargePad pad) {
        net.minecraftforge.fluids.capability.IFluidHandler h = findFluidHandler(pad);
        if (h == null) return -1;
        int total = 0;
        for (net.minecraftforge.fluids.capability.IFluidTankProperties p : h.getTankProperties()) {
            if (p.getContents() != null) total += p.getContents().amount;
        }
        return total;
    }

    // §5.7 / §7.13 — fluid handling probes (generic Forge IFluidHandler) -------

    /**
     * {@code /artest fluid inject <dim> <x> <y> <z> <fluidName> <amount>} —
     * fills the tile's Forge IFluidHandler with the named fluid.
     * <p>
     * {@code /artest fluid stored <dim> <x> <y> <z>} — dumps tank states.
     */
    private void handleFluid(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 4 && "stored".equalsIgnoreCase(args[0])) {
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
            net.minecraftforge.fluids.capability.IFluidHandler handler =
                    findFluidHandler(tile);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("tileClass", tile.getClass().getName());
            if (handler == null) {
                info.put("hasFluid", false);
            } else {
                info.put("hasFluid", true);
                net.minecraftforge.fluids.capability.IFluidTankProperties[] props = handler.getTankProperties();
                StringBuilder tanksJson = new StringBuilder("[");
                for (int i = 0; i < props.length; i++) {
                    net.minecraftforge.fluids.FluidStack contents = props[i].getContents();
                    if (i > 0) tanksJson.append(',');
                    tanksJson.append("{\"capacity\":").append(props[i].getCapacity());
                    if (contents == null) {
                        tanksJson.append(",\"fluid\":null}");
                    } else {
                        tanksJson.append(",\"fluid\":\"").append(escapeJson(contents.getFluid().getName()))
                                .append("\",\"amount\":").append(contents.amount).append('}');
                    }
                }
                tanksJson.append(']');
                info.put("tanks_RAW", tanksJson.toString());
            }
            // Hand-emit because jsonMap doesn't pass tanks_RAW through cleanly.
            StringBuilder out = new StringBuilder("{");
            out.append("\"tileClass\":\"").append(escapeJson(tile.getClass().getName())).append('"');
            out.append(",\"hasFluid\":").append(handler != null);
            if (handler != null) {
                out.append(",\"tanks\":").append(info.get("tanks_RAW"));
            }
            out.append('}');
            send(sender, out.toString());
            return;
        }
        if (args.length >= 7 && "inject".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            String fluidName = args[5];
            int amount = parseIntOr(args[6], 0);
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
            net.minecraftforge.fluids.Fluid fluid =
                    net.minecraftforge.fluids.FluidRegistry.getFluid(fluidName);
            if (fluid == null) {
                send(sender, "{\"error\":\"fluid not registered\",\"name\":\""
                        + escapeJson(fluidName) + "\"}");
                return;
            }
            net.minecraftforge.fluids.capability.IFluidHandler handler =
                    findFluidHandler(tile);
            if (handler == null) {
                send(sender, "{\"error\":\"tile has no IFluidHandler capability\"}");
                return;
            }
            int filled = handler.fill(new net.minecraftforge.fluids.FluidStack(fluid, amount), true);
            send(sender, "{\"ok\":true,\"filled\":" + filled
                    + ",\"fluid\":\"" + escapeJson(fluidName) + "\"}");
            return;
        }
        send(sender, "{\"error\":\"unknown fluid subcommand — try stored <dim> <x> <y> <z> | inject <dim> <x> <y> <z> <fluidName> <amount>\"}");
    }

    private static net.minecraftforge.fluids.capability.IFluidHandler findFluidHandler(TileEntity tile) {
        for (net.minecraft.util.EnumFacing dir : net.minecraft.util.EnumFacing.values()) {
            if (tile.hasCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler
                    .FLUID_HANDLER_CAPABILITY, dir)) {
                return tile.getCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler
                        .FLUID_HANDLER_CAPABILITY, dir);
            }
        }
        if (tile.hasCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler
                .FLUID_HANDLER_CAPABILITY, null)) {
            return tile.getCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler
                    .FLUID_HANDLER_CAPABILITY, null);
        }
        return null;
    }

    // §7.13 — oxygen vent state probe -----------------------------------------

    /**
     * {@code /artest vent info <dim> <x> <y> <z>} — exposes the oxygen vent's
     * internal seal state, blob size, and the atmosphere it has imposed on its
     * blob. Used by §7.13 sealed-room scenario to verify the seal-detect cycle.
     *
     * Returns:
     * <pre>
     * {
     *   "isVent": true,
     *   "isSealed": true|false,        // private TileOxygenVent.isSealed
     *   "blobSize": &lt;int&gt;,             // AtmosphereHandler.getBlobSize(vent)
     *   "blobAtmosphere": "...",       // current AreaBlob atmosphere unlocalized name
     *   "hasFluid": true|false,        // private TileOxygenVent.hasFluid
     *   "fluidAmount": &lt;int&gt;,          // tank contents
     *   "energyStored": &lt;int&gt;
     * }
     * </pre>
     */
    private void handleVent(MinecraftServer server, ICommandSender sender, String[] args) {
        // /artest vent reseal <dim> <x> <y> <z> — force a one-shot
        // addBlock(handler, pos) on a vent's blob. Production runs the same
        // call inside performFunction every 100 world-time ticks, but
        // force-tick doesn't advance world time, so tests need an explicit
        // probe to drive the seal cycle.
        if (args.length >= 4 && "reseal".equalsIgnoreCase(args[0])) {
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
            if (!(tile instanceof zmaster587.advancedRocketry.tile.atmosphere.TileOxygenVent)) {
                send(sender, "{\"error\":\"not a TileOxygenVent\"}");
                return;
            }
            zmaster587.advancedRocketry.tile.atmosphere.TileOxygenVent vent =
                    (zmaster587.advancedRocketry.tile.atmosphere.TileOxygenVent) tile;
            zmaster587.advancedRocketry.atmosphere.AtmosphereHandler handler =
                    zmaster587.advancedRocketry.atmosphere.AtmosphereHandler
                            .getOxygenHandler(dim);
            if (handler == null) {
                send(sender, "{\"error\":\"no atmosphere handler for dim\"}");
                return;
            }
            // First-tick parity: ensure blob is registered before the seal
            // check. addBlock NPEs if the vent isn't a registered blob.
            try {
                handler.getBlobSize(vent);
            } catch (NullPointerException notRegistered) {
                handler.registerBlob(vent, vent.getPos());
            }

            // Vent's canFormBlob() returns isTurnedOn(); default redstone
            // state is ON which means the vent only runs when getting a
            // redstone signal — useless for headless tests. Force state to OFF
            // (the "always running, suppressed by redstone" mode in production).
            try {
                java.lang.reflect.Field stateF = zmaster587.advancedRocketry.tile.atmosphere
                        .TileOxygenVent.class.getDeclaredField("state");
                stateF.setAccessible(true);
                stateF.set(vent, zmaster587.libVulpes.util.ZUtils.RedstoneState.OFF);
            } catch (ReflectiveOperationException ignore) {
                // Not fatal — addBlock will simply be a no-op when the vent
                // can't form a blob, and the test will see sealed=false.
            }
            // AtmosphereBlob.addBlock is a no-op when the seed position is
            // already in the graph (production re-evaluates the seal only when
            // the blob is explicitly cleared). Clear the blob first so the
            // flood-fill re-evaluates against current world state — critical
            // for "wall just got broken, recheck seal" assertions.
            handler.clearBlob(vent);

            // AtmosphereBlob runs flood-fill ASYNC when
            // atmosphereHandleBitMask&1==1 (default config bitMask=3).
            // Schedule the work, then busy-wait up to 2s for the worker to
            // settle so the test can read a stable sealed state.
            handler.addBlock(vent,
                    new zmaster587.libVulpes.util.HashedBlockPosition(vent.getPos()));
            long deadline = System.currentTimeMillis() + 2000L;
            while (System.currentTimeMillis() < deadline) {
                try {
                    java.lang.reflect.Field execF = zmaster587.advancedRocketry.util.AtmosphereBlob
                            .class.getDeclaredField("executing");
                    execF.setAccessible(true);
                    Object blob = null;
                    try {
                        java.lang.reflect.Field blobsF =
                                zmaster587.advancedRocketry.atmosphere.AtmosphereHandler
                                        .class.getDeclaredField("blobs");
                        blobsF.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        java.util.HashMap<Object, Object> blobs =
                                (java.util.HashMap<Object, Object>) blobsF.get(handler);
                        blob = blobs.get(vent);
                    } catch (Exception ignore) {}
                    if (blob != null && !execF.getBoolean(blob)) break;
                } catch (Exception ignore) {
                    break;
                }
                try { Thread.sleep(10); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
            int finalBlobSize = handler.getBlobSize(vent);
            boolean newlySealed = finalBlobSize > 0;
            // Mirror the production setSealed(...) via reflection.
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.tile.atmosphere
                        .TileOxygenVent.class.getDeclaredField("isSealed");
                f.setAccessible(true);
                f.setBoolean(vent, newlySealed);
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed: " + escapeJson(e.getMessage()) + "\"}");
                return;
            }
            send(sender, "{\"ok\":true,\"sealed\":" + newlySealed
                    + ",\"blobSize\":" + finalBlobSize + "}");
            return;
        }
        if (args.length < 4 || !"info".equalsIgnoreCase(args[0])) {
            send(sender, "{\"error\":\"unknown vent subcommand — try info <dim> <x> <y> <z> | reseal <dim> <x> <y> <z>\"}");
            return;
        }
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
        if (!(tile instanceof zmaster587.advancedRocketry.tile.atmosphere.TileOxygenVent)) {
            send(sender, "{\"isVent\":false,\"tile\":\""
                    + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
            return;
        }
        zmaster587.advancedRocketry.tile.atmosphere.TileOxygenVent vent =
                (zmaster587.advancedRocketry.tile.atmosphere.TileOxygenVent) tile;

        boolean isSealed;
        boolean hasFluid;
        try {
            java.lang.reflect.Field f1 = zmaster587.advancedRocketry.tile.atmosphere.TileOxygenVent
                    .class.getDeclaredField("isSealed");
            f1.setAccessible(true);
            isSealed = f1.getBoolean(vent);
            java.lang.reflect.Field f2 = zmaster587.advancedRocketry.tile.atmosphere.TileOxygenVent
                    .class.getDeclaredField("hasFluid");
            f2.setAccessible(true);
            hasFluid = f2.getBoolean(vent);
        } catch (ReflectiveOperationException e) {
            send(sender, "{\"error\":\"reflection failed: " + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        zmaster587.advancedRocketry.atmosphere.AtmosphereHandler handler =
                zmaster587.advancedRocketry.atmosphere.AtmosphereHandler
                        .getOxygenHandler(dim);
        // Blob lookup throws NPE if the vent hasn't yet had performFunction
        // called once (which is what registers the blob). Guard for that.
        int blobSize;
        if (handler == null) {
            blobSize = -1;
        } else {
            try {
                blobSize = handler.getBlobSize(vent);
            } catch (NullPointerException notRegisteredYet) {
                blobSize = -2; // sentinel: blob not registered
            }
        }
        String blobAtm = "no-handler";
        if (handler != null) {
            zmaster587.advancedRocketry.api.IAtmosphere atm =
                    handler.getAtmosphereType(new BlockPos(x, y + 1, z));
            blobAtm = atm == null ? "null" : atm.getUnlocalizedName();
        }

        // Tank contents.
        net.minecraftforge.fluids.capability.IFluidHandler fluidH = findFluidHandler(tile);
        int fluidAmount = 0;
        if (fluidH != null) {
            for (net.minecraftforge.fluids.capability.IFluidTankProperties p : fluidH.getTankProperties()) {
                if (p.getContents() != null) fluidAmount += p.getContents().amount;
            }
        }

        // Energy.
        int energyStored = 0;
        net.minecraftforge.energy.IEnergyStorage es = null;
        for (net.minecraft.util.EnumFacing dir : net.minecraft.util.EnumFacing.values()) {
            if (tile.hasCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, dir)) {
                es = tile.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, dir);
                break;
            }
        }
        if (es == null && tile.hasCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null)) {
            es = tile.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
        }
        if (es != null) energyStored = es.getEnergyStored();

        StringBuilder out = new StringBuilder("{");
        out.append("\"isVent\":true");
        out.append(",\"isSealed\":").append(isSealed);
        out.append(",\"blobSize\":").append(blobSize);
        out.append(",\"blobAtmosphere\":\"").append(escapeJson(blobAtm)).append('"');
        out.append(",\"hasFluid\":").append(hasFluid);
        out.append(",\"fluidAmount\":").append(fluidAmount);
        out.append(",\"energyStored\":").append(energyStored);
        out.append('}');
        send(sender, out.toString());
    }

    // §7.18 — beacon location probe -------------------------------------------

    /**
     * {@code /artest beacon list <dim>} — returns the dim's registered beacon
     * locations. Beacons add themselves to
     * {@code DimensionProperties.beaconLocations} when their multiblock is
     * enabled (via {@code TileBeacon.setMachineEnabled(true)}).
     */
    private void handleBeacon(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 2 || !"list".equalsIgnoreCase(args[0])) {
            send(sender, "{\"error\":\"unknown beacon subcommand — try list <dim>\"}");
            return;
        }
        int dim = parseIntOr(args[1], Integer.MIN_VALUE);
        // getDimensionProperties has an overworld-fallback for unknown ids —
        // use isDimensionCreated to detect "truly registered AR dim".
        if (!zmaster587.advancedRocketry.dimension.DimensionManager.getInstance()
                .isDimensionCreated(dim)) {
            send(sender, "{\"error\":\"dim not registered\",\"dim\":" + dim + "}");
            return;
        }
        zmaster587.advancedRocketry.dimension.DimensionProperties props =
                zmaster587.advancedRocketry.dimension.DimensionManager.getInstance()
                        .getDimensionProperties(dim);
        java.util.Set<zmaster587.libVulpes.util.HashedBlockPosition> locs =
                props.getBeacons();
        StringBuilder out = new StringBuilder("{\"dim\":").append(dim);
        out.append(",\"count\":").append(locs == null ? -1 : locs.size());
        out.append(",\"locations\":[");
        if (locs != null) {
            boolean first = true;
            for (zmaster587.libVulpes.util.HashedBlockPosition p : locs) {
                if (!first) out.append(',');
                first = false;
                out.append('[').append(p.x).append(',').append(p.y).append(',').append(p.z).append(']');
            }
        }
        out.append("]}");
        send(sender, out.toString());
    }

    // §7.18 — entity spawn probe ----------------------------------------------

    /**
     * {@code /artest entity spawn <dim> <x> <y> <z> <entityRegistryName>} —
     * spawns an entity by its registry name (e.g.
     * {@code advancedrocketry:hovercraft}). Returns the spawned entity id, or
     * an error if the entity class doesn't have a {@code (World,double,double,double)}
     * or {@code (World)} ctor.
     *
     * {@code /artest entity info <dim> <entityId>} — reports the entity's
     * class + position + alive state.
     */
    private void handleEntity(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 6 && "spawn".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            double x = parseDoubleOr(args[2], 0);
            double y = parseDoubleOr(args[3], 0);
            double z = parseDoubleOr(args[4], 0);
            String entityName = args[5];
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            Class<? extends net.minecraft.entity.Entity> clazz =
                    net.minecraft.entity.EntityList.getClass(new ResourceLocation(entityName));
            if (clazz == null) {
                send(sender, "{\"error\":\"unknown entity name\",\"name\":\""
                        + escapeJson(entityName) + "\"}");
                return;
            }
            net.minecraft.entity.Entity entity;
            try {
                entity = spawnEntityReflectively(clazz, world, x, y, z, args);
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"spawn failed: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage())
                        + "\"}");
                return;
            }
            // TASK-08-mixin Phase 3 pin tests: a freshly-spawned falling
            // block / TNT / minecart needs a force-loaded chunk under it,
            // otherwise the first onUpdate tick can early-out before any
            // mixin-injected gravity hook fires. We don't force-load here —
            // tests are expected to do so via the `chunk forceload` probe.
            boolean spawned = world.spawnEntity(entity);
            // Optional: drive N onUpdate ticks atomically in the same
            // probe call — used by mixin gravity pins that need to
            // observe motionY/posY accumulation BEFORE the natural
            // server tick gets a chance to setDead the entity (vanilla
            // EntityFallingBlock + co. have aggressive auto-setDead
            // logic on the very next worldTick). The 7th arg (after the
            // entity name) names the IBlockState for FallingBlock-style
            // ctors; an 8th arg requests that many immediate ticks.
            int extraTicks = args.length >= 8 ? Math.max(0, parseIntOr(args[7], 0)) : 0;
            int ticked = 0;
            if (spawned && extraTicks > 0) {
                for (int i = 0; i < extraTicks; i++) {
                    if (entity.isDead) break;
                    entity.onUpdate();
                    ticked++;
                }
            }
            send(sender, "{\"ok\":true,\"spawned\":" + spawned
                    + ",\"entityId\":" + entity.getEntityId()
                    + ",\"entityClass\":\"" + escapeJson(entity.getClass().getName()) + "\""
                    + ",\"ticked\":" + ticked
                    + ",\"isDead\":" + entity.isDead
                    + ",\"motionY\":" + entity.motionY
                    + ",\"posY\":" + entity.posY + "}");
            return;
        }
        if (args.length >= 3 && "info".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int id = parseIntOr(args[2], -1);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            net.minecraft.entity.Entity entity = world.getEntityByID(id);
            if (entity == null) {
                send(sender, "{\"isAlive\":false,\"entityId\":" + id + "}");
                return;
            }
            send(sender, "{\"isAlive\":true,\"entityId\":" + id
                    + ",\"entityClass\":\"" + escapeJson(entity.getClass().getName()) + "\""
                    + ",\"posX\":" + entity.posX
                    + ",\"posY\":" + entity.posY
                    + ",\"posZ\":" + entity.posZ
                    + ",\"motionX\":" + entity.motionX
                    + ",\"motionY\":" + entity.motionY
                    + ",\"motionZ\":" + entity.motionZ
                    + ",\"hasNoGravity\":" + entity.hasNoGravity()
                    + ",\"isDead\":" + entity.isDead + "}");
            return;
        }
        if (args.length >= 3 && "tick".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int id = parseIntOr(args[2], -1);
            int count = args.length >= 4 ? Math.max(1, parseIntOr(args[3], 1)) : 1;
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            net.minecraft.entity.Entity entity = world.getEntityByID(id);
            if (entity == null) {
                send(sender, "{\"error\":\"entity not found\",\"entityId\":" + id + "}");
                return;
            }
            int ticked = 0;
            for (int i = 0; i < count; i++) {
                if (entity.isDead) break;
                entity.onUpdate();
                ticked++;
            }
            send(sender, "{\"ok\":true,\"entityId\":" + id
                    + ",\"requested\":" + count
                    + ",\"ticked\":" + ticked
                    + ",\"isDead\":" + entity.isDead
                    + ",\"motionY\":" + entity.motionY
                    + ",\"posY\":" + entity.posY + "}");
            return;
        }
        // TASK-25 — scan a box around (cx,cy,cz) for EntityItem instances
        // and emit each one's registry name + count + position. Used to
        // pin recipe outputs that spawn as world entities (e.g. PlatePress
        // drops its output as an EntityItem next to the press).
        if (args.length >= 6 && "scan-items".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            double cx = parseDoubleOr(args[2], 0);
            double cy = parseDoubleOr(args[3], 0);
            double cz = parseDoubleOr(args[4], 0);
            double radius = parseDoubleOr(args[5], 1);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            net.minecraft.util.math.AxisAlignedBB bb = new net.minecraft.util.math.AxisAlignedBB(
                    cx - radius, cy - radius, cz - radius,
                    cx + radius, cy + radius, cz + radius);
            java.util.List<net.minecraft.entity.item.EntityItem> items =
                    world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, bb);
            StringBuilder b = new StringBuilder("{\"ok\":true,\"count\":")
                    .append(items.size()).append(",\"items\":[");
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) b.append(',');
                net.minecraft.entity.item.EntityItem ei = items.get(i);
                net.minecraft.item.ItemStack stack = ei.getItem();
                ResourceLocation rn = stack.getItem().getRegistryName();
                b.append("{\"item\":\"").append(rn == null ? "null" : rn.toString())
                        .append("\",\"count\":").append(stack.getCount())
                        .append(",\"meta\":").append(stack.getMetadata())
                        .append(",\"posX\":").append(ei.posX)
                        .append(",\"posY\":").append(ei.posY)
                        .append(",\"posZ\":").append(ei.posZ)
                        .append('}');
            }
            b.append("]}");
            send(sender, b.toString());
            return;
        }
        // ── TASK-30 Gap 3: EntityElevatorCapsule probes ──────────────────
        //
        // The elevator capsule exposes four motion-state methods used by
        // RenderElevatorCapsule (client) and TileSpaceElevator (controller):
        // isAscending / isDescending / isInMotion / getStandTime. These
        // probes let testServer pin the contract that setCapsuleMotion(N)
        // → flags reflect, plus that NBT round-trip preserves motionDir +
        // dst/src tile coordinates. Bridges what entity spawn + tick +
        // info already provide.
        if (args.length >= 3 && "capsule-state".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int id = parseIntOr(args[2], -1);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            net.minecraft.entity.Entity entity = world.getEntityByID(id);
            if (!(entity instanceof zmaster587.advancedRocketry.entity.EntityElevatorCapsule)) {
                send(sender, "{\"error\":\"entity not an EntityElevatorCapsule\",\"entityId\":" + id + "}");
                return;
            }
            zmaster587.advancedRocketry.entity.EntityElevatorCapsule cap =
                    (zmaster587.advancedRocketry.entity.EntityElevatorCapsule) entity;
            send(sender, "{\"ok\":true,\"entityId\":" + id
                    + ",\"isAscending\":" + cap.isAscending()
                    + ",\"isDescending\":" + cap.isDescending()
                    + ",\"isInMotion\":" + cap.isInMotion()
                    + ",\"standTime\":" + cap.getStandTime() + "}");
            return;
        }
        if (args.length >= 4 && "capsule-set-motion".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int id = parseIntOr(args[2], -1);
            int motion = parseIntOr(args[3], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            net.minecraft.entity.Entity entity = world.getEntityByID(id);
            if (!(entity instanceof zmaster587.advancedRocketry.entity.EntityElevatorCapsule)) {
                send(sender, "{\"error\":\"entity not an EntityElevatorCapsule\",\"entityId\":" + id + "}");
                return;
            }
            zmaster587.advancedRocketry.entity.EntityElevatorCapsule cap =
                    (zmaster587.advancedRocketry.entity.EntityElevatorCapsule) entity;
            cap.setCapsuleMotion(motion);
            send(sender, "{\"ok\":true,\"entityId\":" + id
                    + ",\"motion\":" + motion + "}");
            return;
        }
        if (args.length >= 7 && "capsule-set-dst".equalsIgnoreCase(args[0])) {
            // capsule-set-dst <dim> <entityId> <dstDim> <dstX> <dstY> <dstZ>
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int id = parseIntOr(args[2], -1);
            int dstDim = parseIntOr(args[3], 0);
            int dstX = parseIntOr(args[4], 0);
            int dstY = parseIntOr(args[5], 0);
            int dstZ = parseIntOr(args[6], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            net.minecraft.entity.Entity entity = world.getEntityByID(id);
            if (!(entity instanceof zmaster587.advancedRocketry.entity.EntityElevatorCapsule)) {
                send(sender, "{\"error\":\"entity not an EntityElevatorCapsule\",\"entityId\":" + id + "}");
                return;
            }
            zmaster587.advancedRocketry.entity.EntityElevatorCapsule cap =
                    (zmaster587.advancedRocketry.entity.EntityElevatorCapsule) entity;
            cap.setDst(new zmaster587.advancedRocketry.util.DimensionBlockPosition(
                    dstDim,
                    new zmaster587.libVulpes.util.HashedBlockPosition(dstX, dstY, dstZ)));
            send(sender, "{\"ok\":true,\"entityId\":" + id
                    + ",\"dstDim\":" + dstDim
                    + ",\"dstX\":" + dstX + ",\"dstY\":" + dstY + ",\"dstZ\":" + dstZ + "}");
            return;
        }
        if (args.length >= 7 && "capsule-set-src".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int id = parseIntOr(args[2], -1);
            int srcDim = parseIntOr(args[3], 0);
            int srcX = parseIntOr(args[4], 0);
            int srcY = parseIntOr(args[5], 0);
            int srcZ = parseIntOr(args[6], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            net.minecraft.entity.Entity entity = world.getEntityByID(id);
            if (!(entity instanceof zmaster587.advancedRocketry.entity.EntityElevatorCapsule)) {
                send(sender, "{\"error\":\"entity not an EntityElevatorCapsule\",\"entityId\":" + id + "}");
                return;
            }
            zmaster587.advancedRocketry.entity.EntityElevatorCapsule cap =
                    (zmaster587.advancedRocketry.entity.EntityElevatorCapsule) entity;
            cap.setSourceTile(new zmaster587.advancedRocketry.util.DimensionBlockPosition(
                    srcDim,
                    new zmaster587.libVulpes.util.HashedBlockPosition(srcX, srcY, srcZ)));
            send(sender, "{\"ok\":true,\"entityId\":" + id
                    + ",\"srcDim\":" + srcDim
                    + ",\"srcX\":" + srcX + ",\"srcY\":" + srcY + ",\"srcZ\":" + srcZ + "}");
            return;
        }
        if (args.length >= 3 && "capsule-nbt-roundtrip".equalsIgnoreCase(args[0])) {
            // Writes the capsule's current state via writeEntityToNBT into a
            // fresh NBTTagCompound, then constructs a peer capsule and reads
            // the NBT back. Emits the readback state so a test can assert
            // motionDir + dst/src survive the save/load cycle.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int id = parseIntOr(args[2], -1);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            net.minecraft.entity.Entity entity = world.getEntityByID(id);
            if (!(entity instanceof zmaster587.advancedRocketry.entity.EntityElevatorCapsule)) {
                send(sender, "{\"error\":\"entity not an EntityElevatorCapsule\",\"entityId\":" + id + "}");
                return;
            }
            zmaster587.advancedRocketry.entity.EntityElevatorCapsule src =
                    (zmaster587.advancedRocketry.entity.EntityElevatorCapsule) entity;
            net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
            // writeEntityToNBT / readEntityFromNBT are protected on
            // EntityElevatorCapsule (vanilla Entity contract) — invoke
            // via reflection so we can drive a save/load cycle from this
            // probe without leaking a public hook into production.
            zmaster587.advancedRocketry.entity.EntityElevatorCapsule peer =
                    new zmaster587.advancedRocketry.entity.EntityElevatorCapsule(world);
            try {
                java.lang.reflect.Method write = net.minecraft.entity.Entity.class
                        .getDeclaredMethod("writeEntityToNBT", net.minecraft.nbt.NBTTagCompound.class);
                write.setAccessible(true);
                write.invoke(src, nbt);
                java.lang.reflect.Method read = net.minecraft.entity.Entity.class
                        .getDeclaredMethod("readEntityFromNBT", net.minecraft.nbt.NBTTagCompound.class);
                read.setAccessible(true);
                read.invoke(peer, nbt);
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflective NBT round-trip failed: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage())
                        + "\"}");
                return;
            }
            StringBuilder b = new StringBuilder("{\"ok\":true,\"entityId\":")
                    .append(id)
                    .append(",\"nbtKeys\":[");
            java.util.Set<String> keys = nbt.getKeySet();
            int ki = 0;
            for (String k : keys) {
                if (ki++ > 0) b.append(',');
                b.append('"').append(escapeJson(k)).append('"');
            }
            b.append("]")
                    .append(",\"peerIsAscending\":").append(peer.isAscending())
                    .append(",\"peerIsDescending\":").append(peer.isDescending())
                    .append(",\"peerIsInMotion\":").append(peer.isInMotion())
                    .append(",\"hasDstKey\":").append(nbt.hasKey("dstDimid"))
                    .append(",\"hasSrcKey\":").append(nbt.hasKey("srcDimid"))
                    .append(",\"motionDirNbt\":").append(nbt.getByte("motionDir"));
            if (nbt.hasKey("dstDimid")) {
                int[] dstLoc = nbt.getIntArray("dstLoc");
                b.append(",\"dstDim\":").append(nbt.getInteger("dstDimid"))
                        .append(",\"dstX\":").append(dstLoc[0])
                        .append(",\"dstY\":").append(dstLoc[1])
                        .append(",\"dstZ\":").append(dstLoc[2]);
            }
            if (nbt.hasKey("srcDimid")) {
                int[] srcLoc = nbt.getIntArray("srcLoc");
                b.append(",\"srcDim\":").append(nbt.getInteger("srcDimid"))
                        .append(",\"srcX\":").append(srcLoc[0])
                        .append(",\"srcY\":").append(srcLoc[1])
                        .append(",\"srcZ\":").append(srcLoc[2]);
            }
            b.append('}');
            send(sender, b.toString());
            return;
        }
        send(sender, "{\"error\":\"unknown entity subcommand — try spawn <dim> <x> <y> <z> <name> [block-id] | info <dim> <entityId> | tick <dim> <entityId> [count] | scan-items <dim> <cx> <cy> <cz> <radius> | capsule-state <dim> <id> | capsule-set-motion <dim> <id> <value> | capsule-set-dst <dim> <id> <dstDim> <x> <y> <z> | capsule-set-src <dim> <id> <srcDim> <x> <y> <z> | capsule-nbt-roundtrip <dim> <id>\"}");
    }

    /**
     * Reflective entity spawn helper that knows about three constructor
     * shapes seen on vanilla 1.12.2 entities used by TASK-08-mixin pin
     * tests:
     *
     * <ol>
     *   <li>{@code (World, double, double, double, IBlockState)} —
     *       {@link net.minecraft.entity.item.EntityFallingBlock}. The
     *       block-state is taken from a 6th probe arg ({@code block-id});
     *       defaults to {@code minecraft:sand} when omitted.</li>
     *   <li>{@code (World, double, double, double)} — most ticking
     *       entities ({@code EntityTNTPrimed},
     *       {@code EntityMinecartEmpty}, ...).</li>
     *   <li>{@code (World)} — fall-through; setPosition is applied
     *       manually.</li>
     * </ol>
     */
    private static net.minecraft.entity.Entity spawnEntityReflectively(
            Class<? extends net.minecraft.entity.Entity> clazz,
            net.minecraft.world.WorldServer world,
            double x, double y, double z,
            String[] args) throws ReflectiveOperationException {
        // 1) FallingBlock-style ctor — needs an IBlockState.
        try {
            java.lang.reflect.Constructor<? extends net.minecraft.entity.Entity> ctor =
                    clazz.getConstructor(net.minecraft.world.World.class,
                            double.class, double.class, double.class,
                            net.minecraft.block.state.IBlockState.class);
            String blockId = args.length >= 7 ? args[6] : "minecraft:sand";
            net.minecraft.block.Block block = ForgeRegistries.BLOCKS.getValue(
                    new ResourceLocation(blockId));
            if (block == null) {
                throw new IllegalArgumentException("unknown block-id for "
                        + clazz.getSimpleName() + " fall-state: " + blockId);
            }
            return ctor.newInstance(world, x, y, z, block.getDefaultState());
        } catch (NoSuchMethodException ignored) { /* fall through */ }

        // 2) Most ticking entities: (World, x, y, z).
        try {
            java.lang.reflect.Constructor<? extends net.minecraft.entity.Entity> ctor =
                    clazz.getConstructor(net.minecraft.world.World.class,
                            double.class, double.class, double.class);
            return ctor.newInstance(world, x, y, z);
        } catch (NoSuchMethodException ignored) { /* fall through */ }

        // 3) Bare (World) ctor; setPosition manually.
        java.lang.reflect.Constructor<? extends net.minecraft.entity.Entity> ctor =
                clazz.getConstructor(net.minecraft.world.World.class);
        net.minecraft.entity.Entity entity = ctor.newInstance(world);
        entity.setPosition(x, y, z);
        return entity;
    }

    private static double parseDoubleOr(String s, double dflt) {
        try { return Double.parseDouble(s); } catch (NumberFormatException nfe) { return dflt; }
    }

    /**
     * Player-state probe. Used by TASK-08-mixin's testClient e2e pin for
     * the {@code MixinEntityPlayer(MP)InventoryAccess} {@code @Redirect}:
     * a real-player GUI session can only exercise the rocket-inventory
     * bypass when {@link zmaster587.advancedRocketry.util.RocketInventoryHelper}
     * has the player in its bypass set — but the helper's public mutators
     * are normally driven by AR's own rocket-mount lifecycle. This probe
     * exposes them directly so the e2e test can toggle the bypass and
     * assert the open container GUI survives a distance-driven close
     * cycle that would otherwise fire from {@code EntityPlayerMP.onUpdate}.
     *
     * <p>Subcommands:</p>
     * <ul>
     *   <li>{@code /artest player inv-bypass add} — add the first
     *       connected player to the bypass set.</li>
     *   <li>{@code /artest player inv-bypass remove} — remove them.</li>
     *   <li>{@code /artest player inv-bypass status} — report whether
     *       the first connected player is in the bypass set.</li>
     *   <li>{@code /artest player open-container} — report whether the
     *       first connected player currently has an open container
     *       (i.e. {@code openContainer != inventoryContainer}).</li>
     * </ul>
     */
    private void handlePlayer(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 1) {
            send(sender, "{\"error\":\"usage: /artest player inv-bypass <add|remove|status> | open-container\"}");
            return;
        }
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        java.util.List<net.minecraft.entity.player.EntityPlayerMP> players =
                server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            send(sender, "{\"error\":\"no players connected\"}");
            return;
        }
        net.minecraft.entity.player.EntityPlayerMP player = players.get(0);
        if ("inv-bypass".equals(sub) && args.length >= 2) {
            String action = args[1].toLowerCase(java.util.Locale.ROOT);
            switch (action) {
                case "add":
                    zmaster587.advancedRocketry.util.RocketInventoryHelper
                            .addPlayerToInventoryBypass(player);
                    send(sender, "{\"ok\":true,\"action\":\"add\",\"player\":\""
                            + escapeJson(player.getName()) + "\""
                            + ",\"inBypass\":true}");
                    return;
                case "remove":
                    zmaster587.advancedRocketry.util.RocketInventoryHelper
                            .removePlayerFromInventoryBypass(player);
                    send(sender, "{\"ok\":true,\"action\":\"remove\",\"player\":\""
                            + escapeJson(player.getName()) + "\""
                            + ",\"inBypass\":"
                            + zmaster587.advancedRocketry.util.RocketInventoryHelper
                                    .canPlayerBypassInvChecks(player) + "}");
                    return;
                case "status":
                    send(sender, "{\"ok\":true,\"player\":\""
                            + escapeJson(player.getName()) + "\""
                            + ",\"inBypass\":"
                            + zmaster587.advancedRocketry.util.RocketInventoryHelper
                                    .canPlayerBypassInvChecks(player) + "}");
                    return;
            }
        }
        if ("open-container".equals(sub)) {
            boolean isInventoryContainer = player.openContainer == player.inventoryContainer;
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"openContainerClass\":\""
                    + escapeJson(player.openContainer.getClass().getName()) + "\""
                    + ",\"isInventoryContainer\":" + isInventoryContainer + "}");
            return;
        }
        if ("health".equals(sub)) {
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"health\":" + player.getHealth()
                    + ",\"maxHealth\":" + player.getMaxHealth()
                    + ",\"dim\":" + player.world.provider.getDimension()
                    + ",\"posX\":" + player.posX
                    + ",\"posY\":" + player.posY
                    + ",\"posZ\":" + player.posZ + "}");
            return;
        }
        if ("held-air".equals(sub)) {
            // Probe the air-buffer NBT on the player's chest-armor slot
            // (the canonical AR space-suit slot — ItemSpaceChest wraps
            // ItemAirUtils). Falls back to the main-hand stack for tests
            // that hand the suit raw to the player without equipping.
            net.minecraft.item.ItemStack chest = player.getItemStackFromSlot(
                    net.minecraft.inventory.EntityEquipmentSlot.CHEST);
            net.minecraft.item.ItemStack mainHand = player.getHeldItemMainhand();
            int chestAir = chest.isEmpty() ? -1
                    : zmaster587.advancedRocketry.util.ItemAirUtils.INSTANCE.getAirRemaining(chest);
            int mainHandAir = mainHand.isEmpty() ? -1
                    : zmaster587.advancedRocketry.util.ItemAirUtils.INSTANCE.getAirRemaining(mainHand);
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"chestSlot\":\""
                    + escapeJson(chest.isEmpty() ? "" : chest.getItem().getRegistryName().toString())
                    + "\""
                    + ",\"chestAir\":" + chestAir
                    + ",\"mainHand\":\""
                    + escapeJson(mainHand.isEmpty() ? "" : mainHand.getItem().getRegistryName().toString())
                    + "\""
                    + ",\"mainHandAir\":" + mainHandAir + "}");
            return;
        }
        if ("set-health".equals(sub) && args.length >= 2) {
            float newHealth = (float) parseDoubleOr(args[1], 20.0);
            player.setHealth(newHealth);
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"health\":" + player.getHealth() + "}");
            return;
        }
        if ("try-fall".equals(sub) && args.length >= 2) {
            // /artest player try-fall <distance>
            //
            // Posts a synthetic LivingFallEvent with the supplied raw
            // fall distance and reports the post-handler distance.
            // PlanetEventHandler.fallEvent (line 612-618) scales by the
            // provider's gravitational multiplier on IPlanetaryProvider
            // dims, so the returned distance is < input on low-grav and
            // equal-to-input on the overworld (no IPlanetaryProvider).
            float input = (float) parseDoubleOr(args[1], 20.0);
            net.minecraftforge.event.entity.living.LivingFallEvent ev =
                    new net.minecraftforge.event.entity.living.LivingFallEvent(player, input, 1.0F);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(ev);
            double gravity = -1.0;
            if (player.world.provider instanceof zmaster587.advancedRocketry.api.IPlanetaryProvider) {
                gravity = ((zmaster587.advancedRocketry.api.IPlanetaryProvider) player.world.provider)
                        .getGravitationalMultiplier(player.getPosition());
            }
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"dim\":" + player.world.provider.getDimension()
                    + ",\"inputDistance\":" + input
                    + ",\"resultDistance\":" + ev.getDistance()
                    + ",\"isPlanetaryProvider\":"
                    + (player.world.provider instanceof zmaster587.advancedRocketry.api.IPlanetaryProvider)
                    + ",\"gravityMultiplier\":" + gravity + "}");
            return;
        }
        if ("try-sleep".equals(sub)) {
            // /artest player try-sleep
            //
            // Fires a synthetic PlayerSleepInBedEvent at the player's
            // current BlockPos and reports the post-handler result
            // status. Used to pin PlanetEventHandler.sleepEvent's
            // vacuum-refuses-sleep guard without going through the
            // real bed-right-click code path (which would need a
            // placed bed block + the vanilla EntityPlayer.trySleep
            // pre-checks like night-time, no enemies, etc.).
            net.minecraftforge.event.entity.player.PlayerSleepInBedEvent ev =
                    new net.minecraftforge.event.entity.player.PlayerSleepInBedEvent(player, player.getPosition());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(ev);
            net.minecraft.entity.player.EntityPlayer.SleepResult status = ev.getResultStatus();
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"dim\":" + player.world.provider.getDimension()
                    + ",\"resultStatus\":\""
                    + (status == null ? "null" : status.name()) + "\"}");
            return;
        }
        if ("try-ignite".equals(sub)) {
            // /artest player try-ignite
            //
            // Equips a flint-and-steel into the player's main hand,
            // posts a synthetic RightClickBlock event at the player's
            // position with EnumFacing.UP, and reports event.isCanceled().
            // Used to pin PlanetEventHandler.blockRightClicked's
            // vacuum-no-fire guard.
            net.minecraft.item.ItemStack flint = new net.minecraft.item.ItemStack(
                    net.minecraft.init.Items.FLINT_AND_STEEL);
            player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, flint);
            net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock ev =
                    new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                            player,
                            net.minecraft.util.EnumHand.MAIN_HAND,
                            player.getPosition(),
                            net.minecraft.util.EnumFacing.UP,
                            net.minecraft.util.math.Vec3d.ZERO);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(ev);
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"dim\":" + player.world.provider.getDimension()
                    + ",\"canceled\":" + ev.isCanceled() + "}");
            return;
        }
        if ("advancement".equals(sub) && args.length >= 2) {
            // /artest player advancement <id>
            // /artest player advancement reset <id>
            //
            // <id> is a ResourceLocation accepted by AdvancementManager —
            // e.g. "advancedrocketry:moonlanding". Returns isDone() for the
            // root completion criterion. The reset path revokes ALL
            // criteria on the advancement (used by counter-tests that need
            // to re-run within a single workdir).
            String maybeReset = args[1].toLowerCase(java.util.Locale.ROOT);
            boolean reset = "reset".equals(maybeReset) && args.length >= 3;
            String idStr = reset ? args[2] : args[1];
            net.minecraft.util.ResourceLocation rl;
            try {
                rl = new net.minecraft.util.ResourceLocation(idStr);
            } catch (Exception ex) {
                send(sender, "{\"error\":\"invalid advancement id\",\"value\":\""
                        + escapeJson(idStr) + "\"}");
                return;
            }
            net.minecraft.advancements.Advancement adv = server.getAdvancementManager().getAdvancement(rl);
            if (adv == null) {
                send(sender, "{\"error\":\"unknown advancement\",\"id\":\""
                        + escapeJson(idStr) + "\"}");
                return;
            }
            net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getProgress(adv);
            if (reset) {
                for (String crit : progress.getCompletedCriteria()) {
                    player.getAdvancements().revokeCriterion(adv, crit);
                }
                progress = player.getAdvancements().getProgress(adv);
            }
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"advancement\":\"" + escapeJson(idStr) + "\""
                    + ",\"isDone\":" + progress.isDone()
                    + ",\"reset\":" + reset + "}");
            return;
        }
        if ("last-chat".equals(sub)) {
            // /artest player last-chat
            //
            // Returns the most-recently observed outbound SPacketChat
            // translation key (or unformatted text) sent to this player,
            // captured by the Netty-pipeline chat-tap installed on first
            // use. Empty deque → "key" reports null.
            installChatTap(player);
            String head = chatLog.peekFirst();
            int size = chatLog.size();
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"key\":" + (head == null ? "null" : "\"" + escapeJson(head) + "\"")
                    + ",\"size\":" + size + "}");
            return;
        }
        if ("chat-clear".equals(sub)) {
            // /artest player chat-clear
            //
            // Drops every captured chat entry. Tests call this before the
            // operation under test to avoid cross-contamination from
            // prior chat traffic.
            installChatTap(player);
            chatLog.clear();
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"size\":0}");
            return;
        }
        if ("try-seal-detect".equals(sub) && args.length >= 5) {
            // /artest player try-seal-detect <dim> <x> <y> <z>
            //
            // Equips the player with ItemSealDetector and invokes
            // onItemUse(...) against the target block, then reports the
            // most-recent translation key the production code dispatched
            // via player.sendMessage(...). The chat-tap is installed and
            // drained synchronously by flushing the channel event-loop
            // before reading.
            //
            // Production: ItemSealDetector.onItemUse:34-50 sends one of
            // six msg.sealdetector.<branch> translation keys
            // (sealed | notsealmat | notsealblock | notfullblock | fluid
            // | other). This probe pins the player-visible side of the
            // dispatch — i.e. that the chat actually reaches the player
            // with the correct i18n key.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int x = parseIntOr(args[2], 0);
            int y = parseIntOr(args[3], 0);
            int z = parseIntOr(args[4], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            installChatTap(player);
            chatLog.clear();
            net.minecraft.item.Item detector =
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemSealDetector;
            net.minecraft.item.ItemStack held = new net.minecraft.item.ItemStack(detector);
            player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, held);
            BlockPos pos = new BlockPos(x, y, z);
            net.minecraft.util.EnumActionResult res =
                    detector.onItemUse(player, world, pos,
                            net.minecraft.util.EnumHand.MAIN_HAND,
                            net.minecraft.util.EnumFacing.UP, 0.5F, 1.0F, 0.5F);
            flushPlayerChannel(player);
            String head = chatLog.peekFirst();
            String branch = stripBranchPrefix(head);
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"pos\":[" + x + "," + y + "," + z + "]"
                    + ",\"result\":\"" + res.name() + "\""
                    + ",\"key\":" + (head == null ? "null" : "\"" + escapeJson(head) + "\"")
                    + ",\"branch\":" + (branch == null ? "null" : "\"" + escapeJson(branch) + "\"")
                    + "}");
            return;
        }
        if ("try-orescanner-rclick".equals(sub)) {
            // /artest player try-orescanner-rclick [register-satellite-on-dim]
            //
            // Gap T3 #12 — equip the player with ItemOreScanner and
            // invoke onItemRightClick. The production path opens the
            // OreMapping GUI WHEN the stored satellite-ID resolves to a
            // SatelliteOreMapping on the current dim — otherwise the
            // right-click is a no-op. Pin "no crash" on the empty path
            // and the path-with-satellite.
            //
            // If args[1] is a dim id, register a fresh SatelliteOreMapping
            // on that dim and seed the held item's NBT to point at it.
            // Otherwise (no arg or "none"), the held item has no NBT —
            // production must early-out without NPE.
            int satRegisterDim = (args.length >= 2 && !"none".equalsIgnoreCase(args[1]))
                    ? parseIntOr(args[1], Integer.MIN_VALUE) : Integer.MIN_VALUE;
            long satId = -1;
            if (satRegisterDim != Integer.MIN_VALUE) {
                net.minecraft.world.WorldServer satWorld = server.getWorld(satRegisterDim);
                zmaster587.advancedRocketry.dimension.DimensionProperties props = satWorld == null ? null
                        : zmaster587.advancedRocketry.dimension.DimensionManager.getInstance()
                                .getDimensionProperties(satRegisterDim);
                if (satWorld != null && props != null) {
                    zmaster587.advancedRocketry.satellite.SatelliteOreMapping sat =
                            new zmaster587.advancedRocketry.satellite.SatelliteOreMapping();
                    satId = System.nanoTime();
                    sat.getProperties().setId(satId);
                    props.addSatellite(sat, satWorld);
                }
            }

            net.minecraft.item.Item scanner =
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemOreScanner;
            net.minecraft.item.ItemStack held = new net.minecraft.item.ItemStack(scanner);
            if (satId != -1) {
                ((zmaster587.advancedRocketry.item.ItemOreScanner) scanner)
                        .setSatelliteID(held, satId);
            }
            player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, held);

            String error = null;
            try {
                scanner.onItemRightClick(player.world, player, net.minecraft.util.EnumHand.MAIN_HAND);
            } catch (RuntimeException e) {
                error = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            send(sender, "{\"ok\":true"
                    + ",\"hadSatelliteId\":" + (satId != -1)
                    + ",\"satelliteId\":" + satId
                    + ",\"registeredOnDim\":" + satRegisterDim
                    + ",\"error\":" + (error == null ? "null" : "\"" + escapeJson(error) + "\"")
                    + "}");
            return;
        }
        if ("try-biomechanger-rclick".equals(sub) && args.length >= 2) {
            // /artest player try-biomechanger-rclick <dim>
            //
            // Constructs a SatelliteBiomeChanger, registers it on the
            // supplied dim, equips an ItemBiomeChanger with NBT pointing
            // to that satellite, then invokes onItemRightClick. The
            // contract pinned by callers: after the right-click, the
            // satellite's writeToNBT must emit a "posList" int-array
            // populated with positions to change (save-format contract).
            // An empty posList means production short-circuited
            // performAction or stopped queuing positions — both are
            // player-visible regressions (the BiomeChanger silently does
            // nothing after right-click).
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger sat =
                    new zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger();
            long satId = System.nanoTime();
            sat.getProperties().setId(satId);
            zmaster587.advancedRocketry.dimension.DimensionProperties props =
                    zmaster587.advancedRocketry.dimension.DimensionManager.getInstance()
                            .getDimensionProperties(dim);
            if (props == null) {
                send(sender, "{\"error\":\"no DimensionProperties for dim\",\"dim\":" + dim + "}");
                return;
            }
            props.addSatellite(sat, world);

            net.minecraft.item.Item chip =
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemBiomeChanger;
            net.minecraft.item.ItemStack held = new net.minecraft.item.ItemStack(chip);
            net.minecraft.nbt.NBTTagCompound chipNbt = new net.minecraft.nbt.NBTTagCompound();
            chipNbt.setString("satelliteName", sat.getName());
            chipNbt.setInteger("dimId", dim);
            chipNbt.setLong("satelliteId", satId);
            held.setTagCompound(chipNbt);
            player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, held);

            int posListBefore;
            {
                net.minecraft.nbt.NBTTagCompound snap = new net.minecraft.nbt.NBTTagCompound();
                sat.writeToNBT(snap);
                posListBefore = snap.getIntArray("posList").length;
            }

            net.minecraft.util.ActionResult<net.minecraft.item.ItemStack> res =
                    chip.onItemRightClick(world, player, net.minecraft.util.EnumHand.MAIN_HAND);

            int posListAfter;
            {
                net.minecraft.nbt.NBTTagCompound snap = new net.minecraft.nbt.NBTTagCompound();
                sat.writeToNBT(snap);
                posListAfter = snap.getIntArray("posList").length;
            }
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"dim\":" + dim
                    + ",\"satId\":" + satId
                    + ",\"result\":\"" + res.getType().name() + "\""
                    + ",\"posListBefore\":" + posListBefore
                    + ",\"posListAfter\":" + posListAfter
                    + ",\"posListDelta\":" + (posListAfter - posListBefore)
                    + "}");
            return;
        }
        if ("try-hovercraft".equals(sub) && args.length >= 7) {
            // /artest player try-hovercraft <dim> <px> <py> <pz> <yaw> <pitch>
            //
            // Teleports the player to the given location/angles, equips
            // ItemHovercraft, and invokes onItemRightClick. The production
            // path ray-traces 5 blocks forward from the player's eye, and
            // spawns an EntityHoverCraft at the hit position. Returns
            // result code, EntityHoverCraft count delta (snapshot
            // before/after), and the held-stack count after — tests
            // confirm both the spawn and the consumption contract.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            double px = parseDoubleOr(args[2], 0);
            double py = parseDoubleOr(args[3], 0);
            double pz = parseDoubleOr(args[4], 0);
            float yaw = (float) parseDoubleOr(args[5], 0);
            float pitch = (float) parseDoubleOr(args[6], 0);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            // Force survival so the consumption branch fires.
            player.setGameType(net.minecraft.world.GameType.SURVIVAL);
            // setLocationAndAngles updates pos + prev{Pos,Rotation} so the
            // onItemRightClick lerp (prevPosX + (posX - prevPosX)) gives
            // exactly the target pos / angles instead of a tween from the
            // previous tick's frame.
            player.setLocationAndAngles(px, py, pz, yaw, pitch);
            player.prevPosX = px; player.prevPosY = py; player.prevPosZ = pz;
            player.lastTickPosX = px; player.lastTickPosY = py; player.lastTickPosZ = pz;
            player.prevRotationYaw = yaw; player.prevRotationPitch = pitch;

            net.minecraft.item.Item hover =
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemHovercraft;
            player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND,
                    new net.minecraft.item.ItemStack(hover));

            com.google.common.base.Predicate<net.minecraft.entity.Entity> alwaysTrue =
                    com.google.common.base.Predicates.alwaysTrue();
            int before = world.getEntities(
                    zmaster587.advancedRocketry.entity.EntityHoverCraft.class, alwaysTrue).size();

            net.minecraft.util.ActionResult<net.minecraft.item.ItemStack> res =
                    hover.onItemRightClick(world, player, net.minecraft.util.EnumHand.MAIN_HAND);

            int after = world.getEntities(
                    zmaster587.advancedRocketry.entity.EntityHoverCraft.class, alwaysTrue).size();
            int heldAfter = player.getHeldItem(net.minecraft.util.EnumHand.MAIN_HAND).getCount();
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"dim\":" + dim
                    + ",\"result\":\"" + res.getType().name() + "\""
                    + ",\"entitiesBefore\":" + before
                    + ",\"entitiesAfter\":" + after
                    + ",\"entityDelta\":" + (after - before)
                    + ",\"heldAfter\":" + heldAfter
                    + ",\"creative\":" + player.capabilities.isCreativeMode
                    + "}");
            return;
        }
        if ("try-atm-analyze".equals(sub) && args.length >= 2) {
            // /artest player try-atm-analyze <dim>
            //
            // Equips ItemAtmosphereAnalzer and invokes its server-side
            // onItemRightClick against the supplied dim. Production sends
            // TWO messages: a "%s %s %s" wrapping (msg.atmanal.atmtype,
            // <atm-name>, pressure-string) followed by a "%s %s" wrapping
            // (msg.atmanal.canbreathe, msg.yes|msg.no). Both are captured
            // by the chat-tap and returned as a JSON array of joined
            // translation-key chains (newest first).
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            installChatTap(player);
            chatLog.clear();
            net.minecraft.item.Item analyzer =
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemAtmAnalyser;
            player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND,
                    new net.minecraft.item.ItemStack(analyzer));
            net.minecraft.util.ActionResult<net.minecraft.item.ItemStack> res =
                    analyzer.onItemRightClick(world, player, net.minecraft.util.EnumHand.MAIN_HAND);
            flushPlayerChannel(player);
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (String k : chatLog) {
                if (!first) sb.append(',');
                sb.append('"').append(escapeJson(k)).append('"');
                first = false;
            }
            sb.append(']');
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"dim\":" + dim
                    + ",\"result\":\"" + res.getType().name() + "\""
                    + ",\"messageCount\":" + chatLog.size()
                    + ",\"messages\":" + sb.toString()
                    + "}");
            return;
        }
        if ("give-suit-chest".equals(sub)) {
            // Equip a fresh full-air space-suit chestplate into the
            // player's CHEST armor slot. The 6th-arg `air` (optional)
            // sets a specific air buffer for drain tests; defaults to
            // the configured max.
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemSpaceSuit_Chest);
            int air = args.length >= 2 ? parseIntOr(args[1], -1) : -1;
            if (air >= 0) {
                zmaster587.advancedRocketry.util.ItemAirUtils.INSTANCE
                        .setAirRemaining(stack, air);
            } else {
                // Trigger getAirRemaining once to initialise the NBT to max.
                zmaster587.advancedRocketry.util.ItemAirUtils.INSTANCE
                        .getAirRemaining(stack);
            }
            player.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.CHEST, stack);
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"chestSlot\":\""
                    + escapeJson(stack.getItem().getRegistryName().toString()) + "\""
                    + ",\"chestAir\":"
                    + zmaster587.advancedRocketry.util.ItemAirUtils.INSTANCE.getAirRemaining(stack)
                    + "}");
            return;
        }
        if ("equip-airsuit".equals(sub)) {
            // /artest player equip-airsuit [initialChestAir]
            //
            // Equips four vanilla iron-armor pieces, each enchanted with
            // AdvancedRocketryAPI.enchantmentSpaceProtection — this is
            // the "Path 1" branch of AtmosphereNeedsSuit.protectsFrom
            // (ItemAirUtils.isStackValidAirContainer → enchant-tag check
            // → ItemAirWrapper.protectsFromSubstance), which drains the
            // chest's static "air" NBT key by 1 per AtmosphereVacuum
            // tick (every 10 game ticks). The held-air probe reads
            // that same "air" NBT.
            //
            // Why not itemSpaceSuit_Chest: ItemSpaceChest goes through
            // the capability branch and stores its O2 buffer as oxygen
            // fluid inside an embedded fluid-tank inventory — drain
            // setup would require also seeding the embedded inventory.
            // Enchanted vanilla armor is the cleanest fixture for
            // pinning the drain contract end-to-end.
            int initialChestAir = args.length >= 2 ? parseIntOr(args[1], 1000) : 1000;
            net.minecraft.enchantment.Enchantment ench =
                    zmaster587.advancedRocketry.api.AdvancedRocketryAPI.enchantmentSpaceProtection;
            if (ench == null) {
                send(sender, "{\"error\":\"enchantmentSpaceProtection is null — AR not initialised?\"}");
                return;
            }
            net.minecraft.item.ItemStack helm =
                    new net.minecraft.item.ItemStack(net.minecraft.init.Items.IRON_HELMET);
            helm.addEnchantment(ench, 1);
            net.minecraft.item.ItemStack chest =
                    new net.minecraft.item.ItemStack(net.minecraft.init.Items.IRON_CHESTPLATE);
            chest.addEnchantment(ench, 1);
            zmaster587.advancedRocketry.util.ItemAirUtils.INSTANCE
                    .setAirRemaining(chest, initialChestAir);
            net.minecraft.item.ItemStack legs =
                    new net.minecraft.item.ItemStack(net.minecraft.init.Items.IRON_LEGGINGS);
            legs.addEnchantment(ench, 1);
            net.minecraft.item.ItemStack feet =
                    new net.minecraft.item.ItemStack(net.minecraft.init.Items.IRON_BOOTS);
            feet.addEnchantment(ench, 1);
            player.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD, helm);
            player.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.CHEST, chest);
            player.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.LEGS, legs);
            player.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.FEET, feet);
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"chestSlot\":\""
                    + escapeJson(chest.getItem().getRegistryName().toString()) + "\""
                    + ",\"initialChestAir\":" + initialChestAir
                    + ",\"chestAir\":"
                    + zmaster587.advancedRocketry.util.ItemAirUtils.INSTANCE.getAirRemaining(chest)
                    + "}");
            return;
        }
        if ("held-air-component-route".equals(sub)) {
            // /artest player held-air-component-route
            //
            // Reads the player's chest stack's air via the IFillableArmor
            // surface of the chest's Item class — NOT via ItemAirUtils'
            // static "air" NBT key (which is only the enchanted-vanilla
            // path). For ItemSpaceChest this walks the embedded inventory
            // and sums each component's FluidStack amount.
            net.minecraft.item.ItemStack chest = player.getItemStackFromSlot(
                    net.minecraft.inventory.EntityEquipmentSlot.CHEST);
            int chestAir = -1;
            if (!chest.isEmpty()
                    && chest.getItem() instanceof zmaster587.advancedRocketry.api.armor.IFillableArmor) {
                chestAir = ((zmaster587.advancedRocketry.api.armor.IFillableArmor) chest.getItem())
                        .getAirRemaining(chest);
            }
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"chestSlot\":\""
                    + escapeJson(chest.isEmpty() ? "" : chest.getItem().getRegistryName().toString())
                    + "\""
                    + ",\"chestAir\":" + chestAir + "}");
            return;
        }
        if ("equip-space-chest".equals(sub)) {
            // /artest player equip-space-chest [pressureTankOxygenAmount]
            //
            // TASK-24 — equip the player with the AR ItemSpaceChest carrying
            // an oxygen-filled ItemPressureTank component in slot 0. The
            // pressure tank's FluidStack is what drains in vacuum via
            // ItemSpaceChest.decrementAir (capability route), NOT the
            // chest's top-level "air" NBT (the enchanted-vanilla route
            // pinned by equip-airsuit).
            //
            // Differs from equip-airsuit:
            //   - This places itemSpaceSuit_Chest, not enchanted vanilla.
            //   - Air buffer lives inside the pressure-tank component's
            //     FluidStack, not on the chest's NBT.
            //   - readChestAir via ItemAirUtils still works because that
            //     method dispatches through IFillableArmor.getAirRemaining
            //     which ItemSpaceChest overrides to walk components.
            int initialOxygen = args.length >= 2 ? parseIntOr(args[1], 1000) : 1000;
            net.minecraft.item.Item suitItem =
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemSpaceSuit_Chest;
            net.minecraft.item.Item tankItem =
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemPressureTank;
            if (suitItem == null || tankItem == null) {
                send(sender, "{\"error\":\"AR space-suit items missing (chest="
                        + (suitItem != null) + ", tank=" + (tankItem != null) + ")\"}");
                return;
            }

            net.minecraft.item.ItemStack chest = new net.minecraft.item.ItemStack(suitItem);
            net.minecraft.item.ItemStack tank = new net.minecraft.item.ItemStack(tankItem);

            // Fill the tank with oxygen via its Forge IFluidHandlerItem capability.
            // The capability is created lazily by ItemPressureTank.initCapabilities.
            net.minecraftforge.fluids.capability.IFluidHandlerItem tankFluid =
                    tank.getCapability(net.minecraftforge.fluids.capability
                            .CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY,
                            net.minecraft.util.EnumFacing.UP);
            if (tankFluid == null) {
                send(sender, "{\"error\":\"pressure tank exposes no IFluidHandlerItem capability\"}");
                return;
            }
            int filled = tankFluid.fill(new net.minecraftforge.fluids.FluidStack(
                    zmaster587.advancedRocketry.api.AdvancedRocketryFluids.fluidOxygen,
                    initialOxygen), true);

            // Embed via the production addArmorComponent path so any future
            // validation in onComponentAdded fires as it would in-game.
            ((zmaster587.advancedRocketry.armor.ItemSpaceArmor) suitItem)
                    .addArmorComponent(player.world, chest, tank, 0);

            // Equip ALL 4 suit pieces so AtmosphereNeedsSuit.isImmune returns
            // true (the gate requires leg + feet + helm + chest all protect).
            // Without the other 3, vacuum damage fires before the chest drain
            // ever gets exercised.
            net.minecraft.item.Item helmItem =
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemSpaceSuit_Helmet;
            net.minecraft.item.Item legItem =
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemSpaceSuit_Leggings;
            net.minecraft.item.Item bootItem =
                    zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemSpaceSuit_Boots;
            if (helmItem != null) {
                player.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD,
                        new net.minecraft.item.ItemStack(helmItem));
            }
            if (legItem != null) {
                player.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.LEGS,
                        new net.minecraft.item.ItemStack(legItem));
            }
            if (bootItem != null) {
                player.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.FEET,
                        new net.minecraft.item.ItemStack(bootItem));
            }
            player.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.CHEST, chest);

            int readBack = ((zmaster587.advancedRocketry.api.armor.IFillableArmor) suitItem)
                    .getAirRemaining(chest);
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\""
                    + ",\"chestSlot\":\""
                    + escapeJson(chest.getItem().getRegistryName().toString()) + "\""
                    + ",\"requestedOxygen\":" + initialOxygen
                    + ",\"tankFilled\":" + filled
                    + ",\"chestAir\":" + readBack + "}");
            return;
        }
        if ("mount-entity".equals(sub) && args.length >= 2) {
            // /artest player mount-entity <entityId>
            //
            // TASK-20 P1 — start the player riding the given entity.
            // Bridges the testClient bot's lack of "right-click on
            // entity" interaction by calling startRiding server-side.
            // Observable result identical: player.getRidingEntity()
            // == that entity.
            int entityId = parseIntOr(args[1], Integer.MIN_VALUE);
            net.minecraft.entity.Entity entity = player.world.getEntityByID(entityId);
            if (entity == null) {
                send(sender, "{\"error\":\"entity not found\",\"entityId\":" + entityId + "}");
                return;
            }
            boolean mounted = player.startRiding(entity);
            send(sender, "{\"ok\":true,\"mounted\":" + mounted
                    + ",\"ridingEntityId\":" + (player.getRidingEntity() == null
                            ? -1 : player.getRidingEntity().getEntityId()) + "}");
            return;
        }
        if ("dismount".equals(sub)) {
            // /artest player dismount — dismount the player from any
            // ridden entity. TASK-20 P1 — bridges the bot's lack of
            // "sneak input" by calling dismountRidingEntity server-side.
            net.minecraft.entity.Entity wasRiding = player.getRidingEntity();
            int wasRidingId = wasRiding == null ? -1 : wasRiding.getEntityId();
            player.dismountRidingEntity();
            send(sender, "{\"ok\":true"
                    + ",\"wasRidingId\":" + wasRidingId
                    + ",\"ridingEntityIdNow\":" + (player.getRidingEntity() == null
                            ? -1 : player.getRidingEntity().getEntityId()) + "}");
            return;
        }
        if ("riding-entity".equals(sub)) {
            // /artest player riding-entity — observability probe for
            // the player's current riding state.
            net.minecraft.entity.Entity riding = player.getRidingEntity();
            send(sender, "{\"ok\":true"
                    + ",\"ridingEntityId\":" + (riding == null ? -1 : riding.getEntityId())
                    + ",\"ridingEntityClass\":\""
                    + escapeJson(riding == null ? "" : riding.getClass().getName())
                    + "\"}");
            return;
        }
        if ("exec-as-player".equals(sub) && args.length >= 1) {
            // /artest player exec-as-player <command-and-args...>
            //
            // TASK-21 — runs a command via the server's command manager
            // with the bot's player as the sender. Used to drive /ar
            // player-equipped verbs (goto, giveStation, addTorch,
            // fillData, addSolidBlockOverride) which gate on "sender
            // instanceof Entity". The bot must already have op (use
            // op-self probe below).
            //
            // The whole rest of args[] is concatenated with spaces and
            // sent as a single command string (matching how chat-
            // command parsing works).
            StringBuilder cmd = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) cmd.append(' ');
                cmd.append(args[i]);
            }
            int result = server.getCommandManager().executeCommand(player, cmd.toString());
            send(sender, "{\"ok\":true"
                    + ",\"command\":\"" + escapeJson(cmd.toString()) + "\""
                    + ",\"result\":" + result
                    + ",\"playerDim\":" + player.world.provider.getDimension()
                    + ",\"playerPosX\":" + player.posX
                    + ",\"playerPosY\":" + player.posY
                    + ",\"playerPosZ\":" + player.posZ + "}");
            return;
        }
        if ("op-self".equals(sub)) {
            // /artest player op-self — elevate the bot's player to op
            // level 4 in the server's PlayerList. Reset by removing
            // from ops after the test (via deop-self).
            server.getPlayerList().addOp(player.getGameProfile());
            send(sender, "{\"ok\":true,\"opped\":true"
                    + ",\"playerName\":\"" + escapeJson(player.getName()) + "\"}");
            return;
        }
        if ("deop-self".equals(sub)) {
            server.getPlayerList().removeOp(player.getGameProfile());
            send(sender, "{\"ok\":true,\"opped\":false}");
            return;
        }
        if ("inventory-contains".equals(sub) && args.length >= 2) {
            // /artest player inventory-contains <item-registry-name>
            //
            // Returns true if the bot's main inventory has at least one
            // ItemStack of the given item. Used by /ar giveStation
            // positive test to verify the chip was added.
            String registryName = args[1];
            net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.getValue(
                    new ResourceLocation(registryName));
            int count = 0;
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                net.minecraft.item.ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem().getRegistryName() != null
                        && stack.getItem().getRegistryName().toString().equals(registryName)) {
                    count += stack.getCount();
                }
            }
            send(sender, "{\"ok\":true"
                    + ",\"item\":\"" + escapeJson(registryName) + "\""
                    + ",\"count\":" + count + "}");
            return;
        }
        if ("give-held".equals(sub) && args.length >= 2) {
            // /artest player give-held <item-registry-name>
            //
            // Equip the named item in the player's main hand. Used to
            // set up the /ar addTorch / fillData positive paths which
            // require a specific held item.
            String registryName = args[1];
            net.minecraft.item.Item item =
                    net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS
                            .getValue(new ResourceLocation(registryName));
            if (item == null) {
                send(sender, "{\"error\":\"unknown item\",\"name\":\""
                        + escapeJson(registryName) + "\"}");
                return;
            }
            player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND,
                    new net.minecraft.item.ItemStack(item));
            send(sender, "{\"ok\":true,\"held\":\"" + escapeJson(registryName) + "\"}");
            return;
        }
        if ("drive-ridden-entity".equals(sub) && args.length >= 3) {
            // /artest player drive-ridden-entity <moveForward> <ticks>
            //
            // TASK-20 P2 — composite probe that re-applies
            // player.moveForward immediately before each entity.onUpdate
            // call. The standalone set-move-forward probe is racy in
            // testClient because the bot client's CPacketInput stream
            // resets the field between probe round-trips. This probe
            // keeps the field stable across the whole tick burst by
            // setting it inline.
            float forward = (float) parseDoubleOr(args[1], 0.0);
            int ticks = Math.max(1, parseIntOr(args[2], 1));
            net.minecraft.entity.Entity ridden = player.getRidingEntity();
            if (ridden == null) {
                send(sender, "{\"error\":\"player not riding any entity\"}");
                return;
            }
            int ticked = 0;
            for (int i = 0; i < ticks; i++) {
                if (ridden.isDead) break;
                player.moveForward = forward;
                ridden.onUpdate();
                ticked++;
            }
            send(sender, "{\"ok\":true,\"ticked\":" + ticked
                    + ",\"moveForward\":" + player.moveForward
                    + ",\"riddenIsDead\":" + ridden.isDead
                    + ",\"riddenPosX\":" + ridden.posX
                    + ",\"riddenPosY\":" + ridden.posY
                    + ",\"riddenPosZ\":" + ridden.posZ + "}");
            return;
        }
        if ("set-move-forward".equals(sub) && args.length >= 2) {
            // /artest player set-move-forward <value>
            //
            // TASK-20 P2 — set the player's moveForward input field
            // server-side. EntityHoverCraft.onUpdate reads
            // player.moveForward via getPassengerMovingForward; setting
            // it directly drives the throttle without needing client-
            // side W-key simulation (which our testClient ClientBot
            // does not support).
            float value = (float) parseDoubleOr(args[1], 0.0);
            player.moveForward = value;
            send(sender, "{\"ok\":true,\"moveForward\":" + player.moveForward + "}");
            return;
        }
        if ("clear-armor".equals(sub)) {
            // /artest player clear-armor — empty all four armor slots.
            // Used by drain counter-tests where the player must be
            // bare-skinned in vacuum to observe the no-suit branch.
            for (net.minecraft.inventory.EntityEquipmentSlot s : new net.minecraft.inventory.EntityEquipmentSlot[]{
                    net.minecraft.inventory.EntityEquipmentSlot.HEAD,
                    net.minecraft.inventory.EntityEquipmentSlot.CHEST,
                    net.minecraft.inventory.EntityEquipmentSlot.LEGS,
                    net.minecraft.inventory.EntityEquipmentSlot.FEET}) {
                player.setItemStackToSlot(s, net.minecraft.item.ItemStack.EMPTY);
            }
            send(sender, "{\"ok\":true,\"player\":\""
                    + escapeJson(player.getName()) + "\"}");
            return;
        }
        send(sender, "{\"error\":\"unknown player subcommand — try inv-bypass <add|remove|status> | open-container | health | set-health <hp> | held-air | give-suit-chest [air] | equip-airsuit [air] | clear-armor | advancement <id> | advancement reset <id> | last-chat | chat-clear | try-seal-detect <dim> <x> <y> <z> | try-atm-analyze <dim> | try-hovercraft <dim> <px> <py> <pz> <yaw> <pitch> | try-biomechanger-rclick <dim>\"}");
    }

    // ── chat-tap (TASK-10b Phase 7) ──────────────────────────────────────
    //
    // Bounded deque of translation keys (or unformatted text) captured
    // from outbound SPacketChat packets sent to tapped players. Tests
    // observe a player-visible chat message by:
    //   1) /artest player chat-clear           (drain stale entries)
    //   2) trigger production code that fires player.sendMessage(...)
    //   3) /artest player last-chat            (read head of deque)
    //
    // Capture happens at the Netty pipeline level so any production
    // path that eventually calls EntityPlayerMP.sendMessage(ITextComponent)
    // is observed — there's no production-side instrumentation to
    // forget to add.
    private static final java.util.concurrent.ConcurrentLinkedDeque<String> chatLog =
            new java.util.concurrent.ConcurrentLinkedDeque<>();
    private static final String CHAT_TAP_HANDLER_NAME = "ar-test-chat-tap";
    private static final int CHAT_LOG_MAX = 64;

    private static io.netty.channel.Channel playerChannel(net.minecraft.entity.player.EntityPlayerMP player) {
        net.minecraft.network.NetworkManager nm = player.connection.netManager;
        java.lang.reflect.Field f;
        try {
            f = net.minecraft.network.NetworkManager.class.getDeclaredField("channel");
        } catch (NoSuchFieldException ignored) {
            try {
                f = net.minecraft.network.NetworkManager.class.getDeclaredField("field_150746_c");
            } catch (NoSuchFieldException nested) {
                return null;
            }
        }
        f.setAccessible(true);
        try {
            return (io.netty.channel.Channel) f.get(nm);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static void installChatTap(net.minecraft.entity.player.EntityPlayerMP player) {
        // Idempotency is keyed on the live channel's pipeline rather than
        // a per-UUID flag because the FG6 client harness may reconnect
        // mid-suite (new channel, same UUID); a UUID-set would then leave
        // the new channel untapped.
        io.netty.channel.Channel ch = playerChannel(player);
        if (ch == null) return;
        if (ch.pipeline().get(CHAT_TAP_HANDLER_NAME) != null) return;
        // addLast: in Netty, outbound events flow tail->head, so addLast
        // puts us at the very source of outbound writes — we see the
        // SPacketChat BEFORE the PacketEncoder serializes it to a ByteBuf.
        // (addFirst would put us last on outbound, after encoding, where
        // `msg instanceof SPacketChat` is always false.)
        ch.pipeline().addLast(CHAT_TAP_HANDLER_NAME,
                new io.netty.channel.ChannelOutboundHandlerAdapter() {
                    @Override
                    public void write(io.netty.channel.ChannelHandlerContext ctx,
                                      Object msg,
                                      io.netty.channel.ChannelPromise promise) throws Exception {
                        if (msg instanceof net.minecraft.network.play.server.SPacketChat) {
                            net.minecraft.util.text.ITextComponent comp =
                                    readSPacketChatComponent((net.minecraft.network.play.server.SPacketChat) msg);
                            if (comp != null) {
                                String key = componentKey(comp);
                                // Drop command-echo broadcasts ("Player issued
                                // server command: /artest …"). Every /artest
                                // call triggers one of these, which would
                                // otherwise drown the player-visible chat the
                                // tests want to observe. startsWith because
                                // componentKey now joins nested translation
                                // keys with "|" — the announcement carries the
                                // player name + raw command as nested args, so
                                // the captured key will be e.g.
                                // "chat.type.announcement|...".
                                if (key != null && !key.startsWith("chat.type.announcement")) {
                                    chatLog.offerFirst(key);
                                    while (chatLog.size() > CHAT_LOG_MAX) chatLog.pollLast();
                                }
                            }
                        }
                        super.write(ctx, msg, promise);
                    }
                });
    }

    // SPacketChat exposes its component as `getChatComponent()` in MCP
    // mappings, `func_148915_a()` in SRG. The deobf transformer is not
    // applied to the testClient runtime classpath, so calling the MCP
    // name compiles but throws NoSuchMethodError at run time. Resolve
    // the method reflectively, caching the lookup, and fall back to
    // direct field access if neither name is available.
    private static volatile java.lang.reflect.Method SPACKETCHAT_GET_COMPONENT;
    private static volatile boolean SPACKETCHAT_LOOKUP_DONE;
    private static volatile java.lang.reflect.Field SPACKETCHAT_COMPONENT_FIELD;

    private static net.minecraft.util.text.ITextComponent readSPacketChatComponent(
            net.minecraft.network.play.server.SPacketChat pkt) {
        if (!SPACKETCHAT_LOOKUP_DONE) {
            synchronized (TestProbeCommand.class) {
                if (!SPACKETCHAT_LOOKUP_DONE) {
                    for (String name : new String[]{"getChatComponent", "func_148915_a"}) {
                        try {
                            java.lang.reflect.Method m =
                                    net.minecraft.network.play.server.SPacketChat.class.getMethod(name);
                            if (net.minecraft.util.text.ITextComponent.class.isAssignableFrom(m.getReturnType())) {
                                m.setAccessible(true);
                                SPACKETCHAT_GET_COMPONENT = m;
                                break;
                            }
                        } catch (NoSuchMethodException ignored) { /* try next */ }
                    }
                    if (SPACKETCHAT_GET_COMPONENT == null) {
                        for (String fname : new String[]{"chatComponent", "field_148919_a"}) {
                            try {
                                java.lang.reflect.Field f =
                                        net.minecraft.network.play.server.SPacketChat.class.getDeclaredField(fname);
                                f.setAccessible(true);
                                SPACKETCHAT_COMPONENT_FIELD = f;
                                break;
                            } catch (NoSuchFieldException ignored) { /* try next */ }
                        }
                    }
                    SPACKETCHAT_LOOKUP_DONE = true;
                }
            }
        }
        try {
            if (SPACKETCHAT_GET_COMPONENT != null) {
                return (net.minecraft.util.text.ITextComponent) SPACKETCHAT_GET_COMPONENT.invoke(pkt);
            }
            if (SPACKETCHAT_COMPONENT_FIELD != null) {
                return (net.minecraft.util.text.ITextComponent) SPACKETCHAT_COMPONENT_FIELD.get(pkt);
            }
        } catch (ReflectiveOperationException ignored) { /* fall through */ }
        return null;
    }

    /** Returns a stable handle for a chat component without rendering it
     *  through the i18n table. For a plain TextComponentTranslation we
     *  emit just the key (e.g. {@code msg.sealdetector.sealed}). For a
     *  composite translation whose key has %s placeholders filled by
     *  child translations (e.g. AtmosphereAnalzer's
     *  {@code "%s %s %s"} wrapping {@code msg.atmanal.atmtype} + atmType
     *  name + pressure), we recursively walk the format args + siblings
     *  and join every nested translation key with {@code |}. Result:
     *  {@code "%s %s %s|msg.atmanal.atmtype|air"} — tests can pin on
     *  presence of any inner key without depending on i18n output.
     *  Falls back to unformatted text when no translations are found. */
    private static String componentKey(net.minecraft.util.text.ITextComponent comp) {
        StringBuilder sb = new StringBuilder();
        collectTranslationKeys(comp, sb);
        if (sb.length() > 0) return sb.toString();
        return comp.getUnformattedComponentText();
    }

    private static void collectTranslationKeys(net.minecraft.util.text.ITextComponent comp, StringBuilder sb) {
        if (comp == null) return;
        if (comp instanceof net.minecraft.util.text.TextComponentTranslation) {
            net.minecraft.util.text.TextComponentTranslation tct =
                    (net.minecraft.util.text.TextComponentTranslation) comp;
            if (sb.length() > 0) sb.append('|');
            sb.append(tct.getKey());
            for (Object arg : tct.getFormatArgs()) {
                if (arg instanceof net.minecraft.util.text.ITextComponent) {
                    collectTranslationKeys((net.minecraft.util.text.ITextComponent) arg, sb);
                }
            }
        }
        for (net.minecraft.util.text.ITextComponent sib : comp.getSiblings()) {
            collectTranslationKeys(sib, sb);
        }
    }

    /** Submits a no-op to the player's Netty event-loop and blocks for
     *  it to run, ensuring any prior queued packet writes (and the
     *  chat-tap's deque mutation) have executed before we read. */
    private static void flushPlayerChannel(net.minecraft.entity.player.EntityPlayerMP player) {
        io.netty.channel.Channel ch = playerChannel(player);
        if (ch == null) return;
        try {
            ch.eventLoop().submit(() -> null)
                    .get(500, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // best-effort; tests have their own retry/wait loop
        }
    }

    /** {@code msg.sealdetector.notsealmat} → {@code notsealmat}. Returns
     *  null when the key doesn't carry the SealDetector prefix. Lets
     *  tests assert on a clean branch name without re-parsing the key. */
    private static String stripBranchPrefix(String key) {
        if (key == null) return null;
        final String prefix = "msg.sealdetector.";
        if (key.startsWith(prefix)) return key.substring(prefix.length());
        return null;
    }

    // §7.18 — generic block-state probe ---------------------------------------

    /**
     * {@code /artest block at <dim> <x> <y> <z>} — returns the block registry
     * name + meta at a position. Used by tests that need to assert on world
     * blockstate changes (e.g. force-field projection, terraformer block
     * mutation) without going through a tile entity.
     */
    private void handleBlock(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 5
                || !("at".equalsIgnoreCase(args[0]) || "biome-at".equalsIgnoreCase(args[0]))) {
            send(sender, "{\"error\":\"unknown block subcommand — try at <dim> <x> <y> <z> | biome-at <dim> <x> <y> <z>\"}");
            return;
        }
        boolean biomeMode = "biome-at".equalsIgnoreCase(args[0]);
        int dim = parseIntOr(args[1], Integer.MIN_VALUE);
        int x = parseIntOr(args[2], 0);
        int y = parseIntOr(args[3], 0);
        int z = parseIntOr(args[4], 0);
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (biomeMode) {
            net.minecraft.world.biome.Biome biome = world.getBiome(pos);
            net.minecraft.util.ResourceLocation rn = biome.getRegistryName();
            send(sender, "{\"pos\":[" + x + "," + y + "," + z + "]"
                    + ",\"biome\":\"" + escapeJson(rn == null ? "null" : rn.toString()) + "\""
                    + ",\"biomeId\":" + net.minecraft.world.biome.Biome.getIdForBiome(biome) + "}");
            return;
        }
        net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        net.minecraft.util.ResourceLocation rn = state.getBlock().getRegistryName();
        @SuppressWarnings("deprecation")
        int meta = state.getBlock().getMetaFromState(state);
        send(sender, "{\"pos\":[" + x + "," + y + "," + z + "]"
                + ",\"block\":\"" + escapeJson(rn == null ? "null" : rn.toString()) + "\""
                + ",\"meta\":" + meta
                + ",\"isAir\":" + world.isAirBlock(pos)
                + "}");
    }

    // §7.X — ItemSealDetector dispatch-matrix probe ---------------------------

    /**
     * {@code /artest seal-detector check <dim> <x> <y> <z>} — reports
     * which of the six branches in {@link
     * zmaster587.advancedRocketry.item.ItemSealDetector#onItemUse}
     * (lines 34-50) would fire at the given position. Drives the same
     * {@link zmaster587.advancedRocketry.util.SealableBlockHandler}
     * predicates production uses, in the same order — so any change to
     * SealableBlockHandler is reflected. Only the if/else ordering is
     * replicated here; tests document the cross-reference back to
     * ItemSealDetector so a reordering of production gates is caught
     * during review even if the test still passes.
     *
     * <p>Returns {@code {"branch":"sealed"|"notsealmat"|"notsealblock"
     * |"notfullblock"|"fluid"|"other"}}. The branch name is exactly the
     * suffix of the corresponding {@code msg.sealdetector.&lt;branch&gt;}
     * i18n key the production code emits to the player.</p>
     */
    private void handleSealDetector(net.minecraft.server.MinecraftServer server,
                                    ICommandSender sender, String[] args) {
        if (args.length >= 2 && "add-block-ban".equalsIgnoreCase(args[0])) {
            // /artest seal-detector add-block-ban <block-id>
            String blockId = args[1];
            net.minecraft.block.Block block =
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
            if (block == null) {
                send(sender, "{\"error\":\"unknown block id\",\"id\":\""
                        + escapeJson(blockId) + "\"}");
                return;
            }
            zmaster587.advancedRocketry.util.SealableBlockHandler.INSTANCE
                    .addUnsealableBlock(block);
            send(sender, "{\"ok\":true,\"id\":\"" + escapeJson(blockId)
                    + "\",\"action\":\"added-to-blockBanList\"}");
            return;
        }
        if (args.length >= 2 && "remove-block-ban".equalsIgnoreCase(args[0])) {
            // /artest seal-detector remove-block-ban <block-id> — undo of
            // add-block-ban. Reaches the package-private blockBanList via
            // reflection because SealableBlockHandler has no public removal
            // API for the block ban list (addSealableBlock would also flip
            // into the allow list, which is not the right undo here).
            String blockId = args[1];
            net.minecraft.block.Block block =
                    ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
            if (block == null) {
                send(sender, "{\"error\":\"unknown block id\",\"id\":\""
                        + escapeJson(blockId) + "\"}");
                return;
            }
            try {
                java.lang.reflect.Field f = zmaster587.advancedRocketry.util.SealableBlockHandler
                        .class.getDeclaredField("blockBanList");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<net.minecraft.block.Block> list =
                        (java.util.List<net.minecraft.block.Block>) f.get(
                                zmaster587.advancedRocketry.util.SealableBlockHandler.INSTANCE);
                boolean removed = list.remove(block);
                send(sender, "{\"ok\":true,\"id\":\"" + escapeJson(blockId)
                        + "\",\"removed\":" + removed + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        if (args.length < 5 || !"check".equalsIgnoreCase(args[0])) {
            send(sender, "{\"error\":\"unknown seal-detector subcommand — "
                    + "try check <dim> <x> <y> <z> | add-block-ban <block-id> | "
                    + "remove-block-ban <block-id>\"}");
            return;
        }
        int dim = parseIntOr(args[1], Integer.MIN_VALUE);
        int x = parseIntOr(args[2], 0);
        int y = parseIntOr(args[3], 0);
        int z = parseIntOr(args[4], 0);
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        zmaster587.advancedRocketry.util.SealableBlockHandler h =
                zmaster587.advancedRocketry.util.SealableBlockHandler.INSTANCE;
        String branch;
        if (h.isBlockSealed(world, pos)) {
            branch = "sealed";
        } else {
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            net.minecraft.block.material.Material mat = state.getMaterial();
            if (h.isMaterialBanned(mat)) {
                branch = "notsealmat";
            } else if (h.isBlockBanned(state.getBlock())) {
                branch = "notsealblock";
            } else if (zmaster587.advancedRocketry.util.SealableBlockHandler.isFullBlock(world, pos)) {
                branch = "notfullblock";
            } else if (state.getBlock() instanceof net.minecraftforge.fluids.IFluidBlock) {
                branch = "fluid";
            } else {
                branch = "other";
            }
        }
        send(sender, "{\"pos\":[" + x + "," + y + "," + z + "]"
                + ",\"branch\":\"" + branch + "\"}");
    }

    // §7.19 — mission probe (TASK-06) ----------------------------------------

    /**
     * {@code /artest mission ...} — drives MissionResourceCollection
     * subclasses (MissionGasCollection / MissionOreMining) without
     * requiring a real rocket launch.
     *
     * <p>Verbs:</p>
     * <ul>
     *   <li>{@code start-gas <dim> <rocketEntityId> <duration> <fluidName>}
     *       — construct a MissionGasCollection bound to the rocket, register
     *       on the dim, return the mission's satellite id.</li>
     *   <li>{@code start-ore <dim> <rocketEntityId> <duration> <drillingPower>}
     *       — analogue for MissionOreMining; injects an ItemAsteroidChip
     *       into the rocket's guidance computer with mid-range data values
     *       and the requested drilling-power into the rocket's StatsRocket.</li>
     *   <li>{@code state <missionId>} — JSON dump of mission progress + state.</li>
     *   <li>{@code advance <missionId> <ticks>} — backdates
     *       {@code startWorldTime} by the given tick count, observationally
     *       equivalent to advancing world time but deterministic + cheap.</li>
     *   <li>{@code complete-now <missionId>} — advances until progress reaches
     *       1.0 then drives one {@code tickEntity()} to fire side effects.</li>
     *   <li>{@code rocket-cargo <missionId>} — after completion, scan launch
     *       coords for the respawned rocket entity and report its fluid +
     *       inventory tile contents as JSON.</li>
     *   <li>{@code infra-state <missionId>} — list infrastructureCoords + how
     *       many resolve to live IInfrastructure tiles currently pointing
     *       back at this mission via {@code getLinkedMission()}.</li>
     *   <li>{@code rocket-relink-state <dim>} — class-filtered scan
     *       of the launch dim for EntityStationDeployedRocket entities
     *       (post-completion respawn target), reporting each rocket's
     *       infrastructureCoords list. Unlike {@code rocket-cargo} this
     *       is not bbox-limited, so it finds the respawned rocket even
     *       when production positions it at world origin (vanilla
     *       EntityRocket's writeMissionPersistentNBT no-op default).</li>
     * </ul>
     *
     * <p>Reads/writes the mission's package-private fields
     * ({@code startWorldTime}, {@code duration}, {@code x/y/z},
     * {@code launchDimension}, {@code infrastructureCoords}) via reflection
     * — the contract being pinned is the player-visible save/lifecycle
     * shape, not the internal field naming. If a future refactor renames
     * these, this probe needs updating but the test assertions don't.</p>
     */
    private void handleMission(net.minecraft.server.MinecraftServer server,
                               ICommandSender sender, String[] args) {
        if (args.length == 0) {
            send(sender, "{\"error\":\"missing mission subcommand — try start-gas | start-ore | state | advance | complete-now | rocket-cargo | link-infra | infra-state | rocket-relink-state\"}");
            return;
        }
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        try {
            if ("start-gas".equals(sub) && args.length >= 5) {
                // /artest mission start-gas <dim> <rocketEntityId> <duration> <fluidName> [intakePower]
                // intakePower defaults to 0 (matches a freshly assembled rocket
                // with no intake module). Set > 0 to exercise the fluid-fill
                // branch in MissionGasCollection.onMissionComplete.
                int dim = parseIntOr(args[1], Integer.MIN_VALUE);
                int rocketId = parseIntOr(args[2], -1);
                long duration = (long) parseDoubleOr(args[3], 0);
                String fluidName = args[4];
                int intakePower = args.length >= 6 ? parseIntOr(args[5], 0) : 0;
                net.minecraft.world.WorldServer world = server.getWorld(dim);
                if (world == null) {
                    send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                    return;
                }
                net.minecraft.entity.Entity ent = world.getEntityByID(rocketId);
                if (!(ent instanceof zmaster587.advancedRocketry.entity.EntityRocket)) {
                    send(sender, "{\"error\":\"entity " + rocketId + " is not an EntityRocket\"}");
                    return;
                }
                zmaster587.advancedRocketry.entity.EntityRocket rocket =
                        (zmaster587.advancedRocketry.entity.EntityRocket) ent;
                net.minecraftforge.fluids.Fluid fluid =
                        net.minecraftforge.fluids.FluidRegistry.getFluid(fluidName);
                if (fluid == null) {
                    send(sender, "{\"error\":\"unknown fluid\",\"name\":\"" + escapeJson(fluidName) + "\"}");
                    return;
                }
                // Set intakePower BEFORE the mission ctor so the mission's
                // rocketStats reference reads the configured value at
                // completion time. StatsRocket.setStatTag(name, int) writes
                // the named tag in the NBT-keyed tag map.
                rocket.stats.setStatTag("intakePower", intakePower);
                java.util.LinkedList<zmaster587.advancedRocketry.api.IInfrastructure> infra =
                        new java.util.LinkedList<>();
                zmaster587.advancedRocketry.mission.MissionGasCollection mission =
                        new zmaster587.advancedRocketry.mission.MissionGasCollection(
                                duration, rocket, infra, fluid);
                mission.setDimensionId(dim);
                zmaster587.advancedRocketry.dimension.DimensionProperties props =
                        zmaster587.advancedRocketry.dimension.DimensionManager.getInstance()
                                .getDimensionProperties(dim);
                if (props == null) {
                    send(sender, "{\"error\":\"no DimensionProperties for dim\",\"dim\":" + dim + "}");
                    return;
                }
                props.addSatellite(mission, world);
                send(sender, "{\"ok\":true,\"missionId\":" + mission.getId()
                        + ",\"dim\":" + dim
                        + ",\"duration\":" + duration
                        + ",\"gas\":\"" + escapeJson(fluidName) + "\""
                        + ",\"intakePower\":" + intakePower
                        + ",\"type\":\"gas\"}");
                return;
            }
            if ("start-ore".equals(sub) && args.length >= 5) {
                int dim = parseIntOr(args[1], Integer.MIN_VALUE);
                int rocketId = parseIntOr(args[2], -1);
                long duration = (long) parseDoubleOr(args[3], 0);
                float drillingPower = (float) parseDoubleOr(args[4], 0);
                net.minecraft.world.WorldServer world = server.getWorld(dim);
                if (world == null) {
                    send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                    return;
                }
                net.minecraft.entity.Entity ent = world.getEntityByID(rocketId);
                if (!(ent instanceof zmaster587.advancedRocketry.entity.EntityRocket)) {
                    send(sender, "{\"error\":\"entity " + rocketId + " is not an EntityRocket\"}");
                    return;
                }
                zmaster587.advancedRocketry.entity.EntityRocket rocket =
                        (zmaster587.advancedRocketry.entity.EntityRocket) ent;
                // Equip a programmed asteroid chip in the guidance computer's
                // slot 0 with full max-data values so production's random
                // rolls (distance/composition/mass over maxData) effectively
                // always fire. The chip's "type" is set to a sentinel; if no
                // asteroid type is registered for it production will short
                // circuit on `asteroid != null` and skip the harvest fill —
                // chip-replacement still runs (the post-condition tests).
                net.minecraft.item.ItemStack chipStack = new net.minecraft.item.ItemStack(
                        zmaster587.advancedRocketry.api.AdvancedRocketryItems.itemAsteroidChip);
                zmaster587.advancedRocketry.item.ItemAsteroidChip chip =
                        (zmaster587.advancedRocketry.item.ItemAsteroidChip) chipStack.getItem();
                chip.setMaxData(chipStack, 100);
                chip.setData(chipStack, 100, zmaster587.advancedRocketry.api.DataStorage.DataType.DISTANCE);
                chip.setData(chipStack, 100, zmaster587.advancedRocketry.api.DataStorage.DataType.COMPOSITION);
                chip.setData(chipStack, 100, zmaster587.advancedRocketry.api.DataStorage.DataType.MASS);
                chip.setType(chipStack, "ar-test-fixture");
                chip.setUUID(chipStack, System.nanoTime());
                if (rocket.storage == null) {
                    send(sender, "{\"error\":\"rocket has null storage chunk\"}");
                    return;
                }
                zmaster587.advancedRocketry.tile.TileGuidanceComputer gc =
                        rocket.storage.getGuidanceComputer();
                if (gc == null) {
                    send(sender, "{\"error\":\"rocket has no guidance computer\"}");
                    return;
                }
                gc.setInventorySlotContents(0, chipStack);
                rocket.stats.setDrillingPower(drillingPower);

                java.util.LinkedList<zmaster587.advancedRocketry.api.IInfrastructure> infra =
                        new java.util.LinkedList<>();
                zmaster587.advancedRocketry.mission.MissionOreMining mission =
                        new zmaster587.advancedRocketry.mission.MissionOreMining(
                                duration, rocket, infra);
                mission.setDimensionId(dim);
                zmaster587.advancedRocketry.dimension.DimensionProperties props =
                        zmaster587.advancedRocketry.dimension.DimensionManager.getInstance()
                                .getDimensionProperties(dim);
                if (props == null) {
                    send(sender, "{\"error\":\"no DimensionProperties for dim\",\"dim\":" + dim + "}");
                    return;
                }
                props.addSatellite(mission, world);
                send(sender, "{\"ok\":true,\"missionId\":" + mission.getId()
                        + ",\"dim\":" + dim
                        + ",\"duration\":" + duration
                        + ",\"drillingPower\":" + drillingPower
                        + ",\"type\":\"ore\"}");
                return;
            }
            if ("state".equals(sub) && args.length >= 2) {
                long missionId = (long) parseDoubleOr(args[1], -1);
                zmaster587.advancedRocketry.mission.MissionResourceCollection m = findMission(missionId);
                if (m == null) {
                    send(sender, "{\"error\":\"mission not found\",\"missionId\":" + missionId + "}");
                    return;
                }
                long startTime = readLongField(m, "startWorldTime");
                long duration = readLongField(m, "duration");
                int worldId = readIntField(m, "worldId");
                int launchDim = readIntField(m, "launchDimension");
                net.minecraft.world.World w = net.minecraftforge.common.DimensionManager.getWorld(m.getDimensionId());
                double progress = w == null ? -1.0 : m.getProgress(w);
                String type = m instanceof zmaster587.advancedRocketry.mission.MissionGasCollection
                        ? "gas"
                        : (m instanceof zmaster587.advancedRocketry.mission.MissionOreMining ? "ore" : "resource");
                java.util.LinkedList<?> infra =
                        (java.util.LinkedList<?>) readObjectField(m, "infrastructureCoords");
                int infraCount = infra == null ? 0 : infra.size();
                send(sender, "{\"ok\":true,\"missionId\":" + missionId
                        + ",\"type\":\"" + type + "\""
                        + ",\"progress\":" + progress
                        + ",\"startWorldTime\":" + startTime
                        + ",\"duration\":" + duration
                        + ",\"worldId\":" + worldId
                        + ",\"launchDim\":" + launchDim
                        + ",\"infraCount\":" + infraCount
                        + ",\"isDead\":" + m.isDead() + "}");
                return;
            }
            if ("advance".equals(sub) && args.length >= 3) {
                long missionId = (long) parseDoubleOr(args[1], -1);
                long ticks = (long) parseDoubleOr(args[2], 0);
                zmaster587.advancedRocketry.mission.MissionResourceCollection m = findMission(missionId);
                if (m == null) {
                    send(sender, "{\"error\":\"mission not found\",\"missionId\":" + missionId + "}");
                    return;
                }
                long startTime = readLongField(m, "startWorldTime");
                writeLongField(m, "startWorldTime", startTime - ticks);
                net.minecraft.world.World w = net.minecraftforge.common.DimensionManager.getWorld(m.getDimensionId());
                double progress = w == null ? -1.0 : m.getProgress(w);
                send(sender, "{\"ok\":true,\"missionId\":" + missionId
                        + ",\"ticksAdvanced\":" + ticks
                        + ",\"newStartWorldTime\":" + (startTime - ticks)
                        + ",\"progress\":" + progress + "}");
                return;
            }
            if ("complete-now".equals(sub) && args.length >= 2) {
                long missionId = (long) parseDoubleOr(args[1], -1);
                zmaster587.advancedRocketry.mission.MissionResourceCollection m = findMission(missionId);
                if (m == null) {
                    send(sender, "{\"error\":\"mission not found\",\"missionId\":" + missionId + "}");
                    return;
                }
                long duration = readLongField(m, "duration");
                net.minecraft.world.World w = net.minecraftforge.common.DimensionManager.getWorld(m.getDimensionId());
                if (w == null) {
                    send(sender, "{\"error\":\"mission dim not loaded\",\"dim\":" + m.getDimensionId() + "}");
                    return;
                }
                long now = w.getTotalWorldTime();
                // Snapshot launch coords + dim BEFORE tickEntity so we can
                // read cargo from the re-spawned rocket atomically — the
                // natural DimensionProperties.tick loop prunes dead
                // satellites between commands, so a follow-up rocket-cargo
                // call would race the prune.
                double lx = readDoubleField(m, "x");
                double ly = readDoubleField(m, "y");
                double lz = readDoubleField(m, "z");
                int launchDim = readIntField(m, "launchDimension");
                // Backdate startWorldTime so progress = 1.0 exactly.
                writeLongField(m, "startWorldTime", now - duration);
                boolean wasDead = m.isDead();
                m.tickEntity();
                // Synchronous cargo readback while we still know the launch
                // coords — even after the prune the respawned rocket entity
                // persists in the launch dim, but the mission registry no
                // longer exposes its coords.
                String cargo = snapshotCargoJson(server, launchDim, lx, ly, lz);
                send(sender, "{\"ok\":true,\"missionId\":" + missionId
                        + ",\"wasDeadBefore\":" + wasDead
                        + ",\"isDeadAfter\":" + m.isDead()
                        + ",\"completed\":" + (!wasDead && m.isDead())
                        + ",\"launchDim\":" + launchDim
                        + ",\"launchPos\":[" + lx + "," + ly + "," + lz + "]"
                        + "," + cargo + "}");
                return;
            }
            if ("rocket-cargo".equals(sub) && args.length >= 2) {
                long missionId = (long) parseDoubleOr(args[1], -1);
                zmaster587.advancedRocketry.mission.MissionResourceCollection m = findMission(missionId);
                if (m == null) {
                    send(sender, "{\"error\":\"mission not found\",\"missionId\":" + missionId + "}");
                    return;
                }
                double lx = readDoubleField(m, "x");
                double ly = readDoubleField(m, "y");
                double lz = readDoubleField(m, "z");
                int launchDim = readIntField(m, "launchDimension");
                String cargo = snapshotCargoJson(server, launchDim, lx, ly, lz);
                send(sender, "{\"ok\":true,\"missionId\":" + missionId
                        + ",\"launchDim\":" + launchDim
                        + ",\"launchPos\":[" + lx + "," + ly + "," + lz + "]"
                        + "," + cargo + "}");
                return;
            }
            if ("link-infra".equals(sub) && args.length >= 6) {
                // /artest mission link-infra <missionId> <dim> <x> <y> <z>
                // Mirrors what EntityRocket.createMission does AFTER the mission
                // ctor: registers the infrastructure tile coord on the mission
                // AND calls tile.linkMission(mission) so the tile starts
                // tracking the mission. Pre-condition: tile at (x,y,z) is
                // already placed and implements IInfrastructure.
                long missionId = (long) parseDoubleOr(args[1], -1);
                int dim = parseIntOr(args[2], Integer.MIN_VALUE);
                int ix = parseIntOr(args[3], 0);
                int iy = parseIntOr(args[4], 0);
                int iz = parseIntOr(args[5], 0);
                zmaster587.advancedRocketry.mission.MissionResourceCollection m = findMission(missionId);
                if (m == null) {
                    send(sender, "{\"error\":\"mission not found\",\"missionId\":" + missionId + "}");
                    return;
                }
                net.minecraft.world.WorldServer w = server.getWorld(dim);
                if (w == null) {
                    send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                    return;
                }
                net.minecraft.tileentity.TileEntity tile = w.getTileEntity(new net.minecraft.util.math.BlockPos(ix, iy, iz));
                if (!(tile instanceof zmaster587.advancedRocketry.api.IInfrastructure)) {
                    send(sender, "{\"error\":\"tile not IInfrastructure\",\"pos\":[" + ix + "," + iy + "," + iz + "]}");
                    return;
                }
                @SuppressWarnings("unchecked")
                java.util.LinkedList<zmaster587.libVulpes.util.HashedBlockPosition> coords =
                        (java.util.LinkedList<zmaster587.libVulpes.util.HashedBlockPosition>) readObjectField(m, "infrastructureCoords");
                coords.add(new zmaster587.libVulpes.util.HashedBlockPosition(ix, iy, iz));
                boolean linked = ((zmaster587.advancedRocketry.api.IInfrastructure) tile).linkMission(m);
                send(sender, "{\"ok\":true,\"missionId\":" + missionId
                        + ",\"linked\":" + linked
                        + ",\"infraCount\":" + coords.size() + "}");
                return;
            }
            if ("infra-state".equals(sub) && args.length >= 5) {
                // /artest mission infra-state <dim> <x> <y> <z>
                // Reads the infrastructure tile's current mission ref via
                // reflection on the package-private `mission` field (present
                // on TileRocketMonitoringStation / TileFuelingStation /
                // TileRocketServiceStation / TileRocketLoader / TileRocketFluidLoader).
                int dim = parseIntOr(args[1], Integer.MIN_VALUE);
                int ix = parseIntOr(args[2], 0);
                int iy = parseIntOr(args[3], 0);
                int iz = parseIntOr(args[4], 0);
                net.minecraft.world.WorldServer w = server.getWorld(dim);
                if (w == null) {
                    send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                    return;
                }
                net.minecraft.tileentity.TileEntity tile = w.getTileEntity(new net.minecraft.util.math.BlockPos(ix, iy, iz));
                if (tile == null) {
                    send(sender, "{\"error\":\"no tile at pos\",\"pos\":[" + ix + "," + iy + "," + iz + "]}");
                    return;
                }
                Object missionRef = readObjectFieldOrNull(tile, "mission");
                long mid = (missionRef instanceof zmaster587.advancedRocketry.api.IMission)
                        ? ((zmaster587.advancedRocketry.api.IMission) missionRef).getMissionId()
                        : -1;
                send(sender, "{\"ok\":true,\"tileClass\":\"" + escapeJson(tile.getClass().getSimpleName())
                        + "\",\"hasMission\":" + (missionRef != null)
                        + ",\"missionId\":" + mid + "}");
                return;
            }
            if ("rocket-relink-state".equals(sub) && args.length >= 2) {
                // /artest mission rocket-relink-state <dim>
                // After MissionGasCollection.onMissionComplete spawns a fresh
                // EntityStationDeployedRocket and calls rocket.linkInfrastructure
                // on each linked infra tile (production line 84), the new
                // rocket's infrastructureCoords set holds those tile coords.
                // The rocket-cargo / snapshotCargoJson scan is bbox-limited
                // around the mission's stored launch coords, but the freshly
                // spawned rocket is positioned by EntityStationDeployedRocket
                // .launchLocation (restored from missionPersistantNBT). With
                // a vanilla EntityRocket fixture, writeMissionPersistentNBT
                // is a no-op so launchLocation defaults to (0,0,0) and the
                // new rocket spawns at world origin — outside the cargo
                // bbox. This verb is class-filtered (not bbox-filtered) so
                // it finds EntityStationDeployedRocket entities regardless
                // of position. Takes a dim arg directly because the mission
                // satellite may have been pruned by the time this verb is
                // called as a follow-up command after complete-now.
                int launchDim = parseIntOr(args[1], Integer.MIN_VALUE);
                net.minecraft.world.WorldServer lw = server.getWorld(launchDim);
                if (lw == null) {
                    send(sender, "{\"error\":\"launch dim not loaded\",\"dim\":" + launchDim + "}");
                    return;
                }
                com.google.common.base.Predicate<net.minecraft.entity.Entity> alwaysTrue =
                        com.google.common.base.Predicates.alwaysTrue();
                java.util.List<zmaster587.advancedRocketry.entity.EntityStationDeployedRocket> deployed =
                        lw.getEntities(zmaster587.advancedRocketry.entity.EntityStationDeployedRocket.class, alwaysTrue);
                StringBuilder per = new StringBuilder("[");
                int totalInfra = 0;
                for (int idx = 0; idx < deployed.size(); idx++) {
                    zmaster587.advancedRocketry.entity.EntityStationDeployedRocket r = deployed.get(idx);
                    Object coordsObj;
                    try {
                        coordsObj = readObjectField(r, "infrastructureCoords");
                    } catch (ReflectiveOperationException ignored) {
                        coordsObj = null;
                    }
                    StringBuilder coordsJson = new StringBuilder("[");
                    int n = 0;
                    if (coordsObj instanceof java.util.Collection) {
                        for (Object pos : (java.util.Collection<?>) coordsObj) {
                            zmaster587.libVulpes.util.HashedBlockPosition hbp =
                                    (zmaster587.libVulpes.util.HashedBlockPosition) pos;
                            if (n++ > 0) coordsJson.append(',');
                            coordsJson.append('[').append(hbp.x).append(',').append(hbp.y)
                                    .append(',').append(hbp.z).append(']');
                        }
                    }
                    coordsJson.append(']');
                    totalInfra += n;
                    if (idx > 0) per.append(',');
                    per.append("{\"entityId\":").append(r.getEntityId())
                            .append(",\"pos\":[").append(r.posX).append(',').append(r.posY)
                            .append(',').append(r.posZ).append(']')
                            .append(",\"infraCount\":").append(n)
                            .append(",\"infrastructure\":").append(coordsJson).append('}');
                }
                per.append(']');
                send(sender, "{\"ok\":true,\"launchDim\":" + launchDim
                        + ",\"deployedCount\":" + deployed.size()
                        + ",\"totalInfraEntries\":" + totalInfra
                        + ",\"rockets\":" + per + "}");
                return;
            }
            send(sender, "{\"error\":\"unknown mission subcommand — try start-gas | start-ore | state | advance | complete-now | rocket-cargo | link-infra | infra-state | rocket-relink-state\"}");
        } catch (ReflectiveOperationException e) {
            send(sender, "{\"error\":\"reflection failed: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    /**
     * Returns a JSON fragment (without enclosing braces) describing the
     * fluid + inventory contents of all EntityRockets within a 128-block
     * cube around the given coords. Used by both the standalone
     * {@code rocket-cargo} verb and the atomic {@code complete-now}
     * which embeds cargo readback to avoid the natural-tick-prune
     * race between commands.
     *
     * <p>Fragment shape:</p>
     * <pre>"rocketCount":N,"fluidEntries":F,"itemEntries":I,
     * "fluids":[...],"items":[...]</pre>
     */
    private static String snapshotCargoJson(net.minecraft.server.MinecraftServer server,
                                            int launchDim, double lx, double ly, double lz) {
        net.minecraft.world.WorldServer lw = server.getWorld(launchDim);
        if (lw == null) {
            return "\"rocketCount\":0,\"fluidEntries\":0,\"itemEntries\":0"
                    + ",\"fluids\":[],\"items\":[],\"cargoError\":\"launch dim not loaded\"";
        }
        net.minecraft.util.math.AxisAlignedBB bb = new net.minecraft.util.math.AxisAlignedBB(
                lx - 128, ly - 64, lz - 128, lx + 128, ly + 256, lz + 128);
        java.util.List<zmaster587.advancedRocketry.entity.EntityRocket> rockets =
                lw.getEntitiesWithinAABB(zmaster587.advancedRocketry.entity.EntityRocket.class, bb);
        StringBuilder fluidsJson = new StringBuilder("[");
        StringBuilder itemsJson = new StringBuilder("[");
        StringBuilder infraJson = new StringBuilder("[");
        int fluidEntries = 0, itemEntries = 0, infraEntries = 0;
        for (zmaster587.advancedRocketry.entity.EntityRocket r : rockets) {
            // Post-completion re-link verification: EntityRocket.infrastructureCoords
            // (HashSet<HashedBlockPosition>) lists tiles this rocket considers
            // connected. Production's MissionGasCollection.onMissionComplete
            // calls rocket.linkInfrastructure(tile) for each mission infra tile,
            // which adds to this set.
            try {
                Object coordsObj = readObjectField(r, "infrastructureCoords");
                if (coordsObj instanceof java.util.Collection) {
                    for (Object pos : (java.util.Collection<?>) coordsObj) {
                        zmaster587.libVulpes.util.HashedBlockPosition hbp =
                                (zmaster587.libVulpes.util.HashedBlockPosition) pos;
                        if (infraEntries++ > 0) infraJson.append(',');
                        infraJson.append('[').append(hbp.x).append(',').append(hbp.y)
                                .append(',').append(hbp.z).append(']');
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // Field absent in a future refactor — leave infra list empty.
            }
            if (r.storage == null) continue;
            for (net.minecraft.tileentity.TileEntity t : r.storage.getFluidTiles()) {
                if (t.hasCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
                    net.minecraftforge.fluids.capability.IFluidHandler fh =
                            t.getCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
                    if (fh == null) continue;
                    for (net.minecraftforge.fluids.capability.IFluidTankProperties p : fh.getTankProperties()) {
                        net.minecraftforge.fluids.FluidStack fs = p.getContents();
                        if (fs == null || fs.amount == 0) continue;
                        if (fluidEntries++ > 0) fluidsJson.append(',');
                        fluidsJson.append("{\"type\":\"")
                                .append(escapeJson(fs.getFluid().getName()))
                                .append("\",\"amount\":").append(fs.amount).append('}');
                    }
                }
            }
            for (net.minecraft.tileentity.TileEntity t : r.storage.getInventoryTiles()) {
                net.minecraftforge.items.IItemHandler ih = t.hasCapability(
                        net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                        net.minecraft.util.EnumFacing.UP)
                        ? t.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                                net.minecraft.util.EnumFacing.UP)
                        : null;
                if (ih != null) {
                    for (int i = 0; i < ih.getSlots(); i++) {
                        net.minecraft.item.ItemStack s = ih.getStackInSlot(i);
                        if (s == null || s.isEmpty()) continue;
                        if (itemEntries++ > 0) itemsJson.append(',');
                        itemsJson.append("{\"id\":\"")
                                .append(escapeJson(s.getItem().getRegistryName() == null
                                        ? "null"
                                        : s.getItem().getRegistryName().toString()))
                                .append("\",\"count\":").append(s.getCount())
                                .append(",\"slot\":").append(i).append('}');
                    }
                }
            }
        }
        fluidsJson.append(']');
        itemsJson.append(']');
        infraJson.append(']');
        return "\"rocketCount\":" + rockets.size()
                + ",\"fluidEntries\":" + fluidEntries
                + ",\"itemEntries\":" + itemEntries
                + ",\"infraEntries\":" + infraEntries
                + ",\"fluids\":" + fluidsJson
                + ",\"items\":" + itemsJson
                + ",\"infrastructure\":" + infraJson;
    }

    private static zmaster587.advancedRocketry.mission.MissionResourceCollection findMission(long id) {
        zmaster587.advancedRocketry.api.satellite.SatelliteBase sat =
                zmaster587.advancedRocketry.dimension.DimensionManager.getInstance().getSatellite(id);
        return sat instanceof zmaster587.advancedRocketry.mission.MissionResourceCollection
                ? (zmaster587.advancedRocketry.mission.MissionResourceCollection) sat
                : null;
    }

    private static long readLongField(Object target, String name) throws ReflectiveOperationException {
        java.lang.reflect.Field f = findFieldInHierarchy(target.getClass(), name);
        f.setAccessible(true);
        return f.getLong(target);
    }

    private static void writeLongField(Object target, String name, long value) throws ReflectiveOperationException {
        java.lang.reflect.Field f = findFieldInHierarchy(target.getClass(), name);
        f.setAccessible(true);
        f.setLong(target, value);
    }

    private static int readIntField(Object target, String name) throws ReflectiveOperationException {
        java.lang.reflect.Field f = findFieldInHierarchy(target.getClass(), name);
        f.setAccessible(true);
        return f.getInt(target);
    }

    private static double readDoubleField(Object target, String name) throws ReflectiveOperationException {
        java.lang.reflect.Field f = findFieldInHierarchy(target.getClass(), name);
        f.setAccessible(true);
        return f.getDouble(target);
    }

    private static Object readObjectField(Object target, String name) throws ReflectiveOperationException {
        java.lang.reflect.Field f = findFieldInHierarchy(target.getClass(), name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static Object readObjectFieldOrNull(Object target, String name) {
        try {
            return readObjectField(target, name);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static java.lang.reflect.Field findFieldInHierarchy(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    /** Reads a private static final int field via reflection. Returns
     *  {@code Integer.MIN_VALUE} on reflective failure (caller treats
     *  that as "field missing"). Used by TASK-22 to expose
     *  {@code MAX_SIZE_Y} / {@code MAX_SIZE} constants of the two
     *  assembler classes. */
    private static int readPrivateIntStatic(Class<?> cls, String name) {
        try {
            java.lang.reflect.Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(null);
        } catch (ReflectiveOperationException e) {
            return Integer.MIN_VALUE;
        }
    }

    /** Non-throwing variant of {@link #findFieldInHierarchy} — returns
     *  {@code null} if no field with the given name exists anywhere in
     *  {@code cls}'s ancestry. Used by diagnostic probes that should
     *  emit partial state instead of bailing on the first absent field. */
    private static java.lang.reflect.Field findFieldOrNull(Class<?> cls, String name) {
        try {
            return findFieldInHierarchy(cls, name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    // §7.18 — force-field projector state probe -------------------------------

    /**
     * {@code /artest field info <dim> <x> <y> <z>} — reads the projector's
     * private {@code extensionRange} field via reflection so tests can verify
     * "the field has grown" without scanning blocks. Also blocks the server
     * thread up to ~12s (240 sleeps × 50ms) to let the projector's
     * {@code % 5 == 0} time gate hit naturally — production runs the
     * extension cycle only every 5 world ticks, and {@code tile force-tick}
     * doesn't advance world time, so a wait against the natural tick loop is
     * the only way to drive extension without modifying production logic.
     * The 12 s ceiling absorbs parallel-fork pressure that stretches effective
     * tick rate; happy-path callers exit on the first observed non-zero range.
     *
     * <p>{@code /artest field info-now <dim> <x> <y> <z>} — same probe but
     * without the wait (snapshot the current state).</p>
     */
    private void handleField(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 5 ||
                !("info".equalsIgnoreCase(args[0]) || "info-now".equalsIgnoreCase(args[0])
                  || "tick".equalsIgnoreCase(args[0]))) {
            send(sender, "{\"error\":\"unknown field subcommand — try info <dim> <x> <y> <z> | info-now <dim> <x> <y> <z> | tick <dim> <x> <y> <z> [n]\"}");
            return;
        }
        boolean waitForTickGate = "info".equalsIgnoreCase(args[0]);
        boolean directTick = "tick".equalsIgnoreCase(args[0]);
        int dim = parseIntOr(args[1], Integer.MIN_VALUE);
        int x = parseIntOr(args[2], 0);
        int y = parseIntOr(args[3], 0);
        int z = parseIntOr(args[4], 0);
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof zmaster587.advancedRocketry.tile.TileForceFieldProjector)) {
            send(sender, "{\"isProjector\":false,\"tile\":\""
                    + (tile == null ? "null" : tile.getClass().getName()) + "\"}");
            return;
        }
        zmaster587.advancedRocketry.tile.TileForceFieldProjector proj =
                (zmaster587.advancedRocketry.tile.TileForceFieldProjector) tile;

        if (waitForTickGate) {
            // Loop up to 240 × 50ms = 12s while releasing the server thread so
            // natural ticks (and the projector's % 5 time gate) fire. Bail
            // early once we observe ANY non-zero extensionRange.
            for (int iter = 0; iter < 240; iter++) {
                if (readExtensionRange(proj) != 0) break;
                try { Thread.sleep(50L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        } else if (directTick) {
            // Drive the projector's extension cycle directly, bypassing the
            // natural %5 tick gate. Each call advances extension by 1 (when
            // powered) or retracts by 1 (when unpowered). Optional count arg
            // defaults to 1; tests typically pass N>=MAX_RANGE (32) for full
            // extension or retraction in a single probe round-trip.
            int count = (args.length >= 6) ? parseIntOr(args[5], 1) : 1;
            if (count < 1) count = 1;
            if (count > 64) count = 64;
            for (int i = 0; i < count; i++) {
                proj.onIntermittentUpdate();
            }
        }

        short range = readExtensionRange(proj);
        boolean powered = world.isBlockPowered(pos);
        send(sender, "{\"isProjector\":true,\"extensionRange\":" + range
                + ",\"isPowered\":" + powered + "}");
    }

    private static short readExtensionRange(zmaster587.advancedRocketry.tile.TileForceFieldProjector proj) {
        try {
            java.lang.reflect.Field f = zmaster587.advancedRocketry.tile.TileForceFieldProjector
                    .class.getDeclaredField("extensionRange");
            f.setAccessible(true);
            return f.getShort(proj);
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    /**
     * Test-only cross-dim teleport: /artest tp &lt;dim&gt; [player-name].
     *
     * <p>Bypasses {@code /advancedrocketry goto} (which gates on
     * {@code sender instanceof Entity} and isn't reachable from a server
     * console driving the harness). Runs the same
     * {@code PlayerList.transferPlayerToDimension} path goto eventually uses,
     * so {@code PlayerChangedDimensionEvent} fires and downstream listeners
     * (e.g. {@code PlanetWeatherEventHandler.syncToPlayer}) are exercised
     * exactly as they would be in normal gameplay.</p>
     *
     * <p>Player defaults to the first connected player when omitted — handy
     * for client-E2E tests that run a single player whose name is generated
     * (FG6's legacydev assigns a random "Player###").</p>
     */
    private void handleTp(net.minecraft.server.MinecraftServer server,
                          ICommandSender sender, String[] args) {
        if (args.length < 1) {
            send(sender, "{\"error\":\"usage: /artest tp <dim> [player]\"}");
            return;
        }
        int dim = parseIntOr(args[0], Integer.MIN_VALUE);
        if (dim == Integer.MIN_VALUE) {
            send(sender, "{\"error\":\"invalid dim id\",\"value\":\"" + args[0] + "\"}");
            return;
        }
        net.minecraft.entity.player.EntityPlayerMP target = null;
        if (args.length >= 2) {
            target = server.getPlayerList().getPlayerByUsername(args[1]);
            if (target == null) {
                send(sender, "{\"error\":\"unknown player\",\"name\":\"" + args[1] + "\"}");
                return;
            }
        } else {
            java.util.List<net.minecraft.entity.player.EntityPlayerMP> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) {
                send(sender, "{\"error\":\"no players online\"}");
                return;
            }
            target = players.get(0);
        }
        if (!net.minecraftforge.common.DimensionManager.isDimensionRegistered(dim)) {
            send(sender, "{\"error\":\"dimension not registered\",\"dim\":" + dim + "}");
            return;
        }
        net.minecraftforge.common.DimensionManager.keepDimensionLoaded(dim, true);
        if (net.minecraftforge.common.DimensionManager.getWorld(dim) == null) {
            net.minecraftforge.common.DimensionManager.initDimension(dim);
        }
        net.minecraft.world.WorldServer destWorld = server.getWorld(dim);
        if (destWorld == null) {
            send(sender, "{\"error\":\"destination world failed to load\",\"dim\":" + dim + "}");
            return;
        }
        int fromDim = target.world.provider.getDimension();
        server.getPlayerList().transferPlayerToDimension(target, dim,
                new zmaster587.advancedRocketry.world.util.TeleporterNoPortalSeekBlock(destWorld));
        send(sender, "{\"ok\":true,\"player\":\"" + target.getName() + "\",\"fromDim\":"
                + fromDim + ",\"toDim\":" + dim + "}");
    }

    // §5 Event handler probes -------------------------------------------------
    //
    // No real player in headless dedicated server tests → we can't assert
    // player-dimension-change side effects directly. What we CAN assert is:
    //   1. {@link zmaster587.advancedRocketry.event.PlanetEventHandler} is
    //      actually subscribed to the Forge event bus (its tick counter must
    //      advance under normal server ticks); a regression in the @Mod init
    //      wiring would silently leave AR running without an event handler.
    //   2. The dim-side wrap-up effects we DO have a probe surface for
    //      (ARWeatherWorldInfo install, atmosphere registration, sky-color
    //      override) are pinned on a freshly loaded AR dim.
    //   3. The transition queue size is observable — a counter-test for the
    //      "no leaked transitions when the harness has no players" invariant.
    private void handleEvent(net.minecraft.server.MinecraftServer server,
                             ICommandSender sender, String[] args) {
        if (args.length == 0) {
            send(sender, "{\"error\":\"usage: /artest event tick-counter | handlers | dim-side-effects <dim> | transitions\"}");
            return;
        }
        String sub = args[0].toLowerCase();
        if ("tick-counter".equals(sub)) {
            // PlanetEventHandler.time is the simplest wiring smoke: it
            // increments on every ServerTickEvent.END phase. If the
            // subscription was lost, the value freezes at zero or wherever
            // the last successful tick left it.
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("time", zmaster587.advancedRocketry.event.PlanetEventHandler.time);
            // World total time as a cross-check: if the world is also frozen
            // (e.g. server paused), our counter wouldn't advance for a
            // legitimate reason — surface both so the test author can
            // disambiguate.
            net.minecraft.world.WorldServer overworld = server.getWorld(0);
            out.put("worldTotalTime", overworld == null ? -1L : overworld.getTotalWorldTime());
            send(sender, jsonMap(out));
            return;
        }
        if ("handlers".equals(sub)) {
            // Heuristic registration check: instantiate the handler classes
            // by name (via Class.forName) and probe the Forge event bus.
            // Forge doesn't expose a "is X registered?" API directly, but
            // the listeners list inside EventBus is reflectable. Simpler /
            // less fragile: verify the well-known static field initial-state
            // contracts that only run if the @Mod init phase completed.
            Map<String, Object> out = new LinkedHashMap<>();
            // PlanetEventHandler.time is 0 before any ServerTickEvent fires
            // and >0 after at least one. Either way the field MUST be
            // readable (regression would be a ClassNotFoundException or a
            // static initializer crash).
            try {
                long t = zmaster587.advancedRocketry.event.PlanetEventHandler.time;
                out.put("planetEventHandler", "loaded");
                out.put("planetEventHandlerTime", t);
            } catch (Throwable e) {
                out.put("planetEventHandler", "missing: " + e.getClass().getSimpleName());
            }
            // RocketEventHandler imports client-only classes (LWJGL GL11,
            // FontRenderer, etc.) so a static `.class` reference on a
            // dedicated server triggers NoClassDefFoundError during
            // class verification. Probe via resource lookup instead —
            // the .class file IS shipped in the jar, we just can't load
            // it cleanly server-side. Resource presence is enough proof
            // that @Mod packaging didn't drop it.
            out.put("rocketEventHandler", classResourcePresent(
                    "zmaster587/advancedRocketry/event/RocketEventHandler") ? "shipped" : "missing");
            // PlanetWeatherEventHandler is server-loadable — direct static
            // reference works and additionally proves the class verifies.
            out.put("planetWeatherEventHandler",
                    zmaster587.advancedRocketry.world.weather.PlanetWeatherEventHandler.class.getName());
            send(sender, jsonMap(out));
            return;
        }
        if ("dim-side-effects".equals(sub) && args.length >= 2) {
            // For the given AR dim, dump the player-facing side effects
            // that *would* fire when a player joins:
            //   - WorldInfo class (ARWeatherWorldInfo wrapper present? — B1)
            //   - AtmosphereHandler registered? (dictates oxygen/vacuum on join)
            //   - DimensionProperties.skyColor (rendered by client on join)
            //   - DimensionProperties.gravity (applied by gravity handler)
            // No player needed — we just confirm the SERVER-SIDE state is
            // ready for the join to be coherent.
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            if (dim == Integer.MIN_VALUE) {
                send(sender, "{\"error\":\"invalid dim id\",\"value\":\"" + args[1] + "\"}");
                return;
            }
            net.minecraftforge.common.DimensionManager.keepDimensionLoaded(dim, true);
            if (net.minecraftforge.common.DimensionManager.getWorld(dim) == null) {
                net.minecraftforge.common.DimensionManager.initDimension(dim);
            }
            net.minecraft.world.WorldServer world =
                    net.minecraftforge.common.DimensionManager.getWorld(dim);
            zmaster587.advancedRocketry.dimension.DimensionProperties props =
                    zmaster587.advancedRocketry.dimension.DimensionManager
                            .getInstance().getDimensionProperties(dim);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("dim", dim);
            out.put("loaded", world != null);
            out.put("worldInfoClass", world == null ? "null"
                    : world.getWorldInfo().getClass().getName());
            out.put("hasAtmosphereHandler",
                    zmaster587.advancedRocketry.atmosphere.AtmosphereHandler
                            .hasAtmosphereHandler(dim));
            out.put("isARPlanet",
                    zmaster587.advancedRocketry.dimension.DimensionManager
                            .getInstance().isDimensionCreated(dim));
            if (props != null) {
                out.put("planetName", props.getName());
                out.put("gravity", props.getGravitationalMultiplier());
                out.put("hasSkyColor", props.skyColor != null && props.skyColor.length > 0);
            }
            send(sender, jsonMap(out));
            return;
        }
        if ("transitions".equals(sub)) {
            // PlanetEventHandler.transitionMap is package-private static;
            // reach it via reflection just for read-back. The size of the
            // queue is the only piece test code legitimately needs.
            try {
                java.lang.reflect.Field f =
                        zmaster587.advancedRocketry.event.PlanetEventHandler
                                .class.getDeclaredField("transitionMap");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<?> list = (java.util.List<?>) f.get(null);
                send(sender, "{\"ok\":true,\"size\":" + list.size() + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"could not read transitionMap\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        send(sender, "{\"error\":\"unknown event subcommand — try tick-counter | handlers | dim-side-effects <dim> | transitions\"}");
    }

    // §5.20 Chunk-anchor probe -----------------------------------------------
    //
    // TASK-07 Phase 4: server-side tests of entity-tick paths (descent,
    // landing) need the rocket's chunk to stay loaded so the natural
    // server tick loop drives EntityRocket.onUpdate in its production
    // context (real neighbour-chunk visibility, real collision data,
    // real packet dispatch). The headless harness has no player, so by
    // default the chunk unloads after a few seconds of idle. We hold
    // an AR-namespaced ForgeChunkManager ticket per (dim, chunkX, chunkZ)
    // to keep them hot. AdvancedRocketry already registers a
    // LoadingCallback in WorldEvents (mod-side, persistent), so
    // requesting tickets here piggy-backs on that registration.
    private static final java.util.Map<String, net.minecraftforge.common.ForgeChunkManager.Ticket>
            CHUNK_TICKETS = new java.util.concurrent.ConcurrentHashMap<>();

    private static String ticketKey(int dim, int cx, int cz) {
        return dim + ":" + cx + ":" + cz;
    }

    private void handleChunk(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            send(sender, "{\"error\":\"usage: /artest chunk forceload <dim> <cx> <cz> | release <dim> <cx> <cz> | release-all | list\"}");
            return;
        }
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        if ("forceload".equals(sub) && args.length >= 4) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int cx = parseIntOr(args[2], Integer.MIN_VALUE);
            int cz = parseIntOr(args[3], Integer.MIN_VALUE);
            // Bring the dimension up if it isn't already — required for
            // tests that force-load chunks in a non-overworld dim that
            // would otherwise be unloaded between tests in the shared
            // harness.
            if (net.minecraftforge.common.DimensionManager.isDimensionRegistered(dim)) {
                net.minecraftforge.common.DimensionManager.keepDimensionLoaded(dim, true);
                if (net.minecraftforge.common.DimensionManager.getWorld(dim) == null) {
                    net.minecraftforge.common.DimensionManager.initDimension(dim);
                }
            }
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            String key = ticketKey(dim, cx, cz);
            net.minecraftforge.common.ForgeChunkManager.Ticket existing = CHUNK_TICKETS.get(key);
            if (existing != null) {
                send(sender, "{\"ok\":true,\"already\":true,\"dim\":" + dim
                        + ",\"cx\":" + cx + ",\"cz\":" + cz + "}");
                return;
            }
            net.minecraftforge.common.ForgeChunkManager.Ticket ticket =
                    net.minecraftforge.common.ForgeChunkManager.requestTicket(
                            zmaster587.advancedRocketry.AdvancedRocketry.instance, world,
                            net.minecraftforge.common.ForgeChunkManager.Type.NORMAL);
            if (ticket == null) {
                send(sender, "{\"error\":\"could not allocate chunk ticket (mod quota exhausted?)\"}");
                return;
            }
            net.minecraftforge.common.ForgeChunkManager.forceChunk(ticket,
                    new net.minecraft.util.math.ChunkPos(cx, cz));
            CHUNK_TICKETS.put(key, ticket);
            send(sender, "{\"ok\":true,\"dim\":" + dim
                    + ",\"cx\":" + cx + ",\"cz\":" + cz + "}");
            return;
        }
        if ("release".equals(sub) && args.length >= 4) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int cx = parseIntOr(args[2], Integer.MIN_VALUE);
            int cz = parseIntOr(args[3], Integer.MIN_VALUE);
            String key = ticketKey(dim, cx, cz);
            net.minecraftforge.common.ForgeChunkManager.Ticket t = CHUNK_TICKETS.remove(key);
            if (t != null) {
                net.minecraftforge.common.ForgeChunkManager.releaseTicket(t);
                send(sender, "{\"ok\":true,\"released\":\"" + key + "\"}");
            } else {
                send(sender, "{\"ok\":true,\"released\":\"none\"}");
            }
            return;
        }
        if ("release-all".equals(sub)) {
            int n = CHUNK_TICKETS.size();
            for (net.minecraftforge.common.ForgeChunkManager.Ticket t : CHUNK_TICKETS.values()) {
                try { net.minecraftforge.common.ForgeChunkManager.releaseTicket(t); }
                catch (RuntimeException ignored) {}
            }
            CHUNK_TICKETS.clear();
            send(sender, "{\"ok\":true,\"released\":" + n + "}");
            return;
        }
        if ("list".equals(sub)) {
            StringBuilder sb = new StringBuilder("{\"tickets\":[");
            boolean first = true;
            for (String k : CHUNK_TICKETS.keySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append("\"").append(k).append("\"");
            }
            sb.append("]}");
            send(sender, sb.toString());
            return;
        }
        if ("warmup".equals(sub) && args.length >= 6) {
            // /artest chunk warmup <dim> <cx1> <cz1> <cx2> <cz2>
            //
            // Synchronously provideChunk(cx, cz) for every (cx,cz) in the
            // rectangle, then ALSO touch a 1-chunk halo on each side so
            // populate(...) fires for the inner rectangle. Vanilla
            // ChunkProviderServer triggers populate when all 4 neighbours
            // are loaded; without the halo, populate fires lazily after
            // a test has already cleared blocks at the rectangle's edge,
            // and worldgen decorations (trees, leaves) silently land
            // back into the cleared region.
            //
            // Returns:
            //   ok          - true iff every chunk in the inner rectangle
            //                 is World.isAreaLoaded after warmup
            //   inner       - number of chunks in the inner rectangle
            //   provided    - total provideChunk calls (inner + halo)
            //   allLoaded   - World.isAreaLoaded over the inner rectangle
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int cx1 = parseIntOr(args[2], Integer.MIN_VALUE);
            int cz1 = parseIntOr(args[3], Integer.MIN_VALUE);
            int cx2 = parseIntOr(args[4], Integer.MIN_VALUE);
            int cz2 = parseIntOr(args[5], Integer.MIN_VALUE);
            if (cx1 == Integer.MIN_VALUE || cz1 == Integer.MIN_VALUE
                    || cx2 == Integer.MIN_VALUE || cz2 == Integer.MIN_VALUE) {
                send(sender, "{\"error\":\"invalid chunk coords\"}");
                return;
            }
            int xMin = Math.min(cx1, cx2), xMax = Math.max(cx1, cx2);
            int zMin = Math.min(cz1, cz2), zMax = Math.max(cz1, cz2);
            // Soft cap — populate() per chunk is expensive (trees, ores,
            // structures); refuse pathological warmups that would block
            // the harness for minutes.
            int innerCount = (xMax - xMin + 1) * (zMax - zMin + 1);
            if (innerCount > 256) {
                send(sender, "{\"error\":\"warmup area too large\",\"innerChunks\":"
                        + innerCount + ",\"cap\":256}");
                return;
            }
            // Init dim if needed (same as forceload).
            if (net.minecraftforge.common.DimensionManager.isDimensionRegistered(dim)) {
                net.minecraftforge.common.DimensionManager.keepDimensionLoaded(dim, true);
                if (net.minecraftforge.common.DimensionManager.getWorld(dim) == null) {
                    net.minecraftforge.common.DimensionManager.initDimension(dim);
                }
            }
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            int provided = 0;
            // Halo: extend 1 chunk on each side so populate fires for the
            // entire inner rectangle (populate(X) needs +1/+1, +1/0, 0/+1
            // neighbours loaded — covered by the halo).
            for (int cx = xMin - 1; cx <= xMax + 1; cx++) {
                for (int cz = zMin - 1; cz <= zMax + 1; cz++) {
                    try {
                        net.minecraft.world.chunk.Chunk c =
                                world.getChunkProvider().provideChunk(cx, cz);
                        if (c != null) provided++;
                    } catch (RuntimeException ignored) {
                        // Worldgen of one bad chunk shouldn't break the whole
                        // warmup; let the caller decide if allLoaded=false is
                        // a fatal error for them.
                    }
                }
            }
            boolean allLoaded = world.isAreaLoaded(
                    new net.minecraft.util.math.BlockPos(xMin << 4, 0, zMin << 4),
                    new net.minecraft.util.math.BlockPos((xMax << 4) + 15, 255, (zMax << 4) + 15),
                    false);
            send(sender, "{\"ok\":" + allLoaded
                    + ",\"dim\":" + dim
                    + ",\"inner\":" + innerCount
                    + ",\"provided\":" + provided
                    + ",\"allLoaded\":" + allLoaded + "}");
            return;
        }
        send(sender, "{\"error\":\"unknown chunk subcommand\"}");
    }

    // §5.21 Server tick-wait probe -------------------------------------------
    //
    // TASK-07 Phase 4: companion to the chunk-anchor probe. Once the
    // rocket's chunk is force-loaded, we need to let the server's
    // natural tick loop run N times so EntityRocket.onUpdate is invoked
    // in its production context (rather than driving it synthetically
    // via /artest rocket tick). This probe polls
    // world.getTotalWorldTime() until the configured number of ticks
    // has elapsed, sleeping 50ms between polls.
    private void handleServer(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length >= 3 && "wait".equalsIgnoreCase(args[0])) {
            int dim = parseIntOr(args[1], Integer.MIN_VALUE);
            int ticksToWait = parseIntOr(args[2], 0);
            if (ticksToWait <= 0 || ticksToWait > 6000) {
                send(sender, "{\"error\":\"ticksToWait must be in (0, 6000]\"}");
                return;
            }
            net.minecraft.world.WorldServer world = server.getWorld(dim);
            if (world == null) {
                send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
                return;
            }
            long start = world.getTotalWorldTime();
            long deadline = start + ticksToWait;
            // Wall-clock guard so a stuck/slow server can't hang the test
            // harness: budget 200ms per requested tick, capped at 30 s.
            // The harness's per-command marker timeout is ~60 s so we
            // stay well clear.
            long wallStart = System.currentTimeMillis();
            long wallBudgetMs = Math.min(30_000L, Math.max(1000L, ticksToWait * 200L));
            while (world.getTotalWorldTime() < deadline) {
                if (System.currentTimeMillis() - wallStart > wallBudgetMs) break;
                try { Thread.sleep(25L); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
            long end = world.getTotalWorldTime();
            send(sender, "{\"ok\":true,\"dim\":" + dim
                    + ",\"startTick\":" + start
                    + ",\"endTick\":" + end
                    + ",\"elapsedTicks\":" + (end - start)
                    + ",\"requested\":" + ticksToWait
                    + ",\"wallMs\":" + (System.currentTimeMillis() - wallStart) + "}");
            return;
        }
        send(sender, "{\"error\":\"usage: /artest server wait <dim> <ticks>\"}");
    }

    /** True if the {@code <slashed>.class} resource is reachable via the
     *  current thread's context classloader. Used to verify the presence of
     *  client-only event handler classes on dedicated server without
     *  triggering verification (which references LWJGL / client classes
     *  that aren't on the dedicated-server classpath). */
    private static boolean classResourcePresent(String slashed) {
        return Thread.currentThread().getContextClassLoader()
                .getResource(slashed + ".class") != null;
    }

    /**
     * TASK-07 — global event-bus listener that counts RocketEvent fires.
     * Registered lazily on first /artest rocket event-counts query.
     * Static counters are visible to all probe handlers and to the
     * launch/orbit-reached/dismantle probes which include
     * "*EventDelta" fields in their responses for inline cause-effect
     * verification.
     */
    public static final class RocketEventRecorder {
        public static volatile int launchCount = 0;
        public static volatile int preLaunchCount = 0;
        public static volatile int orbitReachedCount = 0;
        public static volatile int dismantleCount = 0;
        public static volatile int landedCount = 0;
        public static volatile int deOrbitingCount = 0;

        // Gap #6 payload pins — last-observed entity id + dim for each
        // event type, so tests can verify subscribers receive the right
        // payload (not just that the event fired). Defaults to -1 so a
        // missed event is distinguishable from "fired with entityId=0".
        public static volatile int lastLaunchEntityId = -1;
        public static volatile int lastLaunchDim = Integer.MIN_VALUE;
        public static volatile int lastPreLaunchEntityId = -1;
        public static volatile int lastPreLaunchDim = Integer.MIN_VALUE;
        public static volatile int lastOrbitReachedEntityId = -1;
        public static volatile int lastOrbitReachedDim = Integer.MIN_VALUE;
        public static volatile int lastDismantleEntityId = -1;
        public static volatile int lastDismantleDim = Integer.MIN_VALUE;
        public static volatile int lastLandedEntityId = -1;
        public static volatile int lastLandedDim = Integer.MIN_VALUE;
        public static volatile int lastDeOrbitingEntityId = -1;
        public static volatile int lastDeOrbitingDim = Integer.MIN_VALUE;

        private static volatile boolean registered = false;

        public static synchronized void ensureRegistered() {
            if (registered) return;
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new RocketEventRecorder());
            registered = true;
        }

        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onLaunch(
                zmaster587.advancedRocketry.api.RocketEvent.RocketLaunchEvent e) {
            launchCount++;
            lastLaunchEntityId = e.getEntity() == null ? -1 : e.getEntity().getEntityId();
            lastLaunchDim = e.world == null ? Integer.MIN_VALUE : e.world.provider.getDimension();
        }
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onPreLaunch(
                zmaster587.advancedRocketry.api.RocketEvent.RocketPreLaunchEvent e) {
            preLaunchCount++;
            lastPreLaunchEntityId = e.getEntity() == null ? -1 : e.getEntity().getEntityId();
            lastPreLaunchDim = e.world == null ? Integer.MIN_VALUE : e.world.provider.getDimension();
        }
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onOrbitReached(
                zmaster587.advancedRocketry.api.RocketEvent.RocketReachesOrbitEvent e) {
            orbitReachedCount++;
            lastOrbitReachedEntityId = e.getEntity() == null ? -1 : e.getEntity().getEntityId();
            lastOrbitReachedDim = e.world == null ? Integer.MIN_VALUE : e.world.provider.getDimension();
        }
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onDismantle(
                zmaster587.advancedRocketry.api.RocketEvent.RocketDismantleEvent e) {
            dismantleCount++;
            lastDismantleEntityId = e.getEntity() == null ? -1 : e.getEntity().getEntityId();
            lastDismantleDim = e.world == null ? Integer.MIN_VALUE : e.world.provider.getDimension();
        }
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onLanded(
                zmaster587.advancedRocketry.api.RocketEvent.RocketLandedEvent e) {
            landedCount++;
            lastLandedEntityId = e.getEntity() == null ? -1 : e.getEntity().getEntityId();
            lastLandedDim = e.world == null ? Integer.MIN_VALUE : e.world.provider.getDimension();
        }
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onDeOrbiting(
                zmaster587.advancedRocketry.api.RocketEvent.RocketDeOrbitingEvent e) {
            deOrbitingCount++;
            lastDeOrbitingEntityId = e.getEntity() == null ? -1 : e.getEntity().getEntityId();
            lastDeOrbitingDim = e.world == null ? Integer.MIN_VALUE : e.world.provider.getDimension();
        }
    }
}
