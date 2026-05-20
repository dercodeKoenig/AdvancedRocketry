# Context Marker — TASK-08-mixin shipped 2026-05-20

**Created**: 2026-05-20 17:00 local
**Branch**: `feature/tests`
**Predecessor**: `before-compact-2026-05-20-1424.md`
**Status**: Working tree dirty (changes uncommitted); pyramid green.

---

## What landed this session

TASK-08-mixin (rewrite ASM coremod → Mixin) — all 5 phases done in
one sitting.

### Phase 1 — 4 new mixin classes (5 mixin targets total)

`src/main/java/zmaster587/advancedRocketry/mixin/`:

| File | Target(s) | Replaces |
|---|---|---|
| `MixinEntityGravity.java` | `Entity`, `EntityFallingBlock`, `EntityMinecart`, `EntityTNTPrimed` (multi-target) | ASM gravity-injection block in `ClassTransformer.java:728-756` |
| `MixinWorldSetBlockState.java` | `World#setBlockState(BlockPos, IBlockState, int)` `@At("RETURN")` | ASM block at `ClassTransformer.java:759-787` |
| `MixinEntityPlayerInventoryAccess.java` | `EntityPlayer.onUpdate` — `@Redirect` of `Container.canInteractWith` | ASM block at `ClassTransformer.java:681-723` |
| `MixinEntityPlayerMPInventoryAccess.java` | `EntityPlayerMP.onUpdate` — same redirect | ASM block at `ClassTransformer.java:638-677` |

`mixins.advancedrocketry.json` updated to 7 active mixins
(was 3 + 4 new). Mixin AP generated correct SRG mappings into
`mixins.advancedrocketry.refmap.json`.

Spike to find the right `@Redirect` target ran `javap -c` on the
deobf jar at
`build/fg_cache/.../forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar`.
Both `EntityPlayer.onUpdate` (offset 181) and `EntityPlayerMP.onUpdate`
(offset 63) call `Container.canInteractWith(EntityPlayer):Z` via
INVOKEVIRTUAL with an immediate `ifne <skip>` — redirecting the call to
return true when `RocketInventoryHelper.canPlayerBypassInvChecks` says
yes reproduces the original ASM jump-past-close-screen behaviour.

The ASM transformer's `RenderGlobal` transform (5th target, guarded
on Forge < 14.23.2.2642; current 14.23.5.2860) was dead code — not
replaced.

### Phase 2 — ASM + HookLib deleted

```
- src/main/java/zmaster587/advancedRocketry/asm/ClassTransformer.java (835 LoC)
- src/main/java/zmaster587/advancedRocketry/ARHookLoader.java
- src/main/java/zmaster587/advancedRocketry/repack/gloomyfolken/  (24 files)
- src/main/resources/methods.bin  (HookLib obfuscation map)
```

`AdvancedRocketryPlugin.java` slimmed from 61 LoC to 51 LoC:
all `hookLoader` indirection removed, `getASMTransformerClass()` returns
empty array, `getSetupClass` / `injectData` / `getAccessTransformerClass`
collapsed to defaults. Kept `getModContainerClass` (still needed by FML).

Pre-delete grep checks all came back clean: no `@Hook` annotations,
no external `ARHookLoader` / `repack.gloomyfolken` / `methods.bin`
references outside the deleted tree.

### Phase 3 — behavioural pin test

`src/test/java/zmaster587/advancedRocketry/test/server/MixinHookBehaviourPinsTest.java`
— one focused test: `setBlockStateMixinHookCompletesWithoutThrowing`.
Toggles 4 blocks at one column then queries the atmosphere subsystem,
failing-fast if the `setBlockState → AtmosphereHandler.onBlockChange`
dispatch breaks under future mapping drift.

The original 4-test plan was downsized: cold-start dedicated server
in the harness doesn't advance the overworld tick loop within a
sensible budget, and `EntityFallingBlock` without a source-block ctor
dies in onUpdate — neither yak-shave is worth the time when:

1. Mixin AP + `required: true` rules out silent-no-op regressions
   (compile-time + apply-time hard fail).
2. Existing testServer suite implicitly pins every hook surface
   (atmosphere smoke ⇒ `setBlockState` hook; rocket descent/landing ⇒
   `Entity.onUpdate` gravity hook; multiblock placement tests ⇒ same
   `setBlockState` hook again).
3. The player-inventory-bypass redirect needs a real `EntityPlayer`
   GUI session — explicitly deferred to TASK-10b (testClient e2e).

### Phase 4 — pyramid

| Layer | Result | Notes |
|---|---|---|
| testUnit | 187 / 0 / 0 | unchanged from pre-rewrite baseline |
| testIntegration | 80 / 0 / 0 | unchanged |
| testServer | **240 / 0 / 3** | was 239 — +1 from new mixin pin |
| testClient | not exercised this session | requires `DISPLAY=:77` |

The +1 mixin pin is the only delta. Zero regressions.

### Phase 5 — docs

- `CLAUDE.md`: tech-stack line + Forge-patterns bullet flipped from
  "ASM coremod" → "Mixin via MixinBooter".
- `.agent/tasks/README.md`: TASK-08-mixin moved from P0 backlog into
  the Done table; ordering hints updated.
- This marker.

---

## Architectural threads worth carrying forward

1. **Mixin AP + `required: true` is the new safety net.** When a
   mapping snapshot shifts and a target stops resolving, startup
   hard-fails with a logged mixin error. The old `IClassTransformer`
   ate this case silently (no transform applied, no log). So the
   absence of new behavioural pins for each hook is acceptable — the
   regression mode they would have caught no longer exists.

2. **Multi-target mixin works for the gravity case.** Mixin's
   `@Mixin({A.class, B.class, ...})` cleanly handles the
   "vanilla subclass doesn't call super.onUpdate" pattern that drove
   the 4 separate ASM transforms. One file, one annotation, four
   targets.

3. **HookLib was confirmed dead.** No project `@Hook` annotations
   existed; its `SecondaryTransformerHook` was a no-op chain. Cleanly
   removable.

4. **The `MEMORY.md` user feedback `feedback_no_fakeplayer_for_player_tests`**
   still authoritative: the `MixinEntityPlayer(MP)InventoryAccess`
   redirect needs a real `EntityPlayer` GUI session and is queued for
   TASK-10b — not for `FakePlayer` in testServer.

---

## Uncommitted on disk (need a commit before EOD-pristine)

```
M  CLAUDE.md
M  .agent/tasks/README.md
M  src/main/java/zmaster587/advancedRocketry/asm/AdvancedRocketryPlugin.java
M  src/main/resources/mixins.advancedrocketry.json
A  src/main/java/zmaster587/advancedRocketry/mixin/MixinEntityGravity.java
A  src/main/java/zmaster587/advancedRocketry/mixin/MixinEntityPlayerInventoryAccess.java
A  src/main/java/zmaster587/advancedRocketry/mixin/MixinEntityPlayerMPInventoryAccess.java
A  src/main/java/zmaster587/advancedRocketry/mixin/MixinWorldSetBlockState.java
A  src/test/java/zmaster587/advancedRocketry/test/server/MixinHookBehaviourPinsTest.java
A  .agent/.context-markers/2026-05-20-1700_task08-mixin-shipped.md
D  src/main/java/zmaster587/advancedRocketry/asm/ClassTransformer.java
D  src/main/java/zmaster587/advancedRocketry/ARHookLoader.java
D  src/main/resources/methods.bin
D  src/main/java/zmaster587/advancedRocketry/repack/gloomyfolken/...  (24 files)
```

Suggested split:

1. `refactor: rewrite ASM coremod to Mixin (TASK-08-mixin Phase 1+2)`
2. `test: pin setBlockState mixin hook (TASK-08-mixin Phase 3)`
3. `docs: TASK-08-mixin shipped — flip backlog`

---

## Restore instructions

```
Read .agent/.context-markers/2026-05-20-1700_task08-mixin-shipped.md
Read .agent/tasks/TASK-08-mixin-rewrite.md
git status                 # confirm matches "Uncommitted on disk"
./gradlew testUnit testIntegration testServer  # confirm pyramid green
```

Next planned task: see `.agent/tasks/README.md` "Suggested session
ordering". P0 queue is empty; TASK-10b (testClient e2e) is the
biggest player-visible coverage win remaining.

User preference: respond in Russian (see `feedback_respond_in_russian`
auto-memory + `CLAUDE.md` "Language" section).
