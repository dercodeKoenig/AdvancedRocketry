package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.server.TestClient;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-25 — Small PlatePress end-to-end recipe contract.
 *
 * <p>Single-block, redstone-triggered, item-output-as-EntityItem.
 * Distinct from the TASK-18 / TASK-26 multiblock industrial machines —
 * no hatches, no RF, no force-tick. The contract this class pins:</p>
 *
 * <ol>
 *   <li><b>Fixture shape</b>: the 3-block vertical stack is built —
 *       obsidian below, recipe ingredient in the middle, PlatePress on top
 *       (FACING=DOWN, EXTENDED=false). The fixture builder resolves the
 *       ingredient from the first registered recipe.</li>
 *   <li><b>End-to-end activation</b>: placing a redstone-power source
 *       adjacent to the press destroys the ingredient block and spawns
 *       an {@code EntityItem} next to the press carrying the recipe's
 *       output item. Player-visible behaviour — both effects asserted.</li>
 * </ol>
 *
 * <p>Per {@code testing-principles.md} the kit pins observable outcomes
 * (block-state change + entity spawn) rather than internal machinery
 * (piston EXTENDED state, exact tick where the spawn fires).</p>
 */
public class PlatePressRecipeEndToEndTest extends AbstractSharedServerTest {

    private static final String FIXTURE_KEY = "plate-press";
    private static final String PRESS_FQN   = "zmaster587.advancedRocketry.block.BlockSmallPlatePress";

    @Test
    public void platePressFixtureBuildsExpectedStack() throws Exception {
        int x = 400, y = 70, z = 400;
        TestClient c = client();
        String resp = String.join("\n",
                c.execute("artest fixture machine " + FIXTURE_KEY + " 0 " + x + " " + y + " " + z));
        assertTrue("fixture machine " + FIXTURE_KEY + " failed: " + resp,
                resp.contains("\"ok\":true"));
        assertTrue("response missing pressPos: " + resp,
                resp.contains("\"pressPos\":[" + x + "," + y + "," + z + "]"));

        // Read each cell of the 3-stack and verify the correct block sits there.
        String obsRead = String.join("\n", c.execute(
                "artest block at 0 " + x + " " + (y - 2) + " " + z));
        assertTrue("obsidian missing at " + x + "," + (y - 2) + "," + z + ": " + obsRead,
                obsRead.contains("\"block\":\"minecraft:obsidian\""));

        String pressRead = String.join("\n", c.execute(
                "artest block at 0 " + x + " " + y + " " + z));
        assertTrue("press missing at " + x + "," + y + "," + z + ": " + pressRead,
                pressRead.contains("\"block\":\"advancedrocketry:platepress\""));
    }

    @Test
    public void platePressRedstoneActivationDropsRecipeOutput() throws Exception {
        int x = 500, y = 70, z = 400;
        TestClient c = client();
        // Build fixture + capture the resolved output id.
        String fixture = String.join("\n",
                c.execute("artest fixture machine " + FIXTURE_KEY + " 0 " + x + " " + y + " " + z));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        // Extract the resolved output item + ingredient block ids.
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\"outputItem\":\"([^\"]+)\"").matcher(fixture);
        assertTrue("response missing outputItem: " + fixture, m.find());
        String expectedOutputId = m.group(1);
        assertTrue("first recipe has no output — can't end-to-end test",
                !"null".equals(expectedOutputId));
        java.util.regex.Matcher mb = java.util.regex.Pattern.compile(
                "\"ingredientBlock\":\"([^\"]+)\"").matcher(fixture);
        assertTrue("response missing ingredientBlock: " + fixture, mb.find());
        String ingredientBlockId = mb.group(1);

        // Activate: place a redstone block on top of the press. The press's
        // neighborChanged handler fires synchronously on setBlockState, runs
        // checkForMove → shouldBeExtended=true → setBlockToAir(below) +
        // spawnEntity(EntityItem with recipe output). Adjacent (not above) is
        // also valid; using ABOVE because shouldBeExtended explicitly
        // re-checks pos.up() neighbours and that path is unambiguous.
        String activate = String.join("\n", c.execute(
                "artest place 0 " + x + " " + (y + 1) + " " + z + " minecraft:redstone_block"));
        assertTrue("redstone block placement failed: " + activate,
                activate.contains("\"placed\":true"));

        // Scan for EntityItem within 2 blocks of (x+0.5, y-0.5, z+0.5) —
        // the spawn position from BlockSmallPlatePress.checkForMove.
        String scan = String.join("\n", c.execute(
                "artest entity scan-items 0 " + (x + 0.5) + " " + (y - 0.5) + " " + (z + 0.5) + " 2"));
        assertTrue("entity scan-items failed: " + scan, scan.contains("\"ok\":true"));
        assertTrue("expected output item " + expectedOutputId
                        + " not in scan response — recipe did not produce its EntityItem: " + scan,
                scan.contains("\"item\":\"" + expectedOutputId + "\""));

        // Ingredient block must be gone (consumed by the press). After
        // activation the cell ends up either as AIR (setBlockToAir from
        // checkForMove) or as `minecraft:piston_extension` (transient during
        // piston-head extension). The contract: it is NO LONGER the
        // ingredient block.
        String ingredientRead = String.join("\n", c.execute(
                "artest block at 0 " + x + " " + (y - 1) + " " + z));
        assertTrue("ingredient block " + ingredientBlockId + " still present at "
                        + x + "," + (y - 1) + "," + z + " — press did not consume it: "
                        + ingredientRead,
                !ingredientRead.contains("\"block\":\"" + ingredientBlockId + "\""));
    }
}
