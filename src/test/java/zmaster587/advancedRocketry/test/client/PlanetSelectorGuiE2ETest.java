package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.client.ClientGuiTestSupport.findButtonId;
import static zmaster587.advancedRocketry.test.client.ClientGuiTestSupport.openGuiByRightClick;

/**
 * SMART §7.20 — deep client E2E for the planet selector tile.
 *
 * <p>Right-clicks {@code advancedrocketry:planetSelector} to open the libVulpes
 * modular GUI, introspects its buttons via {@code report_buttons}, then clicks a
 * planet button <em>by its stable mod-assigned id</em> ({@code GuiButton.id} ==
 * the planet's dimension id; see {@code ModulePlanetSelector}). Clicking a
 * planet fires {@code TilePlanetSelector.onSelected} → {@code PacketMachine} →
 * server {@code useNetworkData} → {@code dimCache}, which the
 * {@code /artest selector info} probe then confirms.</p>
 *
 * <p>This drives the whole client→server selection round-trip rather than just
 * asserting the GUI opened. Static control buttons (Up / Select / PlanetList)
 * and star buttons carry ids outside {@code [0, STAR_ID_OFFSET)}, so a planet
 * is picked by id range.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true}; auto-skips on headless CI.</p>
 */
public class PlanetSelectorGuiE2ETest extends AbstractClientE2ETest {

    private static final int X = 8, Y = 64, Z = 8;
    /** {@code zmaster587.advancedRocketry.api.Constants.STAR_ID_OFFSET}. */
    private static final int STAR_ID_OFFSET = 10000;

    @Test
    public void selectingPlanetUpdatesServerSelection() throws Exception {
        String place = String.join("\n", serverClient().execute(
                "artest place 0 " + X + " " + Y + " " + Z + " advancedrocketry:planetSelector"));
        assertTrue("could not place planetSelector: " + place, place.contains("\"placed\":true"));

        // FG6 launcher gives the player a random "Player###" name — target via @a.
        serverClient().execute("tp @a " + (X + 0.5) + " " + (Y + 2) + " " + (Z + 0.5) + " 0 90");
        bot().waitTicks(40);

        String screen = openGuiByRightClick(bot(), X, Y, Z);
        assertTrue("expected the planet selector GUI to open, got: " + screen,
                screen.startsWith("zmaster587.libVulpes.inventory.GuiModular"));

        JsonObject buttons = bot().reportButtons();
        int planetId = findButtonId(buttons, 0, STAR_ID_OFFSET);
        assertTrue("no clickable planet button in selector GUI: " + buttons,
                planetId != Integer.MIN_VALUE);

        bot().clickButtonById(planetId);
        bot().waitTicks(20);

        String selectorInfo = String.join("\n", serverClient().execute(
                "artest selector info 0 " + X + " " + Y + " " + Z));
        assertTrue("clicking planet button " + planetId
                        + " did not register a selection server-side: " + selectorInfo,
                selectorInfo.contains("\"hasSelection\":true"));
        assertTrue("selection did not resolve to a planet: " + selectorInfo,
                selectorInfo.contains("\"selectedDim\":"));
    }
}
