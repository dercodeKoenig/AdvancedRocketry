# SOP: Flake diagnosis — distinguishing races, regressions, and test-design bugs

## Context

Applies whenever a test fails intermittently AND only under load
(typically the full `testServer -Pforks=N` pyramid). Before reaching
for the retry-budget knob, this SOP forces you to identify **which
of three things you're actually looking at** — because the wrong
diagnosis leads to wasted retry cycles, masked production bugs, or
new regressions.

The lessons here come from TASK-27 + TASK-28 (2026-05-23..24): 50+
testServer reruns chasing what turned out to be **four distinct
failure shapes**, one self-introduced regression that masqueraded as
"0/10 flakes", and one test-design bug that masqueraded as a race.

## The three failure modes

### 1. Real race (a "flake")

- **Distribution**: sparse, **non-deterministic test set per run**.
  Run 1 has tests A+B, run 2 has C, run 3 has nothing, run 4 has B+D.
- **Repro**: happens only under load (parallel forks, full suite).
  Passes in isolation 100 %.
- **Fix shape**: retry budget OR removing the timing dependency
  (poll-until, force deterministic tick, force chunk load).

### 2. Regression introduced this session

- **Distribution**: **same exact test set every run**. Run 1 has
  X+Y+Z, run 2 has X+Y+Z, run 3 has X+Y+Z. **100 % reproducible**.
- **Repro**: passes in isolation (the regression touches a shared
  helper / probe / kit, not the test itself).
- **Fix shape**: revert the offending change; the diff between
  green and red is small.

### 3. Test-design bug

- **Distribution**: mixed — failure depends on world / recipe / dim
  state that the test assumed was deterministic but isn't.
  Sometimes consistent within a session, sometimes alternating
  between two outputs.
- **Repro**: passes some environments, fails others. The test's
  ASSERTION is what's wrong, not the production code.
- **Fix shape**: loosen the assertion to what the contract actually
  guarantees, not what the test author hoped was true. (See also:
  [`testing-principles.md`](./testing-principles.md).)

## The diagnosis checklist

When a 10× rerun comes back with N failures, work through this in
order. Skipping steps is how you waste 150-minute reruns chasing
the wrong variable.

### Step 1 — Confirm the test actually ran

Gradle's `:testServer UP-TO-DATE` cache means **a rerun with zero
inputs changed re-runs zero tests**. The build still reports
`BUILD SUCCESSFUL`, and your loop counter still says "PASS". You
have learned nothing.

**Symptoms**:
- Run 1 takes the expected wall-time (~15 min); runs 2-10 take
  ~11 seconds each.
- Per-run `tests-passed=` count is 0 or near-0 on the cached runs.

**Mitigation**: bust the cache on every iteration:

```bash
rm -rf build/reports/tests/testServer \
       build/test-results/testServer \
       build/tmp/testServer
./gradlew testServer ...
```

OR pass `--rerun-tasks` (slower — rebuilds upstream too).

Always grep `grep -c " PASSED" $LOG` per run and assert it matches
the expected pyramid count (in this repo: ~336 server-tier tests).

### Step 2 — Tabulate failures across runs

Before reaching for any tool, write down which tests failed in
which runs. Use the per-run failure list from your loop's summary
file. The shape that emerges tells you what to do next.

**If the same test fails 8+/10**: this is mode 2 (regression).
Skip to Step 3.

**If different tests fail across runs with overlap rare**: this is
mode 1 (race). Skip to Step 4.

**If a single test alternates outputs across runs** (run 1 expected
A, found B; run 2 expected B, found A): this is mode 3 (test
design). Skip to Step 5.

### Step 3 — Suspect your own recent change first

For mode-2 regressions, the change that introduced the regression
is yours, and it's recent. The signature is the giveaway:
**same N tests, 100 % reliable, after a probe / kit / helper edit**
that those N tests touch (transitively).

**The canonical example** (TASK-28 v6, 2026-05-24): three rocket-
launch tests failed 100 % across all 10 runs after a probe-level
chunk pre-load was added. Root cause: 5×5-chunk pre-load (~25 chunk
gens, ~2 s) blocked the server thread, and the post-unblock
natural-tick burst raced `isInFlight = true` back to `false` via
`rocket.onUpdate()` ticking faster than the test could read state.

**Diagnostic move**: `git diff HEAD~1 HEAD -- <suspect files>` then
revert the change in a worktree and rerun. If green, the change is
the regression. Then write a smaller, surgical version — for the
rocket case, "skip-rocket-from-dispatcher-pre-load" + per-handler
pre-load worked.

**Common regression vectors**:

- **Probe-level Thread.sleep / chunk-gen** that blocks the server
  thread → post-unblock natural-tick burst → state machines that
  rely on "few ticks between writes and reads" break.
- **New sentinel string in a wait-loop** that doesn't actually
  appear in the probe's response → wait loops out, test fails on
  the wait assertion (TASK-28 v3, wireless wait-for-tile checking
  `contains("TileWirelessTransciever")` against `{"ok":true,...}`).
- **Argument-index miscount** in a dispatcher that handles multiple
  command shapes — wrong args pre-loaded → wrong chunks → wrong
  behaviour.
- **Test-side budget bump that runs out of timing-budget elsewhere**
  in the same suite (rare but happens with shared time-budgets).

### Step 4 — For races, find the variable that's NOT time

If you're at retry-budget bump number 3 (e.g. 1.5 s → 3 s → 6 s →
12 s) **and the flake rate isn't dropping**, time is not the
variable. Stop bumping. Find what is.

**Common non-time variables**:

- **Chunk load state**: if a probe reads block/tile state at a
  position whose chunk isn't loaded, you get `tile:null` or
  `attempted:false` regardless of wait. Fix: `provideChunk(cx, cz)`
  before the read.
- **Population state**: chunk is loaded but `isTerrainPopulated() ==
  false`; biome / topY data is sentinel. Fix: poll `isTerrainPopulated`.
- **Tick gate (`% N == 0`)**: production runs work only every N
  world ticks. Under load, the gate hits rarely. Fix: extract the
  gated body into a separately-callable method; drive it from a
  test-only probe verb.
- **Recipe / registry ordering**: production iterates a set whose
  order ≠ probe's introspection order. (See mode 3 below.)

**Rule of thumb**: if your retry budget exceeds 5 s and the failure
rate is still > 5 %, the fix is structural, not timed.

### Step 5 — For test-design bugs, loosen, don't tighten

If a test expects "first registered recipe output = X" and
production produces Y, the test is asking the wrong question. The
contract is "centrifuge processes a recipe and produces SOME
output", not "centrifuge processes registration-index-0 specifically".

Tighten until you've named the contract; loosen until you're
naming nothing else. Identity assertions on values production
chooses internally are usually one of those.

(See also [`testing-principles.md`](./testing-principles.md) for
the contract-vs-impl litmus.)

## Probe-author safety rules

Probes run on the server thread. Anything you do that takes wall
time IS time the natural-tick loop isn't running. Anything you do
that THEN releases is followed by a tick burst.

### Rule P1 — Bound your wait budgets

Any `Thread.sleep` in a probe must have a documented ceiling and an
early-exit condition. 12 s is the **absolute upper bound** for a
single probe call; > 12 s and you're masking a real issue (or
breaking harness-level timeouts).

### Rule P2 — Don't pre-load more chunks than the operation needs

A 5×5 chunk pre-load (25 chunks ≈ 2 s gen on cold-start) caused
the TASK-28 v6 regression. Use the smallest area that covers the
real footprint:

| Op | Recommended pre-load |
|---|---|
| Single `place` | 1 chunk |
| Multiblock fixture (5×5×5 max) | 3×3 chunks |
| Rocket-style fixture (multi-tile placement) | **none** — the natural-tick burst risk outweighs the chunk-load risk |
| Volumetric fill | every chunk in the volume |
| Worldgen sample | 3×3 chunks + isPopulated poll |

### Rule P3 — Validate your sentinels against the actual probe response

Before adding `response.contains("XYZ")` to a wait loop, **read an
example response** (run the probe once, log the result). The
probe's `{"ok":true,...}` envelope and its field set are public
contract — `contains("ClassName")` is not, and breaks the moment
the probe stops emitting class names.

### Rule P4 — Refactor production to expose, don't reflect into private

When a test needs to drive a `private final void doX()` that's
gated behind a `% N == 0` clock, extract the body into a
`public void onIntermittentDoX()` and call THAT from the probe.
`update()` keeps the gate; the probe calls `onIntermittentDoX()`
directly. No reflection, no observable behaviour change.

(Done for `TileForceFieldProjector.onIntermittentUpdate()` in
TASK-28 to bypass the `world.getTotalWorldTime() % 5 == 0` gate.)

## Verification loop discipline

When running a 10× sweep to verify a flake fix:

1. **Cache-bust per iteration** (Step 1 above).
2. **Log per-run PASS count** so you catch the "0 actually ran" case
   immediately.
3. **Capture per-failure stack tail** — the loop should grep
   `FAILED|BUILD FAILED` and dump 10-15 surrounding lines to the
   summary, otherwise you'll be re-grepping logs constantly.
4. **Don't kick off the next sweep before reading the previous
   one** — you'll burn 150 minutes on a change that didn't even
   compile correctly.

## When to stop iterating

Each 10× sweep is ~150 minutes on a 3-fork host. Burn rate matters.
Stop and ship a partial when ALL three of these are true:

- The remaining failures are **single-test, single-occurrence in 10**
  (no clustering).
- You've already done the structural fix for the dominant shape
  (chunk pre-load / direct tile drive / contract loosening).
- The next fix-shape would require deeper investigation
  (instrumenting libVulpes internals, debugging chunk-unload
  scheduling, etc.) — i.e. it's a separate task.

Document the residual as a new TASK-NN follow-up. Close the current
one as ✅ **partial** with an honest scorecard. Future-you (or
someone else) will catch the residual when it surfaces a second
time — flake patterns sharpen with sightings, not with speculation.
