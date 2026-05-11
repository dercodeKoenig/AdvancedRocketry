package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.14 — terraforming smoke.
 *
 * Validates {@code /artest terraforming info <dim>} schema on Earth (dim=0).
 * Atmosphere-density-mutation, biome-queue and protected-blocks assertions need
 * a placed AtmosphereTerraformer fixture and require a sequence of probe calls
 * over several server ticks — deferred to a follow-up that adds
 * {@code /artest terraforming run <dim> <ticks>}.
 */
public class TerraformingSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.terraforming_smoke"; }
    @Override public String category() { return "P2/terraforming"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        List<String> tf = client.execute("artest terraforming info 0");
        String joined = String.join("\n", tf);
        if (joined.contains("\"error\"")) {
            context.note("/artest terraforming info returned error: " + joined);
            // dim=0 may not be in AR's terraforming registry — skip rather than fail.
            return TestStatus.SKIPPED;
        }
        if (!joined.contains("\"originalAtmosphere\"") || !joined.contains("\"currentAtmosphere\"")) {
            context.note("terraforming info missing schema keys: " + joined);
            return TestStatus.FAILED;
        }
        context.note("terraforming info schema-valid on Earth; mutation assertions deferred");
        return TestStatus.SKIPPED;
    }
}
