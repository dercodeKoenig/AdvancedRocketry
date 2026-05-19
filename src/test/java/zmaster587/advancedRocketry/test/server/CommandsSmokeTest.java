package zmaster587.advancedRocketry.test.server;

// migrated to AbstractSharedServerTest (TASK-03 B2)
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.19 — commands smoke.
 *
 * Asserts that both {@code /artest} (test-only) and AR's primary command
 * ({@code advancedrocketry}/{@code advrocketry}/{@code ar}) are registered on a
 * fresh server.
 */
public class CommandsSmokeTest extends AbstractSharedServerTest {

    @Test
    public void primaryCommandsAreRegistered() throws Exception {
        String joined = String.join("\n", client().execute("artest commands list"));
        assertTrue("/artest commands list schema invalid: " + joined,
                joined.contains("\"commands\":["));
        assertTrue("/artest itself missing from command list (test mode broken?): " + joined,
                joined.contains("\"artest\""));
        boolean hasAR = joined.contains("\"advancedrocketry\"") || joined.contains("\"advrocketry\"")
                || joined.contains("\"ar\"");
        assertTrue("AR's primary command missing from command list: " + joined, hasAR);
    }

    @Test
    public void arHelpCommandPrintsUsageWithoutCrash() throws Exception {
        // §7.19: AR's primary command must surface usage text without crashing
        // the server. WorldCommand.execute(args=["help"]) prints a
        // "Subcommands:" header followed by the subcommand list; pin both.
        String help = String.join("\n", client().execute("advancedrocketry help"));
        assertTrue("AR help did not include the Subcommands header: " + help,
                help.contains("Subcommands:"));

        // Sanity: server is still responsive after running help.
        String alive = String.join("\n", client().execute("artest commands list"));
        assertTrue("server unresponsive after /advancedrocketry help: " + alive,
                alive.contains("\"commands\":["));
    }

    @Test
    public void arCommandWithInvalidArgsReturnsErrorNotCrash() throws Exception {
        // §7.19: malformed input must not crash the server. WorldCommand.execute
        // currently has no `default` branch — unknown subcommands silently
        // no-op. That is lenient but not a crash, which is what this test
        // pins. If AR ever tightens parsing to surface an explicit error
        // reply, strengthen this assertion accordingly.
        client().execute("advancedrocketry totally-bogus-subcommand-name");

        String alive = String.join("\n", client().execute("artest commands list"));
        assertTrue("server unresponsive after malformed /advancedrocketry: " + alive,
                alive.contains("\"commands\":["));
    }

    @Test
    public void artestRegistryWithBadSubcommandReturnsError() throws Exception {
        // §7.19: /artest itself MUST surface unknown subcommands as a
        // structured JSON error reply, not crash or no-op. Pinned against
        // TestProbeCommand.handleRegistry's "unknown registry subcommand"
        // fallback branch.
        String reply = String.join("\n", client().execute("artest registry bogus"));
        assertTrue("expected JSON error for unknown registry subcommand, got: " + reply,
                reply.contains("\"error\"") && reply.contains("unknown registry subcommand"));
    }
}
