package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-24 Phase 1 — {@code ItemSpaceChest} (suit-family chest) chest
 * sub-inventory drain in vacuum.
 *
 * <p>Closes the deferred {@code ItemSpaceChest} branch from
 * {@link ItemSpaceArmorUseFluidE2ETest}'s docstring: that test exercises
 * the cheaper enchanted-vanilla-armor route which drains a static "air"
 * NBT key. The suit-family {@code itemSpaceSuit_Chest} chestplate
 * carries an {@code ItemPressureTank} component in its embedded fluid-
 * tank inventory; drain on that route walks the components and
 * decrements each component's FluidStack via the Forge
 * {@code IFluidHandlerItem} capability.</p>
 *
 * <p>Production chain pinned (per {@link
 * zmaster587.advancedRocketry.armor.ItemSpaceChest#decrementAir}):</p>
 *
 * <ol>
 *   <li>vacuum atmosphere → {@code AtmosphereNeedsSuit.onTick}</li>
 *   <li>{@code isImmune} returns true only if helm + legs + feet + chest
 *       all {@code protectsFrom}; chest's {@code protectsFrom} calls
 *       {@code decrementAir(stack, 1)}.</li>
 *   <li>{@code decrementAir} loads {@code EmbeddedInventory} from NBT,
 *       walks components, drains 1 mB from the oxygen-charged pressure
 *       tank's FluidStack, persists.</li>
 *   <li>{@code held-air} probe reads back via
 *       {@code ItemAirUtils.getAirRemaining} which delegates to
 *       {@code ItemSpaceChest.getAirRemaining} — sums FluidStack
 *       amounts across components.</li>
 * </ol>
 *
 * <p>Setup uses the new {@code artest player equip-space-chest
 * <initialOxygen>} probe (equips full 4-piece suit + embeds an O2-filled
 * pressure tank in chest slot 0). The probe is testClient-only because
 * vacuum drain requires a real player tick loop.</p>
 */
public class ItemSpaceChestSubInventoryDrainE2ETest extends AbstractClientE2ETest {

    private static final Pattern DENSITY = Pattern.compile("\"atmosphereDensity\":(-?\\d+)");
    private static final Pattern CHEST_AIR = Pattern.compile("\"chestAir\":(-?\\d+)");

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverClient().execute(cmd));
    }

    private int readChestAir() throws Exception {
        // For ItemSpaceChest (capability route), use the component-aware
        // probe — the static "air" NBT route used by /artest player
        // held-air returns 0 for this item because ItemSpaceChest stores
        // its O2 buffer inside an embedded inventory's pressure-tank
        // FluidStacks rather than as a top-level NBT key.
        String resp = exec("artest player held-air-component-route");
        Matcher m = CHEST_AIR.matcher(resp);
        assertTrue("held-air-component-route response must include chestAir: " + resp,
                m.find());
        return Integer.parseInt(m.group(1));
    }

    private void resetPlayer() throws Exception {
        exec("artest place 0 8 78 8 minecraft:stone");
        exec("tp @a 8.5 79 8.5");
        exec("artest player clear-armor");
        exec("gamerule naturalRegeneration false");
        exec("gamemode survival @a");
        bot().waitTicks(10);
    }

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

    /** Vacuum + full suit (chest carries oxygen-charged pressure tank in
     *  slot 0) → atmosphere onTick fires every 10 game ticks; each fire
     *  drains 1 mB of oxygen from the pressure tank's FluidStack via
     *  {@code ItemSpaceChest.decrementAir}. Player takes no damage —
     *  {@code isImmune} returns true while the chain holds. */
    @Test
    public void vacuumDrainsOxygenFromChestSubInventoryTank() throws Exception {
        int originalDensity = snapshotDensity();
        try {
            resetPlayer();
            String equip = exec("artest player equip-space-chest 1000");
            assertTrue("equip-space-chest must succeed: " + equip,
                    equip.contains("\"ok\":true"));
            assertTrue("equip-space-chest must report oxygen filled in tank: " + equip,
                    equip.contains("\"tankFilled\":1000"));
            assertEquals("baseline chestAir read via ItemAirUtils → "
                            + "ItemSpaceChest.getAirRemaining → sum of "
                            + "FluidStack amounts must equal 1000",
                    1000, readChestAir());

            double healthStart = health(bot().reportState());
            String setVac = exec("artest atmosphere set-density 0 0");
            assertTrue("set-density 0 failed: " + setVac,
                    setVac.contains("\"ok\":true"));

            // 80 game ticks ≈ 8 atmosphere ticks (every 10), each
            // decrement the pressure-tank FluidStack by 1.
            bot().waitTicks(80);

            int chestAirAfter = readChestAir();
            assertTrue("chest air must decrease through the CHEST sub-inventory "
                            + "route in vacuum; before=1000 after=" + chestAirAfter,
                    chestAirAfter < 1000);
            double healthAfter = health(bot().reportState());
            assertTrue("full suit must keep isImmune=true while tank has oxygen; "
                            + "healthStart=" + healthStart
                            + " healthAfter=" + healthAfter,
                    healthAfter >= healthStart);
        } finally {
            restoreDim(originalDensity);
        }
    }

    /** Counter-test: same suit + tank, breathable atmosphere → the
     *  {@code AtmosphereType.onTick} for the breathable type is a no-op,
     *  so {@code protectsFrom} → {@code decrementAir} is never called.
     *  Tank's oxygen stays at its initial value. */
    @Test
    public void breathableAtmosphereDoesNotDrainChestTank() throws Exception {
        int originalDensity = snapshotDensity();
        try {
            resetPlayer();
            exec("artest atmosphere set-density 0 100");
            String equip = exec("artest player equip-space-chest 1000");
            assertTrue("equip-space-chest must succeed: " + equip,
                    equip.contains("\"ok\":true"));
            assertEquals("baseline chestAir", 1000, readChestAir());

            bot().waitTicks(80);

            int chestAirAfter = readChestAir();
            assertEquals("chest air must hold steady when atmosphere doesn't drain; "
                            + "before=1000 after=" + chestAirAfter,
                    1000, chestAirAfter);
        } finally {
            restoreDim(originalDensity);
        }
    }

    /** A nearly-drained chest tank transitions the player from suit-
     *  protected to suit-fails-isImmune: once the tank's last mB is
     *  drained, {@code decrementAir(stack, 1)} returns 0 →
     *  {@code chest.protectsFromSubstance} returns false →
     *  {@code isImmune} returns false → vacuum damage applies. Pins the
     *  "drained chest no longer protects" transition. */
    @Test
    public void drainedChestTankTransitionsToVacuumDamage() throws Exception {
        int originalDensity = snapshotDensity();
        try {
            resetPlayer();
            // Seed the tank with just a handful of oxygen — small enough
            // that the 80-tick window below drains it fully and then
            // overshoots into the no-protection branch.
            String equip = exec("artest player equip-space-chest 3");
            assertTrue("equip-space-chest with low oxygen must succeed: " + equip,
                    equip.contains("\"ok\":true"));
            assertEquals("baseline chestAir = 3", 3, readChestAir());

            double healthStart = health(bot().reportState());
            assertTrue("player must start at full health: " + healthStart,
                    healthStart >= 20.0);

            exec("artest atmosphere set-density 0 0");

            // 3 atmosphere ticks drain the tank to 0; subsequent ticks
            // (within the 200-tick budget) start firing the vacuum-damage
            // path. Poll until damage observed OR budget elapsed.
            double current = healthStart;
            for (int waited = 0; waited < 200 && current >= healthStart; waited += 20) {
                bot().waitTicks(20);
                current = health(bot().reportState());
            }
            int chestAirAfter = readChestAir();
            assertEquals("tank must be fully drained after the wait window; "
                            + "chestAir=" + chestAirAfter,
                    0, chestAirAfter);
            assertTrue("vacuum damage must apply once the tank is drained; "
                            + "health held at " + current + " (started "
                            + healthStart + ")",
                    current < healthStart);
        } finally {
            restoreDim(originalDensity);
        }
    }

    private static double health(JsonObject state) {
        return state.has("health") ? state.get("health").getAsDouble() : -1.0;
    }
}
