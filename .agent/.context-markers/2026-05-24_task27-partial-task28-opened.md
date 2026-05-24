# Context marker — 2026-05-24 TASK-27 partial close + TASK-28 opened

**Slug**: 2026-05-24_task27-partial-task28-opened
**Branch**: `feature/tests`
**Session focus**: TASK-27 — flake fixes for shapes #1 + #2 + a broader
shape-#3 sweep that surfaced during 10× verification. Closed as
✅ partial; residual flakes split into new TASK-28.

## Pyramid

237 / 80 / **339** / 41 = **697** (unchanged — no new tests, just
existing-test refactors + probe budget bumps + helper additions).

## What shipped (TASK-27)

### Phase 1 — port-bind retry in `RealDedicatedServerHarness`
(`ForgeTestFramework` sibling repo, composite-build wired via
`-PuseLocalFramework=true`)

- `startInternal()` rewritten as 3-attempt loop. On `BindException`
  in child JVM transcript: kill child, allocate new port, retry.
- New `awaitReadyOrBindFailure(process, transcript, timeout)` polls
  for either the ready marker or the failure marker.
- `bootstrapServerFiles` split into `writeEula` (once on first
  attempt) + per-attempt `server.properties` write.
- **Retry path never observably triggered** across 60+ runs — defensive
  net for harsher CI / modpack scenarios.

### Phase 2 — tick-timing + shape-#3 fixes (AR test code)

- `MachineRecipeEndToEndKit.tryCompleteWithRetry(c, dim, cx, cy, cz)`
  new helper. 8 × 500 ms retry on `attempted:false`. Returns last
  response; callers assert their own `isComplete` expectation.
- `MachineRecipeEndToEndKit.assertFixtureValidates` budget bumped
  5×200 ms → 8×500 ms.
- `BeaconMultiblockTest` migrated to `tryCompleteWithRetry` (5 call
  sites across 3 tests).
- `MachineRecipeIntegrationTest.cuttingMachineRunsFirstRegisteredRecipe`
  — `try-complete` migrated to helper; tick polling budget 300 →
  1200 (12 × force-tick 100, polled per batch).
- `MissionLifecyclePyramidTest.completionPrunesMissionFromSatelliteRegistry`
  — drives prune deterministically via 30 × `force-tick-dim 0`
  instead of waiting on natural ticks.
- `WirelessTransceiverContractTest.placeAt` — added 5 × 200 ms
  wait-for-tile poll using `wireless-info` `"ok":true` sentinel.
- `TestProbeCommand.handleField` (`/artest field info`) budget bumped
  60 × 50 ms → 120 × 50 ms (3 s → 6 s).

### Phase 3 — 10× verification (5 sweeps)

| Sweep | PASS/FAIL | Notes |
|---|---|---|
| v1 | 10/0 | Bogus — Gradle UP-TO-DATE cached runs 2-10. |
| v2 | 1/2 killed | Cache-bust applied. Surfaced shape #3 across multiple multiblocks. |
| v3 | 0/6 killed | My wait-for-tile sentinel was wrong (`contains("TileWirelessTransciever")` never matched). Fixed in v4. |
| v4 | 6/4 | Beacon + cuttingMachine green; PrecisionLaserEtcher / ArcFurnace shape-#3 still flaked at 5×200 ms budget. |
| v5 | 4/6 | Beacon + ArcFurnace green at 8×500 ms; PrecisionLaserEtcher resists even 4 s budget. New shapes surfaced (Centrifuge recipe-order, SolarPanel, MixinHook). |

## Why TASK-27 closed as ✅ partial, not full ✅

Acceptance "10 consecutive PASS" not achieved. Budget tuning hit
diminishing returns:

- PrecisionLaserEtcher `try-complete` still `attempted:false` across
  8 × 500 ms (4 s window) — needs chunk-force pre-load, not longer
  wait.
- ForceField `extensionRange=0` after 6 s — needs direct tile drive,
  not budget bump.
- Centrifuge — recipe-order non-determinism (real test design bug).
- MixinHook fGravityMixin — fall-clearance test design issue.
- SolarPanel — new shape, single sighting.

These flake shapes are **outside the original TASK-27 scope** (which
was framed around port contention + tick race + shape #3 narrow). They
need different strategies than retries — chunk-load forcing, recipe
pinning, fixture redesign. Split into new TASK-28.

## TASK-28 — opened 2026-05-24

7 residual flake shapes documented as F1-F7 with proposed fix shapes.
Total est ~7 h. Will deliver the actual "10× green" acceptance from
TASK-27 once F1-F7 are mitigated.

## Files touched this session

### Production / probe
- `src/main/java/zmaster587/advancedRocketry/command/test/TestProbeCommand.java` —
  `field info` probe budget 1.5 s → 6 s (60 → 120 iterations).

### Framework (sibling repo `../ForgeTestFramework`)
- `src/main/java/com/github/stannismod/forge/testing/server/RealDedicatedServerHarness.java` —
  3-attempt port-bind retry loop + `awaitReadyOrBindFailure` +
  `destroyAndJoin` + `writeEula` split. ~100 LOC net add.

### Test code
- `src/test/java/.../server/MachineRecipeEndToEndKit.java` —
  added `tryCompleteWithRetry` (~25 LOC); bumped retry budget.
- `src/test/java/.../server/BeaconMultiblockTest.java` — 5 call sites
  to kit helper.
- `src/test/java/.../server/MachineRecipeIntegrationTest.java` —
  cuttingMachine `try-complete` + polling refactor.
- `src/test/java/.../server/MissionLifecyclePyramidTest.java` —
  prune polling via force-tick-dim.
- `src/test/java/.../server/WirelessTransceiverContractTest.java` —
  `placeAt` wait-for-tile poll.

### Docs / SSOT
- `.agent/tasks/TASK-27-flake-fix-port-and-tick-races.md` — status
  → ✅ partial; `## Actual scope` written; Followups → TASK-28.
- `.agent/tasks/TASK-28-residual-test-flakes.md` — NEW.
- `.agent/tasks/README.md` — TASK-27 → Done (partial), TASK-28 added
  to Backlog, TASK-16 entry updated re shape #4 promotion.

## Bugs found in production

None. All issues live in test code / probe / harness.

## Bug ledger state

Unchanged — drained per TASK-12 close-out 2026-05-23.

## Next up

- TASK-28 when ready to take on the deeper fixes (F1 = chunk-force
  helper is the biggest single lever; mitigates F1 + F6 + F7).
- TASK-19..24 backlog continues unchanged.
