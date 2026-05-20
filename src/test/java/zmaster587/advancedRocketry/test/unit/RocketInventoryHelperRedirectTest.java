package zmaster587.advancedRocketry.test.unit;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.misc.Unsafe;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;
import zmaster587.advancedRocketry.util.RocketInventoryHelper;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-08-mixin Phase 3 — unit pin for the inventory-bypass redirect
 * logic that the mixins
 * ({@code MixinEntityPlayer(MP)InventoryAccess}) delegate to.
 *
 * <p>The mixins are one-liners that call
 * {@link RocketInventoryHelper#shouldAllowContainerInteract}, so a unit
 * test of that helper is the actual behavioural pin for both redirects.
 * This avoids needing a real {@code EntityPlayer} GUI session (which is
 * the constraint that pushed an end-to-end pin into TASK-10b /
 * testClient e2e).</p>
 *
 * <h2>What's pinned</h2>
 * <ol>
 *   <li>Bypass set member → return {@code true}, container.canInteractWith
 *       MUST NOT be invoked (vanilla close-screen path is skipped
 *       outright).</li>
 *   <li>Non-bypass-set player → delegates to
 *       {@code container.canInteractWith(player)} verbatim, both true and
 *       false outcomes propagate.</li>
 * </ol>
 *
 * <h2>How EntityPlayer is faked</h2>
 *
 * <p>{@link Unsafe#allocateInstance} returns a zero-initialised
 * {@link EntityPlayer} reference. The bypass map only does identity
 * comparison via {@code WeakReference.get() == player} — no
 * {@code EntityPlayer} method is invoked on the value, so the
 * uninitialised instance is safe as a marker object. The same trick is
 * used by other MC unit tests in this tree (see
 * {@code MinecraftBootstrap} usage above).</p>
 */
public class RocketInventoryHelperRedirectTest {

    private static Unsafe UNSAFE;

    @BeforeClass
    public static void setupBootstrap() throws Exception {
        MinecraftBootstrap.ensure();
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        UNSAFE = (Unsafe) theUnsafe.get(null);
    }

    @AfterClass
    public static void clearBypassMap() throws Exception {
        // Sanitise the static bypass map between test classes so this
        // file's reflection-based inserts don't leak into other unit
        // tests that share the JVM.
        Field f = RocketInventoryHelper.class
                .getDeclaredField("inventoryCheckPlayerBypassMap");
        f.setAccessible(true);
        ((HashSet<?>) f.get(null)).clear();
    }

    @Before
    public void resetBypassMap() throws Exception {
        clearBypassMap();
    }

    private static EntityPlayer fakePlayer() throws InstantiationException {
        // EntityPlayer is abstract; allocate a concrete EntityPlayerMP via
        // Unsafe (skips the ctor, so no NetworkManager / GameProfile /
        // PlayerInteractionManager required).
        return (EntityPlayer) UNSAFE.allocateInstance(EntityPlayerMP.class);
    }

    private static Container recordingContainer(AtomicInteger calls, boolean retval) {
        return new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                calls.incrementAndGet();
                return retval;
            }
        };
    }

    @Test
    public void bypassPlayerSkipsCanInteractWithRegardlessOfDistance() throws Exception {
        EntityPlayer player = fakePlayer();
        RocketInventoryHelper.addPlayerToInventoryBypass(player);
        AtomicInteger calls = new AtomicInteger();
        // If the redirect helper consults the container, our recording
        // stub flips calls > 0. Pinning calls==0 proves the bypass branch
        // short-circuits — i.e. the MC close-screen block is skipped.
        boolean allowed = RocketInventoryHelper.shouldAllowContainerInteract(
                recordingContainer(calls, /* canInteractWith */ false), player);
        assertTrue("bypass player must keep container open", allowed);
        assertEquals("container.canInteractWith must NOT be consulted "
                + "for a bypass player", 0, calls.get());
    }

    @Test
    public void nonBypassPlayerDelegatesToContainerCanInteractWithTrue() throws Exception {
        EntityPlayer player = fakePlayer();
        AtomicInteger calls = new AtomicInteger();
        boolean allowed = RocketInventoryHelper.shouldAllowContainerInteract(
                recordingContainer(calls, /* canInteractWith */ true), player);
        assertTrue("non-bypass + canInteractWith=true must allow", allowed);
        assertEquals("container.canInteractWith MUST be invoked exactly once",
                1, calls.get());
    }

    @Test
    public void nonBypassPlayerDelegatesToContainerCanInteractWithFalse() throws Exception {
        EntityPlayer player = fakePlayer();
        AtomicInteger calls = new AtomicInteger();
        boolean allowed = RocketInventoryHelper.shouldAllowContainerInteract(
                recordingContainer(calls, /* canInteractWith */ false), player);
        assertFalse("non-bypass + canInteractWith=false must close", allowed);
        assertEquals("container.canInteractWith MUST be invoked exactly once",
                1, calls.get());
    }

    @Test
    public void removingPlayerFromBypassRestoresVanillaSemantics() throws Exception {
        EntityPlayer player = fakePlayer();
        RocketInventoryHelper.addPlayerToInventoryBypass(player);
        assertTrue(RocketInventoryHelper.canPlayerBypassInvChecks(player));
        RocketInventoryHelper.removePlayerFromInventoryBypass(player);
        assertFalse(RocketInventoryHelper.canPlayerBypassInvChecks(player));

        // After removal, the helper must defer to container.canInteractWith
        // exactly as it would for a player that was never added.
        AtomicInteger calls = new AtomicInteger();
        boolean allowed = RocketInventoryHelper.shouldAllowContainerInteract(
                recordingContainer(calls, /* canInteractWith */ false), player);
        assertFalse(allowed);
        assertEquals(1, calls.get());
    }

    @Test
    public void bypassIsScopedToTheSpecificPlayerInstance() throws Exception {
        EntityPlayer p1 = fakePlayer();
        EntityPlayer p2 = fakePlayer();
        RocketInventoryHelper.addPlayerToInventoryBypass(p1);

        assertTrue("p1 is in bypass", RocketInventoryHelper.canPlayerBypassInvChecks(p1));
        assertFalse("p2 must NOT inherit p1's bypass",
                RocketInventoryHelper.canPlayerBypassInvChecks(p2));

        AtomicInteger calls = new AtomicInteger();
        boolean p2Allowed = RocketInventoryHelper.shouldAllowContainerInteract(
                recordingContainer(calls, /* canInteractWith */ false), p2);
        assertFalse("p2 must take vanilla path", p2Allowed);
        assertEquals("p2 must consult the container", 1, calls.get());
    }
}
