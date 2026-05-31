package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-40c / TASK-44 (audit Gap F.4) — TilePump drains an adjacent
 * Forge-fluid source block into its internal tank.
 *
 * <p>Production:
 * {@link zmaster587.advancedRocketry.tile.TilePump#performFunction} calls
 * {@code getNextBlockLocation} which walks straight down from the pump
 * through air until it hits a non-air block, then — only if that block
 * {@code instanceof IFluidBlock} — drains it via {@code IFluidBlock.drain}
 * into the tank. Drain frequency depends on stored energy: at full
 * energy {@code getFrequencyFromPower} returns 1 (fires every tick).</p>
 *
 * <p><b>Why an AR fluid, not vanilla water:</b> the pump gates every
 * drain on {@code worldBlock instanceof IFluidBlock} (TilePump lines
 * 102 / 120 / 158). Vanilla {@code Blocks.WATER} is a {@code BlockLiquid}
 * and does NOT implement Forge's {@code IFluidBlock} — so the pump can
 * never drain vanilla water (this is why the original water-based draft
 * was @Ignore'd: it misdiagnosed the empty tank as a placement issue
 * when the block simply isn't an IFluidBlock). AR's own fluids
 * ({@code advancedrocketry:rocketFuel} etc.) extend
 * {@code BlockFluidClassic} → ARE {@code IFluidBlock}, and a meta-0
 * placement is a drainable source ({@code BlockFluidClassic.canDrain}
 * returns true for LEVEL==0). Logged as ledger observation: pump does
 * not drain vanilla water — see .agent/tasks/README.md.</p>
 *
 * <p>Pinned: a powered pump with an AR Forge-fluid source block below
 * it has &gt;0 mB in its tank after a tick budget. Player-visible:
 * pump's tank GUI fills up.</p>
 */
public class TilePumpFillsFromAdjacentWaterSourceTest extends AbstractSharedServerTest {

    private static final int PX = 6300;
    private static final int PY = 65;
    private static final int PZ = 6300;

    // The pump's fluid-stored probe emits "fluid":"<name>","amount":<n>.
    // Match the amount on any non-null fluid (band-pin: >0, not an exact
    // mB count which would be an impl-detail pin).
    private static final Pattern FLUID_AMOUNT =
            Pattern.compile("\"fluid\":\"[^\"]+\",\"amount\":(\\d+)");

    @Test
    public void poweredPumpDrainsAdjacentFluidSource() throws Exception {
        // Place pump.
        ok("artest place 0 " + PX + " " + PY + " " + PZ
                + " advancedrocketry:blockPump");

        // Place an AR Forge-fluid source (rocketFuel, non-gaseous) directly
        // below. meta 0 = source block (LEVEL==0 → canDrain true). Unlike
        // vanilla water, this IS an IFluidBlock so the pump's drain gate
        // passes.
        ok("artest place 0 " + PX + " " + (PY - 1) + " " + PZ
                + " advancedrocketry:rocketFuel");

        // Inject 1000 RF — pump's max energy (constructor super(1000)).
        // Full energy → getFrequencyFromPower() returns 1 → canPerformFunction
        // passes the worldTime % freq gate every tick.
        ok("artest energy inject 0 " + PX + " " + PY + " " + PZ + " 1000");

        // Force-tick the pump. Each qualifying tick drains the source into
        // the tank.
        ok("artest tile force-tick 0 " + PX + " " + PY + " " + PZ + " 60");

        // Read pump's tank state via the standard fluid stored probe.
        String stored = exec("artest fluid stored 0 "
                + PX + " " + PY + " " + PZ);
        Matcher m = FLUID_AMOUNT.matcher(stored);
        assertTrue("pump's tank must contain fluid after 60 ticks "
                        + "(the player-visible 'pump fills from adjacent "
                        + "fluid source' contract); stored=" + stored,
                m.find());
        int amount = Integer.parseInt(m.group(1));
        assertTrue("fluid amount must be > 0; actual=" + amount,
                amount > 0);
    }

    private String exec(String cmd) throws Exception {
        return String.join("\n", client().execute(cmd));
    }

    private void ok(String cmd) throws Exception {
        String resp = exec(cmd);
        assertTrue("probe must succeed: cmd='" + cmd + "' resp=" + resp,
                resp.contains("\"ok\":true"));
    }
}
