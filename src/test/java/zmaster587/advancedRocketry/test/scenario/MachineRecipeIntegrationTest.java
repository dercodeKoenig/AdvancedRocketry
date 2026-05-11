package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.7 — machine + recipe integration.
 *
 * Iterates the AR machine list (cutting / precision assembler / electrolyser /
 * arc furnace / lathe / rolling / centrifuge / etc.), inserts inputs via
 * {@code /artest machine ...} probes, ticks until completion, asserts outputs.
 *
 * <p>STATUS: skeleton — depends on extending {@code /artest machine info} and
 * {@code /artest machine tick-until} probes (SMART §5.4) and on a fixture-mode
 * world that places preset machine multiblocks. Both deferred.</p>
 */
public class MachineRecipeIntegrationTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.machine_recipe_integration"; }
    @Override public String category() { return "P1/machines"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // Probe-wiring smoke for /artest machine tick-until.
        //
        // Full recipe-completion assertion requires placing a complete AR
        // machine multiblock + supplying inputs/power, which is its own fixture
        // engine. Until that lands, validate the probe's error handling:
        //   1. tick-until on empty pos → "no tile entity" error
        //   2. tick-until on a placed chest → "tile lacks isComplete" error
        //      (proves reflection guard catches NoSuchMethodException cleanly)
        //   3. tick-until short-timeout returns matched=false with last-seen value
        //      against a tile that DOES expose isRunning (use placed AR block?
        //      skip — chest is simpler and proves the timeout path).

        java.util.List<String> empty = client.execute("artest machine tick-until 0 100 64 100 complete 5");
        String emptyJoined = String.join("\n", empty);
        if (!emptyJoined.contains("\"error\":\"no tile entity\"")) {
            context.note("tick-until on empty pos didn't error: " + emptyJoined);
            return TestStatus.FAILED;
        }

        client.execute("artest place 0 100 64 100 minecraft:chest");
        java.util.List<String> chestProbe = client.execute("artest machine tick-until 0 100 64 100 complete 5");
        String chestJoined = String.join("\n", chestProbe);
        // TileEntityChest doesn't expose isComplete — should report a controlled
        // error. The exact message includes the qualified method signature
        // (e.g. "tile lacks net.minecraft.tileentity.TileEntityChest.isComplete()");
        // match on the prefix.
        if (!chestJoined.contains("\"error\":\"tile lacks ") || !chestJoined.contains("isComplete")) {
            context.note("tick-until didn't gracefully reject TileEntityChest: " + chestJoined);
            return TestStatus.FAILED;
        }

        context.note("/artest machine tick-until probe wired correctly; full recipe path deferred");
        return TestStatus.PASSED;
    }
}
