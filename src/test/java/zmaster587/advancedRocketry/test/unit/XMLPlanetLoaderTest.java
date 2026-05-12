package zmaster587.advancedRocketry.test.unit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import zmaster587.advancedRocketry.util.XMLPlanetLoader;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * §6.1 XML planet definitions — entry-point parsing.
 *
 * The full planet/biome/oregen hierarchy parsing path inside
 * {@link XMLPlanetLoader} is tightly coupled to {@code DimensionManager},
 * {@code AdvancedRocketryBiomes}, {@code Block.REGISTRY} and the AR mod-init
 * lifecycle. Round-tripping a real planet XML belongs in the §7.4
 * {@code PlanetXmlConfigIntegrationTest} scenario.
 *
 * Here we cover:
 *   - {@code XMLPlanetLoader} construction + initial state;
 *   - {@code loadFile} parsing well-formed XML;
 *   - {@code loadFile} rejecting malformed XML;
 *   - {@code isValid} reflecting parse outcome.
 */
public class XMLPlanetLoaderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String MINIMAL_GALAXY_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<galaxy>\n" +
                    "    <star name=\"Sol\" temp=\"100\" x=\"0\" y=\"0\" size=\"1.0\"" +
                    "          isBlackHole=\"false\" diskAngle=\"70\"" +
                    "          numPlanets=\"1\" numGasGiants=\"0\">\n" +
                    "    </star>\n" +
                    "</galaxy>\n";

    @Test
    public void freshLoaderIsNotValidUntilFileLoaded() {
        XMLPlanetLoader loader = new XMLPlanetLoader();
        assertFalse("a freshly constructed loader has no parsed document", loader.isValid());
    }

    @Test
    public void loadFileAcceptsWellFormedXml() throws Exception {
        File xml = tempFolder.newFile("galaxy.xml");
        Files.write(xml.toPath(), MINIMAL_GALAXY_XML.getBytes(StandardCharsets.UTF_8));

        XMLPlanetLoader loader = new XMLPlanetLoader();
        boolean ok = loader.loadFile(xml);

        assertTrue("loadFile must return true for well-formed XML", ok);
        assertTrue("isValid must be true after a successful load", loader.isValid());
    }

    @Test
    public void loadFileRejectsMalformedXml() throws Exception {
        File xml = tempFolder.newFile("broken.xml");
        // Unclosed tag — must fail to parse, but loader must not throw.
        Files.write(xml.toPath(),
                "<galaxy><star></galaxy>".getBytes(StandardCharsets.UTF_8));

        XMLPlanetLoader loader = new XMLPlanetLoader();
        boolean ok = loader.loadFile(xml);

        assertFalse("loadFile must return false for malformed XML", ok);
        assertFalse("isValid must remain false after a failed load", loader.isValid());
    }

    @Test
    public void loadFileWithEmptyXmlIsRejected() throws Exception {
        File xml = tempFolder.newFile("empty.xml");
        Files.write(xml.toPath(), new byte[0]);

        XMLPlanetLoader loader = new XMLPlanetLoader();
        boolean ok = loader.loadFile(xml);

        assertFalse("empty XML must not be considered valid", ok);
        assertFalse(loader.isValid());
    }

    @Test
    public void writeXmlRoundTripWithEmptyGalaxyProducesValidXml() throws Exception {
        // The simplest possible IGalaxy fixture (no stars). All other methods
        // throw — they're not exercised by writeXML's empty-galaxy path.
        zmaster587.advancedRocketry.api.dimension.solar.IGalaxy emptyGalaxy =
                new EmptyGalaxyFixture();

        String xml = XMLPlanetLoader.writeXML(emptyGalaxy);
        assertFalse("writeXML must produce non-empty output", xml.isEmpty());
        assertTrue("writeXML must declare the galaxy element", xml.contains("<galaxy"));

        File parsedBack = tempFolder.newFile("written.xml");
        Files.write(parsedBack.toPath(), xml.getBytes(StandardCharsets.UTF_8));

        XMLPlanetLoader loader = new XMLPlanetLoader();
        assertTrue("XML written by writeXML must be re-parseable by loadFile",
                loader.loadFile(parsedBack));
    }

    /**
     * Minimal IGalaxy implementation that only supports {@link #getStars()}.
     * All other methods throw — they should never be invoked by the writeXML
     * empty-galaxy path; if any future change breaks that invariant, the test
     * will fail loudly with UnsupportedOperationException.
     */
    private static final class EmptyGalaxyFixture
            implements zmaster587.advancedRocketry.api.dimension.solar.IGalaxy {
        @Override
        public java.util.Collection<zmaster587.advancedRocketry.api.dimension.solar.StellarBody>
        getStars() {
            return java.util.Collections.emptyList();
        }

        @Override public Integer[] getRegisteredDimensions() {
            throw new UnsupportedOperationException("not used by writeXML empty-galaxy path");
        }
        @Override public zmaster587.advancedRocketry.api.satellite.SatelliteBase
        getSatellite(long satId) {
            throw new UnsupportedOperationException("not used by writeXML empty-galaxy path");
        }
        @Override public boolean canTravelTo(int dimId) {
            throw new UnsupportedOperationException("not used by writeXML empty-galaxy path");
        }
        @Override public zmaster587.advancedRocketry.api.dimension.IDimensionProperties
        getDimensionProperties(int dimId) {
            throw new UnsupportedOperationException("not used by writeXML empty-galaxy path");
        }
        @Override public zmaster587.advancedRocketry.api.dimension.solar.StellarBody
        getStar(int id) {
            throw new UnsupportedOperationException("not used by writeXML empty-galaxy path");
        }
        @Override public boolean isDimensionCreated(int dimId) {
            throw new UnsupportedOperationException("not used by writeXML empty-galaxy path");
        }
        @Override public boolean areDimensionsInSamePlanetMoonSystem(int a, int b) {
            throw new UnsupportedOperationException("not used by writeXML empty-galaxy path");
        }
    }

    // Full planet/biome/oregen parsing requires DimensionManager + biome registry
    // and is covered by §7.4 PlanetXmlConfigIntegrationTest (write fixture XML →
    // boot server → /artest planet info round-trip). No point keeping @Ignore
    // stubs here that duplicate that coverage at a worse layer.
}
