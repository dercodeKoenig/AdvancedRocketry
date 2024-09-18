package zmaster587.advancedRocketry.tile.hatch;

import zmaster587.libVulpes.tile.multiblock.hatch.TileInventoryHatch;

public class TileInvHatch extends TileInventoryHatch {

    public TileInvHatch(int invSize) {
        super(invSize);
    }

    @Override
    public String getModularInventoryName() {
        return "container.invhatch";
    }
}
