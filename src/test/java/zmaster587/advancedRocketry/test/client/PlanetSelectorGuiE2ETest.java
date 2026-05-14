package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.client.ClientGuiTestSupport.openGuiByRightClick;
import static zmaster587.advancedRocketry.test.client.ClientGuiTestSupport.waitForNoScreen;

/**
 * SMART §7.20 — client GUI smoke for the planet selector tile.
 *
 * <p>Exercises the full right-click → server {@code onBlockActivated} →
 * {@code openGui} → client {@code displayGuiScreen} round-trip:</p>
 * <ol>
 *   <li>Server places {@code advancedrocketry:planetSelector} in the spawn chunk.</li>
 *   <li>The harness player (random {@code Player###} name under the FG6 launcher,
 *       so addressed via the {@code @a} selector) is teleported onto the tile.</li>
 *   <li>{@code rightClickBlock} (retried) triggers the libVulpes modular GUI.</li>
 *   <li>{@code GuiModularFullScreen} is confirmed open.</li>
 *   <li>{@code bot.closeScreen()} releases it.</li>
 * </ol>
 *
 * <p>Does NOT click an individual planet — that needs empirically derived pixel
 * coordinates inside {@code ModulePlanetSelector}'s grid. The full selection
 * cycle is covered headless by {@code server/SelectorServerSmokeTest}.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true}; auto-skips on headless CI.</p>
 */
public class PlanetSelectorGuiE2ETest extends AbstractClientE2ETest {

    private static final int X = 8, Y = 64, Z = 8;

    @Test
    public void rightClickingTileOpensSelectorGui() throws Exception {
        String place = String.join("\n", serverClient().execute(
                "artest place 0 " + X + " " + Y + " " + Z + " advancedrocketry:planetSelector"));
        assertTrue("could not place planetSelector: " + place, place.contains("\"placed\":true"));

        // The FG6 client launcher assigns the player a random "Player###" name,
        // so target the single harness player with the @a selector.
        serverClient().execute("tp @a " + (X + 0.5) + " " + (Y + 2) + " " + (Z + 0.5) + " 0 90");
        bot().waitTicks(40);

        String screen = openGuiByRightClick(bot(), X, Y, Z);
        assertEquals("expected the planet selector modular GUI to open",
                "zmaster587.libVulpes.inventory.GuiModularFullScreen", screen);

        bot().closeScreen();
        assertTrue("closeScreen didn't release the GUI: " + waitForNoScreen(bot(), 60),
                waitForNoScreen(bot(), 60).isEmpty());
    }
}
