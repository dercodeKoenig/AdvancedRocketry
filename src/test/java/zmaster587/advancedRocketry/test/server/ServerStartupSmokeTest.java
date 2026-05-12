package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.1 — server startup + minimum command round-trip smoke test.
 *
 * <ol>
 *   <li>Start the dedicated server harness ({@code @Before} in
 *       {@link AbstractHeadlessServerTest}).</li>
 *   <li>Run {@code /list} as a basic command-round-trip check.</li>
 *   <li>Run {@code /artest registry summary} to prove the test-only probe is registered.</li>
 * </ol>
 */
public class ServerStartupSmokeTest extends AbstractHeadlessServerTest {

    @Test
    public void serverBootsAndCommandsRoundTrip() throws Exception {
        // Server is already up; harness boot waited for the "For help" marker
        // before returning from @Before.
        List<String> listOutput = client().execute("list");
        assertTrue("/list returned no output", !listOutput.isEmpty());

        List<String> registry = client().execute("artest registry summary");
        boolean hasRegistryOutput = registry.stream().anyMatch(line -> line.contains("\"blocks\""));
        assertTrue("/artest registry summary missing 'blocks' key: " + registry,
                hasRegistryOutput);
    }
}
