# Context Marker: client-e2e-fg6-harness

**Created**: 2026-05-14 11:50
**Note**: Cross-repo session — Gradle test-task topology rework + ForgeTestFramework
0.4.0 client-harness FG6 support. AR client E2E now loads the full mod; last
blocker is JEI's access transformer not applying in the harness launch.

---

## Conversation Summary

This session is a continuation of two prior ones (see markers
`2026-05-12-1847_test-suite-junit-migration-eod.md` and
`2026-05-13-1709_smart-8gaps-implemented-eod.md`). It started from "what tests
are next per SMART", drifted into a packet-roundtrip + special-infra batch,
then became a deep cross-repo client-E2E-harness project.

### Phase A — SMART high-ROI batch (COMMITTED as `5bb0abd New tests`)

The user asked "what's next per SMART", I produced a gap list, they picked the
"high-ROI" items:

1. **§6.9 — 4 more packet round-trips** (+5 tests in
   `integration/PacketSerializationTest.java`):
   - `PacketAirParticle` full round-trip
   - `PacketSpaceStationInfo` — non-deletion + deletion branch readClient
   - `PacketSatellitesUpdate` — wire-layout test (readClient hits FML side
     check + DimensionManager, untestable in unit JVM)
   - `PacketMoveRocketInSpace` — `_documentsKnownBugs` test: packet is DEAD
     CODE (no `addDiscriminator`) + has an inverted-`hasWorld` boolean bug +
     `read()` NPEs on default-ctor instance. Documented, not fixed (SMART §3).

2. **§7.18 — special-infra real cycles** (+3 server scenario classes):
   - `ForceFieldProjectionSmokeTest` — place projector facing UP (meta=1) +
     redstone below → `extensionRange` grows → forceField block appears →
     remove redstone → collapses. Needed several iterations: meta=0 (DOWN
     default) put the field into the stone floor; the registry name is
     lowercase `advancedrocketry:forcefield`; force-tick can't drive the
     `% 5 == 0` time gate so a new `/artest field info` probe busy-waits the
     natural tick loop.
   - `HovercraftEntitySmokeTest` — spawn `advancedrocketry:ARHoverCraft` via
     new `/artest entity spawn` probe, poll alive + posY sane.
   - `BeaconLocationProbeSmokeTest` — `/artest beacon list` contract: overworld
     starts with 0 beacons, unknown dim → error.

3. **New `/artest` probes added in this phase**: `field info/info-now`,
   `beacon list`, `entity spawn/info`, `block at`. (Earlier sessions added
   `fluid`, `vent`, `item`, `enchant`.)

   **Result**: `./gradlew test` (unit+integration) → 127 PASSED;
   `testAdvancedRocketryScenarios` → 38 PASSED, 6 SKIPPED. The user committed
   this as `5bb0abd New tests`.

### Phase B — Gradle test-task topology rework (UNCOMMITTED, in build.gradle.kts)

User complaint: "running client tests should need no flags; test type should be
selected by running subdirectories". The old setup had `test` (filtered to
unit+integration) and `testAdvancedRocketryScenarios` (server+client, needing
`-PclientHarness=true` or everything SKIPs).

Reworked `build.gradle.kts` into a per-directory topology:
- `testUnit` → §2.1 (69 tests), `testIntegration` → §2.2 (58 tests) — fast, no
  harness, own classpath.
- `testServer` → §2.3, `testClient` → §2.4 — `configureHarnessLayer()` shared
  config: FG6 runServer classpath augmentation, harness sysprops, parallel
  forks, the env/sysprop-forwarding `doFirst`.
- `test` → umbrella: empty filter + `dependsOn(testUnit, testIntegration,
  testServer, testClient)`. `./gradlew test` == everything.
- `testAdvancedRocketryScenarios` → kept as a back-compat alias
  (`dependsOn testServer, testClient`).
- **No required flags.** `-Pforks` / `-Pweather` remain OPTIONAL overrides with
  defaults. `-Pharness` / `-PclientHarness` REMOVED — harness is always on for
  the harness tasks; client harness auto-detects headless via reflective
  `GraphicsEnvironment.isHeadless()` (build-script classpath doesn't expose
  `java.awt`, hence reflection).
- `mustRunAfter` chain orders the layers fast→heavy.

Verified: `testUnit` 69 ✓, `testIntegration` 58 ✓ (= 127), `test --dry-run`
pulls all 4.

### Phase C — Client E2E harness, FG6 support (the deep dive)

`testClient` ran without a flag (topology works) but the 2 active client tests
(`ClientConnectSmokeTest`, `PlanetSelectorGuiE2ETest`) FAILED — and it turned
out **they had NEVER actually run before** (always among the 6 SKIPPED because
`-PclientHarness` was never passed). So the FG6 client harness path was
completely unverified. A long debugging chain followed; each fix revealed the
next layer:

1. **Natives not found** — `RealClientHarness.resolveNativesDir()` only checked
   the RFG/FG4 cache layout. FIX: added `PROP_NATIVES_DIR` override +
   project-relative auto-scan (`build/natives` for FG6, `run/natives/lwjgl2`
   for RFG, `natives` for older). AFFS proved this works.

2. **Empty client log** — on Windows the framework used a native `CreateProcessW`
   path that discarded stdout, AND `tailFile` ran AFTER `deleteRecursively`.
   FIX: always use `ProcessBuilder` + `LoggedProcess` (stdout→logfile);
   capture tail BEFORE deleting; bumped `tailFile` to 300 lines; on failure,
   preserve the FULL log at `<java.io.tmpdir>/forge-test-client-last.log`.

3. **SplashProgress.<clinit> NPE** — `FMLSanityChecker.fmlLocation` was null
   because the framework launched `mcp.client.Start` (WRONG launcher for FG6).
   The user pushed back ("does FG6 not have a GradleStart analog?") — and it
   does: `net.minecraftforge.legacydev.MainClient` / `MainServer` (the
   `legacydev` module, env-var driven). FIX: `forge.test.launcher.class.client`
   → `net.minecraftforge.legacydev.MainClient`. Also added
   `config/splash.properties` `enabled=false` to `bootstrapClientFiles` as
   defence-in-depth.

4. **Server vs client env conflict** — `AbstractClientE2ETest` boots a server
   harness AND a client in one test JVM. Both inherit ONE environment. AR's
   `doFirst` forwarded `runServer`'s config (server `mainClass`/`tweakClass`);
   the client needs `runClient`'s. FIX (the actual abstraction):
   - **Framework**: `PROP_CLIENT_ENV_PREFIX = "forge.test.client.env."` — any
     such system property is applied as an env var on the client `ProcessBuilder`
     ONLY (`applyClientEnvOverrides`), overriding the inherited server value.
   - **AR build script**: refactored the RunConfig reflection into
     `resolveFg6RunConfig(taskName)` + `packToolOptions(props)` helpers. The
     `doFirst` now resolves BOTH `runServer` (→ test JVM env + JAVA_TOOL_OPTIONS,
     for the server harness) AND, when `enableClient`, `runClient` (→ emitted as
     `forge.test.client.env.*` sysprops, incl. a packed
     `forge.test.client.env.JAVA_TOOL_OPTIONS`).

5. **AR resources not found** — client got past SplashProgress, loaded FML,
   but `ClientProxy.registerRenderers:103` NPE'd: `WavefrontObject` couldn't
   load `advancedrocketry:models/*.obj` and `mcmod.info` was missing ("missing
   required element 'name'"). Root cause: Gradle splits a source set into
   `build/classes/java/main` (classes) + `build/resources/main` (assets +
   mcmod.info). FML's `ModDiscoverer.findClasspathMods()` makes a SEPARATE mod
   candidate per classpath dir — the `@Mod` class is in the classes dir, so
   AR's mod + its IResourceManager resource pack root there with no assets.
   Decompiled FG6's `MinecraftRunTask.exec()` + `RunConfigGenerator` to confirm:
   `MOD_CLASSES` env (`advancedrocketry%%resources;advancedrocketry%%classes`)
   is set but NOTHING reads it in the legacydev+FG6 path (legacydev `Main`
   doesn't, Forge FML source has zero refs) — it's effectively vestigial.
   FIX: `testClient`'s `doFirst` (enableClient branch) `copy { }`s
   `build/resources/main` into `build/classes/java/main` before launch — assets
   co-located with classes, exactly like a packaged mod jar.

   **Result**: AR now loads FULLY on the client — renderers bind, `OBJLoader`
   parses AR's obj models, mcmod.info found.

6. **CURRENT BLOCKER — JEI access transformer** — next crash:
   `IllegalAccessError: tried to access method
   net.minecraft.client.renderer.texture.TextureMap.initMissingImage()V from
   class mezz.jei.gui.textures.JeiTextureMap`. JEI ships an access transformer
   that widens that vanilla method; it isn't being applied in the harness
   launch. This is FG6-dev-launch fidelity for THIRD-PARTY mods (JEI), not AR
   or the framework. NOT yet fixed.

## Documentation Loaded

- Navigator: restored from `2026-05-13-1709_smart-8gaps-implemented-eod.md`
- SMART spec referenced from `C:\Users\Quarter\Downloads\advanced_rocketry_full_test_suite_smart.md`
- Decompiled (read-only, in /tmp, not part of any repo):
  - Forge 1.12.2-14.23.5.2860 sources — `SplashProgress`, `Loader`,
    `ModDiscoverer`, `CoreModManager`
  - `legacydev` 0.2.3.1 + 0.2.4.1 sources — `Main`, `MainClient`, `MainServer`
  - ForgeGradle 6.0.53 — `MinecraftRunTask`, `RunConfigGenerator` (javap)

## Files Modified (UNCOMMITTED — three repos)

### AdvancedRocketry (`C:\Users\Quarter\IdeaProjects\AdvancedRocketry`)
- `build.gradle.kts` — ONLY uncommitted file. Contains:
  - Phase B test-task topology (testUnit/Integration/Server/Client + umbrella
    `test` + alias).
  - `resolveFg6RunConfig()` + `packToolOptions()` helpers.
  - `fg6HarnessProps`: `forge.test.launcher.class.client` →
    `net.minecraftforge.legacydev.MainClient`.
  - `configureHarnessLayer.doFirst`: forwards runServer env + (enableClient)
    runClient env as `forge.test.client.env.*` + merges resources into the
    classes dir.
  - `testClient` depends on `extractNatives`; sets `forge.test.client.nativesDir`.
  - NOTE: the temp `dumpRunConfigs` diagnostic task was added then REMOVED —
    it's gone from the file now.
- Phase A test files (PacketSerializationTest +5, ForceField/Hovercraft/Beacon
  scenario classes, TestProbeCommand probes) were ALREADY COMMITTED as
  `5bb0abd New tests`.

### ForgeTestFramework (`C:\Users\Quarter\IdeaProjects\ForgeTestFramework`)
- `build.gradle` — version `0.3.0` → `0.4.0`
- `src/main/java/.../client/RealClientHarness.java` — `PROP_NATIVES_DIR` +
  project-relative natives auto-scan; `PROP_CLIENT_ENV_PREFIX` +
  `applyClientEnvOverrides`; ProcessBuilder+LoggedProcess always (native path
  behind `forge.test.client.nativeLaunch=true`); tailFile 300 + full-log
  preservation; `bootstrapClientFiles` writes conservative `options.txt` +
  `config/splash.properties enabled=false`; `-Dorg.lwjgl.opengl.Display
  .allowSoftwareOpenGL=true`.
- `src/main/java/.../testing/TestAssertions.java` — NEW (restored from git
  history `c7102a6~1` — it was deleted in the 0.3.0 legacy-layer purge but AFFS
  still imports it).
- **Published to mavenLocal as 0.4.0** (plain + `-dev` + `-sources` jars).

### AdvancedForceField (`C:\Users\Quarter\IdeaProjects\AdvancedForceField`)
- `dependencies.gradle` — `forge-test-framework:0.3.0` → `0.4.0`. (The user had
  briefly set it to `0.3.0`; it's now `0.4.0` and AFFS compiles + its 3 client
  integration tests PASS — the proof that the framework abstraction works.)

## Current Focus

**State**: Cross-repo, all UNCOMMITTED except AR Phase A (`5bb0abd`).
- AR `build.gradle.kts` — dirty
- Framework — `build.gradle` + `RealClientHarness.java` dirty, `TestAssertions.java`
  untracked; 0.4.0 already in mavenLocal
- AFFS — `dependencies.gradle` dirty

**Test outcomes**:
- `./gradlew testUnit` → 69 PASSED
- `./gradlew testIntegration` → 58 PASSED
- `./gradlew testServer` → not re-run this session (was 34 PASSED last full run;
  Phase A added ForceField/Hovercraft/Beacon → should be ~37, all green when
  last run individually)
- `./gradlew testClient` → 2 active tests still FAIL (JEI AT blocker), 4 @Ignore
  SKIP. **No -PclientHarness flag needed any more.**
- AFFS `clientIntegrationTest` → **3 PASSED** (framework abstraction proven)

**Build env**: `JAVA_HOME=/c/Users/Quarter/.jdks/corretto-1.8.0_322` required
(system JAVA_HOME points at a JRE). Pass
`-Dnet.minecraftforge.gradle.check.certs=false` to every gradle invocation
(FG6 cert-check is flaky for libraries.minecraft.net).

## Next Steps

When the user resumes:

1. **DECISION PENDING** — the user was offered three options for the JEI AT
   blocker and hasn't picked yet:
   - (1) keep drilling — fix JEI access-transformer discovery in the harness
     (framework probably needs to forward AT config / FML coremod discovery);
     open-ended, every AT-shipping dep is a potential new layer.
   - (2) consolidate — framework abstraction is done + proven on AFFS; AR
     client reaches full mod load; leave the 2 active + 4 @Ignore client tests
     auto-skipping on headless, document the JEI blocker. **(Claude's
     recommendation.)**
   - (3) intermediate — a JEI-free AR client test (unlikely viable; AR's JEI
     integration is deep).

2. **Commit strategy** when ready (per global CLAUDE.md — user reviews diff,
   approves, THEN commit; Claude must NOT auto-commit):
   - Framework repo: commit `RealClientHarness` + `TestAssertions` + version
     bump as one logical "0.4.0: FG6 client-harness support" commit.
   - AFFS repo: commit the `dependencies.gradle` bump.
   - AR repo: commit `build.gradle.kts` (topology + FG6 client wiring).
   These are three separate repos → three separate commits/PRs.

3. If consolidating (#2): update `src/test/README.md` — the task names changed
   (`testUnit`/`testIntegration`/`testServer`/`testClient` + umbrella `test`);
   the old doc still says `test` = unit+integration and
   `testAdvancedRocketryScenarios` = server+client. Document the no-flags
   directory-driven model.

## Technical Decisions

- **Abstraction boundary**: framework = pure mechanism (spawn + bridge +
  lifecycle + natives autoscan + `forge.test.client.env.*` override channel);
  build script = policy (reflect the build plugin's runClient/runServer
  RunConfig). This is the honest split — the framework can't know FG6's
  package-private `RunConfigGenerator` internals, but it CAN define a flat
  contract the build script fills.
- **No required flags** — test type is selected by which task you run
  (= which directory). `-Pforks`/`-Pweather` stay as OPTIONAL overrides.
  Matches the user's long-standing "подпапки = типы тестов" preference.
- **Resource merge for client only** — the dedicated server never renders, so
  it never needs assets co-located; only `testClient`'s `doFirst` does the
  `build/resources/main` → `build/classes/java/main` copy. Mutating a build
  output dir is slightly unclean but harmless (regenerated by
  processResources/compileJava) and self-contained.
- **`MOD_CLASSES` is vestigial in FG6+legacydev** — confirmed by decompiling
  legacydev `Main` (doesn't read it) + grepping Forge FML source (zero refs).
  FG6 sets the env var but the resource-split is actually handled (in real
  `runClient`) by... still not 100% pinned, but the resource-merge workaround
  sidesteps the question entirely.
- **TestAssertions restored, not re-deleted from AFFS** — it's a tiny pure
  netty round-trip utility with no dependency on the purged legacy runner
  layer; cleaner to keep it in the framework than to rewrite AFFS's tests.
- **AFFS dep kept at 0.4.0** — 0.4.0 now is a strict superset of 0.3.0
  (restored TestAssertions + new client-harness features). AFFS's 3 client
  tests passing against it is the regression proof.

## User Intent & Goals (ToM)

**Primary goal this session**: make AR's client E2E tests actually runnable —
"like AFFS" — and figure out whether the client runtime can be abstracted into
the reusable framework. Secondary: ergonomic Gradle task topology (no flags,
directory-driven).

**Stated preferences (carried + new)**:
- "Никаких флагов, управление типами тестов — запуском поддиректорий" — drove
  Phase B. Honoured exactly.
- Pushes back on hand-wavy claims — "в FG6 нет аналога GradleStart?" forced the
  precise finding (`legacydev.MainClient`). Expects verified facts, not guesses.
- Russian conversational language throughout.
- Comfortable with deep cross-repo work but called `/nav-compact` at a natural
  checkpoint — values clean stopping points.

**Corrections made**:
- The user corrected the framework version: briefly set AFFS to `0.3.0`, then
  the resolution was to RESTORE `TestAssertions` into 0.4.0 rather than chase
  an old version — AFFS is back on `0.4.0`.

## Belief State

**What the user knows**:
- Deep AR / Forge 1.12.2 / FG6 / RFG knowledge.
- Owns all three repos (AR, ForgeTestFramework, AdvancedForceField) — they're
  siblings under `C:\Users\Quarter\IdeaProjects\`.
- Reviews diffs before committing — global CLAUDE.md rule, do NOT auto-commit.
- Knows the JEI blocker is the current edge and that it's a separate concern.

**Assumptions Claude made**:
- The framework 0.4.0 publish is legitimate (no pre-existing 0.4.0 was
  clobbered — framework git has no 0.4.0 tag/commit, version was 0.3.0).
- `./gradlew test` running everything (incl. heavy harness layers) is what the
  user wants ("вся директория test → все тесты") — accepted the slower
  `build`/`check` as a consequence.
- Mutating `build/classes/java/main` with merged resources is acceptable for a
  test-only path.

**Uncertainty areas**:
- Whether the user wants to keep drilling the JEI AT layer or consolidate.
- Whether `src/test/README.md` should be updated now or after the JEI decision.
- Exact mechanism by which FG6's REAL `runClient` makes mod resources load
  (the resource-merge workaround sidesteps it; the "proper" answer was never
  fully pinned).

## Restore Instructions

To restore this marker:
```
Read .agent/.context-markers/2026-05-14-1150_client-e2e-fg6-harness.md
```
Or use `/nav:markers` and select this marker.
