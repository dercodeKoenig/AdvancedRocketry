package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Coverage-audit gap (Tier 3 #12, client slice) — {@code ItemOreScanner}
 * right-click smoke.
 *
 * <p>Pin: {@code onItemRightClick} doesn't crash regardless of the
 * stored satellite-ID resolving to a registered satellite. The
 * production code path opens the OreMapping GUI when the stored
 * satellite-ID resolves to a {@code SatelliteOreMapping} on the
 * current dim. In headless harness, GUI-open is a no-op; what we
 * actually verify is "right-click runs without throwing".</p>
 *
 * <p>Two test methods:</p>
 *
 * <ul>
 *   <li><b>Empty satellite-ID branch</b> — held OreScanner has no NBT;
 *       {@code getSatelliteID} returns -1; {@code getSatellite(-1)}
 *       returns null; {@code instanceof SatelliteOreMapping} is false →
 *       early-out, no GUI, no crash.</li>
 *   <li><b>Resolved satellite-ID branch</b> — a registered
 *       SatelliteOreMapping on dim 0; held OreScanner NBT points at
 *       it; matches both class + dim guards → would open GUI in real
 *       client. Pin: no crash, no error reported.</li>
 * </ul>
 *
 * <p>Why testClient: server-side probe-driven test would be enough
 * for "no crash", but the GUI-open code path interacts with player
 * state in ways that only manifest in the full client harness. Even
 * if the harness skips actual rendering, the openGui packet path
 * runs server-side.</p>
 */
public class OreScannerRightClickClientE2ETest extends AbstractClientE2ETest {

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverClient().execute(cmd));
    }

    @Test
    public void rightClickWithEmptySatelliteIdDoesNotCrash() throws Exception {
        String resp = exec("artest player try-orescanner-rclick none");
        assertTrue("ore-scanner right-click probe must succeed: " + resp,
                resp.contains("\"ok\":true"));
        assertTrue("empty-satellite branch must not error: " + resp,
                resp.contains("\"error\":null"));
        assertTrue("empty branch must report hadSatelliteId:false: " + resp,
                resp.contains("\"hadSatelliteId\":false"));
        // Player is still alive (didn't crash the server thread).
        String state = exec("artest player held-air");
        assertFalse("held-air probe must succeed post-right-click (proves "
                        + "player state still intact): " + state,
                state.contains("\"error\""));
    }

    @Test
    public void rightClickWithRegisteredSatelliteIdResolvesWithoutError() throws Exception {
        // Register a fresh SatelliteOreMapping on dim 0 (overworld —
        // headless harness has a working DimensionProperties for it).
        String resp = exec("artest player try-orescanner-rclick 0");
        assertTrue("ore-scanner right-click probe must succeed: " + resp,
                resp.contains("\"ok\":true"));
        assertTrue("registered-satellite branch must report hadSatelliteId:true: "
                        + resp,
                resp.contains("\"hadSatelliteId\":true"));
        assertTrue("registered-satellite branch must not error: " + resp,
                resp.contains("\"error\":null"));
    }
}
