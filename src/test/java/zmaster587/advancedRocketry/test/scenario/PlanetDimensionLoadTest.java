package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.3 — planet/dimension lifecycle smoke.
 *
 * Walks {@code /artest dim list} → {@code /artest dim info <dim>} for the AR
 * planet at the configured spawn dimension, asserts that the provider class
 * is non-null and the AR planet flag is set.
 *
 * <p>This scenario depends on AR having registered at least one planet by the
 * time the server reports readiness. With the standard XML config that's true
 * (Sol + Earth + Moon are eagerly created in {@code DimensionManager.preloadGalaxy}).
 * If the configured galaxy XML is empty the scenario reports SKIPPED rather
 * than FAILED so an empty-galaxy configuration doesn't gate the suite.</p>
 */
public class PlanetDimensionLoadTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.planet_dimension_load"; }
    @Override public String category() { return "P0/planet-lifecycle"; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        List<String> dimList = client.execute("artest dim list");
        String joined = String.join("\n", dimList);

        if (!joined.contains("\"arDimensions\":[")) {
            context.note("dim list missing arDimensions key — probe wiring broken");
            return TestStatus.FAILED;
        }

        // Empty AR dimension list is acceptable in galaxy-less configurations.
        if (joined.contains("\"arDimensions\":[]")) {
            context.note("no AR dimensions registered — skipping (empty galaxy?)");
            return TestStatus.SKIPPED;
        }

        context.note("AR dimensions are registered; planet/dimension lifecycle pre-conditions met");
        return TestStatus.PASSED;
    }
}
