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
        // Schema-only: probe energy at origin (no tile) — must report "no tile
        // entity" rather than NPE. Fixture-bound generator output assertions
        // require placed AR multiblocks and are deferred.
        java.util.List<String> response = client.execute("artest energy stored 0 0 64 0");
        String joined = String.join("\n", response);
        if (!joined.contains("\"error\"") && !joined.contains("\"hasEnergy\"")) {
            context.note("/artest energy stored returned unexpected schema: " + joined);
            return TestStatus.FAILED;
        }
        context.note("/artest energy stored schema valid; generator-output assertions deferred");
        return TestStatus.SKIPPED;
    }
}
