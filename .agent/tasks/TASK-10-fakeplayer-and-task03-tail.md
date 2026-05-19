# TASK-10: FakePlayer probe + TASK-03 deferred tail (A3 / A2 remainder / B3)

## Ticket

- Source: TASK-03 EOD (2026-05-19) — A3 (FakePlayer + real player-event
  tests) and remainder of A2 (suit / UV / fueling / NBT) deferred.
  B3 (suite-grouping single-method smokes) also deferred. Bundling
  them as the natural continuation of TASK-03.
- Status: Pending
- Created: 2026-05-19
- Predecessor: `.agent/.context-markers/2026-05-19-1230_task03-A-and-B-mostly-done-eod.md`

## Context

Many higher-priority tasks (TASK-05 items, TASK-06 missions) have
**soft-dependencies on a FakePlayer probe** — the headless dedicated
server harness has no connected player, so any test that needs to
exercise a `EntityPlayer`-receiving production method must inject one.
This task delivers the probe + the originally-deferred player-event
tests + cleans up the TASK-03 tail.

**No production logic changes** (same rule as TASK-01 §15).

## Implementation Plan

### Phase 1: FakePlayer probe surface (~3-4 h)

- [ ] `/artest fakeplayer create <name>` — instantiate via
  `FakePlayerFactory.get(...)` against the server's dim-0 world.
- [ ] `/artest fakeplayer teleport <name> <dim> <x> <y> <z>` —
  drives `PlayerList.transferPlayerToDimension` (covers
  `PlanetEventHandler.onPlayerChangedDimensionEvent` chain).
- [ ] `/artest fakeplayer tick <name> [count]` — drives
  `EntityPlayer.update()` ticks.
- [ ] `/artest fakeplayer destroy <name>` — unregisters cleanly.
- [ ] `/artest fakeplayer info <name>` — dim, pos, air, health,
  inventory state.

### Phase 2: Original A3 player-event tests (~3-4 h)

- [ ] `playerJoinArDimAppliesAtmosphereTracking` — fakeplayer
  teleports to AR dim → `/artest oxygen player <name>` reports the
  AR atmosphere (not vanilla).
- [ ] `playerLeavingArDimReleasesTracking` — counter.
- [ ] `playerInSpaceDimWithoutStationForceTeleported` — drive the
  production fallback at `PlanetEventHandler.playerTick:210`.
- [ ] `playerOnLunaTriggersWentToTheMoonAdvancement` —
  advancement integration.

### Phase 3: A2 remainder — heavy tile depth (~5-6 h)

- [ ] `SuitWorkStationAssemblesSuit` — fill component slots with
  fixtures, tick, assert assembled suit in output.
- [ ] `UvAssemblerDivergesFromRocketAssembler` — pin behavioural
  delta in the unmanned-vehicle override.
- [ ] `FuelingStationFuelsAdjacentRocket` — place fueling station
  next to fueled rocket, link, tick, assert rocket fuel rises and
  station fuel falls (matched accounting).
- [ ] `FluidTankNBTRoundTripsAcrossRestart` — multi-boot test.

### Phase 4: B3 — suite-group single-method smokes (~2-3 h)

14 single-method `*SmokeTest` classes spawn 14 separate JVMs today.
Group by domain:

- [ ] `ServerBootSmokeSuite` — combine ServerStartupSmokeTest,
  RegistrySmokeTest, CommandsSmokeTest, HarnessDiagnosticTest,
  NonARDimensionIsolationTest.
- [ ] `RocketDomainSmokeSuite` — RocketLaunchSmokeTest +
  RocketInfrastructureSmokeTest fragments that don't need
  persistence isolation.
- [ ] `MachineDomainSmokeSuite` — MultiMachineControllerSmokeTest,
  MultiblockValidationSmokeTest, EnergySystemsSmokeTest,
  SealedRoomOxygenVentTest, SuitVacuumSubsystemSmokeTest,
  SpecialInfrastructureSmokeTest, ForceFieldProjectionSmokeTest,
  MicrowaveReceiverSmokeTest, BlackHoleGeneratorSmokeTest.
- [ ] Verify wall-time saving (~120 s expected at 3-way parallelism).

### Phase 5: Cross-cutting + EOD (~1 h)

- [ ] Full pyramid PASS.
- [ ] EOD marker.

## Technical Decisions

- FakePlayer probe uses `FakePlayerFactory.get(WorldServer, GameProfile)`
  — vanilla Forge API. Keep one fake-player-per-name in a static map
  inside the probe; destroy removes from the map AND from
  `World.playerEntities`.
- `EntityPlayer.update()` invocation is delicate — Forge's
  `LivingUpdateEvent` chain expects valid player state. Test only the
  cases where the FakePlayer state is sane.
- Suite-grouping for B3: preserve original test names verbatim as
  method names in the suite class (so failure messages stay grep-able).

## Dependencies

**Requires**: TASK-03 base.
**Unlocks**: TASK-05 (items with EntityPlayer surfaces),
              TASK-06 (mission reward grant).
**Does NOT block**: TASK-04, TASK-07, TASK-08, TASK-09.

## Estimated effort

~14-18 hours across 4-5 sessions.

## Completion Checklist

- [x] 7 new `/artest fakeplayer` probe verbs — create / teleport /
      tick / destroy / info / list / fire-living-update.
- [x] 4 player-event behavioural tests (A3) — including one
      `_documentsFakePlayerNPE` pinning AR's player-tick chain
      doesn't tolerate FakePlayer (production NPEs in
      PlayerList.transferPlayerToDimension and
      EntityPlayerMP.onNewPotionEffect).
- [ ] **A2 remainder DEFERRED** — suit workstation real recipe,
      UV vs RocketAssembler divergence, fueling-station-with-rocket,
      fluid tank NBT round-trip. Each needs ~2-3 h fixture work.
- [ ] **B3 suite-grouping DEFERRED** — mechanical wall-time win
      (~120 s); worth a dedicated cleanup session.
- [x] Full pyramid PASS (expected ~435 total)
- [x] EOD marker: `2026-05-19-1600_task10-fakeplayer-eod.md`
