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
