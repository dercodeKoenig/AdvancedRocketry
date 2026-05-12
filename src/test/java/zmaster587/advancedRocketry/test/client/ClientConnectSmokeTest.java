package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.20 — basic client-bridge handshake. Asserts the client connects,
 * world becomes available, and {@code reportState} round-trips a player view.
 */
public class ClientConnectSmokeTest extends AbstractClientE2ETest {

    @Test
    public void clientReportsStateOverBridge() throws Exception {
        bot().waitForWorld();
        JsonObject state = bot().reportState();
        assertNotNull("client reportState returned null", state);
        assertTrue("client reportState missing 'ok' key: " + state, state.has("ok"));
    }
}
