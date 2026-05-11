package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.client.ClientBot;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.20 — open planet selector / holographic selector GUI, click planet,
 * verify selection state propagates to server.
 *
 * STATUS: skeleton — needs a fixture-placed holographic projector + creative
 * inventory hotbar slot for the selector item, plus a {@code /artest selector
 * info} probe to read server-side selection. Deferred.
 */
public class PlanetSelectorGuiE2ETest extends ClientHarnessBoundScenario {

    @Override public String id() { return "ar.scenario.planet_selector_gui_e2e"; }
    @Override public String category() { return "P2/client-e2e/planet-selector"; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient testClient, ClientBot bot) {
        context.note("requires fixture holographic-projector + selector probe — deferred");
        return TestStatus.SKIPPED;
    }
}
