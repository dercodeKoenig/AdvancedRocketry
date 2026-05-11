# Advanced Rocketry — Test Suite

This source set implements the SMART test plan
(`docs/advanced_rocketry_full_test_suite_smart.md` upstream of this branch).

## Layout

```
src/test/java/zmaster587/advancedRocketry/test/
├── AdvancedRocketryTestBootstrap.java     # JUnit + main(String[]) entry-point
├── AdvancedRocketryTestRegistry.java      # Composes scenario list (P0 + P1)
├── AdvancedRocketryTestConstants.java     # Shared constants and -D flag names
├── MinecraftBootstrap.java                # Idempotent MC + AR proxy init for unit tests
├── FrameworkWiringSmokeTest.java          # Verifies the framework jar is on the test classpath
├── unit/                                  # Pure-Java / lightweight bootstrap unit tests (§6)
│   ├── ARConfigurationTest.java
│   ├── AstronomicalBodyHelperTest.java
│   ├── AtmosphereLogicTest.java
│   ├── DimensionPropertiesTest.java
│   ├── FuelRegistryTest.java
│   ├── PacketSerializationTest.java
│   ├── PlanetWeatherStateTest.java        # @Ignore stubs for future B1 model
│   ├── SatellitePropertiesTest.java
│   ├── SealableBlockHandlerTest.java
│   ├── SpacePositionTest.java
│   ├── StatsRocketTest.java
│   └── XMLPlanetLoaderTest.java
└── scenario/                              # HeadlessGameTest scenarios (§7)
    ├── HarnessBoundScenario.java          # Base — graceful SKIP when harness disabled
    ├── ServerStartupSmokeTest.java        # P0 §7.1
    ├── RegistrySmokeTest.java             # P0 §7.2
    ├── PlanetDimensionLoadTest.java       # P0 §7.3
    ├── PlanetXmlConfigIntegrationTest.java# P0 §7.4 (skeleton)
    ├── WeatherBaselineTest.java           # P0 §7.5
    ├── WeatherPersistenceTest.java        # P0 §7.5 (skeleton)
    ├── NonARDimensionIsolationTest.java   # P0 isolation
    ├── MachineRecipeIntegrationTest.java  # P1 §7.7 (skeleton)
    ├── RocketAssemblySmokeTest.java       # P1 §7.9 (skeleton)
    ├── RocketLaunchSmokeTest.java         # P1 §7.9 (skeleton)
    └── PersistenceRestartSmokeTest.java   # P1 §7.6 (skeleton)
```

The reusable test framework lives in `libs/test/forge-test-framework-*.jar`
(built from `C:\Users\Quarter\Documents\Modding\ForgeTestFramework`). All
AR-specific code stays in this module — per SMART §15 framework jars must
stay free of AR imports.

## Running

```bash
# All tests (unit + scenario suite). Scenario suite runs in "harness disabled"
# mode by default — see "Server harness" below.
./gradlew test

# Only the AR scenario suite (P0 + P1):
./gradlew testAdvancedRocketryScenarios

# Override expected weather mode for §7.5 baseline scenario:
./gradlew testAdvancedRocketryScenarios -Pweather=shared          # default
./gradlew testAdvancedRocketryScenarios -Pweather=per_dimension   # post-B1

# Targeted unit-test class:
./gradlew test --tests "zmaster587.advancedRocketry.test.unit.SpacePositionTest"
```

The Gradle `test` task always passes `-Dadvancedrocketry.tests=true`, which
gates the test-only `/artest` server probe commands in
`zmaster587.advancedRocketry.command.test.TestProbeCommand`.

## Reports

Both JUnit's report (`build/reports/tests/test/index.html`) and the
framework-native report (`summary.txt` + `summary.json` produced by
`TestReportWriter`) are written. The framework report ends up in the
JUnit-managed temp directory of `AdvancedRocketryTestBootstrap.scenarioSuiteRunsAndProducesSummary`
and is captured in the test-report attachments when running through CI.

## Server harness

`HarnessBoundScenario` defaults to SKIPPED in `setUp()` because:

1. `RealDedicatedServerHarness` requires `GradleStartServer` on the test JVM
   classpath, which only ForgeGradle's `runServer` task provides — the
   standard `gradle test` task does not.
2. The framework hardcodes RetroFutura's MC asset cache layout; AR uses
   ForgeGradle 6 with a different layout. Direct invocation will time out
   waiting for the server log line that never arrives.

To opt INTO real harness invocation:

```bash
./gradlew test -Dadvancedrocketry.tests.harness=true
```

Today this will currently SKIP every harness-bound scenario with a clear
explanation in `summary.txt`. Once a `runServer`-classpath-aware Gradle task
is added (or once the framework is taught the FG6 cache layout) the same
scenarios will start producing PASSED/FAILED outcomes.

## Test-only `/artest` probe commands

Implemented in `src/main/java/zmaster587/advancedRocketry/command/test/TestProbeCommand.java`,
registered ONLY when `-Dadvancedrocketry.tests=true` is set, via
`TestProbeCommandRegistration.registerIfTestMode(event)` (called from
`AdvancedRocketry.serverStarting`).

Currently implemented sub-commands:

| Command | Maps to SMART | Notes |
|---|---|---|
| `/artest registry summary` | §5.1 | block/item/entity/biome/enchantment/recipe/fluid counts |
| `/artest dim list` | §5.2 | AR + Forge dim arrays |
| `/artest dim info <dim>` | §5.2 | provider class, AR-managed flag, basic planet metadata |
| `/artest planet info <dim>` | §5.3 | full DimensionProperties snapshot |
| `/artest weather get <dim>` | §5.3 | vanilla-facing weather state |
| `/artest weather set <dim> <clear\|rain\|thunder> <ticks>` | §5.3 | deterministic weather setter |
| `/artest rocket list [dim]` | §5.5 | EntityRocket entities across loaded worlds |
| `/artest rocket info <entityId>` | §5.5 | flight/orbit/fuel/storage state |
| `/artest station list` | §5.6 | known SpaceObjects with orbiting body |
| `/artest station info <id>` | §5.6 | orbital state + spawn pos + fuel |
| `/artest satellite list <dim>` | §5.6 | satellites associated with dim |
| `/artest satellite info <dim> <id>` | §5.6 | satellite type, power, data |
| `/artest atmosphere get <dim> <x> <y> <z>` | §5.7 | per-block atmosphere (handler or dim default) |
| `/artest oxygen player <name>` | §5.7 | player's current atmosphere + pressure |

Still pending (need additional probes): `/artest machine info`,
`/artest machine tick-until`, `/artest rocket assemble`, `/artest rocket launch`,
`/artest terraforming info`, `/artest worldgen sample`.

## Known limitations / deferred work

- **Server harness wiring**: see *Server harness* above.
- **Scenario fixtures**: `PlanetXmlConfigIntegrationTest`,
  `RocketAssemblySmokeTest`, `RocketLaunchSmokeTest`, `MachineRecipeIntegrationTest`
  are skeletons that report SKIPPED with deferred-work notes — they need
  fixture-XML or `/artest` probe extensions before they can assert real state.
- **Per-tile assertion probes**: rocket / station / machine state assertions
  require additional `/artest` sub-commands (SMART §5.4–§5.8). The scenario
  classes are wired and registered; only the probe surface is missing.

## Latent bugs documented (not fixed per SMART §3)

These tests **pass** intentionally — they document existing production behavior
so that a future fix flips them to FAILED and surfaces a tripwire:

| Test | Bug |
|---|---|
| `StatsRocketTest.createFromNbtCurrentlyLosesAllFields_documented` | `StatsRocket.createFromNBT(outer)` double-unwraps `rocketStats` and loses every field. Production callers all use `stats.readFromNBT(outer)` directly so the bug is latent. |
| `DimensionPropertiesTest.getGeodeMultiplierReturnsVolcanoMultiplier_documented` | `DimensionProperties.getGeodeMultiplier()` returns `volcanoFrequencyMultiplier` instead of `geodeFrequencyMultiplier` (copy-paste error). The on-wire NBT field is correct, only the accessor is wrong. |
