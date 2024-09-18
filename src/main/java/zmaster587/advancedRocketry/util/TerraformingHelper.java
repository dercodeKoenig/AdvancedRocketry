package zmaster587.advancedRocketry.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraftforge.common.BiomeManager;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.world.ChunkManagerPlanet;
import zmaster587.advancedRocketry.world.ChunkProviderPlanet;

import java.util.*;

import static org.apache.commons.lang3.RandomUtils.nextInt;

enum TerraformingType{
    ALLOWED, BORDER, PROTECTED
}

public class TerraformingHelper {
    public BiomeProvider chunkMgrTerraformed;
    public int dimId;
    public List<BiomeManager.BiomeEntry> biomeList;
    public World world;
    public ChunkProviderPlanet generator;
    private DimensionProperties props;

    private Map<ChunkPos, chunkdata> chunkDataMap = new HashMap<>();


    // A block is placed in queue if (OR)
    // - Initial block placements - both only set blocks if the terrain at the BlockPos is not fully generated in the "real" world
    //      - The TerraformingHelper is created ( causes all loaded chunks to place their blocks here, from DimProps )
    //      - A new chunk is loaded ( called from PlanetEventHandler )
    // - After the BiomeHandler has made a change at a BlockPos
    //      - For type ALLOWED: if the terrain heigt does not match the target, re-add the block to the queue
    //      - For type BORDER: if BiomeHandler has made a change of the height at BlockPos re-add to queue
    //      - For type BORDER: if a type.ALLOWED has changed its height it will add blocks next to it (if type==BORDER) to the queue
    //      - For type PROTECTED: the block is never re-added to queue because it will change its biome only once
    // - When a protecting Block has been removed and chunk status is re-calculated
    //   add every chunk that was PROTECTED and no longer is PROTECTED to the queue for terraforming.
    //   doesn't matter if it is type ALLOWED or type BORDER
    private List<Vec3i> terraformingqueue;
    private List<Vec3i> decorationqueue;
    private List<Vec3i> biomechangingqueue;

    int safe_zone_radius = 3; // radius for protected zone
    int border_zone = 3; // border zone size

    public boolean has_blocks_in_tf_queue(){
        return !terraformingqueue.isEmpty();
    }
    public boolean has_blocks_in_dec_queue(){
        return !decorationqueue.isEmpty();
    }

    public TerraformingHelper(int dimension, List<BiomeManager.BiomeEntry> biomes, HashSet<ChunkPos> generated_chunks, HashSet<ChunkPos> biomechanged_chunks){
        this.dimId = dimension;
        this.props = DimensionManager.getInstance().getDimensionProperties(dimension);
        this.biomeList = biomes;
        this.world = net.minecraftforge.common.DimensionManager.getWorld(dimId);
        this.chunkMgrTerraformed = new ChunkManagerPlanet(world, world.getWorldInfo().getGeneratorOptions(), biomeList);
        this.terraformingqueue = new ArrayList<>();
        this.biomechangingqueue = new ArrayList<>();
        this.decorationqueue = new ArrayList<>();
        chunkDataMap = new HashMap<>();
        generator = new ChunkProviderPlanet(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled(), world.getWorldInfo().getGeneratorOptions());

        System.out.println("register generated chunks...");
        for (ChunkPos i:generated_chunks){
            chunkdata data = new chunkdata(i.x,i.z,null, world, this);
            data.chunk_fully_generated = true;
            chunkDataMap.put(new ChunkPos(data.x,data.z), data);
        }
        System.out.println("register biome changed chunks...");
        for (ChunkPos i:biomechanged_chunks){

            generate_new_chunkdata(new ChunkPos(i.x, i.z));
            chunkdata data = getChunkFromList(i.x,i.z);
            data.chunk_fully_biomechanged = true;
        }

        System.out.println("recalculate chunk types...");
        recalculate_chunk_status();
        System.out.println("ok!");
    }

    //0 = no
    //1 = yes
    // -1 = never (if it includes a type.PROTECTED chunk
    public int can_populate(int x, int z){
        chunkdata currentchunk = getChunkFromList(x,z);
        chunkdata currentchunkx1 = getChunkFromList(x+1,z);
        chunkdata currentchunkz1 = getChunkFromList(x,z+1);
        chunkdata currentchunkx1z1 = getChunkFromList(x+1,z+1);

        if (currentchunk != null && currentchunkz1 != null && currentchunkx1 != null && currentchunkx1z1 != null){

            if (currentchunk.type == TerraformingType.PROTECTED || currentchunkz1.type == TerraformingType.PROTECTED ||currentchunkx1.type == TerraformingType.PROTECTED ||currentchunkx1z1.type == TerraformingType.PROTECTED)
                return -1; // chunks contain a protected chunk

            if (currentchunkz1.terrain_fully_generated && currentchunkx1.terrain_fully_generated && currentchunkx1z1.terrain_fully_generated && currentchunk.terrain_fully_generated) {
                currentchunk.populate_chunk_if_not_already_done();
                return 1;
            }
        }
        return 0;
    }

    /*
    used to add BORDER type block positions to queue for updating again
     */
    public void register_height_change_actual(BlockPos pos){
        ChunkPos cpos = getChunkPosFromBlockPos(pos);
        chunkdata data= getChunkFromList(cpos.x,cpos.z);
        if (data !=null  && data.type == TerraformingType.BORDER) {
            add_position_to_queue(pos);
            //System.out.println("add position to queue again");
        }
    }
    public void register_height_change(BlockPos pos){
        register_height_change_actual(pos.add(1,0,0));
        register_height_change_actual(pos.add(-1,0,0));
        register_height_change_actual(pos.add(0,0,1));
        register_height_change_actual(pos.add(0,0,-1));
    }


    /*
     When a chunk is fully terrain-generated it will call this method to update border chunks next to it.
     A Border chunk is considered fully generated when every type.ALLOWED chunks next to it are fully generated.
     This is because a fully terrain generated chunk will no longer change its heightmap so it will not modify the heightmap of the border chunk next to it
     */
    public synchronized void check_next_border_chunk_fully_generated(int px, int pz) {

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                chunkdata data = getChunkFromList(px + x, pz + z);
                if (data == null)continue;
                if (data != null && !data.terrain_fully_generated && data.type == TerraformingType.BORDER) {
                    int chunkposxlow = data.x * 16;
                    int chunkposzlow = data.z * 16;
                    int chunkposxhigh = chunkposxlow + 16;
                    int chunkposzhigh = chunkposzlow + 16;


                    for(Vec3i p : terraformingqueue){
                        if (p.getX() >= chunkposxlow && p.getX() < chunkposxhigh) {
                            if (p.getZ() >= chunkposzlow && p.getZ() < chunkposzhigh) {
                                return;
                            }
                        }
                    }


                    for (int x2 = -1; x2 <= 1; x2++) {
                        for (int z2 = -1; z2 <= 1; z2++) {
                            chunkdata data2 = getChunkFromList(px + x + x2, pz + z + z2);
                            if (data2 == null) return;
                            if (data2.type == TerraformingType.ALLOWED) {
                                if (!data2.terrain_fully_generated) {
                                    return;
                                }
                            }
                        }
                    }
                    System.out.println("border chunk terrain fully generated");
                    data.terrain_fully_generated = true;
                    data.blockStates = null; // no longer needed, gc should collect them now - actually, these are never needed but who cares...
                    check_next_border_chunk_fully_generated(data.x, data.z); // update border chunks next to this one to check if they can decorate
                    check_can_decorate(data.x, data.z);

                }
            }
        }
    }
    public void check_can_decorate(int px, int pz){
        // for every chunk next to this one, check if it can decorate (except for if it is already decorated)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (getChunkFromList(px+x,pz+z) != null && !getChunkFromList(px+x,pz+z).chunk_fully_generated){
                    if (can_populate(px+x,pz+z) != 0){
                        //re-add all position to queue for decoration
                        System.out.println("chunk can populate now: "+(px+x)+":"+(pz+z));
                        for (int bx = 0; bx < 16; bx++) {
                            for (int bz = 0; bz < 16; bz++) {
                                add_position_to_decoration_queue(new BlockPos((px+x)*16+bx, 0, (pz+z)*16+bz));
                            }
                        }
                    }
                }
            }
        }

    }
    public ChunkPos getChunkPosFromBlockPos(BlockPos pos) {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }



    public TerraformingType get_chunk_type(int x, int z) {

        TerraformingType type = TerraformingType.ALLOWED;
        for (BlockPos i : DimensionProperties.proxylists.getProtectingBlocksForDimension(props.getId())) {
            //System.out.println("found protecting block at "+i.getX()+":"+i.getY()+":"+i.getZ());
            ChunkPos cpos = getChunkPosFromBlockPos(i);
            int dx = cpos.x - x;
            int dz = cpos.z - z;
            if (Math.abs(dx) <= safe_zone_radius && Math.abs(dz) <= safe_zone_radius)
                return TerraformingType.PROTECTED;
            else if (Math.abs(dx) <= safe_zone_radius + border_zone && Math.abs(dz) <= safe_zone_radius + border_zone) {
                type = TerraformingType.BORDER;
            }
        }
        return type;
    }

    public void recalculate_chunk_status() {
        Iterator<Map.Entry<ChunkPos, chunkdata>> iterator = chunkDataMap.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<ChunkPos, chunkdata> entry = iterator.next();

            chunkdata data = entry.getValue();

            TerraformingType prevtype = data.type;

            data.type = get_chunk_type(data.x, data.z);

            if (prevtype != TerraformingType.ALLOWED)
                if (data.type == TerraformingType.ALLOWED || data.type == TerraformingType.BORDER)
                    if (data.terrain_fully_generated)
                        iterator.remove();

        }
    }





    public chunkdata getChunkFromList(int x, int z){
        ChunkPos key = new ChunkPos(x,z);
        return chunkDataMap.get(key);
    }



    public synchronized void add_position_to_queue(BlockPos p){
        //System.out.println("add position: "+p.getX()+":"+p.getZ());
        if (p == null){
            System.out.print("ERROR POSITION IS NULL");
            return;
        }

        //if on protected chunk, instantly add to decoration queue as it will not work protected chunks
        ChunkPos cp = getChunkPosFromBlockPos(p);
        if(get_chunk_type(cp.x,cp.z) == TerraformingType.PROTECTED){
            int inchunkx = ((p.getX() % 16) + 16) % 16;
            int inchunkz = ((p.getZ() % 16) + 16) % 16;
            getChunkFromList(cp.x, cp.z).set_position_fully_generated(inchunkx, inchunkz);
        }else {
            terraformingqueue.add(new Vec3i(p.getX(), p.getY(), p.getZ()));
        }
    }

    public synchronized void add_position_to_biomechanging_queue(BlockPos p){
        //System.out.println("add position: "+p.getX()+":"+p.getZ());
        if (p == null){
            System.out.print("ERROR POSITION IS NULL");
            return;
        }
        biomechangingqueue.add(new Vec3i(p.getX(),p.getY(),p.getZ()));
    }

    public synchronized void add_position_to_decoration_queue(BlockPos p){
        //System.out.println("add position: "+p.getX()+":"+p.getZ());
        if (p == null){
            System.out.print("ERROR POSITION IS NULL");
            return;
        }
        int insertionIndex = 0;
        if (decorationqueue.size() > 0) {
            insertionIndex = new Random().nextInt(Math.min(decorationqueue.size(), 1000));
        }
        decorationqueue.add(insertionIndex, new Vec3i(p.getX(),p.getY(),p.getZ()));
    }


    public synchronized BlockPos get_next_position(boolean random){
        if (terraformingqueue.isEmpty())
            return null;
        int index = 0;
        if (random)
            index = nextInt(0,terraformingqueue.size());

        Vec3i pos = terraformingqueue.remove(index);
        return new BlockPos(pos);
    }
    public synchronized BlockPos get_next_position_biomechanging(boolean random) {
        if (biomechangingqueue.isEmpty()) {
            return null;
        }

        int index;
        if (random) {
            index = new Random().nextInt(Math.min(8192, biomechangingqueue.size()));
        } else {
            index = 0; // Default to the first element if not random mode
        }

        Vec3i pos = biomechangingqueue.remove(index);
        return new BlockPos(pos);
    }

    public synchronized BlockPos get_next_position_decoration(boolean random){
        if (decorationqueue.isEmpty())
            return null;
        int index = 0;
        if (random)
            index = nextInt(0,decorationqueue.size());

        Vec3i pos = decorationqueue.remove(index);
        return new BlockPos(pos);
    }

    public void set_chunk_biomechanged(ChunkPos pos){ // mark a chunk ready for terraforming
        props.add_chunk_to_terraforming_list_but_this_time_real_terraforming_and_not_biomechanging(pos);
        DimensionProperties.proxylists.getChunksFullyBiomeChanged(props.getId()).add(pos); // set it fully biome changed

    }
    public void generate_new_chunkdata(ChunkPos cpos){
        chunkdata data;



        data = new chunkdata(cpos.x,cpos.z, null, world, this);
        data.type = get_chunk_type(data.x,data.z);
        chunkDataMap.put(new ChunkPos(data.x,data.z),data);
    }

    public IBlockState[] getBlocksAt(int x, int z){
        ChunkPos cpos = getChunkPosFromBlockPos(new BlockPos(x,0,z));
        chunkdata data = getChunkFromList(cpos.x,cpos.z);
        if (data == null){
            System.out.println("generate new chunk: "+cpos.x+":"+cpos.z);
            generate_new_chunkdata(new ChunkPos(cpos.x, cpos.z));
            data = getChunkFromList(cpos.x,cpos.z);
        }
        if (data.blockStates == null){
            System.out.println("generate new blockstates: "+cpos.x+":"+cpos.z);
            ChunkPrimer primer = generator.getChunkPrimer(cpos.x, cpos.z, chunkMgrTerraformed);
            IBlockState[][][] blockStates = new IBlockState[16][16][256];
            for (int px = 0; px < 16; px++) {
                for (int pz = 0; pz < 16; pz++) {
                    for (int py = 0; py < 256; py++) {
                        blockStates[px][pz][py] = primer.getBlockState(px,py,pz);
                    }
                }
            }
            data.blockStates = blockStates;
        }
        int chunkx = ((x % 16) + 16) % 16;
        int chunkz = ((z % 16) + 16) % 16;

        if (data.blockStates == null)
            return null;

        return data.blockStates[chunkx][chunkz];
    }


    public void setChunkFullyGenerated(int x, int z) {
        getChunkFromList(x,z).chunk_fully_generated = true;
        DimensionProperties.proxylists.getChunksFullyTerraformed(props.getId()).add(new ChunkPos(x,z));
    }
}
