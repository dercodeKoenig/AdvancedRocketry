package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMART §7.11 — space station lifecycle.
 *
 * <ol>
 *   <li>Fresh server reports {@code "stations":[]}.</li>
 *   <li>{@code /artest station create 0} registers a real
 *       {@link zmaster587.advancedRocketry.stations.SpaceStationObject} orbiting
 *       the overworld via the production
 *       {@link zmaster587.advancedRocketry.stations.SpaceObjectManager} singleton.</li>
 *   <li>{@code /artest station list} now contains the new id.</li>
 *   <li>{@code /artest station info <id>} reports orbitingBody=0 and the
 *       default fuel amount (0 for a freshly created station).</li>
 * </ol>
 */
public class SpaceStationLifecycleSmokeTest extends HarnessBoundScenario {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(-?\\d+)");

    @Override public String id() { return "ar.scenario.space_station_lifecycle_smoke"; }
    @Override public String category() { return "P1/station"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // 1. Fresh server has no stations.
        String emptyList = String.join("\n", client.execute("artest station list"));
        if (!emptyList.contains("\"stations\":[]")) {
            context.note("expected empty stations on fresh server, got: " + emptyList);
            return TestStatus.FAILED;
        }

        // 2. Create one orbiting Earth (dim=0).
        String createResp = String.join("\n", client.execute("artest station create 0"));
        if (!createResp.contains("\"ok\":true")) {
            context.note("station create failed: " + createResp);
            return TestStatus.FAILED;
        }
        Matcher m = ID_PATTERN.matcher(createResp);
        if (!m.find()) {
            context.note("could not extract station id: " + createResp);
            return TestStatus.FAILED;
        }
        int stationId = Integer.parseInt(m.group(1));

        // 3. List contains it.
        String listAfter = String.join("\n", client.execute("artest station list"));
        if (!listAfter.contains("\"id\":" + stationId)) {
            context.note("created station " + stationId + " missing from list: " + listAfter);
            return TestStatus.FAILED;
        }

        // 4. Info reports expected state.
        String info = String.join("\n", client.execute("artest station info " + stationId));
        if (!info.contains("\"orbitingPlanetId\":0")) {
            context.note("station info wrong orbitingPlanetId: " + info);
            return TestStatus.FAILED;
        }
        if (!info.contains("\"fuelAmount\":0")) {
            context.note("station info wrong default fuelAmount: " + info);
            return TestStatus.FAILED;
        }

        context.note("created+verified station id=" + stationId + " orbiting=0");
        return TestStatus.PASSED;
    }
}
