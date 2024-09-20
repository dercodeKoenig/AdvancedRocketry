package zmaster587.advancedRocketry.tile.heat;

import net.minecraft.tileentity.TileEntity;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.heat.IHeatAccumulator;

public class TileDefaultHeatAccumulator extends TileEntity implements IHeatAccumulator {

    @Override
    public int getHeatCapacity() {
        return ARConfiguration.getCurrentConfig().heatAccumulatorCapacity;
    }
}
