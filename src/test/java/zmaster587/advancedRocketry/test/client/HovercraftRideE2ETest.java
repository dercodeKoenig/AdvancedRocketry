package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-20 — Hovercraft mount / dismount / throttle / motion
 * contracts.
 *
 * <p>Pre-this-test the hovercraft had:</p>
 * <ul>
 *   <li>{@code HovercraftEntitySmokeTest} — spawn + tick alive
 *       (server tier, shallow).</li>
 *   <li>{@code ItemHovercraftSpawnE2ETest} — right-click ground
 *       spawns entity (testClient, item-side).</li>
 * </ul>
 *
 * <p>This test pins the <b>ride contracts</b>:</p>
 * <ol>
 *   <li>Player mounts via {@code startRiding} → ridingEntity matches.</li>
 *   <li>Player dismounts → ridingEntity is null.</li>
 *   <li>Player input {@code moveForward > 0} → hovercraft accelerates
 *       forward.</li>
 *   <li>No input → hovercraft hovers (lateral position stable).</li>
 * </ol>
 *
 * <p><b>Bot-driven vs probe-driven inputs</b>: AR's testClient
 * {@code ClientBot} surface doesn't include "right-click on entity",
 * "sneak", or "forward movement input" — only block right-clicks
 * and GUI clicks are exposed. To pin hovercraft ride behaviour we
 * drive mount / dismount / moveForward via new server-side probe
 * verbs ({@code /artest player mount-entity}, {@code dismount},
 * {@code set-move-forward}). The observable result is identical:
 * the EntityHoverCraft sees the same {@code player.moveForward}
 * field that {@code getPassengerMovingForward()} reads from.</p>
 *
 * <p><b>No fuel test</b>: the EntityHoverCraft class has zero fuel
 * or energy logic — onUpdate only reads player input and applies
 * acceleration. The audit's "fuel drain" gap was based on assumed
 * (not actual) fuel mechanics. Documented in this class's javadoc
 * so a future addition of fuel logic must add a corresponding
 * contract pin.</p>
 */
public class HovercraftRideE2ETest extends AbstractClientE2ETest {

    private static final Pattern ENTITY_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern RIDING_ID = Pattern.compile("\"ridingEntityId(?:Now)?\":(-?\\d+)");
    private static final Pattern POS_X = Pattern.compile("\"posX\":(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern POS_Z = Pattern.compile("\"posZ\":(-?\\d+(?:\\.\\d+)?)");

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverClient().execute(cmd));
    }

    /** Spawns a fresh hovercraft entity near the bot's pad coords and
     *  returns its entity id. */
    private int spawnHovercraftAt(double x, double y, double z) throws Exception {
        String resp = exec("artest entity spawn 0 " + x + " " + y + " " + z
                + " advancedrocketry:ARHoverCraft");
        assertTrue("hovercraft spawn must succeed: " + resp,
                resp.contains("\"ok\":true") && resp.contains("\"spawned\":true"));
        Matcher m = ENTITY_ID.matcher(resp);
        assertTrue("spawn response must include entityId: " + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    @Test
    public void playerMountsHovercraftViaStartRiding() throws Exception {
        // Spawn craft adjacent to a known stone pad so the bot is
        // close enough to be in the same world tick + chunk.
        exec("artest place 0 8 78 8 minecraft:stone");
        exec("tp @a 8.5 79 8.5");
        bot().waitTicks(5);

        int craftId = spawnHovercraftAt(8.5, 79, 10.5);

        String mount = exec("artest player mount-entity " + craftId);
        assertTrue("mount-entity probe must succeed: " + mount,
                mount.contains("\"ok\":true"));
        assertTrue("mount-entity must report mounted:true: " + mount,
                mount.contains("\"mounted\":true"));

        String riding = exec("artest player riding-entity");
        assertEquals("after mount, riding-entity probe must report the craft's id",
                craftId, extract(riding, RIDING_ID));
        assertTrue("riding entity class must be EntityHoverCraft: " + riding,
                riding.contains("EntityHoverCraft"));
    }

    @Test
    public void playerDismountClearsRidingEntity() throws Exception {
        exec("artest place 0 28 78 8 minecraft:stone");
        exec("tp @a 28.5 79 8.5");
        bot().waitTicks(5);

        int craftId = spawnHovercraftAt(28.5, 79, 10.5);
        exec("artest player mount-entity " + craftId);

        String dismount = exec("artest player dismount");
        assertTrue("dismount probe must succeed: " + dismount,
                dismount.contains("\"ok\":true"));
        assertEquals("dismount must report ridingEntityIdNow:-1",
                -1, extract(dismount, RIDING_ID));

        String riding = exec("artest player riding-entity");
        assertEquals("after dismount, player must report no riding entity (-1)",
                -1, extract(riding, RIDING_ID));
    }

    @Test
    public void forwardThrottleMovesHovercraftLaterally() throws Exception {
        // Place the bot at a stable position with the hovercraft right
        // next to it. The craft's onUpdate reads player.moveForward
        // each tick — setting it via probe drives acceleration in the
        // direction of the craft's yaw.
        exec("artest place 0 48 78 8 minecraft:stone");
        exec("tp @a 48.5 79 8.5");
        bot().waitTicks(5);

        int craftId = spawnHovercraftAt(48.5, 79, 10.5);
        exec("artest player mount-entity " + craftId);
        // Reset any latent moveForward from prior input.
        exec("artest player set-move-forward 0");
        bot().waitTicks(2);

        // Snapshot baseline lateral position.
        String preInfo = exec("artest entity info 0 " + craftId);
        double xBefore = extractDouble(preInfo, POS_X);
        double zBefore = extractDouble(preInfo, POS_Z);

        // Drive forward — the combined probe re-applies moveForward
        // inline before each onUpdate so the bot client's CPacketInput
        // doesn't reset the field between iterations.
        String drive = exec("artest player drive-ridden-entity 1 40");
        assertTrue("drive-ridden-entity must succeed: " + drive,
                drive.contains("\"ok\":true"));

        String postInfo = exec("artest entity info 0 " + craftId);
        double xAfter = extractDouble(postInfo, POS_X);
        double zAfter = extractDouble(postInfo, POS_Z);

        double dx = xAfter - xBefore;
        double dz = zAfter - zBefore;
        double lateralDist = Math.sqrt(dx * dx + dz * dz);
        assertTrue("throttled hovercraft must move at least 0.1 blocks "
                        + "laterally over 40 ticks (got " + lateralDist + "): "
                        + " before=(" + xBefore + "," + zBefore + ")"
                        + " after=(" + xAfter + "," + zAfter + ")",
                lateralDist > 0.1);

        // Cleanup — release throttle so subsequent tests start fresh.
        exec("artest player set-move-forward 0");
        exec("artest player dismount");
    }

    @Test
    public void unmountedHovercraftDoesNotMoveLaterally() throws Exception {
        // Counter-test: an unmounted hovercraft has no passenger →
        // getPassengerMovingForward returns 0 → no lateral acceleration.
        // The Y position may drift (gravity/hover), but X+Z should
        // stay stable.
        exec("artest place 0 68 78 8 minecraft:stone");
        exec("tp @a 68.5 79 8.5");
        bot().waitTicks(5);

        int craftId = spawnHovercraftAt(68.5, 79, 10.5);
        // Confirm unmounted state.
        String riding = exec("artest player riding-entity");
        assertNotEquals("baseline: player must NOT be riding the craft "
                        + "(spawn doesn't auto-mount)",
                craftId, extract(riding, RIDING_ID));

        String preInfo = exec("artest entity info 0 " + craftId);
        double xBefore = extractDouble(preInfo, POS_X);
        double zBefore = extractDouble(preInfo, POS_Z);

        // Drive ticks WITHOUT mounting.
        exec("artest entity tick 0 " + craftId + " 40");

        String postInfo = exec("artest entity info 0 " + craftId);
        double xAfter = extractDouble(postInfo, POS_X);
        double zAfter = extractDouble(postInfo, POS_Z);
        double lateralDrift = Math.sqrt(Math.pow(xAfter - xBefore, 2)
                + Math.pow(zAfter - zBefore, 2));
        // Tolerance: the craft's motion damping (×0.9 per tick) lets
        // any latent motion bleed off within ~30 ticks. Lateral drift
        // over 40 ticks should be near zero.
        assertTrue("unmounted hovercraft must hover in place laterally; "
                        + "drift=" + lateralDrift + " before=(" + xBefore + ","
                        + zBefore + ") after=(" + xAfter + "," + zAfter + ")",
                lateralDrift < 0.5);
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }

    private static double extractDouble(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Double.parseDouble(m.group(1));
    }
}
