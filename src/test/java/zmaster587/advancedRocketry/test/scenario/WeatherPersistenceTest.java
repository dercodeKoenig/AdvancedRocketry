package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.5 — weather persistence across server restart.
 *
 * <p>Sequence:</p>
 * <ol>
 *   <li>Start server with a known temp world dir.</li>
 *   <li>{@code /artest weather set 0 rain 12000}.</li>
 *   <li>Stop the server cleanly.</li>
 *   <li>Restart with the SAME world dir (requires
 *       {@code RealDedicatedServerHarness.startWith(workDir)} — not yet exposed
 *       by the framework).</li>
 *   <li>{@code /artest weather get 0} — assert isRaining=true.</li>
 * </ol>
 *
 * <p>STATUS: skeleton — depends on framework support for restarting against an
 * existing work directory.</p>
 */
public class WeatherPersistenceTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.weather_persistence"; }
    @Override public String category() { return "P0/weather-persistence"; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) {
        context.note("requires framework to expose RealDedicatedServerHarness.startWith(workDir) — deferred");
        return TestStatus.SKIPPED;
    }
}
