# TASK-40b — Batch 2: Gap F.2 + Gap C (testClient)

**Status: ✅ Gap F.2 shipped (testClient PASSED); Gap C @Ignore — design needs revisit (2026-05-29)**

## Ticket

- Source: 2026-05-27 coverage audit Gaps F.2 (`TileGasChargePad`) and C
  (`TileAreaGravityController` player effect). Both testClient per audit.
- Status: ✅ Authored; local-harness blocked.
- Created: 2026-05-29.

## Context

Two testClient gaps batched. Both pin player-effect contracts requiring
a real `EntityPlayer` in the world (server-side cannot be satisfied
without `FakePlayer` which is explicitly forbidden by project policy —
see TASK-10 marker).

## Phase-0 reshape notes

### Gap F.2 — TileGasChargePad

**Audit framing**: "A player standing on a powered + filled GasChargePad
has their ItemPressureTank fluid amount increase tick over tick."

**Phase-0 finding**: production
(`TileGasChargePad.canPerformFunction` lines 55-117) scans the 1×2×1
AABB starting at the pad's pos for `EntityPlayer`. For each player, reads
the CHEST slot; if `IFillableArmor` (or air-container wrapper), drains
the pad's tank by the missing-air amount and calls
`fillable.increment(stack, drained)`. `getPowerPerOperation() == 0` —
pad doesn't consume RF. `performFunction()` is a no-op — the work is
inside `canPerformFunction`.

**Contract pinned** (unchanged from audit): "standing on a
powered + oxygen-filled GasChargePad with a partially-empty space chest
raises the chest's air reading over a wait window."

**Test**: `GasChargePadFillsPressureTankE2ETest`. Reuses `equip-space-chest`
probe from TASK-24 + existing `player held-air` + `fluid inject`.

### Gap C — TileAreaGravityController

**Audit framing**: "A formed AreaGravityController with target = 0.5
applied to a player inside its projection radius causes the player's
fall-step distance over N ticks to fall within the 0.5-gravity band."

**Phase-0 finding**: production
(`TileAreaGravityController.update` lines 184-226) walks every
`Entity` in a cube of side `2*getRadius()` (default radius=5 →
getRadius()=15) around its pos. For each entity, **unconditionally** sets
`e.fallDistance = 0` BEFORE iterating directions/side-selector states.
Motion modification only fires if a direction is enabled via
`sideSelectorModule.getStateForSide(dir) != 0` (default: all 0 → no
motion change). The fallDistance reset fires regardless.

**Contract reshape**: pin the cheap player-visible `fallDistance = 0`
gate (which itself proves: isRunning + in-radius + entity-found). The
audit's "fall-step distance band pin" requires side-selector state setup
+ sustained motion sampling — more infrastructure, same gate. Pin the
gate, defer the band-quality pin.

**Test**: `AreaGravityControllerResetsFallDistanceE2ETest`. Uses new
`player set-fall-distance` + `player get-fall-distance` probes +
existing `multiblock gravity-controller` fixture.

## Implementation Plan

### Phase 1: Probe additions ✅

- [x] `player set-fall-distance <amount>` — sets EntityPlayer's
  `fallDistance` field directly.
- [x] `player get-fall-distance` — reads the same field.

### Phase 2: Tests ✅

- [x] `GasChargePadFillsPressureTankE2ETest.standingOnPoweredPadRefillsSuitAir`
- [x] `AreaGravityControllerResetsFallDistanceE2ETest.poweredControllerResetsFallDistanceOfNearbyPlayer`

### Phase 3: Validation — partial

**Harness fix (2026-05-29)**: build.gradle.kts now forwards
`DISPLAY`, `XAUTHORITY`, and `LIBGL_ALWAYS_SOFTWARE` env vars from
the parent shell to the spawned client subprocess via the
framework's `forge.test.client.env.*` channel. Without this,
the client JVM had no DISPLAY and LWJGL's LinuxDisplay NPE'd in
`getAvailableDisplayModes` during the static `Display.<clinit>`.

**Phase 0 finding (2026-05-29)**: the dev-box's running Xorg at
`:99` (amdgpu DDX) is incompatible with LWJGL 2.9.4's old XRandR
query path. Standalone LWJGL test against `:99` NPE's even with
DISPLAY set. Workaround: start a fresh Xvfb at `:100` with
`+extension GLX +extension RANDR +render`; LWJGL works fine there.
Run testClient with `DISPLAY=:100 ./gradlew testClient -PuseLocalFramework=true`.

**Validation results**:

- `GasChargePadFillsPressureTankE2ETest` ✅ PASSED on `DISPLAY=:100`.
- `AreaGravityControllerResetsFallDistanceE2ETest` ⏸ now @Ignore —
  the test set fallDistance > 0 then read it back as 0 because
  vanilla MC's `EntityLivingBase.updateFallState` resets a grounded
  bot's fallDistance to 0 every tick. The controller's reset is
  indistinguishable from the vanilla reset on a grounded bot. To
  un-ignore: rewrite around a falling EntityItem (no
  onGround/motionY=0 vanilla-reset path) — see test class docstring.

- [x] Tests compile (verified via `./gradlew compileTestJava -PuseLocalFramework=true`).
- [ ] **testClient run blocked in this dev environment** by OpenGL
  context creation failure. The dev-box Xvfb at `:99` advertises GLX
  (server glx vendor "SGI"), but the spawned client JVM fails with
  `RuntimeException: No OpenGL context found in the current thread`
  during the LWJGL `Display.create()` call. **Existing testClient tests
  (e.g. `LowGravFallDamageE2ETest`) fail identically** — confirms the
  blocker is environmental, not code-side. `LIBGL_ALWAYS_SOFTWARE=1`
  in the parent shell does not propagate to the forked client subprocess
  because the harness layer's `forge.test.client.env.*` channel only
  forwards env vars from FG6's `runClient` config (which doesn't
  include LIBGL_ALWAYS_SOFTWARE). Test code is structurally correct
  and follows the established testClient pattern from
  `LowGravFallDamageE2ETest` / `ItemSpaceArmorUseFluidE2ETest`.

## Technical Decisions

- **Contract reshape for Gap C**: prefer the cheap reset-pin over the
  band-quality motion pin. The fallDistance unconditional reset is a
  strict prerequisite of every player-effect downstream — pinning it
  guards the same gate (isRunning + in-AABB + entity-found) without
  needing side-selector or slider state setup. Future tightening can
  add a band-quality pin once the side-selector probe lands.
- **No production logic changes** (same rule as TASK-01 §15).

## Probe surface additions

| Probe | Purpose |
|---|---|
| `player set-fall-distance <amount>` | sets `EntityPlayer.fallDistance` for Gap C |
| `player get-fall-distance` | reads same field |

## Dependencies

**Requires**: TASK-24 (`equip-space-chest` + `player held-air` probes),
TASK-04 (`multiblock gravity-controller` fixture).

**Does NOT block** future batches.

## Estimated effort vs actual

Audit estimate: F.2=4h + C=4h = **8h**. Actual: ~2h authoring (Phase-0
reshape on Gap C cut scope significantly). +0h validation (blocked).

## Completion Checklist

- [x] 2 testClient tests authored against current production code paths.
- [x] 2 new probe verbs added.
- [x] Tests compile.
- [x] tasks/README.md updated.
- [ ] testClient PASSED — pending an env with working OpenGL context for
  the spawned client JVM.

## Notes for future runs

When the testClient harness runs in a proper-GL environment, these tests
should fire. Both are short (≤ 1 min wall time each) on a working
harness. If they fail there, the most likely causes:

- **Gap F.2**: the pad's `canPerformFunction` cadence may need a few
  more game ticks than 100. Bump `bot().waitTicks(100)` to 200.
  Alternatively: confirm pad's libVulpes parent ticks naturally without
  power (since `getPowerPerOperation()==0`).
- **Gap C**: `isRunning()` may require explicit `setMachineEnabled(true)`
  via a new probe — the default may need wiring. If so, add `infra
  agc-enable <dim> <x> <y> <z>` and call it after `energy inject`.
