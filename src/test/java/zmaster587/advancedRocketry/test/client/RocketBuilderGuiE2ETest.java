package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.client.ClientGuiTestSupport.openGuiByRightClick;

/**
 * SMART §7.20 — deep client E2E for the rocket assembling machine.
 *
 * <p>Builds the full rocket structure with {@code /artest fixture rocket},
 * opens the assembler's libVulpes modular GUI by right-clicking the builder
 * block, then drives the real two-button assembly flow entirely through the
 * client GUI:</p>
 * <ol>
 *   <li>{@code clickButtonById(0)} — the <em>Scan</em> button.</li>
 *   <li>{@code clickButtonById(1)} — the <em>Build</em> button — pressed on a
 *       poll: {@code TileRocketAssemblingMachine.useNetworkData} ignores a Build
 *       press while {@code isScanning()}, so it simply "takes" once the scan
 *       pass finishes.</li>
 * </ol>
 *
 * <p>Unlike the headless {@code server/RocketAssemblySmokeTest} — which calls
 * {@code scanRocket}/{@code assembleRocket} directly — this exercises the
 * machine's real energy-gated {@code performFunction} tick loop, so the builder
 * is kept powered via {@code /artest energy inject}. The spawned
 * {@code EntityRocket} appearing in {@code /artest rocket list} is the
 * completion signal.</p>
 *
 * <p>Button ids 0 (scan) / 1 (build) are the stable ids assigned in
 * {@code TileRocketAssemblingMachine.getModules}; clicks are routed by id
 * through {@code GuiModular.actionPerformed}.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true}; auto-skips on headless CI.</p>
 */
public class RocketBuilderGuiE2ETest extends AbstractClientE2ETest {

    private static final int BASE_X = 200, BASE_Y = 64, BASE_Z = 200;
    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");

    @Test
    public void clickingScanThenBuildAssemblesRocket() throws Exception {
        String fixture = String.join("\n", serverClient().execute(
                "artest fixture rocket 0 " + BASE_X + " " + BASE_Y + " " + BASE_Z));
        assertTrue("fixture rocket failed: " + fixture, fixture.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture response missing builderPos: " + fixture, bp.find());
        int bx = Integer.parseInt(bp.group(1));
        int by = Integer.parseInt(bp.group(2));
        int bz = Integer.parseInt(bp.group(3));
        String builder = "0 " + bx + " " + by + " " + bz;

        // Stand on the launchpad, within interaction reach of the builder.
        serverClient().execute("tp @a " + (BASE_X + 2.5) + " " + (BASE_Y + 1) + " "
                + (BASE_Z + 2.5) + " 0 0");
        bot().waitTicks(40);

        String screen = openGuiByRightClick(bot(), bx, by, bz);
        assertTrue("expected the assembler GUI to open, got: " + screen,
                screen.startsWith("zmaster587.libVulpes.inventory.GuiModular"));

        // Scan (button id 0) starts the scan pass; keep the machine powered
        // so its performFunction tick loop actually runs.
        serverClient().execute("artest energy inject " + builder + " 100000000");
        bot().clickButtonById(0);

        // Build (button id 1): ignored by useNetworkData while scanning, so
        // press it on a poll while topping up energy, until the EntityRocket
        // spawns — the unambiguous completion signal.
        String rocketList = "";
        for (int waited = 0; waited < 3600; waited += 40) {
            serverClient().execute("artest energy inject " + builder + " 100000000");
            bot().clickButtonById(1);
            bot().waitTicks(40);
            rocketList = String.join("\n", serverClient().execute("artest rocket list 0"));
            if (!rocketList.contains("\"rockets\":[]")) {
                break;
            }
        }
        assertTrue("clicking Scan then Build did not assemble a rocket: " + rocketList,
                !rocketList.contains("\"rockets\":[]") && rocketList.contains("\"id\":"));
    }
}
