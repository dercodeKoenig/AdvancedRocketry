package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * Coverage-audit gap (post-TASK-26) — counter-test side of the
 * {@code TileOxygenVent} functional cycle.
 *
 * <p>{@link zmaster587.advancedRocketry.test.server.MachineDomainSmokeSuite#sealedRoomBecomesBreathableThenLeaks}
 * already pins the happy path: vent + oxygen + energy + sealed room →
 * blob populates, atmosphere becomes pressurised. The audit gap was
 * the <b>counter-branches</b> — what happens when one of the two
 * required inputs is missing.</p>
 *
 * <p>Production gate at {@code TileOxygenVent.update}:{@code 286-294}:</p>
 *
 * <pre>{@code
 *   if (canPerformFunction()) {
 *       if (hasEnoughEnergy(getPowerPerOperation())) {
 *           performFunction();
 *           if (!world.isRemote && isSealed) energy.extractEnergy(...);
 *       } else notEnoughEnergyForFunction();
 *   }
 * }</pre>
 *
 * <p>Plus the fluid drain at {@code performFunction:255-273}: if the
 * tank can't yield the requested O2 amount, the vent un-seals and the
 * atmosphere reverts to the dim's baseline. Pins three contracts:</p>
 *
 * <ul>
 *   <li>energy-required: no power → no seal regardless of fluid.</li>
 *   <li>fluid-required: no oxygen → no seal regardless of power.</li>
 *   <li>active-cycle drain: with both inputs and sealed state, energy
 *       buffer decreases over ticks (the {@code extractEnergy} call
 *       isn't a no-op).</li>
 * </ul>
 */
public class OxygenVentRequiresFuelAndPowerTest extends AbstractSharedServerTest {

    private static final Pattern VENT_SEALED = Pattern.compile("\"isSealed\":(true|false)");
    private static final Pattern VENT_BLOB_SIZE = Pattern.compile("\"blobSize\":(-?\\d+)");
    private static final Pattern VENT_ENERGY = Pattern.compile("\"energyStored\":(-?\\d+)");

    private static final int CY_BASE = 64;
    private static final int CZ_BASE = 2000;
    /** Three test patches, X-spread far enough to avoid any blob-blob
     *  interaction across the shared harness. */
    private static final int CX_NO_FLUID  = 2000;
    private static final int CX_NO_POWER  = 2200;
    private static final int CX_DRAIN     = 2400;

    /** Vent + energy, NO oxygen → fluid drain at {@code performFunction:258}
     *  fails the "drainedFluid != null && >= amtToDrain" guard → vent's
     *  {@code hasFluid} flag flips false AND the atmosphere type reverts
     *  from {@code PRESSURIZEDAIR} to the dim baseline.
     *
     *  <p>Note: {@code isSealed} CAN remain {@code true} in this branch —
     *  production keeps the blob registered while flipping the
     *  atmosphere type to the dim's default. The player-visible
     *  outcome is "the room reverts to outside air, vent shows red
     *  status" not "the vent disconnects". Pin the observable
     *  effects, not the {@code isSealed} flag.</p> */
    @Test
    public void ventWithoutOxygenLosesHasFluidAndRevertsAtmosphere() throws Exception {
        buildSealableRoom(CX_NO_FLUID);
        placeVent(CX_NO_FLUID);
        injectEnergy(CX_NO_FLUID, 1_000_000);
        // Deliberately skip oxygen inject.
        forceTickAndReseal(CX_NO_FLUID);
        // Extra ticks past first-run + reseal so the drain-fail branch
        // has a chance to fire (it gates on getBlobSize > 0 to compute
        // amtToDrain, and the blob registers on tick 1).
        exec("artest tile force-tick 0 " + CX_NO_FLUID + " " + CY_BASE + " " + CZ_BASE + " 20");

        String info = ventInfo(CX_NO_FLUID);
        assertTrue("vent without oxygen must report hasFluid:false after the "
                        + "drain-fail branch fires: " + info,
                info.contains("\"hasFluid\":false"));
        assertFalse("vent without oxygen must NOT report PRESSURIZEDAIR — the "
                        + "atmosphere should have reverted to the dim baseline: "
                        + info,
                info.contains("\"blobAtmosphere\":\"PRESSURIZEDAIR\""));
    }

    /** Vent + oxygen, NO energy → {@code hasEnoughEnergy} guard fails at
     *  {@code update:288} → {@code performFunction} never invoked →
     *  vent never seals. */
    @Test
    public void ventWithoutPowerDoesNotSealEvenWhenFueled() throws Exception {
        buildSealableRoom(CX_NO_POWER);
        placeVent(CX_NO_POWER);
        injectOxygen(CX_NO_POWER, 16000);
        // Deliberately skip energy inject.
        forceTickAndReseal(CX_NO_POWER);

        String info = ventInfo(CX_NO_POWER);
        assertEquals("vent without energy must NOT report sealed: " + info,
                "false", matchOrFail(VENT_SEALED, info));
        assertEquals("vent without energy must have zero blob size: " + info,
                0, extract(info, VENT_BLOB_SIZE));
    }

    /** Vent + oxygen + energy + sealed → active cycle drains the energy
     *  buffer per tick. The drain is the observable side-effect that
     *  proves {@code energy.extractEnergy} actually runs (and that the
     *  {@code if (!world.isRemote && isSealed)} guard isn't silently
     *  short-circuiting on the harness's server-side dim). */
    @Test
    public void poweredFueledSealedVentDrainsEnergyOverTicks() throws Exception {
        buildSealableRoom(CX_DRAIN);
        placeVent(CX_DRAIN);
        injectEnergy(CX_DRAIN, 1_000_000);
        injectOxygen(CX_DRAIN, 16000);
        forceTickAndReseal(CX_DRAIN);

        // Confirm the vent reached the sealed state — without it, the
        // extractEnergy guard wouldn't fire and the test below would
        // pass for the wrong reason.
        String preInfo = ventInfo(CX_DRAIN);
        assertEquals("baseline: vent must be sealed for this counter-test "
                        + "to validate active-cycle drain: " + preInfo,
                "true", matchOrFail(VENT_SEALED, preInfo));
        int energyBefore = extract(preInfo, VENT_ENERGY);

        // Force-tick the vent. Each successful tick where isSealed=true
        // calls extractEnergy(getPowerPerOperation(), false).
        exec("artest tile force-tick 0 " + CX_DRAIN + " " + CY_BASE + " " + CZ_BASE + " 50");

        String postInfo = ventInfo(CX_DRAIN);
        int energyAfter = extract(postInfo, VENT_ENERGY);
        assertTrue("powered+fueled+sealed vent must drain energy over ticks "
                        + "(before=" + energyBefore + " after=" + energyAfter + "): "
                        + postInfo,
                energyAfter < energyBefore);
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private void buildSealableRoom(int cx) throws Exception {
        int by = CY_BASE, bz = CZ_BASE;
        // Floor slab.
        exec("artest fill 0 " + (cx - 2) + " " + (by - 1) + " " + (bz - 2)
                + " " + (cx + 2) + " " + by + " " + (bz + 2) + " minecraft:stone");
        // Walls + interior air for y+1, y+2.
        for (int yy = by + 1; yy <= by + 2; yy++) {
            exec("artest fill 0 " + (cx - 2) + " " + yy + " " + (bz - 2)
                    + " " + (cx + 2) + " " + yy + " " + (bz + 2) + " minecraft:stone");
            exec("artest fill 0 " + (cx - 1) + " " + yy + " " + (bz - 1)
                    + " " + (cx + 1) + " " + yy + " " + (bz + 1) + " minecraft:air");
        }
        // Roof.
        exec("artest fill 0 " + (cx - 2) + " " + (by + 3) + " " + (bz - 2)
                + " " + (cx + 2) + " " + (by + 3) + " " + (bz + 2) + " minecraft:stone");
    }

    private void placeVent(int cx) throws Exception {
        String resp = exec("artest place 0 " + cx + " " + CY_BASE + " " + CZ_BASE
                + " advancedrocketry:oxygenVent");
        assertTrue("vent place failed: " + resp, resp.contains("\"placed\":true"));
    }

    private void injectEnergy(int cx, int amount) throws Exception {
        String resp = exec("artest energy inject 0 " + cx + " " + CY_BASE + " " + CZ_BASE
                + " " + amount);
        assertTrue("energy inject failed: " + resp, resp.contains("\"ok\":true"));
    }

    private void injectOxygen(int cx, int amount) throws Exception {
        String resp = exec("artest fluid inject 0 " + cx + " " + CY_BASE + " " + CZ_BASE
                + " oxygen " + amount);
        assertTrue("oxygen inject failed: " + resp, resp.contains("\"ok\":true"));
    }

    /** Wakes the vent from "first run" state into its operating loop and
     *  forces an attempt to seal — mirrors the pattern from the existing
     *  {@code sealedRoomBecomesBreathableThenLeaks} test in
     *  {@link MachineDomainSmokeSuite}. */
    private void forceTickAndReseal(int cx) throws Exception {
        exec("artest tile force-tick 0 " + cx + " " + CY_BASE + " " + CZ_BASE + " 1");
        exec("artest vent reseal 0 " + cx + " " + CY_BASE + " " + CZ_BASE);
        exec("artest tile force-tick 0 " + cx + " " + CY_BASE + " " + CZ_BASE + " 5");
    }

    private String ventInfo(int cx) throws Exception {
        return exec("artest vent info 0 " + cx + " " + CY_BASE + " " + CZ_BASE);
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }

    private static String matchOrFail(Pattern pattern, String src) {
        Matcher m = pattern.matcher(src);
        assertFalse("pattern " + pattern.pattern() + " not found in: " + src,
                !m.find());
        return m.group(1);
    }
}
