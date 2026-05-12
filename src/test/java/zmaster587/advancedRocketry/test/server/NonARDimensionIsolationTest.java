package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * SMART §8 P0.7 — vanilla / non-AR dimension isolation.
 *
 * Asserts that AR's per-planet data does NOT leak into vanilla nether (-1) or
 * end (1). Note: the overworld (dim=0) IS intentionally AR-managed.
 */
public class NonARDimensionIsolationTest extends AbstractHeadlessServerTest {

    @Test
    public void netherAndEndAreNotARPlanets() throws Exception {
        String nether = String.join("\n", client().execute("artest dim info -1"));
        assertFalse("nether is mis-classified as an AR planet: " + nether,
                nether.contains("\"isARPlanet\":true"));

        String end = String.join("\n", client().execute("artest dim info 1"));
        assertFalse("end is mis-classified as an AR planet: " + end,
                end.contains("\"isARPlanet\":true"));
    }
}
