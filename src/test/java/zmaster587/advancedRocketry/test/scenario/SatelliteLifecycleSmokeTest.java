package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.12 — satellite lifecycle smoke.
 *
 * Validates {@code /artest satellite list <dim>} schema on Earth (dim=0) and
 * confirms {@code /artest satellite info <dim> <unknown-id>} reports "not found"
 * rather than NPE. Real satellite NBT round-trip is covered by
 * {@code SatellitePropertiesTest} (unit) and a future fixture-bound assertion
 * once a satellite-builder probe lands.
 */
public class SatelliteLifecycleSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.satellite_lifecycle_smoke"; }
    @Override public String category() { return "P1/satellite"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // Schema validation paths.
        List<String> satList = client.execute("artest satellite list 0");
        String joined = String.join("\n", satList);
        if (!joined.contains("\"satellites\":")) {
            context.note("/artest satellite list schema invalid: " + joined);
            return TestStatus.FAILED;
        }

        List<String> satInfo = client.execute("artest satellite info 0 999999");
        String infoJoined = String.join("\n", satInfo);
        if (!infoJoined.contains("\"error\":\"satellite not found\"")) {
            context.note("/artest satellite info <unknown> did not return 'not found': " + infoJoined);
            return TestStatus.FAILED;
        }

        // SatelliteRegistry must contain the canonical AR satellite types.
        List<String> types = client.execute("artest satellite types");
        String typesJoined = String.join("\n", types);
        if (!typesJoined.contains("\"satelliteTypes\":[")) {
            context.note("/artest satellite types schema invalid: " + typesJoined);
            return TestStatus.FAILED;
        }
        // SMART §6.6 lists: optical, density, composition, mass, asteroidMiner,
        // gasCollection, solarEnergy, microwave, oreScanner, biomeChanger,
        // weatherController. The exact registry-name strings used by AR may differ;
        // validate via count rather than exact names — at least 5 types registered.
        int typeCount = countOccurrences(typesJoined, "\"") - 2; // 2 quotes per type, minus the "satelliteTypes":["...", ..."] outer
        // Actually each type adds 2 quotes. typeCount = (totalQuotes - 2) / 2.
        int totalQuotes = countOccurrences(typesJoined, "\"");
        int actualCount = (totalQuotes - 2) / 2;  // -2 for the "satelliteTypes" key
        if (actualCount < 5) {
            context.note("expected ≥5 satellite types registered, got " + actualCount + ": " + typesJoined);
            return TestStatus.FAILED;
        }

        context.note("satellite probes schema-valid; " + actualCount + " types registered");
        return TestStatus.PASSED;
    }

    private static int countOccurrences(String s, String needle) {
        int count = 0, idx = 0;
        while ((idx = s.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
