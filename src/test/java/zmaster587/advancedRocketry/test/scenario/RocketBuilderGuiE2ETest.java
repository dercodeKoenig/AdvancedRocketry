package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Ignore;
import org.junit.Test;

/**
 * SMART §7.20 — open rocket assembling machine GUI, click build/scan, verify
 * server reports an assembled rocket via {@code /artest rocket list}.
 *
 * <p>Deferred — the server-side {@code /artest fixture rocket} + {@code rocket
 * assemble} probes cover the headless path; the GUI variant needs explicit
 * client-side button-click automation that's not yet wired.</p>
 */
@Ignore("Requires GUI button click automation for the assembler — deferred")
public class RocketBuilderGuiE2ETest extends AbstractClientE2ETest {

    @Test
    public void clickingBuildButtonAssemblesRocket() throws Exception {
        // Filled in once the GUI click path lands.
    }
}
