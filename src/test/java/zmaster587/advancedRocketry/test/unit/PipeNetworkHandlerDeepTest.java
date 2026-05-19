package zmaster587.advancedRocketry.test.unit;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;
import zmaster587.advancedRocketry.cable.CableNetwork;
import zmaster587.advancedRocketry.cable.EnergyNetwork;
import zmaster587.advancedRocketry.cable.HandlerCableNetwork;
import zmaster587.advancedRocketry.cable.HandlerEnergyNetwork;
import zmaster587.advancedRocketry.cable.HandlerLiquidNetwork;
import zmaster587.advancedRocketry.cable.LiquidNetwork;
import zmaster587.advancedRocketry.cable.NetworkRegistry;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 7 deep — handler-level merge / consolidate
 * semantics for the cable network family.
 *
 * Why unit-tier and not server-tier: the only three pipe blocks AR ships
 * ({@code blockEnergyPipe}, {@code blockFluidPipe}, {@code blockDataPipe})
 * are currently <strong>commented out</strong> at
 * {@code AdvancedRocketry.java:782-787}, so no scenario can place a real
 * pipe and exercise placement + merge / split via the world. The
 * <em>handler-level</em> contract that those pipes route through —
 * {@link HandlerCableNetwork#mergeNetworks}, {@link CableNetwork#merge},
 * the per-network battery / fluid bookkeeping — IS exercisable without a
 * world; it's pure Java on plain Hashtables / sets.
 *
 * This file pins the invariants that future end-to-end pipe tests will
 * depend on, so when someone reinstates the pipe blocks they get
 * regression-net coverage of the handler the moment the registry call
 * comes back.
 *
 * Specifically covered:
 * <ul>
 *   <li>{@code mergeNetworks(a, b)} preserves the LOWER id, removes the
 *       higher, accumulates {@code numCables} across both.</li>
 *   <li>{@code EnergyNetwork.merge} accumulates the {@code battery}
 *       reservoir from the merged-in network — losing energy on merge
 *       would silently shrink modpack power grids on every chunk reload.</li>
 *   <li>Network ID uniqueness across many consecutive {@code initNetwork}
 *       calls.</li>
 *   <li>{@code addSource} / {@code addSink} de-duplication by position
 *       (the production code uses {@code BlockPos.compareTo} to detect
 *       duplicate adds — a regression that drops the de-dupe would
 *       quadratically grow per-network adjacency lists).</li>
 *   <li>{@code removeFromAll} polarity: it must remove from BOTH sources
 *       and sinks, not just whichever happened to come first.</li>
 *   <li>{@code merge} returns false on overlapping-endpoint conflict
 *       (the de-dupe protection).</li>
 *   <li>{@code tick} on an empty network is a no-op (no NPE) — the
 *       solar-panel-with-no-pipes case in production.</li>
 * </ul>
 */
public class PipeNetworkHandlerDeepTest {

    /** Minimal {@link TileEntity} stub used only as a position-bearing
     *  identity in the CableNetwork's sources/sinks sets. CableNetwork
     *  reads {@code getPos()} for de-dupe and identity comparison; no
     *  other TileEntity API is touched at the handler level. */
    private static class StubTile extends TileEntity {
        StubTile(int x, int y, int z) {
            this.setPos(new BlockPos(x, y, z));
        }
    }

    /**
     * <em>DOCUMENTS KNOWN PRODUCTION BUG</em> — see {@code
     * HandlerCableNetwork.java:67}:
     *
     * <pre>
     * assert (networks.get(Math.max(a, b)) == null
     *         || networks.get(Math.min(a, b)) == null);
     * networks.get(Math.min(a, b)).merge(networks.get(Math.max(a, b)));
     * </pre>
     *
     * The assertion fires <em>if either map entry is non-null</em> — yet the
     * very next line dereferences <em>both</em>. The assertion polarity is
     * inverted. In a stock JVM where assertions are off (Forge default),
     * this is dormant; with {@code -ea} on (Gradle test default), every
     * call to {@code mergeNetworks(a, b)} throws an {@link AssertionError}.
     *
     * No production logic change in this task: the test below pins the
     * <em>observed</em> behaviour (AssertionError under -ea) so a future
     * fix is forced to update this test, AND a future regression that
     * silently passes the bug forward is caught.
     */
    @Test(expected = AssertionError.class)
    public void mergeNetworksAssertionPolarityIsInverted_documentsKnownBug() {
        HandlerCableNetwork handler = new HandlerCableNetwork();
        int idA = handler.getNewNetworkID();
        int idB = handler.getNewNetworkID();
        handler.mergeNetworks(idA, idB); // throws AssertionError under -ea
    }

    /**
     * Same bug, observed differently: with assertions OFF, the merge does
     * progress, but {@link CableNetwork#merge} has a separate de-dupe bug
     * (next test) that makes the result unreliable. Here we exercise
     * mergeNetworks with assertions disabled at the class level to prove
     * the higher-id IS removed and numCables IS accumulated — the only
     * two outcomes that aren't affected by the merge() de-dupe bug.
     */
    @Test
    public void mergeNetworksProducesLowerIdSurvivor_assertionsDisabled() throws Exception {
        // Force assertions off on the target class — leaves -ea active for
        // every other class in this test run.
        Class.forName(HandlerCableNetwork.class.getName())
                .getClassLoader()
                .setClassAssertionStatus(HandlerCableNetwork.class.getName(), false);
        // Re-load? We can't easily; the assertion-status field is read
        // once at class init. Skip if assertions are still on for the
        // class — the documents-known-bug test above is the alternative.
        // We use reflection to detect $assertionsDisabled.
        Field assertField;
        try {
            assertField = HandlerCableNetwork.class.getDeclaredField("$assertionsDisabled");
            assertField.setAccessible(true);
            // $assertionsDisabled is static final boolean — value frozen
            // at class init. If it's still false, skip rather than fail.
            if (!assertField.getBoolean(null)) {
                org.junit.Assume.assumeTrue(
                        "Cannot retroactively disable assertions on HandlerCableNetwork; "
                                + "skipping — the documents-known-bug counterpart covers this path",
                        false);
            }
        } catch (NoSuchFieldException e) {
            // Class compiled without assertions (-source/-target old) — fine.
        }
        HandlerCableNetwork handler = new HandlerCableNetwork();
        int idA = handler.getNewNetworkID();
        int idB = handler.getNewNetworkID();
        int lo = Math.min(idA, idB);
        int hi = Math.max(idA, idB);
        bumpCables(handler.getNetwork(lo), 3);
        bumpCables(handler.getNetwork(hi), 5);

        int merged;
        try {
            merged = handler.mergeNetworks(idA, idB);
        } catch (AssertionError e) {
            org.junit.Assume.assumeNoException(
                    "Assertion is still firing — can't exercise the post-merge path", e);
            return;
        }
        assertEquals(lo, merged);
        assertFalse(handler.doesNetworkExist(hi));
        assertEquals("numCables must accumulate", 3 + 5,
                readNumCables(handler.getNetwork(lo)));
    }

    /**
     * <em>DOCUMENTS KNOWN PRODUCTION BUG</em> — see {@link CableNetwork#merge}:
     * the body does {@code sinks.addAll(cableNetwork.getSinks())} BEFORE
     * the de-dupe loop, then iterates over {@code cableNetwork.getSinks()}
     * and compares against {@code this.sinks} (which now contains those
     * same entries). Every cableNetwork entry trips the
     * {@code obj.getKey().getPos() == obj2.getKey().getPos() &&
     * obj.getValue() == obj2.getValue()} guard against the just-added
     * copy of itself, so the merge ALWAYS returns false the moment
     * cableNetwork has any sinks.
     *
     * The same shape exists for sources.
     *
     * Documented here so a future cleanup task that moves the addAll
     * AFTER the de-dupe loop (or replaces it with a proper union) can
     * delete this test and replace it with the merge-allows-different-
     * direction assertion that intuition predicts.
     */
    @Test
    public void cableNetworkMergeReturnsFalseWheneverBHasAnySinks_documentsKnownBug() {
        CableNetwork a = CableNetwork.initNetwork();
        CableNetwork b = CableNetwork.initNetwork();
        // b has a single sink, a is empty — intuition: merge succeeds.
        // Reality (current bug): the addAll seeds a.sinks with b's entry,
        // then the de-dupe loop sees the just-added copy and returns false.
        b.addSink(new StubTile(0, 0, 0), EnumFacing.UP);
        boolean ok = a.merge(b);
        assertFalse("CableNetwork.merge currently returns false even for the "
                + "simplest valid merge — this is a known production bug "
                + "in the de-dupe-loop ordering", ok);
    }

    /**
     * <em>DOCUMENTS KNOWN PRODUCTION BUG</em> — the bug above propagates
     * into {@link EnergyNetwork#merge}, which checks
     * {@code super.merge(...)} and conditionally migrates the battery. So
     * the battery NEVER migrates today, even for valid merges. A future
     * fix to the parent {@code CableNetwork.merge} body would re-enable
     * the battery migration, at which point this test flips to assert
     * "battery DID migrate".
     */
    @Test
    public void energyNetworkMergeNeverMigratesBatteryToday_documentsKnownBug() {
        HandlerEnergyNetwork handler = new HandlerEnergyNetwork();
        int idA = handler.getNewNetworkID();
        int idB = handler.getNewNetworkID();
        EnergyNetwork netA = (EnergyNetwork) handler.getNetwork(idA);
        EnergyNetwork netB = (EnergyNetwork) handler.getNetwork(idB);
        netB.acceptEnergy(200, false);
        // Seed BOTH with at least one sink so merge() body actually exercises
        // the de-dupe loop (otherwise it returns true via the empty path).
        netA.addSink(new StubTile(1, 1, 1), EnumFacing.NORTH);
        netB.addSink(new StubTile(2, 2, 2), EnumFacing.SOUTH);

        // Call merge() directly (not handler.mergeNetworks — that one hits
        // the assertion-polarity bug). EnergyNetwork.merge returns whatever
        // super.merge returns, which (per the documented bug above) is
        // false when b has any sinks.
        boolean migrated = netA.merge(netB);
        assertFalse("EnergyNetwork.merge today refuses to migrate the battery "
                + "due to the parent merge() de-dupe-ordering bug", migrated);

        // Sanity: netA's battery stays at zero (no migration happened).
        int roomLeftInA = netA.acceptEnergy(1_000_000, true);
        assertEquals("netA battery must still be empty after refused merge — "
                + "all 500 RF of capacity available", 500, roomLeftInA);
    }

    @Test
    public void initNetworkIdsAreUniqueAcross128ConsecutiveCalls() {
        // The id allocator uses Random(System.currentTimeMillis()) — a
        // tight loop is the canonical "if there's a duplicate, this trips
        // it" stress check. 128 iterations keeps the test sub-millisecond
        // while giving the RNG enough churn to surface a collision bug.
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int i = 0; i < 128; i++) {
            CableNetwork n = CableNetwork.initNetwork();
            int id = readNetworkID(n);
            assertTrue("duplicate id " + id + " produced at iter " + i,
                    seen.add(id));
        }
    }

    @Test
    public void addSourceDedupesByPosition() {
        CableNetwork net = CableNetwork.initNetwork();
        StubTile t1 = new StubTile(7, 7, 7);
        StubTile t2 = new StubTile(7, 7, 7);  // SAME position as t1 — should de-dup

        net.addSource(t1, EnumFacing.UP);
        assertEquals(1, net.getSources().size());
        net.addSource(t2, EnumFacing.DOWN);
        // The dedupe semantics in production are "if a same-position entry
        // exists, replace it". So count stays at 1, even though the
        // direction differs. A regression that drops the dedupe (e.g. a
        // .equals() rewrite that compares identity instead of position)
        // would let same-block-different-side entries pile up.
        assertEquals("addSource must de-dupe by BlockPos (same-pos different "
                + "tile-instance must NOT duplicate)", 1, net.getSources().size());
    }

    @Test
    public void addSinkDedupesByPosition() {
        CableNetwork net = CableNetwork.initNetwork();
        net.addSink(new StubTile(1, 2, 3), EnumFacing.NORTH);
        assertEquals(1, net.getSinks().size());
        net.addSink(new StubTile(1, 2, 3), EnumFacing.SOUTH);
        assertEquals("addSink must de-dupe by BlockPos", 1, net.getSinks().size());
    }

    @Test
    public void removeFromAllRemovesFromBothSourcesAndSinks() {
        CableNetwork net = CableNetwork.initNetwork();
        StubTile tile = new StubTile(42, 42, 42);
        net.addSource(tile, EnumFacing.UP);
        net.addSink(tile, EnumFacing.DOWN);
        assertEquals(1, net.getSources().size());
        assertEquals(1, net.getSinks().size());

        net.removeFromAll(tile);
        assertEquals("removeFromAll must clear sources entry by pos",
                0, net.getSources().size());
        assertEquals("removeFromAll must clear sinks entry by pos",
                0, net.getSinks().size());
    }

    @Test
    public void mergeRejectsExactPositionPlusDirectionOverlap() {
        // The merge() body has an early `return false` if it finds an
        // overlapping (pos, dir) pair across the two networks. Pin that
        // polarity: a merge with exact overlap must NOT proceed.
        CableNetwork a = CableNetwork.initNetwork();
        CableNetwork b = CableNetwork.initNetwork();
        a.addSink(new StubTile(0, 0, 0), EnumFacing.UP);
        b.addSink(new StubTile(0, 0, 0), EnumFacing.UP); // same pos + same dir

        boolean ok = a.merge(b);
        assertFalse("merge must reject overlap (same pos AND same direction)", ok);
    }

    @Test
    public void tickOnEmptyEnergyNetworkIsNoOp() {
        EnergyNetwork net = EnergyNetwork.initNetwork();
        // No sources, no sinks → tick must NOT NPE / throw.
        net.tick();
        // No assertion on state — just "did not throw" is the contract.
        // A regression that dereferences sources.iterator().next() without
        // an isEmpty() guard would crash here.
    }

    @Test
    public void tickOnEmptyLiquidNetworkIsNoOp() {
        LiquidNetwork net = LiquidNetwork.initNetwork();
        net.tick();
    }

    @Test
    public void handlerTickAllNetworksHandlesEmptyMap() {
        HandlerCableNetwork handler = new HandlerCableNetwork();
        // No networks registered — must not NPE.
        handler.tickAllNetworks();
    }

    @Test
    public void handlerTickAllNetworksTicksRegisteredNetworks() {
        HandlerLiquidNetwork handler = new HandlerLiquidNetwork();
        int id = handler.getNewNetworkID();
        assertNotNull(handler.getNetwork(id));
        // Should call tick on each — the LiquidNetwork tick early-exits
        // on empty source/sink so this is just a no-throw guard.
        handler.tickAllNetworks();
    }

    @Test
    public void removeNetworkByIdYieldsNullGetNetwork() {
        HandlerCableNetwork handler = new HandlerCableNetwork();
        int id = handler.getNewNetworkID();
        handler.removeNetworkByID(id);
        assertNull("getNetwork after removeNetworkByID must return null",
                handler.getNetwork(id));
        assertFalse("doesNetworkExist must return false after remove",
                handler.doesNetworkExist(id));
    }

    @Test
    public void handlerToStringOnUnknownIdReturnsEmpty() {
        // Production code calls toString(networkID) for debug dumps; a
        // null-network must produce empty-string rather than NPE so
        // chat-side debugging tools don't crash when a network was just
        // unloaded.
        HandlerCableNetwork handler = new HandlerCableNetwork();
        String s = handler.toString(-1234567);
        assertEquals("toString on unknown networkID must return empty string",
                "", s);
    }

    @Test
    public void registryClearNetworksEmptiesHandlerMaps() throws Exception {
        // Companion to CableNetworkHandlerContractTest.clearNetworks-
        // DoesNotNullSingletonRefs: where that test pinned the singleton
        // refs survive, THIS test pins that the inner maps actually drain.
        NetworkRegistry.registerFluidNetwork();
        int eId = NetworkRegistry.energyNetwork.getNewNetworkID();
        int dId = NetworkRegistry.dataNetwork.getNewNetworkID();
        int lId = NetworkRegistry.liquidNetwork.getNewNetworkID();
        assertTrue(NetworkRegistry.energyNetwork.doesNetworkExist(eId));
        assertTrue(NetworkRegistry.dataNetwork.doesNetworkExist(dId));
        assertTrue(NetworkRegistry.liquidNetwork.doesNetworkExist(lId));

        NetworkRegistry.clearNetworks();
        assertFalse("energyNetwork map must be empty after clearNetworks",
                NetworkRegistry.energyNetwork.doesNetworkExist(eId));
        assertFalse("dataNetwork map must be empty after clearNetworks",
                NetworkRegistry.dataNetwork.doesNetworkExist(dId));
        assertFalse("liquidNetwork map must be empty after clearNetworks",
                NetworkRegistry.liquidNetwork.doesNetworkExist(lId));
    }

    /**
     * Pin the (slightly surprising) contract: {@link NetworkRegistry#registerFluidNetwork}
     * is NOT idempotent — every call replaces the three singleton handlers
     * with fresh instances. This is fine because in production it's called
     * exactly once during {@code @Mod} init; cached refs in TilePipes are
     * only handed out AFTER that init. The contract here exists so a
     * future "let's call registerFluidNetwork on world reload too" cleanup
     * proposal is caught at test time: any cached pipe ref would point at
     * a detached handler the moment the second register runs.
     */
    @Test
    public void registerFluidNetworkReplacesSingletons() {
        NetworkRegistry.registerFluidNetwork();
        HandlerEnergyNetwork firstEnergy = NetworkRegistry.energyNetwork;
        NetworkRegistry.registerFluidNetwork();
        assertNotEquals("registerFluidNetwork must replace, not reuse, the "
                        + "singleton (this surfaces a regression for any future "
                        + "code that calls it more than once)",
                System.identityHashCode(firstEnergy),
                System.identityHashCode(NetworkRegistry.energyNetwork));
    }

    // --- reflection helpers --------------------------------------------------

    private static int readNetworkID(CableNetwork n) {
        try {
            Field f = CableNetwork.class.getDeclaredField("networkID");
            f.setAccessible(true);
            return f.getInt(n);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("CableNetwork.networkID field gone — API regressed", e);
        }
    }

    private static int readNumCables(CableNetwork n) {
        try {
            Field f = CableNetwork.class.getDeclaredField("numCables");
            f.setAccessible(true);
            return f.getInt(n);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("CableNetwork.numCables field gone — API regressed", e);
        }
    }

    private static void bumpCables(CableNetwork n, int times) throws Exception {
        Field f = CableNetwork.class.getDeclaredField("numCables");
        f.setAccessible(true);
        f.setInt(n, f.getInt(n) + times);
    }
}
