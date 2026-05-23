# TASK-02: Functional coverage expansion

## Ticket

- Source: continuation of TASK-01 SMART pyramid; user ask 2026-05-18 —
  "I need to be sure all the core mod functionality is tested"
- Status: ✅ Completed (Phases 0-8, 11) — see `.agent/tasks/README.md` Done table. Phase 9 (companion-mod integration) + Phase 10 (visual regression) deferred without a successor ticket.
- Created: 2026-05-18
- Predecessor markers:
  - `.agent/.context-markers/2026-05-18-1530_task01-phase4-pyramid-complete.md`
  - `.agent/.context-markers/2026-05-18-1900_merge-fix-weather-into-feature-tests.md`

## Context

TASK-01 brought the SMART §7 scenarios up to prose-level depth (191 tests
across 4 layers). But SMART §7 was scoped to the user-visible *scenarios*
the audit author called out; it does **not** enumerate every subsystem in
the codebase. A structural audit on 2026-05-18 (Explore agent, recorded
in this task's "Audit notes" section below) shows ~480 source files split
across 24+ top-level packages; the 191 tests touch maybe 30–35 % of them.

This task closes the gap to "core functionality has a regression net you
can trust". It is intentionally **broader than SMART** — the SMART suite
proves the named scenarios work; TASK-02 proves the subsystems that
deliver those scenarios won't silently rot when someone changes a
neighbouring file.

Out of scope:
- Rendering pixel-fidelity tests (use visual-regression tooling, not JUnit).
- Mod-incompatibility coverage (covered by separate Phase 3).
- Performance / load tests.

**No production logic changes**, same rule as TASK-01: tests + probe
extensions only. If a test reveals a bug, mark `_documentsKnownBug` and
file a separate ticket.

## Implementation Plan

### Phase 0: Probe gap audit (~1 h, blocking nothing)

Before adding tests, scan `TestProbeCommand.handleX` methods and list what
probes are missing for each subsystem in the phases below. Carry the
findings into each phase's "Probe extensions" bullet so probe + test land
in the same session.

- [ ] Audit which `/artest` subcommands cover: event handlers (`/artest
      event …`?), gen (`/artest gen sample <dim> <cx> <cz> <expected>`),
      armor (`/artest armor air <player>`, `/artest armor breaktime <pos>`),
      recipes (`/artest recipe lookup <machine> <input>`), missions
      (`/artest mission state <id>`), stations
      (`/artest station info <id>`), networks
      (`/artest network energy/data/liquid <pos>`).
- [ ] Resolve the **`case "help"` advisory** carried over from TASK-01
      §5: add a uniform `help` sub to every `handleX`, returning
      `{"usage":"..."}` instead of an `{"error":...}` fallback. Lets
      future audits self-document.

### Phase 1: P0 — Event handlers (~6-8 h)

Event handlers are 600+ line classes that intercept core game logic
(dimension changes, rocket launches, world ticks). Currently 0 tests.
Highest risk: a subtle bug here corrupts game state across every player
session.

#### 1a — `PlanetEventHandler` (619L) — server tests, real harness

- [ ] **Probe extensions**: `/artest event playerJoinPlanet <player> <dim>`,
      `/artest event playerLeavePlanet <player> <dim>`,
      `/artest event tickCounter <dim>` (read tick count for sanity).
- [ ] `playerChangedDimensionTriggersExpectedSideEffects` —
      teleport player to AR planet → assert side effects (sky colour
      applied, gravity registered, ARWeatherWorldInfo present).
- [ ] `worldTickAdvancesOnLoadedArDim` — load AR dim, observe tickCounter
      increments after `/artest dim tick <id> 20`.
- [ ] `eventHandlerSurvivesUnloadedAdjacentDim` — load dim A, unload
      dim B, fire event on A — must not NPE on B's state.
- [ ] `eventHandlerThreadSafetyOnConcurrentTeleport` — two probe-driven
      teleports back-to-back in a single tick window; both produce
      coherent state.

#### 1b — `RocketEventHandler` (437L) — server tests

- [ ] **Probe extensions**: `/artest event rocketLaunch <id>`,
      `/artest event rocketLand <id> <dim>`.
- [ ] `rocketLaunchEventFiresExactlyOnce` — assemble + launch → check
      counter incremented by 1.
- [ ] `rocketLandEventFiresOnTargetDim` — launch with target dim → assert
      lander entity present on target dim post-event.
- [ ] `rocketEventDoesNotFireForUnassembledEntity` — spawn raw
      `EntityRocket` skipping assembly → fire tick → no spurious launch
      event.

#### 1c — Remaining event handlers (smaller, batched) — unit + server

- [ ] `CableTickHandler` (102L) — network tick advances pipe state.
- [ ] `EntityEventHandler` (52L) — entity capability attachment hook fires.
- [ ] `BlockBreakEvent` (36L) — block break preserves AR-tracked metadata.
- [ ] `WorldEvents` (15L) — `WorldEvent.Load` triggers
      `PlanetWeatherEventHandler.wrapWorldInfoIfNeeded` (regression net
      for the B1 wiring).

### Phase 2: P0 — World generation (~8-10 h)

Worldgen is 61 files; 0 tests; ships terrain on every AR dimension. Risk:
a layer change silently makes every planet generate the wrong biomes /
the wrong ore distribution / unreachable bedrock. Bugs surface as
modpack bug reports months later.

- [ ] **Probe extensions**: `/artest gen biome <dim> <x> <z>`,
      `/artest gen height <dim> <x> <z>`,
      `/artest gen ore <dim> <cx> <cz> <oreId>` (returns count in chunk),
      `/artest gen sample <dim> <cx> <cz> <yMin> <yMax>` (returns
      `{block: count}` histogram capped at top 10).
- [ ] **Determinism fixtures**: use `DETERMINISTIC_WORLD_SEED`
      (`AdvancedRocketryTestConstants`) for every gen scenario.
- [ ] `chunkProviderPlanetGeneratesExpectedHeightAt0_0`
- [ ] `chunkProviderPlanetIsDeterministicAcrossRestarts` — boot1 generate
      → record histogram → boot2 same seed → identical histogram
- [ ] `chunkProviderAsteroidsGeneratesScatteredAsteroids` — sample 3×3
      chunks; assert ≥N non-air blocks in expected y-range
- [ ] `chunkProviderSpaceIsMostlyAirAroundOrigin`
- [ ] `chunkProviderCavernGeneratesVoidsAtExpectedY`
- [ ] `genLayerBiomePlanetReturnsBiomeFromPlanetPalette` — assert biome
      ID is in the planet's configured palette, not vanilla overworld
- [ ] `chunkManagerPlanetReportsBiomeMatchingGenLayer` — `getBiome` agrees
      with `getBiomesForGeneration` at same coords
- [ ] `oreGenerationRespectsConfiguredOres` — set `oreProperties` on
      fixture planet → sample → expected ores present, unexpected absent

### Phase 3: P0 — Armor / suit / breathing (~4-5 h)

`ItemSpaceArmor` (289L), `ItemSpaceChest` (287L),
`EnchantmentSpaceBreathing` — player-survival critical. Currently 1-3
test refs total (just registry presence).

- [ ] **Probe extensions**: `/artest armor air <player>` (return current
      air units), `/artest armor capacity <player>`,
      `/artest armor breath-rate <player>`,
      `/artest enchant level <player> <enchant-id>`.
- [ ] `spaceArmorReducesVacuumAirConsumption` — equip suit → tick in
      vacuum dim → assert air drops slower than no-suit baseline
- [ ] `spaceArmorWithEmptyTankStopsProtecting` — drain tank → tick →
      damage starts accruing
- [ ] `gasChargePadRefillsSpaceArmorTank` — extend `2b §7.13` fixture:
      step on charge pad → tank fills
- [ ] `spaceBreathingEnchantBypassesVacuumDamage` (already in 2b §7.13,
      strengthen: also assert it bypasses *low-oxygen* not just vacuum)
- [ ] `spaceArmorChestStoresInventoryAcrossRespawn` — fill chest →
      `/kill` → respawn → inventory preserved
- [ ] `spaceArmorChestRejectsTooLargeStack` — capacity boundary check

### Phase 4: P1 — Tile machines depth (~10-12 h, splittable)

71 tile entity classes; only `MachineRecipeIntegrationTest` and
`MultiMachineControllerSmokeTest` touch them. Pick the top ~10 by
in-game importance and probe-test each in isolation.

- [ ] **Probe extensions**: `/artest tile state <pos>` (returns
      capability-exposed state map: power, fluid, items, enabled,
      progress, etc.), `/artest tile tick <pos> <ticks>` (force-ticks via
      `world.scheduledTicks`).
- [ ] `tileSolarPanel`: produces energy proportional to skylight in
      overworld; clamped to 0 in vacuum dim
- [ ] `tileFluidTank`: NBT round-trip preserves fluid stack
- [ ] `tilePump`: drains adjacent fluid block into internal tank;
      respects vanilla water source rules
- [ ] `tileForceFieldProjector`: state machine on/off transitions;
      energy drain proportional to projected area
- [ ] `tileGuidanceComputer`: writes target dim to chip; chip read-back
- [ ] `tileSuitWorkStation`: assemble suit from parts; partial assembly
      surfaces missing component error
- [ ] `tileUnmannedVehicleAssembler`: same idempotency / partial-fail
      contract as `tileRocketAssemblingMachine`
- [ ] `tileLandingPad` / `tileFuelingStation` — extend existing infra
      smokes to per-tile isolated tests
- [ ] `tileOxygenVent`: emits oxygen into sealed room; sealed-room
      detection respects `SealableBlockHandler`
- [ ] `tileCrystallizer` / `tileLathe` / `tileCentrifuge` /
      `tileElectrolyser`: machine recipe each — separately, not bundled

### Phase 5: P1 — Recipes (~3-4 h)

10 `Recipe*` classes (12L each, mostly data carriers). Test that each
machine resolves its registered recipes correctly + NBT/JSON round-trip.

- [ ] `recipeLatheResolvesByInput`
- [ ] `recipeCentrifugeResolvesMultiOutput`
- [ ] `recipeCrystallizerResolvesByFluid`
- [ ] `recipeElectrolyserResolvesByFluidPair`
- [ ] `recipePrecisionAssemblerResolvesByItemGrid`
- [ ] `recipeRollingMachineResolvesByInput`
- [ ] `recipeCuttingMachineResolvesByInput`
- [ ] `recipeChemicalReactorResolvesByFluidPair`
- [ ] `recipeArcFurnaceResolvesByInput`
- [ ] `recipeRegistryReturnsEmptyForUnknownMachine` — negative test
- [ ] **Round-trip**: write/load each recipe's JSON config — values
      preserved

### Phase 6: P1 — Missions (~3-4 h)

`MissionResourceCollection` (188L), `MissionGasCollection` (100L),
`MissionOreMining` (135L) — completely untested user-facing features.

- [ ] **Probe extensions**: `/artest mission start <id> <type> <args…>`,
      `/artest mission state <id>` (returns progress %), `/artest mission
      complete-now <id>` (force-completes for test determinism).
- [ ] `missionResourceCollectionAccrues` — start → tick → progress
      advances
- [ ] `missionGasCollectionRequiresGasCollectorSatellite` — without
      collector satellite, no progress
- [ ] `missionOreMiningRespectsAsteroidMinerOreSet`
- [ ] `missionPersistsAcrossServerRestart` — NBT save/load
- [ ] `missionCompletionGrantsRewardToSelectedPlayer`

### Phase 7: P1 — Pipe network handlers (~4-5 h)

3 `Handler{Energy,Data,Liquid}Network` classes; current pipe coverage
is 3 tests + 3 skipped. The handlers themselves (graph traversal, capacity
accounting) aren't tested in isolation.

- [ ] **Probe extensions**: `/artest network info <pos>` (returns network
      id, endpoint count, capacity, current load).
- [ ] `handlerEnergyNetworkAggregatesAcrossSegments`
- [ ] `handlerEnergyNetworkSplitsOnNodeBreak`
- [ ] `handlerEnergyNetworkMergesOnNodeJoin`
- [ ] `handlerLiquidNetworkBalancesFluidAcrossTanks`
- [ ] `handlerDataNetworkRoutesPacketShortestPath`
- [ ] `handlerNetworkSurvivesChunkUnloadReload`
- [ ] Un-skip the 3 currently `@Ignore`d pipe tests by reinstating the
      production blocks they depend on (separate ticket if reinstatement
      isn't trivial — file as blocker).

### Phase 8: P1 — Stations (~3 h)

`SpaceObjectManager` (64 refs), `SpaceStationObject`, `SpaceObjectBase`.
Existing `SpaceStationLifecycleSmokeTest` covers create-id-info but not
docking, fuel transfer, multi-station registry consistency.

- [ ] **Probe extensions**: `/artest station dock <stationId> <rocketId>`,
      `/artest station undock <stationId> <rocketId>`,
      `/artest station fuel <stationId> <amount>`.
- [ ] `multipleStationsCoexistInSameOrbit`
- [ ] `rocketDocksToStationAndAppearsInDockedList`
- [ ] `stationFuelTransferRespectsCapacity`
- [ ] `stationPersistsOrbitalParametersAcrossRestart`

### Phase 9: P2 — Integration compatibility (~4-6 h, optional)

51 files for GalacticCraft, MatterOverdrive, JEI compat. Risk: silent
compat breakage on third-party mod update. Tests must `Assume` the
companion mod is present (skip otherwise) — keeps base CI green.

- [ ] `jeiPluginRegistersExpectedCategories`
- [ ] `galacticCraftBridgeMapsArDimsToGcCompat`
- [ ] `matterOverdriveBridgeRegistersEnergyAdapter`
- [ ] `compatibilityMgrLoadsOnlyForPresentMods`

### Phase 10: P2 — Client rendering (~deferred, advisory)

`ClientProxy` (520L) + `ClientHelper` (742L) + the GUI screens not yet
covered. JUnit can only do so much for rendering; the right tooling is
visual regression (Storybook + Percy/Chromatic equivalent for MC).
Carry as a separate proposal — do **not** attempt to JUnit-test
rendering pixels. Acceptable scope here:

- [ ] `clientHelperUtilityMethodsAreDeterministic` — pure math/colour
      utilities have unit tests
- [ ] Additional GUI E2E tests for `OrbitalLaserDrillGui`,
      `OreMappingSatelliteGui` (mirror existing pattern from
      RocketBuilderGuiE2ETest).

### Phase 11: Final pyramid validation + report

- [ ] Run full pyramid; record actual deltas vs current 191 baseline.
- [ ] Update `src/test/README.md` SMART §7 table — adding any new
      scenario rows.
- [ ] EOD marker documenting which subsystems now have which depth of
      coverage; explicit list of intentionally-deferred items.

## Technical Decisions

- **Probe-first, then test** (same as TASK-01).
- **Test placement** (same convention): pure-math → `unit/`,
  Forge-bootstrap → `integration/`, real-server-fork → `server/`,
  real-client → `client/`.
- **Determinism**: every test that touches worldgen MUST use
  `AdvancedRocketryTestConstants.DETERMINISTIC_WORLD_SEED`; every
  scheduled-tick test MUST go through `/artest tile tick <pos> <ticks>`
  rather than relying on wall-clock.
- **No production logic changes** (same as TASK-01 §15). Tests that
  reveal a bug → `_documentsKnownBug` + separate ticket.
- **GL availability for testClient**: this Linux sandbox requires
  `DISPLAY=:77 LIBGL_ALWAYS_SOFTWARE=1` (Xvfb :77 has a connected output
  at 1920x1080; :99 has none, triggering LWJGL's
  `LinuxDisplay.getAvailableDisplayModes` NPE). See
  `sops/development/client-tests-on-linux.md` once written.

## Dependencies

**Requires**:
- TASK-01 complete (✅ as of `70410da4`).
- `feature/tests` includes B1 weather merge (✅ as of `7531bf2f`).
- `forge-test-framework` ≥ 0.4.2 in mavenLocal (carried from prior tasks).

**Blocks**:
- Closing the modpack-readiness gate: until major subsystems have a
  regression net, every modpack update is a gamble.

**Does NOT block**:
- Releases / merges to `1.12`. New tests gate by `Assume` on
  `forge.test.harness.enabled`, so a missing harness skips cleanly.

## Audit notes (2026-05-18 Explore agent)

~480 source files in `src/main/java/zmaster587/advancedRocketry/` across
24+ top-level packages. Existing 191 tests cover ~30–35 % of subsystem
breadth, concentrated in:

- ✅ Well-covered: dimension/, satellite/, util/math, atmosphere (oxygen
  side), weather (entire B1 path), unit/integration for
  XML loader and packet round-trip.
- ⚠ Partial: tile/ (smokes only), inventory/ (3 of N GUIs), cable/
  (3 of 6 pipe types), block/ (registry only), api/ (interface mostly).
- ❌ **Zero coverage**: event/ (6 handlers, 1 261 LoC), world/ (61 files
  worldgen), recipe/ (10 recipe classes), mission/ (3 mission classes,
  423 LoC), integration/ (51 files mod-compat), client/ (6 files,
  1 282 LoC), advancements/, armor & enchant beyond registry presence.

Hot files (large + many imports + no individual tests):
`ClientProxy.java` 520L, `ClientHelper.java` 742L,
`PlanetEventHandler.java` 619L, `RocketEventHandler.java` 437L,
`ItemSpaceArmor.java` 289L, `ItemSpaceChest.java` 287L,
`MissionResourceCollection.java` 188L, `AdvancedRocketryBlocks.java`
112L (70 imports), `TextureResources.java` 89L (61 imports).

## Completion Checklist

- [x] Phase 0 done; probe gaps documented (2026-05-18: station fuel +
      ore-stats AIR-fallback fix; uniform `case "help"` still optional)
- [x] Phase 1 covered (shallow `EventHandlerWiringTest` 2026-05-18 +
      deep `RocketLaunchEventTest` 2026-05-19 + player-event wiring
      `PlayerEventHandlerWiringTest` 2026-05-19 11:00 — 5 server tests
      covering tick-counter advance, handler class-load smoke, AR-dim
      pre-join side effects, non-AR counter-test, transition-queue
      invariant); full FakePlayer-driven dim-change side effects still
      deferred
- [x] Phase 2 worldgen has a regression net (2026-05-18 — 6 server +
      8 unit; cross-session determinism deferred)
- [x] Phase 3 armor/breathing covered (2026-05-18 — 20 unit tests)
- [x] Phase 4 done — `TileMachineDepthTest` 8 server tests covering
      solar generator, fluid tank, force field, guidance computer,
      oxygen vent, pump, satellite builder, sanity counter-test
      (2026-05-19). Round 2 (2026-05-19 11:00) adds
      `TileMachineDepthRound2Test` 6 server tests for suit workstation,
      UV assembler, landing pad, fueling station, terraformer
      pre-assembly + force-tick safety. Full 10+ tile depth essentially
      covered now.
- [x] Phase 5 done; recipes covered (2026-05-18, 10 unit tests)
- [x] Phase 6 done; missions covered (2026-05-18, 7 unit tests)
- [x] Phase 7 unit slice done (2026-05-18, 5 unit tests); deep handler
      contract covered in `PipeNetworkHandlerDeepTest` 2026-05-19 11:00
      (17 unit, including 3 `_documentsKnownBug` pinning real prod bugs
      in HandlerCableNetwork.mergeNetworks assert polarity, CableNetwork.merge
      addAll-before-dedup ordering, and EnergyNetwork.merge battery-migration
      cascade). End-to-end with PLACED pipes blocked by commented-out
      pipe block registrations (`AdvancedRocketry.java:782-787`).
- [x] Phase 8 done; stations depth extended (2026-05-18, 4 server + 7
      unit tests); dock/undock + cross-restart covered 2026-05-19 11:00:
      `SpaceStationDockUndockTest` (9 server tests) +
      `SpaceStationPadPersistenceTest` (1 multi-boot server test).
      6 new `/artest station` probe verbs (add-pad, remove-pad, pads,
      dock, undock, set-autoland).
- [ ] Phase 9 — DEFERRED (mod compat: companion mods not in this dev
      environment's classpath; tests would `Assume.assumeTrue(false)`
      trivially). Pick up when GC / MO / JEI are in scope.
- [ ] Phase 10 — DEFERRED (client rendering: JUnit is the wrong tool;
      needs visual-regression scaffolding ticket)
- [x] Phase 11 done (round-2 2026-05-19); EOD markers authored
- [x] Full pyramid PASS — testUnit 142/0/0, testServer 115/0/3,
      testIntegration 80/0/0, testClient 6/0/0 = **343/0/3** total
      (was 263/0/3 baseline). Target ≥300 hit; 3 SKIPs are pre-existing
      PipeNetworkSmokeTest blocks waiting for commented-out production
      paths to be reinstated.
- [x] Round 3 (2026-05-19 11:00) PASS — testUnit **159**/0/1,
      testServer **136**/0/3, testIntegration 80/0/0, testClient
      6/0/0 (or 1/0/0 headless) = **381/0/4** total (with DISPLAY)
      / 376/0/4 (headless). +33 tests over previous round, 0 failures.
      New testUnit SKIP is an Assume guard on
      `mergeNetworksProducesLowerIdSurvivor_assertionsDisabled` (JVM
      assertion flag can't be retroactively flipped post-class-init).

## Estimated effort

~50-65 hours across 12-15 sessions. Suggested order:

1. **Session 1** — Phase 0 (probe audit + `case "help"`), ~2 h.
2. **Session 2-3** — Phase 1 (event handlers).
3. **Session 4-5** — Phase 2 (worldgen).
4. **Session 6** — Phase 3 (armor / breathing).
5. **Sessions 7-9** — Phase 4 (tiles), split by category.
6. **Session 10** — Phase 5 (recipes).
7. **Session 11** — Phase 6 (missions).
8. **Session 12** — Phase 7 (networks).
9. **Session 13** — Phase 8 (stations).
10. **Session 14** — Phase 9 if pursued, else Phase 11 directly.
11. **Session 15** — Phase 11 (final validation + marker).
