# TASK-40 — Batch 1: Gap E + Gap A + Gap D

**Status: ✅ Completed (2026-05-29)**

## Ticket

- Source: 2026-05-27 coverage audit, parent
  `.agent/audits/2026-05-27-full-coverage-audit.md` §3 Gaps E/A/D;
  2026-05-29 delta audit §6 recommended landing order.
- Status: ✅ Completed (2026-05-29).
- Created: 2026-05-29.

## Context

Three contract gaps from the 2026-05-27 audit, batched together because
the probe-surface additions overlap and the test classes share fixture
patterns. Each gap reshaped during Phase 0 — the audit's framing was
speculative for two of three (D and A).

## Phase-0 reshape notes

### Gap E — Rocket loader/unloader item active transfer

**Audit framing**: "An armed RocketLoader adjacent to a placed rocket
transfers > 0 items from its inventory into the rocket's storage chunk
under a real server tick; an armed RocketUnloader drains > 0 items back
into its own inventory."

**Phase-0 finding**: the LOADER half is **already pinned** by the
pre-existing
[`RocketInfrastructureSmokeTest#rocketLoaderTransfersItemsAfterLanding`](../../src/test/java/zmaster587/advancedRocketry/test/server/RocketInfrastructureSmokeTest.java)
(TASK-09 SMART §7.10 #1). The UNLOADER half was deferred there to a
"once a chest-pre-populate probe lands" comment.

**Scope**: ship 1 test for the unloader half + 1 new probe verb
(`rocket storage-item-fill`) mirroring TASK-34's `storage-fluid-fill`.

### Gap A — Railgun firing

**Audit framing**: "A formed + fully-powered Railgun, given a target
dimension token, emits the orbital firing event and deducts > 0 RF from
its battery."

**Phase-0 finding**: `TileRailgun.attemptCargoTransfer` is actually a
**paired-railgun item-transport** system, not a weapon. Source picks an
item from its input port, dispatches to a linked destination railgun
across dims; destination's `onReceiveCargo` deposits to its output port;
`EntityItemAbducted` is the in-flight delivery visual. No orbital
projectile, no weapon damage.

**Contract reshape**: pin the **receiver-side** contract (player-visible
endpoint: cargo arrives at destination's output port). The source-side
firing requires TWO paired railguns — outside reach of a single-fixture
test; deferred per
`.agent/sops/development/testing-principles.md` "test the contract you
can pin cheaply, not the contract you wish you could pin".

**Scope**: 1 test calling `infra railgun-receive-cargo` on a SOLO assembled
railgun + scanning `itemOutPorts` for deposit.

### Gap D — Planet Analyser scan output

**Audit framing**: "A formed + powered PlanetAnalyser tick-fed with a
planet-id chip produces a `SatelliteData` slot output matching the chip's
planet's properties."

**Phase-0 finding**: the production class is `TileAstrobodyDataProcessor`
(registry name `planetAnalyser`). It processes an `ItemAsteroidChip`
(NOT a planet-id chip), via connected `TileDataBus` cables containing
per-type DataStorage. Per research cycle (10 ticks per data type,
`maxResearchTime` constant), the chip's data field is incremented by 1
and the corresponding amount is extracted from the data bus.

**Contract reshape**: "powered + AsteroidChip with UUID + non-zero
maxData in slot 0 + DataBus with COMPOSITION data + researching flag set
→ after a research cycle, the chip's COMPOSITION data field rises by ≥ 1."

**Sub-finding** during test authoring: `ItemMultiData.isFull` returns
`true` when `maxData == data`. A fresh chip has `maxData = 0` → isFull is
true for any value → `attemptAllResearchStart` rejects the cycle before
it begins. The probe sets `maxData = 30` (≥ 3 cycles' headroom) along
with the UUID so the gate passes.

**Scope**: 4 new probe verbs (`astrobody-set-research`,
`astrobody-load-chip`, `astrobody-chip-data`, `databus-set-data`) + 1 test.

## Implementation Plan

### Phase 1: Probe additions ✅

- [x] `rocket storage-item-fill <entityId> <itemId> <count>` — mirror of
  TASK-34's `storage-fluid-fill`. Iterates `rocket.storage.getInventoryTiles()`
  with the production filter (skips `TileGuidanceComputer`, matching
  `TileRocketLoader.update()` line 126), prefers `IInventory` cast over
  ITEM_HANDLER capability (matches unloader iteration order so test
  pre-fills land in the same tiles the unloader reads). Calls
  `markDirty()` on the receiving tile.
- [x] `infra railgun-receive-cargo <dim> <x> <y> <z> <itemId> [count]` —
  calls `TileRailgun.onReceiveCargo(stack)`, then reflects on
  `TileMultiBlock.itemOutPorts` (libVulpes — declared in the
  grand-parent class, not `TileMultiblockMachine`) to count deposited
  stacks. Returns `canReceive` gate + `outPortCount` + `matchedCount`.
- [x] `infra astrobody-load-chip <dim> <x> <y> <z>` — creates an
  `ItemAsteroidChip` with UUID=1L and maxData=30, places into the
  analyser's slot 0 via `setInventorySlotContents`.
- [x] `infra astrobody-set-research <dim> <x> <y> <z> <bits>` — sets
  private `researchingAtmosphere`/`Distance`/`Mass` fields via reflection
  (bits 1/2/4), then invokes `attemptAllResearchStart` to arm progress
  fields.
- [x] `infra astrobody-chip-data <dim> <x> <y> <z>` — reads slot 0 chip's
  COMPOSITION/DISTANCE/MASS/max values.
- [x] `infra databus-set-data <dim> <x> <y> <z> <type> <amount>` — calls
  public `TileDataBus.setData(amount, type)` to seed analyser-side data.
- [x] `infra unloader-debug <dim> <x> <y> <z>` — diagnostic verb dumping
  rocket linkage + storage tile classes + per-tile slot 0 contents +
  unloader's own slots + worldIsRemote. Kept after green for future
  rocket-storage debugging.

### Phase 2: Tests ✅

- [x] `RocketItemUnloaderActiveTransferTest.unloaderPullsItemsFromRocketStorage`
  — `with-cargo` fixture (vanilla chest in storage) → fill chest with
  cobblestone via `storage-item-fill` → link unloader → force-tick 60 →
  assert unloader inventory has cobblestone.
- [x] `RailgunCargoReceiveContractTest.railgunOnReceiveCargoDepositsStackToOutputPort`
  — assemble fixture + try-complete → `infra railgun-receive-cargo 16
  cobblestone` → assert `canReceive=true`, `outPortCount ≥ 1`,
  `matchedCount ≥ 16`.
- [x] `PlanetAnalyserResearchContractTest.poweredAnalyserIncrementsChipCompositionFromDataBus`
  — assemble + try-complete → seed all 3 data hatches with COMPOSITION=30
  → energy inject 100k RF → load chip → assert composition=0 baseline →
  set researching=Atmosphere → force-tick 30 → assert composition ≥ 1.

### Phase 3: Validation ✅

- [x] testServer green for all 3 test classes (1m 12s wall).

## Technical Decisions

- **Mirror of TASK-34 storage-fluid-fill pattern**: same probe verb
  shape, same fixture variant (with-cargo here; with-fluid-cargo there);
  same 60-tick force-tick budget for end-state pins on natural-tick
  transfers.
- **Reshape over force-fit**: Gap A's source-side firing requires
  paired railguns — explicitly OUT OF SCOPE here; the receiver-side
  contract is the player-visible endpoint and pins the same destination
  guarantees a working firing path would deliver.
- **Phase-0 reshape for Gap D**: the audit's "planet-id chip →
  SatelliteData" framing was speculative. Production reality is
  asteroid-chip research via DataBus aggregation. The reshaped contract
  passes the SOP litmus blank cleanly: "this test fails if production
  breaks the contract that asteroid-research increments chip data
  fields under powered + flagged conditions."
- **No production logic changes** (same rule as TASK-01 §15).

## Probe surface additions

| Probe | Purpose | Lines (~) |
|---|---|---|
| `rocket storage-item-fill` | mirror of `storage-fluid-fill` for items | ~50 |
| `infra unloader-debug` | diagnostic dump of unloader state | ~75 |
| `infra railgun-receive-cargo` | call onReceiveCargo + scan output ports | ~70 |
| `infra astrobody-set-research` | reflection-set researching flags | ~40 |
| `infra astrobody-load-chip` | place chip with UUID + maxData in slot 0 | ~30 |
| `infra astrobody-chip-data` | read chip's data values | ~30 |
| `infra databus-set-data` | direct `TileDataBus.setData` call | ~35 |

Total ~330 LOC added to TestProbeCommand.

## Bugs surfaced

None. Phase-0 reads revealed two contract-reshapes (D and A) but no
production-side bugs.

## Dependencies

**Requires**: TASK-04 (Railgun/PlanetAnalyser assembly fixtures),
TASK-09 (rocket loader smoke baseline), TASK-34 (storage-fluid-fill
pattern).

**Does NOT block** future batches in the TASK-40-N series.

## Estimated effort vs actual

Audit estimate: E=3h + A=3h + D=3h = **9h**. Actual: ~3h authoring + 1
debug cycle = **~4h** (Phase 0 reuse reduced scope on E).

## Completion Checklist

- [x] 3 new server tests authored.
- [x] 7 new probe verbs added to TestProbeCommand.
- [x] testServer PASSED locally for all 3 test classes.
- [x] tasks/README.md Done table updated.
- [x] Counter regenerated.
- [x] Parent audit doc: Gaps E/A/D marked ✅ Shipped 2026-05-29.

## Notes for future agents

- The IDE root mismatch in `.agent/sops/development/mcp-intellij-usage.md`
  needs updating — in this session IntelliJ opened the project at
  `/workspace/AdvancedRocketry` directly, not at `/workspace`. MCP
  `path` arguments resolve from `/workspace/AdvancedRocketry`. The SOP's
  blanket statement about `/workspace` IDE root is project-config
  dependent.
- The `unloader-debug` probe is kept after green because the
  rocket-storage state-debug pattern (rocket linkage / tile list / slot
  contents / world.isRemote) is broadly useful for future
  loader/unloader/infrastructure tests.
- TileGuidanceComputer is in the rocket's inventory tile list but
  production explicitly skips it everywhere. New probes that touch
  rocket storage MUST mirror the same filter.
