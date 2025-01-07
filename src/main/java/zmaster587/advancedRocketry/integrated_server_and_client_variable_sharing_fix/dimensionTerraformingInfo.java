package zmaster587.advancedRocketry.integrated_server_and_client_variable_sharing_fix;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import zmaster587.advancedRocketry.util.TerraformingHelper;

import java.util.ArrayList;
import java.util.HashSet;

public class dimensionTerraformingInfo{
    public dimensionTerraformingInfo(){}
    public ArrayList<BlockPos> terraformingProtectedBlocks;
    public HashSet<ChunkPos> terraformingChunksDone;
    public HashSet<ChunkPos> biomeChangingChunksDone;
    public TerraformingHelper terraformingHelper;
}
