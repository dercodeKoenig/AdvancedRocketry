package zmaster587.advancedRocketry.test.integration;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;
import zmaster587.advancedRocketry.util.XMLPlanetLoader;
import zmaster587.advancedRocketry.util.XMLPlanetLoader.DimensionPropertyCoupling;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * §6.1 XML planet definitions — deep parsing path that needs
 * {@link MinecraftBootstrap#ensure()}.
 *
 * <p>The simple {@code loadFile}/{@code isValid} sanity checks live in
 * {@code unit/XMLPlanetLoaderTest}. This class drives {@code readAllPlanets()}
 * through actual XML fixtures and verifies every parsed field (DIMID
 * resolution, atmosphere/gravity clamping, weather field preservation,
 * defaults, parent/child planet hierarchy).</p>
 */
public class XMLPlanetLoaderTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private DimensionPropertyCoupling parse(String xml) throws IOException {
        File file = tempFolder.newFile();
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        XMLPlanetLoader loader = new XMLPlanetLoader();
        assertTrue("loader.loadFile must succeed for XML fixture", loader.loadFile(file));
        return loader.readAllPlanets();
    }

    private static String galaxy(String stars) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<galaxy>\n" + stars + "</galaxy>\n";
    }

    private static String star(String name, String body) {
        return "<star name=\"" + name + "\" temp=\"100\" x=\"0\" y=\"0\" size=\"1.0\""
                + " isBlackHole=\"false\" diskAngle=\"70\""
                + " numPlanets=\"1\" numGasGiants=\"0\">\n"
                + body
                + "</star>\n";
    }

    // ---- Star/planet discovery -----------------------------------------------

    @Test
    public void readAllPlanetsReturnsAtLeastOneStar() throws Exception {
        DimensionPropertyCoupling coupling = parse(galaxy(star("Sol", "")));
        assertEquals("expected 1 star parsed", 1, coupling.stars.size());
        StellarBody star = coupling.stars.get(0);
        assertEquals("Sol", star.getName());
        assertEquals(0, coupling.dims.size());
    }

    @Test
    public void planetWithExplicitDimIdGetsThatId() throws Exception {
        DimensionPropertyCoupling coupling = parse(galaxy(star("Sol",
                "<planet name=\"Earth\" DIMID=\"9001\">\n"
              + "  <isKnown>true</isKnown>\n"
              + "</planet>\n")));
        assertEquals(1, coupling.dims.size());
        DimensionProperties props = coupling.dims.get(0);
        assertEquals("DIMID attribute must override the allocator", 9001, props.getId());
        assertEquals("Earth", props.getName());
    }

    @Test
    public void planetWithoutDimIdGetsAllocatedDim() throws Exception {
        DimensionPropertyCoupling coupling = parse(galaxy(star("Sol",
                "<planet name=\"Earth\">\n"
              + "  <isKnown>true</isKnown>\n"
              + "</planet>\n")));
        assertEquals(1, coupling.dims.size());
        DimensionProperties props = coupling.dims.get(0);
        // INVALID_PLANET is Integer.MIN_VALUE — an allocation failure marker. The
        // allocator must return a usable dim.
        assertNotEquals("allocator returned INVALID_PLANET",
                zmaster587.advancedRocketry.api.Constants.INVALID_PLANET, props.getId());
        // Vanilla dims 0/-1/1 are reserved; allocator skips them.
        assertTrue("auto-allocated dim should be ≥ 2 (vanilla reserved 0/-1/1), got " + props.getId(),
                props.getId() >= 2);
    }

    @Test
    public void nestedPlanetBecomesChildOfParent() throws Exception {
        // Moon → child of Earth via nested <planet>.
        DimensionPropertyCoupling coupling = parse(galaxy(star("Sol",
                "<planet name=\"Earth\" DIMID=\"7001\">\n"
              + "  <isKnown>true</isKnown>\n"
              + "  <planet name=\"Moon\" DIMID=\"7002\">\n"
              + "    <isKnown>true</isKnown>\n"
              + "  </planet>\n"
              + "</planet>\n")));
        // readAllPlanets flattens hierarchy — both Earth + Moon in dims list.
        assertEquals("Earth + Moon = 2 dims parsed", 2, coupling.dims.size());

        DimensionProperties earth = findByName(coupling.dims, "Earth");
        DimensionProperties moon = findByName(coupling.dims, "Moon");
        assertNotNull(earth);
        assertNotNull(moon);

        // The parent's getChildPlanets() must contain the moon's dim id.
        assertTrue("Earth.getChildPlanets() must include the moon dim id ("
                        + moon.getId() + "): " + earth.getChildPlanets(),
                earth.getChildPlanets().contains(moon.getId()));
        assertEquals("Moon.getParentPlanet() must be Earth's dim id",
                earth.getId(), moon.getParentPlanet());
    }

    // ---- Weather fields ------------------------------------------------------

    @Test
    public void weatherFieldsAreParsed() throws Exception {
        DimensionPropertyCoupling coupling = parse(galaxy(star("Sol",
                "<planet name=\"Stormworld\" DIMID=\"7100\">\n"
              + "  <isKnown>true</isKnown>\n"
              + "  <rainStartLength>3000</rainStartLength>\n"
              + "  <rainProlongationLength>4000</rainProlongationLength>\n"
              + "  <thunderStartLength>5000</thunderStartLength>\n"
              + "  <thunderProlongationLength>6000</thunderProlongationLength>\n"
              + "  <rainMarker>1</rainMarker>\n"
              + "  <thunderMarker>-1</thunderMarker>\n"
              + "</planet>\n")));
        DimensionProperties props = coupling.dims.get(0);
        assertEquals(3000, props.rainStartLength);
        assertEquals(4000, props.rainProlongationLength);
        assertEquals(5000, props.thunderStartLength);
        assertEquals(6000, props.thunderProlongationLength);
        assertEquals("rainMarker=1 → always rain", 1, props.getRainMarker());
        assertEquals("thunderMarker=-1 → never thunder", -1, props.getThunderMarker());
    }

    @Test
    public void weatherFieldsDefaultWhenMissing() throws Exception {
        DimensionPropertyCoupling coupling = parse(galaxy(star("Sol",
                "<planet name=\"NoWeatherXml\" DIMID=\"7101\">\n"
              + "  <isKnown>true</isKnown>\n"
              + "</planet>\n")));
        DimensionProperties props = coupling.dims.get(0);
        // Production defaults: see DimensionProperties.rainStartLength=168000 etc.
        assertEquals(168000, props.rainStartLength);
        assertEquals(168000, props.thunderStartLength);
        assertEquals("default rainMarker=0 (regular weather)", 0, props.getRainMarker());
        assertEquals("default thunderMarker=0 (regular weather)", 0, props.getThunderMarker());
    }

    @Test
    public void invalidWeatherMarkerFailsExplicitly() throws Exception {
        // ELEMENT_RAIN_MARKER parsing uses Integer.parseInt with no try/catch
        // around it (production code). Verify the existing behaviour: parser
        // throws NumberFormatException loudly rather than silently accepting
        // garbage. Future hardening can replace this assertion with a
        // "normalized to 0" check if the parsing path adds a try/catch.
        try {
            parse(galaxy(star("Sol",
                    "<planet name=\"BadWeather\" DIMID=\"7102\">\n"
                  + "  <isKnown>true</isKnown>\n"
                  + "  <rainMarker>NOT_A_NUMBER</rainMarker>\n"
                  + "</planet>\n")));
            fail("XMLPlanetLoader must reject non-numeric rainMarker (or be updated to "
                    + "normalize it — adjust this assertion when production adds the guard)");
        } catch (NumberFormatException expected) {
            // OK — current behaviour: parser propagates the exception.
        }
    }

    // ---- Clamping ------------------------------------------------------------

    @Test
    public void atmosphereDensityClampsAboveMax() throws Exception {
        DimensionPropertyCoupling coupling = parse(galaxy(star("Sol",
                "<planet name=\"DenseAtm\" DIMID=\"7200\">\n"
              + "  <isKnown>true</isKnown>\n"
              + "  <atmosphereDensity>99999</atmosphereDensity>\n"
              + "</planet>\n")));
        DimensionProperties props = coupling.dims.get(0);
        assertEquals("atmosphere density must clamp to MAX_ATM_PRESSURE",
                DimensionProperties.MAX_ATM_PRESSURE, props.getAtmosphereDensity());
    }

    @Test
    public void atmosphereDensityClampsBelowMin() throws Exception {
        DimensionPropertyCoupling coupling = parse(galaxy(star("Sol",
                "<planet name=\"VacuumAtm\" DIMID=\"7201\">\n"
              + "  <isKnown>true</isKnown>\n"
              + "  <atmosphereDensity>-999</atmosphereDensity>\n"
              + "</planet>\n")));
        DimensionProperties props = coupling.dims.get(0);
        assertEquals("atmosphere density must clamp to MIN_ATM_PRESSURE",
                DimensionProperties.MIN_ATM_PRESSURE, props.getAtmosphereDensity());
    }

    @Test
    public void gravityClampsAboveMax() throws Exception {
        DimensionPropertyCoupling coupling = parse(galaxy(star("Sol",
                "<planet name=\"HeavyG\" DIMID=\"7202\">\n"
              + "  <isKnown>true</isKnown>\n"
              + "  <gravitationalMultiplier>99999</gravitationalMultiplier>\n"
              + "</planet>\n")));
        DimensionProperties props = coupling.dims.get(0);
        // Stored as float = clamped int / 100.
        assertEquals("gravity must clamp to MAX_GRAVITY/100",
                DimensionProperties.MAX_GRAVITY / 100f,
                props.getGravitationalMultiplier(), 1e-6);
    }

    // ---- Write → read full round-trip ---------------------------------------

    /**
     * §6.1 #10 — writeXML produces XML that readAllPlanets parses back into a
     * DimensionProperties carrying every field we wrote.
     *
     * Production save path: AR writes planet definitions to
     * {@code config/advRocketry/planetDefs.xml} via {@code XMLPlanetLoader.writeXML(DimensionManager)}.
     * That XML is later re-read at startup. The contract this test pins down is:
     * critical numeric/identity fields survive the round-trip.
     */
    @Test
    public void writeThenReadPreservesCriticalFields() throws Exception {
        // 1. Build an in-memory galaxy: 1 star + 1 planet attached to it.
        zmaster587.advancedRocketry.api.dimension.solar.StellarBody star =
                new zmaster587.advancedRocketry.api.dimension.solar.StellarBody();
        star.setId(7301);
        star.setName("WriteRtStar");
        star.setTemperature(120);
        star.setSize(1.25f);
        star.setBlackHole(false);

        DimensionProperties planet = new DimensionProperties(7302, "WriteRtPlanet");
        planet.gravitationalMultiplier = 1.5f;
        planet.orbitalDist = 175;
        planet.rotationalPeriod = 19_200;
        planet.setAtmosphereDensityDirect(125);
        planet.setStar(star);
        planet.hasOxygen = true;

        star.addPlanet(planet);

        // 2. Wrap into a minimal IGalaxy and serialise.
        zmaster587.advancedRocketry.api.dimension.solar.IGalaxy galaxy =
                new SingleStarGalaxyFixture(star);

        String xml = XMLPlanetLoader.writeXML(galaxy);
        assertTrue("writeXML must include the star name", xml.contains("WriteRtStar"));
        assertTrue("writeXML must include the planet name", xml.contains("WriteRtPlanet"));

        // 3. Round-trip through a temp file + loadFile + readAllPlanets.
        File out = tempFolder.newFile("written-planets.xml");
        Files.write(out.toPath(), xml.getBytes(StandardCharsets.UTF_8));

        XMLPlanetLoader reader = new XMLPlanetLoader();
        assertTrue("loadFile must accept self-generated XML", reader.loadFile(out));

        DimensionPropertyCoupling restored = reader.readAllPlanets();
        assertEquals("1 star round-trips", 1, restored.stars.size());
        assertEquals("WriteRtStar", restored.stars.get(0).getName());

        assertEquals("1 planet round-trips", 1, restored.dims.size());
        DimensionProperties restoredPlanet = restored.dims.get(0);
        assertEquals("WriteRtPlanet", restoredPlanet.getName());

        // Critical numeric fields — anything off-by-one here means a writeXML →
        // loadFile divergence that corrupts saves.
        assertEquals("gravity must round-trip",
                1.5f, restoredPlanet.getGravitationalMultiplier(), 1e-3);
        assertEquals("orbitalDist must round-trip",
                175, restoredPlanet.orbitalDist);
        assertEquals("rotationalPeriod must round-trip",
                19_200, restoredPlanet.rotationalPeriod);
        assertEquals("atmosphereDensity must round-trip",
                125, restoredPlanet.getAtmosphereDensity());
    }

    /**
     * Minimal IGalaxy fixture wrapping a single star. Only {@link #getStars()}
     * is consumed by {@link XMLPlanetLoader#writeXML}; all other methods throw
     * so that if the write path expands its API usage we fail loudly.
     */
    private static final class SingleStarGalaxyFixture
            implements zmaster587.advancedRocketry.api.dimension.solar.IGalaxy {
        private final zmaster587.advancedRocketry.api.dimension.solar.StellarBody star;
        SingleStarGalaxyFixture(zmaster587.advancedRocketry.api.dimension.solar.StellarBody s) {
            this.star = s;
        }

        @Override
        public java.util.Collection<zmaster587.advancedRocketry.api.dimension.solar.StellarBody>
        getStars() {
            return java.util.Collections.singletonList(star);
        }
        @Override public Integer[] getRegisteredDimensions() { throw new UnsupportedOperationException(); }
        @Override public zmaster587.advancedRocketry.api.satellite.SatelliteBase getSatellite(long satId) { throw new UnsupportedOperationException(); }
        @Override public boolean canTravelTo(int dimId) { throw new UnsupportedOperationException(); }
        @Override public zmaster587.advancedRocketry.api.dimension.IDimensionProperties getDimensionProperties(int dimId) { throw new UnsupportedOperationException(); }
        @Override public zmaster587.advancedRocketry.api.dimension.solar.StellarBody getStar(int id) { throw new UnsupportedOperationException(); }
        @Override public boolean isDimensionCreated(int dimId) { throw new UnsupportedOperationException(); }
        @Override public boolean areDimensionsInSamePlanetMoonSystem(int a, int b) { throw new UnsupportedOperationException(); }
    }

    @Test
    public void gravityClampsBelowMin() throws Exception {
        DimensionPropertyCoupling coupling = parse(galaxy(star("Sol",
                "<planet name=\"NoG\" DIMID=\"7203\">\n"
              + "  <isKnown>true</isKnown>\n"
              + "  <gravitationalMultiplier>-100</gravitationalMultiplier>\n"
              + "</planet>\n")));
        DimensionProperties props = coupling.dims.get(0);
        assertEquals("gravity must clamp to MIN_GRAVITY/100",
                DimensionProperties.MIN_GRAVITY / 100f,
                props.getGravitationalMultiplier(), 1e-6);
    }

    // ---- helpers -------------------------------------------------------------

    private static DimensionProperties findByName(List<DimensionProperties> list, String name) {
        for (DimensionProperties p : list) {
            if (name.equals(p.getName())) return p;
        }
        return null;
    }
}
