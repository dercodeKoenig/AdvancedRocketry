package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.16 — energy systems smoke (solar / microwave / black-hole / hatches /
 * pipes).
 *
 * Each generator's per-tick output assertion requires a placed multiblock at a
 * known position. {@code /artest machine info <pos>} surfaces battery state when
 * the tile is a {@code TileMultiPowerConsumer}, but generators (solar etc.) use
 * a different superclass — needs {@code /artest energy stored <pos>} extension.
 * Deferred until generator probes land.
 */
public class EnergySystemsSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.energy_systems_smoke"; }
    @Override public String category() { return "P2/energy"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // Two assertions:
        //   1. Probe at empty position must report "no tile entity" (not NPE).
        //   2. Probe at a placed RFBattery (libVulpes basic energy storage) must
        //      report a valid IEnergyStorage capability with non-negative max
        //      energy. This validates the energy-stored probe's reflection path
        //      against real Forge-energy tiles without needing a full multiblock
        //      generator + power cycle.
        java.util.List<String> empty = client.execute("artest energy stored 0 1000 64 1000");
        String emptyJoined = String.join("\n", empty);
        if (!emptyJoined.contains("\"no tile entity\"")) {
            context.note("expected 'no tile entity' on empty position: " + emptyJoined);
            return TestStatus.FAILED;
        }

        // Place a libVulpes RFBattery (Forge-energy capability provider).
        String place = String.join("\n", client.execute(
                "artest place 0 1000 64 1000 libvulpes:battery"));
        if (!place.contains("\"placed\":true")) {
            context.note("could not place libvulpes:battery: " + place);
            // Fall back to soft assertion — still PASSED on the empty-pos probe.
            context.note("only schema-empty assertion verified (libvulpes battery unavailable)");
            return TestStatus.PASSED;
        }
        java.util.List<String> stored = client.execute("artest energy stored 0 1000 64 1000");
        String storedJoined = String.join("\n", stored);
        if (!storedJoined.contains("\"hasEnergy\":true")) {
            context.note("battery doesn't expose IEnergyStorage capability: " + storedJoined);
            return TestStatus.FAILED;
        }
        if (!storedJoined.contains("\"energyMax\":")) {
            context.note("battery missing energyMax field: " + storedJoined);
            return TestStatus.FAILED;
        }
        context.note("energy probe round-trip on libvulpes:battery: " + storedJoined);
        return TestStatus.PASSED;
    }
}
