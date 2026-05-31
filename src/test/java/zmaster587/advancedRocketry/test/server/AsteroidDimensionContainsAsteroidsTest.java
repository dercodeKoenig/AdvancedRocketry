package zmaster587.advancedRocketry.test.server;

import org.junit.Assume;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-44 (audit Gap N) — the Asteroid worldprovider dimension actually
 * generates asteroids (floating fill-block islands), not empty void.
 *
 * <p>Production:
 * {@link zmaster587.advancedRocketry.world.ChunkProviderAsteroids} builds
 * island-noise "stems" of the dimension's fill block (defaulting to
 * {@code minecraft:stone} when {@code DimensionProperties.getStoneBlock()}
 * is null) floating in a void. The dimension's defining player-visible
 * feature is that asteroids exist there to mine.</p>
 *
 * <p>Pinned (band / end-state, per SOP — NOT an exact density which is a
 * chunkgen-RNG impl detail): loading a freshly-registered asteroid dim and
 * scanning a chunk region finds &gt; 0 fill blocks. The asteroid dim is
 * created on demand by cloning an existing AR planet's DimensionProperties
 * (inheriting star/atmosphere linkage) and flipping its generator type to
 * asteroid, via the {@code /artest worldgen create-asteroid-dim} probe.</p>
 */
public class AsteroidDimensionContainsAsteroidsTest extends AbstractSharedServerTest {

    private static final int ASTEROID_DIM = 60123;

    private static final Pattern AR_DIMS_ARRAY =
            Pattern.compile("\"arDimensions\":\\[([^]]*)]");
    private static final Pattern COUNT = Pattern.compile("\"count\":(\\d+)");

    @Test
    public void asteroidDimGeneratesFillBlocks() throws Exception {
        // Find a registered non-overworld AR planet to clone as a template.
        int template = firstNonOverworldArDimOrSkip();

        // Create + register the asteroid dim from that template.
        String create = exec("artest worldgen create-asteroid-dim "
                + ASTEROID_DIM + " " + template);
        assertTrue("create-asteroid-dim must succeed: " + create,
                create.contains("\"ok\":true"));
        assertTrue("created dim must report isAsteroid:true: " + create,
                create.contains("\"isAsteroid\":true"));

        // Force the dim loaded so its WorldProviderAsteroid + ChunkProviderAsteroids
        // come online.
        exec("artest dim load " + ASTEROID_DIM);

        // Scan a 5×5 chunk region around origin for the fill block. Asteroids
        // are sparse floating islands, so the contract is "> 0 exist", not a
        // density figure.
        String stats = exec("artest worldgen ore-stats "
                + ASTEROID_DIM + " 0 0 2 minecraft:stone");
        Matcher m = COUNT.matcher(stats);
        assertTrue("ore-stats must report a count: " + stats, m.find());
        int count = Integer.parseInt(m.group(1));
        assertTrue("asteroid dimension must generate > 0 fill (stone) blocks "
                        + "across the scanned region — the 'asteroids exist' "
                        + "contract; count=" + count + " stats=" + stats,
                count > 0);
    }

    private int firstNonOverworldArDimOrSkip() throws Exception {
        String joined = exec("artest dim list");
        Assume.assumeFalse("No AR dimensions registered — skipping",
                joined.contains("\"arDimensions\":[]"));
        Matcher m = AR_DIMS_ARRAY.matcher(joined);
        assertTrue("could not parse arDimensions: " + joined, m.find());
        for (String part : m.group(1).split(",")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            int dim = Integer.parseInt(t);
            if (dim != 0 && dim != ASTEROID_DIM) return dim;
        }
        Assume.assumeTrue("Only overworld registered — skipping", false);
        return -1;
    }

    private String exec(String cmd) throws Exception {
        return String.join("\n", client().execute(cmd));
    }
}
