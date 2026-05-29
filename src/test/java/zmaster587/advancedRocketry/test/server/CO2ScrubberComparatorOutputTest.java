package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-40c (audit Gap F.1) — TileCO2Scrubber's comparator output
 * reflects the cartridge's remaining charge.
 *
 * <p>Production:
 * {@link zmaster587.advancedRocketry.tile.atmosphere.TileCO2Scrubber#getComparatorOverride}
 * returns {@code (32766 - stack.getItemDamage() + 2184) / 2185} when
 * slot 0 holds any (non-empty) cartridge, and 0 when empty.
 * Player-visible: a redstone comparator placed adjacent reports
 * remaining cartridge charge as a 0..15 level.</p>
 *
 * <p>Pinned (loose-bound, contract-not-formula):</p>
 * <ul>
 *   <li>Empty slot → comparator output = 0.</li>
 *   <li>Fresh cartridge (damage = 0) → comparator output &gt; 0
 *       (the player-visible "scrubber has fuel" signal).</li>
 * </ul>
 *
 * <p>NOT pinned (impl per SOP): the exact 32766 / 2184 / 2185 formula.
 * The constants are tuning, not contract.</p>
 */
public class CO2ScrubberComparatorOutputTest extends AbstractSharedServerTest {

    private static final int PX = 6400;
    private static final int PY = 65;
    private static final int PZ = 6400;

    private static final Pattern VALUE_PAT =
            Pattern.compile("\"value\":(-?\\d+)");

    @Test
    public void emptyScrubberReportsZeroComparatorOutput() throws Exception {
        int x = PX, y = PY, z = PZ;
        ok("artest place 0 " + x + " " + y + " " + z
                + " advancedrocketry:oxygenScrubber");
        String resp = exec("artest infra comparator-override 0 "
                + x + " " + y + " " + z);
        assertTrue("comparator-override must succeed: " + resp,
                resp.contains("\"ok\":true"));
        int value = extract(resp);
        assertTrue("empty CO2 scrubber must report comparator = 0; "
                        + "actual=" + value + " resp=" + resp,
                value == 0);
    }

    @Test
    public void freshCartridgeReportsNonZeroComparatorOutput() throws Exception {
        int x = PX + 30, y = PY, z = PZ;
        ok("artest place 0 " + x + " " + y + " " + z
                + " advancedrocketry:oxygenScrubber");
        // Drop a fresh (damage=0) cartridge into slot 0 — production
        // damage starts at 0; max is Short.MAX_VALUE-1.
        ok("artest hatch fill 0 " + x + " " + y + " " + z
                + " 0 advancedrocketry:carbonScrubberCartridge 1 0");
        String resp = exec("artest infra comparator-override 0 "
                + x + " " + y + " " + z);
        assertTrue("comparator-override must succeed: " + resp,
                resp.contains("\"ok\":true"));
        int value = extract(resp);
        assertTrue("CO2 scrubber with fresh cartridge must report "
                        + "comparator > 0 (the player-visible 'has "
                        + "fuel' redstone signal); actual=" + value
                        + " resp=" + resp,
                value > 0);
    }

    private String exec(String cmd) throws Exception {
        return String.join("\n", client().execute(cmd));
    }

    private void ok(String cmd) throws Exception {
        String resp = exec(cmd);
        assertTrue("probe must succeed: cmd='" + cmd + "' resp=" + resp,
                resp.contains("\"ok\":true"));
    }

    private static int extract(String src) {
        Matcher m = VALUE_PAT.matcher(src);
        assertTrue("value missing in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }
}
