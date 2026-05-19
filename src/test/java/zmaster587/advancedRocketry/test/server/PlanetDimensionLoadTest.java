package zmaster587.advancedRocketry.test.server;

// migrated to AbstractSharedServerTest (TASK-03 B2)
import org.junit.Assume;
import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.3 — planet/dimension lifecycle smoke.
 *
 * Walks {@code /artest dim list} to verify AR has registered at least one
 * planet, then drills into a representative AR dim to confirm the provider
 * wiring (provider class, biome provider, chunk generator, save folder) and
 * the celestial-angle math is deterministic and time-varying. Empty galaxy
 * configurations skip via {@link Assume} so an empty mod-pack doesn't gate
 * the suite.
 */
public class PlanetDimensionLoadTest extends AbstractSharedServerTest {

    private static final String AR_PROVIDER_FQN =
            "zmaster587.advancedRocketry.world.provider.WorldProviderPlanet";

    private static final Pattern AR_DIM_PATTERN =
            Pattern.compile("\"arDimensions\":\\[(-?\\d+)");

    private static final Pattern AR_DIMS_ARRAY_PATTERN =
            Pattern.compile("\"arDimensions\":\\[([^]]*)\\]");

    private static final Pattern ANGLE_PATTERN =
            Pattern.compile("\"angle\":(-?[0-9.eE+-]+)");

    @Test
    public void arPlanetsArePreloaded() throws Exception {
        String joined = String.join("\n", client().execute("artest dim list"));

        assertTrue("dim list missing arDimensions key — probe wiring broken: " + joined,
                joined.contains("\"arDimensions\":["));

        Assume.assumeFalse(
                "No AR dimensions registered — skipping (empty galaxy?)",
                joined.contains("\"arDimensions\":[]"));
    }

    @Test
    public void dimLoadOnOverworldReportsLoaded() throws Exception {
        // SMART §5.2: /artest dim load <id> must force-load the world and
        // report `loaded:true` afterwards. Overworld (dim 0) is always loaded
        // on a fresh dedicated server, so this smoke pins the probe wiring
        // without depending on any AR-specific dim id. Deeper load behavior
        // (loading a not-yet-touched AR dim and back) belongs to a later phase.
        String joined = String.join("\n", client().execute("artest dim load 0"));

        assertTrue("dim load 0 did not echo dim:0 in response: " + joined,
                joined.contains("\"dim\":0"));
        assertTrue("dim load 0 did not report loaded:true: " + joined,
                joined.contains("\"loaded\":true"));
    }

    @Test
    public void providerClassIsWorldProviderPlanet() throws Exception {
        // AR registers Earth as dim 0 but keeps its vanilla WorldProviderSurface,
        // so this assertion targets the first NON-overworld AR planet — the
        // ones that actually exercise AR's WorldProviderPlanet wiring.
        int arDim = firstNonOverworldArDimOrSkip();
        String info = loadAndInfo(arDim);
        assertTrue(
                "dim " + arDim + " providerClass should be " + AR_PROVIDER_FQN + ": " + info,
                info.contains("\"providerClass\":\"" + AR_PROVIDER_FQN + "\""));
    }

    @Test
    public void biomeProviderIsNonNull() throws Exception {
        int arDim = firstNonOverworldArDimOrSkip();
        String info = loadAndInfo(arDim);
        assertTrue("biomeProviderClass field missing from dim info: " + info,
                info.contains("\"biomeProviderClass\":"));
        assertTrue("biomeProviderClass reported null for AR dim " + arDim + ": " + info,
                !info.contains("\"biomeProviderClass\":\"null\""));
    }

    @Test
    public void chunkGeneratorIsNonNull() throws Exception {
        int arDim = firstNonOverworldArDimOrSkip();
        String info = loadAndInfo(arDim);
        assertTrue("chunkGeneratorClass field missing from dim info: " + info,
                info.contains("\"chunkGeneratorClass\":"));
        assertTrue("chunkGeneratorClass reported null for AR dim " + arDim + ": " + info,
                !info.contains("\"chunkGeneratorClass\":\"null\""));
    }

    @Test
    public void saveFolderResolvesToExpectedPath() throws Exception {
        int arDim = firstNonOverworldArDimOrSkip();
        String info = loadAndInfo(arDim);
        assertTrue("saveDir field missing from dim info: " + info,
                info.contains("\"saveDir\":"));
        // WorldProviderPlanet.getSaveFolder() returns "advRocketry/" + super.getSaveFolder().
        assertTrue("saveDir for AR planet " + arDim + " should be under advRocketry/: " + info,
                info.contains("\"saveDir\":\"advRocketry/"));
    }

    @Test
    public void celestialAngleStableAcrossSameWorldTime() throws Exception {
        int arDim = firstNonOverworldArDimOrSkip();
        // The probe is a pure function of (dim, worldTime), so two calls with
        // identical inputs must produce identical angles. We compare extracted
        // numeric values rather than full response strings — the dedicated
        // server prefixes each console echo with a timestamp, so byte-level
        // response equality would race on tick boundaries.
        loadDim(arDim);
        double first = extractAngle(client().execute(
                "artest dim celestial-angle " + arDim + " 0"));
        double second = extractAngle(client().execute(
                "artest dim celestial-angle " + arDim + " 0"));

        assertEquals(
                "celestial-angle must be deterministic for identical inputs",
                first, second, 0.0);
    }

    @Test
    public void celestialAngleProgressesAcrossDifferentWorldTimes() throws Exception {
        int arDim = firstNonOverworldArDimOrSkip();
        loadDim(arDim);
        double a0 = extractAngle(client().execute(
                "artest dim celestial-angle " + arDim + " 0"));
        double a6k = extractAngle(client().execute(
                "artest dim celestial-angle " + arDim + " 6000"));
        double a12k = extractAngle(client().execute(
                "artest dim celestial-angle " + arDim + " 12000"));

        // Soft assertion: three meaningfully different world times must not
        // collapse to the same angle. The celestial cycle wraps modulo the
        // rotational period, so strict monotonicity isn't safe to assert
        // without first pinning AR's exact rotational-period math; that
        // belongs to a future test once the rocket assembly suite is in.
        assertNotEquals("celestial-angle did not change between t=0 and t=6000 (a0=" + a0
                + ", a6k=" + a6k + ")", a0, a6k, 0.0);
        assertNotEquals("celestial-angle did not change between t=6000 and t=12000 (a6k=" + a6k
                + ", a12k=" + a12k + ")", a6k, a12k, 0.0);
    }

    private int firstArDimOrSkip() throws Exception {
        String joined = String.join("\n", client().execute("artest dim list"));
        Assume.assumeFalse(
                "No AR dimensions registered — skipping (empty galaxy?)",
                joined.contains("\"arDimensions\":[]"));
        Matcher m = AR_DIM_PATTERN.matcher(joined);
        assertTrue("could not parse first AR dim id from probe response: " + joined, m.find());
        return Integer.parseInt(m.group(1));
    }

    private int firstNonOverworldArDimOrSkip() throws Exception {
        String joined = String.join("\n", client().execute("artest dim list"));
        Assume.assumeFalse(
                "No AR dimensions registered — skipping (empty galaxy?)",
                joined.contains("\"arDimensions\":[]"));
        Matcher m = AR_DIMS_ARRAY_PATTERN.matcher(joined);
        assertTrue("could not parse arDimensions array from probe response: " + joined, m.find());
        Integer found = null;
        for (String part : m.group(1).split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            int dim = Integer.parseInt(trimmed);
            if (dim != 0) {
                found = dim;
                break;
            }
        }
        Assume.assumeTrue(
                "Only overworld (dim 0) is registered as an AR planet — skipping " +
                        "WorldProviderPlanet-specific assertions",
                found != null);
        return found;
    }

    private String loadAndInfo(int dim) throws Exception {
        loadDim(dim);
        return String.join("\n", client().execute("artest dim info " + dim));
    }

    private void loadDim(int dim) throws Exception {
        // Force the dim loaded before any property/angle probe — AR dims are
        // not in DimensionManager-loaded state on fresh boot.
        client().execute("artest dim load " + dim);
    }

    private static double extractAngle(List<String> response) {
        String joined = String.join("\n", response);
        Matcher m = ANGLE_PATTERN.matcher(joined);
        if (!m.find()) {
            throw new AssertionError("could not extract angle from probe response: " + joined);
        }
        return Double.parseDouble(m.group(1));
    }
}
