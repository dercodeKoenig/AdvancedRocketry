package zmaster587.advancedRocketry.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public class CustomDerivedWorldInfo extends WorldInfo {

    private final WorldInfo delegate;
    private World world;
    private WorldInfoSavedData saveHandler;

    public CustomDerivedWorldInfo(WorldInfo worldInfoIn) {
        this.delegate = worldInfoIn;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    private WorldInfoSavedData getSaveHandler() {
        if (saveHandler == null) {
            saveHandler = (WorldInfoSavedData) world.getPerWorldStorage().getOrLoadData(WorldInfoSavedData.class, "WorldInfoSavedData");
        }
        return saveHandler;
    }

    public NBTTagCompound cloneNBTCompound(@Nullable NBTTagCompound nbt) {
        return this.delegate.cloneNBTCompound(nbt);
    }

    public long getSeed() {
        return this.delegate.getSeed();
    }

    public int getSpawnX() {
        return this.delegate.getSpawnX();
    }

    public int getSpawnY() {
        return this.delegate.getSpawnY();
    }

    public int getSpawnZ() {
        return this.delegate.getSpawnZ();
    }

    public long getWorldTotalTime() {
        return this.delegate.getWorldTotalTime();
    }

    public long getWorldTime() {
        return this.delegate.getWorldTime();
    }

    @SideOnly(Side.CLIENT)
    public long getSizeOnDisk() {
        return this.delegate.getSizeOnDisk();
    }

    public NBTTagCompound getPlayerNBTTagCompound() {
        return this.delegate.getPlayerNBTTagCompound();
    }

    public String getWorldName() {
        return this.delegate.getWorldName();
    }

    public int getSaveVersion() {
        return this.delegate.getSaveVersion();
    }

    @SideOnly(Side.CLIENT)
    public long getLastTimePlayed() {
        return this.delegate.getLastTimePlayed();
    }

    public GameType getGameType() {
        return this.delegate.getGameType();
    }

    @SideOnly(Side.CLIENT)
    public void setSpawnX(int x) {
    }

    @SideOnly(Side.CLIENT)
    public void setSpawnY(int y) {
    }

    public void setWorldTotalTime(long time) {
    }

    @SideOnly(Side.CLIENT)
    public void setSpawnZ(int z) {
    }

    public void setSpawn(BlockPos spawnPoint) {
    }

    public void setWorldName(String worldName) {
    }

    public void setSaveVersion(int version) {
    }

    public boolean isMapFeaturesEnabled() {
        return this.delegate.isMapFeaturesEnabled();
    }

    public boolean isHardcoreModeEnabled() {
        return this.delegate.isHardcoreModeEnabled();
    }

    public WorldType getTerrainType() {
        return this.delegate.getTerrainType();
    }

    public void setTerrainType(WorldType type) {
    }

    public boolean areCommandsAllowed() {
        return this.delegate.areCommandsAllowed();
    }

    public void setAllowCommands(boolean allow) {
    }

    public boolean isInitialized() {
        return this.delegate.isInitialized();
    }

    public void setServerInitialized(boolean initializedIn) {
    }

    public GameRules getGameRulesInstance() {
        return this.delegate.getGameRulesInstance();
    }

    public EnumDifficulty getDifficulty() {
        return this.delegate.getDifficulty();
    }

    public void setDifficulty(EnumDifficulty newDifficulty) {
    }

    public boolean isDifficultyLocked() {
        return this.delegate.isDifficultyLocked();
    }

    public void setDifficultyLocked(boolean locked) {
    }

    @Deprecated
    public void setDimensionData(DimensionType dimensionIn, NBTTagCompound compound) {
        this.delegate.setDimensionData(dimensionIn, compound);
    }

    @Deprecated
    public NBTTagCompound getDimensionData(DimensionType dimensionIn) {
        return this.delegate.getDimensionData(dimensionIn);
    }

    public void setDimensionData(int dimensionID, NBTTagCompound compound) {
        this.delegate.setDimensionData(dimensionID, compound);
    }

    public NBTTagCompound getDimensionData(int dimensionID) {
        return this.delegate.getDimensionData(dimensionID);
    }

    // Custom
    @Override
    public void setCleanWeatherTime(int cleanWeatherTimeIn) {
        super.setCleanWeatherTime(cleanWeatherTimeIn);
        this.getSaveHandler().markDirty();
    }

    @Override
    public void setRainTime(int time) {
        super.setRainTime(time);
        this.getSaveHandler().markDirty();
    }

    @Override
    public void setThunderTime(int time) {
        super.setThunderTime(time);
        this.getSaveHandler().markDirty();
    }

    @Override
    public void setRaining(boolean isRaining) {
        super.setRaining(isRaining);
        this.getSaveHandler().markDirty();
    }

    @Override
    public void setThundering(boolean thunderingIn) {
        super.setThundering(thunderingIn);
        this.getSaveHandler().markDirty();
    }

    public NBTTagCompound addWeatherData(NBTTagCompound compound) {
        compound.setInteger("clearWeatherTime", getCleanWeatherTime());
        compound.setInteger("rainTime", getRainTime());
        compound.setInteger("thunderTime", getThunderTime());
        compound.setBoolean("raining", isRaining());
        compound.setBoolean("thundering", isThundering());
        return compound;
    }
}
