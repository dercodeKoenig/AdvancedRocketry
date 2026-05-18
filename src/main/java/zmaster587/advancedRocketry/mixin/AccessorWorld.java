package zmaster587.advancedRocketry.mixin;

import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor exposing {@link World#worldInfo} (SRG {@code field_72986_A}) so
 * {@link zmaster587.advancedRocketry.world.weather.PlanetWeatherManager} can swap
 * the per-world {@link WorldInfo} reference in place — the only safe way to wrap
 * vanilla weather without subclassing or replacing {@code WorldServerMulti}.
 *
 * <p>The field is protected in vanilla, so the accessor is the cleanest path
 * (no AT widening, no reflection).</p>
 */
@Mixin(World.class)
public interface AccessorWorld {

    @Accessor("worldInfo")
    WorldInfo ar$getWorldInfo();

    @Accessor("worldInfo")
    void ar$setWorldInfo(WorldInfo worldInfo);
}
