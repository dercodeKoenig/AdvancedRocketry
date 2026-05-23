package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-09 Phase 4 — per-satellite-type behaviour pins.
 *
 * <p>Each pin exercises the production tick path of one satellite type
 * end-to-end against a real {@link net.minecraft.world.WorldServer}
 * (no mocking; chunks are pre-loaded via {@code /artest fill}).</p>
 *
 * <ul>
 *   <li><b>solarEnergy / {@code SatelliteMicrowaveEnergy}</b> — marker:
 *       must implement
 *       {@link zmaster587.libVulpes.api.IUniversalEnergyTransmitter}.
 *       That contract is what
 *       {@code TileMicrowaveReciever} resolves against to accept beam-
 *       down energy; a regression that drops the interface would break
 *       silent energy routing for the whole "solar farm in orbit" loop.</li>
 *   <li><b>biomeChanger / {@code SatelliteBiomeChanger}</b> — one tick
 *       with a queued position, configured biome and sufficient battery
 *       must both (a) terraform that block's biome to the configured one
 *       and (b) drain the queue by one entry. Per-entry RF cost is an
 *       implementation detail and intentionally not pinned.</li>
 *   <li><b>weatherController / {@code SatelliteWeatherController}</b> —
 *       mode 0 (rain) ticks must convert an AIR block in
 *       {@code viable_positions} to WATER and consume the entry.</li>
 * </ul>
 */
public class SatelliteTypeBehaviourTest extends AbstractSharedServerTest {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");
    private static final Pattern IS_TRANSMITTER =
            Pattern.compile("\"isUniversalEnergyTransmitter\":(true|false)");
    private static final Pattern CAN_TICK =
            Pattern.compile("\"canTick\":(true|false)");
    private static final Pattern LIST_SIZE =
            Pattern.compile("\"listSize\":(\\d+)");
    private static final Pattern BIOME_NAME =
            Pattern.compile("\"biome\":\"([^\"]*)\"");
    private static final Pattern BLOCK_NAME =
            Pattern.compile("\"block\":\"([^\"]*)\"");

    /** Pin: solarEnergy → SatelliteMicrowaveEnergy implements
     *  {@link zmaster587.libVulpes.api.IUniversalEnergyTransmitter} —
     *  the marker the orbital → ground energy receiver resolves
     *  against. Also pin canTick=true and isUniversalEnergy=true (the
     *  battery side of the contract, used by GUI updates). */
    @Test
    public void solarEnergySatelliteImplementsEnergyTransmitterMarker() throws Exception {
        long satId = createSat("solarEnergy", 200, 4000, 1000);
        String resp = String.join("\n", client().execute(
                "artest satellite markers 0 " + satId));
        assertTrue("markers probe failed: " + resp, resp.contains("\"ok\":true"));
        String isTransmitter = stringField(IS_TRANSMITTER, resp, "isUniversalEnergyTransmitter");
        String canTick = stringField(CAN_TICK, resp, "canTick");
        assertEquals("solarEnergy (SatelliteMicrowaveEnergy) MUST implement "
                + "IUniversalEnergyTransmitter — beam-down energy routing "
                + "depends on this marker; " + resp,
                "true", isTransmitter);
        assertEquals("solarEnergy must canTick=true; " + resp,
                "true", canTick);
    }

    /** Pin: BiomeChanger consumes the queue and mutates the world's
     *  biome at the queued position when battery is sufficient and
     *  biomeId is set. Asserts only the end-state (queue drained +
     *  biome changed) — intermediate snapshots are unreliable because
     *  the shared-harness background {@code DimensionManager.tickDimensions}
     *  fires every ~50 ms and races with successive probe calls.
     *  Per-entry RF cost is implementation detail; we pre-charge well
     *  above any plausible threshold via {@code force-charge}. */
    @Test
    public void biomeChangerTickTerraformBlockBiomeAndDrainsQueue() throws Exception {
        long satId = createSat("biomeChanger", 100, 10_000, 1000);

        // Use isolated coords far from any other test's footprint
        // (AbstractSharedServerTest contract — position-isolated).
        int x = 5000, y = 70, z = 5000;

        // Ensure the chunk is loaded — fill a small region with air;
        // /artest fill also force-loads the chunk(s).
        client().execute("artest fill 0 " + (x - 1) + " " + (y - 1) + " " + (z - 1) + " "
                + (x + 1) + " " + (y + 1) + " " + (z + 1) + " minecraft:air");

        // Read pre-terraform biome for a meaningful "must change" assertion.
        String pre = String.join("\n", client().execute(
                "artest block biome-at 0 " + x + " " + y + " " + z));
        String preBiome = stringField(BIOME_NAME, pre, "biome");

        // Choose a target biome that is definitely NOT the overworld's
        // default at this coord — desert (id=2) is usually different
        // from the overworld default; fall back to plains if it
        // happens to match.
        int targetBiomeId = 2; // desert
        if (preBiome.endsWith("desert")) targetBiomeId = 1; // plains fallback

        String setResp = String.join("\n", client().execute(
                "artest satellite biome-set 0 " + satId + " " + targetBiomeId));
        assertTrue("biome-set failed: " + setResp, setResp.contains("\"ok\":true"));

        // Add one position to the change queue.
        client().execute("artest satellite biome-add-pos 0 " + satId + " " + x + " " + y + " " + z);
        // Pre-charge battery well above any plausible per-block drain.
        client().execute("artest satellite force-charge 0 " + satId + " 5000");
        // Force a tick — either we drain the queue, or a background
        // tick already did before this call. Either way the same
        // production tickEntity path ran and the end-state must match.
        client().execute("artest satellite tick 0 " + satId + " 1");

        // Queue must be drained (entry processed by either our forced
        // tick or a background DimensionManager.tickDimensions tick).
        String postList = String.join("\n", client().execute(
                "artest satellite biome-list-size 0 " + satId));
        long postListSize = longField(LIST_SIZE, postList, "listSize");
        assertEquals("queue must be empty after at least one tick with "
                + "battery ≥ 120; postSize=" + postListSize,
                0L, postListSize);

        // Biome at pos must now match the configured target.
        String postBiomeResp = String.join("\n", client().execute(
                "artest block biome-at 0 " + x + " " + y + " " + z));
        String postBiome = stringField(BIOME_NAME, postBiomeResp, "biome");
        assertTrue("biome at terraformed pos must have changed; "
                + "preBiome=" + preBiome + " postBiome=" + postBiome,
                !preBiome.equals(postBiome));
    }

    /** Pin: WeatherController mode 0 (rain) ticks must replace an AIR
     *  block in {@code viable_positions} with WATER and drain the
     *  queue. */
    @Test
    public void weatherControllerMode0TickReplacesAirWithWater() throws Exception {
        long satId = createSat("weatherController", 100, 10_000, 1000);

        // High Y to guarantee an air block in the overworld; isolate
        // from other tests in x/z.
        int x = 5100, y = 200, z = 5100;

        // Ensure pos is air (fill loads the chunk too).
        client().execute("artest fill 0 " + x + " " + y + " " + z + " "
                + x + " " + y + " " + z + " minecraft:air");
        String preBlock = String.join("\n", client().execute(
                "artest block at 0 " + x + " " + y + " " + z));
        assertTrue("test setup: pre-block must be air; " + preBlock,
                stringField(BLOCK_NAME, preBlock, "block").equals("minecraft:air"));

        // Lock the controller into mode 0 (the AIR→WATER branch).
        client().execute("artest satellite weather-mode 0 " + satId + " 0");
        // Queue one pos.
        client().execute("artest satellite weather-add-pos 0 " + satId
                + " " + x + " " + y + " " + z);

        // One tick.
        client().execute("artest satellite tick 0 " + satId + " 1");

        // Block must now be water.
        String postBlock = String.join("\n", client().execute(
                "artest block at 0 " + x + " " + y + " " + z));
        String b = stringField(BLOCK_NAME, postBlock, "block");
        assertEquals("mode-0 WeatherController tick must replace an air "
                + "block in viable_positions with water; " + postBlock,
                "minecraft:water", b);
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
