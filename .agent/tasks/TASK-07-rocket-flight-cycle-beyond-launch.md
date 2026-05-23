# TASK-07: Rocket flight cycle beyond launch (orbit reached / descent / landing / dismantle)

## Ticket

- Source: TASK-03 EOD audit (2026-05-19) — A1 / `RocketLaunchDepthTest`
  covers the launch path up to `isInFlight=true`. Everything that
  happens AFTER the rocket is in flight — entering orbit, transitioning
  to destination dim, descent, landing-pad collision, dismantle — has
  ~0 isolated test coverage.
- Status: ✅ Completed — see `.agent/tasks/README.md` Done table.
- Created: 2026-05-19
- Predecessor: `.agent/.context-markers/2026-05-19-1230_task03-A-and-B-mostly-done-eod.md`

## Context

The full rocket flight loop (production paths in `EntityRocket.java`,
2590 LoC) has these phases:

1. **`launch()` / `prepareLaunch()`** — start (TASK-03 A1, covered).
2. **In-flight tick** — gravity offset, fuel decrement, animation.
3. **`onOrbitReached()` event fire** — `setPadStatus(false)` on
   takeoff pad (TASK-03 A5 covered the inverse direction).
4. **Dimension transition** — `transferPlayerToDimension` chain via
   `PlanetEventHandler.transitionMap`.
5. **Descent timer** — `DESCENT_TIMER` countdown in target dim.
6. **Landing** — collision with landing pad / surface, deconstruction.
7. **`RocketDismantleEvent`** — block-pasteback via `StorageChunk`.

The TASK-02 `RocketLaunchEventTest` covered the `RocketLaunchEvent`
emission via the force-launch path. The orbit/descent/landing chain
is uncovered.

A regression in any phase ships a "rocket disappears mid-flight" or
"rocket lands but doesn't deconstruct" bug to modpacks.

**No production logic changes** (same rule as TASK-01 §15).

## Implementation Plan

### Phase 1: Probe surface (~2-3 h)

- [ ] `/artest rocket force-orbit-reached <id>` — invokes production
  `EntityRocketBase.onOrbitReached()`. Already partially exercisable
  via existing `force` launch; this verb makes the orbit-reached
  event-bus emission explicit + testable.
- [ ] `/artest rocket force-descent <id>` — sets `ticksExisted` past
  `DESCENT_TIMER` to drive the descent code path.
- [ ] `/artest rocket dismantle <id>` — invokes production
  `deconstructRocket()`.
- [ ] Extend `/artest rocket info` to expose: `errorMessage` (already
  done in A1), `ticksExisted`, `destDimAtLaunchTime` (snapshot taken
  by launch), `passengerListJson`.

### Phase 2: Orbit-reached event chain (~3-4 h)

- [ ] `forceOrbitReachedFiresRocketReachesOrbitEvent` — check via a
  test-side `@SubscribeEvent` listener (registered in `@BeforeClass`
  on the test JVM, gated by Forge bus availability).
- [ ] `orbitReachedOnStationPadFlipsPadOccupiedFalse` — cause-effect
  inverse of TASK-03 A5: rocket-on-pad reaches orbit → pad becomes
  free.
- [ ] `orbitReachedNonStationDimDoesNotTouchSpaceObjectManager` —
  counter-test: orbit reached over overworld → no station pad state
  mutated anywhere.

### Phase 3: Dimension transition (~3 h)

- [ ] `inFlightRocketTransitionsToDestinationDim` — drive a real
  rocket through the production transition chain
  (`PlanetEventHandler.transitionMap`), assert entity now exists on
  target dim.
- [ ] `transitionPreservesRocketIdentityAndContents` — same rocket
  id, same passenger count, same storage chunk after transition.
- [ ] `transitionToInvalidDimFailsGracefullyAndReportsError` —
  programmed destination is an unregistered dim; transition path
  must not crash, must surface error.

### Phase 4: Descent + landing (~3-4 h)

- [ ] `descentStartsAfterOrbitTimerExpires` — orbit reached →
  tick `DESCENT_TIMER+1` → `isInOrbit()=true` flips to descending.
- [ ] `descentReachesGroundAndDismantles` — tick until rocket hits
  surface → `RocketDismantleEvent` fires → blocks pasted to world.
- [ ] `descentTowardOccupiedPadIsRejectedOrRetargeted` — pad is
  occupied → landing path picks a different pad or fails.
- [ ] `dismantleDecomposesIntoOriginalBlockStates` — paste-back
  matches the pre-launch storage chunk snapshot.

### Phase 5: Failure modes (~2 h)

- [ ] `outOfFuelDuringFlightExplodesRocket` — drain fuel mid-tick →
  `explode()` triggers.
- [ ] `weightExceedsThrustDuringFlightAbortsLaunch` — production
  `stats.getWeight() >= stats.getThrust()` gate at launch time
  (already kind-of covered indirectly; sharpen here).
- [ ] `partsWearSystemEnabledTriggersStorageShouldBreakExplode` —
  `ARConfiguration.partsWearSystem` branch.

### Phase 6: Validation + EOD (~1 h)

- [ ] Full pyramid PASS.
- [ ] EOD marker with phase-by-phase coverage delta.

## Technical Decisions

- Use the existing `RocketLaunchDepthTest` build-and-assemble helper
  (extract to a shared `RocketTestFixtures` helper class).
- For event-bus assertions, register listeners in `@BeforeClass` on
  the test side; deregister in `@AfterClass`.
- Dimension transition tests need a destination AR dim — same pattern
  as `RocketLaunchDepthTest.firstNonOverworldArDimOrSkip`.

## Dependencies

**Requires**: TASK-03 A1 fixture surface, AbstractSharedServerTest.
**Does NOT block**: feature work.

## Estimated effort

~14-17 hours across 4-5 sessions.

## Completion Checklist

- [x] 3 original `/artest rocket` probe verbs (`force-orbit-reached`,
      `dismantle`, `event-counts`) + info-probe extension
      (`ticksExisted`).
- [x] Orbit-reached chain: 5 tests in `RocketFlightCycleDepthTest` +
      sequencing tests in `RocketFlightCycleIntegrationTest`.
- [x] **Dimension transition** — 6 tests in `RocketDimensionTransitionTest`.
      Blocker resolved by: (a) ForgeChunkManager ticket via the new
      `/artest chunk forceload` probe (piggy-backs on AR's existing
      `WorldEvents` LoadingCallback), (b) `/artest rocket find-by-uuid`
      that searches all dims and prefers a live match over an
      isDead stale copy left by Forge's `Entity.changeDimension`
      collect-dead lag.
- [x] **Descent + landing** — 7 tests in `RocketDescentLandingTest`.
      Driven by REAL server ticking with the rocket's chunk grid
      force-loaded; `/artest server wait <dim> <ticks>` blocks until
      `world.getTotalWorldTime()` advances. Covers: descent-timer
      gate flips isInFlight, gravity integrates motionY downward,
      `move()` collides with ground and posts `RocketLandedEvent`,
      `deconstructRocket` pastes storage chunk back into the world.
- [x] Failure modes: 5 tests in `RocketFlightFailureModesTest` —
      `explode()` produces isDead=true, out-of-fuel mid-flight does
      NOT auto-explode (pins observed contract; flips if production
      adds an out-of-fuel explode branch), zero-fuel launch still
      enters in-flight state (no fuel gate at launch time), probe
      contract negative cases.
- [x] Full pyramid PASS — testServer 239/0\*/3.
- [x] EOD marker: `2026-05-20-2330_task07-fully-closed.md`

\* Two pre-existing flakes surfaced in full-pyramid runs
(`RocketAssemblySmokeTest.seatCountMatchesFixturePlacement` and
`SpaceElevatorMultiblockTest.spaceElevatorMultiblockValidatesWhenFixtureIsBuilt`)
— both PASS in isolation; both predate this work. Tracked as a
separate follow-up.

## Probe surface added this close-out

```
/artest rocket find-by-uuid <uuid>
/artest rocket force-dest-dim <id> <dim>
/artest rocket tick <id> [n]
/artest rocket set-state <id> orbit=... flight=... ticksExisted=N posY=N motionY=N
/artest rocket explode <id>
/artest rocket drain-fuel <id>
/artest rocket event-counts-full              # adds landed + deOrbiting
/artest chunk forceload <dim> <cx> <cz>
/artest chunk release <dim> <cx> <cz>
/artest chunk release-all
/artest chunk list
/artest server wait <dim> <ticks>
```

`rocket info` / `rocket list` responses extended with `uuid`.
`RocketEventRecorder` now also subscribes to `RocketLandedEvent` and
`RocketDeOrbitingEvent`.
