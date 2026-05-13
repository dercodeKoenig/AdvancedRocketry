package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.14 deepen — terraformer multiblock controller smoke.
 *
 * <p>The full atmosphere terraformer is a 17×17×3+ multiblock made almost
 * entirely of libVulpes' {@code blockAdvStructureBlock} (whose registry name
 * isn't part of AR's public API). Building the complete fixture from /artest
 * primitives would need ~500 individual placements — wired through a future
 * {@code /artest fixture terraformer} probe.</p>
 *
 * <p>This scenario locks down the production paths that DON'T require the
 * complete multiblock:</p>
 * <ol>
 *   <li>{@code advancedrocketry:terraformer} controller block places + creates
 *       the {@link zmaster587.advancedRocketry.tile.multiblock.TileAtmosphereTerraformer};</li>
 *   <li>force-ticking the controller without a complete structure does NOT
 *       crash (the production path checks {@code isComplete} before doing
 *       any work);</li>
 *   <li>{@code /artest terraforming info} reports a consistent
 *       {@code proxyInitialized} state — the cross-cutting field every
 *       terraforming production path depends on.</li>
 * </ol>
 *
 * <p>The atmosphere mutation path (set-density → real density change with
 * original preserved) is exercised by {@link TerraformingSmokeTest}; this
 * scenario verifies the production controller doesn't blow up before the
 * mutation gets to run.</p>
 */
public class TerraformerMultiBlockCycleTest extends AbstractHeadlessServerTest {

    @Test
    public void terraformerControllerSurvivesTickWithoutStructure() throws Exception {
        int x = 2000, y = 64, z = 2000;

        String place = String.join("\n", client().execute(
                "artest place 0 " + x + " " + y + " " + z + " advancedrocketry:terraformer"));
        assertTrue("terraformer place failed: " + place,
                place.contains("\"placed\":true"));

        String info = String.join("\n", client().execute(
                "artest machine info 0 " + x + " " + y + " " + z));
        assertTrue("expected terraformer tile: " + info,
                info.contains("TileAtmosphereTerraformer"));

        // Try-complete on incomplete structure must report isComplete=false.
        String tryComplete = String.join("\n", client().execute(
                "artest machine try-complete 0 " + x + " " + y + " " + z));
        assertTrue("incomplete terraformer should report isComplete=false: " + tryComplete,
                tryComplete.contains("\"isComplete\":false"));

        // Force-tick — must not crash even with incomplete structure.
        String tick = String.join("\n", client().execute(
                "artest tile force-tick 0 " + x + " " + y + " " + z + " 60"));
        assertTrue("force-tick errored: " + tick, tick.contains("\"ok\":true"));
        assertEquals("must tick all 60 iterations",
                "60", extract(tick, "\"ticked\":(\\d+)"));

        // Tile must still resolve.
        String postInfo = String.join("\n", client().execute(
                "artest machine info 0 " + x + " " + y + " " + z));
        assertTrue("tile must survive tick burst: " + postInfo,
                postInfo.contains("TileAtmosphereTerraformer"));

        // Terraforming info must keep reporting proxyInitialized — the
        // cross-cutting field every gameplay path depends on. (Production:
        // DimensionProperties.proxyInitialized governs whether the
        // terraforming-helper has been built lazily.)
        String terraInfo = String.join("\n", client().execute(
                "artest terraforming info 0"));
        assertTrue("terraforming info missing proxyInitialized: " + terraInfo,
                terraInfo.contains("\"proxyInitialized\""));
    }

    private static String extract(String s, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(s);
        return m.find() ? m.group(1) : "";
    }
}
