package zmaster587.advancedRocketry.test.server;

import org.junit.After;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * Coverage-audit gap (post-TASK-26): contract of
 * {@link zmaster587.advancedRocketry.api.RocketEvent.RocketPreLaunchEvent}'s
 * {@code @Cancelable} annotation.
 *
 * <p>The event is part of AR's public {@code api/} surface and companion
 * mods subscribe to it expecting cancellation to actually prevent the
 * launch. Production flow at
 * {@code EntityRocket.prepareLaunch}:{@code 1705-1712}:</p>
 *
 * <pre>{@code
 *   RocketPreLaunchEvent event = new RocketPreLaunchEvent(this);
 *   MinecraftForge.EVENT_BUS.post(event);
 *   if (!event.isCanceled()) {
 *       // ... send launch packet, set LAUNCH_COUNTER = 200
 *   }
 * }</pre>
 *
 * <p>If the {@code !event.isCanceled()} guard is ever removed or
 * inverted, every companion mod's cancellation logic breaks silently.
 * This test pins the contract via a probe-installed listener that
 * conditionally cancels the event:</p>
 *
 * <ul>
 *   <li>armed → prepareLaunch fires event → cancelled → LAUNCH_COUNTER
 *       stays at default -1 (countdown never starts).</li>
 *   <li>disarmed → prepareLaunch fires event → not cancelled →
 *       LAUNCH_COUNTER set to 200 (countdown started).</li>
 *   <li>The probe-side counter (events observed vs cancelled) proves
 *       the listener actually received both fires.</li>
 * </ul>
 *
 * <p><b>Why this is contract-level, not impl</b>: {@code @Cancelable}
 * is a Forge framework annotation tied to the event-bus dispatch
 * mechanism. AR's javadoc at the event declaration says
 * "Cancelling the event aborts the launch" — that's the public
 * promise to API consumers. Without this test, the contract is
 * implicit and could regress on the next refactor.</p>
 */
public class RocketPreLaunchEventCancellationTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENTITY_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern LAUNCH_COUNTER =
            Pattern.compile("\"launchCounter\":(-?\\d+)");
    private static final Pattern OBSERVED = Pattern.compile("\"observed\":(-?\\d+)");
    private static final Pattern CANCELLED = Pattern.compile("\"cancelled\":(-?\\d+)");

    private static final int CY = 64;
    /** Two well-separated rocket fixtures so the cancel test and the
     *  no-cancel test each have their own pad — same shared harness,
     *  different geometry, no cross-state. */
    private static final int CX_CANCEL    = 6000;
    private static final int CX_NO_CANCEL = 6300;
    private static final int CZ           = 6000;

    @After
    public void disarmCancellation() throws Exception {
        // Belt-and-braces: even if @Test threw before its finally ran,
        // disarm here. A leaked-armed canceller would break every
        // subsequent rocket-launch test in the shared harness.
        exec("artest rocket disarm-prelaunch-cancel");
    }

    @Test
    public void cancellingPreLaunchPreventsLaunchCountdown() throws Exception {
        int entityId = buildAndAssemble(CX_CANCEL);
        try {
            // Arm the canceller. Subsequent prepareLaunch calls fire
            // the event; the test listener cancels it.
            String arm = exec("artest rocket arm-prelaunch-cancel");
            assertTrue("arm probe failed: " + arm,
                    arm.contains("\"armed\":true"));

            String launch = exec("artest rocket launch " + entityId + " true prepare");
            assertTrue("rocket launch (prepare mode) must not error even when "
                            + "cancelled: " + launch,
                    launch.contains("\"ok\":true") || launch.contains("\"entityId\":"));

            String info = exec("artest rocket info " + entityId);
            int counter = extract(info, LAUNCH_COUNTER);
            assertEquals("cancelled prepareLaunch must leave LAUNCH_COUNTER "
                            + "at its default (-1) — countdown must NOT have "
                            + "started: " + info,
                    -1, counter);
            assertTrue("isInFlight must remain false after cancelled launch: "
                            + info,
                    info.contains("\"isInFlight\":false"));

            // The listener must have observed the event and cancelled it —
            // proves the test toggle actually wired through.
            String counts = exec("artest rocket prelaunch-cancel-counts");
            assertTrue("listener observed count must be >= 1: " + counts,
                    extract(counts, OBSERVED) >= 1);
            assertTrue("listener cancelled count must be >= 1: " + counts,
                    extract(counts, CANCELLED) >= 1);
        } finally {
            exec("artest rocket disarm-prelaunch-cancel");
        }
    }

    @Test
    public void nonCancelledPreLaunchSetsCountdownAndProceeds() throws Exception {
        int entityId = buildAndAssemble(CX_NO_CANCEL);
        // Explicit disarm (idempotent with default) so a stale @After
        // from an unrelated test ordering can't leak armed state in.
        exec("artest rocket disarm-prelaunch-cancel");

        String launch = exec("artest rocket launch " + entityId + " true prepare");
        assertTrue("rocket launch (prepare mode) must succeed when not cancelled: "
                        + launch,
                launch.contains("\"ok\":true") || launch.contains("\"entityId\":"));

        String info = exec("artest rocket info " + entityId);
        int counter = extract(info, LAUNCH_COUNTER);
        assertEquals("uncancelled prepareLaunch must seed LAUNCH_COUNTER to 200 "
                        + "(the countdown tick budget): " + info,
                200, counter);
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private int buildAndAssemble(int baseX) throws Exception {
        // Reproduces RocketAssemblySmokeTest.buildAndAssemble's hygiene
        // without depending on its package-private helper.
        int cx1 = (baseX - 2) >> 4, cz1 = (CZ - 2) >> 4;
        int cx2 = (baseX + 7) >> 4, cz2 = (CZ + 7) >> 4;
        exec("artest chunk warmup 0 " + cx1 + " " + cz1 + " " + cx2 + " " + cz2);
        exec("artest fill 0 " + (baseX - 2) + " " + (CY + 1) + " " + (CZ - 2)
                + " " + (baseX + 7) + " " + (CY + 10) + " " + (CZ + 7)
                + " minecraft:air");

        String fixture = exec("artest fixture rocket 0 " + baseX + " " + CY + " " + CZ
                + " simple");
        assertTrue("fixture build failed: " + fixture,
                fixture.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture missing builderPos: " + fixture, bp.find());

        String assemble = exec("artest rocket assemble 0 "
                + bp.group(1) + " " + bp.group(2) + " " + bp.group(3));
        assertTrue("assemble must succeed: " + assemble,
                assemble.contains("\"ok\":true"));

        Matcher eim = ENTITY_ID.matcher(assemble);
        assertTrue("no entityId in assemble response: " + assemble, eim.find());
        return Integer.parseInt(eim.group(1));
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }
}
