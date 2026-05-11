package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.13 — atmosphere / oxygen gameplay smoke.
 *
 * Validates {@code /artest atmosphere get <dim> <x> <y> <z>} schema on Earth
 * (dim=0) — must report a breathable atmosphere by default. Suit / vacuum /
 * tank-charge interactions need a player fixture and are exercised in
 * {@code OxygenSuitClientStateE2ETest} (§7.20, client-side, deferred).
 */
public class AtmosphereOxygenSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.atmosphere_oxygen_smoke"; }
    @Override public String category() { return "P1/atmosphere"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        List<String> atm = client.execute("artest atmosphere get 0 0 70 0");
        String joined = String.join("\n", atm);
        if (joined.contains("\"error\"")) {
            context.note("/artest atmosphere get returned error on Earth dim=0: " + joined);
            return TestStatus.FAILED;
        }
        if (!joined.contains("\"breathable\":true")) {
            context.note("Earth atmosphere unexpectedly not breathable: " + joined);
            return TestStatus.FAILED;
        }
        context.note("Earth atmosphere reports breathable=true at (0,70,0)");
        // Vacuum / sealed-room assertions require placed oxygen vent fixture.
        return TestStatus.PASSED;
    }
}
