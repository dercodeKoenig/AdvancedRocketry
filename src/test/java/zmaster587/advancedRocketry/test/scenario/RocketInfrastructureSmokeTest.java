package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.10 — rocket infrastructure smoke (fueling station, loaders, monitors,
 * linker distance limits).
 *
 * Requires a built rocket plus placed infrastructure tiles. {@code /artest rocket
 * info} surfaces {@code linkedInfrastructure} but probing per-IInfrastructure
 * tile state from a single command requires extending {@code /artest machine info}
 * with infrastructure-specific awareness, OR a dedicated {@code /artest infra
 * <type>} probe. Deferred to a follow-up.
 */
public class RocketInfrastructureSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.rocket_infrastructure_smoke"; }
    @Override public String category() { return "P1/rocket-infrastructure"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // Schema-only: probe infra at origin (no tile) — must return error, not crash.
        java.util.List<String> response = client.execute("artest infra info 0 0 64 0");
        String joined = String.join("\n", response);
        if (!joined.contains("\"error\"") && !joined.contains("\"isInfrastructure\"")) {
            context.note("/artest infra info returned unexpected schema: " + joined);
            return TestStatus.FAILED;
        }
        context.note("/artest infra info schema valid; rocket+linker fixture assertions deferred");
        return TestStatus.SKIPPED;
    }
}
