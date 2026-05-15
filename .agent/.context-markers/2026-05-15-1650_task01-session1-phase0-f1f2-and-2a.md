# TASK-01 Session 1 — Phase 0 (F1 + F2) + Phase 2a (Commands depth)

**Date**: 2026-05-15, 16:50 local
**Branch**: `feature/tests`
**Predecessor marker**: `2026-05-15-1610_smart-pyramid-skeleton-complete.md`
**Scope deviation from TASK-01 Session 1**: weather-related work intentionally
excluded by user direction. F2 audit covered non-weather categories only;
Phase 2a dropped `artestWeatherSetWithMalformedTicksReturnsError`.

## What landed

### F1 — §6.7 #3 closed
- `src/test/.../unit/AstronomicalBodyHelperTest.java`:
  added `planetaryLightMultiplierWithinExpectedBounds` (final §6.7 named-test
  gap). Sweeps distances {50, 100, 200, 400} through
  `getStellarBrightness` → `getPlanetaryLightLevelMultiplier`, asserts each
  result is inside a tight analytic band around `1.5^log2(SBM)`.
- Validation: `./gradlew testUnit --tests "*.AstronomicalBodyHelperTest"` →
  **12/12 PASSED** (11 prior + new method).

### F2 — non-weather /artest audit
Static audit of `TestProbeCommand` (28 non-weather top-level categories).
Findings written into TASK-01 Phase 5:
- **MISSING**: `/artest dim load <id>` — implicit via `weather`/`worldgen`
  paths, but no explicit verb. Concrete add: `case "load"` in `handleDim`
  mirroring `keepDimensionLoaded` + `initDimension`.
- **PRESENT**: `/artest worldgen sample <dim> <cx> <cz>` (line 1283), plus
  bonus `ore-stats` subcommand.
- **PRESENT**: `/artest oxygen player <name>` (line 814).
- **PRESENT, BUT VERIFY**: `/artest planet info <dim>` returns 15 fields; SMART
  §5.3 calls for full DimensionProperties — cross-check against SMART source
  during Phase 4 and file any specific gap.
- **ADVISORY**: no category implements `case "help"` — `/artest <cat> help`
  falls through to "unknown subcommand". Not a SMART §5 hard requirement;
  optional cosmetic cleanup.
- **DEFERRED**: weather scope not audited this session per user direction.

### Phase 2a — CommandsSmokeTest depth (3/4 methods)
- `arHelpCommandPrintsUsageWithoutCrash` — asserts `/advancedrocketry help`
  prints "Subcommands:" header + a follow-up `/artest commands list` still
  works (server stays alive).
- `arCommandWithInvalidArgsReturnsErrorNotCrash` — asserts server survives a
  bogus subcommand. Comment in code notes that AR's `WorldCommand.execute`
  currently has no `default` branch (silent no-op on unknown subcommand);
  the test pins "no crash" rather than "explicit error" because tightening
  AR's parsing would be a production logic change (forbidden per SMART §15
  in this task scope).
- `artestRegistryWithBadSubcommandReturnsError` — asserts `/artest registry
  bogus` returns JSON `{"error":"unknown registry subcommand","sub":"bogus"}`.
- **SKIPPED**: `artestWeatherSetWithMalformedTicksReturnsError` (weather).
- Validation: `./gradlew testServer --tests "*.CommandsSmokeTest"` →
  **4/4 PASSED** (1 prior + 3 new), 1m 47s including dedicated-server boot.

## What did NOT land (carried over)

- Weather scope of F2 audit + Phase 2a's 4th method
  (`artestWeatherSetWithMalformedTicksReturnsError`).
- `weatherMode = per_dimension` default flip (TASK-01 Dependencies §): still
  uncommitted on `feature/tests`. Will surface only when Phase 2b
  (AtmosphereOxygen) or any weather-touching phase resumes.

## Pyramid status (unchanged delta)

- testUnit: +1 method (AstronomicalBodyHelperTest now 12 tests). Baseline
  unit count grew by 1.
- testServer: +3 methods in CommandsSmokeTest (1 → 4 tests). Baseline server
  count grew by 3.
- testIntegration: unchanged.
- testClient: unchanged.
- Predecessor marker reported 201/193-PASS/8-SKIP/0-FAIL across the full
  pyramid; after this session, +4 PASS expected on a full pyramid run, but
  full pyramid was NOT re-run end-to-end in this session — only the two
  scoped task slices above.

## Files touched

- `src/test/java/zmaster587/advancedRocketry/test/unit/AstronomicalBodyHelperTest.java`
  (+19 lines, 1 new method)
- `src/test/java/zmaster587/advancedRocketry/test/server/CommandsSmokeTest.java`
  (+44 lines, 3 new methods)
- `.agent/tasks/TASK-01-smart-depth-coverage.md`
  (Phase 5 stub replaced with concrete F2 findings, ~35 lines)

## Addendum (same session, ~17:00) — Phase 5 micro-fix landed

- `TestProbeCommand.handleDim`: added `case "load"` that pins the dim via
  `keepDimensionLoaded(true)` + `initDimension` and returns
  `{dim, loaded, providerClass, isARPlanet}`. Mirrors the
  weather/worldgen pattern, so test code can share idioms.
- `PlanetDimensionLoadTest`: added `dimLoadOnOverworldReportsLoaded` smoke
  test pinning the probe wiring against dim 0 (always loaded on boot).
- Validation: `./gradlew testServer --tests "*.PlanetDimensionLoadTest"` →
  **2/2 PASSED**, 1m 17s.
- TASK-01 Phase 5 checklist updated: `dim load` item now `[x]`.

## Next session candidates (TASK-01)

1. **Session 2 — Phase 1 PlanetDimensionLoad** (~3 h). Probe extension first
   (`providerClass` already present; add `biomeProviderClass`,
   `chunkGeneratorClass`, `saveDir` to `handleDim info`), then the 6 test
   methods enumerated in TASK-01 Phase 1.
2. **Phase 2a §7.19 4th method** — only if weather scope reopens.

No outstanding errors. No regressions observed. Branch state ready for commit.
