# Context marker — 2026-05-23 TASK-18 shipped (7 of 9 machines)

**Slug**: 2026-05-23_task18-industrial-machines-shipped
**Branch**: `feature/tests`
**Session focus**: TASK-18 (industrial machine powered-cycle
coverage). Shipped 7 of 9 multiblock machines + 3 probe
extensions + shared kit; 2 wildcard-structure machines deferred
to TASK-26; PlatePress was pre-emptively split to TASK-25
earlier in the session.

## What shipped

### Tests (+14 server-tier @Tests, ~20 LOC each)

- `RollingMachineRecipeEndToEndTest` (items+fluid → item)
- `LatheRecipeEndToEndTest` (items → item)
- `CrystallizerRecipeEndToEndTest` (items → item)
- `PrecisionLaserEtcherRecipeEndToEndTest` (items → item, lens catalyst)
- `ElectrolyserRecipeEndToEndTest` (fluid → fluid)
- `CentrifugeRecipeEndToEndTest` (fluid → fluid+item)
- `ChemicalReactorRecipeEndToEndTest` (2-fluid → fluid; rocketfuel)

Each class has 2 tests: `*FixtureValidates` + `*RunsFirstRegisteredRecipe`.
The middle `*AcceptsRecipeInputs` test from the original plan was
trimmed after SOP self-audit — see TASK-18 file "Test depth"
section. Drain-pin was added to the runs-first-recipe path to
close the free-output regression gap.

### Shared kit (~280 LOC)

`MachineRecipeEndToEndKit` — fixture → validate → fill items →
fill fluids (multi-hatch aware) → inject power → enable →
force-tick → **assert input drained** (soft: any-slot, tolerates
catalysts) → assert output (item OR fluid). Auto-discovers recipe
shape from `recipe-info` probe. Handles all 4 quadrants of
item/fluid input × item/fluid output.

### Probe extensions (3)

1. **`/artest fixture machine <key>`** — new dispatch in `handleFixture`
   for 9 multiblock industrial machines via the existing generic
   `handleFixtureGenericFromStructure`. Lookup table:
   `lookupMultiblockMachineSpec` maps kebab-case keys → {namespace,
   controller registry path, tile FQN}.
2. **`handleFixtureGenericFromStructure` enhanced** — now scans the
   structure array for libVulpes hatch chars 'I'/'O'/'P'/'p'/'L'/'l'
   and emits per-char position **lists** (e.g.
   `liquidInputPositions: [[x,y,z],[x,y,z]]`). Backward-compatible
   first-position aliases (`liquidInputPos: [x,y,z]`) retained.
   Multi-position lists were required for ChemicalReactor (two 'L'
   hatches, two-fluid recipe).
3. **`/artest machine recipe-info` enhanced** — now emits
   `fluidIngredients` and `fluidOutputs` sections (was item-only).
   Backward-compatible: missing sections fall through to empty.

### Pyramid

237 / 80 / **333** / 41 = **691** (was 677, +14 from this task
after SOP-driven trim).

## Bugs found in production (none)

No production bugs surfaced. The 7 machines run end-to-end. The
test-side bugs encountered during iteration (`meta`-less hatch fill
silently failing, single-fluid limit on ChemicalReactor) all sat
in the test infrastructure, not in production code.

Worth noting for future: the `productsheet:0` (iron) vs
`productsheet:1` (steel) meta mismatch is a sharp gotcha — any
ingredient pattern that drops meta from hatch-fill calls will
silently fail to match recipes whose oredict entries pin specific
metas. The kit captures meta now.

## Files touched

- `src/main/java/.../TestProbeCommand.java` — +3 probe extensions
  (lookup table, fluid sections in recipe-info, position-list emit
  in handleFixtureGenericFromStructure).
- `src/test/java/.../server/MachineRecipeEndToEndKit.java` — new,
  280 LOC.
- `src/test/java/.../server/{Rolling,Lathe,PrecisionAssembler,
  Electrolyser,ChemicalReactor,Crystallizer,Centrifuge,
  PrecisionLaserEtcher}RecipeEndToEndTest.java` — 7 new thin classes.
  Note: PrecisionAssembler + ArcFurnace were transiently created
  then deleted before commit when their wildcard structure shape
  surfaced; see TASK-26.
- `.agent/tasks/TASK-18-*` — closed, "Actual scope" + "Result"
  sections added.
- `.agent/tasks/TASK-25-plate-press-coverage.md` — created earlier
  in session for PlatePress.
- `.agent/tasks/TASK-26-wildcard-based-machine-coverage.md` — new,
  successor for ArcFurnace + PrecisionAssembler.
- `.agent/tasks/README.md` — TASK-18 row in Done, TASK-25 + TASK-26
  in Backlog; pyramid counter regenerated to 698.

## Deferred to successors

- **TASK-25** (PlatePress) — split out pre-emptively in Phase 0 when
  the redstone-pulse single-block shape surfaced as fundamentally
  different from the multiblock pipeline.
- **TASK-26** (ArcFurnace + PrecisionAssembler) — split out mid-Phase 1
  when `'*'` wildcard structures broke the generic fixture's
  hatch-position scanning. Cleanest fix is per-machine bespoke
  handlers that overwrite specific wildcard cells with hatches.

## Full-pyramid run result

`./gradlew testServer` after the TASK-18 additions ran 331 tests
with 2 failures and 3 ignored. Both failures passed in isolation
on the immediate rerun — confirmed flakes:

- `WarpControllerDepthTest` (classMethod) — `BindException: Address
  already in use` on the test harness port. Classic parallel-fork
  port contention.
- `MissionLifecyclePyramidTest.completionPrunesMissionFromSatelliteRegistry`
  — mission at `progress=1.0` + `isDead=true` but not yet pruned.
  Tick-timing race.

Both filed in **TASK-16** recurrence log. The promotion trigger
("a third test joins the flake list") fired — TASK-16 moved from
👁 Watching → 🟢 Backlog for active investigation. Two distinct
flake shapes are now visible (port contention vs tick-timing
race); the implementation phase should treat them as
related-but-separable.

Neither flake is caused by TASK-18 changes — the harness probe
extensions are pure additions, no shared-state mutation. The
green pyramid baseline is 329 / 0 / 3 (PASSED / FAILED / IGNORED).

## Resume conditions

`feature/tests` — all TASK-18 changes ready to commit per
`task-lifecycle.md` step 5 (single commit, awaiting user
review per CLAUDE.md "never auto-commit" rule).

After commit, two attractive next candidates:

- **TASK-16** (now Backlog) — promotion-triggered today; covers
  the test-stability investigation. ~3-4 h.
- **TASK-19** (multiblock powered-cycle trio — Terraformer / BHG /
  Beacon) — builds directly on TASK-18 infrastructure. ~9-10 h.

TASK-16 is the more disciplined next pick (close the loop on a
fired trigger before adding more coverage).
