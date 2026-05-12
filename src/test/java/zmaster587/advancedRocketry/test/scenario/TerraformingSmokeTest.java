package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMART §7.14 — terraforming smoke.
 *
 * <p>Drives the same {@link
 * zmaster587.advancedRocketry.dimension.DimensionProperties#setAtmosphereDensity(int)}
 * path that a working atmosphere terraformer multiblock invokes each cycle, and
 * verifies the canonical terraforming invariants:</p>
 *
 * <ul>
 *   <li>{@code originalAtmosphereDensity} is preserved across mutations
 *       (this is what makes terraforming reversible / abandonable).</li>
 *   <li>{@code currentAtmosphereDensity} tracks the new value.</li>
 *   <li>The {@code DimensionProperties.proxylists} state remains queryable
 *       (no NPE on terraforming-helper load).</li>
 *   <li>The probe path is restorable so subsequent scenarios see Earth at its
 *       original density.</li>
 * </ul>
 *
 * <p>Driving the full multiblock terraformer ({@code TileAtmosphereTerraformer})
 * requires placing a valid structure + powering it for several minutes — out of
 * scope for a smoke scenario.</p>
 */
public class TerraformingSmokeTest extends HarnessBoundScenario {

    private static final Pattern ORIG = Pattern.compile("\"originalAtmosphere\":(-?\\d+)");
    private static final Pattern CURRENT = Pattern.compile("\"currentAtmosphere\":(-?\\d+)");

    @Override public String id() { return "ar.scenario.terraforming_smoke"; }
    @Override public String category() { return "P2/terraforming"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // 1. Baseline.
        String before = String.join("\n", client.execute("artest terraforming info 0"));
        if (before.contains("\"error\"")) {
            context.note("baseline terraforming info errored: " + before);
            return TestStatus.FAILED;
        }
        Matcher om = ORIG.matcher(before), cm = CURRENT.matcher(before);
        if (!om.find() || !cm.find()) {
            context.note("could not extract original/current from: " + before);
            return TestStatus.FAILED;
        }
        int original = Integer.parseInt(om.group(1));
        int currentBefore = Integer.parseInt(cm.group(1));

        int target = currentBefore == 25 ? 75 : 25;
        try {
            String set = String.join("\n", client.execute("artest terraforming set-density 0 " + target));
            if (!set.contains("\"ok\":true") || !set.contains("\"newDensity\":" + target)) {
                context.note("set-density did not stick: " + set);
                return TestStatus.FAILED;
            }

            String after = String.join("\n", client.execute("artest terraforming info 0"));
            Matcher om2 = ORIG.matcher(after), cm2 = CURRENT.matcher(after);
            if (!om2.find() || !cm2.find()) {
                context.note("could not extract from post-mutation: " + after);
                return TestStatus.FAILED;
            }
            int originalAfter = Integer.parseInt(om2.group(1));
            int currentAfter = Integer.parseInt(cm2.group(1));

            if (currentAfter != target) {
                context.note("currentAtmosphere did not move to " + target + ": " + after);
                return TestStatus.FAILED;
            }
            if (originalAfter != original) {
                context.note("originalAtmosphere unexpectedly mutated " + original + " → "
                        + originalAfter + ": " + after);
                return TestStatus.FAILED;
            }
            if (!after.contains("\"proxyInitialized\"")) {
                context.note("proxylists not reported: " + after);
                return TestStatus.FAILED;
            }

            context.note("terraforming mutation: current " + currentBefore + " → " + currentAfter
                    + " (original=" + original + " preserved)");
        } finally {
            client.execute("artest terraforming set-density 0 " + currentBefore);
        }

        return TestStatus.PASSED;
    }
}
