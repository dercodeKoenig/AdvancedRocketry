package zmaster587.advancedRocketry.heat;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import zmaster587.advancedRocketry.util.nbt.NBTTagCompoundBuilder;

import javax.annotation.Nullable;

public class HeatContainerStorage implements Capability.IStorage<IHeatContainer> {

    @Nullable
    @Override
    public NBTBase writeNBT(final Capability<IHeatContainer> capability, final IHeatContainer instance, final EnumFacing side) {
        return NBTTagCompoundBuilder.create()
                .setInt("heat", instance.getCurrentHeat())
                .setInt("maxHeat", instance.getMaxHeat())
                .build();
    }

    @Override
    public void readNBT(final Capability<IHeatContainer> capability, final IHeatContainer instance, final EnumFacing side, final NBTBase nbt) {
        NBTTagCompound compound = (NBTTagCompound) nbt;
        instance.setMaxHeat(compound.getInteger("maxHeat"));
        instance.setHeat(compound.getInteger("heat"));
    }
}
