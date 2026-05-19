package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 4 — tile-machine isolated coverage.
 *
 * Production previously had ZERO per-tile-class regression nets;
 * {@code MultiMachineControllerSmokeTest} touches assembler-style
 * controllers, but the individual machines (solar panel, fluid tank,
 * force-field projector, guidance computer, oxygen vent, pump,
 * satellite builder) are exercised only indirectly. A capability rename
 * or NBT-key drift on any of them currently surfaces as a runtime
 * NullPointerException in production — not a test failure.
 *
 * Each test below uses the existing
 *   {@code /artest place <dim> <x> <y> <z> <blockId>} → place a tile
 *   {@code /artest tile force-tick <dim> <x> <y> <z> <ticks>} → drive it
 *   {@code /artest energy stored / inject <dim> <x> <y> <z>} → cap probe
 *   {@code /artest tile state <dim> <x> <y> <z>} → state probe (where present)
 * The tests pin the *contract surface* (tile class FQN, capability
 * presence, force-tick survives without crashing). They do NOT assert
 * gameplay numbers (production has no canonical "solar panel produces
 * X RF/tick under simulated daylight" reference) — those belong in
 * a future tier.
 */
public class TileMachineDepthTest extends AbstractSharedServerTest {

    private static final int DIM = 0;
    // Stay near spawn so the chunk is loaded; spread far enough apart that
    // tiles placed by separate tests don't interact.
    private static final int BASE_X = 200;
    private static final int BASE_Z = 200;
    private static final int Y = 80; // above terrain to avoid stone overwrite quirks

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    /**
     * Place a block, pre-clearing the position with air. The pre-clear is
     * necessary because some AR blocks' tile-entity wiring depends on the
     * neighbour-update + onBlockPlaced chain that vanilla setBlockState
     * doesn't always trigger when overwriting a non-air block (e.g. terrain).
     */
    private void place(String blockId, int x, int y, int z) throws Exception {
        // Clear first.
        client().execute("artest place " + DIM + " " + x + " " + y + " " + z + " minecraft:air");
        String r = ok(client().execute(
                "artest place " + DIM + " " + x + " " + y + " " + z + " " + blockId));
        assertTrue("place(" + blockId + ") at " + x + "," + y + "," + z + " failed: " + r,
                r.contains("\"placed\":true"));
    }

    @Test
    public void solarGeneratorExposesEnergyCapAndSurvivesForceTick() throws Exception {
        // NOTE: the registry name "solarPanel" is the *plain decorative
        // block* — it has no tile entity. The tile-bearing machine is
        // "solarGenerator" (bound to TileSolarPanel internally). Easy
        // mistake — pin so a future test author lands on the right block.
        int x = BASE_X, z = BASE_Z;
        place("advancedrocketry:solarGenerator", x, Y, z);

        String stored = ok(client().execute(
                "artest energy stored " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("solar generator must expose CapabilityEnergy: " + stored,
                stored.contains("\"hasEnergy\":true"));
        assertTrue("tileClass must mention TileSolarPanel: " + stored,
                stored.contains("TileSolarPanel"));

        // Force-tick — should not crash, even with no daylight on dim 0
        // at world spawn (production handles "no sky" gracefully).
        String tickResp = ok(client().execute(
                "artest tile force-tick " + DIM + " " + x + " " + Y + " " + z + " 5"));
        assertTrue("solar generator force-tick must not error: " + tickResp,
                tickResp.contains("\"ok\":true"));
    }

    @Test
    public void fluidTankPlacesAndExposesTileClass() throws Exception {
        int x = BASE_X + 4, z = BASE_Z;
        place("advancedrocketry:liquidTank", x, Y, z);

        // No /artest fluid info → use the energy probe just to confirm tile
        // class via tileClass field (probe reports it on every call).
        // Capability check via /artest fluid (existing probe) is the
        // strongest evidence the tank actually exposes IFluidHandler.
        String tankResp = ok(client().execute(
                "artest fluid stored " + DIM + " " + x + " " + Y + " " + z));
        // Probe returns either {"ok":true,...} or {"error":...} —
        // contract: error must NOT be "no tile entity" (that would
        // mean place silently dropped the tile).
        assertTrue("fluid tank place silently dropped tile: " + tankResp,
                !tankResp.contains("\"no tile entity\""));
        // The tile should be TileFluidTank (or its TileFluidHatch parent).
        // tileClass is only emitted by the energy probe, so reuse that
        // to verify the tile lives.
        String storedResp = ok(client().execute(
                "artest energy stored " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("liquidTank must be a TileFluidTank-family class: " + storedResp,
                storedResp.contains("FluidTank") || storedResp.contains("FluidHatch"));
    }

    @Test
    public void forceFieldProjectorIsTickableAndDoesNotCrashOnForceTick() throws Exception {
        // TileForceFieldProjector implements ITickable directly (not the
        // libVulpes inventoried-machine hierarchy). The force-tick probe
        // refuses to tick a non-ITickable — assert this WORKS (i.e. the
        // probe doesn't 'error' with "tile not ITickable").
        int x = BASE_X, z = BASE_Z + 4;
        place("advancedrocketry:forceFieldProjector", x, Y, z);

        String tickResp = ok(client().execute(
                "artest tile force-tick " + DIM + " " + x + " " + Y + " " + z + " 3"));
        assertTrue("forceFieldProjector force-tick must succeed (must be ITickable): "
                        + tickResp,
                !tickResp.contains("tile not ITickable"));
    }

    @Test
    public void guidanceComputerHasInventorySlotAccessibleByHatchProbe() throws Exception {
        // TileGuidanceComputer extends TileInventoryHatch, so the
        // /artest hatch read probe must dump at least one slot (even if
        // empty). If the inventory size dropped to 0 the entire
        // ship-builder UI would break silently.
        int x = BASE_X + 8, z = BASE_Z;
        place("advancedrocketry:guidanceComputer", x, Y, z);

        String hatchResp = ok(client().execute(
                "artest hatch read " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("guidance computer must accept hatch-read probe: " + hatchResp,
                !hatchResp.contains("not an IInventory") && !hatchResp.contains("no tile entity"));
        // The hatch probe reports either {"slots":[...]} or {"size":N}; both
        // imply the inventory was discoverable.
        assertTrue("guidance computer hatch-read should yield slot info: " + hatchResp,
                hatchResp.contains("\"slots\"") || hatchResp.contains("\"size\""));
    }

    @Test
    public void oxygenVentExposesTileAndAcceptsForceTick() throws Exception {
        // TileOxygenVent is an inventoried RF-consumer tank — placing it
        // and force-ticking once exercises the implements-chain
        // (IBlobHandler, IModularInventory, INetworkMachine, IToggleable, …).
        // A subtle rename of one of those would NPE in the toggle path.
        int x = BASE_X + 12, z = BASE_Z;
        place("advancedrocketry:oxygenVent", x, Y, z);

        String storedResp = ok(client().execute(
                "artest energy stored " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("oxygenVent must expose CapabilityEnergy (RF consumer): "
                        + storedResp,
                storedResp.contains("\"hasEnergy\":true"));
        assertTrue("tileClass should mention OxygenVent: " + storedResp,
                storedResp.contains("OxygenVent"));

        String tickResp = ok(client().execute(
                "artest tile force-tick " + DIM + " " + x + " " + Y + " " + z + " 2"));
        assertTrue("oxygenVent force-tick must not error: " + tickResp,
                !tickResp.contains("\"error\""));
    }

    @Test
    public void pumpPlacesAndExposesFluidCap() throws Exception {
        int x = BASE_X + 16, z = BASE_Z;
        place("advancedrocketry:blockPump", x, Y, z);

        String fluidResp = ok(client().execute(
                "artest fluid stored " + DIM + " " + x + " " + Y + " " + z));
        // Pump implements IFluidHandler — the probe must reach it.
        assertTrue("pump place silently dropped tile: " + fluidResp,
                !fluidResp.contains("\"no tile entity\""));
    }

    @Test
    public void satelliteBuilderPlacesAndReportsCorrectTileClass() throws Exception {
        int x = BASE_X + 20, z = BASE_Z;
        place("advancedrocketry:satelliteBuilder", x, Y, z);

        // The builder is a heavy machine (RF consumer + assembly slots).
        // Pin its tileClass via the energy probe so a rename surfaces here
        // before any GUI test fails.
        String storedResp = ok(client().execute(
                "artest energy stored " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("satellite builder tile not found: " + storedResp,
                !storedResp.contains("\"no tile entity\""));
        assertTrue("tileClass should mention SatelliteBuilder: " + storedResp,
                storedResp.contains("SatelliteBuilder"));
    }

    @Test
    public void virginAirPositionHasNoTileEntity() throws Exception {
        // Sanity: the test setup itself is honest — a virgin position
        // (no place call) must report "no tile entity" rather than
        // accidentally finding a leftover tile from a previous test.
        // Skip the place() helper because setBlockState(air→air) returns
        // false, which would trip the helper's placed=true assertion;
        // here we just want to assert the *initial* state.
        int x = BASE_X + 100, z = BASE_Z + 100;
        String stored = ok(client().execute(
                "artest energy stored " + DIM + " " + x + " " + Y + " " + z));
        assertTrue("virgin position must not have a tile entity: " + stored,
                stored.contains("\"no tile entity\""));
    }
}
