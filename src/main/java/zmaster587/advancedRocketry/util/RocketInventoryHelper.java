package zmaster587.advancedRocketry.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.WeakHashMap;

public class RocketInventoryHelper {

    /**
     * Decides whether the vanilla {@code openContainer.canInteractWith}
     * check inside {@code EntityPlayer(MP).onUpdate} should be force-skipped
     * for a given player. Returns {@code true} (i.e. "behave as if the
     * container is in interaction range") when the player is currently in
     * the rocket-inventory bypass set; otherwise delegates to vanilla's
     * own check.
     *
     * <p>Extracted so the
     * {@code MixinEntityPlayer(MP)InventoryAccess @Redirect}
     * bodies stay one line and the redirect's semantics are unit-testable
     * without running the full Mixin pipeline. The mixin redirects to this
     * helper; this helper is the single source of truth for "should AR
     * keep the rocket inventory GUI open past the vanilla distance gate".
     * </p>
     *
     * @param container the container vanilla was about to {@code
     *                  canInteractWith}-check (never {@code null} on the
     *                  vanilla call site — the {@code openContainer != null}
     *                  guard fires first).
     * @param player    the player whose {@code onUpdate} tick is running.
     * @return {@code true} when AR's bypass set says yes (skips
     *         close-screen path); otherwise the container's own
     *         {@code canInteractWith} result.
     */
    public static boolean shouldAllowContainerInteract(Container container, EntityPlayer player) {
        if (canPlayerBypassInvChecks(player)) {
            return true;
        }
        return container.canInteractWith(player);
    }

    //TODO: more robust way of inv checking
    //Has weak refs so if the player gets killed/logsout etc the entry doesnt stay trapped in RAM
    private static HashSet<WeakReference<EntityPlayer>> inventoryCheckPlayerBypassMap = new HashSet<>();
    private static WeakHashMap<EntityPlayer, Long> inventoryTimingMap = new WeakHashMap<>();
    private static WeakHashMap<EntityPlayer, BlockPos> inventoryDismapping = new WeakHashMap<>();

    //TODO: check for rocket
    public static boolean allowAccess(Object tile) {
        EntityPlayer player = (EntityPlayer) tile;


        //If a small amount of time is passed since interfacing with the rocket and the player has moved then assume the player is no longer accessing the rocket
        //and possibly trying to abuse AR to circumvent inv checks
        if (inventoryTimingMap.containsKey(player)) {
            if (inventoryTimingMap.get(player) + 10 < player.world.getTotalWorldTime() &&
                    inventoryDismapping.get(player).getDistance(player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ()) > 3)
                removePlayerFromInventoryBypass(player);
        }
        //else
        //	removePlayerFromInventoryBypass(player);

        //return !player.worldObj.getEntitiesWithinAABB(EntityRocketBase.class, new AxisAlignedBB(player.getPosition().add(-64,-64,-64), player.getPosition().add(64,64,64))).isEmpty();

        return !canPlayerBypassInvChecks((EntityPlayer) tile);
    }

    public static boolean canPlayerBypassInvChecks(EntityPlayer player) {
        for (WeakReference<EntityPlayer> player2 : inventoryCheckPlayerBypassMap) {
            if (player2.get() == player)
                return true;
        }
        return false;
    }

    public static void removePlayerFromInventoryBypass(EntityPlayer player) {

        inventoryCheckPlayerBypassMap.removeIf(player2 -> player2.get() == player || player2.get() == null);
    }

    public static void addPlayerToInventoryBypass(EntityPlayer player) {
        inventoryCheckPlayerBypassMap.add(new WeakReference<>(player));
    }

    public static void updateTime(EntityPlayer entity, long worldTime) {
        inventoryTimingMap.put(entity, worldTime);
        inventoryDismapping.put(entity, entity.getPosition());
    }
}
