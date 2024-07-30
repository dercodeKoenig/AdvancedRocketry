package zmaster587.advancedRocketry.satellite;

import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.api.satellite.SatelliteProperties;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.item.ItemWeatherController;
import zmaster587.advancedRocketry.network.PacketAirParticle;
import zmaster587.advancedRocketry.network.PacketFluidParticle;
import zmaster587.libVulpes.api.IUniversalEnergy;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.RandomUtils.nextInt;

public class SatelliteWeatherController extends SatelliteBase {

    public int mode_id;
    //public int timer;
    public int last_mode_id = 0;
    public int floodlevel = -1;
    private List<BlockPos> viable_positions;

    public SatelliteWeatherController() {
        super();
        mode_id = 0;
        last_mode_id = 0;
        //timer = 0;
        floodlevel = -1;
        viable_positions = new ArrayList<>();
    }

    public int getMode_id() {
        return mode_id;
    }
    public int getFloodlevel(){
        if (floodlevel == -1){
            floodlevel = DimensionManager.getInstance().getDimensionProperties(getDimensionId()).getSeaLevel();
        }
        return floodlevel;
    }

    @Override
    public String getInfo(World world) {
        return "Ready";
    }

    @Override
    public String getName() {
        return "Weather Satellite";
    }

    @Override
    @Nonnull
    public ItemStack getControllerItemStack(@Nonnull ItemStack satIdChip,
                                            SatelliteProperties properties) {

        ItemWeatherController idChipItem = (ItemWeatherController) satIdChip.getItem();
        idChipItem.setSatellite(satIdChip, properties);
        return satIdChip;
    }

    @Override
    public boolean isAcceptableControllerItemStack(@Nonnull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemWeatherController;
    }


    @Override
    public void tickEntity() {
        super.tickEntity();

        if (last_mode_id != mode_id) {
            last_mode_id = mode_id;
            //this.timer = 0;
            viable_positions.clear();
        }

        //if (this.timer > 0) {
        //    this.timer--;
        int listsize = viable_positions.size();
        World world = net.minecraftforge.common.DimensionManager.getWorld(getDimensionId());
        if (listsize > 0) {
            if (mode_id == 0) {
                BlockPos new_block = viable_positions.remove(nextInt(0, listsize));
                //BlockPos new_block = viable_positions.remove(0);
                if (world.getBlockState(new_block).getBlock() == Blocks.AIR || world.getBlockState(new_block).getBlock() == Blocks.FLOWING_WATER || (world.getBlockState(new_block).getBlock() == Blocks.WATER && world.getBlockState(new_block).getValue(BlockLiquid.LEVEL) != 0)) {
                    world.setBlockState(new_block, Blocks.WATER.getDefaultState());
                    world.notifyBlockUpdate(new_block, world.getBlockState(new_block), world.getBlockState(new_block), 3);
                    PacketHandler.sendToNearby(new PacketFluidParticle(new_block.up().up().up().up().up().up().up().up().up().up().up().up(), new_block, 45, 0xd4f1f9), world.provider.getDimension(), new_block, 128);
                    PacketHandler.sendToNearby(new PacketFluidParticle(new_block.up().up().up().up().up().up().up().up().up().up().up().up(), new_block, 35, 0xd4f1f9), world.provider.getDimension(), new_block, 128);
                    PacketHandler.sendToNearby(new PacketFluidParticle(new_block.up().up().up().up().up().up().up().up().up().up().up().up(), new_block, 25, 0xd4f1f9), world.provider.getDimension(), new_block, 128);
                    PacketHandler.sendToNearby(new PacketFluidParticle(new_block.up().up().up().up().up().up().up().up().up().up().up().up(), new_block, 15, 0xd4f1f9), world.provider.getDimension(), new_block, 128);
                }
            }
        }
        if (listsize > 0) {
            if (mode_id == 1) {
                //BlockPos new_block = viable_positions.remove(0);
                BlockPos new_block = viable_positions.remove(nextInt(0, listsize));
                DimensionManager.getInstance().getDimensionProperties(getDimensionId()).addWaterLockedPos(new HashedBlockPosition(new_block));
                if (world.getBlockState(new_block).getBlock() == Blocks.WATER || world.getBlockState(new_block).getBlock() == Blocks.FLOWING_WATER) {
                    world.setBlockState(new_block, Blocks.AIR.getDefaultState());
                    world.notifyBlockUpdate(new_block, world.getBlockState(new_block), world.getBlockState(new_block), 3);
                    PacketHandler.sendToNearby(new PacketAirParticle(new HashedBlockPosition(new_block.getX(), new_block.getY() + 1, new_block.getZ())), world.provider.getDimension(), new_block, 128);
                    PacketHandler.sendToNearby(new PacketAirParticle(new HashedBlockPosition(new_block.getX(), new_block.getY() + 2, new_block.getZ())), world.provider.getDimension(), new_block, 128);
                    PacketHandler.sendToNearby(new PacketAirParticle(new HashedBlockPosition(new_block.getX(), new_block.getY() + 3, new_block.getZ())), world.provider.getDimension(), new_block, 128);
                    PacketHandler.sendToNearby(new PacketAirParticle(new HashedBlockPosition(new_block.getX(), new_block.getY() + 4, new_block.getZ())), world.provider.getDimension(), new_block, 128);
                    PacketHandler.sendToNearby(new PacketAirParticle(new HashedBlockPosition(new_block.getX(), new_block.getY() + 5, new_block.getZ())), world.provider.getDimension(), new_block, 128);
                }
            }
        }
        if (listsize > 0) {
            if (mode_id == 2) {
                //BlockPos new_block = viable_positions.remove(0);
                BlockPos new_block = viable_positions.remove(nextInt(0, listsize));

                if (world.getBlockState(new_block).getBlock() == Blocks.AIR || world.getBlockState(new_block).getBlock() == Blocks.FLOWING_WATER || world.getBlockState(new_block).getBlock() == Blocks.WATER) {
                    world.setBlockState(new_block, Blocks.WATER.getDefaultState());
                    world.notifyBlockUpdate(new_block, world.getBlockState(new_block), world.getBlockState(new_block), 3);
                    PacketHandler.sendToNearby(new PacketFluidParticle(new_block.up().up().up().up().up().up().up().up().up().up().up().up(), new_block, 45, 0xd4f1f9), world.provider.getDimension(), new_block, 128);
                    PacketHandler.sendToNearby(new PacketFluidParticle(new_block.up().up().up().up().up().up().up().up().up().up().up().up(), new_block, 35, 0xd4f1f9), world.provider.getDimension(), new_block, 128);
                    PacketHandler.sendToNearby(new PacketFluidParticle(new_block.up().up().up().up().up().up().up().up().up().up().up().up(), new_block, 25, 0xd4f1f9), world.provider.getDimension(), new_block, 128);
                    PacketHandler.sendToNearby(new PacketFluidParticle(new_block.up().up().up().up().up().up().up().up().up().up().up().up(), new_block, 15, 0xd4f1f9), world.provider.getDimension(), new_block, 128);
                }
            }
        }
    }





    private boolean is_block_in_list(List<BlockPos> l, BlockPos p) {
        for (BlockPos i : l) {
            if (i.getX() == p.getX() && i.getZ() == p.getZ() && i.getY() == p.getY()) {
                return true;
            }
        }
        return false;
    }

    private boolean can_be_made_water(BlockPos block, World world) {
        return world.getBlockState(block).getBlock() == Blocks.AIR ||
                world.getBlockState(block).getBlock() == Blocks.WATER ||
                world.getBlockState(block).getBlock() == Blocks.FLOWING_WATER ||
                world.getBlockState(block).getMaterial().isReplaceable()
                ;
    }
    /*
    private boolean is_at_edge(BlockPos init, BlockPos current){
        int dx = init.getX() - current.getX();
        int dz = init.getZ() - current.getZ();
        int r = (int) Math.sqrt( dx*dx+dz*dz );
        return r > radius;
    }
    */

    private boolean check_one_block(BlockPos block_x, World world, List<BlockPos> checked_blocks, int in, int max_vol) {


/*
        if (is_at_edge(init, block_x))
            return false;
 */

        if (is_block_in_list(viable_positions, block_x))
            return true;
        if (checked_blocks.size() > max_vol)
            return false;
        if (!is_block_in_list(checked_blocks, block_x) && can_be_made_water(block_x, world)) {
            checked_blocks.add(block_x);
            if (!find_connected_blocks_down(block_x, world, checked_blocks, in + 1, max_vol))
                return false;
        }
        return true;
    }

    private boolean find_connected_blocks_down(BlockPos start, World world, List<BlockPos> checked_blocks, int in, int max_vol) {

        if (!check_one_block(start.north(), world, checked_blocks, in, max_vol))
            return false;
        if (!check_one_block(start.west(), world, checked_blocks, in, max_vol))
            return false;
        if (!check_one_block(start.south(), world, checked_blocks, in, max_vol))
            return false;
        if (!check_one_block(start.east(), world, checked_blocks, in, max_vol))
            return false;
        //if (!check_one_block(init, start.down(), world, checked_blocks, in, max_vol))
        List<BlockPos>lower_layer = new LinkedList<>();
        if (!check_one_block(start.down(), world, lower_layer, in, max_vol))
            return false;
        checked_blocks.addAll(lower_layer);


        return true;
    }

    @Override
    public boolean performAction(EntityPlayer player, World world, BlockPos pos) {
        if (world.isRemote) return false;

        if (mode_id == 0) {

            long startTime = System.nanoTime();

            //reset timer so that tick loop can run
            //this.timer = 20 * 180; // run for max 180 seconds
            int max_vol = 300;
            int radius = 4;
            if (viable_positions.size() > 4000)return false;
            for (int z = -radius; z < radius + 1; z++) {
                for (int x = -radius; x < radius + 1; x++) {

                    BlockPos top_block = world.getHeight(new BlockPos(x + pos.getX(), 0, z + pos.getZ()));

                    // go down until top_block has a solid block below it
                    while (!world.getBlockState(top_block.down()).isOpaqueCube() && top_block.getY() > 0)
                        top_block = top_block.down();

                    // rek flood check
                    List<BlockPos> checked_blocks = new ArrayList<>();
                    while (check_one_block(top_block, world, checked_blocks, 0, max_vol)) {
                        viable_positions.addAll(checked_blocks);
                        checked_blocks.clear();
                        // up one block and check again
                        top_block = top_block.up();
                    }
                }
            }

            long endTime = System.nanoTime();
            long durationInNanoseconds = endTime - startTime;
        } else if(mode_id == 1) {

            long startTime = System.nanoTime();

            //reset timer so that tick loop can run
            //this.timer = 20 * 180; // run for max 180 seconds
            int max_vol = 1000;
            int radius = 16;
            if (viable_positions.size() > 4000)return false;
            for (int z = -radius; z < radius + 1; z++) {
                for (int x = -radius; x < radius + 1; x++) {

                    BlockPos top_block = world.getHeight(new BlockPos(x + pos.getX(), 0, z + pos.getZ()));
                    top_block = top_block.down();
                    while (world.getBlockState(top_block).getBlock() == Blocks.WATER){
                        viable_positions.add(top_block);
                        top_block = top_block.down();
                    }
                }
            }

            long endTime = System.nanoTime();
            long durationInNanoseconds = endTime - startTime;
        }else if(mode_id == 2) {

            long startTime = System.nanoTime();

            //reset timer so that tick loop can run
            //this.timer = 20 * 180; // run for max 180 seconds
            int max_vol = 1000;
            int radius = 16;
            if (viable_positions.size() > 4000)return false;
            for (int z = -radius; z < radius + 1; z++) {
                for (int x = -radius; x < radius + 1; x++) {

                    BlockPos top_block = new BlockPos(x + pos.getX(), this.floodlevel, z + pos.getZ());
                    top_block = top_block.down();
                    while (can_be_made_water(top_block, world)){
                        viable_positions.add(top_block);
                        top_block = top_block.down();
                    }
                }
            }

            long endTime = System.nanoTime();
            long durationInNanoseconds = endTime - startTime;
        }
        return false;
    }


    @Override
    public double failureChance() {
        return 0;
    }

    public IUniversalEnergy getBattery() {
        return battery;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("mode_id", mode_id);
        nbt.setInteger("last_mode_id", last_mode_id);
        nbt.setInteger("floodlevel", floodlevel);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        mode_id = nbt.getInteger("mode_id");
        last_mode_id = nbt.getInteger("last_mode_id");
        floodlevel = nbt.getInteger("floodlevel");
    }
}

