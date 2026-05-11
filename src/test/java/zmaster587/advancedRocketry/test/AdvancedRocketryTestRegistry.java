package zmaster587.advancedRocketry.test;

import com.github.stannismod.forge.testing.HeadlessGameTest;
import com.github.stannismod.forge.testing.TestRegistry;
import zmaster587.advancedRocketry.test.scenario.AtmosphereOxygenSmokeTest;
import zmaster587.advancedRocketry.test.scenario.ClientConnectSmokeTest;
import zmaster587.advancedRocketry.test.scenario.CommandsSmokeTest;
import zmaster587.advancedRocketry.test.scenario.EnergySystemsSmokeTest;
import zmaster587.advancedRocketry.test.scenario.GuidanceComputerGuiE2ETest;
import zmaster587.advancedRocketry.test.scenario.MachineRecipeIntegrationTest;
import zmaster587.advancedRocketry.test.scenario.OxygenSuitClientStateE2ETest;
import zmaster587.advancedRocketry.test.scenario.PlanetSelectorGuiE2ETest;
import zmaster587.advancedRocketry.test.scenario.RocketBuilderGuiE2ETest;
import zmaster587.advancedRocketry.test.scenario.WeatherClientSyncE2ETest;
import zmaster587.advancedRocketry.test.scenario.MultiblockValidationSmokeTest;
import zmaster587.advancedRocketry.test.scenario.NonARDimensionIsolationTest;
import zmaster587.advancedRocketry.test.scenario.PersistenceRestartSmokeTest;
import zmaster587.advancedRocketry.test.scenario.PipeNetworkSmokeTest;
import zmaster587.advancedRocketry.test.scenario.PlanetDimensionLoadTest;
import zmaster587.advancedRocketry.test.scenario.PlanetXmlConfigIntegrationTest;
import zmaster587.advancedRocketry.test.scenario.RegistrySmokeTest;
import zmaster587.advancedRocketry.test.scenario.RocketAssemblySmokeTest;
import zmaster587.advancedRocketry.test.scenario.RocketInfrastructureSmokeTest;
import zmaster587.advancedRocketry.test.scenario.RocketLaunchSmokeTest;
import zmaster587.advancedRocketry.test.scenario.SatelliteLifecycleSmokeTest;
import zmaster587.advancedRocketry.test.scenario.ServerStartupSmokeTest;
import zmaster587.advancedRocketry.test.scenario.SpaceStationLifecycleSmokeTest;
import zmaster587.advancedRocketry.test.scenario.SpecialInfrastructureSmokeTest;
import zmaster587.advancedRocketry.test.scenario.TerraformingSmokeTest;
import zmaster587.advancedRocketry.test.scenario.WeatherBaselineTest;
import zmaster587.advancedRocketry.test.scenario.WeatherPersistenceTest;
import zmaster587.advancedRocketry.test.scenario.WorldgenSmokeTest;

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
        for (HeadlessGameTest test : p2Scenarios()) {
            registry.register(test);
        }
        for (HeadlessGameTest test : clientE2EScenarios()) {
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
                new MultiblockValidationSmokeTest(),
                new RocketAssemblySmokeTest(),
                new RocketLaunchSmokeTest(),
                new RocketInfrastructureSmokeTest(),
                new SpaceStationLifecycleSmokeTest(),
                new SatelliteLifecycleSmokeTest(),
                new AtmosphereOxygenSmokeTest(),
                new PersistenceRestartSmokeTest(),
        };
    }

    /** SMART §8 P2 — broad feature coverage. */
    private static HeadlessGameTest[] p2Scenarios() {
        return new HeadlessGameTest[] {
                new TerraformingSmokeTest(),
                new WorldgenSmokeTest(),
                new EnergySystemsSmokeTest(),
                new PipeNetworkSmokeTest(),
                new SpecialInfrastructureSmokeTest(),
                new CommandsSmokeTest(),
        };
    }

    /** SMART §7.20 — client E2E (require display + AR client bridge). */
    private static HeadlessGameTest[] clientE2EScenarios() {
        return new HeadlessGameTest[] {
                new ClientConnectSmokeTest(),
                new PlanetSelectorGuiE2ETest(),
                new GuidanceComputerGuiE2ETest(),
                new RocketBuilderGuiE2ETest(),
                new WeatherClientSyncE2ETest(),
                new OxygenSuitClientStateE2ETest(),
        };
    }
}
