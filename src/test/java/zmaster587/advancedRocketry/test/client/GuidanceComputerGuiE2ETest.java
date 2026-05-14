package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.client.ClientGuiTestSupport.findSlotWithItem;
import static zmaster587.advancedRocketry.test.client.ClientGuiTestSupport.openGuiByRightClick;

/**
 * SMART §7.20 — deep client E2E for the guidance computer block.
 *
 * <p>Right-clicks {@code advancedrocketry:guidanceComputer} to open its
 * libVulpes modular GUI, then drives a real inventory interaction through the
 * client: a planet-id chip handed to the player is shift-clicked
 * ({@code ClickType.QUICK_MOVE}) out of the player inventory and into the
 * guidance computer's own slot. {@code report_slots} confirms the chip crossed
 * from a {@code playerSlot} into a machine slot — i.e. the click drove
 * {@code Container.transferStackInSlot} on the server and the result synced
 * back.</p>
 *
 * <p>Slots are addressed by the container slot number reported by
 * {@code report_slots}, not by guessed coordinates.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true}; auto-skips on headless CI.</p>
 */
public class GuidanceComputerGuiE2ETest extends AbstractClientE2ETest {

    private static final int X = 8, Y = 64, Z = 8;
    private static final String CHIP = "advancedrocketry:planetidchip";

    @Test
    public void shiftClickingChipMovesItIntoTheGuidanceComputer() throws Exception {
        String place = String.join("\n", serverClient().execute(
                "artest place 0 " + X + " " + Y + " " + Z + " advancedrocketry:guidanceComputer"));
        assertTrue("could not place guidanceComputer: " + place, place.contains("\"placed\":true"));

        // FG6 launcher gives the player a random "Player###" name — target via @a.
        serverClient().execute("tp @a " + (X + 0.5) + " " + (Y + 2) + " " + (Z + 0.5) + " 0 90");
        serverClient().execute("give @a " + CHIP + " 1");
        bot().waitTicks(40);

        String screen = openGuiByRightClick(bot(), X, Y, Z);
        assertTrue("expected the guidance computer GUI to open, got: " + screen,
                screen.startsWith("zmaster587.libVulpes.inventory.GuiModular"));

        JsonObject before = bot().reportSlots();
        int chipSlot = findSlotWithItem(before, CHIP, true);
        assertTrue("chip not found in the player inventory portion of the GUI: " + before,
                chipSlot != -1);

        // Shift-click the chip — should quick-move it into the machine's slot.
        bot().clickSlot(chipSlot, 0, "QUICK_MOVE");
        bot().waitTicks(10);

        JsonObject after = bot().reportSlots();
        int machineSlot = findSlotWithItem(after, CHIP, false);
        assertTrue("shift-click did not move the chip into a guidance computer slot: " + after,
                machineSlot != -1);
        assertTrue("chip still left behind in the player inventory: " + after,
                findSlotWithItem(after, CHIP, true) == -1);
    }
}
