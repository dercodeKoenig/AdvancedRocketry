package zmaster587.advancedRocketry.test;

import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
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
 * <p>This is intentionally OUTSIDE the AdvancedRocketryTestRegistry so it doesn't
 * count toward suite outcomes — it's purely for inspecting server stdout when the
 * harness is failing to satisfy {@code awaitOutputContaining("For help, type ...")}.</p>
 */
public class HarnessDiagnosticTest {

    @Test
    public void bootOneServerAndDumpTranscript() throws Exception {
        if (!Boolean.getBoolean("advancedrocketry.tests.harness")) {
            System.out.println("[diagnostic] Harness disabled — set -Dadvancedrocketry.tests.harness=true to run");
            return;
        }

        System.out.println("[diagnostic] Launcher class: " + System.getProperty("forge.test.launcher.class.server", "(default)"));
        System.out.println("[diagnostic] Assets dir:     " + System.getProperty("forge.test.assets.dir", "(default)"));
        System.out.println("[diagnostic] Legacy args:    " + System.getProperty("forge.test.launcher.legacyArgs", "(default true)"));
        System.out.println("[diagnostic] Test classpath has " + System.getProperty("java.class.path").split(System.getProperty("path.separator")).length + " entries");

        RealDedicatedServerHarness harness = null;
        try {
            harness = RealDedicatedServerHarness.start();
            System.out.println("[diagnostic] Harness started successfully on port " + harness.port());
            System.out.println("[diagnostic] Running /list to verify command path…");
            List<String> listOut = harness.client().execute("list");
            System.out.println("[diagnostic] /list returned " + listOut.size() + " lines:");
            listOut.forEach(line -> System.out.println("  | " + line));
        } catch (Throwable t) {
            System.out.println("[diagnostic] Harness FAILED: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            // Even if start() failed, the TestClient may have captured transcript before
            // the timeout. Try to reflect into the most recent server work dir to see
            // if the JVM logged anything.
            t.printStackTrace(System.out);
        } finally {
            if (harness != null) {
                try {
                    Method tx = harness.client().getClass().getDeclaredMethod("transcriptSnapshot");
                    tx.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<String> transcript = (List<String>) tx.invoke(harness.client());
                    System.out.println("[diagnostic] Captured " + transcript.size() + " transcript lines (last 80):");
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
