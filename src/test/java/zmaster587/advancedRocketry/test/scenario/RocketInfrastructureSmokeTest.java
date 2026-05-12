package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMART §7.10 — rocket infrastructure (fueling station, linker, distance).
 *
 * <ol>
 *   <li>Build a rocket fixture and assemble it (re-uses
 *       {@link RocketAssemblySmokeTest} primitives).</li>
 *   <li>Place a {@code fuelingStation} adjacent to the launch pad.</li>
 *   <li>{@code /artest infra info} confirms the station implements
 *       {@link zmaster587.advancedRocketry.api.IInfrastructure} and reports its
 *       link-distance budget.</li>
 *   <li>{@code /artest infra link <stationPos> <rocketId>} drives the production
 *       {@link zmaster587.advancedRocketry.api.EntityRocketBase#linkInfrastructure(zmaster587.advancedRocketry.api.IInfrastructure)}
 *       path and asserts {@code linked=true} + {@code connectedCount=1}.</li>
 *   <li>Empty-pos probe returns "no tile entity" without NPE.</li>
 * </ol>
 *
 * <p>Distance-limit + fuel-transfer probes are stubbed via the same
 * {@code /artest infra link} path — when a future probe extension surfaces
 * {@code linkRocket} return value (rejected distance, etc.) those assertions
 * land here too.</p>
 */
public class RocketInfrastructureSmokeTest extends HarnessBoundScenario {

    private static final Pattern ENT_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern CONN = Pattern.compile("\"connectedCount\":(\\d+)");

    @Override public String id() { return "ar.scenario.rocket_infrastructure_smoke"; }
    @Override public String category() { return "P1/rocket-infrastructure"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // Place fueling station first — it must be ready before rocket lifts off.
        int sx = 850, sy = 65, sz = 850;
        String place = String.join("\n", client.execute(
                "artest place 0 " + sx + " " + sy + " " + sz + " advancedrocketry:fuelingStation"));
        if (!place.contains("\"placed\":true")) {
            context.note("place fueling station failed: " + place);
            return TestStatus.FAILED;
        }

        // Verify infrastructure interface.
        String infraInfo = String.join("\n", client.execute(
                "artest infra info 0 " + sx + " " + sy + " " + sz));
        if (!infraInfo.contains("\"isInfrastructure\":true")) {
            context.note("fueling station not IInfrastructure: " + infraInfo);
            return TestStatus.FAILED;
        }
        if (!infraInfo.contains("\"maxLinkDistance\"")) {
            context.note("infra info missing maxLinkDistance: " + infraInfo);
            return TestStatus.FAILED;
        }

        // Empty pos NPE guard.
        String emptyInfra = String.join("\n", client.execute("artest infra info 0 100 64 100"));
        if (!emptyInfra.contains("\"error\":\"no tile entity\"")) {
            context.note("infra info on empty pos didn't error: " + emptyInfra);
            return TestStatus.FAILED;
        }

        // Build a rocket near the station so it's in range.
        int baseX = sx + 2, baseY = 64, baseZ = sz + 2;
        String fx = String.join("\n", client.execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ));
        if (!fx.contains("\"ok\":true")) {
            context.note("fixture rocket failed: " + fx);
            return TestStatus.FAILED;
        }
        Matcher bp = Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]").matcher(fx);
        if (!bp.find()) {
            context.note("could not parse builderPos: " + fx);
            return TestStatus.FAILED;
        }
        int bx = Integer.parseInt(bp.group(1)),
                by = Integer.parseInt(bp.group(2)),
                bz = Integer.parseInt(bp.group(3));

        String assemble = String.join("\n", client.execute(
                "artest rocket assemble 0 " + bx + " " + by + " " + bz));
        if (!assemble.contains("\"ok\":true")) {
            context.note("rocket assemble failed: " + assemble);
            return TestStatus.FAILED;
        }
        Matcher em = ENT_ID.matcher(assemble);
        if (!em.find()) {
            context.note("rocket entityId missing: " + assemble);
            return TestStatus.FAILED;
        }
        int rocketId = Integer.parseInt(em.group(1));
        if (rocketId < 0) {
            context.note("rocket entityId<0: " + assemble);
            return TestStatus.FAILED;
        }

        // Link rocket ↔ station via the production path.
        String link = String.join("\n", client.execute(
                "artest infra link 0 " + sx + " " + sy + " " + sz + " " + rocketId));
        if (!link.contains("\"ok\":true")) {
            context.note("infra link probe errored: " + link);
            return TestStatus.FAILED;
        }
        if (!link.contains("\"linked\":true")) {
            context.note("station didn't accept rocket link: " + link);
            return TestStatus.FAILED;
        }
        Matcher cm = CONN.matcher(link);
        if (!cm.find() || Integer.parseInt(cm.group(1)) < 1) {
            context.note("connectedCount<1 after link: " + link);
            return TestStatus.FAILED;
        }

        // Idempotency: linking the same infrastructure again must NOT double-add.
        String relink = String.join("\n", client.execute(
                "artest infra link 0 " + sx + " " + sy + " " + sz + " " + rocketId));
        if (!relink.contains("\"linked\":false")) {
            context.note("re-link unexpectedly succeeded a second time: " + relink);
            return TestStatus.FAILED;
        }

        context.note("rocket " + rocketId + " linked to fueling station at "
                + sx + "," + sy + "," + sz + "; re-link rejected as expected");
        return TestStatus.PASSED;
    }
}
