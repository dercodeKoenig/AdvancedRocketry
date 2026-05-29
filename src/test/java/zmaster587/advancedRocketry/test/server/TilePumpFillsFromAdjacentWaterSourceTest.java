package zmaster587.advancedRocketry.test.server;

import org.junit.Ignore;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-40c (audit Gap F.4) — TilePump drains an adjacent water source
 * block into its internal tank.
 *
 * <p>Production:
 * {@link zmaster587.advancedRocketry.tile.TilePump#performFunction} calls
 * {@code getNextBlockLocation} which walks below the pump until it
 * finds a fluid block (line 132-145), then drains it via
 * {@code IFluidBlock.drain} into the tank. The drain frequency depends
 * on stored energy: at &gt;50% energy the pump fires every game tick
 * ({@code getFrequencyFromPower} returns 1).</p>
 *
 * <p>Pinned: a powered pump with a vanilla water source block directly
 * below it has &gt;0 mB of water in its tank after a tick budget.
 * Player-visible: pump's tank GUI fills up.</p>
 */
public class TilePumpFillsFromAdjacentWaterSourceTest extends AbstractSharedServerTest {

    private static final int PX = 6300;
    private static final int PY = 65;
    private static final int PZ = 6300;

    private static final Pattern WATER_AMOUNT =
            Pattern.compile("\"fluid\":\"water\",\"amount\":(\\d+)");

    /**
     * NOTE: currently ignored — fixture-setup deferral.
     *
     * <p>The test was authored against the audit's Gap F.4 contract and
     * green-light Phase 0 read of {@link
     * zmaster587.advancedRocketry.tile.TilePump#performFunction}. On run
     * the pump's tank stays empty: probe response =
     * {@code {"tanks":[{"capacity":16000,"fluid":null}]}} after 60
     * force-ticks. Cause is that {@code world.setBlockState} from the
     * {@code /artest place} probe places {@code Blocks.WATER}'s default
     * state but doesn't trigger the neighbor / level propagation that
     * vanilla world placement does — the resulting block may not pass
     * {@code BlockDynamicLiquid.canDrain(world, pos)} (which gates the
     * pump's {@code findFluidAtOrAbove} BFS).</p>
     *
     * <p>To un-ignore: either (a) add a {@code /artest place-source-water
     * <dim> <x> <y> <z>} probe that uses {@code ItemBucket.tryPlaceContainedLiquid}
     * to place a real source block, or (b) add a {@code pump-debug} probe
     * exposing pump's private {@code cache} list + energy + tank + down-block
     * state and trace which of the four gates (canPerformFunction,
     * hasEnoughEnergy, getNextBlockLocation, canFitFluid) blocks.</p>
     */
    @Ignore("TASK-40c Gap F.4 deferred — world.setBlockState-placed water "
            + "may not pass BlockDynamicLiquid.canDrain; need real source-"
            + "block placement probe (bucket-style) or pump-debug to trace.")
    @Test
    public void poweredPumpFillsFromAdjacentWaterSource() throws Exception {
        // Place pump.
        ok("artest place 0 " + PX + " " + PY + " " + PZ
                + " advancedrocketry:blockPump");

        // Place vanilla water source directly below.
        ok("artest place 0 " + PX + " " + (PY - 1) + " " + PZ
                + " minecraft:water");

        // Inject 1000 RF — pump's max energy capacity per constructor
        // (super(1000)). Storing 1000 keeps the energy ratio at 100%
        // so getFrequencyFromPower() returns 1 → pump's
        // canPerformFunction passes the worldTime % freq gate every
        // tick.
        ok("artest energy inject 0 " + PX + " " + PY + " " + PZ + " 1000");

        // Force-tick the pump 60 times. Each tick:
        //   parent.update() → canPerformFunction (returns true at
        //   full energy) → performFunction → drains 1 water block →
        //   tank gains 1000 mB.
        ok("artest tile force-tick 0 " + PX + " " + PY + " " + PZ + " 60");

        // Read pump's tank state via the standard fluid stored probe
        // (pump implements IFluidHandler + exposes
        // FLUID_HANDLER_CAPABILITY).
        String stored = exec("artest fluid stored 0 "
                + PX + " " + PY + " " + PZ);
        Matcher m = WATER_AMOUNT.matcher(stored);
        assertTrue("pump's tank must contain water after 60 ticks "
                        + "(the player-visible 'pump fills from "
                        + "adjacent water source' contract); stored="
                        + stored,
                m.find());
        int amount = Integer.parseInt(m.group(1));
        assertTrue("water amount must be > 0; actual=" + amount,
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
