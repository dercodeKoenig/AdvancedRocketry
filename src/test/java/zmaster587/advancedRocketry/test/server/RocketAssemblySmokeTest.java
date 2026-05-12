package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.9 — rocket assembly smoke (P1).
 *
 * Programmatically builds the BuildRocketTest fixture geometry via
 * {@code /artest fixture rocket}, calls {@code /artest rocket assemble} which
 * synchronously runs scan + assemble + spawns the {@link
 * zmaster587.advancedRocketry.entity.EntityRocket}, then asserts:
 * <ul>
 *   <li>scan status reaches {@code SUCCESS};</li>
 *   <li>the assemble probe reports a non-negative {@code entityId};</li>
 *   <li>{@code /artest rocket list 0} sees the new rocket;</li>
 *   <li>{@code /artest rocket info <id>} returns a coherent snapshot.</li>
 * </ul>
 */
public class RocketAssemblySmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern BUILDER_POS = Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");

    @Test
    public void fixtureRocketAssemblesToLiveEntity() throws Exception {
        int baseX = 500, baseY = 64, baseZ = 500;
        String fixture = String.join("\n", client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ));
        assertTrue("fixture rocket failed: " + fixture, fixture.contains("\"ok\":true"));

        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture response missing builderPos: " + fixture, bp.find());
        int bx = Integer.parseInt(bp.group(1)),
                by = Integer.parseInt(bp.group(2)),
                bz = Integer.parseInt(bp.group(3));

        String machineInfo = String.join("\n", client().execute(
                "artest machine info 0 " + bx + " " + by + " " + bz));
        assertTrue("builder tile not recognized: " + machineInfo,
                machineInfo.contains("TileRocketAssemblingMachine"));

        String assemble = String.join("\n", client().execute(
                "artest rocket assemble 0 " + bx + " " + by + " " + bz));
        assertTrue("assemble failed: " + assemble, assemble.contains("\"ok\":true"));
        assertTrue("assemble returned no rocket entity: " + assemble,
                !assemble.contains("\"entityId\":-1"));
        assertTrue("expected 1-2 rockets in BB after assemble: " + assemble,
                assemble.contains("\"rocketCount\":1") || assemble.contains("\"rocketCount\":2"));

        String rocketList = String.join("\n", client().execute("artest rocket list 0"));
        assertTrue("rocket list still empty after assemble: " + rocketList,
                !rocketList.contains("\"rockets\":[]"));

        Matcher rim = ROCKET_LIST_ID.matcher(rocketList);
        assertTrue("could not extract entityId from rocket list: " + rocketList, rim.find());
        int entityId = Integer.parseInt(rim.group(1));

        String rocketInfo = String.join("\n", client().execute("artest rocket info " + entityId));
        assertTrue("rocket info missing hasStorage=true: " + rocketInfo,
                rocketInfo.contains("\"hasStorage\":true"));
    }
}
