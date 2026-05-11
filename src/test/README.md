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

## Server harness (FG6 wiring)

The `testAdvancedRocketryScenarios` Gradle task spins up a **real** Forge
1.12.2 dedicated server per scenario via the reusable test framework's
`RealDedicatedServerHarness`. This required two pieces of plumbing:

1. **Test framework v0.2.0** ([forge-test-framework-0.2.0-dev.jar](../../libs/test/forge-test-framework-0.2.0-dev.jar)) —
   adds three system properties so the harness can target either RFG or FG6
   layouts:
   - `forge.test.launcher.class.server` — main class (default `GradleStartServer`)
   - `forge.test.assets.dir` — MC assets dir
   - `forge.test.launcher.legacyArgs` — toggle the RFG-style 20-arg launcher header

   Plus a fast-fail in `TestClient.awaitMarker`: if the server JVM exits before
   the expected stdout marker, the assertion now reports the crash immediately
   rather than blocking 3 minutes.

2. **AR build glue** ([build.gradle.kts](../../build.gradle.kts) `tasks.testAdvancedRocketryScenarios`):
   - Augments the test classpath with FG6's `runServer` classpath (so the spawned
     JVM has `net.minecraftforge.legacydev.MainServer` + the full MC dev cp)
   - Forwards the FG6-specific system properties listed above
   - Reflects out FG6's `RunConfig.environment + properties` and replays them
     through token-resolution so the server JVM gets `mainClass`, `tweakClass`,
     `MCP_TO_SRG`, `MCP_MAPPINGS`, `FORGE_VERSION`, `FORGE_GROUP`, `MC_VERSION`,
     `forge.logging.console.level`, `net.minecraftforge.gradle.GradleStart.csvDir`
     and `.srg.notch-srg`. Without this the server crashes with
     `Must specify mainClass environment variable`.

By default the harness is **enabled** when running `testAdvancedRocketryScenarios`
and **disabled** when running `test` (because the unit-test task lacks the runServer
classpath). Override with:

```bash
./gradlew test -Dadvancedrocketry.tests.harness=true            # not generally useful
./gradlew testAdvancedRocketryScenarios -Pharness=false         # skip server boot
```

### Diagnostic helper

[HarnessDiagnosticTest](java/zmaster587/advancedRocketry/test/HarnessDiagnosticTest.java)
boots ONE server, dumps the transcript regardless of outcome, and is registered
in the `testAdvancedRocketryScenarios` filter so you can run it as:

```bash
./gradlew testAdvancedRocketryScenarios \
    --tests "zmaster587.advancedRocketry.test.HarnessDiagnosticTest"
```

Useful when the harness setup is being debugged — output goes to
`build/reports/tests/testAdvancedRocketryScenarios/...HarnessDiagnosticTest.html`.

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

### Active server-startup bug surfaced by the harness

When the dedicated-server harness is enabled, every `HarnessBoundScenario`
currently SKIPPED with this message:

```
Server process exited (code=0) before marker 'For help, type "help" or "?"' appeared.
…
NullPointerException at TileElectricArcFurnace.getAllowableWildCardBlocks(TileElectricArcFurnace.java:63)
  list.addAll(TileMultiBlock.getMapping('O'))   ← null
  at zmaster587.libVulpes.items.ItemProjector.registerMachine
  at zmaster587.advancedRocketry.AdvancedRocketry.postInit
```

`TileMultiBlock.getMapping('O')` returns null at AR `postInit` — likely the
mapping for that wildcard character is registered later than the arc furnace's
projector setup expects. **This is an existing AR/libVulpes bug** that no one
caught earlier because production tests have always run via `runClient` (where
the call path differs) — the dedicated-server harness exposes it for the first
time. Per SMART §3 it is documented but not fixed in this PR. Until the bug
is fixed every scenario reports SKIPPED with the crash transcript.
