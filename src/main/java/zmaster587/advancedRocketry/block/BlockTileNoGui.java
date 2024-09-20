package zmaster587.advancedRocketry.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.libVulpes.block.BlockTile;

public class BlockTileNoGui extends BlockTile {
    public BlockTileNoGui(Class<? extends TileEntity> tileClass, int guiId) {
        super(tileClass, guiId);
    }

    public BlockTileNoGui(Class<? extends TileEntity> tileClass, int guiId, Material material) {
        super(tileClass, guiId, material);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return false;
    }
}
