package zmaster587.advancedRocketry.repack.gloomyfolken.hooklib.minecraft;

import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.Loader;
import zmaster587.advancedRocketry.repack.gloomyfolken.hooklib.asm.Hook;

public class SecondaryTransformerHook {

    /**
     * Регистрирует хук-трансформер последним.
     */
    @Hook
    public static void injectData(Loader loader, Object... data) {
        ClassLoader classLoader = SecondaryTransformerHook.class.getClassLoader();
        if (classLoader instanceof LaunchClassLoader) {
            ((LaunchClassLoader)classLoader).registerTransformer(MinecraftClassTransformer.class.getName());
        } else {
            System.out.println("HookLib was not loaded by LaunchClassLoader. Hooks will not be injected.");
        }
    }

}
