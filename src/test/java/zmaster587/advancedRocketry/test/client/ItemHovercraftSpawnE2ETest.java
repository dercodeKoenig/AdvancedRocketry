package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10b Phase 7 — player-visible side of
 * {@link zmaster587.advancedRocketry.item.ItemHovercraft#onItemRightClick}.
 *
 * <p>Contract: right-click while looking at a block within ~5 blocks
 * spawns an EntityHoverCraft at the hit position and (in survival)
 * consumes one item from the stack. PASS if nothing is in front of the
 * player; FAIL if there is no room to spawn at the hit pos.</p>
 *
 * <p>Fixture: place a stone block at (X, Y, Z), teleport player two
 * blocks above looking straight down. The 5-block ray-trace from the
 * eye hits the stone top face — entity spawns at the hit pos.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true}; auto-skips on
 * headless CI.</p>
 */
public class ItemHovercraftSpawnE2ETest extends AbstractClientE2ETest {

    private static final int DIM = 0;
    // Distinct fixture column from SealDetector (300..350) and the
    // existing inventory-bypass test (-200..-200) so multiple tests
    // can share one testClient JVM without colliding.
    private static final int X = 400;
    // High above natural overworld terrain so the EntityHoverCraft's
    // 2.5×1×2.5 bounding box (shrunk to -0.1) has guaranteed empty
    // neighbours when checked at hitVec.y + small offset — terrain at
    // y≈72 caused intermittent FAIL from incidental grass/leaves
    // intersecting the spawn box.
    private static final int Y_BLOCK = 150;
    private static final int Z = 300;

    private void forceLoadAround(int x, int z) throws Exception {
        int cx = x >> 4;
        int cz = z >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                serverClient().execute("artest chunk forceload " + DIM
                        + " " + (cx + dx) + " " + (cz + dz));
            }
        }
    }

    /** Right-click looking down at a stone block must spawn exactly one
     *  EntityHoverCraft and (in survival) consume the held stack. */
    @Test
    public void rightClickAtTargetBlockSpawnsHovercraftAndConsumesStack() throws Exception {
        forceLoadAround(X, Z);

        // Place fixture block under the player's eye line.
        String placeResp = String.join("\n", serverClient().execute(
                "artest place " + DIM + " " + X + " " + Y_BLOCK + " " + Z + " minecraft:stone"));
        assertFalse("place must not error; resp=" + placeResp,
                placeResp.contains("\"error\""));

        // Player 2 blocks above, looking straight down. The 5-block ray
        // from the eye (~(Y_BLOCK+2)+1.62) hits the stone top face.
        double px = X + 0.5;
        double py = Y_BLOCK + 2;
        double pz = Z + 0.5;
        String resp = String.join("\n", serverClient().execute(
                "artest player try-hovercraft " + DIM + " "
                        + px + " " + py + " " + pz + " 0 90"));

        assertFalse("try-hovercraft must not error; resp=" + resp,
                resp.contains("\"error\""));
        assertTrue("right-click on a target block must SUCCEED; resp=" + resp,
                resp.contains("\"result\":\"SUCCESS\""));
        assertTrue("exactly one EntityHoverCraft must have spawned; resp=" + resp,
                resp.contains("\"entityDelta\":1"));
        assertTrue("survival player must have stack consumed (0 left); resp=" + resp,
                resp.contains("\"heldAfter\":0"));
        assertTrue("probe must confirm survival gamemode for the consume pin; resp=" + resp,
                resp.contains("\"creative\":false"));
    }

    /** Right-click into open air (no block within 5 blocks of the eye)
     *  must PASS rather than SUCCESS — no entity spawned, stack
     *  preserved. Pins the empty-ray-trace branch. */
    @Test
    public void rightClickIntoEmptyAirReturnsPassWithoutSpawn() throws Exception {
        forceLoadAround(X + 20, Z);
// Player at y=200 looking up — nothing within 5 blocks.
        double px = X + 20 + 0.5;
        double py = 200;
        double pz = Z + 0.5;
        String resp = String.join("\n", serverClient().execute(
                "artest player try-hovercraft " + DIM + " "
                        + px + " " + py + " " + pz + " 0 -90"));

        assertFalse("try-hovercraft must not error; resp=" + resp,
                resp.contains("\"error\""));
        assertTrue("empty ray-trace must report PASS; resp=" + resp,
                resp.contains("\"result\":\"PASS\""));
        assertTrue("no entity must have spawned; resp=" + resp,
                resp.contains("\"entityDelta\":0"));
        assertTrue("stack must NOT be consumed on PASS; resp=" + resp,
                resp.contains("\"heldAfter\":1"));
    }

    /** Probe must surface an error JSON for missing args. */
    @Test
    public void tryHovercraftErrorsWithoutFullArgs() throws Exception {
        String resp = String.join("\n", serverClient().execute(
                "artest player try-hovercraft 0 100"));
        assertTrue("missing args must surface an error; resp=" + resp,
                resp.contains("\"error\""));
    }
}
