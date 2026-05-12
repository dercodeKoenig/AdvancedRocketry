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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.6 — full persistence/restart smoke.
 *
 * Boot 1 creates a station + satellite + mutates Earth atmosphere density.
 * Boot 2 (same workDir) verifies every mutation survived save/load + registry
 * counts are stable.
 */
public class PersistenceRestartSmokeTest {

    private static final Pattern STATION_ID = Pattern.compile("\"id\":(-?\\d+),\"orbitingBody\":");
    private static final Pattern SAT_ID_FALLBACK = Pattern.compile("\"id\":(\\d+)");
    private static final Pattern ATM_DENSITY = Pattern.compile("\"atmosphereDensity\":(-?\\d+)");

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    @Before
    public void prepareWorkDir() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -Dforge.test.harness.enabled=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        workDir = Files.createTempDirectory("forge-server-persistence-restart-");
    }

    @After
    public void closeAll() throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
    }

    @Test
    public void stationAndSatelliteAndDensitySurviveRestart() throws Exception {
        long stationId;
        long satelliteId;
        int targetDensity = 33;
        int[] firstCounts;

        firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);

        String regSummary = String.join("\n", firstBoot.client().execute("artest registry summary"));
        firstCounts = extractCounts(regSummary, "blocks", "items", "entities", "biomes");
        assertTrue("first boot registry summary malformed: " + regSummary, firstCounts != null);

        // Mutation A: station orbiting Earth.
        String createStation = String.join("\n", firstBoot.client().execute("artest station create 0"));
        Matcher sm = STATION_ID.matcher(createStation);
        assertTrue("could not extract station id: " + createStation, sm.find());
        stationId = Long.parseLong(sm.group(1));

        // Mutation B: satellite on Earth.
        String createSat = String.join("\n", firstBoot.client().execute(
                "artest satellite create 0 mass 300 6000 2048"));
        Matcher sat = SAT_ID_FALLBACK.matcher(createSat);
        assertTrue("could not extract satellite id: " + createSat, sat.find());
        satelliteId = Long.parseLong(sat.group(1));

        // Mutation C: atmosphere density.
        firstBoot.client().execute("artest atmosphere set-density 0 " + targetDensity);

        firstBoot.close();
        firstBoot = null;

        secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        String secondSummary = String.join("\n", secondBoot.client().execute("artest registry summary"));
        int[] secondCounts = extractCounts(secondSummary, "blocks", "items", "entities", "biomes");
        assertTrue("second boot registry summary malformed: " + secondSummary, secondCounts != null);
        for (int i = 0; i < firstCounts.length; i++) {
            assertEquals("registry count mismatch at idx " + i,
                    firstCounts[i], secondCounts[i]);
        }

        String dimInfo = String.join("\n", secondBoot.client().execute("artest dim info 0"));
        assertTrue("Earth lost AR-managed status after restart: " + dimInfo,
                dimInfo.contains("\"isARPlanet\":true"));

        String stations = String.join("\n", secondBoot.client().execute("artest station list"));
        assertTrue("station " + stationId + " did NOT survive restart: " + stations,
                stations.contains("\"id\":" + stationId));
        String stationInfo = String.join("\n",
                secondBoot.client().execute("artest station info " + stationId));
        assertTrue("station's orbitingPlanetId did not survive: " + stationInfo,
                stationInfo.contains("\"orbitingPlanetId\":0"));

        String sats = String.join("\n", secondBoot.client().execute("artest satellite list 0"));
        assertTrue("satellite " + satelliteId + " did NOT survive restart: " + sats,
                sats.contains("\"id\":" + satelliteId));
        String satInfo = String.join("\n",
                secondBoot.client().execute("artest satellite info 0 " + satelliteId));
        assertTrue("satellite type did not survive restart: " + satInfo,
                satInfo.contains("\"type\":\"mass\""));

        String planet = String.join("\n", secondBoot.client().execute("artest planet info 0"));
        Matcher am = ATM_DENSITY.matcher(planet);
        assertTrue("planet info missing atmosphereDensity: " + planet, am.find());
        assertEquals("atmosphereDensity did not survive",
                targetDensity, Integer.parseInt(am.group(1)));
    }

    private static int[] extractCounts(String json, String... keys) {
        int[] result = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            String needle = "\"" + keys[i] + "\":";
            int idx = json.indexOf(needle);
            if (idx < 0) return null;
            int start = idx + needle.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
            try {
                result[i] = Integer.parseInt(json.substring(start, end));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result;
    }
}
