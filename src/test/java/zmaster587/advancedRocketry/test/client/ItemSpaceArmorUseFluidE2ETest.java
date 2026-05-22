package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10b Phase 7 — SpaceArmor "use fluid" / air-drain behavioural pin.
 *
 * <p>Closes the deferred suite from {@link OxygenSuitClientStateE2ETest}'s
 * docstring: that test pins the vacuum-damage path on a bare-skinned
 * player, but defers the suited-survives + air-decrements path because
 * the {@code ItemSpaceChest} fixture would need a populated embedded
 * fluid-tank inventory.</p>
 *
 * <p>The {@code AtmosphereNeedsSuit.protectsFrom} method (line 49) has
 * two routes through which a CHEST stack can prove protection:</p>
 *
 * <ol>
 *   <li><b>Air-enchanted vanilla armor route</b> —
 *       {@code ItemAirUtils.isStackValidAirContainer} (enchant-tag
 *       check) gates entry, then
 *       {@code ItemAirUtils.ItemAirWrapper.protectsFromSubstance(stack,
 *       commit=true)} fires {@code decrementAir(stack, 1)} on the
 *       static "air" NBT key. This is the path this test exercises —
 *       it's the cheapest fixture (vanilla iron armor + ench tag +
 *       NBT pre-seed via the {@code equip-airsuit} probe).</li>
 *   <li><b>CapabilitySpaceArmor route</b> — used by the
 *       {@code itemSpaceSuit_*} family; chest drain reads from
 *       embedded oxygen-fluid components instead of the static "air"
 *       NBT. Fixture for that route is heavier and lives in a
 *       follow-up.</li>
 * </ol>
 *
 * <p>Pinned behaviours:</p>
 * <ul>
 *   <li>{@link #suitedPlayerInVacuumLosesChestAirOverTime} — drain
 *       fires when atmosphere ticks in vacuum: chest "air" NBT drops
 *       AND the player takes no damage (suit absorbs).</li>
 *   <li>{@link #suitedPlayerInBreathableDimDoesNotLoseChestAir} —
 *       counter: same suit, no drain in breathable atmosphere
 *       (atmosphere onTick is a no-op for non-vacuum types).</li>
 *   <li>{@link #unsuitedPlayerInVacuumLosesNoAirAndTakesDamage} —
 *       cross-check against the existing
 *       {@link OxygenSuitClientStateE2ETest} damage pin via a fresh
 *       run: no chest = no decrement, and the vacuum-damage path
 *       still applies.</li>
 * </ul>
 *
 * <p>Pattern adopted from {@link OxygenSuitClientStateE2ETest}: flip
 * overworld atmosphereDensity to 0 in-place rather than staging XML
 * planet defs, drop to survival for the damage window, restore
 * afterwards. The harness server defaults to creative, under which
 * {@code AtmosphereNeedsSuit.isImmune} short-circuits regardless of
 * suit.</p>
 */
public class ItemSpaceArmorUseFluidE2ETest extends AbstractClientE2ETest {

    private static final Pattern DENSITY = Pattern.compile("\"atmosphereDensity\":(-?\\d+)");
    private static final Pattern CHEST_AIR = Pattern.compile("\"chestAir\":(-?\\d+)");

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverClient().execute(cmd));
    }

    private int readChestAir() throws Exception {
        String resp = exec("artest player held-air");
        Matcher m = CHEST_AIR.matcher(resp);
        assertTrue("held-air response must include chestAir: " + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    /** Reset the player to a known location + bare-skinned state so each
     *  test has a clean baseline regardless of order. */
    private void resetPlayer() throws Exception {
        exec("artest place 0 8 78 8 minecraft:stone");
        exec("tp @a 8.5 79 8.5");
        exec("artest player clear-armor");
        exec("gamerule naturalRegeneration false");
        exec("gamemode survival @a");
        bot().waitTicks(10);
    }

    /** Reads overworld baseline density so {@link #restoreDim(int)} can
     *  return it after the test mutates it to 0 (vacuum). */
    private int snapshotDensity() throws Exception {
        String planet = exec("artest planet info 0");
        Matcher dm = DENSITY.matcher(planet);
        return dm.find() ? Integer.parseInt(dm.group(1)) : 100;
    }

    private void restoreDim(int originalDensity) {
        try {
            exec("artest atmosphere set-density 0 " + Math.max(originalDensity, 1));
        } catch (Exception ignored) {
        }
        try {
            exec("gamemode creative @a");
        } catch (Exception ignored) {
        }
        try {
            exec("gamerule naturalRegeneration true");
        } catch (Exception ignored) {
        }
    }

    /** Vacuum + full enchanted suit → atmosphere onTick fires (every
     *  10 game ticks), each fire decrements the chest "air" NBT by 1.
     *  Health holds because the four enchanted slots make
     *  {@code isImmune} return true (no {@code attackEntityFrom}). */
    @Test
    public void suitedPlayerInVacuumLosesChestAirOverTime() throws Exception {
        int originalDensity = snapshotDensity();
        try {
            resetPlayer();
            String equip = exec("artest player equip-airsuit 1000");
            assertTrue("equip-airsuit must succeed: " + equip,
                    equip.contains("\"ok\":true"));
            assertEquals("baseline chest air before vacuum exposure",
                    1000, readChestAir());

            double healthStart = health(bot().reportState());
            String setVac = exec("artest atmosphere set-density 0 0");
            assertTrue("set-density 0 failed: " + setVac,
                    setVac.contains("\"ok\":true"));

            // 80 game ticks ≈ 8 atmosphere ticks (every 10), each
            // decrements chest air by 1 via ItemAirWrapper.
            bot().waitTicks(80);

            int chestAirAfter = readChestAir();
            assertTrue("chest air must decrease in vacuum with suit; "
                            + "before=1000 after=" + chestAirAfter,
                    chestAirAfter < 1000);
            // Health must hold — suit absorbed; if isImmune returned
            // false the vacuum-damage tick would have shaved hearts.
            double healthAfter = health(bot().reportState());
            assertTrue("suited player must not take vacuum damage; "
                            + "healthStart=" + healthStart
                            + " healthAfter=" + healthAfter,
                    healthAfter >= healthStart);
        } finally {
            restoreDim(originalDensity);
        }
    }

    /** Counter: same suit in breathable atmosphere → chest air stays
     *  at the configured initial value. The breathable atmosphere
     *  type's {@code onTick} is a no-op (only {@code AtmosphereVacuum}
     *  / pressure variants drive drain) so the protectsFrom branch
     *  never gets evaluated and no decrement fires. */
    @Test
    public void suitedPlayerInBreathableDimDoesNotLoseChestAir() throws Exception {
        int originalDensity = snapshotDensity();
        try {
            resetPlayer();
            // Make sure overworld is breathable (default density,
            // but in case a prior test left it modified).
            exec("artest atmosphere set-density 0 100");
            String equip = exec("artest player equip-airsuit 1000");
            assertTrue("equip-airsuit must succeed: " + equip,
                    equip.contains("\"ok\":true"));
            assertEquals("baseline chest air", 1000, readChestAir());

            bot().waitTicks(80);

            int chestAirAfter = readChestAir();
            assertEquals("chest air must be unchanged in breathable atmosphere; "
                            + "before=1000 after=" + chestAirAfter,
                    1000, chestAirAfter);
        } finally {
            restoreDim(originalDensity);
        }
    }

    /** Cross-check against {@link OxygenSuitClientStateE2ETest}: a
     *  bare-skinned player in vacuum loses HEALTH (the no-suit branch
     *  of {@code AtmosphereVacuum.onTick}) and the {@code chestAir}
     *  probe reports -1 (no chest stack). Pins the contract that
     *  drain is gated on having a chest with a valid air container —
     *  no chest, no decrement, just damage. */
    @Test
    public void unsuitedPlayerInVacuumLosesNoAirAndTakesDamage() throws Exception {
        int originalDensity = snapshotDensity();
        try {
            resetPlayer();
            // Sanity: no chest after clear-armor, held-air reports -1.
            assertEquals("bare-skinned baseline chest air must be -1",
                    -1, readChestAir());

            double healthStart = health(bot().reportState());
            assertTrue("player must start at full health, got " + healthStart,
                    healthStart >= 20.0);

            exec("artest atmosphere set-density 0 0");

            double current = healthStart;
            for (int waited = 0; waited < 200 && current >= healthStart; waited += 20) {
                bot().waitTicks(20);
                current = health(bot().reportState());
            }
            assertTrue("vacuum damage must apply to bare-skinned player; "
                            + "health held at " + current
                            + " (started " + healthStart + ")",
                    current < healthStart);
            assertEquals("chestAir must remain -1 throughout — no chest = no decrement path",
                    -1, readChestAir());
        } finally {
            restoreDim(originalDensity);
        }
    }

    private static double health(JsonObject state) {
        return state.has("health") ? state.get("health").getAsDouble() : -1.0;
    }
}
