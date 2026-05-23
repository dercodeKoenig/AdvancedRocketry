# TASK-26: Wildcard-based machine recipe coverage (ArcFurnace + PrecisionAssembler)

## Ticket

- Source: TASK-18 scope split (2026-05-23). Two of the 9 multiblock
  industrial machines could not be covered by the generic
  fixture-from-structure helper because their structures use
  {@code '*'} wildcards for hatch positions rather than explicit
  'I'/'O'/'P' chars.
- Status: **Backlog**.
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

- [ ] `ArcFurnaceRecipeEndToEndTest` exists with 3 tests; all pass.
- [ ] `PrecisionAssemblerRecipeEndToEndTest` exists with 3 tests; all pass.
- [ ] Tests reuse `MachineRecipeEndToEndKit` from TASK-18 unchanged
      (or with minimal addition).
- [ ] Full testServer green.
- [ ] Pyramid counter regenerated per task-lifecycle step 2.5.

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
