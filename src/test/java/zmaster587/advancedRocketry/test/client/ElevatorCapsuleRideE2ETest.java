package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-30 Gap 3 Phase 1 — EntityElevatorCapsule mount / dismount
 * contracts.
 *
 * <p>Mirrors the methodological pattern from
 * {@link HovercraftRideE2ETest}: the testClient bot has no exposed
 * "right-click on entity" interaction, so player→capsule mounting
 * is driven through server-side probe verbs
 * ({@code /artest player mount-entity}, {@code dismount}) which call
 * {@code startRiding} / {@code dismountRidingEntity} respectively.
 * The observable result is identical to the production mount path
 * triggered from {@code EntityElevatorCapsule.onEntityUpdate} when
 * the capsule is in motion and a player enters its AABB
 * ({@code line 230-234, 313-317} call the same
 * {@code ent.startRiding(this)}).</p>
 *
 * <p>Pinned contracts:</p>
 * <ol>
 *   <li>{@code player.startRiding(capsule)} succeeds + observable
 *       ridingEntity matches the capsule's id and class.</li>
 *   <li>{@code dismountRidingEntity()} clears the riding relationship.</li>
 * </ol>
 *
 * <p>Deferred (heavy fixture cost — see TASK-30 Phase deferrals):
 * the full ascent/descent loop with stand-time accrual requires a
 * built and powered TileSpaceElevator multiblock tethered to a
 * peer in a different dimension on a station in geostationary
 * orbit, plus a properly anchored {@code dstTilePos} that makes
 * {@code TileSpaceElevator.isDestinationValid} return true. That
 * lives behind the same gating as the existing
 * {@code SpaceElevatorMultiblockTest} but layered with station
 * fixtures we do not yet have.</p>
 */
public class ElevatorCapsuleRideE2ETest extends AbstractClientE2ETest {

    private static final Pattern ENTITY_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern RIDING_ID = Pattern.compile("\"ridingEntityId(?:Now)?\":(-?\\d+)");

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverClient().execute(cmd));
    }

    /** Spawns a fresh elevator capsule via the entity-spawn probe.
     *  The capsule uses the {@code (World)} ctor, so the reflective
     *  spawn helper falls through to the no-coord branch and calls
     *  setPosition manually. */
    private int spawnCapsuleAt(double x, double y, double z) throws Exception {
        String resp = exec("artest entity spawn 0 " + x + " " + y + " " + z
                + " advancedrocketry:ARSpaceElevatorCapsule");
        assertTrue("capsule spawn must succeed: " + resp,
                resp.contains("\"ok\":true") && resp.contains("\"spawned\":true"));
        Matcher m = ENTITY_ID.matcher(resp);
        assertTrue("spawn response must include entityId: " + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    @Test
    public void playerMountsElevatorCapsuleViaStartRiding() throws Exception {
        // Pad + tp pattern is from HovercraftRideE2ETest — keeps the
        // bot in the same chunk as the spawned entity so id resolution
        // through world.getEntityByID stays in-tick.
        exec("artest place 0 108 78 8 minecraft:stone");
        exec("tp @a 108.5 79 8.5");
        bot().waitTicks(5);

        int capsuleId = spawnCapsuleAt(108.5, 79, 10.5);

        String mount = exec("artest player mount-entity " + capsuleId);
        assertTrue("mount-entity probe must succeed: " + mount,
                mount.contains("\"ok\":true"));
        assertTrue("mount-entity must report mounted:true: " + mount,
                mount.contains("\"mounted\":true"));

        String riding = exec("artest player riding-entity");
        assertEquals("after mount, riding-entity probe must report the capsule's id",
                capsuleId, extract(riding, RIDING_ID));
        assertTrue("riding entity class must be EntityElevatorCapsule: " + riding,
                riding.contains("EntityElevatorCapsule"));

        // Cleanup — dismount so subsequent tests in the same JVM start fresh.
        exec("artest player dismount");
    }

    @Test
    public void playerDismountClearsRidingEntity() throws Exception {
        exec("artest place 0 128 78 8 minecraft:stone");
        exec("tp @a 128.5 79 8.5");
        bot().waitTicks(5);

        int capsuleId = spawnCapsuleAt(128.5, 79, 10.5);
        exec("artest player mount-entity " + capsuleId);

        String dismount = exec("artest player dismount");
        assertTrue("dismount probe must succeed: " + dismount,
                dismount.contains("\"ok\":true"));
        assertEquals("dismount must report ridingEntityIdNow:-1",
                -1, extract(dismount, RIDING_ID));

        String riding = exec("artest player riding-entity");
        assertEquals("after dismount, player must report no riding entity (-1)",
                -1, extract(riding, RIDING_ID));
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }
}
