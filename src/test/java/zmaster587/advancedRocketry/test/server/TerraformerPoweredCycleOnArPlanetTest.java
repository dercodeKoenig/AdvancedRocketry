package zmaster587.advancedRocketry.test.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-19 Phase 1a — terraformer powered cycle on an AR-native planet.
 *
 * <p>Pins the <b>native-dim branch</b> of {@code TileAtmosphereTerraformer
 * .processComplete()}'s gate:</p>
 *
 * <pre>{@code
 *     (WorldProviderPlanet && isNativeDimension) || allowTerraformNonAR
 * }</pre>
 *
 * <p>Generates a fresh AR planet via {@code /ar planet generate}, builds
 * the 17×17 multiblock there, drives the libVulpes machine cycle with
 * fuel + power, and asserts the dim's {@code currentAtmosphere} moves.
 * The {@code allowTerraformNonAR} branch is pinned separately by
 * {@code TerraformerPoweredCycleOnOverworldTest} (Phase 1b).</p>
 *
 * <p>Counter-tests pin the no-fuel and no-power branches: each must
 * leave atmosphere density unchanged so the contract reads "all three
 * preconditions necessary, not just one or two".</p>
 *
 * <p>The fresh planet is generated per-method (not class-scope) because
 * the powered-cycle mutates dim-global atmosphere state — sharing it
 * across methods would leak the increase-mode mutation into the no-fuel
 * baseline read.</p>
 */
public class TerraformerPoweredCycleOnArPlanetTest extends AbstractSharedServerTest {

    private static final Pattern DIM_LINE = Pattern.compile("DIM(\\d+):");
    private static final Pattern CURRENT_ATMOS =
            Pattern.compile("\"currentAtmosphere\":(-?\\d+)");
    private static final Pattern POWER_POS =
            Pattern.compile("\"powerPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern LIQUID_INPUT_POS =
            Pattern.compile("\"liquidInputPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    /** Captures each {@code [x,y,z]} triple inside
     *  {@code "liquidInputPositions":[...]}. Iterating `find()` enumerates
     *  all four 'L' hatches in the terraformer structure. */
    private static final Pattern LIQUID_TRIPLE =
            Pattern.compile("\\[(-?\\d+),(-?\\d+),(-?\\d+)]");

    /** Each method picks distinct controller coords so per-method planets
     *  don't collide if a future refactor moves to class-scope. */
    private static final int CY = 128;
    private static final int CX_POSITIVE = 200;
    private static final int CX_NO_FUEL  = 400;
    private static final int CX_NO_POWER = 600;
    private static final int CZ = 200;

    /** Per-method dim id, allocated in @Before and torn down in @After. */
    private int newDim = -1;

    @Before
    public void generatePlanet() throws Exception {
        Set<Integer> before = arDims();
        // Args 10 10 10 are positive randomness factors — see
        // WorldCommandPlanetLifecycleContractTest for the same idiom.
        exec("ar planet generate 0 Phase1aTerraformer 10 10 10");
        Set<Integer> diff = arDims();
        diff.removeAll(before);
        assertEquals("planet generate must add exactly one dim — diff=" + diff,
                1, diff.size());
        newDim = diff.iterator().next();

        // Force-load the new dim so subsequent block/fluid/energy probes
        // can find a live WorldServer for it.
        String load = exec("artest dim load " + newDim);
        assertTrue("dim load did not report loaded:true — " + load,
                load.contains("\"loaded\":true") || load.contains("\"ok\":true"));
    }

    @After
    public void cleanupPlanet() throws Exception {
        if (newDim != -1) {
            try {
                exec("ar planet delete " + newDim);
            } catch (Exception ignored) {
                // Best-effort cleanup; harness teardown will reclaim anyway.
            }
            newDim = -1;
        }
    }

    /** Powered + fueled + enabled multiblock on a native planet must
     *  mutate atmosphere density at least once over a long force-tick
     *  burst. Direction (increase vs decrease) is the default the GUI
     *  ships with — {@code buttonIncrease} defaults to true per
     *  {@code TileAtmosphereTerraformer.<init>}.
     *
     *  <p>A single density step requires {@code completionTime = 18000 ×
     *  terraformSpeed} (default 18000) onRunningPoweredTick() calls, each
     *  consuming {@code terraformliquidRate = 40} mB of both N2 and O2.
     *  The test runs in a fill→tick refill loop because no fluid hatch
     *  can hold the full 18000×40 = 720000 mB single-step requirement.</p> */
    @Test
    public void nativePlanetTerraformerWithFuelAndPowerStepsDensity() throws Exception {
        assertDimIsNativeArPlanet();

        String fixture = buildAndCompleteFixture(CX_POSITIVE);
        // 18000 ticks × 1000 powerPerTick = 18 M energy. Inject 30 M for
        // headroom; libVulpes power hatches accept large bursts.
        injectPower(fixture, 30_000_000);
        enableMachine(CX_POSITIVE);

        // DIAGNOSTIC — dump the controller's internal aggregator state so
        // a failure points directly at integration (P/L hatches not added)
        // vs cycle (currentTime not incrementing).
        String preState = exec("artest machine controller-state "
                + newDim + " " + CX_POSITIVE + " " + CY + " " + CZ);
        assertTrue("controller-state probe missing batteries readout — " + preState,
                preState.contains("\"batteriesPresent\":true"));

        int densityBefore = readDensity();
        // Refill loop: terraformer needs BOTH N2 and O2 each tick.
        // TileFluidHatch holds one fluid per tank — so split: hatch 0+1
        // are N2 sources, hatch 2+3 are O2 sources. The controller's
        // drain loop iterates fluidInPorts; it picks up N2 from the
        // first two and O2 from the last two.
        // Budget: 60 iterations × 400 ticks = 24000 ticks → at least
        // one density step (every 18000 ticks).
        for (int i = 0; i < 60; i++) {
            injectFluidAt(fixture, 0, "nitrogen", 16000);
            injectFluidAt(fixture, 1, "nitrogen", 16000);
            injectFluidAt(fixture, 2, "oxygen", 16000);
            injectFluidAt(fixture, 3, "oxygen", 16000);
            forceTick(CX_POSITIVE, 400);
        }
        int densityAfter = readDensity();

        String postState = exec("artest machine controller-state "
                + newDim + " " + CX_POSITIVE + " " + CY + " " + CZ);
        assertNotEquals("powered + fueled terraformer did not move density"
                        + " (before=" + densityBefore + " after=" + densityAfter + ")"
                        + "; preState=" + preState
                        + "; postState=" + postState,
                densityBefore, densityAfter);
    }

    /** Counter-test: fuel hatch empty → setOOF(true) → no power consumed,
     *  no progress, no density mutation. Pins the fuel-required branch. */
    @Test
    public void nativePlanetTerraformerWithoutFuelDoesNotStep() throws Exception {
        assertDimIsNativeArPlanet();
        String fixture = buildAndCompleteFixture(CX_NO_FUEL);
        injectPower(fixture, 30_000_000);
        // Deliberately skip fluid injection.
        enableMachine(CX_NO_FUEL);

        int densityBefore = readDensity();
        // Same tick budget as the positive test — proves OOF gate holds
        // for the full window during which the positive test mutates.
        forceTick(CX_NO_FUEL, 24000);
        int densityAfter = readDensity();

        assertEquals("fuel-less terraformer moved density anyway"
                        + " (before=" + densityBefore + " after=" + densityAfter + ")",
                densityBefore, densityAfter);
    }

    /** Counter-test: controller's battery aggregator cleared
     *  ({@code MultiBattery.clear()}) so {@code hasEnergy(powerPerTick)}
     *  reads 0 →  libVulpes' update() skips onRunningPoweredTick →
     *  currentTime never increments → processComplete never fires →
     *  density unchanged. Pins the power-required branch.
     *
     *  <p><b>Why clear-batteries instead of skip-inject</b>: the default
     *  'P'-mapping fixture places creative input plugs whose
     *  {@code TileCreativePowerInput.getUniversalEnergyStored()} returns
     *  {@code Integer.MAX_VALUE >> 4} unconditionally. Skipping
     *  {@code energy inject} still leaves the controller with effectively
     *  infinite aggregated power, so this counter-test wouldn't actually
     *  exercise the no-power branch without the explicit clear.</p> */
    @Test
    public void nativePlanetTerraformerWithoutPowerDoesNotStep() throws Exception {
        assertDimIsNativeArPlanet();
        String fixture = buildAndCompleteFixture(CX_NO_POWER);
        enableMachine(CX_NO_POWER);
        // Wipe the aggregator AFTER integrateTile populated it, so the
        // controller observes an empty battery list each tick.
        String drain = exec("artest machine clear-batteries " + newDim
                + " " + CX_NO_POWER + " " + CY + " " + CZ);
        assertTrue("clear-batteries probe failed: " + drain,
                drain.contains("\"cleared\":true"));

        // Top up fluid each iteration so OOF can't be the cause of any
        // non-progression observed below — power-absence must be the
        // sole reason.
        int densityBefore = readDensity();
        for (int i = 0; i < 60; i++) {
            injectFluidAt(fixture, 0, "nitrogen", 16000);
            injectFluidAt(fixture, 1, "nitrogen", 16000);
            injectFluidAt(fixture, 2, "oxygen", 16000);
            injectFluidAt(fixture, 3, "oxygen", 16000);
            forceTick(CX_NO_POWER, 400);
        }
        int densityAfter = readDensity();

        assertEquals("battery-drained terraformer moved density anyway"
                        + " (before=" + densityBefore + " after=" + densityAfter + ")",
                densityBefore, densityAfter);
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private String buildAndCompleteFixture(int cx) throws Exception {
        String fixture = exec("artest fixture multiblock terraformer "
                + newDim + " " + cx + " " + CY + " " + CZ);
        assertTrue("terraformer fixture build failed: " + fixture,
                fixture.contains("\"ok\":true") && fixture.contains("\"unresolved\":0"));
        String tryComplete = exec("artest machine try-complete "
                + newDim + " " + cx + " " + CY + " " + CZ);
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
                + newDim + " " + px + " " + py + " " + pz + " " + amount);
        assertTrue("energy inject failed: " + resp, resp.contains("\"ok\":true"));
    }

    /** Precondition guard: a freshly-generated AR planet must report as
     *  a native AR dim (controller's {@code processComplete()} gate
     *  requires this). If this assert fires, the planet-generate or
     *  dim-load handshake has regressed and the powered-cycle assertions
     *  below would fail for an irrelevant reason. */
    private void assertDimIsNativeArPlanet() throws Exception {
        String info = exec("artest dim info " + newDim);
        assertTrue("dim info missing isARPlanet:true — " + info,
                info.contains("\"isARPlanet\":true"));
        // The terraformer gate also needs WorldProviderPlanet; the dim
        // info verb reports providerClass.
        assertTrue("dim provider is not WorldProviderPlanet — " + info,
                info.contains("WorldProviderPlanet"));
    }

    /** Injects {@code amount} mB of {@code fluidName} into the
     *  {@code hatchIndex}-th 'L' hatch returned by the fixture probe.
     *  {@code TileFluidHatch} holds one fluid type at a time, so the
     *  terraformer's onRunningPoweredTick (which demands BOTH N2 and O2)
     *  needs N2 in some hatches and O2 in others — see
     *  {@link #nativePlanetTerraformerWithFuelAndPowerStepsDensity}'s
     *  per-hatch loop. */
    private void injectFluidAt(String fixture, int hatchIndex, String fluidName,
                               int amount) throws Exception {
        int[] pos = nthLiquidInputPos(fixture, hatchIndex);
        String resp = exec("artest fluid inject "
                + newDim + " " + pos[0] + " " + pos[1] + " " + pos[2]
                + " " + fluidName + " " + amount);
        assertTrue(fluidName + " inject failed at hatch " + hatchIndex + ": " + resp,
                resp.contains("\"ok\":true"));
    }

    /** Scans the fixture response's {@code liquidInputPositions} array
     *  for the n-th triple. */
    private static int[] nthLiquidInputPos(String fixture, int n) {
        // Slice the substring starting at "liquidInputPositions" so we
        // don't accidentally pick up the back-compat single
        // "liquidInputPos" or unrelated position lists.
        int sectionStart = fixture.indexOf("\"liquidInputPositions\"");
        assertTrue("no liquidInputPositions in fixture response: " + fixture,
                sectionStart >= 0);
        Matcher m = LIQUID_TRIPLE.matcher(fixture);
        m.region(sectionStart, fixture.length());
        for (int i = 0; i <= n; i++) {
            assertTrue("liquidInputPositions has fewer than " + (n + 1)
                    + " hatches: " + fixture, m.find());
        }
        return new int[]{
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))};
    }

    private void enableMachine(int cx) throws Exception {
        String resp = exec("artest machine set-enabled "
                + newDim + " " + cx + " " + CY + " " + CZ + " true");
        assertTrue("machine set-enabled failed: " + resp, resp.contains("\"enabled\":true"));
    }

    private void forceTick(int cx, int ticks) throws Exception {
        String resp = exec("artest tile force-tick "
                + newDim + " " + cx + " " + CY + " " + CZ + " " + ticks);
        assertTrue("force-tick errored: " + resp, resp.contains("\"ok\":true"));
    }

    private int readDensity() throws Exception {
        String info = exec("artest terraforming info " + newDim);
        Matcher m = CURRENT_ATMOS.matcher(info);
        assertTrue("no currentAtmosphere in terraforming info: " + info, m.find());
        return Integer.parseInt(m.group(1));
    }

    private static Set<Integer> arDims() throws Exception {
        String list = exec("ar planet list");
        Set<Integer> ids = new HashSet<>();
        Matcher m = DIM_LINE.matcher(list);
        while (m.find()) ids.add(Integer.parseInt(m.group(1)));
        return ids;
    }
}
