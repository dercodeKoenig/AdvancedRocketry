package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-09 Phase 5 — coverage-gap closure for per-satellite behaviour.
 *
 * <p>Adds contract-level pins for behaviour the first two TASK-09
 * suites didn't reach. All assertions target observable outcomes
 * (block state, biome state, satellite-query result) rather than
 * internal counters or specific impl constants — see
 * {@code .agent/sops/development/testing-principles.md}.</p>
 *
 * <ul>
 *   <li>WeatherController mode 1 (water → air) and mode 2 (air →
 *       water) — pinning the visible block-state outcomes of each
 *       mode (the prior suite only covered mode 0).</li>
 *   <li>Mode-change discards old-mode queued work — switching modes
 *       between queue-build and tick must NOT process the queued
 *       positions under the OLD mode's rules.</li>
 *   <li>BiomeChanger drains multiple queued positions over time when
 *       given power.</li>
 *   <li>BiomeChanger with no configured biome leaves the world's
 *       biomes unchanged (no rogue mutation under misconfiguration).</li>
 *   <li>{@code canTick()=false} gate — production's
 *       {@code DimensionProperties.addSatellite} filters non-tickable
 *       satellites out of the tick loop's data structure. This pin
 *       is necessarily structural (SpyTelescope's tickEntity is a
 *       no-op so an observable-side-effect probe can't distinguish
 *       "in map but no-op" from "not in map"); the comment in the
 *       test documents the rationale.</li>
 *   <li>Dead satellites are purged from satellite queries on the next
 *       dim tick — the user-observable consequence of the
 *       {@code DimensionProperties.tick} removal branch.</li>
 * </ul>
 */
public class SatelliteCoverageGapsTest extends AbstractSharedServerTest {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");
    private static final Pattern BLOCK = Pattern.compile("\"block\":\"([^\"]*)\"");
    private static final Pattern BIOME = Pattern.compile("\"biome\":\"([^\"]*)\"");
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

    /** Pin: when the satellite's mode changes between queue-build and
     *  tick, the queued positions must NOT be processed under either
     *  the old mode's rules or the new mode's rules — they're
     *  discarded.
     *
     *  <p>This contract represents the player atomically changing mode
     *  while work is queued. The test uses an atomic compound probe
     *  ({@code weather-discard-test}) so the mode-mismatch window
     *  isn't lost to a racing background dim tick — without
     *  atomicity, a background tick can synchronise
     *  {@code last_mode_id} before our forced tick observes the
     *  mismatch, and subsequent ticks would process the queue
     *  normally.</p>
     */
    @Test
    public void weatherControllerModeChangeDiscardsQueuedWork() throws Exception {
        long satId = createSat("weatherController", 100, 10_000, 1000);
        int x = 5400, y = 200, z = 5400;
        int n = 3;

        // Set up AIR blocks. Without a mode change, mode 2 would
        // convert these to WATER.
        client().execute("artest fill 0 " + x + " " + y + " " + z + " "
                + (x + n - 1) + " " + y + " " + z + " minecraft:air");

        // Atomic: queue N AIR-targeting positions, switch mode_id to 2
        // (last_mode_id stays 0), invoke tickEntity once — all on the
        // same server-thread call, so the mode-mismatch IS the state
        // visible to that single tickEntity.
        client().execute("artest satellite weather-discard-test 0 " + satId
                + " 2 " + x + " " + y + " " + z + " " + n);

        // None of the queued positions must have been converted to
        // water — the mode-change discarded the work.
        for (int i = 0; i < n; i++) {
            int px = x + i;
            String b = stringField(BLOCK, String.join("\n", client().execute(
                    "artest block at 0 " + px + " " + y + " " + z)), "block");
            assertEquals("queued positions must NOT be processed when "
                    + "mode changes between queue-build and tick; pos=("
                    + px + "," + y + "," + z + ") block=" + b,
                    "minecraft:air", b);
        }
    }

    /** Pin: multiple queued positions all eventually terraform when
     *  the satellite has sufficient power and time. Pins the
     *  multi-position contract (queueing more than one pos isn't a
     *  stuck path) without nailing down exact batch size or per-tick
     *  throughput. */
    @Test
    public void biomeChangerEventuallyTerraformsAllQueuedPositions() throws Exception {
        long satId = createSat("biomeChanger", 100, 10_000, 1000);
        int baseX = 5500, y = 70, z = 5500;
        int n = 5;

        // Pre-load chunks at the synthetic test positions.
        client().execute("artest fill 0 " + baseX + " " + y + " " + z + " "
                + (baseX + n - 1) + " " + y + " " + z + " minecraft:air");

        // Snapshot the pre-terraform biome — for the assertion to be
        // meaningful, the target must differ from this. Use desert
        // (id=2); fall back to plains if pre is already desert.
        String preBiomeResp = String.join("\n", client().execute(
                "artest block biome-at 0 " + baseX + " " + y + " " + z));
        String preBiome = stringField(BIOME, preBiomeResp, "biome");
        int targetBiomeId = preBiome.endsWith("desert") ? 1 : 2;

        client().execute("artest satellite biome-set 0 " + satId + " " + targetBiomeId);
        client().execute("artest satellite force-charge 0 " + satId + " 5000");
        for (int i = 0; i < n; i++) {
            client().execute("artest satellite biome-add-pos 0 " + satId
                    + " " + (baseX + i) + " " + y + " " + z);
        }
        // Give the satellite a generous tick budget — the contract is
        // "all queued positions eventually terraform with power"; the
        // exact tick count is impl. 5 ticks is enough even under
        // pessimistic per-tick throughput.
        client().execute("artest satellite tick 0 " + satId + " 5");

        for (int i = 0; i < n; i++) {
            int px = baseX + i;
            String b = stringField(BIOME, String.join("\n", client().execute(
                    "artest block biome-at 0 " + px + " " + y + " " + z)), "biome");
            assertTrue("queued position must be terraformed after the "
                    + "satellite ticks with sufficient power; pos=("
                    + px + "," + y + "," + z + ") preBiome=" + preBiome
                    + " biome=" + b,
                    !preBiome.equals(b));
        }
    }

    /** Pin: a BiomeChanger with no configured biome does NOT mutate
     *  world biomes — under misconfiguration (null biomeId from save
     *  corruption, mod-compat fallback, etc.) the satellite must be
     *  inert from the player's POV, not silently terraform to some
     *  default. */
    @Test
    public void biomeChangerWithoutConfiguredBiomeLeavesWorldUnchanged() throws Exception {
        long satId = createSat("biomeChanger", 100, 10_000, 1000);
        int x = 5600, y = 70, z = 5600;

        client().execute("artest fill 0 " + (x - 1) + " " + (y - 1) + " " + (z - 1) + " "
                + (x + 1) + " " + (y + 1) + " " + (z + 1) + " minecraft:air");
        String preBiome = stringField(BIOME, String.join("\n", client().execute(
                "artest block biome-at 0 " + x + " " + y + " " + z)), "biome");

        // Null biomeId BEFORE queueing so any background tick can only
        // observe the null state.
        client().execute("artest satellite biome-null 0 " + satId);
        client().execute("artest satellite force-charge 0 " + satId + " 5000");
        client().execute("artest satellite biome-add-pos 0 " + satId + " " + x + " " + y + " " + z);
        client().execute("artest satellite tick 0 " + satId + " 1");

        String postBiome = stringField(BIOME, String.join("\n", client().execute(
                "artest block biome-at 0 " + x + " " + y + " " + z)), "biome");
        assertEquals("biome at pos must be unchanged when satellite has "
                + "no configured biome; preBiome=" + preBiome
                + " postBiome=" + postBiome,
                preBiome, postBiome);
    }

    /** Structural pin (NOT a pure user-visible contract — see SOP):
     *  a satellite with {@code canTick()=false} (SpyTelescope) is
     *  registered in the dim's satellite list but excluded from the
     *  production tick loop's data structure ({@code tickingSatellites}).
     *
     *  <p>Ideally we'd assert this via an observable side effect (i.e.
     *  "no tick = no battery growth"), but SpyTelescope's
     *  {@code tickEntity} is a no-op that doesn't call super either —
     *  battery would not change whether the production gate fires or
     *  not, so the observable-side-effect approach can't distinguish
     *  "in the map but no-op" from "not in the map".</p>
     *
     *  <p>The contract this pin protects: production's tick loop
     *  iterates {@code tickingSatellites} (not {@code satellites}),
     *  so the gate matters for any future canTick=false satellite
     *  with side-effect-bearing tickEntity. We test the gate
     *  directly because the visible consequence isn't reachable
     *  through SpyTelescope.</p>
     */
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

    /** Pin: a dead satellite disappears from satellite queries —
     *  callers querying it by id after death receive a "not found"
     *  response, not a stale reference. Observable through the
     *  public {@code satellite info} probe (which itself wraps the
     *  same {@code DimensionProperties.getSatellite} call any
     *  in-mod consumer would use). */
    @Test
    public void deadSatelliteIsRemovedFromSatelliteQueries() throws Exception {
        long satId = createSat("oreScanner", 100, 1000, 1000);

        // Sanity: freshly-created satellite is queryable.
        String pre = String.join("\n", client().execute(
                "artest satellite info 0 " + satId));
        assertTrue("freshly-created satellite must be queryable via "
                + "satellite info; resp=" + pre,
                pre.contains("\"id\":" + satId));

        // Mark dead + drive one DimensionProperties.tick() so the
        // production removal branch fires synchronously (instead of
        // waiting for the next background dim tick).
        client().execute("artest satellite set-dead 0 " + satId);
        client().execute("artest satellite force-tick-dim 0");

        String post = String.join("\n", client().execute(
                "artest satellite info 0 " + satId));
        assertTrue("dead satellite must no longer be queryable by id; "
                + "info should report not-found, got=" + post,
                post.contains("\"error\":\"satellite not found\""));
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

    private String stringField(Pattern p, String src, String name) {
        Matcher m = p.matcher(src);
        assertTrue("field " + name + " missing in: " + src, m.find());
        return m.group(1);
    }
}
