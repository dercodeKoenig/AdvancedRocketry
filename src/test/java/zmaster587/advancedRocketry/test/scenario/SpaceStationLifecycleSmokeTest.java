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

        context.note("station probes schema-valid on empty server; lifecycle assertions deferred");
        return TestStatus.SKIPPED;
    }
}
