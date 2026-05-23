# Context marker — 2026-05-23 TASK-26 shipped

**Slug**: 2026-05-23_task26-wildcard-machines-shipped
**Branch**: `feature/tests`
**Session focus**: TASK-26 (wildcard-structure machine coverage). The
2 remaining machines deferred from TASK-18 — ArcFurnace and
PrecisionAssembler — both shipped with full end-to-end recipe pins
reusing `MachineRecipeEndToEndKit` + a small probe refactor.

## What shipped

- 4 server-tier @Tests (2 classes × 2 methods).
- 1 probe refactor (`handleFixtureGenericFromStructure` gains a
  `WildcardConfig` trailing parameter; processes overlay + filler
  for `'*'` cells; merges into hatch-position response lists).
- 1 kit hook (`FirstRecipe.time` + adaptive `tickBudget`).
- 3 existing call sites pass `null` for the new parameter
  (terraformer, orbital-laser-drill, generic-machine-non-wildcard).

## Wildcard layout decisions

ArcFurnace — hatches on the y=3 base ring opposite the controller:
- I at structure[3][4][1]
- O at structure[3][4][3]
- Filler at every other y=3 wildcard: `blockBlastBrick`
- P is already explicit in structure at y=0 (3 cells)

PrecisionAssembler — all 3 hatches on the y=2 front row:
- I at structure[2][0][1]
- O at structure[2][0][2]
- P at structure[2][0][3]
- Filler at every other y=2 wildcard: `blockStructureBlock`

## Pyramid

237 / 80 / **337** / 41 = **695** (was 691, +4 from this task).

## Bugs found in production (none)

No production bugs surfaced.

## Flakes captured

Two intermittent failures during the 9-class RecipeEndToEnd group
run (ArcFurnace + RollingMachine fixture-validates), both with
`attempted:false` from `attemptCompleteStructure` immediately after
fixture build. Same shape — and likely same chunk-load /
world-state race. Logged into TASK-16 as a third distinct flake
shape (now: port contention, tick-timing, post-fixture-validate).
Both passed in isolation on the immediate re-run.

## Files touched

- `src/main/java/.../TestProbeCommand.java` — `lookupWildcardMachineOverrides`,
  `WildcardConfig`, `HatchOverride`, `packCell`, refactored
  `handleFixtureGenericFromStructure` body. ~80 LOC net add.
- `src/test/java/.../server/MachineRecipeEndToEndKit.java` —
  `FirstRecipe.time`, `TIME_FIELD` pattern, adaptive `tickBudget`. ~10 LOC.
- `src/test/java/.../server/ArcFurnaceRecipeEndToEndTest.java` — new (29 LOC).
- `src/test/java/.../server/PrecisionAssemblerRecipeEndToEndTest.java` — new (28 LOC).
- `.agent/tasks/TASK-26-*.md` — closed (Actual scope + Result sections added).
- `.agent/tasks/TASK-16-*.md` — recurrence log row + flake-shape #3 note.
- `.agent/tasks/README.md` — TASK-26 row in Done, removed from Backlog, pyramid counter regen.

## Next up in this session

- TASK-25 (PlatePress single-block redstone).
- TASK-16 (flake watch investigation — promotion trigger has now fired three times).
