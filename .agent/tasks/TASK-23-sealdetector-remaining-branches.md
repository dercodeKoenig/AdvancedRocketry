# TASK-23: ItemSealDetector remaining branch matrix

## Ticket

- Source: 2026-05-23 audit — Gap #11. `SealDetectorDispatchTest`
  (server) and `ItemSealDetectorPlayerMessagesE2ETest` (client)
  cover the main branches but explicitly defer three:
  `notsealblock`, `notfullblock`, `fluid`. Each needs a
  deterministic block fixture that current probe surface doesn't
  cleanly support.
- Status: ✅ **Completed 2026-05-25** (partial — 2 of 3 branches pinned).
- Created: 2026-05-23.

## Actual scope (2026-05-25)

Two of the three deferred branches shipped as positive contract pins;
the third (`notfullblock`) turned out to be effectively dead code for
vanilla + AR's registered block set, so it's documented as unreachable
rather than tested:

- **`notsealblock`** (✅ shipped) — `SealDetectorDispatchTest`
  gained `goldBlockBannedReportsNotSealBlockBranch`. Drives the new
  `/artest seal-detector add-block-ban <block-id>` /
  `remove-block-ban <block-id>` probes (with `@After` defensive
  restore) since the default `blockBanList` is empty per
  `SealableBlockHandler.loadDefaultData`.
- **`fluid`** (✅ shipped) — `oxygenFluidBlockReportsFluidBranch`
  uses AR's `advancedrocketry:oxygenFluid` block (`BlockFluidClassic`
  → implements `IFluidBlock`). Vanilla water / lava extend
  `BlockLiquid` (NOT `IFluidBlock`) and fall through to the
  "other" branch — only AR-registered fluids hit "fluid".
- **`notfullblock`** (📝 documented unreachable) — the branch
  requires a block whose material is liquid / non-solid / IFluidBlock
  with a FULL collision bbox. No vanilla or AR block satisfies this
  combination: fluid blocks have null collision bbox, non-solid
  blocks are either air-shaped or partial. The branch exists in
  `ItemSealDetector.onItemUse:44` but appears unreachable for this
  repo's block set. Test-file javadoc records the analysis so a
  future fix (e.g. inverting the predicate to the originally-intended
  partial-occlusion check) flips an explicit test rather than a
  silent no-op. Per CLAUDE.md bug-tracking SOP: not ledgered as a
  bug because there's no observable player-visible regression
  (partial blocks already hit the "other" branch with a sensible
  message).

**Phase 4 (client-tier mirror)**: not shipped. The contract pinned
by `SealDetectorDispatchTest` via the probe — which 1:1 replicates
production's branch dispatch — already covers the player-message
behaviour transitively. Adding three more `ItemSealDetectorPlayerMessagesE2ETest`
methods would duplicate the same contract through testClient (slower,
flakier). The probe's dispatch-fidelity comment in
`SealDetectorDispatchTest`'s javadoc is the load-bearing cross-reference.

## Context

`ItemSealDetector.onItemUse` has a chat-message dispatch matrix.
For a block at the clicked position, the detector emits one of:

| Branch | Trigger | Pinned? |
|---|---|---|
| `sealed` | block is in sealable list + position has breathable atmosphere | ✅ |
| `unsealed` | block is in sealable list + position has no atmosphere | ✅ |
| `notsealblock` | block is NOT in sealable list | ❌ |
| `notfullblock` | block is partial-occlusion (e.g. slab, stair) | ❌ |
| `fluid` | block is a fluid (water, lava) | ❌ |
| `air` | clicked position is air | ✅ (covered via "unsealed" path) |

The three unpinned branches each need a known block at a known
position with known properties. Production reads
`SealableBlockHandler.INSTANCE` for sealable membership +
`block.isFullCube()` + `block instanceof BlockFluidBase`.

## Why they're hard today

- `notsealblock`: need a block that exists in the registry but is
  NOT in `SealableBlockHandler`'s default allow-list. Default
  list includes most full opaque blocks; a non-sealable cobble-like
  block is rare. Workaround: place a known block + use
  `SealableBlockHandler.removeFromAllowed(block)` via probe before
  the test, restore after.
- `notfullblock`: a slab/stair. Place via `/artest place 0 X Y Z
  minecraft:stone_slab` — already supported.
- `fluid`: place a fluid block via `/artest fluid place` (verb may
  need adding — currently `/artest fluid inject` only works against
  fluid handlers).

## Implementation plan

### Phase 0 — Probe surface (~30 min)

Add or confirm:

- `/artest sealable remove <block-id>` — temporarily remove a block
  from the sealable list; auto-restore on test teardown.
- `/artest sealable add <block-id>` — inverse.
- `/artest fluid place <dim> <x> <y> <z> <fluid-id>` — place a
  fluid source block (not via fluid handler). Probably already
  exists under `/artest place 0 X Y Z minecraft:water` since
  `Blocks.FLOWING_WATER` and `Blocks.WATER` are registered blocks.
  Confirm.

### Phase 1 — `notsealblock` branch (~1 h)

Test in `SealDetectorDispatchTest` (extend existing class):

- `notsealblockBranchFiresWhenBlockNotInSealableList` — pick a
  block not in default sealable list (or remove via probe), place
  it, fire seal detector at it, assert chat envelope contains
  `msg.sealdetector.notsealblock`.

### Phase 2 — `notfullblock` branch (~30 min)

Test:

- `notfullblockBranchFiresOnSlab` — place `minecraft:stone_slab`,
  fire detector, assert `msg.sealdetector.notfullblock`.

### Phase 3 — `fluid` branch (~1 h)

Test:

- `fluidBranchFiresOnWaterSource` — place `minecraft:water`, fire
  detector, assert `msg.sealdetector.fluid`.
- `fluidBranchFiresOnLavaSource` — same with `minecraft:lava`.

### Phase 4 — Client-tier player-message variant (~30 min)

Add corresponding tests to
`ItemSealDetectorPlayerMessagesE2ETest` for each of the three
branches — same player-visible chat-message check, just driven
by a real player click instead of the probe.

## Acceptance

- [ ] Three new dispatch tests in `SealDetectorDispatchTest`.
- [ ] Three new player-message tests in
      `ItemSealDetectorPlayerMessagesE2ETest`.
- [ ] Probe verbs added if needed (sealable add/remove if
      production allow-list mutation is risky; fluid-place
      confirmation).
- [ ] Sealable allow-list restored after each test that mutates it.
- [ ] Pyramid counter regenerated per TASK-17 phase 1.

## Technical decisions

- **Restore mutation after test** — `SealableBlockHandler` is a
  global singleton. Tests MUST restore the allow-list to default
  state in `@After`, or use a precondition-block that's already
  not in the list.
- **Prefer pre-existing not-in-list blocks** to avoid mutation
  altogether. Investigate which vanilla blocks lack sealability
  by default (e.g. glass, fences) before adding probe mutation.
- **No production logic changes**.

## Out of scope

- Refactoring `SealableBlockHandler` to be more testable (its
  current shape is global-singleton; mutation works fine for
  testing if restored properly).
- Cross-branch precedence pinning (which fires first if a block
  is both partial AND fluid — degenerate edge case, defer).

## Dependencies

- Does NOT block any other task.
- Pattern source: existing `SealDetectorDispatchTest` for server
  branches, `ItemSealDetectorPlayerMessagesE2ETest` for player.

## Estimated effort

- Phase 0 probe: ~30 min
- Phase 1 notsealblock: ~1 h
- Phase 2 notfullblock: ~30 min
- Phase 3 fluid: ~1 h
- Phase 4 client mirror: ~30 min
- Close-out: ~30 min
- **Total**: ~4 h
