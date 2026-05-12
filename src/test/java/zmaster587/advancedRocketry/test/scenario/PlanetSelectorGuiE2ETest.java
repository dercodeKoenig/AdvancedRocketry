package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Ignore;
import org.junit.Test;

/**
 * SMART §7.20 — open planet selector / holographic selector GUI, click planet,
 * verify selection state propagates to server.
 *
 * <p>Deferred until a {@code /artest selector info} probe lands + fixture
 * holographic projector placement is automated.</p>
 */
@Ignore("Requires /artest selector info probe + fixture holographic projector — deferred")
public class PlanetSelectorGuiE2ETest extends AbstractClientE2ETest {

    @Test
    public void clickingPlanetSetsServerSelection() throws Exception {
        // Filled in once the selector probe lands.
    }
}
