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
            send(sender, jsonMap(info));
            return;
        }
        send(sender, "{\"error\":\"unknown planet subcommand\"}");
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
        send(sender, "{\"error\":\"unknown atmosphere subcommand — try get <dim> <x> <y> <z> | set-density <dim> <value>\"}");
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
            if (target == null) {
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
     * {@code /artest hatch read <dim> <x> <y> <z>} — dumps every non-empty slot
     * as {@code {"slot":N,"item":"<id>","count":K,"meta":M}}.
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
                        .append(",\"meta\":").append(stack.getMetadata()).append('}');
            }
            builder.append("]}");
            send(sender, builder.toString());
            return;
        }
        send(sender, "{\"error\":\"unknown hatch subcommand — try fill <dim> <x> <y> <z> <slot> <itemId> [count] [meta] | read <dim> <x> <y> <z>\"}");
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
        send(sender, "{\"error\":\"unknown tile subcommand — try force-tick <dim> <x> <y> <z> <ticks>\"}");
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
        send(sender, "{\"error\":\"unknown infra subcommand — try info <dim> <x> <y> <z> | link <dim> <x> <y> <z> <entityId>\"}");
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
            world.setBlockState(new BlockPos(rocketX - 1, rocketY, rocketZ), advEngine.getDefaultState());
            world.setBlockState(new BlockPos(rocketX + 1, rocketY, rocketZ), advEngine.getDefaultState());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 1; dy <= 2; dy++) {
                    world.setBlockState(new BlockPos(rocketX + dx, rocketY + dy, rocketZ),
                            fuelTank.getDefaultState());
                }
            }
            world.setBlockState(new BlockPos(rocketX, rocketY + 3, rocketZ), guidanceComputer.getDefaultState());
            world.setBlockState(new BlockPos(rocketX, rocketY + 4, rocketZ), seat.getDefaultState());

            send(sender, "{\"ok\":true,\"builderPos\":[" + builderPos.getX() + ","
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
        send(sender, "{\"error\":\"unknown fixture subcommand — try rocket <dim> <x> <y> <z> | machine cutting <dim> <x> <y> <z>\"}");
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
        if (args.length < 2 || !"check".equalsIgnoreCase(args[0])) {
            send(sender, "{\"error\":\"unknown enchant subcommand — try check <id>\"}");
            return;
        }
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
                // Prefer (World, x, y, z) ctor.
                java.lang.reflect.Constructor<? extends net.minecraft.entity.Entity> ctor;
                try {
                    ctor = clazz.getConstructor(net.minecraft.world.World.class,
                            double.class, double.class, double.class);
                    entity = ctor.newInstance(world, x, y, z);
                } catch (NoSuchMethodException nsm) {
                    ctor = clazz.getConstructor(net.minecraft.world.World.class);
                    entity = ctor.newInstance(world);
                    entity.setPosition(x, y, z);
                }
            } catch (ReflectiveOperationException e) {
                send(sender, "{\"error\":\"spawn failed: "
                        + escapeJson(e.getClass().getSimpleName() + ": " + e.getMessage())
                        + "\"}");
                return;
            }
            boolean spawned = world.spawnEntity(entity);
            send(sender, "{\"ok\":true,\"spawned\":" + spawned
                    + ",\"entityId\":" + entity.getEntityId()
                    + ",\"entityClass\":\"" + escapeJson(entity.getClass().getName()) + "\"}");
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
                    + ",\"isDead\":" + entity.isDead + "}");
            return;
        }
        send(sender, "{\"error\":\"unknown entity subcommand — try spawn <dim> <x> <y> <z> <name> | info <dim> <entityId>\"}");
    }

    private static double parseDoubleOr(String s, double dflt) {
        try { return Double.parseDouble(s); } catch (NumberFormatException nfe) { return dflt; }
    }

    // §7.18 — generic block-state probe ---------------------------------------

    /**
     * {@code /artest block at <dim> <x> <y> <z>} — returns the block registry
     * name + meta at a position. Used by tests that need to assert on world
     * blockstate changes (e.g. force-field projection, terraformer block
     * mutation) without going through a tile entity.
     */
    private void handleBlock(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 5 || !"at".equalsIgnoreCase(args[0])) {
            send(sender, "{\"error\":\"unknown block subcommand — try at <dim> <x> <y> <z>\"}");
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
}
