package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMART §7.15 — world generation + ore smoke.
 *
 * <ol>
 *   <li>Sample Earth chunk (0,0) — must yield non-air top and a real biome
 *       (proves the chunk generator runs without crash).</li>
 *   <li>Run a statistical ore scan over a 3x3 chunk area for the canonical
 *       vanilla {@code minecraft:iron_ore} — Earth must contain at least one
 *       block within that radius (deterministic enough at default
 *       biome/oregen settings).</li>
 *   <li>Run the same scan for {@code minecraft:bedrock} — at least
 *       {@code chunksScanned} blocks must be found (one bedrock layer is
 *       guaranteed by vanilla generation).</li>
 * </ol>
 *
 * <p>Ore-stats uses {@code /artest worldgen ore-stats} which counts block
 * occurrences in a chunk grid via the live world (not Perlin samples), so
 * results reflect the actual post-population state including AR oregen.</p>
 */
public class WorldgenSmokeTest extends HarnessBoundScenario {

    private static final Pattern COUNT = Pattern.compile("\"count\":(-?\\d+)");
    private static final Pattern CHUNKS = Pattern.compile("\"chunksScanned\":(-?\\d+)");

    @Override public String id() { return "ar.scenario.worldgen_smoke"; }
    @Override public String category() { return "P2/worldgen"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // 1. Top-block sanity at chunk (0,0).
        String sample = String.join("\n", client.execute("artest worldgen sample 0 0 0"));
        if (sample.contains("\"error\"")) {
            context.note("worldgen sample failed: " + sample);
            return TestStatus.FAILED;
        }
        if (sample.contains("\"topBlock\":\"minecraft:air\"")) {
            context.note("worldgen reports air on top — generator likely crashed: " + sample);
            return TestStatus.FAILED;
        }

        // 2. Statistical bedrock count. Vanilla generates a 1-3 block bedrock
        //    floor at y=0..4. A 3x3 chunk window (9 chunks × 256 blocks of floor
        //    space) must yield > 50 bedrock blocks (very conservative; actual is
        //    several hundred).
        String bedrock = String.join("\n",
                client.execute("artest worldgen ore-stats 0 0 0 1 minecraft:bedrock"));
        if (bedrock.contains("\"error\"")) {
            context.note("ore-stats bedrock failed: " + bedrock);
            return TestStatus.FAILED;
        }
        long bedrockCount = parseLong(COUNT, bedrock);
        long chunksScanned = parseLong(CHUNKS, bedrock);
        if (chunksScanned != 9L) {
            context.note("expected 9 chunks scanned, got " + chunksScanned + ": " + bedrock);
            return TestStatus.FAILED;
        }
        if (bedrockCount < 50L) {
            context.note("vanilla bedrock count too low: " + bedrockCount + " in " + bedrock);
            return TestStatus.FAILED;
        }

        // 3. Iron ore — Earth-like dimension at default ore config must have at
        //    least 1 iron ore in 9 chunks. AR's oregen is additive, so this is
        //    a regression tripwire: if oregen is broken, count drops to ~0.
        //
        //    To keep this deterministic across worldgen-config changes, only
        //    fail if count is 0 (extreme outlier; default-config Earth produces
        //    ~50-200 iron ores per 9 chunks).
        String iron = String.join("\n",
                client.execute("artest worldgen ore-stats 0 0 0 1 minecraft:iron_ore"));
        long ironCount = parseLong(COUNT, iron);
        if (ironCount <= 0L) {
            context.note("iron ore count=0 in 9-chunk window — oregen broken? " + iron);
            return TestStatus.FAILED;
        }

        context.note("worldgen OK: bedrock=" + bedrockCount + " iron=" + ironCount
                + " in " + chunksScanned + " chunks");
        return TestStatus.PASSED;
    }

    private static long parseLong(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Long.parseLong(m.group(1)) : -1L;
    }
}
