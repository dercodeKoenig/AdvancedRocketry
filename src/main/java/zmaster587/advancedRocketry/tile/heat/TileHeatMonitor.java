package zmaster587.advancedRocketry.tile.heat;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.stations.ISpaceObject;
import zmaster587.advancedRocketry.heat.HeatContainerProvider;
import zmaster587.advancedRocketry.heat.IHeatContainer;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStationObject;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.inventory.modules.IModularInventory;
import zmaster587.libVulpes.inventory.modules.ModuleBase;
import zmaster587.libVulpes.inventory.modules.ModuleText;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.TileEntityRFConsumer;
import zmaster587.libVulpes.util.INetworkMachine;

import java.util.ArrayList;
import java.util.List;

public class TileHeatMonitor extends TileEntityRFConsumer implements IModularInventory, INetworkMachine {

    private final ModuleText text;
    private int processedTicks;
    private ISpaceObject station;

    public TileHeatMonitor() {
        super(1000);
        text = new ModuleText(40, 40, LibVulpes.proxy.getLocalizedString("msg.heat.totalHeatNA"), 0x2f2f2f);
    }

    @Override
    public void performFunction() {
        if (!world.isRemote) {
            if (!(station instanceof SpaceStationObject)) {
                return;
            }

            PacketHandler.sendToNearby(new PacketMachine(this, (byte) 0), getWorld().provider.getDimension(), getPos(), 64);
        }
    }

    @Override
    public boolean canPerformFunction() {
        if (processedTicks++ < 20) {
            processedTicks = 0;
            return (station = SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(getPos())) != null;
        }
        return false;
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {
        List<ModuleBase> modules = new ArrayList<>();

        modules.add(text);

        return modules;
    }


    @Override
    public int getPowerPerOperation() {
        return 1;
    }

    @Override
    public String getModularInventoryName() {
        return AdvancedRocketryBlocks.blockHeatMonitor.getLocalizedName();
    }

    @Override
    public boolean canInteractWithContainer(EntityPlayer entity) {
        return true;
    }

    @Override
    public void writeDataToNetwork(ByteBuf byteBuf, byte b) {
        if (b == 0) {
            IHeatContainer heatContainer = ((SpaceStationObject) station).getCapability(HeatContainerProvider.HEAT_CAP, null);
            byteBuf.writeInt(heatContainer.getCurrentHeat());
            byteBuf.writeInt(heatContainer.getMaxHeat());
        }
    }

    @Override
    public void readDataFromNetwork(ByteBuf byteBuf, byte b, NBTTagCompound nbtTagCompound) {
        if (b == 0) {
            nbtTagCompound.setInteger("heat", byteBuf.readInt());
            nbtTagCompound.setInteger("maxHeat", byteBuf.readInt());
        }
    }

    @Override
    public void useNetworkData(EntityPlayer entityPlayer, Side side, byte b, NBTTagCompound nbtTagCompound) {
        text.setText(LibVulpes.proxy.getLocalizedString("msg.heat.totalHeat") + ": "
                + nbtTagCompound.getInteger("heat") + " / " + nbtTagCompound.getInteger("maxHeat"));
    }
}
