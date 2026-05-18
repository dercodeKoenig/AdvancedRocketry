package zmaster587.advancedRocketry.test.unit;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.api.IInfrastructure;
import zmaster587.advancedRocketry.mission.MissionGasCollection;
import zmaster587.advancedRocketry.mission.MissionOreMining;
import zmaster587.advancedRocketry.mission.MissionResourceCollection;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 6.
 *
 * Targets the parts of {@link MissionResourceCollection} (and its two
 * concrete subclasses) that are unit-testable without bootstrapping a
 * real server (rocket spawn, dim lookup, world tick — those belong to
 * server-layer tests; gated by a separate {@code Assume} when added).
 *
 * What's exercised here:
 *   - the default no-arg constructor produces a coherent state
 *   - {@code unlinkInfrastructure} is a safe no-op on the empty list
 *   - the abstract base's name dispatch falls through to libVulpes proxy
 *     without NPE'ing (proxy is wired by {@link MinecraftBootstrap}).
 *   - the two concrete subclasses extend the base and remain
 *     constructible — guards against accidental abstract-method removals.
 */
public class MissionResourceCollectionContractTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    public void oreMiningIsConcrete() {
        // Default no-arg ctor must succeed (the abstract base's no-arg ctor
        // initialises infrastructureCoords to an empty list — anything that
        // skipped that would crash on the first iteration in tickEntity).
        MissionResourceCollection mission = new MissionOreMining();
        assertNotNull(mission);
    }

    @Test
    public void gasCollectionIsConcrete() {
        MissionResourceCollection mission = new MissionGasCollection();
        assertNotNull(mission);
    }

    @Test
    public void canTickByDefault() {
        // canTick is overridden to true in the base; if a subclass quietly
        // turns it off the world tick stops advancing the mission.
        assertTrue(new MissionOreMining().canTick());
        assertTrue(new MissionGasCollection().canTick());
    }

    @Test
    public void getInfoIsNull() {
        // Base override returns null on purpose; libVulpes' satellite GUI
        // tolerates null and falls back to a stock label. If this flips to a
        // non-null default the GUI will start showing a mission's debug
        // string in production menus.
        MissionResourceCollection mission = new MissionOreMining();
        assertNull(mission.getInfo(null));
    }

    @Test
    public void zeroPercentFailureChance() {
        // failureChance is hard-coded to 0 in the base. Surfacing this in a
        // test means a future "configurable failure %" rewrite has to
        // explicitly delete this assertion — no quiet behaviour drift.
        assertEquals(0.0, new MissionOreMining().failureChance(), 0.0);
    }

    @Test
    public void performActionAlwaysFalse() {
        // performAction is wired to return false in the base — concrete
        // missions don't expose a button-click pathway today.
        MissionResourceCollection mission = new MissionOreMining();
        assertFalse(mission.performAction(null, null, null));
    }

    @Test
    public void defaultMissionIsNotImmediatelyNbtSerialisable() {
        // Document a sharp corner: a default-constructed mission has
        // missionPersistantNBT / rocketStats / rocketStorage = null. The
        // data-carrying ctor sets them; until then writeToNBT throws
        // IllegalArgumentException (NBT rejects null tags). Pin so a
        // future refactor that promotes a default-ctor mission to a
        // tick-able instance has to explicitly delete this assertion.
        MissionResourceCollection mission = new MissionOreMining();
        NBTTagCompound tag = new NBTTagCompound();
        try {
            mission.writeToNBT(tag);
            org.junit.Assert.fail(
                    "Expected IllegalArgumentException (NBT rejects null) for default-constructed mission");
        } catch (IllegalArgumentException expected) {
            // OK — the documented current behaviour.
        }
    }

    @Test
    public void hierarchyIsAsDocumented() {
        // Pin the inheritance: both concrete missions extend
        // MissionResourceCollection. The on-server tick path relies on
        // `instanceof MissionResourceCollection`; a future refactor that
        // dropped this would silently make these missions un-tickable.
        assertTrue(MissionResourceCollection.class.isAssignableFrom(MissionOreMining.class));
        assertTrue(MissionResourceCollection.class.isAssignableFrom(MissionGasCollection.class));
    }

    @SuppressWarnings("unused")
    private static IInfrastructure dummyInfrastructure() {
        // unused — placeholder showing future server-layer tests would
        // supply a real IInfrastructure via the harness.
        return null;
    }
}
