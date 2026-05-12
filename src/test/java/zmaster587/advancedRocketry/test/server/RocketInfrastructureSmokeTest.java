package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.10 — rocket infrastructure (fueling station, linker, distance).
 */
public class RocketInfrastructureSmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern BUILDER_POS = Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENT_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern CONN = Pattern.compile("\"connectedCount\":(\\d+)");

    @Test
    public void fuelingStationLinksToAssembledRocket() throws Exception {
        int sx = 850, sy = 65, sz = 850;
        String place = String.join("\n", client().execute(
                "artest place 0 " + sx + " " + sy + " " + sz + " advancedrocketry:fuelingStation"));
        assertTrue("place fueling station failed: " + place,
                place.contains("\"placed\":true"));

        String infraInfo = String.join("\n", client().execute(
                "artest infra info 0 " + sx + " " + sy + " " + sz));
        assertTrue("fueling station not IInfrastructure: " + infraInfo,
                infraInfo.contains("\"isInfrastructure\":true"));
        assertTrue("infra info missing maxLinkDistance: " + infraInfo,
                infraInfo.contains("\"maxLinkDistance\""));

        String emptyInfra = String.join("\n", client().execute("artest infra info 0 100 64 100"));
        assertTrue("infra info on empty pos didn't error: " + emptyInfra,
                emptyInfra.contains("\"error\":\"no tile entity\""));

        int baseX = sx + 2, baseY = 64, baseZ = sz + 2;
        String fx = String.join("\n", client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ));
        assertTrue("fixture rocket failed: " + fx, fx.contains("\"ok\":true"));

        Matcher bp = BUILDER_POS.matcher(fx);
        assertTrue("could not parse builderPos: " + fx, bp.find());
        int bx = Integer.parseInt(bp.group(1)),
                by = Integer.parseInt(bp.group(2)),
                bz = Integer.parseInt(bp.group(3));

        String assemble = String.join("\n", client().execute(
                "artest rocket assemble 0 " + bx + " " + by + " " + bz));
        assertTrue("rocket assemble failed: " + assemble, assemble.contains("\"ok\":true"));

        Matcher em = ENT_ID.matcher(assemble);
        assertTrue("rocket entityId missing: " + assemble, em.find());
        int rocketId = Integer.parseInt(em.group(1));
        assertTrue("rocket entityId<0: " + assemble, rocketId >= 0);

        String link = String.join("\n", client().execute(
                "artest infra link 0 " + sx + " " + sy + " " + sz + " " + rocketId));
        assertTrue("infra link probe errored: " + link, link.contains("\"ok\":true"));
        assertTrue("station didn't accept rocket link: " + link, link.contains("\"linked\":true"));

        Matcher cm = CONN.matcher(link);
        assertTrue("connectedCount missing", cm.find());
        assertTrue("connectedCount<1 after link: " + link,
                Integer.parseInt(cm.group(1)) >= 1);

        // Idempotency: re-linking same infrastructure must NOT double-add.
        String relink = String.join("\n", client().execute(
                "artest infra link 0 " + sx + " " + sy + " " + sz + " " + rocketId));
        assertTrue("re-link unexpectedly succeeded a second time: " + relink,
                relink.contains("\"linked\":false"));
    }
}
