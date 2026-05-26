# TASK-31: External-subscriber payload contracts for RocketLanded / RocketDismantle / RocketDeOrbiting events

## Ticket

- Source: 2026-05-25 Tier 2 audit, gap #3 (RocketLandedEvent /
  RocketDismantleEvent / RocketDeOrbiting payload). Carried
  forward into 2026-05-26 audit out-of-scope.
- Status: **✅ Completed 2026-05-26** — see `.agent/tasks/README.md`
  Done table.
- Created: 2026-05-26.

## Actual scope shipped

Three new tests appended to
`src/test/java/zmaster587/advancedRocketry/test/server/RocketEventPayloadContractTest.java`:

1. `rocketLandedEventCarriesRocketEntityAndWorld` — driven by the
   real-tick descent + collision pattern from `RocketDescentLandingTest`.
   Asserts both the counter advanced AND `lastLandedEntityId ==
   rocketId`, `lastLandedDim == 0`.
2. `rocketDeOrbitingEventCarriesRocketEntityAndWorld` — sets
   `ticksExisted=18 + orbit=true`, waits 3 ticks for the
   `ticksExisted == 20` branch in `EntityRocket.onUpdate` to fire.
3. `rocketReachesOrbitEventCarriesRocketEntityAndWorld` — uses
   `force-orbit-reached` probe to drive the production
   `onOrbitReached()` codepath; pins payload identity for the
   sixth and last `RocketEvent` subtype.

The pre-existing `RocketDismantleEvent` + `RocketPreLaunchEvent`
payload tests in the same class already covered the dismantle leg
of the TASK; together the file now pins entity-id + dim payload
for all six `api.RocketEvent` subtypes — the companion-mod-facing
surface is complete.

No new probe verbs needed (RocketEventRecorder already exposes
`lastXxxEntityId` / `lastXxxDim` fields, and the existing rocket
state-mutation + chunk-forceload probes cover the harness side).

## Context

`zmaster587.advancedRocketry.api.RocketEvent.*` exposes six events
on the Forge event bus. Companion mods subscribe to these to
react to rocket lifecycle stages. Three are well-covered:

- `RocketLaunchEvent` — pinned (count + payload) by TASK-07.
- `RocketPreLaunchEvent` — pinned by `arm-prelaunch-cancel` Tier 1
  gap #1 batch.
- `RocketReachesOrbitEvent` — pinned by TASK-07.

The three NOT covered for external-subscriber payload contracts:

- `RocketLandedEvent` — fires when rocket sets down on planet.
- `RocketDismantleEvent` — fires when the assembler dismantles.
- `RocketDeOrbitingEvent` — fires during de-orbit transition.

The existing `RocketEventRecorder` (in `TestProbeCommand`) already
tracks `landedCount`, `dismantleCount`, `deOrbitingCount` and the
last-observed `entityId` + `dim` for each. What's missing is a
test that **asserts a subscriber receives the payload that
production sends** — specifically that `event.getEntity()`
references the right rocket entity and `event.world.provider
.getDimension()` matches.

## Why it matters

These events are public API (`api.RocketEvent`). A companion mod
that subscribes to `RocketLandedEvent` to drop an achievement on
"first landing on planet X" depends on:

1. The event fires when a landing actually occurs (count pin).
2. `event.getEntity()` returns the rocket that landed (payload
   pin — what TASK-09 covered for the launch / orbit / dismantle
   triad).
3. `event.world.provider.getDimension()` reports the destination
   dim, not the source dim.

Pin (2) and (3) are missing for the three new events.

## Implementation plan

| Phase | Effort | Result |
|---|---|---|
| 0 | ~30 min | Verify `RocketEventRecorder` already exposes `lastLandedEntityId`, `lastLandedDim`, `lastDismantleEntityId`, `lastDismantleDim`, `lastDeOrbitingEntityId`, `lastDeOrbitingDim` (it does — see TestProbeCommand lines 11011-11014). |
| 1 | ~2 h | `RocketLandedDismantleDeOrbitPayloadTest` — three tests, one per event. Trigger the event via existing fixture (rocket landing scenario from TASK-07, dismantle via assembler-recipe pattern, de-orbit via descent), then read `event-payloads` and assert `lastXxxEntityId == known rocket id`, `lastXxxDim == known dim`. |

## Acceptance

- [ ] 3 tests pinning entity-id and dim payload per event.
- [ ] Counters reset between tests via subscriber re-arm pattern
      (or per-test delta measurement).
- [ ] Pyramid counter regenerated.

## Out of scope

- Cross-event ordering invariants (e.g. "Landed fires before
  Dismantle"). Out of base payload contract.
- Cancellation behaviour of these events — none of them are
  cancellable in production, per the `Cancelable` annotation
  surface.

## Dependencies

- Does NOT block any other task.
- Builds on existing event-recorder infrastructure; no new probe
  surface needed.

## Estimated effort

- Phase 0: 30 min
- Phase 1: 2 h
- **Total**: ~2.5 h

## Risk

Low. The probe surface is in place; the test just needs to drive
the events and assert.
