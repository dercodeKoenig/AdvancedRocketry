# TASK-28: Residual test flakes from TASK-27 10× verification

## Ticket

- Source: TASK-27 Phase 3 10× testServer verification (v4 + v5 sweeps,
  2026-05-23 / 2026-05-24). Budget tuning hit diminishing returns;
  remaining flakes need different strategies.
- Status: 🟢 **Backlog**.
- Created: 2026-05-24.

## Why this is split out of TASK-27

TASK-27 delivered defensive retry + budget infrastructure for the
shapes documented at the time of its writing (port contention, tick
race, post-fixture validate race). Verification of "10 consecutive
PASS" surfaced **additional** flake shapes that defy further budget
tuning — they need different fixes (chunk-load forcing, recipe-pinning,
fixture redesign). Bundling all that into TASK-27 would have widened
its scope past what the original investigation framed. New TASK lets
each residual shape get a dedicated root-cause + fix entry.

## Residual flakes

Each entry lists: shape, last-seen evidence, suspected root cause,
proposed fix shape.

### F1 — PrecisionLaserEtcher try-complete attempted:false

- **Last seen**: v4 run 3, v5 run 3
  (`PrecisionLaserEtcherRecipeEndToEndTest.precisionLaserEtcherFixtureValidates`).
- **Evidence**: 8 consecutive `try-complete` attempts each return
  `{"attempted":false, "isComplete":false}` across 4 s window.
- **Suspected cause**: chunk where the multiblock sits isn't fully
  loaded — `attemptCompleteStructure` short-circuits on its
  `world.isAreaLoaded` check.
- **Fix shape**: pre-load the relevant chunk(s) in the fixture probe
  before returning success. New `world.getChunk(cx, cz)` calls or a
  ChunkProvider forceLoad before the structure validates.

### F2 — ForceFieldProjection extensionRange stays 0 under load

- **Last seen**: v4 run 1, v5 run 9, v5 run 10
  (`ForceFieldProjectionSmokeTest.poweredProjectorProjectsAndUnpoweredCollapses`).
- **Evidence**: 6 s probe budget (120 × 50 ms) elapses with
  `extensionRange:0, isPowered:true`.
- **Suspected cause**: parallel-fork pressure stretches effective
  server tick rate so the projector's `% 5 == 0` time gate doesn't
  fire often enough within 6 s. The probe currently waits on natural
  ticks rather than driving the tile directly.
- **Fix shape**: bypass the % 5 gate via reflection — invoke the
  projector's extension cycle on the server thread directly,
  bypassing the natural-tick wait. Or driver the tile's update
  method N times via `tile force-tick`.

### F3 — Centrifuge recipe-order non-determinism

- **Last seen**: v4 run 1, v5 runs 1 + 2
  (`CentrifugeRecipeEndToEndTest.centrifugeRunsFirstRegisteredRecipe`).
- **Evidence**: probe resolves first-registered recipe expecting
  `minecraft:iron_nugget`; runtime processes a different recipe and
  output hatch ends up with `libvulpes:productnugget` (or vice
  versa — alternation observed v3 ↔ v4). Both recipes are valid for
  the same fluid input.
- **Suspected cause**: when multiple recipes match the same input,
  libVulpes' runtime selection differs from probe's
  `recipe-info <className> 0` (which returns registration index 0).
- **Fix shape**: pin recipe selection by output identity in the test
  (drop "first registered" framing — pick a known recipe by name
  and configure inputs accordingly). NOT a flake of TASK-27's shapes
  — pure test design.

### F4 — MixinHook fGravityMixin: falling block dies in 1 tick

- **Last seen**: v4 run 4
  (`MixinHookBehaviourPinsTest.fGravityMixinAffectsFallingBlockInOverworld`).
- **Evidence**: probe spawns `EntityFallingBlock`, asks for 3 ticks;
  response shows `ticked:1, isDead:true, motionY:0.0` — block landed
  in one tick.
- **Suspected cause**: the fall-clearance loop (`-10..-1` y-offset
  set to air) is too tight relative to the mixin-accelerated gravity
  the test is meant to verify. Block accelerates fast, hits the
  cleared-air floor on tick 1.
- **Fix shape**: deepen the cleared column, OR read motionY mid-tick
  (snapshot the first call to `onUpdate` before subsequent ticks
  can land the entity). NOT a TASK-27 flake — test design.

### F5 — SolarPanel new shape

- **Last seen**: v5 run 9
  (`MachineDomainSmokeSuite.solarPanelAccumulatesEnergyOverTicks`).
- **Evidence**: single sighting, log snippet not captured before
  TASK-27 close-out.
- **Status**: 👁 **Watching** — needs second occurrence to characterise.

### F6 — Wireless secondary tile:null after place

- **Last seen**: v5 run 5
  (`WirelessTransceiverContractTest.pairingBothUnpairedAssignsFreshSharedIdRegisteredOnNetwork`).
- **Evidence**: `placeAt` waits 5 × 200 ms = 1 s for `wireless-info`
  `"ok":true` sentinel; tile remains `null` past budget.
- **Suspected cause**: same chunk-load race as F1 but at the place
  layer. Block placement succeeded but tile entity creation lagged
  past 1 s.
- **Fix shape**: same as F1 — force chunk load before/during
  `artest place`. Or bump the test-side budget.

### F7 — Worldgen sampling race (TASK-16 shape #4 promoted)

- **Last seen**: v3 run 4, v5 run 5; total 3 sightings (1 in
  TASK-16 close-out, 2 in TASK-27 verification).
- **Evidence**: three spaced chunks return identical (topY, biome)
  under full-pyramid pressure
  (`WorldgenDeterminismAndSamplingTest.differentChunksReturnIndependentlyAddressableData`).
- **Suspected cause**: chunk sampling probe doesn't force chunk
  generation; under load some chunks return placeholder data.
- **Fix shape**: probe should force-generate the chunks it samples
  (call `world.getChunk(cx, cz)` then wait for `isPopulated`).
- **Note**: moves out of TASK-16's "watching" status now that the
  pattern is confirmed across 3 sightings.

## Implementation plan

Phased rollout, biggest-impact-first:

| Phase | Effort | Result |
|---|---|---|
| F1 + F6 + F7 | ~3 h | Single chunk-force helper in `TestProbeCommand`; F1 & F6 call it from their fixture/place probes; F7's worldgen probe calls it before sampling. |
| F2 | ~2 h | New probe `/artest field tick <dim> <x> <y> <z> <count>` — reflective drive of the projector's `IntermittentTickable.onIntermittentUpdate` (or equivalent). Replace `field info`'s natural-tick wait with explicit drive. |
| F3 | ~1 h | Refactor `CentrifugeRecipeEndToEndTest` to pick a recipe by name (not registration index). Possible kit-helper extension. |
| F4 | ~1 h | Deepen cleared column in `MixinHookBehaviourPinsTest` or change motionY read strategy. |
| F5 | needs sighting | Backlog until reproduced. |
| **Total** | **~7 h** | |

## Acceptance

- [ ] F1 + F6 + F7 mitigated via chunk-force probe helper.
- [ ] F2 mitigated via direct tile drive.
- [ ] F3 + F4 fixed at test layer.
- [ ] F5 either reproduced + fixed or marked Obsolete after 5
      consecutive TASK-28-rerun cycles without recurrence.
- [ ] 10 consecutive `./gradlew testServer -Pforks=3` PASS — finally.

## Out of scope

- Anything not in the F1-F7 list above. Future flake shapes get their
  own TASK file per `task-lifecycle.md`.
- Changing `-Pforks=N` default — `gradle.properties` is protected.

## Estimated effort

~7 h across F1-F4 + watching F5 + chunk-force helper unifies F1/F6/F7.
