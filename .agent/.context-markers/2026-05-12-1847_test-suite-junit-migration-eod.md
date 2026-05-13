# Context Marker: test-suite-junit-migration-eod

**Created**: 2026-05-12 18:47
**Note**: End of session — full test-suite migration to JUnit-native + framework
0.3.0 + selector test layer added. All targeted work landed green.

---

## Conversation Summary

Long session covering the full evolution of the AR test suite:

1. **Audit start**: Read SMART task spec + branch `feature/tests`. Identified
   that previous bootstrap pattern (`AdvancedRocketryTestBootstrap` + custom
   `TestRegistry`/`TestOrchestrator`) was an anti-pattern wrapping 28 scenarios
   in a single JUnit method — blocked parallelism, blocked `--tests` filtering,
   blocked IDE integration.

2. **Mass test deepening**: Migrated framework dep to mavenLocal, added new
   `/artest` probes (station create, satellite create, atmosphere set-density,
   terraforming set-density, energy inject, tile force-tick, infra link,
   worldgen ore-stats, machine recipes-summary). Deepened 11 scenarios from
   probe-wiring smoke to real gameplay drivers.

3. **JUnit migration** (framework 0.2.1→0.2.2→0.3.0):
   - 0.2.2: Added JUnit base classes `AbstractHeadlessServerTest` +
     `AbstractClientE2ETest` to framework. Republished to mavenLocal.
   - AR migration: 28 scenarios → JUnit @Test methods extending new bases.
     Deleted bootstrap/registry/HarnessBoundScenario legacy wrappers.
     Configured `maxParallelForks` (default 3) + `forkEvery=1`.
   - Build glue: `JAVA_TOOL_OPTIONS` env trick to forward FG6 system properties
     to harness subprocess JVM (fixes FMLDeobfuscatingRemapper NPE).
   - 0.3.0: Deleted framework's entire legacy runner layer (TestRegistry,
     TestOrchestrator, TestBootstrap, HeadlessGameTest, TestContext, TestOutcome,
     TestStatus, TestReportWriter, TestAssertions, TestFrameworkTest). Framework
     is now a pure library — harness + bot + JUnit bases only.

4. **Reorganization**: Tests split by SMART §2 pyramid layer into
   `unit/`, `integration/`, `server/`, `client/` subpackages. Build glue routes
   `gradle test` → unit+integration (fast, no harness) and
   `gradle testAdvancedRocketryScenarios` → server+client (heavy, harness=true).

5. **Multiblock + recipe end-to-end**: Added `fixture machine cutting`,
   `machine try-complete`, `machine set-enabled`, `machine recipe-info`,
   `hatch fill/read` probes. `MultiblockValidationSmokeTest` now validates real
   multiblock isComplete cycle (build → validate → break → invalidate → restore
   → validate). `MachineRecipeIntegrationTest` now drives the full recipe cycle
   (build fixture → validate → resolve first recipe → fill input → charge power
   → enable → tick → verify output in output hatch).

6. **Dead-stub cleanup**: User pointed out @Ignore stubs claiming "covered in
   §X.Y" were lies — scenarios cited didn't actually test packet wire format or
   deep XML parsing. Audited each claim, then:
   - Deleted `PlanetWeatherStateTest` (7 weather-B1 stubs — class doesn't exist
     yet, file was just an empty TODO list).
   - Added `integration/PacketSerializationTest` (6 wire-format round-trips:
     PacketDimInfo, PacketSatellite, PacketStationUpdate FUEL+ORBIT,
     PacketConfigSync, PacketDimInfo null-deletion signal).
   - Added `integration/XMLPlanetLoaderTest` (11 deep parsing tests: DIMID
     resolution, parent/child planet hierarchy, weather fields parsed +
     defaulted + invalid-marker behavior, atm/gravity clamping).

7. **Planet selector test** (last batch):
   - `/artest selector info` + `/artest selector simulate-click` probes.
   - `server/SelectorServerSmokeTest` (always-on): place block, simulate-click,
     verify dimCache populates. Rejects unregistered planet dims.
   - `client/PlanetSelectorGuiE2ETest` rewritten as minimal GUI smoke:
     rightClickBlock → reportState shows GUI → closeScreen. No empirically-
     derived pixel coords. Opt-in via `forge.test.client.enabled=true`.

## Documentation Loaded

- Navigator: ✅ `.agent/DEVELOPMENT-README.md` (session start)
- SMART task: `docs/advanced_rocketry_full_test_suite_smart.md` (referenced
  throughout via test categories §6/§7)
- No additional system docs / SOPs read

## Files Modified

### Framework (`C:\Users\Quarter\IdeaProjects\ForgeTestFramework`)
- `build.gradle` — version 0.2.1 → 0.3.0
- `dependencies.gradle` — added `compileOnly junit:junit:4.13.2`
- `README.md` — rewritten to declare framework as harness-only library
- `TEST_FRAMEWORK.md` — removed legacy-runner layer from architecture
- NEW: `src/main/java/.../junit/AbstractHeadlessServerTest.java`
- NEW: `src/main/java/.../junit/AbstractClientE2ETest.java`
- DELETED: `HeadlessGameTest`, `TestRegistry`, `TestOrchestrator`,
  `TestBootstrap`, `TestContext`, `TestOutcome`, `TestStatus`,
  `TestReportWriter`, `TestAssertions`, `TestFrameworkTest`

### AR (`C:\Users\Quarter\IdeaProjects\AdvancedRocketry`)
- `build.gradle.kts` — framework dep 0.2.1 → 0.3.0; `maxParallelForks=3`
  default; filter split into test (unit+integration) vs scenarios (server+
  client); `JAVA_TOOL_OPTIONS` env forwarding; explicit `MC_VERSION` env
- `src/test/README.md` — rewritten Layout + Running sections to reflect
  4-package pyramid layout + new tests + new probes
- `src/main/java/.../command/test/TestProbeCommand.java` — many new sub-commands
- `src/main/java/.../tile/multiblock/{TileObservatory,TileBlackHoleGenerator,
  TileMicrowaveReciever}.java` + `tile/multiblock/machine/{TileElectricArcFurnace,
  TilePrecisionAssembler}.java` — null-guard in `getAllowableWildCardBlocks`
  (server-postInit NPE fix, strictly outside SMART §3 but unblocks all
  scenarios)
- 28 scenario test classes rewritten as JUnit @Test extending new bases
- 3 unit tests moved to integration/ (need MinecraftBootstrap)
- DELETED: `AdvancedRocketryTestBootstrap`, `AdvancedRocketryTestRegistry`,
  `FrameworkWiringSmokeTest`, `HarnessBoundScenario`, `ClientHarnessBoundScenario`
- NEW: `server/SelectorServerSmokeTest`, `integration/PacketSerializationTest`,
  `integration/XMLPlanetLoaderTest`

## Current Focus

**State**: All work in this session is committed-ready (working tree green;
not yet committed per global rule — user must approve diff).

**Test outcomes**:
- `./gradlew test` → **102 PASSED, 0 SKIPPED, 0 FAILED** (~30s)
- `./gradlew testAdvancedRocketryScenarios` → **27 PASSED, 6 SKIPPED, 0 FAILED**
  (~9m @ default `-Pforks=3`)

**Branch**: `feature/tests`. Working tree has many modifications + deletions +
additions. Not committed.

## Technical Decisions

- **Framework cleanup**: Decided to fully delete the legacy runner layer rather
  than keep it as "standalone fallback". Framework is now strictly a library:
  harness + bot + JUnit base classes. Bumped to 0.3.0 to signal breaking change.
- **Test layering by SMART §2 pyramid**: `unit/` (pure JVM) → `integration/`
  (MC bootstrap in-JVM) → `server/` (real dedicated server JVM) → `client/`
  (real server + real MC client). Gradle tasks routed accordingly.
- **JAVA_TOOL_OPTIONS workaround**: framework harness doesn't forward parent
  JVM `-D` flags to spawned server JVM. Solution: pack them into
  `JAVA_TOOL_OPTIONS` env var which every JVM auto-prepends. Documented in
  `src/test/README.md`.
- **Recipe test approach**: Resolve first recipe dynamically via probe rather
  than hardcoding "log→planks" — keeps test config-agnostic. Cutting machine
  picked as canonical recipe because its multiblock is the smallest (1×2×3) of
  all AR machines.
- **Planet selector test split**: Two-layer — server-side state machine
  always-on (`SelectorServerSmokeTest`), client GUI smoke gated on display
  (`PlanetSelectorGuiE2ETest` minimal, no empirically-derived coords).
- **Dead-stub cleanup**: User insisted (correctly) that @Ignore tests claiming
  "covered in §X.Y" were often false. Audited and either deleted (weather B1
  stubs — class doesn't exist) or replaced with real tests (4 packets + 11 XML
  deep parsing).

## Next Steps

When user resumes / starts a new session:

1. **Review + commit current work**. Working tree has substantial changes
   across framework + AR. Per global CLAUDE.md rule, user must approve diff
   before any commit; Claude should NOT auto-commit.

2. **Continue test-deepening backlog** (if user wants more coverage). Order
   from earlier plan:
   - #2 Sealed-room oxygen vent end-to-end (`AtmosphereOxygenSmokeTest`)
   - #3 Real rocket launch with guidance chip (`RocketLaunchSmokeTest`)
   - #4 AR-tile NBT + EntityRocket persistence (`PersistenceRestartSmokeTest`)
   - #5 Coal generator real cycle (`EnergySystemsSmokeTest`)
   - #6 Distance limit + fuel transfer (`RocketInfrastructureSmokeTest`)
   - #7 Real terraformer multiblock cycle (`TerraformingSmokeTest`)

3. **Implement remaining client E2E tests** (when user has display):
   - GuidanceComputerGuiE2ETest — needs chip-insertion fixture
   - RocketBuilderGuiE2ETest — needs GUI button click sequence
   - OxygenSuitClientStateE2ETest — needs bridge probe for player effects
   - WeatherClientSyncE2ETest — blocked on weather B1 production refactor

4. **Weather B1 production refactor** — the original purpose of this branch.
   All P0 weather tests are ready (`WeatherBaselineTest` parameterized via
   `-Pweather=shared|per_dimension`, `WeatherPersistenceTest`). After B1 lands,
   flip the expected mode and re-run.

## User Intent & Goals (ToM)

**Primary goal this session**: Build a robust, fast, parallelizable test suite
that gives real regression signal across AR's gameplay surfaces — and clean up
the existing test-suite design debt (custom orchestrator, fake @Ignore stubs,
flat layout). Final outcome: tests are the safety net for an upcoming weather
B1 production refactor.

**Stated preferences**:
- "Никаких командных аргументов" for IDE workflow — IDE "Run all in directory"
  should just work without `-Pforks=N` etc. → reorganized by test type.
- "forks=3 многовато 6" → lowered default to 3.
- Prefer cleanup over half-measures — when shown that the orchestrator could be
  removed entirely, user immediately approved full deletion ("Вариант A").
- Russian conversational language throughout.

**Corrections made**:
- "Tests for weather B1 cannot reference classes that don't exist — first you
  write the change, only then cover it" — pushed back on TDD-stub @Ignore
  approach for weather B1 specifically. Resolved by deleting the file entirely
  rather than keeping empty stubs.
- "Покрытые в других пунктах SMART — но на самом деле не реализованные" —
  identified that @Ignore "covered in §X.Y" claims were lies for XML loader +
  packet round-trip tests. Resolved by adding real integration tests instead of
  deleting the @Ignore stubs.
- "Подпапки = типы тестов, не приоритеты" — corrected the initial subpackage
  proposal (P0/P1/P2) to SMART §2 pyramid layers (unit/integration/server/
  client).

## Belief State

**What user knows**:
- Deep familiarity with Minecraft Forge 1.12.2 modding, libVulpes multiblock
  patterns, AR's `DimensionManager`/`SpaceObjectManager`/`SatelliteRegistry`
  architecture.
- Knows what SMART task spec contains and references sections by number.
- Comfortable with Kotlin DSL Gradle, FG6 internals (runServer classpath
  reflection).
- Senior-level developer — push-back on dubious patterns, expects clean
  decomposition, dislikes "fake coverage".

**Assumptions I made**:
- User is on Windows desktop (filesystem paths, machine sized for 3-fork
  parallel server JVMs but not 6).
- Display IS available locally → client E2E tests would work with
  `-PclientHarness=true` but user hasn't tested that.
- Framework's `feature/tests` work is the user's own project — they want clean
  state to publish, not just experiments.
- `ARConfiguration.getCurrentConfig()` works in unit JVM (verified empirically
  — singleton lazy-inits).

**Uncertainty areas**:
- Whether user will adopt the `-PclientHarness=true` workflow OR drop client
  tests entirely.
- Whether the user plans to publish the framework as an actual open-source
  library or it's just for their personal use across mods.
- What the weather B1 refactor scope looks like — only the tests' expected
  behavior is documented, not the implementation plan.

## Restore Instructions

To restore this marker:
```
Read .agent/.context-markers/2026-05-12-1847_test-suite-junit-migration-eod.md
```

Or use: `/nav:markers` and select this marker.
