# TASK-36: Deeper contracts — TerraformingTerminal biome-mutation + ServiceStation repair cycle

## Ticket

- Source: 2026-05-25 Tier 1+2 audits. Both deferred at the time
  because of fixture work; carried forward into 2026-05-26 audit
  out-of-scope.
- Status: **36b ✅ Completed 2026-05-26 / 36a 🟡 Phase 0 audit complete, awaiting implementation.**
- Created: 2026-05-26.

## Outcome (36b)

Shipped `ServiceStationBrokenPartScanContractTest` (3 server tests) +
2 new probe verbs (`/artest infra inject-broken-part` and
`/artest infra service-relink`). Pinned contracts:
- Inject + link → scan finds it (1 worn part surfaces).
- Multi-part scan (2 worn parts surface, not just first).
- Post-link injection requires explicit re-scan
  (`service-relink`) — `updateRepairList` is link-time, not
  tick-time.

Implementation insight that cut probe cost from ~30-50 LOC to ~15
LOC of actual logic: `TileBrokenPart` instances pre-exist in
`rocket.storage.tileEntities` because every IBrokenPartBlock
(BlockRocketMotor / BlockAdvancedRocketMotor / etc.) returns a
TileBrokenPart from `createTileEntity`, copied into StorageChunk
by `cutWorldBB` on assemble. Probe just calls `setStage(stage)` on
the first stage==0 entry — no construction, no world-wiring.

**36b extension shipped 2026-05-26**: 2 additional server tests
in `ServiceStationAssemblerScanTest` pinning the assembler-discovery
half of the cycle:
- `scanForAssemblers` picks up a nearby `TilePrecisionAssembler`
  block (5-block radius, instanceof check — formed multiblock not
  required).
- No-assembler-no-progress: with no nearby assembler, broken parts
  stay in `partsToRepair` across tick windows (giveWorkToAssemblers
  loop is safe under empty list — no NPE, no silent dequeue).

New probe `/artest infra service-scan-assemblers` bypasses the
`canPerformFunction` `worldTime % 20 == 0` gate that
`tile force-tick` can't satisfy (force-tick doesn't advance world
time).

**36b deep shipped 2026-05-27**: full repair cycle with formed
PrecisionAssembler multiblock pinned in
`ServiceStationFullRepairCycleTest` (1 server test).

The earlier deferral was based on a misread of
`MachineRecipeEndToEndKit`'s "Out of scope: wildcard-based
machines" caveat — that note refers to the kit's RECIPE end-to-end
helper, not the underlying fixture probe. TASK-26 had already
landed the wildcard-overlay support in
`/artest fixture machine precision-assembler` via
`lookupWildcardMachineOverrides` (overlays I/O/P hatches onto the
three front-row wildcards at structure[2][0][1..3]).

Test path:
1. `fixture machine precision-assembler` → builds + forms the
   multiblock, returns I/O/P hatch positions.
2. Rocket fixture + assemble in a separate lane.
3. `infra inject-broken-part rocketId 5` → mark one motor worn.
4. Place service station within 5 blocks of the assembler
   controller, link rocket, apply redstone.
5. `infra service-perform-function` #1 → `!was_powered` rising
   edge triggers `scanForAssemblers`, then
   `giveWorkToAssemblers` → `consumePartToRepair` moves part to
   `partsProcessing[0]`.
6. `hatch fill <outputPos> 0 advancedrocketry:advrocketmotor 1`
   → injects a "rocket"-named item into the assembler output.
   `InventoryUtil.hasItemInInventory` does case-insensitive
   substring match on `getUnlocalizedName`, so
   "tile.advrocketmotor" satisfies the "rocket" filter.
7. `infra service-perform-function` #2 → observes the output
   item, runs `processAssemblerResult` which clears
   `partsProcessing[0]`, calls `te.setStage(0)`, and re-adds the
   tile to the rocket's StorageChunk at its original blockState.
8. Post-cycle pin: `inject-broken-part rocketId 7` succeeds —
   proving rocket storage still has a stage-0 TileBrokenPart
   available (the repaired motor would have been lost if Phase 2
   misfired).

New probes:
- `/artest infra service-perform-function <dim> <x> <y> <z>` —
  calls `TileRocketServiceStation.performFunction()` directly,
  bypassing the `canPerformFunction` `worldTime % 20 == 0` gate
  (force-tick can't advance world time). performFunction itself
  still requires `getEquivalentPower()` and a `linkedRocket` —
  those preconditions remain in production hands.
- `service-state` extended with `partsProcessingCount` (counts
  non-null entries in the `partsProcessing` reflection array).

## Context

Two tiles with shallow coverage today, grouped because both need
new fixture/probe surfaces and they share the "depth tile contracts
behind specific item requirements" character.

### 36a. TerraformingTerminal biome-mutation

`TileTerraformingTerminal` plus its companion satellite
(BiomeChanger) implements a player loop:

1. Player programs a BiomeChanger chip with target biome.
2. Player feeds the chip + satellite to a TerraformingTerminal.
3. Terminal queues a biome change at the satellite's coords.

Current coverage:
- `SatelliteTypeBehaviourTest` covers BiomeChanger satellite
  `tickEntity()` with a pre-set queue.
- `ItemBiomeChangerSatelliteActionE2ETest` covers the chip's
  right-click action surface.

What's NOT pinned: the **terminal-to-satellite** wiring — feeding
the terminal a programmed chip results in the satellite getting
the right queue. That's the player-visible mid-game gate.

### 36b. ServiceStation repair cycle

`TileServiceStation` accepts a rocket and repairs damaged parts
(broken from prior re-entry / explosion damage). Current coverage:
- TASK-18 audit pinned placement smoke.
- No test pins the actual repair (broken part in → repaired part
  out).

What's NOT pinned: the full "broken rocket part in, intact part
out after N ticks" loop.

## Blockers

### 36a blocker

Needs a probe to construct a programmed BiomeChanger chip with
known target coords. Concretely:
- `item make-biomechanger-chip <stack-slot> <biome-id> <x> <y> <z>`
  — sets the BiomeChanger chip's NBT to a target biome at known
  coordinates.

Without this, the test can't differentiate "chip wired correctly"
from "chip wasn't programmed".

### 36b blocker

Needs a probe to inject a `TileBrokenPart` into the service
station's input. Concretely:
- `service-station inject-broken-part <dim> <x> <y> <z> <partType>`
  — places a broken part into the station's input slot.

Without this, the test can't set up the "rocket arrives with
broken part" precondition. Production's broken-part injection
happens during launch failures, which is heavy to drive in a
test.

## Implementation plan

### 36a (~3 h)

1. Add `item make-biomechanger-chip` probe (~1 h).
2. `TerraformingTerminalBiomeMutationTest` (~2 h) — feed the
   terminal a programmed chip + a BiomeChanger satellite, force-
   tick, assert the satellite's `viable_positions` (or equivalent
   queue field) contains the target coords.

### 36b (~3 h)

1. Add `service-station inject-broken-part` probe (~1 h).
2. `ServiceStationRepairCycleTest` (~2 h) — inject broken part,
   force-tick station, assert input slot empty + adjacent output
   has intact part.

## Acceptance

- [ ] 1-2 tests per subscope (2-4 total).
- [ ] Probe verbs documented.
- [ ] Pyramid counter regenerated.

## Out of scope

- Per-biome enumeration (test 1-2 representative biomes, not all).
- Concurrent terminal usage (multi-player).
- Specific repair part types (test 1 representative type).

## Dependencies

- 36a and 36b are independent of each other but share this TASK
  for index efficiency. Either can ship first.
- Does NOT block any other task.

## Estimated effort

- 36a: 3 h
- 36b: 3 h
- **Total**: ~6 h

## Risk

Low-medium. Both blockers are probe additions — once probes land,
the tests are mechanical.

## Phase 0 audit findings (2026-05-26)

### 36a — BiomeChanger chip probe

**Verdict: REUSE existing surface — no dedicated probe needed.**

- `ItemBiomeChanger` extends `ItemSatelliteIdentificationChip`
  (`ItemBiomeChanger.java:35`).
- NBT contract pinned at `TestProbeCommand.java:9096-9160`
  (`handleSatellite "item-action"` subcommand). NBT keys:
  `satelliteName`, `dimId`, `satelliteId` (lines 9129-9131);
  position list via `posList` int-array (`:9139, :9149`).
- Existing probe `/artest satellite-builder build <dim> <typeId>`
  already manufactures + registers `SatelliteBiomeChanger` via
  reflection (`:9112-9123`).

**Cleanest approach:** extend dispatcher to accept
`typeId="biomeChanger"` and route through
`ItemBiomeChanger.setSatellite()` NBT packing already in place.

### 36b — Service-station inject-broken-part probe

**Verdict: FEASIBLE. ~30-50 LOC. THIS IS WHAT WE'RE SHIPPING TODAY.**

- `EntityRocket.storage.getBrokenBlocks() → List<TileBrokenPart>`
  (`StorageChunk.java:907`).
- `TileBrokenPart` (`tile/TileBrokenPart.java:10-99`) — TileEntity
  with NBT keys `stage`, `maxStage`, `transitionProb`.
- `TileRocketServiceStation.partsToRepair` is a
  `LinkedList<TileBrokenPart>` (`:69`). Repair loop:
  `linkRocket → updateRepairList()` scans
  `rocket.storage.getTileEntityList()` for `TileBrokenPart` with
  `stage > 0` (`:117-139`).
- Existing probe `infra service-state` already reads
  `partsToRepairCount` via reflection at `TestProbeCommand.java:1237-1241`
  — mirror pattern for write side.

**Probe shape:** `/artest infra inject-broken-part <dim> <x> <y> <z> <stage> <maxStage>`
— takes service-station pos, locates linkedRocket, picks a block in
the storage chunk whose Block implements `IBrokenPartBlock`, swaps
the existing TileEntity with a fresh `TileBrokenPart(stage, maxStage, 0.5f)`,
then calls `updateRepairList()`. Falls back to a graceful error if
the linked rocket has no IBrokenPartBlock blocks.

### Recommended batch order

36b cheapest first (this session); 33 + 36a together (shared
satellite-builder probe extension); 35 last (most flake risk).
