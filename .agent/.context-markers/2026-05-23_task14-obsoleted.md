# Context marker — 2026-05-23 TASK-14 obsoleted

**Slug**: 2026-05-23_task14-obsoleted
**Branch**: `feature/tests`
**Session focus**: TASK-14 investigation → close as Obsolete.

## What happened

Same-day investigation of TASK-14 (companion-mod integration
coverage — JEI / GalacticCraft / MatterOverdrive) found the
original premise misleading. Reality:

1. Doc-claimed file sizes (~800 LoC) were 3× the actual (230 LoC
   integration + ~150 LoC of thin JEI wrappers).
2. Every call site is already Loader-gated (5 of them inventoried
   in the TASK-14 "Why obsolete" section).
3. Mod-absent paths are implicitly pinned by 441 existing tests
   (every test boots AR without MO/GC; the JEI null-guard is
   explicitly pinned by TASK-11's
   `reloadRecipesEmitsSuccessConfirmationMessage`).
4. Mod-present paths require non-trivial infrastructure (shim
   classes Option B ≈8 h, or vendoring companion jars Option A
   ≈12 h+) and no cross-mod regression signal exists today.

User chose to close as Obsolete; if a real cross-mod regression
gets reported in the future, open a narrow successor TASK tied
to that specific regression — not a sweep.

## Stale-claim sweep performed

- `tasks/README.md` — TASK-14 row moved from Backlog table to Done
  table with ❌ Obsolete marker.
- `TASK-15-visual-regression.md:88` — "Companion-mod GUIs (depends
  on TASK-14)" updated to reference TASK-14's obsolescence rather
  than waiting for it.
- Prior session's marker (`2026-05-23_task13-wireless-...`) NOT
  edited — historical snapshot stays accurate to its time.

## Branch state

`feature/tests` — about to commit the TASK-14 obsolescence pass.
Previous commit (TASK-13) at `194c1c99`.

## Resume conditions

Backlog after this close-out: TASK-15 (visual regression) and
TASK-16 (test-stability flake watch) — both lower priority. User
asked about TASK-15 next; that's the immediate follow-up.
