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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10b Phase 4 — sleep and flint-and-steel guards in vacuum dims.
 *
 * <p>Pins two production handlers in
 * {@link zmaster587.advancedRocketry.event.PlanetEventHandler}:</p>
 *
 * <ul>
 *   <li><b>{@code sleepEvent}</b> (lines 237-249) — a vacuum
 *       (non-breathable) AR dim must refuse sleep via
 *       {@code event.setResult(SleepResult.OTHER_PROBLEM)}.</li>
 *   <li><b>{@code blockRightClicked}</b> (lines 281-294) — a vacuum
 *       (no-combustion) AR dim must cancel right-clicks holding
 *       flint+steel / fire-charge / blaze-powder / blaze-rod.</li>
 * </ul>
 *
 * <p>Both guards fire only when the dim has an {@code AtmosphereHandler}
 * registered AND the atmosphere is non-breathable / no-combustion, so
 * the breathable AR-dim counter-tests prove the gate is atmosphere-typed
 * (not just "always cancel on AR dim").</p>
 *
 * <p>Drives the guards through {@code /artest player try-sleep} and
 * {@code /artest player try-ignite}: synthetic event posts that exercise
 * the AR handler in isolation, sidestepping the vanilla bed-right-click
 * pre-checks (night-time, hostile-mobs nearby) and the flint+steel block
 * mutation. The pin is on the AR handler's decision, not on the
 * downstream vanilla bookkeeping.</p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VacuumGuardsE2ETest {

    private static final int DIM_VAC = 9601;
    private static final int DIM_AIR = 9602;

    private static final Pattern SLEEP_RESULT =
            Pattern.compile("\"resultStatus\":\"([^\"]*)\"");
    private static final Pattern CANCELED =
            Pattern.compile("\"canceled\":(true|false)");

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

        workDir = Files.createTempDirectory("forge-client-vac-guards-");
        Path arConfigDir = workDir.resolve("config").resolve("advRocketry");
        Files.createDirectories(arConfigDir);
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<galaxy>\n"
                + "    <star name=\"Sol\" temp=\"100\" x=\"0\" y=\"0\" size=\"1.0\" "
                + "          isBlackHole=\"false\" diskAngle=\"70\" "
                + "          numPlanets=\"2\" numGasGiants=\"0\">\n"
                + planetXml("VacuumPlanet", DIM_VAC, 0)
                + planetXml("AirPlanet", DIM_AIR, 100)
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

    private static String planetXml(String name, int dim, int atmosDensity) {
        return "        <planet name=\"" + name + "\" DIMID=\"" + dim + "\">\n"
                + "            <isKnown>true</isKnown>\n"
                + "            <fogColor>0.5,0.5,0.5</fogColor>\n"
                + "            <skyColor>0.4,0.6,0.9</skyColor>\n"
                + "            <gravitationalMultiplier>100</gravitationalMultiplier>\n"
                + "            <orbitalDistance>100</orbitalDistance>\n"
                + "            <orbitalTheta>0</orbitalTheta>\n"
                + "            <orbitalPhi>0</orbitalPhi>\n"
                + "            <retrograde>false</retrograde>\n"
                + "            <averageTemperature>250</averageTemperature>\n"
                + "            <rotationalPeriod>24000</rotationalPeriod>\n"
                + "            <atmosphereDensity>" + atmosDensity + "</atmosphereDensity>\n"
                + "            <generateCraters>false</generateCraters>\n"
                + "            <generateCaves>true</generateCaves>\n"
                + "            <generateVolcanos>false</generateVolcanos>\n"
                + "        </planet>\n";
    }

    private String exec(String cmd) throws Exception {
        return String.join("\n", serverHarness.client().execute(cmd));
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

    /** Ensures the dim's AtmosphereHandler is installed and the player's
     *  per-tick atmosphere refresh has run, so the handler-side guards
     *  see a fully-initialised atmosphere when they query it. */
    private void enterDim(int dim) throws Exception {
        exec("artest tp " + dim);
        waitForClientDim(dim);
        clientHarness.bot().waitTicks(40);
    }

    /** Pin: posting PlayerSleepInBedEvent at a vacuum-dim coordinate
     *  goes through PlanetEventHandler.sleepEvent and emerges with
     *  {@code resultStatus == OTHER_PROBLEM}. */
    @Test
    public void aSleepInVacuumDimIsRefused() throws Exception {
        enterDim(DIM_VAC);
        String resp = exec("artest player try-sleep");
        String status = stringField(SLEEP_RESULT, resp, "resultStatus");
        assertEquals("sleep in vacuum dim must be refused with OTHER_PROBLEM; "
                + resp, "OTHER_PROBLEM", status);
    }

    /** Counter-test: a breathable AR dim must NOT refuse with
     *  OTHER_PROBLEM — the vacuum gate must depend on
     *  isBreathable(), not on \"is AR dim\". */
    @Test
    public void bSleepInBreathableArDimNotRefusedByVacuumGate() throws Exception {
        enterDim(DIM_AIR);
        String resp = exec("artest player try-sleep");
        String status = stringField(SLEEP_RESULT, resp, "resultStatus");
        // Vanilla EntityPlayer.SleepResult has OK, NOT_POSSIBLE_HERE,
        // NOT_POSSIBLE_NOW, TOO_FAR_AWAY, OTHER_PROBLEM, NOT_SAFE.
        // The AR handler ONLY sets OTHER_PROBLEM in vacuum; in a
        // breathable dim it leaves the result alone (null when no
        // other handler ran). Any value EXCEPT OTHER_PROBLEM proves
        // the AR guard didn't fire.
        assertNotEquals("breathable AR dim must NOT be refused by the "
                + "vacuum-sleep gate; resultStatus=" + status + " " + resp,
                "OTHER_PROBLEM", status);
    }

    /** Pin: posting RightClickBlock with flint+steel in a vacuum dim
     *  emerges canceled. */
    @Test
    public void cFlintInVacuumDimDoesNotIgnite() throws Exception {
        enterDim(DIM_VAC);
        String resp = exec("artest player try-ignite");
        String canceled = stringField(CANCELED, resp, "canceled");
        assertEquals("flint-and-steel right-click in vacuum dim must be "
                + "canceled by PlanetEventHandler.blockRightClicked; " + resp,
                "true", canceled);
    }

    /** Counter-test: same right-click in a breathable AR dim must NOT
     *  be canceled by the no-combustion gate. */
    @Test
    public void dFlintInBreathableArDimDoesIgnite() throws Exception {
        enterDim(DIM_AIR);
        String resp = exec("artest player try-ignite");
        String canceled = stringField(CANCELED, resp, "canceled");
        assertEquals("flint-and-steel right-click in breathable AR dim "
                + "must NOT be canceled (combustion allowed); " + resp,
                "false", canceled);
    }
}
