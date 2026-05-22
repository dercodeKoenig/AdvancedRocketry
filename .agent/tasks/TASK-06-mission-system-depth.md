# TASK-06: Mission-system behavioural depth

## Ticket

- Source: TASK-03 EOD audit (2026-05-19) — `mission/` has 3 classes;
  only `MissionResourceCollection` covered at unit tier
  (`MissionResourceCollectionContractTest`). `MissionGasCollection`
  and `MissionOreMining` are completely untested.
- Status: Phases 1-5 + persistence + rocket-side relink ✅ Completed
  (2026-05-22, three sessions same day).
- Created: 2026-05-19; replanned 2026-05-22; Phases 1-4 close 2026-05-22;
  Phase 5 + persistence + strong fluid pin close 2026-05-22 (later).
- Predecessor: `.agent/.context-markers/2026-05-19-1230_task03-A-and-B-mostly-done-eod.md`
- Successor markers:
  `.agent/.context-markers/2026-05-22_task06-phases-1-4-shipped.md`,
  `.agent/.context-markers/2026-05-22_task06-shipped.md` (this session)

## Context

Missions are a player-facing late-game feature: a rocket launches with a
guidance computer chip → a `Mission*` subclass is constructed on launch
→ the mission gets registered with the orbital `DimensionProperties` as
a tickable `SatelliteBase` → progress accrues over world time → on
completion the rocket is respawned in its launch dim with cargo (fluid
or items) filled in.

A regression in any of these would silently corrupt the progression
loop:

- `MissionResourceCollection.getProgress` returning wrong fraction →
  mission completes too early / never completes.
- `MissionResourceCollection.tickEntity` not firing `onMissionComplete`
  at the crossing → mission stalls.
- `MissionGasCollection.onMissionComplete` not filling fluid tiles, or
  respawning the wrong rocket type / wrong dim → cargo loss.
- `MissionOreMining.onMissionComplete` not replacing the consumed
  asteroid chip → chip leak; or not filling inventory → ore loss.
- NBT round-trip dropping `gas` / `infrastructure` / `rocketStorage`
  keys → save-on-reboot drops mission state entirely.
- Infrastructure tiles not unlinked + relinked → orphan
  `linkMission(...)` pointers, stuck infrastructure GUIs.

Out of scope: client-side mission GUI; mission XML config loader;
player-facing reward retrieval (the rocket-access flow, separate
ticket — production doesn't grant anything directly to a player at
completion, cargo is in the respawned rocket).

**No production logic changes** (same rule as TASK-01 §15).

## History

A 2026-05-19 draft of this task proposed 4 sub-tests that don't map
to real production contracts after a code audit:

| Old plan item | Audit result |
|---|---|
| `gasMissionRefusesIncompatibleSatellite_documentsContract` | No satellite-type gate exists in `MissionGasCollection`; the mission ticks unconditionally. Not a contract. |
| `gasMissionRespectsGasTypeConfig` (different fluids → different rates) | Rate is purely the `duration` ctor arg; `gasFluid` only matters at completion time (sets the fluid type filled). Not a contract. |
| `missionCompletionGrantsConfiguredRewardToSelectedPlayer` | No player grant at completion — cargo ends up in the respawned rocket, retrieved by normal rocket access. Reframed as `rocket-cargo` pin. |
| `missionRewardClampsByInventoryCapacity` | Overflow handling = vanilla `IItemHandler.insertItem` semantics. Not a mod contract. |

These items removed from the current plan. Phase numbering preserved
where the scope was real.

## Implementation Plan

### Phase 1 — Mission probe surface (~3-4 h)

New namespace `/artest mission ...`. Verbs:

- [ ] `start-gas <dim> <duration> <fluidName>` — build a fixture
  rocket, instantiate `MissionGasCollection(duration, rocket, infra,
  FluidRegistry.getFluid(fluidName))`, register via
  `DimensionProperties.addSatellite`, return JSON `{missionId, dim,
  duration, gas}`.
- [ ] `start-ore <dim> <duration> [drillingPower]` — analogue for
  `MissionOreMining`. Equips a default `ItemAsteroidChip` into the
  fixture rocket's guidance computer with mid-range data values so
  the random asteroid harvest can fire (`distanceData/maxData` etc.
  not zero).
- [ ] `state <missionId>` — JSON dump: `progress` (double),
  `startWorldTime`, `duration`, `dim`, `infraCount`, `isDead`,
  `type`.
- [ ] `advance <missionId> <ticks>` — backdate `startWorldTime` by
  `-ticks` (observationally equivalent to advancing world time;
  cheaper + deterministic vs scheduling N real ticks).
- [ ] `complete-now <missionId>` — `advance` until `progress >= 1`,
  then drive `tickEntity()` once so `onMissionComplete` fires.
  Returns post-state JSON.
- [ ] `rocket-cargo <missionId>` — after completion, finds the
  respawned rocket entity via the satellite's stored launch coords
  + dim and returns fluid-tile + inventory-tile contents as JSON
  (`{fluids:[{type, amount}], items:[{id, count, slot}]}`).
- [ ] `infra-state <missionId> <infraDim> <ix> <iy> <iz>` — read a
  fixture infrastructure tile's `getLinkedMission()` and report
  whether it still points at this mission's id.

**Fixture support**: check whether `/artest fixture rocket` already
exists (TASK-04 / TASK-07 land); if so, reuse — otherwise build a
minimal one as part of this phase (lifts from
`TileGuidanceComputerAccessHatch`-side rocket assembly OR uses
reflection on `EntityRocket` to seed the minimum fields the mission
ctor reads — `posX/Y/Z`, `world`, `storage`, `stats`, and
`writeMissionPersistentNBT`).

### Phase 2 — Progress / completion contract (~2 h)

`MissionLifecyclePyramidTest` (server-tier):

- [ ] `progressAdvancesLinearlyWithWorldTime` — duration=1000;
  advance 250 → progress ≈ 0.25 (±epsilon); 500 → ≈ 0.5; 1000 → 1.0
  exactly.
- [ ] `progressIsUnboundedAboveOne` — advance 2000 → progress = 2.0
  (no upper cap — pin the unbounded behaviour so a future cap
  surfaces here).
- [ ] `progressClampsAtZeroWhenStartTimeInFuture` — synthesize
  startTime > now via direct field write → progress = 0 (Math.max
  guard).
- [ ] `completionDoesNotFireBelowProgressOne` — advance to 999;
  state shows `isDead=false`.
- [ ] `completionFiresAtProgressOne` — advance to 1000; state shows
  `isDead=true` and side-effects observable.
- [ ] `completionFiresExactlyOnce` — repeated `complete-now` after
  the first complete doesn't re-fire (setDead guard).

### Phase 3 — Gas mission specifics (~2-3 h)

`MissionGasCompletionTest` (server-tier):

- [ ] `gasCompletionFillsRocketFluidTilesWithConfiguredFluid` —
  start-gas with fluid="oxygen"; complete; rocket-cargo shows
  oxygen=64000 mB in each fluid-tile.
- [ ] `gasCompletionRespawnsStationDeployedRocketAtLaunchPos` —
  EntityStationDeployedRocket exists in launch dim near
  `launchLocation ± production offsets`; not a plain EntityRocket.
- [ ] `gasCompletionSubtractsFuelByOneThousandForBipropellant` —
  fuel-type=BIPROPELLANT → both liquid + oxidizer decremented by
  1000; Math.max guards against going below 0.
- [ ] `gasCompletionDoesNotFillWhenIntakePowerZero` — production
  guard `(int)getStatTag("intakePower") > 0`; rocket-cargo fluid
  list empty.

`MissionGasNbtRoundTripTest` (unit-tier — small):

- [ ] `gasNbtRoundTripPreservesFluidName` — write → read → fluid
  restored via `FluidRegistry`.

`MissionGasPersistenceTest` (server-tier multi-boot, extends
`PersistenceRestartSmokeTest` pattern):

- [ ] `gasMissionPersistsAcrossServerRestart` — start; save +
  reboot; state shows same `progress`, `duration`, `gas`, and same
  fixture infra coords.

### Phase 4 — Ore mission specifics (~2 h)

`MissionOreCompletionTest` (server-tier):

- [ ] `oreCompletionReplacesConsumedAsteroidChipWithEmpty` —
  guidance computer slot 0 post-complete: empty asteroid chip
  (registry name match, no NBT).
- [ ] `oreCompletionRespawnsPlainEntityRocketAtLaunchPos` — type is
  `EntityRocket`, not `EntityStationDeployedRocket`.
- [ ] `oreCompletionNoopsWhenDrillingPowerZero` — production gate
  `rocketStats.getDrillingPower() != 0f`; rocket-cargo inventory
  list empty.
- [ ] `oreCompletionFillsInventoryWithinExpectedBoundsWithValidChip`
  — loose pin: ≥0 stacks (don't pin exact roll outcomes — random
  is impl); presence of at least the empty-asteroid-chip refill.

`MissionOrePersistenceTest`:

- [ ] `oreMissionPersistsAcrossServerRestart`.

### Phase 5 — Infrastructure lifecycle (~1 h)

`MissionInfrastructureLifecycleTest` (server-tier):

- [ ] `startLinksInfrastructureToMission` — start-* with infra coord
  → infra-state shows that mission id.
- [ ] `completionUnlinksInfrastructureFromMissionAndLinksToRocket`
  — after complete-now, infra-state shows null mission; the
  respawned rocket's `connectedInfrastructure` contains the infra
  tile (verify via a new probe verb or via rocket-cargo extension).
- [ ] `infrastructureCoordsSurviveNbtRoundTrip` — unit-tier on
  `MissionResourceCollection.writeToNBT/readFromNBT` cycle through
  the "infrastructure" tag list.

### Phase 6 — Validation + EOD (~1 h)

- [ ] Full pyramid PASS (`./gradlew test` — unit + integration +
  server; testClient untouched this round).
- [ ] EOD marker `.agent/.context-markers/2026-05-XX_task06-shipped.md`.
- [ ] Update `.agent/tasks/README.md` Done table + bug-ledger
  counter if any new `_documentsKnownBug` lands.

## Technical Decisions

- **No FakePlayer**. Probe verbs run on the server thread without a
  player object; the mission code paths that take `EntityPlayer` are
  only used by `performAction`, which in this hierarchy always
  returns false (the `getProgress`/`tickEntity`/`onMissionComplete`
  paths take no player).
- **`advance` over real ticking**. Backdating `startWorldTime` is
  observationally equivalent to elapsing world time (production
  reads `now - startWorldTime`), much faster, and side-effect free.
  `complete-now` then does a single explicit `tickEntity()` to fire
  side effects deterministically.
- **Asteroid randomness is loose-pinned**. The 3 `Math.random()`
  rolls in `MissionOreMining.onMissionComplete` are impl; tests pin
  "≥0 stacks" + chip-replace + respawn-type. A future ticket could
  add a seeded `RandomFixture` if needed.
- **Mission XML config loader is out of scope** for this ticket.
- **No production logic changes** — record any production bug found
  in `.agent/tasks/README.md` ledger per CLAUDE.md's bug-tracking
  rule; do not silently fix.

## Dependencies

- **Requires**: TASK-03 base, TASK-09 (satellite registration patterns
  reused).
- **Cross-cuts**: rocket-cargo retrieval by player would live in
  testClient e2e — separate ticket, not this one.
- **Reuses if available**: `/artest fixture rocket` (TASK-04 /
  TASK-07). If not, ~1-2h extra in Phase 1.

## Estimated effort

~10-12 hours across 3-4 sessions.

## Completion Checklist

- [x] 9 `/artest mission` probe verbs landed (start-gas, start-ore,
      state, advance, complete-now, rocket-cargo, link-infra, infra-state,
      rocket-relink-state)
- [x] Phase 2: 5 lifecycle tests (`MissionLifecyclePyramidTest`)
- [x] Phase 3: 4 gas-completion tests (`MissionGasCompletionTest`,
      including new strong 64000 mB oxygen-fill pin via
      `with-fluid-cargo` fixture variant)
      + 3 NBT round-trip tests (`MissionNbtRoundTripTest` at unit-tier)
- [x] Phase 4: 3 ore-completion tests (`MissionOreCompletionTest`)
- [x] Phase 5: 3 infra-lifecycle tests
      (`MissionInfrastructureLifecycleTest` — link on start,
      tile-side unlink on completion, rocket-side relink on completion)
- [x] Multi-boot persistence: 2 tests
      (`MissionPersistenceRestartTest` — gas + ore survive reboot)
- [x] Full pyramid PASS (3 unit + 17 server, 20/20 green)
- [x] EOD markers (Phases 1-4 + this session's marker)
- [x] `.agent/tasks/README.md` Done table updated

**Tests landed**: 20 (3 unit + 17 server). Rocket-side relink follow-up
closed 2026-05-22 — see "Closed follow-up" below.

## Closed follow-up

1. **Rocket-side relink assertion** — CLOSED 2026-05-22.
   Root cause of the original observation: `MissionResourceCollection`
   ctor seeds `missionPersistantNBT` via `entity.writeMissionPersistentNBT`,
   but `EntityRocket`'s implementation is a no-op (line 2081). The
   freshly spawned `EntityStationDeployedRocket` then restores
   `launchLocation = (0,0,0)` and `forwardDirection = DOWN` from empty
   NBT, so production positions the new rocket at world origin
   `(0.5, y, 0.5)` — outside the bbox that `rocket-cargo` scans
   around the original launch coords. The original rocket alone was
   visible in the bbox, hence `rocketCount=1` + empty
   `infrastructureCoords` (production called `linkInfrastructure` on
   the new rocket at world origin, not the original).
   Resolution: new probe verb `rocket-relink-state <dim>` does a
   class-filtered scan (not bbox-limited), and the new test
   `completionLinksInfrastructureToRespawnedRocket` asserts the
   placed monitoring station's coord appears in some
   EntityStationDeployedRocket's `infrastructureCoords`.
   Production-side note: this no-op `writeMissionPersistentNBT` on
   the vanilla EntityRocket is not a player-facing bug — production
   only constructs a gas mission from `EntityStationDeployedRocket
   .onOrbitReached`, where the entity IS a StationDeployed and its
   overridden `writeMissionPersistentNBT` populates the launch coords
   correctly. Test-fixture-only edge case.

The Phase-5 NBT roundtrip for the `infrastructure` tag list is
already covered structurally by
`MissionNbtRoundTripTest.infrastructureNbtTagListShapeIsKeyLocPlusIntArrayTriple`.
