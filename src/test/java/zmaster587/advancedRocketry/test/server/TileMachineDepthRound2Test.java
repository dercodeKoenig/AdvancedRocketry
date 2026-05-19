package zmaster587.advancedRocketry.test.server;

// migrated to AbstractSharedServerTest (TASK-03 B2)
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 4 round 2 — extends {@link TileMachineDepthTest}
 * onto the next batch of "important but not yet pinned" tile families:
 *
 *   - {@code suitWorkStation}    → {@code TileSuitWorkStation}
 *   - {@code deployableRocketBuilder} → {@code TileUnmannedVehicleAssembler}
 *   - {@code landingPad}         → {@code TileLandingPad}
 *   - {@code fuelingStation}     → {@code TileFuelingStation}
 *   - {@code terraformer}        → {@code TileAtmosphereTerraformer}
 *
 * Same contract surface as round 1: probe the registry name resolves to the
 * expected tile class, probe the capability surface (RF / IInventory / fluid)
 * that production code reads, and where the tile is ITickable, drive
 * {@code force-tick} once to prove the update loop doesn't NPE.
 *
 * No gameplay numbers — those need either a real assembly fixture (UV
 * assembler) or a real multiblock skeleton (terraformer), both of which
 * are out of scope for the per-tile depth tier. Round 2 just nails down
 * "the tile exists, exposes its declared capabilities, and ticks
 * without crashing".
 *
 * Spread positions far enough apart from round 1's {@code BASE_X / BASE_Z}
 * (200,200 + offsets up to 100) that JVM-shared test state can't leak.
 */
public class TileMachineDepthRound2Test extends AbstractSharedServerTest {

    private static final int DIM = 0;
    private static final int BASE_X = 400;
    private static final int BASE_Z = 400;
    private static final int Y = 80;

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    /** Same place() helper as round 1 — see {@link TileMachineDepthTest#place}
     *  for the air-pre-clear rationale. */
    private void place(String blockId, int x, int y, int z) throws Exception {
        client().execute("artest place " + DIM + " " + x + " " + y + " " + z + " minecraft:air");
        String r = ok(client().execute(
                "artest place " + DIM + " " + x + " " + y + " " + z + " " + blockId));
        assertTrue("place(" + blockId + ") at " + x + "," + y + "," + z + " failed: " + r,
                r.contains("\"placed\":true"));
    }

    @Test
    public void suitWorkStationExposesInventoryAndCorrectTileClass() throws Exception {
        // TileSuitWorkStation: bare TileEntity + IInventory + IModularInventory.
        // Critical because EVA-suit assembly is gated entirely by its slot map;
        // an unannounced rename of the underlying class would break the suit
        // GUI silently (no recipe error — just empty output slot forever).
        int x = BASE_X, z = BASE_Z;
        place("advancedrocketry:suitWorkStation", x, Y, z);

        // The energy probe always reports tileClass even when the tile lacks
        // CapabilityEnergy — use it to pin the FQN. Suit workstation has NO
        // energy capability (it's a manual assembler), so hasEnergy=false is
        // the expected contract.
        String stored = ok(client().execute(
                "artest energy stored " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("suit work station tile must be present: " + stored,
                !stored.contains("\"no tile entity\""));
        assertTrue("tileClass should mention TileSuitWorkStation: " + stored,
                stored.contains("TileSuitWorkStation"));
        assertTrue("suit work station is a manual assembler — must NOT report energy cap: "
                        + stored,
                stored.contains("\"hasEnergy\":false"));

        // The hatch-read probe is the IInventory contract gate; size must be
        // strictly positive (the GUI binds slots by index — 0 slots = the
        // entire crafting matrix renders empty).
        String hatch = ok(client().execute(
                "artest hatch read " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("suit work station must be IInventory-accessible: " + hatch,
                !hatch.contains("not an IInventory") && !hatch.contains("\"no tile entity\""));
        assertTrue("suit work station hatch-read should expose size>0: " + hatch,
                hatch.contains("\"size\":") && !hatch.contains("\"size\":0,"));
    }

    @Test
    public void unmannedVehicleAssemblerReportsAssemblerLineageAndIsTickable() throws Exception {
        // TileUnmannedVehicleAssembler extends TileRocketAssemblingMachine —
        // shares all the rocket-builder plumbing (assembly slots, scan logic,
        // status flags). The capability-exposed energy face should mirror the
        // assembler family. Pin lineage via tileClass; pin tickability via
        // force-tick.
        int x = BASE_X + 8, z = BASE_Z;
        place("advancedrocketry:deployableRocketBuilder", x, Y, z);

        String stored = ok(client().execute(
                "artest energy stored " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("UV assembler tile must be present: " + stored,
                !stored.contains("\"no tile entity\""));
        assertTrue("tileClass should mention TileUnmannedVehicleAssembler: " + stored,
                stored.contains("TileUnmannedVehicleAssembler"));

        // Round 1 pinned the rocket builder's energy contract; UV assembler
        // shares the same parent so it MUST also have an energy face. If the
        // parent ever drops the capability, this assertion surfaces it.
        assertTrue("UV assembler must expose CapabilityEnergy (inherits from "
                        + "RocketAssemblingMachine): " + stored,
                stored.contains("\"hasEnergy\":true"));

        String tickResp = ok(client().execute(
                "artest tile force-tick " + DIM + " " + x + " " + Y + " " + z + " 3"));
        // The assembler family is ITickable; force-tick must succeed and not
        // crash on a not-yet-scanned (empty) build area.
        assertTrue("UV assembler force-tick must not error: " + tickResp,
                tickResp.contains("\"ok\":true"));
    }

    @Test
    public void landingPadIsInventoryHatchSubclass() throws Exception {
        // TileLandingPad extends TileInventoryHatch; it's a passive marker
        // tile whose IInventory slots store fuel-related items for rocket
        // landings. A capability regression here would silently break
        // station-to-planet rocket return: the rocket would no longer find
        // the pad's "is this an AR landing pad" sentinel.
        int x = BASE_X + 16, z = BASE_Z;
        place("advancedrocketry:landingPad", x, Y, z);

        String stored = ok(client().execute(
                "artest energy stored " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("landing pad tile must be present: " + stored,
                !stored.contains("\"no tile entity\""));
        assertTrue("tileClass should mention TileLandingPad: " + stored,
                stored.contains("TileLandingPad"));

        // TileInventoryHatch implements IInventory — the hatch-read probe
        // discriminates by IInventory, so its success here pins the
        // parent-class contract surface.
        String hatch = ok(client().execute(
                "artest hatch read " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("landing pad must be IInventory-accessible (extends "
                        + "TileInventoryHatch): " + hatch,
                !hatch.contains("not an IInventory") && !hatch.contains("\"no tile entity\""));
    }

    @Test
    public void fuelingStationExposesEnergyAndFluidCapabilities() throws Exception {
        // TileFuelingStation extends TileInventoriedRFConsumerTank — it must
        // have BOTH an energy cap (RF consumer) and a fluid cap (tank that
        // accepts rocket fuel). These two together are what make a fueling
        // station functional; lose either and the per-tick fuel-transfer
        // loop silently no-ops on every rocket on the pad.
        int x = BASE_X + 24, z = BASE_Z;
        place("advancedrocketry:fuelingStation", x, Y, z);

        String stored = ok(client().execute(
                "artest energy stored " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("fueling station tile must be present: " + stored,
                !stored.contains("\"no tile entity\""));
        assertTrue("tileClass should mention TileFuelingStation: " + stored,
                stored.contains("TileFuelingStation"));
        assertTrue("fueling station must expose CapabilityEnergy (RF consumer): "
                        + stored,
                stored.contains("\"hasEnergy\":true"));

        // The fluid probe surfaces IFluidHandler presence; its error path is
        // "no tile entity" / "tile has no IFluidHandler". Anything else
        // (whether or not the tank is empty) confirms the cap survives.
        String fluid = ok(client().execute(
                "artest fluid stored " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("fueling station tank cap silently dropped: " + fluid,
                !fluid.contains("\"no tile entity\"")
                        && !fluid.contains("\"tile has no IFluidHandler\""));
    }

    @Test
    public void terraformerReportsCorrectTileClassPreAssembly() throws Exception {
        // TileAtmosphereTerraformer extends TileMultiPowerConsumer — it's
        // the *controller* tile of an inert multiblock skeleton. The actual
        // multiblock won't form from a single isolated place; until it's
        // assembled, the controller DOES NOT expose CapabilityEnergy (gated
        // on `isComplete`). What we CAN pin in isolation is the
        // controller's tileClass + that the pre-assembly state is the
        // expected hasEnergy=false (rather than e.g. throwing during
        // capability lookup, which would crash any energy pipe routing
        // adjacent to an unassembled terraformer skeleton).
        int x = BASE_X + 32, z = BASE_Z;
        place("advancedrocketry:terraformer", x, Y, z);

        String stored = ok(client().execute(
                "artest energy stored " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("terraformer controller tile must be present: " + stored,
                !stored.contains("\"no tile entity\""));
        assertTrue("tileClass should mention TileAtmosphereTerraformer: " + stored,
                stored.contains("TileAtmosphereTerraformer"));
        // Contract surprise pinned here: a pre-assembly multiblock controller
        // is "cap-dark" — it has no IEnergyStorage until the structure forms.
        // If a refactor changes the polarity of `isComplete` and the
        // controller starts exposing the cap unconditionally, energy pipes
        // would happily inject RF into a phantom buffer that never updates.
        assertTrue("pre-assembly terraformer controller must NOT expose "
                        + "CapabilityEnergy (gated on isComplete): " + stored,
                stored.contains("\"hasEnergy\":false"));
    }

    @Test
    public void terraformerIsolatedControllerForceTickIsSafe() throws Exception {
        // Companion to the previous test: terraformer multiblocks expose a
        // tick loop on the controller; for an unassembled skeleton the loop
        // MUST early-exit cleanly (NPE on the `isComplete` check would have
        // shipped a runtime crash to every modpack player whose terraformer
        // partially-broke). force-tick a few ticks and assert no exception.
        int x = BASE_X + 32, z = BASE_Z + 8;
        place("advancedrocketry:terraformer", x, Y, z);

        String tickResp = ok(client().execute(
                "artest tile force-tick " + DIM + " " + x + " " + Y + " " + z + " 3"));
        // Either the tile is ITickable and ticks cleanly, OR the probe
        // reports "tile not ITickable" (some libVulpes multiblock controllers
        // delegate ticking to the host structure). Either contract is fine —
        // but a thrown exception is NOT.
        assertTrue("terraformer force-tick threw or hard-errored: " + tickResp,
                tickResp.contains("\"ok\":true")
                        || tickResp.contains("tile not ITickable"));
    }
}
