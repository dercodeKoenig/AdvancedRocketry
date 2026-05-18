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

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.12 — satellite-ID-chip persistence across server restart.
 *
 * <p>Standalone harness lifecycle (mirrors {@link WeatherPersistenceTest})
 * because {@link AbstractHeadlessServerTest} auto-manages a single fresh-dir
 * harness and we need to stop/start across the same {@code workDir}. The
 * client-side chip is the carrier of the ID; what really survives is the
 * dim's serialised satellite registry — the assertion below pins that
 * server-side behaviour.</p>
 */
public class SatelliteIdChipPersistenceTest {

    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    @Before
    public void prepareWorkDir() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -Dforge.test.harness.enabled=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        workDir = Files.createTempDirectory("forge-server-satellite-persistence-");
    }

    @After
    public void closeAll() throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
    }

    @Test
    public void satelliteIdSurvivesRestartOnSameWorkDir() throws Exception {
        firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);

        String create = String.join("\n", firstBoot.client().execute(
                "artest satellite create 0 composition 200 4000 2048"));
        assertTrue("satellite create failed on first boot: " + create,
                create.contains("\"ok\":true"));
        Matcher m = ID.matcher(create);
        assertTrue("create response missing satellite id: " + create, m.find());
        long satId = Long.parseLong(m.group(1));

        String preStop = String.join("\n", firstBoot.client().execute(
                "artest satellite info 0 " + satId));
        assertTrue("pre-stop satellite info must report composition: " + preStop,
                preStop.contains("\"type\":\"composition\""));

        firstBoot.close();
        firstBoot = null;

        secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);
        String postBoot = String.join("\n", secondBoot.client().execute(
                "artest satellite info 0 " + satId));
        assertTrue("satellite must survive restart and resolve by id "
                + satId + ": " + postBoot, postBoot.contains("\"type\":\"composition\""));
        assertTrue("powerStorage must persist across restart: " + postBoot,
                postBoot.contains("\"powerStorage\":4000"));
    }
}
