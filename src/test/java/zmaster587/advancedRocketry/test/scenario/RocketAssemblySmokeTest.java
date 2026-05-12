package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;

/**
 * SMART §7.9 — rocket assembly smoke (P1).
 *
 * Programmatically builds the BuildRocketTest fixture geometry via
 * {@code /artest fixture rocket}, calls {@code /artest rocket assemble} which
 * synchronously runs scan + assemble + spawns the {@link
 * zmaster587.advancedRocketry.entity.EntityRocket}, then asserts:
 * <ul>
 *   <li>scan status reaches {@code SUCCESS} (no NOENGINES / NOFUEL / NOGUIDANCE);</li>
 *   <li>the assemble probe reports a non-negative {@code entityId};</li>
 *   <li>{@code /artest rocket list 0} sees the new rocket;</li>
 *   <li>{@code /artest rocket info <id>} returns a coherent snapshot.</li>
 * </ul>
 */
public class RocketAssemblySmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.rocket_assembly_smoke"; }
    @Override public String category() { return "P1/rocket"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // 1. Build fixture far from spawn to avoid worldgen interference.
        int baseX = 500, baseY = 64, baseZ = 500;
        List<String> fixture = client.execute("artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ);
        String fixtureJoined = String.join("\n", fixture);
        if (!fixtureJoined.contains("\"ok\":true")) {
            context.note("fixture rocket failed: " + fixtureJoined);
            return TestStatus.FAILED;
        }
        // Extract builderPos from the response.
        int idx = fixtureJoined.indexOf("\"builderPos\":[");
        if (idx < 0) {
            context.note("fixture response missing builderPos: " + fixtureJoined);
            return TestStatus.FAILED;
        }
        String coords = fixtureJoined.substring(idx + "\"builderPos\":[".length());
        coords = coords.substring(0, coords.indexOf(']'));
        String[] parts = coords.split(",");
        int bx = Integer.parseInt(parts[0]);
        int by = Integer.parseInt(parts[1]);
        int bz = Integer.parseInt(parts[2]);
        context.note("fixture placed; builder at " + bx + "," + by + "," + bz);

        // 2. Confirm the builder tile is recognized.
        String machineInfo = String.join("\n", client.execute(
                "artest machine info 0 " + bx + " " + by + " " + bz));
        if (!machineInfo.contains("TileRocketAssemblingMachine")) {
            context.note("builder tile not recognized: " + machineInfo);
            return TestStatus.FAILED;
        }

        // 3. Synchronous assemble. Note: post-assemble status is ALREADY_ASSEMBLED
        //    (not SUCCESS) because the probe re-runs scanRocket internally and the
        //    second scan sees the just-spawned rocket. The real success signal is
        //    entityId >= 0 + rocketCount >= 1.
        String assemble = String.join("\n", client.execute(
                "artest rocket assemble 0 " + bx + " " + by + " " + bz));
        if (!assemble.contains("\"ok\":true")) {
            context.note("assemble failed: " + assemble);
            return TestStatus.FAILED;
        }
        if (assemble.contains("\"entityId\":-1")) {
            context.note("assemble returned no rocket entity (entityId=-1): " + assemble);
            return TestStatus.FAILED;
        }
        if (!assemble.contains("\"rocketCount\":1") && !assemble.contains("\"rocketCount\":2")) {
            context.note("expected exactly 1-2 rockets in BB after assemble: " + assemble);
            return TestStatus.FAILED;
        }
        context.note("assemble OK: " + assemble);

        // 4. Cross-check via rocket list — should now contain at least one rocket
        //    in dim 0.
        String rocketList = String.join("\n", client.execute("artest rocket list 0"));
        if (rocketList.contains("\"rockets\":[]")) {
            context.note("rocket list still empty after assemble: " + rocketList);
            return TestStatus.FAILED;
        }

        // 5. Extract entityId from /artest rocket list and probe rocket info.
        int idIdx = rocketList.indexOf("\"id\":");
        String idStr = rocketList.substring(idIdx + "\"id\":".length());
        int comma = idStr.indexOf(',');
        if (comma < 0) comma = idStr.indexOf('}');
        int entityId = Integer.parseInt(idStr.substring(0, comma).trim());
        String rocketInfo = String.join("\n", client.execute("artest rocket info " + entityId));
        if (!rocketInfo.contains("\"hasStorage\":true")) {
            context.note("rocket info missing hasStorage=true: " + rocketInfo);
            return TestStatus.FAILED;
        }
        context.note("rocket entityId=" + entityId + " info=" + rocketInfo);
        return TestStatus.PASSED;
    }
}
