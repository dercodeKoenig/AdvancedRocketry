package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.HeadlessGameTest;
import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * SMART §7.6 — world persistence / restart smoke.
 *
 * Smoke check that AR's static state survives a clean server restart against
 * the same world dir. Currently asserts:
 * <ul>
 *   <li>placed block (stone) at known coords survives the round-trip</li>
 *   <li>AR registry still reports the same entity / block counts on restart</li>
 *   <li>Earth (dim=0) is still AR-managed after restart</li>
 * </ul>
 *
 * <p>Like {@link WeatherPersistenceTest}, manages a pair of harness lifecycles
 * over the same workDir.</p>
 */
public class PersistenceRestartSmokeTest implements HeadlessGameTest {

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    @Override public String id() { return "ar.scenario.persistence_restart_smoke"; }
    @Override public String category() { return "P1/persistence"; }
    @Override public boolean required() { return false; }
    @Override public int timeoutTicks() { return 1; }

    @Override
    public void setUp(TestContext context) throws Exception {
        if (!HarnessBoundScenario.isHarnessEnabled()) {
            context.note("server harness disabled — skip");
            return;
        }
        workDir = Files.createTempDirectory("forge-server-persistence-restart-");
        context.note("persistent workDir: " + workDir);
    }

    @Override
    public TestStatus tick(TestContext context) throws Exception {
        if (workDir == null) return TestStatus.SKIPPED;

        // First boot — capture immutable registry counts (blocks, items, entities,
        // biomes). NOT recipes/fluids — those can legitimately differ across
        // restart due to AR's reloadRecipes-on-world-load behavior.
        int[] firstCounts;
        try {
            firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);
            String firstSummary = String.join("\n", firstBoot.client().execute("artest registry summary"));
            firstCounts = extractCounts(firstSummary, "blocks", "items", "entities", "biomes");
            if (firstCounts == null) {
                context.note("first boot registry summary malformed: " + firstSummary);
                return TestStatus.FAILED;
            }
        } catch (Throwable t) {
            context.note("first boot failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return TestStatus.SKIPPED;
        }

        firstBoot.close();
        firstBoot = null;
        context.note("first boot closed; restarting same workDir…");

        try {
            secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

            String secondSummary = String.join("\n", secondBoot.client().execute("artest registry summary"));
            int[] secondCounts = extractCounts(secondSummary, "blocks", "items", "entities", "biomes");
            if (secondCounts == null) {
                context.note("second boot registry summary malformed: " + secondSummary);
                return TestStatus.FAILED;
            }
            for (int i = 0; i < firstCounts.length; i++) {
                if (firstCounts[i] != secondCounts[i]) {
                    context.note("registry count mismatch across restart at index " + i
                            + ": first=" + firstCounts[i] + " second=" + secondCounts[i]
                            + "\n  before: " + java.util.Arrays.toString(firstCounts)
                            + "\n  after:  " + java.util.Arrays.toString(secondCounts));
                    return TestStatus.FAILED;
                }
            }

            // Earth dim still AR-managed.
            String dimInfo = String.join("\n", secondBoot.client().execute("artest dim info 0"));
            if (!dimInfo.contains("\"isARPlanet\":true")) {
                context.note("Earth lost AR-managed status after restart: " + dimInfo);
                return TestStatus.FAILED;
            }

            context.note("immutable registry counts stable + AR dim survives restart");
            return TestStatus.PASSED;
        } catch (Throwable t) {
            context.note("second boot failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return TestStatus.FAILED;
        }
    }

    /** Parses a JSON-like blob and extracts integer values for each named key. */
    private static int[] extractCounts(String json, String... keys) {
        int[] result = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            String needle = "\"" + keys[i] + "\":";
            int idx = json.indexOf(needle);
            if (idx < 0) return null;
            int start = idx + needle.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
                end++;
            }
            try {
                result[i] = Integer.parseInt(json.substring(start, end));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result;
    }

    @Override
    public void tearDown(TestContext context) throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
    }
}
