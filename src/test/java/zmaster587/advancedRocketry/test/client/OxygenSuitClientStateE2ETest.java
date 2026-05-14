package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.20 — client-side propagation of AR's vacuum / oxygen subsystem.
 *
 * <p>Flips Earth (dim 0) to a vacuum via {@code /artest atmosphere set-density
 * 0 0}, disables natural regeneration so health is monotonic, drops the harness
 * player to survival, and then observes — through the client bridge — that
 * {@code bot.reportState().health} <b>drops</b>. That confirms the server-side
 * {@code AtmosphereVacuum} damage tick ({@code attackEntityFrom}) reaches and is
 * visible on the real Minecraft client, end to end.</p>
 *
 * <p>The suit-protection path ({@code AtmosphereNeedsSuit.isImmune}) is
 * validated server-side by {@code server/SuitVacuumSubsystemSmokeTest} (all four
 * suit pieces registered + exposing {@code IProtectiveArmor}). A client-side
 * "suited player survives" variant additionally needs an O2-filled chest piece
 * — that lives in a multi-component sub-inventory with no test fixture yet — so
 * it is deferred. The harness server runs creative ({@code gamemode=1}), under
 * which {@code isImmune} short-circuits regardless of suit, so this test
 * explicitly drops to survival for the damage window and restores creative
 * afterwards.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true}; auto-skips on headless CI.</p>
 */
public class OxygenSuitClientStateE2ETest extends AbstractClientE2ETest {

    private static final Pattern DENSITY = Pattern.compile("\"atmosphereDensity\":(-?\\d+)");

    @Test
    public void vacuumDamageReachesTheClient() throws Exception {
        String planet = String.join("\n", serverClient().execute("artest planet info 0"));
        Matcher dm = DENSITY.matcher(planet);
        int originalDensity = dm.find() ? Integer.parseInt(dm.group(1)) : 100;

        try {
            serverClient().execute("gamerule naturalRegeneration false");
            serverClient().execute("gamemode survival @a");
            // Stand the player on a known solid block so leaving creative flight
            // doesn't inflict fall damage that would masquerade as vacuum damage.
            serverClient().execute("artest place 0 8 78 8 minecraft:stone");
            serverClient().execute("tp @a 8.5 79 8.5");
            bot().waitTicks(10);

            double healthStart = health(bot().reportState());
            assertTrue("player should start at full health, got " + healthStart,
                    healthStart >= 20.0);

            String setVac = String.join("\n", serverClient().execute(
                    "artest atmosphere set-density 0 0"));
            assertTrue("set-density 0 failed: " + setVac, setVac.contains("\"ok\":true"));

            // AtmosphereVacuum damages every 10 world ticks. Poll so the test
            // stops as soon as damage registers — robust against slow ticking
            // under parallel forks, and well clear of lethal exposure.
            double current = healthStart;
            for (int waited = 0; waited < 200 && current >= healthStart; waited += 20) {
                bot().waitTicks(20);
                current = health(bot().reportState());
            }
            assertTrue("vacuum damage never reached the client: health held at "
                            + current + " (started " + healthStart + ")",
                    current < healthStart);
        } finally {
            try {
                serverClient().execute("artest atmosphere set-density 0 "
                        + Math.max(originalDensity, 1));
            } catch (Exception ignored) {
            }
            try {
                serverClient().execute("gamemode creative @a");
            } catch (Exception ignored) {
            }
            try {
                serverClient().execute("gamerule naturalRegeneration true");
            } catch (Exception ignored) {
            }
        }
    }

    private static double health(JsonObject state) {
        return state.has("health") ? state.get("health").getAsDouble() : -1.0;
    }
}
