package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * SMART §7.18 — special infrastructure (railgun, laser, beacon, elevator, force
 * field). For each, place block, probe tile, force-tick if ITickable — no
 * exceptions allowed.
 */
public class SpecialInfrastructureSmokeTest extends AbstractHeadlessServerTest {

    @Test
    public void allSpecialBlocksPlaceAndTickWithoutException() throws Exception {
        int y = 64;
        int baseX = 700, baseZ = 700;

        Map<String, Integer> devices = new LinkedHashMap<>();
        devices.put("advancedrocketry:railgun", 0);
        devices.put("advancedrocketry:beacon", 10);
        devices.put("advancedrocketry:forceFieldProjector", 20);
        devices.put("advancedrocketry:spaceLaser", 30);
        devices.put("advancedrocketry:spaceElevatorController", 40);

        StringBuilder failures = new StringBuilder();
        int errors = 0;
        for (Map.Entry<String, Integer> e : devices.entrySet()) {
            String blockId = e.getKey();
            int x = baseX + e.getValue();
            String place = String.join("\n", client().execute(
                    "artest place 0 " + x + " " + y + " " + baseZ + " " + blockId));
            if (!place.contains("\"placed\":true")) {
                failures.append(blockId).append("=PLACE_FAILED;");
                errors++;
                continue;
            }
            String info = String.join("\n", client().execute(
                    "artest machine info 0 " + x + " " + y + " " + baseZ));
            if (info.contains("Exception")
                    || (!info.contains("\"tileClass\"") && !info.contains("\"no tile entity\""))) {
                failures.append(blockId).append("=INFO_BAD;");
                errors++;
                continue;
            }
            if (info.contains("\"tileClass\"")) {
                String tick = String.join("\n", client().execute(
                        "artest tile force-tick 0 " + x + " " + y + " " + baseZ + " 5"));
                if (tick.contains("Exception") || tick.contains("\"error\":\"tile.update")) {
                    failures.append(blockId).append("=TICK_THREW(").append(tick).append(");");
                    errors++;
                }
            }
        }

        assertEquals("special infrastructure failures: " + failures, 0, errors);
    }
}
