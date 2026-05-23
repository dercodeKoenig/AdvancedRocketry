# Context marker — 2026-05-23 TASK-13 wireless transceiver

**Slug**: 2026-05-23_task13-wireless-transceiver-shipped
**Branch**: `feature/tests`
**Session focus**: SSOT cleanup pass → TASK-13 pivot + close.

## What shipped this session

Single branch, two commits when this marker is written:

1. `cba7c99d` — SSOT discipline: history/ ledger move, task-lifecycle
   SOP, 4 new TASK files (13/14/15/16), README rewrite, DEVELOPMENT-
   README deduplication.
2. (next commit) — TASK-13 wireless transceiver close-out per the new
   SOP.

## TASK-13 pivot — the key discovery

The original TASK-13 was scoped as "Pipe end-to-end", **Blocked** on
the commented-out registrations at `AdvancedRocketry.java:782-787`.
Investigation via `git log -S 'blockFluidPipe'` surfaced upstream
commit `48610953` titled
**"deprecating pipes, added wireless transciever, closes #1075 #1034
#771 #757"**. The TODO comment "add back after fixing the cable
network" was misleading — the pipes were intentionally retired in
favour of `BlockTransciever`.

User chose the **pivot** path: drop pipe E2E scope entirely, repoint
TASK-13 at the live replacement (`TileWirelessTransciever`). Got
green pyramid on the first try.

## Shipped artefacts

**Probe extensions** at `TestProbeCommand.handlePipe`:
- `wireless-info` extended (now surfaces `mode` + `enabled`).
- `wireless-set-mode <dim> <x> <y> <z> <extract|inject>`.
- `wireless-set-enabled <dim> <x> <y> <z> <true|false>`.
- `wireless-role-on-network <dim> <x> <y> <z>` — reads observed
  source/sink registration on the live `dataNetwork`.

**Tests** — 11 server-tier pins across:
- `WirelessTransceiverContractTest` — 10 tests (shared harness).
- `WirelessTransceiverRestartTest` — 1 test (per-method harness for
  NBT round-trip + onLoad role re-registration across restart).

**Stale-claim sweep**:
- `AdvancedRocketry.java:781` — misleading "fix the cable network"
  TODO replaced with honest deprecation note.
- `PipeNetworkSmokeTest.java:185-192` — `@Ignore` reasons updated
  from "fix cable network" to "deprecated upstream (48610953)".
- `PipeNetworkHandlerDeepTest.java:31-50, 239-251` — class javadoc
  + obsolete inline "DOCUMENTS KNOWN PRODUCTION BUG" block both
  rewritten to reflect TASK-12 fix state.

## Pyramid impact

Before: 430 / 0 / 3 (testUnit 162 / testIntegration 80 / testServer
179 / testClient 9).
After: 441 / 0 / 3 (testUnit 162 / testIntegration 80 / testServer
**190** / testClient 9). +11 server-tier.

testServer wall time unchanged (~8m 27s; shared harness amortises
the new 10-test class).

## Process notes — first run of the new task-lifecycle SOP

This is the first task closed under the freshly-written
[`task-lifecycle.md`](../sops/development/task-lifecycle.md). Of
note:

- The mandatory **stale-claim sweep** (step 3) caught 4 sites of
  drift in test javadoc / production comments. Three of them
  referenced "fix the cable network" — exactly the misleading
  phrase that wasted ~30 min of investigation earlier in the
  session. Without the sweep, those phrases would have cost future
  sessions the same time.
- The `_documentsKnownBug` historical ledger (moved to
  `.agent/history/known-bugs-ledger.md` in the prior commit) had
  zero impact on TASK-13's flow — the ledger is genuinely frozen
  and the new task didn't produce new bugs.
- TASK-13 doc was renamed via `git mv` (kept blame chain), edited
  with new content. README Backlog row removed in same edit pass.

## Follow-ups (deferred — not opened as tasks yet)

- `WirelessTransceiverContractTest` does NOT cover adjacent-tile
  data-flow (`update()` pushing/pulling via an adjacent
  `IDataHandler`). If a regression in that path surfaces, open
  TASK-13b. Scope estimate: ~3 h (need a placed `IDataHandler`
  partner tile + tick-loop driver).
- `ItemLinker` end-to-end under a real player would belong on the
  testClient layer; not opened — not a regression class today.

## Branch state at marker write time

`feature/tests` ahead of origin by 1 commit (the SSOT-discipline
commit pushed earlier in the session). The TASK-13 commit is about
to be created — see next commit hash in `git log` after this marker
is committed.

## Resume conditions

Next session can pick from the Backlog table:
- TASK-14 (companion-mod integration) — clean independent start,
  approach choice deferred to session start.
- TASK-15 (visual regression) — pre-emptive infra, only worth
  starting if a planned GUI refactor or modpack-report justifies.
- TASK-16 (test-stability flake watch) — still in "watching" state,
  no promotion trigger fired yet.
