package zmaster587.advancedRocketry.world.weather;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Single {@link WorldSavedData} instance living on the overworld's
 * {@code mapStorage} holding weather state for every AR planet dimension.
 *
 * <p>The decision to centralise (one saved-data, keyed by dimension id) rather
 * than per-world saved-data is from SMART §2.2: avoids being entangled with
 * {@code WorldServerMulti}'s per-world storage layout, and avoids depending on
 * any wrapping of the secondary world's {@link net.minecraft.world.storage.WorldInfo}.
 * Overworld is loaded for the entire server lifetime, so this storage is always
 * reachable from anywhere weather state is touched.</p>
 */
public final class PlanetWeatherSavedData extends WorldSavedData {

    public static final String STORAGE_KEY = "advancedrocketry_planet_weather";

    private final Map<Integer, PlanetWeatherState> statesByDimension = new HashMap<>();

    public PlanetWeatherSavedData() {
        super(STORAGE_KEY);
    }

    public PlanetWeatherSavedData(String name) {
        super(name);
    }

    public PlanetWeatherState getOrCreate(int dimensionId) {
        PlanetWeatherState state = statesByDimension.get(dimensionId);
        if (state == null) {
            state = new PlanetWeatherState();
            statesByDimension.put(dimensionId, state);
            markDirty();
        }
        return state;
    }

    public PlanetWeatherState getIfPresent(int dimensionId) {
        return statesByDimension.get(dimensionId);
    }

    public void put(int dimensionId, PlanetWeatherState state) {
        statesByDimension.put(dimensionId, state);
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        statesByDimension.clear();
        NBTTagList list = nbt.getTagList("dimensions", 10 /* NBTTagCompound */);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int dim = entry.getInteger("dim");
            PlanetWeatherState state = new PlanetWeatherState();
            state.readFromNBT(entry);
            statesByDimension.put(dim, state);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Integer, PlanetWeatherState> e : statesByDimension.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("dim", e.getKey());
            e.getValue().writeToNBT(entry);
            list.appendTag(entry);
        }
        compound.setTag("dimensions", list);
        return compound;
    }
}
