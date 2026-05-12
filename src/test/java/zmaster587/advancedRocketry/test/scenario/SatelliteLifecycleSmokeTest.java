package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMART §7.12 — satellite lifecycle.
 *
 * <ol>
 *   <li>{@code /artest satellite types} reports the canonical AR set (≥5 entries).</li>
 *   <li>{@code /artest satellite create 0 solarEnergy …} instantiates a real
 *       {@link zmaster587.advancedRocketry.api.satellite.SatelliteBase} and adds
 *       it to Earth's {@code DimensionProperties.satellites} map.</li>
 *   <li>{@code /artest satellite list 0} contains the new ID.</li>
 *   <li>{@code /artest satellite info 0 <id>} reports the expected type and
 *       power generation value (no NBT shortcut — the full
 *       {@code SatelliteProperties} object was injected via the production
 *       {@code addSatellite} path).</li>
 * </ol>
 *
 * <p>Type round-trip via NBT is covered by {@code SatellitePropertiesTest} (unit).</p>
 */
public class SatelliteLifecycleSmokeTest extends HarnessBoundScenario {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

    @Override public String id() { return "ar.scenario.satellite_lifecycle_smoke"; }
    @Override public String category() { return "P1/satellite"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // 1. Type registry — ≥5 entries means satellite system is wired.
        List<String> typesResp = client.execute("artest satellite types");
        String types = String.join("\n", typesResp);
        if (!types.contains("\"satelliteTypes\":[")) {
            context.note("satellite types schema invalid: " + types);
            return TestStatus.FAILED;
        }
        int typeCount = (countOccurrences(types, "\"") - 2) / 2;
        if (typeCount < 5) {
            context.note("expected ≥5 satellite types, got " + typeCount + ": " + types);
            return TestStatus.FAILED;
        }

        // 2. Create — use "solarEnergy" because it's a known-registered AR type
        //    (see AdvancedRocketry.serverStarting satellite registration).
        List<String> createResp = client.execute(
                "artest satellite create 0 solarEnergy 250 5000 1024");
        String create = String.join("\n", createResp);
        if (!create.contains("\"ok\":true")) {
            context.note("satellite create failed: " + create);
            return TestStatus.FAILED;
        }
        Matcher m = ID_PATTERN.matcher(create);
        if (!m.find()) {
            context.note("could not extract satellite id from: " + create);
            return TestStatus.FAILED;
        }
        long satId = Long.parseLong(m.group(1));

        // 3. List — must include our id.
        List<String> listResp = client.execute("artest satellite list 0");
        String list = String.join("\n", listResp);
        if (!list.contains("\"id\":" + satId)) {
            context.note("created satellite " + satId + " not in list: " + list);
            return TestStatus.FAILED;
        }

        // 4. Info — must match type + power gen we passed in.
        List<String> infoResp = client.execute("artest satellite info 0 " + satId);
        String info = String.join("\n", infoResp);
        if (!info.contains("\"type\":\"solarEnergy\"")) {
            context.note("info missing/wrong type: " + info);
            return TestStatus.FAILED;
        }
        if (!info.contains("\"powerGen\":250")) {
            context.note("info missing/wrong powerGen: " + info);
            return TestStatus.FAILED;
        }
        if (!info.contains("\"powerStorage\":5000")) {
            context.note("info missing/wrong powerStorage: " + info);
            return TestStatus.FAILED;
        }
        context.note("created+verified satellite id=" + satId + " type=solarEnergy");
        return TestStatus.PASSED;
    }

    private static int countOccurrences(String s, String needle) {
        int c = 0, i = 0;
        while ((i = s.indexOf(needle, i)) != -1) { c++; i += needle.length(); }
        return c;
    }
}
