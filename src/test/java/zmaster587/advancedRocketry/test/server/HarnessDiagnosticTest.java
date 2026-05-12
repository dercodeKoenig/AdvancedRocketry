package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import org.junit.Assume;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Diagnostic test — boots ONE server, dumps transcript regardless of outcome.
 *
 * <p>Run with:</p>
 * <pre>{@code
 *   ./gradlew testAdvancedRocketryScenarios \
 *       --tests "zmaster587.advancedRocketry.test.HarnessDiagnosticTest"
 * }</pre>
 *
 * <p>Doesn't extend {@link AbstractHeadlessServerTest} on purpose — this test
 * needs to inspect the transcript even when {@code start()} fails (which
 * {@code @Before} can't gracefully recover from). Manages the harness manually
 * inside the {@code @Test} method.</p>
 */
public class HarnessDiagnosticTest {

    @Test
    public void bootOneServerAndDumpTranscript() throws Exception {
        Assume.assumeTrue(
                "Harness disabled — set -D" + AbstractHeadlessServerTest.PROP_HARNESS_ENABLED + "=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));

        System.out.println("[diagnostic] Launcher class: "
                + System.getProperty("forge.test.launcher.class.server", "(default)"));
        System.out.println("[diagnostic] Assets dir:     "
                + System.getProperty("forge.test.assets.dir", "(default)"));
        System.out.println("[diagnostic] Legacy args:    "
                + System.getProperty("forge.test.launcher.legacyArgs", "(default true)"));
        System.out.println("[diagnostic] Test classpath has "
                + System.getProperty("java.class.path").split(System.getProperty("path.separator")).length
                + " entries");

        RealDedicatedServerHarness harness = null;
        try {
            harness = RealDedicatedServerHarness.start();
            System.out.println("[diagnostic] Harness started successfully on port " + harness.port());
            System.out.println("[diagnostic] Running /list to verify command path…");
            List<String> listOut = harness.client().execute("list");
            System.out.println("[diagnostic] /list returned " + listOut.size() + " lines:");
            listOut.forEach(line -> System.out.println("  | " + line));
        } catch (Throwable t) {
            System.out.println("[diagnostic] Harness FAILED: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            t.printStackTrace(System.out);
        } finally {
            if (harness != null) {
                try {
                    Method tx = harness.client().getClass().getDeclaredMethod("transcriptSnapshot");
                    tx.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<String> transcript = (List<String>) tx.invoke(harness.client());
                    System.out.println("[diagnostic] Captured " + transcript.size()
                            + " transcript lines (last 80):");
                    int from = Math.max(0, transcript.size() - 80);
                    for (int i = from; i < transcript.size(); i++) {
                        System.out.println("  > " + transcript.get(i));
                    }
                } catch (Throwable reflectError) {
                    System.out.println("[diagnostic] Could not access transcript: " + reflectError);
                }
                try { harness.close(); } catch (Throwable ignored) {}
            }
        }
    }
}
