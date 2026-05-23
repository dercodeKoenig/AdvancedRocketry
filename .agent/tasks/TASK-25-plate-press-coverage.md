# TASK-25: PlatePress recipe coverage (single-block redstone-triggered)

## Ticket

- Source: TASK-18 scope split (2026-05-23). PlatePress was
  originally listed alongside the 9 multiblock industrial machines
  but has a fundamentally different test shape and was deferred
  here.
- Status: ✅ **Completed 2026-05-23**.
- Created: 2026-05-23.

## Context

`BlockSmallPlatePress` (`zmaster587.advancedRocketry.block.BlockSmallPlatePress`)
is a `BlockPistonBase` subclass — a single block, not a
multiblock. The "fill hatch → inject energy → force-tick → read
hatch" pattern TASK-18 used for the other 9 industrial machines
does not apply because PlatePress:

- has no input / output / power hatches;
- has no `RF` energy input — runs on redstone activation
  (`isSidePowered`);
- runs instantly on redstone trigger, not on `force-tick`;
- outputs an `EntityItem` spawn adjacent to the press, not into
  an output hatch slot;
- registers its recipes against `BlockSmallPlatePress.class` —
  the existing `recipe-info` probe accepts only tile-class FQNs
  under `tile.multiblock.machine.*`.

Player-visible contract: with obsidian below, ingredient block in
the middle, PlatePress on top, and a redstone signal — the
ingredient block should be consumed and the recipe output should
appear as an `EntityItem` next to the press.

## Probe surface needed

Two new probe verbs (or extensions):

1. `/artest fixture machine plate-press <dim> <x> <y> <z>` —
   places the 3-block stack: obsidian at y-1, ingredient block
   (first recipe ingredient) at y, PlatePress at y+1.
2. `/artest recipe-info-block <FQN> [recipeIndex]` — same shape
   as the existing `recipe-info` but accepts an arbitrary class
   FQN instead of restricting to the `tile.multiblock.machine.*`
   package. (Or: add a flag to `recipe-info` for raw FQN.)
3. `/artest entityitem-scan <dim> <x> <y> <z> <radius>` — scan
   for `EntityItem` instances within a radius and report the
   first match (item registry name, count, position). Trigger
   is the redstone pulse from a neighbouring block.

PlatePress is activated by redstone — the existing
`/artest place 0 X Y Z minecraft:redstone_block` adjacent to the
press should drive the activation. Verify in Phase 0.

## Implementation plan

| Phase | Effort | Result |
|---|---|---|
| 0 | ~30 min | Probe verbs added: `fixture machine plate-press`, `recipe-info-block`, `entityitem-scan`. Verify redstone trigger path. |
| 1 | ~1 h | `PlatePressRecipeEndToEndTest` — 3 tests: fixture validates, ingredient block resolves, redstone pulse drops expected output `EntityItem`. |
| 2 | ~30 min | Close-out: pyramid counter regen, README sync, marker, commit. |
| **Total** | **~2 h** | |

## Acceptance

- [x] `PlatePressRecipeEndToEndTest` exists with **2** tests (see
      "Actual scope" below — TASK-18 / TASK-26 settled on 2-tests-per-class
      shape; the originally-proposed 3rd test was an impl-pin per the
      `testing-principles` SOP).
- [x] Test uses `RecipesMachine.getInstance().getRecipes(BlockSmallPlatePress.class)`
      for recipe discovery (reflectively from the probe) — no hardcoded
      ingredients/outputs.
- [x] Test asserts only player-visible contract (3-block fixture stack
      built + EntityItem with recipe output spawns next to press +
      ingredient block consumed). The transient `piston_extension`
      state is tolerated — assert is "no longer the ingredient block",
      not "specifically AIR".
- [x] testServer green for the 2 PlatePress tests in isolation.
- [x] Pyramid counter regenerated per task-lifecycle step 2.5:
      237 / 80 / 339 / 41 = **697** (was 695 after TASK-26, +2 from this task).

## Actual scope (shipped)

### Probe extensions (3)

- `/artest fixture machine plate-press <dim> <x> <y> <z>` — new
  branch in the `fixture machine` dispatch. Resolves the first
  recipe from `RecipesMachine.getInstance().getRecipes(BlockSmallPlatePress.class)`,
  picks the first ingredient alternative, places obsidian at y-2,
  the resolved ingredient block at y-1, the PlatePress at y
  (FACING=DOWN, EXTENDED=false), pre-clears the column + 4 adjacent
  redstone slots, and returns press / ingredient / obsidian
  positions plus the resolved ingredient + output registry names.
- `/artest machine recipe-info-block <FQN> [recipeIndex]` — new
  branch in `handleMachine`. Same shape as the existing `recipe-info`
  but takes an arbitrary class FQN (used by tests outside the
  `tile.multiblock.machine.*` package). Not consumed by the
  PlatePress test directly — the fixture verb already does the
  reflective lookup — but added for completeness so future
  block-class-keyed machines have a discovery surface.
- `/artest entity scan-items <dim> <cx> <cy> <cz> <radius>` — new
  branch in `handleEntity`. Reports every `EntityItem` inside a box
  around the given centre, each as `{item, count, meta, posX, posY,
  posZ}`. The end-to-end test asserts a single match for the recipe
  output's registry name.

### Test (1 class, 2 @Tests)

- `PlatePressRecipeEndToEndTest.platePressFixtureBuildsExpectedStack`
  — reads the 3-block stack via `block at` and asserts each cell
  has the right block id.
- `PlatePressRecipeEndToEndTest.platePressRedstoneActivationDropsRecipeOutput`
  — places fixture, places `minecraft:redstone_block` adjacent above
  the press, scans for EntityItem with the recipe's output id within
  2 blocks of the spawn point, asserts ingredient block no longer
  matches the original id.

### Activation path chosen

`minecraft:redstone_block` placed at `press.up()`. The redstone
block emits weak power 15 on all sides; `setBlockState` fires
`neighborChanged` on the press synchronously, which runs
`checkForMove` → `shouldBeExtended()` returns true → the press
spawns the EntityItem and clears the ingredient. The flow is
fully synchronous within `setBlockState`, so no force-tick is
needed.

## Result

- 2 new server-tier @Tests; pyramid 695 → 697.
- 3 new probe verbs (one used directly by the test, two banked
  for future block-class-keyed machines).
- No production logic changes. No production bugs surfaced.

## Out of scope

- Multiple recipes per press cycle (single recipe is enough to
  prove the integration shape).
- The piston-extension `EXTENDED` state machine — that's libVulpes
  / vanilla territory and pinning it would be impl-tied.
- Per-tick timing pins.

## Dependencies

- Depends on TASK-18 closing (which establishes the per-recipe
  end-to-end pattern that this borrows shape from, even though
  the activation path differs).

## Estimated effort

~2 h single session.
