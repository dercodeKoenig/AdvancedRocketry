# TASK-43 — Mitigate 4 flaky / stable testServer + testClient failures

**Status: 🟥 Open (opened 2026-05-30 from TASK-42 Phase 0 split).**

## Ticket

- Source: spun off from TASK-42 Phase 0 once each of the 5 ledger-#5
  failures was triaged. TASK-42 closed the broken-since-inception
  InventoryBypass via `@Ignore`. The remaining 4 split into two
  distinct shape buckets, both needing real diagnosis work — hence
  this dedicated ticket.
- Status: 🟥 Open.
- Created: 2026-05-30.
- Predecessor: [TASK-42](TASK-42-pre-existing-test-failures-investigation.md).
- Ledger entry: `tasks/README.md` "Current state" #5.

## The 4 tests in scope

### Shape A — parallel-fork contention (3 tests, RECIPE-NO-REGISTRATIONS)

All three fail with the same probe response
(`"error":"no recipes registered","machine":"TileXxx"`) at
`MachineRecipeEndToEndKit.resolveFirstRecipe:196` when running in
the full `testServer` suite. **All three PASS in isolation**
(verified 2026-05-30) via:
```
./gradlew testServer -PuseLocalFramework=true \
  --tests zmaster587.advancedRocketry.test.server.ElectrolyserRecipeEndToEndTest
./gradlew testServer -PuseLocalFramework=true \
  --tests zmaster587.advancedRocketry.test.server.PrecisionAssemblerRecipeEndToEndTest \
  --tests zmaster587.advancedRocketry.test.server.PrecisionLaserEtcherRecipeEndToEndTest
```
4 of 4 isolated test methods passed.

- `ElectrolyserRecipeEndToEndTest.electrolyserRunsFirstRegisteredRecipe`
- `PrecisionAssemblerRecipeEndToEndTest.precisionAssemblerRunsFirstRegisteredRecipe`
- `PrecisionLaserEtcherRecipeEndToEndTest.precisionLaserEtcherRunsFirstRegisteredRecipe`

(The `*FixtureValidates` companion of each test class also passes
in isolation; only the `RunsFirstRegisteredRecipe` companion
fails in suite per the original 4-fail count.)

**Classification per `sops/development/flake-diagnosis.md`**:
real race. Distribution = same N tests every full suite run, none
when isolated. Not a regression — the production code is fine.

**Suspected mechanism**: every harness fork spins a fresh dedicated
server in its own tempDir. `RecipesMachine` is a static singleton
in libVulpes but each fork has its own JVM, so global state isn't
shared across forks. Yet contention manifests when N forks run
concurrently. Candidate root causes:
- File-system race on `run/config/advRocketry/<Machine>.xml`
  defaults — the harness may share a config template dir between
  forks at copy time, and a fork that observes the file half-written
  parses 0 recipes.
- Port-bind / startup-order race that lets the test probe a
  not-fully-init server (recipe registration happens late in
  `FMLPreInitializationEvent` ordering, after a `/artest` probe
  may already be reachable on the dedicated-server console).
- Shared classloader / static map pollution if forks somehow
  ride the same VM (`setForkEvery(1L)` should rule this out — but
  worth verifying with a `Process.toString()`-style probe).

**Mitigation playbook**: TASK-27 / TASK-28 patterns —
probe-driven wait for "recipe registry settled", retry with
exponential backoff at the kit level, or pin a sentinel recipe
(e.g. via `/artest fixture machine ... register-recipe`) at
test setup so the test no longer depends on the default-XML
registration race.

### Shape B — stable fail even in isolation (1 test)

`WorldCommandFetchModeratorTest.moderatorFetchTeleportsTargetToSenderPosition`
fails with `IOException: Client bridge closed unexpectedly`
(`ClientBot.execute:210`) both in the full `testClient` suite
AND when run in isolation (verified 2026-05-30, single-test
invocation took 3m 10s, FAILED). 

**Classification per `sops/development/flake-diagnosis.md`**:
real test-design or production bug, NOT a parallel-fork contention.
Stable shape.

**What's pinned**: the multi-client `/ar fetch <target>` flow with
a moderator sender bot. The bridge drop happens at `bot().waitTicks`
inside the test body — server stops responding to the client bridge
mid-test.

**Probable cause window**: the test was added in commit `b8d13958`
(TASK-36b ext). It exercises a NEW multi-client harness pattern
(two client bots), which is not used elsewhere in `testClient`.
Likely the harness wiring for the second bot drops the bridge
when the test crosses some state transition (logout, dim-change,
fetch-tp).

**Mitigation playbook**: instrument the test with extra
`bot().reportState()` polls before / after each `waitTicks` to
narrow which exact tick the bridge drops. If it's a server-side
restart-on-error, the cause is one of the server's tick handlers
throwing. If it's a client-side socket timeout, increase the
harness's per-bot read-timeout (forge-test-framework 0.4.x
config).

## Phase plan

### Phase 1 — Shape A (3 recipe tests)

1. Add a `/artest machine wait-for-recipe-registry <TileShortName>`
   probe verb that polls `RecipesMachine.getInstance().getRecipes(...).size() > 0`
   with a tight budget (e.g. 100 ticks, 1 s wall).
2. Update `MachineRecipeEndToEndKit.resolveFirstRecipe` to call the
   wait probe before the `recipe-info` probe.
3. Re-run full `testServer` suite 3× to confirm 0/3 occurrences
   of the "no recipes registered" shape.

### Phase 2 — Shape B (FetchModerator)

1. Instrument `WorldCommandFetchModeratorTest` with per-step
   `bot().reportState()` and `serverClient().execute("artest probe
   alive")` calls to bisect which tick drops the bridge.
2. Either fix the underlying production handler (if a server-side
   exception is causing the disconnect) OR adjust the test
   sequence to avoid the destructive transition (often a
   cross-dimension `/tp` mid-fetch).
3. Re-run isolated 10× to confirm PASS, then in full suite to
   confirm no contention.

## Closure criteria

- All 4 tests PASS in BOTH isolation AND full suite.
- TASK-42 ledger entry #5 marked ✅ FIXED.
- If Shape A mitigation reveals a real production bug in
  `RecipesMachine` (recipe-registration timing), promote to a
  new ledger entry / bug-fix task.

## Dependencies

**Requires**: nothing (independent).

**Blocks**: nothing — these failures are pre-existing and ledgered,
not a release blocker.

## Estimated effort

~4-6 h (Phase 1 lighter — probe verb + kit hook + suite re-runs;
Phase 2 unknown until instrumentation reveals the bridge-drop tick).
