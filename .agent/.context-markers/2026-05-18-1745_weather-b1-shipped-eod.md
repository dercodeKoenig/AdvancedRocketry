# Context Marker: weather-b1-shipped-eod

**Created**: 2026-05-18 17:45
**Branch**: `fix/weather` — clean, up to date with `origin/fix/weather`
**Status**: ✅ **DONE.** Per-dimension weather (B1 Mixin design) fully landed
and green on every test layer. The blocker described in marker
`2026-05-15-1805_weather-b1-impl-and-client-e2e-debug.md` is resolved.

---

## TL;DR

- **Full pyramid: 191 / 0 / 0** (pass / fail / skip) — unit + integration +
  server + client, including `WeatherClientSyncE2ETest`.
- **Root cause of the prior E2E hang**: inverted weather protocol constants.
  `SPacketChangeGameState` code **1 = BEGIN_RAINING**, code **2 = END_RAINING**
  (we'd assumed the opposite). `STATE_BEGIN_RAINING`/`STATE_END_RAINING` in
  `PlanetWeatherManager` were swapped, and `MixinPlayerList`'s "is raining"
  branch was therefore sending END instead of BEGIN.
- **Fixed in**: `96e12c2a fix: correct inverted weather packet codes and
  unblock client sync` (today, 2026-05-18 17:44 +0200).
- **No outstanding code work** on this branch. Nothing dirty in `git status`
  except untracked Navigator state files (`.agent/.nav-read-counter.json`,
  `.agent/.nav-workflow-state.json`).

---

## Repo state snapshot

### Branch / working tree

```
$ git branch --show-current
fix/weather

$ git status
On branch fix/weather
Your branch is up to date with 'origin/fix/weather'.

Untracked files:
  .agent/.nav-read-counter.json
  .agent/.nav-workflow-state.json
```

### Recent history on `fix/weather` (since the 2026-05-15 marker)

```
96e12c2a  2026-05-18  fix: correct inverted weather packet codes and unblock client sync
7cd9446c  2026-05-15  Weather reimplemented
0cf5a56a  2026-05-15  Fixed client test compatibility with FG6
```

`7cd9446c` is the big B1 landing (31 files, +2541/−589) — Mixin classes,
`PlanetWeather*` model/saved-data/manager/event-handler, `ARWeatherWorldInfo`
wrapper, deletion of the old `CustomDerivedWorldInfo` /
`WorldServerNotMulti` / `WorldInfoSavedData` invasive path, plus all new
weather tests (unit + integration + server + client).

`96e12c2a` is the small (6 files, +78/−66) fix that flipped the constants and
cleaned up debug aids.

---

## What `96e12c2a` actually changed

- `world/weather/PlanetWeatherManager.java` — swapped `STATE_BEGIN_RAINING`
  and `STATE_END_RAINING` to match the protocol (1 = begin, 2 = end).
- `mixin/MixinPlayerList.java` — flipped the rain-vs-no-rain branches so the
  "raining" path sends code 1 (begin) and the "not raining" path sends
  code 2 (end). Also stripped the `System.err.println` debug print.
- `mixin/MixinNetHandlerPlayClient.java` — **deleted** (was only ever a
  debug-only client-side packet logger).
- `resources/mixins.advancedrocketry.json` — removed
  `MixinNetHandlerPlayClient` from the `client[]` array.
- `ARHookLoader.java` — `registerHooks` is now a no-op. The old HookLib
  hooks were all weather-motivated and replaced by Mixins; the leftover
  call site was triggering an NPE during server boot.
- `CLAUDE.md` — documented the commit-message prompt template for future
  sessions (the "Commit message prompt" block).

---

## What's actually on disk now (B1 design, post-fix)

### New Mixin layer

`src/main/java/zmaster587/advancedRocketry/mixin/`:

- `AccessorWorld.java` — `@Accessor("worldInfo")` for `World.worldInfo`
- `MixinWorldServerMulti.java` — `@Inject` at `<init>` RETURN, installs
  `ARWeatherWorldInfo` on AR planet dims at world-load time
- `MixinPlayerList.java` — fixes the vanilla 1.12.2
  `updateTimeAndWeatherForPlayer` bug; reads `worldIn.getWorldInfo()
  .isRaining()` directly (vanilla's `world.isRaining()` checks strength
  > 0.2 which is wrong at teleport time)
- `MixinNetHandlerPlayClient.java` — *gone*

`src/main/resources/mixins.advancedrocketry.json` — config: `AccessorWorld`,
`MixinWorldServerMulti`, `MixinPlayerList`. Programmatically registered from
`asm/AdvancedRocketryPlugin.java` via `Mixins.addConfiguration(...)` in the
plugin constructor (manifest entry alone doesn't work in dev — mod is
loaded from `classes/java/main` without a manifest).

MixinBooter pinned at **7.0** (8.x / 9.x bundle Mixin 0.8.5 which needs
ASM 7+ classes — `ConstantDynamic` — that Forge 1.12.2 doesn't ship).

### New per-dim weather model

`src/main/java/zmaster587/advancedRocketry/world/weather/`:

- `PlanetWeatherState.java` — pure state model
- `PlanetWeatherSavedData.java` — overworld MapStorage, keyed by dim id
- `ARWeatherWorldInfo.java` — wrapper, delegates non-weather methods; calls
  `super()` no-arg (calling `delegate.cloneNBTCompound(null)` triggered
  `FMLCommonHandler.getDataFixer()` NPE in unit tests)
- `PlanetWeatherManager.java` — `shouldWrap` / `wrap` / `syncToPlayer`
  (sends `SPacketChangeGameState` codes **1**/**2**/7/8 — correct now),
  legacy `MigrationProbe`. **`STATE_BEGIN_RAINING = 1`,
  `STATE_END_RAINING = 2`.**
- `PlanetWeatherEventHandler.java` — immediate sync in event handlers,
  registered from `AdvancedRocketry.java`

### Config flags (`ARConfiguration.java`)

- `enableCustomPlanetWeather` — default `true`
- `logPlanetWeatherWrapping` — default `true`
- `forcePlanetWeatherWorldInfoWrapper` — default `false`
- `weatherMode` default flipped `shared` → `per_dimension`

### Build (`build.gradle.kts`)

- Cleanroom + Sponge mavens
- `implementation(fg.deobf("zone.rong:mixinbooter:7.0"))`
- AP + refmap via `mixinReverseSrg` task (FG6 emits TSRG2; Mixin AP 0.8.5
  can't parse it, so we reverse `createSrgToMcp/output.srg` columns)
- `MixinConfigs` manifest entry
- `forge-test-framework` at `0.4.2:dev`

### Deletions vs the old invasive path

- `world/CustomDerivedWorldInfo.java` — gone
- `world/WorldInfoSavedData.java` — gone
- `world/WorldServerNotMulti.java` — gone
- `ARHooks.java` — emptied (still present as a class)
- `ARHookLoader.registerHooks` — no-op'd today

---

## Test pyramid (all green)

| Layer            | Count          | Notes |
|------------------|----------------|-------|
| `testUnit`       | 79 passed      | +10 weather: `PlanetWeatherStateTest` x4, `PlanetWeatherSavedDataTest` x6 |
| `testIntegration`| 64 passed      | +6 `ARWeatherWorldInfoTest` |
| `testServer`     | 42 passed      | incl. `WeatherBaselineTest`, `PerDimensionWeatherIsolationTest` (3), `WeatherPersistenceTest`, `NonARDimensionIsolationTest` |
| `testClient`     | 6 passed       | `WeatherClientSyncE2ETest` finally green |
| **Total**        | **191 / 0 / 0**| pass / fail / skip |

Server log proves the wrap chain works (B1 design intact):

```
[ARWeather]: Wrapped WorldInfo for AR planet dim=9101 provider=WorldProviderPlanet
worldInfoClass":"zmaster587.advancedRocketry.world.weather.ARWeatherWorldInfo"
```

---

## Cross-repo state

### `ForgeTestFramework` 0.4.2

Still uses the framework changes published to `mavenLocal` on 2026-05-15:

- `report_weather` probe in `ForgeTestClientBootstrap.java`
- `ClientBot.reportWeather()`
- `RealClientHarness.close()` preserves `client.log` to
  `<java.io.tmpdir>/forge-test-client-last.log` (kept — generally useful)

These were dirty on 2026-05-15; status today is unchanged from this repo's
perspective (we depend on the published `0.4.2:dev` artifact). Whether
those changes have been committed in the framework repo itself is tracked
there, not here.

### AdvancedRocketry

Nothing dirty. All of the work described in the 2026-05-15 marker is
committed under `7cd9446c` + `96e12c2a`.

---

## Build commands (unchanged)

```bash
# JAVA_HOME is REQUIRED — system one points at JRE
JAVA_HOME=/c/Users/Quarter/.jdks/corretto-1.8.0_322 \
  ./gradlew testClient -Dnet.minecraftforge.gradle.check.certs=false --no-daemon \
  --tests "*WeatherClientSync*"

# Logs are preserved automatically (framework 0.4.2):
ls -lat /tmp/forge-test-client-last.log
ls -tr /c/Users/Quarter/AppData/Local/Temp/forge-client-weather-sync-*/logs/latest.log
```

---

## What could be next (none of this is in progress)

- Decide whether to merge `fix/weather` to the integration branch or leave it
  pending modpack review.
- Tag a release / update changelog for "per-dimension weather (B1)".
- The CLAUDE.md commit-message prompt template added in `96e12c2a` is now
  the canonical formatter for any commit-message asks.

---

## Restore Instructions

```
Read .agent/.context-markers/2026-05-18-1745_weather-b1-shipped-eod.md
```

Or run `/nav:start` and pick this marker (none is currently set `.active`).

This is an EOD snapshot — there is no in-flight work to resume.
