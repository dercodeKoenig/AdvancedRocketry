package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.7 — controller-level recipe-machine smoke for all 10 AR multiblock
 * machines (cutting is covered end-to-end by {@link MachineRecipeIntegrationTest},
 * the other 9 here).
 *
 * <p>For each machine:</p>
 * <ol>
 *   <li>place the controller block via {@code /artest place};</li>
 *   <li>verify the expected {@code Tile*} class is created;</li>
 *   <li>force-tick the controller without a complete multiblock — the
 *       production {@code performFunction} branch must guard {@code isComplete}
 *       and not crash;</li>
 *   <li>{@code /artest machine try-complete} on a bare controller must report
 *       {@code isComplete=false} (the negative path that gameplay relies on
 *       before a player builds the structure);</li>
 *   <li>{@code /artest machine recipes-summary} reports the machine's class
 *       (recipe count itself may be zero for default config — that's covered
 *       by {@link MachineRecipeIntegrationTest#recipesSummaryReportsNonZeroCounts}
 *       on the canonical 5).</li>
 * </ol>
 *
 * <p>End-to-end recipe cycles for the other 9 machines need per-machine
 * fixtures (libVulpes registry names — {@code blockAdvStructureBlock},
 * {@code blockCoil}, {@code casingCentrifuge}). Those land in follow-up
 * {@code /artest fixture machine &lt;name&gt;} probes; see the cutter fixture
 * for the canonical pattern.</p>
 */
public class MultiMachineControllerSmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern TICKED = Pattern.compile("\"ticked\":(\\d+)");

    /** Machine block id → expected Tile* class short name. */
    private static final Map<String, String> MACHINES = new LinkedHashMap<>();
    static {
        MACHINES.put("advancedrocketry:rollingMachine",               "TileRollingMachine");
        MACHINES.put("advancedrocketry:lathe",                        "TileLathe");
        MACHINES.put("advancedrocketry:crystallizer",                 "TileCrystallizer");
        MACHINES.put("advancedrocketry:electrolyser",                 "TileElectrolyser");
        MACHINES.put("advancedrocketry:chemicalReactor",              "TileChemicalReactor");
        MACHINES.put("advancedrocketry:centrifuge",                   "TileCentrifuge");
        MACHINES.put("advancedrocketry:arcfurnace",                   "TileElectricArcFurnace");
        MACHINES.put("advancedrocketry:precisionassemblingmachine",   "TilePrecisionAssembler");
        MACHINES.put("advancedrocketry:precisionlaseretcher",         "TilePrecisionLaserEtcher");
    }

    @Test
    public void allMachineControllersPlaceTickAndHaveRecipes() throws Exception {
        // recipes-summary baseline.
        String summary = String.join("\n",
                client().execute("artest machine recipes-summary"));
        assertTrue("recipes-summary errored: " + summary,
                !summary.contains("\"error\""));

        // Layout: row of machines on flat stone at y=64, x=2100..2200 step 5.
        int y = 64;
        int z = 2100;
        int xOff = 2100;

        StringBuilder failures = new StringBuilder();
        int idx = 0;
        for (Map.Entry<String, String> e : MACHINES.entrySet()) {
            String blockId = e.getKey();
            String tileClass = e.getValue();
            int x = xOff + idx * 5;
            idx++;

            // 1. Place.
            String place = String.join("\n", client().execute(
                    "artest place 0 " + x + " " + y + " " + z + " " + blockId));
            if (!place.contains("\"placed\":true")) {
                failures.append(blockId).append("=PLACE_FAILED(").append(place).append(");\n");
                continue;
            }

            // 2. Tile class match.
            String info = String.join("\n", client().execute(
                    "artest machine info 0 " + x + " " + y + " " + z));
            if (!info.contains(tileClass)) {
                failures.append(blockId).append("=WRONG_TILE_CLASS(expected ")
                        .append(tileClass).append("; got: ").append(info).append(");\n");
                continue;
            }

            // 3. try-complete on bare controller → isComplete=false.
            String tryComplete = String.join("\n", client().execute(
                    "artest machine try-complete 0 " + x + " " + y + " " + z));
            if (!tryComplete.contains("\"isComplete\":false")) {
                failures.append(blockId).append("=BARE_TRY_COMPLETE_NOT_FALSE(")
                        .append(tryComplete).append(");\n");
                continue;
            }

            // 4. Force-tick — must not throw.
            String tick = String.join("\n", client().execute(
                    "artest tile force-tick 0 " + x + " " + y + " " + z + " 20"));
            if (!tick.contains("\"ok\":true")) {
                failures.append(blockId).append("=TICK_FAILED(").append(tick).append(");\n");
                continue;
            }
            Matcher tm = TICKED.matcher(tick);
            if (!tm.find() || Integer.parseInt(tm.group(1)) != 20) {
                failures.append(blockId).append("=INCOMPLETE_TICK(").append(tick).append(");\n");
                continue;
            }

            // 5. Tile must still be queryable.
            String postInfo = String.join("\n", client().execute(
                    "artest machine info 0 " + x + " " + y + " " + z));
            if (!postInfo.contains(tileClass)) {
                failures.append(blockId).append("=POST_TICK_TILE_LOST(")
                        .append(postInfo).append(");\n");
                continue;
            }

            // 6. recipes-summary reports the machine's class (count may be 0
            // for machines with no default recipes — that's a config matter,
            // not a wiring regression). The non-zero subset is guarded by
            // MachineRecipeIntegrationTest.
            Pattern p = Pattern.compile("\"" + tileClass + "\":(-?\\d+|\"[^\"]+\")");
            if (!p.matcher(summary).find()) {
                failures.append(blockId).append("=NOT_IN_RECIPE_SUMMARY;\n");
                continue;
            }
        }
        assertEquals("machine smoke failures:\n" + failures,
                "", failures.toString());
    }
}
