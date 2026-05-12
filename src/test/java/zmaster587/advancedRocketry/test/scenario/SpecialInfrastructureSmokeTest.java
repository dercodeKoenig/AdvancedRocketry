package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SMART §7.18 — special infrastructure (railgun, orbital laser drill, beacon,
 * space elevator, force field).
 *
 * <p>For each device:</p>
 * <ol>
 *   <li>Place the block.</li>
 *   <li>Probe its tile entity — either reports a {@code tileClass} or returns
 *       "no tile entity" (for non-TE devices). Both are acceptable; an
 *       exception is not.</li>
 *   <li>If the tile is {@link net.minecraft.util.ITickable}, drive it with
 *       {@code /artest tile force-tick … 5} and confirm no exception is thrown.
 *       Production tick paths must be crash-free even on minimally-configured
 *       fixtures.</li>
 * </ol>
 *
 * <p>Per SMART §15, this scenario does NOT exercise the full activation paths
 * (laser firing, force-field projection cycle) — those need GUI input or
 * adjacent multiblock structure validation. The tick smoke is the
 * regression-tripwire layer.</p>
 */
public class SpecialInfrastructureSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.special_infrastructure_smoke"; }
    @Override public String category() { return "P2/special-infra"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        int y = 64;
        int baseX = 700, baseZ = 700;

        Map<String, Integer> devices = new LinkedHashMap<>();
        devices.put("advancedrocketry:railgun", 0);
        devices.put("advancedrocketry:beacon", 10);
        devices.put("advancedrocketry:forceFieldProjector", 20);
        devices.put("advancedrocketry:spaceLaser", 30);
        devices.put("advancedrocketry:spaceElevatorController", 40);

        StringBuilder details = new StringBuilder();
        int errors = 0;
        for (Map.Entry<String, Integer> e : devices.entrySet()) {
            String blockId = e.getKey();
            int x = baseX + e.getValue();
            String place = String.join("\n", client.execute(
                    "artest place 0 " + x + " " + y + " " + baseZ + " " + blockId));
            if (!place.contains("\"placed\":true")) {
                details.append(blockId).append("=PLACE_FAILED;");
                errors++;
                continue;
            }
            String info = String.join("\n", client.execute(
                    "artest machine info 0 " + x + " " + y + " " + baseZ));
            if (info.contains("Exception")
                    || (!info.contains("\"tileClass\"") && !info.contains("\"no tile entity\""))) {
                details.append(blockId).append("=INFO_BAD;");
                errors++;
                continue;
            }
            // If the tile is ITickable, drive a few ticks. Non-tickable tiles
            // return "tile not ITickable" — that's not an error.
            if (info.contains("\"tileClass\"")) {
                String tick = String.join("\n", client.execute(
                        "artest tile force-tick 0 " + x + " " + y + " " + baseZ + " 5"));
                if (tick.contains("Exception") || tick.contains("\"error\":\"tile.update")) {
                    details.append(blockId).append("=TICK_THREW(").append(tick).append(");");
                    errors++;
                    continue;
                }
            }
            details.append(blockId).append("=OK;");
        }

        context.note(details.toString());
        if (errors > 0) {
            return TestStatus.FAILED;
        }
        return TestStatus.PASSED;
    }
}
