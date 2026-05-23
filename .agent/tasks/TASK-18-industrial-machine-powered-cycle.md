# TASK-18: Industrial machine powered-cycle coverage (×9 machines)

## Ticket

- Source: 2026-05-23 full repo audit — Gap #1 ("Powered-cycle for
  9 of 10 industrial machines"). Highest player-impact gap in the
  audit findings.
- Status: **Backlog**.
- Created: 2026-05-23.

## Context

Only **one** of AR's 10 industrial machines has end-to-end
powered-cycle coverage:

- `TileCuttingMachine` — pinned by `MachineRecipeIntegrationTest`
  (3 tests: fixture build, recipe-info, hatch fill, energy inject,
  force-tick, hatch read).

The other 9 have:
- ✅ Structure-validation coverage (via per-machine
  `*MultiblockTest` + `MultiblockControllerPreAssemblyTest`).
- ✅ Class FQN + capability surface coverage (via
  `TileMachineDepthTest` / `Round2`).
- ❌ **No** "fuel + recipe → output appears in output hatch"
  end-to-end.

Player-visible regression class this gap allows: a recipe-system
change silently breaks one specific machine's recipe path without
breaking the fixture validation OR the unit-tier registry binding.

## The 9 machines

| Machine | TileEntity | Recipe class |
|---|---|---|
| Rolling Machine | `TileRollingMachine` | `RecipeRollingMachine` |
| Lathe | `TileLathe` | `RecipeLathe` |
| Precision Assembler | `TilePrecisionAssembler` | `RecipePrecisionAssembler` |
| Electrolyzer | `TileElectrolyser` | `RecipeElectrolyser` |
| Chemical Reactor | `TileChemicalReactor` | `RecipeChemicalReactor` |
| Crystallizer | `TileCrystallizer` | `RecipeCrystallizer` |
| Arc Furnace | `TileElectricArcFurnace` | `RecipeArcFurnace` |
| Plate Press | `BlockSmallPlatePress` (block-form) | `RecipePlatePress` |
| Centrifuge | `TileCentrifuge` | `RecipeCentrifuge` |
| Precision Laser Etcher | `TilePrecisionLaserEtcher` | `RecipePrecisionLaserEtcher` |

(That's actually 10 with PlatePress — Cutting is the 11th and
already covered. 9 industrial + Plate Press = 10 uncovered.)

## Implementation plan

### Phase 0 — Probe surface confirmation (~30 min)

`MachineRecipeIntegrationTest` uses `/artest fixture multiblock`,
`/artest hatch fill`, `/artest hatch read`, `/artest energy inject`,
`/artest machine recipes-summary`, `/artest machine force-tick`.
Confirm each verb works against every target machine — multiblock
fixtures may have per-machine layout variations. Extend any probe
that has machine-class-specific assumptions.

### Phase 1 — Per-machine end-to-end test (~30 min each × 10 = ~5 h)

Single test class per machine, ~3 tests each, all extending
`AbstractSharedServerTest` for one cold-start amortisation:

For each `MACHINE`:

- `MACHINERecipeEndToEndTest`:
  - `MACHINEFixtureValidatesWithStandardLayout` (already cross-cut
    via MultiblockControllerPreAssemblyTest, but assert via this
    suite for self-contained reproduction)
  - `MACHINEAcceptsKnownRecipeInputs` — hatch fill with first
    registered recipe inputs; assert recipe-info echo
  - `MACHINERunsFirstRegisteredRecipe` — energy inject + force-tick;
    assert output hatch contains expected output

Recipe selection: pick the first recipe registered for each
machine via `RecipesMachine.getInstance().getRecipes(machineClass)`.
Tests must NOT hardcode specific recipe outputs — read the
expected output from the recipe object itself, then assert hatch
contents match. That keeps the test robust to recipe-edit changes.

### Phase 2 — Consolidate or split (~30 min)

After Phase 1, decide whether to keep 10 separate classes (one per
machine, ~30 tests total) OR consolidate into a single
`IndustrialMachineRecipeEndToEndSuite` parameterised over the
machine list. The trade-off:

- 10 separate classes — each independently filterable by
  `--tests`, but cold-start cost (mitigated by
  `AbstractSharedServerTest` shared harness).
- 1 parameterised suite — cleaner shape, but JUnit 4 parameterised
  tests don't always play well with `AbstractSharedServerTest`'s
  `@BeforeClass` lifecycle.

Default: 10 separate classes. Reconsider only if test wall-time
becomes a problem.

## Acceptance

- [ ] Each of the 10 machines has a `*RecipeEndToEndTest` class.
- [ ] Each class has ≥3 tests covering fixture → input → power →
      output.
- [ ] Tests use `RecipesMachine.getInstance().getRecipes(class)`
      to discover the recipe, never hardcode outputs.
- [ ] Full testServer green after the addition.
- [ ] Pyramid counter regenerated per TASK-17 phase 1.

## Technical decisions

- **No new probe verbs unless one is missing** for a specific
  machine. Reuse `/artest hatch/energy/machine` family.
- **First registered recipe**, not "the one I think is canonical".
  Insulates tests from recipe-list reorderings.
- **One test per machine, not one class for all** — failure
  isolation: one machine breaking shouldn't fail the suite for the
  others.
- **No production logic changes** per CLAUDE.md rule.

## Out of scope

- Per-recipe coverage (each machine has many recipes; pinning the
  first one is enough to verify the integration shape).
- GUI-level interaction (testClient territory; recipe execution
  is server-side).
- Performance pins (recipe time bound is impl per SOP).

## Dependencies

- Does NOT block any other backlog task.
- Pattern source: `MachineRecipeIntegrationTest` for
  `TileCuttingMachine`.

## Estimated effort

- Phase 0 probe confirmation: ~30 min
- Phase 1 (10 machines × ~30 min each): ~5 h
- Phase 2 close-out + pyramid regen + commit: ~30 min
- **Total**: ~6 h

## Player-impact justification

Highest gap-priority in audit (#1). Catches the regression class
"a recipe-system change silently breaks one machine's path" — a
class that the cutting-machine test ALREADY catches for cutting,
proving the contract has real teeth.
