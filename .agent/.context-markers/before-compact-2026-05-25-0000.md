# Context marker — pre-compact 2026-05-25

**Slug**: before-compact-2026-05-25-0000
**Branch**: `feature/tests`
**Trigger**: `/navigator:nav-compact` after long TASK-27 + TASK-28 +
flake-diagnosis SOP session. Context at ~41 % (414k / 1M tokens),
383k in messages. Compacting at a clean boundary.

## Session arc (chronological)

1. **TASK-27** opened from TASK-16 investigation — port-bind retry
   + tick-timing race fixes. Shipped Phase 1 + Phase 2 + extended
   shape-#3 sweep. Closed ✅ partial.
2. **TASK-28** opened for residual flakes (F1-F7). Five 10×-rerun
   sweeps (v6-v10) iteratively narrowed root causes. v6 catastrophic
   regression (0/10) traced + fixed: 5×5 chunk pre-load in
   `handleFixture` dispatcher was blocking server thread → post-block
   natural-tick burst race-cleared `isInFlight` on force-launched
   rockets. Closed ✅ partial with 9/10 PASS in v10.
3. **Flake-diagnosis SOP** authored from the v6-v10 lessons.
   Universal rules: failure distribution as diagnostic axis,
   probe-author safety, cache-bust discipline, when to stop
   iterating.

## Pyramid

237 / 80 / **339** / 41 = **697**. Unchanged across both tasks —
zero new tests, only refactors + probe helpers + budget tuning.

## What shipped (committed + pushed)

### Commits on `feature/tests`

| SHA | Subject |
|---|---|
| `76ef926e` | TASK-27 — flake fix (port-bind retry + shape-#3 kit helper) |
| `64a60bb9` | TASK-28 — residual flake mitigations (chunk-force + tile drive) |
| `9ba95014` | docs: SOP — flake diagnosis (race / regression / test-design) |

### `ForgeTestFramework` (sibling repo, `master`)

| SHA | Subject |
|---|---|
| `948d5fd` | port-bind retry in `RealDedicatedServerHarness` |

### Files touched

**Production**:
- `TileForceFieldProjector.java` — extract `onIntermittentUpdate()`
  for direct test drive (behaviour-preserving).

**Probe / test-only**:
- `TestProbeCommand.java` — chunk-load helpers, per-handler pre-loads,
  `field tick` verb, multiple budget bumps.

**Tests** (12 files):
- `BeaconMultiblockTest`, `CentrifugeRecipeEndToEndTest`,
  `ForceFieldProjectionSmokeTest`, `MachineRecipeEndToEndKit`,
  `MachineRecipeIntegrationTest`, `MissionLifecyclePyramidTest`,
  `ObservatoryMultiblockTest`, `WirelessTransceiverContractTest`,
  `WorldgenDeterminismAndSamplingTest`.

**Docs / SSOT**:
- `.agent/sops/development/flake-diagnosis.md` — NEW SOP, 246 lines.
- `.agent/tasks/TASK-27-flake-fix-port-and-tick-races.md` — partial.
- `.agent/tasks/TASK-28-residual-test-flakes.md` — partial + F8 follow-up.
- `.agent/tasks/README.md` — TASK-27 + TASK-28 in Done partial.
- `CLAUDE.md` + `.agent/DEVELOPMENT-README.md` — gated on
  flake-diagnosis SOP before retry-budget tuning.
- `~/.gradle/gradle.properties` — `forks=5` (user-global, project
  untouched).

**Markers**:
- `2026-05-23_task27-partial-task28-opened.md`
- `2026-05-24_task28-partial-9of10-shipped.md`
- This file (`before-compact-2026-05-25-0000.md`).

**Memory + knowledge graph**:
- `feedback_flake_diagnosis.md` added; MEMORY.md updated.
- 5 graph memories from earlier in session (testing-contracts,
  port-bind TOCTOU, tick-race, bug-ledger, ASM→Mixin) + 3 today
  (flake-distribution, probe-safety, gradle-cache).

## v6-v10 verification summary (TASK-28)

| Sweep | PASS / FAIL | Key shape introduced or removed |
|---|---|---|
| v6 | 0 / 10 | Aggressive 5×5 pre-load → rocket regression (caught + reverted). |
| v7 | 8 / 2 | Per-handler pre-load instead. Observatory + Wireless 1/10. |
| v8 | 7 / 3 | Observatory migrated + Wireless 20×500ms. Beacon + Centrifuge + ForceField. |
| v9 | 7 / 3 | Dispatcher pre-load returned (non-rocket); Worldgen widened. |
| v10 | **9 / 1** | F2 direct-tick + F3 permissive. Only Beacon 1/10 residual. |

## Open follow-ups

- **F8** (Beacon `try-complete attempted:false` 1/10 residual) —
  deferred to a future TASK-29 watching when a 2nd consecutive
  occurrence sharpens the pattern. Not a regression — historical
  shape-#3 with retry-resistant tail.
- **TASK-19..24** — original coverage backlog unchanged.
- **TASK-15** — visual regression, watching (no triggers).

## Bug ledger

Drained 2026-05-23 (TASK-12). No live bugs.

## Resumption tips for next session

1. Run `/nav:start` — `.active` marker will offer restoration.
2. If a 10× testServer sweep flakes again: read
   `.agent/sops/development/flake-diagnosis.md` BEFORE bumping
   anything. Failure distribution is the axis.
3. If a probe edit needs verification: cache-bust per iteration
   (`rm -rf build/{reports,test-results,tmp}/testServer`) AND
   grep per-run PASSED count.
4. `forks=5` is set machine-globally; project default 3 still
   applies if you cd'd into other projects.
5. TASK-29 placeholder lives in the F8 followup section of
   TASK-28; promote it when Beacon recurs.

## Why compacting here

Long session: TASK-27 (port-bind + Phase 2) → TASK-28 (5 reruns
×10 testServer = ~12 hours wall-time, lots of log analysis +
mid-sweep regression catch + Centrifuge test-design pivot) →
SOP authoring → 4 commits pushed. Logical boundary. No
in-progress work.
