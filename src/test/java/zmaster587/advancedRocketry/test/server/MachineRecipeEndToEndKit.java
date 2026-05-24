package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.server.TestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-18 — shared protocol for industrial-machine recipe end-to-end tests.
 *
 * <p>All 9 AR multiblock industrial machines share the broad recipe
 * pipeline shape, but specific machines vary on:</p>
 *
 * <ul>
 *   <li><b>Item ingredients</b> — some recipes have item inputs (RollingMachine,
 *       Lathe, Crystallizer, PrecisionLaserEtcher), some are fluid-only
 *       (ChemicalReactor rocketfuel = oxygen + hydrogen → rocketfuel).</li>
 *   <li><b>Fluid ingredients</b> — many require fluid in the liquid input
 *       hatch (RollingMachine pressuretank needs 100mB water).</li>
 *   <li><b>Item outputs vs fluid outputs</b> — most produce items, some
 *       produce fluids (ChemicalReactor rocketfuel).</li>
 *   <li><b>Hatch presence</b> — fluid-only machines (Electrolyser, Centrifuge,
 *       ChemicalReactor) have no 'I' / 'O' chars in their structure — only
 *       'L' / 'l' / 'P'. Tests must handle missing item-hatch positions.</li>
 * </ul>
 *
 * <p>Recipe selection: always the first registered recipe — discovered
 * via {@code RecipesMachine.getInstance().getRecipes(MachineClass)}.
 * No hardcoded item/fluid identities anywhere; tests stay valid as long
 * as the machine has at least one recipe and the recipe-info probe
 * reports it.</p>
 *
 * <p>Out of scope: wildcard-based machines (ArcFurnace, PrecisionAssembler)
 * place hatches via {@code '*'} wildcards rather than explicit
 * 'I' / 'O' / 'P' chars. The kit's generic fixture handler can't compute
 * hatch positions for them. Tracked separately in TASK-26.</p>
 */
final class MachineRecipeEndToEndKit {

    private static final Pattern INPUT_POS         = Pattern.compile("\"inputPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern OUTPUT_POS        = Pattern.compile("\"outputPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern POWER_POS         = Pattern.compile("\"powerPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern LIQUID_INPUT_POS  = Pattern.compile("\"liquidInputPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern LIQUID_OUTPUT_POS = Pattern.compile("\"liquidOutputPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");

    /** Captures every ingredient slot — slot index, item id, count, meta. */
    private static final Pattern ANY_INGREDIENT =
            Pattern.compile("\\{\"slot\":(\\d+),\"item\":\"([^\"]+)\",\"count\":(\\d+),\"meta\":(\\d+)");
    /** Captures every output slot — slot index, item id (meta optional). */
    private static final Pattern ANY_OUTPUT =
            Pattern.compile("\\{\"slot\":(\\d+),\"item\":\"([^\"]+)\"");
    private static final Pattern FLUID_INGREDIENT =
            Pattern.compile("\\{\"fluid\":\"([^\"]+)\",\"amount\":(\\d+)\\}");

    private MachineRecipeEndToEndKit() {}

    // ---- Position discovery -------------------------------------------------

    /** Positions reported by the fixture probe. Each list contains all
     *  positions of the corresponding hatch char (some machines have
     *  multiples — ChemicalReactor has two 'L' liquid inputs). Empty
     *  list = no hatch of that type in the machine's structure. */
    static final class FixturePositions {
        final List<String> inputPositions;        // 'I'
        final List<String> outputPositions;       // 'O'
        final List<String> powerPositions;        // 'P'  (required: non-empty)
        final List<String> liquidInputPositions;  // 'L'
        final List<String> liquidOutputPositions; // 'l'
        final String fullResp;
        FixturePositions(List<String> in, List<String> out, List<String> pwr,
                         List<String> lin, List<String> lout, String resp) {
            this.inputPositions = in; this.outputPositions = out;
            this.powerPositions = pwr; this.liquidInputPositions = lin;
            this.liquidOutputPositions = lout; this.fullResp = resp;
        }
        String firstInput()       { return inputPositions.isEmpty()        ? null : inputPositions.get(0); }
        String firstOutput()      { return outputPositions.isEmpty()       ? null : outputPositions.get(0); }
        String firstPower()       { return powerPositions.get(0); }
        String firstLiquidInput() { return liquidInputPositions.isEmpty()  ? null : liquidInputPositions.get(0); }
        String firstLiquidOutput(){ return liquidOutputPositions.isEmpty() ? null : liquidOutputPositions.get(0); }
    }

    static FixturePositions placeFixture(TestClient c, String fixtureKey,
                                         int cx, int cy, int cz) throws Exception {
        String resp = String.join("\n", c.execute(
                "artest fixture machine " + fixtureKey + " 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture machine " + fixtureKey + " failed: " + resp,
                resp.contains("\"ok\":true"));
        List<String> in   = matchAllPos(resp, "inputPositions");
        List<String> out  = matchAllPos(resp, "outputPositions");
        List<String> pwr  = matchAllPos(resp, "powerPositions");
        List<String> lin  = matchAllPos(resp, "liquidInputPositions");
        List<String> lout = matchAllPos(resp, "liquidOutputPositions");
        assertTrue("fixture machine " + fixtureKey
                        + " did not report any powerPositions (required): " + resp,
                !pwr.isEmpty());
        return new FixturePositions(in, out, pwr, lin, lout, resp);
    }

    /** Extract a list of "x y z" strings from a JSON field like
     *  {@code "<key>":[[x,y,z],[x,y,z]]}. Returns empty if key absent. */
    private static List<String> matchAllPos(String resp, String key) {
        String marker = "\"" + key + "\":[";
        int idx = resp.indexOf(marker);
        if (idx < 0) return Collections.emptyList();
        int start = idx + marker.length();
        // Find matching ']' — scan until first ']' at the same nesting level.
        // The contents are pure "[a,b,c],[d,e,f]" with no nested objects.
        int depth = 1, end = -1;
        for (int i = start; i < resp.length(); i++) {
            char ch = resp.charAt(i);
            if (ch == '[') depth++;
            else if (ch == ']') { depth--; if (depth == 0) { end = i; break; } }
        }
        if (end < 0) return Collections.emptyList();
        String section = resp.substring(start, end);
        Pattern triple = Pattern.compile("\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
        Matcher m = triple.matcher(section);
        List<String> all = new ArrayList<>();
        while (m.find()) all.add(m.group(1) + " " + m.group(2) + " " + m.group(3));
        return all;
    }

    /**
     * Drives {@code /artest machine try-complete} with a TASK-16-shape-#3 retry
     * shim. Returns the response from the last attempt that produced
     * {@code attempted:true}, or the response from the final retry on
     * timeout. Callers must assert their own {@code isComplete} expectation
     * — this helper only guarantees that the validator actually ran.
     *
     * <p>The race: {@code attemptCompleteStructure} occasionally returns
     * {@code false} on the immediate first call after the fixture is built
     * (chunk-load + finalization race). Re-invoking it across the natural
     * tick gap between two probe round-trips lets the finalization settle.
     * Budget: 8 attempts × 500 ms gap (~4 s ceiling on the non-happy path;
     * ~0 ms cost when the first call succeeds — which is the common case).
     * Earlier 5×200ms budget proved insufficient under parallel-3-fork
     * pressure during the TASK-27 10× rerun on multiple multiblocks
     * (ArcFurnace, PrecisionLaserEtcher, Beacon).</p>
     */
    static String tryCompleteWithRetry(TestClient c, int dim, int cx, int cy, int cz) throws Exception {
        String resp = null;
        for (int attempt = 0; attempt < 8; attempt++) {
            resp = String.join("\n",
                    c.execute("artest machine try-complete " + dim + " " + cx + " " + cy + " " + cz));
            if (resp.contains("\"attempted\":true")) return resp;
            Thread.sleep(500);
        }
        return resp;
    }

    static void assertFixtureValidates(TestClient c, int cx, int cy, int cz,
                                       String tag, String fixtureResp) throws Exception {
        // TASK-16 shape #3 mitigation — see tryCompleteWithRetry above.
        StringBuilder attempts = new StringBuilder();
        String resp = null;
        for (int attempt = 0; attempt < 8; attempt++) {
            resp = String.join("\n",
                    c.execute("artest machine try-complete 0 " + cx + " " + cy + " " + cz));
            if (resp.contains("\"isComplete\":true")) return;
            attempts.append("\n  attempt ").append(attempt + 1).append(": ").append(resp);
            Thread.sleep(500);
        }
        throw new AssertionError(tag + " — multiblock not complete after 8 attempts"
                + attempts + "\n  fixture: " + fixtureResp);
    }

    // ---- Recipe discovery --------------------------------------------------

    /** Full first-recipe info from the recipe-info probe. */
    static final class FirstRecipe {
        final List<String[]> itemIngredients;  // {slot, item, count, meta}
        final List<String[]> itemOutputs;      // {slot, item}
        final List<String[]> fluidIngredients; // {fluid, amount}
        final List<String[]> fluidOutputs;     // {fluid, amount}
        final int time;                        // recipe.getTime() — ticks needed
        final String raw;
        FirstRecipe(List<String[]> ii, List<String[]> io,
                    List<String[]> fi, List<String[]> fo, int time, String raw) {
            this.itemIngredients = ii; this.itemOutputs = io;
            this.fluidIngredients = fi; this.fluidOutputs = fo;
            this.time = time; this.raw = raw;
        }
    }

    private static final Pattern TIME_FIELD = Pattern.compile("\"time\":(\\d+)");

    static FirstRecipe resolveFirstRecipe(TestClient c, String tileShortName) throws Exception {
        String resp = String.join("\n",
                c.execute("artest machine recipe-info " + tileShortName + " 0"));
        assertTrue("recipe-info errored for " + tileShortName + ": " + resp,
                !resp.contains("\"error\""));
        int time = 0;
        Matcher tm = TIME_FIELD.matcher(resp);
        if (tm.find()) time = Integer.parseInt(tm.group(1));
        return new FirstRecipe(
                parseSection(resp, "\"ingredients\":[", ANY_INGREDIENT, 4),
                parseSection(resp, "\"outputs\":[",     ANY_OUTPUT,     2),
                parseSection(resp, "\"fluidIngredients\":[", FLUID_INGREDIENT, 2),
                parseSection(resp, "\"fluidOutputs\":[",     FLUID_INGREDIENT, 2),
                time, resp);
    }

    private static List<String[]> parseSection(String resp, String key,
                                               Pattern pattern, int groupCount) {
        int idx = resp.indexOf(key);
        if (idx < 0) return Collections.emptyList();
        int start = idx + key.length();
        int end = resp.indexOf(']', start);
        if (end < 0) return Collections.emptyList();
        String section = resp.substring(start, end);
        Matcher m = pattern.matcher(section);
        List<String[]> out = new ArrayList<>();
        while (m.find()) {
            String[] groups = new String[groupCount];
            for (int i = 0; i < groupCount; i++) groups[i] = m.group(i + 1);
            out.add(groups);
        }
        return out;
    }

    // ---- Sub-test #1: fixture validates -----------------------------------

    static void runFixtureValidates(TestClient c, String fixtureKey,
                                    int cx, int cy, int cz) throws Exception {
        FixturePositions p = placeFixture(c, fixtureKey, cx, cy, cz);
        assertFixtureValidates(c, cx, cy, cz, fixtureKey, p.fullResp);
    }

    // ---- Sub-test #2: machine runs first recipe end-to-end -----------------

    /**
     * TASK-28 F3 — same as {@link #runFirstRecipeEndToEnd} except output
     * identity is NOT asserted. Returns the final output-hatch read so the
     * caller can apply a permissive assertion (e.g. "any item present").
     * Use for machines whose recipe set shares input keys and whose
     * runtime recipe-selection order differs from
     * {@code recipe-info 0} (Centrifuge).
     */
    static String runFirstRecipeEndToEndPermissive(TestClient c, String fixtureKey,
                                                   String tileShortName,
                                                   int cx, int cy, int cz) throws Exception {
        FixturePositions p = placeFixture(c, fixtureKey, cx, cy, cz);
        assertFixtureValidates(c, cx, cy, cz, fixtureKey, p.fullResp);
        FirstRecipe r = resolveFirstRecipe(c, tileShortName);
        fillItemIngredients(c, fixtureKey, p, r.itemIngredients);
        fillFluidIngredients(c, fixtureKey, p, r.fluidIngredients);
        String inject = String.join("\n", c.execute(
                "artest energy inject 0 " + p.firstPower() + " 10000000"));
        assertTrue("power inject failed: " + inject, inject.contains("\"ok\":true"));
        String enable = String.join("\n", c.execute(
                "artest machine set-enabled 0 " + cx + " " + cy + " " + cz + " true"));
        assertTrue("machine set-enabled failed: " + enable,
                enable.contains("\"ok\":true") && enable.contains("\"enabled\":true"));
        int tickBudget = Math.max(2000, r.time + 1000);
        String tick = String.join("\n", c.execute(
                "artest tile force-tick 0 " + cx + " " + cy + " " + cz + " " + tickBudget));
        assertTrue("force-tick failed: " + tick, tick.contains("\"ok\":true"));
        return String.join("\n", c.execute("artest hatch read 0 " + p.firstOutput()));
    }

    static void runFirstRecipeEndToEnd(TestClient c, String fixtureKey,
                                       String tileShortName,
                                       int cx, int cy, int cz) throws Exception {
        FixturePositions p = placeFixture(c, fixtureKey, cx, cy, cz);
        assertFixtureValidates(c, cx, cy, cz, fixtureKey, p.fullResp);
        FirstRecipe r = resolveFirstRecipe(c, tileShortName);
        assertTrue("recipe-info has no outputs (item or fluid) for "
                        + tileShortName + " — can't end-to-end test: " + r.raw,
                !r.itemOutputs.isEmpty() || !r.fluidOutputs.isEmpty());

        fillItemIngredients(c, fixtureKey, p, r.itemIngredients);
        fillFluidIngredients(c, fixtureKey, p, r.fluidIngredients);

        String inject = String.join("\n", c.execute(
                "artest energy inject 0 " + p.firstPower() + " 10000000"));
        assertTrue("power inject failed for " + fixtureKey + ": " + inject,
                inject.contains("\"ok\":true"));

        String enable = String.join("\n", c.execute(
                "artest machine set-enabled 0 " + cx + " " + cy + " " + cz + " true"));
        assertTrue("machine set-enabled failed for " + fixtureKey + ": " + enable,
                enable.contains("\"ok\":true") && enable.contains("\"enabled\":true"));

        // Force-tick budget adapts to the recipe's declared completion time.
        // Most AR machine recipes are <500 ticks; the wildcard-structure
        // machines from TASK-26 push higher (ArcFurnace=6000, PrecisionAssembler=4000).
        // Floor of 2000 keeps the 7 TASK-18 machines on their original budget;
        // ceiling extends to `time + 1000` for the long ones.
        int tickBudget = Math.max(2000, r.time + 1000);
        String tick = String.join("\n", c.execute(
                "artest tile force-tick 0 " + cx + " " + cy + " " + cz + " " + tickBudget));
        assertTrue("force-tick failed for " + fixtureKey + ": " + tick,
                tick.contains("\"ok\":true"));

        // Input-drain check — pins the "recipe consumed its ingredients"
        // contract. Without this, a regression where the machine generates
        // output items without consuming inputs (free-output exploit) would
        // slip through — the output assertion below would still pass.
        //
        // Soft form: at least ONE ingredient slot must have changed from its
        // initial state. Some recipes legitimately use catalysts that stay
        // (e.g. PrecisionLaserEtcher's lens) — requiring every slot to drain
        // would false-positive on those. But if ALL slots remain at initial
        // count after recipe-time × N cycles, the recipe did not actually run.
        if (!r.itemIngredients.isEmpty()) {
            String inputRead = String.join("\n", c.execute("artest hatch read 0 " + p.firstInput()));
            boolean anyDrained = false;
            for (String[] ing : r.itemIngredients) {
                String stillUntouched = "\"slot\":" + ing[0] + ",\"item\":\""
                        + ing[1] + "\",\"count\":" + ing[2];
                if (!inputRead.contains(stillUntouched)) { anyDrained = true; break; }
            }
            assertTrue("no input items consumed for " + fixtureKey
                            + " — recipe appears to run but every ingredient slot still "
                            + "holds the full initial count (potential free-output regression; "
                            + "expected at least one slot drained, catalysts aside): " + inputRead,
                    anyDrained);
        }

        // Output check — item output OR fluid output depending on the recipe.
        if (!r.itemOutputs.isEmpty()) {
            String expectedItem = r.itemOutputs.get(0)[1];
            assertTrue(fixtureKey + " produces item " + expectedItem
                            + " but has no outputPos ('O' in structure)",
                    p.firstOutput() != null);
            String read = String.join("\n", c.execute("artest hatch read 0 " + p.firstOutput()));
            assertTrue("hatch read errored for " + fixtureKey + ": " + read,
                    !read.contains("\"error\""));
            assertTrue("expected output " + expectedItem
                            + " not in output hatch — recipe did not complete for "
                            + fixtureKey + " (item-inputs=" + r.itemIngredients.size()
                            + ", fluid-inputs=" + r.fluidIngredients.size()
                            + ", response=" + read + ")",
                    read.contains("\"item\":\"" + expectedItem + "\""));
        }
        if (!r.fluidOutputs.isEmpty()) {
            String expectedFluid = r.fluidOutputs.get(0)[0];
            assertTrue(fixtureKey + " produces fluid " + expectedFluid
                            + " but has no liquidOutputPos ('l' in structure)",
                    p.firstLiquidOutput() != null);
            // Scan ALL liquid output hatches — output may land in any of
            // them (controller picks the first hatch that can accept).
            boolean found = false;
            StringBuilder seen = new StringBuilder();
            for (String pos : p.liquidOutputPositions) {
                String read = String.join("\n", c.execute("artest fluid stored 0 " + pos));
                seen.append(pos).append(" → ").append(read).append('\n');
                if (read.contains("\"fluid\":\"" + expectedFluid + "\"")) {
                    found = true; break;
                }
            }
            assertTrue("expected output fluid " + expectedFluid
                            + " not in any liquid output hatch for " + fixtureKey
                            + " (item-inputs=" + r.itemIngredients.size()
                            + ", fluid-inputs=" + r.fluidIngredients.size()
                            + "):\n" + seen,
                    found);
        }
    }

    // ---- helpers -----------------------------------------------------------

    private static void fillItemIngredients(TestClient c, String fixtureKey,
                                            FixturePositions p,
                                            List<String[]> items) throws Exception {
        if (items.isEmpty()) return;
        assertTrue("recipe needs item inputs but fixture " + fixtureKey
                        + " has no inputPos ('I' in structure): " + p.fullResp,
                p.firstInput() != null);
        for (String[] ing : items) {
            // hatch fill <dim> <pos> <slot> <itemId> [count] [meta]
            String fill = String.join("\n", c.execute(
                    "artest hatch fill 0 " + p.firstInput() + " " + ing[0] + " "
                            + ing[1] + " " + ing[2] + " " + ing[3]));
            assertTrue("hatch fill (slot " + ing[0] + " " + ing[1]
                            + ":" + ing[3] + " ×" + ing[2] + ") failed for "
                            + fixtureKey + ": " + fill,
                    fill.contains("\"ok\":true"));
        }
    }

    private static void fillFluidIngredients(TestClient c, String fixtureKey,
                                             FixturePositions p,
                                             List<String[]> fluids) throws Exception {
        if (fluids.isEmpty()) return;
        assertTrue("recipe needs " + fluids.size() + " fluid input(s) but fixture "
                        + fixtureKey + " has " + p.liquidInputPositions.size()
                        + " liquid input hatch(es) ('L' in structure): " + p.fullResp,
                fluids.size() <= p.liquidInputPositions.size());
        // Each fluid goes into a SEPARATE hatch — TileFluidHatch tanks hold
        // exactly one fluid type at a time, so ChemicalReactor's two-fluid
        // recipes need two distinct 'L' positions.
        for (int i = 0; i < fluids.size(); i++) {
            String[] f = fluids.get(i);
            String pos = p.liquidInputPositions.get(i);
            // ×10 safety margin — some impls drain slightly more than declared.
            int amount = Integer.parseInt(f[1]) * 10;
            String fluidResp = String.join("\n", c.execute(
                    "artest fluid inject 0 " + pos + " " + f[0] + " " + amount));
            assertTrue("fluid inject (" + f[0] + " ×" + amount + " into "
                            + pos + ") failed for " + fixtureKey + ": " + fluidResp,
                    fluidResp.contains("\"ok\":true"));
        }
    }
}
