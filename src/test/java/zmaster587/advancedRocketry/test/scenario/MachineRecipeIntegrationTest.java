package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMART §7.7 — machine + recipe integration.
 *
 * <p>The 11 canonical AR multiblock recipe machines load their recipes from
 * config/advRocketry/*.xml during AR's {@code preInit}. This scenario verifies:</p>
 *
 * <ol>
 *   <li>{@code /artest machine recipes-summary} reports a non-zero recipe count
 *       for every machine class (proves the XML loaders + libVulpes'
 *       {@code RecipesMachine} registration both work).</li>
 *   <li>The probe-wiring smoke for {@code /artest machine tick-until}: empty pos
 *       and a non-multiblock tile (vanilla chest) both return controlled errors
 *       instead of NPE.</li>
 * </ol>
 *
 * <p>Full multiblock recipe execution (place machine, supply inputs+power, wait
 * for output) requires per-machine fixture geometry — every machine has a
 * different structure shape. That's a fixture-engine effort, deferred. The
 * recipe-count assertion is the practical regression tripwire that catches XML
 * loader / classloader / recipe-handler-registration regressions.</p>
 */
public class MachineRecipeIntegrationTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.machine_recipe_integration"; }
    @Override public String category() { return "P1/machines"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // 1. Probe-wiring smoke (kept from previous version — guards against
        //    NPE regressions in the probe itself).
        String empty = String.join("\n",
                client.execute("artest machine tick-until 0 100 64 100 complete 5"));
        if (!empty.contains("\"error\":\"no tile entity\"")) {
            context.note("tick-until on empty pos didn't error: " + empty);
            return TestStatus.FAILED;
        }
        client.execute("artest place 0 100 64 100 minecraft:chest");
        String chest = String.join("\n",
                client.execute("artest machine tick-until 0 100 64 100 complete 5"));
        if (!chest.contains("\"error\":\"tile lacks ") || !chest.contains("isComplete")) {
            context.note("tick-until didn't gracefully reject TileEntityChest: " + chest);
            return TestStatus.FAILED;
        }

        // 2. Real recipe-registry assertion. Each AR machine MUST have >0 recipes.
        String summary = String.join("\n", client.execute("artest machine recipes-summary"));
        if (summary.contains("\"error\"")) {
            context.note("recipes-summary errored: " + summary);
            return TestStatus.FAILED;
        }
        // Required machines (a subset that's stable across AR builds — these XML
        // files always ship with content).
        // Subset of machines that always ship with non-empty XML configs in
        // upstream AR. {@code TilePrecisionAssembler} is intentionally omitted —
        // its XML is empty in some configurations (recipes added by user via
        // datapacks / external configs).
        String[] requiredMachines = {
                "TileCuttingMachine",
                "TileElectricArcFurnace",
                "TileLathe",
                "TileRollingMachine",
                "TileChemicalReactor",
        };
        StringBuilder failures = new StringBuilder();
        for (String name : requiredMachines) {
            Pattern p = Pattern.compile("\"" + name + "\":(-?\\d+)");
            Matcher m = p.matcher(summary);
            if (!m.find()) {
                failures.append(name).append("=NOT_REPORTED;");
                continue;
            }
            int count = Integer.parseInt(m.group(1));
            if (count <= 0) {
                failures.append(name).append("=").append(count).append(";");
            }
        }
        if (failures.length() > 0) {
            context.note("machine recipe counts: " + failures + " full=" + summary);
            return TestStatus.FAILED;
        }

        context.note("all required AR machines have recipes loaded: " + summary);
        return TestStatus.PASSED;
    }
}
