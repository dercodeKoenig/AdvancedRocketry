# TASK-14: Companion-mod integration coverage (JEI / GalacticCraft / MatterOverdrive)

## Ticket

- Source: TASK-02 Phase 9 deferral, promoted into a tracked task on
  2026-05-23 during the SSOT cleanup.
- Status: **❌ Obsolete 2026-05-23** — see "Why obsolete" below.
- Created: 2026-05-23.

## Why obsolete

Reviewed 2026-05-23 same day the task was created. The premise that
the three integrations have "zero test coverage" turned out to be
misleading. The actual state:

### File sizes were over-estimated

The original task doc claimed ~800 LoC of "zero coverage". Actual
file sizes:

| Integration | Doc claim | Actual |
|---|---|---|
| `ARPlugin.java` | ~600 | 166 |
| `GalacticCraftHandler.java` | ~150 | 39 |
| `MatterOvedriveIntegration.java` | ~50 | 25 |

Plus ~150 LoC of thin JEI category/wrapper classes that are pure
pass-through to `RecipesMachine` (libVulpes) and JEI API — no
AR-specific contract to pin.

### All call sites are Loader-gated

Production code is already defensive at every call point:

- `AdvancedRocketry.java:1121` — `GalacticCraftHandler` only
  instantiated when `Loader.isModLoaded("galacticraftcore")`.
- `ARConfiguration.java:718` —
  `MatterOvedriveIntegration.addAndroidsToBypassList` only called
  when `Loader.isModLoaded("matteroverdrive")`.
- `AtmosphereNeedsSuit.java:30` —
  `MatterOvedriveIntegration.isAndroidNeedNoOxygen` only called
  when `Loader.isModLoaded("matteroverdrive")`.
- `CompatibilityMgr.java:24` — `ARPlugin.reload()` only called when
  `Class.forName("mezz.jei.api.BlankModPlugin")` succeeds; the
  catch block silently swallows `ClassNotFoundException`.
- `ARPlugin.java:75` — `jeiHelpers.reload()` only called when
  `jeiHelpers != null` (TASK-12 bug #7 fix).

### Mod-absent paths are implicitly pinned by every existing test

Build state at test time:

- JEI: `implementation` in build script — on runtime classpath, but
  `@JEIPlugin.registerCategories` is client-only. On testServer
  (dedicated server harness), `jeiHelpers` stays null → all the
  null-guarded paths above are exercised by **every** server test
  that calls `/ar reloadRecipes`. TASK-11's
  `reloadRecipesEmitsSuccessConfirmationMessage` explicitly pins
  this contract.
- GalacticCraft: `compileOnly` only — NOT on runtime classpath.
  Every testServer/testClient boot hits the
  `Loader.isModLoaded("galacticraftcore") == false` branch.
- MatterOverdrive: not in `build.gradle.kts` at all. Every boot
  hits the absent branch.

Pyramid currently has 441 tests, all of which boot AR with these
absent paths. If any Loader-guard regressed, the entire pyramid
would crash on boot. The "explicit pin per integration" Option C
from the original plan would duplicate this implicit coverage with
~3 tests of marginal new value.

### Adding present-branch coverage is not blocker-free

The original Option B (shim classes) and Option A (vendor companion
jars) are the only paths to test the **present** branches of these
integrations. Both have real cost — Option A blocked on licensing
for GC 1.12.2; Option B carries shim-drift risk vs the real APIs.
Neither is justified by current modpack-side signal: no cross-mod
regression has been reported.

### Decision

Closed as Obsolete because:

1. Mod-absent paths are already pinned (implicitly + explicitly).
2. Mod-present paths require an infrastructure investment whose
   value is unclear without a reported regression.
3. Keeping it in Backlog risks future-me re-litigating the same
   investigation.

If a cross-mod regression IS reported (e.g. a modpack player files
an issue about GC + AR interaction), open a fresh task — most
likely "TASK-NN: GC oxygen-event handler regression pin" with a
narrow scope tied to the specific regression, NOT a sweep of all
three integrations.

## Context

Three companion-mod integration surfaces exist in production with
**zero test coverage**:

| Integration | Production file | Lines |
|---|---|---|
| JEI | `integration/jei/ARPlugin.java` (+ supporting category classes) | ~600 |
| GalacticCraft | `integration/GalacticCraftHandler.java` | ~150 |
| MatterOverdrive | `integration/MatterOvedriveIntegration.java` | ~50 |

JEI integration was last touched in TASK-12 (added `jeiHelpers` null
guard on the dedicated-server path — historical bug #7b). The fix
ships unit-tested only indirectly via the
`reloadRecipesEmitsSuccessConfirmationMessage` server pin. The
**positive** JEI surface (recipe-category registration, runtime
handlers, ingredient lookups) is unguarded.

GalacticCraft and MatterOverdrive integrations are even more exposed:
zero tests, zero probe coverage, and both touch cross-mod hooks
(GC: dimension-type compatibility shims; MO: matter-replication
recipe import paths).

## What makes this task hard — the classpath problem

The CI / dev `testServer` classpath does **not** include
GalacticCraft or MatterOverdrive. JEI is partially present
(`compileOnly` in `build.gradle.kts`) but not initialised in the
test launch profile. The integration code paths therefore either:

- early-return on missing-mod sentinel (`Loader.isModLoaded("…")`)
- crash on classloader misses when test harness invokes them
  unconditionally

Both are valid production behaviours — the bug surface is in the
"mod is present" branch, which the harness cannot reach without
companion jars on the classpath.

## Approach options (decide at session start)

### Option A — Add companion mods to test classpath

Cost: significant (license + version compatibility for GC 1.12.2
fork, MatterOverdrive 1.12.2, JEI runtime launch). Plus longer
testServer wall time.

Benefit: real integration shape, end-to-end coverage of the actual
production code paths.

### Option B — Stub the missing classes on the classpath

Vendor minimal shim classes for the GC + MO API surface the
integrations call, declared in `src/test/.../shims/` and added to
the test classpath. The production code thinks the mod is present,
calls the shim, the shim records the call.

Cost: medium. Shim classes need to faithfully mirror the GC/MO API
signatures so the production code's reflection / instanceof checks
pass.

Benefit: covers the "mod is present" branch without licensing /
distribution concerns. Risk: shim drifts from real API, masking
regressions.

### Option C — Pin only the "mod absent" branch as a sanity contract

The narrowest scope. Just assert that with no companion mods on the
classpath, the integration handlers no-op cleanly (no NPE, no
crash, no spurious chat). One server test per integration.

Cost: trivial. ~3 tests.

Benefit: catches the regression class that TASK-12 bug #7b
represented (NPE on missing-companion path). Does NOT catch
positive-shape regressions.

**Recommended starting point**: Option C, then upgrade to B if a
positive-shape regression actually surfaces. Option A is
over-investment unless the modpack actively cares about cross-mod
behaviour stability.

## Out-of-scope deferrals

- Visual / GUI coverage of JEI recipe categories (would belong in
  TASK-15 visual regression once that infrastructure exists).
- Cross-mod chat messages / OreDict synonyms.

## Dependencies

- Does NOT block any other task.
- Touches the same `ARPlugin.java` that TASK-12 bug #7 fixed —
  future changes there should re-run whatever this task pins.

## Estimated effort

- Option C: ~2 h, 3 tests.
- Option B: ~8 h (~2 h shims + ~3 h per integration × 2 + JEI surface).
- Option A: ~12 h+ if companion-jar licensing allows it at all.
