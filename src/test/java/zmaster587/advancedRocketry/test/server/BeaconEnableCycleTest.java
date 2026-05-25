package zmaster587.advancedRocketry.test.server;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-19 Phase 3 — Beacon multiblock enable cycle on an AR-native
 * planet.
 *
 * <p>Production contract ({@code TileBeacon.setMachineEnabled(boolean)}):</p>
 *
 * <pre>{@code
 * if (DimensionManager.getInstance().isDimensionCreated(dim)) {
 *     DimensionProperties props = ...getDimensionProperties(dim);
 *     if (enabled)  props.addBeaconLocation(world, pos);
 *     else          props.removeBeaconLocation(world, pos);
 * }
 * }</pre>
 *
 * <p>Plus the block-break path ({@code BlockBeacon.breakBlock}):</p>
 *
 * <pre>{@code
 * if (tile instanceof TileBeacon && isDimensionCreated(dim))
 *     props.removeBeaconLocation(world, pos);
 * }</pre>
 *
 * <p>Pinning the registry mutation makes the "beacon-finder item locates
 * powered beacons" feature provable: the contract between {@code TileBeacon}
 * and {@code DimensionProperties.beaconLocations} is what the finder
 * item reads. Without these pins, either link can silently regress
 * (enable doesn't add, disable doesn't remove, break leaves an orphan
 * entry) and the finder item starts misbehaving with no compile-time
 * signal.</p>
 *
 * <p><b>Why AR-native planet only</b>: the {@code isDimensionCreated}
 * guard skips the registry call on overworld + any non-AR dim. Tests on
 * overworld would pass trivially (no mutation at all) — the contract
 * being verified is the WHOLE chain incl. the guard, so the test must
 * run on a dim the guard accepts.</p>
 *
 * <p><b>State sharing</b>: one AR planet generated in {@code @BeforeClass}
 * for all three methods — beacon locations don't leak between methods
 * because each test uses distinct controller coords and queries its
 * own pos in the dim's beacon set.</p>
 */
public class BeaconEnableCycleTest extends AbstractSharedServerTest {

    private static final int CY = 64;
    private static final int CZ = 100;
    private static final int CX_ENABLE  = 100;
    private static final int CX_DISABLE = 200;
    private static final int CX_BREAK   = 300;

    private static final Pattern DIM_LINE = Pattern.compile("DIM(\\d+):");
    private static final Pattern BEACON_TRIPLE =
            Pattern.compile("\\[(-?\\d+),(-?\\d+),(-?\\d+)]");

    private static int planetDim = -1;

    @BeforeClass
    public static void generateSharedPlanet() throws Exception {
        Set<Integer> before = arDims();
        exec("ar planet generate 0 BeaconPhase3 10 10 10");
        Set<Integer> diff = arDims();
        diff.removeAll(before);
        assertTrue("planet generate must add exactly one dim — diff=" + diff,
                diff.size() == 1);
        planetDim = diff.iterator().next();

        String load = exec("artest dim load " + planetDim);
        assertTrue("planet dim load failed: " + load,
                load.contains("\"loaded\":true") || load.contains("\"ok\":true"));
    }

    @AfterClass
    public static void deleteSharedPlanet() throws Exception {
        if (planetDim != -1) {
            try { exec("ar planet delete " + planetDim); } catch (Exception ignored) {}
            planetDim = -1;
        }
    }

    /** Powered + enabled beacon on an AR-created dim → controller pos
     *  appears in {@code DimensionProperties.beaconLocations}. */
    @Test
    public void enabledBeaconRegistersLocation() throws Exception {
        buildFixture(CX_ENABLE);
        enableMachine(CX_ENABLE, true);

        assertTrue("beacon list does not contain enabled controller pos"
                        + " (" + CX_ENABLE + "," + CY + "," + CZ + ") — "
                        + readBeaconList(),
                beaconListContains(CX_ENABLE, CY, CZ));
    }

    /** Counter-test: a beacon that's never enabled stays absent from
     *  the dim's beacon registry. */
    @Test
    public void disabledBeaconDoesNotRegister() throws Exception {
        buildFixture(CX_DISABLE);
        // Explicit set-enabled false (idempotent with default) so a stale
        // value from sibling test methods can't masquerade as "default
        // false" — even though setMachineEnabled(false) when already
        // false is a no-op, this guards against test ordering issues.
        enableMachine(CX_DISABLE, false);

        assertFalse("never-enabled beacon ended up in registry anyway"
                        + " (" + CX_DISABLE + "," + CY + "," + CZ + ") — "
                        + readBeaconList(),
                beaconListContains(CX_DISABLE, CY, CZ));
    }

    /** After enabling + verifying registration, breaking the controller
     *  block must unregister via the
     *  {@code BlockBeacon.breakBlock → removeBeaconLocation} path. */
    @Test
    public void breakingControllerBlockUnregisters() throws Exception {
        buildFixture(CX_BREAK);
        enableMachine(CX_BREAK, true);
        assertTrue("baseline: enabled beacon must be registered first — "
                        + readBeaconList(),
                beaconListContains(CX_BREAK, CY, CZ));

        // Break the controller via place-air. world.setBlockState calls
        // the old block's breakBlock callback in Forge 1.12, which is
        // how BlockBeacon.breakBlock gets a chance to clean up the
        // registry entry.
        String breakResp = exec("artest place " + planetDim + " "
                + CX_BREAK + " " + CY + " " + CZ + " minecraft:air");
        assertTrue("could not air-replace controller block: " + breakResp,
                breakResp.contains("\"ok\":true"));

        assertFalse("broken-controller beacon still in registry"
                        + " (" + CX_BREAK + "," + CY + "," + CZ + ") — "
                        + readBeaconList(),
                beaconListContains(CX_BREAK, CY, CZ));
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private static void buildFixture(int cx) throws Exception {
        String fixture = exec("artest fixture multiblock beacon "
                + planetDim + " " + cx + " " + CY + " " + CZ);
        assertTrue("beacon fixture build failed: " + fixture,
                fixture.contains("\"ok\":true"));
        String tryComplete = exec("artest machine try-complete "
                + planetDim + " " + cx + " " + CY + " " + CZ);
        assertTrue("beacon structure failed to complete: " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
    }

    private static void enableMachine(int cx, boolean enabled) throws Exception {
        String resp = exec("artest machine set-enabled " + planetDim + " "
                + cx + " " + CY + " " + CZ + " " + enabled);
        assertTrue("machine set-enabled failed: " + resp,
                resp.contains("\"enabled\":" + enabled));
    }

    private static String readBeaconList() throws Exception {
        return exec("artest beacon list " + planetDim);
    }

    /** True iff the dim's beacon-locations registry contains the triple
     *  (x, y, z). Walks each {@code [x,y,z]} entry in the {@code locations}
     *  array of {@code /artest beacon list}. */
    private static boolean beaconListContains(int x, int y, int z) throws Exception {
        String resp = readBeaconList();
        int locsStart = resp.indexOf("\"locations\"");
        assertTrue("beacon list response missing locations field: " + resp,
                locsStart >= 0);
        Matcher m = BEACON_TRIPLE.matcher(resp);
        m.region(locsStart, resp.length());
        while (m.find()) {
            if (Integer.parseInt(m.group(1)) == x
                    && Integer.parseInt(m.group(2)) == y
                    && Integer.parseInt(m.group(3)) == z) {
                return true;
            }
        }
        return false;
    }

    private static Set<Integer> arDims() throws Exception {
        String list = exec("ar planet list");
        Set<Integer> ids = new HashSet<>();
        Matcher m = DIM_LINE.matcher(list);
        while (m.find()) ids.add(Integer.parseInt(m.group(1)));
        return ids;
    }
}
