package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.client.ClientBot;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.20 — multi-planet weather isolation as visible from the client.
 *
 * Sequence (after weather B1 lands):
 * <ol>
 *   <li>Teleport client to AR planet A.</li>
 *   <li>{@code /artest weather set <A> rain 12000}.</li>
 *   <li>Verify client-visible rain via {@code bot.reportState()}.</li>
 *   <li>Teleport to AR planet B (clear).</li>
 *   <li>Verify no stale rain on B.</li>
 * </ol>
 *
 * STATUS: skeleton — needs weather B1 implementation (§7.5 expected mode flips
 * from {@code shared} → {@code per_dimension}) and a teleport probe. Deferred.
 */
public class WeatherClientSyncE2ETest extends ClientHarnessBoundScenario {

    @Override public String id() { return "ar.scenario.weather_client_sync_e2e"; }
    @Override public String category() { return "P2/client-e2e/weather"; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient testClient, ClientBot bot) {
        context.note("requires weather B1 + teleport probe — deferred");
        return TestStatus.SKIPPED;
    }
}
