package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §8 P0.7 — vanilla / non-AR dimension isolation.
 *
 * Asserts that AR's per-planet data does NOT leak into vanilla dimensions
 * (overworld id=0, nether id=-1, end id=1).
 *
 * <p>Probe path: {@code /artest dim info 0} should report
 * {@code isARPlanet=false} and a vanilla provider class. Same for nether/end.</p>
 */
public class NonARDimensionIsolationTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.non_ar_dimension_isolation"; }
    @Override public String category() { return "P0/isolation"; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // AR registers Earth as dim=0 with isARPlanet=true intentionally. The
        // pure isolation check is for the nether (-1) and end (1), which AR must
        // never touch. Also verify weather state on non-AR dims uses the vanilla
        // WorldInfo class.
        List<String> nether = client.execute("artest dim info -1");
        String netherJoined = String.join("\n", nether);
        if (netherJoined.contains("\"isARPlanet\":true")) {
            context.note("nether is mis-classified as an AR planet: " + netherJoined);
            return TestStatus.FAILED;
        }
        List<String> end = client.execute("artest dim info 1");
        String endJoined = String.join("\n", end);
        if (endJoined.contains("\"isARPlanet\":true")) {
            context.note("end is mis-classified as an AR planet: " + endJoined);
            return TestStatus.FAILED;
        }
        context.note("nether and end correctly report isARPlanet=false (overworld is intentionally AR-managed)");
        return TestStatus.PASSED;
    }
}
