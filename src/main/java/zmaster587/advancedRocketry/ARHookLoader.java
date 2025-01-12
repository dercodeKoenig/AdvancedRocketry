package zmaster587.advancedRocketry;


import zmaster587.advancedRocketry.repack.gloomyfolken.hooklib.minecraft.HookLoader;
import zmaster587.advancedRocketry.repack.gloomyfolken.hooklib.minecraft.PrimaryClassTransformer;

public class ARHookLoader extends HookLoader {

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{PrimaryClassTransformer.class.getName()};
    }

    @Override
    public void registerHooks() {
        registerHookContainer("zmaster587.advancedRocketry.ARHooks");
    }
}
