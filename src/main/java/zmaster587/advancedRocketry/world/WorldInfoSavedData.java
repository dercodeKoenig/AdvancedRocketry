package zmaster587.advancedRocketry.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSavedData;

public class WorldInfoSavedData extends WorldSavedData {

    private World world;
    private WorldInfo readInfo;

    public WorldInfoSavedData(String name) {
        super(name);
    }

    public WorldInfoSavedData(World world) {
        this("WorldInfoSavedData");
        this.world = world;
        this.markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        readInfo = new WorldInfo(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return ((CustomDerivedWorldInfo) world.getWorldInfo()).addWeatherData(compound);
    }

    public void updateWorldInfo(World world) {
        if (readInfo == null) {
            // WorldSavedData not loaded from NBT
            return;
        }

        this.world = world;

        WorldInfo target = world.getWorldInfo();

        target.setCleanWeatherTime(readInfo.getCleanWeatherTime());
        target.setRaining(readInfo.isRaining());
        target.setRainTime(readInfo.getRainTime());
        target.setThundering(readInfo.isThundering());
        target.setThunderTime(readInfo.getThunderTime());
    }
}
