package zmaster587.advancedRocketry.mixin;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;

/**
 * Notifies the per-world {@link AtmosphereHandler} after every successful
 * {@link World#setBlockState(BlockPos, IBlockState, int)} so atmosphere
 * volumes recompute when an air-tight boundary block is placed/broken.
 *
 * <p>Hook fires on RETURN (after vanilla has updated the chunk + lighting),
 * matching the original ASM injection that was placed immediately before
 * the {@code IRETURN}.</p>
 *
 * <p>Replaces the equivalent {@code IClassTransformer} hook formerly in
 * {@code asm/ClassTransformer.java}.</p>
 */
@Mixin(World.class)
public abstract class MixinWorldSetBlockState {

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;I)Z",
            at = @At("RETURN"))
    private void ar$notifyAtmosphere(BlockPos pos, IBlockState newState, int flags,
                                     CallbackInfoReturnable<Boolean> cir) {
        AtmosphereHandler.onBlockChange((World) (Object) this, pos);
    }
}
