# TASK-12: Production bug-fix pass — flip the `_documentsKnownBug` ledger

## Ticket

- Source: bug ledger accumulated across TASK-02 / TASK-03 / TASK-05 /
  TASK-10b / TASK-11. 7 entries logged in
  [`.agent/tasks/README.md`](README.md) "Notes on `_documentsKnownBug`",
  6 of them pinned by `_documentsKnownBug` tests that assert the
  current (wrong) behaviour as expected. Plus 1 ledger-only (#6) and
  1 surplus pin (`planetChipSetDimensionIdWithInvalidPlanetDoesNotAttachNbt`)
  not yet ledgered.
- Status: ✅ Completed 2026-05-23.
- Created: 2026-05-23.

## Context

Across the test-coverage build-up the agent surfaced real production
bugs without scope to fix them — the per-task "no production logic
changes" rule was the right discipline for keeping each ticket
focused, but it left an accumulated debt: every `_documentsKnownBug`
test is a test that **fails when production gets correct**. So each
ledgered bug is a "fix me and update the test" pair.

This ticket is the dedicated sweep. Outside the scope of any
test-coverage TASK; explicitly OK to change production logic here.

## Scope — bugs to fix

Each row: bug → file:line → fix shape → test to flip.

| # | Bug | File:Line | Fix shape | Pin to flip |
|---|---|---|---|---|
| 1 | `HandlerCableNetwork` assertion polarity inverted | `network/HandlerCableNetwork.java:67` | flip the boolean | `PipeNetworkHandlerDeepTest.mergeNetworksAssertionPolarityIsInverted_documentsKnownBug` |
| 2 | `CableNetwork.merge` — addAll-before-dedupe causes duplicate node retention | `network/CableNetwork.java` (merge path) | dedupe first OR switch to a `Set`-based merge | `PipeNetworkHandlerDeepTest.cableNetworkMergeReturnsFalseWheneverBHasAnySinks_documentsKnownBug` |
| 3 | `EnergyNetwork.merge` — battery-migration cascade from (2) | `network/EnergyNetwork.java` | cascades from #2 fix; verify post-fix | `PipeNetworkHandlerDeepTest.energyNetworkMergeNeverMigratesBatteryToday_documentsKnownBug` |
| 4 | `SpaceStationObject` writes NBT key `"autoLand"`, reads `"occupied"` | `stations/SpaceStationObject.java:801` (and the read site) | use the same key on both sides; pick one and migrate the other with a one-version legacy-NBT read | `SpaceStationPadPersistenceTest.autoLandFlagWithoutDockDoesNotSurviveRestart_documentsKnownBug` |
| 5 | `ItemSpaceElevatorChip.clearPositions` calls `removeTag("positions")` but the data lives under `"list"` | `item/ItemSpaceElevatorChip.java:42` | replace the removeTag key with `"list"` (or use `NBTStorableListList`'s clear API directly) | `ItemDataCarrierNBTRoundTripTest.elevatorChipSetEmptyAfterNonEmptyDoesNotClearList_documentsKnownBug` |
| 6 | `ItemSatelliteIdentificationChip.setSatellite(stack, SatelliteBase)` else-branch builds a fresh NBT but never calls `stack.setTagCompound(nbt)` | `item/ItemSatelliteIdentificationChip.java:54-64` | add the missing `stack.setTagCompound(nbt);` (mirrors the sibling overload at line 87) | **none yet** — write a new pin for the chip-programming path then flip it |
| 7 | `commandReloadRecipes` crashes post-init: Forge freezes the recipe registry | `command/WorldCommand.java:256-258` → `RecipeHandler.createAutoGennedRecipes:122` | options: (a) call `GameData.unfreezeData()` around the reload (Forge-internal API, fragile), (b) move XML reload to a `FMLServerStartedEvent` handler that runs while the registry is still mutable, (c) document the command as deprecated and remove it. Recommend (b) if XML hot-reload is still wanted, (c) if not | `WorldCommandStarMiscContractTest.reloadRecipesEmitsErrorEnvelopeDueToFrozenRegistry_documentsKnownBug` |
| 8 | `ItemPlanetIdentificationChip.setDimensionId(stack, INVALID_PLANET)` builds a fresh NBT but never calls `stack.setTagCompound(nbt)` (same shape as #6, different class) | `item/ItemPlanetIdentificationChip.java:73-77` | add the missing `stack.setTagCompound(nbt);` | `ChipNBTRoundTripTest.planetChipSetDimensionIdWithInvalidPlanetDoesNotAttachNbt_documentsKnownBug` |

Bug #8 is currently pinned but not in the ledger — add a ledger row
in the close-out commit so the bookkeeping is consistent.

## Implementation Plan

### Phase 1 — Trivial NBT-attach pair (#6, #8) (~30 min)

Two one-line fixes. Each adds a missing `stack.setTagCompound(nbt);`
in the else-branch of a chip's `setX(stack, ...)`. Both flips are
unit-tier so iteration is fast.

For #6, also write a new `_documentsKnownBug` test BEFORE the fix
(to verify the assertion fires as expected), then flip it to a
positive assertion in the same commit as the fix. Without writing
the pre-test first, we have nothing to flip.

### Phase 2 — Wrong-key bugs (#4, #5) (~1 h)

#5 is a one-character key change. #4 needs a legacy-NBT migration:
existing save files have `"autoLand"` written, the fix uses
`"occupied"` (or vice versa). Migration shape:

```java
if (nbt.hasKey("autoLand") && !nbt.hasKey("occupied"))
    nbt.setBoolean("occupied", nbt.getBoolean("autoLand"));
nbt.removeTag("autoLand");
```

Verify by re-running `SpaceStationPadPersistenceTest` end-to-end.

### Phase 3 — Cable/energy network merge (#1, #2, #3) (~2 h)

#1 is a polarity flip. #2 is a dedup ordering change. #3 cascades
from #2 (battery migration depends on correct merge result).

These are coupled — fix #1 first, then #2, then re-run the suite
to confirm #3 also flips. If #3 still pins, investigate as a
distinct bug.

### Phase 4 — Recipe reload (#7) (~2-3 h)

Production decision required: do we want XML hot-reload at runtime?

- If yes: move the reload path to an event handler that runs during
  `FMLServerStartedEvent` (registry still mutable). The `/ar
  reloadRecipes` console command becomes a thin trigger that
  re-fires that handler via a custom event, OR is deprecated in
  favour of `/reload` (vanilla server reload) if Forge's vanilla
  path is enough.
- If no: remove `commandReloadRecipes` from `WorldCommand`,
  remove the subcommand from the switch, drop the
  `_documentsKnownBug` test.

Decide in the ticket discussion before implementing.

### Phase 5 — Close-out (~30 min)

- Bug ledger updated: entry #6 flipped from "ledger only" to
  "pinned + fixed"; entry #8 added retroactively then flipped.
- README counter unchanged (no new tests, just pin flips —
  except #6 which adds one new positive test).
- EOD marker.

## Technical decisions

- **Tests flip in the same commit as the production fix.** Avoids
  a "broken state" window where production is fixed but the test
  still asserts the wrong behaviour and the build is red.
- **Each phase = its own commit.** Easier to bisect if a fix
  introduces a regression elsewhere. Single ticket, multiple commits.
- **No new abstractions for fixes.** The bug-fix sweep is the wrong
  time to refactor — fixes are 1-3 line changes; bigger reshapes
  go to a separate ticket.
- **Full pyramid PASS gates close-out.** Production logic changes
  can ripple — testServer + testClient must both be green at the
  end. Especially for #4 (NBT migration), where save-format changes
  can break unrelated persistence tests.

## Risks

1. **#4 save-format migration could ghost-break old saves.** The
   migration must be backwards-compatible for one release cycle —
   read both keys, write the canonical one. Verify by spinning
   up a server with an old workdir if available, or by writing
   a `legacy-nbt` unit test that pre-seeds the old key.
2. **#7 unfreeze hack is brittle.** Forge internals can change
   between versions; if we go the unfreeze route, gate it on
   `Forge.version == 14.23.5.2860` (the project's pinned version)
   so a future Forge bump fails loud.
3. **#3 may not cascade cleanly.** If the battery-migration test
   still pins after #2 is fixed, treat it as a separate bug and
   investigate; do NOT widen scope of #2's fix to absorb it.

## Dependencies

- **Requires**: the existing `_documentsKnownBug` tests as
  regression nets — fixes flip them, not delete them.
- **Does NOT block**: anything in the current backlog (backlog
  is empty post-TASK-11).

## Estimated effort

~6-7 hours across 4-5 sessions:
- Phase 1: 30 min
- Phase 2: 1 h
- Phase 3: 2 h
- Phase 4: 2-3 h (incl. design discussion)
- Phase 5: 30 min

## Completion Checklist

- [x] Phase 1: #6 + #8 fixed (one-line `setTagCompound(nbt)` adds);
      pin #8 flipped to positive; pin #6 added (new positive test
      `satelliteChipSetSatelliteAttachesNbtToFreshStack`).
- [x] Phase 2: #4 (autoLand/occupied) read-side key matches write +
      legacy-NBT default-true fallback; #5 (elevator chip)
      removeTag key changed `"positions"` → `"list"`. Both pins
      flipped.
- [x] Phase 3: #1 assertion polarity flipped; #2 CableNetwork.merge
      restored to per-entry dedupe (no premature addAll); #3
      cascaded automatically; updated existing
      `mergeRejectsExactPositionPlusDirectionOverlap` to reflect
      new merge contract (dedupe vs reject).
- [x] Phase 4: #7 fixed by (a) dropping the runtime
      `createAutoGennedRecipes` call (init-time call at
      `AdvancedRocketry.java:1044` is the sole site) AND (b) null-
      guard on `jeiHelpers` in `ARPlugin.reload` for dedicated-server
      mode. Pin flipped to
      `reloadRecipesEmitsSuccessConfirmationMessage`.
- [x] Bug ledger updated — all 8 entries marked fixed; entry #8
      retroactively added to the historical list.
- [x] Full pyramid PASS:
      - `testUnit + testIntegration + testServer` BUILD SUCCESSFUL
        in 16m 17s on retry (first run had 2 flaky failures —
        `beaconMultiblockValidatesWhenFixtureIsBuilt` and
        `cuttingMachineRunsFirstRegisteredRecipe` — that both
        passed in isolation AND on the rerun; pre-existing
        parallel-forks flakiness, not regression from these
        fixes).
      - `testClient` BUILD SUCCESSFUL in 29m 31s under
        `DISPLAY=:77`.
- [x] EOD marker
      `.agent/.context-markers/2026-05-23_task12-bugs-drained.md`.

**Outcome**: bug ledger fully drained. The `_documentsKnownBug`
suffix is no longer in use anywhere in the test suite — every former
"document the bug" pin now asserts the corrected contract.
