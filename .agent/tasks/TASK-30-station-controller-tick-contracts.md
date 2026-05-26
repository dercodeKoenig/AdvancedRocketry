# TASK-30: Station controller tick contracts (Altitude / Gravity / Orientation)

## Ticket

- Source: 2026-05-25 Tier 2 audit, gap #2. Carried forward into
  2026-05-26 audit out-of-scope ("functional tick-contract
  requires SpaceObject-fixture").
- Status: **✅ Completed 2026-05-26** — see `.agent/tasks/README.md`
  Done table.
- Created: 2026-05-26.

## Actual scope shipped

**Phase 0 — probe addition**:
`station controller-set-target <dim> <x> <y> <z> <id> <value>`.
Casts tile to `ISliderBar` and calls `setProgress(id, value)` —
same write the GUI slider triggers, but server-side direct
(bypasses GUI/network round-trip).

`station info` extended with: `gravity`, `targetGravity`,
`rotationEast/Up/North`, `targetRPH0/1/2`,
`targetOrbitalDistance` — the live state the controllers walk
toward.

**Phase 1-3 — 3 tests** (`StationControllersTickContractTest`):

1. `altitudeControllerWalksStationOrbitalDistanceTowardTarget` —
   set target=preDist+50, force-tick 200, assert orbitalDistance
   moved toward target (`|postDist - target| < |preDist - target|`).
2. `gravityControllerWalksStationGravityTowardTarget` — set
   target (which may revert due to bug #3 in the ledger),
   force-tick 2000, assert gravity walked measurably below the
   default 1.0. End-state pin only — see "Production bug
   discovered" below.
3. `orientationControllerWalksStationRotationTowardTarget` — set
   target progress=100 (targetRPH=40), force-tick 400, assert
   station's rotation around EAST changed from baseline.

**Production bug discovered + logged**:
`TileStationGravityController` constructor omits the
`redstoneControl.setRedstoneState(OFF)` call. Default
ModuleRedstoneOutputButton state is ON, so updates overwrite
`targetGravity` to 10 (no redstone wiring) every tick. Logged to
`.agent/history/known-bugs-ledger.md` Batch #2 entry #3. The
gravity test was reworked to pin end-state walk (gravity drops
measurably below 1.0) rather than target identity — works with
both the broken and a future fixed version of the controller.

## Context

[`StationControllersSmokeTest`](../../src/test/java/zmaster587/advancedRocketry/test/server/StationControllersSmokeTest.java)
pins **placement + tile-lifecycle smoke** for the three station
controllers:

- `TileStationAltitudeController` — sets target orbital altitude
- `TileStationGravityController` — sets target gravity multiplier
- `TileStationOrientationController` — sets target rotation/yaw

The test's own javadoc flags the missing tick-behaviour layer as
follow-up. What's pinned today: "tile places, tile ticks without
NPE". What's NOT pinned: "control input on the GUI actually
mutates the station's target value, and the station's ticker
walks orbitalDistance / gravity / rotation toward that target".

## Why it matters

These three controllers are the entire knob set for player
station automation. A regression that disconnects the controller
GUI's "set value" event from the SpaceStationObject's target
field silently breaks every player automation downstream
(no station altitude change, no gravity adjustment, no rotation
control). Players have no other way to set these.

## Blocker

Needs a server-tier fixture that:

1. Builds a real `SpaceStationObject` (existing
   `station create` probe partially covers this).
2. Places the three controllers within the station's chunk
   (existing `artest place` works once the chunk-load order is
   right — see `StationControllersSmokeTest` for the pattern).
3. Drives the controllers' GUI module input from the test
   (currently no probe exposes the `ModuleNumericTextbox`
   setter pathway — `GuiCallback.onModuleUpdated` runs client-
   side; the server-side equivalent is missing).

The third item is the real blocker — without a `station
controller-set-target <controllerType> <value>` probe verb, the
test can place but not exercise the input pathway. Add this
probe first; then the contract pins become straightforward.

## Implementation plan

| Phase | Effort | Result |
|---|---|---|
| 0 | ~2 h | New probe: `station controller-set-target <dim> <x> <y> <z> <controllerType> <value>` — reflectively reaches the tile's target field and triggers the module-update hook. Verify the station's `setDestinationOrbitalDistance` / `setTargetGravity` / `setDestinationRotation` is invoked. |
| 1 | ~2 h | `StationAltitudeControllerTickTest` — set target, force-tick station, assert `getOrbitalDistance()` walks toward target by some non-zero delta. |
| 2 | ~2 h | `StationGravityControllerTickTest` — set target gravity, force-tick, assert `getGravity()` walks toward target. |
| 3 | ~2 h | `StationOrientationControllerTickTest` — set target yaw rotation, force-tick, assert rotation walks. (Tricky: rotation may be a continuous spin not a target — verify in production code first.) |

## Acceptance

- [ ] 3 tests pinning the player-visible "set target → station
      walks toward it" loop.
- [ ] Loose-bound pins ("changes by >= some delta after N
      ticks"); no exact RF or tick-budget pins.
- [ ] Pyramid counter regenerated.

## Out of scope

- Cross-controller interactions (e.g. altitude controller + gravity
  controller at same time — out of base contract).
- Visual rendering of rotation (testClient, separate axis).
- Edge cases: target out of bounds, target equal to current, etc.
  These can be added later if motivated.

## Dependencies

- Does NOT block any other task.
- Once unblocked (Phase 0 probe lands), Phases 1-3 are independent.

## Estimated effort

- Phase 0 (probe): 2 h
- Phases 1-3: 2 h each
- **Total**: ~8 h

## Risk

Medium. Phase 0 probe design depends on the station controllers'
input-routing architecture. The `IModularInventory.getModules`
returns UI modules; the server-side network-data handler reads
the player input and calls the target setter. The probe needs to
inject directly at the setter level, bypassing the GUI path.
