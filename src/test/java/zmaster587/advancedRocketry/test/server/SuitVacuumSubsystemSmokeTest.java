package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.13 deepen — suit + vacuum-damage subsystem smoke.
 *
 * <p>The full "player in vacuum wearing suit takes no damage" loop needs a
 * real {@link net.minecraft.entity.player.EntityPlayer} on a vacuum dim —
 * that's the {@code OxygenSuitClientStateE2ETest} client-E2E scenario which
 * runs only with {@code -PclientHarness=true}. Here we lock down the
 * server-side preconditions of that loop:</p>
 * <ol>
 *   <li>all four suit pieces (helmet / chest / leggings / boots) are
 *       registered in the item registry;</li>
 *   <li>each suit piece exposes the {@code IProtectiveArmor} capability — the
 *       runtime hook the atmosphere-damage event handler consults to decide
 *       whether to skip damage;</li>
 *   <li>the {@code spacebreathing} enchantment is registered (alternate path
 *       to avoid vacuum damage);</li>
 *   <li>setting Earth's atmosphere density to 0 yields a non-breathable
 *       atmosphere through the production probe — the precondition for any
 *       vacuum-damage gameplay to fire.</li>
 * </ol>
 */
public class SuitVacuumSubsystemSmokeTest extends AbstractHeadlessServerTest {

    @Test
    public void suitItemsAndEnchantAreWiredUp() throws Exception {
        // 1. All four suit pieces registered + expose IProtectiveArmor capability.
        for (String id : new String[]{
                "advancedrocketry:spaceHelmet",
                "advancedrocketry:spaceChestplate",
                "advancedrocketry:spaceLeggings",
                "advancedrocketry:spaceBoots"
        }) {
            String resp = String.join("\n", client().execute(
                    "artest item check " + id + " protective-armor"));
            assertTrue(id + " not registered: " + resp,
                    resp.contains("\"registered\":true"));
            assertTrue(id + " missing IProtectiveArmor capability: " + resp,
                    resp.contains("\"hasCapability\":true"));
        }

        // 2. SpaceBreathing enchantment registered.
        String ench = String.join("\n", client().execute(
                "artest enchant check advancedrocketry:spacebreathing"));
        assertTrue("spacebreathing enchant missing: " + ench,
                ench.contains("\"registered\":true"));

        // 3. Vacuum precondition: Earth → density 0 → non-breathable.
        // Snapshot original so we restore it after.
        String planet = String.join("\n", client().execute("artest planet info 0"));
        java.util.regex.Matcher dm = java.util.regex.Pattern
                .compile("\"atmosphereDensity\":(-?\\d+)").matcher(planet);
        int originalDensity = dm.find() ? Integer.parseInt(dm.group(1)) : 100;

        try {
            String setVac = String.join("\n", client().execute(
                    "artest atmosphere set-density 0 0"));
            assertTrue("set-density 0 failed: " + setVac,
                    setVac.contains("\"ok\":true"));

            String atm = String.join("\n", client().execute(
                    "artest atmosphere get 0 0 70 0"));
            assertTrue("density=0 must yield non-breathable atmosphere: " + atm,
                    atm.contains("\"breathable\":false"));
        } finally {
            client().execute("artest atmosphere set-density 0 " + originalDensity);
        }
    }
}
