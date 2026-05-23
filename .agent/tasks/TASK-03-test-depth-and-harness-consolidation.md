# TASK-03: Test-depth deepening + harness consolidation

## Ticket

- Source: user ask 2026-05-19 — "проанализируем глубину всех наших тестов" + "какие
  real-тесты можно объединить, чтобы не запускать отдельные инстансы".
- Status: ✅ Completed partial — A1/A2/A4/A5/A6/A7 + B1/B2/B4/C shipped; A2 tail + B3 absorbed by TASK-10; A3 reframed as testClient e2e in TASK-10b. See `.agent/tasks/README.md` Done table.
- Created: 2026-05-19 11:30
- Predecessor markers:
  - `.agent/.context-markers/2026-05-19-1100_task02-phase4r2-phase1-phase7-phase8-eod.md`

## Context

TASK-02 brought the suite to **381/0/4** total tests. Honest audit of round-3
additions (and a sample of older ones) revealed that ~30 % of what we labelled
"depth" is actually **wiring smoke**: tests that catch class renames, registry
drift, or capability removal, but pass cleanly when the underlying gameplay
logic regresses. This task pushes the named-as-deep tests into actually-deep
territory, and in parallel reorganises the test runner so the wall-time cost
stays acceptable as the suite grows.

Out of scope:
- New subsystems (covered by future TASKs).
- Visual / pixel-level regression (Phase 10 separate proposal).

**No production logic changes** (same rule as TASK-01 §15) for the depth phase.
The known-bug pins in `PipeNetworkHandlerDeepTest` already flag the three
prod bugs to be fixed in a *separate* ticket if/when chosen.

The harness-consolidation phase IS a test-infrastructure change but stays
within `src/test/`: new abstract base class + opt-in migration; existing
per-method harness behaviour preserved as default.

## Implementation Plan

### Phase A: Depth deepening — replace wiring smoke with real logic exercise

Order: high-impact first.

#### A1 — Real rocket launch path (`RocketLaunchEventTest`) ~3-4 h

Current `launchInstantRespondsOkAndEchoesMode` (TASK-02 round 2) explicitly
admits it can only pin the wiring contract: a fixture rocket in mid-air has
no launchpad, so the production `rocket.launch()` early-exits and
`isInFlight` stays false. The main rocket-launch flow is therefore
**uncovered**.

- [ ] Extend `/artest fixture rocket simple` (or new sibling
      `fixture rocket on-pad`) to place the fixture **on a real launchpad
      multiblock** — a `dockingPad` block under the rocket builder, with a
      configured target dim in the guidance computer.
- [ ] New test `launchInstantOnRealPadActuallyTakesOff`:
      build → assemble → launch (no force) → assert `isInFlight=true`
      via the production `rocket.launch()` path (not the bypass).
- [ ] New test `launchSetsTargetDimAndPropagatesToPostLaunchInfo`:
      assert guidance-computer-supplied target dim survives into
      `/artest rocket info`.
- [ ] New test `launchWithEmptyFuelStaysGrounded`: production gate —
      no fuel, no flight. Currently the test fixture has `fuelFill=true`
      so this path is invisible.
- [ ] Counter-test `launchTwiceFromSamePadBlocksSecondAttempt`:
      pin the pad's occupied-state-guards-launch contract.

#### A2 — Real Phase 4 tile-machine logic (vs round-2 placement smoke) ~5-6 h

`TileMachineDepthRound2Test` pins class FQNs + capability presence. Real
behavioural depth needs per-tile fixtures + per-tile success criteria.

- [ ] **Fueling station**: `EnergySystemsSmokeTest`-style. Place
      fueling station next to a fueled rocket → tick N → assert rocket
      fuel rises AND station fuel falls (matched accounting). Counter-test:
      empty energy → no fuel transfer.
- [ ] **Suit workstation**: load slots with valid suit-component recipe →
      tick → assert assembled suit appears in output slot. Counter-test:
      missing one component → output stays empty.
- [ ] **UV assembler vs RocketAssemblingMachine**: pin the actual
      behavioural divergence between them (parent has manned-launch logic;
      UV variant deploys unmanned probes). Without a divergent test,
      a regression that flattens the override is invisible.
- [ ] **Solar generator under skylight vs vacuum**: extend
      `EnergySystemsSmokeTest` — place at y=100 above overworld AND in a
      vacuum dim → assert RF accumulates in overworld, **stays at zero**
      in vacuum. Skylight gate is currently untested.
- [ ] **Liquid tank NBT round-trip**: fill with water → save-all → read
      back via `/artest fluid stored`. Doesn't need restart (TileFluidTank
      serialises on chunk save) — `/artest tile force-tick` then a fresh
      probe call exercises the NBT path.

#### A3 — FakePlayer for real player-event tests ~6-8 h

`PlayerEventHandlerWiringTest` proves the handler is **alive** but never
fires a real `PlayerChangedDimensionEvent`. To close the gap:

- [ ] Add `/artest fakeplayer create <name>` + `/artest fakeplayer
      teleport <name> <dim> <x> <y> <z>` probes. Forge has
      `FakePlayerFactory` — the probe wraps it.
- [ ] **`playerJoinArDimAppliesSideEffectsImmediately`** — create
      fake player, teleport into AR dim → assert
      `oxygen player <name>` reports the AR atmosphere (proves dim-side
      AtmosphereHandler is applied to the player on join).
- [ ] **`playerLeavingArDimReleasesAtmosphereTracking`** — counter.
- [ ] **`playerInSpaceDimWithoutStationGetsForceTeleported`** — drive
      the production fallback in `PlanetEventHandler.playerTick` (line
      ~210): fake player in spaceDim with NO station underneath →
      tick → assert player either moved to a station spawn point OR
      to dim 0 (production has both branches).
- [ ] **`playerOnLunaTriggersWentToTheMoonAdvancement`** — fake player
      at the exact Luna trigger coords → tick 20 → assert advancement
      granted via `/advancement test <name> advancedrocketry:wenttothemoon`
      (already a vanilla command).

#### A4 — Pad persistence: tighten the soft Assume ~30 min

`SpaceStationPadPersistenceTest` currently has
`Assume.assumeTrue("padB allowAutoLand did NOT survive — ... ", false)` —
it silently skips when allowAutoLand doesn't restore. Either:

- [ ] Confirm allowAutoLand DOES survive: drop the Assume, replace with
      hard `assertTrue`.
- [ ] OR confirm it does NOT: convert to `@Test(expected = AssertionError.class)`
      OR `_documentsKnownBug` style — explicitly pin the gap in
      `SpaceStationObject.writeToNBT`'s spawnLocations branch.

Same treatment for pad NAME (currently asserts the (x,z) coords survive
but not the name — names persist in production NBT but never asserted).

#### A5 — Dock/undock cause-effect (vs API bookkeeping) ~3 h

Current `SpaceStationDockUndockTest` exercises the production state-machine
methods through thin probes. The CHAIN "real rocket lands on pad → pad
occupied flag flips" is not covered — only "calling setPadStatus flips it".

- [ ] **`assembledRocketLandingOnPadMarksPadOccupied`** — build pad +
      auto-land-enable + teleport assembled rocket above pad + tick
      landing path → assert pad reports occupied=true. Doesn't need a
      real player.
- [ ] **`rocketTakeoffFromPadMarksPadFree`** — converse.

#### A6 — Drop the null test + clean up Assume guards ~30 min

- [ ] Delete `mergeNetworksProducesLowerIdSurvivor_assertionsDisabled`
      from `PipeNetworkHandlerDeepTest` — it Assume-skips when -ea is on
      (always, under Gradle) and the documents-known-bug counterpart
      already covers the path.
- [ ] OR re-target it to `testServer` tier (assertions off there) and
      flip to a real assertion.

#### A7 — Tick-based depth for empty-network smokes ~1 h

`tickOnEmpty{Energy,Liquid}NetworkIsNoOp` only assert "doesn't throw".
Replace with assertions that exercise the **non-empty** tick path's actual
behavior: register a fake source + fake sink → tick → assert energy /
fluid moved. Without real placed pipes the sink/source need to be plain
TileEntity stubs that expose IEnergyStorage / IFluidHandler via the
capability registration system — feasible at unit tier with anonymous
TileEntity subclasses.

### Phase B: Harness consolidation — share dedicated-server JVMs across methods

**Today's cost model**:
- `AbstractHeadlessServerTest` does `@Before` server boot + `@After` shutdown.
- Per-test-method dedicated-server JVM startup ≈ 10-15 s.
- 136 server tests = 136 server JVM spawns. With `parallelForks=3`, wall
  time ≈ 17 min (current measured).
- Gradle `forkEvery(1)` puts each test class in its own JVM, so even
  state internal to a class doesn't help.

**Target**: group test classes whose methods are independent (unique
positions / unique ids / read-only probes) under a shared-server base
class. Estimated wall-time saving ≈ 30-50 %.

#### B1 — `AbstractSharedServerTest` base class (~2 h)

Mirror `AbstractHeadlessServerTest` but with `@BeforeClass`/`@AfterClass`
lifecycle:

```java
public abstract class AbstractSharedServerTest {
    private static RealDedicatedServerHarness shared;

    @BeforeClass
    public static void startSharedHarness() throws Exception {
        Assume.assumeTrue("...", harnessEnabled());
        shared = RealDedicatedServerHarness.start();
    }
    @AfterClass
    public static void stopSharedHarness() throws Exception {
        if (shared != null) shared.close();
    }
    protected static TestClient client() { return shared.client(); }
}
```

- [ ] Author the base class.
- [ ] Document the contract loudly: every `@Test` method MUST be
      position-isolated and state-cleanup-disciplined. Persistence-style
      tests stay on `AbstractHeadlessServerTest`.
- [ ] Add a CI smoke that flags any test class extending the shared base
      whose methods declare incompatible state (e.g. `set-density 0`).

#### B2 — Migrate clear "independent-method" candidates (~3-4 h)

Top candidates by per-class boot saving (methods × ~12s saved per extra
method):

| Class | Methods | Saved per class |
|---|---|---|
| SatelliteLifecycleSmokeTest | 11 | ~120 s |
| RocketAssemblySmokeTest | 9 | ~100 s |
| SpaceStationDockUndockTest | 9 | ~100 s |
| TileMachineDepthTest | 8 | ~85 s |
| RocketInfrastructureSmokeTest | 8 | ~85 s |
| PlanetDimensionLoadTest | 8 | ~85 s |
| PipeNetworkSmokeTest | 7 | ~70 s |
| TileMachineDepthRound2Test | 6 | ~60 s |
| WorldgenDeterminismAndSamplingTest | 6 | ~60 s |
| SpaceStationDepthTest | 5 | ~60 s |
| PlayerEventHandlerWiringTest | 5 | ~60 s |
| RocketLaunchEventTest | 4 | ~50 s |
| CommandsSmokeTest | 4 | ~50 s |
| Total raw savings | | **~985 s** |

With `parallelForks=3` wall-time gain ≈ 330 s ≈ **5-6 min off the 17-min
run**.

- [ ] One class at a time: switch base, run twice (cold + warm), verify
      no test added regresses.
- [ ] Audit each class's methods for hidden state coupling (esp.
      `AtmosphereOxygenSmokeTest` — set-density 0 leaks across methods;
      that one stays on the per-method base).

#### B3 — Suite-grouping for single-method "smoke" classes (~2 h)

14 single-method `*SmokeTest` classes spawn 14 servers today. Most are
just "registry/handler/block X is registered and probable". Group by
domain:

- `ServerBootSmokeSuite`:
  ServerStartupSmokeTest + RegistrySmokeTest + CommandsSmokeTest +
  HarnessDiagnosticTest + NonARDimensionIsolationTest
- `RocketDomainSmokeSuite`:
  RocketLaunchSmokeTest + RocketInfrastructureSmokeTest +
  RocketInfrastructureLinkPersistenceTest (latter needs own boot — split
  inside suite)
- `MachineDomainSmokeSuite`:
  MultiMachineControllerSmokeTest + MultiblockValidationSmokeTest +
  EnergySystemsSmokeTest + SealedRoomOxygenVentTest +
  SuitVacuumSubsystemSmokeTest + SpecialInfrastructureSmokeTest +
  ForceFieldProjectionSmokeTest + MicrowaveReceiverSmokeTest +
  BlackHoleGeneratorSmokeTest
- `StationDomainSmokeSuite`:
  SpaceStationLifecycleSmokeTest + SatelliteLifecycleSmokeTest +
  ServerStartupSmokeTest pieces, etc.

- [ ] Author one suite per domain — methods preserve the original
      assertions verbatim (mechanical pull-up), name preserved as
      `{originalTestName}_{domain}` to keep failure messages helpful.
- [ ] Delete the redundant single-method classes after migration.
- [ ] Cross-check `testAdvancedRocketryScenarios` SMART §11 references
      against renames — the scenario IDs ship in the SMART doc.

Estimated additional wall-time saving: ~120 s wall (10 boot saves at 3-way
parallelism).

#### B4 — Client tier: investigate sharing (~2 h, advisory)

`AbstractClientE2ETest` spawns BOTH a server and a client JVM per method.
Client JVM cold-start cost ≈ 30-40 s. Even sharing 2-3 methods per class
is worth it. But: the client is harder to keep clean across methods
(GUI state, packet history, render state). Out-of-scope deep-dive for this
phase — capture findings in a follow-up SOP. Action item:

- [ ] One-page SOP `sops/development/sharing-client-harness.md` with a
      list of which client tests COULD share and which can't, and the
      risk inventory (packet state, GUI back-stack, etc.).

### Phase C: Wall-time validation (~1 h)

- [ ] Three-run measurement before / after Phase B: cold, warm, warm.
- [ ] Update `src/test/README.md` perf section with the new timings.
- [ ] Update this task's marker with the actual delta.

## Technical Decisions

- **Same "no production changes" rule** as TASK-01/02 §15 for Phase A
  (depth deepening).
- **New base class, not modify existing**: keep `AbstractHeadlessServerTest`
  for tests with hard isolation needs (persistence-style, weather-state
  mutations). Migration is opt-in.
- **Position isolation contract for shared-harness tests**: every test
  method places blocks at unique (x,z); every station/satellite create
  uses a fresh id (already true in production — they're auto-allocated).
  No test in the shared pool should mutate global state like atmosphere
  density or weather without resetting in `@After`.
- **Probe extensions for Phase A**:
  - `/artest fixture rocket on-pad` (or extend `simple` with optional pad)
  - `/artest fakeplayer create | teleport | tick | destroy`
  - none for A4-A7 (use existing probes)

## Dependencies

**Requires**:
- TASK-02 complete (✅ as of 60b1fd65 on `feature/tests`).
- `forge-test-framework` ≥ 0.4.2 (carried).

**Does NOT block**:
- Any feature work — Phase A is test-only, Phase B is harness-only.

## Estimated effort

~25-30 hours across 6-8 sessions:

1. **Session 1** — A1 (rocket launch on real pad).
2. **Session 2-3** — A2 (Phase 4 machine depth, splittable by tile).
3. **Session 4-5** — A3 (FakePlayer probe + player-event tests).
4. **Session 6** — A4 + A5 + A6 + A7 (assorted cleanups + cause-effect).
5. **Session 7** — B1 + B2 (shared base + migrate top-paying classes).
6. **Session 8** — B3 + B4 + C (suite-grouping + client SOP + perf measurement).

## Completion Checklist

- [x] A1 — real rocket launch path tested (no force bypass) —
      `RocketLaunchDepthTest` 6 server tests (2026-05-19 12:00)
- [x] A2 — partial: solar insolation depth (2 tests). Suit
      workstation / UV-assembler / fueling-station-with-rocket /
      fluid-tank NBT deferred to follow-up.
- [ ] A3 — FakePlayer probe + four player-event behaviour tests
      DEFERRED — ~6-8 h budget, needs dedicated session
- [x] A4 — pad-persistence Assume guards tightened; documented
      production bug at SpaceStationObject:801 via `_documentsKnownBug`
- [x] A5 — `RocketStationCauseEffectTest` 5 server tests covering
      gc.overrideLandingStation → station-side pad state
- [x] A6 — `mergeNetworksProducesLowerIdSurvivor_assertionsDisabled`
      removed (was always Assume-skipped under -ea)
- [x] A7 — empty-network tick tests replaced; +6 meat-path tests using
      CapabilityRecordingTile stub
- [x] B1 — `AbstractSharedServerTest` authored with subclass contract
      doc (position-isolated, fresh ids, no state-leak)
- [x] B2 — 16 multi-method classes migrated; full server suite
      verified ✅
- [ ] B3 — DEFERRED (~120 s wall saving, diminishing returns vs the
      disruption of moving methods across classes)
- [x] B4 — `sops/development/sharing-client-harness.md` SOP authored
- [x] C — testServer wall time **17m 01s → 8m 27s ≈ 50 % reduction**
      (well above the ≥30 % target)
- [x] EOD marker authored:
      `2026-05-19-1230_task03-A-and-B-mostly-done-eod.md`
- [x] Full pyramid PASS — testUnit 162/0/0, testIntegration 80/0/0,
      testServer 150/0/3, testClient 6/0/0 = **398/0/3** total
      (one intermittent flake in `ForceFieldProjectionSmokeTest` — pre-
      existing, untouched by this task, documented in marker)
