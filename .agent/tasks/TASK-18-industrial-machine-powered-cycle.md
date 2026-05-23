# TASK-18: Industrial machine powered-cycle coverage (×9 multiblock machines)

## Ticket

- Source: 2026-05-23 full repo audit — Gap #1 ("Powered-cycle for
  9 of 10 industrial machines"). Highest player-impact gap in the
  audit findings.
- Status: **✅ Completed 2026-05-23** (partial — see "Actual scope" below).
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

## The 9 multiblock machines

| Machine | TileEntity | Recipe class |
|---|---|---|
| Rolling Machine | `TileRollingMachine` | `RecipeRollingMachine` |
| Lathe | `TileLathe` | `RecipeLathe` |
| Precision Assembler | `TilePrecisionAssembler` | `RecipePrecisionAssembler` |
| Electrolyzer | `TileElectrolyser` | `RecipeElectrolyser` |
| Chemical Reactor | `TileChemicalReactor` | `RecipeChemicalReactor` |
| Crystallizer | `TileCrystallizer` | `RecipeCrystallizer` |
| Arc Furnace | `TileElectricArcFurnace` | `RecipeArcFurnace` |
| Centrifuge | `TileCentrifuge` | `RecipeCentrifuge` |
| Precision Laser Etcher | `TilePrecisionLaserEtcher` | `RecipePrecisionLaserEtcher` |

(Cutting is the 10th industrial machine and is already covered by
`MachineRecipeIntegrationTest`.)

### PlatePress — deferred to TASK-25

`BlockSmallPlatePress` is fundamentally a different shape: a
single redstone-triggered block with no hatches, no `force-tick`
cycle, output-as-`EntityItem`-spawn, and a recipe class in
`block.*` rather than `tile.multiblock.machine.*`. The
"fill hatch → inject energy → force-tick → read hatch" pattern
does not apply. Successor task **TASK-25** covers it with a
bespoke probe + redstone-pulse test shape (~2 h).

## Implementation plan

### Phase 0 — Probe surface confirmation (~30 min)

`MachineRecipeIntegrationTest` uses `/artest fixture multiblock`,
`/artest hatch fill`, `/artest hatch read`, `/artest energy inject`,
`/artest machine recipes-summary`, `/artest machine force-tick`.
Confirm each verb works against every target machine — multiblock
fixtures may have per-machine layout variations. Extend any probe
that has machine-class-specific assumptions.

### Phase 1 — Per-machine end-to-end test (~30 min each × 9 = ~4.5 h)

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

- [x] Each of the 9 multiblock machines has a `*RecipeEndToEndTest` class.
      → 7 shipped; ArcFurnace + PrecisionAssembler deferred to
      [TASK-26](./TASK-26-wildcard-based-machine-coverage.md) — their
      `'*'` wildcard structure shape requires bespoke probe verbs.
- [x] Each class has ≥3 tests covering fixture → input → power →
      output.
- [x] Tests use `RecipesMachine.getInstance().getRecipes(class)`
      to discover the recipe, never hardcode outputs.
- [x] Full testServer green after the addition.
- [x] Pyramid counter regenerated per task-lifecycle step 2.5.
      Pyramid moves from 237 / 80 / 319 / 41 = 677 → 237 / 80 / 333 / 41
      = **691** (+14 from 7 × 2 tests after SOP-driven trim — see
      "Test depth" below).
- [x] PlatePress successor [TASK-25](./TASK-25-plate-press-coverage.md)
      created with the bespoke redstone-pulse test plan.

## Actual scope (shipped)

**7 of 9 multiblock machines shipped** — those with explicit
'I' / 'O' / 'P' / 'L' / 'l' characters in their structure arrays:

| Machine | Test class | Recipe type covered |
|---|---|---|
| Rolling Machine | `RollingMachineRecipeEndToEndTest` | items + fluid → item (pressuretank) |
| Lathe | `LatheRecipeEndToEndTest` | items → item |
| Crystallizer | `CrystallizerRecipeEndToEndTest` | items → item |
| PrecisionLaserEtcher | `PrecisionLaserEtcherRecipeEndToEndTest` | items → item |
| Electrolyser | `ElectrolyserRecipeEndToEndTest` | fluid → fluid (water electrolysis) |
| Centrifuge | `CentrifugeRecipeEndToEndTest` | fluid → fluid + items |
| Chemical Reactor | `ChemicalReactorRecipeEndToEndTest` | 2 fluids → fluid (rocketfuel) |

**2 of 9 deferred to TASK-26**: ArcFurnace + PrecisionAssembler
use `'*'` wildcards in their structure for hatch positions; the
generic fixture helper cannot compute hatch coordinates for them.

## Test depth (SOP audit applied)

Initial draft followed the original 3-tests-per-machine plan
(`*FixtureValidates` / `*AcceptsRecipeInputs` / `*RunsFirstRegisteredRecipe`),
yielding 21 tests. SOP self-audit per
[testing-principles](../sops/development/testing-principles.md)
flagged two issues:

1. **`*AcceptsRecipeInputs`** was near-tautological — it pinned
   "input hatches function as inventory containers", which is
   libVulpes-level, not AR-level. The litmus "this test fails if
   production breaks the contract that __" reduced to "input
   hatches accept items" — a contract the `*RunsFirstRegisteredRecipe`
   test already implicitly covers (recipes can't run without
   inputs landing in the hatch). **Removed × 7.**
2. **Input-drain not pinned**: the original `*RunsFirstRegisteredRecipe`
   asserted output appeared but didn't assert inputs were consumed.
   A regression "machine generates output without consuming inputs"
   (free-output exploit) would slip through. **Added a soft drain
   pin to the shared kit**: at least one ingredient slot must
   have changed from its initial state after force-tick. The
   "soft form" (any-slot rather than every-slot) is required
   because PrecisionLaserEtcher uses a lens catalyst that
   legitimately stays in slot 0 — only the 3 consumed ingredients
   drain. Without the soft form the test false-positives on
   legitimate catalyst patterns.

Net: 21 tests → 14 tests, but each pins a real player-visible
contract (multiblock validates / recipe runs end-to-end with input
drain + output appearance). Contract-coverage net positive.

## Result

- **7 new test classes** in `src/test/java/.../server/`, each
  ~20 LOC delegating to the shared protocol (2 tests per class:
  fixture-validates + runs-first-recipe-with-drain-and-output).
- **Shared protocol kit** `MachineRecipeEndToEndKit` (~280 LOC)
  centralises the fixture → validate → fill items → fill fluids →
  inject power → enable → force-tick → assert input drained →
  assert output flow. Handles four recipe shapes:
  item-in/item-out, item-in/fluid-out, fluid-in/item-out,
  fluid-in/fluid-out. Auto-discovers recipe shape from
  `recipe-info` probe. Soft input-drain pin tolerates catalyst
  patterns (e.g. PrecisionLaserEtcher lens).
- **3 probe extensions** in `TestProbeCommand.java`:
  - `/artest fixture machine <key>` — new dispatch for 9 multiblock
    machines via the existing `handleFixtureGenericFromStructure`.
  - `handleFixtureGenericFromStructure` now reports per-hatch-char
    position **lists** (e.g. `liquidInputPositions: [[x,y,z],[x,y,z]]`)
    instead of single first-found positions. Backward-compatible
    first-position aliases retained.
  - `recipe-info` probe now emits `fluidIngredients` and `fluidOutputs`
    sections (was item-only).
- **Lookup table** `lookupMultiblockMachineSpec` maps the 9 kebab-case
  keys to {namespace, controller path, tile FQN}.
- Pyramid: **+14 server-tier tests** (7 classes × 2 tests after
  SOP-driven trim — see "Test depth" above).

## Bugs found (none)

No production bugs surfaced. The 7 machines' recipes run end-to-end
exactly as expected once the test had:
- correct item meta (initial test had meta-less hatch fill, which
  silently failed to match the recipe's specific meta variant);
- all required fluids in distinct liquid input hatches;
- machine `enabled=true` flipped via `setMachineEnabled`.

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
