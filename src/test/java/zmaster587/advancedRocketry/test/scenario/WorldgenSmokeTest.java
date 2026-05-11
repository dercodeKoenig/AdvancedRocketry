package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.15 — world generation + ore smoke.
 *
 * Samples chunk (0,0) on Earth (dim=0) via {@code /artest worldgen sample}.
 * Verifies a non-air top block and a known biome exist after world gen runs —
 * which is enough to confirm the chunk generator runs without crashing. Per-ore
 * statistical assertions (SMART §7.15 "ore generation appears within sampled
 * statistical threshold") require sampling many chunks with a deterministic
 * seed — deferred to a follow-up.
 */
public class WorldgenSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.worldgen_smoke"; }
    @Override public String category() { return "P2/worldgen"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        List<String> sample = client.execute("artest worldgen sample 0 0 0");
        String joined = String.join("\n", sample);
        if (joined.contains("\"error\"")) {
            context.note("/artest worldgen sample failed on Earth chunk (0,0): " + joined);
            return TestStatus.FAILED;
        }
        if (!joined.contains("\"topBlock\"") || !joined.contains("\"biome\"")) {
            context.note("worldgen sample missing schema keys: " + joined);
            return TestStatus.FAILED;
        }
        if (joined.contains("\"topBlock\":\"minecraft:air\"")) {
            // Empty chunk top means worldgen never ran — likely a generator crash
            // suppressed somewhere. Surface as FAILED.
            context.note("worldgen sample reports air at top: " + joined);
            return TestStatus.FAILED;
        }
        context.note("Earth chunk (0,0) generated successfully");
        return TestStatus.PASSED;
    }
}
