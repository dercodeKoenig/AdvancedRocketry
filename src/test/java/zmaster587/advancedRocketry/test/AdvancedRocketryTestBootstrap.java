package zmaster587.advancedRocketry.test;

import com.github.stannismod.forge.testing.TestBootstrap;
import com.github.stannismod.forge.testing.TestOutcome;
import com.github.stannismod.forge.testing.TestRegistry;
import com.github.stannismod.forge.testing.TestReportWriter;
import com.github.stannismod.forge.testing.TestStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Entry-point and JUnit-runnable wrapper for the AR scenario suite (SMART §13 step 2).
 *
 * <p>The class doubles as:</p>
 * <ul>
 *   <li>A JUnit test that runs the registered scenarios and asserts a usable
 *       {@code summary.txt} / {@code summary.json} are produced (so a CI-time
 *       gradle test invocation always validates the bootstrap).</li>
 *   <li>A {@code main(String[])} entry-point so the suite can be invoked outside
 *       JUnit (e.g. from a custom Gradle task or manual debugging).</li>
 * </ul>
 *
 * <p>Per SMART §3 (Non-goals) and §15 (Strict prohibitions) — this class MUST NOT
 * import or reference reusable framework internals other than its public API
 * ({@code TestRegistry}, {@code TestBootstrap}, {@code TestReportWriter},
 * {@code TestOutcome}, {@code TestStatus}). All AR-specific scenarios live in
 * {@code zmaster587.advancedRocketry.test.scenario}.</p>
 */
public class AdvancedRocketryTestBootstrap {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void scenarioSuiteRunsAndProducesSummary() throws Exception {
        TestRegistry registry = AdvancedRocketryTestRegistry.composeAll();
        assertFalse("registry must not be empty", registry.tests().isEmpty());

        Path reportRoot = tempFolder.getRoot().toPath();
        List<TestOutcome> outcomes = new TestBootstrap(registry, new TestReportWriter()).run(reportRoot);

        assertNotNull(outcomes);
        assertTrue("at least one scenario must run", outcomes.size() > 0);
        assertTrue("summary.txt must be produced",
                reportRoot.resolve("summary.txt").toFile().exists());
        assertTrue("summary.json must be produced",
                reportRoot.resolve("summary.json").toFile().exists());

        // Per-scenario log so the JUnit test report shows outcomes inline (the
        // framework-native summary.txt is in tempFolder and gets cleaned up by
        // JUnit). Without this, only PASSED/FAILED on the outer test is visible.
        int passed = 0, failed = 0, skipped = 0;
        StringBuilder summary = new StringBuilder("\n=== AR Scenario Suite Outcomes ===\n");
        for (TestOutcome o : outcomes) {
            summary.append(String.format("  %-7s %-12s %-50s%s%n",
                    o.status(), o.category(), o.id(),
                    o.failure() == null ? "" : "  ← " + o.failure().getClass().getSimpleName() + ": "
                            + truncate(String.valueOf(o.failure().getMessage()), 200)));
            // Dump notes for FAILED outcomes — they carry the diagnostic detail
            // emitted by runScenario via context.note(...).
            if (o.status() == TestStatus.FAILED && o.notes() != null) {
                for (String note : o.notes()) {
                    summary.append("        | ").append(truncate(note, 200)).append('\n');
                }
            }
            switch (o.status()) {
                case PASSED:  passed++; break;
                case FAILED:  failed++; break;
                case SKIPPED: skipped++; break;
                default: break;
            }
        }
        summary.append(String.format("Total=%d  PASSED=%d  FAILED=%d  SKIPPED=%d%n",
                outcomes.size(), passed, failed, skipped));
        System.out.println(summary);

        // Required scenarios must not be in FAILED state. SKIPPED is acceptable when
        // an environmental prerequisite (server harness, deobf MC) is not present.
        for (TestOutcome outcome : outcomes) {
            if (outcome.required() && outcome.status() == TestStatus.FAILED) {
                throw new AssertionError(
                        "Required scenario " + outcome.id() + " failed: "
                                + (outcome.failure() == null ? "no failure object" : outcome.failure().toString())
                                + summary);
            }
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    public static void main(String[] args) throws Exception {
        Path reportRoot = java.nio.file.Files.createTempDirectory("ar-scenarios-");
        TestRegistry registry = AdvancedRocketryTestRegistry.composeAll();
        List<TestOutcome> outcomes = new TestBootstrap(registry, new TestReportWriter()).run(reportRoot);
        int passed = 0, failed = 0, skipped = 0;
        for (TestOutcome o : outcomes) {
            switch (o.status()) {
                case PASSED:  passed++; break;
                case FAILED:  failed++; break;
                case SKIPPED: skipped++; break;
                default: break;
            }
        }
        System.out.println("Reports: " + reportRoot.toAbsolutePath());
        System.out.println("Total=" + outcomes.size() + " passed=" + passed + " failed=" + failed + " skipped=" + skipped);
        System.exit(failed == 0 ? 0 : 1);
    }
}
