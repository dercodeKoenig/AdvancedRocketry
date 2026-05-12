package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.14 — terraforming smoke.
 *
 * Drives {@link
 * zmaster587.advancedRocketry.dimension.DimensionProperties#setAtmosphereDensity(int)}
 * and verifies that {@code currentAtmosphere} changes while {@code originalAtmosphere}
 * is preserved (terraforming reversibility invariant).
 */
public class TerraformingSmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern ORIG = Pattern.compile("\"originalAtmosphere\":(-?\\d+)");
    private static final Pattern CURRENT = Pattern.compile("\"currentAtmosphere\":(-?\\d+)");

    @Test
    public void mutationKeepsOriginalDensityIntact() throws Exception {
        String before = String.join("\n", client().execute("artest terraforming info 0"));
        assertTrue("baseline terraforming info errored: " + before,
                !before.contains("\"error\""));

        Matcher om = ORIG.matcher(before), cm = CURRENT.matcher(before);
        assertTrue("could not extract original/current from: " + before, om.find() && cm.find());
        int original = Integer.parseInt(om.group(1));
        int currentBefore = Integer.parseInt(cm.group(1));

        int target = currentBefore == 25 ? 75 : 25;
        try {
            String set = String.join("\n",
                    client().execute("artest terraforming set-density 0 " + target));
            assertTrue("set-density did not stick: " + set,
                    set.contains("\"ok\":true") && set.contains("\"newDensity\":" + target));

            String after = String.join("\n", client().execute("artest terraforming info 0"));
            Matcher om2 = ORIG.matcher(after), cm2 = CURRENT.matcher(after);
            assertTrue("could not extract from post-mutation: " + after,
                    om2.find() && cm2.find());

            assertEquals("currentAtmosphere did not move to " + target + ": " + after,
                    target, Integer.parseInt(cm2.group(1)));
            assertEquals("originalAtmosphere unexpectedly mutated: " + after,
                    original, Integer.parseInt(om2.group(1)));
            assertTrue("proxylists not reported: " + after,
                    after.contains("\"proxyInitialized\""));
        } finally {
            client().execute("artest terraforming set-density 0 " + currentBefore);
        }
    }
}
