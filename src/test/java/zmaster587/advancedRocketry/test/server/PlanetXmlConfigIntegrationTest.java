package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.4 — planet XML config integration test.
 *
 * Pre-writes a deterministic fixture {@code planetDefs.xml} into
 * {@code <workDir>/config/advRocketry/} BEFORE the harness boots, then asserts
 * that {@code /artest planet info <fixture-dim>} round-trips the values from
 * the XML.
 *
 * <p>Doesn't extend {@link AbstractHeadlessServerTest} because the standard
 * harness lifecycle pre-creates its workDir via {@code Files.createTempDirectory}
 * AFTER spawning. We need to write the XML BEFORE startup, so the harness is
 * managed manually via {@link RealDedicatedServerHarness#startWith}.</p>
 */
public class PlanetXmlConfigIntegrationTest {

    /** Dim id we declare in the fixture. Must be outside vanilla 0/-1/1 + AR's
     *  defaults (Sol=0, AR uses 2+ for first planet). 9001 is well clear. */
    private static final int FIXTURE_DIM = 9001;
    private static final String FIXTURE_PLANET_NAME = "ARTestPlanet";
    private static final int FIXTURE_GRAVITY_HUNDREDTHS = 75;          // 0.75 multiplier
    private static final int FIXTURE_ORBITAL_DISTANCE = 250;
    private static final int FIXTURE_ATM_DENSITY = 50;
    private static final int FIXTURE_ROTATIONAL_PERIOD = 16000;

    private Path workDir;
    private RealDedicatedServerHarness harness;

    @Before
    public void writeFixtureXml() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -Dforge.test.harness.enabled=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));

        workDir = Files.createTempDirectory("forge-server-planet-xml-");
        Path arConfigDir = workDir.resolve("config").resolve("advRocketry");
        Files.createDirectories(arConfigDir);

        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<galaxy>\n" +
                "    <star name=\"Sol\" temp=\"100\" x=\"0\" y=\"0\" size=\"1.0\" " +
                "          isBlackHole=\"false\" diskAngle=\"70\" " +
                "          numPlanets=\"1\" numGasGiants=\"0\">\n" +
                "        <planet name=\"" + FIXTURE_PLANET_NAME + "\" DIMID=\"" + FIXTURE_DIM + "\">\n" +
                "            <isKnown>true</isKnown>\n" +
                "            <fogColor>0.5,0.5,0.5</fogColor>\n" +
                "            <skyColor>0.4,0.6,0.9</skyColor>\n" +
                "            <gravitationalMultiplier>" + FIXTURE_GRAVITY_HUNDREDTHS + "</gravitationalMultiplier>\n" +
                "            <orbitalDistance>" + FIXTURE_ORBITAL_DISTANCE + "</orbitalDistance>\n" +
                "            <orbitalTheta>0</orbitalTheta>\n" +
                "            <orbitalPhi>0</orbitalPhi>\n" +
                "            <retrograde>false</retrograde>\n" +
                "            <averageTemperature>250</averageTemperature>\n" +
                "            <rotationalPeriod>" + FIXTURE_ROTATIONAL_PERIOD + "</rotationalPeriod>\n" +
                "            <atmosphereDensity>" + FIXTURE_ATM_DENSITY + "</atmosphereDensity>\n" +
                "            <generateCraters>false</generateCraters>\n" +
                "            <generateCaves>true</generateCaves>\n" +
                "            <generateVolcanos>false</generateVolcanos>\n" +
                "        </planet>\n" +
                "    </star>\n" +
                "</galaxy>\n";

        Files.write(arConfigDir.resolve("planetDefs.xml"), xml.getBytes(StandardCharsets.UTF_8));
    }

    @After
    public void stopHarness() throws Exception {
        if (harness != null) harness.close();
    }

    @Test
    public void fixtureXmlRoundTripsThroughServerStart() throws Exception {
        harness = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        String dimList = String.join("\n", harness.client().execute("artest dim list"));
        assertTrue("dim list malformed: " + dimList,
                dimList.contains("\"arDimensions\":["));
        assertTrue("fixture dim " + FIXTURE_DIM + " not in arDimensions: " + dimList,
                dimList.contains(String.valueOf(FIXTURE_DIM)));

        String planetInfo = String.join("\n",
                harness.client().execute("artest planet info " + FIXTURE_DIM));
        assertTrue("planet info errored: " + planetInfo,
                !planetInfo.contains("\"error\""));

        for (String expected : new String[] {
                "\"name\":\"" + FIXTURE_PLANET_NAME + "\"",
                "\"orbitalDistance\":" + FIXTURE_ORBITAL_DISTANCE,
                "\"atmosphereDensity\":" + FIXTURE_ATM_DENSITY,
                "\"rotationalPeriod\":" + FIXTURE_ROTATIONAL_PERIOD,
                "\"gravity\":0.75",
        }) {
            assertTrue("planet info missing " + expected + ": " + planetInfo,
                    planetInfo.contains(expected));
        }
    }
}
