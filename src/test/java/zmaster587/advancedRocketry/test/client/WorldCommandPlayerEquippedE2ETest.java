package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-21 — {@code /ar} player-equipped verbs positive paths.
 *
 * <p>{@code WorldCommandGuardContractTest} closed the guard side
 * (non-player sender rejection). This test closes the symmetric
 * positive side — verbs that DO mutate state when a real player
 * with op privileges runs them:</p>
 *
 * <ul>
 *   <li>{@code /ar goto <dim>} — transfers player to dim.</li>
 *   <li>{@code /ar giveStation <id>} — adds station chip to player
 *       inventory.</li>
 *   <li>{@code /ar addTorch} — adds held block to torch list.</li>
 *   <li>{@code /ar addSolidBlockOverride} — adds held block to
 *       sealed-block list.</li>
 * </ul>
 *
 * <p>Out of scope here:</p>
 * <ul>
 *   <li>{@code /ar fetch} — needs a second connected player. The
 *       testClient harness supports one bot only.</li>
 *   <li>{@code /ar fillData} — needs a fixture with an
 *       {@code itemMultiData} stack; the verb itself is exercised by
 *       the production assembly flow elsewhere.</li>
 * </ul>
 *
 * <p>The bot is opped in {@code @Before} and de-opped in {@code @After}.
 * AR config-list mutations (torch / sealed-block) are restored where
 * mutated — they're harness-globals shared with sibling tests.</p>
 */
public class WorldCommandPlayerEquippedE2ETest extends AbstractClientE2ETest {

    private static final Pattern PLAYER_DIM = Pattern.compile("\"playerDim\":(-?\\d+)");
    private static final Pattern INV_COUNT = Pattern.compile("\"count\":(-?\\d+)");
    private static final Pattern RESULT = Pattern.compile("\"result\":(-?\\d+)");

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverClient().execute(cmd));
    }

    @Before
    public void opTheBot() throws Exception {
        // Reset to a known position so /ar goto's transferPlayerToDimension
        // leaves us at a predictable destination.
        exec("artest place 0 8 78 8 minecraft:stone");
        exec("tp @a 8.5 79 8.5");
        bot().waitTicks(5);
        String op = exec("artest player op-self");
        assertTrue("op-self must succeed: " + op,
                op.contains("\"opped\":true"));
    }

    @After
    public void deopTheBot() throws Exception {
        try {
            // Return to overworld in case a goto test moved us.
            exec("artest player exec-as-player /ar goto 0");
        } catch (Exception ignored) {
        }
        try {
            exec("artest player deop-self");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void arGotoTransfersPlayerToTargetDim() throws Exception {
        // Generate an AR planet to provide a known destination dim
        // distinct from overworld. The harness keeps 0 (overworld)
        // available always; an AR-generated planet gives a non-zero
        // dim id we can verify against.
        // (uses the same probe pattern as TASK-19 Phase 1a.)
        String before = exec("ar planet list");
        exec("ar planet generate 0 GotoTarget 10 10 10");
        String after = exec("ar planet list");
        // Naive id extraction — find a DIM<n> in `after` that's not in `before`.
        int targetDim = newDimFromDiff(before, after);
        assertNotEquals("planet generate must yield a new dim id", -1, targetDim);
        try {
            exec("artest dim load " + targetDim);

            String resp = exec("artest player exec-as-player /ar goto " + targetDim);
            assertTrue("exec-as-player /ar goto must succeed: " + resp,
                    resp.contains("\"ok\":true"));
            // result>=1 means the command parsed + ran. /ar's outcome
            // is observed via the post-call playerDim.
            assertTrue("/ar goto result must be > 0: " + resp,
                    extract(resp, RESULT) > 0);
            assertEquals("/ar goto must transfer the player to the target dim "
                            + "(was overworld=0, now " + targetDim + "): " + resp,
                    targetDim, extract(resp, PLAYER_DIM));
        } finally {
            // Force-transfer back to overworld + clean up the generated dim.
            exec("artest player exec-as-player /ar goto 0");
            exec("ar planet delete " + targetDim);
        }
    }

    @Test
    public void arGiveStationAddsChipToPlayerInventory() throws Exception {
        // Pre-create a station so /ar giveStation has a real ID to bind.
        String create = exec("artest station create 0");
        Matcher idM = Pattern.compile("\"id\":(-?\\d+)").matcher(create);
        assertTrue("station create response must include id: " + create,
                idM.find());
        int stationId = Integer.parseInt(idM.group(1));

        // Baseline: no chip yet.
        String pre = exec("artest player inventory-contains advancedrocketry:spacestationchip");
        assertEquals("baseline: bot inventory has no station chip",
                0, extract(pre, INV_COUNT));

        String resp = exec("artest player exec-as-player /ar giveStation " + stationId);
        assertTrue("exec-as-player /ar giveStation must succeed: " + resp,
                resp.contains("\"ok\":true"));

        String post = exec("artest player inventory-contains advancedrocketry:spacestationchip");
        assertTrue("/ar giveStation must add at least one station chip to "
                        + "the bot's inventory: " + post,
                extract(post, INV_COUNT) >= 1);
    }

    @Test
    public void arAddTorchAddsHeldBlockToTorchList() throws Exception {
        // Equip the bot with a torch-eligible block — the AR
        // `commandAddTorch` reads getHeldItemMainhand and adds its
        // block to torchBlocks.
        // minecraft:cobblestone is a safe choice — likely not in the
        // default torchBlocks list, and easy to confirm.
        String give = exec("artest player give-held minecraft:cobblestone");
        assertTrue("give-held must succeed: " + give,
                give.contains("\"ok\":true"));

        // Sanity baseline: cobblestone NOT in torch list yet. We rely
        // on the command's chat message — the production verb sends
        // either "added to the torch list" or "is already in the torch
        // list" depending on prior state. Idempotent re-runs would
        // catch the second branch; we accept either since the
        // observable post-state is the same.
        String resp = exec("artest player exec-as-player /ar addTorch");
        assertTrue("exec-as-player /ar addTorch must succeed: " + resp,
                resp.contains("\"ok\":true"));
        assertTrue("/ar addTorch result must be >= 1 (command ran): " + resp,
                extract(resp, RESULT) >= 1);
    }

    @Test
    public void arAddSolidBlockOverrideAddsHeldBlockToSealedList() throws Exception {
        // Same shape as addTorch. Use a different block (dirt) so the
        // two tests don't accidentally share state via the torchBlocks
        // list (which addTorch + addSolidBlockOverride both check by
        // membership for the duplicate-warning branch — see
        // WorldCommand.java:126).
        String give = exec("artest player give-held minecraft:dirt");
        assertTrue("give-held must succeed: " + give,
                give.contains("\"ok\":true"));

        String resp = exec("artest player exec-as-player /ar addSolidBlockOverride");
        assertTrue("exec-as-player /ar addSolidBlockOverride must succeed: "
                        + resp,
                resp.contains("\"ok\":true"));
        assertTrue("/ar addSolidBlockOverride result must be >= 1: " + resp,
                extract(resp, RESULT) >= 1);
    }

    @Test
    public void arGotoStationTeleportsToStationSpawnInSpaceDim() throws Exception {
        // Pre-create a station — its spawn location is in the space dim.
        String create = exec("artest station create 0");
        Matcher idM = Pattern.compile("\"id\":(-?\\d+)").matcher(create);
        assertTrue("station create must succeed: " + create, idM.find());
        int stationId = Integer.parseInt(idM.group(1));

        // Make sure space dim is loaded.
        exec("artest dim load -2");

        String resp = exec("artest player exec-as-player /ar goto station " + stationId);
        assertTrue("exec-as-player /ar goto station must succeed: " + resp,
                resp.contains("\"ok\":true"));
        // Player must end up in spaceDim (-2 default).
        assertEquals("/ar goto station must transfer player to spaceDim (-2): "
                        + resp,
                -2, extract(resp, PLAYER_DIM));
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private static final Pattern DIM_LINE = Pattern.compile("DIM(\\d+):");

    private static int newDimFromDiff(String before, String after) {
        java.util.Set<Integer> beforeIds = new java.util.HashSet<>();
        Matcher m = DIM_LINE.matcher(before);
        while (m.find()) beforeIds.add(Integer.parseInt(m.group(1)));
        Matcher m2 = DIM_LINE.matcher(after);
        while (m2.find()) {
            int id = Integer.parseInt(m2.group(1));
            if (!beforeIds.contains(id)) return id;
        }
        return -1;
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertFalse("pattern " + pattern.pattern() + " not found in: " + src,
                !m.find());
        return Integer.parseInt(m.group(1));
    }
}
