package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.HeadlessGameTest;
import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * SMART §7.4 — planet XML config integration test.
 *
 * Pre-writes a deterministic fixture {@code planetDefs.xml} into
 * {@code <workDir>/config/advRocketry/} BEFORE the harness boots, then asserts
 * that {@code /artest planet info <fixture-dim>} round-trips the values from
 * the XML.
 */
public class PlanetXmlConfigIntegrationTest implements HeadlessGameTest {

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

    @Override public String id() { return "ar.scenario.planet_xml_config_integration"; }
    @Override public String category() { return "P0/planet-config"; }
    @Override public boolean required() { return true; }
    @Override public int timeoutTicks() { return 1; }

    @Override
    public void setUp(TestContext context) throws Exception {
        if (!HarnessBoundScenario.isHarnessEnabled()) {
            context.note("server harness disabled — skip");
            return;
        }
        workDir = Files.createTempDirectory("forge-server-planet-xml-");
        Path arConfigDir = workDir.resolve("config").resolve("advRocketry");
        Files.createDirectories(arConfigDir);

        // Minimal fixture — one star (Sol-equivalent at id=0 to match AR defaults)
        // hosting one custom planet at FIXTURE_DIM. Numbers chosen distinct from
        // any AR default so an accidental fallback to the embedded galaxy would
        // fail the assertion below.
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
        context.note("wrote fixture planetDefs.xml at " + arConfigDir.resolve("planetDefs.xml"));
    }

    @Override
    public TestStatus tick(TestContext context) throws Exception {
        if (workDir == null) return TestStatus.SKIPPED;

        try {
            harness = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);
        } catch (Throwable t) {
            context.note("harness start failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return TestStatus.SKIPPED;
        }

        // Step 1: confirm the fixture dim is registered.
        List<String> dimList = harness.client().execute("artest dim list");
        String dimJoined = String.join("\n", dimList);
        if (!dimJoined.contains("\"arDimensions\":[")) {
            context.note("dim list malformed: " + dimJoined);
            return TestStatus.FAILED;
        }
        if (!dimJoined.contains(String.valueOf(FIXTURE_DIM))) {
            context.note("fixture dim " + FIXTURE_DIM + " not in arDimensions: " + dimJoined);
            return TestStatus.FAILED;
        }

        // Step 2: planet info must reflect the XML values.
        List<String> planetInfo = harness.client().execute("artest planet info " + FIXTURE_DIM);
        String planetJoined = String.join("\n", planetInfo);
        if (planetJoined.contains("\"error\"")) {
            context.note("planet info errored: " + planetJoined);
            return TestStatus.FAILED;
        }
        // Match the values precisely — gravity is stored as float (xmlValue/100f).
        for (String expected : new String[] {
                "\"name\":\"" + FIXTURE_PLANET_NAME + "\"",
                "\"orbitalDistance\":" + FIXTURE_ORBITAL_DISTANCE,
                "\"atmosphereDensity\":" + FIXTURE_ATM_DENSITY,
                "\"rotationalPeriod\":" + FIXTURE_ROTATIONAL_PERIOD,
        }) {
            if (!planetJoined.contains(expected)) {
                context.note("planet info missing expected " + expected + ": " + planetJoined);
                return TestStatus.FAILED;
            }
        }
        // Gravity — value is fixture/100 = 0.75 = float; check by literal substring.
        if (!planetJoined.contains("\"gravity\":0.75")) {
            context.note("planet info gravity != 0.75: " + planetJoined);
            return TestStatus.FAILED;
        }

        context.note("fixture XML round-tripped through server start; " + FIXTURE_PLANET_NAME + " loaded");
        return TestStatus.PASSED;
    }

    @Override
    public void tearDown(TestContext context) throws Exception {
        if (harness != null) harness.close();
    }
}
