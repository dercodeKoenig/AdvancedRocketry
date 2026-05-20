package zmaster587.advancedRocketry.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zmaster587.advancedRocketry.util.RocketInventoryHelper;

/**
 * Lets a player keep a rocket-inventory GUI open even when the rocket entity
 * has drifted outside the vanilla 64-block container-interaction range.
 *
 * <p>Vanilla {@link EntityPlayer#onUpdate()} closes any open container whose
 * {@link Container#canInteractWith(EntityPlayer)} returns {@code false}.
 * Rockets move; the GUI that should follow the entity gets nuked the moment
 * the rocket leaves the player's vicinity. We {@link Redirect} the
 * {@code canInteractWith} call inside {@code onUpdate} and force-return
 * {@code true} for players in
 * {@link RocketInventoryHelper#canPlayerBypassInvChecks(EntityPlayer)}.</p>
 *
 * <p>Mirrors the original ASM injection: ASM inserted an extra
 * {@code IFEQ} jump past the close-screen block when
 * {@code allowAccess(player)} said the player was in the bypass set;
 * forcing {@code canInteractWith} to return {@code true} produces the same
 * {@code ifne 199} branch in {@code EntityPlayer.onUpdate}, skipping
 * {@code closeScreen()}.</p>
 *
 * <p>Replaces the equivalent {@code IClassTransformer} hook formerly in
 * {@code asm/ClassTransformer.java} (the EntityPlayer half — the
 * EntityPlayerMP half is covered by {@link MixinEntityPlayerMPInventoryAccess}).</p>
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayerInventoryAccess {

    @Redirect(method = "onUpdate",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/inventory/Container;"
                            + "canInteractWith(Lnet/minecraft/entity/player/EntityPlayer;)Z"))
    private boolean ar$bypassForRocketGui(Container container, EntityPlayer player) {
        return RocketInventoryHelper.shouldAllowContainerInteract(container, player);
    }
}
