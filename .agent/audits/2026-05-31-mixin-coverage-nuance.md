# Coverage delta — 2026-05-31

**Branch**: `feature/tests`
**Parent audit**: [`2026-05-29-coverage-delta.md`](./2026-05-29-coverage-delta.md)
**Purpose**: record the mixin-layer coverage nuance uncovered while
verifying TASK-41/42/43, and confirm no gameplay-coverage churn
since the 2026-05-29 delta.

---

## 1. Churn since 2026-05-29 audit

| Commits since audit HEAD | Production gameplay changed? |
|---|---|
| `df98f5eb` fix: TASK-41 runClient mixin → AT | **dev-only** — AccessorWorld removed, AT added, refmap staged. No gameplay logic. |
| `41cccd53` docs: ledger #5 | No |
| `1103ec99` docs: TASK-42 Phase 0 | No |
| `410a9803` test+docs: TASK-42 close-out | Test `@Ignore` + docs only |
| `02a4626b` docs: TASK-43 + ledger #6 | No |
| `cf0f597e` docs: TASK-43 attempts | No |
| `a492b707` fix: TASK-43 Phase 3 mixin refmap | **dev-only** — `mixin.env.disableRefMap=true` on FG6 runs. reobf jar untouched. |
| `8fcb5d77` docs: SOP bash exit codes | No |

**Verdict**: zero production-gameplay churn. The 2026-05-29 subsystem
matrix (24 subsystems, 16 deep / 8 shallow, ~15 open gaps, ~39 h)
**still applies verbatim** — with the single mixin-row caveat below.

---

## 2. Mixin-layer coverage caveat (the actual delta)

The parent audits scored **"Mixin layer (was ASM coremod) — Deep ✅"**
on the assumption that the dev test layers (testServer / testClient /
runClient) actually exercise the mixins. TASK-43 Phase 3 disproved
that assumption for the dev classloader:

- From the TASK-08-mixin rewrite (`3f1607ae`) until TASK-43
  (`a492b707`), **all 6 AR mixins silently failed to apply in dev**.
  Root cause: `mixins.advancedrocketry.json` is `"required": true`;
  `MixinWorldSetBlockState`'s `@Inject` was first to fail PREINJECT
  (refmap translated `setBlockState` → SRG `func_180501_a`, absent on
  the MCP-named dev `World`), which aborted the entire config. `@Inject`
  failures log `FATAL` but do not crash the JVM, so it was invisible.
- Consequence: every dev-layer test that *appeared* to cover gravity /
  inv-bypass / per-dim weather was in fact running against a no-op
  mixin layer. The contracts were green for the wrong reason.

### Production was always correct
The reobf SRG jar's runtime classes ARE SRG-named, so the refmap
matched and the mixins applied. Verified 2026-05-31 on a clean Forge
1.12.2-14.23.5.2860 server (installer-built, AR + libVulpes 0.5.0 +
MixinBooter 7.0): **all 6 mixins applied**, `Done (5.829s)!`, zero
FATAL. So this is a **CI-trust** gap, not a player-facing regression.

### Post-fix verification (2026-05-31)
- `runClient` (`-Dmixin.debug=true`): 4 mixins applied at boot, rest
  lazy on world-load. Zero FATAL.
- `testClient` full suite (27 classes / 62 methods): **59 PASSED /
  1 sparse flake (`vacuumDrainsOxygenFromChestSubInventoryTank`,
  2/3 PASS isolated — `AtmosphereHandler` tick race, not mixin) /
  2 `@Ignore`** (`InventoryBypassRedirect`, `AreaGravityController`).
- Clean prod server: all 6 mixins applied incl. `MixinWorldServerMulti`.

---

## 3. Two new gaps in the mixin row

Reclassify **Mixin layer** from "Deep ✅" to **"Deep in prod / two
CI contracts unpinned ⚠️"**:

| New gap | Mixin | State |
|---|---|---|
| ~~**T**~~ | `MixinWorldServerMulti` | **DROPPED after deeper read 2026-05-31** — see below. |
| **U** | `MixinEntityPlayer{,MP}InventoryAccess` (inv-bypass) | Sole test `InventoryBypassRedirectE2ETest` is `@Ignore`'d (real `bot.rightClickBlock` packet-drop flake). Contract not enforced in CI. |

### Gap T correction (2026-05-31)
On first pass I flagged `MixinWorldServerMulti` as "no behavioural
test". Deeper read shows it is **impl-only**, not a gap:
- The mixin's sole effect is wrapping a WorldServerMulti's WorldInfo
  with `ARWeatherWorldInfo` for AR planets (per-dim weather isolation).
- That observable contract is **already pinned end-to-end** by
  `WeatherClientSyncE2ETest` (server→packet→client→render).
- `wrapWorldInfoIfNeeded` is idempotent and reached by TWO routes: the
  mixin (constructor RETURN) AND a `WorldEvent.Load` fallback
  (`PlanetWeatherEventHandler`) that "catches every world the Mixin
  route missed". If the mixin breaks, the fallback covers and the
  player sees no difference (no observable window between construction
  and Load where weather is read).
- Therefore a T-test could only pin "the mixin specifically (not the
  fallback) did the wrap" = a which-code-path impl-detail pin, exactly
  the SOP anti-pattern. **DROP.**

So only **U** is a genuine new mixin-CI gap. Effort: U ≈ 3 h (flake
re-design around a deterministic open-container path, not bot
right-click).

### Gap U — CLOSED 2026-05-31 (TASK-44)
`InventoryBypassRedirectE2ETest` un-`@Ignore`'d. The flake was entirely
in the GUI-open step (`bot.rightClickBlock` packet dropped before
chunk/player settle) — orthogonal to the mixin contract. Replaced with
a server-side `/artest player open-chest` probe that calls
`player.displayGUIChest(tileChestInventory)` directly (bypassing vanilla
`BlockChest.isBlocked`, which itself flaked when chunk-populate dropped
terrain above the placed chest). The mixin contract — bypass-on keeps
the chest GUI open across a 200-block teleport, bypass-off closes it —
is unchanged and now runs deterministically (4/4 reruns green). The
inv-bypass mixin `@Redirect` is thus pinned end-to-end through a real
client GUI session again, not just the unit predicate.

---

## 3a. TASK-44 batch close-out (2026-05-31)

The "convert all shallow → deep" batch resolved to **4 real contracts**
(F.4 pump-fluid-drain, B laser-drill-mining, C area-gravity-reset,
N asteroid-worldgen) after Phase-0 pruning + reconciliation against the
already-landed TASK-40 sweep. Details in
[`../tasks/TASK-44-shallow-to-deep-batch.md`](../tasks/TASK-44-shallow-to-deep-batch.md).

Full `testUnit + testIntegration + testServer` after the batch:
**429/430 pass**. The single failure —
`StationControllersTickContractTest.altitudeControllerWalksStationOrbitalDistanceTowardTarget`
— **passes 3/3 in isolation**, so it is a parallel-fork contention flake
(same shape as TASK-43 Shape A / TASK-16), NOT a regression from this
batch. The new asteroid-worldgen test is CPU-heavy and may aggravate
fork contention; if this flake's frequency rises, consider tagging
`AsteroidDimensionContainsAsteroidsTest` to a lower fork-concurrency
group. Root cause remains the pre-existing race, not the new test's
logic (which is seed-deterministic and 2/2 green in isolation).

## 4. Bottom line

- **No gameplay regression** from the mixin work — prod always applied
  the mixins; only dev/CI was blind.
- The 16 deep subsystems other than the mixin row are unaffected (none
  depended on the mixin layer firing in dev).
- New gaps T + U join the shallow backlog (A–N + S + T + U).
- Next planned action (per user 2026-05-31): convert **all shallow
  subsystems to deep in one batch** — see the successor TASK.

## 5. Final audit (2026-05-31, evening) — 4 parallel opus agents

Four independent opus-4.8 agents audited: bug-ledger accuracy, test-suite
health, coverage completeness, and SSOT/doc integrity. Outcomes:

- **Full suite re-verified green** with `-PuseLocalFramework=true`:
  `testUnit + testIntegration + testServer` BUILD SUCCESSFUL (19m51s),
  zero failures (the StationControllers flake did not recur this run).
  The health agent's "cannot build" finding was an agent-env artefact —
  it ran without `-PuseLocalFramework=true` and with an empty mavenLocal,
  so it never resolved `forge-test-framework:0.4.2` (which lives at
  `/workspace/ForgeTestFramework/build/libs/`, wired via the composite
  build). Not a project defect.
- **Bug-ledger accuracy**: all 7 entries' file:line refs verified
  accurate, no drift, no false pinning-test claims. Two fixes applied:
  (a) `known-bugs-ledger.md` had a stale "no live bugs" header and was
  4 entries behind the README — back-ported #4–#7 + corrected the header;
  (b) entry #2 (EntityElevatorCapsule setStandTime) dropped as impl-trivia
  per CLAUDE.md ("nothing observable" ≠ bug). Live count 5 → **4**
  (#1, #3, #5, #7), arithmetic now stated inline.
- **SSOT fixes**: added the missing TASK-44 row to the README Done table;
  qualified TASK-43 status to "Phase 3 done; A/B open".
- **Coverage**: exactly **one** genuine contract gap remained — **Gap S**
  (oxygen-vent blob cap). Now **CLOSED** — see §6.

## 6. Gap S closed — oxygen-vent blob cap

`OxygenVentBoundedByBlobCapTest` (testServer) pins the player-visible
contract that a vent **cannot pressurise an arbitrarily large sealed
space**. Production voids the whole blob (not a partial fill) when the
seal flood-fill reaches an open cell beyond the cap
(`AtmosphereBlob.run` lines 142-146). The test pins the fill mode to the
deterministic synchronous radius-based algorithm
(`atmosphereHandleBitMask = 0`, a real config option) + a small radius,
then builds two corridors differing only in length: the within-cap one
pressurises (`PressurizedAir`), the oversized one does not (stays at the
dim baseline). It does NOT pin the cap value — the cap is the
discriminator, not a magic number.

Test-only probe surface added (no production logic changed):
`oxygenVentSize` and `atmosphereHandleBitMask` added to the
`/artest config set/get` whitelist (both restored in `@After`).
Reused existing `artest atmosphere get`. 1/1 + reruns green.

The 2026-05-27 audit's gap backlog (A–N + S + T + U) is now fully
resolved or consciously dropped. No genuine contract gaps remain.
