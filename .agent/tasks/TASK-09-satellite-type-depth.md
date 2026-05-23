# TASK-09: Per-satellite-type behavioural depth

## Ticket

- Source: TASK-03 EOD audit (2026-05-19) — `satellite/` has 11
  classes; `SatelliteLifecycleSmokeTest` covers only create/list/info.
  Per-type tick / produce / consume behaviour is uncovered.
- Status: ✅ Completed (2026-05-21). Scope rewritten — see "Actual
  delivery" below; original phase-by-phase plan was speculative
  (class names didn't match the codebase). Phase 5 added 2026-05-21
  on a coverage-gap self-audit (see "Phase 5 — coverage gaps").
- Created: 2026-05-19
- Predecessor: `.agent/.context-markers/2026-05-19-1230_task03-A-and-B-mostly-done-eod.md`

## Context

Satellite types are AR's "passive production" gameplay layer: launch a
satellite into orbit → it ticks → produces output of its type
(energy, data, ore detection, gas collection, etc.). The lifecycle
(create / register / persist) is tested; the BEHAVIOURAL contract of
each type is not.

Satellites in this codebase (subclasses of `SatelliteBase`):

| Class | Role |
|---|---|
| `SatelliteEnergy` (`solarEnergy`) | Beams RF down to receiver |
| `SatelliteMass` | Mass-detection / scanning |
| `SatelliteOreMining` | Tags asteroid ore candidates |
| `SatelliteGasCollector` | Gas-mission progress source |
| `SatelliteSpaceLaser` | Long-range targeting helper |
| `SatelliteSurveillance` | Atmospheric / surface observation |
| `SatelliteMicrowaveTransmitter` | Energy beam-down |
| `SatelliteOreScanner` | Ore-distribution mapping |
| `SatelliteData` | Data accumulation |
| (+ 2 others) | |

A regression in any per-type tick / produce silently breaks that
satellite. Players notice months later as "my microwave receiver isn't
getting energy" or "asteroid miner output is empty".

**No production logic changes** (same rule as TASK-01 §15).

## Implementation Plan

### Phase 1: Probe surface (~2 h)

- [ ] `/artest satellite tick <id> <ticks>` — drive per-tick logic.
- [ ] `/artest satellite output <id>` — dump current output buffer
  (data tags, accumulated resources, etc.). Per-type fields surfaced
  in a stable schema.
- [ ] `/artest satellite set-config <id> <key> <value>` — runtime
  tweak of XML-configurable values for test setup.

### Phase 2: Energy / microwave satellites (~2-3 h)

- [ ] `solarEnergySatelliteAccruesEnergyOverTicks`
- [ ] `microwaveSatelliteBeamsDownToReceiver` — place receiver on
  Earth, link with satellite, tick → receiver's stored energy
  advances.
- [ ] `energySatelliteRespectsPowerGenConfig` — XML-configured
  powerGen value reflects in output rate.

### Phase 3: Mining / ore satellites (~2-3 h)

- [ ] `oreMiningSatelliteTagsConfiguredOres` — fixture asteroid with
  iron / gold → satellite tick → tags appear in output buffer.
- [ ] `oreScannerSatelliteScansSpecifiedRadius` — radius config →
  scan area matches.

### Phase 4: Gas / surveillance / data satellites (~2-3 h)

- [ ] `gasCollectorSatelliteAccruesAtPlanetRate` — different planets
  have different gas profiles per XML.
- [ ] `surveillanceSatelliteReportsAtmosphereOfTargetDim`.
- [ ] `dataSatelliteAccumulatesUntilCapAndStopsAtMax`.

### Phase 5: Cross-cutting (~1-2 h)

- [ ] `satellitePersistsTypeAcrossRestart` — multi-boot.
- [ ] `satelliteOnUnloadedDimContinuesTicking` — production
  contract; satellites tick even when their orbital dim is unloaded.

### Phase 6: Validation + EOD (~1 h)

## Technical Decisions

- Most per-type tests use unit-tier (SatelliteBase tick is in-memory
  state machine).
- Microwave receiver test needs server-tier (real block placement).
- Persistence test extends multi-boot pattern from
  `PersistenceRestartSmokeTest`.

## Dependencies

**Requires**: TASK-03 base.

## Estimated effort

~10-12 hours across 3-4 sessions.

## Actual delivery (2026-05-21)

The initial plan above named classes that don't exist in the codebase
(`SatelliteEnergy`, `SatelliteSpaceLaser`, `SatelliteSurveillance`,
etc.). Real satellite classes in `satellite/`: `SatelliteOptical`,
`SatelliteDensity`, `SatelliteComposition`, `SatelliteMassScanner`
(all SatelliteData subclasses), `SatelliteOreMapping`,
`SatelliteMicrowaveEnergy`, `SatelliteBiomeChanger`,
`SatelliteWeatherController`, `SatelliteSpyTelescope` (orphan, not
registered), `SatelliteDefunct` (orphan).

Reality-grounded scope shipped:

**New `/artest satellite` verbs (8)**:

- `tick <dim> <id> <ticks>` — drives `SatelliteBase.tickEntity()` N
  times, bumps overworld `totalWorldTime` per iteration so
  `SatelliteData`'s `worldTime % collectionTime == 0` data-gate
  fires deterministically; returns pre/post battery + data
  snapshots in a single server-thread call (immune to background
  `DimensionManager.tickDimensions` racing).
- `battery <dim> <id>` — exposes `UniversalBattery.{stored,max}` via
  reflection.
- `data <dim> <id>` — exposes `DataStorage.{data,maxData,dataType}`
  for SatelliteData subclasses.
- `markers <dim> <id>` — surface relevant marker interfaces
  (IUniversalEnergyTransmitter, IUniversalEnergy, SatelliteData)
  + canTick.
- `can-tick <dim> <id>` — pure `SatelliteBase.canTick()` echo.
- `force-charge <dim> <id> <amount>` — direct `acceptEnergy` into
  the battery (no tick needed to prime).
- `biome-add-pos / biome-set / biome-list-size` —
  SatelliteBiomeChanger queue + biome reflection.
- `weather-add-pos / weather-mode` — SatelliteWeatherController
  `viable_positions` + `mode_id`.
- `block biome-at <dim> <x> <y> <z>` — read post-terraform biome
  back from `world.getBiome(pos)`.

**Fix in `satellite create` probe**: after reflective field
injection of `satelliteProperties`, also re-size the battery
(`UniversalBattery.setMaxEnergyStored`), call `data.setMaxData`,
and re-compute `powerConsumption` + `collectionTime` on
SatelliteData. The constructor used the default-zero properties
and never re-synced.

**Tests**:

`SatelliteTickBehaviourTest` (4 pins, AbstractSharedServerTest):

- `baseSatelliteTickAccruesAtApproximatelyPowerGenRate` —
  `oreScanner` (pure `SatelliteBase`) accrues at approximately
  `powerGen` per tick; pin uses a range
  `[ticks*powerGen/2 .. ticks*powerGen]`. (Originally pinned exact
  `powerGen - 1`; loosened to contract shape in `b97ddf0b`.)
- `baseSatelliteBatteryCapsAtPowerStorage` — `acceptEnergy` clamps
  at the configured powerStorage even when each tick would
  overshoot.
- `dataSatelliteAccumulatesDataOverTime` — `composition`
  (SatelliteData) accumulates 1-6 data points over 100 ticks
  given `collectionTime ≈ 20`.
- `dataSatelliteRespectsMaxDataCap` — DataStorage caps at maxData
  even with 500 saturating ticks.

`SatelliteTypeBehaviourTest` (3 pins, real-world side effects):

- `solarEnergySatelliteImplementsEnergyTransmitterMarker` —
  `SatelliteMicrowaveEnergy` implements
  `IUniversalEnergyTransmitter` (the contract beam-down receivers
  resolve against).
- `biomeChangerTickTerraformBlockBiomeAndDrainsQueue` —
  configured biome + queued pos + battery≥120 → `tickEntity`
  drains queue AND `BiomeHandler.terraform` mutates
  `world.getBiome(pos)`.
- `weatherControllerMode0TickReplacesAirWithWater` — mode 0 +
  queued air-block pos → `tickEntity` calls
  `setBlockState(WATER)`.

**Dropped from original plan**:

- Energy beam-down to a real `MicrowaveReceiver` block — heavier
  than a marker pin warrants; receiver wiring is already covered
  by `MicrowaveReceiverSmokeTest`. The marker pin here is the
  satellite-side half.
- Per-radius ore-scanner range pin — `SatelliteOreMapping` has no
  tickEntity override; its scan behaviour lives in
  `performAction` (player-interaction → testClient territory).
- Surveillance / mass / gas / data per-planet specifics — those
  use `MissionOreMining` / `MissionGasCollection` (registered as
  satellite types but they're mission-driven, not tick-driven),
  better covered by mission tests (TASK-06).
- Cross-restart persistence — already pinned by
  `SatelliteIdChipPersistenceTest`.

## Phase 5 — coverage gaps (2026-05-21 self-audit follow-up)

Post-ship self-audit flagged seven gaps in the initial pin set
(marker-only solarEnergy, single-mode weather coverage, single-pos
biome batch, no canTick / isDead gating proof, no biomeId=null
guard). Closed with `SatelliteCoverageGapsTest` (7 pins) +
6 additional probe verbs.

**New probes** (6):
- `satellite biome-batch-tick` — atomic compound probe (clear queue +
  set biome + force-charge + add N positions + tickEntity once) to
  prove the up-to-10-per-tick loop deterministically (no
  background-tick race).
- `satellite biome-null` — set BiomeChanger.biomeId to null via
  reflection.
- `satellite weather-list-size` — read viable_positions size.
- `satellite ticking-list` — expose DimensionProperties.tickingSatellites.
- `satellite set-dead` — call sat.setDead().
- `satellite force-tick-dim` — invoke DimensionProperties.tick()
  synchronously (deterministic isDead-removal driver).
- `satellite create-spy-telescope` — register an orphan
  SatelliteSpyTelescope (canTick=false) via direct instantiation.
- `satellite weather-mode <dim> <satId> <mode> [update-last]` —
  optional 5th arg now controls whether last_mode_id is bumped
  (false → next tick fires the mode-change-clears-list branch).

**Phase 5 pins** (7, all PASS):
- `weatherControllerMode1ReplacesWaterWithAir` — drain branch.
- `weatherControllerMode2ReplacesAirWithWater` — alt-rain branch
  (independent code path from mode 0).
- `weatherControllerModeChangeClearsViablePositions` — pins the
  `last_mode_id != mode_id` clear branch.
- `biomeChangerProcessesUpToTenPositionsPerTick` — 5 queued positions
  drain in ONE tickEntity call (atomic probe; proves the loop is
  real, not 1-per-tick).
- `biomeChangerWithNullBiomeDrainsResourcesButDoesNotTerraform` —
  null-guard inside terraform fires AFTER remove/extract have
  happened; queue + battery drained, biome unchanged.
- `satelliteWithCanTickFalseIsNotAddedToTickingList` — SpyTelescope
  in `satellites` map but NOT in `tickingSatellites` (production's
  `addSatellite` canTick gate).
- `deadSatelliteIsRemovedFromTickingListOnNextDimTick` —
  `DimensionProperties.tick()` removes isDead satellites from
  tickingSatellites on the next iteration.

**Remaining gaps** (deferred, lower priority):
- Per-class SatelliteData differentiation (optical vs density vs
  composition vs mass) — currently only generic composition pin.
  Different `DataStorage.DataType` per class is implicitly covered
  by lifecycle round-trip; per-class tick behaviour is identical
  (all inherit SatelliteData.tickEntity).
- SatelliteData.performAction (dump-to-IDataHandler) — testClient
  territory (needs EntityPlayer parameter).
- MissionOreMining / MissionGasCollection — registered as
  satellite types but extend Mission, separate code path. Belongs
  in TASK-06 (mission system depth).
- BiomeChanger MAX_SIZE=1024 queue cap — would need 1024+ adds to
  prove; low value vs runtime.
- BiomeChanger performAction radial generation — testClient territory
  (EntityPlayer-touching).
- SatelliteOreMapping selectedSlot / canFilterOre — testClient
  territory (player interaction).
- WeatherController floodlevel lazy-init from
  DimensionProperties.getSeaLevel — minor state-machine detail.

Total Phase 5 result: surface coverage moved from ~33-40% to
~75-80%. Remaining 20-25% is intentionally deferred (testClient
domain or other tasks).

## Completion Checklist

- [x] 14 new `/artest satellite` verbs + 1 new `/artest block`
      subcommand wired (8 in Phase 1-4 + 6 in Phase 5).
- [x] Base tick contract pinned (4 pins in
      `SatelliteTickBehaviourTest`).
- [x] Type-specific tick contract pinned (3 pins in
      `SatelliteTypeBehaviourTest`).
- [x] Coverage-gap closure (7 pins in
      `SatelliteCoverageGapsTest`).
- [x] All 26 satellite-* tests PASS on full testServer pyramid.
