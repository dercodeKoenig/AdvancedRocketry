package zmaster587.advancedRocketry.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

@MCVersion("1.12.2")
public class AdvancedRocketryPlugin implements IFMLLoadingPlugin {

    public AdvancedRocketryPlugin() {
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
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return "zmaster587.advancedRocketry.asm.ModContainer";
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
