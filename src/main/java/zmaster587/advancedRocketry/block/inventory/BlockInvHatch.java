package zmaster587.advancedRocketry.block.inventory;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.tile.hatch.TileInvHatch;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.inventory.GuiHandler;

public class BlockInvHatch extends Block {

    public BlockInvHatch(Material material) {
        super(material);
    }

    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileInvHatch(1);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            playerIn.openGui(LibVulpes.instance, GuiHandler.guiId.MODULAR.ordinal(), worldIn, pos.getX(), pos.getY(), pos.getZ());
        }

        return true;
    }
}
