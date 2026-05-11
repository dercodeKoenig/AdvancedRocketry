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

        // Required scenarios must not be in FAILED state. SKIPPED is acceptable when
        // an environmental prerequisite (server harness, deobf MC) is not present.
        for (TestOutcome outcome : outcomes) {
            if (outcome.required() && outcome.status() == TestStatus.FAILED) {
                throw new AssertionError(
                        "Required scenario " + outcome.id() + " failed: "
                                + (outcome.failure() == null ? "no failure object" : outcome.failure().toString()));
            }
        }
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
