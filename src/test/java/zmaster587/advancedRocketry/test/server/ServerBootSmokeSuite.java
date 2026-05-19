package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * TASK-10 Phase 2 (B3) — server-boot smoke suite.
 *
 * <p>Consolidates 2 single-method smoke classes that previously each spawned
 * their own dedicated-server JVM into a single {@link AbstractSharedServerTest}
 * subclass (one boot for both methods).</p>
 *
 * <h2>Consolidated from</h2>
 * <ul>
 *   <li>{@code ServerStartupSmokeTest} → {@link #serverBootsAndCommandsRoundTrip()}</li>
 *   <li>{@code RegistrySmokeTest}      → {@link #arRegistriesArePopulated()}</li>
 * </ul>
 *
 * <h2>Not folded in</h2>
 * <ul>
 *   <li>{@code CommandsSmokeTest} — 4 methods, already extends
 *       {@link AbstractSharedServerTest} since TASK-03 B2.</li>
 *   <li>{@code HarnessDiagnosticTest} — 2 methods, standalone harness
 *       lifecycle. Diagnostic-purposed; keep separate.</li>
 *   <li>{@code NonARDimensionIsolationTest} — 2 methods that explicitly need
 *       a pristine JVM to count fresh registry entries (see
 *       {@link AbstractSharedServerTest} "When NOT to use this base").</li>
 * </ul>
 *
 * <h2>State-leak audit</h2>
 *
 * <p>Neither method mutates world state, atmosphere, time, or weather.
 * Both are pure server-state read probes.</p>
 */
public class ServerBootSmokeSuite extends AbstractSharedServerTest {

    // ─────────────────────────────────────────────────────────────────────
    // From ServerStartupSmokeTest — SMART §7.1
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void serverBootsAndCommandsRoundTrip() throws Exception {
        List<String> listOutput = client().execute("list");
        assertTrue("/list returned no output", !listOutput.isEmpty());

        List<String> registry = client().execute("artest registry summary");
        boolean hasRegistryOutput = registry.stream().anyMatch(line -> line.contains("\"blocks\""));
        assertTrue("/artest registry summary missing 'blocks' key: " + registry,
                hasRegistryOutput);
    }

    // ─────────────────────────────────────────────────────────────────────
    // From RegistrySmokeTest — SMART §7.2
    // ─────────────────────────────────────────────────────────────────────

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
