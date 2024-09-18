package zmaster587.advancedRocketry.event;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import zmaster587.advancedRocketry.api.Constants;
import zmaster587.advancedRocketry.heat.HeatContainerProvider;
import zmaster587.advancedRocketry.stations.SpaceStationObject;

import javax.annotation.Nullable;

public class CapabilityEventHandler {

    public static final ResourceLocation HEAT_CAP = new ResourceLocation(Constants.modId, "heat_cap");

    @SubscribeEvent
    public void attachCapability(AttachCapabilitiesEvent<SpaceStationObject> event) {
        HeatContainerProvider obj = new HeatContainerProvider();
        obj.getCapability(HeatContainerProvider.HEAT_CAP, null).setMaxHeat(event.getObject().getMaxHeat());
        event.addCapability(HEAT_CAP, obj);
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
