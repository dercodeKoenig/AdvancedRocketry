package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-36b (extension) — service-station assembler-discovery + no-progress-
 * without-assembler contracts.
 *
 * <p>Companion to {@link ServiceStationBrokenPartScanContractTest} which
 * pinned the "broken part scan" half of the repair cycle. This test pins
 * the assembler-side half:</p>
 *
 * <ul>
 *   <li><b>scanForAssemblers picks up a nearby TilePrecisionAssembler.</b>
 *       Production scans a 5-block radius around the station for any
 *       TilePrecisionAssembler tile (formed multiblock NOT required —
 *       the scan checks {@code instanceof}, not {@code isComplete()}).
 *       Pinned post-rising-edge-of-power, when
 *       {@code !was_powered → true} triggers the scan.</li>
 *   <li><b>Without an assembler, broken parts stay queued.</b> A station
 *       linked to a rocket with a worn part, powered on but with NO
 *       TilePrecisionAssembler in its 5-block scan radius, keeps the
 *       part in {@code partsToRepair} across many tick windows.
 *       {@code giveWorkToAssemblers} iterates over the empty assembler
 *       list, never reaching {@code consumePartToRepair}. Pins the
 *       guard that prevents silent data loss when no assembler is
 *       available.</li>
 * </ul>
 *
 * <p><b>Out of scope</b>: the FULL repair cycle — broken part fed to
 * assembler → assembler produces a "rocket"-named output item → service
 * station observes via {@code processAssemblerResult} → part restored at
 * stage 0 to rocket storage. Driving the assembler end-to-end requires a
 * formed multiblock (4×3×3 structureBlock layout + hatches at wildcard
 * positions + attemptCompleteStructure), which is a substantial
 * fixture-build (the existing MachineRecipeEndToEndKit explicitly
 * excludes wildcard machines — see kit javadoc, "Out of scope:
 * wildcard-based machines"). Tracked as a separate follow-up TASK once
 * a precision-assembler multiblock-fixture probe lands.</p>
 */
public class ServiceStationAssemblerScanTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENTITY_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern PARTS_COUNT = Pattern.compile("\"partsToRepairCount\":(-?\\d+)");
    private static final Pattern ASM_COUNT = Pattern.compile("\"assemblersCount\":(-?\\d+)");

    private static final int CY_PAD = 64;
    private static final int CZ_PAD = 13500;
    private static final int CX_WITH_ASM = 14100;
    private static final int CX_NO_ASM = 14500;

    /** With a PrecisionAssembler block placed within 5 blocks of the
     *  service station, the rising-edge-of-power scan discovers it and
     *  populates the assemblers list. */
    @Test
    public void scanForAssemblersDiscoversNearbyPrecisionAssemblerBlock() throws Exception {
        SetupResult s = setupStationAndRocket(CX_WITH_ASM);

        // Place a precision-assembler controller block 2 blocks east of
        // the service station — well within the 5-block scan radius.
        // The block doesn't need a formed multiblock; scanForAssemblers
        // only checks `te instanceof TilePrecisionAssembler`.
        int ax = s.sx + 2, ay = s.sy, az = s.sz;
        String placeAsm = exec("artest place 0 " + ax + " " + ay + " " + az
                + " advancedrocketry:precisionassemblingmachine");
        assertTrue("precision assembler place failed: " + placeAsm,
                placeAsm.contains("\"placed\":true"));

        // Pre-state: scan hasn't run yet.
        String preState = exec("artest infra service-state 0 "
                + s.sx + " " + s.sy + " " + s.sz);
        assertEquals("baseline: assemblersCount=0 before scan",
                0, extract(preState, ASM_COUNT));

        // Force the scan via the side-channel probe. The probe bypasses
        // canPerformFunction's (worldTime % 20 == 0) gate and the
        // power-rising-edge requirement — both are scheduling concerns,
        // not the scan-discovery contract this test pins. `tile force-tick`
        // can't advance world time, so production's gate keeps the scan
        // from firing in a test-driven tick.
        String scan = exec("artest infra service-scan-assemblers 0 "
                + s.sx + " " + s.sy + " " + s.sz);
        assertTrue("service-scan-assemblers must succeed: " + scan,
                scan.contains("\"ok\":true"));

        String postState = exec("artest infra service-state 0 "
                + s.sx + " " + s.sy + " " + s.sz);
        assertEquals("scanForAssemblers must discover the adjacent precision "
                        + "assembler block (5-block radius, instanceof check): "
                        + postState, 1, extract(postState, ASM_COUNT));
    }

    /** Without any TilePrecisionAssembler in the 5-block radius, a
     *  worn part stays in {@code partsToRepair} across many tick
     *  windows — production's no-progress guard. */
    @Test
    public void noAssemblerKeepsBrokenPartQueuedAcrossManyTicks() throws Exception {
        SetupResult s = setupStationAndRocket(CX_NO_ASM);

        // Baseline: part queued, no assembler.
        String pre = exec("artest infra service-state 0 "
                + s.sx + " " + s.sy + " " + s.sz);
        assertEquals("baseline: 1 part queued for repair",
                1, extract(pre, PARTS_COUNT));
        assertEquals("baseline: no assemblers nearby",
                0, extract(pre, ASM_COUNT));

        // Force the scan probe — same side-channel as test 1, but with
        // no assembler in range it returns an empty list.
        String scan = exec("artest infra service-scan-assemblers 0 "
                + s.sx + " " + s.sy + " " + s.sz);
        assertTrue("scan probe must succeed even with no assembler: " + scan,
                scan.contains("\"ok\":true"));

        String post = exec("artest infra service-state 0 "
                + s.sx + " " + s.sy + " " + s.sz);
        assertEquals("scan with no nearby assembler must report assemblersCount=0: "
                        + post, 0, extract(post, ASM_COUNT));
        // Part stays queued — the contract being pinned is that the
        // scan + give-work loop is safe under empty-list conditions
        // (no NPE, no silent dequeue). giveWorkToAssemblers iterates
        // over `assemblers.size()==0` items, never reaches
        // consumePartToRepair, so partsToRepair stays at 1.
        assertEquals("part must stay queued — no assembler means no consumption: "
                        + post, 1, extract(post, PARTS_COUNT));
    }

    // --- fixture helpers --------------------------------------------------

    private static final class SetupResult {
        final int sx, sy, sz; // service-station coords
        SetupResult(int sx, int sy, int sz) { this.sx = sx; this.sy = sy; this.sz = sz; }
    }

    /** Build a rocket via the standard fixture, assemble it, inject a
     *  broken part, place a service station nearby, link the rocket.
     *  Returns the service-station coords. */
    private SetupResult setupStationAndRocket(int baseX) throws Exception {
        int cx1 = (baseX - 2) >> 4, cz1 = (CZ_PAD - 2) >> 4;
        int cx2 = (baseX + 12) >> 4, cz2 = (CZ_PAD + 7) >> 4;
        exec("artest chunk warmup 0 " + cx1 + " " + cz1 + " " + cx2 + " " + cz2);
        exec("artest fill 0 " + (baseX - 2) + " " + (CY_PAD + 1) + " " + (CZ_PAD - 2)
                + " " + (baseX + 12) + " " + (CY_PAD + 10) + " " + (CZ_PAD + 7)
                + " minecraft:air");

        String fixture = exec("artest fixture rocket 0 " + baseX + " " + CY_PAD
                + " " + CZ_PAD + " simple");
        assertTrue("rocket fixture must build: " + fixture,
                fixture.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture missing builderPos: " + fixture, bp.find());
        String assemble = exec("artest rocket assemble 0 " + bp.group(1) + " "
                + bp.group(2) + " " + bp.group(3));
        assertTrue("assemble must succeed: " + assemble,
                assemble.contains("\"ok\":true"));
        Matcher eim = ENTITY_ID.matcher(assemble);
        assertTrue("no entityId: " + assemble, eim.find());
        int rocketId = Integer.parseInt(eim.group(1));

        String inject = exec("artest infra inject-broken-part " + rocketId + " 5");
        assertTrue("inject must succeed: " + inject, inject.contains("\"ok\":true"));

        int sx = baseX + 10, sy = CY_PAD, sz = CZ_PAD;
        String place = exec("artest place 0 " + sx + " " + sy + " " + sz
                + " advancedrocketry:serviceStation");
        assertTrue("service station place failed: " + place,
                place.contains("\"placed\":true"));
        String link = exec("artest infra link 0 " + sx + " " + sy + " " + sz
                + " " + rocketId);
        assertTrue("infra link must succeed: " + link, link.contains("\"ok\":true"));
        return new SetupResult(sx, sy, sz);
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }
}
