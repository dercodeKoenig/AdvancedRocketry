package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-04 Phase 4 — Black Hole Generator multiblock validation.
 *
 * <p>Pins the production {@code TileBlackHoleGenerator.completeStructure}
 * path: with all blocks in place per the 5×3×3 structure
 * (controller + 5 advStructureMachine + 1 power-output plug + 1 item-input
 * hatch + 1 advStructureMachine filler), {@code attemptCompleteStructure}
 * must accept the layout. Breaking any required block must flip
 * {@code isComplete} back to false.</p>
 *
 * <p>Uses the new {@code /artest fixture multiblock blackhole-gen} probe
 * (handleFixtureBlackHoleGenerator in TestProbeCommand) — first concrete
 * use of the libVulpes structure-block char-mapping outside the existing
 * cutting-machine fixture.</p>
 *
 * <p>Position-isolated patch at x=3000 (no collision with existing test
 * fixtures up to x ≈ 2700).</p>
 */
public class BlackHoleGeneratorMultiblockTest extends AbstractSharedServerTest {

    private static final int CX = 3000;
    private static final int CY = 64;
    private static final int CZ = 3000;

    @Test
    public void blackHoleGeneratorMultiblockValidatesWhenFixtureIsBuilt() throws Exception {
        // Build the multiblock via the new fixture probe.
        String fixture = join(client().execute(
                "artest fixture multiblock blackhole-gen 0 " + CX + " " + CY + " " + CZ));
        assertTrue("fixture multiblock blackhole-gen failed: " + fixture,
                fixture.contains("\"ok\":true"));

        // Sanity: controller is the right tile class.
        String info = join(client().execute(
                "artest machine info 0 " + CX + " " + CY + " " + CZ));
        assertTrue("expected TileBlackHoleGenerator tile at controller pos: " + info,
                info.contains("TileBlackHoleGenerator"));

        // Diagnostic: dump every fixture position so we can see exactly what's
        // there if validation fails (e.g. wrong block resolved, wrong meta).
        StringBuilder layout = new StringBuilder("\nfixture layout:\n");
        int[][] positions = {
                {CX,     CY,     CZ},      // controller
                {CX,     CY + 1, CZ + 1},  // topCap
                {CX,     CY,     CZ + 1},  // centre
                {CX,     CY - 1, CZ},      // lower1Front
                {CX,     CY - 1, CZ + 1},  // lower1Mid
                {CX,     CY - 2, CZ + 1},  // lower2
                {CX,     CY - 3, CZ + 1},  // lower3
                {CX + 1, CY,     CZ + 1},  // powerOutPos
                {CX - 1, CY,     CZ + 1},  // itemInputPos
                {CX,     CY,     CZ + 2},  // backFiller
        };
        for (int[] p : positions) {
            String b = join(client().execute(
                    "artest block at 0 " + p[0] + " " + p[1] + " " + p[2]));
            layout.append("  (").append(p[0]).append(',').append(p[1]).append(',').append(p[2])
                    .append("): ").append(b).append('\n');
        }

        // try-complete: production attemptCompleteStructure must accept the layout.
        String tryComplete = join(client().execute(
                "artest machine try-complete 0 " + CX + " " + CY + " " + CZ));
        assertTrue("try-complete probe errored: " + tryComplete,
                tryComplete.contains("\"ok\":true"));
        assertTrue("BHG multiblock didn't validate (isComplete=false): " + tryComplete + layout,
                tryComplete.contains("\"isComplete\":true"));
    }

    @Test
    public void blackHoleGeneratorMultiblockInvalidatesWhenStructureBreaks() throws Exception {
        // Independent fixture patch — shifted by 30 blocks east to avoid
        // collision with the previous method's structure.
        int cx = CX + 30, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock blackhole-gen 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture build failed: " + fixture, fixture.contains("\"ok\":true"));

        // First validate — should pass.
        String first = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("baseline try-complete should pass: " + first,
                first.contains("\"isComplete\":true"));

        // Break the lower1Mid column block (directly under centre at y=cy-1).
        // Production validator must notice and flip isComplete back to false.
        String breakBlock = join(client().execute(
                "artest place 0 " + cx + " " + (cy - 1) + " " + (cz + 1) + " minecraft:air"));
        assertTrue("could not replace lower1 with air: " + breakBlock,
                breakBlock.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("try-complete after break errored: " + broken,
                broken.contains("\"ok\":true"));
        assertTrue("structure stayed complete after column block removal — "
                        + "validator broken: " + broken,
                broken.contains("\"isComplete\":false"));
    }

    @Test
    public void powerOutputPlugExposesEnergyCapacityAfterFormation() throws Exception {
        // Position-isolated patch.
        int cx = CX + 60, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock blackhole-gen 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture build failed: " + fixture, fixture.contains("\"ok\":true"));

        // Form it so the controller's MultiBattery wires up the output plug.
        String formed = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("formation must succeed: " + formed,
                formed.contains("\"isComplete\":true"));

        // The forgePowerOutput plug at (cx+1, cy, cz+1) must expose an
        // IEnergyStorage capability with non-zero max. After formation the
        // libVulpes MultiBattery routes the controller's per-multiblock
        // capacity through this plug; a regression that drops the energy-
        // capability wiring would silently make the BHG un-drainable.
        int px = cx + 1, py = cy, pz = cz + 1;
        String energy = join(client().execute(
                "artest energy stored 0 " + px + " " + py + " " + pz));
        assertTrue("power output plug must expose IEnergyStorage: " + energy,
                energy.contains("\"hasEnergy\":true"));
        // Capacity is configured per-controller; just assert non-zero —
        // the exact value depends on AR config defaults.
        Matcher m = Pattern.compile("\"energyMax\":(\\d+)").matcher(energy);
        assertTrue("could not parse energyMax: " + energy, m.find());
        long capacity = Long.parseLong(m.group(1));
        assertTrue("formed BHG must have non-zero energy capacity at the "
                        + "output plug; got energyMax=" + capacity + " response=" + energy,
                capacity > 0L);
    }

    @Test
    public void formedBhgInOverworldStaysIdleWithoutBlackHole_documentsContract() throws Exception {
        // Production contract: TileBlackHoleGenerator.update only produces
        // energy when isAroundBlackHole() returns true, i.e. the controller
        // is in the space dimension AND on a space station whose parent
        // star is classified as a black hole. The test harness runs in dim
        // 0 (overworld); the guard must fire and keep powerMadeLastTick=0
        // even with a valid formation and force-ticks. A regression that
        // removes the guard would let BHGs produce free power on any dim.
        int cx = CX + 90, cy = CY, cz = CZ;
        String fixture = join(client().execute(
                "artest fixture multiblock blackhole-gen 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture build failed: " + fixture, fixture.contains("\"ok\":true"));

        String formed = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("formation must succeed: " + formed,
                formed.contains("\"isComplete\":true"));

        // Drive many controller updates — production update() consults
        // isAroundBlackHole() each call. With no black-hole context, the
        // guard returns false and powerMadeLastTick must stay 0.
        String tick = join(client().execute(
                "artest tile force-tick 0 " + cx + " " + cy + " " + cz + " 100"));
        assertTrue("force-tick must complete without exception: " + tick,
                tick.contains("\"ok\":true"));

        // Energy stored at the output plug must remain 0 (no production).
        int px = cx + 1, py = cy, pz = cz + 1;
        String energy = join(client().execute(
                "artest energy stored 0 " + px + " " + py + " " + pz));
        Matcher m = Pattern.compile("\"energyStored\":(\\d+)").matcher(energy);
        assertTrue("could not parse energyStored: " + energy, m.find());
        long stored = Long.parseLong(m.group(1));
        assertEquals("BHG in overworld must NOT produce power "
                + "(isAroundBlackHole guard); response=" + energy,
                0L, stored);
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
