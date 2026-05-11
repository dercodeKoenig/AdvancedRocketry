package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;
import zmaster587.advancedRocketry.test.AdvancedRocketryTestConstants;

import java.util.List;

/**
 * SMART §7.5 — weather baseline / future B1 regression.
 *
 * Two modes selected via {@code -Dadvancedrocketry.tests.expectedWeatherMode=}:
 * <ul>
 *   <li>{@code shared} (default, pre-B1): weather changes on planet A also affect planet B.</li>
 *   <li>{@code per_dimension} (post-B1): planets have isolated weather.</li>
 * </ul>
 *
 * Both modes use the same probes:
 * <pre>
 *   /artest weather get  &lt;dim&gt;
 *   /artest weather set  &lt;dim&gt; rain &lt;ticks&gt;
 *   /artest weather get  &lt;otherDim&gt;
 * </pre>
 * and assert based on the configured expected mode.
 *
 * <p>STATUS: skeleton — full implementation requires fixture planets at known
 * dimensions (the §7.4 fixture). Until that lands, reports SKIPPED with a
 * clear note about the chosen expected mode so the suite output documents
 * which baseline the run was configured for.</p>
 */
public class WeatherBaselineTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.weather_baseline"; }
    @Override public String category() { return "P0/weather"; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        String mode = AdvancedRocketryTestConstants.expectedWeatherMode();
        context.note("expected weather mode: " + mode);

        // Probe at least one dimension to verify the /artest weather get path works.
        List<String> overworld = client.execute("artest weather get 0");
        String joined = String.join("\n", overworld);
        if (!joined.contains("\"isRaining\"")) {
            context.note("overworld weather probe did not return expected schema: " + joined);
            return TestStatus.FAILED;
        }
        context.note("overworld weather probe returned schema-valid response");
        context.note("multi-planet baseline assertions deferred until §7.4 fixture XML lands");
        return TestStatus.SKIPPED;
    }
}
