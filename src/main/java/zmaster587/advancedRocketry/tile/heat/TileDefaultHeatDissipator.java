package zmaster587.advancedRocketry.tile.heat;

import net.minecraft.tileentity.TileEntity;
import zmaster587.advancedRocketry.heat.IHeatDissipator;

public class TileDefaultHeatDissipator extends TileEntity implements IHeatDissipator {

    @Override
    public int getDissipationPerSecond() {
        return 10;
    }
}
