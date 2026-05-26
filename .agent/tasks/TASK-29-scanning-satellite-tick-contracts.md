# TASK-29: Scanning satellite tick behaviour contracts

## Ticket

- Source: 2026-05-25 Tier 2/3 audit, gap #1. Carried forward into
  2026-05-26 audit out-of-scope as still-deferred.
- Status: **Backlog** — ready to ship, no blocker.
- Created: 2026-05-26.

## Context

[`ScanningSatelliteContractTest`](../../src/test/java/zmaster587/advancedRocketry/test/unit/ScanningSatelliteContractTest.java)
pins **constructor invariants** (name uniqueness, failureChance
sanity, OreMapping ore-filter gate) for the six scanning satellite
types:

- `SatelliteOreMapping`
- `SatelliteDensity`
- `SatelliteComposition`
- `SatelliteMassScanner`
- `SatelliteOptical`
- `SatelliteSpyTelescope`

What is **not** pinned: their `tickEntity()` behaviour. Each scanner
emits player-visible data (chunk scan results, mass readings,
density samples) on tick, and the read-back / Item data-stick
output contract is the actual user surface. The deep-tier audit
explicitly recommended this as the next batch.

## Why it matters

Each scanning satellite is the player's only way to acquire the
corresponding data type (mass for fuel calc, ore distribution for
laser-drill seeding, composition for biome predictions). If a
scanner stops emitting data on tick, the corresponding gameplay
gate silently breaks.

## Implementation plan

| Phase | Effort | Result |
|---|---|---|
| 0 | ~30 min | Audit current probe surface: `satellite tick-once`, `satellite battery-set`, `satellite data-readback` already exist? Extend or add as needed. Look at TASK-09 probe additions for shape. |
| 1 | ~3 h | `ScanningSatelliteTickContractTest` — 6 tests, one per scanner type. Each: seed battery → tick → observe at least one data-output side effect (data stick filled, NBT data field present, registry-visible state changed). Loose end-state pins (no exact RF / loop bound). |
| 2 | ~1 h | Optional: cross-type negative — a scanner with NO battery does not produce output. Catches the "missing power gate" regression class. |

## Acceptance

- [ ] 6-7 tests added; suite stays green at the same wall-time
      bucket.
- [ ] Each test's contract litmus passes: "fails if production
      stops emitting data of type X on tick". No impl-detail pins
      (exact RF, loop bound, internal field shape).
- [ ] Pyramid counter regenerated per
      [`task-lifecycle.md`](../sops/development/task-lifecycle.md)
      step 2.5.

## Out of scope

- Per-chunk scan output exact values (depends on worldgen RNG,
  not contract). Pin "data field non-zero after tick", not "data
  field == 1234".
- Cross-scanner interactions (multiple scanners on the same
  satellite chip — out of base contract).

## Dependencies

- Does NOT block any other task.
- Reuses `ScanningSatelliteContractTest` infrastructure
  (MinecraftBootstrap, no server tier needed unless data-stick
  read-back requires it).

## Estimated effort

- Phase 0: 30 min
- Phase 1: 3 h
- Phase 2: 1 h
- **Total**: ~4-5 h

## Risk

Low. Existing ScanningSatelliteContractTest pattern proves the
unit-tier path works; the tick depth is additive coverage on the
same fixture.
