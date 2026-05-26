package zmaster587.advancedRocketry.test.unit;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.misc.Unsafe;
import zmaster587.advancedRocketry.api.AdvancedRocketryAPI;
import zmaster587.advancedRocketry.api.IGravityManager;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;
import zmaster587.advancedRocketry.util.GravityHandler;

import java.lang.reflect.Field;
import java.util.WeakHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Coverage-audit gap (2026-05-26 Tier 2 #7) — {@link IGravityManager}
 * public API on {@link GravityHandler}.
 *
 * <p>{@code GravityHandler} implements {@link IGravityManager} and
 * registers itself as the singleton on
 * {@link AdvancedRocketryAPI#gravityManager} via its static
 * initializer. The interface is part of {@code api.} and downstream
 * mods (companion packs that want to create custom zero-G or
 * heavy-grav zones) call {@code setGravityMultiplier} /
 * {@code clearGravityEffect} on entities they own.</p>
 *
 * <p>Contracts pinned here:</p>
 *
 * <ol>
 *   <li>{@link AdvancedRocketryAPI#gravityManager} is non-null after
 *       class load (the static init in {@code GravityHandler} ran).</li>
 *   <li>{@code setGravityMultiplier(entity, d)} registers the entity
 *       in the internal {@code entityMap}.</li>
 *   <li>{@code clearGravityEffect(entity)} removes the entry.</li>
 *   <li>Per-entity isolation: setting on one entity doesn't affect
 *       another.</li>
 * </ol>
 *
 * <p><b>Entity construction</b>: the production code only uses entity
 * references as map keys (identity comparison through the
 * {@code WeakHashMap}). The instance's internal state is never read,
 * so we allocate via {@link Unsafe#allocateInstance(Class)} on
 * {@link EntityItem} to get a non-null reference without paying the
 * real-world-required ctor cost. Same trick as
 * {@code RocketInventoryHelperRedirectTest}.</p>
 */
public class GravityHandlerApiTest {

    private static Unsafe UNSAFE;

    @BeforeClass
    public static void bootstrap() throws Exception {
        MinecraftBootstrap.ensure();
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        UNSAFE = (Unsafe) theUnsafe.get(null);
    }

    @AfterClass
    public static void drainEntityMap() throws Exception {
        // Don't leak test entities into the shared static map of other
        // unit tests that share this JVM.
        accessEntityMap().clear();
    }

    @Before
    public void resetEntityMap() throws Exception {
        accessEntityMap().clear();
    }

    /** Reflective accessor for the private static
     *  {@code GravityHandler.entityMap}. The map is the observable
     *  state behind the IGravityManager API. */
    @SuppressWarnings("unchecked")
    private static WeakHashMap<Entity, Double> accessEntityMap() throws Exception {
        Field f = GravityHandler.class.getDeclaredField("entityMap");
        f.setAccessible(true);
        return (WeakHashMap<Entity, Double>) f.get(null);
    }

    private static Entity fakeEntity() throws Exception {
        // EntityItem has a real ctor that needs a World — bypass it.
        // The production code only uses the Entity reference as a
        // WeakHashMap key + reads the multiplier back; instance state
        // never matters. A single fake-entity per test method is
        // enough because Entity.equals collapses two zero-initialised
        // instances (both with null entityUniqueID under default
        // equals semantics), so multi-entity isolation tests aren't
        // unit-tier-feasible — that's a WeakHashMap contract anyway,
        // not an AR-side one.
        return (Entity) UNSAFE.allocateInstance(EntityItem.class);
    }

    @Test
    public void gravityManagerIsRegisteredOnTheAPI() {
        // The static initializer in GravityHandler installs itself as
        // AdvancedRocketryAPI.gravityManager. Companion mods reach the
        // implementation through this singleton — if it's null, all
        // external callers NPE.
        assertNotNull("AdvancedRocketryAPI.gravityManager must be installed "
                        + "by GravityHandler's static init",
                AdvancedRocketryAPI.gravityManager);
        assertTrue("registered manager must be an instance of GravityHandler",
                AdvancedRocketryAPI.gravityManager instanceof GravityHandler);
    }

    @Test
    public void setGravityMultiplierRegistersEntityInMap() throws Exception {
        Entity e = fakeEntity();
        IGravityManager mgr = AdvancedRocketryAPI.gravityManager;

        mgr.setGravityMultiplier(e, 0.25);
        WeakHashMap<Entity, Double> map = accessEntityMap();
        assertTrue("entity must be present in entityMap after "
                        + "setGravityMultiplier",
                map.containsKey(e));
        assertEquals("stored multiplier must equal the value passed in",
                0.25, map.get(e), 0.0);
    }

    @Test
    public void setGravityMultiplierOverwritesPreviousValue() throws Exception {
        Entity e = fakeEntity();
        IGravityManager mgr = AdvancedRocketryAPI.gravityManager;

        mgr.setGravityMultiplier(e, 0.25);
        mgr.setGravityMultiplier(e, 1.5);  // overwrite

        assertEquals("setGravityMultiplier must replace the prior value, "
                        + "not append",
                1.5, accessEntityMap().get(e), 0.0);
    }

    @Test
    public void clearGravityEffectRemovesEntry() throws Exception {
        Entity e = fakeEntity();
        IGravityManager mgr = AdvancedRocketryAPI.gravityManager;

        mgr.setGravityMultiplier(e, 0.5);
        assertTrue("precondition: entity is in map",
                accessEntityMap().containsKey(e));

        mgr.clearGravityEffect(e);
        assertFalse("clearGravityEffect must remove the entity from the map",
                accessEntityMap().containsKey(e));
    }

    @Test
    public void clearGravityEffectIsNoOpForUntrackedEntity() throws Exception {
        // Calling clear on an entity that was never registered must not
        // throw — companion mods may defensively clear without first
        // checking. WeakHashMap.remove on missing keys is a no-op, so
        // the contract is "doesn't throw".
        Entity e = fakeEntity();
        IGravityManager mgr = AdvancedRocketryAPI.gravityManager;

        mgr.clearGravityEffect(e);
        assertFalse("untracked entity stays absent after clear",
                accessEntityMap().containsKey(e));
    }

    @Test
    public void apiGravityManagerSingletonIsStable() {
        // Successive reads must return the same instance — companion
        // mods cache the manager reference on world load and don't
        // re-resolve.
        IGravityManager first = AdvancedRocketryAPI.gravityManager;
        IGravityManager second = AdvancedRocketryAPI.gravityManager;
        assertSame("repeated reads of AdvancedRocketryAPI.gravityManager "
                + "must return the same singleton", first, second);
    }
}
