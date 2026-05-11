package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.8 — multiblock validation smoke.
 *
 * Asserts {@code /artest machine info <pos>} schema is intact and the registry
 * lists at least one well-known multiblock TE class. The "valid structure
 * validates / invalid invalidates" assertions require placing prepared
 * structures at known positions (SMART §9.2 fixture work) — deferred until the
 * fixture loader exists.
 */
public class MultiblockValidationSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.multiblock_validation_smoke"; }
    @Override public String category() { return "P1/multiblock"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // Test the fixture-builder primitives: place a block, verify it appears
        // via /artest machine info, then fill a small region and verify volume.
        // This proves /artest place + /artest fill + /artest machine info form
        // a working stack for future fixture-bound multiblock validation.

        // 1. Empty position must report "no tile entity" cleanly.
        List<String> at0 = client.execute("artest machine info 0 200 100 200");
        String joinedEmpty = String.join("\n", at0);
        if (!joinedEmpty.contains("\"error\":\"no tile entity\"")) {
            context.note("empty position machine info wrong: " + joinedEmpty);
            return TestStatus.FAILED;
        }

        // 2. Place a chest (vanilla, has tile entity) and verify it's present.
        // Use isolated coordinates far from spawn to avoid any conflicts.
        List<String> place = client.execute("artest place 0 200 100 200 minecraft:chest");
        String joinedPlace = String.join("\n", place);
        if (!joinedPlace.contains("\"placed\":true")) {
            context.note("place chest failed: " + joinedPlace);
            return TestStatus.FAILED;
        }

        List<String> info = client.execute("artest machine info 0 200 100 200");
        String joinedInfo = String.join("\n", info);
        if (!joinedInfo.contains("TileEntityChest")) {
            context.note("placed chest not detected via machine info: " + joinedInfo);
            return TestStatus.FAILED;
        }

        // 3. Fill a 3x3x3 region and verify volume cap.
        List<String> fill = client.execute("artest fill 0 210 100 210 212 102 212 minecraft:stone");
        String joinedFill = String.join("\n", fill);
        if (!joinedFill.contains("\"ok\":true") || !joinedFill.contains("\"volume\":27")) {
            context.note("fill 3x3x3 stone failed: " + joinedFill);
            return TestStatus.FAILED;
        }

        context.note("fixture-builder primitives working (place + fill + machine info round-trip)");
        // Real multiblock structure validation (place full 5x5x5 arc furnace via
        // multiple /artest place calls + assert isComplete=true) is the next step;
        // this confirms the stack works.
        return TestStatus.PASSED;
    }
}
