/* Temporarily stores tile/blocks to move a block of them
 *
 *
 */

package zmaster587.advancedRocketry.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.*;
import zmaster587.advancedRocketry.api.fuel.FuelRegistry;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.api.stations.IStorageChunk;
import zmaster587.advancedRocketry.block.*;
import zmaster587.advancedRocketry.item.ItemPackedStructure;
import zmaster587.advancedRocketry.tile.TileBrokenPart;
import zmaster587.advancedRocketry.tile.TileGuidanceComputer;
import zmaster587.advancedRocketry.tile.hatch.TileSatelliteHatch;
import zmaster587.advancedRocketry.world.util.WorldDummy;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.Vector3F;
import zmaster587.libVulpes.util.ZUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class StorageChunk implements IBlockAccess, IStorageChunk, IWeighted, IBreakable {

    public Chunk chunk;
    public WorldDummy world;
    public boolean finalized = false; // Make sure we are ready to render
    private Block[][][] blocks;
    public int sizeX, sizeY, sizeZ;
    private short[][][] metas;
    private Map<BlockPos, TileEntity> pos2te = new HashMap<>();
    private ArrayList<TileEntity> tileEntities;
    //To store inventories (All inventories)
    private ArrayList<TileEntity> inventoryTiles;
    private ArrayList<TileEntity> liquidTiles;
    private Entity entity;
    private float weight;
    private boolean hasServiceMonitor;

    public Block[][][] getblocks() {
        return blocks;
    }

    public boolean hasServiceMonitor() {
        return hasServiceMonitor;
    }

    public StorageChunk() {
        sizeX = 0;
        sizeY = 0;
        sizeZ = 0;
        tileEntities = new ArrayList<>();
        inventoryTiles = new ArrayList<>();
        liquidTiles = new ArrayList<>();

        world = new WorldDummy(AdvancedRocketry.proxy.getProfiler(), this);
        world.init();
        this.chunk = new Chunk(world, 0, 0);
    }

    protected StorageChunk(int xSize, int ySize, int zSize) {
        blocks = new Block[xSize][ySize][zSize];
        metas = new short[xSize][ySize][zSize];

        sizeX = xSize;
        sizeY = ySize;
        sizeZ = zSize;

        tileEntities = new ArrayList<>();
        inventoryTiles = new ArrayList<>();
        liquidTiles = new ArrayList<>();

        world = new WorldDummy(AdvancedRocketry.proxy.getProfiler(), this);
        world.init();
        this.chunk = new Chunk(world, 0, 0);
    }

    private static boolean isInventoryBlock(TileEntity tile) {
        return tile instanceof IInventory || tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP) && !(tile instanceof TileGuidanceComputer);
    }

    private static boolean isLiquidContainerBlock(TileEntity tile) {
        return tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public float getWeight() {
        return this.weight;
    }

    public float recalculateWeight() {
        this.weight = 0;

        // plain blocks
        for (int x = 0; x < this.sizeX; x++) {
            for (int y = 0; y < this.sizeY; y++) {
                for (int z = 0; z < this.sizeZ; z++) {
                    Block block = this.blocks[x][y][z];
                    if (block != null) {
                        this.weight += WeightEngine.INSTANCE.getWeight(null, block);
                    }
                }
            }
        }

        // TEs
        for (TileEntity te : this.tileEntities) {
            this.weight += WeightEngine.INSTANCE.getTEWeight(te);

            if (te instanceof TileSatelliteHatch) {
                TileSatelliteHatch hatch = (TileSatelliteHatch) te;
                if (hatch.getSatellite() != null) {
                    weight += hatch.getSatellite().getProperties().getWeight();
                } else if (hatch.getStackInSlot(0).getItem() instanceof ItemPackedStructure) {
                    ItemPackedStructure struct = (ItemPackedStructure) hatch.getStackInSlot(0).getItem();
                    weight += struct.getStructure(hatch.getStackInSlot(0)).getWeight();
                }
            }
        }
        return this.weight;
    }

    public void recalculateStats(StatsRocket stats) {
        int thrustMonopropellant = 0;
        int thrustBipropellant = 0;
        int thrustNuclearNozzleLimit = 0;
        int thrustNuclearReactorLimit = 0;
        int thrustNuclearTotalLimit = 0;
        int monopropellantfuelUse = 0;
        int bipropellantfuelUse = 0;
        int nuclearWorkingFluidUseMax = 0;
        int fuelCapacityMonopropellant = 0;
        int fuelCapacityBipropellant = 0;
        int fuelCapacityOxidizer = 0;
        int fuelCapacityNuclearWorkingFluid = 0;

        float drillPower = 0f;
        //stats.reset_no_fuel();
        stats.reset_no_fuel();// Oh Quarter... you can not keep adding engine and seat locations every launch

        float weight = 0;

        for (int yCurr = 0; yCurr <= this.sizeY; yCurr++) {
            for (int xCurr = 0; xCurr <= this.sizeX; xCurr++) {
                for (int zCurr = 0; zCurr <= this.sizeZ; zCurr++) {
                    BlockPos currBlockPos = new BlockPos(xCurr, yCurr, zCurr);
                    BlockPos abovePos = new BlockPos(xCurr, yCurr + 1, zCurr);
                    BlockPos belowPos = new BlockPos(xCurr, yCurr - 1, zCurr);

                    if (this.getBlockState(currBlockPos).getBlock() != Blocks.AIR) {
                        IBlockState state = world.getBlockState(currBlockPos);
                        Block block = state.getBlock();

                        if (ARConfiguration.getCurrentConfig().advancedWeightSystem) {
                            weight += WeightEngine.INSTANCE.getWeight(world, currBlockPos);
                        } else {
                            weight += 1;
                        }

                        //If rocketEngine increaseThrust
                        if (block instanceof IRocketEngine && (world.getBlockState(belowPos).getBlock().isAir(world.getBlockState(belowPos), world, belowPos) || world.getBlockState(belowPos).getBlock() instanceof BlockLandingPad || world.getBlockState(belowPos).getBlock() == AdvancedRocketryBlocks.blockLaunchpad)) {
                            if (block instanceof BlockNuclearRocketMotor) {
                                nuclearWorkingFluidUseMax += ((IRocketEngine) block).getFuelConsumptionRate(world, xCurr, yCurr, zCurr);
                                thrustNuclearNozzleLimit += ((IRocketEngine) block).getThrust(world, currBlockPos);
                            } else if (block instanceof BlockBipropellantRocketMotor) {
                                bipropellantfuelUse += ((IRocketEngine) block).getFuelConsumptionRate(world, xCurr, yCurr, zCurr);
                                thrustBipropellant += ((IRocketEngine) block).getThrust(world, currBlockPos);
                            } else if (block instanceof BlockRocketMotor) {
                                monopropellantfuelUse += ((IRocketEngine) block).getFuelConsumptionRate(world, xCurr, yCurr, zCurr);
                                thrustMonopropellant += ((IRocketEngine) block).getThrust(world, currBlockPos);
                            }
                            stats.addEngineLocation(xCurr - (float) this.sizeX /2+0.5f, yCurr+0.5f, zCurr- (float) this.sizeZ /2+0.5f);
                        }

                        if (block instanceof IFuelTank) {
                             if (block instanceof BlockBipropellantFuelTank) {
                                fuelCapacityBipropellant += (((IFuelTank) block).getMaxFill(world, currBlockPos, state) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                            } else if (block instanceof BlockOxidizerFuelTank) {
                                fuelCapacityOxidizer += (((IFuelTank) block).getMaxFill(world, currBlockPos, state) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                            } else if (block instanceof BlockNuclearFuelTank) {
                                fuelCapacityNuclearWorkingFluid += (((IFuelTank) block).getMaxFill(world, currBlockPos, state) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                            }else if (block instanceof BlockFuelTank) {
                                fuelCapacityMonopropellant += (((IFuelTank) block).getMaxFill(world, currBlockPos, state) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                            }
                        }

                        if (block instanceof IRocketNuclearCore && ((world.getBlockState(belowPos).getBlock() instanceof IRocketNuclearCore) || (world.getBlockState(belowPos).getBlock() instanceof IRocketEngine))) {
                            thrustNuclearReactorLimit += ((IRocketNuclearCore) block).getMaxThrust(world, currBlockPos);
                        }

                        if (block instanceof BlockSeat && world.getBlockState(abovePos).getBlock().isPassable(world, abovePos)) {
                            stats.addPassengerSeat((int) (xCurr - (float) this.sizeX / 2 + 0.5f), yCurr, (int) (zCurr - (float) this.sizeZ / 2 + 0.5f));
                        }

                        if (block instanceof IMiningDrill) {
                            drillPower += ((IMiningDrill) block).getMiningSpeed(world, currBlockPos);
                        }

                        if (block.getUnlocalizedName().contains("servicemonitor")) {
                            hasServiceMonitor = true;
                        }

                        TileEntity tile = world.getTileEntity(currBlockPos);
                        if (tile instanceof TileSatelliteHatch) {
                            if (ARConfiguration.getCurrentConfig().advancedWeightSystem) {
                                TileSatelliteHatch hatch = (TileSatelliteHatch) tile;
                                if (hatch.getSatellite() != null) {
                                    weight += hatch.getSatellite().getProperties().getWeight();
                                } else if (hatch.getStackInSlot(0).getItem() instanceof ItemPackedStructure) {
                                    ItemPackedStructure struct = (ItemPackedStructure) hatch.getStackInSlot(0).getItem();
                                    weight += struct.getStructure(hatch.getStackInSlot(0)).getWeight();
                                }
                            }
                        }
                    }
                }
            }
        }

        int nuclearWorkingFluidUse = 0;
        if (thrustNuclearNozzleLimit > 0) {
            //Only run the number of engines our cores can support - we can't throttle these effectively because they're small, so they shut off if they don't get full power
            thrustNuclearTotalLimit = Math.min(thrustNuclearNozzleLimit, thrustNuclearReactorLimit);
            nuclearWorkingFluidUse = (int) (nuclearWorkingFluidUseMax * (thrustNuclearTotalLimit / (float) thrustNuclearNozzleLimit));
            thrustNuclearTotalLimit = (nuclearWorkingFluidUse * thrustNuclearNozzleLimit) / nuclearWorkingFluidUseMax;
        }

        //Set fuel stats
        //Thrust depending on rocket type
        stats.setBaseFuelRate(FuelRegistry.FuelType.LIQUID_MONOPROPELLANT, monopropellantfuelUse);
        stats.setBaseFuelRate(FuelRegistry.FuelType.LIQUID_BIPROPELLANT, bipropellantfuelUse);
        stats.setBaseFuelRate(FuelRegistry.FuelType.LIQUID_OXIDIZER, bipropellantfuelUse);
        stats.setBaseFuelRate(FuelRegistry.FuelType.NUCLEAR_WORKING_FLUID, nuclearWorkingFluidUse);
        //Fuel storage depending on rocket type
        stats.setFuelCapacity(FuelRegistry.FuelType.LIQUID_MONOPROPELLANT, fuelCapacityMonopropellant);
        stats.setFuelCapacity(FuelRegistry.FuelType.LIQUID_BIPROPELLANT, fuelCapacityBipropellant);
        stats.setFuelCapacity(FuelRegistry.FuelType.LIQUID_OXIDIZER, fuelCapacityOxidizer);
        stats.setFuelCapacity(FuelRegistry.FuelType.NUCLEAR_WORKING_FLUID, fuelCapacityNuclearWorkingFluid);

        //Non-fuel stats
        stats.setWeight(weight);
        stats.setThrust(Math.max(Math.max(thrustMonopropellant, thrustBipropellant), thrustNuclearTotalLimit));
        stats.setDrillingPower(drillPower);
    }

    public void addTileEntity(TileEntity te) {
        pos2te.put(te.getPos(), te);
        tileEntities.add(te);
    }

    public static StorageChunk copyWorldBB(World world, AxisAlignedBB bb) {
        int actualMinX = (int) bb.maxX,
                actualMinY = (int) bb.maxY,
                actualMinZ = (int) bb.maxZ,
                actualMaxX = (int) bb.minX,
                actualMaxY = (int) bb.minY,
                actualMaxZ = (int) bb.minZ;

        //Try to fit to smallest bounds
        for (int x = (int) bb.minX; x <= bb.maxX; x++) {
            for (int z = (int) bb.minZ; z <= bb.maxZ; z++) {
                for (int y = (int) bb.minY; y <= bb.maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    Block block = world.getBlockState(pos).getBlock();

                    if (!block.isAir(world.getBlockState(pos), world, pos)) {
                        if (x < actualMinX)
                            actualMinX = x;
                        if (y < actualMinY)
                            actualMinY = y;
                        if (z < actualMinZ)
                            actualMinZ = z;
                        if (x > actualMaxX)
                            actualMaxX = x;
                        if (y > actualMaxY)
                            actualMaxY = y;
                        if (z > actualMaxZ)
                            actualMaxZ = z;
                    }
                }
            }
        }

        StorageChunk ret = new StorageChunk((actualMaxX - actualMinX + 1), (actualMaxY - actualMinY + 1), (actualMaxZ - actualMinZ + 1));

        float weight = 0;

        //Iterate though the bounds given storing blocks/meta/tiles
        for (int x = actualMinX; x <= actualMaxX; x++) {
            for (int z = actualMinZ; z <= actualMaxZ; z++) {
                for (int y = actualMinY; y <= actualMaxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    weight += WeightEngine.INSTANCE.getWeight(world, pos);

                    IBlockState state = world.getBlockState(pos);
                    ret.blocks[x - actualMinX][y - actualMinY][z - actualMinZ] = state.getBlock();
                    ret.metas[x - actualMinX][y - actualMinY][z - actualMinZ] = (short) state.getBlock().getMetaFromState(state);

                    if (state.getBlock() == AdvancedRocketryBlocks.blockServiceMonitor) {
                        ret.hasServiceMonitor = true;
                    }

                    TileEntity entity = world.getTileEntity(pos);
                    if (entity != null) {
                        NBTTagCompound nbt = new NBTTagCompound();
                        entity.writeToNBT(nbt);

                        //Transform tileEntity coords
                        nbt.setInteger("x", nbt.getInteger("x") - actualMinX);
                        nbt.setInteger("y", nbt.getInteger("y") - actualMinY);
                        nbt.setInteger("z", nbt.getInteger("z") - actualMinZ);

                        //XXX: Hack to make chisels & bits renderable
                        if (nbt.getString("id").equals("minecraft:mod.chiselsandbits.tileentitychiseled"))
                            nbt.setString("id", "minecraft:mod.chiselsandbits.tileentitychiseled.tesr");

                        TileEntity newTile = ZUtils.createTile(nbt);
                        if (newTile != null) {
                            newTile.setWorld(ret.world);

                            if (isInventoryBlock(newTile)) {
                                ret.inventoryTiles.add(newTile);
                            }

                            if (isLiquidContainerBlock(newTile)) {
                                ret.liquidTiles.add(newTile);
                            }

                            ret.addTileEntity(newTile);
                        }
                    }
                }
            }
        }

        ret.weight = weight;

        return ret;
    }

    public static StorageChunk cutWorldBB(World worldObj, AxisAlignedBB bb) {
        StorageChunk chunk = StorageChunk.copyWorldBB(worldObj, bb);

        for (int x = (int) bb.minX; x <= bb.maxX; x++) {
            for (int z = (int) bb.minZ; z <= bb.maxZ; z++) {
                for (int y = (int) bb.minY; y <= bb.maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    //Workaround for dupe
                    TileEntity tile = worldObj.getTileEntity(pos);
                    if (tile instanceof IInventory) {
                        IInventory inv = (IInventory) tile;
                        for (int i = 0; i < inv.getSizeInventory(); i++) {
                            inv.setInventorySlotContents(i, ItemStack.EMPTY);
                        }
                    }

                    worldObj.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                }
            }
        }

        //Carpenter's block's dupe
        for (Entity entity : worldObj.getEntitiesWithinAABB(EntityItem.class, bb.grow(5, 5, 5))) {
            entity.setDead();
        }

        return chunk;
    }

    public EntityRocketBase getEntity() {
        return (EntityRocketBase) entity;
    }

    public void setEntity(EntityRocketBase entity) {
        this.entity = entity;
    }

    @Override
    public int getSizeX() {
        return sizeX;
    }

    @Override
    public int getSizeY() {
        return sizeY;
    }

    @Override
    public int getSizeZ() {
        return sizeZ;
    }

    @Override
    public List<TileEntity> getTileEntityList() {
        return tileEntities;
    }

    /**
     * @return list of fluid handing tiles on the rocket all also implement IFluidHandler
     */
    public List<TileEntity> getFluidTiles() {
        return liquidTiles;
    }

    public List<TileEntity> getInventoryTiles() {
        return inventoryTiles;
    }

    public List<TileEntity> getGUITiles() {

		/*TileEntity guidanceComputer = getGuidanceComputer();
		if(guidanceComputer != null)
			list.add(getGuidanceComputer());*/
        return new LinkedList<>(inventoryTiles);
    }

    @Override
    @Nonnull
    public IBlockState getBlockState(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ || blocks[x][y][z] == null) {
            return Blocks.AIR.getDefaultState();
        }
        return blocks[x][y][z].getStateFromMeta(metas[x][y][z]);
    }

    public void setBlockState(BlockPos pos, IBlockState state) {

//        System.out.println("Block "+pos.getX()+":"+pos.getY()+":"+pos.getZ()+" set to "+state.getBlock().getUnlocalizedName());

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        blocks[x][y][z] = state.getBlock();
        metas[x][y][z] = (short) state.getBlock().getMetaFromState(state);
    }

    public void rotateBy(EnumFacing dir) {

        HashedBlockPosition newSizes = new HashedBlockPosition(getSizeX(), getSizeY(), getSizeZ());

        HashedBlockPosition newerSize = remapCoord(newSizes, dir);
        newSizes = remapCoord(newSizes, dir);

        Block[][][] blocks = new Block[newSizes.x][newSizes.y][newSizes.z];
        short[][][] metas = new short[newSizes.x][newSizes.y][newSizes.z];

        for (int y = 0; y < getSizeY(); y++) {
            for (int z = 0; z < getSizeZ(); z++) {
                for (int x = 0; x < getSizeX(); x++) {
                    newSizes = getNewCoord(new HashedBlockPosition(x, y, z), dir);
                    blocks[newSizes.x][newSizes.y][newSizes.z] = this.blocks[x][y][z];
                    metas[newSizes.x][newSizes.y][newSizes.z] = this.metas[x][y][z];
                }
            }
        }
        this.blocks = blocks;
        this.metas = metas;


        for (TileEntity e : tileEntities) {
            newSizes = getNewCoord(new HashedBlockPosition(e.getPos()), dir);
            e.setPos(newSizes.getBlockPos());
        }

        this.sizeX = newerSize.x;
        this.sizeY = newerSize.y;
        this.sizeZ = newerSize.z;
    }

    private HashedBlockPosition remapCoord(HashedBlockPosition in, EnumFacing dir) {

        HashedBlockPosition out = new HashedBlockPosition(0, 0, 0);

        switch (dir) {
            case DOWN:
            case UP:
                out.x = in.z;
                out.y = in.y;
                out.z = in.x;
                break;
            case NORTH:
            case SOUTH:
                out.x = in.y;
                out.y = (short) (in.x);
                out.z = in.z;
                break;
            case EAST:
            case WEST:
                out.x = in.x;
                out.y = (short) (in.z);
                out.z = in.y;
                break;
        }

        return out;
    }

    public HashedBlockPosition getNewCoord(HashedBlockPosition in, EnumFacing dir) {

        HashedBlockPosition out = new HashedBlockPosition(0, 0, 0);

        switch (dir) {
            case DOWN:
                out.x = in.z;
                out.y = in.y;
                out.z = getSizeX() - in.x - 1;
                break;
            case UP:
                out.x = getSizeZ() - in.z - 1;
                out.y = in.y;
                out.z = in.x;
                break;
            case NORTH:
                out.x = in.y;
                out.y = (short) (getSizeX() - in.x - 1);
                out.z = in.z;
                break;
            case SOUTH:
                out.x = getSizeY() - in.y - 1;
                out.y = (short) in.x;
                out.z = in.z;
                break;
            case EAST:
                out.x = in.x;
                out.y = (short) (getSizeZ() - in.z - 1);
                out.z = in.y;
                break;
            case WEST:
                out.x = in.x;
                out.y = (short) in.z;
                out.z = getSizeY() - in.y - 1;
                break;
        }

        return out;
    }

    //TODO: optimize the F*** out of this
    public void writeToNBT(NBTTagCompound nbt) {

        if (world.isRemote) return; //client has no business writing here

        nbt.setInteger("xSize", sizeX);
        nbt.setInteger("ySize", sizeY);
        nbt.setInteger("zSize", sizeZ);
        nbt.setFloat("weight", weight);
        nbt.setBoolean("hasServiceMonitor", hasServiceMonitor);

        Iterator<TileEntity> tileEntityIterator = tileEntities.iterator();
        NBTTagList tileList = new NBTTagList();
        while (tileEntityIterator.hasNext()) {
            TileEntity tile = tileEntityIterator.next();
            try {
                NBTTagCompound tileNbt = new NBTTagCompound();
                tile.writeToNBT(tileNbt);
                tileList.appendTag(tileNbt);
            } catch (RuntimeException e) {
                AdvancedRocketry.logger.warn("A tile entity has thrown an error: " + tile.getClass().getCanonicalName());
                blocks[tile.getPos().getX()][tile.getPos().getY()][tile.getPos().getZ()] = Blocks.AIR;
                metas[tile.getPos().getX()][tile.getPos().getY()][tile.getPos().getZ()] = 0;
                tileEntityIterator.remove();
            }
        }

        int[] blockId = new int[sizeX * sizeY * sizeZ];
        int[] metasId = new int[sizeX * sizeY * sizeZ];
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    blockId[z + (sizeZ * y) + (sizeZ * sizeY * x)] = Block.getIdFromBlock(blocks[x][y][z]);
                    metasId[z + (sizeZ * y) + (sizeZ * sizeY * x)] = metas[x][y][z];
                }
            }
        }

        NBTTagIntArray idList = new NBTTagIntArray(blockId);
        NBTTagIntArray metaList = new NBTTagIntArray(metasId);

        nbt.setTag("idList", idList);
        nbt.setTag("metaList", metaList);
        nbt.setTag("tiles", tileList);


		/*for(int x = 0; x < sizeX; x++) {
			for(int y = 0; y < sizeY; y++) {
				for(int z = 0; z < sizeZ; z++) {

					idList.appendTag(new NBTTagInt(Block.getIdFromBlock(blocks[x][y][z])));
					metaList.appendTag(new NBTTagInt(metas[x][y][z]));

					//NBTTagCompound tag = new NBTTagCompound();
					tag.setInteger("block", Block.getIdFromBlock(blocks[x][y][z]));
					tag.setShort("meta", metas[x][y][z]);

					NBTTagCompound tileNbtData = null;

					for(TileEntity tile : tileEntities) {
						NBTTagCompound tileNbt = new NBTTagCompound();

						tile.writeToNBT(tileNbt);

						if(tileNbt.getInteger("x") == x && tileNbt.getInteger("y") == y && tileNbt.getInteger("z") == z){
							tileNbtData = tileNbt;
							break;
						}
					}

					if(tileNbtData != null)
						tag.setTag("tile", tileNbtData);

					nbt.setTag(String.format("%d.%d.%d", x,y,z), tag);
				}

			}
		}*/
    }

    public void readFromNBT(NBTTagCompound nbt) {

        //System.out.println("read from nbt");

        sizeX = nbt.getInteger("xSize");
        sizeY = nbt.getInteger("ySize");
        sizeZ = nbt.getInteger("zSize");
        weight = nbt.getFloat("weight");
        hasServiceMonitor = nbt.getBoolean("hasServiceMonitor");

        blocks = new Block[sizeX][sizeY][sizeZ];
        metas = new short[sizeX][sizeY][sizeZ];

        tileEntities.clear();
        inventoryTiles.clear();
        liquidTiles.clear();
        chunk = new Chunk(world, 0, 0);

        int[] blockId = nbt.getIntArray("idList");
        int[] metasId = nbt.getIntArray("metaList");


        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    blocks[x][y][z] = Block.getBlockById(blockId[z + (sizeZ * y) + (sizeZ * sizeY * x)]);
                    metas[x][y][z] = (short) metasId[z + (sizeZ * y) + (sizeZ * sizeY * x)];

                    chunk.setBlockState(new BlockPos(x, y, z), this.blocks[x][y][z].getStateFromMeta(this.metas[x][y][z]));
                    world.checkLightFor(EnumSkyBlock.BLOCK, new BlockPos(x, y, z));
                }
            }
        }

        NBTTagList tileList = nbt.getTagList("tiles", NBT.TAG_COMPOUND);

        for (int i = 0; i < tileList.tagCount(); i++) {

            try {
                TileEntity tile = ZUtils.createTile(tileList.getCompoundTagAt(i));
                tile.setWorld(world);

                if (isInventoryBlock(tile)) {
                    inventoryTiles.add(tile);
                }

                if (isLiquidContainerBlock(tile)) {
                    liquidTiles.add(tile);
                }

                this.addTileEntity(tile);
                tile.setWorld(world);

                chunk.addTileEntity(tile);

            } catch (Exception e) {
                AdvancedRocketry.logger.warn("Rocket missing Tile (was a mod removed?)");
            }

        }

		/*for(int x = 0; x < sizeX; x++) {
			for(int y = 0; y < sizeY; y++) {
				for(int z = 0; z < sizeZ; z++) {



					NBTTagCompound tag = (NBTTagCompound)nbt.getTag(String.format("%d.%d.%d", x,y,z));

					if(!tag.hasKey("block"))
						continue;
					int blockId = tag.getInteger("block");
					blocks[x][y][z] = Block.getBlockById(blockId);
					metas[x][y][z] = tag.getShort("meta");


					if(blockId != 0 && blocks[x][y][z] == Blocks.air) {
						AdvancedRocketry.logger.warn("Removed pre-existing block with id " + blockId + " from a rocket (Was a mod removed?)");
					}
					else if(tag.hasKey("tile")) {

						if(blocks[x][y][z].hasTileEntity(metas[x][y][z])) {
							TileEntity tile = TileEntity.createAndLoadEntity(tag.getCompoundTag("tile"));
							tile.setWorldObj(world);

							tileEntities.add(tile);

							//Machines would throw a wrench in the works
							if(isUsableBlock(tile)) {
								inventories.add((IInventory)tile);
								usableTiles.add(tile);
							}
						}
					}
				}
			}
		}*/
        this.chunk.generateSkylightMap();
    }

    //pass the coords of the xmin, ymin, zmin as well as the world to move the rocket
    @Override
    public void pasteInWorld(World world, int xCoord, int yCoord, int zCoord) {

        //Set all the blocks
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int y = 0; y < sizeY; y++) {

                    if (blocks[x][y][z] != Blocks.AIR) {
                        world.setBlockState(new BlockPos(xCoord + x, yCoord + y, zCoord + z), blocks[x][y][z].getStateFromMeta(metas[x][y][z]), 2);
                    }
                }
            }
        }

        //Set tiles for each block
        for (TileEntity tile : tileEntities) {
            NBTTagCompound nbt = new NBTTagCompound();
            tile.writeToNBT(nbt);
            int x = nbt.getInteger("x");
            int y = nbt.getInteger("y");
            int z = nbt.getInteger("z");

            int tmpX = x + xCoord;
            int tmpY = y + yCoord;
            int tmpZ = z + zCoord;

            //Set blocks of tiles again to avoid weirdness caused by updates
            //world.setBlock(xCoord + x, yCoord + y, zCoord + z, blocks[x][y][z], metas[x][y][z], 2);


            nbt.setInteger("x", tmpX);
            nbt.setInteger("y", tmpY);
            nbt.setInteger("z", tmpZ);

            TileEntity entity = world.getTileEntity(new BlockPos(tmpX, tmpY, tmpZ));

            if (entity != null)
                entity.readFromNBT(nbt);
        }
    }

    public void damageParts() {
        for (TileEntity tile : tileEntities) {
            if (tile instanceof TileBrokenPart) {
                ((TileBrokenPart) tile).transition();
            }
        }
    }

    @Nullable
    public TileEntity getTileEntity(@Nonnull BlockPos pos) {
        return pos2te.getOrDefault(pos, null);
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        if (pos.getX() >= blocks.length || pos.getY() >= blocks[0].length || pos.getZ() >= blocks[0][0].length)
            return true;
        return blocks[pos.getX()][pos.getY()][pos.getZ()] == Blocks.AIR;
    }

    @Override
    @Nonnull
    public Biome getBiome(@Nullable BlockPos pos) {
        //Don't care, gen ocean
        return Biomes.OCEAN;
    }

    @Override
    public boolean isSideSolid(BlockPos pos, @Nonnull EnumFacing side, boolean _default) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ || x + side.getFrontOffsetX() < 0
                || x + side.getFrontOffsetX() >= sizeX || y + side.getFrontOffsetY() < 0 || y + side.getFrontOffsetY() >= sizeY
                || z + side.getFrontOffsetZ() < 0 || z + side.getFrontOffsetZ() >= sizeZ)
            return false;

        return blocks[x + side.getFrontOffsetX()][y + side.getFrontOffsetY()][z + side.getFrontOffsetZ()].isSideSolid(blocks[x + side.getFrontOffsetX()][y + side.getFrontOffsetY()][z + side.getFrontOffsetZ()].getStateFromMeta(metas[x + side.getFrontOffsetX()][y + side.getFrontOffsetY()][z + side.getFrontOffsetZ()]), this, pos.offset(side), side.getOpposite());

    }

    public List<TileSatelliteHatch> getSatelliteHatches() {
        LinkedList<TileSatelliteHatch> satelliteHatches = new LinkedList<>();
        for (TileEntity tile : getTileEntityList()) {
            if (tile instanceof TileSatelliteHatch) {
                satelliteHatches.add((TileSatelliteHatch) tile);
            }
        }

        return satelliteHatches;
    }

    @Deprecated
    public List<SatelliteBase> getSatellites() {
        LinkedList<SatelliteBase> satellites = new LinkedList<>();
        LinkedList<TileSatelliteHatch> satelliteHatches = new LinkedList<>();
        for (TileEntity tile : getTileEntityList()) {
            if (tile instanceof TileSatelliteHatch) {
                satelliteHatches.add((TileSatelliteHatch) tile);
            }
        }


        for (TileSatelliteHatch tile : satelliteHatches) {
            SatelliteBase satellite = tile.getSatellite();
            if (satellite != null)
                satellites.add(satellite);
        }
        return satellites;
    }

    public TileGuidanceComputer getGuidanceComputer() {
        for (TileEntity tile : getTileEntityList()) {
            if (tile instanceof TileGuidanceComputer) {
                return (TileGuidanceComputer) tile;
            }
        }

        return null;
    }

    public float getBreakingProbability() {
        float prob = 0;

        for (TileEntity te : tileEntities) {
            if (te instanceof TileBrokenPart) {
                TileBrokenPart brokenPart = (TileBrokenPart) te;
                float additionalProb = 0;

                if (te.getBlockType() instanceof BlockNuclearRocketMotor) {
                    additionalProb = 1F;
                } else if (te.getBlockType() instanceof BlockRocketMotor || te.getBlockType() instanceof BlockBipropellantRocketMotor) {
                    additionalProb = 0.2F;
                }
                prob += additionalProb * brokenPart.getStage() / 10;
                if (prob >= 1) {
                    return Math.min(1, prob);
                }
            }
        }

        return prob;
    }

    public List<TileBrokenPart> getBrokenBlocks() {
        List<TileBrokenPart> res = new ArrayList<>();

        for (TileEntity te : tileEntities) {
            if (te instanceof TileBrokenPart) {
                res.add((TileBrokenPart) te);
            }
        }

        return res;
    }

    public boolean shouldBreak() {
        return world.rand.nextFloat() < this.getBreakingProbability();
    }

    /**
     * @return destination ID or Constants.INVALID_PLANET if none
     */
    public int getDestinationDimId(int currentDimId, int x, int z) {
        for (TileEntity tile : getTileEntityList()) {
            if (tile instanceof TileGuidanceComputer) {
                return ((TileGuidanceComputer) tile).getDestinationDimId(currentDimId, new BlockPos(x, 0, z));
            }
        }

        return Constants.INVALID_PLANET;
    }

    public Vector3F<Float> getDestinationCoordinates(int destDimID, boolean commit) {
        for (TileEntity tile : getTileEntityList()) {
            if (tile instanceof TileGuidanceComputer) {
                return ((TileGuidanceComputer) tile).getLandingLocation(destDimID, commit);
            }
        }
        return null;
    }

    public String getDestinationName(int destDimID) {
        for (TileEntity tile : getTileEntityList()) {
            if (tile instanceof TileGuidanceComputer) {
                return ((TileGuidanceComputer) tile).getDestinationName(destDimID);
            }
        }
        return "";
    }

    public void setDestinationCoordinates(Vector3F<Float> vec, int dimid) {
        for (TileEntity tile : getTileEntityList()) {
            if (tile instanceof TileGuidanceComputer) {
                ((TileGuidanceComputer) tile).setReturnPosition(vec, dimid);
            }
        }
    }


    public void readtiles(ByteBuf in) {
        PacketBuffer buffer = new PacketBuffer(in);
        short numTiles = buffer.readShort();

        //tileEntities.clear();
        //inventoryTiles.clear();
        //liquidTiles.clear();
        //this can cause a ConcurrentModificationException in render if the rocket is loaded with fluids and tiles get updated
        //so we need a hacky fix here and not modify the lists without deleting or adding elements

        for (short i = 0; i < numTiles; i++) {
            try {
                NBTTagCompound nbt = buffer.readCompoundTag();

                TileEntity tile = ZUtils.createTile(nbt);
                BlockPos tilepos = tile.getPos();

                for (int j = 0; j < tileEntities.size(); j++) {
                    TileEntity t = tileEntities.get(j);
                    if (t.getPos().equals(tilepos)) {
                        t.readFromNBT(nbt);
                    }
                }
                if (isInventoryBlock(tile)) {
                    for (int j = 0; j < inventoryTiles.size(); j++) {
                        TileEntity t = inventoryTiles.get(j);
                        if (t.getPos().equals(tilepos)) {
                            t.readFromNBT(nbt);
                        }
                    }
                }
                if (isLiquidContainerBlock(tile)) {
                    for (int j = 0; j < liquidTiles.size(); j++) {
                        TileEntity t = liquidTiles.get(j);
                        if (t.getPos().equals(tilepos)) {
                            t.readFromNBT(nbt);
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void writetiles(ByteBuf out) {
        PacketBuffer buffer = new PacketBuffer(out);
        buffer.writeShort(tileEntities.size());
        Iterator<TileEntity> tileIterator = tileEntities.iterator();

        while (tileIterator.hasNext()) {
            TileEntity tile = tileIterator.next();

            NBTTagCompound nbt = new NBTTagCompound();

            try {
                tile.writeToNBT(nbt);

                try {
                    buffer.writeCompoundTag(nbt);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (RuntimeException e) {
                AdvancedRocketry.logger.warn("A tile entity has thrown an error while writing to network: " + tile.getClass().getCanonicalName());
                tileIterator.remove();
            }
        }
    }

    public void writeToNetwork(ByteBuf out) {

        if (DimensionManager.getWorld(0).isRemote) System.out.println("This should have never been called!");

        PacketBuffer buffer = new PacketBuffer(out);

        buffer.writeByte(this.sizeX);
        buffer.writeByte(this.sizeY);
        buffer.writeByte(this.sizeZ);
        buffer.writeShort(tileEntities.size());

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    buffer.writeInt(Block.getIdFromBlock(this.blocks[x][y][z]));
                    buffer.writeShort(this.metas[x][y][z]);
                }
            }
        }

        Iterator<TileEntity> tileIterator = tileEntities.iterator();

        while (tileIterator.hasNext()) {
            TileEntity tile = tileIterator.next();

            NBTTagCompound nbt = new NBTTagCompound();

            try {
                tile.writeToNBT(nbt);

                try {
                    buffer.writeCompoundTag(nbt);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (RuntimeException e) {
                AdvancedRocketry.logger.warn("A tile entity has thrown an error while writing to network: " + tile.getClass().getCanonicalName());
                tileIterator.remove();
            }
        }

        buffer.writeBoolean(hasServiceMonitor);
    }

    public void readFromNetwork(ByteBuf in) {
        //System.out.println("read from network");

        finalized = false;
        PacketBuffer buffer = new PacketBuffer(in);

        this.sizeX = buffer.readByte();
        this.sizeY = buffer.readByte();
        this.sizeZ = buffer.readByte();
        short numTiles = buffer.readShort();

        this.blocks = new Block[sizeX][sizeY][sizeZ];
        this.metas = new short[sizeX][sizeY][sizeZ];
        chunk = new Chunk(world, 0, 0);


        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {

                    this.blocks[x][y][z] = Block.getBlockById(buffer.readInt());
                    this.metas[x][y][z] = buffer.readShort();

                    chunk.setBlockState(new BlockPos(x, y, z), this.blocks[x][y][z].getStateFromMeta(this.metas[x][y][z]));
                    world.checkLightFor(EnumSkyBlock.BLOCK,new BlockPos(x, y, z));
                }
            }
        }

        for (short i = 0; i < numTiles; i++) {
            try {
                NBTTagCompound nbt = buffer.readCompoundTag();

                TileEntity tile = ZUtils.createTile(nbt);
                tile.setWorld(world);
                this.addTileEntity(tile);

                if (isInventoryBlock(tile)) {
                    inventoryTiles.add(tile);
                }

                if (isLiquidContainerBlock(tile))
                    liquidTiles.add(tile);


                chunk.addTileEntity(tile);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        hasServiceMonitor = buffer.readBoolean();

        //We are now ready to render
        this.chunk.generateSkylightMap();
        finalized = true;
    }

    @Override
    public int getCombinedLight(@Nullable BlockPos pos, int lightValue) {
        return lightValue;
    }

    @Override
    public int getStrongPower(@Nullable BlockPos pos, @Nullable EnumFacing direction) {
        return 0;
    }

    @Override
    @Nonnull
    public WorldType getWorldType() {
        return WorldType.CUSTOMIZED;
    }
}
