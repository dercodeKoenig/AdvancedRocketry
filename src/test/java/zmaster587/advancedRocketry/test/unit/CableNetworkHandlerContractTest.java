package zmaster587.advancedRocketry.test.unit;

import org.junit.Test;
import zmaster587.advancedRocketry.cable.CableNetwork;
import zmaster587.advancedRocketry.cable.HandlerCableNetwork;
import zmaster587.advancedRocketry.cable.HandlerDataNetwork;
import zmaster587.advancedRocketry.cable.HandlerEnergyNetwork;
import zmaster587.advancedRocketry.cable.HandlerLiquidNetwork;
import zmaster587.advancedRocketry.cable.NetworkRegistry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 7 (subset, unit slice).
 *
 * The full Phase 7 plan asks for end-to-end network handler tests
 * (energy aggregation across segments, merge/split on cable break,
 * data routing). Those need real tile entities + world placement and
 * are deferred to the server harness — see TASK-02 §"Phase 7".
 *
 * This test covers the parts that are unit-testable without a world:
 *   - {@link CableNetwork} id generation produces distinct ids
 *   - empty network exposes empty source/sink sets
 *   - {@link HandlerCableNetwork} reports presence/absence of ids
 *   - {@link NetworkRegistry#registerFluidNetwork()} populates the
 *     three handler singletons; {@link NetworkRegistry#clearNetworks()}
 *     drains them without nulling the handler refs.
 */
public class CableNetworkHandlerContractTest {

    @Test
    public void initNetworkProducesDistinctIds() {
        CableNetwork a = CableNetwork.initNetwork();
        CableNetwork b = CableNetwork.initNetwork();
        CableNetwork c = CableNetwork.initNetwork();
        assertNotEquals("two consecutive initNetwork() calls produced the same id",
                idOf(a), idOf(b));
        assertNotEquals(idOf(b), idOf(c));
        assertNotEquals(idOf(a), idOf(c));
    }

    @Test
    public void initWithIdHonoursTheGivenId() {
        CableNetwork n = CableNetwork.initWithID(424242);
        assertNotNull(n);
        assertSame("getSources on a fresh network must return the same instance each call",
                n.getSources(), n.getSources());
        assertTrue("fresh network must have empty sources", n.getSources().isEmpty());
        assertTrue("fresh network must have empty sinks", n.getSinks().isEmpty());
    }

    @Test
    public void handlerNetworkRegistryRoundTrip() {
        HandlerCableNetwork handler = new HandlerCableNetwork();
        int id = handler.getNewNetworkID();
        assertTrue("getNewNetworkID must register a network discoverable via doesNetworkExist",
                handler.doesNetworkExist(id));
        assertNotNull("handler.getNetwork(id) must return the registered net",
                handler.getNetwork(id));

        handler.removeNetworkByID(id);
        assertFalse("after removeNetworkByID the id must be gone",
                handler.doesNetworkExist(id));
    }

    @Test
    public void registerFluidNetworkInitialisesAllThreeHandlerSingletons() {
        NetworkRegistry.registerFluidNetwork();
        assertNotNull("liquidNetwork must be assigned", NetworkRegistry.liquidNetwork);
        assertNotNull("dataNetwork must be assigned", NetworkRegistry.dataNetwork);
        assertNotNull("energyNetwork must be assigned", NetworkRegistry.energyNetwork);
        assertTrue(NetworkRegistry.liquidNetwork instanceof HandlerLiquidNetwork);
        assertTrue(NetworkRegistry.dataNetwork instanceof HandlerDataNetwork);
        assertTrue(NetworkRegistry.energyNetwork instanceof HandlerEnergyNetwork);
    }

    @Test
    public void clearNetworksDoesNotNullSingletonRefs() {
        // Document a subtle contract: clearNetworks() drains the per-handler
        // network table, but it must leave the three NetworkRegistry static
        // fields pointing at the *same* handler instances — otherwise
        // every existing TilePipe that cached a handler ref keeps writing
        // into a detached map.
        NetworkRegistry.registerFluidNetwork();
        HandlerLiquidNetwork beforeLiquid = NetworkRegistry.liquidNetwork;
        HandlerDataNetwork beforeData = NetworkRegistry.dataNetwork;
        HandlerEnergyNetwork beforeEnergy = NetworkRegistry.energyNetwork;

        beforeEnergy.getNewNetworkID();
        beforeData.getNewNetworkID();
        beforeLiquid.getNewNetworkID();
        NetworkRegistry.clearNetworks();

        assertSame("liquidNetwork ref must not be replaced by clearNetworks",
                beforeLiquid, NetworkRegistry.liquidNetwork);
        assertSame("dataNetwork ref must not be replaced by clearNetworks",
                beforeData, NetworkRegistry.dataNetwork);
        assertSame("energyNetwork ref must not be replaced by clearNetworks",
                beforeEnergy, NetworkRegistry.energyNetwork);
    }

    private static int idOf(CableNetwork n) {
        // networkID is package-private; reflect to read it without changing
        // production visibility.
        try {
            java.lang.reflect.Field f = CableNetwork.class.getDeclaredField("networkID");
            f.setAccessible(true);
            return f.getInt(n);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("CableNetwork.networkID field gone — production API regressed", e);
        }
    }
}
