# Context marker — 2026-05-23 TASK-17 shipped

**Slug**: 2026-05-23_task17-ssot-integrity-shipped
**Branch**: `feature/tests`
**Session focus**: TASK-17 (SSOT integrity follow-ups from
2026-05-23 audit) — closed as ✅ Completed.

## What happened

Picked the lowest-numbered actively-Backlog task (TASK-17) after
finding TASK-10 already shipped (it was listed `[open]` in the
SessionStart inject — pure SSOT drift, the exact class of bug
TASK-17 was created to prevent).

Three of four phases of TASK-17 were no-ops once revisited:

- **Phase 1 (SOP step 2.5)** — real work. Added a new step
  between Done-table sync (step 2) and the free-form stale-claim
  sweep (step 3) in `task-lifecycle.md`. Step 2.5 carries the
  per-tier `grep '@Test'` command, the rationale (counter line
  reads as a labelled fact so step 3 misses it), and an explicit
  skip-clause for TASK closures that don't move the counter.
  Also updated the `DEVELOPMENT-README.md` TL;DR to mention step
  2.5 alongside the stale-claim sweep as the two mandatory drift
  gates.
- **Phase 2a (`SatelliteTickBehaviourTest`)** — already shipped
  by `b97ddf0b` on 2026-05-21, two days before TASK-17 was
  created. The audit referenced a stale state of the file. Sweep
  found TASK-09 doc still naming the old method name + old
  contract phrasing; fixed.
- **Phase 2b (`SatelliteTypeBehaviourTest`)** — premise wrong.
  No `assertEquals(120, drainDelta)` ever existed in the test;
  only descriptive doc-comments claimed "exactly 120 RF". Did
  the consistent thing — cleaned the misleading doc-comments
  (class-level Javadoc, method Javadoc, one inline comment) and
  removed the leftover `STORED` `Pattern` declaration which was
  infrastructure for the never-written 120-RF assertion. Zero
  behaviour change.
- **Phase 3 (README pyramid counter)** — already inline-fixed in
  the backlog-formation commit `8f5e2ea7`. Counter re-verified
  at close-out: 237 / 80 / 319 / 41 = 677.

## Files touched

- `.agent/sops/development/task-lifecycle.md` — +step 2.5
- `.agent/DEVELOPMENT-README.md` — TL;DR mentions step 2.5
- `.agent/tasks/README.md` — TASK-17 row moved to Done
- `.agent/tasks/TASK-17-ssot-integrity-followups.md` — closed
  with full `## Result` section
- `.agent/tasks/TASK-09-satellite-type-depth.md` — stale method
  name + contract phrasing fixed
- `src/test/java/zmaster587/advancedRocketry/test/server/SatelliteTypeBehaviourTest.java`
  — doc-comments + dead `STORED` field

No production code touched. No test method added or removed
(pyramid counter unchanged at 677).

## Verification

- `./gradlew compileTestJava` PASS (only signal needed —
  changes are non-functional).
- Full pyramid run skipped intentionally; no runtime change.

## Resume conditions

`feature/tests` — TASK-17 file + README + doc edits + the
SatelliteTypeBehaviourTest cleanup all staged-or-modified;
diff awaiting user review before commit (per CLAUDE.md
"never auto-commit" rule).

After commit, next lowest-numbered actively-Backlog task is
**TASK-18** (Industrial machine powered-cycle coverage, ~6 h,
highest player-impact gap per audit §1).
