package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.9 — rocket launch through the classic scripted path (P1, critical
 * before any free-flight refactor).
 *
 * <p>Sequence: assembled rocket → countdown → vertical ascent → orbit/station
 * transition. Asserts fuel consumption, in-flight state, guidance computer
 * destination respected.</p>
 *
 * <p>STATUS: skeleton — depends on {@code /artest rocket launch} (SMART §5.5)
 * to drive the launch through the same server-side path as gameplay (NOT by
 * teleporting). Deferred.</p>
 */
public class RocketLaunchSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.rocket_launch_smoke"; }
    @Override public String category() { return "P1/rocket-launch"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) {
        context.note("requires /artest rocket launch probe — deferred");
        return TestStatus.SKIPPED;
    }
}
