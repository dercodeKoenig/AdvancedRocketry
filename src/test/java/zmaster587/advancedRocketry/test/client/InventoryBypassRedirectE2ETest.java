package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.client.ClientBot;
import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.client.ClientGuiTestSupport.screenOf;
import static zmaster587.advancedRocketry.test.client.ClientGuiTestSupport.waitForNoScreen;

/**
 * TASK-08-mixin Phase 3 (e2e closure) — live end-to-end pin for the
 * {@code MixinEntityPlayer(MP)InventoryAccess} {@code @Redirect}.
 *
 * <p>The unit-level pin
 * ({@code testUnit.RocketInventoryHelperRedirectTest}) covers the
 * boolean-logic surface of
 * {@link zmaster587.advancedRocketry.util.RocketInventoryHelper#shouldAllowContainerInteract},
 * but it can't prove the mixin's {@code @Redirect} actually intercepts
 * vanilla's {@code Container.canInteractWith} call inside
 * {@code EntityPlayerMP.onUpdate}. That requires a live
 * {@code EntityPlayer} with an open container GUI, ticked by the
 * dedicated server's normal loop — which is what {@code testClient}
 * provides over the FG6 client bridge.</p>
 *
 * <h2>What's pinned</h2>
 *
 * <p>The classic AR rocket-inventory use case: a player opens a container,
 * then the rocket (or the player) moves far past vanilla's 8-block
 * {@code BlockChest.canPlayerInteractWith} reach. Without the mixin
 * intercept, {@code EntityPlayerMP.onUpdate} would close the screen on the
 * next tick. With
 * {@code RocketInventoryHelper.canPlayerBypassInvChecks(player) = true},
 * the mixin redirects {@code canInteractWith} to return {@code true},
 * skipping the close-screen branch.</p>
 *
 * <p>Two phases in a single test:</p>
 * <ol>
 *   <li><b>Bypass on</b> — add the player to the bypass set, teleport
 *       far from the open container, wait, assert GUI is still open.</li>
 *   <li><b>Bypass off</b> — remove from bypass, wait, assert GUI closed
 *       (vanilla's distance check now wins).</li>
 * </ol>
 *
 * <p>A vanilla chest is used as the container so the redirect target
 * ({@code net.minecraft.inventory.Container.canInteractWith}) is the
 * exact vanilla signature the mixin pins.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true}; auto-skips on
 * headless CI.</p>
 */
public class InventoryBypassRedirectE2ETest extends AbstractClientE2ETest {

    // Far from other testClient suites (Guidance @ 8,64,8 / RocketBuilder
     // @ 200,64,200, etc.) so leftover state from earlier tests doesn't
    // collide.
    private static final int CHEST_X = -200;
    private static final int CHEST_Y = 64;
    private static final int CHEST_Z = -200;
    private static final String GUI_CHEST = "net.minecraft.client.gui.inventory.GuiChest";

    @Test
    public void mixinRedirectKeepsContainerOpenAcrossDistance() throws Exception {
        // Make sure no leftover bypass / inventory state from earlier tests
        // in this testClient suite interferes.
        serverClient().execute("artest player inv-bypass remove");
        serverClient().execute("clear @a");

        // Force-load the chunk before placing so the place op doesn't hit
        // an unloaded chunk.
        int cx = CHEST_X >> 4;
        int cz = CHEST_Z >> 4;
        for (int dxc = -1; dxc <= 1; dxc++) {
            for (int dzc = -1; dzc <= 1; dzc++) {
                serverClient().execute("artest chunk forceload 0 "
                        + (cx + dxc) + " " + (cz + dzc));
            }
        }

        // Place a vanilla chest at a known location.
        String place = String.join("\n", serverClient().execute(
                "artest place 0 " + CHEST_X + " " + CHEST_Y + " " + CHEST_Z
                        + " minecraft:chest"));
        assertTrue("chest place must succeed: " + place,
                place.contains("\"placed\":true"));

        // Stand directly above the chest, looking straight down (pitch=90)
        // so the right-click hits the chest's top face. Mirrors the
        // pose used by other testClient GUI suites.
        serverClient().execute("tp @a " + (CHEST_X + 0.5) + " " + (CHEST_Y + 2)
                + " " + (CHEST_Z + 0.5) + " 0 90");
        bot().waitTicks(40);

        // Open the chest container GUI SERVER-SIDE (mirrors
        // BlockChest.onBlockActivated → player.displayGUIChest) instead of via
        // bot.rightClickBlock. The right-click packet was dropped before the
        // chunk/player settled (the prior @Ignore reason — see bug ledger #6),
        // which is orthogonal to the mixin contract under test. The S2C
        // open-window packet makes the real client render GuiChest.
        String open = String.join("\n", serverClient().execute(
                "artest player open-chest 0 " + CHEST_X + " " + CHEST_Y + " " + CHEST_Z));
        assertTrue("server-side open-chest must succeed: " + open,
                open.contains("\"ok\":true"));
        String screen = waitForScreen(bot(), GUI_CHEST, 100);
        assertEquals("chest GUI must open after server-side displayGUIChest; "
                + "openResp=" + open, GUI_CHEST, screen);

        // PHASE 1 — bypass on: GUI must survive a long-distance teleport.
        String addResp = String.join("\n", serverClient().execute(
                "artest player inv-bypass add"));
        assertFalse("inv-bypass add must succeed: " + addResp,
                addResp.contains("\"error\""));
        assertTrue("inv-bypass add must report inBypass:true: " + addResp,
                addResp.contains("\"inBypass\":true"));

        // Teleport ~200 blocks away — well past vanilla's 8-block
        // canPlayerInteractWith reach for BlockChest.
        serverClient().execute("tp @a " + (CHEST_X + 200) + " " + (CHEST_Y + 1)
                + " " + (CHEST_Z + 200) + " 0 0");
        bot().waitTicks(40);

        JsonObject afterTpWithBypass = bot().reportState();
        String screenAfterTp = screenOf(afterTpWithBypass);
        // Diagnostic: re-check bypass status post-teleport so a failure can
        // distinguish "bypass dropped from set" from "mixin redirect didn't
        // fire". The bypass map uses WeakReferences; if some prior test
        // cleared inventoryCheckPlayerBypassMap or the player reference
        // was reset by a reconnect, status would flip back to false here.
        String statusAfterTp = String.join("\n", serverClient().execute(
                "artest player inv-bypass status"));
        assertEquals("with inv-bypass active, the chest GUI must remain open "
                + "across a 200-block teleport (mixin redirect should force "
                + "canInteractWith → true on every EntityPlayerMP.onUpdate "
                + "tick); reportState=" + afterTpWithBypass
                + " bypassStatus=" + statusAfterTp,
                GUI_CHEST, screenAfterTp);

        // PHASE 2 — bypass off: GUI must close on the next tick.
        String removeResp = String.join("\n", serverClient().execute(
                "artest player inv-bypass remove"));
        assertFalse("inv-bypass remove must succeed: " + removeResp,
                removeResp.contains("\"error\""));
        assertTrue("inv-bypass remove must report inBypass:false: " + removeResp,
                removeResp.contains("\"inBypass\":false"));

        // Without bypass, vanilla's canInteractWith returns false → the
        // next EntityPlayerMP.onUpdate tick runs closeScreen → the S2C
        // packet closes the GUI on the client.
        String finalScreen = waitForNoScreen(bot(), 200);
        assertEquals("after removing inv-bypass, vanilla distance check "
                + "must close the chest GUI; final screen=" + finalScreen,
                "", finalScreen);
    }

    /** Poll the client for up to {@code maxTicks} until the given screen class
     *  is showing; returns the last-seen screen (empty string if none). */
    private static String waitForScreen(ClientBot bot, String wantScreen, int maxTicks)
            throws IOException {
        String screen = screenOf(bot.reportState());
        for (int i = 0; i < maxTicks && !wantScreen.equals(screen); i++) {
            bot.waitTicks(2);
            screen = screenOf(bot.reportState());
        }
        return screen;
    }
}
