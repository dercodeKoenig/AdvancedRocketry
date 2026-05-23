# TASK-19: Multiblock powered-cycle (Terraformer / BHG / Beacon enable)

## Ticket

- Source: 2026-05-23 audit — Gaps #4, #5, #6. Three multiblocks
  with structure-validation coverage but no powered-cycle pin.
- Status: **Backlog**.
- Created: 2026-05-23.

## Context

Three multiblocks share the same coverage shape: their structure
validation is pinned, but the actual "powered + ticking → observable
side-effect" cycle is not. Grouped into one task because they share
the implementation pattern: place fixture, supply input(s), force a
tick, assert observable effect.

| # | Multiblock | Already pinned | Gap |
|---|---|---|---|
| 5 | Terraformer | `TerraformerMultiblockTest` (2 tests, structure), `TerraformerMultiBlockCycleTest` (1 test, no-NPE on partial), `TerraformingSmokeTest` (1 test, density set via probe) | Full multiblock-powered atmosphere step |
| 6 | Black Hole Generator | `BlackHoleGeneratorMultiblockTest` (4 tests, structure) | Powered-tick produces energy in output |
| 4 | Beacon | `BeaconMultiblockTest` (3 tests, structure), `BeaconLocationProbeSmokeTest` (2 tests, probe shape) | Redstone power + setMachineEnabled → location appears in `DimensionProperties.beaconLocations` |

## Implementation plan — three phases, one per multiblock

### Phase 1 — Terraformer powered-tick (~3 h)

The terraformer's contract: with the full multiblock built + powered
+ fuel, repeated ticks should incrementally change atmosphere
density on the dimension. `TerraformingSmokeTest` already pins the
density-mutation primitive via `/artest atmosphere set-density`;
this phase pins the multiblock as the **driver** of that mutation.

Test: `TerraformerPoweredCycleTest extends AbstractSharedServerTest`:

- `terraformerWithFuelAndPowerStepsDensityToward(target)` —
  build full 17×17 fixture via `/artest fixture multiblock`,
  inject fuel via `/artest hatch fill`, inject power via
  `/artest energy inject`, force N ticks via `/artest machine
  force-tick`, assert density drifted toward configured target.

- `terraformerWithoutFuelDoesNotStepDensity` — same fixture, no
  fuel, force tick, assert density unchanged.

- `terraformerWithoutPowerDoesNotStepDensity` — same fixture, no
  power injected, force tick, assert density unchanged.

Test contract: "density drifts" loose bound (e.g. delta ≥ 1), NOT
exact step size (per SOP — exact-step is impl).

### Phase 2 — Black Hole Generator powered cycle (~3 h)

BHG's contract: full structure + matter input → energy in output
buffer. Similar shape to terraformer.

Test: `BlackHoleGeneratorPoweredCycleTest`:

- `bhgWithMatterAccrualOutputsEnergy` — build fixture, fill matter
  hatch, force N ticks, assert output energy hatch's `stored`
  field increased.

- `bhgWithoutMatterDoesNotAccrueEnergy` — same fixture, empty
  matter, force tick, assert stored unchanged.

- `bhgAtCapacityClampsAccrual` — pre-fill energy hatch to capacity,
  force tick, assert overflow not lost (clamped, not silently
  dropped).

### Phase 3 — Beacon enable cycle (~3 h)

The current beacon coverage pins:
- Structure validates (`BeaconMultiblockTest`)
- Probe envelope is well-formed (`BeaconLocationProbeSmokeTest`)

The gap is the **player-visible effect**: a powered beacon must
register its position in `DimensionProperties.beaconLocations` so
the beacon-finder item can locate it. Without this pin, the chain
"redstone power → setMachineEnabled → beacon visible to finder"
can silently break in either link.

Test: `BeaconEnableCycleTest`:

- `poweredBeaconRegistersLocationInDimensionProperties` — build
  fixture, redstone-power via `/artest redstone set <pos> 15`
  (verb may need adding), invoke `setMachineEnabled` via probe,
  assert `DimensionProperties.beaconLocations` contains the
  fixture's center position.

- `unpoweredBeaconDoesNotRegister` — build fixture, no redstone,
  assert location absent.

- `removingBeaconBlockUnregisters` — build + register, break the
  controller block via `/artest break`, assert location absent.

If `/artest redstone set` doesn't exist, add as part of Phase 3
infrastructure (~30 min).

## Acceptance

- [ ] Three new test classes, ~3 tests each = 9 tests total.
- [ ] All assertions are loose-bound on numeric magnitudes (per
      SOP), tight on observable side-effects (densities change,
      stored increases, location appears).
- [ ] Pyramid counter regenerated per TASK-17 phase 1.

## Technical decisions

- **Three classes, not one suite**. Each multiblock fails for
  independent reasons; failure isolation matters.
- **Shared harness via AbstractSharedServerTest** — single
  cold-start amortises across all 9 tests.
- **No exact density / energy magnitudes** — only directionality
  (increased, decreased, stayed) and presence/absence.
- **No production logic changes**.

## Out of scope

- Visual rendering of the beacon beam (testClient + visual-diff
  territory; deferred per TASK-15 status).
- Atmosphere terraformer terminal interactions (separate scope).
- Microwave Receiver and Solar Array — both already have basic
  powered-cycle coverage via `MachineDomainSmokeSuite` and
  `SolarPanelInsolationTest`.

## Dependencies

- Does NOT block any other backlog task.
- May need `/artest redstone set` verb (Phase 3 infrastructure).

## Estimated effort

- Phase 1 Terraformer: ~3 h
- Phase 2 BHG: ~3 h
- Phase 3 Beacon (incl. probe verb if needed): ~3-3.5 h
- **Total**: ~9-10 h
