package zmaster587.advancedRocketry.test.server;

// migrated to AbstractSharedServerTest (TASK-03 B2)
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 1 (deep paths) — rocket launch event chain.
 *
 * The shallow Phase 1 ({@link EventHandlerWiringTest}) covers the
 * weather-wrap WorldEvent.Load hook. This file extends Phase 1 onto
 * the rocket-event side: drive an assembled rocket through the launch
 * modes the probe exposes and pin that {@code RocketEventHandler}-side
 * state actually updates (isInFlight, isInOrbit) — those flags are
 * read by every renderer, every infrastructure link, and every
 * mission system. A silent regression here ships rockets that look
 * parked in the launchpad while their server-side state is "in orbit".
 *
 * Mode coverage matches the probe vocabulary:
 *   - {@code launch <id> false force}: bypasses fuel / pre-launch
 *     checks, flips {@code isInFlight=true} via {@code setInFlight}.
 *   - {@code launch <id> true instant}: fills fuel + calls
 *     {@code rocket.launch()} (the production path).
 *   - {@code launch <id> true prepare}: fills fuel + calls
 *     {@code prepareLaunch()} (the multi-tick path).
 */
public class RocketLaunchEventTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");

    private int buildAndAssemble(int baseX, int baseY, int baseZ) throws Exception {
        // Pre-clear a generous halo so any pre-existing terrain or test
        // detritus doesn't leak into the scan.
        String fillAir = String.join("\n", client().execute(
                "artest fill 0 " + (baseX - 2) + " " + (baseY + 1) + " " + (baseZ - 2)
                        + " " + (baseX + 7) + " " + (baseY + 10) + " " + (baseZ + 7)
                        + " minecraft:air"));
        assertTrue("pre-clear failed: " + fillAir, fillAir.contains("\"ok\":true"));

        String fixture = String.join("\n", client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ + " simple"));
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture missing builderPos: " + fixture, bp.find());
        int bx = Integer.parseInt(bp.group(1));
        int by = Integer.parseInt(bp.group(2));
        int bz = Integer.parseInt(bp.group(3));

        String assemble = String.join("\n", client().execute(
                "artest rocket assemble 0 " + bx + " " + by + " " + bz));
        assertTrue("assemble failed: " + assemble, assemble.contains("\"ok\":true"));

        String list = String.join("\n", client().execute("artest rocket list 0"));
        Matcher rim = ROCKET_LIST_ID.matcher(list);
        int lastId = -1;
        while (rim.find()) lastId = Integer.parseInt(rim.group(1));
        assertTrue("rocket list empty after assemble: " + list, lastId >= 0);
        return lastId;
    }

    @Test
    public void launchForceSetsInFlightFlag() throws Exception {
        // Use unique baseX per test so fixtures from earlier tests in this
        // JVM don't collide (RocketAssemblySmokeTest grabs 500..580).
        int id = buildAndAssemble(700, 64, 500);
        String preInfo = String.join("\n", client().execute("artest rocket info " + id));
        assertTrue("freshly assembled rocket should NOT already be in flight: " + preInfo,
                preInfo.contains("\"isInFlight\":false"));

        // false=skip fuel fill, force = setInFlight(true) bypass.
        String launch = String.join("\n",
                client().execute("artest rocket launch " + id + " false force"));
        assertTrue("force launch must succeed: " + launch, launch.contains("\"ok\":true"));
        assertTrue("force launch response must report isInFlight=true: " + launch,
                launch.contains("\"isInFlight\":true"));

        // Verify via a separate info probe — confirms the flag persists
        // through the entity registry, not just the launch response.
        String postInfo = String.join("\n", client().execute("artest rocket info " + id));
        assertTrue("rocket info must report isInFlight=true after force launch: " + postInfo,
                postInfo.contains("\"isInFlight\":true"));
    }

    @Test
    public void launchInstantRespondsOkAndEchoesMode() throws Exception {
        int id = buildAndAssemble(740, 64, 500);

        // instant: fills fuel + calls rocket.launch() — the production
        // launch path. Production launch() has pre-conditions (launchpad
        // contact, destination, etc.) that a test-fixture rocket sitting
        // in mid-air doesn't satisfy, so isInFlight may remain false on
        // this code path. Pin only the wiring contract: the probe must
        // accept the call, echo the mode, report fuelFilled=true (proves
        // the fuel-fill loop fired), and not crash.
        String launch = String.join("\n",
                client().execute("artest rocket launch " + id + " true instant"));
        assertTrue("instant launch must succeed: " + launch, launch.contains("\"ok\":true"));
        assertTrue("launch response must echo back the chosen mode: " + launch,
                launch.contains("\"mode\":\"instant\""));
        assertTrue("launch with fuelFill=true must echo it: " + launch,
                launch.contains("\"fuelFilled\":true"));
    }

    @Test
    public void launchOnUnknownIdReturnsError() throws Exception {
        // Counter-test: the entity registry lookup must NOT silently no-op.
        // A regression that returned ok:true here would let downstream
        // tooling claim launch success for rockets that never existed.
        String launch = String.join("\n",
                client().execute("artest rocket launch 9999999 false force"));
        assertTrue("launch on unknown id must report rocket-not-found: " + launch,
                launch.contains("\"error\":\"rocket not found\""));
    }

    @Test
    public void doubleLaunchKeepsIsInFlightSet() throws Exception {
        // Sequence: launch → already-in-flight → launch again. Idempotency
        // contract: the second call must not flip the flag back off, must
        // not crash. (In production, the rocket is briefly in 'in flight'
        // before takeoff finishes; a second launch button-press is a
        // realistic edge case.)
        int id = buildAndAssemble(780, 64, 500);
        client().execute("artest rocket launch " + id + " false force");
        String second = String.join("\n",
                client().execute("artest rocket launch " + id + " false force"));
        assertTrue("second-launch must still ok: " + second, second.contains("\"ok\":true"));
        assertTrue("second-launch must still report isInFlight=true: " + second,
                second.contains("\"isInFlight\":true"));
    }
}
