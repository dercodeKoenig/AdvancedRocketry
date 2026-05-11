package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.19 — commands smoke.
 *
 * Asserts the AR-managed and test-only commands are registered on a fresh
 * server: {@code advancedrocketry} (alias {@code advrocketry}/{@code ar}) and
 * {@code artest} (test mode only).
 */
public class CommandsSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.commands_smoke"; }
    @Override public String category() { return "P2/commands"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        List<String> cmds = client.execute("artest commands list");
        String joined = String.join("\n", cmds);
        if (!joined.contains("\"commands\":[")) {
            context.note("/artest commands list schema invalid: " + joined);
            return TestStatus.FAILED;
        }
        // The probe itself should register, plus AR's primary command.
        boolean hasArtest = joined.contains("\"artest\"");
        boolean hasAR = joined.contains("\"advancedrocketry\"") || joined.contains("\"advrocketry\"")
                || joined.contains("\"ar\"");
        if (!hasArtest) {
            context.note("/artest itself missing from command list (test mode broken?): " + joined);
            return TestStatus.FAILED;
        }
        if (!hasAR) {
            context.note("AR's primary command missing from command list: " + joined);
            return TestStatus.FAILED;
        }
        context.note("AR + /artest commands registered as expected");
        return TestStatus.PASSED;
    }
}
