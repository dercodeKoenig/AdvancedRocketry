package zmaster587.advancedRocketry;

import net.minecraft.command.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.*;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.repack.gloomyfolken.hooklib.asm.Hook;
import zmaster587.advancedRocketry.repack.gloomyfolken.hooklib.asm.ReturnCondition;
import zmaster587.advancedRocketry.world.WorldServerNotMulti;
import zmaster587.advancedRocketry.world.provider.WorldProviderPlanet;

import java.util.Random;

import static net.minecraftforge.common.DimensionManager.getWorld;

public class ARHooks {

//    @Hook(returnCondition = ReturnCondition.ON_TRUE, booleanReturnConstant = true)
//    public static boolean bindEntityTexture(Render<? extends Entity> instance, Entity entity) {
////        if (Minecraft.getMinecraft().isSingleplayer()) {
////            return false;
////        }
//        if (entity instanceof EntityPlayer) {
//            ITextureObject t = Skin.forPlayer(entity.getName()).getSkin();
//            if (t == null) {
//                return false;
//            }
//            GlStateManager.bindTexture(t.getGlTextureId());
//            return true;
//        } else {
//            return false;
//        }
//    }


    @Hook(returnCondition = ReturnCondition.ALWAYS)
    public static void initDimension(DimensionManager mgr, int dim) {
        WorldServer overworld = getWorld(0);
        if (overworld == null) {
            throw new RuntimeException("Cannot Hotload Dim: Overworld is not Loaded!");
        }
        try {
            DimensionManager.getProviderType(dim);
        } catch (Exception e) {
            FMLLog.log.error("Cannot Hotload Dim: {}", dim, e);
            return; // If a provider hasn't been registered then we can't hotload the dim
        }
        MinecraftServer mcServer = overworld.getMinecraftServer();
        ISaveHandler savehandler = overworld.getSaveHandler();
        //WorldSettings worldSettings = new WorldSettings(overworld.getWorldInfo());

        WorldServer world = (dim == 0 ? overworld : (WorldServer) (new WorldServerNotMulti(mcServer, savehandler, dim, overworld, mcServer.profiler).init()));
        world.addEventListener(new ServerWorldEventHandler(mcServer, world));
        MinecraftForge.EVENT_BUS.post(new WorldEvent.Load(world));
        if (!mcServer.isSinglePlayer()) {
            world.getWorldInfo().setGameType(mcServer.getGameType());
        }

        mcServer.setDifficultyForAllWorlds(mcServer.getDifficulty());
    }

    @Hook(returnCondition = ReturnCondition.ALWAYS)
    public static void loadAllWorlds(MinecraftServer server, String saveName, String worldNameIn, long seed, WorldType type, String generatorOptions) {
        server.convertMapIfNeeded(saveName);
        server.setUserMessage("menu.loadingLevel");
        ISaveHandler isavehandler = server.anvilConverterForAnvilFile.getSaveLoader(saveName, true);
        server.setResourcePackFromWorld(server.getFolderName(), isavehandler);
        WorldInfo worldinfo = isavehandler.loadWorldInfo();
        WorldSettings worldsettings;

        if (worldinfo == null) {
            if (server.isDemo()) {
                worldsettings = WorldServerDemo.DEMO_WORLD_SETTINGS;
            } else {
                worldsettings = new WorldSettings(seed, server.getGameType(), server.canStructuresSpawn(), server.isHardcore(), type);
                worldsettings.setGeneratorOptions(generatorOptions);

                if (server.enableBonusChest) {
                    worldsettings.enableBonusChest();
                }
            }

            worldinfo = new WorldInfo(worldsettings, worldNameIn);
        } else {
            worldinfo.setWorldName(worldNameIn);
            worldsettings = new WorldSettings(worldinfo);
        }

        WorldServer overWorld = (WorldServer) (server.isDemo() ? new WorldServerDemo(server, isavehandler, worldinfo, 0, server.profiler).init() : new WorldServer(server, isavehandler, worldinfo, 0, server.profiler).init());
        overWorld.initialize(worldsettings);
        for (int dim : net.minecraftforge.common.DimensionManager.getStaticDimensionIDs()) {
            WorldServer world = (dim == 0 ? overWorld : (WorldServer) new WorldServerNotMulti(server, isavehandler, dim, overWorld, server.profiler).init());
            world.addEventListener(new ServerWorldEventHandler(server, world));

            if (!server.isSinglePlayer()) {
                world.getWorldInfo().setGameType(server.getGameType());
            }
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Load(world));
        }

        server.playerList.setPlayerManager(new WorldServer[]{overWorld});
        server.setDifficultyForAllWorlds(server.getDifficulty());
        server.initialWorldChunkLoad();
    }

    @SideOnly(Side.CLIENT)
    @Hook(returnCondition = ReturnCondition.ALWAYS)
    public static void loadAllWorlds(IntegratedServer server, String saveName, String worldNameIn, long seed, WorldType type, String generatorOptions) {
        server.convertMapIfNeeded(saveName);
        ISaveHandler isavehandler = server.getActiveAnvilConverter().getSaveLoader(saveName, true);
        server.setResourcePackFromWorld(server.getFolderName(), isavehandler);
        WorldInfo worldinfo = isavehandler.loadWorldInfo();

        if (worldinfo == null) {
            worldinfo = new WorldInfo(server.worldSettings, worldNameIn);
        } else {
            worldinfo.setWorldName(worldNameIn);
        }

        WorldServer overWorld = (server.isDemo() ? (WorldServer) (new WorldServerDemo(server, isavehandler, worldinfo, 0, server.profiler)).init() :
                (WorldServer) (new WorldServer(server, isavehandler, worldinfo, 0, server.profiler)).init());
        overWorld.initialize(server.worldSettings);
        for (int dim : net.minecraftforge.common.DimensionManager.getStaticDimensionIDs()) {
            WorldServer world = (dim == 0 ? overWorld : (WorldServer) new WorldServerNotMulti(server, isavehandler, dim, overWorld, server.profiler).init());
            world.addEventListener(new ServerWorldEventHandler(server, world));
            if (!server.isSinglePlayer()) {
                world.getWorldInfo().setGameType(server.getGameType());
            }
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Load(world));
        }

        server.getPlayerList().setPlayerManager(new WorldServer[]{overWorld});

        if (overWorld.getWorldInfo().getDifficulty() == null) {
            server.setDifficultyForAllWorlds(server.mc.gameSettings.difficulty);
        }

        server.initialWorldChunkLoad();
    }

    @Hook(returnCondition = ReturnCondition.ALWAYS)
    public static void execute(CommandWeather command, MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 1 && args.length <= 2) {
            int i = (300 + (new Random()).nextInt(600)) * 20;

            if (args.length >= 2) {
                i = CommandBase.parseInt(args[1], 1, 1000000) * 20;
            }

            World world = sender.getEntityWorld();
            WorldInfo worldinfo = world.getWorldInfo();
            WorldProvider provider = world.provider;
            DimensionProperties props = null;
            if (provider instanceof WorldProviderPlanet) {
                props = ((WorldProviderPlanet) provider).getDimensionProperties();
            }

            if ("clear".equalsIgnoreCase(args[0])) {
                if (props != null && (props.getRainMarker() == 1 || props.getThunderMarker() == 1)) {
                    CommandBase.notifyCommandListener(sender, command, "commands.weather.always_not_clear", new Object[0]);
                    return;
                }

                worldinfo.setCleanWeatherTime(i);
                worldinfo.setRainTime(0);
                worldinfo.setThunderTime(0);
                worldinfo.setRaining(false);
                worldinfo.setThundering(false);
                CommandBase.notifyCommandListener(sender, command, "commands.weather.clear", new Object[0]);
            } else if ("rain".equalsIgnoreCase(args[0])) {
                if (props != null && props.getRainMarker() == -1) {
                    CommandBase.notifyCommandListener(sender, command, "commands.weather.cannot_rain", new Object[0]);
                    return;
                }

                worldinfo.setCleanWeatherTime(0);
                worldinfo.setRainTime(i);
                worldinfo.setThunderTime(i);
                worldinfo.setRaining(true);
                worldinfo.setThundering(false);
                CommandBase.notifyCommandListener(sender, command, "commands.weather.rain", new Object[0]);
            } else {
                if (!"thunder".equalsIgnoreCase(args[0])) {
                    throw new WrongUsageException("commands.weather.usage", new Object[0]);
                }
                if (props != null && props.getThunderMarker() == -1) {
                    CommandBase.notifyCommandListener(sender, command, "commands.weather.cannot_thunder", new Object[0]);
                    return;
                }

                worldinfo.setCleanWeatherTime(0);
                worldinfo.setRainTime(i);
                worldinfo.setThunderTime(i);
                worldinfo.setRaining(true);
                worldinfo.setThundering(true);
                CommandBase.notifyCommandListener(sender, command, "commands.weather.thunder", new Object[0]);
            }
        } else {
            throw new WrongUsageException("commands.weather.usage", new Object[0]);
        }
    }
}
