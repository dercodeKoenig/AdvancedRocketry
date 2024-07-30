package zmaster587.advancedRocketry.terraforming;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import zmaster587.advancedRocketry.util.TerraformingHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DimensionTerraformingInfo {
    public DimensionTerraformingInfo() {

    }
    public List<BlockPos> terraformProtectedBlocks = new ArrayList<>();
    public Set<ChunkPos> terraformChunksDone = new HashSet<>();
    public TerraformingHelper terraformingHelper;
}
