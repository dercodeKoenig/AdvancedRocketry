# TASK-19: Multiblock powered-cycle (Terraformer / BHG / Beacon enable)

## Ticket

- Source: 2026-05-23 audit — Gaps #4, #5, #6. Three multiblocks
  with structure-validation coverage but no powered-cycle pin.
- Status: ✅ **Completed 2026-05-25**.
- Created: 2026-05-23.
- Shipped: 11 server-tier tests (3+2+3+3) + 5 probe verbs
  (`machine controller-state`, `machine clear-batteries`,
  `config get/set`, `star get/set-blackhole`).

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

### Phase 2 — Black Hole Generator powered cycle (~3 h) — ✅ shipped 2026-05-25

`BlackHoleGeneratorPoweredCycleTest` — 3/3 tests passing:

- `bhgOnStationAroundBlackHoleProducesEnergy` — flips Sol star 0
  to black hole, creates station orbiting Sol (dim 10000), queries
  station spawn coords, builds fixture on space dim (-2) at those
  coords, feeds 64 dirt to input hatch, set-enabled true, force-ticks
  600, asserts output buffer accumulated > 0 RF.
- `bhgWithoutBlackHoleStarDoesNotProduce` — same setup but Sol
  black-hole flag stays false; same tick budget; asserts output
  buffer unchanged. Pins the `isStar() && isBlackHole()` branch.
- `bhgOnOverworldDoesNotProduceEvenWithBlackHoleStar` — counter-
  test: Sol IS a black hole, but BHG built on dim 0 (overworld,
  not spaceDimId); `isAroundBlackHole()` short-circuits on the
  first guard (`dim == spaceDimId`). Pins the dim-gate.

**Probe addition** for Phase 2:

- `artest star <get|set-blackhole> <starId> [value]` — reads or
  mutates a `StellarBody`'s black-hole flag via the public
  `setBlackHole` / `isBlackHole` API. Sol star 0 flag MUST be
  restored in `@After` — leaking a black-hole Sol would corrupt
  sky-render and orbital-mechanics paths in sibling tests.

**Key learning**:

- `TileBlackHoleGenerator.isAroundBlackHole()` requires THREE
  things — space dim placement + space station at coords + that
  station orbiting a black-hole star. Tests must arrange all three
  via existing probes (`artest station create`,
  `artest dim load -2`) plus the new `artest star set-blackhole`.

### Phase 3 — Beacon enable cycle (~2 h) — ✅ shipped 2026-05-25

`BeaconEnableCycleTest` — 3/3 tests passing:

- `enabledBeaconRegistersLocation` — generates fresh AR planet
  (shared via `@BeforeClass` since beacon registry doesn't
  cross-pollinate between coords), builds fixture, try-completes,
  set-enabled true, asserts controller pos appears in
  `DimensionProperties.beaconLocations`.
- `disabledBeaconDoesNotRegister` — set-enabled false (idempotent
  with default), asserts pos absent.
- `breakingControllerBlockUnregisters` — enable + register, then
  `artest place ... minecraft:air` replaces the controller block;
  Forge's `BlockBeacon.breakBlock` callback fires and calls
  `removeBeaconLocation`. Asserts pos absent post-break.

**No new probe verbs needed** — Phase 3 reuses:
- `ar planet generate` / `dim load` (Phase 1a infra),
- `fixture multiblock beacon` (existing, TASK-04),
- `machine try-complete` / `set-enabled` (existing),
- `place` (existing),
- `beacon list <dim>` (existing, TASK-13 era).

**Why AR-native planet required**: `TileBeacon.setMachineEnabled` and
`BlockBeacon.breakBlock` both guard the registry mutation behind
`isDimensionCreated(dim)`. Overworld returns false; the tests would
pass trivially (no mutation) without exercising the contract.

**Note on the original plan's "redstone" framing**: the task plan
listed `/artest redstone set <pos> 15` as needed infra. Production
reality (verified at Phase 3 start): the beacon's redstone block is
INSIDE the multiblock structure (top of the 5-tall pillar). Once the
fixture validates, "powered" means `setMachineEnabled(true)` — there's
no external redstone trigger. No `/artest redstone set` verb shipped;
the existing `machine set-enabled` covers the contract entirely.

## Acceptance

- [x] Four new test classes (Phase 1a / 1b / 2 / 3), 11 tests total
      (3+2+3+3).
- [x] All assertions are loose-bound on numeric magnitudes (per
      SOP), tight on observable side-effects (densities change,
      stored increases, location appears, location removed).
- [x] Phase 1b's `config set` probe verb is whitelisted to
      `allowTerraformNonAR` + `terraformRequiresFluid` — single
      constant in `TestProbeCommand.CONFIG_WHITELIST` is the SSOT.
- [x] Phase 2 restores Sol star 0's black-hole flag in `@After`.
- [x] Phase 1a + 1b restore atmosphere density + config flags in
      `@After`; Phase 1a + 3 delete generated planets.
- [x] Pyramid counter regenerated: 708 (237 / 80 / 350 / 41).

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
