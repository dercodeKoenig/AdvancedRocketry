# TASK-10: TASK-03 deferred tail — A2 remainder + B3 suite-grouping

> **History (2026-05-19)**: an earlier draft of this task included a
> "FakePlayer probe" (Phases 1-2) for player-behaviour coverage on the
> headless dedicated-server harness. That direction was rejected — the
> project already has a `testClient` source set (§2.4 real GL client +
> dedicated server) that is the correct layer for any "real player"
> behaviour. Phases 1-2 were shipped on `feature/tests` (commit
> `d0c3cba`) and then reverted (commit `df2b927`).
>
> If a player has to participate in a test, the test belongs in
> `src/test/java/zmaster587/advancedRocketry/test/client/` and runs
> under `./gradlew testClient`. Do NOT reintroduce a FakePlayer probe.

## Ticket

- Source: TASK-03 EOD (2026-05-19) — remainder of A2 (suit / UV /
  fueling / NBT) and B3 (suite-grouping single-method smokes) deferred.
  Originally bundled with A3 (player-event tests via FakePlayer); A3 is
  now out of scope here — it's a `testClient` job, see TASK-10b proposal.
- Status: Pending
- Created: 2026-05-19
- Revised: 2026-05-19 (FakePlayer direction reverted)
- Predecessor: `.agent/.context-markers/2026-05-19-1530_task07-rocket-flight-cycle-eod.md`

## Context

The TASK-03 audit deferred two clusters that don't require a player:

1. **A2 remainder** — heavy tile depth (suit assembly, UV assembler,
   fueling station, fluid-tank NBT). These exercise tile-entity logic
   only — no `EntityPlayer` is needed; the existing `testServer` harness
   is sufficient.
2. **B3 suite-grouping** — 14 single-method `*SmokeTest` classes each
   spawn their own JVM. Grouping them by domain cuts wall-time.

A3 (player-behaviour tests — atmosphere apply, advancement grant, etc.)
is **out of scope** for this task. It will be planned separately as a
`testClient` e2e expansion (proposed TASK-10b — see [tasks/README.md](./README.md)).

**No production logic changes** (same rule as TASK-01 §15).

## Implementation Plan

### Phase 1: A2 remainder — heavy tile depth (~5-6 h)

- [ ] `SuitWorkStationAssemblesSuit` — fill component slots with
  fixtures, tick, assert assembled suit in output.
- [ ] `UvAssemblerDivergesFromRocketAssembler` — pin behavioural
  delta in the unmanned-vehicle override.
- [ ] `FuelingStationFuelsAdjacentRocket` — place fueling station
  next to fueled rocket, link, tick, assert rocket fuel rises and
  station fuel falls (matched accounting).
- [ ] `FluidTankNBTRoundTripsAcrossRestart` — multi-boot test.

### Phase 2: B3 — suite-group single-method smokes ✅ COMPLETE

Merged single-method `*SmokeTest` classes into shared-harness suites
(one server boot per suite class instead of one per smoke class).

- [x] `MachineDomainSmokeSuite` — 9 classes → 1
  (MultiMachineControllerSmokeTest, MultiblockValidationSmokeTest,
  EnergySystemsSmokeTest, SealedRoomOxygenVentTest,
  SuitVacuumSubsystemSmokeTest, SpecialInfrastructureSmokeTest,
  ForceFieldProjectionSmokeTest, MicrowaveReceiverSmokeTest,
  BlackHoleGeneratorSmokeTest).
- [x] `ServerBootSmokeSuite` — 2 classes → 1
  (ServerStartupSmokeTest, RegistrySmokeTest). CommandsSmokeTest
  already shared; HarnessDiagnosticTest and NonARDimensionIsolationTest
  must remain per-method (diagnostic / requires-pristine-JVM).
- [x] `RocketDomainSmokeSuite` — **SKIPPED**. The only single-method
  class in this domain is `RocketLaunchSmokeTest`; wrapping a single
  class saves zero JVM-boots. `RocketInfrastructureSmokeTest` already
  uses the shared harness since TASK-03 B2.
- [x] Wall-time delta measured:
  - pre-merge baseline: testServer ~8 m 27 s
  - post-merge:        testServer    7 m 59 s
  - delta:             −28 s wall (parallelism diluted the per-JVM-boot
    saving; full-serial save would be ~108 s with 9 merged JVMs).
  - MachineDomainSmokeSuite: 9 / 9 PASSED in 16.3 s.

### Phase 3: Cross-cutting + EOD (~1 h)

- [ ] Full pyramid PASS.
- [ ] EOD marker.

## Technical Decisions

- Suite-grouping for B2/B3: preserve original test names verbatim as
  method names in the suite class (so failure messages stay grep-able).
- Suit / UV / fueling tests rely on the existing tile-entity fixture
  pattern from TASK-04 multiblock phase 1 — no new harness machinery.

## Dependencies

**Requires**: TASK-03 base.
**Does NOT block**: TASK-04, TASK-05, TASK-06, TASK-07, TASK-08, TASK-09.
(Note: in the original draft TASK-05 / TASK-06 were marked as
"soft-deps on FakePlayer". They are not — see TASK-05 / TASK-06 docs
for the testClient-based plan.)

## Estimated effort

~8-10 hours across 2-3 sessions.

## Completion Checklist

- [ ] 4 deep-tile tests (A2 remainder)
- [x] Single-method-smoke suites grouped (B3): 2 suites shipped
      (Machine + ServerBoot); Rocket suite dropped as not useful.
- [x] Wall-time delta measured for B3
- [ ] Full pyramid PASS
- [ ] EOD marker
