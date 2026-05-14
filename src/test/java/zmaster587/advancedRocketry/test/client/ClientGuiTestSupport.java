package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.client.ClientBot;
import com.google.gson.JsonObject;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;

import java.io.IOException;

/**
 * Shared helpers for the client GUI E2E tests.
 *
 * <p>The right-click → server {@code onBlockActivated} → {@code openGui} →
 * client {@code displayGuiScreen} round-trip is driven over a socket bridge to
 * a real Minecraft client. A single {@code rightClickBlock} call is
 * occasionally a no-op (the interaction lands a tick before the chunk/player is
 * fully settled, or the packet is otherwise dropped), and no amount of polling
 * recovers a click that never registered — so {@link #openGuiByRightClick}
 * <em>re-issues</em> the right-click between polls.</p>
 */
final class ClientGuiTestSupport {

    private ClientGuiTestSupport() {
    }

    /** {@code report_state} reports the open screen's class under the {@code screen} key. */
    static String screenOf(JsonObject state) {
        return state != null && state.has("screen") ? state.get("screen").getAsString() : "";
    }

    /**
     * Right-clicks the block at (x,y,z) until a GUI screen opens, re-issuing the
     * click each attempt. Returns the open screen's class, or {@code ""} if no
     * GUI opened across all attempts.
     */
    static String openGuiByRightClick(ClientBot bot, int x, int y, int z) throws IOException {
        for (int attempt = 0; attempt < 6; attempt++) {
            String already = screenOf(bot.reportState());
            if (!already.isEmpty()) {
                return already;
            }
            bot.rightClickBlock(x, y, z, EnumFacing.UP, EnumHand.MAIN_HAND);
            // Poll a short window for this click to take effect before retrying.
            for (int waited = 0; waited < 60; waited += 10) {
                bot.waitTicks(10);
                String screen = screenOf(bot.reportState());
                if (!screen.isEmpty()) {
                    return screen;
                }
            }
        }
        return "";
    }

    /**
     * Polls {@code report_state} until no GUI screen is open or the budget is
     * exhausted. Returns the final screen class ({@code ""} when released).
     */
    static String waitForNoScreen(ClientBot bot, int maxTicks) throws IOException {
        for (int waited = 0; waited < maxTicks; waited += 5) {
            String screen = screenOf(bot.reportState());
            if (screen.isEmpty()) {
                return "";
            }
            bot.waitTicks(5);
        }
        return screenOf(bot.reportState());
    }
}
