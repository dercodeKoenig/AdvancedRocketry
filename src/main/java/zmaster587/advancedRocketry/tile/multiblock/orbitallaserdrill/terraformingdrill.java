package zmaster587.advancedRocketry.tile.multiblock.orbitallaserdrill;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.common.ForgeChunkManager;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.entity.EntityLaserNode;
import zmaster587.advancedRocketry.util.BiomeHandler;
import zmaster587.advancedRocketry.util.TerraformingHelper;
import zmaster587.advancedRocketry.world.ChunkManagerPlanet;

/**
 * This drill is used if the laserDrillPlanet config option is disabled. It simply conjures ores from nowhere
 */
class terraformingdrill extends AbstractDrill{


    private ForgeChunkManager.Ticket ticketLaser;
    private ChunkPos last_worked_chunk = null;
    private EntityLaserNode laser;

    terraformingdrill(){

    }
    public TerraformingHelper get_my_helper() {
     return get_my_helper(null);
    }
    public TerraformingHelper get_my_helper(World w){
        if (w == null)
            w = laser.world;

        if (!DimensionProperties.proxylists.isinitialized(w.provider.getDimension())){
            DimensionProperties.proxylists.initdim(w.provider.getDimension());
        }

        TerraformingHelper t = DimensionProperties.proxylists.gethelper(w.provider.getDimension());

        if (t == null) {
            DimensionManager.getInstance().getDimensionProperties(w.provider.getDimension()).load_terraforming_helper(false);
            t = DimensionProperties.proxylists.gethelper(w.provider.getDimension());
        }
        return t;
    }

    /**
     * Performs a single drilling operation
     *
     * @return The ItemStacks produced by this tick of drilling
     */
    ItemStack[] performOperation() {
        try {


            TerraformingHelper t = get_my_helper();
            BiomeProvider chunkmgr = t.chunkMgrTerraformed;
            BlockPos next_block_pos = null;
            Vec3d laserpos = null;
            for (int i = 0; i<6;i++) {
                next_block_pos = t.get_next_position(false);

                if (next_block_pos == null) {
                    //System.out.println("Queue empty - returning");
                    return null;
                }

                // blocks are usually added per chunk so it may be efficient to keep it loaded
                // one chunk has to be loaded anyway to prevent dim unloading

                //System.out.println("terraform heightmap at "+next_block_pos.getX()+":"+next_block_pos.getZ());



                ChunkPos currentChunk = t.getChunkPosFromBlockPos(next_block_pos);
                if (last_worked_chunk != null) {
                    if (!currentChunk.equals(last_worked_chunk)) {
                        releaseticket();
                        //just load chunk 0 0 to keep the dimension loaded
                        //System.out.println("request ticket at chunk " + currentChunk.x + ":" + currentChunk.z);
                        ticketLaser = ForgeChunkManager.requestTicket(AdvancedRocketry.instance, laser.world, ForgeChunkManager.Type.NORMAL);
                        if (ticketLaser != null) {
                            ForgeChunkManager.forceChunk(ticketLaser, new ChunkPos(currentChunk.x, currentChunk.z));
                        }
                    }
                }
                last_worked_chunk = currentChunk;


                BiomeHandler.terraform(laser.world, ((ChunkManagerPlanet) chunkmgr).getBiomeGenAt(next_block_pos.getX(), next_block_pos.getZ()), next_block_pos, false, laser.world.provider.getDimension());

                //because it syncs entity position not every tick, just place it in the middle of the chunk it is currently working in
                 laserpos = new Vec3d(currentChunk.x*16+8, laser.world.getHeight(currentChunk.x*16+8, currentChunk.z*16+8), currentChunk.z*16+8);

            }

            laser.setPositionAndUpdate(laserpos.x,laserpos.y,laserpos.z);


            //} catch (NullPointerException e) {
            //    e.printStackTrace();
        } catch (NoClassDefFoundError e){
            e.printStackTrace(); //WTF
        }
        return null;
    }

    boolean activate(World world, int x, int z) {



            if (laser == null) {
                laser = new EntityLaserNode(world, x, 0, z);
                laser.markValid();
                laser.forceSpawn = true;
                world.spawnEntity(laser);
            }
            return true;

    }

    void releaseticket(){
        if (ticketLaser != null)
            ForgeChunkManager.releaseTicket(ticketLaser);
    }

    void deactivate() {
        if (laser != null) {
            laser.setDead();
            laser = null;
        }

        releaseticket();



    }

    boolean isFinished() {
        return laser==null || !get_my_helper().has_blocks_in_tf_queue();
    }

    boolean needsRestart() {
        return this.laser == null;
    }
}
