# TASK-40d — Batch 4: Gap L + Gap K (force field + laser gun)

**Status: ✅ Partial 2026-05-29 (Gap L shipped; Gap K deferred — testClient infra)**

## Ticket

- Source: 2026-05-27 coverage audit Gap L (TileForceFieldProjector
  behavioural) + Gap K (ItemBasicLaserGun firing). Audit said "visual-
  effect / firing pair".
- Status: ✅ Gap L shipped; Gap K deferred to a future testClient
  authoring batch.

## Shipped — Gap L

`TileForceFieldProjector.onIntermittentUpdate` is a public probe-
friendly method (split from `update()` by a prior task-driven
refactor) that handles one extension or retraction cycle. When
`world.isBlockPowered(getPos())` fires it places `blockForceField`
one cell along the projector's facing per call; when unpowered, it
retracts one cell per call.

**Contract pinned**: powered + tick → block in front becomes
`advancedrocketry:forcefield`; un-power + 3 ticks → reverts to air.

- New probe: `infra forcefield-tick <dim> <x> <y> <z> [ticks]` —
  calls `TileForceFieldProjector.onIntermittentUpdate` N times,
  bypassing the natural-tick `worldTime % 5 == 0` gate.
- Test: `ForceFieldProjectorProjectsAndRetractsTest.poweredProjectorPlacesForceFieldThenRetractsOnUnpower`.

**Notes on the retraction tick count**: after a single extension
cycle, `extensionRange` ends at 2 (the field is at offset 1; the
counter is incremented after placement). The retraction branch
checks `pos.offset(facing, extensionRange + 1)` and
`pos.offset(facing, extensionRange)` each call. The first
retraction tick checks distances 3 and 2 (both air → no-op) and
decrements `extensionRange` to 1. The second tick checks distance
2 (air) then 1 (the placed field) and clears it. 3 ticks gives
safety margin.

## Deferred — Gap K (ItemBasicLaserGun firing)

Phase 0: laser gun's primary effects (entity damage on right-click /
hold-use, block harvest on full charge) fire in
`onItemRightClick`, `onUsingTick`, `onItemUseFinish`. All require
a real `EntityPlayer` with a held item. Server-tier requires
`FakePlayer` which is forbidden by project policy.

**testClient implementation path**: equip bot with laser gun, point
at target entity (spawn an EntityItem proxy), right-click, assert
target took damage. The `LIVING_ATTACK` event with source `GENERIC`
is the player-visible contract endpoint.

**Status**: deferred pending the testClient harness fix (the
2026-05-29 batch found the harness fails locally with
"No OpenGL context found"; user note 2026-05-29 indicates the
display should be capable). Gap K can ride alongside the Batch 2
testClient tests (`GasChargePadFillsPressureTankE2ETest`,
`AreaGravityControllerResetsFallDistanceE2ETest`) once they run.

## Probe additions

| Probe | Purpose |
|---|---|
| `infra forcefield-tick <dim> <x> <y> <z> [ticks]` | drive TileForceFieldProjector deterministic extension / retraction |

## Effort vs estimate

Audit estimate: L=4h + K=4h = 8h. Actual: ~1.5 h (Gap L Phase 0 +
authoring + 1 debug iteration for the retraction-tick count). Gap K
unshipped.

## Completion Checklist

- [x] 1 server test authored + green.
- [x] 1 new probe verb.
- [x] tasks/README.md updated.
- [ ] Gap K testClient — pending harness fix.
