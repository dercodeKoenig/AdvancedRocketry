package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.9 — rocket launch through the classic scripted path (P1).
 *
 * Builds + assembles a rocket via the same fixture as
 * {@link RocketAssemblySmokeTest}, then calls {@code /artest rocket launch} to
 * trigger the production launch path. Falls back to {@code force} mode if the
 * regular launch path can't find a destination (no guidance chip on the
 * fixture).
 */
public class RocketLaunchSmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern BUILDER_POS = Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENT_ID = Pattern.compile("\"entityId\":(-?\\d+)");

    @Test
    public void assembledRocketTransitionsToFlight() throws Exception {
        int baseX = 600, baseY = 64, baseZ = 600;
        String fixture = String.join("\n", client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ));
        assertTrue("fixture rocket failed: " + fixture, fixture.contains("\"ok\":true"));

        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("missing builderPos: " + fixture, bp.find());
        int bx = Integer.parseInt(bp.group(1)),
                by = Integer.parseInt(bp.group(2)),
                bz = Integer.parseInt(bp.group(3));

        String assemble = String.join("\n", client().execute(
                "artest rocket assemble 0 " + bx + " " + by + " " + bz));
        assertTrue("assemble didn't produce a rocket: " + assemble,
                assemble.contains("\"ok\":true") && !assemble.contains("\"entityId\":-1"));

        Matcher em = ENT_ID.matcher(assemble);
        assertTrue("assemble response missing entityId: " + assemble, em.find());
        int entityId = Integer.parseInt(em.group(1));
        assertTrue("assemble succeeded but entityId=-1", entityId >= 0);

        // Try the real launch path first (instant — bypasses 200-tick countdown).
        String launchInstant = String.join("\n", client().execute(
                "artest rocket launch " + entityId + " true instant"));
        assertTrue("instant launch errored: " + launchInstant,
                launchInstant.contains("\"ok\":true"));

        if (launchInstant.contains("\"isInFlight\":true") || launchInstant.contains("\"isInOrbit\":true")) {
            // Real path succeeded.
            return;
        }

        // Real path errored silently (no destination chip). Fall back to force.
        String launchForce = String.join("\n", client().execute(
                "artest rocket launch " + entityId + " true force"));
        assertTrue("force launch errored: " + launchForce,
                launchForce.contains("\"ok\":true"));
        assertTrue("force launch didn't set isInFlight=true: " + launchForce,
                launchForce.contains("\"isInFlight\":true"));
    }
}
