# TASK-41 — Fix runClient AccessorWorld mixin apply error

**Status: 🟥 Open (2026-05-29) — first-priority next session**

## Ticket

- Source: surfaced 2026-05-29 by user — `./gradlew runClient` (any
  DISPLAY value) fails during the Mixin APPLY phase before LWJGL
  initialises.
- Status: 🟥 Open.
- Created: 2026-05-29.
- **Priority: first-priority next session.** runClient is the
  primary live-debug entrypoint for mod development; this bug
  blocks any client-side live testing outside the testClient
  harness path.

## Symptom

```
[main/FATAL] [mixin]: Mixin apply failed mixins.advancedrocketry.json:AccessorWorld
  -> net.minecraft.world.World:
  InvalidAccessorException: No candidates were found matching
  field_72986_A:Lnet/minecraft/world/storage/WorldInfo;
  in net/minecraft/world/World
  for mixins.advancedrocketry.json:AccessorWorld->@Accessor[FIELD_GETTER]
      ::ar$getWorldInfo()Lnet/minecraft/world/storage/WorldInfo;
  [INJECT Applicator Phase -> mixins.advancedrocketry.json:AccessorWorld
   -> Apply Accessors -> -> Locate -> ...]

Caused by: java.lang.NoClassDefFoundError: net/minecraft/world/World
Caused by: java.lang.ClassNotFoundException: net.minecraft.world.World
Caused by: org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError
Caused by: org.spongepowered.asm.mixin.throwables.MixinApplyError
```

The `ClassNotFoundException: net.minecraft.world.World` underneath
the InvalidAccessorException is the actual root: at the moment Mixin
tries to apply `AccessorWorld`, the target class isn't reachable via
the launchwrapper classloader, so the field-lookup pass reports
"no candidates".

## What's not the cause

- **Not display/LWJGL**: confirmed by reproducing on `DISPLAY=:100`
  (Xvfb where LWJGL works fine for testClient). Same error.
- **Not my recent commits**: AccessorWorld was added in `3f1607ae`
  (TASK-08-mixin "rewrite ASM coremod to Mixin"). The TASK-40* sweep
  in this session didn't touch any mixin code or refmap.
- **Not a refmap miss**: `build/refmaps/mixins.advancedrocketry.refmap.json`
  correctly maps `worldInfo → field_72986_A:Lnet/minecraft/world/storage/WorldInfo;`
  in both `mappings` and `data.searge` sections. The refmap entry is
  *exactly* what the Mixin transformer is searching for.

## Reproducing

```bash
# Inside the dev container as `dev`:
cd /workspace/AdvancedRocketry
DISPLAY=:99 ./gradlew runClient   # fails (mixin)
DISPLAY=:100 ./gradlew runClient  # also fails (mixin, same trace)

# The testClient harness path does NOT hit this — it uses a
# different launchwrapper classpath / mixin-config assembly:
DISPLAY=:100 ./gradlew testClient -PuseLocalFramework=true
# runs the live client successfully (LowGravFallDamageE2ETest passes).
```

## Hypothesis

The legacydev `MainClient.main` launcher used by `runClient` initialises
launchwrapper with a classpath that doesn't include the deobfuscated
Minecraft jar at the point Mixin scans for `AccessorWorld`'s target.
By the time Minecraft IS on the classpath, the @Accessor pass has
already failed.

testClient's framework launcher (`legacydev-0.2.4.1-fatjar` per the
project dependencies) assembles a different classpath that does include
the MC jar at mixin-apply time.

## Phase 0 — verify the hypothesis

1. Run `runClient` with `--info` / `--debug` Gradle and capture the
   exact launchwrapper classpath at the moment mixin processor
   constructs.
2. Compare with testClient's launchwrapper classpath (re-run on `:100`
   with the same `--info` capture).
3. If MC jar is missing from runClient's CP at mixin apply, that's
   the smoking gun.

## Approach options

### Option A — Fix the classpath assembly for runClient (preferred)

Add the deobf MC jar to runClient's launchwrapper classpath earlier.
Likely a build.gradle.kts tweak to FG6's run config or a launchwrapper
tweaker that pre-loads `net.minecraft.world.World` before Mixin's
DEFAULT phase fires.

### Option B — Swap @Accessor for an access transformer (AT)

`AccessorWorld` only needs to widen visibility of `World.worldInfo`
(protected → public). An AT line in `META-INF/accesstransformer.cfg`:

```
public net.minecraft.world.World field_72986_A # worldInfo
```

…removes the Mixin dependency entirely for this field. Cost: AR adopts
ATs as a parallel patching mechanism (slight infra debt, but ATs are
the standard 1.12.2 Forge idiom for visibility widening).

PlanetWeatherManager would then access `world.worldInfo` directly via
reflection-less get/set (after the AT widens it to public).

### Option C — Use string-target `@Mixin(targets = "...")` instead of class literal

```java
@Mixin(targets = "net.minecraft.world.World")
public interface AccessorWorld { … }
```

The launchwrapper's mixin transformer locates string targets later
(in Phase.PREINIT) than class-literal targets. Might delay the lookup
past the point where MC is on the classpath. Worth a 10-minute test.

### Option D — Force-load `net.minecraft.world.World` from a coremod

Pre-touch the class in `AdvancedRocketryPlugin.injectData` so it's
loaded before Mixin's apply phase. Hacky; B is cleaner.

**Recommended**: try C first (cheapest), then fall back to B.

## Test plan

After fix:

1. `./gradlew runClient` (any DISPLAY) launches without mixin error.
2. PlanetWeatherManager weather-swap still works (the original reason
   AccessorWorld exists — TASK-08-mixin's "wrap vanilla weather without
   subclassing").
3. Existing testServer / testClient runs still green.

## Dependencies

**Requires**: nothing (independent investigation).

**Blocks**: any developer who wants to live-debug AR via `runClient`.

## Estimated effort

~2 h (Phase 0 + Option C attempt + Option B fallback + validation).

## Adjacent ledger entry

This bug is added to `.agent/tasks/README.md` "Current state" bug
ledger Batch #2 as entry #4 (per `flake-diagnosis.md` /
`task-lifecycle.md` rule — every production bug discovered must be
logged the moment it's found).
