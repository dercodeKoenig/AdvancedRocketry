package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.8 — multiblock validation smoke.
 *
 * Exercises the {@code /artest place} + {@code /artest fill} + {@code /artest
 * machine info} fixture-builder primitives that any future fixture-bound
 * multiblock validation will rely on.
 */
public class MultiblockValidationSmokeTest extends AbstractHeadlessServerTest {

    @Test
    public void fixtureBuilderPrimitivesWork() throws Exception {
        String empty = String.join("\n", client().execute("artest machine info 0 200 100 200"));
        assertTrue("empty position machine info wrong: " + empty,
                empty.contains("\"error\":\"no tile entity\""));

        String place = String.join("\n",
                client().execute("artest place 0 200 100 200 minecraft:chest"));
        assertTrue("place chest failed: " + place, place.contains("\"placed\":true"));

        String info = String.join("\n", client().execute("artest machine info 0 200 100 200"));
        assertTrue("placed chest not detected: " + info, info.contains("TileEntityChest"));

        String fill = String.join("\n",
                client().execute("artest fill 0 210 100 210 212 102 212 minecraft:stone"));
        assertTrue("fill 3x3x3 stone failed: " + fill,
                fill.contains("\"ok\":true") && fill.contains("\"volume\":27"));
    }
}
