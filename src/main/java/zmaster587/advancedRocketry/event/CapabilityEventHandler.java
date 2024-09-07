package zmaster587.advancedRocketry.event;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import zmaster587.advancedRocketry.api.Constants;
import zmaster587.advancedRocketry.heat.HeatContainerCapability;
import zmaster587.advancedRocketry.heat.IHeatable;

import javax.annotation.Nullable;

public class CapabilityEventHandler {

    public static final ResourceLocation HEAT_CAP = new ResourceLocation(Constants.modId, "heat_cap");

    @SubscribeEvent
    public void attachCapability(AttachCapabilitiesEvent<TileEntity> event) {
        if (!(event.getObject() instanceof IHeatable)) return;

        event.addCapability(HEAT_CAP, new HeatContainerCapability());
    }

    public static CapabilityDispatcher gatherCapabilities(AttachCapabilitiesEvent<?> event, @Nullable ICapabilityProvider parent) {
        MinecraftForge.EVENT_BUS.post(event);
        return event.getCapabilities().size() > 0 || parent != null ? new CapabilityDispatcher(event.getCapabilities(), parent) : null;
    }

    @Nullable
    public static <T> CapabilityDispatcher gatherCapabilities(Class<T> cls, T target) {
        return gatherCapabilities(new AttachCapabilitiesEvent<>(cls, target), null);
    }
}
