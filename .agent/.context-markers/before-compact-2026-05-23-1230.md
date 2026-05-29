# Context marker — 2026-05-23 1230 (pre-compact)

**Slug**: before-compact-2026-05-23-1230
**Branch (AR)**: `feature/tests` — 4 commits ahead of `5836f113`,
all pushed (origin up-to-date).
**Branch (libVulpes)**: `feature/tests` — new branch, 1 commit
pushed to `StannisMod/libVulpes`.
**Session focus**: All work since the 2026-05-22 TASK-10b/SpaceArmor
session — covers 3 fully-shipped tasks (TASK-06 relink, TASK-11
WorldCommand, TASK-12 bug sweep), 2 doc passes (stale-header sync,
XML hot-reload pin), 1 cross-repo init (libVulpes Navigator).

## Session arc

Day broke down into 5 focused chunks, all done same day on the
same branch with no rollback:

1. **TASK-06 rocket-side relink** (commit `f35e5b6e`) — closed
   the deferred follow-up from prior session. Root cause was
   the no-op `EntityRocket.writeMissionPersistentNBT` leaking
   empty NBT into `EntityStationDeployedRocket` which then
   restored launchLocation=(0,0,0) and spawned the rocket at
   world origin (outside the bbox the rocket-cargo probe
   scanned). Fix: new `rocket-relink-state <dim>` probe verb
   that does class-filtered scan (not bbox-limited). +1 server
   pin.

2. **Stale-header sync + TASK-12 plan** (commit `eae27073`) —
   all TASK-01..08 doc headers updated from `Pending` to actual
   state per README Done table. Wrote TASK-12 plan for the
   bug-fix pass.

3. **TASK-11 /ar WorldCommand coverage** (commit `4b30398e`) —
   shipped between (1) and (2) chronologically. 23 server-tier
   tests across 4 classes: planet set/get/list +
   generate/delete/reset + star + dumpBiomes/reloadRecipes +
   console-sender guards. Found bug #7 (reloadRecipes frozen
   registry) en route — pinned via `_documentsKnownBug`.

4. **TASK-12 bug-fix sweep** (commit `e76f7134`) — drained all
   8 ledgered bugs in one pass. 4 phases: NBT-attach pair (#6,
   #8) → wrong-key bugs (#4, #5) → cable/energy network merge
   (#1, #2, #3) → recipe reload (#7, compound fix with JEI null
   guard). All `_documentsKnownBug` pins flipped to positive
   contract assertions; suffix no longer in use anywhere.

5. **XML hot-reload pin** (commit `5aeb7286`) — closed the gap
   the user spotted: my new pin for bug #7 only checked chat
   envelope. Added
   `reloadRecipesPreservesProgrammaticAndXmlRecipesForCuttingMachine`
   that snapshots TileCuttingMachine recipe count via
   `/artest machine recipes-summary`, runs `/ar reloadRecipes`,
   asserts post-reload count ≥ pre-reload count and > 0. Closes
   the "clear-then-fail-to-re-register" regression class.

6. **libVulpes Navigator init** (libVulpes commit `b9ef59c`) —
   user asked to put the XML-format-knowledge gap into the
   libVulpes backlog. Initialised `.agent/` structure mirroring
   AR's layout (minimal — config + DEVELOPMENT-README + tasks
   index + first task). TASK-01 there asks libVulpes to
   document XML layout/schema + add
   `loadXMLRecipe(Class, String)` inline overload for testability.
   Pushed to new branch `StannisMod/libVulpes:feature/tests`
   using AR's git identity (`StannisMod` /
   `stas.batalenkov@mail.ru`), local-only config.

## Discoveries worth carrying forward

### Bug #7 was compound, not single

Initial fix for `commandReloadRecipes` (drop the
`createAutoGennedRecipes` call to avoid frozen-registry crash)
revealed a second bug downstream: `CompatibilityMgr.reloadRecipes`
→ `ARPlugin.reload` → `jeiHelpers.reload()` NPE'd on dedicated
server because `jeiHelpers` is set in `registerCategories` which
only runs on the client. Pin caught both — one e2e assertion vs
two impl-detail unit pins. Argument for keeping e2e pins on CLI
commands even when underlying logic is split across files.

### parallel-forks flakiness on full testServer

First full `./gradlew testServer` run after TASK-12 had 2
failures (`beaconMultiblockValidatesWhenFixtureIsBuilt`,
`cuttingMachineRunsFirstRegisteredRecipe`) that both passed in
isolation AND on the immediate rerun. Diagnosed as parallel-forks
resource contention. Pre-existing, not caused by my fixes. Flag
these two for a future test-stability ticket if pattern recurs.

### Depth audit verdict on TASK-12 flips

User pushed back on whether `_documentsKnownBug → positive
assertion` flips might silently reduce depth. Did the mental
walk-through for each: 5 of 8 pins became **strictly stronger**
(added post-condition checks); 3 stayed equivalent depth with
opposite polarity. The `mergeRejects... → mergeOf...Dedupes...`
test (not a `_documentsKnownBug`, just a flipped semantic) also
became stronger. Pattern: every flip pairs the polarity swap
with an explicit "what state must be observable" assertion.

### testClient harness needs DISPLAY=:77

Note for future testClient runs: dev environment's default
`DISPLAY=:99` doesn't match the LWJGL init path; runs hang for
~7 minutes on `SocketTimeoutException` in `RealClientHarness
.awaitClientBot` before failing. Repo has Xvfb at `:77` running;
that's the canonical display. Two prior sessions hit this.

## Files changed (all session)

**AR production**:
- `cable/CableNetwork.java`, `cable/HandlerCableNetwork.java`
- `command/WorldCommand.java`
- `command/test/TestProbeCommand.java` (+rocket-relink-state,
  +equip-airsuit, +clear-armor verbs — actually those were
  prior-session and TASK-12 didn't add probes)
- `integration/jei/ARPlugin.java`
- `item/ItemPlanetIdentificationChip.java`
- `item/ItemSatelliteIdentificationChip.java`
- `item/ItemSpaceElevatorChip.java`
- `stations/SpaceStationObject.java`

**AR test**:
- New: 4 WorldCommand test classes + `WorldCommandFixtures`
- New: `MissionInfrastructureLifecycleTest.completionLinksInfrastructureToRespawnedRocket`
- Flipped: PipeNetworkHandlerDeepTest 3 pins,
  ChipNBTRoundTripTest 1 flip + 1 new pin,
  ItemDataCarrierNBTRoundTripTest 1 flip,
  SpaceStationPadPersistenceTest 1 flip,
  WorldCommandStarMiscContractTest 1 flip + 1 new XML pin

**AR docs**:
- Sync'd 8 stale TASK headers
- Added TASK-11, TASK-12 to Done table
- Bug ledger rewritten as "all 8 fixed"
- 3 EOD markers (relink, world-command, bugs-drained)

**libVulpes**:
- Navigator init: `.nav-config.json`, `DEVELOPMENT-README.md`,
  `tasks/README.md`, `tasks/TASK-01-xml-recipe-loader-testable.md`

## Test status at compact time

Full pyramid PASS post-TASK-12 (retry was needed due to flakiness):
- testUnit + testIntegration + testServer: BUILD SUCCESSFUL 16:17
- testClient (DISPLAY=:77): BUILD SUCCESSFUL 29:31

Plus XML hot-reload pin verified in isolation post-commit.

## Open backlog at compact time

**AR P0/P1/P2**: empty.
**AR deferred (no task)**:
- Phase 9 (companion-mod integration)
- Phase 10 (visual regression)
- Pipe e2e (blocked on uncommented registrations)
- Test-stability for the 2 flaky tests above

**libVulpes P1**: TASK-01 (XML loader testable surface).
This is `StannisMod/libVulpes:feature/tests` branch — when
that ships, AR side gets stronger XML pin via inline overload.

## What's safe to resume after compact

- "Start my session" → nav-start picks up THIS marker via .active
- All commits pushed; no uncommitted in-progress work
- Two repos in sync with their respective remotes
- env-drift (.nav-config.json timestamp + .claude/settings*)
  still present but irrelevant — never committed

## Things NOT to redo

- Don't re-run the full pyramid unless you change production
  code — last run was within the hour, environment hasn't
  changed.
- Don't re-flip the `_documentsKnownBug` pins — TASK-12
  already drained the ledger. New bugs found go in a fresh
  ledger row + new pin.
- Don't touch `.claude/settings*` or `.agent/.nav-config.json` —
  they are local env-drift, never committed.

## Stale stuff cleaned this session

- All 8 task-doc Status lines synced (no more "Pending" lies).
- Bug ledger rewritten end-to-end (was "Notes on
  `_documentsKnownBug`", now "historical ledger — all fixed").
- README counter bumped accurately at each commit.
