# TASK-01: SMART per-scenario depth coverage

## Ticket

- Source: `C:\Users\batalenkov.s\Downloads\advanced_rocketry_full_test_suite_smart.md`
- Status: Pending
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

### Phase 1: P0 depth — §7.3 PlanetDimensionLoadTest (~2-3 h)

- [ ] **Probe extension**: `/artest dim info <id>` must return
      `providerClass`, `biomeProviderClass`, `chunkGeneratorClass`, `saveDir`.
      If any are missing today, add to `TestProbeCommand.dim` case.
- [ ] `providerClassIsWorldProviderPlanet` — assert `dim info 0` reports
      AR's `WorldProviderPlanet`
- [ ] `biomeProviderIsNonNull`
- [ ] `chunkGeneratorIsNonNull`
- [ ] `saveFolderResolvesToExpectedPath`
- [ ] `celestialAngleStableAcrossSameWorldTime` — two probe calls at identical
      world time must produce identical results
- [ ] `celestialAngleProgressesAcrossDifferentWorldTimes` — monotonic on
      `t=0 → 6000 → 12000`

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

Filled in dynamically from Phase 0 results. Likely candidates:

- `/artest dim load <id>` if missing
- `/artest worldgen sample <dim> <cx> <cz>`
- `/artest oxygen player <name>`

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

- [ ] Phase 0 (F1, F2) done; F2 results merged into Phase 5
- [ ] Phase 1 (PlanetDimensionLoad) done; testServer green
- [ ] Phase 2a (Commands) done
- [ ] Phase 2b (AtmosphereOxygen) done
- [ ] Phase 2c (RocketAssembly) done
- [ ] Phase 2d (Satellite types) done
- [ ] Phase 2e (RocketInfrastructure) done
- [ ] Phase 3 (PipeNetwork) done
- [ ] Phase 4 done; new "pyramid complete" marker authored
- [ ] Phase 5 (any leftover probe additions) done
- [ ] `./gradlew test testAdvancedRocketryScenarios` PASS
- [ ] SMART §16 final report bullet-by-bullet against §7 prose
- [ ] Predecessor marker linked from this task (already linked above)

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
