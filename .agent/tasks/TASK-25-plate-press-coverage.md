# TASK-25: PlatePress recipe coverage (single-block redstone-triggered)

## Ticket

- Source: TASK-18 scope split (2026-05-23). PlatePress was
  originally listed alongside the 9 multiblock industrial machines
  but has a fundamentally different test shape and was deferred
  here.
- Status: **Backlog**.
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

- [ ] `PlatePressRecipeEndToEndTest` exists with 3 tests.
- [ ] Test uses `RecipesMachine.getInstance().getRecipes(BlockSmallPlatePress.class)`
      for recipe discovery — no hardcoded ingredients/outputs.
- [ ] Test asserts only player-visible contract (item dropped) —
      no internal-state pins (piston extension state, intermediate
      flag values, exact tick where event fires).
- [ ] Full testServer green.
- [ ] Pyramid counter regenerated per task-lifecycle step 2.5.

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
