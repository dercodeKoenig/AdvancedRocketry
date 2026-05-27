package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.client.RealClientHarness;
import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-35 follow-up — true moderator-fetch coverage using two connected
 * Minecraft client bots.
 *
 * <p>{@link WorldCommandFetchTest} closed the resolvable contract surface
 * with self-fetch + unknown-name pins, but the original task framing
 * called for the canonical "moderator fetches another player to their
 * location" pin. That requires TWO connected players — bot1 (the
 * moderator) and bot2 (the target). This test runs both clients in the
 * same JVM via the multi-client variant of
 * {@link RealClientHarness#start(RealDedicatedServerHarness, String)},
 * each with a distinct username.</p>
 *
 * <p>Contract pinned:</p>
 *
 * <ul>
 *   <li><b>Two-player /ar fetch.</b> Bot1 (op) at position A, bot2 at
 *       position B. {@code /ar fetch bot2-name} (issued as bot1)
 *       resolves bot2 via {@code World.getPlayerEntityByName}, transfers
 *       bot2 to bot1's dim, and sets bot2's coords to bot1's. Post-fetch
 *       bot2's coords must be (≈) bot1's pre-fetch coords. Pins the
 *       full positive path that the single-client harness can't reach.</li>
 * </ul>
 *
 * <p><b>Resource cost</b>: ~3-4 minutes of wall time + ~7 GB RAM
 * (server JVM + 2 × client JVM + 2 × LWJGL/GL context). Run with
 * {@code maxParallelForks=1} for this test class — concurrent multi-
 * client tests will exhaust display/RAM on a typical dev box.</p>
 */
public class WorldCommandFetchModeratorTest {

    /** Distinct usernames — the server's PlayerList keys on these and
     *  rejects duplicates as "already connected", so bot1 ≠ bot2 must
     *  hold for both to be online simultaneously. */
    private static final String BOT1_NAME = "ModBot1";
    private static final String BOT2_NAME = "ModBot2";

    private static final Pattern PLAYER_POS_X = Pattern.compile("\"playerPosX\":(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern PLAYER_POS_Z = Pattern.compile("\"playerPosZ\":(-?\\d+(?:\\.\\d+)?)");

    private RealDedicatedServerHarness server;
    private RealClientHarness bot1Harness;
    private RealClientHarness bot2Harness;

    @Before
    public void startAll() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -D" + AbstractHeadlessServerTest.PROP_HARNESS_ENABLED
                        + "=true to enable",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        Assume.assumeTrue(
                "Client harness disabled — set -D" + AbstractClientE2ETest.PROP_CLIENT_ENABLED
                        + "=true to enable",
                Boolean.parseBoolean(System.getProperty(
                        AbstractClientE2ETest.PROP_CLIENT_ENABLED, "false")));

        server = RealDedicatedServerHarness.start();
        try {
            // Start clients sequentially — each takes ~60-90s for JVM +
            // GL handshake + world join. Starting them in parallel risks
            // RealClientHarness.start()'s internal control-socket-accept
            // racing on the same ServerSocket; the sequential path is
            // straightforward.
            bot1Harness = RealClientHarness.start(server, BOT1_NAME);
            bot2Harness = RealClientHarness.start(server, BOT2_NAME);
        } catch (Exception startupException) {
            try {
                if (bot2Harness != null) bot2Harness.close();
            } catch (Exception cleanup) { startupException.addSuppressed(cleanup); }
            try {
                if (bot1Harness != null) bot1Harness.close();
            } catch (Exception cleanup) { startupException.addSuppressed(cleanup); }
            try {
                server.close();
            } catch (Exception cleanup) { startupException.addSuppressed(cleanup); }
            server = null; bot1Harness = null; bot2Harness = null;
            throw startupException;
        }
    }

    @After
    public void stopAll() throws Exception {
        Exception deferred = null;
        if (bot2Harness != null) {
            try { bot2Harness.close(); } catch (Exception e) { deferred = e; }
            bot2Harness = null;
        }
        if (bot1Harness != null) {
            try { bot1Harness.close(); } catch (Exception e) {
                if (deferred == null) deferred = e; else deferred.addSuppressed(e);
            }
            bot1Harness = null;
        }
        if (server != null) {
            try { server.close(); } catch (Exception e) {
                if (deferred == null) deferred = e; else deferred.addSuppressed(e);
            }
            server = null;
        }
        if (deferred != null) throw deferred;
    }

    private String exec(String cmd) throws Exception {
        return String.join("\n", server.client().execute(cmd));
    }

    /** Moderator (bot1, op) fetches bot2 from position B to position A. */
    @Test
    public void moderatorFetchTeleportsTargetToSenderPosition() throws Exception {
        // Stage both bots at known, well-separated positions.
        // Use vanilla /tp from the server console — works for any
        // connected player and doesn't need probe machinery.
        int sx = 100, sy = 80, sz = 100; // bot1 (moderator) destination
        int tx = 200, ty = 80, tz = 200; // bot2 (target) starting position

        // Place stone to stand on so /tp doesn't drop into the void.
        exec("artest place 0 " + sx + " " + (sy - 1) + " " + sz + " minecraft:stone");
        exec("artest place 0 " + tx + " " + (ty - 1) + " " + tz + " minecraft:stone");

        exec("tp " + BOT1_NAME + " " + (sx + 0.5) + " " + sy + " " + (sz + 0.5));
        exec("tp " + BOT2_NAME + " " + (tx + 0.5) + " " + ty + " " + (tz + 0.5));

        // Give the clients a few ticks to acknowledge their new positions
        // before we sample them.
        bot1Harness.bot().waitTicks(5);
        bot2Harness.bot().waitTicks(5);

        // Sanity-check pre-state: bots are at distinct positions.
        String bot1Pre = exec("artest player position-of " + BOT1_NAME);
        String bot2Pre = exec("artest player position-of " + BOT2_NAME);
        double bot1PreX = extractDouble(bot1Pre, PLAYER_POS_X);
        double bot1PreZ = extractDouble(bot1Pre, PLAYER_POS_Z);
        double bot2PreX = extractDouble(bot2Pre, PLAYER_POS_X);
        double bot2PreZ = extractDouble(bot2Pre, PLAYER_POS_Z);
        assertTrue("baseline: bot1 should be near (" + sx + "," + sz + "), got ("
                        + bot1PreX + "," + bot1PreZ + ")",
                Math.abs(bot1PreX - (sx + 0.5)) < 2.0
                        && Math.abs(bot1PreZ - (sz + 0.5)) < 2.0);
        assertTrue("baseline: bot2 should be near (" + tx + "," + tz + "), got ("
                        + bot2PreX + "," + bot2PreZ + ")",
                Math.abs(bot2PreX - (tx + 0.5)) < 2.0
                        && Math.abs(bot2PreZ - (tz + 0.5)) < 2.0);
        // The two bots MUST be at clearly distinct positions for the
        // moderator-fetch result to be observable.
        assertNotEquals("baseline: bots must start at distinct X coords",
                Math.round(bot1PreX), Math.round(bot2PreX));

        // Op bot1 so /ar fetch (player-equipped verb) is authorised.
        String op = exec("artest player op-named " + BOT1_NAME);
        assertTrue("op-named must succeed for bot1: " + op,
                op.contains("\"opped\":true"));

        // The moderator (bot1) runs /ar fetch bot2.
        String fetch = exec("artest player exec-as-named " + BOT1_NAME
                + " /ar fetch " + BOT2_NAME);
        assertTrue("exec-as-named /ar fetch must succeed: " + fetch,
                fetch.contains("\"ok\":true"));

        // Verify bot2's position is now bot1's pre-fetch position.
        String bot2Post = exec("artest player position-of " + BOT2_NAME);
        double bot2PostX = extractDouble(bot2Post, PLAYER_POS_X);
        double bot2PostZ = extractDouble(bot2Post, PLAYER_POS_Z);
        // setPosition copies sender coords exactly — sub-block tolerance
        // covers any same-dim transferPlayerToDimension nudging.
        assertTrue("post-fetch: bot2 must be at bot1's pre-fetch X ("
                        + bot1PreX + "), got " + bot2PostX,
                Math.abs(bot2PostX - bot1PreX) < 1.5);
        assertTrue("post-fetch: bot2 must be at bot1's pre-fetch Z ("
                        + bot1PreZ + "), got " + bot2PostZ,
                Math.abs(bot2PostZ - bot1PreZ) < 1.5);
        // And NOT at its prior position any more.
        assertTrue("post-fetch: bot2 must have moved away from its prior X ("
                        + bot2PreX + "), got " + bot2PostX,
                Math.abs(bot2PostX - bot2PreX) > 10.0);
    }

    private static double extractDouble(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Double.parseDouble(m.group(1));
    }
}
