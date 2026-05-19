# TASK-04: Multiblock machine depth (Warp / Laser Drill / Elevator / Black Hole / Space Laser)

## Ticket

- Source: TASK-03 EOD audit (2026-05-19) — `tile/multiblock/*` has 18 classes,
  most >500 LoC; only smoke-level coverage via `SpecialInfrastructureSmokeTest`.
- Status: Pending
- Created: 2026-05-19
- Predecessor: `.agent/.context-markers/2026-05-19-1230_task03-A-and-B-mostly-done-eod.md`

## Context

Multiblock machines are AR's late-game gameplay anchors. The top-5 by LoC and
player visibility are:

| Tile class | LoC | Role | Current coverage |
|---|---|---|---|
| `TileWarpController` (station/) | 958 | Drives station warp jumps | smoke only |
| `TileOrbitalLaserDrill` | 863 | Asteroid mining | smoke only |
| `TileSpaceElevator` | 538 | Planet-to-station transport | smoke only |
| `TileBlackHoleGenerator` | ~500 | End-game power source | smoke only |
| `TileSpaceLaser` | ~400 | Long-range targeting | smoke only |

A regression in any of these silently breaks the corresponding gameplay loop —
modpack players hit it months after the change. `SpecialInfrastructureSmokeTest`
just confirms placement + tickability; no behavioural assertion (formed→working,
energy in→output produced, NBT round-trip).

Out of scope: visual-regression / GUI testing (Phase 10 separate); pipe
end-to-end (blocked by commented-out pipe blocks).

**No production logic changes** (same rule as TASK-01 §15).

## Implementation Plan

### Phase 1: TileWarpController + warp cycle (~5-6 h)

- [ ] Probe extensions:
  - `/artest fixture warp-multiblock <dim> <x> <y> <z>` — places the
    valid warp-controller multiblock structure (controller + warp core +
    monitor + linked station).
  - `/artest warp info <dim> <x> <y> <z>` — dumps controller state:
    isFormed, fuelStored, targetDimId, isWarping, ticksRemaining.
  - `/artest warp trigger <dim> <x> <y> <z>` — invokes production
    `tile.beginWarp()`.
- [ ] Tests:
  - `warpControllerFormsValidMultiblockAndExposesEnergyCap`
  - `warpTriggerStartsCountdownAndConsumesFuel`
  - `warpWithInsufficientFuelStaysIdle`
  - `warpToInvalidDimReportsError`
  - `warpStateSurvivesChunkUnloadReload`

### Phase 2: TileOrbitalLaserDrill + asteroid mining (~4-5 h)

- [ ] Probe extensions:
  - `/artest fixture laser-drill-multiblock <dim> <x> <y> <z>`
  - `/artest laser-drill state <dim> <x> <y> <z>` — energy, mining
    progress, target asteroid, output buffer.
- [ ] Tests:
  - `laserDrillFormsAndAcceptsEnergy`
  - `laserDrillWithoutTargetStaysIdleNoEnergyDrain`
  - `laserDrillWithTargetConsumesEnergyAndProducesOutput`
  - `laserDrillOutputBufferRespectsCapacity`
  - `laserDrillNBTRoundTripPreservesOutputBuffer`

### Phase 3: TileSpaceElevator (~3-4 h)

- [ ] Probe extensions: form/info/ride probes.
- [ ] Tests:
  - `elevatorFormsAndExposesCapsuleSpawnPoint`
  - `elevatorAscendDescendCycleCompletes`
  - `elevatorRequiresStationLinkBeforeOperation`
  - `elevatorChipIsRespectedForTargetStation`

### Phase 4: TileBlackHoleGenerator (~3 h)

- [ ] Probe extensions: form / set-input / read-output.
- [ ] Tests:
  - `blackHoleGeneratorFormsAndExposesEnergyCap`
  - `blackHoleGeneratorConsumesInputAndProducesEnergy`
  - `blackHoleGeneratorWithoutFuelStaysIdle`
  - `blackHoleGeneratorOutputClampsAtCap`

### Phase 5: TileSpaceLaser (~2-3 h)

- [ ] Probe extensions: form / target / fire.
- [ ] Tests:
  - `spaceLaserFormsAndAcceptsEnergy`
  - `spaceLaserFireWithoutTargetReportsError`
  - `spaceLaserFireDrainsEnergyPerOperation`
  - `spaceLaserRespectsTargetDimensionGate`

### Phase 6: Cross-cutting validation (~1 h)

- [ ] Full pyramid PASS.
- [ ] EOD marker documenting probe surface additions + behavioural deltas.

## Technical Decisions

- **Each multiblock fixture is its own `/artest fixture` verb** — keeps
  the probe surface readable and tests focused.
- Migrate test classes to `AbstractSharedServerTest` where possible
  (multiblock fixtures are position-isolated by definition).
- For ITickable multiblocks, use `force-tick` with explicit tick counts;
  never rely on wall-clock.
- For energy assertions, use the existing `/artest energy stored / inject`
  probes — they already support multiblock controller tiles.

## Dependencies

**Requires**: TASK-03 (AbstractSharedServerTest base, probe surface).
**Does NOT block**: any feature work.

## Estimated effort

~18-22 hours across 5-6 sessions (one per phase).

## Completion Checklist

- [x] Warp controller depth — `WarpControllerDepthTest` 7 server tests
      (2026-05-19 14:00). Post-assembly fuel-trigger-moves-station
      deferred (needs full station-side fixture).
- [x] Multiblock pre-assembly contract for ALL 7 controllers
      consolidated into `MultiblockControllerPreAssemblyTest` 8 server
      tests (2026-05-19 14:15). Covers orbital laser drill, space
      elevator, black hole generator, warp core, observatory, railgun,
      planet analyser.
- [ ] **Post-assembly depth** for each multiblock — DEFERRED. Each
      needs its own fixture probe (~3-5 h). Queued as a future TASK-04b
      session or splittable into TASK-04a / 04b / 04c.
- [x] Migrated to AbstractSharedServerTest
- [x] Full pyramid PASS (expected ~413 total)
- [x] EOD marker: `2026-05-19-1430_task04-multiblock-partial-eod.md`
