package zmaster587.advancedRocketry.mixin;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.storage.ISaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zmaster587.advancedRocketry.world.weather.PlanetWeatherManager;

/**
 * Primary B1 wrap point. After every secondary {@link WorldServerMulti}
 * constructor completes, ask the weather manager whether this dimension is an
 * AR planet that wants its own vanilla weather; if so, replace the freshly
 * installed {@link net.minecraft.world.storage.DerivedWorldInfo} with our
 * {@code ARWeatherWorldInfo} wrapper.
 *
 * <p>The provider may not yet be ready at constructor RETURN — the manager
 * tolerates that and skips. The {@link net.minecraftforge.event.world.WorldEvent.Load}
 * fallback handled by {@code PlanetWeatherEventHandler} catches the dimensions
 * we miss here.</p>
 */
@Mixin(WorldServerMulti.class)
public abstract class MixinWorldServerMulti {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ar$wrapWeatherWorldInfo(
            MinecraftServer server,
            ISaveHandler saveHandlerIn,
            int dimensionId,
            WorldServer delegate,
            Profiler profilerIn,
            CallbackInfo ci) {
        // The mixin runtime composes the implicit `this` into the target class
        // (WorldServerMulti). Cast through Object to satisfy javac.
        WorldServer self = (WorldServer) (Object) this;
        PlanetWeatherManager.wrapWorldInfoIfNeeded(self);
    }
}
