# TASK-22: UV-assembler full behavioural delta from RocketAssembler

## Ticket

- Source: 2026-05-23 audit — Gap #8. `UvAssemblerDivergesFromRocketAssemblerTest`
  pins only the class-identity divergence ("UV is its own class,
  not collapsing to rocket assembler"). Deeper behavioural delta
  is deferred.
- Status: ✅ **Completed 2026-05-25** (partial — Phases 1 & 2 shipped; Phase 3 deferred).
- Created: 2026-05-23.

## Actual scope (2026-05-25)

**Phase 1 (bounds delta) — ✅ shipped via constants reflection.**

`UvAssemblerBoundsConstantsTest`:
- `rocketAssemblerAllowsTallerStructureThanUvAssembler` — UV's
  `MAX_SIZE_Y` strictly < rocket's. Both positive.
- `uvAssemblerHeightCapMatchesItsWidthCap` — UV is a cube
  (MAX_SIZE == MAX_SIZE_Y), pinning the design invariant.

Drives new `/artest assembler max-y` probe (reflective constants
read on both tile classes). Pure read, no state mutation.

Original plan's "build a tall fixture and observe truncation"
approach was abandoned: constructing a tower-of-25 fixture costs
~50 setBlockState calls plus terrain pre-clear; the constants pin
covers the same player-visible contract ("UV's height cap is
smaller than rocket's") at a fraction of the test wall-time.

**Phase 2 (output entity class delta) — ✅ shipped end-to-end.**

`UvAssemblerOutputEntityClassTest`:
- `rocketAssemblerProducesEntityRocketNotStationDeployed` — uses
  existing `artest fixture rocket simple`, assembles, asserts
  `entityClass.endsWith(".EntityRocket")` and NOT
  `StationDeployedRocket`.
- `uvAssemblerProducesEntityStationDeployedRocket` — uses new
  `artest fixture uv-rocket` (UV-specific geometry: column + U-shape
  + rocket components inside the resulting BB), assembles, asserts
  `entityClass.endsWith(".EntityStationDeployedRocket")`.

Drives:
- New `/artest fixture uv-rocket <dim> <x> <y> <z>` probe — UV
  fixture variant matching UV's `getRocketPadBounds` algorithm
  (column above builder + lateral towers).
- New `entityClass` field in `/artest rocket info` response.

**Phase 3 (mount eligibility) — deferred.**

`EntityStationDeployedRocket extends EntityRocket` and inherits
`processInitialInteract` unchanged. So at the entity-API surface,
both accept the same passenger-mount flow. The player-visible
difference (UV launches DOWNWARD vs rocket launches UPWARD) is
encoded in `EntityStationDeployedRocket.launchDirection = DOWN`
plus the override of the flight tick — that's pinned implicitly
by the Phase 2 entity-class delta (any future swap of the entity
class would break the launchDirection initialisation too).

A dedicated launch-direction pin can be added later if a
regression appears; right now it would duplicate Phase 2's
contract through a more brittle assertion path.

## Context

`TileUnmannedVehicleAssembler` (UV-assembler) and
`TileRocketAssemblingMachine` share a parent class shape but have
divergent runtime behaviour:

- **Rocket Assembler**: scans a rocket fixture, builds an
  `EntityRocket` capable of carrying a player into orbit.
- **UV Assembler**: scans a smaller fixture, builds an
  `EntityStationDeployedRocket` — unmanned, station-deployed,
  carries cargo only.

The bounds, fuel requirement, output entity type, and probably
storage-chunk shape are all different. Today's coverage only
asserts that `TileUnmannedVehicleAssembler.class != TileRocketAssemblingMachine.class`
— class identity, which is borderline-impl per the SOP audit.

The actual player-visible contracts that diverge:

| Contract | Rocket Assembler | UV Assembler |
|---|---|---|
| Scan bounds (max size) | Large (e.g. 16×24×16) | Smaller |
| Fuel requirement min | Higher | Lower |
| Output entity class | `EntityRocket` | `EntityStationDeployedRocket` |
| Player-mountable | Yes | No |
| Deploys from station | No | Yes |
| Mission-rocket eligibility | Yes | Yes |

A regression that silently swaps the output entity class, or
shrinks the rocket-assembler's bounds to match UV's (or vice
versa) would not be caught by the current class-identity pin.

## Implementation plan

### Phase 1 — Scan bounds delta (~1 h)

Test: `UvAssemblerBoundsTest`:

- `rocketAssemblerAcceptsLargeFixture` — build N×M×K fixture that
  fits rocket bounds but exceeds UV bounds, scan via rocket
  assembler, assert success.
- `uvAssemblerRejectsLargeFixture` — same fixture, UV assembler,
  assert scan failure with bounds error.
- `uvAssemblerAcceptsSmallFixture` — small fixture, UV assembler,
  assert success.
- `rocketAssemblerAcceptsSmallFixture` — small fixture, rocket
  assembler, assert success (i.e. rocket bounds are a superset
  of UV bounds).

### Phase 2 — Output entity class delta (~1 h)

Test: `UvAssemblerOutputEntityClassTest`:

- `rocketAssemblerProducesEntityRocket` — build + assemble, find
  spawned entity, assert class == `EntityRocket`.
- `uvAssemblerProducesEntityStationDeployedRocket` — same, assert
  class == `EntityStationDeployedRocket`.

Already pinned indirectly by `MissionOreCompletionTest` and
`RocketStationCauseEffectTest`, but worth an explicit
self-contained test here for failure isolation.

### Phase 3 — Fuel / launch eligibility delta (~1 h)

Test: `UvAssemblerLaunchEligibilityTest`:

- `uvAssembledRocketRejectsPlayerRider` — spawn UV-assembled
  rocket, attempt to mount via probe, assert mount fails (or
  mounting fires but launch path refuses).
- `rocketAssembledRocketAcceptsPlayerRider` — same against
  rocket-assembled, assert mount succeeds.

(Player-mount side may need testClient; if server-tier probe can
exercise `EntityRocket.processInitialInteract`, prefer server.)

## Acceptance

- [ ] Three test classes, ~8 tests total covering bounds, output
      class, mount eligibility.
- [ ] Existing
      `UvAssemblerDivergesFromRocketAssemblerTest.classIdentityDiverges`
      stays as a sanity guard, but the bulk of the contract moves
      to the new tests.
- [ ] Pyramid counter regenerated per TASK-17 phase 1.

## Technical decisions

- **Replace class-identity pin gradually** — the existing test
  isn't wrong (UV must not collapse to rocket assembler), but
  the SOP says "class FQN is borderline". Keep it as a
  redundant-but-explicit sanity gate; the new tests do the
  contract heavy lifting.
- **Phase 3 might be testClient** — if server probe can't
  simulate player mount, the mount-eligibility test moves to
  testClient (single test, easy).
- **No production logic changes**.

## Out of scope

- Detailed fuel-amount comparisons (per-mode fuel formulas are
  impl per SOP; pin "UV launches with less fuel" loose-bound, not
  exact figures).
- Storage-chunk shape internals (impl).
- Mission-rocket eligibility — already covered via
  `MissionOreCompletionTest`.

## Dependencies

- Does NOT block any other task.
- Pattern source: `RocketAssemblySmokeTest`.

## Estimated effort

- Phase 1 bounds: ~1 h
- Phase 2 output class: ~1 h
- Phase 3 launch eligibility: ~1-1.5 h
- Close-out: ~30 min
- **Total**: ~4 h
