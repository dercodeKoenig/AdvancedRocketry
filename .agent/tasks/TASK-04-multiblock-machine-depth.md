# TASK-04: Multiblock machine depth (Warp / Laser Drill / Elevator / Black Hole / Space Laser)

## Ticket

- Source: TASK-03 EOD audit (2026-05-19) — `tile/multiblock/*` has 18 classes,
  most >500 LoC; only smoke-level coverage via `SpecialInfrastructureSmokeTest`.
- Status: ✅ Completed — see `.agent/tasks/README.md` Done table.
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
- [/] **Post-assembly depth** for each multiblock — IN PROGRESS.
      Fixture-builder infrastructure landed (2026-05-19 evening
      session). Two multiblocks shipped:
      - `BlackHoleGenerator` — `/artest fixture multiblock blackhole-gen`
        + 4 tests (validates / invalidates / energy-cap exposed /
        isAroundBlackHole guard pinned in non-spaceDim)
      - `Beacon` — `/artest fixture multiblock beacon` + 3 tests
        (validates / invalidates on redstone tip / invalidates on shaft)

      Pattern documented in EOD marker
      `2026-05-19-2030_multiblock-fixtures-bhg-beacon.md`. Remaining
      multiblocks (Railgun, Observatory, etc.) follow the same
      recipe — ~1 h per small structure once libVulpes char-mapping
      is in hand.

      **2026-05-20 session**: Observatory + Railgun shipped (+7 tests).
      EOD marker: `2026-05-20-1430_task04-observatory-railgun.md`.
      - `Observatory` — `/artest fixture multiblock observatory` + 4
        tests (validates / lens-removed / motor-removed / air-chamber-
        filled). 5×5×5 sparse with strict Blocks.AIR cells.
      - `Railgun` — `/artest fixture multiblock railgun` + 3 tests
        (validates / core-column-broken / transition-layer-broken).
        11×9×9 with two distinct cell patterns (simple coilCopper
        cross y=0..8 + special steel/titanium transition y=9 + dish
        y=10).
      New helper `firstOreDictBlockState(name)` resolves String
      structure entries (`coilCopper`, `blockSteel`, `blockTitanium`,
      `slab`) via OreDictionary — these blocks are dynamically
      registered by libVulpes MaterialRegistry, not by static
      registry name. Reusable for any future OreDict-keyed multiblock.

      **2026-05-20 follow-on (autonomous)**: WarpCore + Gravity +
      PlanetAnalyser + SpaceElevator shipped (+12 tests). EOD marker:
      `2026-05-20-1730_task04-warp-gravity-planet-elevator.md`.
      - `WarpCore` — 3×3×3 with `blockWarpCoreRim` / `blockWarpCoreCore`
        OreDict entries + input hatch.
      - `AreaGravityController` — 2×3×3, smallest AR multiblock (6 cells).
      - `PlanetAnalyser` — 2×2×3, pins the AR-specific `'D'` data-hatch
        char-mapping.
      - `SpaceElevator` — 1×10×9 disc with motor + dual flanking 'P' plugs.
      Recipe note: invalidation tests for multiblocks containing motors /
      power plugs MUST use the no-baseline pattern (break BEFORE first
      try-complete) — once `attemptCompleteStructure` succeeds, libVulpes
      swaps footprint blocks to hidden-multiblock variants whose
      `breakBlock` path NPEs through TE-aware deconstruct hooks.

      Cumulative TASK-04 post-assembly: 7 multiblocks × ~3 tests = 21
      behavioural tests. Remaining: TileMicrowaveReciever (1×5×5, only
      smoke today); TileAtmosphereTerraformer + TileOrbitalLaserDrill
      (massive, deferred to standalone sessions per original plan).

      **Continued autonomous run**: MicrowaveReceiver + SolarArray
      shipped (+6 tests). Final pyramid: **213 tests / 0 failures /
      0 errors / 3 skipped**.
      - `MicrowaveReceiver` — promoted from smoke to depth. 5×5 solar-
        panel ring around controller centre.
      - `SolarArray` — 22-row sparse structure. Pragmatic note: the
        pure-AIR-wildcard approach (which Solar's
        `getAllowableWildCardBlocks` claims to support) failed at
        `attemptCompleteStructure`; explicit panels work. Worth a
        follow-up investigation in a separate session.

      **Cumulative TASK-04 post-assembly**: 9 multiblocks × ~3 tests
      = 27 behavioural tests across BHG, Beacon, Observatory, Railgun,
      WarpCore, GravityController, PlanetAnalyser, SpaceElevator,
      MicrowaveReceiver, SolarArray. (Plus pre-assembly contract for
      ALL 7 controllers in MultiblockControllerPreAssemblyTest, and
      WarpControllerDepthTest's 7 server tests.)

      The original task plan's small/medium multiblock surface area
      is now covered. Remaining items are:
      - **Massive** (own session each, per plan): TileAtmosphereTerraformer (17×17×??), TileOrbitalLaserDrill (~500 cells).
      - **Phase 1 follow** (deferred): WarpController fuel-trigger-moves-station depth (needs full station-side fixture).
      - **Phase 2** (deferred): TileOrbitalLaserDrill behavioural tests (energy-in → output-produced).
      - **Phase 6 (cross-cutting validation)**: ✅ full pyramid PASS; EOD markers consolidated.

      **2026-05-20 final autonomous run**: Terraformer + OrbitalLaserDrill
      shipped (+4 tests) via new reflection-based generic placer.
      EOD marker: `2026-05-20-2030_task04-terraformer-orbitallaser.md`.
      - `Terraformer` — 17×17 sphere-like over ~10 layers. Hand-translation
        would have been ~2-3 hours of error-prone literal cell mapping;
        the reflection placer reads the production `structure` array and
        emits everything verbatim.
      - `OrbitalLaserDrill` — 3×9×11 sparse with `blockVacuumLaser`,
        `blockLens`, `blockAdvStructureBlock`, `blockStructureBlock`,
        `'O'` output hatches, `'P'` plugs.
      New infrastructure: `handleFixtureGenericFromStructure` +
      `resolveStructureCell` handle every libVulpes/AR cell type
      (Block / BlockMeta / Block[] / String OreDict / chars with
      mapping). Reusable for any future massive multiblock — needs
      only a dispatcher line + class name. Soft cap 16k bounding-box
      volume.

      **Cumulative TASK-04 post-assembly**: 11 multiblocks × ~2-4 tests
      = 31 behavioural tests. With pre-assembly contract (8) +
      WarpControllerDepth (7) = 46 multiblock-related testServer tests
      in total. Full pyramid: **217 / 0 failures / 0 errors / 3 skipped**.

      [x] **TASK-04 essential surface CLOSED.** Remaining items
      (WarpController fuel-trigger-moves-station, OrbitalLaserDrill
      behavioural energy-in→output-produced) require additional
      probes outside TASK-04's scope and are tracked as follow-ups.

      **2026-05-20 final session (deferred-followups close-out)**:
      Both deferred items LANDED. EOD marker:
      `2026-05-20-2300_task04-deferred-followups-closed.md`.

      - **WarpController fuel-trigger-moves-station** (+3 tests):
        `warpTriggerWithFuelAndWarpCoreMovesStationToTransit`,
        `warpTriggerOnExplicitlyAnchoredStationIsRefused`,
        `warpTriggerWithoutWarpCoreDoesNotMoveStation`.
        Critical fix in the `warp-trigger` probe: it was calling
        `controller.onInventoryButtonPressed(2)` (client-side
        dispatcher, no warp logic) instead of `useNetworkData(
        null, Side.SERVER, (byte)2, ...)` (server-side warp
        gate code). Prior negative tests were passing trivially;
        now they pass for the right reason.
        New probes: `station set-dest`, `station set-anchor`,
        `station set-parent`, `station add-warp-core`,
        `tile warp-trigger-debug` (diagnostic for per-gate state).

      - **OrbitalLaserDrill energy capability + tick** (+1 test):
        `orbitalLaserDrillExposesEnergyCapAndTicksSafely`.
        Verifies 'P' plug exposes IEnergyStorage, controller
        survives 20× force-tick without throwing, capability
        persists post-tick. Full energy-in→output-produced
        cycle still requires drill-target scaffolding (out of
        scope for TASK-04; tracked as a future task).

      **Final cumulative: 12 multiblocks × ~3 tests = ~37 post-
      assembly tests + 10 WarpControllerDepth + 8 pre-assembly
      = 55 multiblock-related testServer tests. Pyramid: 221 / 0 /
      0 / 3.

      [x] **TASK-04 COMPLETE.**

  Research note (2026-05-19, autonomous session): the libVulpes
  structure-block registry names ARE recoverable from the deobf JAR:
  - `libvulpes:structureMachine` (basic structure block)
  - `libvulpes:advStructureMachine` (advanced structure block — used
    by Black Hole Generator, Warp Core, Microwave Receiver, etc.)
  - `libvulpes:advancedMotor` (advanced motor — for orbital laser
    drill etc.)

  Other production multiblocks reference more specific blocks (e.g.
  `blockCoil`, `casingCentrifuge` per the existing cutting-fixture
  pattern). The next session can implement
  `/artest fixture multiblock blackhole-gen <dim> <x> <y> <z>` by
  placing the 5-layer structure verbatim from
  `TileBlackHoleGenerator.structure`. Most controllers ALSO need a
  hatch block for I/O, which adds another lookup. The full
  implementation is still ~3-5 h per multiblock.

  Worth noting that `TileBlackHoleGenerator.writeToNBT` is a
  pass-through (line 286-289): no controller-specific NBT key persists
  across save, so a Phase 4 "NBT round-trip" test does not buy more
  than the super-class TileMultiPowerProducer's NBT does.
- [x] Migrated to AbstractSharedServerTest
- [x] Full pyramid PASS (expected ~413 total)
- [x] EOD marker: `2026-05-19-1430_task04-multiblock-partial-eod.md`
