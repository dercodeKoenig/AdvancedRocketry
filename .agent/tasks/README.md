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

- **Pyramid**: 677 / 0 / 3 (testUnit 237 / testIntegration 80 /
  testServer 319 / testClient 41). Counter verified 2026-05-23 via
  `grep -rc '@Test$' src/test/java/.../{unit,integration,server,client}/`.
  Earlier README claim of 441 was stale by 236 tests — see TASK-17.
- **testServer wall time**: 8m 27s (50 % faster than pre-B2).
- **Bug ledger**: drained (8 of 8 fixed in TASK-12 on 2026-05-23).
  See `.agent/history/known-bugs-ledger.md` for the historical
  batch. No live bugs tracked at present.

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

## Backlog

Backlog promoted 2026-05-23 from full-repo audit findings. Each
entry is an actionable TASK with a defined plan + acceptance.

| ID | Title | Status | Blocker / trigger |
|---|---|---|---|
| [TASK-15](TASK-15-visual-regression.md) | Visual regression infrastructure for Minecraft client | 👁 Watching | 4 explicit promotion triggers in task file (GUI refactor / modpack-report / JEI rework / texture-pipeline bump). Revisit + consider Obsolete if no trigger in 6 months. |
| [TASK-16](TASK-16-test-stability-flake-watch.md) | Test-stability flake watch (`BeaconMultiblockTest` + `MachineRecipeIntegrationTest` parallel-fork contention) | 👁 Watching | Promote when the flake reoccurs within ~5 testServer runs OR a third test joins. Currently 1 of 1 occurrences. |
| [TASK-18](TASK-18-industrial-machine-powered-cycle.md) | Industrial machine powered-cycle depth (×10 machines) | 🟢 Backlog | Highest player-impact gap (#1). Pattern source: `MachineRecipeIntegrationTest`. ~6 h. |
| [TASK-19](TASK-19-multiblock-powered-cycle-trio.md) | Multiblock powered-cycle (Terraformer / BHG / Beacon enable) | 🟢 Backlog | Three independent multiblocks, shared shape. ~9-10 h. |
| [TASK-20](TASK-20-hovercraft-ride-coverage.md) | Hovercraft ride / throttle / fuel-drain coverage (testClient) | 🟢 Backlog | testClient territory; player-input simulation. ~9 h. Largest single testClient task in backlog. |
| [TASK-21](TASK-21-ar-player-equipped-positives.md) | `/ar` player-equipped subcommand positive paths (testClient) | 🟢 Backlog | Completes `/ar` surface that TASK-11 started — guard side already deep, this is the positive side. ~6 h. |
| [TASK-22](TASK-22-uv-assembler-full-delta.md) | UV-assembler full behavioural delta from rocket assembler | 🟢 Backlog | Bounds / output entity class / mount eligibility. Class-identity pin replaced by real contracts. ~4 h. |
| [TASK-23](TASK-23-sealdetector-remaining-branches.md) | SealDetector remaining branches (`notsealblock` / `notfullblock` / `fluid`) | 🟢 Backlog | Three branches deferred from `SealDetectorDispatchTest`. ~4 h. |
| [TASK-24](TASK-24-spacearmor-chest-route.md) | SpaceArmor CHEST sub-inventory drain route | 🟢 Backlog | Phase 7 (TASK-10b) closed the cheaper enchanted-armor route; this finishes the suit-family chest route. ~2.5 h. |

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

No other open work. Future deferrals must land here as TASK files —
free-form bullet lists in this README are forbidden (see
`task-lifecycle.md`).

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
