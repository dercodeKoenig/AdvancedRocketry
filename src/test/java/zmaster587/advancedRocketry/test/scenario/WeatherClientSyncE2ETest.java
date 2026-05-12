package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Ignore;
import org.junit.Test;

/**
 * SMART §7.20 — multi-planet weather isolation as visible from the client.
 *
 * <p>Deferred until per-dimension weather (B1) lands and a client-side teleport
 * + weather-overlay-state probe is available.</p>
 */
@Ignore("Requires weather B1 + client teleport probe — deferred")
public class WeatherClientSyncE2ETest extends AbstractClientE2ETest {

    @Test
    public void rainOnPlanetAIsNotVisibleOnPlanetB() throws Exception {
        // Filled in after B1.
    }
}
