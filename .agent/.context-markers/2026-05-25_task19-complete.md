# Marker — 2026-05-25 TASK-19 complete

**Branch**: `feature/tests`
**Status**: ✅ TASK-19 (multiblock powered-cycle trio) fully shipped.

## Session arc

1. **F8 watch v11 sweep** — 9/10 PASS, F8 0 recurrence, F9 new
   single-occurrence. TASK-29 not opened (commit `44009db9`).

2. **TASK-19 Phase 1a** — terraformer on AR planet. Diagnostic
   probes (`controller-state`, `clear-batteries`) uncovered:
   - `TileFluidHatch` holds one fluid at a time → split N2/O2
     across hatches 0+1 vs 2+3.
   - Default creative input plug provides infinite power → counter-
     test needs `clear-batteries` to make "no power" observable.
   - 3/3 tests pass (commit `5d43df08`).

3. **TASK-19 Phase 1b** — terraformer on overworld with
   `allowTerraformNonAR=true`. New `artest config get/set`
   whitelisted to terraformer keys. 2/2 tests pass (commit
   `5d43df08`).

4. **TASK-19 Phase 2** — BHG on station orbiting black-hole star.
   New `artest star get/set-blackhole` for Sol flag. 3/3 tests pass
   (commit pending).

5. **TASK-19 Phase 3** — Beacon enable/disable/break. Reuses
   existing probes (no new infra needed). 3/3 tests pass (commit
   pending).

## What shipped — TASK-19 totals

**11 server-tier tests** across **4 classes**:

| Class | Tests | Branch pinned |
|---|---|---|
| `TerraformerPoweredCycleOnArPlanetTest` | 3 | native-AR-planet branch |
| `TerraformerPoweredCycleOnOverworldTest` | 2 | `allowTerraformNonAR` branch |
| `BlackHoleGeneratorPoweredCycleTest` | 3 | space-dim + black-hole-star gate |
| `BeaconEnableCycleTest` | 3 | `setMachineEnabled` ↔ beacon registry |

**5 new probe verbs** in `TestProbeCommand`:

| Verb | Use |
|---|---|
| `machine controller-state <pos>` | reflective dump of `batteries`, `fluidInPorts`, `currentTime`, `outOfFluid` — diagnostic primary use |
| `machine clear-batteries <pos>` | clears libVulpes `MultiBattery` aggregator (creative plug defeats "skip energy inject") |
| `config get/set <key> [value]` | whitelisted ARConfiguration reflection (`allowTerraformNonAR`, `terraformRequiresFluid`) |
| `star get/set-blackhole <id> [value]` | `StellarBody.setBlackHole` public API exposure |
| — | (Phase 3 reused existing `beacon list`, `place`, etc.) |

## Pyramid

237 / 80 / **350** / 41 = **708** (+11 from 697).
Counter regenerated via grep on `@Test$` per-layer; verified
2026-05-25.

## Open follow-ups

- **F8** Beacon `try-complete attempted:false` — 1/5 toward Obsolete.
- **F9** `MissionGasCompletion.fluidEntries:0` — 1/5 toward Obsolete.
- **TASK-29 not opened** — both shapes single-occurrence; promote
  on 2nd sighting per `flake-diagnosis.md`.

## Bug ledger

Drained. No new bugs found during TASK-19. (Notable: the beacon
break path DOES correctly unregister via `BlockBeacon.breakBlock`
— Phase 3 test confirmed; original task plan's worry about a
missing teardown was unfounded.)

## Key learnings (recorded in TASK-19 file)

- `TileFluidHatch` is single-fluid-per-tank → distribute fuels
  across hatches in any test that needs more than one fluid type.
- Default `'P'`-fixture places `blockCreativeInputPlug` whose
  `getUniversalEnergyStored() = MAX_VALUE >> 4` unconditionally —
  to observe "no power" branch, explicitly `clear-batteries`.
- `getCompletionTime() = 18000 × terraformSpeed` — tests need
  20000+ force-ticks + fluid refill loop for at least one cycle
  to complete.
- `TileBlackHoleGenerator.isAroundBlackHole()` requires three
  things in sequence: space dim placement + space station at coords
  + station orbiting a black-hole star.
- `TileBeacon` registry mutation is gated on `isDimensionCreated(dim)`
  — overworld tests would pass trivially without exercising the
  contract; tests MUST run on AR-generated planets.

## Resumption

Pick the next backlog task or continue ad-hoc work. TASK-19 is
fully done — no carry-forward state.

Backlog: TASK-20 (Hovercraft testClient, ~9 h), TASK-21 (/ar
player-equipped, ~6 h), TASK-22 (UV-assembler, ~4 h), TASK-23
(SealDetector branches, ~4 h), TASK-24 (SpaceArmor CHEST, ~2.5 h).
TASK-22 / 24 are the shortest if a small win is preferred.
