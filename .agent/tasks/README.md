# Test-coverage tasks — roadmap and dependency graph

## Source of truth

**Status of every task lives in its own `TASK-NN-*.md` file.** This
README is a derived index — Done and Backlog tables below mirror
each task file's header, nothing more. When in doubt, the
individual task file wins.

Lifecycle rules and the closure checklist are in
[`../sops/development/task-lifecycle.md`](../sops/development/task-lifecycle.md).
Bug-ledger history lives in
[`../history/known-bugs-ledger.md`](../history/known-bugs-ledger.md).

## Current state

- **Pyramid**: 805 (testUnit **284** / testIntegration 81 /
  testServer **383** / testClient 57). +35 on 2026-05-26 from
  the second audit batch: Gap 3 PlanetaryTravelHelper (11 unit),
  Gap 1 RocketLoader polarity (6 unit), Gap 7 GravityHandler (6 unit),
  Gap 4 SatelliteWeatherController NBT (2 unit), Gap 8 SatelliteMicrowave
  teir NBT (2 unit), Gap 6 FluidTank stacked-fill (2 server),
  Gap 5 TileDockingPort NBT+packet (4 server), Gap 2 MonitoringStation
  redstone trigger (2 server). Earlier same-day batch: +7 from
  TASK-30 Gap 3 elevator capsule (5 server + 2 client).
  +66 on 2026-05-25 from TASK-19 (11) + TASK-23 (2) + TASK-22 (4) +
  TASK-24 (3) + Tier 1 audit gaps (10) + Tier 2/3 audit gaps (27) +
  TASK-20 hovercraft (4) + TASK-21 /ar player-equipped (5).
  Counter regenerated via
  `grep -rc '@Test$' src/test/java/.../{unit,integration,server,client}/`.
- **testServer wall time**: 8m 27s (50 % faster than pre-B2).
- **Bug ledger**: 2 live bugs (Batch #2 opened 2026-05-25).
  Batch #1 fully drained by TASK-12 on 2026-05-23. Entries:
  (1) `SatelliteRegistry.getNewSatellite` returns `null` for unknown
  types instead of the documented `SatelliteDefunct` fallback —
  pinned by `SatelliteRegistryFallbackTest._documentsKnownBug` pair.
  Found during coverage-audit Gap 4.
  (2) `EntityElevatorCapsule.setStandTime(int)` ignores its
  argument and writes the `standTime` field — masked today because
  the single caller passes the field value. Ledger-only.
  Found during TASK-30 Gap 3 authoring (2026-05-26).
  See `.agent/history/known-bugs-ledger.md` Batch #2.

## Done

| ID | Title | Status |
|---|---|---|
| [TASK-01](TASK-01-smart-depth-coverage.md) | SMART per-scenario depth coverage | ✅ |
| [TASK-02](TASK-02-functional-coverage-expansion.md) | Functional coverage expansion (Phases 0–8, 11; Phase 9 → TASK-14, Phase 10 → TASK-15) | ✅ |
| [TASK-03](TASK-03-test-depth-and-harness-consolidation.md) | Test depth deepening + harness consolidation (A1/A2/A4/A5/A6/A7 + B1/B2/B4/C); A2 tail + B3 → TASK-10; A3 → TASK-10b | ✅ |
| [TASK-04](TASK-04-multiblock-machine-depth.md) | Multiblock machine depth (Warp / Laser Drill / Elevator / Black Hole / 12 multiblocks) | ✅ |
| [TASK-05](TASK-05-item-behaviour-suite.md) | Item-behaviour suite — unit-tier for 12 of 21 classes + SealDetector dispatch; player-tier → TASK-10b Phase 7 | ✅ partial |
| [TASK-06](TASK-06-mission-system-depth.md) | Mission-system depth — 20 tests + 9 mission probe verbs, rocket-side relink shipped | ✅ |
| [TASK-07](TASK-07-rocket-flight-cycle-beyond-launch.md) | Rocket flight cycle beyond launch (orbit / dim-transition / descent / landing / dismantle / failure) | ✅ |
| [TASK-08](TASK-08-asm-coremod-safety-net.md) | ASM coremod safety net | ❌ Obsolete (superseded by TASK-08-mixin) |
| [TASK-08-mixin](TASK-08-mixin-rewrite.md) | Rewrite ASM coremod (`ClassTransformer.java` + vendored HookLib) to Mixin | ✅ |
| [TASK-09](TASK-09-satellite-type-depth.md) | Per-satellite-type behavioural depth (3 suites / 14 pins + 15 satellite probe verbs) | ✅ |
| [TASK-10](TASK-10-fakeplayer-and-task03-tail.md) | TASK-03 deferred tail — A2 remainder (4 deep-tile tests) + B3 single-method-smoke suite-grouping | ✅ |
| [TASK-10b](TASK-10b-testclient-player-events.md) | testClient e2e player-event coverage — Phases 1-7 (5 suites + 15 pins + 9 probe verbs + Phase 7 player-tier item closures) | ✅ |
| [TASK-11](TASK-11-world-command-coverage.md) | `/ar` (WorldCommand) coverage — 23 tests across 4 classes (planet / star / misc / console-sender) | ✅ |
| [TASK-12](TASK-12-bug-fix-pass.md) | Production bug-fix sweep — 8 ledgered bugs fixed across 4 phases; pins flipped from `_documentsKnownBug` to positive contracts | ✅ |
| [TASK-13](TASK-13-wireless-transceiver-coverage.md) | Wireless transceiver E2E coverage (pivoted from pipe E2E — upstream deprecated pipes in commit 48610953) — 11 server-tier pins + 4 new probe verbs | ✅ |
| [TASK-14](TASK-14-companion-mod-integration-coverage.md) | Companion-mod integration coverage (JEI / GC / MO) — closed as Obsolete: mod-absent paths already pinned implicitly by 441 boot-the-server tests + TASK-11's JEI null-guard pin | ❌ Obsolete |
| [TASK-17](TASK-17-ssot-integrity-followups.md) | SSOT integrity follow-ups — `task-lifecycle.md` step 2.5 (counter regen) shipped; Phase 2a already done in `b97ddf0b`; Phase 2b premise wrong (no exact-120-RF assertion existed) → doc-comment cleanup only | ✅ |
| [TASK-18](TASK-18-industrial-machine-powered-cycle.md) | Industrial machine powered-cycle coverage — 7 of 9 multiblock machines shipped (14 server-tier tests + 3 probe extensions + shared `MachineRecipeEndToEndKit` with input-drain pin); ArcFurnace + PrecisionAssembler → TASK-26 (wildcard structure shape) | ✅ partial |
| [TASK-25](TASK-25-plate-press-coverage.md) | PlatePress (single-block redstone-triggered) recipe coverage — 1 class × 2 tests + 3 probe verbs (`fixture machine plate-press`, `recipe-info-block`, `entity scan-items`) | ✅ |
| [TASK-26](TASK-26-wildcard-based-machine-coverage.md) | Wildcard-structure machine coverage — ArcFurnace + PrecisionAssembler (2 classes × 2 tests = 4 server-tier tests + 1 generic-helper refactor with hatch-overlay + structure-block-filler for `'*'` cells + 1 kit hook for adaptive force-tick budget) | ✅ |
| [TASK-27](TASK-27-flake-fix-port-and-tick-races.md) | Flake fix — port-bind retry in `RealDedicatedServerHarness` + per-test polling for tick races + shape-#3 `tryCompleteWithRetry` kit helper (Beacon + cuttingMachine migrated) + `wireless-info` wait-for-tile probe + `field info` budget bump. **Acceptance partial**: 10× metric not achieved — residual flake shapes outside original scope → TASK-28. | ✅ partial |
| [TASK-28](TASK-28-residual-test-flakes.md) | Residual flake shapes from TASK-27 — chunk-force probe helper (F1/F6/F7), ForceField direct-tick refactor (F2), Centrifuge permissive output (F3), Observatory + Wireless migrations. **9/10 PASS in v10**; v11 F8 watch sweep (2026-05-25) confirmed **0/10 Beacon recurrence** — F8 in watching mode (1/5 clean reruns toward Obsolete), new F9 (MissionGasCompletion fluidEntries:0) at 1/5. TASK-29 not opened — triggers not met. | ✅ partial |
| [TASK-19](TASK-19-multiblock-powered-cycle-trio.md) | Multiblock powered-cycle (Terraformer / BHG / Beacon) — 11 server-tier tests across 4 classes: Phase 1a AR-native terraformer (3), Phase 1b non-AR config flip (2), Phase 2 BHG on station orbiting black-hole star (3), Phase 3 Beacon enable/disable/break (3). 5 new probe verbs (`machine controller-state`, `machine clear-batteries`, `config get/set` whitelisted, `star get/set-blackhole`). | ✅ |
| [TASK-22](TASK-22-uv-assembler-full-delta.md) | UV-assembler full behavioural delta from rocket assembler — 4 server-tier tests across 2 classes: Phase 1 bounds-constants delta via reflection (2), Phase 2 output entity class delta (rocket → EntityRocket, UV → EntityStationDeployedRocket) via new `uv-rocket` fixture probe (2). Phase 3 mount eligibility deferred — implicitly covered by Phase 2's entity-class pin. | ✅ partial |
| [TASK-23](TASK-23-sealdetector-remaining-branches.md) | SealDetector remaining branches — 2 of 3 deferred branches pinned: `notsealblock` via probe-driven `blockBanList` mutation, `fluid` via AR's `oxygenFluid` (IFluidBlock). Third branch `notfullblock` documented as unreachable (no vanilla/AR block satisfies the required full-collision-bbox + liquid/IFluidBlock combination). Phase 4 client mirror skipped — server-tier probe replicates dispatch 1:1. | ✅ partial |
| [TASK-24](TASK-24-spacearmor-chest-route.md) | SpaceArmor CHEST sub-inventory drain (testClient) — 3 testClient tests pinning vacuum-drain through `ItemSpaceChest.decrementAir` (component-walking + FluidStack drain in embedded pressure tank). 2 new probes (`player equip-space-chest`, `player held-air-component-route`). testClient harness requires `xvfb-run` wrapper on headless dev boxes. Phase 2 (Suit Workstation drive-through) deferred. | ✅ |
| [TASK-20](TASK-20-hovercraft-ride-coverage.md) | Hovercraft ride / mount / throttle / motion (testClient) — 4 client tests: mount via startRiding probe, dismount, throttle-via-drive-ridden-entity probe (composite that re-applies moveForward inline to defeat CPacketInput reset), unmounted hovercraft doesn't drift. Phase 3 fuel reframed as documentation — production has zero fuel logic; documented so future addition forces a contract pin. 5 new probes. | ✅ partial |
| [TASK-21](TASK-21-ar-player-equipped-positives.md) | `/ar` player-equipped positive paths (testClient) — 5 client tests: goto dim, goto station, giveStation chip, addTorch, addSolidBlockOverride. New `player exec-as-player` probe (bot-as-sender via commandManager) + op-self/deop-self + inventory-contains + give-held probes. `/ar fetch` deferred (needs two-bot harness); `/ar fillData` covered transitively by satellite-construction flow. | ✅ partial |

## Backlog

Backlog promoted 2026-05-23 from full-repo audit findings. Each
entry is an actionable TASK with a defined plan + acceptance.

| ID | Title | Status | Blocker / trigger |
|---|---|---|---|
| [TASK-15](TASK-15-visual-regression.md) | Visual regression infrastructure for Minecraft client | 👁 Watching | 4 explicit promotion triggers in task file (GUI refactor / modpack-report / JEI rework / texture-pipeline bump). Revisit + consider Obsolete if no trigger in 6 months. |
| [TASK-16](TASK-16-test-stability-flake-watch.md) | Test-stability flake watch — investigation deliverable. Three flake shapes root-caused; shape #3 mitigated in TASK-26 via kit retry; #1+#2 split into TASK-27; #4 (worldgen sampling) confirmed across 3 sightings, promoted to TASK-28 F7. | 🟡 Investigation complete | Investigation done 2026-05-23. |
| [TASK-29](TASK-29-scanning-satellite-tick-contracts.md) | Scanning satellite tick behaviour (6 types: OreMapping / Density / Composition / MassScanner / Optical / SpyTelescope) | Backlog | None — ready to ship (~4-5h). |
| [TASK-30](TASK-30-station-controller-tick-contracts.md) | Station controller tick contracts (Altitude / Gravity / Orientation) | Blocked | Needs `station controller-set-target` probe verb (Phase 0 ~2h). |
| [TASK-31](TASK-31-rocket-event-payload-contracts.md) | RocketLanded / RocketDismantle / RocketDeOrbiting external-subscriber payload contracts | Backlog | None — RocketEventRecorder already exposes the needed counters. |
| [TASK-32](TASK-32-tier3-misc-coverage.md) | Tier 3 misc — ItemPackedStructure deploy + custom atmosphereType NBT + MonitoringStation comparatorOverride | Backlog | None — three small contracts grouped for index efficiency. |
| [TASK-33](TASK-33-satellitebuilder-real-construction.md) | SatelliteBuilder real end-to-end construction (full GUI flow) | Blocked | Needs `bot().click()` audit for `ModuleBuildButton` or new `gui press-build-button` probe (Phase 0 ~2h). |
| [TASK-34](TASK-34-fuel-loader-active-transfer.md) | Fuel loader active fluid transfer (loader + unloader, both directions) | Blocked | Storage chunk capability loss — Phase 0 investigation may flip this to Obsolete. |
| [TASK-35](TASK-35-ar-fetch-two-bot-harness.md) | `/ar fetch` positive coverage (two-player verb) | Blocked | Needs `player spawn-fake-player` probe to provide a second resolvable player (Phase 0 ~3h). |
| [TASK-36](TASK-36-terraforming-and-service-station-depth.md) | Deeper contracts — TerraformingTerminal biome-mutation + ServiceStation repair cycle | Blocked | 36a needs `item make-biomechanger-chip` probe; 36b needs `service-station inject-broken-part` probe. |

## Conscious non-goals

Two audit findings are explicit **non-goals**, not gaps. They do
NOT get TASK files because they're deliberate decisions, not
deferred work:

- **Cross-session worldgen determinism** — same-seed-across-reboot
  histogram pins. Within-session determinism is covered by
  `WorldgenDeterminismAndSamplingTest`; cross-session adds
  significant fixture cost for a contract that's already
  implicitly preserved by Forge's chunk cache. Reopen only if a
  chunkgen change introduces a real cross-session divergence.
- **Rocket out-of-fuel mid-flight auto-explosion** —
  `RocketFlightFailureModesTest` deliberately pins the **current
  contract** ("no auto-explosion"). If production adds an
  explosion branch, the test flips polarity — no new task needed
  until then.

TASK-29 through TASK-36 promoted 2026-05-26 from the 2026-05-25
Tier 1/2/3 audit deferrals + 2026-05-26 audit out-of-scope list —
each prior free-form bullet is now an actionable TASK with
defined plan + blocker per `task-lifecycle.md`. Future deferrals
must land here as TASK files; free-form bullet lists in this
README are forbidden.

## Dependency graph

```
TASK-03 ──┬─► TASK-04  (multiblock)
          ├─► TASK-05  (items)        ─┐
          ├─► TASK-06  (missions)     ─┤── EntityPlayer paths
          ├─► TASK-07  (rocket cycle) ─┤   live in testClient e2e
          ├─► TASK-08  (ASM)           │   (TASK-10b)
          ├─► TASK-09  (satellite types)
          └─► TASK-10  (A2 tail + B3 grouping)

TASK-13 ✅ — independent (closed 2026-05-23)
TASK-14 ❌ — independent (closed Obsolete 2026-05-23)
TASK-15 👁 — independent, watching for 4 triggers
TASK-16 👁 — independent, watches flake pattern from TASK-12 close-out

Audit-2026-05-23 backlog (all independent of each other):
TASK-17 — SSOT integrity (touches TASK-09 satellite tests)
TASK-18 — industrial machine powered-cycle (touches TASK-04 multiblocks)
TASK-19 — multiblock trio (touches TASK-04 + TASK-06 surfaces)
TASK-20 — hovercraft testClient (touches TASK-10b layer)
TASK-21 — /ar positives (extends TASK-11)
TASK-22 — UV-assembler depth (extends TASK-07 / TASK-06)
TASK-23 — sealdetector branches (extends TASK-10b Phase 7)
TASK-24 — SpaceArmor chest route (extends TASK-10b Phase 7)
```

## Conventions

All TASK-NN docs share a structure:

- **Ticket**: source, status, creation date.
- **Context**: what's currently uncovered + why it matters.
- **Implementation Plan** (or **Approach options** for Backlog tasks
  with multiple paths): phased; each phase ~2-5 h.
- **Technical Decisions** (for In Progress / Completed tasks): same
  `no production logic changes` rule as TASK-01 §15. New probe verbs
  documented inline.
- **Dependencies**: explicit `requires` / `does NOT block` calls.
- **Completion Checklist** (Completed tasks): per
  [`task-lifecycle.md`](../sops/development/task-lifecycle.md).
- **EOD marker**: in `.agent/.context-markers/` with the date and a
  short slug. The `.active` file points at the most recent marker
  for `/nav:start` to pick up.

## Bug-tracking pointer

Live bug tracking is OFF the README. Per
[`CLAUDE.md`](../../CLAUDE.md#bug-tracking--every-discovered-production-bug-must-be-logged):

- New bugs are pinned in the test suite (positive contract or
  `_documentsKnownBug`-style pinning of wrong behaviour) AND
  recorded in `.agent/history/known-bugs-ledger.md` under a new
  batch heading.
- The historical Batch #1 is drained; future bugs open Batch #2.
- The `_documentsKnownBug` suffix is no longer used in test method
  names (three javadoc breadcrumbs remain — see history file).
