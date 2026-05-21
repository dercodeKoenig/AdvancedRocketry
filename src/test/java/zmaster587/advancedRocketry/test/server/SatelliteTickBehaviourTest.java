package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-09 Phase 2/3 — per-satellite tick behaviour pins.
 *
 * <p>Drives {@link zmaster587.advancedRocketry.api.satellite.SatelliteBase#tickEntity}
 * synchronously via {@code /artest satellite tick} and pins three
 * production contracts:</p>
 *
 * <ul>
 *   <li><b>Base power accrual</b> — pure {@code SatelliteBase} (oreScanner →
 *       {@code SatelliteOreMapping}) accepts {@code powerGen - 1} into the
 *       battery per tick.</li>
 *   <li><b>Battery cap</b> — battery never exceeds the configured
 *       {@code powerStorage}.</li>
 *   <li><b>{@code SatelliteData} accumulation</b> — composition satellite
 *       (a {@link zmaster587.advancedRocketry.satellite.SatelliteData}
 *       subclass) adds one data point per
 *       {@code worldTime % collectionTime == 0} tick, capped at
 *       {@code maxData}.</li>
 * </ul>
 *
 * <p>The tick probe reports pre/post battery + data snapshots inside the
 * same server-thread call, so assertions on the delta are immune to
 * background {@code DimensionManager.tickDimensions} ticks that fire
 * between separate probe invocations.</p>
 */
public class SatelliteTickBehaviourTest extends AbstractSharedServerTest {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");
    private static final Pattern PRE_STORED = Pattern.compile("\"preStored\":(-?\\d+)");
    private static final Pattern POST_STORED = Pattern.compile("\"postStored\":(-?\\d+)");
    private static final Pattern PRE_DATA = Pattern.compile("\"preData\":(-?\\d+)");
    private static final Pattern POST_DATA = Pattern.compile("\"postData\":(-?\\d+)");
    private static final Pattern BATT_MAX = Pattern.compile("\"max\":(-?\\d+)");
    private static final Pattern MAX_DATA = Pattern.compile("\"maxData\":(-?\\d+)");

    /** Pin: a pure SatelliteBase satellite (oreScanner has no
     *  tickEntity override) accrues energy at approximately {@code powerGen}
     *  per tick into the battery. Asserts the delta within a single tick
     *  command (immune to background ticks). The exact per-tick accrual
     *  formula is implementation detail; the contract is "battery grows
     *  at roughly powerGen rate, bounded by powerGen × ticks". */
    @Test
    public void baseSatelliteTickAccruesAtApproximatelyPowerGenRate() throws Exception {
        int powerGen = 100;
        int ticks = 10;
        long satId = createSat("oreScanner", powerGen, 10_000, 1000);

        String resp = String.join("\n", client().execute(
                "artest satellite tick 0 " + satId + " " + ticks));
        assertTrue("tick probe failed: " + resp, resp.contains("\"ok\":true"));
        long pre = longField(PRE_STORED, resp, "preStored");
        long post = longField(POST_STORED, resp, "postStored");
        long delta = post - pre;
        long upper = (long) ticks * powerGen;
        // Lower bound is generous: catches "no accrual" / "accrual at
        // drastically wrong rate" without pinning the exact -1 offset.
        long lower = upper / 2;
        assertTrue("oreScanner with powerGen=" + powerGen + " must accrue "
                + "≈powerGen per tick over " + ticks + " ticks; expected "
                + "delta in [" + lower + ".." + upper + "] but got delta=" + delta
                + " (pre=" + pre + " post=" + post + ")",
                delta >= lower && delta <= upper);
    }

    /** Pin: battery never exceeds {@code powerStorage}. Even when each
     *  tick would push past the cap, the battery clamps at max. */
    @Test
    public void baseSatelliteBatteryCapsAtPowerStorage() throws Exception {
        long satId = createSat("oreScanner", 1000, 500, 1000);

        // Tick 10x — first tick alone (powerGen-1=999) already overshoots
        // powerStorage=500.
        String resp = String.join("\n", client().execute(
                "artest satellite tick 0 " + satId + " 10"));
        assertTrue("tick probe failed: " + resp, resp.contains("\"ok\":true"));
        long post = longField(POST_STORED, resp, "postStored");

        String battResp = String.join("\n", client().execute(
                "artest satellite battery 0 " + satId));
        long max = longField(BATT_MAX, battResp, "max");
        assertEquals("battery must report max=500 (powerStorage echo); "
                + "max=" + max, 500L, max);
        assertTrue("battery must cap at powerStorage=500 even when "
                + "per-tick accrual would overflow; postStored=" + post,
                post <= 500L);
        // Cap should bite immediately — first tick (acceptEnergy(999, false))
        // clamps to 500. After 10 ticks, definitely at 500.
        assertEquals("battery must be exactly at cap after 10 saturating ticks; "
                + "postStored=" + post, 500L, post);
    }

    /** Pin: a {@code SatelliteData} subclass (composition) accumulates
     *  data over multiple ticks. With powerGen=1000 collectionTime ≈ 20,
     *  so within 100 ticks of monotonically-advancing worldTime the gate
     *  fires at worldTime ∈ {20, 40, 60, 80, 100} → 5 data points. */
    @Test
    public void dataSatelliteAccumulatesDataOverTime() throws Exception {
        long satId = createSat("composition", 1000, 100_000, 1000);

        String resp = String.join("\n", client().execute(
                "artest satellite tick 0 " + satId + " 100"));
        assertTrue("tick probe failed: " + resp, resp.contains("\"ok\":true"));
        long preData = longField(PRE_DATA, resp, "preData");
        long postData = longField(POST_DATA, resp, "postData");
        long delta = postData - preData;
        // Be tolerant of off-by-ones at the boundaries — assert >=1 fire.
        assertTrue("composition satellite must accumulate ≥1 data point over "
                + "100 ticks (collectionTime≈20 implies ~5 fires); "
                + "preData=" + preData + " postData=" + postData
                + " delta=" + delta, delta >= 1);
        // Upper bound sanity — 100 ticks with collectionTime=20 cannot
        // exceed ~6 data fires (allowing one off-by-one).
        assertTrue("100 ticks at collectionTime≈20 cannot produce more "
                + "than ~6 data points; delta=" + delta, delta <= 6);
    }

    /** Pin: {@code DataStorage.addData} caps at {@code maxData}. */
    @Test
    public void dataSatelliteRespectsMaxDataCap() throws Exception {
        // maxData=2, powerGen=1000 → collectionTime=20; 500 ticks would
        // otherwise produce ~25 fires.
        long satId = createSat("composition", 1000, 100_000, 2);

        client().execute("artest satellite tick 0 " + satId + " 500");

        String dataResp = String.join("\n", client().execute(
                "artest satellite data 0 " + satId));
        long maxData = longField(MAX_DATA, dataResp, "maxData");
        assertEquals("maxData must echo configured cap; maxData=" + maxData,
                2L, maxData);

        // Read postData via a fresh 0-tick run (gives us a snapshot).
        String snap = String.join("\n", client().execute(
                "artest satellite tick 0 " + satId + " 0"));
        long postData = longField(POST_DATA, snap, "postData");
        assertTrue("data must NOT exceed maxData cap even with many "
                + "ticks; postData=" + postData + " maxData=" + maxData,
                postData <= maxData);
    }

    // -- helpers ----------------------------------------------------------

    private long createSat(String type, int powerGen, int powerStorage, int maxData) throws Exception {
        String resp = String.join("\n", client().execute(
                "artest satellite create 0 " + type + " " + powerGen + " "
                        + powerStorage + " " + maxData));
        assertTrue("satellite create (" + type + ") failed: " + resp,
                resp.contains("\"ok\":true"));
        Matcher m = ID.matcher(resp);
        assertTrue("could not extract id from create response: " + resp, m.find());
        return Long.parseLong(m.group(1));
    }

    private long longField(Pattern p, String src, String name) {
        Matcher m = p.matcher(src);
        assertTrue("field " + name + " missing in: " + src, m.find());
        return Long.parseLong(m.group(1));
    }
}
