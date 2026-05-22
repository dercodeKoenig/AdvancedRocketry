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
     * §7.17 — wireless transceiver pairing.
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
     * tile's current {@code networkID} (read-only).</p>
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
                java.lang.reflect.Field f = zmaster587.advancedRocketry.tile.cables
                        .TileWirelessTransciever.class.getDeclaredField("networkID");
                f.setAccessible(true);
                int id = f.getInt(tile);
                send(sender, "{\"ok\":true,\"networkID\":" + id + "}");
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"reflection failed\",\"msg\":\""
                        + escapeJson(e.getMessage()) + "\"}");
            }
            return;
        }
        send(sender, "{\"error\":\"unknown pipe subcommand — try wireless-pair <dim> <x1> <y1> <z1> <x2> <y2> <z2> | wireless-info <dim> <x> <y> <z>\"}");
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

            if (launchpad == null || rocketBuilder == null || advEngine == null
                    || fuelTank == null || guidanceComputer == null || seat == null) {
                send(sender, "{\"error\":\"missing AR block(s) in registry\"}");
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
                    "structure");
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
                    "structure");
            return;
        }
        send(sender, "{\"error\":\"unknown fixture subcommand — try rocket <dim> <x> <y> <z> | machine cutting <dim> <x> <y> <z> | multiblock blackhole-gen|beacon|observatory|railgun|warp-core|gravity-controller|planet-analyser|space-elevator|microwave-receiver|solar-array|terraformer|orbital-laser-drill <dim> <x> <y> <z>\"}");
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
     * </ol>
     */
    private void handleFixtureGenericFromStructure(MinecraftServer server, ICommandSender sender,
            int dim, int cx, int cy, int cz,
            String controllerNamespace, String controllerPath,
            String tileClassName, String structureFieldName) {
        net.minecraft.world.WorldServer world = server.getWorld(dim);
        if (world == null) {
            send(sender, "{\"error\":\"world not loaded\",\"dim\":" + dim + "}");
            return;
        }

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

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ok", true);
        info.put("controllerPos", new int[]{cx, cy, cz});
        info.put("dimensions",    new int[]{dimX, dimY, dimZ});
        info.put("offset",        new int[]{ox, oy, oz});
        info.put("boundingBox",   new int[]{minX, minY, minZ, maxX, maxY, maxZ});
        info.put("placed",        placed);
        info.put("skipped",       skipped);
        info.put("unresolved",    unresolved);
        send(sender, jsonMap(info));
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
        send(sender, "{\"error\":\"unknown entity subcommand — try spawn <dim> <x> <y> <z> <name> [block-id] | info <dim> <entityId> | tick <dim> <entityId> [count]\"}");
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
        send(sender, "{\"error\":\"unknown player subcommand — try inv-bypass <add|remove|status> | open-container | health | set-health <hp> | held-air | give-suit-chest [air] | advancement <id> | advancement reset <id> | last-chat | chat-clear | try-seal-detect <dim> <x> <y> <z> | try-atm-analyze <dim>\"}");
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
        if (args.length < 5 || !"check".equalsIgnoreCase(args[0])) {
            send(sender, "{\"error\":\"unknown seal-detector subcommand — "
                    + "try check <dim> <x> <y> <z>\"}");
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

    // §7.18 — force-field projector state probe -------------------------------

    /**
     * {@code /artest field info <dim> <x> <y> <z>} — reads the projector's
     * private {@code extensionRange} field via reflection so tests can verify
     * "the field has grown" without scanning blocks. Also blocks the server
     * thread up to ~1.5s (30 sleeps × 50ms) to let the projector's
     * {@code % 5 == 0} time gate hit naturally — production runs the
     * extension cycle only every 5 world ticks, and {@code tile force-tick}
     * doesn't advance world time, so a wait against the natural tick loop is
     * the only way to drive extension without modifying production logic.
     *
     * <p>{@code /artest field info-now <dim> <x> <y> <z>} — same probe but
     * without the wait (snapshot the current state).</p>
     */
    private void handleField(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 5 ||
                !("info".equalsIgnoreCase(args[0]) || "info-now".equalsIgnoreCase(args[0]))) {
            send(sender, "{\"error\":\"unknown field subcommand — try info <dim> <x> <y> <z> | info-now <dim> <x> <y> <z>\"}");
            return;
        }
        boolean waitForTickGate = "info".equalsIgnoreCase(args[0]);
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
            // Loop up to 30 × 50ms = 1.5s while releasing the server thread so
            // natural ticks (and the projector's % 5 time gate) fire. Bail
            // early once we observe ANY non-zero extensionRange.
            for (int iter = 0; iter < 30; iter++) {
                if (readExtensionRange(proj) != 0) break;
                try { Thread.sleep(50L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
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
        }
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onPreLaunch(
                zmaster587.advancedRocketry.api.RocketEvent.RocketPreLaunchEvent e) {
            preLaunchCount++;
        }
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onOrbitReached(
                zmaster587.advancedRocketry.api.RocketEvent.RocketReachesOrbitEvent e) {
            orbitReachedCount++;
        }
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onDismantle(
                zmaster587.advancedRocketry.api.RocketEvent.RocketDismantleEvent e) {
            dismantleCount++;
        }
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onLanded(
                zmaster587.advancedRocketry.api.RocketEvent.RocketLandedEvent e) {
            landedCount++;
        }
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onDeOrbiting(
                zmaster587.advancedRocketry.api.RocketEvent.RocketDeOrbitingEvent e) {
            deOrbitingCount++;
        }
    }
}
