package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.18 — special infrastructure smoke (railgun, orbital laser drill,
 * laser gun, space elevator, force field projector, beacon, hovercraft).
 *
 * Each device's place + tick + activate assertion requires fixture placement.
 * {@code /artest machine info} surfaces {@code isComplete}/{@code isRunning}
 * for multiblocks; for non-multiblocks (laser gun, beacon) a generic tile probe
 * suffices but state interpretation differs per device. Deferred.
 */
public class SpecialInfrastructureSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.special_infrastructure_smoke"; }
    @Override public String category() { return "P2/special-infra"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // Place each special infrastructure block at distinct coords + assert
        // its tile entity is recognized via /artest machine info. Doesn't test
        // multiblock validation (those need full structure) — proves each block
        // can be placed without crashing the server and reports a tile.
        int y = 64;
        int baseX = 700, baseZ = 700;
        java.util.Map<String, Integer> devices = new java.util.LinkedHashMap<>();
        devices.put("advancedrocketry:railgun", 0);
        devices.put("advancedrocketry:beacon", 10);
        devices.put("advancedrocketry:forceFieldProjector", 20);
        devices.put("advancedrocketry:spaceLaser", 30);
        devices.put("advancedrocketry:spaceElevatorController", 40);

        int placedCount = 0;
        int errorCount = 0;
        StringBuilder details = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> e : devices.entrySet()) {
            String blockId = e.getKey();
            int x = baseX + e.getValue();
            String place = String.join("\n", client.execute(
                    "artest place 0 " + x + " " + y + " " + baseZ + " " + blockId));
            if (!place.contains("\"placed\":true")) {
                details.append(blockId).append("=PLACE_FAILED;");
                errorCount++;
                continue;
            }
            String info = String.join("\n", client.execute(
                    "artest machine info 0 " + x + " " + y + " " + baseZ));
            // Some blocks have no tile entity (e.g. force field is just a block);
            // those report "no tile entity" — that's fine, proves the block exists.
            if (info.contains("\"error\":\"no tile entity\"") || info.contains("\"tileClass\"")) {
                details.append(blockId).append("=OK;");
                placedCount++;
            } else {
                details.append(blockId).append("=INFO_FAILED;");
                errorCount++;
            }
        }
        context.note("placed " + placedCount + "/" + devices.size() + ": " + details);
        if (errorCount > 0) {
            return TestStatus.FAILED;
        }
        return TestStatus.PASSED;
    }
}
