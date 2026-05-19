package zmaster587.advancedRocketry.test.server;

// migrated to AbstractSharedServerTest (TASK-03 B2)
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 8 — extends {@code SpaceStationLifecycleSmokeTest}
 * (create-list-info already covered there) with:
 *   - multiple stations coexisting in the same orbit have distinct ids
 *   - fuel set / add / use are accounted (respect max capacity for add,
 *     clamp at zero for use)
 *   - fuelAmount survives across a station-info round trip
 */
public class SpaceStationDepthTest extends AbstractSharedServerTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern AFTER_PATTERN = Pattern.compile("\"after\":(-?\\d+)");
    private static final Pattern MAX_PATTERN = Pattern.compile("\"max\":(-?\\d+)");
    private static final Pattern RETURNED_PATTERN = Pattern.compile("\"returned\":(-?\\d+)");

    private int createStation(int orbitingDim) throws Exception {
        String resp = String.join("\n", client().execute("artest station create " + orbitingDim));
        assertTrue("station create failed: " + resp, resp.contains("\"ok\":true"));
        Matcher m = ID_PATTERN.matcher(resp);
        assertTrue("could not parse station id from create response: " + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    private static int parseGroup(Pattern pattern, String resp, String label) {
        Matcher m = pattern.matcher(resp);
        if (!m.find()) {
            throw new AssertionError("could not parse " + label + " from response: " + resp);
        }
        return Integer.parseInt(m.group(1));
    }

    @Test
    public void multipleStationsCoexistWithDistinctIds() throws Exception {
        int a = createStation(0);
        int b = createStation(0);
        int c = createStation(0);
        assertNotEquals("station ids must be unique within the same orbit", a, b);
        assertNotEquals(b, c);
        assertNotEquals(a, c);

        String list = String.join("\n", client().execute("artest station list"));
        assertTrue("station " + a + " missing from list: " + list,
                list.contains("\"id\":" + a));
        assertTrue("station " + b + " missing from list: " + list,
                list.contains("\"id\":" + b));
        assertTrue("station " + c + " missing from list: " + list,
                list.contains("\"id\":" + c));
    }

    @Test
    public void fuelSetUpdatesPersistsAndIsObservableViaInfo() throws Exception {
        int id = createStation(0);

        String setResp = String.join("\n",
                client().execute("artest station fuel " + id + " set 500"));
        int max = parseGroup(MAX_PATTERN, setResp, "max");
        int after = parseGroup(AFTER_PATTERN, setResp, "after");
        // setFuelAmount clamps to [0, max]; if max < 500 the after value will
        // be max, not 500. Assert the relationship rather than a literal.
        int expected = Math.min(500, max);
        assertEquals("fuel set did not produce expected after value", expected, after);

        String info = String.join("\n", client().execute("artest station info " + id));
        assertTrue("info must reflect the fuel amount we just set: " + info,
                info.contains("\"fuelAmount\":" + expected));
    }

    @Test
    public void fuelAddRespectsMaxCapacity() throws Exception {
        int id = createStation(0);
        // Drain to zero first so we have a known baseline.
        client().execute("artest station fuel " + id + " set 0");

        // SpaceStationObject.addFuel semantics: returns the amount actually
        // consumed (= inserted) AFTER the clamp to MAX_FUEL. Overshoot is
        // dropped silently. Surface this contract explicitly so the
        // "returned == clamp room" relationship is pinned.
        String addResp = String.join("\n",
                client().execute("artest station fuel " + id + " add 999999"));
        int max = parseGroup(MAX_PATTERN, addResp, "max");
        int after = parseGroup(AFTER_PATTERN, addResp, "after");
        int returned = parseGroup(RETURNED_PATTERN, addResp, "returned");

        assertEquals("after-add fuel must clamp at max", max, after);
        assertEquals("addFuel must return the amount actually added (= clamp room)",
                max, returned);
    }

    @Test
    public void fuelUseAllOrNothingWhenInsufficient() throws Exception {
        int id = createStation(0);
        client().execute("artest station fuel " + id + " set 100");

        // SpaceStationObject.useFuel semantics: if amt > current, it returns
        // 0 WITHOUT consuming anything. Pin this contract — it's
        // non-obvious and a "convenience" rewrite that clamps to current
        // available fuel would silently change rocket-launch fuel maths.
        String useResp = String.join("\n",
                client().execute("artest station fuel " + id + " use 999999"));
        int after = parseGroup(AFTER_PATTERN, useResp, "after");
        int returned = parseGroup(RETURNED_PATTERN, useResp, "returned");

        assertEquals("useFuel on insufficient stock must not consume anything",
                100, after);
        assertEquals("useFuel on insufficient stock must return 0",
                0, returned);
    }

    @Test
    public void fuelUseExactAmountDrains() throws Exception {
        int id = createStation(0);
        client().execute("artest station fuel " + id + " set 100");

        String useResp = String.join("\n",
                client().execute("artest station fuel " + id + " use 60"));
        int after = parseGroup(AFTER_PATTERN, useResp, "after");
        int returned = parseGroup(RETURNED_PATTERN, useResp, "returned");

        assertEquals("useFuel(60) on 100 stock must leave 40", 40, after);
        assertEquals("useFuel(60) must return 60 consumed", 60, returned);

        String info = String.join("\n", client().execute("artest station info " + id));
        assertTrue("info must reflect the partial drain: " + info,
                info.contains("\"fuelAmount\":40"));
    }
}
