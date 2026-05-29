# TASK-10: TASK-03 deferred tail — A2 remainder + B3 suite-grouping

**Status: ✅ Completed (2026-05-19; index sync 2026-05-21)**

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
- Status: ✅ Completed (2026-05-19; index sync 2026-05-21)
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

### Phase 1: A2 remainder — heavy tile depth (~5-6 h) ✅

All four tests shipped. Two required small `/artest` probe-surface
additions documented inline below.

- [x] `FluidTankNBTRoundTripsAcrossRestartTest` — two-boot persistence
  pin for libVulpes FluidTank NBT format on AR's TileFluidTank.
  Boot 1 places `liquidTank`, injects 7 500 mB oxygen, closes harness
  (drives chunk-save). Boot 2 reopens same workDir, asserts fluid +
  amount round-tripped exactly.
- [x] `UvAssemblerDivergesFromRocketAssemblerTest` — class-identity
  pin: `rocketBuilder` and `deployableRocketBuilder` register distinct
  tile classes (TileRocketAssemblingMachine vs
  TileUnmannedVehicleAssembler). Catches any regression that collapses
  UV onto the crewed code path. Deeper behavioural pin (pad bounds,
  spawned entity type, fuel requirement) would need a dedicated
  `/artest assembler bounds` verb — left as a future tightening.
- [x] `SuitWorkStationAssemblesSuitTest` — places a `suitWorkStation`,
  puts a `spaceChestplate` in slot 0 and a `jetPack` in slot 1, then
  asserts (via the new `/artest hatch read ... nbt` option) that the
  chestplate's NBT now contains the jetpack registry reference
  (`outputItems:[{Slot:0b,id:"...:jetpack",...}]`) and that slot 1
  read-throughs to `getComponentInSlot(armor, 0)`.

  Required probe additions:
  - `/artest hatch read ... nbt` — optional 6th arg dumps each slot's
    `getTagCompound().toString()` Mojangson into the JSON response
    (~15 LOC in `handleHatch`).
  - `/artest tile init-modules <dim> <x> <y> <z>` — calls
    `IModularInventory.getModules(0, null)` on the tile, swallowing
    any NPE from player-using modules. Needed because
    `TileSuitWorkStation.setInventorySlotContents(0, ...)` iterates
    `slotArray`, which is populated only on GUI open (~30 LOC in
    `handleTile`).
  - **Production-side finding pinned**: server-tier `slot 0` mutation
    NPEs before any GUI open. Test bypasses via `init-modules`; a
    future production fix should null-check `slotArray` entries.

- [x] `FuelingStationFuelsAdjacentRocketTest` — builds a rocket
  fixture, assembles it, places a `fuelingStation`, links via
  `/artest infra link`, injects `rocketFuel` + RF, force-ticks the
  station, asserts station tank drained AND rocket's
  `LIQUID_MONOPROPELLANT` amount rose (matched accounting).

  Required probe addition:
  - `/artest rocket fuel <entityId>` — exposes per-FuelType
    `getFuelAmount` + `getFuelCapacity` + `getRocketFuelType()`. New
    branch in `handleRocket` (~30 LOC).

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

### Phase 3: Cross-cutting + EOD (~1 h) ✅

- [x] Full pyramid PASS — testServer 179 / 0 / 3 at 8 m 30 s.
- [x] EOD marker `2026-05-19-1745_task10-redone-without-fakeplayer.md`.

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

- [x] 4 deep-tile tests (A2 remainder): all shipped (FluidTank NBT,
      UV class-identity, SuitWorkStation assembly via 2 new probe
      verbs, FuelingStation matched-accounting via new rocket-fuel
      verb).
- [x] Single-method-smoke suites grouped (B3): 2 suites shipped
      (Machine + ServerBoot); Rocket suite dropped as not useful.
- [x] Wall-time delta measured for B3.
- [x] Full pyramid PASS — testServer 179 / 0 / 3 at 8 m 30 s pre-
      finish-line; final run pending after the new tests land.
- [x] EOD marker `2026-05-19-1745_task10-redone-without-fakeplayer.md`.
