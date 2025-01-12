package zmaster587.advancedRocketry.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;
import zmaster587.advancedRocketry.ARHookLoader;
import zmaster587.advancedRocketry.repack.gloomyfolken.hooklib.minecraft.HookLoader;

import java.util.Map;

@TransformerExclusions(value = {"zmaster587.advancedRocketry.asm.ClassTransformer"})
@MCVersion("1.12.2")
public class AdvancedRocketryPlugin implements IFMLLoadingPlugin {

    private final HookLoader hookLoader;

    public AdvancedRocketryPlugin() {
        hookLoader = new ARHookLoader();
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
