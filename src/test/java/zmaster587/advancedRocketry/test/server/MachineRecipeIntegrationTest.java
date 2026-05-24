package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.7 — machine + recipe integration: full end-to-end recipe run.
 *
 * <ol>
 *   <li>{@code /artest fixture machine cutting} builds the multiblock fixture.</li>
 *   <li>{@code /artest machine try-complete} → asserts {@code isComplete=true}.</li>
 *   <li>{@code /artest machine recipe-info TileCuttingMachine 0} returns the
 *       first registered recipe's first ingredient + first output.</li>
 *   <li>{@code /artest hatch fill <inputPos> 0 <ingredient.item> <count>} —
 *       inserts the ingredient into input hatch slot 0.</li>
 *   <li>{@code /artest energy inject <powerPos> 1000000} — fills power hatch.</li>
 *   <li>{@code /artest tile force-tick <controllerPos> 200} — drives recipe
 *       cycle. The recipe time is ≤ 100 ticks for default cutting recipes; 200
 *       gives generous headroom.</li>
 *   <li>{@code /artest hatch read <outputPos>} — asserts the expected output
 *       item appeared in the output hatch.</li>
 * </ol>
 *
 * <p>Keeps probe-wiring smoke (empty pos / non-multiblock tile rejection)
 * because regressions there would mask gameplay failures.</p>
 */
public class MachineRecipeIntegrationTest extends AbstractHeadlessServerTest {

    private static final Pattern INPUT_POS = Pattern.compile("\"inputPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern OUTPUT_POS = Pattern.compile("\"outputPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern POWER_POS = Pattern.compile("\"powerPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern FIRST_INGREDIENT_ITEM =
            Pattern.compile("\"ingredients\":\\[\\{\"slot\":0,\"item\":\"([^\"]+)\",\"count\":(\\d+)");
    private static final Pattern FIRST_OUTPUT_ITEM =
            Pattern.compile("\"outputs\":\\[\\{\"slot\":0,\"item\":\"([^\"]+)\"");

    @Test
    public void probeWiringStillHealthy() throws Exception {
        // tick-until on empty pos → controlled error.
        String empty = String.join("\n",
                client().execute("artest machine tick-until 0 100 64 100 complete 5"));
        assertTrue("tick-until on empty pos didn't error: " + empty,
                empty.contains("\"error\":\"no tile entity\""));

        client().execute("artest place 0 100 64 100 minecraft:chest");
        String chest = String.join("\n",
                client().execute("artest machine tick-until 0 100 64 100 complete 5"));
        assertTrue("tick-until didn't gracefully reject TileEntityChest: " + chest,
                chest.contains("\"error\":\"tile lacks ") && chest.contains("isComplete"));
    }

    @Test
    public void recipesSummaryReportsNonZeroCounts() throws Exception {
        String summary = String.join("\n", client().execute("artest machine recipes-summary"));
        assertTrue("recipes-summary errored: " + summary, !summary.contains("\"error\""));
        String[] requiredMachines = {
                "TileCuttingMachine", "TileElectricArcFurnace", "TileLathe",
                "TileRollingMachine", "TileChemicalReactor",
        };
        StringBuilder failures = new StringBuilder();
        for (String name : requiredMachines) {
            Pattern p = Pattern.compile("\"" + name + "\":(-?\\d+)");
            Matcher m = p.matcher(summary);
            if (!m.find()) { failures.append(name).append("=NOT_REPORTED;"); continue; }
            if (Integer.parseInt(m.group(1)) <= 0) failures.append(name).append("=0;");
        }
        assertTrue("machine recipe counts: " + failures + " full=" + summary,
                failures.length() == 0);
    }

    @Test
    public void cuttingMachineRunsFirstRegisteredRecipe() throws Exception {
        // 1. Build the cutting-machine multiblock.
        int cx = 400, cy = 64, cz = 400;
        String fixture = String.join("\n", client().execute(
                "artest fixture machine cutting 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture machine cutting failed: " + fixture,
                fixture.contains("\"ok\":true"));

        Matcher ipm = INPUT_POS.matcher(fixture);
        Matcher opm = OUTPUT_POS.matcher(fixture);
        Matcher ppm = POWER_POS.matcher(fixture);
        assertTrue("fixture didn't return input/output/power positions: " + fixture,
                ipm.find() && opm.find() && ppm.find());
        String inPos = ipm.group(1) + " " + ipm.group(2) + " " + ipm.group(3);
        String outPos = opm.group(1) + " " + opm.group(2) + " " + opm.group(3);
        String pwrPos = ppm.group(1) + " " + ppm.group(2) + " " + ppm.group(3);

        // 2. Validate multiblock. Use the kit's retry helper — under
        //    parallel-fork pressure `attemptCompleteStructure` rarely loses
        //    the chunk-load + finalization race on the immediate first call
        //    (TASK-16 shape #3).
        String complete = MachineRecipeEndToEndKit.tryCompleteWithRetry(
                client(), 0, cx, cy, cz);
        assertTrue("multiblock not complete: " + complete,
                complete.contains("\"isComplete\":true"));

        // 3. Resolve first recipe ingredient + expected output.
        String recipe = String.join("\n",
                client().execute("artest machine recipe-info TileCuttingMachine 0"));
        assertTrue("recipe-info errored: " + recipe, !recipe.contains("\"error\""));
        Matcher im = FIRST_INGREDIENT_ITEM.matcher(recipe);
        Matcher om = FIRST_OUTPUT_ITEM.matcher(recipe);
        assertTrue("recipe-info missing first ingredient: " + recipe, im.find());
        assertTrue("recipe-info missing first output: " + recipe, om.find());
        String ingredientItem = im.group(1);
        int ingredientCount = Integer.parseInt(im.group(2));
        String expectedOutput = om.group(1);

        // 4. Stuff input hatch.
        String hatchFill = String.join("\n", client().execute(
                "artest hatch fill 0 " + inPos + " 0 " + ingredientItem + " " + ingredientCount));
        assertTrue("hatch fill failed: " + hatchFill, hatchFill.contains("\"ok\":true"));

        // 5. Charge power hatch.
        String inject = String.join("\n", client().execute(
                "artest energy inject 0 " + pwrPos + " 10000000"));
        assertTrue("power inject failed: " + inject, inject.contains("\"ok\":true"));

        // 5b. Flip the machine's enable toggle. libVulpes machines default to
        // disabled until a player flips the GUI switch; tests have to toggle
        // it via reflection.
        String enable = String.join("\n", client().execute(
                "artest machine set-enabled 0 " + cx + " " + cy + " " + cz + " true"));
        assertTrue("machine set-enabled failed: " + enable,
                enable.contains("\"ok\":true") && enable.contains("\"enabled\":true"));

        // 6. Drive ticks in batches and poll the output hatch each batch.
        //    Default cutting recipes take ~100 ticks; serial budget 300 was
        //    enough but parallel-3-fork pressure stretches effective tick
        //    rate (server thread shared across forks). Budget 12×100=1200
        //    ticks (4× the recipe length) absorbs the worst case observed
        //    in the 10× testServer rerun under load. Early-exit keeps the
        //    happy-path cost at ~1 batch.
        String out = "n/a";
        boolean found = false;
        for (int batch = 0; batch < 12; batch++) {
            String tick = String.join("\n", client().execute(
                    "artest tile force-tick 0 " + cx + " " + cy + " " + cz + " 100"));
            assertTrue("force-tick failed: " + tick, tick.contains("\"ok\":true"));
            out = String.join("\n", client().execute("artest hatch read 0 " + outPos));
            assertTrue("hatch read errored: " + out, !out.contains("\"error\""));
            if (out.contains("\"item\":\"" + expectedOutput + "\"")) {
                found = true;
                break;
            }
        }
        assertTrue("expected output " + expectedOutput
                        + " not in output hatch — recipe didn't complete: ingredient=" + ingredientItem
                        + " last response=" + out,
                found);
    }
}
