package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-44 (audit Gap C) — TileAreaGravityController resets the fallDistance
 * of entities inside its projection radius, and ONLY inside it.
 *
 * <p>Production:
 * {@link zmaster587.advancedRocketry.tile.multiblock.TileAreaGravityController#update}
 * — when {@code isRunning()}, it gathers every {@link
 * net.minecraft.entity.Entity} in an AABB grown by {@code getRadius()}
 * (= {@code radius + 10}; default radius 5 → 15) around the controller and
 * unconditionally sets {@code e.fallDistance = 0}. Player-visible: fall
 * damage is canceled for anything inside the gravity field.</p>
 *
 * <p><b>Why this supersedes the old @Ignore'd client test.</b> The original
 * {@code AreaGravityControllerResetsFallDistanceE2ETest} tp'd a grounded
 * player into the field and checked fallDistance dropped to 0 — but vanilla
 * MC resets a grounded entity's fallDistance every tick anyway, so the test
 * could not distinguish the controller from vanilla physics (hence the
 * @Ignore). This rewrite makes the reset DISCRIMINATING and moves it to the
 * server layer (the contract is purely server-side; no client needed):</p>
 *
 * <ul>
 *   <li>Spawn two armor stands with <b>no gravity</b> (so neither vanilla
 *       falling nor an onGround landing can touch their fallDistance) — one
 *       INSIDE the radius, one well OUTSIDE.</li>
 *   <li>Seed both with fallDistance = 7.5.</li>
 *   <li>Force-tick the powered, complete controller.</li>
 *   <li>The in-radius stand must read 0 (controller reset it); the
 *       out-of-radius stand must still read 7.5 (controller's spatial gate
 *       left it alone, and nothing else mutates a still no-gravity entity).</li>
 * </ul>
 *
 * <p>The contrast is what makes this a real contract pin: only the
 * controller's in-radius reset can produce 0-here / 7.5-there.</p>
 */
public class AreaGravityControllerFallDistanceResetTest extends AbstractSharedServerTest {

    private static final int CX = 5560;
    private static final int CY = 64;
    private static final int CZ = 5560;

    private static final Pattern FALL_DIST =
            Pattern.compile("\"fallDistance\":(-?[0-9.eE+-]+)");

    @Test
    public void controllerResetsFallDistanceInsideRadiusOnly() throws Exception {
        // 0) Hold the controller + out-of-radius chunks hot so spawned
        //    entities aren't lost to chunk-unload between probe calls
        //    (no player nearby in a headless server). CX=5560 → chunk 347;
        //    CX+40=5600 → chunk 350.
        ok("artest chunk forceload 0 " + (CX >> 4) + " " + (CZ >> 4));
        ok("artest chunk forceload 0 " + ((CX + 40) >> 4) + " " + (CZ >> 4));

        // 1) Build + validate the controller multiblock.
        ok("artest fixture multiblock gravity-controller 0 " + CX + " " + CY + " " + CZ);
        String complete = exec("artest machine try-complete 0 " + CX + " " + CY + " " + CZ);
        assertTrue("controller must validate: " + complete,
                complete.contains("\"isComplete\":true"));

        // 2) Power the plug below the controller + enable the machine.
        //    isRunning() = getMachineEnabled() && isStateActive(...); a freshly
        //    built controller is NOT enabled by default, so without this the
        //    update() entity loop never runs (this is exactly what the old
        //    grounded-bot test masked — vanilla reset the fallDistance whether
        //    or not the controller ran).
        ok("artest energy inject 0 " + CX + " " + (CY - 1) + " " + CZ + " 100000");
        ok("artest machine set-enabled 0 " + CX + " " + CY + " " + CZ + " true");

        // 3) Spawn two no-gravity armor stands: A inside the radius (at the
        //    controller), B far outside (getRadius() default = 15).
        int idIn = spawnPinnedStand(CX + 0.5, CY + 2.5, CZ + 0.5);
        int idOut = spawnPinnedStand(CX + 40.5, CY + 2.5, CZ + 0.5);

        // 4) Seed both fallDistances non-zero. (We do NOT read the in-radius
        //    one back as a "baseline" — the now-running controller resets it
        //    on its very next natural tick, so it would already read 0. That
        //    the controller does this is precisely the contract under test.)
        ok("artest entity set-fall-distance 0 " + idIn + " 7.5");
        ok("artest entity set-fall-distance 0 " + idOut + " 7.5");

        // 5) Drive the controller's update() loop deterministically too.
        ok("artest tile force-tick 0 " + CX + " " + CY + " " + CZ + " 5");

        // 6) Discriminating assertions. The out-of-radius reading still being
        //    7.5 doubles as proof that set-fall-distance worked at all.
        double in = readFallDistance(idIn);
        double out = readFallDistance(idOut);
        assertTrue("controller must reset fallDistance of the IN-radius entity "
                        + "to 0 (the 'no fall damage in gravity field' contract); "
                        + "in=" + in + " out=" + out,
                in < 0.5);
        assertTrue("controller must NOT touch the OUT-of-radius entity "
                        + "(spatial gate); it should still read ~7.5 — which also "
                        + "confirms set-fall-distance took effect; "
                        + "in=" + in + " out=" + out,
                out > 0.5);
    }

    private int spawnPinnedStand(double x, double y, double z) throws Exception {
        String resp = exec("artest entity spawn 0 " + x + " " + y + " " + z
                + " minecraft:armor_stand");
        assertTrue("entity spawn must succeed: " + resp, resp.contains("\"ok\":true"));
        Matcher m = Pattern.compile("\"entityId\":(-?\\d+)").matcher(resp);
        assertTrue("spawn must report entityId: " + resp, m.find());
        int id = Integer.parseInt(m.group(1));
        // Pin it in mid-air so neither falling nor landing mutates fallDistance.
        ok("artest entity set-no-gravity 0 " + id + " true");
        return id;
    }

    private double readFallDistance(int id) throws Exception {
        String resp = exec("artest entity info 0 " + id);
        Matcher m = FALL_DIST.matcher(resp);
        assertTrue("entity info must include fallDistance: " + resp, m.find());
        return Double.parseDouble(m.group(1));
    }

    private String exec(String cmd) throws Exception {
        return String.join("\n", client().execute(cmd));
    }

    private void ok(String cmd) throws Exception {
        String resp = exec(cmd);
        assertTrue("probe must succeed: cmd='" + cmd + "' resp=" + resp,
                resp.contains("\"ok\":true"));
    }
}
