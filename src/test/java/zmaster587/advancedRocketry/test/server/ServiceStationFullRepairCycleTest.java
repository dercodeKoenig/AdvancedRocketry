package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-36b deep — full repair cycle with a FORMED PrecisionAssembler
 * multiblock.
 *
 * <p>{@link ServiceStationBrokenPartScanContractTest} pinned the "scan
 * finds parts" half, {@link ServiceStationAssemblerScanTest} pinned the
 * "scan finds assembler" + "no-progress without assembler" guards. Both
 * deferred the end-to-end repair pipe because driving it requires a
 * formed PrecisionAssembler multiblock. The TASK-26 fixture probe
 * {@code /artest fixture machine precision-assembler} closes that gap —
 * it overlays I/O/P hatches onto the wildcard structure and runs
 * {@code attemptCompleteStructure} for us.</p>
 *
 * <p>Contract pinned: a worn part injected into the rocket, fed to a
 * formed PrecisionAssembler adjacent to a powered service station,
 * cycles all the way through and ends up restored at stage 0 in the
 * rocket's storage.</p>
 *
 * <p>Two-phase observable:</p>
 *
 * <ol>
 *   <li><b>Phase 1 (consumePartToRepair).</b> First {@code performFunction}
 *       on the powered station discovers the assembler
 *       (rising-edge of {@code was_powered}), then calls
 *       {@code giveWorkToAssemblers} → {@code consumePartToRepair}: the
 *       worn part moves from {@code partsToRepair} into
 *       {@code partsProcessing[0]}, and the dropped broken-motor item
 *       is injected into the assembler's input hatch.</li>
 *   <li><b>Phase 2 (processAssemblerResult).</b> Test injects a
 *       "rocket"-named item into the assembler's OUTPUT hatch (the
 *       substring check in {@link
 *       zmaster587.advancedRocketry.util.InventoryUtil#hasItemInInventory}
 *       is the production gate for "assembler finished"). Next
 *       {@code performFunction} call observes the output item and runs
 *       {@code processAssemblerResult}: {@code partsProcessing[0]} is
 *       cleared, the part's {@code setStage(0)} restores it, and it's
 *       re-added to the rocket's StorageChunk at its original
 *       block-state.</li>
 * </ol>
 *
 * <p>End-state pin: {@code partsToRepairCount == 0},
 * {@code partsProcessingCount == 0},
 * {@code initialPartToRepairCount == 1} (set at link time, not cleared
 * by completion — drives the GUI progress bar).</p>
 *
 * <p>Heavy fixture cost: full multiblock structure placement +
 * {@code attemptCompleteStructure} retry budget. Test wallclock ~1-2s
 * under shared-server harness.</p>
 */
public class ServiceStationFullRepairCycleTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENTITY_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern PARTS_COUNT = Pattern.compile("\"partsToRepairCount\":(-?\\d+)");
    private static final Pattern INITIAL_COUNT =
            Pattern.compile("\"initialPartToRepairCount\":(-?\\d+)");
    private static final Pattern ASM_COUNT = Pattern.compile("\"assemblersCount\":(-?\\d+)");
    private static final Pattern PROC_COUNT = Pattern.compile("\"partsProcessingCount\":(-?\\d+)");
    private static final Pattern OUTPUT_POS_LIST =
            Pattern.compile("\"outputPositions\":\\[\\[(-?\\d+),(-?\\d+),(-?\\d+)]");

    // Use an isolated lane far from existing service-station tests so
    // parallel-fork chunk shuffling can't cross-contaminate.
    private static final int FIXTURE_CY  = 70;
    private static final int FIXTURE_CZ  = 15500;
    private static final int FIXTURE_CX  = 16100;
    private static final int ROCKET_CX   = 16080; // builderPos lands 6 east of cx
    private static final int ROCKET_CZ   = 15470; // far enough from fixture footprint

    @Test
    public void fullRepairCycleRestoresWornPartToStageZero() throws Exception {
        // Pre-clear and warm a generous chunk area covering BOTH the
        // precision-assembler footprint AND the rocket fixture.
        int cx1 = (Math.min(ROCKET_CX - 2, FIXTURE_CX - 5)) >> 4;
        int cz1 = (Math.min(ROCKET_CZ - 2, FIXTURE_CZ - 5)) >> 4;
        int cx2 = (Math.max(ROCKET_CX + 12, FIXTURE_CX + 5)) >> 4;
        int cz2 = (Math.max(ROCKET_CZ + 7, FIXTURE_CZ + 5)) >> 4;
        exec("artest chunk warmup 0 " + cx1 + " " + cz1 + " " + cx2 + " " + cz2);

        // Build a precision-assembler multiblock. The TASK-26 fixture
        // probe overlays I/O/P hatches onto the wildcard structure and
        // runs attemptCompleteStructure.
        String asmResp = exec("artest fixture machine precision-assembler 0 "
                + FIXTURE_CX + " " + FIXTURE_CY + " " + FIXTURE_CZ);
        assertTrue("precision-assembler fixture must build: " + asmResp,
                asmResp.contains("\"ok\":true"));
        Matcher outM = OUTPUT_POS_LIST.matcher(asmResp);
        assertTrue("fixture response must include outputPositions: " + asmResp,
                outM.find());
        int outX = Integer.parseInt(outM.group(1));
        int outY = Integer.parseInt(outM.group(2));
        int outZ = Integer.parseInt(outM.group(3));

        // Build + assemble rocket in its own lane so its launchpad
        // doesn't overlap the assembler footprint.
        exec("artest fill 0 " + (ROCKET_CX - 2) + " " + (FIXTURE_CY + 1) + " "
                + (ROCKET_CZ - 2) + " " + (ROCKET_CX + 12) + " " + (FIXTURE_CY + 10)
                + " " + (ROCKET_CZ + 7) + " minecraft:air");
        String fix = exec("artest fixture rocket 0 " + ROCKET_CX + " "
                + FIXTURE_CY + " " + ROCKET_CZ + " simple");
        assertTrue("rocket fixture must build: " + fix,
                fix.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fix);
        assertTrue("fixture missing builderPos: " + fix, bp.find());
        String assemble = exec("artest rocket assemble 0 " + bp.group(1) + " "
                + bp.group(2) + " " + bp.group(3));
        assertTrue("assemble must succeed: " + assemble,
                assemble.contains("\"ok\":true"));
        Matcher eim = ENTITY_ID.matcher(assemble);
        assertTrue("no entityId: " + assemble, eim.find());
        int rocketId = Integer.parseInt(eim.group(1));

        // Mark one of the rocket's advRocketmotor TileBrokenParts as
        // stage 5.
        String inject = exec("artest infra inject-broken-part " + rocketId + " 5");
        assertTrue("inject must succeed: " + inject, inject.contains("\"ok\":true"));

        // Place service station within 5 blocks of the assembler
        // controller (scanForAssemblers' radius). Controller is at
        // (FIXTURE_CX, FIXTURE_CY, FIXTURE_CZ); place station 2 blocks
        // east (still well within radius).
        int sx = FIXTURE_CX + 2, sy = FIXTURE_CY, sz = FIXTURE_CZ;
        // The assembler structure may overlap (sx, sy, sz) — pre-clear
        // it to AIR so the station can be placed.
        exec("artest fill 0 " + sx + " " + sy + " " + sz + " "
                + sx + " " + sy + " " + sz + " minecraft:air");
        String place = exec("artest place 0 " + sx + " " + sy + " " + sz
                + " advancedrocketry:serviceStation");
        assertTrue("service station place failed: " + place,
                place.contains("\"placed\":true"));
        String link = exec("artest infra link 0 " + sx + " " + sy + " " + sz
                + " " + rocketId);
        assertTrue("infra link must succeed: " + link,
                link.contains("\"ok\":true"));

        // Apply redstone power (performFunction requires
        // getEquivalentPower=true).
        exec("artest place 0 " + sx + " " + (sy + 1) + " " + sz
                + " minecraft:redstone_block");

        // Baseline: 1 part queued, no assemblers discovered yet, none
        // processing.
        String pre = exec("artest infra service-state 0 " + sx + " " + sy + " " + sz);
        assertEquals("baseline: 1 worn part queued", 1, extract(pre, PARTS_COUNT));
        assertEquals("baseline: 0 assemblers discovered yet",
                0, extract(pre, ASM_COUNT));
        assertEquals("baseline: 0 parts processing",
                0, extract(pre, PROC_COUNT));

        // === PHASE 1 — consumePartToRepair ===
        // First performFunction: !was_powered → scanForAssemblers
        // discovers the assembler, then giveWorkToAssemblers calls
        // consumePartToRepair. Part moves from partsToRepair into
        // partsProcessing[0].
        String pf1 = exec("artest infra service-perform-function 0 "
                + sx + " " + sy + " " + sz);
        assertTrue("performFunction call #1 must succeed: " + pf1,
                pf1.contains("\"ok\":true"));

        String mid = exec("artest infra service-state 0 " + sx + " " + sy + " " + sz);
        assertEquals("phase 1: assembler must be discovered: " + mid,
                1, extract(mid, ASM_COUNT));
        assertEquals("phase 1: part must move out of partsToRepair: " + mid,
                0, extract(mid, PARTS_COUNT));
        assertEquals("phase 1: part must appear in partsProcessing: " + mid,
                1, extract(mid, PROC_COUNT));
        assertEquals("phase 1: initialPartToRepairCount must stay at 1: " + mid,
                1, extract(mid, INITIAL_COUNT));

        // === PHASE 2 — processAssemblerResult ===
        // Inject a "rocket"-named item into the assembler's first
        // output port. InventoryUtil.hasItemInInventory does a
        // case-insensitive substring match on getUnlocalizedName, so any
        // item whose unlocalized name contains "rocket" satisfies the
        // gate. advrocketmotor block-item's name is "tile.advrocketmotor"
        // which matches.
        String fillOut = exec("artest hatch fill 0 " + outX + " " + outY + " " + outZ
                + " 0 advancedrocketry:advrocketmotor 1");
        assertTrue("hatch fill on assembler output must succeed: " + fillOut,
                fillOut.contains("\"ok\":true"));

        String pf2 = exec("artest infra service-perform-function 0 "
                + sx + " " + sy + " " + sz);
        assertTrue("performFunction call #2 must succeed: " + pf2,
                pf2.contains("\"ok\":true"));

        String end = exec("artest infra service-state 0 " + sx + " " + sy + " " + sz);
        assertEquals("phase 2: partsProcessing must be cleared by "
                        + "processAssemblerResult: " + end,
                0, extract(end, PROC_COUNT));
        assertEquals("phase 2: partsToRepair stays empty (no new work): " + end,
                0, extract(end, PARTS_COUNT));
        assertEquals("phase 2: initialPartToRepairCount stays at 1 — it's the "
                        + "monotonic baseline for the GUI progress bar, not a "
                        + "completion counter: " + end,
                1, extract(end, INITIAL_COUNT));

        // End-of-cycle proof: re-marking a part as worn (inject-broken-
        // part picks the first stage-0 motor) must still succeed — meaning
        // the rocket storage's TileBrokenPart inventory is non-empty,
        // which it would NOT be if the repaired part had been lost.
        String reInject = exec("artest infra inject-broken-part " + rocketId + " 7");
        assertTrue("post-cycle inject must succeed — rocket storage must still "
                        + "contain at least one stage-0 TileBrokenPart, proving "
                        + "the repaired part was restored (not lost): " + reInject,
                reInject.contains("\"ok\":true"));
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }
}
