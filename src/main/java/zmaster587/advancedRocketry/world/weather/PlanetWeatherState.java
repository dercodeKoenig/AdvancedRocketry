package zmaster587.advancedRocketry.world.weather;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Per-dimension weather state pulled out of {@link net.minecraft.world.storage.WorldInfo}.
 *
 * <p>Held by {@link PlanetWeatherSavedData} keyed by dimension id; mutated only
 * via {@link ARWeatherWorldInfo} setters. Mutations flip the {@code dirty} flag
 * — the manager pushes that flip down to the saved-data so vanilla disk save
 * picks it up. Per-listener "lastSynced" snapshots support the explicit
 * client sync (begin/end raining edges) emitted on player join / dim change.</p>
 */
public final class PlanetWeatherState {

    private int cleanWeatherTime;
    private int rainTime;
    private int thunderTime;
    private boolean raining;
    private boolean thundering;

    private transient boolean lastSyncedRaining;
    private transient boolean lastSyncedThundering;

    public PlanetWeatherState() {
    }

    public int getCleanWeatherTime() {
        return cleanWeatherTime;
    }

    public void setCleanWeatherTime(int value) {
        this.cleanWeatherTime = value;
    }

    public int getRainTime() {
        return rainTime;
    }

    public void setRainTime(int value) {
        this.rainTime = value;
    }

    public int getThunderTime() {
        return thunderTime;
    }

    public void setThunderTime(int value) {
        this.thunderTime = value;
    }

    public boolean isRaining() {
        return raining;
    }

    public void setRaining(boolean value) {
        this.raining = value;
    }

    public boolean isThundering() {
        return thundering;
    }

    public void setThundering(boolean value) {
        this.thundering = value;
    }

    public boolean wasLastSyncedRaining() {
        return lastSyncedRaining;
    }

    public void markSyncedRaining(boolean value) {
        this.lastSyncedRaining = value;
    }

    public boolean wasLastSyncedThundering() {
        return lastSyncedThundering;
    }

    public void markSyncedThundering(boolean value) {
        this.lastSyncedThundering = value;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        this.cleanWeatherTime = nbt.getInteger("cleanWeatherTime");
        this.rainTime = nbt.getInteger("rainTime");
        this.thunderTime = nbt.getInteger("thunderTime");
        this.raining = nbt.getBoolean("raining");
        this.thundering = nbt.getBoolean("thundering");
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("cleanWeatherTime", cleanWeatherTime);
        nbt.setInteger("rainTime", rainTime);
        nbt.setInteger("thunderTime", thunderTime);
        nbt.setBoolean("raining", raining);
        nbt.setBoolean("thundering", thundering);
    }
}
