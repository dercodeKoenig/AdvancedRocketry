package zmaster587.advancedRocketry.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.IRocketEngine;
import zmaster587.advancedRocketry.tile.TileBrokenPart;
import zmaster587.advancedRocketry.util.IBrokenPartBlock;
import zmaster587.libVulpes.block.BlockFullyRotatable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockRocketMotor extends BlockFullyRotatable implements IRocketEngine, IBrokenPartBlock {

    public BlockRocketMotor(Material mat) {
        super(mat);
        this.setDefaultState(this.getDefaultState().withProperty(FACING, EnumFacing.DOWN));
    }

    public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
        if (world.getBlockState(pos.add(0, 1, 0)).getBlock() instanceof BlockFuelTank) {
            return state.withProperty(FACING, EnumFacing.DOWN);
        }
        if (world.getBlockState(pos.add(0, -1, 0)).getBlock() instanceof BlockFuelTank) {
            return state.withProperty(FACING, EnumFacing.UP);
        }
        if (world.getBlockState(pos.add(1, 0, 0)).getBlock() instanceof BlockFuelTank) {
            return state.withProperty(FACING, EnumFacing.EAST);
        }
        if (world.getBlockState(pos.add(-1, 0, 0)).getBlock() instanceof BlockFuelTank) {
            return state.withProperty(FACING, EnumFacing.WEST);
        }
        if (world.getBlockState(pos.add(0, 0, 1)).getBlock() instanceof BlockFuelTank) {
            return state.withProperty(FACING, EnumFacing.SOUTH);
        }
        if (world.getBlockState(pos.add(0, 0, -1)).getBlock() instanceof BlockFuelTank) {
            return state.withProperty(FACING, EnumFacing.NORTH);
        }
        return super.getActualState(state, world, pos);
    }

    @Override
    public int getThrust(World world, BlockPos pos) {
        return 10;
    }

    @Override
    public int getFuelConsumptionRate(World world, int x, int y, int z) {
        return 1;
    }

    @Override
    public boolean isOpaqueCube(@Nonnull IBlockState state) {
        return false;
    }

    @Override
    public boolean isBlockNormalCube(@Nonnull final IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(final IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, @Nonnull ItemStack stack) {
        world.setBlockState(pos, state.withProperty(FACING, EnumFacing.DOWN));

        TileEntity te = world.getTileEntity(pos);
        ((TileBrokenPart) te).setStage(stack.getItemDamage());
    }

//    @Override
//    public boolean onBlockActivated(final World worldIn, final BlockPos pos, final IBlockState state, final EntityPlayer playerIn, final EnumHand hand, final EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
//        if (!worldIn.isRemote) {
//            TileEntity te = worldIn.getTileEntity(pos);
//            ((TileBrokenPart) te).transition();
//        }
//        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
//    }

    @Override
    public void harvestBlock(final World world, final EntityPlayer player, final BlockPos pos, final IBlockState state, @Nullable final TileEntity te, final ItemStack stack) {
        if (!world.isRemote && !player.isCreative()) {
            TileBrokenPart tile = (TileBrokenPart) te;

            ItemStack drop = getDropItem(state, world, tile);

            world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), drop));
        }

        super.harvestBlock(world, player, pos, state, te, stack);
    }

    public IBlockState getStateFromMeta(int meta) {
        if (meta > 5) {
            return this.getDefaultState();
        }
        return super.getStateFromMeta(meta);
    }

    @Override
    public void getDrops(final NonNullList<ItemStack> drops, final IBlockAccess world, final BlockPos pos, final IBlockState state, final int fortune) {
        // no drops here
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final World worldIn, final IBlockState state) {
        return new TileBrokenPart(10, 2 * (float) ARConfiguration.getCurrentConfig().increaseWearIntensityProb);
    }

    @Override
    public ItemStack getDropItem(final IBlockState state, final World world, final @Nullable TileBrokenPart te) {
        ItemStack drop = new ItemStack(this.getItemDropped(state, world.rand, 0));
        if (te != null) {
            drop.setItemDamage(te.getStage());
        }
        return drop;
    }
}
