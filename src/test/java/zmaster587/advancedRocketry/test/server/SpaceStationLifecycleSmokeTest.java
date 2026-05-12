package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.11 — space station lifecycle.
 *
 * Empty list → create real {@link zmaster587.advancedRocketry.stations.SpaceStationObject}
 * via probe → assert list/info reflect it.
 */
public class SpaceStationLifecycleSmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(-?\\d+)");

    @Test
    public void stationCreateRegistersAndPersistsForList() throws Exception {
        String emptyList = String.join("\n", client().execute("artest station list"));
        assertTrue("expected empty stations on fresh server, got: " + emptyList,
                emptyList.contains("\"stations\":[]"));

        String createResp = String.join("\n", client().execute("artest station create 0"));
        assertTrue("station create failed: " + createResp, createResp.contains("\"ok\":true"));

        Matcher m = ID_PATTERN.matcher(createResp);
        assertTrue("could not extract station id: " + createResp, m.find());
        int stationId = Integer.parseInt(m.group(1));

        String listAfter = String.join("\n", client().execute("artest station list"));
        assertTrue("created station " + stationId + " missing from list: " + listAfter,
                listAfter.contains("\"id\":" + stationId));

        String info = String.join("\n", client().execute("artest station info " + stationId));
        assertTrue("station info wrong orbitingPlanetId: " + info,
                info.contains("\"orbitingPlanetId\":0"));
        assertTrue("station info wrong default fuelAmount: " + info,
                info.contains("\"fuelAmount\":0"));
    }
}
