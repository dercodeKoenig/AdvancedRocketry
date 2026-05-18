package zmaster587.advancedRocketry.world.weather;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * {@link WorldInfo} wrapper installed on AR-planet {@link net.minecraft.world.WorldServer}s
 * by {@link PlanetWeatherManager}. Delegates every non-weather call to the
 * underlying {@link WorldInfo} (the secondary world's
 * {@link net.minecraft.world.storage.DerivedWorldInfo} that vanilla
 * {@code WorldServerMulti} installed) and serves weather state from a
 * per-dimension {@link PlanetWeatherState} living in
 * {@link PlanetWeatherSavedData}.
 *
 * <p>Why a wrapper instead of subclassing {@code DerivedWorldInfo}: this class
 * cares only about behaviour, not about the secret persistent state hidden
 * inside vanilla's {@code WorldInfo} (level format version, custom boss event
 * registry, etc.). Delegation keeps that state intact.</p>
 *
 * <p>The {@code dirtyMarker} callback lets the manager push "saved-data is
 * stale, schedule a re-save" without us holding a {@link net.minecraft.world.World}
 * reference (the wrapper outlives world unload during dim flicker — a hard
 * world reference would leak the entire dimension).</p>
 */
public final class ARWeatherWorldInfo extends WorldInfo {

    private final WorldInfo delegate;
    private final PlanetWeatherState weatherState;
    private final Runnable dirtyMarker;

    public ARWeatherWorldInfo(WorldInfo delegate, PlanetWeatherState weatherState, Runnable dirtyMarker) {
        // Call the WorldInfo no-arg ctor — initialises the (never-read)
        // internal scaffolding (GameRules, dimensionData, customBossEvents)
        // to safe defaults. We deliberately do NOT seed from the delegate's
        // NBT (that path goes through FMLCommonHandler.getDataFixer() and is
        // brittle outside a fully-initialised Forge runtime, e.g. unit tests).
        // Every public getter is overridden to delegate, and cloneNBTCompound
        // is overridden too, so the wrapper's own super-state stays inert.
        // The deleted CustomDerivedWorldInfo used the same pattern in production.
        super();
        this.delegate = delegate;
        this.weatherState = weatherState;
        this.dirtyMarker = dirtyMarker;
    }

    /** Used by {@link PlanetWeatherManager#unwrap} to peel the wrapper off without losing state. */
    public WorldInfo getDelegate() {
        return delegate;
    }

    // ── Weather: backed by PlanetWeatherState ─────────────────────────────

    @Override
    public int getCleanWeatherTime() {
        return weatherState.getCleanWeatherTime();
    }

    @Override
    public void setCleanWeatherTime(int cleanWeatherTimeIn) {
        weatherState.setCleanWeatherTime(cleanWeatherTimeIn);
        dirtyMarker.run();
    }

    @Override
    public boolean isRaining() {
        return weatherState.isRaining();
    }

    @Override
    public void setRaining(boolean isRaining) {
        weatherState.setRaining(isRaining);
        dirtyMarker.run();
    }

    @Override
    public int getRainTime() {
        return weatherState.getRainTime();
    }

    @Override
    public void setRainTime(int time) {
        weatherState.setRainTime(time);
        dirtyMarker.run();
    }

    @Override
    public boolean isThundering() {
        return weatherState.isThundering();
    }

    @Override
    public void setThundering(boolean thunderingIn) {
        weatherState.setThundering(thunderingIn);
        dirtyMarker.run();
    }

    @Override
    public int getThunderTime() {
        return weatherState.getThunderTime();
    }

    @Override
    public void setThunderTime(int time) {
        weatherState.setThunderTime(time);
        dirtyMarker.run();
    }

    // ── Everything else: delegate ─────────────────────────────────────────
    //
    // The setters mostly no-op (vanilla DerivedWorldInfo does the same — a
    // secondary world is not supposed to mutate shared overworld state).
    // Where vanilla DerivedWorldInfo does write through (dimension data), we
    // forward to match its semantics.

    @Override
    public NBTTagCompound cloneNBTCompound(@Nullable NBTTagCompound nbt) {
        return delegate.cloneNBTCompound(nbt);
    }

    @Override
    public long getSeed() {
        return delegate.getSeed();
    }

    @Override
    public int getSpawnX() {
        return delegate.getSpawnX();
    }

    @Override
    public int getSpawnY() {
        return delegate.getSpawnY();
    }

    @Override
    public int getSpawnZ() {
        return delegate.getSpawnZ();
    }

    @Override
    public long getWorldTotalTime() {
        return delegate.getWorldTotalTime();
    }

    @Override
    public long getWorldTime() {
        return delegate.getWorldTime();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public long getSizeOnDisk() {
        return delegate.getSizeOnDisk();
    }

    @Override
    public NBTTagCompound getPlayerNBTTagCompound() {
        return delegate.getPlayerNBTTagCompound();
    }

    @Override
    public String getWorldName() {
        return delegate.getWorldName();
    }

    @Override
    public int getSaveVersion() {
        return delegate.getSaveVersion();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public long getLastTimePlayed() {
        return delegate.getLastTimePlayed();
    }

    @Override
    public GameType getGameType() {
        return delegate.getGameType();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setSpawnX(int x) {
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setSpawnY(int y) {
    }

    @Override
    public void setWorldTotalTime(long time) {
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setSpawnZ(int z) {
    }

    @Override
    public void setSpawn(BlockPos spawnPoint) {
    }

    @Override
    public void setWorldName(String worldName) {
    }

    @Override
    public void setSaveVersion(int version) {
    }

    @Override
    public boolean isMapFeaturesEnabled() {
        return delegate.isMapFeaturesEnabled();
    }

    @Override
    public boolean isHardcoreModeEnabled() {
        return delegate.isHardcoreModeEnabled();
    }

    @Override
    public WorldType getTerrainType() {
        return delegate.getTerrainType();
    }

    @Override
    public void setTerrainType(WorldType type) {
    }

    @Override
    public boolean areCommandsAllowed() {
        return delegate.areCommandsAllowed();
    }

    @Override
    public void setAllowCommands(boolean allow) {
    }

    @Override
    public boolean isInitialized() {
        return delegate.isInitialized();
    }

    @Override
    public void setServerInitialized(boolean initializedIn) {
    }

    @Override
    public GameRules getGameRulesInstance() {
        return delegate.getGameRulesInstance();
    }

    @Override
    public EnumDifficulty getDifficulty() {
        return delegate.getDifficulty();
    }

    @Override
    public void setDifficulty(EnumDifficulty newDifficulty) {
    }

    @Override
    public boolean isDifficultyLocked() {
        return delegate.isDifficultyLocked();
    }

    @Override
    public void setDifficultyLocked(boolean locked) {
    }

    @Override
    @Deprecated
    public void setDimensionData(DimensionType dimensionIn, NBTTagCompound compound) {
        delegate.setDimensionData(dimensionIn, compound);
    }

    @Override
    @Deprecated
    public NBTTagCompound getDimensionData(DimensionType dimensionIn) {
        return delegate.getDimensionData(dimensionIn);
    }

    @Override
    public void setDimensionData(int dimensionID, NBTTagCompound compound) {
        delegate.setDimensionData(dimensionID, compound);
    }

    @Override
    public NBTTagCompound getDimensionData(int dimensionID) {
        return delegate.getDimensionData(dimensionID);
    }
}
