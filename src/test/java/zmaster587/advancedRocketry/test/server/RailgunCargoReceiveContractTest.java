package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-40 (Gap A) — Railgun cargo-receive contract.
 *
 * <p>Phase-0 reshape: the audit framed this as "Railgun firing — orbital
 * projectile + RF debit". Production
 * ({@link zmaster587.advancedRocketry.tile.multiblock.TileRailgun#attemptCargoTransfer})
 * is actually a paired-railgun ITEM TRANSPORT system: a source railgun
 * picks an item from its input port, dispatches it to a linked
 * destination railgun (same or another dim), and the destination's
 * {@code onReceiveCargo} deposits it in the output port. The
 * {@code EntityItemAbducted} that spawns is the in-flight visual, not a
 * weapon projectile.</p>
 *
 * <p>The full source-side firing path requires TWO assembled railguns at
 * linked positions — outside the reach of a single-multiblock fixture.
 * The <b>receiver-side</b> contract is the player-visible endpoint of
 * the system: cargo emitted by the source arrives at the destination's
 * output port. We pin that endpoint here via a probe that calls
 * {@code onReceiveCargo} on a SOLO assembled railgun, then scans
 * {@code itemOutPorts} to count matching stacks.</p>
 *
 * <p>The pre-existing {@link RailgunMultiblockTest} pins assembly +
 * structure invalidation; nothing covered the cargo-receive surface
 * before this test.</p>
 *
 * <p>Position-isolated at x=4700 (no collision with RailgunMultiblockTest's
 * x=4500 + 30 + 60 = x=4560 fixtures, or BHG/Beacon/Observatory at lower
 * x-values).</p>
 */
public class RailgunCargoReceiveContractTest extends AbstractSharedServerTest {

    private static final int CX = 4700;
    private static final int CY = 64;
    private static final int CZ = 4700;

    private static final Pattern MATCHED_COUNT =
            Pattern.compile("\"matchedCount\":(\\d+)");
    private static final Pattern OUT_PORT_COUNT =
            Pattern.compile("\"outPortCount\":(\\d+)");

    /**
     * TASK-40 Gap A — assembled railgun's {@code onReceiveCargo} deposits
     * a 16-cobblestone stack into its output port. Asserts both:
     *
     * <ol>
     *   <li>{@code canReceiveCargo} returned true (output port has room)
     *       — guards against a regression that breaks
     *       {@code ZUtils.numEmptySlots} on the output hatch.</li>
     *   <li>The output port now contains &ge; 16 of the deposited item —
     *       the deposit landed where the source railgun's transfer loop
     *       expects it.</li>
     * </ol>
     */
    @Test
    public void railgunOnReceiveCargoDepositsStackToOutputPort() throws Exception {
        String fixture = exec("artest fixture multiblock railgun 0 "
                + CX + " " + CY + " " + CZ);
        assertTrue("fixture multiblock railgun failed: " + fixture,
                fixture.contains("\"ok\":true"));

        // Validate structure so libVulpes' integrateTile populates
        // itemOutPorts (the field the probe reads via reflection).
        String tryComplete = exec("artest machine try-complete 0 "
                + CX + " " + CY + " " + CZ);
        assertTrue("railgun must validate (precondition for itemOutPorts "
                        + "to be populated): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));

        // Probe call: receive 16 cobblestone on the controller-side tile.
        String receive = exec("artest infra railgun-receive-cargo 0 "
                + CX + " " + CY + " " + CZ + " minecraft:cobblestone 16");
        assertTrue("railgun-receive-cargo probe must succeed: " + receive,
                receive.contains("\"ok\":true"));
        assertTrue("canReceiveCargo must be true on freshly-assembled "
                        + "railgun (output port has empty slots): " + receive,
                receive.contains("\"canReceive\":true"));

        int outPortCount = extract(receive, OUT_PORT_COUNT);
        assertTrue("railgun must have >= 1 output port after assembly: "
                        + receive,
                outPortCount >= 1);

        int matched = extract(receive, MATCHED_COUNT);
        assertTrue("output ports must contain >= 16 cobblestone after "
                        + "onReceiveCargo (the player-visible 'cargo "
                        + "arrives at destination' contract); matched="
                        + matched + " receive=" + receive,
                matched >= 16);
    }

    // -- helpers ----------------------------------------------------------

    private static String exec(String cmd) throws Exception {
        return String.join("\n", client().execute(cmd));
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }
}
