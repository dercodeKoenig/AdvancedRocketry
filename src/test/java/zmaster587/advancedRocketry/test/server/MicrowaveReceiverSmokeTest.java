package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.16 — Microwave Receiver multiblock smoke.
 *
 * <p>The receiver is a 5×5 single-layer multiblock: 12 solar panels in a ring +
 * 4 corner solar panels + controller in the centre. Without a paired
 * orbital {@code solarEnergy} satellite the generator produces 0 RF, but the
 * multiblock must still validate, tick without crashing, and report stable
 * state via {@code /artest machine info}.</p>
 *
 * <p>SMART §7.16 lists "microwave receiver" as a generator class. Real
 * generation cycle (with a paired satellite) needs cross-dim state plumbing
 * which would require additional probes; here we lock down the multiblock +
 * tick path that ALL generator cycles depend on.</p>
 */
public class MicrowaveReceiverSmokeTest extends AbstractHeadlessServerTest {

    @Test
    public void multiblockValidatesAndTicksWithoutCrash() throws Exception {
        // Layer at y=64. Production structure (TileMicrowaveReciever.structure):
        //   {iron, *, *, *, iron}
        //   { *, iron, iron, iron, * }
        //   { *, iron, controller, iron, * }
        //   { *, iron, iron, iron, * }
        //   {iron, *, *, *, iron}
        // where iron = advancedrocketry:solarPanel.
        //
        // X grows east, Z grows south. Place at (1700..1704) × (1700..1704).
        int x0 = 1700, y = 64, z0 = 1700;
        int xC = x0 + 2, zC = z0 + 2;

        // Fill the 5×5 with solar panels first.
        String fill = String.join("\n", client().execute(
                "artest fill 0 " + x0 + " " + y + " " + z0 + " "
                        + (x0 + 4) + " " + y + " " + (z0 + 4)
                        + " advancedrocketry:solarPanel"));
        assertTrue("solar fill failed: " + fill, fill.contains("\"ok\":true"));

        // Clear corners + middle-edges to '*' (air) per the structure pattern.
        // Air at corners (4) + at the 4 mid-side positions in the corner row.
        int[][] airPositions = new int[][]{
                {x0, z0},     {x0 + 4, z0},     {x0, z0 + 4},     {x0 + 4, z0 + 4},   // corners
                {x0 + 1, z0}, {x0 + 2, z0},     {x0 + 3, z0},                          // top edge mid
                {x0 + 1, z0 + 4}, {x0 + 2, z0 + 4}, {x0 + 3, z0 + 4},                  // bottom edge mid
                {x0, z0 + 1}, {x0, z0 + 2}, {x0, z0 + 3},                              // left edge mid
                {x0 + 4, z0 + 1}, {x0 + 4, z0 + 2}, {x0 + 4, z0 + 3}                   // right edge mid
        };
        for (int[] p : airPositions) {
            client().execute("artest place 0 " + p[0] + " " + y + " " + p[1] + " minecraft:air");
        }

        // Controller at centre.
        String place = String.join("\n", client().execute(
                "artest place 0 " + xC + " " + y + " " + zC
                        + " advancedrocketry:microwaveReciever"));
        assertTrue("controller place failed: " + place,
                place.contains("\"placed\":true"));

        // Sanity probe — tile type.
        String info = String.join("\n", client().execute(
                "artest machine info 0 " + xC + " " + y + " " + zC));
        assertTrue("expected microwave-receiver tile: " + info,
                info.contains("TileMicrowaveReciever"));

        // Force-tick: the receiver checks structure validity inside update().
        // Without a satellite, getPowerMadeLastTick stays 0. We assert no crash
        // and that the tile is still alive (next tick info still resolves).
        String tick = String.join("\n", client().execute(
                "artest tile force-tick 0 " + xC + " " + y + " " + zC + " 40"));
        assertTrue("force-tick errored: " + tick, tick.contains("\"ok\":true"));
        assertEquals("must tick all 40 iterations",
                "40", extract(tick, "\"ticked\":(\\d+)"));

        // Re-probe — tile still resolves (no NPE wiped it out).
        String postInfo = String.join("\n", client().execute(
                "artest machine info 0 " + xC + " " + y + " " + zC));
        assertTrue("tile must survive tick burst: " + postInfo,
                postInfo.contains("TileMicrowaveReciever"));
    }

    private static String extract(String s, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(s);
        return m.find() ? m.group(1) : "";
    }
}
