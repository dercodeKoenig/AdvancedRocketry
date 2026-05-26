package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Coverage-audit gap (2026-05-26 Tier 2 #6) — TileFluidTank's
 * "stacked fill" delegation.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.TileFluidTank}'s
 * {@code fill(FluidStack, boolean)} (line 53-65) walks UP until it
 * finds the top of the tank stack, then {@code fillInternal2}
 * (line 67-88) recurses DOWN, filling the bottom-most tank first.
 * Any overflow propagates up.</p>
 *
 * <p>Player-visible contract: when a player stacks two liquidTank
 * blocks vertically and pumps fluid into the column, the bottom
 * tank fills first; only when the bottom is full does the top
 * receive any. This matches the visual gravity heuristic players
 * expect and makes the column a valid pump-source — drain reads
 * from the top, fills travel to the bottom.</p>
 *
 * <p>Existing coverage:
 * {@link FluidTankNBTRoundTripsAcrossRestartTest} pins single-tank
 * NBT round-trip; this class pins multi-tank fill delegation. No
 * other test exercises stacked-tank topology.</p>
 *
 * <p>Position-isolated at x=8000. Uses
 * {@link AbstractSharedServerTest} so the harness JVM cold-starts
 * once per class.</p>
 */
public class FluidTankStackedFillTest extends AbstractSharedServerTest {

    private static final Pattern AMOUNT_NTH =
            Pattern.compile("\"amount\":(\\d+)");
    private static final Pattern CAPACITY =
            Pattern.compile("\"capacity\":(\\d+)");

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    /** Force-load chunks around the test column so subsequent place +
     *  inject operations don't race against vanilla chunk-populate. */
    private static void warmup(int blockX, int blockZ) {
        int cx = blockX >> 4;
        int cz = blockZ >> 4;
        try {
            String resp = join(client().execute(
                    "artest chunk warmup 0 " + (cx - 1) + " " + (cz - 1) + " "
                            + (cx + 1) + " " + (cz + 1)));
            assertTrue("chunk warmup failed: " + resp,
                    resp.contains("\"ok\":true"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Place a TileFluidTank at the given coords. */
    private static void placeTank(int x, int y, int z) throws Exception {
        String resp = join(client().execute(
                "artest place 0 " + x + " " + y + " " + z
                        + " advancedrocketry:liquidTank"));
        assertTrue("liquidTank place failed at (" + x + "," + y + "," + z
                        + "): " + resp,
                resp.contains("\"placed\":true"));
    }

    /** Return the {@code capacity} reported by {@code fluid stored}.
     *  Capacity is config-driven (libVulpes default × AR's
     *  {@code blockLiquidHatchCapacityMultiplier}) so the test reads
     *  it dynamically rather than pinning a magic number. */
    private static int storedCapacity(int x, int y, int z) throws Exception {
        String resp = join(client().execute(
                "artest fluid stored 0 " + x + " " + y + " " + z));
        Matcher m = CAPACITY.matcher(resp);
        assertTrue("capacity must be present: " + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    /** Return the {@code amount} field from the {@code fluid stored}
     *  response, or 0 if the tank is empty. */
    private static int storedAmount(int x, int y, int z) throws Exception {
        String resp = join(client().execute(
                "artest fluid stored 0 " + x + " " + y + " " + z));
        assertTrue("fluid stored must succeed: " + resp,
                resp.contains("\"hasFluid\":true"));
        if (resp.contains("\"fluid\":null")) {
            return 0;
        }
        Matcher m = AMOUNT_NTH.matcher(resp);
        assertTrue("amount field must be present when fluid is non-null: "
                + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    @Test
    public void smallInjectionFillsBottomTankAndLeavesTopEmpty() throws Exception {
        // Stack:
        //   top at (BASE_X, 65, BASE_Z)
        //   bottom at (BASE_X, 64, BASE_Z)
        int baseX = 8000;
        int baseZ = 8000;
        int bottomY = 64;
        int topY = 65;
        warmup(baseX, baseZ);
        placeTank(baseX, bottomY, baseZ);
        placeTank(baseX, topY, baseZ);

        int capacity = storedCapacity(baseX, bottomY, baseZ);
        // Inject a small amount well under one tank's capacity.
        int injectAmt = Math.max(1, capacity / 4);
        String inject = join(client().execute(
                "artest fluid inject 0 " + baseX + " " + topY + " " + baseZ
                        + " oxygen " + injectAmt));
        assertTrue("inject must succeed: " + inject,
                inject.contains("\"ok\":true"));
        assertTrue("inject must report filled=injectAmt: " + inject,
                inject.contains("\"filled\":" + injectAmt));

        int topAmt = storedAmount(baseX, topY, baseZ);
        int bottomAmt = storedAmount(baseX, bottomY, baseZ);

        assertEquals("bottom tank must receive the entire injection — "
                        + "production line 73-75 recurses fillInternal2 DOWN "
                        + "until it reaches the lowest tank, then super.fill "
                        + "consumes the resource there. bottom=" + bottomAmt
                        + " top=" + topAmt + " capacity=" + capacity,
                injectAmt, bottomAmt);
        assertEquals("top tank must remain empty when injection fits in bottom",
                0, topAmt);
    }

    @Test
    public void overflowingInjectionFillsBottomThenSpillsIntoTop() throws Exception {
        // Different column from the first test (position isolation).
        int baseX = 8020;
        int baseZ = 8000;
        int bottomY = 64;
        int topY = 65;
        warmup(baseX, baseZ);
        placeTank(baseX, bottomY, baseZ);
        placeTank(baseX, topY, baseZ);

        // Read capacity from the actual tile so the test doesn't pin
        // a libVulpes magic number — the contract is "bottom fills to
        // capacity, leftover goes up" regardless of the exact capacity.
        int capacity = storedCapacity(baseX, bottomY, baseZ);
        int overflowOver = Math.max(1, capacity / 4);
        int injectAmt = capacity + overflowOver;

        String inject = join(client().execute(
                "artest fluid inject 0 " + baseX + " " + topY + " " + baseZ
                        + " oxygen " + injectAmt));
        assertTrue("inject must succeed: " + inject,
                inject.contains("\"ok\":true"));
        assertTrue("inject must report filled=injectAmt (no clamping): " + inject,
                inject.contains("\"filled\":" + injectAmt));

        int topAmt = storedAmount(baseX, topY, baseZ);
        int bottomAmt = storedAmount(baseX, bottomY, baseZ);

        assertEquals("bottom tank must be at capacity after overflow "
                        + "(capacity=" + capacity + ", inject=" + injectAmt + ")",
                capacity, bottomAmt);
        assertEquals("top tank must hold the leftover after the bottom "
                        + "filled to capacity (overflow=" + overflowOver + ")",
                overflowOver, topAmt);
    }
}
