# TASK-01 Session 2 — Phase 1 (PlanetDimensionLoad depth, P0)

**Date**: 2026-05-15, 17:33 local
**Branch**: `feature/tests`
**Predecessor marker**: `2026-05-15-1650_task01-session1-phase0-f1f2-and-2a.md`
**Scope**: Phase 1 only — P0 depth for §7.3 PlanetDimensionLoadTest. No
weather, no other phases.

## What landed

### Probe extensions in `TestProbeCommand.handleDim`

- `dim info <id>` now returns three additional fields beyond the previous
  set: `biomeProviderClass`, `chunkGeneratorClass`, `saveDir`. The chunk
  field drills past `ChunkProviderServer` to the inner `IChunkGenerator`
  via `chunkGeneratorClassOf(WorldServer)` (private static helper) so the
  reported class is the informative one. `saveDir` calls
  `world.provider.getSaveFolder()` — null for overworld, prefixed
  `advRocketry/` for AR planets (per `WorldProviderPlanet.getSaveFolder`).
- `dim celestial-angle <id> <worldTime>` new subcommand: pure read-only
  call into `world.provider.calculateCelestialAngle(worldTime, 0.0f)`,
  returns `{dim, worldTime, partialTicks, angle}`. Deterministic by
  construction (no world-state mutation).

### Tests in `PlanetDimensionLoadTest`

- `providerClassIsWorldProviderPlanet` — asserts FQN
  `zmaster587.advancedRocketry.world.provider.WorldProviderPlanet`.
- `biomeProviderIsNonNull` — asserts non-null biome provider.
- `chunkGeneratorIsNonNull` — asserts non-null inner chunk generator.
- `saveFolderResolvesToExpectedPath` — asserts `saveDir` starts with
  `advRocketry/`.
- `celestialAngleStableAcrossSameWorldTime` — two identical probes return
  bit-identical angles (compared via extracted doubles with delta 0.0).
- `celestialAngleProgressesAcrossDifferentWorldTimes` — soft pairwise
  distinct assertion across `t={0, 6000, 12000}`. Strict monotonicity
  intentionally deferred until rotational-period math is pinned.

### Helpers added

- `firstArDimOrSkip()` — already existed (returns first AR dim).
- `firstNonOverworldArDimOrSkip()` — new. Required because AR registers
  Earth as dim 0 but keeps its vanilla `WorldProviderSurface`, so any test
  asserting `WorldProviderPlanet` must skip dim 0 and pick the next AR dim.
  If only Earth is registered, the test reports SKIP via JUnit Assume.
- `extractAngle(List<String>)` — extracts numeric `"angle":<value>` via
  regex. Required because dedicated-server console echoes prefix every
  line with a timestamp, so byte-level response comparison would race tick
  boundaries.

## Validation

- `./gradlew testServer --tests "*.PlanetDimensionLoadTest"` →
  **8/8 PASSED**, 3m 54s.
- Tests at one point had 3/7 failures on first run; root causes (Earth
  having a vanilla provider despite being an AR planet; timestamp prefix
  in console echoes) were fixed before this marker — no failures remain.

## Pyramid status (delta)

- testServer: PlanetDimensionLoadTest grew from 2 → 8 tests (+6).
- testUnit, testIntegration, testClient: unchanged.
- Cumulative gain across Session 1 + Phase 5 micro-fix + Session 2:
  unit +1, server +10 (3 in CommandsSmoke + 1 in PlanetDimLoad smoke + 6
  here). Predecessor "skeleton" marker reported 201/193-PASS; this branch
  should now sit around ~211/203-PASS once a full pyramid run is done.

## Files touched (Session 2 only)

- `src/main/java/.../command/test/TestProbeCommand.java` (+~70 lines:
  3 extra fields in `dim info`, new `dim celestial-angle` case,
  `chunkGeneratorClassOf` helper)
- `src/test/java/.../server/PlanetDimensionLoadTest.java` (full rewrite
  to 6 added tests + 3 helpers + 3 regex constants, ~170 lines total
  including the 2 prior tests)
- `.agent/tasks/TASK-01-smart-depth-coverage.md` (Phase 1 checklist now
  all `[x]`; top-level Completion Checklist boxes Phase 0 + Phase 1
  marked done)

## What did NOT land

- Phase 2a §7.19 4th method (weather malformed-ticks) — still deferred.
- Phase 2b/c/d/e, Phase 3, Phase 4 — not started.
- Phase 5 §5.3 `planet info` field cross-check vs SMART prose — still
  open.
- Full pyramid re-run end-to-end — only PlanetDimensionLoadTest was run
  this session.

## Next session candidates

1. **Phase 2b — §7.13 AtmosphereOxygen** (~3 h). Probe extensions for
   `atmosphere detector-output <pos>` and `fluid tank <pos>` if missing,
   then 5 test methods. **But** SMART §7.13 includes `torchExtinguishes
   InLowOxygenConfig` (config-gated) — verify the user wants atmosphere
   work before starting, since some bullets touch weather-adjacent code.
2. **Phase 2c — §7.9 RocketAssembly** (~4-5 h). Probe + fixture
   extensions plus 9 test methods. Largest single P1 phase; bigger
   dividend toward SMART §16 DoD.
3. **Full pyramid validation run** (~10 min) — just verify
   `./gradlew test` is still green and capture the new baseline counts
   for the next checkpoint marker.

No outstanding errors. No regressions observed. Branch state ready for
commit when the user approves.
