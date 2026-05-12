package zmaster587.advancedRocketry.test;

import com.github.stannismod.forge.testing.HeadlessGameTest;
import com.github.stannismod.forge.testing.TestBootstrap;
import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestOutcome;
import com.github.stannismod.forge.testing.TestRegistry;
import com.github.stannismod.forge.testing.TestReportWriter;
import com.github.stannismod.forge.testing.TestStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the Forge test framework
 * ({@code com.github.stannismod.forge:forge-test-framework:<ver>:dev}, resolved via
 * mavenLocal or composite build — see {@code settings.gradle.kts}) is correctly
 * wired into the AR test source set, that JUnit 4 runs, and that the orchestrator
 * can execute a no-op scenario and produce summary.txt / summary.json.
 *
 * If this test fails, no other AR test can run — fix the build wiring first.
 */
public class FrameworkWiringSmokeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void registryAndOrchestratorRunNoopScenarioAndWriteReports() throws Exception {
        TestRegistry registry = new TestRegistry().register(new NoopTest());
        TestBootstrap bootstrap = new TestBootstrap(registry, new TestReportWriter());

        List<TestOutcome> outcomes = bootstrap.run(tempFolder.getRoot().toPath());

        assertEquals(1, outcomes.size());
        TestOutcome outcome = outcomes.get(0);
        assertEquals(TestStatus.PASSED, outcome.status());
        assertEquals("ar.framework.smoke.noop", outcome.id());
        assertTrue("summary.txt should be written",
                tempFolder.getRoot().toPath().resolve("summary.txt").toFile().exists());
        assertTrue("summary.json should be written",
                tempFolder.getRoot().toPath().resolve("summary.json").toFile().exists());
    }

    private static final class NoopTest implements HeadlessGameTest {
        @Override public String id() { return "ar.framework.smoke.noop"; }
        @Override public String category() { return "framework-smoke"; }
        @Override public boolean required() { return true; }
        @Override public int timeoutTicks() { return 1; }
        @Override public void setUp(TestContext context) { }
        @Override public TestStatus tick(TestContext context) { return TestStatus.PASSED; }
        @Override public void tearDown(TestContext context) { }
    }
}
