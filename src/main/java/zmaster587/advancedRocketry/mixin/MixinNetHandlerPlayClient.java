package zmaster587.advancedRocketry.mixin;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketChangeGameState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DEBUG-ONLY (temporary): logs every {@link SPacketChangeGameState} the
 * client receives so the post-transition rain-state regression can be traced.
 * Remove once the weather sync issue is understood.
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Inject(method = "handleChangeGameState", at = @At("HEAD"))
    private void ar$logChangeGameState(SPacketChangeGameState packet, CallbackInfo ci) {
        System.err.println("[ARWeather-CLIENT] handleChangeGameState reason="
                + packet.getGameState() + " value=" + packet.getValue());
    }
}
