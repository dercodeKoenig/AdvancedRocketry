# Context Marker: weather-b1-impl-and-client-e2e-debug

**Created**: 2026-05-15 18:05
**Branch**: `fix/weather` (cross-repo: also `ForgeTestFramework` 0.4.0 → 0.4.2 dirty)
**Status**: B1 weather wrapper landed + 3 of 4 test layers green. **STUCK** on
the deep client E2E (`WeatherClientSyncE2ETest`): mystery END_RAINING packets
spam the client AFTER our correct BEGIN_RAINING, leaving the client at
`isRaining=false`. Investigation in progress; framework now preserves client
log on close so the next iteration has packet trace available.

---

> ## ✅ RESOLVED — 2026-05-18
>
> The END_RAINING packet spam was caused by **inverted protocol constants** —
> the names `STATE_BEGIN_RAINING` / `STATE_END_RAINING` in
> `PlanetWeatherManager` were swapped, and the "raining" branch of
> `MixinPlayerList` was sending code 2 (actually END) instead of code 1
> (actually BEGIN). Fixed in commit `96e12c2a`
> ("fix: correct inverted weather packet codes and unblock client sync"):
>
> - Swapped `STATE_BEGIN_RAINING`/`STATE_END_RAINING` constants to match
>   vanilla protocol (1=begin, 2=end — opposite of what we'd assumed)
> - Flipped the `MixinPlayerList` branch accordingly
> - No-op'd `ARHookLoader.registerHooks` to drop a server-boot NPE
> - Deleted debug-only `MixinNetHandlerPlayClient` + its config entry
> - Stripped the `System.err` debug print from `MixinPlayerList`
>
> **Full pyramid now 191 / 0 / 0** (pass / fail / skip) including
> `WeatherClientSyncE2ETest`. Tree is clean on `fix/weather`.
> See marker `2026-05-18-1745_weather-b1-shipped-eod.md` for the EOD snapshot.

---

## TL;DR for next session

1. **B1 implementation is DONE and working** — Mixin wrap of `WorldServerMulti`,
   `ARWeatherWorldInfo`, `PlanetWeatherSavedData`, `PlanetWeatherManager`,
   `PlanetWeatherEventHandler`, programmatic `Mixins.addConfiguration` in
   `AdvancedRocketryPlugin`, MixinBooter 7.0 (8.x and 9.x require ASM 7+
   which Forge 1.12.2 doesn't have).
2. **3/4 test layers green**: testUnit (79), testIntegration (64), testServer
   (42 — all weather server scenarios passing), testClient: 5/6 (everything
   except WeatherClientSyncE2ETest).
3. **The blocker**: client receives stream of `SPacketChangeGameState reason=1`
   (END_RAINING) packets from an unknown vanilla path that we haven't pinned
   yet. Our `MixinPlayerList` fix + multi-shot `syncToPlayer` both send
   correct BEGIN_RAINING (reason=2), but they get drowned out.
4. **Next concrete step**: read `/tmp/forge-test-client-last.log` (now
   preserved on every close thanks to framework 0.4.2), correlate the code-1
   spam timestamps against server log, and identify which vanilla code path
   on the server is emitting them. Then either Mixin that path, or remove the
   trigger.

---

## Session arc (3 phases)

### Phase 1 — per-dim weather B1 implementation (DONE, committed-ready)

Refactor away from invasive HookLib path (initDimension/loadAllWorlds/
CommandWeather hooks + `WorldServerNotMulti` + `CustomDerivedWorldInfo`) into
clean Mixin B1:

- **New files** under `src/main/java/zmaster587/advancedRocketry/`:
  - `mixin/AccessorWorld.java` — `@Accessor("worldInfo")` for `World.worldInfo`
  - `mixin/MixinWorldServerMulti.java` — `@Inject` at `<init>` RETURN
  - `mixin/MixinPlayerList.java` — fixes vanilla 1.12.2 bug in
    `updateTimeAndWeatherForPlayer` (currently DEBUG-instrumented with
    `System.err.println`; needs cleanup before commit)
  - `mixin/MixinNetHandlerPlayClient.java` — **DEBUG ONLY**, client-side
    packet logger; **MUST be removed before commit**
  - `world/weather/PlanetWeatherState.java` — pure state model
  - `world/weather/PlanetWeatherSavedData.java` — overworld MapStorage, dim id
    keyed
  - `world/weather/ARWeatherWorldInfo.java` — wrapper, delegates non-weather,
    `super()` no-arg (calling `delegate.cloneNBTCompound(null)` triggered
    `FMLCommonHandler.getDataFixer()` NPE in unit tests)
  - `world/weather/PlanetWeatherManager.java` — `shouldWrap` / `wrap` /
    `syncToPlayer` (sends `SPacketChangeGameState` codes 1/2/7/8) / legacy
    `MigrationProbe`
  - `world/weather/PlanetWeatherEventHandler.java` — currently uses
    **immediate sync** in event handlers; previously had multi-shot
    `{1, 10, 20}` deferred sync via `ServerTickEvent` that also didn't fix
    the client bug. Reverted to immediate after MixinPlayerList was added.
- **Modified**:
  - `build.gradle.kts` — Cleanroom + Sponge maven, `implementation(fg.deobf(
    "zone.rong:mixinbooter:7.0"))`, AP + refmap (uses `createSrgToMcp/output
    .srg` with REVERSED columns since FG6 emits TSRG2 and Mixin AP 0.8.5
    can't parse it — see `mixinReverseSrg` task), `MixinConfigs` manifest
    entry, `weatherMode` default flipped `shared` → `per_dimension`,
    forge-test-framework bumped to `0.4.2:dev`
  - `asm/AdvancedRocketryPlugin.java` — `MixinBootstrap.init()` +
    `Mixins.addConfiguration("mixins.advancedrocketry.json")` from
    constructor (manifest entry alone doesn't work in dev — mod loaded from
    classes/java/main without manifest)
  - `ARConfiguration.java` — added `enableCustomPlanetWeather` (default
    true), `logPlanetWeatherWrapping` (true), `forcePlanetWeatherWorldInfoWrapper`
    (false)
  - `WorldProviderPlanet.updateWeather` — warn-once if WorldInfo not wrapped
  - `AdvancedRocketry.java` — registers `PlanetWeatherEventHandler`
  - `ARHooks.java` — emptied (all 4 hooks were weather-motivated and replaced)
  - `command/test/TestProbeCommand.java` — added `/artest tp <dim> [player]`
    sub (bypasses `commandGoto`'s `sender instanceof Entity` gate)
  - `mixins.advancedrocketry.json` — config: AccessorWorld,
    MixinWorldServerMulti, MixinPlayerList, MixinNetHandlerPlayClient (client)
- **Deleted**: `world/CustomDerivedWorldInfo.java`,
  `world/WorldInfoSavedData.java`, `world/WorldServerNotMulti.java`

#### MixinBooter version journey
- 9.3 → `NoClassDefFoundError: org/objectweb/asm/ConstantDynamic` (bundled
  Mixin 0.8.5 uses ASM 7+ classes at `MixinInfo.validateClassFeatures`;
  Forge 1.12.2 ships ASM 5.2)
- 8.9 → same issue (bundled Mixin still calls `LanguageFeatures
  .scanMethodFeatures` requiring ConstantDynamic)
- **7.0 → WORKS** (Mixin 0.8.4 doesn't have validateClassFeatures path)

### Phase 2 — test suite (3/4 layers green)

#### Unit / Integration / Server — ALL GREEN

- `testUnit` 79 PASSED (+10 weather: PlanetWeatherStateTest x4,
  PlanetWeatherSavedDataTest x6)
- `testIntegration` 64 PASSED (+6 ARWeatherWorldInfoTest)
- `testServer` 42 PASSED, including:
  - `WeatherBaselineTest` (strengthened: now asserts
    `worldInfoClass=ARWeatherWorldInfo` on AR planets)
  - `PerDimensionWeatherIsolationTest` (NEW, 3 tests: rain A → not B/0; rain
    B → not A/0; clear A → B stays raining)
  - `WeatherPersistenceTest` (rewritten: uses AR planet dim 9301, asserts
    wrapper installed on both boots)
  - `NonARDimensionIsolationTest` (strengthened: nether/end/0 NOT wrapped)
- Server log proves wrap chain works:
  ```
  [ARWeather]: Wrapped WorldInfo for AR planet dim=9101 provider=WorldProviderPlanet
  worldInfoClass":"zmaster587.advancedRocketry.world.weather.ARWeatherWorldInfo"
  ```

#### testClient — 5/6, WeatherClientSyncE2ETest FAILING

5 pre-existing tests still PASS:
- `ClientConnectSmokeTest`, `GuidanceComputerGuiE2ETest`,
  `OxygenSuitClientStateE2ETest`, `PlanetSelectorGuiE2ETest`,
  `RocketBuilderGuiE2ETest`

`WeatherClientSyncE2ETest` reworked from `@Ignore` stub to real test:
- Doesn't extend `AbstractClientE2ETest` (its `@Before final` doesn't allow
  pre-staging workDir) — manages harnesses manually
- Pre-stages 2-planet XML (dims 9301, 9302) into `forge-client-weather-sync-*`
  tmp dir
- Uses `/artest tp <dim>` for cross-dim teleport (vanilla `/tp` doesn't cross
  dims; `/advancedrocketry goto` needs Entity sender)
- Uses `bot.reportWeather()` — new probe added to framework

### Phase 3 — Framework upgrades (`ForgeTestFramework` 0.4.0 → 0.4.2)

`ForgeTestFramework` checkout at `C:\Users\Quarter\IdeaProjects\ForgeTestFramework`,
modified files **dirty, published to mavenLocal**:

- **0.4.1**: added `report_weather` probe to `ForgeTestClientBootstrap.java`
  returning `{dim, worldInfoClass, isRaining, isThundering, rainTime,
  thunderTime, rainStrength, thunderStrength}`, plus `ClientBot.reportWeather()`
- **0.4.2**: `RealClientHarness.close()` now preserves `client.log` to
  `<java.io.tmpdir>/forge-test-client-last.log` BEFORE `deleteRecursively`
  (previously only preserved on startup failure). This finally let us see
  client-side packet flow.

---

## The deep dive — WeatherClientSyncE2ETest failure (UNRESOLVED)

### Observed failure mode (consistent across all retries)

```
client-visible isRaining must be true on dim A:
{ok:true, worldReady:true, dim:9301,
 worldInfoClass:net.minecraft.world.storage.WorldInfo,
 isRaining:false, isThundering:false,
 rainTime:0, thunderTime:0,
 rainStrength:0.089999996, thunderStrength:0.0}
```

- `dim:9301` ✓ teleport worked, client is on planet A
- `worldInfoClass: WorldInfo` ✓ (client-side is always vanilla; wrapper is
  server-only)
- `isRaining: false` ✗ — should be true since server-side `info.isRaining()=true`
- `rainStrength: 0.09` — climbing-but-not-1.0, indicates rain WAS on briefly

### Hypotheses tested + ruled out

1. **Vanilla 1.12.2 `PlayerList.updateTimeAndWeatherForPlayer` bug** —
   confirmed it has `if (worldIn.isRaining()) sendPacket(SPacketChangeGameState(1
   /*END_RAINING — wrong*/, 0.0F))`. Fixed via `MixinPlayerList.@Inject` at
   HEAD + cancel. Verified mixin fires correctly with:
   ```
   [ARWeather-MIXIN] updateTimeAndWeatherForPlayer name=Player734 dim=9301
   info.isRaining=true info.class=ARWeatherWorldInfo rainStr=0.04
   ```
   So my mixin sends BEGIN_RAINING (code 2) correctly. Client still ends up
   with isRaining=false anyway.

2. **`World.isRaining()` checks strength > 0.2, not flag** — discovered while
   debugging. Vanilla's wrapper code used `worldIn.isRaining()` which delegates
   to strength check. At teleport time strength=0.04 so vanilla skipped the
   entire `if` block (didn't send the buggy END either, but also didn't send
   BEGIN). My MixinPlayerList now uses `worldIn.getWorldInfo().isRaining()`
   directly. Doesn't help — client still false.

3. **Timing — `PlayerChangedDimensionEvent` fires before vanilla packet** —
   tried multi-shot deferred sync at {1, 10, 20} server ticks via
   `ServerTickEvent`. Verified all 3 syncs fire with correct state. Client
   still ends up false. Reverted to immediate sync after MixinPlayerList
   landed.

4. **AR's `WorldProviderPlanet.updateWeather` server-only block** — confirmed
   all weather logic + strength lerp is inside `if (!world.isRemote)`, so on
   AR planets the client has NO local strength lerp. But this means client's
   isRaining can ONLY be changed by packets — which makes the observed
   `isRaining=false` even more suspicious.

5. **Cleanup-on-close ate logs** — initially `cleanupOnClose=true` deleted
   the workdir before we could inspect. Switched to false locally for
   debugging. Then framework 0.4.2 added persistent preservation.

### THE smoking gun (just before nav-compact)

Once framework 0.4.2 preserved `client.log`, added `MixinNetHandlerPlayClient`
to log every `handleChangeGameState` packet on the client. Output sample:

```
[ARWeather-CLIENT] handleChangeGameState reason=2 value=0.0   ← BEGIN
[ARWeather-CLIENT] handleChangeGameState reason=2 value=0.0   ← BEGIN
[ARWeather-CLIENT] handleChangeGameState reason=7 value=0.0   ← strength=0
[ARWeather-CLIENT] handleChangeGameState reason=7 value=0.0
[ARWeather-CLIENT] handleChangeGameState reason=8 value=0.0
[ARWeather-CLIENT] handleChangeGameState reason=8 value=0.0
[ARWeather-CLIENT] handleChangeGameState reason=1 value=0.0   ← END (?!)
[ARWeather-CLIENT] handleChangeGameState reason=7 value=0.0
[ARWeather-CLIENT] handleChangeGameState reason=1 value=0.0   ← END
[ARWeather-CLIENT] handleChangeGameState reason=7 value=0.0
[ARWeather-CLIENT] handleChangeGameState reason=8 value=0.0
[ARWeather-CLIENT] handleChangeGameState reason=8 value=0.0
[ARWeather-CLIENT] handleChangeGameState reason=1 value=0.0   ← END
[ARWeather-CLIENT] handleChangeGameState reason=1 value=0.0   ← END
... continues with many more code 1 ...
```

- 2 BEGIN_RAINING packets arrive (from MixinPlayerList + handler sync)
- Then a STREAM of code-1 (END_RAINING) packets keeps arriving
- Strength packets (code 7) all have value=0.0

Server-side `MixinPlayerList` stderr only logged 2 calls (login dim 0 +
teleport dim 9301). So the code-1 spam is from a DIFFERENT vanilla call site,
NOT `updateTimeAndWeatherForPlayer`.

### Possible sources of `SPacketChangeGameState(1, ...)` in vanilla 1.12.2

Need to grep MCP-decompiled source for `new SPacketChangeGameState(1`. Known
candidates:
- `PlayerList.updateTimeAndWeatherForPlayer` — fixed by our mixin
- `WorldServer.updateWeatherBody` — broadcasts END on `prevRain ∧ !isRaining`
  edge. **Believed NOT to run for AR planets** because `WorldProviderPlanet
  .updateWeather` REPLACES `WorldProvider.updateWeather` (which is what
  delegates to `updateWeatherBody`). BUT — does it actually replace? Worth
  re-verifying.
- `MinecraftServer.tick` or `PlayerList.tick` per-player path?
- ServerWorldEventHandler?
- Some interaction with the OTHER AR planet's WorldServer ticking — note all
  AR planets share overworld's MapStorage and our `PlanetWeatherSavedData`.
  Could there be a feedback loop where dim 9302's tick affects something?

### Hypothesis to test FIRST next session

Looking at the packet stream: many `code 1 value=0.0` + `code 7 value=0.0`
+ `code 8 value=0.0` pattern. That matches what my `MixinPlayerList` sends
in the **"not raining" branch**:
```java
} else {
    playerIn.connection.sendPacket(new SPacketChangeGameState(1, 0.0F));
    playerIn.connection.sendPacket(new SPacketChangeGameState(7, 0.0F));
    playerIn.connection.sendPacket(new SPacketChangeGameState(8, 0.0F));
}
```

What if `updateTimeAndWeatherForPlayer` is being called MORE times than my
stderr log shows? Or my stderr is being buffered/dropped?

**ALSO** — the "not raining" else branch in my mixin might be too aggressive.
Vanilla only sent rain packets if isRaining; I added an ELSE that sends END.
This might fire for the overworld every server tick somehow.

Wait — `updateTimeAndWeatherForPlayer` is only called on login/dim-change/
respawn. Not per-tick. So that's not it.

UNLESS something is calling it repeatedly. Add additional logging in mixin to
count invocations.

### Next-session action plan

1. **Stop debug-spam mixins** for a clean run: temporarily disable
   `MixinNetHandlerPlayClient` (remove from `client[]` in
   `mixins.advancedrocketry.json`) to confirm whether logging IS the issue
   (it shouldn't be but eliminate).
2. **Count mixin invocations**: change MixinPlayerList stderr to include a
   call counter. If it's >2, find the extra caller (capture stack trace via
   `Thread.currentThread().getStackTrace()`).
3. **Decompile vanilla `WorldServer.updateWeatherBody` and verify** it's
   really not running for AR planet dims. If it IS running, that explains
   the spam — broadcasts code 1 on every edge. Could be that my AR-planet
   mixin somehow keeps toggling state.
4. **Check `PlanetWeatherEventHandler.onWorldLoad`** — fires
   `wrapWorldInfoIfNeeded` per loaded world. If the player joins, does
   `WorldEvent.Load` fire for *every* dim again, re-wrapping and broadcasting?
5. **Last resort**: instead of trying to fix vanilla's quirks, broadcast
   BEGIN_RAINING from `WorldProviderPlanet.updateWeather` itself on every
   tick where `info.isRaining()` differs from `prevRain`. Make AR the
   authoritative source of weather packets for its dims.

### Files to inspect next session

- `client.log` at `C:\Users\Quarter\AppData\Local\Temp\forge-test-client-last
  .log` (already preserved by framework 0.4.2)
- `latest.log` at `C:\Users\Quarter\AppData\Local\Temp\forge-client-weather-
  sync-{LATEST}\logs\latest.log`
- Vanilla source for `WorldServer.tick`, `WorldServer.updateWeatherBody`,
  `MinecraftServer.tick`, `PlayerList.serverUpdateMovingPlayer` — at
  `/tmp/forge-...` decompile or via IntelliJ navigation

---

## Cross-repo state (uncommitted)

### AdvancedRocketry (`C:\Users\Quarter\IdeaProjects\AdvancedRocketry`, branch `fix/weather`)

`git status -s`:
```
 M build.gradle.kts
 M src/main/java/zmaster587/advancedRocketry/ARHooks.java
 M src/main/java/zmaster587/advancedRocketry/AdvancedRocketry.java
 M src/main/java/zmaster587/advancedRocketry/api/ARConfiguration.java
 M src/main/java/zmaster587/advancedRocketry/asm/AdvancedRocketryPlugin.java
 M src/main/java/zmaster587/advancedRocketry/command/test/TestProbeCommand.java
 D src/main/java/zmaster587/advancedRocketry/world/CustomDerivedWorldInfo.java
 D src/main/java/zmaster587/advancedRocketry/world/WorldInfoSavedData.java
 D src/main/java/zmaster587/advancedRocketry/world/WorldServerNotMulti.java
 M src/main/java/zmaster587/advancedRocketry/world/provider/WorldProviderPlanet.java
 M src/test/java/.../AdvancedRocketryTestConstants.java
 M src/test/java/.../client/WeatherClientSyncE2ETest.java
 M src/test/java/.../server/NonARDimensionIsolationTest.java
 M src/test/java/.../server/WeatherBaselineTest.java
 M src/test/java/.../server/WeatherPersistenceTest.java
?? src/main/java/zmaster587/advancedRocketry/mixin/        (4 files: AccessorWorld,
   MixinWorldServerMulti, MixinPlayerList, MixinNetHandlerPlayClient)
?? src/main/java/zmaster587/advancedRocketry/world/weather/ (5 files: state, savedData,
   wrapper, manager, event handler)
?? src/main/resources/mixins.advancedrocketry.json
?? src/test/java/.../integration/ARWeatherWorldInfoTest.java
?? src/test/java/.../server/PerDimensionWeatherIsolationTest.java
?? src/test/java/.../unit/PlanetWeatherSavedDataTest.java
?? src/test/java/.../unit/PlanetWeatherStateTest.java
```

### ForgeTestFramework (`C:\Users\Quarter\IdeaProjects\ForgeTestFramework`)

Dirty, **0.4.2 already in mavenLocal**:
```
 M build.gradle                                              (0.4.0 → 0.4.2)
 M src/main/java/.../client/ClientBot.java                   (+ reportWeather())
 M src/main/java/.../client/RealClientHarness.java           (preserve log on close)
 M src/main/java/.../client/bridge/ForgeTestClientBootstrap.java (+ report_weather case)
```

### Pre-commit cleanup TODO

- Remove `System.err.println` debug line in `MixinPlayerList.ar$fixUpdateTime*`
- Remove `MixinNetHandlerPlayClient.java` (purely for debug)
- Remove its entry from `mixins.advancedrocketry.json` `client[]`
- Keep the framework `RealClientHarness.close()` log-preservation — that's
  generally useful and worth keeping in 0.4.2

---

## Build commands

```bash
# JAVA_HOME is REQUIRED — system one points at JRE
JAVA_HOME=/c/Users/Quarter/.jdks/corretto-1.8.0_322 \
  ./gradlew testClient -Dnet.minecraftforge.gradle.check.certs=false --no-daemon \
  --tests "*WeatherClientSync*"

# Logs are now preserved automatically (framework 0.4.2):
ls -lat /tmp/forge-test-client-last.log
ls -tr /c/Users/Quarter/AppData/Local/Temp/forge-client-weather-sync-*/logs/latest.log
```

---

## Restore Instructions

```
Read .agent/.context-markers/2026-05-15-1805_weather-b1-impl-and-client-e2e-debug.md
```

Or run `/nav:start` and confirm the active marker.

When restored, **the next action** is reading the preserved client.log and
correlating it with the server log to find the END_RAINING spam source.
