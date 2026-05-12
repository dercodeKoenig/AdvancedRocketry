package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.8 — real multiblock validation.
 *
 * <ol>
 *   <li>{@code /artest fixture machine cutting} builds a complete cutting-machine
 *       multiblock (controller + 2 hatches + motor + sawblade + power input).</li>
 *   <li>{@code /artest machine try-complete} invokes libVulpes'
 *       {@code attemptCompleteStructure} on the controller → expects
 *       {@code isComplete=true}.</li>
 *   <li>Break a required block (the sawblade) → re-run try-complete → expects
 *       {@code isComplete=false}. Validates that the multiblock validator
 *       actually checks required positions, not just neighbor presence.</li>
 *   <li>Restore the block → try-complete → {@code isComplete=true} again.
 *       Proves validation is idempotent and stateless w.r.t. controller state.</li>
 * </ol>
 *
 * <p>Also keeps the original probe-wiring smoke: empty-position info path +
 * {@code /artest fill} volume sanity, since those primitives gate every other
 * fixture-based test.</p>
 */
public class MultiblockValidationSmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern POS_PATTERN =
            Pattern.compile("\"sawBladePos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");

    @Test
    public void cuttingMachineMultiblockValidatesAndInvalidates() throws Exception {
        // Step 0 — fixture-builder primitives still healthy.
        String emptyInfo = String.join("\n", client().execute("artest machine info 0 200 100 200"));
        assertTrue("empty position machine info wrong: " + emptyInfo,
                emptyInfo.contains("\"error\":\"no tile entity\""));
        String fill = String.join("\n",
                client().execute("artest fill 0 210 100 210 212 102 212 minecraft:stone"));
        assertTrue("fill 3x3x3 stone failed: " + fill,
                fill.contains("\"ok\":true") && fill.contains("\"volume\":27"));

        // Step 1 — build the multiblock fixture.
        int cx = 300, cy = 64, cz = 300;
        String fixture = String.join("\n",
                client().execute("artest fixture machine cutting 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture machine cutting failed: " + fixture,
                fixture.contains("\"ok\":true"));

        Matcher m = POS_PATTERN.matcher(fixture);
        assertTrue("could not parse sawBladePos: " + fixture, m.find());
        int sx = Integer.parseInt(m.group(1)),
                sy = Integer.parseInt(m.group(2)),
                sz = Integer.parseInt(m.group(3));

        // Step 2 — try-complete on the controller → isComplete=true.
        String complete = String.join("\n",
                client().execute("artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("try-complete errored: " + complete, complete.contains("\"ok\":true"));
        assertTrue("structure didn't validate (isComplete=false): " + complete,
                complete.contains("\"isComplete\":true"));

        // Step 3 — break the sawblade (a required interior block) → re-validate → isComplete=false.
        String breakBlock = String.join("\n",
                client().execute("artest place 0 " + sx + " " + sy + " " + sz + " minecraft:air"));
        assertTrue("could not replace sawBlade with air: " + breakBlock,
                breakBlock.contains("\"ok\":true"));

        String broken = String.join("\n",
                client().execute("artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("try-complete errored after break: " + broken, broken.contains("\"ok\":true"));
        assertTrue("structure stayed complete after sawBlade removal — validator broken: " + broken,
                broken.contains("\"isComplete\":false"));

        // Step 4 — restore the sawblade → re-validate → isComplete=true again.
        String restore = String.join("\n",
                client().execute("artest place 0 " + sx + " " + sy + " " + sz + " advancedrocketry:sawBlade"));
        assertTrue("could not restore sawBlade: " + restore,
                restore.contains("\"placed\":true"));

        String recomplete = String.join("\n",
                client().execute("artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("validator failed to re-detect a restored structure: " + recomplete,
                recomplete.contains("\"isComplete\":true"));
    }
}
