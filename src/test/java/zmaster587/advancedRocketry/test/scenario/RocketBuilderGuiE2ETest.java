package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.client.ClientBot;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.20 — open rocket assembling machine GUI, click build/scan, verify
 * server reports an assembled rocket via {@code /artest rocket list}.
 *
 * STATUS: skeleton — needs a placed RocketAssemblingMachine + valid rocket
 * structure (in-game {@code BuildRocketTest} provides a programmatic builder
 * but it requires a player; client harness has one but the BuildRocketTest
 * orchestrator is currently chat-driven). Deferred.
 */
public class RocketBuilderGuiE2ETest extends ClientHarnessBoundScenario {

    @Override public String id() { return "ar.scenario.rocket_builder_gui_e2e"; }
    @Override public String category() { return "P2/client-e2e/rocket-builder"; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient testClient, ClientBot bot) {
        context.note("requires placed RocketAssemblingMachine fixture + valid rocket structure — deferred");
        return TestStatus.SKIPPED;
    }
}
