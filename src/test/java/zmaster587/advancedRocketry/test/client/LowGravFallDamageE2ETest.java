package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.client.RealClientHarness;
import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10b Phase 5 — gravity-scaled fall damage pin.
 *
 * <p>Production:
 * {@link zmaster587.advancedRocketry.event.PlanetEventHandler#fallEvent}
 * (lines 611-618). On any
 * {@link zmaster587.advancedRocketry.api.IPlanetaryProvider} dim the
 * handler scales {@code LivingFallEvent.getDistance()} by the planet's
 * gravitational multiplier — so a 20-block fall on a Luna-like
 * 0.166-grav dim resolves as a ~3.32-block fall (no damage past the
 * vanilla 3-block exempt window). Overworld is not an
 * IPlanetaryProvider, so the handler skips it entirely and the
 * distance is unchanged.</p>
 *
 * <p>Drives the handler through {@code /artest player try-fall} —
 * posts a synthetic LivingFallEvent at the player's position and
 * reports the post-handler distance plus the dim's gravity multiplier
 * (for cross-check).</p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LowGravFallDamageE2ETest {

    private static final int DIM_LOW_GRAV = 9701;

    private static final Pattern RESULT_DIST =
            Pattern.compile("\"resultDistance\":(-?[0-9.eE+-]+)");
    private static final Pattern INPUT_DIST =
            Pattern.compile("\"inputDistance\":(-?[0-9.eE+-]+)");
    private static final Pattern GRAVITY =
            Pattern.compile("\"gravityMultiplier\":(-?[0-9.eE+-]+)");
    private static final Pattern IS_PLANETARY =
            Pattern.compile("\"isPlanetaryProvider\":(true|false)");

    private Path workDir;
    private RealDedicatedServerHarness serverHarness;
    private RealClientHarness clientHarness;

    @Before
    public void startBoth() throws Exception {
        Assume.assumeTrue("Server harness disabled",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        Assume.assumeTrue("Client harness disabled",
                Boolean.parseBoolean(System.getProperty(
                        AbstractClientE2ETest.PROP_CLIENT_ENABLED, "false")));

        workDir = Files.createTempDirectory("forge-client-fall-grav-");
        Path arConfigDir = workDir.resolve("config").resolve("advRocketry");
        Files.createDirectories(arConfigDir);
        // gravitationalMultiplier in planetDefs.xml is an integer
        // percentage: 17 ≈ 0.17, i.e. Luna-like.
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<galaxy>\n"
                + "    <star name=\"Sol\" temp=\"100\" x=\"0\" y=\"0\" size=\"1.0\" "
                + "          isBlackHole=\"false\" diskAngle=\"70\" "
                + "          numPlanets=\"1\" numGasGiants=\"0\">\n"
                + "        <planet name=\"LowGravPlanet\" DIMID=\"" + DIM_LOW_GRAV + "\">\n"
                + "            <isKnown>true</isKnown>\n"
                + "            <fogColor>0.5,0.5,0.5</fogColor>\n"
                + "            <skyColor>0.4,0.6,0.9</skyColor>\n"
                + "            <gravitationalMultiplier>17</gravitationalMultiplier>\n"
                + "            <orbitalDistance>100</orbitalDistance>\n"
                + "            <orbitalTheta>0</orbitalTheta>\n"
                + "            <orbitalPhi>0</orbitalPhi>\n"
                + "            <retrograde>false</retrograde>\n"
                + "            <averageTemperature>250</averageTemperature>\n"
                + "            <rotationalPeriod>24000</rotationalPeriod>\n"
                + "            <atmosphereDensity>100</atmosphereDensity>\n"
                + "            <generateCraters>false</generateCraters>\n"
                + "            <generateCaves>true</generateCaves>\n"
                + "            <generateVolcanos>false</generateVolcanos>\n"
                + "        </planet>\n"
                + "    </star>\n"
                + "</galaxy>\n";
        Files.write(arConfigDir.resolve("planetDefs.xml"), xml.getBytes(StandardCharsets.UTF_8));

        serverHarness = RealDedicatedServerHarness.startWith(workDir, false);
        try {
            clientHarness = RealClientHarness.start(serverHarness);
        } catch (Exception ex) {
            try { serverHarness.close(); } catch (Exception cleanup) { ex.addSuppressed(cleanup); }
            serverHarness = null;
            throw ex;
        }
    }

    @After
    public void stopBoth() throws Exception {
        Exception deferred = null;
        if (clientHarness != null) {
            try { clientHarness.close(); } catch (Exception e) { deferred = e; }
            clientHarness = null;
        }
        if (serverHarness != null) {
            try { serverHarness.close(); }
            catch (Exception e) { if (deferred == null) deferred = e; else deferred.addSuppressed(e); }
            serverHarness = null;
        }
        if (deferred != null) throw deferred;
    }

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverHarness.client().execute(cmd));
    }

    private double doubleField(Pattern p, String src, String name) {
        Matcher m = p.matcher(src);
        assertTrue("field " + name + " missing in: " + src, m.find());
        return Double.parseDouble(m.group(1));
    }

    private String stringField(Pattern p, String src, String name) {
        Matcher m = p.matcher(src);
        assertTrue("field " + name + " missing in: " + src, m.find());
        return m.group(1);
    }

    private void waitForClientDim(int dim) throws Exception {
        for (int i = 0; i < 200; i++) {
            JsonObject w = clientHarness.bot().reportWeather();
            if (w != null && w.has("dim") && w.get("dim").getAsInt() == dim) return;
            clientHarness.bot().waitTicks(2);
        }
    }

    /** Counter-test: vanilla overworld is NOT an IPlanetaryProvider, so
     *  PlanetEventHandler.fallEvent skips the scaling branch entirely —
     *  the post-handler distance equals the input. */
    @Test
    public void aOverworldDoesNotScaleFallDistance() throws Exception {
        clientHarness.bot().waitForWorld();
        String resp = exec("artest player try-fall 20");
        // Sanity: overworld provider is not an IPlanetaryProvider.
        assertEquals("overworld must NOT be an IPlanetaryProvider; " + resp,
                "false", stringField(IS_PLANETARY, resp, "isPlanetaryProvider"));
        double input = doubleField(INPUT_DIST, resp, "inputDistance");
        double result = doubleField(RESULT_DIST, resp, "resultDistance");
        assertEquals("overworld fall distance must be unchanged by AR "
                + "handler; input=" + input + " result=" + result + " " + resp,
                input, result, 0.001);
    }

    /** Pin: on a low-grav AR dim the handler scales LivingFallEvent.distance
     *  by the provider's gravitational multiplier. With grav=0.17 and a
     *  20-block input fall, expected post-handler distance ≈ 3.4. */
    @Test
    public void bLowGravDimScalesFallDistanceByGravityMultiplier() throws Exception {
        clientHarness.bot().waitForWorld();
        exec("artest tp " + DIM_LOW_GRAV);
        waitForClientDim(DIM_LOW_GRAV);
        // Let the dim settle so the WorldProvider is fully initialised
        // before posting the synthetic event.
        clientHarness.bot().waitTicks(20);

        String resp = exec("artest player try-fall 20");
        assertEquals("low-grav AR dim must report as IPlanetaryProvider; " + resp,
                "true", stringField(IS_PLANETARY, resp, "isPlanetaryProvider"));
        double input = doubleField(INPUT_DIST, resp, "inputDistance");
        double result = doubleField(RESULT_DIST, resp, "resultDistance");
        double gravity = doubleField(GRAVITY, resp, "gravityMultiplier");
        // Cross-check the configured multiplier — planetDefs.xml had
        // <gravitationalMultiplier>17</gravitationalMultiplier> which AR
        // normalises to 0.17. Tolerate ±0.02 for any rounding inside
        // DimensionProperties.
        assertEquals("gravity multiplier must be ~0.17; " + resp,
                0.17, gravity, 0.02);
        // Pin the scaling: result = input * gravity, within a small
        // floating-point epsilon.
        assertEquals("low-grav AR dim must scale fall distance by gravity; "
                + "input=" + input + " gravity=" + gravity
                + " expected=" + (input * gravity) + " result=" + result
                + " " + resp,
                input * gravity, result, 0.05);
        // Sanity: result MUST be strictly less than input.
        assertTrue("scaled distance must be strictly less than input on a "
                + "low-grav dim; input=" + input + " result=" + result,
                result < input);
    }
}
