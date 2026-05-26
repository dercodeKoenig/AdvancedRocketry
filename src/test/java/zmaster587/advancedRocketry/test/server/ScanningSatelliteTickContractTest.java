package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-29 — per-type tick-emits-data contracts for the four scanning
 * satellites that extend {@link zmaster587.advancedRocketry.satellite.SatelliteData}
 * (optical / density / mass / composition), plus the non-{@code SatelliteData}
 * oreScanner (player-driven, tick is battery-accrual only) and the
 * non-tickable spyTelescope.
 *
 * <p>The contract pinned here is per-type, not generic: each scanner is
 * <em>the</em> producer of one specific {@link
 * zmaster587.advancedRocketry.api.DataStorage.DataType}, and the data
 * stick / satellite-builder GUI / mission system branch on
 * {@code DataStorage.getDataType()}. A regression that swapped (say)
 * optical to emit {@code MASS} instead of {@code DISTANCE} would
 * silently break mission completion paths that demand a specific
 * data-type input — caught only by per-type pins.</p>
 *
 * <p>Generic {@code SatelliteData} accumulation (delta sanity, maxData
 * cap) is already pinned by {@link SatelliteTickBehaviourTest}; this
 * suite focuses on the per-type identity that suite intentionally
 * doesn't assert.</p>
 *
 * <p>Pyramid layer: testServer (needs the {@code /artest satellite tick}
 * probe path which advances worldTime to fire the {@code SatelliteData}
 * collection-time gate deterministically).</p>
 */
public class ScanningSatelliteTickContractTest extends AbstractSharedServerTest {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");
    private static final Pattern PRE_STORED = Pattern.compile("\"preStored\":(-?\\d+)");
    private static final Pattern POST_STORED = Pattern.compile("\"postStored\":(-?\\d+)");
    private static final Pattern PRE_DATA = Pattern.compile("\"preData\":(-?\\d+)");
    private static final Pattern POST_DATA = Pattern.compile("\"postData\":(-?\\d+)");
    private static final Pattern DATA_TYPE = Pattern.compile("\"dataType\":\"([^\"]*)\"");
    private static final Pattern IS_SAT_DATA = Pattern.compile("\"isSatelliteData\":(true|false)");
    private static final Pattern CAN_TICK = Pattern.compile("\"canTick\":(true|false)");

    /**
     * Pin: optical scanner emits DISTANCE-type data on powered tick.
     *
     * <p>Contract litmus: "fails if production breaks the contract that
     * the {@code optical} satellite is the producer of DISTANCE-type
     * data and accumulates that data over time when powered". The
     * dataType identity is what the optical-scanner item-tool and
     * companion-mod subscribers branch on; swapping it would break
     * downstream consumers silently.</p>
     */
    @Test
    public void opticalPoweredTickEmitsDistanceTypeData() throws Exception {
        long satId = createSat("optical", 1000, 100_000, 1000);

        String tickResp = String.join("\n", client().execute(
                "artest satellite tick 0 " + satId + " 100"));
        assertTrue("tick probe failed: " + tickResp, tickResp.contains("\"ok\":true"));
        long preData = longField(PRE_DATA, tickResp, "preData");
        long postData = longField(POST_DATA, tickResp, "postData");
        assertTrue("optical with powerGen=1000 must accumulate ≥1 data point "
                        + "over 100 ticks (collectionTime≈20); preData=" + preData
                        + " postData=" + postData,
                postData - preData >= 1);

        String dataResp = String.join("\n", client().execute(
                "artest satellite data 0 " + satId));
        assertEquals("optical satellite MUST emit DISTANCE-typed data — "
                        + "downstream mission gates branch on dataType; "
                        + dataResp,
                "DISTANCE", stringField(DATA_TYPE, dataResp, "dataType"));
    }

    /**
     * Pin: density scanner emits ATMOSPHEREDENSITY-type data on powered tick.
     *
     * <p>Contract litmus: "fails if production breaks the contract that
     * the {@code density} satellite is the producer of ATMOSPHEREDENSITY
     * data". The atmosphere-density readout drives the fuel-calc UI and
     * mission completion paths; a type swap would break those paths.</p>
     */
    @Test
    public void densityPoweredTickEmitsAtmosphereDensityTypeData() throws Exception {
        long satId = createSat("density", 1000, 100_000, 1000);

        String tickResp = String.join("\n", client().execute(
                "artest satellite tick 0 " + satId + " 100"));
        assertTrue("tick probe failed: " + tickResp, tickResp.contains("\"ok\":true"));
        long preData = longField(PRE_DATA, tickResp, "preData");
        long postData = longField(POST_DATA, tickResp, "postData");
        assertTrue("density with powerGen=1000 must accumulate ≥1 data point "
                        + "over 100 ticks; preData=" + preData
                        + " postData=" + postData,
                postData - preData >= 1);

        String dataResp = String.join("\n", client().execute(
                "artest satellite data 0 " + satId));
        assertEquals("density satellite MUST emit ATMOSPHEREDENSITY-typed data; "
                        + dataResp,
                "ATMOSPHEREDENSITY", stringField(DATA_TYPE, dataResp, "dataType"));
    }

    /**
     * Pin: mass scanner emits MASS-type data on powered tick.
     *
     * <p>Contract litmus: "fails if production breaks the contract that
     * the {@code mass} satellite is the producer of MASS data". MASS
     * feeds the fuel-cost calculation for landings; a wrong type would
     * silently change calc-failure messages or skip the calc entirely.</p>
     */
    @Test
    public void massScannerPoweredTickEmitsMassTypeData() throws Exception {
        long satId = createSat("mass", 1000, 100_000, 1000);

        String tickResp = String.join("\n", client().execute(
                "artest satellite tick 0 " + satId + " 100"));
        assertTrue("tick probe failed: " + tickResp, tickResp.contains("\"ok\":true"));
        long preData = longField(PRE_DATA, tickResp, "preData");
        long postData = longField(POST_DATA, tickResp, "postData");
        assertTrue("mass with powerGen=1000 must accumulate ≥1 data point "
                        + "over 100 ticks; preData=" + preData
                        + " postData=" + postData,
                postData - preData >= 1);

        String dataResp = String.join("\n", client().execute(
                "artest satellite data 0 " + satId));
        assertEquals("mass satellite MUST emit MASS-typed data; " + dataResp,
                "MASS", stringField(DATA_TYPE, dataResp, "dataType"));
    }

    /**
     * Pin: composition scanner emits COMPOSITION-type data on powered tick.
     *
     * <p>Overlaps the generic {@link SatelliteTickBehaviourTest#dataSatelliteAccumulatesDataOverTime}
     * (which already uses {@code composition} as its sample
     * {@link zmaster587.advancedRocketry.satellite.SatelliteData}
     * subclass) — but the existing test pins the
     * <em>generic SatelliteData</em> contract; this pin asserts the
     * <em>per-type identity</em> contract (dataType==COMPOSITION).
     * Together they would catch the case where a refactor moves the
     * dataType lock without breaking accumulation.</p>
     */
    @Test
    public void compositionPoweredTickEmitsCompositionTypeData() throws Exception {
        long satId = createSat("composition", 1000, 100_000, 1000);

        String tickResp = String.join("\n", client().execute(
                "artest satellite tick 0 " + satId + " 100"));
        assertTrue("tick probe failed: " + tickResp, tickResp.contains("\"ok\":true"));
        long preData = longField(PRE_DATA, tickResp, "preData");
        long postData = longField(POST_DATA, tickResp, "postData");
        assertTrue("composition with powerGen=1000 must accumulate ≥1 data point "
                        + "over 100 ticks; preData=" + preData
                        + " postData=" + postData,
                postData - preData >= 1);

        String dataResp = String.join("\n", client().execute(
                "artest satellite data 0 " + satId));
        assertEquals("composition satellite MUST emit COMPOSITION-typed data; "
                        + dataResp,
                "COMPOSITION", stringField(DATA_TYPE, dataResp, "dataType"));
    }

    /**
     * Pin: oreScanner is a non-{@code SatelliteData} satellite — its
     * tick accrues only battery and does not produce a
     * {@link zmaster587.advancedRocketry.api.DataStorage} stream.
     *
     * <p>Contract: oreScanner is player-driven (the player right-clicks
     * the chip to invoke {@code scanChunk}). Converting it to a
     * passive emitter would silently change the UX surface — the player
     * would no longer need to interact with it.</p>
     *
     * <p>Observable assertions:
     * <ol>
     *   <li>{@code markers.isSatelliteData == false} — the class-family
     *       gate that downstream consumers (terminal UI, satellite-info
     *       packet) branch on.</li>
     *   <li>{@code satellite data} returns the "not a SatelliteData
     *       subclass" error — confirming there is no data-stream surface
     *       to query.</li>
     *   <li>Battery accrues over ticks (delta &gt; 0) — confirms the
     *       inherited {@link
     *       zmaster587.advancedRocketry.api.satellite.SatelliteBase#tickEntity}
     *       still runs.</li>
     * </ol>
     */
    @Test
    public void oreMappingIsNotSatelliteDataAndPoweredTickAccruesBatteryOnly()
            throws Exception {
        long satId = createSat("oreScanner", 200, 100_000, 1000);

        String markers = String.join("\n", client().execute(
                "artest satellite markers 0 " + satId));
        assertEquals("oreScanner MUST report isSatelliteData=false — it is a "
                        + "player-driven scanner, not a passive data emitter; "
                        + markers,
                "false", stringField(IS_SAT_DATA, markers, "isSatelliteData"));

        String dataResp = String.join("\n", client().execute(
                "artest satellite data 0 " + satId));
        assertTrue("oreScanner has no DataStorage surface — `satellite data` "
                        + "probe must report it is not a SatelliteData subclass; "
                        + dataResp,
                dataResp.contains("\"error\":\"not a SatelliteData subclass\""));

        String tickResp = String.join("\n", client().execute(
                "artest satellite tick 0 " + satId + " 10"));
        assertTrue("tick probe failed: " + tickResp, tickResp.contains("\"ok\":true"));
        long preStored = longField(PRE_STORED, tickResp, "preStored");
        long postStored = longField(POST_STORED, tickResp, "postStored");
        assertTrue("oreScanner tick must still accrue battery (inherited "
                        + "SatelliteBase.tickEntity) — without this, the chip "
                        + "would never charge; preStored=" + preStored
                        + " postStored=" + postStored,
                postStored - preStored > 0);
    }

    /**
     * Pin: SpyTelescope's {@code canTick()} returns false AND its
     * (empty) {@code tickEntity} body produces no observable state
     * change even when invoked directly.
     *
     * <p>Defense-in-depth complement to {@link
     * SatelliteCoverageGapsTest#satelliteWithCanTickFalseIsNotAddedToTickingList}.
     * That test pins the registration gate (the satellite stays out of
     * {@code tickingSatellites}); this one pins the inner contract that
     * the {@code tickEntity} body itself is also a no-op — so any
     * future regression that bypasses the registration gate (mod-compat
     * hook calling tickEntity directly, etc.) doesn't smuggle in state
     * changes.</p>
     */
    @Test
    public void spyTelescopeCannotTickAndDirectTickEntityIsNoOp() throws Exception {
        String createResp = String.join("\n", client().execute(
                "artest satellite create-spy-telescope 0"));
        assertTrue("create-spy-telescope failed: " + createResp,
                createResp.contains("\"ok\":true"));
        Matcher m = ID.matcher(createResp);
        assertTrue("could not extract id from create response: " + createResp,
                m.find());
        long spyId = Long.parseLong(m.group(1));
        assertEquals("spyTelescope MUST report canTick=false; " + createResp,
                "false", stringField(CAN_TICK, createResp, "canTick"));

        // Directly invoke tickEntity 10 times via the probe — the
        // probe doesn't gate on canTick (it tests the production
        // tickEntity body unconditionally). SpyTelescope's body is
        // empty (does NOT call super.tickEntity), so battery must not
        // change.
        String tickResp = String.join("\n", client().execute(
                "artest satellite tick 0 " + spyId + " 10"));
        assertTrue("tick probe failed: " + tickResp,
                tickResp.contains("\"ok\":true"));
        long preStored = longField(PRE_STORED, tickResp, "preStored");
        long postStored = longField(POST_STORED, tickResp, "postStored");
        assertEquals("spyTelescope tickEntity is an empty body — even when "
                        + "called directly via the probe (bypassing the "
                        + "canTick=false registration gate) it must produce "
                        + "no battery change; preStored=" + preStored
                        + " postStored=" + postStored,
                preStored, postStored);
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

    private String stringField(Pattern p, String src, String name) {
        Matcher m = p.matcher(src);
        assertTrue("field " + name + " missing in: " + src, m.find());
        return m.group(1);
    }
}
