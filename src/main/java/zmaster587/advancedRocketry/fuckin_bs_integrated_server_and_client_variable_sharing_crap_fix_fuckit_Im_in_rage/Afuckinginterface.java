package zmaster587.advancedRocketry.fuckin_bs_integrated_server_and_client_variable_sharing_crap_fix_fuckit_Im_in_rage;


import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import zmaster587.advancedRocketry.util.TerraformingHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public interface Afuckinginterface {

    public void initdim(int dim);

    public boolean isinitialized(int dim);

    public List<BlockPos> getProtectingBlocksForDimension(int dim);

    public void setProtectingBlocksForDimension(int dim, ArrayList<BlockPos> blocks);

    public void setChunksFullyTerraformed(int dim, HashSet<ChunkPos> lpos);

    public HashSet<ChunkPos> getChunksFullyTerraformed(int dim);


    public void setChunksFullyBiomeChanged(int dim, HashSet<ChunkPos> lpos);

    public HashSet<ChunkPos> getChunksFullyBiomeChanged(int dim);

    public void sethelper(int dim, TerraformingHelper helper);

    public TerraformingHelper gethelper(int dim);

}
