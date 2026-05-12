package zmaster587.advancedRocketry.test.scenario;

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
}
