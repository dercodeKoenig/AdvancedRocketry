package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * Coverage-audit gap (post-TASK-26) — service-station observability +
 * link contract.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.infrastructure.TileRocketServiceStation}
 * is the only way for a player to repair a rocket's worn parts late-game.
 * Pre-this-test, the 562-line tile had ZERO server-tier coverage — only
 * {@code IInfrastructure} link/unlink + {@code maxLinkDistance} from
 * {@code RocketInfrastructureSmokeTest} touched it generically.</p>
 *
 * <p>Pins three contracts:</p>
 *
 * <ul>
 *   <li><b>Unlinked + no power + no rocket = stable tick</b>: production
 *       must not crash on idle ticks. Documented invariant of every
 *       AR tile-entity.</li>
 *   <li><b>Link contract</b>: {@code rocket.linkInfrastructure(serviceStation)}
 *       sets {@code linkedRocket} to that rocket entity; service-state
 *       probe reads it back as the entity id.</li>
 *   <li><b>Fresh-rocket invariant</b>: a freshly-assembled rocket
 *       (no flight time, no wear) has zero {@code partsToRepair} —
 *       the production "wear via use" path is the ONLY way to grow
 *       that list. Pin protects against a regression that auto-marks
 *       freshly-assembled parts as worn.</li>
 * </ul>
 *
 * <p><b>Out of scope (deferred)</b>: the full repair cycle (worn parts
 * → linked PrecisionAssembler → completed item → stage=0) requires
 * fixture infrastructure for injecting {@code TileBrokenPart} instances
 * with stage&gt;0 into a rocket's {@code StorageChunk}, plus an
 * adjacent {@code TilePrecisionAssembler} with a valid recipe. That
 * fixture cost (~6-8 hours) was deemed too high relative to the
 * audit-batch ROI. This test pins the OBSERVABILITY surface needed by
 * a future repair-cycle test to detect that production state is
 * sane between phases.</p>
 */
public class RocketServiceStationLinkAndStateTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENTITY_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern LINKED_ID = Pattern.compile("\"linkedRocketId\":(-?\\d+)");
    private static final Pattern PARTS_COUNT = Pattern.compile("\"partsToRepairCount\":(-?\\d+)");

    private static final int CY_PAD       = 64;
    private static final int CZ_PAD       = 7000;
    private static final int CX_NO_LINK   = 7000;
    private static final int CX_WITH_LINK = 7400;

    /** Service-station tick path with no linked rocket and no power
     *  surfaces — the same path a freshly-placed station traverses
     *  on world load. Must not throw. */
    @Test
    public void serviceStationTicksWithoutLinkedRocketWithoutCrash() throws Exception {
        // Place service station in isolation (not adjacent to any rocket
        // fixture, no assemblers nearby).
        int sx = CX_NO_LINK, sy = CY_PAD, sz = CZ_PAD - 50;
        String place = exec("artest place 0 " + sx + " " + sy + " " + sz
                + " advancedrocketry:serviceStation");
        assertTrue("service station place failed: " + place,
                place.contains("\"placed\":true"));

        // Tick. Production performFunction guards on `linkedRocket instanceof
        // EntityRocket` before doing any work — null branch must be a no-op.
        String tick = exec("artest tile force-tick 0 " + sx + " " + sy + " " + sz
                + " 40");
        assertTrue("force-tick on unlinked service station must succeed: " + tick,
                tick.contains("\"ok\":true"));

        // State probe must succeed and report linkedRocketId = -1.
        String state = exec("artest infra service-state 0 " + sx + " " + sy + " " + sz);
        assertEquals("unlinked service station must report linkedRocketId=-1",
                -1, extract(state, LINKED_ID));
        assertEquals("unlinked service station must report 0 parts-to-repair",
                0, extract(state, PARTS_COUNT));
    }

    /** Building a rocket via {@code fixture rocket simple}, placing a
     *  service station nearby, and calling {@code infra link} attaches
     *  the rocket to the station — service-state probe reads back the
     *  entity id.
     *
     *  <p>Also pins the fresh-rocket invariant: a just-assembled rocket
     *  has zero worn parts (TileBrokenPart with stage>0). Production
     *  only marks parts as worn through the wear-on-use path; if a
     *  regression auto-stages-up new parts, this test fires.</p> */
    @Test
    public void linkedFreshRocketAppearsInServiceStationStateWithZeroWornParts()
            throws Exception {
        // Build + assemble a standard rocket fixture far from any other patch.
        int cx1 = (CX_WITH_LINK - 2) >> 4, cz1 = (CZ_PAD - 2) >> 4;
        int cx2 = (CX_WITH_LINK + 7) >> 4, cz2 = (CZ_PAD + 7) >> 4;
        exec("artest chunk warmup 0 " + cx1 + " " + cz1 + " " + cx2 + " " + cz2);
        exec("artest fill 0 " + (CX_WITH_LINK - 2) + " " + (CY_PAD + 1) + " "
                + (CZ_PAD - 2) + " " + (CX_WITH_LINK + 7) + " " + (CY_PAD + 10)
                + " " + (CZ_PAD + 7) + " minecraft:air");

        String fixture = exec("artest fixture rocket 0 " + CX_WITH_LINK + " "
                + CY_PAD + " " + CZ_PAD + " simple");
        assertTrue("fixture must build: " + fixture, fixture.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture missing builderPos: " + fixture, bp.find());

        String assemble = exec("artest rocket assemble 0 "
                + bp.group(1) + " " + bp.group(2) + " " + bp.group(3));
        assertTrue("assemble must succeed: " + assemble,
                assemble.contains("\"ok\":true"));
        Matcher eim = ENTITY_ID.matcher(assemble);
        assertTrue("no entityId in assemble: " + assemble, eim.find());
        int rocketId = Integer.parseInt(eim.group(1));

        // Place service station near the launchpad (not on it — the pad
        // is occupied). Position-isolated from CX_NO_LINK.
        int sx = CX_WITH_LINK + 10, sy = CY_PAD, sz = CZ_PAD;
        String place = exec("artest place 0 " + sx + " " + sy + " " + sz
                + " advancedrocketry:serviceStation");
        assertTrue("service station place failed: " + place,
                place.contains("\"placed\":true"));

        // Link.
        String link = exec("artest infra link 0 " + sx + " " + sy + " " + sz
                + " " + rocketId);
        assertTrue("infra link must succeed: " + link,
                link.contains("\"ok\":true"));

        // Verify the service station now reports the rocket's entityId.
        String state = exec("artest infra service-state 0 " + sx + " " + sy + " " + sz);
        assertEquals("linked service station must report rocket's entityId",
                rocketId, extract(state, LINKED_ID));

        // Fresh-rocket invariant — no worn parts (TileBrokenPart with
        // stage>0). Production wear-on-use is the only path that grows
        // this count; a regression that auto-stages-up fresh parts
        // would surface here as a non-zero count.
        assertEquals("freshly-assembled rocket must have zero worn parts: "
                        + state,
                0, extract(state, PARTS_COUNT));
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }
}
