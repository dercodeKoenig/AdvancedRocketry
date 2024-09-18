package zmaster587.advancedRocketry.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class chunkdata {
    public int x;
    public int z;
    public TerraformingType type;


    // chunk can be populated if itself and the 3 chunks with (x+1), (z+1) and (x+1,z+1) are fully generated in the world


    // if a chunks terrain is fully generated and does not need any future updates - causes it to set the IBlockState[][][] to null to free memory
    // if the terrain is fully generated it allows for calling populate()
    public boolean terrain_fully_generated;

    // will be set by BiomeHandler after calling the populate method, makes the chunk fully generated and it does not need any future updates
    // as long as the atmosphere does not change, chunks that are fully generated will not register their blocks in the terraforming queue
    public boolean chunk_fully_generated;

    public boolean chunk_populated;

    public boolean chunk_fully_biomechanged;

    // if the y values of new chunk and chunk in world match, terrain at this position is considered fully generated
    public boolean[][] fully_generated;

    // if all positions have been decorated
    public boolean[][] fully_decorated;

    // if all positions have been biomechanged
    public boolean[][] fully_biomechanged;


    public IBlockState[][][] blockStates;

    World world; // used to call populate after decoration is finished
    TerraformingHelper helper;

    public chunkdata(int x, int z, IBlockState[][][] blockStates, World world, TerraformingHelper helper) {
        this.x = x;
        this.world = world;
        this.helper = helper;
        this.z = z;
        this.blockStates = blockStates;
        this.type = TerraformingType.ALLOWED;
        chunk_fully_generated = false;
        terrain_fully_generated = false;
        chunk_populated = false;
        fully_generated = new boolean[16][16];
        fully_decorated = new boolean[16][16];
        fully_biomechanged = new boolean[16][16];
    }

    /*
    sets if a blockpos is fully generated and computed if the chunk is fully generated
    called from BiomeHandler
     */
    public void set_position_fully_generated(int x, int z){
        fully_generated[x][z] = true;
        boolean all_generated = true;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (fully_generated[i][j] == false) {
                    all_generated = false;
                }
            }
        }
        if (all_generated){
            System.out.println("chunk fully generated: "+this.x+":"+this.z);
            terrain_fully_generated = true;
            this.blockStates = null; // no longer needed, gc should collect them now
            helper.check_next_border_chunk_fully_generated(this.x,this.z); // update border chunks next to this one to check if they can decorate
            helper.check_can_decorate(this.x,this.z);

        }
    }


    public void set_position_biomechanged(int x, int z){
        fully_biomechanged[x][z] = true;
        boolean all_generated = true;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (fully_biomechanged[i][j] == false) {
                    all_generated = false;
                }
            }
        }
        if (all_generated){
            System.out.println("chunk fully biomechanged: "+this.x+":"+this.z);
            chunk_fully_biomechanged = true;

            /// add chunk to terraforming queue now
            helper.set_chunk_biomechanged(new ChunkPos(this.x, this.z));

        }
    }

    public void populate_chunk_if_not_already_done() {
        // populate uses the biome at blockpos 0,0, in the chunk x+1,z+1, that's why we need the chunks next to it generated
        if (!chunk_fully_generated && !chunk_populated) {

            //2 times i want more population!
            world.provider.createChunkGenerator().populate(this.x, this.z);
            world.provider.createChunkGenerator().populate(this.x, this.z);

            System.out.println("populate chunk " + this.x + ":" + this.z);
            chunk_populated = true;
            //make a biome lasers here
            // no - looks bad
            //for (int i = 0; i < 32; i++) {
            //    int bx = world.rand.nextInt(16);
            //    int bz = world.rand.nextInt(16);
            //    BlockPos pos = new BlockPos(this.x*16+bx,0,this.z*16+bz);
            //    PacketHandler.sendToNearby(new PacketBiomeIDChange(world.getChunkFromChunkCoords(this.x,this.z), world, new HashedBlockPosition(pos)), world.provider.getDimension(), pos, 1024);
            //}
        }
    }

    public void set_position_decorated(int x, int z){

        fully_decorated[x][z] = true;
        boolean all_decorated = true;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (fully_decorated[i][j] == false) {
                    all_decorated = false;
                }
            }
        }
        if (all_decorated){

            helper.setChunkFullyGenerated(this.x,this.z);
        }


    }

}
