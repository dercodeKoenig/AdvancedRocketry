# TASK-16: Test-stability flake watch — parallel-fork contention

## Ticket

- Source: TASK-12 close-out marker 2026-05-23 ("flag these two for a
  future test-stability ticket if pattern recurs"), promoted into a
  tracked task on 2026-05-23 during the SSOT cleanup.
- Status: **Backlog (watching)**.
- Created: 2026-05-23.

## Context

The first full `./gradlew testServer` run after TASK-12 had **2
test failures** that passed in isolation AND on the immediate rerun
with no source changes between:

1. `BeaconMultiblockTest.beaconMultiblockValidatesWhenFixtureIsBuilt`
2. `MachineRecipeIntegrationTest.cuttingMachineRunsFirstRegisteredRecipe`

Both diagnosed as **parallel-forks resource contention** — the test
runner's worker forks tripped over each other on shared world /
fixture state. The flakes pre-date TASK-12 and were not introduced
by its production fixes (verified — the two failures pass when run
serially or one-at-a-time).

## Status: watching, not actively fixing

Rationale:

- One occurrence is not a pattern. A flake that fired exactly once
  during a single full-pyramid run is below the threshold to
  invest in.
- Both tests are real contract pins (TASK-04 multiblock + TASK-02
  machine-recipe shape). They are NOT spurious assertions —
  disabling them would actually lose coverage.
- The parallel-fork count is a global gradle setting; tuning it
  trades wall-time for stability across the **entire** test suite,
  not just these two.

**Promotion trigger**: bump out of "watching" if the flake reoccurs
on a clean run within the next ~5 testServer runs, OR if any third
test joins the flake list.

## Investigation sketch (when promoted)

### Step 1 — Identify the shared resource

Both flakes are server-tier. Candidates for the contention:

- Shared `World` / `WorldServer` instance across forks (unlikely
  — gradle forks are JVM-isolated).
- Shared file-system fixture (`/run-server` working dir,
  `.agent/visual-baselines/` if TASK-15 lands).
- Shared port binding (the testServer harness binds a debug port;
  if forked workers reuse the same number they'll collide).
- Shared registry state in static init that depends on the order
  forks reach `serverStart`.

Run with `--max-workers=1` to confirm contention is the cause (if
flakes disappear in serial, it's confirmed). Then re-introduce
parallelism with explicit per-test exclusion / serial grouping.

### Step 2 — Fix shape (one of)

- Per-fork file-system sandbox (assign `run-server/<fork-id>/` per
  worker).
- Mark these two specific tests with a serialisation hint that
  gradle honours.
- Convert one or both to testUnit if the world dependency is
  shallow.

### Step 3 — Pin the contract

The investigation must NOT delete the offending assertions. They
pin real contracts. Per
[`testing-principles`](../sops/development/testing-principles.md),
"flaky tests get fixed, they do not get deleted".

## Recurrence log

| Date | Test | Trigger | Run number that day | Resolution |
|---|---|---|---|---|
| 2026-05-23 | both | `./gradlew testServer` post-TASK-12 | run 1 of 1 | passed on rerun, no investigation yet |

When promoted, add subsequent occurrences here before opening an
implementation phase.

## Dependencies

- Does NOT block any other task.
- Touches the gradle build configuration, which is on the
  protected list (`gradle.properties` cannot be changed without
  explicit ask — `CLAUDE.md` rule).

## Estimated effort

- Investigation: ~2 h.
- Fix (if it's the file-system sandbox approach): ~1-2 h.
- Re-run + confirm flake-free across 10 testServer runs: ~30 min.
