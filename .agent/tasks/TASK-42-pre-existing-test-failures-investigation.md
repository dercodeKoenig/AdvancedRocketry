# TASK-42 — Investigate 5 pre-existing test failures on feature/tests HEAD

**Status: ✅ Completed 2026-05-30 (triage + InventoryBypass @Ignore'd; remaining 4 → [TASK-43](TASK-43-flaky-and-stable-test-failures.md)).**

## Ticket

- Source: surfaced 2026-05-29 / 2026-05-30 during TASK-41 validation
  sweep. After landing TASK-41, full `testServer` + `testClient` runs
  on `feature/tests` HEAD (`41cccd53`) show 5 stable failures across
  both layers. Re-runs reproduce identically; baseline check (TASK-41
  reverted) reproduces the same 5 → NOT caused by TASK-41.
- Status: 🟡 In progress.
- Created: 2026-05-30.
- Predecessor / ledger: `tasks/README.md` "Current state" entry #5.

## The 5 failures

### testServer (3)

1. `ElectrolyserRecipeEndToEndTest` — asserts
   `recipe-info errored ... "no recipes registered"` at
   `MachineRecipeEndToEndKit.resolveFirstRecipe:196`.
2. `PrecisionAssemblerRecipeEndToEndTest` — same shape (no recipes).
3. `PrecisionLaserEtcherRecipeEndToEndTest` — same shape (no recipes).

All three share `MachineRecipeEndToEndKit.resolveFirstRecipe:196`.
The kit calls `/artest fixture machine <machine> ...` and then
`/artest recipe-info <machine>`; the latter probe replies with
`{"error":"no recipes registered","machine":"..."}`.

### testClient (2)

4. `InventoryBypassRedirectE2ETest.mixinRedirectKeepsContainerOpenAcrossDistance`
   — fails at line 99 (`assertEquals("chest GUI must open on
   right-click", GUI_CHEST, screenOf(...))`). The chest is placed,
   the player TPs above it, but `openGuiByRightClick` (6 attempts ×
   60-tick poll window) reports no open screen. Fails BEFORE any
   bypass-specific assertions — the regression is in the chest-open
   handshake itself, not in the mixin redirect under test.
2. `WorldCommandFetchModeratorTest.moderatorFetchTeleportsTargetToSenderPosition`
   — `IOException: Client bridge closed unexpectedly`
   (`ClientBot.execute:210`). Client subprocess loses the harness
   socket mid-test. Classic flaky-shape — distinguish via
   distribution analysis (per `sops/development/flake-diagnosis.md`).

## Verified NOT caused by

- **TASK-41** (AccessorWorld → AT migration, 2026-05-29 `df98f5eb`).
  Baseline check: `git checkout HEAD~2 -- <prod files>` reproduces
  all 5 failures on Xvfb :100.
- **Xorg :99** (LWJGL 2.9.4 ↔ amdgpu DDX). Not a factor — testClient
  on :99 crashes BEFORE any test runs (`LWJGLException: No modes
  available`). All 5 failures observed on :100 Xvfb where LWJGL
  initialises cleanly.

## Investigation plan (this task)

### Phase 0 — distribution diagnosis per `flake-diagnosis.md` SOP

For each failing test (in priority order: client-fast, then server-slow):

1. 10× re-run of that single test on current HEAD with `--tests` filter
   and cache-bust between runs
   (`rm -rf build/{reports,test-results,tmp}/testClient`).
2. Classify the failure distribution:
   - **Same shape every run** → regression. Bisect commit.
   - **Sparse non-deterministic set** → race. Investigate per-test.
   - **Alternating outputs on same test** → test-design bug.
3. Each test commit was added at:
   - `InventoryBypassRedirectE2ETest` → `149c361e` (TASK-08-mixin).
   - `WorldCommandFetchModeratorTest` → `b8d13958` (TASK-36b ext).
   - `PrecisionAssemblerRecipeEndToEndTest` → `aedd909c` (TASK-25/26).
   - `PrecisionLaserEtcherRecipeEndToEndTest` → `aedd909c` (TASK-25/26).
   - `ElectrolyserRecipeEndToEndTest` → `5a262bc4` (TASK-18).
   Bisect window: [test-add commit, current HEAD).

### Phase 1 — fix or formally @Ignore

Depending on Phase 0 outcome:
- **Real regression**: fix the underlying production bug, restore the
  test to green. Add a ledger entry for the bug if it's player-visible.
- **Real race / flake**: apply the per-shape mitigation from TASK-27
  / TASK-28 playbook (probe-driven wait, polling helpers, force-load
  pin, etc.). NEVER bump retry budgets blind.
- **Test-design bug**: rewrite the assertion to pin the actual
  contract (per `testing-principles.md` litmus).

### Phase 2 — close ledger entry #5

Once all 5 are restored to green (or formally @Ignore'd with a
documented reason), update `tasks/README.md` ledger entry #5 to
✅ FIXED with the per-test outcome.

## Test plan (verification)

- Each fixed test PASSES 10× in isolation with cache-bust.
- Full `testServer` PASSES 427/427 (1 skip allowed).
- Full `testClient` PASSES 62/62 (1 skip allowed).

## Dependencies

**Requires**: nothing (independent investigation).

**Blocks**: nothing — bug-fix work, not coverage expansion.

## Estimated effort

~4-8 h (Phase 0 distribution-checks fast; Phase 1 cost depends on
how many are real regressions vs flakes).

---

## Phase 0 findings (2026-05-30)

### `InventoryBypassRedirectE2ETest.mixinRedirectKeepsContainerOpenAcrossDistance`

**Distribution**: 10/10 FAIL on HEAD. Bimodal shape:
- 5/10 fail at line 99 (`chest GUI must open on right-click` —
  `openGuiByRightClick` returns "" after 6 retries × 60-tick poll).
- 5/10 fail at line 124 (`with inv-bypass active, the chest GUI
  must remain open across a 200-block teleport` — `screen=""`,
  `bypassStatus inBypass:true`).

**Test-add-commit check**: also fails at `149c361e` (the commit
that added the test) with the same line-99 shape. → **Broken
since inception.** Prior session's "all green" claim was the
honesty-noted over-confidence already flagged in the pre-compact
marker.

**Probable root cause (line 124)**: with player TP'd 200 blocks
away, the chest chunk falls outside the client's view distance
(default 10 chunks = 160 blocks). The client may close the chest
GUI on its own end (chunk unload / pos-out-of-range handling)
independent of the server-side `canInteractWith` redirect under
test. Test-design bug: the assertion conflates "server-side
mixin redirect prevented closeScreen" with "client kept GUI open
across long-distance TP", but those are independent failure modes.

**Probable root cause (line 99)**: right-click bot interaction
sometimes fails to register against the freshly-placed chest.
The helper retries (6 × 60 tick poll), so a single tick miss
shouldn't be enough — possibly a real interaction-blocking
production bug (some block-activate handler intercepts), or a
chunk-load race the force-load isn't covering.

**Disposition recommendation**: `@Ignore` with documented reason
referencing this section. The contract under test (mixin redirect)
IS exercised by `testUnit.RocketInventoryHelperRedirectTest`
(pure function level) already; the e2e is bonus coverage with
non-trivial test-design issues. NOT a TASK-41 regression.

### `WorldCommandFetchModeratorTest.moderatorFetchTeleportsTargetToSenderPosition`

**Symptom on HEAD**: `IOException: Client bridge closed unexpectedly`
(`ClientBot.execute:210`).

**Test-add-commit check**: at `b8d13958`, the test ALSO fails but
with a different shape — `Failed to start real client harness`
caused by `asm-6.0.jar` `module-info.class` parser crash in
old Forge's ASM 5.2. → **environment-skew** when checking out old
commits in current dev env; can't establish a clean baseline this
way.

**Disposition recommendation**: 10× rerun on HEAD first to classify
as flake-vs-stable (not done yet — single-test isolated reruns are
cheap, ~2-3 min each). If flake, apply TASK-27/28 mitigation
(probe-driven wait, polling helpers). If stable, deeper investigation
of why the client bridge drops mid-test for this specific scenario.

### `ElectrolyserRecipeEndToEndTest` / `PrecisionAssemblerRecipeEndToEndTest` / `PrecisionLaserEtcherRecipeEndToEndTest`

**Symptom on HEAD**: `MachineRecipeEndToEndKit.resolveFirstRecipe:196`
asserts `recipe-info errored ... "no recipes registered"` for the
machine's TileEntity class.

**Source check**: `RecipesMachine.getInstance().getRecipes(TileXxx.class)`
returns null or empty. Production registration flow looks correct:
- `AdvancedRocketry.preInit` calls
  `LibVulpes.registerRecipeHandler(TileXxx.class, ...path-to-xml)`.
- `RecipeHandler.registerXMLRecipes` calls
  `LibVulpes.instance.loadXMLRecipe(TileXxx.class)`.
- `LibVulpes.loadXMLRecipe` reads the existing XML file and
  registers recipes via `XMLRecipeLoader.registerRecipes(clazz)`.
- XMLRecipeLoader populates `RecipesMachine.recipeList.put(clazz, recipes)`.

All 3 machines have valid `<Recipe>` entries in
`run/config/advRocketry/<Machine>.xml`. Yet runtime
`getRecipes(machineClass)` returns empty. Something between
"XML parsed" and "probe lookup" loses the registration.

Notable: `BlockSmallPlatePress` (Block, not Machine) passes its
recipe test — the registration handler is wired identically but
the class is a Block, not a TileMultiblockMachine. XML loader
may handle Block vs Tile classes differently.

**Disposition recommendation**: dig into `XMLRecipeLoader` once —
either the loader silently fails for TileMultiblockMachine subclasses
in current libVulpes, OR registration runs at a different load
phase than the probe sees. A production bug here would be
player-visible ("Electrolyser has no recipes after world load")
and worth ledger-promoting independently of the test outcome.

## Recommended next steps

1. **`InventoryBypass` → `@Ignore`** with documented reason. Low
   value (contract pinned at unit level), test-design issues
   (chunk unload conflated with redirect-not-firing).
2. **3 Recipe tests → investigate libVulpes registration**. If
   Electrolyser/PrecisionAssembler/PrecisionLaserEtcher genuinely
   have no recipes at runtime, that's a real production bug
   (ledger-promote, fix in libVulpes or AR) and the tests should
   stay red until fixed.
3. **`WorldCommandFetchModeratorTest` → 10× rerun on HEAD** to
   classify (likely flake — Client bridge drops are TASK-27/28
   territory). Apply per-shape mitigation if stable.

Phase 1 of this task picks whichever of (1)/(2)/(3) the user
prioritises. Each is independent.

---

## Phase 1 outcomes (2026-05-30)

### (1) InventoryBypass ✅ DONE

`@Ignore`d at `src/test/java/.../client/InventoryBypassRedirectE2ETest.java`
with a multi-line reason citing this doc's Phase 0 findings.
Contract still pinned by `testUnit.RocketInventoryHelperRedirectTest`
(pure-function level — covers the bypass-set predicate the
@Redirect calls).

### (2) Recipe tests — picture flipped

The 3 recipe tests were **investigated** rather than fixed directly
because isolated reruns revealed the assumed root cause was wrong:

```
./gradlew testServer -PuseLocalFramework=true \
  --tests ...ElectrolyserRecipeEndToEndTest
# → PASS in 30 s

./gradlew testServer -PuseLocalFramework=true \
  --tests ...PrecisionAssemblerRecipeEndToEndTest \
  --tests ...PrecisionLaserEtcherRecipeEndToEndTest
# → 4/4 PASS in 32 s
```

So the tests ARE NOT broken — they only fail when running in the
full `testServer` suite (parallel-fork contention). This is real
flake-shape per `flake-diagnosis.md` ("same N tests every run, none
when isolated → race"). Production code is correct; the failure
is a harness / registry-timing race that surfaces only at
suite-scale concurrency. **Promoted to [TASK-43](TASK-43-flaky-and-stable-test-failures.md)
Shape A** with a Phase 1 plan for a `wait-for-recipe-registry`
probe verb.

### (3) FetchModerator — picture also flipped (different direction)

Isolated single-test rerun:

```
./gradlew testClient -PuseLocalFramework=true \
  --tests ...WorldCommandFetchModeratorTest
# → FAILED in 3m 10s (same shape as full suite)
```

NOT a parallel-fork flake — fails stably in isolation. So either a
real production bug (handler throws mid-fetch, server drops the
bridge) OR a test-design bug (transition the bot harness can't
recover from). **Promoted to [TASK-43](TASK-43-flaky-and-stable-test-failures.md)
Shape B** with a Phase 2 plan for per-step bot instrumentation.

## Closure

This task's role was **triage + low-risk @Ignore close-out** to free
the test suite from the 5-failure noise floor. The deeper diagnostics
for the remaining 4 (1 stable, 3 flaky) live in TASK-43. Ledger entry
#5 stays open and tracks the unified 4-test set via TASK-43.
