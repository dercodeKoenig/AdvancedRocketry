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
        // ARHooks was a HookLib hook container holding 4 weather-motivated
        // ASM patches. All four moved to Mixin under mixins.advancedrocketry
        // .json (MixinPlayerList, MixinWorldServerMulti, MixinWorldServer)
        // and the container class itself was deleted; calling
        // registerHookContainer against a missing class NPEs inside
        // HookContainerParser at boot. The PrimaryClassTransformer (still
        // used for non-weather AR ASM) is registered via
        // getASMTransformerClass above, so it remains active.
    }
}
