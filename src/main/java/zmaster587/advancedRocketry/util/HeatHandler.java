package zmaster587.advancedRocketry.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class HeatHandler {

    // mapping from IBlockState string representation to heat dissipation from it
    private final Map<String, Integer> blockHeat = new HashMap<>();
    private final Map<Class<?>, IHeatHandler<?>> heatHandlers = new HashMap<>();

    public int getHeatAt(World world, BlockPos pos) {
        return getHeat(world.getBlockState(pos)) + getHeat(world.getTileEntity(pos));
    }

    public int getHeat(@Nonnull IBlockState state) {
        return blockHeat.getOrDefault(state.toString(), 0);
    }

    public int getHeat(@Nullable TileEntity te) {
        if (te == null) {
            return 0;
        }

        IHeatHandler<TileEntity> handler = (IHeatHandler<TileEntity>) heatHandlers.getOrDefault(te.getClass(), null);

        if (handler == null) {
            return 0;
        }

        return handler.getHeatDissipation(te);
    }

    public int getHeat(@Nullable Entity entity) {
        if (entity == null) {
            return 0;
        }

        IHeatHandler<Entity> handler = (IHeatHandler<Entity>) heatHandlers.getOrDefault(entity.getClass(), null);

        if (handler == null) {
            return 0;
        }

        return handler.getHeatDissipation(entity);
    }
}
