# TASK-09: Per-satellite-type behavioural depth

## Ticket

- Source: TASK-03 EOD audit (2026-05-19) — `satellite/` has 11
  classes; `SatelliteLifecycleSmokeTest` covers only create/list/info.
  Per-type tick / produce / consume behaviour is uncovered.
- Status: Pending
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

## Completion Checklist

- [ ] 3 new `/artest satellite` verbs
- [ ] Energy / microwave: 3 tests
- [ ] Mining: 2 tests
- [ ] Gas / surveillance / data: 3 tests
- [ ] Cross-cutting: 2 tests
- [ ] Full pyramid PASS
- [ ] EOD marker
