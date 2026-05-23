# TASK-08-mixin: Rewrite ASM coremod transformations to Mixin

## Ticket

- Source: TASK-07 close-out follow-up (2026-05-20). Replaces the
  original TASK-08 ("ASM coremod safety net") whose goal was to test
  the existing `IClassTransformer`-based coremod. The project already
  carries a Mixin runtime (MixinBooter, `mixins.advancedrocketry.json`
  with 3 active mixins). Rewriting the 5 active ASM transformations to
  Mixin is cheaper than building bytecode-level tests and gives us
  compile-time target validation + fail-loud apply errors for free.
- Status: ✅ Completed — see `.agent/tasks/README.md` Done table.
- Created: 2026-05-20
- Predecessor: `.agent/.context-markers/2026-05-20-2330_task07-fully-closed.md`

## Context

`src/main/java/zmaster587/advancedRocketry/asm/ClassTransformer.java`
is **835 LoC**, of which ~410 LoC is commented-out legacy 1.7.10
gravity-rotation code. The **5 active transformations** are:

| # | Target | Method | Action |
|---|--------|--------|--------|
| 1 | `RenderGlobal` | `setupTerrain` | Strip a null-check branch — **guarded by Forge < 14.23.2.2642**; current is 14.23.5.2860, code path is dead. |
| 2 | `EntityPlayerMP` | `onUpdate` | Insert early-out via `RocketInventoryHelper.allowAccess(this)` — skip the inventory-distance check when a rocket GUI is open. |
| 3 | `EntityPlayer` | `onUpdate` | Same hook, client-side. |
| 4 | `Entity` + `EntityFallingBlock` + `EntityMinecart` + `EntityTNTPrimed` | `onUpdate` | Call `GravityHandler.applyGravity(this)` at the start. Necessary because none of these three subclasses call `super.onUpdate` and the `Entity` version of the hook would not propagate. |
| 5 | `World` | `setBlockState(BlockPos, IBlockState, int)` | Before the return, call `AtmosphereHandler.onBlockChange(this, pos)`. |

The `repack/gloomyfolken/hooklib/` directory (24 vendored Java files
+ `methods.bin`) is dead weight: **zero project-level `@Hook`
annotations** exist. HookLib is wired only via
`SecondaryTransformerHook` which registers its own transformer — but
nothing in our code provides hook containers for it to process.

Mixin infrastructure is already wired:
- `AdvancedRocketryPlugin` calls `MixinBootstrap.init()` and adds the
  config programmatically (also via manifest in packaged jars).
- 3 mixins are active: `AccessorWorld`, `MixinWorldServerMulti`,
  `MixinPlayerList`. The pattern works.

**No behavioural changes** — every replacement must be observably
equivalent to the ASM version. The bar is "modpack saves still load,
gravity still applies, rocket inventories still open at distance,
atmosphere still recomputes on block change".

## Implementation Plan

### Phase 1: Mixins for the 5 active transformations (~3-4 h)

**1.1 — `MixinEntityGravity` (×4, one per target subclass)**

Four parallel mixins because vanilla `Entity` subclasses
(`EntityFallingBlock`, `EntityMinecart`, `EntityTNTPrimed`) override
`onUpdate` without calling `super.onUpdate`. The base `Entity.onUpdate`
hook must also be applied (covers entities that DO call super, and
`EntityItem`).

```java
@Mixin(Entity.class)
public class MixinEntityGravity {
    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void ar_applyGravity(CallbackInfo ci) {
        GravityHandler.applyGravity((Entity) (Object) this);
    }
}
```

Plus identical mixins for the 3 subclasses. (Consider a single
multi-target mixin with `@Mixin({Entity.class, EntityFallingBlock.class,
EntityMinecart.class, EntityTNTPrimed.class})` if the targets share no
type-bound logic — they don't here, so it should work.)

**1.2 — `MixinWorldSetBlockState`**

```java
@Mixin(World.class)
public class MixinWorldSetBlockState {
    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;"
        + "Lnet/minecraft/block/state/IBlockState;I)Z",
        at = @At("RETURN"))
    private void ar_notifyAtmosphere(BlockPos pos, IBlockState state,
                                     int flags, CallbackInfoReturnable<Boolean> cir) {
        AtmosphereHandler.onBlockChange((World) (Object) this, pos);
    }
}
```

**1.3 — `MixinPlayerInventoryAccess` (×2: server + client)**

The hardest of the five. The ASM injection uses an `IFEQ` jump past
the distance-check block — meaning the hook runs `RocketInventoryHelper
.allowAccess` first, and if true, jumps past the
`canPlayerUseChest`-style guard to whatever comes next. Two viable Mixin
approaches:

- **Option A (preferred)** — `@Redirect` of the specific vanilla call
  inside `EntityPlayer.onUpdate` / `EntityPlayerMP.onUpdate` that
  checks inventory distance. Need to disassemble vanilla to identify
  the exact call (`Container.canInteractWith`, `InventoryPlayer
  .isUsableByPlayer`, or a `getDistance(...) < 64` comparison).
  Then return `true` when `allowAccess` says yes.

  ```java
  @Mixin(EntityPlayer.class)
  public class MixinEntityPlayerInventoryAccess {
      @Redirect(method = "onUpdate",
          at = @At(value = "INVOKEVIRTUAL", target =
              "Lnet/minecraft/inventory/Container;canInteractWith"
              + "(Lnet/minecraft/entity/player/EntityPlayer;)Z"))
      private boolean ar_bypassForRocketGui(Container c, EntityPlayer p) {
          return RocketInventoryHelper.allowAccess(p) || c.canInteractWith(p);
      }
  }
  ```

- **Option B (fallback)** — `@Inject(cancellable = true)` at a specific
  expression-offset; less stable across Forge mapping shifts.

Phase 1.3 starts with a 30-min spike to dump vanilla `onUpdate`
bytecode (`javap -c` on the deobf class) to confirm the right Mixin
target.

**Output of Phase 1:** 5-6 new mixin files in
`src/main/java/zmaster587/advancedRocketry/mixin/`, added to
`mixins.advancedrocketry.json`.

### Phase 2: Strip dead ASM + HookLib (~2 h)

After Phase 1 is green:

- Delete `asm/ClassTransformer.java` (835 LoC).
- Simplify `asm/AdvancedRocketryPlugin.java`:
  - drop `hookLoader` field + import.
  - `getASMTransformerClass()` returns `new String[]{}` (or remove
    override entirely; default is empty).
  - drop `getSetupClass`, `injectData`, `getAccessTransformerClass`
    overrides — they all delegated to `hookLoader`.
- Delete `src/main/java/zmaster587/advancedRocketry/ARHookLoader.java`
  (check no other references first via grep).
- Delete `src/main/java/zmaster587/advancedRocketry/repack/gloomyfolken/`
  (24 vendored HookLib files).
- Delete `src/main/resources/methods.bin` (HookLib's obfuscation map).
- Keep `asm/ModContainer.java` — `IFMLLoadingPlugin
  .getModContainerClass()` still needs it.

**Pre-delete grep checks** (must return only matches inside the files
being deleted):
- `grep -r "ARHookLoader" src/`
- `grep -r "repack.gloomyfolken" src/`
- `grep -r "methods\.bin" src/`
- `grep -r "@Hook[^L]" src/` — must return zero hits outside the
  vendored library itself.

### Phase 3: Behavioural pin tests (~3 h)

Replaces the rejected "ASM-test-everything" goal of the original
TASK-08 with **integration tests for the 4 hook points** that survive
the rewrite. Each test exercises the production path that the mixin
hooks into, not the mixin itself (mixin AP already statically verifies
the target resolves at apply time, and a failed apply hard-fails
startup with a logged error — silent regression is not possible the
way it was with ASM).

- **`GravityHookFiresForEntityItemUnderRealTicks`** — spawn
  `EntityItem` in an AR dim with `getGravitationalMultiplier=2.0`,
  chunk-forceload, `server wait 5` → motionY has decreased measurably
  more than under default gravity. Pin via a probe that exposes the
  delta. Also `EntityFallingBlock` (separate test — that one's hook
  is in a different mixin), `EntityMinecart`, `EntityTNTPrimed`.
- **`SetBlockStateHookFiresAtmosphereOnBlockChange`** —
  `AtmosphereHandler.onBlockChange` adds a counter probe; `world
  .setBlockState(pos, AIR)` near an atmosphere boundary advances the
  counter exactly once.
- **`RocketInventoryAllowAccessHookBypassesDistanceCheck`** — open a
  rocket inventory GUI, push the rocket entity 100 blocks away from the
  player, assert the GUI stays open (no distance-driven close). This
  one is likely a testClient e2e test (needs real EntityPlayer) — pin
  in TASK-10b list if it can't land in testServer.
- **`DeadAsmRenderGlobalTransformDoesNotImpactRender`** — counter-test:
  before/after rewrite, a `runClient` smoke load completes without
  throwing. Belongs in testClient (existing
  `ClientConnectSmokeTest`).

### Phase 4: Validation (~2 h)

- Full pyramid PASS.
- `runClient` smoke: launch a real client briefly, look for
  `[mixin] Mixing zmaster587.advancedRocketry.mixin.MixinEntityGravity
  into net.minecraft.entity.Entity` in the log, look for zero
  `[mixin/WARN]` lines about failed targets.
- `runServer` smoke: world load + place falling block + tick →
  gravity applies, no exceptions.
- Open a rocket inventory in dev, walk 70+ blocks away → confirm GUI
  doesn't close.

### Phase 5: Docs + EOD (~1 h)

- `.agent/system/project-architecture.md` — update bytecode-patching
  section: ASM coremod removed; Mixin is the only patching path.
- `CLAUDE.md` — drop ASM-related cautionary notes if present.
- `tasks/README.md` — flip TASK-08 to ✅, note the rewrite scope and
  the deferred player-inventory-distance test pointer.
- EOD marker in `.agent/.context-markers/`.

## Technical Decisions

- **Why Mixin over keeping ASM**: compile-time target validation (Mixin
  AP), fail-loud at apply time, readability (`@Inject(method =
  "onUpdate")` vs hand-walking `IFEQ` offsets), already in the project.
- **Why not refmap-only switch**: that just renames obfuscated calls;
  the IClassTransformer plumbing is the maintenance burden, not the
  mappings.
- **Why delete vendored HookLib**: zero project-level `@Hook`
  annotations means the library has no consumers. Its only side effect
  is `SecondaryTransformerHook` registering a no-op transformer chain.
- **Behaviour-preservation bar**: every mixin must be observably
  equivalent to the ASM hook it replaces. Phase 3 tests pin the
  observable effect of each surviving hook so any future bug-for-bug
  divergence surfaces.
- **No "no production logic changes" rule**: the production source IS
  the asm coremod here, and the goal is to rewrite it. Behavioural
  output must match; internal mechanism is fair game.

## Dependencies

**Requires**: existing Mixin setup (MixinBooter, `mixins.advancedrocketry.json`).
**Does NOT block**: other test work — the project still builds and runs
during the rewrite as long as Phase 1 lands before Phase 2.

**Cross-references**:
- `RocketInventoryGuiOpenSkipsDistanceCheck` likely belongs in TASK-10b
  (testClient e2e) — the EntityPlayer-touching part of Phase 3.

## Risks

1. **EntityPlayer.onUpdate Mixin target fragility** — the IFEQ jump in
   the ASM version pinned a specific instruction; the Mixin equivalent
   needs to pin a specific vanilla call. If the target call name
   shifts between MCP mapping snapshots, the mixin fails-loud (good)
   but needs a manual fix. Mitigation: use `@Redirect` of a
   semantically-stable target (`Container.canInteractWith` exists
   across all 1.12.2 mapping snapshots).
2. **Modpack compatibility** — if another mod's coremod patches the
   same methods, Mixin priority management may need tuning. Existing
   `MixinPlayerList` already coexists with other mods successfully —
   pattern is known-working.
3. **HookLib removal regression** — extremely unlikely (vendored,
   project-private prefix) but check via grep for any external
   reference before deletion.
4. **`methods.bin`** — verify it's only referenced by HookLib code
   before deletion.
5. **Lost test coverage from cancelling original TASK-08** — Phase 3
   substitutes behavioural pins; we no longer test "the ASM
   transformer ran". This is OK because Mixin replaces the
   silent-no-op failure mode with hard apply errors.

## Estimated effort

~11 h on the rewrite + ~3 h on Phase 3 behavioural pins = **~14 h**
across 4-5 sessions.

## Completion Checklist

- [ ] Phase 1: 5-6 new mixin classes covering the 4 surviving hook
      points, added to `mixins.advancedrocketry.json`. All mixins
      apply cleanly at runtime (no `[mixin/WARN]` lines).
- [ ] Phase 2: `ClassTransformer.java`, `ARHookLoader.java`,
      `repack/gloomyfolken/`, `methods.bin` deleted.
      `AdvancedRocketryPlugin` reduced to ~25 LoC.
- [ ] Phase 3: ≥4 behavioural integration tests covering each
      surviving hook. Player-distance-bypass test deferred to
      TASK-10b if testServer can't host it.
- [ ] Phase 4: full pyramid PASS; `runClient` + `runServer` smoke
      clean.
- [ ] Phase 5: `system/project-architecture.md` updated; `tasks/README.md`
      backlog flipped; EOD marker shipped.
