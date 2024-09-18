package zmaster587.advancedRocketry.util;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.network.PacketBiomeIDChange;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.util.Random;


public class BiomeHandler {

    public static void decorate_simple(World world, Biome biomeId, Biome old_biome, BlockPos pos) {

        BlockPos yy = world.getHeight(pos);
        while (!world.getBlockState(yy.down()).isOpaqueCube() && yy.getY() > 0)
            yy = yy.down();

        if (old_biome.topBlock != biomeId.topBlock) {
            if (world.getBlockState(yy.down()) == old_biome.topBlock)
                world.setBlockState(yy.down(), biomeId.topBlock);
        }

        decorateBiome(world, yy, biomeId);
    }

    // Bro I am sorry for changing this again and I know it will mess up your mixin but terraforming had a BIG update so....
    public static void changeBiome(World world, Biome biomeId, Biome old_biome, BlockPos pos) {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        if (old_biome != biomeId) {
            byte[] biomeArr = chunk.getBiomeArray();
            try {
                biomeArr[(pos.getX() & 15) + (pos.getZ() & 15) * 16] = (byte) Biome.getIdForBiome(biomeId);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    public static int get_height_blocks_only(IBlockState[] blocks) {
        int h = blocks.length-1;
        while (h>0 && (!blocks[h].isOpaqueCube())){
            h--;
        }
        return h;
    }

    public static int get_height_blocks_only(World world, BlockPos pos) {
        BlockPos yy = world.getHeight(pos);
        while ((!world.getBlockState(yy).isOpaqueCube()) && yy.getY() > 0)
            yy = yy.down();
        return yy.getY();
    }

    public static void do_heavy_terraforming(World world, Biome biomeId, Biome old_biome, BlockPos pos, int dimId) {

        int inchunkx = ((pos.getX() % 16) + 16) % 16;
        int inchunkz = ((pos.getZ() % 16) + 16) % 16;

        long startTime;
        startTime = System.currentTimeMillis();
        DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dimId);

        ChunkPos cpos = DimensionProperties.proxylists.gethelper(props.getId()).getChunkPosFromBlockPos(pos);


        IBlockState[] target_blocks = DimensionProperties.proxylists.gethelper(props.getId()).getBlocksAt(pos.getX(), pos.getZ());
        chunkdata data = DimensionProperties.proxylists.gethelper(props.getId()).getChunkFromList(cpos.x, cpos.z);
        //System.out.println("d1"+(System.currentTimeMillis()-startTime));
        //startTime = System.currentTimeMillis();

        //this should never be executed because it was removed from queue
        //protected chunks will not be added to this queue again
        if (data.type == TerraformingType.PROTECTED) {
            //System.out.println("working protected");
            decorate_simple(world, biomeId, old_biome, pos);
            DimensionProperties.proxylists.gethelper(props.getId()).getChunkFromList(cpos.x, cpos.z).set_position_fully_generated(inchunkx, inchunkz);
            DimensionProperties.proxylists.gethelper(props.getId()).register_height_change(pos); // it does not really changetheheight but it will notify the border to update
        } else if (data.type == TerraformingType.ALLOWED) {
            //System.out.println("working full");

            if (!data.fully_generated[inchunkx][inchunkz]) {
                //fast replacing
                //for (int i = 0; i < 255; i++) {
                //    world.setBlockState(new BlockPos(pos.getX(), i, pos.getZ()), target_blocks[i], 2);
                //}


                int target_height = 255;
                for (int i = 255; i >= 0; i--) {
                    if(!target_blocks[i].equals(Blocks.AIR.getDefaultState())){
                        target_height = i;
                        break;
                    }
                }
                int current_world_height = world.getHeight(pos.getX(), pos.getZ());

                //slow replacing
                int y_per_iteration = 3;
                int blocks_replaced = 0;
                //go up from 0
                for (int i = 0; i < 255; i++) {
                    if (i>target_height)break;

                    if (blocks_replaced >= y_per_iteration)
                        break;

                    if (!world.getBlockState(new BlockPos(pos.getX(), i, pos.getZ())).equals(target_blocks[i])) {
                        world.setBlockState(new BlockPos(pos.getX(), i, pos.getZ()), target_blocks[i], 2);

                        //if both are underground player would not notice so only limit the actual height changes
                        if (i>=current_world_height) {
                            blocks_replaced += 1;
                        }
                    }
                }
                //now go down from 255
                for (int i = 255; i >=0; i--) {
                    if (i<=target_height)break;

                    if (blocks_replaced >= y_per_iteration)
                        break;

                    if (!world.getBlockState(new BlockPos(pos.getX(), i, pos.getZ())).equals(target_blocks[i])) {
                        world.setBlockState(new BlockPos(pos.getX(), i, pos.getZ()), target_blocks[i], 2);
                        blocks_replaced += 1;
                    }
                }



                // as long as terrain does not match the target height, re-add position to queue
                //System.out.println("heights:"+get_height_blocks_only(world, pos) +":"+ get_height_blocks_only(target_blocks));

                // you need to check this for the entire chunk
                // or the laser will reset on every new world load to its starting position and ignore the radius and iterate every chunk and skip over the ones that are already generated
                // or make it like this: every time a single position is fully generated, check every chunk 3x3 around it and see if you can populate them
                // use the chunk list again - replace spiral mod  with global mode that scatters the chunks to make it a little more random. every new load it will start from its starting position again
                if (get_height_blocks_only(world, pos) == get_height_blocks_only(target_blocks)) {
                    DimensionProperties.proxylists.gethelper(props.getId()).getChunkFromList(cpos.x, cpos.z).set_position_fully_generated(inchunkx, inchunkz);
                } else {
                    DimensionProperties.proxylists.gethelper(props.getId()).add_position_to_queue(pos);
                    DimensionProperties.proxylists.gethelper(props.getId()).register_height_change(pos);

                    //because height was changed, decorate the top block again
                    //this will update the top block and make some grass/flowers
                    world.setBlockState(world.getHeight(pos).down(),biomeId.topBlock);
                    //decorateBiome(world, pos, biomeId); //if we want to do grass and flowers - but this would not fit right after laser hits
                }

            }
        } else if (data.type == TerraformingType.BORDER) {

            //this is to be sure the top block is changed even if the height matches already
            BlockPos yy = world.getHeight(pos);
            while (!world.getBlockState(yy.down()).isOpaqueCube() && yy.getY() > 0)
                yy = yy.down();

            if (old_biome.topBlock != biomeId.topBlock) {
                if (world.getBlockState(yy.down()) == old_biome.topBlock)
                    world.setBlockState(yy.down(), biomeId.topBlock);
            }


            if (target_blocks != null) {

                int filter_size = 5;
                //System.out.println("working border");

                world.getChunkFromBlockCoords(pos.add(-filter_size, 0, 0)); // Ensure the chunk of the target positions are generated
                world.getChunkFromBlockCoords(pos.add(filter_size, 0, 0)); // Ensure the chunk of the target positions are generated
                world.getChunkFromBlockCoords(pos.add(0, 0, -filter_size)); // Ensure the chunk of the target positions are generated
                world.getChunkFromBlockCoords(pos.add(0, 0, filter_size)); // Ensure the chunk of the target positions are generated


                //this has to be weighted by distance
                float heightsum = 0;
                float num_samples = 0;
                for (int x = -filter_size; x <= filter_size; x++) {
                    for (int z = -filter_size; z <= filter_size; z++) {
                        float w = (1.0f / (0.2f + x * x + z * z));
                        heightsum += get_height_blocks_only(world, pos.add(x, 0, z)) * w;
                        num_samples += 1 * w;
                    }
                }

                int avg_height = Math.round(heightsum / num_samples);

                int prev_height = get_height_blocks_only(world, pos);
                if (avg_height == prev_height) { // nothing to do
                    return;
                }

                //now we want to set the target height to >= sea level to fill it with the oceanblock
                avg_height = Math.max(props.getSeaLevel(), avg_height);

                //fast replacing
                for (int i = 0; i < 256; i++) {
                    IBlockState target = target_blocks[i];
                    if (i < avg_height)
                        if (target == Blocks.AIR.getDefaultState())
                            target = biomeId.topBlock;
                    if (i == avg_height)
                        target = biomeId.topBlock;
                    if (i > avg_height)
                        target = Blocks.AIR.getDefaultState();
                    world.setBlockState(new BlockPos(pos.getX(), i, pos.getZ()), target, 2);
                }

                int new_height = get_height_blocks_only(world, pos);
                if (prev_height != new_height) {
                    DimensionProperties.proxylists.gethelper(props.getId()).register_height_change(pos);
                    DimensionProperties.proxylists.gethelper(props.getId()).add_position_to_queue(pos);
                } else {
                    DimensionProperties.proxylists.gethelper(props.getId()).check_next_border_chunk_fully_generated(cpos.x, cpos.z); // maybe this was the last border block in queue? if yes, its terrain is done!
                }
            } else
                DimensionProperties.proxylists.gethelper(props.getId()).check_next_border_chunk_fully_generated(cpos.x, cpos.z); // maybe this was the last border block in queue? if yes, its terrain is done!
        }
    }


    public static void do_decoration(World world, BlockPos pos, int dimId){

        Biome biomeId = world.getBiome(pos);
        int inchunkx = ((pos.getX() % 16) + 16) % 16;
        int inchunkz = ((pos.getZ() % 16) + 16) % 16;

        DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dimId);

        ChunkPos cpos = DimensionProperties.proxylists.gethelper(props.getId()).getChunkPosFromBlockPos(pos);



        int can_populate = DimensionProperties.proxylists.gethelper(props.getId()).can_populate(cpos.x, cpos.z);
        if (can_populate == -1){
            //because it can never be populated, it is considered "done with population"
            DimensionProperties.proxylists.gethelper(props.getId()).getChunkFromList(cpos.x, cpos.z).set_position_decorated(inchunkx, inchunkz);
        }
        if (can_populate == 1) {

            // we do some hacky tricks here to ensure that trees can fully generate
            // we shift the actual tree generation by 8 blocks so that it overlaps with the chunks next to it
            // can_populate() ensures that the chunks next to it are ready for decoration

            if (!DimensionProperties.proxylists.gethelper(props.getId()).getChunkFromList(cpos.x,cpos.z).fully_decorated[inchunkx][inchunkz]) {
                //System.out.println("decorate block");
                int treegen = biomeId.decorator.treesPerChunk;
                if (world.rand.nextInt(16 * 16) < treegen) {
                    biomeId.getRandomTreeFeature(world.rand).generate(world, world.rand, world.getHeight(pos.add(8, 0, 8)));
                }

                //some more flowers, grass, ....
                BlockPos yy = world.getHeight(pos);
                while (!world.getBlockState(yy.down()).isOpaqueCube() && yy.getY() > 0)
                    yy = yy.down();
                decorateBiome(world, yy, biomeId);

                DimensionProperties.proxylists.gethelper(props.getId()).getChunkFromList(cpos.x, cpos.z).set_position_decorated(inchunkx, inchunkz);
            }
        }
    }



    public static void terraform_biomes(World world, Biome biomeId, BlockPos pos, int dimId) {
        Biome old_biome = world.getBiome(pos);
        changeBiome(world,biomeId, old_biome, pos);
        decorate_simple(world, biomeId, old_biome, pos);

        DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(dimId);
        ChunkPos cpos = DimensionProperties.proxylists.gethelper(props.getId()).getChunkPosFromBlockPos(pos);
        chunkdata data = DimensionProperties.proxylists.gethelper(props.getId()).getChunkFromList(cpos.x, cpos.z);

        int inchunkx = ((pos.getX() % 16) + 16) % 16;
        int inchunkz = ((pos.getZ() % 16) + 16) % 16;
        if (data == null){
            DimensionProperties.proxylists.gethelper(props.getId()).generate_new_chunkdata(new ChunkPos(cpos.x, cpos.z));
            data = DimensionProperties.proxylists.gethelper(props.getId()).getChunkFromList(cpos.x, cpos.z);
        }
        data.set_position_biomechanged(inchunkx,inchunkz);

        Chunk chunk = world.getChunkFromBlockCoords(pos);
        PacketHandler.sendToNearby(new PacketBiomeIDChange(chunk, world, new HashedBlockPosition(pos)), world.provider.getDimension(), pos, 1024);

    }

    public static void terraform(World world, Biome biomeId, BlockPos pos, boolean was_biome_remote, int dimId) {
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        if (biomeId == null)return;

        Biome old_biome = world.getBiome(pos);


        //for biome remote use, only change top block and do simple decoration
        if(was_biome_remote) {
            changeBiome(world,biomeId, old_biome, pos); // this should not be needed in heavy terraforming because it should have already been done before
            decorate_simple(world, biomeId, old_biome, pos);
            PacketHandler.sendToNearby(new PacketBiomeIDChange(chunk, world, new HashedBlockPosition(pos)), world.provider.getDimension(), pos, 1024);
        }

        if (!was_biome_remote) { // heavy terraforming here...
            do_heavy_terraforming(world, biomeId,old_biome,pos,dimId);
        }




    }

    private static void spawn_decoration_block(World world, BlockPos pos, IBlockState state, int p){
        if (p < 0){
            System.out.println("p should not be negative!!");
            return;
        }
        if (world.rand.nextInt(p) == 0) {
            if (world.isAirBlock(pos) && state.getBlock().canPlaceBlockAt(world, pos)) {
                world.setBlockState(pos, state);
            }
        }
    }


    private static void decorateBiome(World world, BlockPos pos, Biome biome) {
        Random rand = world.rand;
        BiomeDecorator decorator = biome.decorator;
        int chunksize = 16 * 16;

        // if block above topblock == grass/flower..., remove it
        Block block_above_biome_topblock = world.getBlockState(pos).getBlock();
        if (block_above_biome_topblock instanceof BlockTallGrass ||
                block_above_biome_topblock instanceof BlockFlower ||
                block_above_biome_topblock instanceof BlockCactus ||
                block_above_biome_topblock instanceof BlockMushroom
        )
            world.setBlockState(pos, Blocks.AIR.getDefaultState());


        IBlockState deadbush = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.DEAD_BUSH);
        IBlockState grass = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);
        IBlockState fern = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.FERN);


        if (decorator.flowersPerChunk > 0)
            spawn_decoration_block(world, pos, biome.pickRandomFlower(rand, pos).getBlockType().getBlock().getDefaultState(), chunksize / decorator.flowersPerChunk);

        if (decorator.deadBushPerChunk > 0)
            spawn_decoration_block(world, pos, deadbush, chunksize / decorator.deadBushPerChunk);

        if (decorator.grassPerChunk > 0) {
            int grass_rate = chunksize / decorator.grassPerChunk;
            // increase fern chance in cold / forest
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.COLD) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.FOREST)) {
                spawn_decoration_block(world, pos, fern, grass_rate / 2);
                spawn_decoration_block(world, pos, grass, grass_rate / 2);
            } else {
                spawn_decoration_block(world, pos, fern, (int) (grass_rate / 0.2));
                spawn_decoration_block(world, pos, grass, (int) (grass_rate / 0.8));
            }
        }

        if (decorator.mushroomsPerChunk > 0) {
            IBlockState mushroom_b = Blocks.BROWN_MUSHROOM.getDefaultState();
            IBlockState mushroom_r = Blocks.RED_MUSHROOM.getDefaultState();
            spawn_decoration_block(world, pos, mushroom_b, chunksize / decorator.mushroomsPerChunk / 2);
            spawn_decoration_block(world, pos, mushroom_r, chunksize / decorator.mushroomsPerChunk / 2);
        }
        if (decorator.cactiPerChunk > 0) {
            IBlockState cactus = Blocks.CACTUS.getDefaultState();
            spawn_decoration_block(world, pos, cactus, chunksize / decorator.cactiPerChunk);
        }
    }

}


