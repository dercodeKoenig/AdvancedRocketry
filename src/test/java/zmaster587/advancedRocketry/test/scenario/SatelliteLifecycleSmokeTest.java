package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.12 — satellite lifecycle.
 *
 * Registry has ≥5 types → create real {@link
 * zmaster587.advancedRocketry.api.satellite.SatelliteBase} via probe → assert
 * list/info reflect it.
 */
public class SatelliteLifecycleSmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

    @Test
    public void satelliteCreatePopulatesDimensionProperties() throws Exception {
        // Registry sanity.
        String types = String.join("\n", client().execute("artest satellite types"));
        assertTrue("satellite types schema invalid: " + types,
                types.contains("\"satelliteTypes\":["));
        int totalQuotes = countOccurrences(types, "\"");
        int actualCount = (totalQuotes - 2) / 2; // -2 for "satelliteTypes" key quotes
        assertTrue("expected ≥5 satellite types, got " + actualCount + ": " + types,
                actualCount >= 5);

        // Create real satellite.
        String create = String.join("\n", client().execute(
                "artest satellite create 0 solarEnergy 250 5000 1024"));
        assertTrue("satellite create failed: " + create, create.contains("\"ok\":true"));

        Matcher m = ID_PATTERN.matcher(create);
        assertTrue("could not extract satellite id from: " + create, m.find());
        long satId = Long.parseLong(m.group(1));

        String list = String.join("\n", client().execute("artest satellite list 0"));
        assertTrue("created satellite " + satId + " not in list: " + list,
                list.contains("\"id\":" + satId));

        String info = String.join("\n", client().execute("artest satellite info 0 " + satId));
        assertTrue("info missing/wrong type: " + info, info.contains("\"type\":\"solarEnergy\""));
        assertTrue("info missing/wrong powerGen: " + info, info.contains("\"powerGen\":250"));
        assertTrue("info missing/wrong powerStorage: " + info, info.contains("\"powerStorage\":5000"));
    }

    private static int countOccurrences(String s, String needle) {
        int c = 0, i = 0;
        while ((i = s.indexOf(needle, i)) != -1) { c++; i += needle.length(); }
        return c;
    }
}
