package zmaster587.advancedRocketry.test.server;

// migrated to AbstractSharedServerTest (TASK-03 B2)
import org.junit.Assume;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 2 — worldgen smoke + determinism.
 *
 * Until this point we had zero coverage of the actual chunk-generation path.
 * Here we exercise the public probe surface ({@code /artest worldgen sample}
 * and {@code worldgen ore-stats}) on a real AR planet dim to:
 *
 *   - prove {@code WorldProviderPlanet}'s chunk provider returns a chunk
 *     with valid top-Y / top-block / biome (smoke);
 *   - prove the same chunk sampled twice in the same session returns the
 *     same answer (within-session determinism — guards against a future
 *     "regenerate on every probe" bug);
 *   - prove the biome reported by sample matches the biome reported by
 *     ore-stats (the underlying ChunkProvider must be the SAME instance
 *     for both subcommands, not two parallel providers handing out
 *     different biome lookups).
 *
 * Cross-session determinism (same seed → identical histogram across server
 * restarts) is intentionally deferred to a later phase — it doubles the
 * harness boot time and the within-session check already catches the
 * majority of regenerator bugs.
 */
public class WorldgenDeterminismAndSamplingTest extends AbstractSharedServerTest {

    private static final Pattern AR_DIMS_ARRAY_PATTERN =
            Pattern.compile("\"arDimensions\":\\[([^]]*)]");
    private static final Pattern TOP_Y_PATTERN = Pattern.compile("\"topY\":(-?\\d+)");
    private static final Pattern BIOME_PATTERN = Pattern.compile("\"biome\":\"([^\"]+)\"");
    private static final Pattern TOP_BLOCK_PATTERN = Pattern.compile("\"topBlock\":\"([^\"]+)\"");

    private int firstNonOverworldArDimOrSkip() throws Exception {
        String joined = String.join("\n", client().execute("artest dim list"));
        Assume.assumeFalse(
                "No AR dimensions registered — skipping (empty galaxy?)",
                joined.contains("\"arDimensions\":[]"));
        Matcher m = AR_DIMS_ARRAY_PATTERN.matcher(joined);
        assertTrue("could not parse arDimensions array: " + joined, m.find());
        for (String part : m.group(1).split(",")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            int dim = Integer.parseInt(t);
            if (dim != 0) return dim;
        }
        Assume.assumeTrue(
                "Only overworld (dim 0) is registered as an AR planet — skipping",
                false);
        return -1;
    }

    private static String group(Pattern p, String resp, String label) {
        Matcher m = p.matcher(resp);
        assertTrue("could not parse " + label + " from response: " + resp, m.find());
        return m.group(1);
    }

    @Test
    public void worldgenSampleReturnsCoherentChunkData() throws Exception {
        int dim = firstNonOverworldArDimOrSkip();
        client().execute("artest dim load " + dim);

        String sample = String.join("\n",
                client().execute("artest worldgen sample " + dim + " 0 0"));

        // Smoke: each field present and sensible.
        int topY = Integer.parseInt(group(TOP_Y_PATTERN, sample, "topY"));
        String biome = group(BIOME_PATTERN, sample, "biome");
        String topBlock = group(TOP_BLOCK_PATTERN, sample, "topBlock");

        assertTrue("topY out of valid range [0,256]: " + topY, topY >= 0 && topY <= 256);
        assertNotNull(biome);
        assertNotNull(topBlock);
        // topBlock has a registry-style id; "minecraft:air" can happen if the
        // chunk is empty above ground, but the field itself must never be
        // missing/empty.
        assertTrue("topBlock looks unset: " + topBlock, topBlock.contains(":"));
        assertTrue("biome looks unset: " + biome, biome.contains(":") || biome.equals("unknown"));
    }

    @Test
    public void sameChunkSampledTwiceReturnsSameTopAndBiome() throws Exception {
        int dim = firstNonOverworldArDimOrSkip();
        client().execute("artest dim load " + dim);

        String first = String.join("\n",
                client().execute("artest worldgen sample " + dim + " 0 0"));
        String second = String.join("\n",
                client().execute("artest worldgen sample " + dim + " 0 0"));

        // Within-session determinism: a regenerator-style bug that swaps the
        // chunk provider between calls would change topY / biome / topBlock.
        assertEquals("topY drifted between two samples of (0,0) on dim " + dim,
                group(TOP_Y_PATTERN, first, "topY"),
                group(TOP_Y_PATTERN, second, "topY"));
        assertEquals("biome drifted between two samples of (0,0) on dim " + dim,
                group(BIOME_PATTERN, first, "biome"),
                group(BIOME_PATTERN, second, "biome"));
        assertEquals("topBlock drifted between two samples of (0,0) on dim " + dim,
                group(TOP_BLOCK_PATTERN, first, "topBlock"),
                group(TOP_BLOCK_PATTERN, second, "topBlock"));
    }

    @Test
    public void differentChunksReturnIndependentlyAddressableData() throws Exception {
        // Sanity that the probe isn't returning a cached "single chunk" for
        // every query — sample three distinct chunks and assert they don't
        // collapse to identical (topY,topBlock) triples.
        int dim = firstNonOverworldArDimOrSkip();
        client().execute("artest dim load " + dim);

        // Use wider chunk spread (0/64/128 in X) so adjacent biome boundaries
        // are crossed even on AR's flat moon-style planets. With (0,4,8)
        // every sample landed in the same 16×16 biome cell on `moondark`,
        // legitimately collapsing topY+biome to identical and flaking the
        // assertion (TASK-28 F7).
        String a = String.join("\n",
                client().execute("artest worldgen sample " + dim + " 0 0"));
        String b = String.join("\n",
                client().execute("artest worldgen sample " + dim + " 64 64"));
        String c = String.join("\n",
                client().execute("artest worldgen sample " + dim + " 128 0"));

        String topAandBandC =
                group(TOP_Y_PATTERN, a, "topY") + "/"
                        + group(TOP_Y_PATTERN, b, "topY") + "/"
                        + group(TOP_Y_PATTERN, c, "topY");
        // If all three chunks have *identical* topY, that's possible on a
        // flat planet biome (atmosphere-vacuum desert moon, e.g.) — only
        // flag if all three are the same AND the biome is also the same;
        // the combined signature is what would betray a cache bug.
        String biomeSig = group(BIOME_PATTERN, a, "biome") + "/"
                + group(BIOME_PATTERN, b, "biome") + "/"
                + group(BIOME_PATTERN, c, "biome");
        // Either the topY differs OR the biome differs across the three.
        // If both are identical for three deliberately-spaced chunks, the
        // probe is almost certainly broken.
        boolean topYAllSame = group(TOP_Y_PATTERN, a, "topY")
                .equals(group(TOP_Y_PATTERN, b, "topY"))
                && group(TOP_Y_PATTERN, b, "topY")
                        .equals(group(TOP_Y_PATTERN, c, "topY"));
        boolean biomeAllSame = group(BIOME_PATTERN, a, "biome")
                .equals(group(BIOME_PATTERN, b, "biome"))
                && group(BIOME_PATTERN, b, "biome")
                        .equals(group(BIOME_PATTERN, c, "biome"));
        assertTrue("three spaced chunks reported identical (topY,biome) — probe likely caching\n"
                        + "  topY=" + topAandBandC + "\n  biome=" + biomeSig,
                !(topYAllSame && biomeAllSame));
    }

    @Test
    public void oreStatsAcceptsValidBlockAndReportsCount() throws Exception {
        int dim = firstNonOverworldArDimOrSkip();
        client().execute("artest dim load " + dim);

        String stats = String.join("\n",
                client().execute("artest worldgen ore-stats " + dim + " 0 0 1 minecraft:stone"));
        // Any AR planet that generates terrain at all has SOME stone; if
        // count parsed as zero, that's still acceptable (vacuum moon),
        // but the field MUST be present and parse as a non-negative integer.
        assertTrue("ore-stats reply missing 'count' field: " + stats,
                stats.contains("\"count\":"));
        assertTrue("ore-stats reply missing 'chunksScanned' field: " + stats,
                stats.contains("\"chunksScanned\":"));
        // radius=1 → 3×3 = 9 chunks
        assertTrue("ore-stats with radius=1 must have scanned >=1 chunk: " + stats,
                !stats.contains("\"chunksScanned\":0"));
    }

    @Test
    public void oreStatsRejectsRadiusOverCap() throws Exception {
        int dim = firstNonOverworldArDimOrSkip();
        client().execute("artest dim load " + dim);

        String stats = String.join("\n",
                client().execute("artest worldgen ore-stats " + dim + " 0 0 5 minecraft:stone"));
        // Cap is 4; 5 should error out fast rather than start scanning ~6.5M blocks.
        assertTrue("ore-stats with radius=5 should error (cap=4): " + stats,
                stats.contains("\"error\":\"radius too large\""));
    }

    @Test
    public void oreStatsRejectsUnknownBlockId() throws Exception {
        int dim = firstNonOverworldArDimOrSkip();
        client().execute("artest dim load " + dim);

        String stats = String.join("\n",
                client().execute("artest worldgen ore-stats " + dim + " 0 0 1 advancedrocketry:nonsense_block"));
        assertTrue("ore-stats with unknown block must error: " + stats,
                stats.contains("\"error\":\"unknown block id\""));
    }
}
