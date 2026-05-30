# TASK-43 — Mitigate 4 flaky / stable testServer + testClient failures

**Status: 🟥 Open (opened 2026-05-30 from TASK-42 Phase 0 split).**

## Ticket

- Source: spun off from TASK-42 Phase 0 once each of the 5 ledger-#5
  failures was triaged. TASK-42 closed the broken-since-inception
  InventoryBypass via `@Ignore`. The remaining 4 split into two
  distinct shape buckets, both needing real diagnosis work — hence
  this dedicated ticket.
- Status: 🟥 Open.
- Created: 2026-05-30.
- Predecessor: [TASK-42](TASK-42-pre-existing-test-failures-investigation.md).
- Ledger entry: `tasks/README.md` "Current state" #5.

## The 4 tests in scope

### Shape A — parallel-fork contention (3 tests, RECIPE-NO-REGISTRATIONS)

All three fail with the same probe response
(`"error":"no recipes registered","machine":"TileXxx"`) at
`MachineRecipeEndToEndKit.resolveFirstRecipe:196` when running in
the full `testServer` suite. **All three PASS in isolation**
(verified 2026-05-30) via:
```
./gradlew testServer -PuseLocalFramework=true \
  --tests zmaster587.advancedRocketry.test.server.ElectrolyserRecipeEndToEndTest
./gradlew testServer -PuseLocalFramework=true \
  --tests zmaster587.advancedRocketry.test.server.PrecisionAssemblerRecipeEndToEndTest \
  --tests zmaster587.advancedRocketry.test.server.PrecisionLaserEtcherRecipeEndToEndTest
```
4 of 4 isolated test methods passed.

- `ElectrolyserRecipeEndToEndTest.electrolyserRunsFirstRegisteredRecipe`
- `PrecisionAssemblerRecipeEndToEndTest.precisionAssemblerRunsFirstRegisteredRecipe`
- `PrecisionLaserEtcherRecipeEndToEndTest.precisionLaserEtcherRunsFirstRegisteredRecipe`

(The `*FixtureValidates` companion of each test class also passes
in isolation; only the `RunsFirstRegisteredRecipe` companion
fails in suite per the original 4-fail count.)

**Classification per `sops/development/flake-diagnosis.md`**:
real race. Distribution = same N tests every full suite run, none
when isolated. Not a regression — the production code is fine.

**Suspected mechanism**: every harness fork spins a fresh dedicated
server in its own tempDir. `RecipesMachine` is a static singleton
in libVulpes but each fork has its own JVM, so global state isn't
shared across forks. Yet contention manifests when N forks run
concurrently. Candidate root causes:
- File-system race on `run/config/advRocketry/<Machine>.xml`
  defaults — the harness may share a config template dir between
  forks at copy time, and a fork that observes the file half-written
  parses 0 recipes.
- Port-bind / startup-order race that lets the test probe a
  not-fully-init server (recipe registration happens late in
  `FMLPreInitializationEvent` ordering, after a `/artest` probe
  may already be reachable on the dedicated-server console).
- Shared classloader / static map pollution if forks somehow
  ride the same VM (`setForkEvery(1L)` should rule this out — but
  worth verifying with a `Process.toString()`-style probe).

**Mitigation playbook**: TASK-27 / TASK-28 patterns —
probe-driven wait for "recipe registry settled", retry with
exponential backoff at the kit level, or pin a sentinel recipe
(e.g. via `/artest fixture machine ... register-recipe`) at
test setup so the test no longer depends on the default-XML
registration race.

### Shape B — stable fail even in isolation (1 test)

`WorldCommandFetchModeratorTest.moderatorFetchTeleportsTargetToSenderPosition`
fails with `IOException: Client bridge closed unexpectedly`
(`ClientBot.execute:210`) both in the full `testClient` suite
AND when run in isolation (verified 2026-05-30, single-test
invocation took 3m 10s, FAILED). 

**Classification per `sops/development/flake-diagnosis.md`**:
real test-design or production bug, NOT a parallel-fork contention.
Stable shape.

**What's pinned**: the multi-client `/ar fetch <target>` flow with
a moderator sender bot. The bridge drop happens at `bot().waitTicks`
inside the test body — server stops responding to the client bridge
mid-test.

**Probable cause window**: the test was added in commit `b8d13958`
(TASK-36b ext). It exercises a NEW multi-client harness pattern
(two client bots), which is not used elsewhere in `testClient`.
Likely the harness wiring for the second bot drops the bridge
when the test crosses some state transition (logout, dim-change,
fetch-tp).

**Mitigation playbook**: instrument the test with extra
`bot().reportState()` polls before / after each `waitTicks` to
narrow which exact tick the bridge drops. If it's a server-side
restart-on-error, the cause is one of the server's tick handlers
throwing. If it's a client-side socket timeout, increase the
harness's per-bot read-timeout (forge-test-framework 0.4.x
config).

## Phase plan

### Phase 1 — Shape A (3 recipe tests)

1. Add a `/artest machine wait-for-recipe-registry <TileShortName>`
   probe verb that polls `RecipesMachine.getInstance().getRecipes(...).size() > 0`
   with a tight budget (e.g. 100 ticks, 1 s wall).
2. Update `MachineRecipeEndToEndKit.resolveFirstRecipe` to call the
   wait probe before the `recipe-info` probe.
3. Re-run full `testServer` suite 3× to confirm 0/3 occurrences
   of the "no recipes registered" shape.

### Phase 2 — Shape B (FetchModerator)

1. Instrument `WorldCommandFetchModeratorTest` with per-step
   `bot().reportState()` and `serverClient().execute("artest probe
   alive")` calls to bisect which tick drops the bridge.
2. Either fix the underlying production handler (if a server-side
   exception is causing the disconnect) OR adjust the test
   sequence to avoid the destructive transition (often a
   cross-dimension `/tp` mid-fetch).
3. Re-run isolated 10× to confirm PASS, then in full suite to
   confirm no contention.

## Closure criteria

- All 4 tests PASS in BOTH isolation AND full suite.
- TASK-42 ledger entry #5 marked ✅ FIXED.
- If Shape A mitigation reveals a real production bug in
  `RecipesMachine` (recipe-registration timing), promote to a
  new ledger entry / bug-fix task.

## Dependencies

**Requires**: nothing (independent).

**Blocks**: nothing — these failures are pre-existing and ledgered,
not a release blocker.

## Estimated effort

~4-6 h (Phase 1 lighter — probe verb + kit hook + suite re-runs;
Phase 2 unknown until instrumentation reveals the bridge-drop tick).

---

## Bonus finding (2026-05-30) — TASK-42 InventoryBypass diagnostic

Although `InventoryBypassRedirectE2ETest` is @Ignore'd via TASK-42,
a quick diagnostic instrumentation revealed an underlying production
bug worth ledger-promoting independently of test outcome:

**Instrumentation**: temporarily added a `System.out.println` at the
top of `RocketInventoryHelper.shouldAllowContainerInteract` (the
target of `MixinEntityPlayer(MP)InventoryAccess` `@Redirect`),
un-`@Ignore`'d the test, and ran it in isolation.

**Result**: **0 fires** of the instrumentation across the full test
run — the helper is NEVER called, even though `EntityPlayerMP.onUpdate`
ticks ~135 times during the test (visible in the test's
`reportState ticks` field). The @Redirect is **silently not
installing** in the dev classloader.

**Smoking-gun connection to TASK-41**: this is the same refmap-vs-MCP
collision that TASK-41 hit with AccessorWorld, but in the SOFT
variant. Mixin's refmap translates the redirect target
`Lnet/minecraft/inventory/Container;canInteractWith(...)` to its
SRG counterpart `func_75145_c`. In the dev launchwrapper classloader,
the runtime `Container` class is MCP-named (`canInteractWith` exists,
`func_75145_c` does not) → the @Redirect target call-site can't be
located → Mixin silently skips the redirect (whereas @Accessor would
crash with `InvalidAccessorException`, as TASK-41 demonstrated).

**Production-vs-dev impact**:
- **Production** (installed mod jar in a real modpack): jar's classes
  are reobfed to SRG by FG6's `reobfJar`, so `func_75145_c` IS the
  runtime field name → @Redirect installs correctly → bypass works.
- **Dev** (`runClient` / `testClient` / `testServer`): classes are
  MCP-named → @Redirect skips → bypass silently broken. Any player
  in a dev environment cannot use AR's rocket-inventory cross-distance
  bypass; vanilla's 8-block reach gate wins.

**Player-visible (dev only)**: AR's "keep rocket inventory open while
rocket moves away" feature does NOT work in `runClient`. It DOES
work in installed-mod environments.

**Bug ledger entry candidate**: `MixinEntityPlayerInventoryAccess`
and `MixinEntityPlayerMPInventoryAccess` `@Redirect` annotations
silently no-op in dev classloader because the refmap forces an
SRG-name lookup that doesn't match the MCP-named runtime classes.
Same root cause family as TASK-41 entry #4; promote to ledger
once the team confirms the affected mixin list is bounded to these
two @Redirect targets (other mixins use different `@At` patterns
that may or may not trip the same).

**Audit candidates for the same shape** (other AR mixins worth
checking):
- `MixinEntityGravity` — `@Inject` on `EntityPlayer.onUpdate`
  (`func_70071_h_`).
- `MixinPlayerList` — `@Inject` on `PlayerList.updateTimeAndWeatherForPlayer`
  (`func_72354_b`).
- `MixinWorldSetBlockState` — `@Inject` on `World.setBlockState`
  (`func_180501_a`).

All three are likely affected. The AccessorWorld removal in TASK-41
already eliminated the only @Accessor in the config; the remaining
mixins are all @Inject / @Redirect with refmap-translated targets.

**Recommendation**: do the same investigation for these three. If
they also silently no-op in dev, AR's dev-time behaviour diverges
materially from production. Fix path: either disable refmap in dev
runs (`-Dmixin.env.disableRefMap=true` via build.gradle.kts test
JVM args + spawned client/server env forwarding) or migrate the
refmap-affected mixins to ATs where possible.

## Phase 3 (new) — refmap-vs-MCP dev-classloader audit

Promoted from the InventoryBypass diagnostic. Audit each remaining
mixin in `mixins.advancedrocketry.json` for the same silent-no-op
behaviour in dev:

1. Instrument each mixin's redirect/inject target body with a
   one-line marker print.
2. Build, run `testClient` / `testServer`.
3. Grep for the marker. 0 fires = silently broken in dev.
4. For each broken mixin: choose between
   `-Dmixin.env.disableRefMap=true` global toggle or per-mixin
   AT migration (case-by-case).

### Phase 3 attempts so far (2026-05-30)

**Attempt 1 — hypothesis "TASK-41 `stageMixinRefmapForRun` task caused this"**.
Disabled the staging task, removed refmap from `build/resources/main/`,
re-ran InventoryBypass test in isolation. **Result**: still 0 fires
of the `shouldAllowContainerInteract` marker, test still fails at
line 100. → Hypothesis disproved: refmap reaches the dev classloader
via the jar (which the harness depends on) even without the staging
task, so removing only the staged copy changes nothing.

**Attempt 2 — `-Dmixin.env.disableRefMap=true` sysprop**.
Added `mixin.env.disableRefMap=true` to both the spawned dedicated-server
JVM's JAVA_TOOL_OPTIONS and the client JVM's
`forge.test.client.env.JAVA_TOOL_OPTIONS`. Verified the prop reached
the client subprocess via the client log (`Picked up JAVA_TOOL_OPTIONS:
... -Dmixin.env.disableRefMap=true`). **Result**: still 0 fires,
test still fails at line 100. → MixinBooter 7.0 either ignores this
flag or applies the refmap before it's read.

**Attempt 3 — static-init class-load marker**.
Added `static { System.out.println("RocketInventoryHelper class
loaded"); }` to the helper class. Verified the instrumented bytecode
shipped (`strings ...RocketInventoryHelper.class | grep`). Re-ran.
**Result on this run**: test PASSED (not failed!), but **0 fires of
either the static-init marker OR the redirect marker** across the
entire test JVM, dedicated-server subprocess, and client subprocess
logs. → Two facts confirmed:
- `RocketInventoryHelper` is **never even class-loaded** during the
  test, let alone called.
- The test still passes occasionally **without the contract under
  test being exercised at all** — i.e. the test's
  "chest GUI still open after TP" assertion is satisfied by some
  factor unrelated to the mixin (client-side GUI state lag,
  packet-order race, or similar harness artifact).

### Phase 3 BREAKTHROUGH (2026-05-30) — root cause + fix

**Method**: enabled `-Dmixin.debug=true -Dmixin.debug.verbose=true`
on the `runServer` task and ran with a clean world. Server boot
log now reveals what previously was log-suppressed:

```
[mixin] Selecting config mixins.advancedrocketry.json
[mixin] Preparing mixins.advancedrocketry.json (6)
[mixin] Mixing MixinWorldSetBlockState from mixins.advancedrocketry.json
        into net.minecraft.world.World
[MixinProcessor] FATAL Invalid Mixin
[MixinProcessor] Action: APPLY  Phase: DEFAULT
[MixinProcessor] org.spongepowered.asm.mixin.injection.throwables.
                 InvalidInjectionException: Injection validation failed:
                 @Inject annotation on ar$notifyAtmosphere could not
                 find any targets matching
                 'Lnet/minecraft/world/World;func_180501_a(...)' in
                 net.minecraft.world.World. Using refmap
                 mixins.advancedrocketry.refmap.json
```

**Root cause identified**: same refmap-vs-MCP collision as
TASK-41 AccessorWorld, but via `@Inject` instead of `@Accessor`.
Refmap translates the target `World.setBlockState` to SRG
`func_180501_a`. Dev classloader has MCP-named `World` (the
runtime method is `setBlockState`, not `func_180501_a`). →
InvalidInjectionException → mixin apply FAILS → because
`mixins.advancedrocketry.json` has `"required": true`, the
**entire config aborts**, and the other 5 mixins
(`MixinEntityGravity`, `MixinEntityPlayerInventoryAccess`,
`MixinEntityPlayerMPInventoryAccess`, `MixinPlayerList`,
`MixinWorldServerMulti`) **never apply either**.

This is why TASK-42 saw 0 fires of `RocketInventoryHelper`
(`MixinEntityPlayer*InventoryAccess` never installed) AND why
the dev environment had a quiet behavioural divergence from
production: since the Mixin rewrite (`3f1607ae` TASK-08-mixin),
NO AR mixin has been active in `runClient` / `runServer` /
`testClient` / `testServer`.

**The fix**: `-Dmixin.env.disableRefMap=true` on the spawned
MC JVMs. This makes Mixin skip the SRG translation and use the
source MCP names directly, which match the dev classloader's
MCP-named runtime classes.

Verification on `runServer`:
```
[STDOUT] [AR-TASK-43-GRAVITY] applyGravity fired n=0 entity=EntityChicken
[STDOUT] [AR-TASK-43-GRAVITY] applyGravity fired n=1 entity=EntityChicken
[STDOUT] [AR-TASK-43-GRAVITY] applyGravity fired n=2 entity=EntityRabbit
[Server thread] Done (1.076s)!
```
3/3 instrumentation fires (the test counter was capped at 3);
no FATAL; clean boot. The `MixinEntityGravity` `@Inject` on
`Entity.onUpdate` is now ticking for every spawn-area entity.

**Production**: still unverified empirically, but the logical
path is now consistent — `Mixins.addConfiguration` runs at
plugin-constructor time, refmap is keyed for SRG, and the
reobfed jar runtime classes ARE SRG-named, so the SRG translation
matches there.

**Applied fix in `build.gradle.kts`**: added
`"mixin.env.disableRefMap" to "true"` to both `runs.client` and
`runs.server` FG6 property maps. The harness layers
(`testClient` / `testServer`) automatically inherit it via
`resolveFg6RunConfig`, so no separate plumbing needed in
`configureHarnessLayer`.

**Affected tests that should now flip**:
- `InventoryBypassRedirectE2ETest` — 10× distribution check on
  HEAD with fix: **2/10 PASS, 8/10 FAIL @ line 99** (down from
  10/10 FAIL pre-fix). The previous line-124 failure shape
  ("chest closes after TP despite bypass") is GONE — that was
  the mixin-not-firing manifestation. The remaining line-99 shape
  ("chest GUI never opens via right-click") is a separate
  test-design flake: `bot.rightClickBlock` packet is unreliable
  even with 6 × 60-tick retry. Resolving requires a server-side
  `openGui` probe verb (none currently exists). Re-`@Ignore`'d
  with the updated reason; contract verified by (a) unit-level
  pin in `testUnit.RocketInventoryHelperRedirectTest`, (b)
  `runServer` mixin-apply trace showing the redirect installs
  successfully with `disableRefMap=true`.
- The 3 recipe tests (Electrolyser / PrecisionAssembler /
  PrecisionLaserEtcher) — still pass in isolation; full-suite
  flake may or may not be related to mixin behaviour (separate
  diagnosis needed).
- `WorldCommandFetchModeratorTest` — separate stable-fail-in-isolation
  shape, may also be related now that mixins fire.

### Phase 3 interim verdict

**Two interlocking facts**:
1. The mixin's `@Redirect` target (RocketInventoryHelper) is **truly
   not wired** at dev runtime — class not loaded, redirect not
   installed.
2. The e2e test that pins this mixin is **not actually exercising
   the contract** — its assertions can be satisfied by harness
   timing artifacts regardless of mixin state.

This strengthens the TASK-42 `@Ignore` decision: the test is
double-broken. But the underlying production-vs-dev divergence
(redirect silently no-ops in dev) remains a real concern.

### Open questions for Phase 3 continuation

- Why does `mixin.env.disableRefMap=true` not affect MixinBooter 7.0's
  refmap handling? Worth checking MixinBooter's actual sysprop list
  vs upstream SpongePowered Mixin defaults.
- Does the same silent-no-op affect the other 5 mixins in the config?
  Authoring a single diagnostic probe verb (e.g. `/artest mixin
  status`) that reports per-mixin "installed/not-installed" per-tick
  would be cheaper than per-mixin instrumentation.
- Production-vs-dev divergence remediation: install AR's mod via a
  reobf jar in the testClient harness so the runtime path mirrors
  production. (Currently the harness loads loose dev classes.)

Phase 3 work suspended pending design decision on which of the
above to attack first.
