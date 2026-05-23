package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-13 — wireless transceiver NBT + onLoad-role persistence.
 *
 * <p>Boot 1 places + configures a transceiver in a non-default state
 * (extract mode, enabled, paired into a fresh network). Boot 2 — same
 * workDir — reads back via {@code wireless-info} and
 * {@code wireless-role-on-network}, asserting:</p>
 *
 * <ul>
 *   <li>{@code mode}, {@code enabled}, {@code networkID} survived
 *       {@code TileWirelessTransciever.writeToNBT / readFromNBT}.</li>
 *   <li>{@code onLoad} re-registered the tile on its dataNetwork with
 *       the saved role (extract → source). Without this side, a player
 *       configuring a transceiver before restart would find it silently
 *       absent from its network on restart.</li>
 * </ul>
 *
 * <p>Mirrors the lifecycle pattern of {@code PersistenceRestartSmokeTest}
 * (per-method workDir, two harness instances).</p>
 */
public class WirelessTransceiverRestartTest {

    private static final Pattern NET_ID = Pattern.compile("\"networkID\":(-?\\d+)");
    private static final Pattern SHARED_ID = Pattern.compile("\"sharedNetworkId\":(-?\\d+)");
    private static final Pattern MODE = Pattern.compile("\"mode\":\"(extract|inject)\"");
    private static final Pattern ENABLED = Pattern.compile("\"enabled\":(true|false)");
    private static final Pattern IS_SOURCE = Pattern.compile("\"isSource\":(true|false)");
    private static final Pattern IS_SINK = Pattern.compile("\"isSink\":(true|false)");

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    private static final int DIM = 0;
    private static final int X_A = 1100;
    private static final int X_B = 1125;
    private static final int Y = 65;
    private static final int Z = 1100;

    @Before
    public void prepareWorkDir() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -Dforge.test.harness.enabled=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        workDir = Files.createTempDirectory("forge-server-wireless-restart-");
    }

    @After
    public void closeAll() throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
    }

    @Test
    public void modeEnabledAndNetworkIdSurviveRestartWithRoleReRegistration() throws Exception {
        firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);

        // Boot 1 — non-default state.
        place(firstBoot, X_A);
        place(firstBoot, X_B);
        int sharedId = pair(firstBoot, X_A, X_B);
        setMode(firstBoot, X_A, "extract");
        setEnabled(firstBoot, X_A, true);

        // Sanity — boot 1 sees what we wrote.
        String pre = info(firstBoot, X_A);
        assertEquals("extract", extractMode(pre));
        assertTrue("enabled set", extractBool(ENABLED, pre));
        assertEquals(sharedId, extractInt(NET_ID, pre));

        firstBoot.close();
        firstBoot = null;

        // Boot 2 — same workDir. The tile must be loaded by virtue of
        // being in a spawn chunk; NBT round-trip and onLoad fire on
        // chunk-load.
        secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        String post = info(secondBoot, X_A);
        assertEquals("mode must survive NBT round-trip",
                "extract", extractMode(post));
        assertTrue("enabled must survive NBT round-trip",
                extractBool(ENABLED, post));
        assertEquals("networkID must survive NBT round-trip",
                sharedId, extractInt(NET_ID, post));

        // onLoad re-registers as source for extract mode. Without onLoad
        // running, isSource would be false and the player's pre-restart
        // configuration would be invisible to the network.
        String role = String.join("\n", secondBoot.client().execute(
                "artest pipe wireless-role-on-network "
                        + DIM + " " + X_A + " " + Y + " " + Z));
        assertTrue("onLoad must re-register extract-mode tile as source: " + role,
                extractBool(IS_SOURCE, role));
        assertFalse("source must not also be a sink: " + role,
                extractBool(IS_SINK, role));
    }

    // --- helpers -----------------------------------------------------------

    private static void place(RealDedicatedServerHarness h, int x) throws Exception {
        String r = String.join("\n", h.client().execute(
                "artest place " + DIM + " " + x + " " + Y + " " + Z
                        + " advancedrocketry:wirelessTransciever"));
        assertTrue("place failed at x=" + x + ": " + r, r.contains("\"placed\":true"));
    }

    private static int pair(RealDedicatedServerHarness h, int xA, int xB) throws Exception {
        String r = String.join("\n", h.client().execute(
                "artest pipe wireless-pair " + DIM + " "
                        + xA + " " + Y + " " + Z + " "
                        + xB + " " + Y + " " + Z));
        assertTrue("pair failed: " + r, r.contains("\"ok\":true"));
        return extractInt(SHARED_ID, r);
    }

    private static String info(RealDedicatedServerHarness h, int x) throws Exception {
        return String.join("\n", h.client().execute(
                "artest pipe wireless-info " + DIM + " " + x + " " + Y + " " + Z));
    }

    private static void setMode(RealDedicatedServerHarness h, int x, String mode) throws Exception {
        String r = String.join("\n", h.client().execute(
                "artest pipe wireless-set-mode " + DIM + " "
                        + x + " " + Y + " " + Z + " " + mode));
        assertTrue("set-mode failed: " + r, r.contains("\"ok\":true"));
    }

    private static void setEnabled(RealDedicatedServerHarness h, int x, boolean enabled) throws Exception {
        String r = String.join("\n", h.client().execute(
                "artest pipe wireless-set-enabled " + DIM + " "
                        + x + " " + Y + " " + Z + " " + enabled));
        assertTrue("set-enabled failed: " + r, r.contains("\"ok\":true"));
    }

    private static String extractMode(String haystack) {
        Matcher m = MODE.matcher(haystack);
        if (!m.find()) throw new AssertionError("no mode in: " + haystack);
        return m.group(1);
    }

    private static int extractInt(Pattern p, String haystack) {
        Matcher m = p.matcher(haystack);
        if (!m.find()) throw new AssertionError("pattern " + p + " did not match: " + haystack);
        return Integer.parseInt(m.group(1));
    }

    private static boolean extractBool(Pattern p, String haystack) {
        Matcher m = p.matcher(haystack);
        if (!m.find()) throw new AssertionError("pattern " + p + " did not match: " + haystack);
        return Boolean.parseBoolean(m.group(1));
    }
}
