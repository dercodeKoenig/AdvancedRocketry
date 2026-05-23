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

- **Pyramid**: 430 / 0 / 3 (testUnit 162 / testIntegration 80 /
  testServer 179 / testClient 9).
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

## Backlog

| ID | Title | Status | Blocker / trigger |
|---|---|---|---|
| [TASK-13](TASK-13-pipe-end-to-end-coverage.md) | Pipe end-to-end coverage (placed-block pipes) | 🔒 Blocked | `AdvancedRocketry.java:782-787` pipe-block registrations are commented out (`//TODO: add back after fixing the cable network`). Production-side reinstatement required. |
| [TASK-14](TASK-14-companion-mod-integration-coverage.md) | Companion-mod integration coverage (JEI / GalacticCraft / MatterOverdrive) | 🟢 Backlog | Approach choice required at session start (A/B/C — see task file). Recommended starting point: Option C (≈2 h). |
| [TASK-15](TASK-15-visual-regression.md) | Visual regression infrastructure for Minecraft client | 🟢 Backlog | "Build when there's a reason" — no pre-emptive scope. Worth promoting if a planned GUI refactor lands or modpack reports surface visual regressions. |
| [TASK-16](TASK-16-test-stability-flake-watch.md) | Test-stability flake watch (`BeaconMultiblockTest` + `MachineRecipeIntegrationTest` parallel-fork contention) | 👁 Watching | Promote when the flake reoccurs within ~5 testServer runs OR a third test joins. Currently 1 of 1 occurrences. |

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

TASK-13 (Blocked) — independent, awaits production-side unblock
TASK-14 — independent of all current work
TASK-15 — independent of all current work
TASK-16 — independent (watches a flake pattern from TASK-12 close-out)
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
