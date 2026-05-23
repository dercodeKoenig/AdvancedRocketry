# Context marker — 2026-05-23 TASK-25 + TASK-26 + TASK-16 batch shipped

**Slug**: 2026-05-23_task25-26-16-batch-shipped
**Branch**: `feature/tests`
**Session focus**: 4-task autonomous batch — TASK-26 (wildcard
machines), TASK-25 (PlatePress), TASK-16 (flake watch
investigation), and TASK-10 verification. Successor TASK-27 opened
for the deferred flake-fix work.

## Pyramid

237 / 80 / **339** / 41 = **697** (was 691 at session start, +6
from TASK-26 ×4 + TASK-25 ×2).

## What shipped

### TASK-26 — wildcard machines (4 @Tests + probe refactor + kit hook)

- `ArcFurnaceRecipeEndToEndTest` + `PrecisionAssemblerRecipeEndToEndTest`.
- `lookupWildcardMachineOverrides` + `WildcardConfig`/`HatchOverride`
  + `packCell` added; `handleFixtureGenericFromStructure` gained a
  trailing `WildcardConfig` param. Three call sites updated.
- `MachineRecipeEndToEndKit` gained adaptive `tickBudget =
  max(2000, recipe.time + 1000)` so longer recipes (ArcFurnace 6000,
  PrecisionAssembler 4000) complete.

### TASK-25 — PlatePress (2 @Tests + 3 probe verbs)

- `PlatePressRecipeEndToEndTest` — fixture validates 3-block stack;
  redstone activation drops EntityItem with recipe output.
- Probe additions: `fixture machine plate-press`,
  `machine recipe-info-block`, `entity scan-items`.

### TASK-16 — flake watch investigation

- Root-caused 3 distinct flake shapes; a 4th was spotted in passing:
  1. **Port contention** in `RealDedicatedServerHarness.reservePort()`
     — TOCTOU between parent socket close and child JVM bind.
  2. **Tick-timing race** in 2 tests asserting on
     "eventually-true" state synchronously.
  3. **Post-fixture validate race** — `attemptCompleteStructure`
     returns `attempted:false` once-in-a-while.
  4. **Worldgen sampling race** (NEW, single sighting) — three
     spaced chunks return identical (topY, biome) under
     full-pyramid pressure. `WorldgenDeterminismAndSamplingTest`.
- Shape #3 mitigated test-side via `assertFixtureValidates` retry
  (5 attempts × 200 ms gap — started at 3×75 ms, bumped after
  full-pyramid pressure surfaced a flake the smaller budget didn't
  cover).
- Shape #4 needs a 2nd occurrence to confirm pattern; logged in
  TASK-16 recurrence table.
- Shapes #1 + #2 deferred to **TASK-27** with concrete fix-shapes.

### TASK-10 — verification

- Already ✅ at session start; SSOT confirmed (both task file +
  README in sync). Closed without code work.

## Bugs found in production

None. The flakes found in TASK-16 live in the test harness
(`RealDedicatedServerHarness`) and in test code — not production.

## Backlog state

- **TASK-25 + TASK-26 + TASK-16**: closed.
- **TASK-27**: new follow-up for flake fix work (port-bind retry +
  per-test polling). ~4 h.
- All other backlog tasks (TASK-15 watching, TASK-19..24)
  unchanged.

## Files touched

- `src/main/java/.../TestProbeCommand.java` — 6 new code paths:
  `WildcardConfig` + `HatchOverride` + `lookupWildcardMachineOverrides`
  + `packCell` + `handleFixturePlatePress`; new branches for
  `fixture machine plate-press`, `machine recipe-info-block`,
  `entity scan-items`. ~280 LOC net add.
- `src/test/java/.../server/MachineRecipeEndToEndKit.java` —
  `FirstRecipe.time` + adaptive `tickBudget` + `assertFixtureValidates`
  retry. ~25 LOC.
- `src/test/java/.../server/ArcFurnaceRecipeEndToEndTest.java` — new.
- `src/test/java/.../server/PrecisionAssemblerRecipeEndToEndTest.java` — new.
- `src/test/java/.../server/PlatePressRecipeEndToEndTest.java` — new.
- `.agent/tasks/TASK-25-*.md` — closed.
- `.agent/tasks/TASK-26-*.md` — closed.
- `.agent/tasks/TASK-16-*.md` — investigation findings + closure.
- `.agent/tasks/TASK-27-*.md` — new (flake fix follow-up).
- `.agent/tasks/README.md` — pyramid 691→697, status table updates.

## Next up

- TASK-27 when the user wants to take on the harness-level fixes.
- TASK-19..24 backlog (multiblock trio, hovercraft, /ar positives,
  UV-assembler delta, sealdetector branches, SpaceArmor chest route).
