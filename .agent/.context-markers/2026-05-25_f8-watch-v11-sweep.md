# Marker — 2026-05-25 F8 watch v11 sweep

**Branch**: `feature/tests`
**Trigger**: Acted on TASK-28 F8 follow-up — ran a 10× `testServer`
sweep to check whether the Beacon `try-complete attempted:false`
flake from v10 recurred, sharpening the trigger for TASK-29.

## Outcome

| Shape | Sightings | Status |
|---|---|---|
| **F8** Beacon `try-complete attempted:false` | **0 / 10** | 1/5 toward Obsolete (was 1/10 in v10). TASK-29 not opened — "2nd consecutive" trigger not met. |
| **F9** (NEW) `MissionGasCompletionTest.gasCompletionFillsRocketFluidTilesWithConfiguredFluid` | 1 / 10 | Watching, 1/5. Probe returned `fluidEntries:0, rocketCount:7` (rocketCount unusually high — fixture pollution or fluid-tank variant didn't substitute). |

**Aggregate**: 9 / 10 PASS. Median run 893 s (~14.9 min). Total wall ~149 min.

## What was touched

- `.agent/tasks/TASK-28-residual-test-flakes.md` — replaced
  "Followups → TASK-29 (deferred)" section with watching-mode
  followups + v11 sweep results.
- `.agent/tasks/README.md` — TASK-28 Done row updated to reflect
  v11 outcome (F8 0/10, F9 1/10, no TASK-29).

**Production code**: untouched. Pyramid: unchanged (237 / 80 / 339 / 41 = 697).
Bug ledger: drained.

## Decision rationale

Per [`flake-diagnosis.md`](../sops/development/flake-diagnosis.md):

- **F8**: 0/10 in v11 means no characterisation-sharpening evidence.
  Opening TASK-29 with 1-in-20 data would speculate, not diagnose.
  F5 convention applies: 5 consecutive clean 10× sweeps → Obsolete.
- **F9**: Sparse single-occurrence in 10. SOP says watch for 2nd
  sighting before structural work. The `rocketCount:7` hint is
  recorded as the likely investigation lead when/if it recurs.

## How to resume

1. If F8 (Beacon `attempted:false`) appears again in any
   testServer run: that's the 2nd sighting → open TASK-29 with
   v10 + new run as concrete data points.
2. If F9 (MissionGasCompletion fluid empty) appears again: 2nd
   sighting → characterise (probe a `with-fluid-cargo` fixture
   inspection to confirm liquidTile substitution worked).
3. Otherwise: keep counting clean sweeps. After 5 clean reruns
   each, downgrade to Obsolete per F5 precedent.

## Artifacts

- `/tmp/f8-sweep/summary.md` — per-run table.
- `/tmp/f8-sweep/run1.failtail` — F9 stack tail.
- `/tmp/f8-sweep/run{1..10}.log` — full gradle logs (purge when stale).

## Why this file exists

Marks a clean inflection: TASK-28 stays partial-closed, no new
TASK opened, but the watching counters moved. Future-me needs the
"1 / 5" framing to know whether to act when F8 or F9 reappears.
