package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SMART §8 P0.7 — vanilla / non-AR dimension isolation.
 *
 * Asserts that:
 * <ul>
 *   <li>nether (-1) and end (1) are not mis-classified as AR planets;</li>
 *   <li>the B1 weather wrapper is NOT installed on them — they must keep
 *       their vanilla {@code DerivedWorldInfo} so unrelated mods that read
 *       weather state see exactly what they would in vanilla;</li>
 *   <li>the overworld (dim=0) is also NOT wrapped — SMART §4 explicitly
 *       excludes dim 0 from the wrap policy.</li>
 * </ul>
 *
 * The wrapper-class assertion is the new (post-B1) guard: without it, a future
 * regression to {@link zmaster587.advancedRocketry.world.weather.PlanetWeatherManager#shouldWrap}
 * that accidentally accepts vanilla dims would only surface as a subtle
 * weather glitch on the Nether ages later.
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

    @Test
    public void overworldAndVanillaDimsAreNotWrapped() throws Exception {
        String overworld = String.join("\n", client().execute("artest weather get 0"));
        assertFalse("overworld must NOT have the AR weather wrapper installed: " + overworld,
                overworld.contains("ARWeatherWorldInfo"));

        String nether = String.join("\n", client().execute("artest weather get -1"));
        assertFalse("nether must NOT have the AR weather wrapper installed: " + nether,
                nether.contains("ARWeatherWorldInfo"));

        String end = String.join("\n", client().execute("artest weather get 1"));
        assertFalse("end must NOT have the AR weather wrapper installed: " + end,
                end.contains("ARWeatherWorldInfo"));

        // Sanity check — these three vanilla dims still respond and look
        // like real WorldInfo (the wrapper would say "ARWeatherWorldInfo",
        // a missing world would say "error", a misconfigured probe would
        // say neither — make sure we're observing real worldInfoClass data).
        assertTrue("overworld weather get must return a worldInfoClass field: " + overworld,
                overworld.contains("\"worldInfoClass\":"));
    }
}
