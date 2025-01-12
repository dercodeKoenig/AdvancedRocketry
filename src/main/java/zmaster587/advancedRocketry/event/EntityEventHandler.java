package zmaster587.advancedRocketry.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketChangeGameState;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class EntityEventHandler {

    @SubscribeEvent
    public void onJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayer && !event.getWorld().isRemote) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
            World world = event.getWorld();
//            if (world.isRaining()) {
//                player.connection.sendPacket(new SPacketChangeGameState(2, ));
//            }
            if (world.isRaining()) {
                player.connection.sendPacket(new SPacketChangeGameState(1, 0.0F));
                player.connection.sendPacket(new SPacketChangeGameState(7, world.getRainStrength(1.0F)));
                player.connection.sendPacket(new SPacketChangeGameState(8, world.getThunderStrength(1.0F)));
            } else {
                player.connection.sendPacket(new SPacketChangeGameState(2, 0.0F));
                player.connection.sendPacket(new SPacketChangeGameState(7, 0.0F));
                player.connection.sendPacket(new SPacketChangeGameState(8, 0.0F));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.player.world.isRemote) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            World world = player.world;
//            if (world.isRaining()) {
//                player.connection.sendPacket(new SPacketChangeGameState(2, ));
//            }
            if (world.isRaining()) {
                player.connection.sendPacket(new SPacketChangeGameState(1, 0.0F));
                player.connection.sendPacket(new SPacketChangeGameState(7, world.getRainStrength(1.0F)));
                player.connection.sendPacket(new SPacketChangeGameState(8, world.getThunderStrength(1.0F)));
            } else {
                player.connection.sendPacket(new SPacketChangeGameState(2, 0.0F));
                player.connection.sendPacket(new SPacketChangeGameState(7, 0.0F));
                player.connection.sendPacket(new SPacketChangeGameState(8, 0.0F));
            }
        }
    }
}
