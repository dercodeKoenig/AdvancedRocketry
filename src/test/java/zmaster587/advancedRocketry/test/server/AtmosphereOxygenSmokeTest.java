package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.13 — atmosphere / oxygen gameplay.
 *
 * Earth breathable by default → set density 0 → vacuum → restore.
 */
public class AtmosphereOxygenSmokeTest extends AbstractHeadlessServerTest {

    @Test
    public void earthDensityZeroFlipsAtmosphereToVacuum() throws Exception {
        String baseline = String.join("\n", client().execute("artest atmosphere get 0 0 70 0"));
        assertTrue("baseline atmosphere probe errored: " + baseline,
                !baseline.contains("\"error\""));
        assertTrue("baseline Earth not breathable — env contamination? " + baseline,
                baseline.contains("\"breathable\":true"));

        String planet = String.join("\n", client().execute("artest planet info 0"));
        int originalDensity = extractInt(planet, "\"atmosphereDensity\":(-?\\d+)");
        assertTrue("could not read Earth atmosphereDensity: " + planet, originalDensity >= 0);

        try {
            String setResp = String.join("\n", client().execute("artest atmosphere set-density 0 0"));
            assertTrue("set-density failed: " + setResp, setResp.contains("\"ok\":true"));
            assertTrue("set-density did not stick: " + setResp,
                    setResp.contains("\"newDensity\":0"));

            String vacResp = String.join("\n", client().execute("artest atmosphere get 0 0 70 0"));
            assertTrue("density=0 should yield non-breathable, got: " + vacResp,
                    vacResp.contains("\"breathable\":false"));
        } finally {
            client().execute("artest atmosphere set-density 0 " + originalDensity);
        }
    }

    private static int extractInt(String haystack, String regex) {
        Matcher m = Pattern.compile(regex).matcher(haystack);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
}
