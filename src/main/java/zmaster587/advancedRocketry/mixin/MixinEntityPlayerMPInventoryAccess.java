package zmaster587.advancedRocketry.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zmaster587.advancedRocketry.util.RocketInventoryHelper;

/**
 * Server-side twin of {@link MixinEntityPlayerInventoryAccess} —
 * {@link EntityPlayerMP#onUpdate()} owns an independent
 * {@code openContainer.canInteractWith(this)} check (it does not delegate
 * to {@link EntityPlayer#onUpdate()} for this guard), so the redirect must
 * be installed on both classes.
 *
 * <p>See {@link MixinEntityPlayerInventoryAccess} for the full rationale.</p>
 */
@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMPInventoryAccess {

    @Redirect(method = "onUpdate",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/inventory/Container;"
                            + "canInteractWith(Lnet/minecraft/entity/player/EntityPlayer;)Z"))
    private boolean ar$bypassForRocketGui(Container container, EntityPlayer player) {
        return RocketInventoryHelper.shouldAllowContainerInteract(container, player);
    }
}
