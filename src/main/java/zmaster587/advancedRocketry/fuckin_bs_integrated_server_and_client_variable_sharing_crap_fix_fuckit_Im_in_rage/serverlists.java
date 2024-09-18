package zmaster587.advancedRocketry.fuckin_bs_integrated_server_and_client_variable_sharing_crap_fix_fuckit_Im_in_rage;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import zmaster587.advancedRocketry.util.TerraformingHelper;

import java.util.*;


public class serverlists implements Afuckinginterface {
    Map<Integer, dimensionTerraformingInfo> terraforminginfolists;

    public serverlists(){
        this.terraforminginfolists = new HashMap<>();
    }

    public void initdim(int dim){
        if (terraforminginfolists.get(dim) != null){
            terraforminginfolists.get(dim).terraformingChunksDone = new HashSet<>();
            terraforminginfolists.get(dim).biomeChangingChunksDone = new HashSet<>();
            terraforminginfolists.get(dim).terraformingProtectedBlocks = new ArrayList<>();
        }else{
            dimensionTerraformingInfo info = new dimensionTerraformingInfo();
            info.terraformingChunksDone = new HashSet<>();
            info.biomeChangingChunksDone = new HashSet<>();
            info.terraformingProtectedBlocks = new ArrayList<>();
            terraforminginfolists.put(dim,info);
        }
    }
    public boolean isinitialized(int dim){
        return (terraforminginfolists.get(dim) != null);
    }
    public List<BlockPos> getProtectingBlocksForDimension(int dim){
        if (terraforminginfolists.get(dim) == null)return null;
        return terraforminginfolists.get(dim).terraformingProtectedBlocks;
    }
    public void setProtectingBlocksForDimension(int dim, ArrayList<BlockPos> blocks){
        if (terraforminginfolists.get(dim) == null)return;
        terraforminginfolists.get(dim).terraformingProtectedBlocks = blocks;
    }

    public void setChunksFullyTerraformed(int dim, HashSet<ChunkPos> lpos){
        if (terraforminginfolists.get(dim) == null)return;
        terraforminginfolists.get(dim).terraformingChunksDone = lpos;
    }
    public HashSet<ChunkPos> getChunksFullyTerraformed(int dim){
        if (terraforminginfolists.get(dim) == null)return null;
        return terraforminginfolists.get(dim).terraformingChunksDone;
    }

    public void setChunksFullyBiomeChanged(int dim, HashSet<ChunkPos> lpos){
        if (terraforminginfolists.get(dim) == null)return;
        terraforminginfolists.get(dim).biomeChangingChunksDone = lpos;
    }
    public HashSet<ChunkPos> getChunksFullyBiomeChanged(int dim){
        if (terraforminginfolists.get(dim) == null)return null;
        return terraforminginfolists.get(dim).biomeChangingChunksDone;
    }

    public void sethelper(int dim, TerraformingHelper helper){
        if (terraforminginfolists.get(dim) == null)return;
        terraforminginfolists.get(dim).terraformingHelper= helper;
    }
    public TerraformingHelper gethelper(int dim){
        if (terraforminginfolists.get(dim) == null)return null;
        return terraforminginfolists.get(dim).terraformingHelper;
    }
}
