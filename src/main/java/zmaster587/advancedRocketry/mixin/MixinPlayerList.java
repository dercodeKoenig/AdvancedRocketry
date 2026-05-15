package zmaster587.advancedRocketry.mixin;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketChangeGameState;
import net.minecraft.server.management.PlayerList;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes a vanilla 1.12.2 bug in
 * {@link PlayerList#updateTimeAndWeatherForPlayer}:
 *
 * <pre>
 * if (worldIn.isRaining()) {
 *     playerIn.connection.sendPacket(new SPacketChangeGameState(1, 0.0F));  // ← wrong: state 1 = STOP RAINING
 *     ...
 * }
 * </pre>
 *
 * <p>The packet code should be {@code 2} (BEGIN_RAINING) when the world is
 * raining — vanilla sends STOP, so any player joining or transitioning into
 * a raining world initially sees the rain as cleared until something else
 * resyncs. For AR-planet weather this is especially visible: every cross-dim
 * teleport into a raining planet flickered through "no rain" until our
 * {@code syncToPlayer} re-broadcast caught up.</p>
 *
 * <p>This mixin runs the corrected sync at {@code HEAD} and cancels the
 * vanilla impl, so vanilla's buggy packet sequence never fires. Because
 * {@code updateTimeAndWeatherForPlayer} is short and well-known, an
 * {@code @Inject(cancellable=true)} replacement is safer than an
 * {@code @Overwrite} (no mapping surprises across Forge minor versions).</p>
 */
@Mixin(PlayerList.class)
public abstract class MixinPlayerList {

    @Inject(method = "updateTimeAndWeatherForPlayer", at = @At("HEAD"), cancellable = true)
    private void ar$fixUpdateTimeAndWeatherForPlayer(EntityPlayerMP playerIn,
                                                     WorldServer worldIn,
                                                     CallbackInfo ci) {
        // World border / time-of-day are uncorrupted by the vanilla impl, so
        // re-issue the same packets vanilla does. We only need to fix the
        // begin/end raining code.
        playerIn.connection.sendPacket(new net.minecraft.network.play.server.SPacketWorldBorder(
                ((net.minecraft.server.management.PlayerList) (Object) this)
                        .getServerInstance().getWorld(0).getWorldBorder(),
                net.minecraft.network.play.server.SPacketWorldBorder.Action.INITIALIZE));
        playerIn.connection.sendPacket(new net.minecraft.network.play.server.SPacketTimeUpdate(
                worldIn.getTotalWorldTime(),
                worldIn.getWorldTime(),
                worldIn.getGameRules().getBoolean("doDaylightCycle")));

        // Two bugs in the vanilla original we fix here:
        //   1. `worldIn.isRaining()` checks rainStrength > 0.2 — NOT the
        //      WorldInfo flag. Right after a "/weather rain" the flag is
        //      true but strength is still climbing from 0, so vanilla
        //      under-reports "not raining" and skips the entire block
        //      (player sees no rain on dim transition until strength
        //      catches up). We check the flag directly.
        //   2. The `new SPacketChangeGameState(1, ...)` in the original is
        //      state 1 = END_RAINING. It should be 2 (BEGIN_RAINING).
        net.minecraft.world.storage.WorldInfo info = worldIn.getWorldInfo();
        System.err.println("[ARWeather-MIXIN] updateTimeAndWeatherForPlayer name=" + playerIn.getName()
                + " dim=" + worldIn.provider.getDimension()
                + " info.isRaining=" + info.isRaining()
                + " info.class=" + info.getClass().getSimpleName()
                + " rainStr=" + worldIn.getRainStrength(1.0F));
        if (info.isRaining()) {
            playerIn.connection.sendPacket(new SPacketChangeGameState(2, 0.0F));
            playerIn.connection.sendPacket(new SPacketChangeGameState(7, worldIn.getRainStrength(1.0F)));
            playerIn.connection.sendPacket(new SPacketChangeGameState(8, worldIn.getThunderStrength(1.0F)));
        } else {
            // Worldinfo says "not raining" — explicitly tell the client.
            // The client's fresh WorldInfo (post-SPacketRespawn) defaults to
            // isRaining=false, but a previous dimension that DID rain may
            // have left the client world in a partial-rain state, so spell
            // it out.
            playerIn.connection.sendPacket(new SPacketChangeGameState(1, 0.0F));
            playerIn.connection.sendPacket(new SPacketChangeGameState(7, 0.0F));
            playerIn.connection.sendPacket(new SPacketChangeGameState(8, 0.0F));
        }
        ci.cancel();
    }
}
