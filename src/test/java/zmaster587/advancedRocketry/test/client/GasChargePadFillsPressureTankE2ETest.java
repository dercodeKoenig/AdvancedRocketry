package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-40 (audit Gap F.2) — TileGasChargePad refills player's chest
 * pressure tank.
 *
 * <p>Production:
 * {@link zmaster587.advancedRocketry.tile.atmosphere.TileGasChargePad#canPerformFunction}
 * scans the 1×2×1 AABB starting at the pad's pos for {@link
 * net.minecraft.entity.player.EntityPlayer}. For each player found it
 * reads the CHEST slot, checks whether the item is
 * {@link zmaster587.advancedRocketry.api.armor.IFillableArmor} or a
 * valid air container, and — if the pad's tank holds oxygen — drains the
 * tank by the missing-air amount and calls
 * {@code fillable.increment(stack, drained)}. Player-visible: suit air
 * meter rises on the HUD; chest tank gains fluid.</p>
 *
 * <p>Pinned: standing on a powered + oxygen-filled GasChargePad with a
 * partially-empty {@code itemSpaceSuit_Chest} (carrying an
 * {@link zmaster587.advancedRocketry.item.ItemPressureTank} component)
 * raises the chest's air reading over a wait window.</p>
 *
 * <p>Why testClient and not testServer: the pad's AABB scan requires a
 * real {@code EntityPlayer} in the world. {@code FakePlayer} server-side
 * is explicitly forbidden by project policy (TASK-10 marker). The
 * real-client bot IS a real {@code EntityPlayerMP} on the server side
 * of the harness, which satisfies the scan.</p>
 */
public class GasChargePadFillsPressureTankE2ETest extends AbstractClientE2ETest {

    private static final Pattern CHEST_AIR =
            Pattern.compile("\"chestAir\":(-?\\d+)");

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverClient().execute(cmd));
    }

    private int readChestAir() throws Exception {
        String resp = exec("artest player held-air-component-route");
        Matcher m = CHEST_AIR.matcher(resp);
        assertTrue("held-air response must include chestAir: " + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    /**
     * TASK-40 Gap F.2 — pad fills the suit's pressure-tank component
     * over time when the player stands on it.
     *
     * <p>The contract is the player-visible "stand on charger → suit
     * refills" interaction; the test pins the END STATE (air rises
     * over a 60-tick window) rather than a per-tick mB rate.</p>
     */
    @Test
    public void standingOnPoweredPadRefillsSuitAir() throws Exception {
        bot().waitForWorld();

        // Place pad at a known spot above ground.
        int px = 100, py = 65, pz = 100;
        String place = exec("artest place 0 " + px + " " + py + " " + pz
                + " advancedrocketry:oxygencharger");
        assertTrue("pad placement must succeed: " + place,
                place.contains("\"ok\":true"));

        // Fill pad tank with oxygen via Forge fluid capability.
        String inj = exec("artest fluid inject 0 " + px + " " + py + " " + pz
                + " oxygen 8000");
        assertTrue("fluid inject must succeed: " + inj,
                inj.contains("\"ok\":true"));

        // Equip the bot with a space suit whose pressure-tank component
        // starts mid-fill so there's room for the pad to fill more.
        // initialOxygen=1000 matches the TASK-24 pattern; 0 starting
        // values led readChestAir to return 0 here for a reason we
        // haven't traced (probe success != non-zero air on a fresh
        // ItemSpaceChest). Pin direction-of-change, not exact mB.
        // initialOxygen=500: half of the pressure tank's 1000 mB capacity.
        // Leaves headroom for the pad to actually add fluid; equip=1000
        // results in tank-already-full → pad's
        // canPerformFunction body short-circuits (amtFluid = 0).
        String equip = exec("artest player equip-space-chest 500");
        assertTrue("equip-space-chest must succeed: " + equip,
                equip.contains("\"ok\":true"));
        int airBefore = readChestAir();
        assertTrue("baseline chest air must be > 0 (probe filled 1000mB "
                        + "into pressure tank); actual=" + airBefore
                        + " equip=" + equip,
                airBefore > 0);

        // Teleport bot to standing on the pad (feet at py+1).
        exec("tp @p " + (px + 0.5) + " " + (py + 1) + " " + (pz + 0.5));
        bot().waitTicks(5);

        // 100 game ticks ≈ 5 seconds of natural pad ticking. The pad's
        // parent libVulpes class polls canPerformFunction on a cadence;
        // 100 ticks comfortably covers multiple fill cycles.
        bot().waitTicks(100);

        int airAfter = readChestAir();
        assertTrue("chest air must increase after standing on powered+"
                        + "filled GasChargePad; before=" + airBefore
                        + " after=" + airAfter,
                airAfter > airBefore);
    }
}
