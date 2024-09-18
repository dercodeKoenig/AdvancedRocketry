package zmaster587.advancedRocketry.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.advancements.ARAdvancements;
import zmaster587.advancedRocketry.tile.multiblock.TileAtmosphereTerraformer;
import zmaster587.libVulpes.block.multiblock.BlockMultiblockMachine;
import zmaster587.libVulpes.tile.multiblock.TileMultiBlock;

public class BlockAtmosphereTerraformer extends BlockMultiblockMachine {


    public BlockAtmosphereTerraformer(Class<? extends TileMultiBlock> tileClass, int guiId) {
        super(tileClass, guiId);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        boolean r = super.onBlockActivated(world,pos,state,player,hand, facing,hitX,hitY,hitZ);

        if (!world.isRemote) {
            if (world.getTileEntity(pos) instanceof TileAtmosphereTerraformer) {
                if (((TileAtmosphereTerraformer) world.getTileEntity(pos)).isComplete()) {
                    ARAdvancements.ATM_TERRAFORMER.trigger((EntityPlayerMP) player);
                }
            }

        }

        return r;
    }


}
