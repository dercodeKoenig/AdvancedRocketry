package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.11 — space station lifecycle smoke.
 *
 * On a fresh server with no stations, {@code /artest station list} should report
 * an empty array and the {@code /artest station info <id>} probe must report
 * "not found" rather than NPE. Once a fixture station is created (deferred),
 * the probe values are asserted against expected initial state.
 */
public class SpaceStationLifecycleSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.space_station_lifecycle_smoke"; }
    @Override public String category() { return "P1/station"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        List<String> stationsList = client.execute("artest station list");
        String joined = String.join("\n", stationsList);
        if (!joined.contains("\"stations\":")) {
            context.note("/artest station list schema invalid: " + joined);
            return TestStatus.FAILED;
        }

        // Probe absent station — must return error, not crash.
        List<String> stationInfo = client.execute("artest station info 9999");
        String infoJoined = String.join("\n", stationInfo);
        if (!infoJoined.contains("\"error\":\"station not found\"")) {
            context.note("/artest station info <unknown> did not return 'not found': " + infoJoined);
            return TestStatus.FAILED;
        }

        // On a fresh server, the stations list MUST be empty (no fixtures
        // created yet). Verifying this catches regressions where station data
        // leaks across server boots in the same JVM.
        if (!joined.contains("\"stations\":[]")) {
            context.note("expected empty stations on fresh server, got: " + joined);
            return TestStatus.FAILED;
        }

        // Verify the AR space dimension id is a sentinel (-2 by default).
        String dimList = String.join("\n", client.execute("artest dim list"));
        // Just confirm probe is responsive — actual sentinel value depends on config.
        if (!dimList.contains("\"forgeDimensions\"")) {
            context.note("dim list malformed: " + dimList);
            return TestStatus.FAILED;
        }

        context.note("station probes valid on empty server (no leaked stations, dim probe responsive)");
        return TestStatus.PASSED;
    }
}
