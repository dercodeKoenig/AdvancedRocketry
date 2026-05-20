# Context Marker: TASK-07 fully closed — flight cycle deferred phases shipped

**Created**: 2026-05-20 23:30 local
**Branch**: `feature/tests` (uncommitted — ready for review/commit)
**Predecessor**: [[before-compact-2026-05-20-2310]] (TASK-04 close-out)
**Predecessor**: [[2026-05-19-1530_task07-rocket-flight-cycle-eod]] (TASK-07 partial)

---

## What shipped this session

The deferred Phase 3/4/5 items from TASK-07 ([[task07-rocket-flight-cycle-eod]] marker)
are now covered. The original deferral reason — "headless server doesn't tick
chunks without a player anchor" — is resolved by a real Forge chunk-loading
ticket (not a synthetic `onUpdate` probe).

### New probe surface

`TestProbeCommand` adds 8 new rocket-related verbs + 2 new top-level
subcommand handlers:

```
/artest rocket find-by-uuid <uuid>
  → searches ALL loaded dims for a rocket matching UUID
  → response includes entityId, dim, posX/Y/Z, isDead, isInFlight/Orbit,
    storageSizeX/Y/Z, engineCount (atomic snapshot — caller doesn't need
    a follow-up info call which may race the dest dim unloading)
  → prefers a LIVE match over an isDead stale copy left in the source
    dim by Forge's Entity.changeDimension collect-dead lag

/artest rocket force-dest-dim <id> <dim>
  → set EntityRocket.destinationDimId via reflection, bypassing the
    launch() canTravelTo guard. Used by the invalid-dim test.

/artest rocket tick <id> [n]
  → call EntityRocket.onUpdate() n times directly. Retained for
    failure-mode tests that need synchronous single-step control.

/artest rocket set-state <id> orbit=true|false flight=... ticksExisted=N
                              posY=N motionY=N
  → direct state mutation (reflection on ticksExisted, setters for the
    rest). Used to set up specific orbit-reached / descent states.

/artest rocket explode <id>
  → invoke production EntityRocket.explode(). Pin: returns isDead=true.

/artest rocket drain-fuel <id>
  → zero out every fuel type. Companion to the existing fuel read probe.

/artest rocket event-counts-full
  → extended counter dump including landed + deOrbiting. Recorder
    now subscribes to RocketLandedEvent + RocketDeOrbitingEvent too.

/artest chunk forceload <dim> <cx> <cz>
/artest chunk release <dim> <cx> <cz>
/artest chunk release-all
/artest chunk list
  → ForgeChunkManager.Ticket-based chunk anchor. Piggy-backs on the AR
    mod's already-registered LoadingCallback (WorldEvents.ticketsLoaded).
    Initializes the destination dim if needed (keepDimensionLoaded +
    initDimension) so cross-dim transitions land in a loaded world.

/artest server wait <dim> <ticks>
  → block until world.getTotalWorldTime() advances by N. Wall-clock
    safety budget: 200ms per requested tick (cap 30s). Used by the
    descent/landed tests to give the real server tick loop time to
    drive EntityRocket.onUpdate through its production code paths.

Extensions to existing probes:
  - /artest rocket info: adds "uuid" field
  - /artest rocket list: adds "uuid" per entry
```

### New tests (18 total, all green)

**`RocketDimensionTransitionTest` (6 tests)** — Phase 3:
- `rocketInfoAndListExposeUuid` — probe-surface contract.
- `inFlightRocketTransitionsToDestinationDim` — real cross-dim
  transition: assemble → set-destination → launch instant →
  force-orbit-reached → find-by-uuid in destDim succeeds.
- `transitionPreservesRocketIdentityAndStorageContents` — entityId
  CHANGES across changeDimension, UUID/storageSize/engineCount
  preserved.
- `transitionToInvalidDimFailsGracefullyAndKeepsRocket` — force destDim
  to -12345 via reflection probe → canTravelTo guard returns null →
  no crash, rocket stays in dim 0.
- `findByUuidOnUnknownUuidReturnsError`, `OnMalformedUuidReturnsError` —
  probe contracts.

**`RocketDescentLandingTest` (7 tests)** — Phase 4 (REAL ticks):
- `chunkAnchorProbeRoundTrips` — forceload + release + list endpoint
  contracts.
- `rocketTickProbeReportsTicksExistedInResponse` — synthetic-tick probe
  surface sanity.
- `descentTimerGateFlipsInFlightUnderRealTicks_realTick` — forceload
  rocket's 3×3 chunk grid, set orbit=true/flight=false/ticksExisted=41,
  `server wait 5` → real server ticks fire onUpdate → gate flips
  isInFlight to true.
- `tickBeforeDescentTimerKeepsFlightOff_realTick` — counter-test:
  ticksExisted=5, after 5 real ticks still well below DESCENT_TIMER=40,
  isInFlight stays false.
- `inFlightDescentApplesGravityUnderRealTicks_realTick` — motionY=0
  start, after 5 real ticks posY has decreased.
- `landedEventFiresOnGroundCollisionUnderRealTicks_realTick` — stone
  floor at y=64, rocket at posY=66 motionY=-10, real `move()` collides,
  RocketLandedEvent counter advances + isInFlight/Orbit cleared.
- `dismantleAfterAssemblePastesBlocksBackAtRocketFootprint` —
  storage.pasteInWorld puts at least one non-air block back.

**`RocketFlightFailureModesTest` (5 tests)** — Phase 5:
- `explodeProbeSetsRocketDeadAndRemovesFromWorld` — explode → isDead=true
  (atomic via probe response; we don't chain a racy follow-up info call).
- `outOfFuelMidFlightDoesNotAutoExplode_documentsCurrentBehavior` —
  pin observed contract: production has no out-of-fuel explode branch.
  Test flips to FAIL if production adds one (assertion will need to flip).
- `launchWithZeroFuelStillTransitionsToInFlight` — production launch()
  has no fuel-amount gate; documents current behaviour.
- `explodeOnUnknownRocketReturnsError`, `drainFuelOnUnknownRocketReturnsError`
  — probe contracts.

### RocketEventRecorder extended

Added counters and `@SubscribeEvent` handlers for `RocketLandedEvent`
and `RocketDeOrbitingEvent`. The `event-counts-full` probe surfaces
them; the original `event-counts` probe still returns the original
4-counter shape for backward compat.

---

## Pyramid state (post-TASK-07 full)

| Layer | Result | Δ from TASK-07 partial (~189) |
|---|---|---|
| testUnit | 187 / 0 / 0 | — |
| testIntegration | 80 / 0 / 0 | — |
| testServer | 239 / 0\* / 3 | **+18 from TASK-07 close-out** |
| testClient | (unchanged; tasks were testServer-only) | — |

\* Across this session the full pyramid surfaced two *pre-existing* flakes
in different runs: `RocketAssemblySmokeTest.seatCountMatchesFixturePlacement`
(once) and `SpaceElevatorMultiblockTest.spaceElevatorMultiblockValidatesWhenFixtureIsBuilt`
(once). Both PASS in isolation; both predate this work. They appear to be
order-sensitive in the shared `AbstractSharedServerTest` harness. Tracking
as a separate follow-up — does not block this delivery.

---

## Architectural pivot mid-session (user feedback)

Initial Phase 4 drafts drove `EntityRocket.onUpdate()` via a synthetic
`/artest rocket tick` probe. User flagged this as testing in an
environment that diverges from real game ticks (neighbor chunks may not
be loaded, no real entity-update scheduling, no real collision
context). Switched to:

  1. AR-namespaced Forge chunk ticket via the existing
     `WorldEvents.ticketsLoaded` LoadingCallback (no new mod-side
     registration needed; we piggy-back on AR's existing
     `setForcedChunkLoadingCallback` call at `AdvancedRocketry.java:1131`).
  2. `/artest chunk forceload <dim> <cx> <cz>` per test.
  3. `/artest server wait <dim> <ticks>` polls
     `WorldServer.getTotalWorldTime()` until N real ticks have elapsed.

Result: the descent/landed tests now exercise the production
`EntityRocket.onUpdate` from inside the natural `WorldServer.updateEntity`
loop with all the same context a real game session has. Synthetic
`rocket tick` retained for the few cases where single-step control
matters (e.g. failure-mode tests that don't depend on physics).

---

## Files touched

- `src/main/java/zmaster587/advancedRocketry/command/test/TestProbeCommand.java`
  — +~390 LoC: new rocket subverbs (find-by-uuid, force-dest-dim, tick,
  set-state, explode, drain-fuel, event-counts-full), new top-level
  handlers (`handleChunk`, `handleServer`), extended `rocket info`/`list`
  with `uuid`, extended `RocketEventRecorder` with `landed`/`deOrbiting`.
- `src/test/java/zmaster587/advancedRocketry/test/server/RocketDimensionTransitionTest.java`
  (new, 6 tests)
- `src/test/java/zmaster587/advancedRocketry/test/server/RocketDescentLandingTest.java`
  (new, 7 tests)
- `src/test/java/zmaster587/advancedRocketry/test/server/RocketFlightFailureModesTest.java`
  (new, 5 tests)
- `.agent/tasks/TASK-07-rocket-flight-cycle-beyond-launch.md` —
  Completion Checklist flipped to ✅ for Phases 3/4/5.

---

## Restore instructions

```
Read .agent/.context-markers/2026-05-20-2330_task07-fully-closed.md
Read .agent/.context-markers/before-compact-2026-05-20-2310.md
Read .agent/tasks/TASK-07-rocket-flight-cycle-beyond-launch.md
Read .agent/tasks/README.md  # for next task selection
git status                   # changes are uncommitted, awaiting review
```

---

## What's next

Per `.agent/tasks/README.md` priorities, with TASK-07 closed the next P0
is **TASK-08 (ASM coremod safety net)** — highest single-point-of-failure
risk. Alternatively the open follow-ups noted in this marker:

  - **Shared-harness flake investigation** — `RocketAssemblySmokeTest` /
    `SpaceElevatorMultiblockTest` flake under full-pyramid runs. Root
    cause unknown; likely shared state in the dedicated server JVM
    between consecutive `AbstractSharedServerTest` classes.
  - **TASK-10 / TASK-10b** — testClient e2e player-event coverage
    (preferred over FakePlayer per `feedback_no_fakeplayer_for_player_tests`).
