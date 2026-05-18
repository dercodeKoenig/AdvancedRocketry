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
 * SMART §7.10 #8 — rocket-infrastructure link survives a clean server
 * restart against the same world dir.
 *
 * <p>Mirrors {@link WeatherPersistenceTest} / {@link
 * SatelliteIdChipPersistenceTest}: standalone harness lifecycle because
 * we need to stop/start across the same workDir, which {@link
 * AbstractHeadlessServerTest} can't do.</p>
 *
 * <p>Caveat: AR's NBT-saves the per-tile {@code linkedRocket}/{@code rocket}
 * field as an entity-id reference. The rocket entity itself is saved by
 * vanilla Minecraft as an EntityRocket NBT in the chunk. After restart, the
 * tile's reference resolves against the rocket's restored entity id. We
 * verify the infrastructure tile still reports {@code isInfrastructure:true}
 * post-restart and that the previously-spawned rocket is still in the world's
 * rocket list — both of which are necessary preconditions for the link to be
 * useful.</p>
 */
public class RocketInfrastructureLinkPersistenceTest {

    private static final Pattern BUILDER_POS = Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENT_ID = Pattern.compile("\"entityId\":(-?\\d+)");

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    @Before
    public void prepareWorkDir() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -Dforge.test.harness.enabled=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        workDir = Files.createTempDirectory("forge-server-infra-link-persistence-");
    }

    @After
    public void closeAll() throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
    }

    @Test
    public void infrastructureLinkSurvivesRestart() throws Exception {
        firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);

        int sx = 1300, sy = 65, sz = 1300;
        String place = String.join("\n", firstBoot.client().execute(
                "artest place 0 " + sx + " " + sy + " " + sz + " advancedrocketry:fuelingStation"));
        assertTrue("place fueling station failed: " + place, place.contains("\"placed\":true"));

        // Pre-clear + build + assemble rocket. Place rocket far enough away
        // (+20 X) so the pre-clear region doesn't wipe the fueling station.
        firstBoot.client().execute("artest fill 0 " + (sx + 18) + " " + (sy + 1) + " " + (sz - 2)
                + " " + (sx + 27) + " " + (sy + 11) + " " + (sz + 7) + " minecraft:air");
        String fx = String.join("\n", firstBoot.client().execute(
                "artest fixture rocket 0 " + (sx + 20) + " 64 " + sz + " simple"));
        assertTrue("fixture rocket failed on first boot: " + fx, fx.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fx);
        assertTrue("could not parse builderPos: " + fx, bp.find());
        int bx = Integer.parseInt(bp.group(1)),
                by = Integer.parseInt(bp.group(2)),
                bz = Integer.parseInt(bp.group(3));

        String assemble = String.join("\n", firstBoot.client().execute(
                "artest rocket assemble 0 " + bx + " " + by + " " + bz));
        assertTrue("rocket assemble failed on first boot: " + assemble, assemble.contains("\"ok\":true"));
        Matcher em = ENT_ID.matcher(assemble);
        assertTrue("rocket entityId missing: " + assemble, em.find());
        int rocketId = Integer.parseInt(em.group(1));

        String link = String.join("\n", firstBoot.client().execute(
                "artest infra link 0 " + sx + " " + sy + " " + sz + " " + rocketId));
        assertTrue("link must succeed on first boot: " + link, link.contains("\"linked\":true"));

        firstBoot.close();
        firstBoot = null;

        secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        String preserved = String.join("\n", secondBoot.client().execute(
                "artest infra info 0 " + sx + " " + sy + " " + sz));
        assertTrue("infrastructure tile must persist across restart: " + preserved,
                preserved.contains("\"isInfrastructure\":true"));

        // Force-load the chunk around the rocket spawn — Minecraft loads
        // entities lazily on chunk load, so {@code rocket list 0} reports
        // nothing until something pokes that chunk back in.
        secondBoot.client().execute("forceload add " + (sx + 20) + " " + sz + " "
                + (sx + 27) + " " + (sz + 7));
        secondBoot.client().execute("artest block at 0 " + (sx + 20) + " 64 " + sz);

        String rockets = String.join("\n", secondBoot.client().execute("artest rocket list 0"));
        assertTrue("rocket entity must persist across restart: " + rockets,
                rockets.contains("\"id\":"));
    }
}
