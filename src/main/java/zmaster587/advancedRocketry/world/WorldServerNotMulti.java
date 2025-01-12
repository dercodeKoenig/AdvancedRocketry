package zmaster587.advancedRocketry.world;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.village.VillageCollection;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.border.IBorderListener;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.storage.ISaveHandler;

public class WorldServerNotMulti extends WorldServer {
    private final WorldServer delegate;
    private final IBorderListener borderListener;

    public WorldServerNotMulti(MinecraftServer server, ISaveHandler saveHandlerIn, int dimensionId, WorldServer delegate, Profiler profilerIn) {
        super(server, saveHandlerIn, new CustomDerivedWorldInfo(delegate.getWorldInfo()), dimensionId, profilerIn);
        ((CustomDerivedWorldInfo) this.getWorldInfo()).setWorld(this);
        this.delegate = delegate;
        this.borderListener = new IBorderListener() {
            public void onSizeChanged(WorldBorder border, double newSize) {
                WorldServerNotMulti.this.getWorldBorder().setTransition(newSize);
            }

            public void onTransitionStarted(WorldBorder border, double oldSize, double newSize, long time) {
                WorldServerNotMulti.this.getWorldBorder().setTransition(oldSize, newSize, time);
            }

            public void onCenterChanged(WorldBorder border, double x, double z) {
                WorldServerNotMulti.this.getWorldBorder().setCenter(x, z);
            }

            public void onWarningTimeChanged(WorldBorder border, int newTime) {
                WorldServerNotMulti.this.getWorldBorder().setWarningTime(newTime);
            }

            public void onWarningDistanceChanged(WorldBorder border, int newDistance) {
                WorldServerNotMulti.this.getWorldBorder().setWarningDistance(newDistance);
            }

            public void onDamageAmountChanged(WorldBorder border, double newAmount) {
                WorldServerNotMulti.this.getWorldBorder().setDamageAmount(newAmount);
            }

            public void onDamageBufferChanged(WorldBorder border, double newSize) {
                WorldServerNotMulti.this.getWorldBorder().setDamageBuffer(newSize);
            }
        };
        this.delegate.getWorldBorder().addListener(this.borderListener);
    }

    @Override
    protected void saveLevel() throws MinecraftException {
        this.perWorldStorage.saveAllData();
    }

    public World init() {
        super.init();
        // load weather data from NBT
        WorldInfoSavedData wi = (WorldInfoSavedData) perWorldStorage.getOrLoadData(WorldInfoSavedData.class, "WorldInfoSavedData");
        if (wi == null) {
            wi = new WorldInfoSavedData(this);
            this.perWorldStorage.setData("WorldInfoSavedData", wi);
        }
        wi.updateWorldInfo(this);

        this.mapStorage = this.delegate.getMapStorage();
        this.worldScoreboard = this.delegate.getScoreboard();
        this.lootTable = this.delegate.getLootTableManager();
        this.advancementManager = this.delegate.getAdvancementManager();
        String s = VillageCollection.fileNameForProvider(this.provider);
        VillageCollection villagecollection = (VillageCollection) this.perWorldStorage.getOrLoadData(VillageCollection.class, s);

        if (villagecollection == null) {
            this.villageCollection = new VillageCollection(this);
            this.perWorldStorage.setData(s, this.villageCollection);
        } else {
            this.villageCollection = villagecollection;
            this.villageCollection.setWorldsForAll(this);
        }

        this.initCapabilities();
        return this;
    }


    @Override
    public void flush() {
        super.flush();
        this.delegate.getWorldBorder().removeListener(this.borderListener); // Unlink ourselves, to prevent world leak.
        this.provider.onWorldSave();
    }
}
