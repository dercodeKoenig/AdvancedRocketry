package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-36b — service-station broken-part scan contract.
 *
 * <p>Follow-up to {@link RocketServiceStationLinkAndStateTest} which left
 * the full repair cycle deferred ("requires fixture infrastructure for
 * injecting TileBrokenPart instances with stage&gt;0"). The
 * {@code /artest infra inject-broken-part} probe lands that fixture; this
 * test pins the SCAN half of the contract — the half that runs
 * unconditionally on every link / re-link, with no dependence on a
 * PrecisionAssembler being present.</p>
 *
 * <p>Contract pinned:</p>
 * <ul>
 *   <li><b>Inject + link → scan finds it.</b> A rocket whose storage
 *       contains a TileBrokenPart with stage&gt;0 is reported as having
 *       {@code partsToRepairCount == 1} after the service station links
 *       to it. This is what {@code updateRepairList()} guarantees and
 *       what every downstream repair step depends on. A regression that
 *       drops the stage&gt;0 filter, or fails to read storage tiles, fires
 *       here.</li>
 *   <li><b>Multi-part scan.</b> Two parts with stage&gt;0 produce
 *       {@code partsToRepairCount == 2}. Pins that the scan is not a
 *       first-match short-circuit.</li>
 *   <li><b>Post-link injection requires re-scan.</b> If the rocket is
 *       already linked, marking a part as worn does NOT change
 *       {@code partsToRepairCount} until the station re-runs
 *       {@code updateRepairList()}. Pins the lifecycle: production walks
 *       this path on link, not every tick. The {@code service-relink}
 *       probe exposes the re-scan side-channel; this test pins both the
 *       no-effect-without-rescan and the with-rescan branches in one
 *       observation.</li>
 * </ul>
 *
 * <p><b>Note on unlink:</b> {@code TileRocketServiceStation.unlinkRocket()}
 * (which calls {@code dropRepairStats}) only fires from the tile's own
 * {@code invalidate()} path — i.e. when the block is broken / unloaded.
 * The {@code /artest infra unlink} probe path (mirroring
 * {@code EntityRocketBase.unlinkInfrastructure} from the rocket side)
 * does NOT trigger the back-callback; station state persists with stale
 * {@code partsToRepair} and a dangling {@code linkedRocket} reference
 * until invalidate. This is impl-detail of the link contract direction
 * and is intentionally NOT pinned here — a cleanup-on-unlink test would
 * pin the probe semantics, not a player-visible contract.</p>
 *
 * <p><b>Still deferred</b>: the WITH-assembler half of the cycle — broken
 * part fed to PrecisionAssembler, processed item returned, part restored
 * to stage 0 + re-attached to rocket storage. That path needs a fixture
 * for placing assemblers with a valid recipe and is left for a follow-up
 * once the recipe surface is auditable.</p>
 */
public class ServiceStationBrokenPartScanContractTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENTITY_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern PART_POS =
            Pattern.compile("\"partPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern PARTS_COUNT = Pattern.compile("\"partsToRepairCount\":(-?\\d+)");
    private static final Pattern INITIAL_COUNT =
            Pattern.compile("\"initialPartToRepairCount\":(-?\\d+)");
    private static final Pattern STAGE = Pattern.compile("\"stage\":(-?\\d+)");

    // Isolation lanes — keep each test on its own block of coordinates so
    // parallel-fork chunk shuffling can't cross-contaminate.
    private static final int CY_PAD          = 64;
    private static final int CZ_PAD          = 7400;
    private static final int CX_SINGLE       = 8100;
    private static final int CX_MULTI        = 8500;
    private static final int CX_POST_LINK    = 8900;

    /** Mark one rocket motor as worn, link → scan picks it up exactly
     *  once. Also pins {@code initialPartToRepairCount} = 1 (the
     *  monotonic baseline counter that drives the GUI progress bar). */
    @Test
    public void injectedBrokenPartAppearsInPartsToRepairAfterLink() throws Exception {
        RocketFixture rf = buildAndAssembleRocket(CX_SINGLE);

        String inject = exec("artest infra inject-broken-part " + rf.rocketId + " 5");
        assertTrue("inject must succeed for advRocketmotor (simple variant has 2): "
                        + inject, inject.contains("\"ok\":true"));
        assertEquals("inject must report stage=5", 5, extract(inject, STAGE));
        Matcher pp = PART_POS.matcher(inject);
        assertTrue("inject must report partPos: " + inject, pp.find());

        // Place + link AFTER injection so updateRepairList() picks it up.
        int sx = CX_SINGLE + 10, sy = CY_PAD, sz = CZ_PAD;
        placeServiceStation(sx, sy, sz);
        linkStation(sx, sy, sz, rf.rocketId);

        String state = exec("artest infra service-state 0 " + sx + " " + sy + " " + sz);
        assertEquals("post-link scan must surface exactly 1 worn part: " + state,
                1, extract(state, PARTS_COUNT));
        assertEquals("initialPartToRepairCount must mirror parts at link time: "
                        + state, 1, extract(state, INITIAL_COUNT));
    }

    /** Two injections → two parts; scan is not a first-match
     *  short-circuit. {@code simple} variant has 2 advRocketmotor blocks
     *  so this is the maximum the fixture can support. */
    @Test
    public void multipleInjectionsAreAllScanned() throws Exception {
        RocketFixture rf = buildAndAssembleRocket(CX_MULTI);

        String inject1 = exec("artest infra inject-broken-part " + rf.rocketId + " 3");
        assertTrue("first inject must succeed: " + inject1,
                inject1.contains("\"ok\":true"));
        String inject2 = exec("artest infra inject-broken-part " + rf.rocketId + " 7");
        assertTrue("second inject must succeed (simple has 2 engines): "
                        + inject2, inject2.contains("\"ok\":true"));

        int sx = CX_MULTI + 10, sy = CY_PAD, sz = CZ_PAD;
        placeServiceStation(sx, sy, sz);
        linkStation(sx, sy, sz, rf.rocketId);

        String state = exec("artest infra service-state 0 " + sx + " " + sy + " " + sz);
        assertEquals("both injected parts must surface: " + state,
                2, extract(state, PARTS_COUNT));
        assertEquals("initialPartToRepairCount must be 2: " + state,
                2, extract(state, INITIAL_COUNT));
    }

    /** Post-link injection is invisible to the station until
     *  {@code service-relink} is invoked. Pins the lifecycle: scan runs
     *  on link, not on every tick. */
    @Test
    public void postLinkInjectionRequiresRescanToBecomeVisible() throws Exception {
        RocketFixture rf = buildAndAssembleRocket(CX_POST_LINK);

        // Link first — at this point the rocket has zero worn parts.
        int sx = CX_POST_LINK + 10, sy = CY_PAD, sz = CZ_PAD;
        placeServiceStation(sx, sy, sz);
        linkStation(sx, sy, sz, rf.rocketId);
        assertEquals("baseline: freshly-linked rocket has no worn parts",
                0, extract(exec("artest infra service-state 0 " + sx + " " + sy + " " + sz),
                        PARTS_COUNT));

        // Now mark a part as worn AFTER linking.
        String inject = exec("artest infra inject-broken-part " + rf.rocketId + " 5");
        assertTrue("inject must succeed post-link: " + inject,
                inject.contains("\"ok\":true"));

        // Without re-scan the station still reports 0 — confirms scan is
        // edge-triggered (on linkRocket), not level-triggered.
        assertEquals("post-link injection must NOT be auto-visible — "
                        + "scan is link-time, not tick-time",
                0, extract(exec("artest infra service-state 0 " + sx + " " + sy + " " + sz),
                        PARTS_COUNT));

        // After re-scan, the new worn part surfaces.
        String relink = exec("artest infra service-relink 0 " + sx + " " + sy + " " + sz);
        assertTrue("service-relink probe must succeed: " + relink,
                relink.contains("\"ok\":true"));
        assertEquals("after explicit re-scan the new worn part is visible",
                1, extract(exec("artest infra service-state 0 " + sx + " " + sy + " " + sz),
                        PARTS_COUNT));
    }

    // --- fixture helpers --------------------------------------------------

    private static final class RocketFixture {
        final int rocketId;
        RocketFixture(int id) { this.rocketId = id; }
    }

    private RocketFixture buildAndAssembleRocket(int baseX) throws Exception {
        int cx1 = (baseX - 2) >> 4, cz1 = (CZ_PAD - 2) >> 4;
        int cx2 = (baseX + 7) >> 4, cz2 = (CZ_PAD + 7) >> 4;
        exec("artest chunk warmup 0 " + cx1 + " " + cz1 + " " + cx2 + " " + cz2);
        exec("artest fill 0 " + (baseX - 2) + " " + (CY_PAD + 1) + " " + (CZ_PAD - 2)
                + " " + (baseX + 7) + " " + (CY_PAD + 10) + " " + (CZ_PAD + 7)
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
        assertTrue("no entityId in assemble: " + assemble, eim.find());
        return new RocketFixture(Integer.parseInt(eim.group(1)));
    }

    private void placeServiceStation(int sx, int sy, int sz) throws Exception {
        String place = exec("artest place 0 " + sx + " " + sy + " " + sz
                + " advancedrocketry:serviceStation");
        assertTrue("service station place failed: " + place,
                place.contains("\"placed\":true"));
    }

    private void linkStation(int sx, int sy, int sz, int rocketId) throws Exception {
        String link = exec("artest infra link 0 " + sx + " " + sy + " " + sz
                + " " + rocketId);
        assertTrue("infra link must succeed: " + link,
                link.contains("\"ok\":true"));
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }
}
