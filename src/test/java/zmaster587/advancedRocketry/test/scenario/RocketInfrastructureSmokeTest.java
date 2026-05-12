package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.10 — rocket infrastructure smoke (fueling station, loaders, monitors,
 * linker distance limits).
 *
 * Requires a built rocket plus placed infrastructure tiles. {@code /artest rocket
 * info} surfaces {@code linkedInfrastructure} but probing per-IInfrastructure
 * tile state from a single command requires extending {@code /artest machine info}
 * with infrastructure-specific awareness, OR a dedicated {@code /artest infra
 * <type>} probe. Deferred to a follow-up.
 */
public class RocketInfrastructureSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.rocket_infrastructure_smoke"; }
    @Override public String category() { return "P1/rocket-infrastructure"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // Place a fueling station and assert it implements IInfrastructure.
        int x = 800, y = 64, z = 800;
        String place = String.join("\n", client.execute(
                "artest place 0 " + x + " " + y + " " + z + " advancedrocketry:fuelingStation"));
        if (!place.contains("\"placed\":true")) {
            context.note("place fueling station failed: " + place);
            return TestStatus.FAILED;
        }

        java.util.List<String> response = client.execute("artest infra info 0 " + x + " " + y + " " + z);
        String joined = String.join("\n", response);
        if (!joined.contains("\"isInfrastructure\":true")) {
            context.note("placed fueling station not detected as infrastructure: " + joined);
            return TestStatus.FAILED;
        }
        if (!joined.contains("\"maxLinkDistance\"")) {
            context.note("infra info missing maxLinkDistance: " + joined);
            return TestStatus.FAILED;
        }

        // Empty position must report not-infrastructure cleanly.
        String emptyResponse = String.join("\n", client.execute("artest infra info 0 100 64 100"));
        if (!emptyResponse.contains("\"error\":\"no tile entity\"")) {
            context.note("infra info on empty pos didn't error: " + emptyResponse);
            return TestStatus.FAILED;
        }

        context.note("fueling station recognized as IInfrastructure with maxLinkDistance");
        return TestStatus.PASSED;
    }
}
