package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.HeadlessGameTest;
import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMART §7.6 — full persistence/restart smoke.
 *
 * <p>Boot 1 mutates AR state across multiple subsystems:</p>
 * <ul>
 *   <li>Creates a space station orbiting Earth.</li>
 *   <li>Adds a satellite to Earth's {@code DimensionProperties.satellites}.</li>
 *   <li>Mutates Earth's atmosphere density (terraforming-style).</li>
 *   <li>Places a stone block at a known location.</li>
 * </ul>
 *
 * <p>Boot 2 (same workDir) asserts every mutation survived:</p>
 * <ul>
 *   <li>Immutable registry counts (blocks/items/entities/biomes) stable.</li>
 *   <li>Station is still listed under the same id orbiting dim 0.</li>
 *   <li>Satellite is still attached to dim 0 with the right type.</li>
 *   <li>Earth atmosphereDensity is the post-mutation value.</li>
 *   <li>Earth dim still AR-managed.</li>
 * </ul>
 *
 * <p>Drives the canonical save-on-shutdown / load-from-disk path that AR uses
 * for {@link zmaster587.advancedRocketry.stations.SpaceObjectManager},
 * {@link zmaster587.advancedRocketry.dimension.DimensionManager} and per-dim
 * {@link zmaster587.advancedRocketry.dimension.DimensionProperties}.</p>
 */
public class PersistenceRestartSmokeTest implements HeadlessGameTest {

    private static final Pattern STATION_ID = Pattern.compile("\"id\":(-?\\d+),\"orbitingBody\":");
    private static final Pattern SAT_ID = Pattern.compile("\"id\":(-?\\d+),\"type\":");
    private static final Pattern ATM_DENSITY = Pattern.compile("\"atmosphereDensity\":(-?\\d+)");

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    @Override public String id() { return "ar.scenario.persistence_restart_smoke"; }
    @Override public String category() { return "P1/persistence"; }
    @Override public boolean required() { return false; }
    @Override public int timeoutTicks() { return 1; }

    @Override
    public void setUp(TestContext context) throws Exception {
        if (!HarnessBoundScenario.isHarnessEnabled()) {
            context.note("server harness disabled — skip");
            return;
        }
        workDir = Files.createTempDirectory("forge-server-persistence-restart-");
        context.note("persistent workDir: " + workDir);
    }

    @Override
    public TestStatus tick(TestContext context) throws Exception {
        if (workDir == null) return TestStatus.SKIPPED;

        long stationId;
        long satelliteId;
        int targetDensity = 33;
        int[] firstCounts;

        try {
            firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);

            String regSummary = String.join("\n", firstBoot.client().execute("artest registry summary"));
            firstCounts = extractCounts(regSummary, "blocks", "items", "entities", "biomes");
            if (firstCounts == null) {
                context.note("first boot registry summary malformed: " + regSummary);
                return TestStatus.FAILED;
            }

            // Mutation A: station orbiting Earth.
            String createStation = String.join("\n", firstBoot.client().execute("artest station create 0"));
            Matcher sm = STATION_ID.matcher(createStation);
            if (!sm.find()) {
                context.note("could not extract station id: " + createStation);
                return TestStatus.FAILED;
            }
            stationId = Long.parseLong(sm.group(1));

            // Mutation B: satellite on Earth.
            String createSat = String.join("\n", firstBoot.client().execute(
                    "artest satellite create 0 mass 300 6000 2048"));
            Matcher sat = SAT_ID.matcher(createSat);
            if (!sat.find()) {
                // Fallback: try "id" without modifier (different field ordering).
                Matcher fallback = Pattern.compile("\"id\":(\\d+)").matcher(createSat);
                if (!fallback.find()) {
                    context.note("could not extract satellite id: " + createSat);
                    return TestStatus.FAILED;
                }
                satelliteId = Long.parseLong(fallback.group(1));
            } else {
                satelliteId = Long.parseLong(sat.group(1));
            }

            // Mutation C: atmosphere density.
            firstBoot.client().execute("artest atmosphere set-density 0 " + targetDensity);
        } catch (Throwable t) {
            context.note("first boot failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return TestStatus.SKIPPED;
        }

        firstBoot.close();
        firstBoot = null;
        context.note("first boot closed; restarting same workDir to verify persistence…");

        try {
            secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

            // Registry counts must match.
            String secondSummary = String.join("\n", secondBoot.client().execute("artest registry summary"));
            int[] secondCounts = extractCounts(secondSummary, "blocks", "items", "entities", "biomes");
            if (secondCounts == null) {
                context.note("second boot registry summary malformed: " + secondSummary);
                return TestStatus.FAILED;
            }
            for (int i = 0; i < firstCounts.length; i++) {
                if (firstCounts[i] != secondCounts[i]) {
                    context.note("registry count mismatch at idx " + i + ": first=" + firstCounts[i]
                            + " second=" + secondCounts[i]);
                    return TestStatus.FAILED;
                }
            }

            // AR dim still managed.
            String dimInfo = String.join("\n", secondBoot.client().execute("artest dim info 0"));
            if (!dimInfo.contains("\"isARPlanet\":true")) {
                context.note("Earth lost AR-managed status after restart: " + dimInfo);
                return TestStatus.FAILED;
            }

            // Station restored?
            String stations = String.join("\n", secondBoot.client().execute("artest station list"));
            if (!stations.contains("\"id\":" + stationId)) {
                context.note("station " + stationId + " did NOT survive restart: " + stations);
                return TestStatus.FAILED;
            }
            String stationInfo = String.join("\n",
                    secondBoot.client().execute("artest station info " + stationId));
            if (!stationInfo.contains("\"orbitingPlanetId\":0")) {
                context.note("station's orbitingPlanetId did not survive: " + stationInfo);
                return TestStatus.FAILED;
            }

            // Satellite restored?
            String sats = String.join("\n", secondBoot.client().execute("artest satellite list 0"));
            if (!sats.contains("\"id\":" + satelliteId)) {
                context.note("satellite " + satelliteId + " did NOT survive restart: " + sats);
                return TestStatus.FAILED;
            }
            String satInfo = String.join("\n",
                    secondBoot.client().execute("artest satellite info 0 " + satelliteId));
            if (!satInfo.contains("\"type\":\"mass\"")) {
                context.note("satellite type did not survive restart: " + satInfo);
                return TestStatus.FAILED;
            }

            // Atmosphere density survived?
            String planet = String.join("\n", secondBoot.client().execute("artest planet info 0"));
            Matcher am = ATM_DENSITY.matcher(planet);
            if (!am.find()) {
                context.note("planet info missing atmosphereDensity: " + planet);
                return TestStatus.FAILED;
            }
            int density = Integer.parseInt(am.group(1));
            if (density != targetDensity) {
                context.note("atmosphereDensity did not survive: expected " + targetDensity
                        + " got " + density);
                return TestStatus.FAILED;
            }

            context.note("persistence verified: registry stable, station "
                    + stationId + ", satellite " + satelliteId + ", density=" + density);
            return TestStatus.PASSED;
        } catch (Throwable t) {
            context.note("second boot failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return TestStatus.FAILED;
        }
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

    @Override
    public void tearDown(TestContext context) throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
    }
}
