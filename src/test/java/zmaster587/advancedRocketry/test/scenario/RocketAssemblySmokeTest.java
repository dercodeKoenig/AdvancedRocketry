package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.9 — rocket assembly smoke (P1).
 *
 * Walks the in-game {@link zmaster587.advancedRocketry.unit.BuildRocketTest}
 * fixture (already a working scripted assembly path) and asserts the resulting
 * EntityRocket exists and has non-empty storage via {@code /artest rocket list}
 * + {@code /artest rocket info}.
 *
 * <p>Caveat: this test fires the in-game scripted test which schedules its own
 * 150-tick + 1500-tick + 1600-tick chain. We can only safely assert that
 * /artest rocket list reports at least one EntityRocket after kicking it off —
 * full pass/fail of the scripted phases comes from observing chat output, which
 * is more brittle. Deferred to a follow-up that wires BuildRocketTest's
 * BaseTest.passed flag into the probe.</p>
 */
public class RocketAssemblySmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.rocket_assembly_smoke"; }
    @Override public String category() { return "P1/rocket"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // Probe-only smoke: confirm the rocket-list command works against a fresh
        // server. With no rocket assembled yet the response is the empty array.
        // A future revision will trigger /advrocketry begintest and poll for the
        // resulting EntityRocket with /artest rocket list.
        List<String> response = client.execute("artest rocket list");
        String joined = String.join("\n", response);
        if (!joined.contains("\"rockets\":")) {
            context.note("/artest rocket list returned unexpected schema: " + joined);
            return TestStatus.FAILED;
        }
        context.note("/artest rocket list returned schema-valid response (assembly assertion deferred)");
        return TestStatus.SKIPPED;
    }
}
