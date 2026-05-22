package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10b Phase 7 — player-visible side of
 * {@link zmaster587.advancedRocketry.item.ItemSealDetector#onItemUse}.
 *
 * <p>The server-tier
 * {@link zmaster587.advancedRocketry.test.server.SealDetectorDispatchTest}
 * already pins the dispatch matrix (which branch fires per fixture) by
 * driving {@code SealableBlockHandler} predicates directly through a
 * mirroring probe. What it does NOT cover is the player-visible side:
 * does {@code onItemUse} actually deliver the matching
 * {@code msg.sealdetector.&lt;branch&gt;} chat message to the player's
 * client.</p>
 *
 * <p>This e2e pins exactly that — a real {@code EntityPlayerMP} holds an
 * {@code ItemSealDetector}, {@code onItemUse} runs against a placed
 * fixture (driven through {@code /artest player try-seal-detect} so we
 * don't have to wrangle the player into a precise right-click pose),
 * and the outbound {@code SPacketChat} packet is captured by a Netty
 * chat-tap so we can read the translation key the production code
 * dispatched.</p>
 *
 * <p>Fixtures mirror {@code SealDetectorDispatchTest} (stone /
 * cobblestone → "sealed", air / leaves / sand → "notsealmat",
 * stone_slab → "other"). The {@code notsealblock}, {@code notfullblock}
 * and {@code fluid} branches are out of scope here for the same reason
 * they are out of scope on the server tier — they need deterministic
 * fixtures (config-driven banned block, fluid registry) that aren't
 * available without extra plumbing.</p>
 *
 * <p>Cross-pin: every player-message branch is asserted to equal the
 * {@code seal-detector check} probe's branch field at the same
 * coordinate. Any future drift between production
 * ({@code ItemSealDetector.onItemUse}) and the mirroring probe
 * ({@code TestProbeCommand.handleSealDetector}) makes the cross-pin
 * fail loud — that's the whole point of running them side by side.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true}; auto-skips on
 * headless CI.</p>
 */
public class ItemSealDetectorPlayerMessagesE2ETest extends AbstractClientE2ETest {

    private static final int DIM = 0;
    private static final int Y = 72;
    private static final int Z = 300;

    // Distinct from SealDetectorDispatchTest (200..260 / y=80 / z=200)
    // and InventoryBypassRedirectE2ETest (-200..-200) to avoid fixture
    // clashes if testClient runs all suites in one JVM.
    private static final int X_STONE       = 300;
    private static final int X_COBBLESTONE = 310;
    private static final int X_AIR         = 320;
    private static final int X_LEAVES      = 330;
    private static final int X_SAND        = 340;
    private static final int X_SLAB        = 350;

    private static final Pattern KEY    = Pattern.compile("\"key\":\"([^\"]+)\"");
    private static final Pattern BRANCH = Pattern.compile("\"branch\":\"([^\"]+)\"");

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

    private void place(int x, String blockId) throws Exception {
        forceLoadAround(x, Z);
        String resp = String.join("\n", serverClient().execute(
                "artest place " + DIM + " " + x + " " + Y + " " + Z + " " + blockId));
        // Air placement is a no-op for /artest place but force-loads the
        // chunk — accept either "placed":true or a "placed":false echoing
        // that the block was already there.
        assertFalse("place must not error at " + x + "," + Y + "," + Z
                        + " with " + blockId + "; resp=" + resp,
                resp.contains("\"error\""));
    }

    private String fieldOf(Pattern p, String src, String label) {
        Matcher m = p.matcher(src);
        assertTrue("expected " + label + " field in: " + src, m.find());
        return m.group(1);
    }

    /** Calls the chat-tap-aware seal-detector probe at (x, Y, Z) and
     *  asserts the captured chat key is {@code msg.sealdetector.<expected>}.
     *  Also cross-pins the result against the server-tier seal-detector
     *  probe so any drift between production dispatch and the mirroring
     *  probe surfaces immediately. */
    private void assertSealDetectorBranch(int x, String fixtureBlock, String expected) throws Exception {
        place(x, fixtureBlock);

        serverClient().execute("artest player chat-clear");
        String tryResp = String.join("\n", serverClient().execute(
                "artest player try-seal-detect " + DIM + " " + x + " " + Y + " " + Z));
        assertFalse("try-seal-detect must not error at " + x + " (" + fixtureBlock
                + "); resp=" + tryResp, tryResp.contains("\"error\""));
        String capturedKey = fieldOf(KEY, tryResp, "key");
        assertEquals("ItemSealDetector.onItemUse on " + fixtureBlock
                        + " at " + x + "," + Y + "," + Z + " must dispatch "
                        + "msg.sealdetector." + expected + "; resp=" + tryResp,
                "msg.sealdetector." + expected, capturedKey);
        String capturedBranch = fieldOf(BRANCH, tryResp, "branch");
        assertEquals("try-seal-detect branch field must equal i18n suffix",
                expected, capturedBranch);

        // Cross-pin against the server-tier dispatch mirror.
        String checkResp = String.join("\n", serverClient().execute(
                "artest seal-detector check " + DIM + " " + x + " " + Y + " " + Z));
        String mirrorBranch = fieldOf(BRANCH, checkResp, "branch");
        assertEquals("production dispatch and server-tier mirror must agree on "
                        + "branch for " + fixtureBlock + " at " + x + "," + Y + "," + Z
                        + "; player-msg branch=" + capturedBranch
                        + " mirror branch=" + mirrorBranch,
                capturedBranch, mirrorBranch);
    }

    // ───────────────────── sealed branch ──────────────────────────────────

    /** Solid ROCK material full-block → "sealed". */
    @Test
    public void stoneFixtureDispatchesSealedMessageToPlayer() throws Exception {
        assertSealDetectorBranch(X_STONE, "minecraft:stone", "sealed");
    }

    /** Pins that "sealed" isn't pinned to the singular stone block —
     *  any solid full-block ROCK material should reach the player as
     *  "sealed", per SealableBlockHandler.isBlockSealed's material gate. */
    @Test
    public void cobblestoneFixtureDispatchesSealedMessageToPlayer() throws Exception {
        assertSealDetectorBranch(X_COBBLESTONE, "minecraft:cobblestone", "sealed");
    }

    // ───────────────────── notsealmat branch ──────────────────────────────

    /** Material.AIR is on the default materialBanList → "notsealmat". */
    @Test
    public void airFixtureDispatchesNotSealMatMessageToPlayer() throws Exception {
        assertSealDetectorBranch(X_AIR, "minecraft:air", "notsealmat");
    }

    /** Material.LEAVES is on the default materialBanList — multi-material
     *  ban-list pin (not just AIR). */
    @Test
    public void leavesFixtureDispatchesNotSealMatMessageToPlayer() throws Exception {
        assertSealDetectorBranch(X_LEAVES, "minecraft:leaves", "notsealmat");
    }

    /** Material.SAND is on the default materialBanList — silent removal
     *  from the ban-list would let sand seal rooms (player-visible
     *  regression). */
    @Test
    public void sandFixtureDispatchesNotSealMatMessageToPlayer() throws Exception {
        assertSealDetectorBranch(X_SAND, "minecraft:sand", "notsealmat");
    }

    // ───────────────────── other branch ───────────────────────────────────

    /** Stone slab: ROCK material (not banned), but half-block bounds →
     *  isFullBlock=false → dispatch falls through to "other" (after
     *  short-circuiting on the non-IFluidBlock check). */
    @Test
    public void stoneSlabFixtureDispatchesOtherMessageToPlayer() throws Exception {
        assertSealDetectorBranch(X_SLAB, "minecraft:stone_slab", "other");
    }

    // ───────────────────── chat-tap shape ─────────────────────────────────

    /** chat-clear must drain the deque so a follow-up last-chat reports
     *  no captured key — guards tests against cross-contamination from
     *  prior chat traffic (login messages, /tp output, etc.). */
    @Test
    public void chatClearEmptiesTheCaptureDeque() throws Exception {
        serverClient().execute("artest player chat-clear");
        String resp = String.join("\n", serverClient().execute(
                "artest player last-chat"));
        assertTrue("after chat-clear, last-chat must report size=0; resp=" + resp,
                resp.contains("\"size\":0"));
        assertTrue("after chat-clear, last-chat must report key=null; resp=" + resp,
                resp.contains("\"key\":null"));
    }

    /** Probe must surface an error JSON for missing args, matching the
     *  rest of the /artest player surface. Catches accidental signature
     *  changes that would silently no-op. */
    @Test
    public void trySealDetectErrorsWithoutCoordinates() throws Exception {
        String resp = String.join("\n", serverClient().execute(
                "artest player try-seal-detect"));
        assertNotNull(resp);
        assertTrue("missing args must surface an error; resp=" + resp,
                resp.contains("\"error\""));
    }
}
