package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.15 — worldgen + ore generation.
 *
 * <ol>
 *   <li>Earth chunk (0,0) must have a non-air top block.</li>
 *   <li>9-chunk window must contain >50 bedrock (vanilla).</li>
 *   <li>9-chunk window must contain >0 iron ore (AR oregen tripwire).</li>
 * </ol>
 */
public class WorldgenSmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern COUNT = Pattern.compile("\"count\":(-?\\d+)");
    private static final Pattern CHUNKS = Pattern.compile("\"chunksScanned\":(-?\\d+)");

    @Test
    public void earthChunkAndOreCountsLookSane() throws Exception {
        String sample = String.join("\n", client().execute("artest worldgen sample 0 0 0"));
        assertTrue("worldgen sample failed: " + sample, !sample.contains("\"error\""));
        assertTrue("worldgen reports air on top — generator likely crashed: " + sample,
                !sample.contains("\"topBlock\":\"minecraft:air\""));

        String bedrock = String.join("\n", client().execute(
                "artest worldgen ore-stats 0 0 0 1 minecraft:bedrock"));
        assertTrue("ore-stats bedrock failed: " + bedrock, !bedrock.contains("\"error\""));
        assertEquals("expected 9 chunks scanned", 9L, parseLong(CHUNKS, bedrock));
        long bedrockCount = parseLong(COUNT, bedrock);
        assertTrue("vanilla bedrock count too low: " + bedrockCount + " in " + bedrock,
                bedrockCount >= 50L);

        String iron = String.join("\n", client().execute(
                "artest worldgen ore-stats 0 0 0 1 minecraft:iron_ore"));
        long ironCount = parseLong(COUNT, iron);
        assertTrue("iron ore count=0 in 9-chunk window — oregen broken? " + iron,
                ironCount > 0L);
    }

    private static long parseLong(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Long.parseLong(m.group(1)) : -1L;
    }
}
