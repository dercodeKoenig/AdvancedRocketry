# TASK-09: Per-satellite-type behavioural depth

## Ticket

- Source: TASK-03 EOD audit (2026-05-19) — `satellite/` has 11
  classes; `SatelliteLifecycleSmokeTest` covers only create/list/info.
  Per-type tick / produce / consume behaviour is uncovered.
- Status: ✅ Completed (2026-05-21). Scope rewritten — see "Actual
  delivery" below; original phase-by-phase plan was speculative
  (class names didn't match the codebase).
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

- `baseSatelliteTickAccruesPowerGenMinusOnePerTick` —
  `oreScanner` (pure `SatelliteBase`) accrues exactly
  `powerGen - 1` per tick.
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

## Completion Checklist

- [x] 8 new `/artest satellite` verbs + 1 new `/artest block`
      subcommand wired.
- [x] Base tick contract pinned (4 pins in
      `SatelliteTickBehaviourTest`).
- [x] Type-specific tick contract pinned (3 pins in
      `SatelliteTypeBehaviourTest`).
- [x] Both tests PASS on full testServer pyramid.
