# Context Marker: TASK-04 — WarpCore + GravityController + PlanetAnalyser + SpaceElevator

**Created**: 2026-05-20 17:30 local
**Branch**: `feature/tests`
**Session type**: autonomous continuation of TASK-04 (multiblock depth).
**Predecessors**: [[2026-05-20-1430_task04-observatory-railgun]], [[2026-05-19-2030_multiblock-fixtures-bhg-beacon]].

---

## What landed this session

| Multiblock | Fixture probe | Tests | Status |
|---|---|---|---|
| WarpCore | `/artest fixture multiblock warp-core` | 3 | ✅ |
| AreaGravityController | `/artest fixture multiblock gravity-controller` | 3 | ✅ |
| PlanetAnalyser (AstrobodyDataProcessor) | `/artest fixture multiblock planet-analyser` | 3 | ✅ |
| SpaceElevator | `/artest fixture multiblock space-elevator` | 3 | ✅ |

**Pyramid delta**: +12 testServer tests. Full pyramid: **207 tests / 0 failures / 0 errors / 3 skipped**.

Cumulative TASK-04 post-assembly coverage: 7 multiblocks × ~3 tests = 21 behavioural tests + the prior BHG/Beacon/Observatory/Railgun.

## Continued same session: MicrowaveReceiver + SolarArray

| Multiblock | Fixture probe | Tests | Status |
|---|---|---|---|
| MicrowaveReceiver | `/artest fixture multiblock microwave-receiver` | 3 | ✅ |
| SolarArray | `/artest fixture multiblock solar-array` | 3 | ✅ |

**Final pyramid**: **213 tests / 0 failures / 0 errors / 3 skipped** (+6 from the 207 baseline above; total +18 for the day across 6 multiblocks).

- **MicrowaveReceiver** — 1×5×5, fixture places `blockSolarPanel` at all 24 non-controller cells (the literal-block cells + wildcards both accept solar panel).
- **SolarArray** — 1×3×22 sparse, fixture places controller + 2 `'p'` plugs at row z=0 plus 63 panels at rows z=1..21. The pure-AIR-wildcard approach failed `attemptCompleteStructure` despite Solar's `getAllowableWildCardBlocks` claiming to accept AIR; switching to explicit panels works. Worth investigating in a follow-up — possibly the controller's NORTH-facing FACING property isn't preserved on `setBlockState` for `BlockMultiblockMachine`, or the AIR-cell match in `getAllowableBlocks` doesn't trigger as expected for `'*'` wildcards. Pragmatic workaround: place explicit panels.

### Sizes / patterns

- **WarpCore** — 3×3×3, OreDict `blockWarpCoreRim`/`blockWarpCoreCore` + `'I'` hatch.
- **AreaGravityController** — 2×3×3, the smallest AR multiblock (6 non-null cells). 'c' on top + advStruct cross with 'P' plug centre below.
- **PlanetAnalyser** — 2×2×3, slabs + I/O/P + three 'D' data hatches. Pins the AR-specific `'D'` char-mapping (registered in `AdvancedRocketry.preInit` line ~1042).
- **SpaceElevator** — 1×10×9 disc, motor + advStruct inner ring, slab outer ring, blockSteel corner caps, dual 'P' plugs flanking the controller, strict `Blocks.AIR` corners.

### Surprises / pitfalls hit

**Once `attemptCompleteStructure` succeeds, libVulpes swaps the footprint blocks to hidden-multiblock variants whose `breakBlock` path can NPE for TE-aware cells (motor, power plug) and even for hidden adv-structures.** For multiblocks containing motors/plugs adjacent to the test break point, the invalidation pattern must be:

```
fixture → break the cell → try-complete (expect isComplete:false)
```

NOT the BHG/Beacon pattern of `fixture → baseline try-complete → break → try-complete`. The baseline try-complete converts blocks to hidden state; the subsequent `artest place` then triggers the NPE.

Updated SpaceElevator tests to use the no-baseline pattern. The smaller multiblocks (WarpCore, Gravity, PlanetAnalyser) hit the baseline-pattern path safely because their footprint blocks are simple (rim/slab/struct), and replacing one with stone doesn't dig into a TE-aware deconstruct hook.

**Recipe note added**: when authoring invalidation tests for any future multiblock containing motors / power plugs / data hatches, default to the no-baseline pattern.

---

## Commits planned on `feature/tests`

```
test: TASK-04 — WarpCore + Gravity + PlanetAnalyser + SpaceElevator multiblocks (+12 tests)
```

(Single commit covering 4 new test classes + 4 new fixture handlers + the existing OreDict helper.)

Plus the CLAUDE.md "Language" section update (separate small commit, or bundle).

---

## Remaining TASK-04 work

Per the original task plan and prior markers — open items:

1. **Massive multiblocks (own session each)**:
   - `TileAtmosphereTerraformer` (17×17×??)
   - `TileOrbitalLaserDrill` (9×11×11, ~500 cells)
2. **TileMicrowaveReciever** — 1×5×5, already covered as smoke (worth promoting to depth).
3. **Original Phase 1 followup**: post-assembly fuel-trigger-moves-station for WarpController — needs full station-side fixture.
4. **Phase 2 of original plan** — `TileOrbitalLaserDrill` post-assembly behavioural tests (energy-in → output-produced).

With the 7 multiblocks done, the task's `[/] Post-assembly depth` checkbox now covers the practical "common gameplay" set. Remaining items are either massive (own sessions) or behaviour-on-formed (which depends on additional probes like energy injection + tile ticking, separate scope).

---

## Restore instructions

```
Read .agent/.context-markers/2026-05-20-1730_task04-warp-gravity-planet-elevator.md
Read .agent/tasks/TASK-04-multiblock-machine-depth.md
Read src/main/java/zmaster587/advancedRocketry/command/test/TestProbeCommand.java  # handleFixtureWarpCore + handleFixtureGravityController + handleFixturePlanetAnalyser + handleFixtureSpaceElevator
Read CLAUDE.md  # Language section (Russian replies)
```
