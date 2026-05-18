package zmaster587.advancedRocketry.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import zmaster587.advancedRocketry.ARHookLoader;
import zmaster587.advancedRocketry.repack.gloomyfolken.hooklib.minecraft.HookLoader;

import java.util.Map;

@TransformerExclusions(value = {"zmaster587.advancedRocketry.asm.ClassTransformer"})
@MCVersion("1.12.2")
public class AdvancedRocketryPlugin implements IFMLLoadingPlugin {

    private final HookLoader hookLoader;

    public AdvancedRocketryPlugin() {
        hookLoader = new ARHookLoader();
        // Register our mixin config programmatically. In a packaged production
        // jar this is also declared via the `MixinConfigs` manifest attribute
        // (set by tasks.jar), but in the dev workspace the mod is loaded from
        // build/classes/java/main with no manifest, so MixinBooter would
        // otherwise never see our config. Mixins.addConfiguration is
        // idempotent on the same file name, so the manifest + programmatic
        // paths can both fire harmlessly.
        //
        // MixinBootstrap.init() is also idempotent — MixinBooter has typically
        // run first and called it, but doing it again is a no-op and protects
        // against load-order surprises (e.g. coremod scan reaching us before
        // MixinBooter on some Forge versions).
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.advancedrocketry.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{ClassTransformer.class.getName(), hookLoader.getASMTransformerClass()[0]};
    }

    @Override
    public String getModContainerClass() {
        return "zmaster587.advancedRocketry.asm.ModContainer";
    }

    @Override
    public String getSetupClass() {
        return hookLoader.getSetupClass();
    }

    @Override
    public void injectData(Map<String, Object> data) {
        hookLoader.injectData(data);
    }

    @Override
    public String getAccessTransformerClass() {
        return hookLoader.getAccessTransformerClass();
    }
}
