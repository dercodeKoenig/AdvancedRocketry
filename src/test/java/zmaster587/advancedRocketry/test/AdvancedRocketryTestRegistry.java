package zmaster587.advancedRocketry.test;

import com.github.stannismod.forge.testing.HeadlessGameTest;
import com.github.stannismod.forge.testing.TestRegistry;
import zmaster587.advancedRocketry.test.scenario.MachineRecipeIntegrationTest;
import zmaster587.advancedRocketry.test.scenario.NonARDimensionIsolationTest;
import zmaster587.advancedRocketry.test.scenario.PersistenceRestartSmokeTest;
import zmaster587.advancedRocketry.test.scenario.PlanetDimensionLoadTest;
import zmaster587.advancedRocketry.test.scenario.PlanetXmlConfigIntegrationTest;
import zmaster587.advancedRocketry.test.scenario.RegistrySmokeTest;
import zmaster587.advancedRocketry.test.scenario.RocketAssemblySmokeTest;
import zmaster587.advancedRocketry.test.scenario.RocketLaunchSmokeTest;
import zmaster587.advancedRocketry.test.scenario.ServerStartupSmokeTest;
import zmaster587.advancedRocketry.test.scenario.WeatherBaselineTest;
import zmaster587.advancedRocketry.test.scenario.WeatherPersistenceTest;

/**
 * Composes the canonical {@link TestRegistry} of AR scenarios (SMART §4 + §13 step 2).
 *
 * <p>Order matters for predictability: P0 scenarios run first so a failing
 * fundamental (no server start, no AR registry) gates the suite immediately.</p>
 *
 * <p>To run from the command line:</p>
 * <pre>{@code
 *   ./gradlew test --tests "zmaster587.advancedRocketry.test.AdvancedRocketryTestBootstrap*"
 * }</pre>
 *
 * <p>Or via the dedicated Gradle task (see SMART §11):</p>
 * <pre>{@code
 *   ./gradlew testAdvancedRocketryScenarios
 * }</pre>
 */
public final class AdvancedRocketryTestRegistry {

    private AdvancedRocketryTestRegistry() {}

    public static TestRegistry composeAll() {
        TestRegistry registry = new TestRegistry();
        for (HeadlessGameTest test : p0Scenarios()) {
            registry.register(test);
        }
        for (HeadlessGameTest test : p1Scenarios()) {
            registry.register(test);
        }
        return registry;
    }

    public static TestRegistry composeP0Only() {
        TestRegistry registry = new TestRegistry();
        for (HeadlessGameTest test : p0Scenarios()) {
            registry.register(test);
        }
        return registry;
    }

    /** SMART §8 P0 — must run before weather B1 refactor. */
    private static HeadlessGameTest[] p0Scenarios() {
        return new HeadlessGameTest[] {
                new ServerStartupSmokeTest(),
                new RegistrySmokeTest(),
                new PlanetDimensionLoadTest(),
                new PlanetXmlConfigIntegrationTest(),
                new WeatherBaselineTest(),
                new WeatherPersistenceTest(),
                new NonARDimensionIsolationTest(),
        };
    }

    /** SMART §8 P1 — core gameplay protection. */
    private static HeadlessGameTest[] p1Scenarios() {
        return new HeadlessGameTest[] {
                new MachineRecipeIntegrationTest(),
                new RocketAssemblySmokeTest(),
                new RocketLaunchSmokeTest(),
                new PersistenceRestartSmokeTest(),
        };
    }
}
