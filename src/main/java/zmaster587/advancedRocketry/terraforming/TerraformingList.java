package zmaster587.advancedRocketry.terraforming;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import zmaster587.advancedRocketry.util.TerraformingHelper;

import java.util.*;

public class TerraformingList {
    Map<Integer, DimensionTerraformingInfo> terraformingInfo;

    public TerraformingList() {
        this.terraformingInfo = new HashMap<>();
    }

    public void initDim(int dim) {
        DimensionTerraformingInfo info = terraformingInfo.getOrDefault(dim, null);
        if (info != null) {
            info.terraformChunksDone = new HashSet<>();
            info.terraformProtectedBlocks = new ArrayList<>();
        } else {
            info = new DimensionTerraformingInfo();
            info.terraformChunksDone = new HashSet<>();
            info.terraformProtectedBlocks = new ArrayList<>();
            terraformingInfo.put(dim, info);
        }
    }

    public boolean isInitialized(int dim) {
        return terraformingInfo.containsKey(dim);
    }

    public List<BlockPos> getProtectingBlocksForDimension(int dim) {
        DimensionTerraformingInfo info = terraformingInfo.getOrDefault(dim, null);
        if (info == null) {
            return null;
        }
        return info.terraformProtectedBlocks;
    }

    public void setProtectingBlocksForDimension(int dim, List<BlockPos> blocks) {
        DimensionTerraformingInfo info = terraformingInfo.getOrDefault(dim, null);
        if (info == null) {
            return;
        }
        info.terraformProtectedBlocks = blocks;
    }

    public void setChunksFullyTerraformed(int dim, Set<ChunkPos> poses) {
        DimensionTerraformingInfo info = terraformingInfo.getOrDefault(dim, null);
        if (info == null) {
            return;
        }
        info.terraformChunksDone = poses;
    }

    public Set<ChunkPos> getChunksFullyTerraformed(int dim) {
        DimensionTerraformingInfo info = terraformingInfo.getOrDefault(dim, null);
        if (info == null) {
            return null;
        }
        return info.terraformChunksDone;
    }

    public void setHelper(int dim, TerraformingHelper helper) {
        DimensionTerraformingInfo info = terraformingInfo.getOrDefault(dim, null);
        if (info == null) {
            return;
        }
        info.terraformingHelper = helper;
    }

    public TerraformingHelper getHelper(int dim) {
        DimensionTerraformingInfo info = terraformingInfo.getOrDefault(dim, null);
        if (info == null) {
            return null;
        }
        return info.terraformingHelper;
    }
}
