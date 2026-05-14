package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.18 — Beacon location list contract.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.multiblock.TileBeacon} adds its
 * world position to {@code DimensionProperties.beaconLocations} when its
 * multiblock structure is complete AND {@code setMachineEnabled(true)} fires
 * (in production: when the controller's redstone block reports power AND the
 * 4-tall structure-block tower is complete). The location is then consumed
 * by item beacon-finders, planet selectors, and the dimension-overview UI.</p>
 *
 * <p>This test pins the read-side API contract via the new
 * {@code /artest beacon list <dim>} probe: overworld must start with an
 * empty beacon set, the probe must emit a well-formed JSON envelope, and
 * the list must be queryable on every AR-managed dim (overworld, since
 * dim 0 reports {@code isARPlanet=true} per §7.7).</p>
 *
 * <p>The full beacon-enable cycle (place 3×5×3 multiblock, redstone power,
 * setMachineEnabled true → list grows) needs a dedicated
 * {@code /artest fixture beacon} probe — left as follow-up for the same
 * reason terraformer / BHG full cycles are deferred. The
 * {@code SpecialInfrastructureSmokeTest} already proves the beacon block
 * places + ticks without crashing.</p>
 */
public class BeaconLocationProbeSmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern COUNT = Pattern.compile("\"count\":(-?\\d+)");

    @Test
    public void beaconListReportsEmptySetOnOverworld() throws Exception {
        String resp = String.join("\n", client().execute("artest beacon list 0"));
        assertTrue("beacon list probe failed on overworld: " + resp,
                !resp.contains("\"error\""));

        Matcher m = COUNT.matcher(resp);
        assertTrue("response must contain count: " + resp, m.find());
        int count = Integer.parseInt(m.group(1));

        // Overworld starts with zero beacon locations because no beacon
        // multiblock has been enabled. A non-zero value would mean state
        // leaked between tests (the shared `RealDedicatedServerHarness`
        // tempdir is supposed to be fresh per test class).
        assertEquals("overworld must start with zero beacons: " + resp, 0, count);

        // Probe must also include the locations array (even when empty).
        assertTrue("response must declare locations array: " + resp,
                resp.contains("\"locations\":["));
    }

    @Test
    public void beaconListRejectsUnknownDim() throws Exception {
        // Choose a dim id that AR has never registered.
        int phantomDim = 30000;
        String resp = String.join("\n", client().execute("artest beacon list " + phantomDim));
        assertTrue("unknown dim must return error: " + resp,
                resp.contains("\"error\":\"dim not registered\""));
    }
}
