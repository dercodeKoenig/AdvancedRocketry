package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
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

        // 4) Let natural ticks run — controller.update() fires every
        // server tick and resets fallDistance for in-AABB entities.
        bot().waitTicks(30);

        // Re-set in case any natural fall reset it between assertion
        // and wait; the contract is "controller reset it to 0".
        // Actually do the inverse: set non-zero, then wait the smallest
        // window in which AT LEAST ONE controller tick must have fired.
        exec("artest player set-fall-distance 5.5");
        // Sanity: probe set 5.5 succeeded.
        double afterSet = readFallDistance();
        assertTrue("post-probe-set fallDistance must be > 0 to be "
                        + "meaningful; actual=" + afterSet,
                afterSet >= 5.0);

        // Wait 5 ticks — 5 ticks of controller update() is enough.
        // 1 tick = 1 update() call = 1 fallDistance reset on every entity
        // in range.
        bot().waitTicks(5);

        double afterTick = readFallDistance();
        assertTrue("controller must reset bot fallDistance to 0 when "
                        + "bot is in projection radius (the player-"
                        + "visible 'no fall damage in gravity field' "
                        + "contract); afterSet=" + afterSet
                        + " afterTick=" + afterTick,
                afterTick < 0.5);
    }
}
