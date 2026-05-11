package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.HeadlessGameTest;
import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.client.ClientBot;
import com.github.stannismod.forge.testing.client.RealClientHarness;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * Convenience base for client E2E scenarios (SMART §7.20). Spins up BOTH a
 * dedicated server (via {@link RealDedicatedServerHarness}) and a real client
 * (via {@link RealClientHarness}), then dispatches to {@link #runScenario}
 * with a connected {@link ClientBot} for issuing GUI / input commands.
 *
 * <p>Same opt-in gating as {@link HarnessBoundScenario} (via
 * {@code -Dadvancedrocketry.tests.harness=true}) plus an extra
 * {@code -Dadvancedrocketry.tests.clientHarness=true} flag — by default
 * client scenarios skip even when the server harness is enabled because
 * client startup needs a display / OpenGL context that's not always available.</p>
 */
public abstract class ClientHarnessBoundScenario implements HeadlessGameTest {

    /** Defaults to false — set to opt INTO real client harness invocation. */
    public static final String CLIENT_HARNESS_ENABLED_PROPERTY = "advancedrocketry.tests.clientHarness";

    protected static final String CTX_SERVER = "serverHarness";
    protected static final String CTX_CLIENT = "clientHarness";
    protected static final String CTX_SKIP_REASON = "skipReason";

    @Override public int timeoutTicks() { return 1; }

    @Override public boolean required() { return false; }

    public static boolean isClientHarnessEnabled() {
        return Boolean.getBoolean(CLIENT_HARNESS_ENABLED_PROPERTY);
    }

    @Override
    public void setUp(TestContext context) {
        if (!HarnessBoundScenario.isHarnessEnabled()) {
            context.put(CTX_SKIP_REASON,
                    "server harness disabled — set -D" + HarnessBoundScenario.HARNESS_ENABLED_PROPERTY + "=true");
            context.note((String) context.get(CTX_SKIP_REASON));
            return;
        }
        if (!isClientHarnessEnabled()) {
            context.put(CTX_SKIP_REASON,
                    "client harness disabled by default — set -D" + CLIENT_HARNESS_ENABLED_PROPERTY
                            + "=true (requires display / OpenGL context)");
            context.note((String) context.get(CTX_SKIP_REASON));
            return;
        }
        try {
            RealDedicatedServerHarness server = RealDedicatedServerHarness.start();
            context.put(CTX_SERVER, server);
            context.note("server harness up on port " + server.port());
            RealClientHarness client = RealClientHarness.start(server);
            context.put(CTX_CLIENT, client);
            context.note("client harness up, bridge connected");
        } catch (Throwable t) {
            context.put(CTX_SKIP_REASON,
                    "harness setUp failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            context.note((String) context.get(CTX_SKIP_REASON));
        }
    }

    @Override
    public TestStatus tick(TestContext context) throws Exception {
        RealClientHarness client = context.get(CTX_CLIENT);
        if (client == null) {
            return TestStatus.SKIPPED;
        }
        RealDedicatedServerHarness server = context.get(CTX_SERVER);
        return runScenario(context, server.client(), client.bot());
    }

    @Override
    public void tearDown(TestContext context) throws Exception {
        // Order matters — close client first so the server doesn't see a hung
        // connection for 30 seconds during /stop.
        RealClientHarness client = context.get(CTX_CLIENT);
        if (client != null) client.close();
        RealDedicatedServerHarness server = context.get(CTX_SERVER);
        if (server != null) server.close();
    }

    /**
     * Implement client GUI / input + server-state-assertion logic here.
     * @param testClient already-connected to-server console for {@code /artest}
     * @param bot client-side controller (right-click, click button, type, …)
     */
    protected abstract TestStatus runScenario(TestContext context, TestClient testClient, ClientBot bot)
            throws Exception;
}
