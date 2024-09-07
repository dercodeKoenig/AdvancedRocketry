package zmaster587.advancedRocketry.heat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.util.IHeatHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public enum HeatHandler {
    INSTANCE("config/advRocketry/heat.json");

    // mapping from IBlockState string representation to heat dissipation from it
    private final Map<String, Integer> blockHeat = new HashMap<>();
    private final Map<Class<?>, IHeatHandler<?>> heatHandlers = new HashMap<>();

    private final String file;

    HeatHandler(String file) {
        this.file = file;
        load();
    }

    public void load() {
        File f = new File(file);
        if (!f.exists()) {
            blockHeat.clear();
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try (Reader r = new FileReader(file)) {
            Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            blockHeat.putAll(GSON.fromJson(root.getAsJsonObject("individual"), HashMap.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
