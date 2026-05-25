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

## Implementation plan — four phases (Phase 1 split, 2026-05-25)

**Revised after Phase 1 recon**: production has two distinct
player-visible code paths gated by:

```java
((WorldProviderPlanet && isNativeDimension) || allowTerraformNonAR)
```

Both are player-relevant — modpacks ship with either AR-native dims
or with `allowTerraformNonAR=true`. Splitting Phase 1 pins both
branches; without this split the suite tests neither branch
realistically (overworld with default config skips the mutation
silently).

### Phase 1a — Terraformer on AR-native planet (~3-4 h) — partially shipped 2026-05-25

**Status**: test class checked in with 2/3 passing. Happy-path test
(`nativePlanetTerraformerWithFuelAndPowerStepsDensity`) currently
FAILs with `progress:0` after 24000 force-ticks — `onRunningPoweredTick`
never fires because `batteries.getUniversalEnergyStored()` reports 0
even though `artest energy inject` lands on the creative input plug
(`TileCreativePowerInput`) at the 'P' position.

**Root cause (suspected)**: `artest energy inject` writes via the
Forge `IEnergyStorage` capability; libVulpes' controller-side
`batteries` aggregator reads via `IUniversalEnergy`. The bridge
either (a) doesn't happen because `integrateTile` isn't running for
the `'P'` block, or (b) the creative plug's `IUniversalEnergy`
state is separate from its Forge capability state.

**Next-session work shape**:

- Add a new probe verb (e.g. `/artest machine inject-controller-energy
  <dim> <x> <y> <z> <amount>`) that reflects into the controller's
  `batteries` field and writes directly — bypasses the
  IUniversalEnergy/Forge-capability bridge. Tag it as terraformer-
  specific (or generic-multiblock) infra.
- OR: confirm via `artest energy stored` at the 'P' position that
  injection actually accepts non-zero energy. If `accepted:0`,
  switch fixture to place `blockForgeInputPlug` (mapping index 1)
  instead of the creative variant via a hatch-override.

**Shipped this session**:
- `TerraformerPoweredCycleOnArPlanetTest` class scaffolded.
- `@Before` generates a fresh AR planet via `/ar planet generate`;
  `@After` deletes it.
- `assertDimIsNativeArPlanet()` precondition guard (passes).
- Counter-tests (`nativePlanetTerraformerWithoutFuelDoesNotStep`,
  `nativePlanetTerraformerWithoutPowerDoesNotStep`) — PASSING.
  Density unchanged in both cases after 24000 force-ticks.

Generates a fresh AR planet via `/ar planet generate`, builds the
17×17 multiblock there, drives the cycle, asserts density drift.
Tests the **native-dim branch** of the gate. Test:
`TerraformerPoweredCycleOnArPlanetTest extends AbstractSharedServerTest`.

- `nativePlanetTerraformerWithFuelAndPowerStepsDensity` —
  generate planet (cleanup in `@After`), build full 17×17 fixture
  via `/artest fixture multiblock terraformer <newDim>`, inject
  fuel + power, force ticks, assert density delta ≠ 0.

- `nativePlanetTerraformerWithoutFuelDoesNotStep` — same setup,
  empty fuel hatch, force ticks, assert density unchanged.

- `nativePlanetTerraformerWithoutPowerDoesNotStep` — same setup,
  no power injected, force ticks, assert density unchanged.

### Phase 1b — Terraformer on non-AR dim with config flip (~2-3 h)

Tests the **`allowTerraformNonAR` branch** of the gate. Needs a
new probe verb `/artest config set <category> <key> <value>` (or
similar) since `ARConfiguration` is the runtime-flip vector. Test:
`TerraformerPoweredCycleOnOverworldTest extends AbstractSharedServerTest`.

- New probe verb in `TestProbeCommand` (~30-45 min): flip a
  whitelisted config key (e.g. `allowTerraformNonAR`) on the live
  `ARConfiguration` instance and restore in finally. Whitelist to
  prevent test pollution beyond terraformer-related fields.

- `overworldTerraformerWithNonArConfigFlipStepsDensity` —
  flip `allowTerraformNonAR=true`, build fixture on dim 0, inject
  fuel+power, force ticks, assert density delta ≠ 0, restore
  config in `@After`.

- `overworldTerraformerWithoutConfigFlipDoesNotStep` — leave
  config default-false, fuel+power+tick, assert density unchanged
  (counter-test pinning the gate's other side).

### Phase 2 — Black Hole Generator powered cycle (~3-4 h)

BHG's `update()` gates on `isAroundBlackHole()` — the test must
arrange that precondition. From production: this checks for a
"black hole" entity / block within range. Phase 2 sets that up via
`/ar` (if a planet-property toggle exists) OR spawns/places the
required entity directly.

Test: `BlackHoleGeneratorPoweredCycleTest extends AbstractSharedServerTest`:

- Setup: place the BHG fixture, then arrange the
  `isAroundBlackHole()` precondition (either toggle the
  `DimensionProperties.isBlackHole` flag via a new probe verb OR
  generate a planet via `/ar planet generate` with the black-hole
  flag set — to be confirmed in recon at Phase 2 start).

- `bhgAroundBlackHolePowersOutputBuffer` — full setup + N ticks,
  assert output energy hatch's `stored` increased.

- `bhgNotAroundBlackHoleDoesNotPower` — same fixture, no black
  hole, force ticks, assert stored unchanged.

- `bhgAtCapacityDoesNotOverflowNegative` — pre-fill energy hatch,
  force ticks, assert no negative-stored regression.

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

- [ ] Four new test classes (Phase 1a / 1b / 2 / 3), ~3 tests each
      = ~11-12 tests total.
- [ ] All assertions are loose-bound on numeric magnitudes (per
      SOP), tight on observable side-effects (densities change,
      stored increases, location appears).
- [ ] Phase 1b's new `config set` probe verb is whitelisted to
      terraformer-related keys (or has explicit policy comment
      enumerating the allowed keys) — avoids becoming a generic
      test-pollution vector.
- [ ] Phase 2 cleans up any generated black hole / planet in
      `@After`.
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

## Estimated effort (revised 2026-05-25 after Phase 1 recon)

- Phase 1a Terraformer (AR planet): ~3-4 h
- Phase 1b Terraformer (overworld + config flip + new probe verb): ~2-3 h
- Phase 2 BHG (incl. black-hole arrangement + likely new probe verb): ~3-4 h
- Phase 3 Beacon (incl. `/artest redstone set` if missing): ~3-3.5 h
- **Total**: ~11-14 h

Pre-revision estimate of 9-10 h underweighed:
1. The native-dim gate on terraformer (forced Phase 1 split).
2. The `isAroundBlackHole()` precondition on BHG (forces black-hole
   arrangement before the test can drive the powered cycle).
