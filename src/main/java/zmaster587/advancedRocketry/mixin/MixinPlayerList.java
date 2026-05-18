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
 * Tightens vanilla 1.12.2 {@link PlayerList#updateTimeAndWeatherForPlayer}.
 *
 * <p>The vanilla packet codes themselves are correct (a careful read of
 * {@link net.minecraft.client.network.NetHandlerPlayClient#handleChangeGameState}
 * shows code 1 → {@code setRaining(true)} and code 2 → {@code setRaining(false)};
 * the wiki/MCP docstrings have the labels swapped, but server + client are
 * consistent with each other). What vanilla DOES get wrong is the gate:
 *
 * <pre>
 * if (worldIn.isRaining()) {
 *     // World.isRaining() returns getRainStrength(1.0F) > 0.2D — i.e. the
 *     // current LERPED strength, NOT the WorldInfo flag.
 *     ...
 * }
 * </pre>
 *
 * <p>So immediately after {@code /weather rain} (flag=true, strength still
 * climbing from 0), vanilla skips the entire weather-sync block: a joining /
 * dim-transitioning player sees no rain until the strength catches up
 * naturally. For AR per-dim weather this is especially visible — every
 * cross-dim teleport into a freshly-raining planet showed clear weather for
 * the first second.</p>
 *
 * <p>We re-issue the same packets vanilla intended, but check the
 * {@link net.minecraft.world.storage.WorldInfo} flag directly, so the
 * client gets the correct begin/end-raining toggle the moment they enter
 * the dim — independent of the lerp's current value.</p>
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

        // Check the WorldInfo flag directly (not getRainStrength), so a
        // freshly-set raining dim syncs even when the lerped strength is
        // still 0. Packet codes match vanilla's NetHandlerPlayClient
        // dispatch:  code 1 → setRaining(true);  code 2 → setRaining(false).
        net.minecraft.world.storage.WorldInfo info = worldIn.getWorldInfo();
        if (info.isRaining()) {
            playerIn.connection.sendPacket(new SPacketChangeGameState(1, 0.0F));
            playerIn.connection.sendPacket(new SPacketChangeGameState(7, worldIn.getRainStrength(1.0F)));
            playerIn.connection.sendPacket(new SPacketChangeGameState(8, worldIn.getThunderStrength(1.0F)));
        } else {
            // WorldInfo says "not raining". Spell it out: a previous dimension
            // that WAS raining may have left the client in a partial-rain
            // state, so explicitly clear the flag + zero the strengths.
            playerIn.connection.sendPacket(new SPacketChangeGameState(2, 0.0F));
            playerIn.connection.sendPacket(new SPacketChangeGameState(7, 0.0F));
            playerIn.connection.sendPacket(new SPacketChangeGameState(8, 0.0F));
        }
        ci.cancel();
    }
}
