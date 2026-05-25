package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * Coverage-audit gap (Tier 2 #6) — RocketEvent payload contract for
 * external subscribers.
 *
 * <p>The pre-existing event-counts probe + {@code RocketEventRecorder}
 * proves an event was POSTED, but doesn't prove what payload the
 * subscriber received. Companion mods subscribing to AR events expect
 * {@code event.getEntity()} to return the rocket and {@code event.world}
 * to be the rocket's current world.</p>
 *
 * <p>Pins:</p>
 * <ul>
 *   <li>{@code RocketDismantleEvent.getEntity().getEntityId()} ==
 *       rocket's actual id.</li>
 *   <li>{@code RocketDismantleEvent.world.provider.getDimension()} ==
 *       rocket's current dim (overworld in the harness).</li>
 *   <li>Same shape for {@code RocketPreLaunchEvent} via the existing
 *       {@code rocket launch ... prepare} path.</li>
 * </ul>
 *
 * <p>{@code RocketReachesOrbitEvent}, {@code RocketLandedEvent}, and
 * {@code RocketDeOrbitingEvent} all fire from internal flight logic
 * (orbit cycle, descent, landing). They're harder to reach from a
 * headless probe than the simple {@code dismantle} + {@code prepareLaunch}
 * paths. The two paths covered here are enough to pin the
 * {@code @EntityEvent}-base-class contract — if {@code event.getEntity()}
 * returns null or a different entity for any AR event, both pinned
 * events would fail because they share that base.</p>
 */
public class RocketEventPayloadContractTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENTITY_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern PRELAUNCH_ID = Pattern.compile("\"preLaunchEntityId\":(-?\\d+)");
    private static final Pattern PRELAUNCH_DIM = Pattern.compile("\"preLaunchDim\":(-?\\d+)");
    private static final Pattern DISMANTLE_ID = Pattern.compile("\"dismantleEntityId\":(-?\\d+)");
    private static final Pattern DISMANTLE_DIM = Pattern.compile("\"dismantleDim\":(-?\\d+)");

    private static final int CY = 64;
    private static final int CZ = 8000;
    private static final int CX_DISMANTLE = 8000;
    private static final int CX_PRELAUNCH = 8400;

    @Test
    public void rocketDismantleEventCarriesRocketEntityAndWorld() throws Exception {
        int rocketId = buildAndAssemble(CX_DISMANTLE);
        // Disarm any leaked PreLaunch cancellation from sibling tests.
        exec("artest rocket disarm-prelaunch-cancel");

        // Trigger dismantle — fires RocketDismantleEvent synchronously.
        String dismantle = exec("artest rocket dismantle " + rocketId);
        assertTrue("dismantle probe must succeed: " + dismantle,
                dismantle.contains("\"ok\":true"));

        String payloads = exec("artest rocket event-payloads");
        assertEquals("RocketDismantleEvent.getEntity().getEntityId() must equal "
                        + "the dismantled rocket's id: " + payloads,
                rocketId, extract(payloads, DISMANTLE_ID));
        assertEquals("RocketDismantleEvent.world.provider.getDimension() must "
                        + "equal the rocket's current dim (overworld=0): " + payloads,
                0, extract(payloads, DISMANTLE_DIM));
    }

    @Test
    public void rocketPreLaunchEventCarriesRocketEntityAndWorld() throws Exception {
        int rocketId = buildAndAssemble(CX_PRELAUNCH);
        exec("artest rocket disarm-prelaunch-cancel");

        // Call prepareLaunch — fires RocketPreLaunchEvent.
        String launch = exec("artest rocket launch " + rocketId + " true prepare");
        assertTrue("rocket launch (prepare) must succeed: " + launch,
                launch.contains("\"ok\":true") || launch.contains("\"entityId\":"));

        String payloads = exec("artest rocket event-payloads");
        assertEquals("RocketPreLaunchEvent.getEntity().getEntityId() must equal "
                        + "the rocket's id: " + payloads,
                rocketId, extract(payloads, PRELAUNCH_ID));
        assertEquals("RocketPreLaunchEvent.world.provider.getDimension() must "
                        + "equal the rocket's current dim: " + payloads,
                0, extract(payloads, PRELAUNCH_DIM));
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private int buildAndAssemble(int baseX) throws Exception {
        int cx1 = (baseX - 2) >> 4, cz1 = (CZ - 2) >> 4;
        int cx2 = (baseX + 7) >> 4, cz2 = (CZ + 7) >> 4;
        exec("artest chunk warmup 0 " + cx1 + " " + cz1 + " " + cx2 + " " + cz2);
        exec("artest fill 0 " + (baseX - 2) + " " + (CY + 1) + " " + (CZ - 2)
                + " " + (baseX + 7) + " " + (CY + 10) + " " + (CZ + 7)
                + " minecraft:air");
        String fixture = exec("artest fixture rocket 0 " + baseX + " " + CY + " " + CZ
                + " simple");
        assertTrue("fixture build failed: " + fixture, fixture.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("no builderPos: " + fixture, bp.find());
        String assemble = exec("artest rocket assemble 0 "
                + bp.group(1) + " " + bp.group(2) + " " + bp.group(3));
        assertTrue("assemble must succeed: " + assemble,
                assemble.contains("\"ok\":true"));
        Matcher eim = ENTITY_ID.matcher(assemble);
        assertTrue("no entityId: " + assemble, eim.find());
        return Integer.parseInt(eim.group(1));
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }
}
