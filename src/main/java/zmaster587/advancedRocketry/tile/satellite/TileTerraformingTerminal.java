package zmaster587.advancedRocketry.tile.satellite;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.fml.relauncher.Side;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.api.SatelliteRegistry;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.inventory.modules.ModuleSatellite;
import zmaster587.advancedRocketry.item.ItemBiomeChanger;
import zmaster587.advancedRocketry.item.ItemSatelliteIdentificationChip;
import zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger;
import zmaster587.advancedRocketry.util.BiomeHandler;
import zmaster587.advancedRocketry.util.TerraformingHelper;
import zmaster587.advancedRocketry.world.ChunkManagerPlanet;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.IUniversalEnergy;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.TileInventoriedRFConsumer;
import zmaster587.libVulpes.util.INetworkMachine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;



public class TileTerraformingTerminal extends TileInventoriedRFConsumer implements INetworkMachine, IModularInventory, IButtonInventory {

    private ModuleText moduleText;

    public boolean was_enabled_last_tick;

    private ModuleButton buttonstopall;

    private int sat_power_per_tick;
    private float randomblocks_per_tick;



    public TileTerraformingTerminal() {
        super(1, 1);
         sat_power_per_tick = 0;
         randomblocks_per_tick = 0;
        was_enabled_last_tick = false;
    }

    @Override
    @Nonnull
    public int[] getSlotsForFace(@Nullable EnumFacing side) {
        return new int[0];
    }

    @Override
    public String getModularInventoryName() {
        return AdvancedRocketryBlocks.blockSatelliteControlCenter.getLocalizedName();
    }

    @Override
    public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public boolean canPerformFunction() {
        return true;
    }

    @Override
    public int getPowerPerOperation() {
        return 0;
    }

    @Override
    public void performFunction() {
        if (world.isRemote)
            updateInventoryInfo();
    }

    @Override
    public void writeDataToNetwork(ByteBuf out, byte packetId) {
        if (packetId == (byte) 22){
            out.writeInt(sat_power_per_tick);
            out.writeFloat(randomblocks_per_tick);
        }
    }

    @Override
    public void readDataFromNetwork(ByteBuf in, byte packetId,
                                    NBTTagCompound nbt) {
        if (packetId == (byte) 22){
            nbt.setInteger("powergen", in.readInt());
            nbt.setFloat("blockpertick", in.readFloat());
        }
    }

    @Override
    public void useNetworkData(EntityPlayer player, Side side, byte id, NBTTagCompound nbt) {
        if (id == (byte) 22){
            this.sat_power_per_tick = nbt.getInteger("powergen");
            this.randomblocks_per_tick=  nbt.getFloat("blockpertick");
            this.updateInventoryInfo();
        }
    }

    @Override
    public void setInventorySlotContents(int slot, @Nonnull ItemStack stack) {
        super.setInventorySlotContents(slot, stack);
        updateInventoryInfo();
    }


    @Override
    public void update() {
        super.update();
        boolean has_redstone = world.isBlockIndirectlyGettingPowered(getPos()) != 0;
        int powerrequired = 80; //120;
        if (!world.isRemote) {

            if (world.getTotalWorldTime() % 20 == 0)
                //world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
                PacketHandler.sendToNearby(new PacketMachine(this,(byte) 22),world.provider.getDimension(),pos,16);

            if (hasValidBiomeChanger() && has_redstone) {
                if (!was_enabled_last_tick) {
                    was_enabled_last_tick = true;

                    Item biomeChanger = getStackInSlot(0).getItem();
                    if (biomeChanger instanceof ItemBiomeChanger) {
                        SatelliteBiomeChanger sat = (SatelliteBiomeChanger) ItemSatelliteIdentificationChip.getSatellite(getStackInSlot(0));
                        sat_power_per_tick = sat.getPowerPerTick();
                        randomblocks_per_tick = (float) sat_power_per_tick / powerrequired;
                    }

                    markDirty();
                }
            } else {
                if (was_enabled_last_tick) {
                    was_enabled_last_tick = false;
                    markDirty();
                }
            }
        }

        if (!world.isRemote && was_enabled_last_tick) {
            if (ARConfiguration.getCurrentConfig().enableTerraforming) {
                Item biomeChanger = getStackInSlot(0).getItem();
                if (biomeChanger instanceof ItemBiomeChanger) {
                    SatelliteBiomeChanger sat = (SatelliteBiomeChanger) ItemSatelliteIdentificationChip.getSatellite(getStackInSlot(0));
                    IUniversalEnergy battery = sat.getBattery();

                    for (int i = 0; i < 1000; i++) {
                        //TODO: Better imp

                        if (battery.getUniversalEnergyStored() > powerrequired) {
                                try {


                                    TerraformingHelper t = DimensionProperties.proxylists.gethelper(world.provider.getDimension());

                                    if (t == null) {
                                        DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).load_terraforming_helper(false);
                                        t = DimensionProperties.proxylists.gethelper(world.provider.getDimension());
                                    }

                                    BiomeProvider chunkmgr = t.chunkMgrTerraformed;
                                    //BlockPos next_block_pos = t.get_next_position(false);
                                    //BlockPos next_block_pos = t.get_next_position_biomechanging(false);
                                    BlockPos next_block_pos = t.get_next_position_biomechanging(true);

                                    if (next_block_pos != null) { // it is null when there is everything terraformed
                                        battery.extractEnergy(powerrequired, false);
                                        //BiomeHandler.terraform(world, ((ChunkManagerPlanet) chunkmgr).getBiomeGenAt(next_block_pos.getX(), next_block_pos.getZ()), next_block_pos, false, world.provider.getDimension());
                                        BiomeHandler.terraform_biomes(world, ((ChunkManagerPlanet) chunkmgr).getBiomeGenAt(next_block_pos.getX(), next_block_pos.getZ()), next_block_pos,  world.provider.getDimension());
                                    }else{
                                        //System.out.println("nothing to terraform");
                                        break; // nothing to do, everything is terraformed
                                    }

                                //} catch (NullPointerException e) {
                                //    e.printStackTrace();
                                } catch (NoClassDefFoundError e){
                                    e.printStackTrace(); //WTF
                                }

                        } else
                            break;
                    }
                }
            }
        }
    }
    public void updateInventoryInfo() {
        if (moduleText != null) {


                if (hasValidBiomeChanger() && world.isBlockIndirectlyGettingPowered(getPos()) != 0) {
                    BigDecimal bd = new BigDecimal(randomblocks_per_tick);
                    bd = bd.setScale(2, RoundingMode.HALF_UP);

                    moduleText.setText("terraforming planet...\n" +
                            "\nPower generation:" + sat_power_per_tick +
                            "\nBlocks per tick:" + bd);

                } else if (hasValidBiomeChanger()) {
                    moduleText.setText("provide redstone signal\nto start the process");
                } else {
                    moduleText.setText("place a biome remote here\nto make the satellite terraform\nthe entire planet");
                }

        }
    }

    public boolean hasValidBiomeChanger() {
        ItemStack biomeChanger = getStackInSlot(0);
        SatelliteBase satellite;

        return !biomeChanger.isEmpty() &&
                (biomeChanger.getItem() instanceof ItemBiomeChanger) &&
                SatelliteRegistry.getSatellite(biomeChanger) != null &&
                (satellite = ((ItemSatelliteIdentificationChip) AdvancedRocketryItems.itemBiomeChanger).getSatellite(biomeChanger)).getDimensionId() == world.provider.getDimension() &&
                satellite instanceof SatelliteBiomeChanger;
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {


        List<ModuleBase> modules = new LinkedList<>();
        //modules.add(buttonstopall);
        ModuleSatellite moduleSatellite = new ModuleSatellite(152, 10, this, 0);
        modules.add(moduleSatellite);

        moduleText = new ModuleText(5, 20, LibVulpes.proxy.getLocalizedString("msg.satctrlcenter.nolink"), 0x404040);
        modules.add(moduleText);

        updateInventoryInfo();
        return modules;
    }


    @Override
    public void onInventoryButtonPressed(int buttonId) {
            PacketHandler.sendToServer(new PacketMachine(this, (byte) (buttonId)));
    }


    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        //nbt.setBoolean("was_enabled_last_tick", was_enabled_last_tick);
        //nbt.setInteger("sat_power_per_tick", sat_power_per_tick);
        //nbt.setFloat("randomblocks_per_tick", randomblocks_per_tick);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        //was_enabled_last_tick = nbt.getBoolean("was_enabled_last_tick");
        //sat_power_per_tick = nbt.getInteger("sat_power_per_tick");
        //randomblocks_per_tick = nbt.getFloat("randomblocks_per_tick");
    }

    //has been replaced with custom network package
    /*
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new SPacketUpdateTileEntity(pos, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound nbt = pkt.getNbtCompound();
        readFromNBT(nbt);
    }
    */


    @Override
    public boolean canInteractWithContainer(EntityPlayer entity) {
        return true;
    }
}
