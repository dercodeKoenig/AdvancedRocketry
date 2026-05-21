package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-05 Phase 3 (server tier) — {@code ItemSealDetector} dispatch
 * matrix pin via the {@code /artest seal-detector check} probe.
 *
 * <p>The probe re-uses production {@link
 * zmaster587.advancedRocketry.util.SealableBlockHandler} predicates
 * in the same if/else order as {@code ItemSealDetector.onItemUse:34-50},
 * so any change to a SealableBlockHandler predicate is reflected
 * directly. Replicating the gate ordering in the probe is intentional —
 * the cross-reference back to {@code ItemSealDetector} is documented so
 * a reordering of production gates is caught during review.</p>
 *
 * <p>Each test places a representative block fixture at an isolated
 * position, asks the probe which branch fires, and asserts the
 * expected i18n-suffix branch name. Branch names match the
 * {@code msg.sealdetector.&lt;branch&gt;} suffix the production code
 * emits to the player.</p>
 *
 * <p>Out of scope: the {@code "notsealblock"} branch (requires a
 * specific banned block whose contents vary by AR config), and the
 * {@code "fluid"} branch (requires an {@code IFluidBlock} fixture
 * which depends on the fluid registry being populated). Those are
 * left for a later expansion once a deterministic banned-block fixture
 * is available.</p>
 */
public class SealDetectorDispatchTest extends AbstractSharedServerTest {

    private static final Pattern BRANCH = Pattern.compile("\"branch\":\"([^\"]+)\"");
    private static final int DIM = 0;

    private static String probe(int x, int y, int z) throws Exception {
        String resp = String.join("\n", client().execute(
                "artest seal-detector check " + DIM + " " + x + " " + y + " " + z));
        Matcher m = BRANCH.matcher(resp);
        assertTrue("probe response must contain a branch field; got: " + resp,
                m.find());
        return m.group(1);
    }

    private static void place(int x, int y, int z, String blockId) throws Exception {
        // /artest place uses minecraft:<name> form; ensure chunk loaded by
        // first placing air at the position (no-op for an already-air cell
        // but force-loads the chunk).
        client().execute("artest place " + DIM + " " + x + " " + y + " " + z
                + " " + blockId);
    }

    // ───────────────────── sealed branch ──────────────────────────────────

    @Test
    public void stoneBlockReportsSealedBranch() throws Exception {
        // Full solid ROCK material → isBlockSealed returns true via the
        // final `isFullBlock` clause. Branch: "sealed".
        int x = 200, y = 80, z = 200;
        place(x, y, z, "minecraft:stone");
        assertEquals("solid stone at " + x + "," + y + "," + z
                        + " must produce branch 'sealed'",
                "sealed", probe(x, y, z));
    }

    @Test
    public void cobblestoneBlockReportsSealedBranch() throws Exception {
        int x = 210, y = 80, z = 200;
        place(x, y, z, "minecraft:cobblestone");
        assertEquals("solid cobblestone must produce branch 'sealed'",
                "sealed", probe(x, y, z));
    }

    // ───────────────────── notsealmat branch ──────────────────────────────

    @Test
    public void airReportsNotSealMatBranch() throws Exception {
        // Material.AIR is on materialBanList (SealableBlockHandler line
        // 219). isBlockSealed returns false (material check); dispatch
        // falls through to isMaterialBanned → true → "notsealmat".
        int x = 220, y = 80, z = 200;
        place(x, y, z, "minecraft:air");
        assertEquals("air must produce branch 'notsealmat' (Material.AIR is banned)",
                "notsealmat", probe(x, y, z));
    }

    @Test
    public void leavesReportNotSealMatBranch() throws Exception {
        // Material.LEAVES is on materialBanList. Pins the multi-material
        // ban contract (not just AIR).
        int x = 230, y = 80, z = 200;
        place(x, y, z, "minecraft:leaves");
        assertEquals("leaves must produce branch 'notsealmat' (Material.LEAVES is banned)",
                "notsealmat", probe(x, y, z));
    }

    @Test
    public void sandReportNotSealMatBranch() throws Exception {
        // Material.SAND is on materialBanList — pinning this guards
        // against silent removal from the default ban list (which would
        // let sand seal rooms, a player-visible regression).
        int x = 240, y = 80, z = 200;
        place(x, y, z, "minecraft:sand");
        assertEquals("sand must produce branch 'notsealmat' (Material.SAND is banned)",
                "notsealmat", probe(x, y, z));
    }

    // ───────────────────── other branch ───────────────────────────────────

    @Test
    public void stoneSlabReportsOtherBranch() throws Exception {
        // Stone slab: Material.ROCK (solid, not banned), but half-block
        // bounds → isFullBlock=false → isBlockSealed=false. Dispatch
        // falls through ROCK-not-banned, slab-not-banned,
        // isFullBlock=false, not-IFluidBlock → "other".
        // (Torch was tried first but vanilla torch requires an attached
        // adjacent block; /artest place succeeds at the placement call
        // but the torch entity immediately detaches, leaving air —
        // which fires "notsealmat" instead.)
        int x = 250, y = 80, z = 200;
        place(x, y, z, "minecraft:stone_slab");
        assertEquals("stone slab must produce branch 'other' (solid ROCK, "
                        + "not banned, half-block bounds, not a fluid)",
                "other", probe(x, y, z));
    }

    // ───────────────────── probe shape ───────────────────────────────────

    @Test
    public void probeReportsPositionInResponse() throws Exception {
        // The probe response must echo the input position alongside the
        // branch — tests rely on this for correlating probe calls to the
        // fixture they evaluated.
        int x = 260, y = 80, z = 200;
        place(x, y, z, "minecraft:stone");
        String resp = String.join("\n", client().execute(
                "artest seal-detector check " + DIM + " " + x + " " + y + " " + z));
        assertTrue("response must echo the position; got: " + resp,
                resp.contains("\"pos\":[" + x + "," + y + "," + z + "]"));
    }

    @Test
    public void probeReportsErrorForUnknownSubcommand() throws Exception {
        String resp = String.join("\n", client().execute(
                "artest seal-detector wibble 0 0 0 0"));
        assertTrue("unknown subcommand must surface an error; got: " + resp,
                resp.contains("\"error\""));
    }
}
