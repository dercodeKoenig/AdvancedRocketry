package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.6 — world persistence / restart smoke.
 *
 * <p>Same pre-requisite as {@link WeatherPersistenceTest}: the framework must
 * support restarting against an existing work directory. Deferred.</p>
 */
public class PersistenceRestartSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.persistence_restart_smoke"; }
    @Override public String category() { return "P1/persistence"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) {
        context.note("requires framework to expose RealDedicatedServerHarness.startWith(workDir) — deferred");
        return TestStatus.SKIPPED;
    }
}
