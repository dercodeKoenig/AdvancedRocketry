package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.google.gson.JsonObject;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.20 — minimal client GUI smoke for the planet selector tile.
 *
 * <p>Sequence:</p>
 * <ol>
 *   <li>Server places {@code advancedrocketry:planetSelector} at a fixed pos.</li>
 *   <li>Client teleports above the tile (the {@code rightClickBlock} bridge
 *       call uses a raycast from the player's eyes — the block must be in
 *       reach).</li>
 *   <li>{@code bot.rightClickBlock(...)} opens the modular GUI server-side.</li>
 *   <li>{@code bot.reportState()} returns the current screen's class — assert
 *       a GUI did open.</li>
 *   <li>{@code bot.closeScreen()} cleans up; the screen handle is released.</li>
 * </ol>
 *
 * <p>Does NOT exercise an actual planet click — that requires empirically
 * derived pixel coordinates inside {@code ModulePlanetSelector}'s rendered
 * grid which vary with module sizing. The full selection cycle (click →
 * server state change) is covered headless by
 * {@code server/SelectorServerSmokeTest}.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true} via
 * {@link AbstractClientE2ETest}. Default-skips on headless CI.</p>
 */
public class PlanetSelectorGuiE2ETest extends AbstractClientE2ETest {

    private static final int X = 8, Y = 64, Z = 8;

    @Test
    public void rightClickingTileOpensSelectorGui() throws Exception {
        // 1. Server places the block within the spawn-chunk so the client's
        //    default join position is in reach.
        String place = String.join("\n", serverClient().execute(
                "artest place 0 " + X + " " + Y + " " + Z + " advancedrocketry:planetSelector"));
        assertTrue("could not place planetSelector: " + place,
                place.contains("\"placed\":true"));

        // 2. Teleport client player above the block, looking down (pitch=90°).
        serverClient().execute("tp ForgeTestClient " + (X + 0.5) + " " + (Y + 2) + " " + (Z + 0.5)
                + " 0 90");
        bot().waitTicks(20);

        // 3. Right-click the block face from above.
        bot().rightClickBlock(X, Y, Z, EnumFacing.UP, EnumHand.MAIN_HAND);
        bot().waitTicks(10);

        // 4. reportState should now show a non-empty currentScreen — the
        //    libVulpes modular GUI hosting ModulePlanetSelector.
        JsonObject state = bot().reportState();
        String screen = state.has("currentScreen")
                ? state.get("currentScreen").getAsString()
                : "";
        assertTrue("expected a GUI to open after right-click, got screen=" + screen
                        + " full=" + state,
                screen != null
                        && !screen.isEmpty()
                        && !"null".equalsIgnoreCase(screen));

        // 5. Close and verify release.
        bot().closeScreen();
        bot().waitTicks(5);
        JsonObject after = bot().reportState();
        String afterScreen = after.has("currentScreen")
                ? after.get("currentScreen").getAsString()
                : "";
        assertTrue("closeScreen didn't release the GUI: " + afterScreen,
                afterScreen == null || afterScreen.isEmpty()
                        || "null".equalsIgnoreCase(afterScreen));
    }
}
