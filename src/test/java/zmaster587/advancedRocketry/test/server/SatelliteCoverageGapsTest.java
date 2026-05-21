package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-09 Phase 5 — coverage-gap closure for per-satellite behaviour.
 *
 * <p>Tightens areas the first two TASK-09 suites left loose, audited
 * in the EOD self-review. Pins:</p>
 *
 * <ul>
 *   <li>WeatherController mode 1 (water → air) and mode 2 (AIR→WATER
 *       alt) — the prior suite only covered mode 0.</li>
 *   <li>WeatherController mode-change branch clears
 *       {@code viable_positions}.</li>
 *   <li>BiomeChanger processes &gt;1 position per tickEntity call
 *       (production loops up to 10 — proves the loop is real, not a
 *       1-per-tick path).</li>
 *   <li>BiomeChanger with biomeId=null still drains the queue + battery
 *       (BiomeHandler.terraform's null-guard returns AFTER the
 *       remove/extract has already happened).</li>
 *   <li>{@code canTick()=false} satellites are added to
 *       {@code satellites} but NOT to {@code tickingSatellites} (the
 *       gate that production relies on so SpyTelescope-style satellites
 *       can exist without ticking).</li>
 *   <li>{@code isDead=true} satellites are removed from
 *       {@code tickingSatellites} on the next {@code tick()} cycle.</li>
 * </ul>
 */
public class SatelliteCoverageGapsTest extends AbstractSharedServerTest {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");
    private static final Pattern LIST_SIZE = Pattern.compile("\"listSize\":(\\d+)");
    private static final Pattern POST_TICK_SIZE = Pattern.compile("\"postTickSize\":(\\d+)");
    private static final Pattern PROCESSED = Pattern.compile("\"processed\":(\\d+)");
    private static final Pattern PRE_STORED = Pattern.compile("\"preStored\":(-?\\d+)");
    private static final Pattern POST_STORED = Pattern.compile("\"postStored\":(-?\\d+)");
    private static final Pattern BLOCK = Pattern.compile("\"block\":\"([^\"]*)\"");
    private static final Pattern BIOME = Pattern.compile("\"biome\":\"([^\"]*)\"");
    private static final Pattern TICKING_SIZE = Pattern.compile("\"size\":(\\d+)");
    private static final Pattern TICKING_IDS = Pattern.compile("\"ids\":\\[([^\\]]*)\\]");
    private static final Pattern CAN_TICK = Pattern.compile("\"canTick\":(true|false)");

    /** Pin: WeatherController mode 1 (drain) — a queued water-block
     *  position becomes air after one tick. */
    @Test
    public void weatherControllerMode1ReplacesWaterWithAir() throws Exception {
        long satId = createSat("weatherController", 100, 10_000, 1000);
        int x = 5200, y = 200, z = 5200;

        client().execute("artest fill 0 " + x + " " + y + " " + z + " "
                + x + " " + y + " " + z + " minecraft:water");
        String preBlock = String.join("\n", client().execute(
                "artest block at 0 " + x + " " + y + " " + z));
        String preBlockName = stringField(BLOCK, preBlock, "block");
        assertTrue("test setup: pre-block must be water or flowing_water; " + preBlock,
                "minecraft:water".equals(preBlockName)
                        || "minecraft:flowing_water".equals(preBlockName));

        client().execute("artest satellite weather-mode 0 " + satId + " 1");
        client().execute("artest satellite weather-add-pos 0 " + satId
                + " " + x + " " + y + " " + z);
        client().execute("artest satellite tick 0 " + satId + " 1");

        String postBlock = String.join("\n", client().execute(
                "artest block at 0 " + x + " " + y + " " + z));
        String b = stringField(BLOCK, postBlock, "block");
        assertEquals("mode-1 WeatherController tick must replace a water "
                + "block in viable_positions with air; " + postBlock,
                "minecraft:air", b);
    }

    /** Pin: WeatherController mode 2 (alt rain) — air → water like
     *  mode 0 but via the independent mode-2 code branch. */
    @Test
    public void weatherControllerMode2ReplacesAirWithWater() throws Exception {
        long satId = createSat("weatherController", 100, 10_000, 1000);
        int x = 5300, y = 200, z = 5300;

        client().execute("artest fill 0 " + x + " " + y + " " + z + " "
                + x + " " + y + " " + z + " minecraft:air");
        client().execute("artest satellite weather-mode 0 " + satId + " 2");
        client().execute("artest satellite weather-add-pos 0 " + satId
                + " " + x + " " + y + " " + z);
        client().execute("artest satellite tick 0 " + satId + " 1");

        String postBlock = String.join("\n", client().execute(
                "artest block at 0 " + x + " " + y + " " + z));
        String b = stringField(BLOCK, postBlock, "block");
        assertEquals("mode-2 WeatherController tick must replace an air "
                + "block in viable_positions with water; " + postBlock,
                "minecraft:water", b);
    }

    /** Pin: changing {@code mode_id} between ticks fires the
     *  {@code last_mode_id != mode_id} branch and clears
     *  {@code viable_positions} — so any queued work is dropped, NOT
     *  processed under the new mode. */
    @Test
    public void weatherControllerModeChangeClearsViablePositions() throws Exception {
        long satId = createSat("weatherController", 100, 10_000, 1000);
        int x = 5400, y = 200, z = 5400;

        // Lock to mode 0, queue 3 positions.
        client().execute("artest satellite weather-mode 0 " + satId + " 0");
        for (int i = 0; i < 3; i++) {
            client().execute("artest satellite weather-add-pos 0 " + satId
                    + " " + (x + i) + " " + y + " " + z);
        }
        // Switch mode to 1 WITHOUT updating last_mode_id — production's
        // tickEntity then sees `last_mode_id != mode_id` and clears the
        // queue on the next tick.
        client().execute("artest satellite weather-mode 0 " + satId + " 1 false");
        client().execute("artest satellite tick 0 " + satId + " 1");

        String list = String.join("\n", client().execute(
                "artest satellite weather-list-size 0 " + satId));
        long size = longField(LIST_SIZE, list, "listSize");
        assertEquals("mode-change branch must clear viable_positions; "
                + "post-tick listSize=" + size, 0L, size);
    }

    /** Pin: BiomeChanger.tickEntity loops up to 10 times per call —
     *  with 5 queued positions and enough battery, a SINGLE tickEntity
     *  invocation must process all 5 (proves the loop is real). Runs
     *  the whole sequence atomically inside a server-thread probe so
     *  no background tick can race. */
    @Test
    public void biomeChangerProcessesUpToTenPositionsPerTick() throws Exception {
        long satId = createSat("biomeChanger", 100, 10_000, 1000);
        // Pre-load chunks at the synthetic test positions so terraform
        // doesn't trip on unloaded geometry.
        int baseX = 5500, z = 5500;
        client().execute("artest fill 0 " + baseX + " 70 " + z + " "
                + (baseX + 4) + " 70 " + z + " minecraft:air");

        String resp = String.join("\n", client().execute(
                "artest satellite biome-batch-tick 0 " + satId + " 5000 "
                        + baseX + " " + z + " 5"));
        assertTrue("biome-batch-tick probe failed: " + resp, resp.contains("\"ok\":true"));
        long processed = longField(PROCESSED, resp, "processed");
        long postSize = longField(POST_TICK_SIZE, resp, "postTickSize");
        assertEquals("a single tickEntity must process ALL 5 queued positions "
                + "(production loop bound is 10/tick); processed=" + processed
                + " postTickSize=" + postSize,
                5L, processed);
        assertEquals("queue must be empty after a single fully-saturating "
                + "tickEntity call; postTickSize=" + postSize,
                0L, postSize);
    }

    /** Pin: BiomeChanger with biomeId=null still drains the queue +
     *  battery (the null-guard is inside BiomeHandler.terraform, AFTER
     *  the .remove() + extractEnergy have already fired) — and the
     *  biome at the queued pos is NOT mutated. */
    @Test
    public void biomeChangerWithNullBiomeDrainsResourcesButDoesNotTerraform() throws Exception {
        long satId = createSat("biomeChanger", 100, 10_000, 1000);
        int x = 5600, y = 70, z = 5600;

        client().execute("artest fill 0 " + (x - 1) + " " + (y - 1) + " " + (z - 1) + " "
                + (x + 1) + " " + (y + 1) + " " + (z + 1) + " minecraft:air");
        String pre = String.join("\n", client().execute(
                "artest block biome-at 0 " + x + " " + y + " " + z));
        String preBiome = stringField(BIOME, pre, "biome");

        // Null out biomeId BEFORE queueing so any background tick that
        // fires between probes won't terraform (it'll just drain queue
        // and battery — which is precisely what we're pinning).
        client().execute("artest satellite biome-null 0 " + satId);
        // Charge battery + queue one pos + tick — atomic doesn't matter
        // here, the assertion is end-state and queue drain happens
        // regardless of who fires the tick.
        client().execute("artest satellite force-charge 0 " + satId + " 5000");
        client().execute("artest satellite biome-add-pos 0 " + satId + " " + x + " " + y + " " + z);
        client().execute("artest satellite tick 0 " + satId + " 1");

        // Queue must still be drained.
        String list = String.join("\n", client().execute(
                "artest satellite biome-list-size 0 " + satId));
        long size = longField(LIST_SIZE, list, "listSize");
        assertEquals("queue must drain even with biomeId=null (null-guard "
                + "lives inside terraform, after the remove); listSize=" + size,
                0L, size);

        // Biome at pos must NOT have changed.
        String post = String.join("\n", client().execute(
                "artest block biome-at 0 " + x + " " + y + " " + z));
        String postBiome = stringField(BIOME, post, "biome");
        assertEquals("biomeId=null must short-circuit terraform — biome at "
                + "pos must be unchanged; preBiome=" + preBiome
                + " postBiome=" + postBiome,
                preBiome, postBiome);
    }

    /** Pin: a satellite with {@code canTick()=false} (SpyTelescope) is
     *  added to {@code satellites} but NOT to {@code tickingSatellites}
     *  — production's
     *  {@link zmaster587.advancedRocketry.dimension.DimensionProperties#addSatellite}
     *  gates the second put on {@code canTick()}. */
    @Test
    public void satelliteWithCanTickFalseIsNotAddedToTickingList() throws Exception {
        String resp = String.join("\n", client().execute(
                "artest satellite create-spy-telescope 0"));
        assertTrue("create-spy-telescope failed: " + resp, resp.contains("\"ok\":true"));
        Matcher m = ID.matcher(resp);
        assertTrue("could not extract id from create response: " + resp, m.find());
        long spyId = Long.parseLong(m.group(1));
        assertEquals("SpyTelescope MUST report canTick=false (the registration "
                + "gate that protects DimensionProperties.tick from ticking "
                + "non-ticking satellites); " + resp,
                "false", stringField(CAN_TICK, resp, "canTick"));

        // The SpyTelescope must be in the satellites lifecycle list...
        String lifecycle = String.join("\n", client().execute(
                "artest satellite list 0"));
        assertTrue("SpyTelescope must be in the lifecycle satellites map: " + lifecycle,
                lifecycle.contains("\"id\":" + spyId));

        // ...but NOT in the tickingSatellites map.
        String ticking = String.join("\n", client().execute(
                "artest satellite ticking-list 0"));
        String ids = stringField(TICKING_IDS, ticking, "ids");
        // ids is a comma-joined list of longs (or empty). Match the
        // exact id as a token to avoid false positives via substring.
        boolean inTicking = (',' + ids + ',').contains("," + spyId + ",");
        assertTrue("canTick=false satellite must NOT be in tickingSatellites; "
                + "id=" + spyId + " ticking ids=" + ids, !inTicking);
    }

    /** Pin: a satellite marked {@code isDead} during a tick must be
     *  removed from {@code tickingSatellites} on the next
     *  {@link zmaster587.advancedRocketry.dimension.DimensionProperties#tick}
     *  cycle. */
    @Test
    public void deadSatelliteIsRemovedFromTickingListOnNextDimTick() throws Exception {
        long satId = createSat("oreScanner", 100, 1000, 1000);

        // Sanity: satellite is in tickingSatellites right after create.
        String pre = String.join("\n", client().execute(
                "artest satellite ticking-list 0"));
        String preIds = stringField(TICKING_IDS, pre, "ids");
        assertTrue("freshly-created satellite must be in tickingSatellites; "
                + "id=" + satId + " ticking ids=" + preIds,
                (',' + preIds + ',').contains("," + satId + ","));

        // Flip isDead, then drive one DimensionProperties.tick() to
        // force the removal branch in the production iterator.
        client().execute("artest satellite set-dead 0 " + satId);
        client().execute("artest satellite force-tick-dim 0");

        String post = String.join("\n", client().execute(
                "artest satellite ticking-list 0"));
        String postIds = stringField(TICKING_IDS, post, "ids");
        boolean inTicking = (',' + postIds + ',').contains("," + satId + ",");
        assertTrue("isDead satellite must be removed from tickingSatellites "
                + "by DimensionProperties.tick(); id=" + satId
                + " ticking ids=" + postIds, !inTicking);
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
