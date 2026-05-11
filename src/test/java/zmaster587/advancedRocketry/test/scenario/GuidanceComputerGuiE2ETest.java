package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.client.ClientBot;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.20 — open guidance computer GUI, insert destination chip, verify the
 * server-side destination via {@code /artest rocket info}.
 *
 * STATUS: skeleton — needs an assembled-rocket fixture (depends on §7.9 rocket
 * assembly path) and a guidance-chip inventory item. Deferred.
 */
public class GuidanceComputerGuiE2ETest extends ClientHarnessBoundScenario {

    @Override public String id() { return "ar.scenario.guidance_computer_gui_e2e"; }
    @Override public String category() { return "P2/client-e2e/guidance"; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient testClient, ClientBot bot) {
        context.note("requires assembled-rocket fixture + guidance-chip probe — deferred");
        return TestStatus.SKIPPED;
    }
}
