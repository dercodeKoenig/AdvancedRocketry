package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.client.ClientGuiTestSupport.openGuiByRightClick;
import static zmaster587.advancedRocketry.test.client.ClientGuiTestSupport.waitForNoScreen;

/**
 * SMART §7.20 — client GUI smoke for the rocket assembling machine.
 *
 * <p>The rocket builder is a {@code BlockTile} bound to the libVulpes
 * {@code MODULARNOINV} GUI id. This exercises the right-click → server
 * {@code openGui} → client {@code displayGuiScreen} round-trip for that block
 * and confirms the assembler's modular GUI opens client-side.</p>
 *
 * <p>The full scan → build → spawned {@code EntityRocket} loop is asynchronous
 * (multi-tick scan progress) and is covered synchronously and deterministically
 * by the headless {@code server/RocketAssemblySmokeTest} via
 * {@code /artest rocket assemble}; this client test locks down the GUI-open
 * path only.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true}; auto-skips on headless CI.</p>
 */
public class RocketBuilderGuiE2ETest extends AbstractClientE2ETest {

    private static final int X = 8, Y = 64, Z = 8;

    @Test
    public void rightClickingRocketBuilderOpensGui() throws Exception {
        String place = String.join("\n", serverClient().execute(
                "artest place 0 " + X + " " + Y + " " + Z + " advancedrocketry:rocketBuilder"));
        assertTrue("could not place rocketBuilder: " + place, place.contains("\"placed\":true"));

        // FG6 launcher gives the player a random "Player###" name — target via @a.
        serverClient().execute("tp @a " + (X + 0.5) + " " + (Y + 2) + " " + (Z + 0.5) + " 0 90");
        bot().waitTicks(40);

        String screen = openGuiByRightClick(bot(), X, Y, Z);
        assertTrue("expected a libVulpes modular GUI to open, got: " + screen,
                screen.startsWith("zmaster587.libVulpes.inventory.GuiModular"));

        bot().closeScreen();
        assertTrue("closeScreen didn't release the GUI: " + waitForNoScreen(bot(), 60),
                waitForNoScreen(bot(), 60).isEmpty());
    }
}
