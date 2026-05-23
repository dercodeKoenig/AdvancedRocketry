# TASK-01: SMART per-scenario depth coverage

## Ticket

- Source: `C:\Users\batalenkov.s\Downloads\advanced_rocketry_full_test_suite_smart.md`
- Status: ✅ Completed — see `.agent/tasks/README.md` Done table.
- Created: 2026-05-15
- Predecessor marker: `.agent/.context-markers/2026-05-15-1610_smart-pyramid-skeleton-complete.md`

## Context

The SMART test suite *skeleton* is in place: 4-layer pyramid (unit, integration,
server, client) runs end-to-end at 201/193-PASS/8-SKIP/0-FAIL; every SMART §6/§7
category has at least one test method; every P0/P1/P2 named item from SMART §8
has a corresponding file.

**But per-scenario depth is below SMART prose targets** for several §7 scenarios.
SMART describes 4–9 "Covers" bullets per scenario; many of ours currently exercise
only one representative bullet. Until depth matches the prose, the suite under-
delivers on its stated goal — a future agent asking "did my change break
satellites / atmosphere / pipe networks?" may get a false-green answer because
only the most trivial slice of the subsystem is being exercised.

This task brings depth up to SMART's prose level. **No production logic
changes** — only test methods + `/artest` probe extensions where probes are
missing.

## Implementation Plan

### Phase 0: Micro-fixes (~30 min, blocking nothing)

- [ ] **F1 — §6.7 #3 `planetaryLightMultiplierWithinExpectedBounds`**
      Add one unit method in
      `src/test/.../unit/AstronomicalBodyHelperTest.java`:
      probe `getPlanetaryLightLevelMultiplier` at distances {50, 100, 200, 400},
      assert each result is in `[expected_min, expected_max]` for the corresponding
      stellar baseline. Closes the last §6.7 named-test gap.
- [ ] **F2 — §5 probes audit**
      Run `/artest <category> help` for all 13 top-level categories. Confirm
      every SMART §5 subcommand exists: `/artest dim load <id>`,
      `/artest worldgen sample <dim> <cx> <cz>`, `/artest oxygen player <name>`,
      `/artest planet info <dim>` returns the full DimensionProperties field
      list per SMART §5.3. File any missing subcommand as Phase 5 work below.

### Phase 1: P0 depth — §7.3 PlanetDimensionLoadTest (~2-3 h) — DONE 2026-05-15

- [x] **Probe extension**: `/artest dim info <id>` now returns `providerClass`
      (already), `biomeProviderClass`, `chunkGeneratorClass` (drills past
      `ChunkProviderServer` to the inner `IChunkGenerator`), and `saveDir`.
      Added in `TestProbeCommand.handleDim`. Also added
      `/artest dim celestial-angle <id> <worldTime>` for the two angle tests
      — pure read-only computation, deterministic.
- [x] `providerClassIsWorldProviderPlanet` — uses
      `firstNonOverworldArDimOrSkip` (AR registers Earth as dim 0 but keeps
      its vanilla `WorldProviderSurface`, so the test targets the first
      non-overworld AR planet); asserts FQN equals
      `zmaster587.advancedRocketry.world.provider.WorldProviderPlanet`.
- [x] `biomeProviderIsNonNull` — same dim, asserts `biomeProviderClass` is
      neither missing nor `"null"`.
- [x] `chunkGeneratorIsNonNull` — same dim, same null-vs-missing assertions
      against the drilled-down generator class.
- [x] `saveFolderResolvesToExpectedPath` — same dim, asserts
      `saveDir` starts with `"advRocketry/"` (per
      `WorldProviderPlanet.getSaveFolder` prefix).
- [x] `celestialAngleStableAcrossSameWorldTime` — two probe calls at
      `worldTime=0`, compare extracted `"angle"` doubles with delta=0.0.
      Note: we compare extracted angle values rather than full response
      strings — server console echoes are timestamp-prefixed, which would
      race a byte-level comparison on tick boundaries.
- [x] `celestialAngleProgressesAcrossDifferentWorldTimes` — soft assertion
      that angles at `t=0`, `t=6000`, `t=12000` are pairwise distinct.
      Strict monotonicity not pinned (the celestial cycle wraps modulo
      `rotationalPeriod`); tightening belongs to a future test after the
      rocket-assembly suite locks down AR's exact rotational-period math.
- Validation: `./gradlew testServer --tests "*.PlanetDimensionLoadTest"` →
  **8/8 PASSED**, 3m 54s.

### Phase 2: P1 depth (~12-17 h, can be split across 3-4 sessions)

#### 2a — §7.19 CommandsSmokeTest (≈1 h)

- [ ] `arHelpCommandPrintsUsageWithoutCrash` — server stays alive after `/help advancedrocketry`
- [ ] `arCommandWithInvalidArgsReturnsErrorNotCrash`
- [ ] `artestRegistryWithBadSubcommandReturnsError`
- [ ] `artestWeatherSetWithMalformedTicksReturnsError`

#### 2b — §7.13 AtmosphereOxygenSmokeTest (≈3 h)

- [ ] **Probe extensions** (if missing): `/artest atmosphere detector-output <pos>`,
      `/artest fluid tank <pos>` for scrubber/charge-pad readouts.
- [ ] `atmosphereDetectorReportsCurrentAtmosphereOnRedstone`
- [ ] `co2ScrubberRemovesCo2InSealedRoom` — sealed room with CO2 atmosphere →
      scrubber + power → atmosphere flips to breathable
- [ ] `gasChargePadFillsSuitTank`
- [ ] `spaceBreathingEnchantBypassesVacuumDamage`
- [ ] `torchExtinguishesInLowOxygenConfig` — config-gated;
      `torchExtinguishInVacuum=true` → torch in vacuum drops as item

#### 2c — §7.9 RocketAssemblySmokeTest (≈4-5 h)

- [ ] **Probe extension**: `/artest rocket info <id>` must return
      `storageChunkSize`, `statsRocket{thrust,weight,fuelCap,fuelRate}`,
      `seatCount`, `engineCount`, `fuelTankCount`, `guidanceComputerSlotOccupied`.
- [ ] **Fixture extension**: invalid rocket fixtures — missing-engine,
      missing-seat, missing-fuel-tank — via `/artest fixture rocket invalid-*`.
- [ ] `rocketStorageChunkMatchesScanFootprint`
- [ ] `statsRocketIsCalculatedFromComponents` — thrust = engineCount × engineThrust
- [ ] `seatCountMatchesFixturePlacement`
- [ ] `engineDetectionFindsAllEngines`
- [ ] `fuelTankDetectionFindsAllTanks`
- [ ] `guidanceComputerSlotPopulatedAfterChipInsert`
- [ ] `invalidRocketMissingEngineFailsAssemblyWithReason`
- [ ] `invalidRocketMissingSeatFailsAssemblyWithReason`

#### 2d — §7.12 SatelliteLifecycleSmokeTest (≈4-6 h, can split)

- [ ] **Probe extensions**: `/artest satellite create <dim> <type> [props...]`
      for every type; `/artest satellite info <id>` must include type-specific
      props (scanRange, fluidStored, etc.)
- [ ] `opticalScannerSatelliteRoundTrips`
- [ ] `densityScannerSatelliteRoundTrips`
- [ ] `compositionScannerSatelliteRoundTrips`
- [ ] `massScannerSatelliteRoundTrips`
- [ ] `asteroidMinerSatelliteRoundTrips`
- [ ] `gasCollectionSatelliteRoundTrips`
- [ ] `biomeChangerSatelliteRoundTrips`
- [ ] `weatherControllerSatelliteRoundTrips`
- [ ] `satelliteBuilderProducesValidSatelliteFromComponents`
- [ ] `satelliteTerminalListsAttachedSatellites`
- [ ] `satelliteIdChipPersistsIdAcrossRestart`

#### 2e — §7.10 RocketInfrastructureSmokeTest (≈6-8 h, hardest)

- [ ] **Probe extensions**: `/artest infra place <type> <x> <y> <z>`,
      `/artest rocket land <id> <pad-pos>`, `/artest infra inventory <pos>`,
      `/artest infra fluid <pos>`
- [ ] `rocketLoaderTransfersItemsAfterLanding`
- [ ] `rocketUnloaderRemovesItemsAfterLanding`
- [ ] `fluidLoaderTransfersFluidAfterLanding`
- [ ] `fluidUnloaderTransfersFluidAfterLanding`
- [ ] `monitoringStationReportsRocketTelemetry`
- [ ] `linkerRejectsInfrastructureBeyondMaxDistance`
- [ ] `unlinkRemovesAssociation`
- [ ] `linkSurvivesSaveLoad`

### Phase 3: P2 depth — §7.17 PipeNetworkSmokeTest (~4-6 h)

Current only covers energy. Missing: data pipe, liquid pipe, wireless
transceiver, data bus.

- [ ] **Probe extensions**: `/artest pipe data send <from> <to> <packet>`,
      `/artest pipe data status <pos>`, `/artest pipe wireless pair <pos1> <pos2>`,
      `/artest pipe liquid contents <pos>`
- [ ] `dataPipeRoutesPacketsBetweenEndpoints`
- [ ] `liquidPipeTransfersFluidAcrossChunkBoundary`
- [ ] `wirelessTransceiverPairsAndTransmits`
- [ ] `dataBusBridgesAdjacentInventories`
- [ ] `inventoryHatchAcceptsAndExportsItems`
- [ ] `fluidHatchAcceptsAndExportsFluids`

### Phase 4: Final pyramid validation + §16 honest report

- [ ] Run full pyramid: `./gradlew test testAdvancedRocketryScenarios`
- [ ] Expected counts: ~250-260 tests, 0 FAIL, 7-8 SKIP (B1 placeholders +
      intentional client `Assume`s)
- [ ] Generate SMART §16-format final report. Bullet-by-bullet cross-check
      against SMART §7 prose — every "Covers" bullet must have ≥1 assertion.
- [ ] Update `.agent/.context-markers/` with a new fully-honest "pyramid
      complete" marker that *also* lists what is still intentionally @Ignored
      (B1) or `Assume`d-out (client weather sync).

### Phase 5: Probe gaps surfaced during F2 audit

F2 results (2026-05-15, static audit of `TestProbeCommand`, weather scope
intentionally skipped per session scoping):

- [x] **§5.2 `/artest dim load <id>` — DONE 2026-05-15.** `handleDim` now has a
      `case "load"` that mirrors the weather/worldgen `keepDimensionLoaded` +
      `initDimension` idiom and returns `{dim, loaded, providerClass,
      isARPlanet}`. Smoke test pinned in `PlanetDimensionLoadTest`
      (`dimLoadOnOverworldReportsLoaded`). Deeper coverage (loading a
      not-yet-touched AR dim) is Phase 1.
- [x] **§5.13 `/artest worldgen sample <dim> <cx> <cz>` — present**
      (`TestProbeCommand.java:1283`). Bonus `ore-stats` subcommand also exposed.
- [x] **§5.10 `/artest oxygen player <name>` — present**
      (`TestProbeCommand.java:814`). Returns
      `{name, dim, posX, posY, posZ, atmosphere, breathable, pressure}`.
- [x] **§5.3 `/artest planet info <dim>` field coverage — DONE 2026-05-18.**
      Extended `handlePlanet` to also emit `averageTemperature`, `genType`,
      `oceanBlock` (registry name, or `null` when vanilla water fallback),
      `skyColor`, `sunriseSunsetColors`. `jsonMap` extended to encode
      `List<?>` as a JSON array (`float[]` colour tuples are mapped to
      `List<Double>` via a local helper to use that path). 20 fields total.
      `originalAtmosphereDensity` deliberately omitted: no public accessor
      on `DimensionProperties`, and reflection-poking a private field from
      a probe is not worth the maintenance cost — add an accessor if a
      test actually needs the distinction.
- [ ] **Advisory: no top-level category implements `case "help"`.** Calling
      `/artest <cat> help` falls through to each category's "unknown
      subcommand" error string. Not a SMART §5 hard requirement, but adding a
      uniform `case "help"` per handler (returning the existing fallback error
      text wrapped as `{"usage":...}` instead of `{"error":...}`) would make
      future audits self-documenting. Optional unless SMART §16 enforces it.

**Weather scope (audited 2026-05-18, post-B1 ship):**
- `/artest weather get <dim>` — present (`TestProbeCommand.handleWeather`,
  returns dim/worldInfoClass/isRaining/isThundering/rainTime/thunderTime/
  cleanWeatherTime/rainStrength/thunderStrength).
- `/artest weather set <dim> {clear|rain|thunder} <ticks>` — all three
  modes present, each correctly resets `cleanWeatherTime` to avoid the
  vanilla force-clear edge case (commented inline in `handleWeather`).
- All callers in `src/test/` (WeatherClientSyncE2ETest,
  PerDimensionWeatherIsolationTest, WeatherBaselineTest,
  WeatherPersistenceTest) only invoke `weather get` / `weather set`.
  No SMART §5 weather verb is missing.

**Post-B1 cleanup (2026-05-18):** `WEATHER_MODE_SHARED` retirement.
Now that the old `CustomDerivedWorldInfo` shared-weather path is deleted,
the `-Pweather=shared` Gradle override and the
`-Dadvancedrocketry.tests.expectedWeatherMode=shared` test toggle are
unreachable. Removed:
- `AdvancedRocketryTestConstants.WEATHER_MODE_*` constants and
  `expectedWeatherMode()` method
- `weatherMode` Kotlin val + `systemProperty(...)` injection in
  `build.gradle.kts`; comment block updated
- The `if (WEATHER_MODE_SHARED)` branch in `WeatherBaselineTest`
  (test now asserts per-dimension isolation + wrapper presence
  unconditionally)
- README mentions of `-Pweather=shared|per_dimension`

## Technical Decisions

- **Probe-first, then test**: every test phase opens with required probe
  extensions. Adding probes mid-test creates churn and ambiguous failures.
- **Probes live in `TestProbeCommand.java`** (Java 8, fits CLAUDE.md vanilla
  Forge 1.12.2 patterns). No new classes unless probe count for a category
  exceeds ~10 sub-cases.
- **Test placement** — pure-math additions to `unit/`; everything that needs
  a real server fork goes to `server/<existing-file>` or a new sibling. No
  new files unless an existing file would exceed ~500 lines.
- **Validation cadence**: after each Phase, run `./gradlew testServer` (or
  `--tests <pattern>` for a single scenario class during iteration). Avoid
  flipping production logic to make tests pass — per SMART §15, if a test
  reveals a bug, document the bug as a known failure / @Ignore'd
  `_documentsKnownBug` test, not a production patch in this task.

## Dependencies

**Requires**:
- Test framework `forge-test-framework:0.4.0+:dev` already published to
  mavenLocal (carried over from the predecessor session).
- FG6 mapping deps fix in `configureHarnessLayer` (committed as `0cf5a56a`).
- `weatherMode` default = `per_dimension` (currently uncommitted; should land
  before resuming this task to avoid spurious WeatherBaselineTest fail noise).

**Blocks**:
- The honest "SMART §16 — DoD met" report. The current
  `.agent/.context-markers/2026-05-15-1610_smart-pyramid-skeleton-complete.md`
  is explicit that depth is incomplete; that footnote disappears when this
  task completes Phase 4.

**Does NOT block**:
- B1 weather refactor. The 7 SKIPPED `@Ignore`d B1 placeholders
  (`unit/PlanetWeatherStateTest`, `unit/ARWeatherWorldInfoTest`) are
  already in place and decoupled from this task.

## Completion Checklist

- [x] Phase 0 (F1, F2) done; F2 results merged into Phase 5 (weather audit
      deferred; non-weather scope complete)
- [x] Phase 1 (PlanetDimensionLoad) done; testServer green (8/8 PASSED)
- [x] Phase 2a (Commands) done — 4/4 PASSED (kept from skeleton)
- [x] Phase 2b (AtmosphereOxygen) done — 5 new + 1 existing PASSED
- [x] Phase 2c (RocketAssembly) done — 8 new + 1 existing PASSED
- [x] Phase 2d (Satellite types) done — 10 new in main class +
      1 standalone persistence test PASSED
- [x] Phase 2e (RocketInfrastructure) done — 7 new + 1 existing in main
      class + 1 standalone persistence test PASSED
- [x] Phase 3 (PipeNetwork) done — 3 new PASSED, 3 SMART bullets
      intentionally `@Ignore`d (production blocks commented out)
- [x] Phase 4 done; new "pyramid complete" marker authored
      (`.agent/.context-markers/2026-05-18-1530_task01-phase4-pyramid-complete.md`)
- [x] Phase 5 (probe additions) — §5.3 `planet info` field coverage
      extended (20 fields); weather subcommands audited (no gaps);
      `WEATHER_MODE_SHARED` retired post-B1. Only the optional
      `case "help"` advisory remains open.
- [x] `./gradlew test` PASS — 239/0/11 (PASS/FAIL/SKIP), 14m 29s wall
- [x] SMART §16 final report bullet-by-bullet against §7 prose
      (embedded in the Phase 4 marker)
- [x] Predecessor marker linked from this task (already linked above)

## Estimated effort

~25-35 hours, 7-8 focused sessions of 1-3 hours each. Suggested order:

1. **Session 1** — Phase 0 (F1 + F2) + Phase 2a (Commands). Quick wins, ~2 h.
2. **Session 2** — Phase 1 (PlanetDimensionLoad), ~3 h.
3. **Session 3** — Phase 2b (AtmosphereOxygen), ~3 h.
4. **Session 4** — Phase 2c (RocketAssembly), ~5 h.
5. **Session 5** — Phase 2d-1 (5 satellite types), ~3 h.
6. **Session 6** — Phase 2d-2 (4 satellite types + builder/terminal/IDchip), ~3 h.
7. **Sessions 7-8** — Phase 2e (RocketInfrastructure), ~7 h.
8. **Session 9** — Phase 3 (PipeNetwork), ~5 h.
9. **Session 10** — Phase 4 final validation + report + marker. ~2 h.
