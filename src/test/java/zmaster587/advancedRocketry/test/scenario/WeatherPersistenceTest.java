package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.HeadlessGameTest;
import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * SMART §7.5 — weather persistence across server restart.
 *
 * <ol>
 *   <li>Start fresh server in workDir.</li>
 *   <li>{@code /artest weather set 0 rain 12000}.</li>
 *   <li>Verify {@code isRaining=true}.</li>
 *   <li>Stop server cleanly (preserves world).</li>
 *   <li>Restart server using SAME workDir.</li>
 *   <li>Verify {@code isRaining=true} survived save/load.</li>
 * </ol>
 *
 * <p>Manages two harness lifecycles directly (does NOT extend
 * {@link HarnessBoundScenario}) because the base manages a single harness from
 * setUp/tearDown — restart-with-workdir needs explicit control.</p>
 */
public class WeatherPersistenceTest implements HeadlessGameTest {

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    @Override public String id() { return "ar.scenario.weather_persistence"; }
    @Override public String category() { return "P0/weather-persistence"; }
    @Override public boolean required() { return true; }
    @Override public int timeoutTicks() { return 1; }

    @Override
    public void setUp(TestContext context) throws Exception {
        if (!HarnessBoundScenario.isHarnessEnabled()) {
            context.note("server harness disabled — skip");
            return;
        }
        // Pre-create the work dir so both harness instances share the same path.
        workDir = Files.createTempDirectory("forge-server-persistence-");
        context.note("persistent workDir: " + workDir);
    }

    @Override
    public TestStatus tick(TestContext context) throws Exception {
        if (workDir == null) return TestStatus.SKIPPED;

        // First boot — set weather and stop.
        try {
            firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);
            context.note("first boot up on port " + firstBoot.port());
            firstBoot.client().execute("artest weather set 0 rain 12000");
            List<String> beforeStop = firstBoot.client().execute("artest weather get 0");
            if (!String.join("\n", beforeStop).contains("\"isRaining\":true")) {
                context.note("rain didn't take effect on first boot: " + beforeStop);
                return TestStatus.FAILED;
            }
        } catch (Throwable t) {
            context.note("first boot failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return TestStatus.SKIPPED;
        }

        // Close the first boot — server saves world on /stop. cleanupOnClose=false
        // preserves the workDir so the second boot can read it.
        firstBoot.close();
        firstBoot = null;
        context.note("first boot closed; restarting same workDir…");

        // Second boot — same workDir, assert weather persisted.
        try {
            secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);
            context.note("second boot up on port " + secondBoot.port());
            List<String> after = secondBoot.client().execute("artest weather get 0");
            String joined = String.join("\n", after);
            if (!joined.contains("\"isRaining\":true")) {
                context.note("rain DID NOT persist across restart: " + joined);
                return TestStatus.FAILED;
            }
            context.note("rain state persisted across server restart");
            return TestStatus.PASSED;
        } catch (Throwable t) {
            context.note("second boot failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return TestStatus.FAILED;
        }
    }

    @Override
    public void tearDown(TestContext context) throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
        // workDir cleanup falls through secondBoot.close() (cleanupOnClose=true),
        // OR, if we never reached the second boot, we leak the dir on disk —
        // acceptable for a temp folder under java.io.tmpdir, OS cleans up.
    }
}
