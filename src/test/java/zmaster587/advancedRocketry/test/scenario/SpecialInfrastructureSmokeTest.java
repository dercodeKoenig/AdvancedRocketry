package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.18 — special infrastructure smoke (railgun, orbital laser drill,
 * laser gun, space elevator, force field projector, beacon, hovercraft).
 *
 * Each device's place + tick + activate assertion requires fixture placement.
 * {@code /artest machine info} surfaces {@code isComplete}/{@code isRunning}
 * for multiblocks; for non-multiblocks (laser gun, beacon) a generic tile probe
 * suffices but state interpretation differs per device. Deferred.
 */
public class SpecialInfrastructureSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.special_infrastructure_smoke"; }
    @Override public String category() { return "P2/special-infra"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) {
        context.note("requires per-device fixture placement — deferred");
        return TestStatus.SKIPPED;
    }
}
