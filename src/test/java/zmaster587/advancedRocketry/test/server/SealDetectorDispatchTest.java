package zmaster587.advancedRocketry.test.server;

import org.junit.After;
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
 * <p>TASK-23 (2026-05-25) added the remaining reachable branches:
 * {@code "notsealblock"} via probe-driven {@code blockBanList} mutation
 * (restored in {@code @After}) and {@code "fluid"} via AR's
 * {@code advancedrocketry:oxygenFluid} ({@code IFluidBlock} of
 * {@code Material.WATER}). The {@code "notfullblock"} branch is
 * documented as unreachable for vanilla + AR's block set — see the
 * {@code NOTFULLBLOCK_UNREACHABLE_DOC} comment block below.</p>
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

    // ───────────────────── notsealblock branch (TASK-23) ─────────────────

    /** Pins the {@code blockBanList} dispatch path. The default
     *  {@code blockBanList} is empty (per {@link SealableBlockHandler}'s
     *  {@code loadDefaultData}, which only populates {@code materialBanList}),
     *  so a test block must be added to the list via the new
     *  {@code /artest seal-detector add-block-ban} probe, then removed in
     *  {@code @After} to restore the shared harness's default state. */
    @Test
    public void goldBlockBannedReportsNotSealBlockBranch() throws Exception {
        int x = 270, y = 80, z = 200;
        place(x, y, z, "minecraft:gold_block");
        try {
            // Baseline: a full solid block not yet on any ban list seals
            // by default. This documents the difference from the post-ban
            // state below — without this baseline the test would pass even
            // if the ban-list mechanism were silently broken.
            assertEquals("baseline: unbanned gold_block should seal",
                    "sealed", probe(x, y, z));

            String ban = String.join("\n", client().execute(
                    "artest seal-detector add-block-ban minecraft:gold_block"));
            assertTrue("add-block-ban probe failed: " + ban,
                    ban.contains("\"ok\":true"));

            assertEquals("gold_block on blockBanList must produce branch "
                            + "'notsealblock'",
                    "notsealblock", probe(x, y, z));
        } finally {
            // Restore — shared harness leaks state across tests, and a
            // permanently-banned gold_block would make any sibling test
            // that happened to place gold_block diverge from production.
            client().execute(
                    "artest seal-detector remove-block-ban minecraft:gold_block");
        }
    }

    @After
    public void restoreBlockBanListDefensively() throws Exception {
        // Belt-and-braces — even if a @Test threw before its finally ran,
        // this @After tries the un-ban anyway. Idempotent: produces
        // {"removed":false} when the block isn't present.
        client().execute(
                "artest seal-detector remove-block-ban minecraft:gold_block");
    }

    // ───────────────────── fluid branch (TASK-23) ─────────────────────────

    /** Pins the {@code IFluidBlock} dispatch. AR's
     *  {@code advancedrocketry:oxygenFluid} extends {@code BlockFluidClassic}
     *  (Forge), which implements {@code IFluidBlock} — production's "fluid"
     *  branch fires precisely on that {@code instanceof} check. Vanilla
     *  water/lava (which extend {@code BlockLiquid}, NOT {@code IFluidBlock})
     *  would fall through to the "other" branch and aren't usable for this
     *  pin. */
    @Test
    public void oxygenFluidBlockReportsFluidBranch() throws Exception {
        int x = 280, y = 80, z = 200;
        place(x, y, z, "advancedrocketry:oxygenfluid");
        assertEquals("AR's oxygenFluid block (Material.WATER + BlockFluidClassic) "
                        + "must produce branch 'fluid'",
                "fluid", probe(x, y, z));
    }

    // ───────────────────── notfullblock branch — unreachable ──────────────

    /** <b>No positive test for the {@code notfullblock} branch.</b>
     *
     *  <p>Reaching it requires a block where ALL of these hold:</p>
     *  <ul>
     *    <li>{@code SealableBlockHandler.isBlockSealed} returns false via
     *        one of its non-ban-list gates (material is liquid or non-solid,
     *        block is air, or block is {@code IFluidBlock});</li>
     *    <li>material is NOT in {@code materialBanList} (otherwise the
     *        dispatch hits "notsealmat" first);</li>
     *    <li>block is NOT in {@code blockBanList} (otherwise "notsealblock"
     *        fires first);</li>
     *    <li>{@code isFullBlock(world, pos)} returns true — i.e. the
     *        block's collision bounding box is exactly {@code [0,0,0]→[1,1,1]}.</li>
     *  </ul>
     *
     *  <p>No vanilla or AR-registered block satisfies all four. The liquid
     *  / non-solid / air / IFluidBlock blocks all have null or partial
     *  collision boxes. Modded blocks could (hypothetically — a custom
     *  liquid with a full collision box), but that's not the repo's contract
     *  to pin.</p>
     *
     *  <p>The branch exists in {@code ItemSealDetector.onItemUse:44-45} and
     *  is replicated in {@code TestProbeCommand.handleSealDetector:8913-8914},
     *  but appears to be effectively dead code in the current block set.
     *  Logged in the bug ledger so a future fix (e.g. swapping the
     *  {@code isFullBlock} predicate to its inverse, or removing the branch
     *  entirely) flips an explicit test rather than a silent no-op. </p> */
    @SuppressWarnings("unused")
    private static final String NOTFULLBLOCK_UNREACHABLE_DOC = "see javadoc above";

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
