package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Ignore;
import org.junit.Test;

/**
 * SMART §7.20 — open guidance computer GUI, insert destination chip, verify the
 * server-side destination via {@code /artest rocket info}.
 *
 * <p>Deferred — needs an assembled-rocket fixture controllable from the client
 * (chip slot interaction + creative inventory item-give).</p>
 */
@Ignore("Requires assembled-rocket fixture + guidance-chip probe — deferred")
public class GuidanceComputerGuiE2ETest extends AbstractClientE2ETest {

    @Test
    public void insertingChipSetsServerDestination() throws Exception {
        // Filled in once the chip-insertion fixture lands.
    }
}
