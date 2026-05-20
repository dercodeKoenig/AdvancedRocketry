package zmaster587.advancedRocketry.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityTNTPrimed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zmaster587.advancedRocketry.util.GravityHandler;

/**
 * Applies AR's per-dimension gravity multiplier to entities that otherwise
 * would not pick it up.
 *
 * <p>Vanilla {@link Entity#onUpdate()} is the natural place — but
 * {@link EntityFallingBlock}, {@link EntityMinecart} and
 * {@link EntityTNTPrimed} override {@code onUpdate} without calling
 * {@code super.onUpdate()}, so the base-class injection alone would not
 * propagate. Hence the multi-target {@link Mixin} list.</p>
 *
 * <p>Replaces the equivalent {@code IClassTransformer} hook formerly in
 * {@code asm/ClassTransformer.java}.</p>
 */
@Mixin({Entity.class, EntityFallingBlock.class, EntityMinecart.class, EntityTNTPrimed.class})
public abstract class MixinEntityGravity {

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void ar$applyGravity(CallbackInfo ci) {
        GravityHandler.applyGravity((Entity) (Object) this);
    }
}
