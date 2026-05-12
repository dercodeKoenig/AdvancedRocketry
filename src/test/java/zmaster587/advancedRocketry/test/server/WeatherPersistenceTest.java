package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.5 — weather persistence across server restart.
 *
 * Manages two harness lifecycles directly against the same workDir — not a
 * fit for {@link AbstractHeadlessServerTest} (which auto-manages a single
 * fresh-dir harness).
 */
public class WeatherPersistenceTest {

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    @Before
    public void prepareWorkDir() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -Dforge.test.harness.enabled=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        workDir = Files.createTempDirectory("forge-server-weather-persistence-");
    }

    @After
    public void closeAll() throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
    }

    @Test
    public void rainSurvivesRestartOnSameWorkDir() throws Exception {
        firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);
        firstBoot.client().execute("artest weather set 0 rain 12000");
        String beforeStop = String.join("\n", firstBoot.client().execute("artest weather get 0"));
        assertTrue("rain didn't take effect on first boot: " + beforeStop,
                beforeStop.contains("\"isRaining\":true"));

        firstBoot.close();
        firstBoot = null;

        secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);
        String after = String.join("\n", secondBoot.client().execute("artest weather get 0"));
        assertTrue("rain DID NOT persist across restart: " + after,
                after.contains("\"isRaining\":true"));
    }
}
