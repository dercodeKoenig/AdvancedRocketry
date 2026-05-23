# TASK-26: Wildcard-based machine recipe coverage (ArcFurnace + PrecisionAssembler)

## Ticket

- Source: TASK-18 scope split (2026-05-23). Two of the 9 multiblock
  industrial machines could not be covered by the generic
  fixture-from-structure helper because their structures use
  {@code '*'} wildcards for hatch positions rather than explicit
  'I'/'O'/'P' chars.
- Status: ✅ **Completed 2026-05-23**.
- Created: 2026-05-23.

## Context

`TileElectricArcFurnace` and `TilePrecisionAssembler` define
multiblock structures with `'*'` wildcards in the cells where
hatches normally go. The structure validator's
`getAllowableWildCardBlocks()` returns
`{structureBlock, 'I' mapping, 'O' mapping, 'L' mapping, 'l' mapping}`
— so a wildcard accepts ANY of those blocks. This means hatch
positions are not fixed at compile time; the player can place
input/output/power hatches at any wildcard cell.

TASK-18's `handleFixtureGenericFromStructure` scans the structure
array for explicit 'I'/'O'/'P' chars and emits their positions in
the response. For wildcard machines, the scan finds none of these
(every wildcard resolves to AIR via the generic helper) — so the
test has no way to know where to fill items / inject power.

TASK-18's `MachineRecipeEndToEndKit` handles 7 of the 9 multiblock
machines this way. ArcFurnace and PrecisionAssembler were left out;
this task covers them.

## Implementation plan

| Phase | Effort | Result |
|---|---|---|
| 0 | ~30 min | Bespoke fixture probe verbs (`/artest fixture machine arc-furnace`, `precision-assembler`) that place the structure AND drop hatches at chosen wildcard positions. Each verb hand-picks 3 wildcard cells for I, O, P. |
| 1 | ~1 h | `ArcFurnaceRecipeEndToEndTest` + `PrecisionAssemblerRecipeEndToEndTest` reuse TASK-18's `MachineRecipeEndToEndKit` once the probe emits inputPos/outputPos/powerPos like the others. |
| 2 | ~30 min | Close-out: pyramid counter regen, README sync, marker, commit. |
| **Total** | **~2 h** | |

### Phase 0 design

For each wildcard machine, hand-author a fixture handler that:

1. Calls the generic structure placement (places base blocks, AIR
   at wildcards).
2. Picks 3 specific wildcard cells (chosen for natural ergonomics:
   input on left, output on right, power adjacent to controller).
3. Overwrites those cells with concrete `libvulpes:hatch` (meta 0
   for input, meta 1 for output) and `libvulpes:forgepowerinput`.
4. Returns response with inputPos/outputPos/powerPos like the
   generic helper.

Alternative: add a generic `placeHatch` pass after `handleFixtureGenericFromStructure`
that takes a list of `(role, x, y, z)` triples. Less code duplication.

## Acceptance

- [x] `ArcFurnaceRecipeEndToEndTest` exists with 2 tests; both pass in
      isolation. (TASK-18 self-audit dropped the middle
      `*AcceptsRecipeInputs` test as impl-pin — same 2-tests-per-class
      shape applies here. The acceptance row originally said "3" by
      mistake — superseded.)
- [x] `PrecisionAssemblerRecipeEndToEndTest` exists with 2 tests; both
      pass in isolation.
- [x] Tests reuse `MachineRecipeEndToEndKit`. Kit gained one adaptive
      hook — `time` field on `FirstRecipe` + adaptive `tickBudget =
      max(2000, recipe.time + 1000)` — to accommodate longer recipes
      (ArcFurnace first = 6000 ticks; PrecisionAssembler first = 4000).
      The 7 TASK-18 machines remain on the 2000-tick floor.
- [x] testServer green for the 9 RecipeEndToEnd classes when run in
      isolation. Two intermittent failures observed when running the
      full RecipeEndToEnd group (ArcFurnace fixture-validates and
      RollingMachine fixture-validates) — passed on re-run with no
      source changes, same `attempted:false` shape as the existing
      TASK-16 flake list. Logged into TASK-16 recurrence table.
- [x] Pyramid counter regenerated per task-lifecycle step 2.5:
      237 / 80 / 337 / 41 = **695** (was 691, +4 from this task).

## Actual scope (shipped)

### Probe extensions (1 refactor + 1 dispatch hook)

- `lookupWildcardMachineOverrides(key)` (new) — returns a
  `WildcardConfig` per kebab-case machine key. Each config carries
  (a) per-cell hatch overlays (libVulpes char + structure-space y,z,x);
  (b) a filler `Block` for every remaining `'*'` cell.
- `handleFixtureGenericFromStructure` (refactored) — gained a
  `WildcardConfig wildcardConfig` trailing parameter. Three existing
  call sites updated to pass `null` (terraformer, orbital-laser-drill,
  generic machine path). For non-null configs, after the regular
  placement loop the helper iterates every `'*'` cell and either
  overlays a hatch (per the override) or places the filler block. The
  resulting hatch positions are merged into the response's
  `inputPositions`/`outputPositions`/`powerPositions` lists so the
  test-side kit consumes them unchanged.

### Kit extension (`MachineRecipeEndToEndKit`)

- `FirstRecipe.time` field, parsed from `recipe-info`'s existing
  `"time":N` JSON section (no probe-side change needed — TASK-18
  already emits this).
- `runFirstRecipeEndToEnd` computes `tickBudget = max(2000, r.time +
  1000)` instead of a hardcoded 2000.

### Tests (2 classes, 4 @Tests)

- `ArcFurnaceRecipeEndToEndTest` (`arc-furnace`, `TileElectricArcFurnace`)
- `PrecisionAssemblerRecipeEndToEndTest` (`precision-assembler`,
  `TilePrecisionAssembler`)

Each: `*FixtureValidates` + `*RunsFirstRegisteredRecipe`.

### Wildcard layout chosen

ArcFurnace — wildcards on the y=3 ring; structure already declares
three explicit `'P'` chars at y=0. Overlay only I+O:
- I at structure[3][4][1] (back-left of base ring)
- O at structure[3][4][3] (back-right of base ring)
- All other y=3 wildcards filled with `blockBlastBrick` (the structure
  block listed last in `TileElectricArcFurnace.getAllowableWildCardBlocks`).

PrecisionAssembler — no explicit hatch chars; overlay all 3 roles
on the front-row wildcards at y=2:
- I at structure[2][0][1]
- O at structure[2][0][2]
- P at structure[2][0][3]
- All other y=2 wildcards filled with `LibVulpesBlocks.blockStructureBlock`
  (added with WILDCARD meta in
  `TilePrecisionAssembler.getAllowableWildCardBlocks`).

## Result

- 4 new server-tier @Tests; pyramid 691 → 695.
- 1 helper refactor (param + filler logic, ~40 LOC).
- 1 kit hook (adaptive tick budget, ~6 LOC).
- All 9 RecipeEndToEnd classes (TASK-18's 7 + this task's 2) pass in
  isolation and pass together when re-run after a flake (TASK-16
  pattern).
- No production logic changes. No production bugs surfaced.

## Out of scope

- Recipe coverage beyond the first registered recipe.
- Per-wildcard exhaustive placement testing (each machine has
  many wildcards — pick a single canonical layout).
- BlockSmallPlatePress — see [TASK-25](./TASK-25-plate-press-coverage.md).

## Dependencies

- Builds on TASK-18's probe extensions (`fixture machine <key>`,
  `recipe-info` with `fluidIngredients` + `fluidOutputs`).
- Reuses `MachineRecipeEndToEndKit`.

## Estimated effort

~2 h single session.
