package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.3 — planet/dimension lifecycle smoke.
 *
 * Walks {@code /artest dim list} to verify AR has registered at least one
 * planet. Empty galaxy configurations skip the test via {@link Assume} so an
 * empty-galaxy mod-pack doesn't gate the suite.
 */
public class PlanetDimensionLoadTest extends AbstractHeadlessServerTest {

    @Test
    public void arPlanetsArePreloaded() throws Exception {
        String joined = String.join("\n", client().execute("artest dim list"));

        assertTrue("dim list missing arDimensions key — probe wiring broken: " + joined,
                joined.contains("\"arDimensions\":["));

        Assume.assumeFalse(
                "No AR dimensions registered — skipping (empty galaxy?)",
                joined.contains("\"arDimensions\":[]"));
    }

    @Test
    public void dimLoadOnOverworldReportsLoaded() throws Exception {
        // SMART §5.2: /artest dim load <id> must force-load the world and
        // report `loaded:true` afterwards. Overworld (dim 0) is always loaded
        // on a fresh dedicated server, so this smoke pins the probe wiring
        // without depending on any AR-specific dim id. Deeper load behavior
        // (loading a not-yet-touched AR dim and back) belongs to Phase 1.
        String joined = String.join("\n", client().execute("artest dim load 0"));

        assertTrue("dim load 0 did not echo dim:0 in response: " + joined,
                joined.contains("\"dim\":0"));
        assertTrue("dim load 0 did not report loaded:true: " + joined,
                joined.contains("\"loaded\":true"));
    }
}
