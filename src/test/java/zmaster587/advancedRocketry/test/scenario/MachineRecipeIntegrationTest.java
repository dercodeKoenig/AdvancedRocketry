package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.7 — machine + recipe integration.
 *
 * Probe-wiring smoke + recipe-registry assertion: every required AR machine
 * class must have >0 recipes loaded by libVulpes' {@code RecipesMachine}.
 */
public class MachineRecipeIntegrationTest extends AbstractHeadlessServerTest {

    @Test
    public void recipeRegistryAndTickUntilProbeAreWired() throws Exception {
        // 1. Probe wiring smoke.
        String empty = String.join("\n",
                client().execute("artest machine tick-until 0 100 64 100 complete 5"));
        assertTrue("tick-until on empty pos didn't error: " + empty,
                empty.contains("\"error\":\"no tile entity\""));

        client().execute("artest place 0 100 64 100 minecraft:chest");
        String chest = String.join("\n",
                client().execute("artest machine tick-until 0 100 64 100 complete 5"));
        assertTrue("tick-until didn't gracefully reject TileEntityChest: " + chest,
                chest.contains("\"error\":\"tile lacks ") && chest.contains("isComplete"));

        // 2. Recipe-registry assertion.
        String summary = String.join("\n", client().execute("artest machine recipes-summary"));
        assertTrue("recipes-summary errored: " + summary, !summary.contains("\"error\""));

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
        assertTrue("machine recipe counts: " + failures + " full=" + summary,
                failures.length() == 0);
    }
}
