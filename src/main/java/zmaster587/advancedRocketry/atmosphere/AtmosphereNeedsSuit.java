package zmaster587.advancedRocketry.atmosphere;

import matteroverdrive.entity.player.MOPlayerCapabilityProvider;
import matteroverdrive.init.OverdriveBioticStats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import zmaster587.advancedRocketry.api.EntityRocketBase;
import zmaster587.advancedRocketry.api.capability.CapabilitySpaceArmor;
import zmaster587.advancedRocketry.entity.EntityElevatorCapsule;
import zmaster587.advancedRocketry.util.ItemAirUtils;

import javax.annotation.Nonnull;

public class AtmosphereNeedsSuit extends AtmosphereType {

    public AtmosphereNeedsSuit(boolean canTick, boolean isBreathable, boolean allowsCombustion, String name) {
        super(canTick, isBreathable, allowsCombustion, name);
    }

    // True if only a helmet is needed
    protected boolean onlyNeedsMask() {
        return allowsCombustion();
    }

    @Override
    public boolean isImmune(EntityLivingBase player) {

        if(Loader.isModLoaded("matteroverdrive")) {
            if (MOPlayerCapabilityProvider.GetAndroidCapability(player) != null
                    && MOPlayerCapabilityProvider.GetAndroidCapability(player).isAndroid()
                    && MOPlayerCapabilityProvider.GetAndroidCapability(player).isUnlocked(OverdriveBioticStats.oxygen, 1)) return true;
        }
        //Checks if player is wearing spacesuit or anything that extends ItemSpaceArmor

        ItemStack feet = player.getItemStackFromSlot(EntityEquipmentSlot.FEET);
        ItemStack leg = player.getItemStackFromSlot(EntityEquipmentSlot.LEGS /*so hot you can fry an egg*/);
        ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        ItemStack helm = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);

        // Note: protectsFrom(chest) is intentionally the last thing to check here.  This is because java will bail on the check early if others fail
        // this will prevent the O2 level in the chest from being needlessly decremented
        return (player instanceof EntityPlayer && ((((EntityPlayer) player).capabilities.isCreativeMode) || ((EntityPlayer) player).isSpectator()))
                || player.getRidingEntity() instanceof EntityRocketBase || player.getRidingEntity() instanceof EntityElevatorCapsule ||
                (((!onlyNeedsMask() && protectsFrom(leg) && protectsFrom(feet)) || onlyNeedsMask()) && protectsFrom(helm) && protectsFrom(chest));
    }

    protected boolean protectsFrom(@Nonnull ItemStack stack) {
        return (ItemAirUtils.INSTANCE.isStackValidAirContainer(stack) && new ItemAirUtils.ItemAirWrapper(stack).protectsFromSubstance(this, stack, true)) || (!stack.isEmpty() && stack.hasCapability(CapabilitySpaceArmor.PROTECTIVEARMOR, null) &&
                stack.getCapability(CapabilitySpaceArmor.PROTECTIVEARMOR, null).protectsFromSubstance(this, stack, true));
    }

}
