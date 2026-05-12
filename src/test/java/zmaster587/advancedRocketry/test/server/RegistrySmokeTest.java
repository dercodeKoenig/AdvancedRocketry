package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.2 — registry smoke test.
 *
 * Asserts that the AR mod loaded successfully by querying
 * {@code /artest registry summary} and verifying expected keys + non-zero entity
 * count.
 */
public class RegistrySmokeTest extends AbstractHeadlessServerTest {

    @Test
    public void arRegistriesArePopulated() throws Exception {
        List<String> output = client().execute("artest registry summary");
        String joined = String.join("\n", output);

        assertTrue("registry summary missing 'blocks' key: " + joined,
                joined.contains("\"blocks\":"));
        assertTrue("registry summary missing 'items' key: " + joined,
                joined.contains("\"items\":"));
        assertTrue("registry summary missing 'entities' key: " + joined,
                joined.contains("\"entities\":"));
        assertTrue("registry summary missing 'biomes' key: " + joined,
                joined.contains("\"biomes\":"));

        int entitiesCount = parseIntKey(joined, "entities");
        assertTrue("entity registry suspiciously small (" + entitiesCount
                        + ") — AR may not have loaded",
                entitiesCount > 1);
    }

    private static int parseIntKey(String json, String key) {
        String needle = "\"" + key + "\":";
        int idx = json.indexOf(needle);
        if (idx < 0) return -1;
        int start = idx + needle.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (NumberFormatException e) { return -1; }
    }
}
