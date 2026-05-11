package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.client.ClientBot;
import com.github.stannismod.forge.testing.server.TestClient;
import com.google.gson.JsonObject;

/**
 * SMART §7.20 — basic client-bridge handshake.
 *
 * Asserts the client connects, world becomes available, and {@code reportState}
 * round-trips a player view from the bridge. This is the minimum proof that the
 * client harness is functional end-to-end.
 */
public class ClientConnectSmokeTest extends ClientHarnessBoundScenario {

    @Override public String id() { return "ar.scenario.client_connect_smoke"; }
    @Override public String category() { return "P2/client-e2e"; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient testClient, ClientBot bot) throws Exception {
        bot.waitForWorld();
        JsonObject state = bot.reportState();
        if (state == null || !state.has("ok")) {
            context.note("client reportState returned malformed payload: " + state);
            return TestStatus.FAILED;
        }
        context.note("client reportState OK; bridge handshake complete");
        return TestStatus.PASSED;
    }
}
