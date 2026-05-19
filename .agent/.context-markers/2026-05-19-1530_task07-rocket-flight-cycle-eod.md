# Context Marker: TASK-07 — Rocket flight cycle beyond launch (Phases 1, 2, 3-5 subset)

**Created**: 2026-05-19 15:30 local
**Branch**: `feature/tests`
**Status**: ✅ Phase 1 (probes) + Phase 2 (orbit-reached event chain) +
Phase 3-5 subset (sequence + ordering integration) shipped. Full
descent/landing E2E and out-of-fuel-explode tests deferred — need
either a FakePlayer to keep the entity's chunk hot, or a probe
that artificially advances the rocket's ticksExisted past
DESCENT_TIMER.

---

## TL;DR

- **+9 server tests** in `RocketFlightCycleDepthTest` — orbit-reached
  event fire, dismantle event fire, launch event in real path,
  errored launch does NOT fire, ticksExisted field exposed, double
  orbit-reached, probe-error contracts, no-satellite-hatch
  defensive baseline.
- **+3 server tests** in `RocketFlightCycleIntegrationTest` — full
  launch→dismantle sequence with strict counter deltas, double
  orbit-reached counters, dismantle doesn't leak into launch
  counter.
- **3 new `/artest rocket` probe verbs**: `force-orbit-reached`,
  `dismantle`, `event-counts`.
- **`RocketEventRecorder` static** registered at server start —
  global counters for the 4 RocketEvent types.
- **`rocket info` extended** with `ticksExisted` field.

---

## Pyramid state (post-TASK-07 partial)

| Layer | Result | Δ from TASK-04 partial (413) |
|---|---|---|
| testUnit | 162 / 0 / 0 | (unchanged) |
| testIntegration | 80 / 0 / 0 | (unchanged) |
| testServer | ~177 / 0 / 3 | +12 (9 depth + 3 integration) |
| testClient | 6 / 0 / 0 | (unchanged) |
| **Total** | **~425 / 0 / 3** | **+12** |

---

## What's pinned

### Probe surface

```
/artest rocket force-orbit-reached <id>
  → invokes EntityRocketBase.onOrbitReached
  → response includes orbitReachedEventDelta (1 if event fired)

/artest rocket dismantle <id>
  → invokes EntityRocketBase.deconstructRocket
  → response includes dismantleEventDelta

/artest rocket event-counts
  → {"launch":N,"preLaunch":N,"orbitReached":N,"dismantle":N}

/artest rocket info <id>
  → adds "ticksExisted" field to existing info dump
```

### RocketFlightCycleDepthTest (9 tests)

- `rocketEventRecorderProbeIsLive` — probe surface shape.
- `forceOrbitReachedFiresRocketReachesOrbitEvent` — production event
  fires both via inline-delta AND global counter advance.
- `dismantleFiresRocketDismantleEvent` — same for dismantle.
- `launchFiresRocketLaunchEventInRealLaunchPath` — verifies TASK-03
  A1 launch path emits the event (the cause-effect side of
  isInFlight=true).
- `erroredLaunchDoesNotFireRocketLaunchEvent` — counter-test: a
  bailed launch (no destination chip) does NOT fire the event.
  Production `setError(...)` runs BEFORE the event post in the
  unrouteable-destination branch.
- `rocketInfoExposesTicksExistedField` — descent-timer gate field
  is readable.
- `forceOrbitReachedOnUnknownRocketReturnsError` + `dismantleOnUnknownRocketReturnsError` — probe contracts.
- `orbitReachedEventChainHandlesAbsentSatelliteHatch` — defensive:
  on the simple-fixture rocket (with seat) the
  reachSpaceManned branch is taken; doesn't crash.

### RocketFlightCycleIntegrationTest (3 tests)

- `launchThenDismantleSequenceFiresExpectedEventsInOrder` — full
  ordering: assemble (no events) → set-destination (no events) →
  launch (+1 RocketLaunchEvent only) → dismantle (+1
  RocketDismantleEvent only). Each step asserts non-target counters
  stay still.
- `doubleOrbitReachedFiresTwoEvents` — documents current contract:
  production has NO idempotency guard on onOrbitReached; calling
  twice fires twice. A future regression that adds a guard (sensible)
  flips this test.
- `dismantleAfterLaunchDoesNotMutateLaunchCounter` — order-of-emission
  contract: dismantle doesn't accidentally re-fire any other event.

### Observation pinned

`onOrbitReached` on a rocket with a programmed destination invokes
`reachSpaceManned()` which schedules a delayed cross-dim transition
via `PlanetEventHandler.addDelayedTransition`. After that call, the
entity may be in a queue rather than the main entity registry —
subsequent `findRocket(id)` lookups become flaky. The integration
test deliberately skips force-orbit-reached between launch and
dismantle to avoid this; the cause-effect is pinned in the per-stage
tests where the rocket is fresh.

---

## What's deferred from TASK-07

### Phase 3 — Dimension transition (deferred)

`inFlightRocketTransitionsToDestinationDim`,
`transitionPreservesRocketIdentityAndContents`,
`transitionToInvalidDimFailsGracefully` — all need the rocket to
actually move through the production transition queue. The
transition fires inside `PlanetEventHandler.tick`'s
`transitionMap`-drain loop after a synchronized clock comparison; in
headless test conditions the chunk holding the rocket isn't kept
hot, so the entity sits in the queue forever. Needs a FakePlayer
(TASK-10) to anchor the chunk.

### Phase 4 — Descent + landing (deferred)

Same chunk-anchoring problem. The descent timer (`ticksExisted >
DESCENT_TIMER && isInOrbit() && !isInFlight()`) requires
ticksExisted to advance, which requires the entity to be ticked,
which requires the chunk to be loaded with a player. FakePlayer
unlock.

### Phase 5 — Failure modes (partial)

- `outOfFuelDuringFlightExplodesRocket` — needs entity ticking.
- `weightExceedsThrustDuringFlightAbortsLaunch` — partly covered by
  TASK-03 A1's `launchWithoutDestinationReportsCannotGetThereError`
  (the weight check happens after destination validation).
- `partsWearSystemEnabledTriggersStorageShouldBreakExplode` — needs
  a config-mutation probe.

All Phase 4-5 items go to a TASK-07b follow-up once TASK-10's
FakePlayer probe lands.

---

## Restore instructions

```
Read .agent/.context-markers/2026-05-19-1530_task07-rocket-flight-cycle-eod.md
Read .agent/.context-markers/2026-05-19-1430_task04-multiblock-partial-eod.md
Read .agent/tasks/TASK-07-rocket-flight-cycle-beyond-launch.md
```

Open items:

1. FakePlayer probe (TASK-10) — unlocks descent/transition E2E.
2. Post-assembly multiblock fixtures (TASK-04 follow-up).
3. TASK-05 item-behaviour suite.
4. TASK-09 satellite-type depth.
5. TASK-06 missions (depends on TASK-10).

Nothing blocks releasing the suite at ~425/0/3.
