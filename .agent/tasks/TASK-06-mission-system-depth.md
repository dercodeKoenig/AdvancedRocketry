# TASK-06: Mission-system behavioural depth

## Ticket

- Source: TASK-03 EOD audit (2026-05-19) — `mission/` has 3 classes;
  only `MissionResourceCollection` covered at unit tier
  (`MissionResourceCollectionContractTest`). `MissionGasCollection`
  and `MissionOreMining` are completely untested.
- Status: Pending
- Created: 2026-05-19
- Predecessor: `.agent/.context-markers/2026-05-19-1230_task03-A-and-B-mostly-done-eod.md`

## Context

Missions are a player-facing feature: a satellite launches → mission
ticks → resources accrue → reward delivered. The chain currently has
test coverage only at the unit-contract layer for one mission type. A
regression in:

- `MissionGasCollection.requiresGasCollectorSatellite` — silently lets
  any satellite tick gas mission progress (free resources).
- `MissionOreMining.respectsAsteroidMinerOreSet` — wrong ore types in
  output.
- `Mission.persistAcrossServerRestart` — mission state lost on every
  reboot (player progress evaporates).
- `Mission.completeGrantsReward` — mission completes silently without
  awarding the configured reward stack.

…would each ship to modpacks without a CI signal.

Out of scope: client-side mission GUI; mission XML config loader (cover
in a separate ticket if needed).

**No production logic changes** (same rule as TASK-01 §15).

## Implementation Plan

### Phase 1: Probe surface (~2 h)

- [ ] `/artest mission create <type> <satelliteId> [args...]` — start a
  mission of the given type bound to a satellite.
- [ ] `/artest mission state <missionId>` — dump current progress %,
  isComplete, accrued resources, target.
- [ ] `/artest mission tick <missionId> <ticks>` — drive the mission's
  per-tick logic deterministically.
- [ ] `/artest mission complete-now <missionId>` — force-complete for
  test determinism (used in reward tests).

### Phase 2: Gas-collection mission (~2-3 h)

- [ ] `gasMissionAccruesWithCompatibleSatellite` — satellite type
  matches; tick → progress advances.
- [ ] `gasMissionRefusesIncompatibleSatellite_documentsContract` —
  e.g. ore-mining satellite tries to run gas mission → progress stays 0.
- [ ] `gasMissionRespectsGasTypeConfig` — different gas types yield
  different progress rates (validates the gas-type lookup).
- [ ] `gasMissionPersistsAcrossServerRestart` — multi-boot:
  start mission → save → reboot → progress survived.

### Phase 3: Ore-mining mission (~2-3 h)

- [ ] `oreMissionAccrues` analog.
- [ ] `oreMissionRespectsAsteroidOreSet` — only configured ores accrue.
- [ ] `oreMissionPersistsAcrossRestart`.

### Phase 4: Mission completion / reward (~2 h)

- [ ] `missionCompletionFiresOnceAtTarget`.
- [ ] `missionCompletionGrantsConfiguredRewardToSelectedPlayer` —
  needs FakePlayer (TASK-10 / A3 dep).
- [ ] `missionRewardClampsByInventoryCapacity` — reward exceeds player
  inventory → overflow handled (drop on ground / refuse / queue).

### Phase 5: Mission lifecycle smoke (~1 h)

- [ ] `missionCanBeAbandonedAndRestartedFresh`
- [ ] `multipleMissionsOnSameSatelliteCoexistOrErrorCleanly`

### Phase 6: Validation + EOD (~1 h)

- [ ] Full pyramid PASS.
- [ ] EOD marker.

## Technical Decisions

- Mission unit tests at unit-tier where state is in-memory.
- Persistence / reward tests at server-tier (multi-boot) — extend
  `PersistenceRestartSmokeTest` pattern.
- Reward-grant tests need FakePlayer; cross-link with TASK-10. If
  TASK-10 lands first, this task's Phase 4 picks up its FakePlayer
  probe; otherwise mark these tests as deferred.

## Dependencies

**Requires**: TASK-03 base. **Soft-requires**: TASK-10 / A3 (FakePlayer)
for reward-grant tests.

## Estimated effort

~10-12 hours across 3-4 sessions.

## Completion Checklist

- [ ] 4 new `/artest mission` probe verbs
- [ ] Gas mission: 4 tests
- [ ] Ore mission: 3 tests
- [ ] Completion / reward: 3 tests (last 2 may be deferred to TASK-10)
- [ ] Lifecycle: 2 tests
- [ ] Full pyramid PASS
- [ ] EOD marker
