package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.17 — pipe / data / energy network smoke.
 *
 * Connectivity + transfer assertions need placed pipe segments + endpoints +
 * tick observation. Requires {@code /artest pipe network <pos>} to report
 * connected nodes — not implemented. Deferred.
 */
public class PipeNetworkSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.pipe_network_smoke"; }
    @Override public String category() { return "P2/networks"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // Pipes are TileEntity instances exposing the standard Forge energy
        // capability. /artest energy stored returns hasEnergy=true on a placed
        // pipe segment. Without a fixture-placed pipe we can only check the
        // probe doesn't crash on an empty position.
        java.util.List<String> response = client.execute("artest energy stored 0 0 64 0");
        String joined = String.join("\n", response);
        if (!joined.contains("\"error\":\"no tile entity\"")) {
            context.note("/artest energy stored at empty pos returned unexpected schema: " + joined);
            return TestStatus.FAILED;
        }
        context.note("/artest energy stored handles empty pos cleanly; pipe fixture assertions deferred");
        return TestStatus.SKIPPED;
    }
}
