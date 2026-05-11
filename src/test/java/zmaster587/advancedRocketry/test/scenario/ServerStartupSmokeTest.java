package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.1 — server startup + minimum command round-trip smoke test.
 *
 * <ol>
 *   <li>Start the dedicated server harness (handled by {@link HarnessBoundScenario#setUp}).</li>
 *   <li>Run {@code /list} as a basic command-round-trip check.</li>
 *   <li>Run {@code /artest registry summary} to prove the test-only probe is registered.</li>
 *   <li>Assert no startup crash; capture transcript for the report.</li>
 * </ol>
 */
public class ServerStartupSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.server_startup_smoke"; }
    @Override public String category() { return "P0/server-lifecycle"; }
    @Override public int timeoutTicks() { return 1; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        List<String> listOutput = client.execute("list");
        context.note("/list output lines: " + listOutput.size());

        List<String> registry = client.execute("artest registry summary");
        boolean hasRegistryOutput = registry.stream().anyMatch(line -> line.contains("\"blocks\""));
        if (!hasRegistryOutput) {
            return TestStatus.FAILED;
        }
        context.note("/artest registry summary returned " + registry.size() + " lines");
        return TestStatus.PASSED;
    }
}
