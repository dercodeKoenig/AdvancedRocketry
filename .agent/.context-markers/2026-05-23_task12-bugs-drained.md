# Context marker — 2026-05-23 (TASK-12 closed — bug ledger drained)

**Slug**: task12-bugs-drained
**Branch**: `feature/tests` (3 commits pending push: stale-header
sync + TASK-12 plan + this fix sweep).
**Session focus**: TASK-12 — fix all 8 ledgered production bugs.

## Session arc

Continuation of the same day's work (stale-header sync + TASK-12
plan written earlier). User said "TASK-12 начинай сейчас" so I
went straight into implementation.

4 phases planned, all closed in one session, ~2 hours total work.

## Bugs fixed (8 total)

### Phase 1 — NBT-attach pair

- **#6** `ItemSatelliteIdentificationChip.setSatellite(SatelliteBase)`
  — added missing `stack.setTagCompound(nbt);`. Was ledger-only;
  new positive pin written and immediately satisfied.
- **#8** `ItemPlanetIdentificationChip.setDimensionId(INVALID_PLANET)`
  — same shape, added the missing `setTagCompound`. Existing pin
  flipped from `_documentsKnownBug` to positive.

### Phase 2 — Wrong-key bugs

- **#4** `SpaceStationObject:801` — read side used `"occupied"`
  while write used `"autoLand"`. Switched read to `"autoLand"` with
  default-true legacy-NBT fallback. Persistence pin flipped.
- **#5** `ItemSpaceElevatorChip:42` — `removeTag("positions")` →
  `removeTag("list")` to match the actual key used by
  `NBTStorableListList`. Unit pin flipped.

### Phase 3 — Cable/energy network merge

- **#1** `HandlerCableNetwork:67` — flipped the assertion polarity
  from "either side null" to "both sides non-null". Pin flipped to
  a positive merge-survivor assertion.
- **#2** `CableNetwork.merge` — restored to per-entry dedupe shape
  (the commented-out `canMerge` blocks confirmed original intent).
  Removed the premature `sinks.addAll` that caused self-collision
  and forced false returns.
- **#3** `EnergyNetwork.merge` battery-migration cascade — fixed
  automatically once parent #2 returned true for valid merges. No
  separate code change needed.

The fix cascaded into an existing positive test
(`mergeRejectsExactPositionPlusDirectionOverlap`) whose semantics
now differ: overlapping entries are deduped (merge succeeds with
no duplicate) rather than rejected at the network level. Updated
the test to reflect the new contract.

### Phase 4 — Recipe reload

- **#7** `commandReloadRecipes` — compound fix:
  (a) removed `createAutoGennedRecipes` from the runtime reload
      path — it calls `ForgeRegistry.register_impl` which crashes
      after Forge freezes the recipe registry. The init-time call
      at `AdvancedRocketry.java:1044` is sufficient; auto-genned
      recipes are static once `modProducts` is set at init.
  (b) added null-guard on `jeiHelpers` in `ARPlugin.reload`. The
      field is set in `registerCategories` (client-only). On a
      dedicated server it's null and the unguarded reload NPE'd,
      triggering the outer catch and the user-visible "Serious
      error" envelope.

## Test status

Full pyramid run post-fix:
- `testUnit + testIntegration + testServer`: BUILD SUCCESSFUL in
  16m 17s **on retry**. First run had 2 flaky failures
  (`beaconMultiblockValidatesWhenFixtureIsBuilt` +
  `cuttingMachineRunsFirstRegisteredRecipe`) that:
    (a) both passed in isolation
    (b) both passed on the immediate rerun
  Diagnosed as pre-existing parallel-forks flakiness, not a
  regression from the production fixes. Note this in any future
  pyramid-failure debugging: these two tests are first suspects.
- `testClient`: BUILD SUCCESSFUL in 29m 31s under `DISPLAY=:77`.

## Discoveries (worth carrying forward)

### Test-pollution flakiness when sharing parallel forks

Two tests (`BeaconMultiblockTest` shared-harness +
`MachineRecipeIntegrationTest` per-method) failed in one pyramid
run but passed in isolation AND on the immediate rerun. Likely
gradle parallel-forks resource contention (forkEvery(1) +
`-Pforks=N`). Not investigated further this session — flag for a
future test-stability ticket if the pattern recurs.

### `commandReloadRecipes` had a hidden secondary bug

The JEI integration cascade (`CompatibilityMgr` → `ARPlugin.reload`
→ `jeiHelpers.reload()`) NPE'd on dedicated server because
`jeiHelpers` is set in `registerCategories` which only runs on the
client. The catch envelope masked it. Fix #7 had to cover both the
ForgeRegistry-frozen path AND this NPE path; pinning only the
ForgeRegistry path would have left the command silently broken on
dedicated servers.

## Files changed (production)

- `src/main/java/.../cable/CableNetwork.java` — per-entry dedupe
- `src/main/java/.../cable/HandlerCableNetwork.java` — assertion polarity
- `src/main/java/.../command/WorldCommand.java` — drop autoGen call in reload
- `src/main/java/.../integration/jei/ARPlugin.java` — jeiHelpers null-guard
- `src/main/java/.../item/ItemPlanetIdentificationChip.java` — setTagCompound
- `src/main/java/.../item/ItemSatelliteIdentificationChip.java` — setTagCompound
- `src/main/java/.../item/ItemSpaceElevatorChip.java` — removeTag key
- `src/main/java/.../stations/SpaceStationObject.java` — read autoLand key

## Files changed (test)

- `src/test/.../unit/ChipNBTRoundTripTest.java` — flipped #8 + added #6
- `src/test/.../unit/ItemDataCarrierNBTRoundTripTest.java` — flipped #5
- `src/test/.../unit/PipeNetworkHandlerDeepTest.java` — flipped #1/#2/#3
  + updated `mergeRejectsExactPositionPlusDirectionOverlap` to new
  dedupe semantics
- `src/test/.../server/SpaceStationPadPersistenceTest.java` — flipped #4
- `src/test/.../server/WorldCommandStarMiscContractTest.java` — flipped #7
- `.agent/tasks/README.md` — ledger rewritten as "all 8 fixed";
  TASK-12 Done row added; counter line updated.
- `.agent/tasks/TASK-12-bug-fix-pass.md` — closed-out.

## Open backlog (post-TASK-12)

**P0/P1/P2**: empty.

**Deferred (no task yet)**:
- Phase 9 (companion-mod integration tests)
- Phase 10 (visual regression for MC client)
- Pipe end-to-end (blocked on uncommented registrations)
- Test-stability ticket for `BeaconMultiblockTest` +
  `MachineRecipeIntegrationTest` flakiness if it recurs

Bug ledger: 0 (drained).
