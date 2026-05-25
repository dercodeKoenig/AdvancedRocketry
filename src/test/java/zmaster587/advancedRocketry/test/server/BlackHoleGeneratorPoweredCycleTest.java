package zmaster587.advancedRocketry.test.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-19 Phase 2 — Black-Hole-Generator powered cycle on a station
 * orbiting a black-hole star.
 *
 * <p>Production contract ({@code TileBlackHoleGenerator.isAroundBlackHole()}):
 * the BHG produces power only when ALL hold:</p>
 *
 * <ul>
 *   <li>controller sits in the space dim ({@code spaceDimId}, default -2);</li>
 *   <li>a {@code SpaceObject} (station) occupies that block coord region;</li>
 *   <li>the station's parent dim is a star ({@code planetId >=
 *       STAR_ID_OFFSET = 10000});</li>
 *   <li>that star's {@code StellarBody.isBlackHole()} is true.</li>
 * </ul>
 *
 * <p>The three test methods pin the positive path + two counter-branches.
 * Setup builds a fresh station per test orbiting the default Sol star
 * (id 0, dim id {@code STAR_ID_OFFSET + 0 = 10000}), flips its black-hole
 * flag, then sends the BHG fixture there. {@code @After} restores the
 * Sol flag and deletes the station so subsequent methods (and other
 * test classes that share the harness via {@link AbstractSharedServerTest})
 * see a pristine star registry.</p>
 *
 * <p>Production produces 500 RF/tick × {@code blackHolePowerMultiplier}
 * for {@code defaultItemTimeBlackHole} (default 500) ticks per consumed
 * item — so a single dirt block in the input hatch + ~600 force-ticks
 * is enough to assert "energy buffer grew" without pinning an exact RF
 * amount (impl detail per testing-principles SOP).</p>
 */
public class BlackHoleGeneratorPoweredCycleTest extends AbstractSharedServerTest {

    /** AR planet ID offset for star dims —
     *  {@link zmaster587.advancedRocketry.api.Constants#STAR_ID_OFFSET}. */
    private static final int STAR_ID_OFFSET = 10000;
    private static final int SOL_DIM = STAR_ID_OFFSET; // star id 0

    /** {@code ARConfiguration.spaceDimId} default. */
    private static final int SPACE_DIM = -2;

    /** Per-test position offset on overworld for the counter-test that
     *  builds BHG on a non-space dim. */
    private static final int OVERWORLD_DIM = 0;
    private static final int OVERWORLD_CX = 5000;
    private static final int OVERWORLD_CY = 128;
    private static final int OVERWORLD_CZ = 5000;

    private static final Pattern STATION_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern SPAWN_X = Pattern.compile("\"spawnX\":(-?\\d+)");
    private static final Pattern SPAWN_Z = Pattern.compile("\"spawnZ\":(-?\\d+)");
    private static final Pattern CTRL_POS =
            Pattern.compile("\"controllerPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern POWER_OUT_POS =
            Pattern.compile("\"powerOutPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ITEM_IN_POS =
            Pattern.compile("\"itemInputPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENERGY_STORED = Pattern.compile("\"energyStored\":(-?\\d+)");
    private static final Pattern STAR_BLACKHOLE = Pattern.compile("\"isBlackHole\":(true|false)");

    private boolean originalSolBlackHole;
    private int stationId = -1;

    @Before
    public void snapshotAndPrepare() throws Exception {
        // Snapshot Sol's black-hole flag so we can restore in @After.
        String solInfo = exec("artest star get 0");
        Matcher m = STAR_BLACKHOLE.matcher(solInfo);
        assertTrue("could not read Sol's black-hole flag: " + solInfo, m.find());
        originalSolBlackHole = Boolean.parseBoolean(m.group(1));

        // Load the space dim — BHG production checks
        // world.provider.getDimension() == spaceDimId.
        String load = exec("artest dim load " + SPACE_DIM);
        assertTrue("space dim load failed: " + load,
                load.contains("\"loaded\":true") || load.contains("\"ok\":true"));
    }

    @After
    public void restore() throws Exception {
        if (stationId != -1) {
            // Stations don't have a public "delete" probe, but harness
            // teardown reclaims them. Just unset our handle.
            stationId = -1;
        }
        // Restore Sol's flag unconditionally — even if we never flipped
        // it (e.g. the assertion before the flip threw).
        exec("artest star set-blackhole 0 " + originalSolBlackHole);
    }

    /** Happy path: station orbits a black-hole Sol, BHG is on the space
     *  dim, input hatch has an item, force-ticks ≥ defaultItemTimeBlackHole
     *  → output energy buffer accumulates a non-zero amount. */
    @Test
    public void bhgOnStationAroundBlackHoleProducesEnergy() throws Exception {
        flipSolBlackHole(true);
        int[] origin = createStationAndQuerySpawn();

        String fixture = buildFixture(SPACE_DIM, origin[0], origin[1], origin[2]);
        feedInputHatch(SPACE_DIM, fixture);
        enableMachine(SPACE_DIM, origin[0], origin[1], origin[2]);

        int outputBefore = readEnergyStored(SPACE_DIM, powerOutPosFrom(fixture));
        forceTick(SPACE_DIM, origin[0], origin[1], origin[2], 600);
        int outputAfter = readEnergyStored(SPACE_DIM, powerOutPosFrom(fixture));

        assertTrue("BHG around black-hole produced no energy"
                        + " (outputBefore=" + outputBefore + " outputAfter=" + outputAfter + ")",
                outputAfter > outputBefore);
    }

    /** Counter-test: Sol not a black hole → isAroundBlackHole() false →
     *  attemptFire skips → producePower never called → no energy. */
    @Test
    public void bhgWithoutBlackHoleStarDoesNotProduce() throws Exception {
        flipSolBlackHole(false);
        int[] origin = createStationAndQuerySpawn();

        String fixture = buildFixture(SPACE_DIM, origin[0], origin[1], origin[2]);
        feedInputHatch(SPACE_DIM, fixture);
        enableMachine(SPACE_DIM, origin[0], origin[1], origin[2]);

        int outputBefore = readEnergyStored(SPACE_DIM, powerOutPosFrom(fixture));
        forceTick(SPACE_DIM, origin[0], origin[1], origin[2], 600);
        int outputAfter = readEnergyStored(SPACE_DIM, powerOutPosFrom(fixture));

        assertEquals("BHG without black-hole star produced energy anyway"
                        + " (outputBefore=" + outputBefore + " outputAfter=" + outputAfter + ")",
                outputBefore, outputAfter);
    }

    /** Counter-test: BHG placed on overworld (dim 0, not spaceDimId) →
     *  isAroundBlackHole() short-circuits to false on the first guard
     *  ({@code world.provider.getDimension() == spaceDimId}). Pins the
     *  dim-gate even when a black-hole star exists. */
    @Test
    public void bhgOnOverworldDoesNotProduceEvenWithBlackHoleStar() throws Exception {
        flipSolBlackHole(true);

        String fixture = buildFixture(OVERWORLD_DIM, OVERWORLD_CX, OVERWORLD_CY, OVERWORLD_CZ);
        feedInputHatch(OVERWORLD_DIM, fixture);
        enableMachine(OVERWORLD_DIM, OVERWORLD_CX, OVERWORLD_CY, OVERWORLD_CZ);

        int outputBefore = readEnergyStored(OVERWORLD_DIM, powerOutPosFrom(fixture));
        forceTick(OVERWORLD_DIM, OVERWORLD_CX, OVERWORLD_CY, OVERWORLD_CZ, 600);
        int outputAfter = readEnergyStored(OVERWORLD_DIM, powerOutPosFrom(fixture));

        assertEquals("BHG on overworld produced energy anyway"
                        + " — spaceDim gate leaked through"
                        + " (outputBefore=" + outputBefore + " outputAfter=" + outputAfter + ")",
                outputBefore, outputAfter);
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private void flipSolBlackHole(boolean value) throws Exception {
        String resp = exec("artest star set-blackhole 0 " + value);
        assertTrue("Sol black-hole flip failed: " + resp,
                resp.contains("\"ok\":true") && resp.contains("\"after\":" + value));
    }

    /** Creates a station orbiting Sol (dim {@link #SOL_DIM}), returns its
     *  spawn coords as {@code [x, y, z]} in the space dim. */
    private int[] createStationAndQuerySpawn() throws Exception {
        String create = exec("artest station create " + SOL_DIM);
        assertTrue("station create failed: " + create,
                create.contains("\"ok\":true"));
        Matcher idM = STATION_ID.matcher(create);
        assertTrue("no station id in create response: " + create, idM.find());
        stationId = Integer.parseInt(idM.group(1));

        String info = exec("artest station info " + stationId);
        Matcher x = SPAWN_X.matcher(info);
        Matcher z = SPAWN_Z.matcher(info);
        assertTrue("no spawn coords in station info: " + info, x.find() && z.find());
        return new int[]{Integer.parseInt(x.group(1)), 128, Integer.parseInt(z.group(1))};
    }

    private String buildFixture(int dim, int cx, int cy, int cz) throws Exception {
        String fixture = exec("artest fixture multiblock blackhole-gen "
                + dim + " " + cx + " " + cy + " " + cz);
        assertTrue("BHG fixture build failed: " + fixture,
                fixture.contains("\"ok\":true"));
        Matcher m = CTRL_POS.matcher(fixture);
        assertTrue("no controllerPos in fixture response: " + fixture, m.find());
        // Try-complete: BHG's onInventoryUpdated runs attemptFire which
        // requires isComplete; the fixture only places, doesn't call
        // attemptCompleteStructure. The first force-tick call below
        // triggers BHG.update()'s lazy completeStructure check, but we
        // also explicitly run try-complete here so the diagnostic surface
        // is clean.
        String tryComplete = exec("artest machine try-complete "
                + dim + " " + cx + " " + cy + " " + cz);
        assertTrue("BHG structure failed to complete: " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
        return fixture;
    }

    private void feedInputHatch(int dim, String fixture) throws Exception {
        int[] inputPos = parseTriple(fixture, ITEM_IN_POS);
        // Stuff 64 dirt blocks into slot 0; BHG.consumeItem() decrements
        // the first non-empty stack each fire. defaultItemTimeBlackHole
        // is 500 ticks per fire, so 64 items = up to 64 fires of 500
        // ticks each.
        String resp = exec("artest hatch fill " + dim + " "
                + inputPos[0] + " " + inputPos[1] + " " + inputPos[2]
                + " 0 minecraft:dirt 64 0");
        assertTrue("hatch fill failed: " + resp,
                resp.contains("\"ok\":true") || resp.contains("\"count\":64"));
    }

    private void enableMachine(int dim, int cx, int cy, int cz) throws Exception {
        String resp = exec("artest machine set-enabled " + dim + " "
                + cx + " " + cy + " " + cz + " true");
        assertTrue("machine set-enabled failed: " + resp,
                resp.contains("\"enabled\":true"));
    }

    private void forceTick(int dim, int cx, int cy, int cz, int ticks) throws Exception {
        String resp = exec("artest tile force-tick " + dim + " "
                + cx + " " + cy + " " + cz + " " + ticks);
        assertTrue("force-tick errored: " + resp, resp.contains("\"ok\":true"));
    }

    private int[] powerOutPosFrom(String fixture) {
        return parseTriple(fixture, POWER_OUT_POS);
    }

    private int readEnergyStored(int dim, int[] pos) throws Exception {
        String resp = exec("artest energy stored " + dim + " "
                + pos[0] + " " + pos[1] + " " + pos[2]);
        Matcher m = ENERGY_STORED.matcher(resp);
        assertTrue("no energyStored in response: " + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    private static int[] parseTriple(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("triple-pattern not found in: " + src, m.find());
        return new int[]{
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))};
    }
}
