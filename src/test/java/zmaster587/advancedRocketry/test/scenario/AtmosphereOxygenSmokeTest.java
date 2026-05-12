package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * SMART §7.13 — atmosphere / oxygen gameplay.
 *
 * <ol>
 *   <li>Earth (dim=0) reports {@code breathable=true} by default.</li>
 *   <li>Force the Earth atmosphere density to 0 via {@code /artest atmosphere
 *       set-density 0 0} — production
 *       {@link zmaster587.advancedRocketry.dimension.DimensionProperties#setAtmosphereDensity(int)}
 *       must atomically flip the dimension's default
 *       {@link zmaster587.advancedRocketry.api.IAtmosphere} to a non-breathable
 *       variant.</li>
 *   <li>Probe atmosphere again — must now report {@code breathable=false}.</li>
 *   <li>Restore Earth's density (100) so other scenarios in the same JVM see a
 *       sane planet.</li>
 * </ol>
 *
 * <p>Suit-protection paths exercise the same {@code IAtmosphere.isBreathable()}
 * gate and are covered indirectly. Client-visible damage state is
 * {@code OxygenSuitClientStateE2ETest}.</p>
 */
public class AtmosphereOxygenSmokeTest extends HarnessBoundScenario {

    @Override public String id() { return "ar.scenario.atmosphere_oxygen_smoke"; }
    @Override public String category() { return "P1/atmosphere"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // 1. Baseline: Earth breathable.
        String baseline = String.join("\n", client.execute("artest atmosphere get 0 0 70 0"));
        if (baseline.contains("\"error\"")) {
            context.note("baseline atmosphere probe errored: " + baseline);
            return TestStatus.FAILED;
        }
        if (!baseline.contains("\"breathable\":true")) {
            context.note("baseline Earth not breathable — env contamination? " + baseline);
            return TestStatus.FAILED;
        }

        // Capture original density for restoration. Planet info reports it.
        String planet = String.join("\n", client.execute("artest planet info 0"));
        int originalDensity = extractInt(planet, "\"atmosphereDensity\":(-?\\d+)");
        if (originalDensity < 0) {
            context.note("could not read Earth atmosphereDensity from: " + planet);
            return TestStatus.FAILED;
        }

        try {
            // 2. Push density to 0 (full vacuum).
            String setResp = String.join("\n", client.execute("artest atmosphere set-density 0 0"));
            if (!setResp.contains("\"ok\":true")) {
                context.note("set-density failed: " + setResp);
                return TestStatus.FAILED;
            }
            if (!setResp.contains("\"newDensity\":0")) {
                context.note("set-density did not stick: " + setResp);
                return TestStatus.FAILED;
            }

            // 3. Atmosphere must now be non-breathable.
            String vacResp = String.join("\n", client.execute("artest atmosphere get 0 0 70 0"));
            if (!vacResp.contains("\"breathable\":false")) {
                context.note("density=0 should produce non-breathable atmosphere, got: " + vacResp);
                return TestStatus.FAILED;
            }
            context.note("density=0 → breathable=false (production IAtmosphere lookup)");
        } finally {
            // 4. Restore density so persistence / weather scenarios after us see
            //    a sane Earth.
            client.execute("artest atmosphere set-density 0 " + originalDensity);
        }

        return TestStatus.PASSED;
    }

    private static int extractInt(String haystack, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(haystack);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
}
