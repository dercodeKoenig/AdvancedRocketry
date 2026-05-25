package zmaster587.advancedRocketry.test.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-19 Phase 1b — terraformer powered cycle on overworld with
 * {@code allowTerraformNonAR=true} config flip.
 *
 * <p>Pins the <b>{@code allowTerraformNonAR} branch</b> of
 * {@code TileAtmosphereTerraformer.processComplete()}'s gate:</p>
 *
 * <pre>{@code
 *     (WorldProviderPlanet && isNativeDimension) || allowTerraformNonAR
 * }</pre>
 *
 * <p>Players running modpacks with {@code allowTerraformingNonARWorlds=true}
 * expect the terraformer to work on the overworld and any other non-AR
 * dim. Phase 1a pinned the native-planet branch (the default config);
 * this phase pins the explicitly-enabled override.</p>
 *
 * <p><b>State restoration</b>: each test snapshots {@code allowTerraformNonAR}
 * and the overworld's current atmosphere density in {@code @Before}, then
 * restores both in {@code @After} — the shared harness is one JVM across
 * all methods of this class, so leaked config or density would corrupt
 * subsequent methods.</p>
 */
public class TerraformerPoweredCycleOnOverworldTest extends AbstractSharedServerTest {

    private static final int DIM = 0;
    private static final int CY = 128;
    private static final int CZ = 4000;
    private static final int CX_POSITIVE = 4000;
    private static final int CX_NEGATIVE = 4200;

    private static final Pattern CONFIG_VALUE = Pattern.compile("\"value\":(true|false|-?\\d+(?:\\.\\d+)?)");
    private static final Pattern CURRENT_ATMOS = Pattern.compile("\"currentAtmosphere\":(-?\\d+)");
    private static final Pattern POWER_POS =
            Pattern.compile("\"powerPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern LIQUID_TRIPLE =
            Pattern.compile("\\[(-?\\d+),(-?\\d+),(-?\\d+)]");

    private boolean originalAllowNonAR;
    private int originalDensity;

    @Before
    public void snapshotConfigAndDensity() throws Exception {
        originalAllowNonAR = readBoolConfig("allowTerraformNonAR");
        originalDensity = readDensity();
    }

    @After
    public void restoreConfigAndDensity() throws Exception {
        // Restore in BOTH cases — even if the config was never flipped this
        // method completes the round-trip cleanly.
        exec("artest config set allowTerraformNonAR " + originalAllowNonAR);
        exec("artest terraforming set-density " + DIM + " " + originalDensity);
    }

    /** With the config flipped, an overworld-placed terraformer with fuel +
     *  power must mutate dim 0's atmosphere density. */
    @Test
    public void overworldTerraformerWithNonArConfigFlipStepsDensity() throws Exception {
        String flip = exec("artest config set allowTerraformNonAR true");
        assertTrue("config flip failed: " + flip,
                flip.contains("\"ok\":true") && flip.contains("\"newValue\":true"));

        String fixture = buildAndCompleteFixture(CX_POSITIVE);
        injectPower(fixture, 30_000_000);
        enableMachine(CX_POSITIVE);

        int densityBefore = readDensity();
        runRefillCycle(fixture, CX_POSITIVE, 60, 400);
        int densityAfter = readDensity();

        assertNotEquals("non-AR-config-flipped terraformer did not move density"
                        + " on overworld (before=" + densityBefore
                        + " after=" + densityAfter + ")",
                densityBefore, densityAfter);
    }

    /** Counter-test: with the default config ({@code allowTerraformNonAR=false}),
     *  the same fuel+power+tick combination on overworld must NOT move
     *  density — the dim-check gate is the sole reason. */
    @Test
    public void overworldTerraformerWithoutConfigFlipDoesNotStep() throws Exception {
        // Explicit set to false (idempotent with the default) so a stale
        // value from a sibling test or harness boot can't masquerade as
        // a passing default-branch test.
        String set = exec("artest config set allowTerraformNonAR false");
        assertTrue("config set-false failed: " + set,
                set.contains("\"ok\":true"));

        String fixture = buildAndCompleteFixture(CX_NEGATIVE);
        injectPower(fixture, 30_000_000);
        enableMachine(CX_NEGATIVE);

        int densityBefore = readDensity();
        runRefillCycle(fixture, CX_NEGATIVE, 60, 400);
        int densityAfter = readDensity();

        assertEquals("default-config terraformer moved overworld density anyway"
                        + " (before=" + densityBefore + " after=" + densityAfter + ")"
                        + " — gate branch ((WorldProviderPlanet && isNative) ||"
                        + " allowTerraformNonAR) leaked through",
                densityBefore, densityAfter);
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private String buildAndCompleteFixture(int cx) throws Exception {
        String fixture = exec("artest fixture multiblock terraformer "
                + DIM + " " + cx + " " + CY + " " + CZ);
        assertTrue("terraformer fixture build failed: " + fixture,
                fixture.contains("\"ok\":true") && fixture.contains("\"unresolved\":0"));
        String tryComplete = exec("artest machine try-complete "
                + DIM + " " + cx + " " + CY + " " + CZ);
        assertTrue("terraformer structure failed to complete: " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));
        return fixture;
    }

    private void injectPower(String fixture, int amount) throws Exception {
        Matcher m = POWER_POS.matcher(fixture);
        assertTrue("no powerPos in fixture response: " + fixture, m.find());
        int px = Integer.parseInt(m.group(1));
        int py = Integer.parseInt(m.group(2));
        int pz = Integer.parseInt(m.group(3));
        String resp = exec("artest energy inject "
                + DIM + " " + px + " " + py + " " + pz + " " + amount);
        assertTrue("energy inject failed: " + resp, resp.contains("\"ok\":true"));
    }

    private void enableMachine(int cx) throws Exception {
        String resp = exec("artest machine set-enabled "
                + DIM + " " + cx + " " + CY + " " + CZ + " true");
        assertTrue("machine set-enabled failed: " + resp, resp.contains("\"enabled\":true"));
    }

    private void runRefillCycle(String fixture, int cx, int iterations, int ticksPerIter)
            throws Exception {
        for (int i = 0; i < iterations; i++) {
            injectFluidAt(fixture, 0, "nitrogen", 16000);
            injectFluidAt(fixture, 1, "nitrogen", 16000);
            injectFluidAt(fixture, 2, "oxygen", 16000);
            injectFluidAt(fixture, 3, "oxygen", 16000);
            String tick = exec("artest tile force-tick "
                    + DIM + " " + cx + " " + CY + " " + CZ + " " + ticksPerIter);
            assertTrue("force-tick errored on iter " + i + ": " + tick,
                    tick.contains("\"ok\":true"));
        }
    }

    private void injectFluidAt(String fixture, int hatchIndex, String fluidName, int amount)
            throws Exception {
        int sectionStart = fixture.indexOf("\"liquidInputPositions\"");
        assertTrue("no liquidInputPositions in fixture response: " + fixture,
                sectionStart >= 0);
        Matcher m = LIQUID_TRIPLE.matcher(fixture);
        m.region(sectionStart, fixture.length());
        for (int i = 0; i <= hatchIndex; i++) {
            assertTrue("liquidInputPositions has fewer than " + (hatchIndex + 1)
                    + " hatches: " + fixture, m.find());
        }
        int lx = Integer.parseInt(m.group(1));
        int ly = Integer.parseInt(m.group(2));
        int lz = Integer.parseInt(m.group(3));
        String resp = exec("artest fluid inject "
                + DIM + " " + lx + " " + ly + " " + lz + " " + fluidName + " " + amount);
        assertTrue(fluidName + " inject failed at hatch " + hatchIndex + ": " + resp,
                resp.contains("\"ok\":true"));
    }

    private int readDensity() throws Exception {
        String info = exec("artest terraforming info " + DIM);
        Matcher m = CURRENT_ATMOS.matcher(info);
        assertTrue("no currentAtmosphere in terraforming info: " + info, m.find());
        return Integer.parseInt(m.group(1));
    }

    private boolean readBoolConfig(String key) throws Exception {
        String resp = exec("artest config get " + key);
        Matcher m = CONFIG_VALUE.matcher(resp);
        assertTrue("config get " + key + " did not yield value: " + resp, m.find());
        return Boolean.parseBoolean(m.group(1));
    }
}
