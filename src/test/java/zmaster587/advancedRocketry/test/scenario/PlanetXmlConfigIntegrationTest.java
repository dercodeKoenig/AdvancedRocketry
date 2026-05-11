package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.4 — planet XML config integration test.
 *
 * <p>Asserts that AR's default planet (Earth, dim=0) is registered with the
 * expected schema after server start. Real fixture-XML round-trip (custom
 * planet → on-disk XML → server reload → assert fields preserved) requires
 * pre-populating the harness work directory with a fixture XML before server
 * startup; that piece of the framework is not yet exposed.</p>
 */
public class PlanetXmlConfigIntegrationTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.planet_xml_config_integration"; }
    @Override public String category() { return "P0/planet-config"; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // Earth (dim=0) is the canonical AR-managed dim. Validate the planet
        // info schema — every key SMART §5.3 requires.
        List<String> earth = client.execute("artest planet info 0");
        String joined = String.join("\n", earth);

        if (joined.contains("\"error\"")) {
            // dim=0 not registered as an AR planet (e.g. galaxy-less config).
            context.note("Earth (dim=0) not registered as an AR planet: " + joined);
            return TestStatus.SKIPPED;
        }

        // Required schema keys per SMART §5.3.
        for (String key : new String[] {
                "\"name\":", "\"starId\":", "\"atmosphereDensity\":", "\"gravity\":",
                "\"orbitalDistance\":", "\"rotationalPeriod\":", "\"seaLevel\":",
                "\"rainStartLength\":", "\"thunderStartLength\":", "\"rainMarker\":", "\"thunderMarker\":"
        }) {
            if (!joined.contains(key)) {
                context.note("planet info missing required key " + key + ": " + joined);
                return TestStatus.FAILED;
            }
        }
        context.note("planet info schema for Earth (dim=0) is valid");
        // Full round-trip with a fixture XML deferred.
        return TestStatus.SKIPPED;
    }
}
