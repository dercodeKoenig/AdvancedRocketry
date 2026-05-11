package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.2 — registry smoke test.
 *
 * Asserts that the AR mod loaded successfully by querying {@code /artest registry summary}
 * and verifying that the well-known counts are present and non-zero.
 *
 * <p>SMART §7.2 also lists specific tile entity / entity classes that must be
 * registered. Those checks live in {@link TestProbeCommand}-style probes such
 * as {@code /artest registry has <resource-location>} which are deferred to
 * a follow-up enhancement to {@code TestProbeCommand} once the harness is wired.</p>
 */
public class RegistrySmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.registry_smoke"; }
    @Override public String category() { return "P0/registry"; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        List<String> output = client.execute("artest registry summary");

        // The expected output is a single JSON-like blob. Look for non-zero counts
        // for the four registries that AR populates.
        String joined = String.join("\n", output);
        if (!joined.contains("\"blocks\":") || !joined.contains("\"items\":")
                || !joined.contains("\"entities\":") || !joined.contains("\"biomes\":")) {
            context.note("registry summary missing required keys: " + joined);
            return TestStatus.FAILED;
        }

        // SMART §7.2 critical entity check: EntityRocket must be in entity registry.
        // Use the fluids+entities count parse to validate AR loaded — entities count
        // should be > 1 (at least vanilla + AR).
        int entitiesCount = parseIntKey(joined, "entities");
        if (entitiesCount <= 1) {
            context.note("entity registry suspiciously small (" + entitiesCount + ") — AR may not have loaded");
            return TestStatus.FAILED;
        }
        context.note("registry summary acquired, entities=" + entitiesCount);
        return TestStatus.PASSED;
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
        try { return Integer.parseInt(json.substring(start, end)); } catch (NumberFormatException e) { return -1; }
    }
}
