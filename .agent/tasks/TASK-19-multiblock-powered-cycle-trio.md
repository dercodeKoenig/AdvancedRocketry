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

### Phase 1a — Terraformer on AR-native planet (~5 h) — ✅ shipped 2026-05-25

`TerraformerPoweredCycleOnArPlanetTest` — 3/3 tests passing:

- `nativePlanetTerraformerWithFuelAndPowerStepsDensity` — generates
  fresh AR planet, builds 17×17 fixture, splits fluid (N2 in
  hatches 0+1, O2 in 2+3), force-ticks 24000, asserts
  `currentAtmosphere` mutates (delta ≥ 1).
- `nativePlanetTerraformerWithoutFuelDoesNotStep` — same setup
  minus fluid injection; OOF gate holds, density unchanged.
- `nativePlanetTerraformerWithoutPowerDoesNotStep` — fixture's
  creative input plugs auto-provide infinite power, so the test
  uses the new `artest machine clear-batteries` probe to drain
  the controller's `MultiBattery` aggregator after enable;
  `hasEnergy` reads 0 thereafter; density unchanged.

**Probe additions** for Phase 1a (in `TestProbeCommand`):

- `artest machine controller-state <dim> <x> <y> <z>` — reflective
  dump of `batteries.getUniversalEnergyStored`, `batteriesCount`,
  `fluidInPortsCount`, `currentTime`, `outOfFluid`. Used to
  diagnose why progress stays 0 (initially turned out to be OOF
  because a single hatch held both N2 + O2 only as one fluid).
- `artest machine clear-batteries <dim> <x> <y> <z>` — clears
  controller's `MultiBattery` via reflection. Counter-tests need
  this because the default `'P'`-fixture places creative input
  plugs whose `getUniversalEnergyStored()` returns MAX
  unconditionally; "skip energy inject" alone doesn't simulate a
  no-power state.

**Key learnings for future powered-cycle tests**:

- `TileFluidHatch` holds **one fluid type** per tank. The
  terraformer's drain logic walks all `fluidInPorts` looking for
  BOTH N2 and O2 each tick — must distribute fluids across
  multiple hatches.
- The default `'P'`-fixture is creative-powered. To exercise the
  no-power branch, use `clear-batteries` (don't rely on
  skip-inject).
- `getCompletionTime() = 18000 × terraformSpeed`; default speed 1
  → ~18000 ticks per density step. Tests need 20000+ force-ticks
  + fluid refill loop (single hatch caps at 16000 mB, drains 40
  mB/t).

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

### Phase 1b — Terraformer on overworld with config flip (~2 h) — ✅ shipped 2026-05-25

`TerraformerPoweredCycleOnOverworldTest` — 2/2 tests passing:

- `overworldTerraformerWithNonArConfigFlipStepsDensity` — flips
  `allowTerraformNonAR=true` via the new `artest config set`
  probe, builds fixture on dim 0 (overworld, `WorldProviderSurface`),
  same fuel+power+tick pipeline as Phase 1a, asserts density
  mutates.
- `overworldTerraformerWithoutConfigFlipDoesNotStep` — counter-test
  with `allowTerraformNonAR=false` (default); same fixture+inputs;
  asserts density unchanged. Pins the gate's blocking side.

**Probe addition** for Phase 1b:

- `artest config <get|set> <key> [value]` — whitelisted ARConfiguration
  field access via reflection. Whitelist:
  `allowTerraformNonAR`, `terraformRequiresFluid`. Tests MUST restore
  the original value in `@After`. The whitelist comment in
  `TestProbeCommand.CONFIG_WHITELIST` is the SSOT for new keys —
  add a key there only when a test actually needs it.

**State-isolation pattern**:

- `@Before` snapshots `allowTerraformNonAR` + dim 0's current
  atmosphere density via new `artest config get` + existing
  `artest terraforming info`.
- `@After` restores both unconditionally. The shared harness keeps
  one JVM across all methods of this class — leaked config or
  density would corrupt subsequent methods.

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
