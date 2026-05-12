package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.19 — commands smoke.
 *
 * Asserts that both {@code /artest} (test-only) and AR's primary command
 * ({@code advancedrocketry}/{@code advrocketry}/{@code ar}) are registered on a
 * fresh server.
 */
public class CommandsSmokeTest extends AbstractHeadlessServerTest {

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
}
