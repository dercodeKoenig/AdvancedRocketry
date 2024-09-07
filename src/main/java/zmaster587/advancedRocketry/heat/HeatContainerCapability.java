package zmaster587.advancedRocketry.heat;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("ConstantConditions")
public class HeatContainerCapability implements ICapabilitySerializable<NBTBase> {

    @CapabilityInject(IHeatContainer.class)
    private static final Capability<IHeatContainer> HEAT_CAP = null;

    private final IHeatContainer instance = HEAT_CAP.getDefaultInstance();

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == HEAT_CAP;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (HEAT_CAP != null && capability == HEAT_CAP) {
            return capability == HEAT_CAP ? HEAT_CAP.<T> cast(this.instance) : null;
        }
        return null;
    }

    @Override
    public NBTBase serializeNBT() {
        return HEAT_CAP.getStorage().writeNBT(HEAT_CAP, this.instance, null);
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        HEAT_CAP.getStorage().readNBT(HEAT_CAP, this.instance, null, nbt);
    }
}
