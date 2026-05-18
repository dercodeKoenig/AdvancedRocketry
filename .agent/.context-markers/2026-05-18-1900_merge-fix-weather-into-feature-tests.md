# Context Marker: merge-fix-weather-into-feature-tests

**Created**: 2026-05-18 19:00
**Branch**: `feature/tests` (merge commit `7531bf2f`)
**Status**: ✅ Merge complete. testUnit / testIntegration / testServer all
green on this Linux sandbox. testClient fails locally for environment
reasons only (no working OpenGL) — not a merge regression.

---

## TL;DR

- Merged `fix/weather` (`3d905a9e`) into `feature/tests` (`be2b05b0`) via
  `git merge --no-ff`. Merge commit: **`7531bf2f`**.
- Four content conflicts, all resolved preserving both branches' intent.
- One add/add conflict resolved by taking `fix/weather`'s real test in
  place of `feature/tests`' SMART-pending stub (B1 has landed, the stub
  is obsolete).
- Local test results post-merge:
  - `testUnit` — **87 / 0 / 0** (pass / fail / skip)
  - `testIntegration` — **80 / 0 / 0**
  - `testServer` — **90 / 0 / 3** (3 skips are PipeNetworkSmokeTest,
    pre-existing intentional skips from `feature/tests`)
  - `testClient` — 6 failures locally, all `Failed to start real client
    harness` → `LinuxDisplay.getAvailableDisplayModes NPE` (no GL
    available in this sandbox). On the author's Windows box this layer
    was 6/6 green at marker `2026-05-18-1745_weather-b1-shipped-eod`.

---

## Conflicts and how they were resolved

### 1. `build.gradle.kts`

Both sides added an explanatory comment above the `weatherMode` default.

- **HEAD (feature/tests)** had a longer comment explaining "default is
  per_dimension because that's current production via
  CustomDerivedWorldInfo".
- **fix/weather** added a shorter comment noting B1 flipped the default.

**Resolution**: dropped both comments. The HEAD comment was already
stale (referenced `CustomDerivedWorldInfo`, which fix/weather deleted).
The line above the conflict (lines 360–369) still carries the relevant
context about the default and the `-Pweather=shared` override. The rest
of `build.gradle.kts` (Cleanroom maven, MixinBooter dep, AP / refmap,
`mixinReverseSrg` task, `MixinConfigs` manifest entry, etc.) auto-merged
in cleanly.

### 2. `src/main/java/.../command/test/TestProbeCommand.java`

Both sides added new `/artest <sub>` subcommands at the same switch:

- HEAD added `scrubber`, `gascharge`, `pipe`
- fix/weather added `tp`

**Resolution**: kept all four. The corresponding `handleScrubber`,
`handleGasCharge`, `handlePipe`, `handleTp` methods all exist in the
merged file (`grep -n handle{Scrubber,GasCharge,Pipe,Tp}` confirms).

### 3. `src/test/java/.../AdvancedRocketryTestConstants.java`

Both sides modified the comment inside `expectedWeatherMode()`.

- HEAD comment referenced `CustomDerivedWorldInfo` — now stale.
- fix/weather comment mentioned B1 had just landed.

**Resolution**: rewrote the comment to reflect the merged reality —
default per_dimension via the B1 Mixin wrapper
(`PlanetWeatherManager` + `MixinWorldServerMulti`), override with
`-Dadvancedrocketry.tests.expectedWeatherMode=shared`.

### 4. `src/test/java/.../unit/PlanetWeatherStateTest.java` (add/add)

- HEAD: 120-line file of `@Ignore`d SMART §6.10 spec stubs documenting
  the contract for B1 before it landed.
- fix/weather: 87-line file of real `PlanetWeatherState` round-trip tests.

**Resolution**: `git checkout --theirs` — took fix/weather's real
implementation. The `@Ignore` stubs were a placeholder that B1 was
supposed to replace, and B1 has now landed.

### Bonus cleanup: deleted `src/test/java/.../unit/ARWeatherWorldInfoTest.java`

`feature/tests` carried this as another SMART §6.10 `@Ignore`d B1-pending
stub (`grep "future weather B1"` only matched this file post-merge). The
real `ARWeatherWorldInfoTest` from fix/weather lives at
`src/test/java/.../integration/ARWeatherWorldInfoTest.java`. Keeping the
stub would have produced confusing duplicate SKIP rows in test reports.
Removed in the merge commit.

---

## What the merge actually brings into `feature/tests`

From `fix/weather` (commits `7cd9446c` "Weather reimplemented",
`96e12c2a` "fix: correct inverted weather packet codes", `3d905a9e`
"docs: update context markers"):

- B1 per-dimension weather via Mixin:
  - `mixin/AccessorWorld.java`, `mixin/MixinWorldServerMulti.java`,
    `mixin/MixinPlayerList.java`
  - `world/weather/{PlanetWeatherState,PlanetWeatherSavedData,
    ARWeatherWorldInfo,PlanetWeatherManager,PlanetWeatherEventHandler}`
  - `mixins.advancedrocketry.json` (config registered programmatically
    from `AdvancedRocketryPlugin.<init>`)
  - MixinBooter 7.0 dep + `mixinReverseSrg` AP/refmap glue in
    `build.gradle.kts`
  - `MixinConfigs` manifest attribute on the jar
  - Default `weatherMode=per_dimension`
- Deletion of the old invasive HookLib path:
  - `world/CustomDerivedWorldInfo.java`, `world/WorldInfoSavedData.java`,
    `world/WorldServerNotMulti.java`
  - `ARHooks.java` emptied; `ARHookLoader.registerHooks` no-op'd
- New weather tests (in their fix/weather locations):
  - `unit/PlanetWeatherStateTest`, `unit/PlanetWeatherSavedDataTest`
  - `integration/ARWeatherWorldInfoTest`
  - `server/PerDimensionWeatherIsolationTest`
  - Strengthened `server/{WeatherBaselineTest,WeatherPersistenceTest,
    NonARDimensionIsolationTest}` and `client/WeatherClientSyncE2ETest`
- `/artest tp <dim> [player]` probe in `TestProbeCommand`
- Inverted-packet-code fix that finally turns client weather sync green
  (`STATE_BEGIN_RAINING=1`, `STATE_END_RAINING=2`)
- CLAUDE.md commit-message-prompt template

All of this stacks on top of `feature/tests`' SMART pyramid work
(Phase 4 complete per `2026-05-18-1530_task01-phase4-pyramid-complete.md`).

---

## Local test pyramid (post-merge, this sandbox)

| Layer            | Result        | Notes |
|------------------|---------------|-------|
| `testUnit`       | 87 / 0 / 0    | pass / fail / skip |
| `testIntegration`| 80 / 0 / 0    | |
| `testServer`     | 90 / 0 / 3    | 3 skips: PipeNetworkSmokeTest (pre-existing) |
| `testClient`     | 0 / 6 / 0 \*  | \* environment failure, see below |

### Why `testClient` is red here

The client harness launches a real GL-rendering Minecraft client per
scenario. This Linux sandbox has no working OpenGL — `glxinfo -B` reports
`MESA-LOADER: failed to open : /usr/lib/dri/_dri.so` (Mesa loader can't
resolve a driver). LWJGL 2.9.4 then crashes at
`LinuxDisplay.getAvailableDisplayModes`:

```
Caused by: java.lang.NullPointerException
    at org.lwjgl.opengl.LinuxDisplay.getAvailableDisplayModes(LinuxDisplay.java:947)
    at org.lwjgl.opengl.LinuxDisplay.init(LinuxDisplay.java:738)
    at org.lwjgl.opengl.Display.<clinit>(Display.java:138)
```

…which is what every failing client test reports as `Failed to start
real client harness`. **No merge regression** — the source code, mixin
config, weather wrapper, and `WeatherClientSyncE2ETest` are exactly what
shipped 6/6 green on the author's Windows machine at marker
`2026-05-18-1745_weather-b1-shipped-eod.md`. Re-run testClient on a host
with real GL to confirm; the expected result is `191/0/0` across all
four layers (87 + 80 + 90 + 6 = 263 minus the 3 server skips = 260
passing, but the prose total used at the prior marker was 191 because
TASK-01's later test additions were on `feature/tests` and only reach
this branch now via the merge).

---

## Git state

```
$ git log --oneline -3
7531bf2f  Merge fix/weather into feature/tests
be2b05b0  test: bring SMART per-scenario depth to prose-level coverage
3d905a9e  docs: update context markers post weather B1 ship   (via fix/weather)

$ git status
On branch feature/tests
Your branch is ahead of 'origin/feature/tests' by 4 commits.
(... untracked: .agent/.nav-{read-counter,workflow-state}.json ...)
```

`fix/weather` and `feature/tests` are now both being pushed to origin.

---

## Restore instructions

```
Read .agent/.context-markers/2026-05-18-1900_merge-fix-weather-into-feature-tests.md
```

No `.active` marker is set — this is an EOD snapshot of completed work,
not in-flight state to resume.
