package zmaster587.advancedRocketry.test;

import net.minecraft.init.Bootstrap;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.common.CommonProxy;
import zmaster587.advancedRocketry.dimension.DimensionManager;

import java.lang.reflect.Field;

/**
 * Idempotent helper that initializes:
 *   1. vanilla Minecraft static registries via {@link Bootstrap#register()};
 *   2. AR's {@code @SidedProxy} field with a plain {@link CommonProxy} instance,
 *      which transitively wires {@code DimensionManager} (its static
 *      {@code dimensionManagerServer} field is eagerly constructed when CommonProxy
 *      is loaded).
 *
 * Mod-specific registries (AR blocks, items, tile entities, packets) and the Forge
 * lifecycle (preInit/init/postInit) are NOT initialized — those require a real
 * Forge mod loader and belong in headless scenario tests under
 * {@code src/test/java/zmaster587/advancedRocketry/test/scenario/}.
 *
 * <p>Usage:</p>
 * <pre>{@code
 *   @BeforeClass public static void bootstrap() { MinecraftBootstrap.ensure(); }
 * }</pre>
 *
 * <p>Calling {@link Bootstrap#register()} multiple times is harmless because MC
 * implementation guards with a flag, but we also de-duplicate here to keep the
 * intent obvious in tests.</p>
 */
public final class MinecraftBootstrap {

    private static volatile boolean done = false;

    private MinecraftBootstrap() {}

    public static void ensure() {
        if (done) return;
        synchronized (MinecraftBootstrap.class) {
            if (done) return;

            // 1. Vanilla MC registries.
            Bootstrap.register();

            // 2. AR proxy. AdvancedRocketry.proxy is null in tests because
            // @SidedProxy is wired by the Forge classloader. Inject a plain
            // CommonProxy so anything that calls AdvancedRocketry.proxy.getXxx()
            // (most notably DimensionProperties.readFromNBT → DimensionManager
            // .getInstance()) has a working dispatch target.
            try {
                Field proxyField = AdvancedRocketry.class.getDeclaredField("proxy");
                proxyField.setAccessible(true);
                if (proxyField.get(null) == null) {
                    proxyField.set(null, new CommonProxy());
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Failed to inject CommonProxy into AdvancedRocketry.proxy — "
                                + "AR test bootstrap broken; check field visibility.",
                        e);
            }

            // 3. Register a deterministic "Sol" star with id=0 so that
            // DimensionProperties.readFromNBT (line ~1646) can resolve
            // DimensionManager.getInstance().getStar(0) without NPE.
            // This mirrors the production world-load path where Sol is the first
            // star registered in DimensionManager.preloadGalaxy.
            if (DimensionManager.getInstance().getStar(0) == null) {
                StellarBody sol = new StellarBody();
                sol.setId(0);
                sol.setName("Sol");
                sol.setTemperature(100);
                DimensionManager.getInstance().addStar(sol);
            }

            done = true;
        }
    }
}
