package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Ignore;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-40 (audit Gap C) — TileAreaGravityController resets fallDistance
 * for entities inside its projection radius.
 *
 * <p>Production:
 * {@link zmaster587.advancedRocketry.tile.multiblock.TileAreaGravityController#update}
 * (lines 184-226). When {@code isRunning() == true} the controller walks
 * every {@link net.minecraft.entity.Entity} in a cube of side
 * {@code 2*getRadius()} around its pos and unconditionally sets
 * {@code e.fallDistance = 0}. Side-selector-state-driven motion
 * modifications happen only if a direction is enabled; the fallDistance
 * reset fires regardless. Player-visible: fall damage gets canceled
 * when the player walks into the projection while falling.</p>
 *
 * <p>Pinned: a formed + powered AreaGravityController with a player in
 * its projection area resets that player's server-side {@code fallDistance}
 * from a non-zero baseline back to 0 over a wait window.</p>
 *
 * <p>This is a strict subset of the contract proposed in the
 * 2026-05-27 audit ("player falls at target gravity inside projection"
 * — band pin on fall-step distance). The fallDistance reset is the
 * cheapest player-visible side-effect — pins the same gate (isRunning
 * + in-radius + entity match) without needing side-selector state setup
 * or sustained-motion sampling.</p>
 */
public class AreaGravityControllerResetsFallDistanceE2ETest extends AbstractClientE2ETest {

    private static final int CX = 5500;
    private static final int CY = 70;
    private static final int CZ = 5500;

    private static final Pattern FALL_DIST =
            Pattern.compile("\"fallDistance\":(-?[0-9.eE+-]+)");

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverClient().execute(cmd));
    }

    private void ok(String cmd) throws Exception {
        String resp = exec(cmd);
        assertTrue("probe must succeed: cmd='" + cmd + "' resp=" + resp,
                resp.contains("\"ok\":true"));
    }

    private double readFallDistance() throws Exception {
        String resp = exec("artest player get-fall-distance");
        Matcher m = FALL_DIST.matcher(resp);
        assertTrue("get-fall-distance must include fallDistance: " + resp, m.find());
        return Double.parseDouble(m.group(1));
    }

    /**
     * TASK-40 Gap C — powered controller resets bot's fallDistance
     * each tick once isRunning() is true. Asserts:
     *
     * <ol>
     *   <li>fallDistance can be set non-zero via the test probe
     *       (baseline sanity).</li>
     *   <li>After teleporting into the projection radius and ticking,
     *       fallDistance is 0 (the controller reset it).</li>
     * </ol>
     */
    /**
     * NOTE: currently @Ignore — test design needs revisit.
     *
     * <p>A grounded bot has its {@code fallDistance} forcibly reset to 0
     * by vanilla MC every server tick via {@code EntityLivingBase.updateFallState}
     * — the controller's reset is indistinguishable from the vanilla
     * reset. Result: the contract is technically pinned (reset is 0
     * after force-tick) but trivially so — the test cannot distinguish
     * controller behaviour from vanilla physics.</p>
     *
     * <p>To un-ignore: rewrite around a falling {@link
     * net.minecraft.entity.item.EntityItem} (predictable physics, no
     * onGround/motionY=0 vanilla-reset path) — spawn at projection
     * centre + elevated y, bot.waitTicks to accumulate non-zero
     * fallDistance, force-tick controller, assert reset. Needs
     * extending the existing {@code entity info} probe to surface
     * {@code fallDistance}, OR a dedicated
     * {@code entity fall-distance <dim> <entityId>} verb.</p>
     */
    @Ignore("TASK-40b Gap C — grounded bot fallDistance reset is "
            + "indistinguishable from vanilla physics reset. Re-design "
            + "around a falling EntityItem; see class docstring.")
    @Test
    public void poweredControllerResetsFallDistanceOfNearbyPlayer() throws Exception {
        bot().waitForWorld();

        // 1) Build a complete area-gravity-controller multiblock.
        String fixture = exec("artest fixture multiblock gravity-controller 0 "
                + CX + " " + CY + " " + CZ);
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        // Validate structure so isComplete() flips.
        String tryComplete = exec("artest machine try-complete 0 "
                + CX + " " + CY + " " + CZ);
        assertTrue("controller must validate: " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));

        // 2) Inject power into the plug at (cx, cy-1, cz). 100k RF is
        // ~1000 ticks at typical powerPerTick — comfortably covers the
        // ticks our wait window needs.
        String energy = exec("artest energy inject 0 "
                + CX + " " + (CY - 1) + " " + CZ + " 100000");
        assertTrue("energy inject must succeed: " + energy,
                energy.contains("\"ok\":true"));

        // 3) Teleport bot into the projection radius. Default radius
        // is 5 → getRadius()=15 → AABB is (cx-15..cx+15, cy-15..cy+15,
        // cz-15..cz+15). Spawn bot 3 blocks above controller, well inside.
        exec("tp @p " + (CX + 0.5) + " " + (CY + 3) + " " + (CZ + 0.5));
        bot().waitTicks(10);

        // Sanity gate: set fallDistance baseline > 0 via the probe.
        String set = exec("artest player set-fall-distance 7.5");
        assertTrue("set-fall-distance must succeed: " + set,
                set.contains("\"ok\":true"));
        double baseline = readFallDistance();
        assertTrue("baseline fallDistance must be > 0 (probe sanity); "
                        + "actual=" + baseline + " set=" + set,
                baseline > 0.5);

        // 4) Set fallDistance baseline, then deterministically tick the
        // controller via `tile force-tick`. Earlier natural-tick approach
        // (bot().waitTicks) was flaky — isRunning may not be true within
        // the wait window. force-tick bypasses the natural-tick scheduler
        // and synchronously invokes the controller's update() N times.
        double afterSet = readFallDistance();
        assertTrue("baseline fallDistance must be > 0 (probe sanity); "
                        + "actual=" + afterSet,
                afterSet > 0.5);

        // Drive controller update() directly. Each call enters
        // `if (isRunning()) { ... e.fallDistance = 0; ... }` if power
        // and complete-structure gates pass.
        ok("artest tile force-tick 0 " + CX + " " + CY + " " + CZ + " 5");

        double afterTick = readFallDistance();
        assertTrue("controller must reset bot fallDistance to 0 when "
                        + "bot is in projection radius (the player-"
                        + "visible 'no fall damage in gravity field' "
                        + "contract); afterSet=" + afterSet
                        + " afterTick=" + afterTick,
                afterTick < 0.5);
    }
}
