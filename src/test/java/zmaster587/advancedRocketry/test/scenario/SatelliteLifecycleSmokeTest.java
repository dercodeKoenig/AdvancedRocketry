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

        context.note("satellite probes schema-valid; mission lifecycle assertions deferred");
        return TestStatus.SKIPPED;
    }
}
