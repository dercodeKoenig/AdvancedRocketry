# Context Marker: TASK-04 Phase 1 + Phases 2-5 consolidated (pre-assembly contract)

**Created**: 2026-05-19 14:30 local
**Branch**: `feature/tests`
**Status**: ✅ Phase 1 (TileWarpController depth) shipped. Phases 2-5
collapsed into a single `MultiblockControllerPreAssemblyTest` covering
the **pre-assembly contract** for 7 multiblock controllers (orbital
laser drill, space elevator, black hole generator, warp core,
observatory, railgun, planet analyser). The **post-assembly** depth
(form multiblock structure → tick → produce output) **remains deferred**
— each multiblock needs its own non-trivial fixture; out of scope for a
single session.

---

## TL;DR

- **+7 server tests** in `WarpControllerDepthTest` — real warp-controller
  depth: controller-in-station vs out-of-station resolution, trigger
  consumes-fuel gate, trigger-on-current-dim rejection, multi-station
  isolation.
- **+8 server tests** in `MultiblockControllerPreAssemblyTest` — every
  multiblock controller in the mod placed in isolation, `isComplete=false`
  pinned, force-tick safety pinned, `canRender` flag pinned where
  exposed.
- **3 new `/artest tile` probe verbs**: `warp-state`, `warp-trigger`,
  `multiblock-state`.
- All tests on `AbstractSharedServerTest` — class-scoped harness lifecycle.

---

## Pyramid state (post-TASK-04 partial)

| Layer | Result | Δ from 2026-05-19 12:30 (TASK-03 final) |
|---|---|---|
| testUnit | 162 / 0 / 0 | (unchanged) |
| testIntegration | 80 / 0 / 0 | (unchanged) |
| testServer | ~165 / 0 / 3 | +15 (7 warp + 8 multiblock pre-assembly) |
| testClient | 6 / 0 / 0 | (unchanged) |
| **Total** | **~413 / 0 / 3** | **+15 over TASK-03 final (398)** |

---

## Phase 1 — `WarpControllerDepthTest` (7 server tests)

Drives the real `TileWarpController` state machine without bypassing
production gates:

- `warpControllerInOverworldHasNoSpaceObject` — sanity: outside spaceDim
  the controller has no station context (regression-net against a
  refactor that returns a spurious station from any block coord).
- `warpControllerForceTickOutsideStationDoesNotCrash` — defensive
  baseline that the per-tick loop handles `getSpaceObject() == null`.
- `warpControllerInsideStationLinksToStation` — create station →
  read its `spawnX/spawnZ` → place controller at those coords in
  spaceDim (-2) → controller's `getSpaceObject()` resolves back to the
  same station id. This pins
  `SpaceObjectManager.getSpaceStationFromBlockCoords`'s coord→station
  formula.
- `warpTriggerWithoutFuelDoesNotMoveStation` — station.useFuel(cost)
  gate. Fuel=0 → no warp.
- `warpTriggerOnAnchoredStationIsRefused` — currently exercises the
  destination-equals-current branch (no anchored-toggle probe yet);
  documents the contract for a future tighter test.
- `travelCostFieldIsExposedAndNonNegative` — probe-surface guard for
  the new `warp-state` probe's `travelCost` field.
- `multipleStationsHaveDistinctWarpControllerContexts` — two stations
  → two controllers at their respective spawn coords → resolve to two
  different station ids. Pins per-station-coord isolation.

## Phases 2-5 — `MultiblockControllerPreAssemblyTest` (8 server tests)

For each of 7 multiblock controllers in the mod, pin:

1. Block places successfully.
2. Tile class FQN matches expected (regression-net for registry-name
   drift).
3. `isComplete()` returns false on isolated placement (no surrounding
   structure built).
4. `force-tick` is safe — `update()` early-exits cleanly when the
   structure isn't complete.

Plus one cross-cutting test pinning `canRender` doesn't lie about
formation state.

Coverage map:

| Registry name | Tile class | Tested |
|---|---|---|
| `spaceLaser` | `TileOrbitalLaserDrill` | ✅ |
| `spaceElevatorController` | `TileSpaceElevator` | ✅ |
| `blackholegenerator` | `TileBlackHoleGenerator` | ✅ |
| `warpCore` | `TileWarpCore` | ✅ |
| `observatory` | `TileObservatory` | ✅ |
| `railgun` | `TileRailgun` | ✅ |
| `planetAnalyser` | `TileAstrobodyDataProcessor` (surprise — tile name diverges from block name) | ✅ |

Surprise pinned: `planetAnalyser` block resolves to
`TileAstrobodyDataProcessor`, not a hypothetical `TilePlanetAnalyser`.
Documented inline so a future test author lands on the correct class
name without trial-and-error.

---

## Probe surface delta

`/artest tile warp-state <dim> <x> <y> <z>` — dumps controller +
hosted station state. Fields: `tileClass`, `hasSpaceObject`,
`stationId`, `stationOrbitingDim`, `stationDestDim`, `stationFuel`,
`stationAnchored`, `hasUsableWarpCore`, `travelCost`.

`/artest tile warp-trigger <dim> <x> <y> <z>` — invokes
`onInventoryButtonPressed(2)` on the controller (the warp-go button).
Does NOT bypass production gates.

`/artest tile multiblock-state <dim> <x> <y> <z>` — generic probe
for libVulpes TileMultiBlock controllers; reflects on `isComplete()`
+ `canRender` field. Returns `<not a multiblock>` if the tile
doesn't expose `isComplete`.

---

## What's intentionally NOT in this marker

**Post-assembly depth** for all 7 multiblocks. Each one needs:
- A `/artest fixture <multiblock-name>` probe to build the right
  surrounding shape.
- Behavioural tests for the formed state (energy intake, per-tick
  output, NBT round-trip of internal state).

That work is ~3-5 h per multiblock. Bundling all 5 in one task is
unrealistic for a single session. The current `MultiblockControllerPreAssemblyTest`
gives us the pre-assembly net; the post-assembly depth is queued as a
TASK-04b (or independent multi-multiblock task) for future sessions.

Stations themselves (warp + station building) ARE covered by the
existing `SpaceStationDockUndockTest` / `SpaceStationDepthTest` /
`SpaceStationPadPersistenceTest` family — the gap was the **controller
on the station-side**, which Phase 1 closes.

---

## Restore instructions

```
Read .agent/.context-markers/2026-05-19-1430_task04-multiblock-partial-eod.md
Read .agent/.context-markers/2026-05-19-1230_task03-A-and-B-mostly-done-eod.md
Read .agent/tasks/TASK-04-multiblock-machine-depth.md
```

Open items (TASK-04 follow-up sessions, in priority order):

1. **Post-assembly fixture for TileOrbitalLaserDrill** — most-used
   late-game machine. Build the multiblock shape via probe, target an
   asteroid, tick, assert output buffer.
2. **Post-assembly fixture for TileWarpCore + TileWarpController** —
   the full station-warp loop: build core, set destination, trigger,
   assert station moved AND fuel consumed AND warp-reached
   advancement granted.
3. **Post-assembly for TileBlackHoleGenerator** — endgame power
   accounting.
4. **Post-assembly for TileSpaceElevator** — ascend/descend cycle
   with capsule.
5. **Post-assembly for TileObservatory / TileRailgun** — likely
   batchable.

Nothing here blocks releasing the suite at the new ~413/0/3 total.
