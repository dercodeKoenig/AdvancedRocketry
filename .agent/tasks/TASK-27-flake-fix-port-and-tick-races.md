# TASK-27: Flake fix — port-bind retry + tick-timing-race per-test polling

## Ticket

- Source: TASK-16 investigation 2026-05-23 — three flake shapes
  identified; shape #3 (post-fixture validate race) mitigated in
  TASK-26, but #1 (port contention) and #2 (tick-timing race) still
  open.
- Status: ✅ **Completed partial 2026-05-24**.
- Created: 2026-05-23.

## Context

TASK-16 root-caused two distinct flake shapes that survive its
in-task mitigations and need their own targeted fix work:

1. **Port contention** in `RealDedicatedServerHarness.reservePort()`
   — TOCTOU between parent JVM's `ServerSocket(0)` close and the
   child server JVM's bind. Observed in `BeaconMultiblockTest`,
   `WarpControllerDepthTest`. Lives in ForgeTestFramework, not
   AdvancedRocketry.
2. **Tick-timing race** in tests that assert on
   "eventually-true" state right after the trigger. Observed in
   `MachineRecipeIntegrationTest.cuttingMachineRunsFirstRegisteredRecipe`
   and `MissionLifecyclePyramidTest.completionPrunesMissionFromSatelliteRegistry`.
   Lives in AR test code.

See [TASK-16](./TASK-16-test-stability-flake-watch.md#investigation-findings-2026-05-23)
for the full root-cause writeup.

## Implementation plan

| Phase | Effort | Result |
|---|---|---|
| 1 | ~2 h | Port-bind retry in `RealDedicatedServerHarness.start()`. Watches the spawned child's stdout for `BindException`. On detect, kill child, allocate a new port, retry up to 3 times. Composite-build via `-PuseLocalFramework=true`. |
| 2 | ~1 h | Convert `MachineRecipeIntegrationTest.cuttingMachineRunsFirstRegisteredRecipe` + `MissionLifecyclePyramidTest.completionPrunesMissionFromSatelliteRegistry` to use `tick-until` polling instead of `force-tick N` + immediate-read. |
| 3 | ~30 min | Re-run testServer 10× to confirm flake-free across runs. |
| 4 | ~30 min | Close-out: pyramid counter (unchanged — no new tests), README sync, marker, commit. |
| **Total** | **~4 h** | |

## Acceptance

- [ ] `RealDedicatedServerHarness.start()` survives a port collision
      and retries with a new port up to 3 times.
- [ ] 10 consecutive `./gradlew testServer` runs all pass with
      `-Pforks=3` (current default).
- [ ] `MachineRecipeIntegrationTest` and `MissionLifecyclePyramidTest`
      no longer race on first-call assertions.
- [ ] No regressions in the rest of the suite.

## Out of scope

- The deep root-cause of shape #3 (post-fixture validate race) —
  TASK-26's kit-side retry shim is sufficient until a clean
  reproduction is available. Reopen only if the kit-side mitigation
  stops working.
- Changing the `-Pforks=N` default — that's `gradle.properties`,
  protected per `CLAUDE.md`.

## Dependencies

- Phase 1 touches `ForgeTestFramework` (sibling checkout). The
  build wires it in via composite-build when
  `-PuseLocalFramework=true` is set, so local development can
  test the change before publishing. After acceptance, the
  framework needs a tag + `publishToMavenLocal` so CI / other
  developers pick it up.

## Estimated effort

~4 h single session.

## Actual scope (2026-05-24)

Shipped — defensive flake-mitigation infrastructure for shapes #1
(port contention) + #2 (tick-timing race) + a broader pass at shape
#3 (post-fixture validate race) that surfaced during verification.

**Phase 1 — port-bind retry (`ForgeTestFramework`)**

`RealDedicatedServerHarness.startInternal()` rewritten as a 3-attempt loop
around `reservePort() + writeServerProperties + launchServer + awaitReady`.
New `awaitReadyOrBindFailure` method polls the child JVM's transcript for
either the ready marker (`For help, type "help" or "?"`) or the failure
marker (`BindException`). On bind failure the child is destroyForcibly'd,
the reader thread joined, and a fresh port is reserved for the next
attempt. Three failures bubble out as an `IOException` listing the last
collision. The `bootstrapServerFiles` helper was split into `writeEula`
(once on first attempt) + the in-loop `server.properties` write so the
port is always fresh. **The retry path was never observably triggered**
across 60+ testServer runs — defensive safety net for harsher modpacks
/ slower CI hardware.

**Phase 2 — tick-until polling + try-complete retry (AR test code)**

Originally just two tests; verification surfaced a broader shape-#3
pattern that drove additional work:

- `MissionLifecyclePyramidTest.completionPrunesMissionFromSatelliteRegistry`
  — was a single follow-up `mission state` call relying on the natural
  overworld tick to fire the satellite-registry prune. Now drives the
  prune deterministically via 30 iterations of
  `artest satellite force-tick-dim 0`, polling `mission state` after
  each tick for the `mission not found` response.
- `MachineRecipeIntegrationTest.cuttingMachineRunsFirstRegisteredRecipe`
  — was `force-tick 300` + immediate-read. Now 12 batches of
  `force-tick 100` interleaved with hatch reads (tick budget 300 → 1200
  to absorb parallel-3-fork pressure that stretches effective tick
  rate). Plus migrated `try-complete` to the new kit helper below.
- `MachineRecipeEndToEndKit.tryCompleteWithRetry` added — generalised
  shape-#3 retry shim that callers (Beacon + cuttingMachine) use
  instead of raw `machine try-complete`. Budget 8 × 500 ms (4 s
  ceiling; ~0 ms cost on the happy path).
- `MachineRecipeEndToEndKit.assertFixtureValidates` budget bumped
  5 × 200 ms → 8 × 500 ms after PrecisionLaserEtcher / ArcFurnace
  flakes resisted the smaller budget in the v3/v4 reruns.
- `BeaconMultiblockTest` migrated to the kit helper (5 `try-complete`
  call sites across 3 tests).
- `WirelessTransceiverContractTest.placeAt` got a 5×200 ms
  wait-for-tile poll using the `wireless-info` `"ok":true` sentinel —
  block-place → tile-init race surfaced under load.
- `TestProbeCommand.handleField` (`/artest field info`) budget bumped
  60×50 ms → 120×50 ms (3 s → 6 s) for the projector extension-tick
  gate.

**Phase 3 — 10× testServer verification (across five reruns)**

The 10× metric was attempted in five sweeps as the picture sharpened:

| Sweep | PASS / FAIL | Notes |
|---|---|---|
| v1 | 10/0 | False positive — only run 1 actually executed; runs 2-10 caught `:testServer UP-TO-DATE`. |
| v2 | 1/2 (killed) | Cache-bust applied. Surfaced cuttingMachine + Beacon shape-#3, ForceField + Wireless + WorldgenDeterminism shapes. |
| v3 | 0/6 (killed) | My new wait-for-tile check was buggy (`contains("TileWirelessTransciever")` never matched). Fixed. |
| v4 | 6/4 | Beacon + cuttingMachine green; PrecisionLaserEtcher/ArcFurnace shape-#3 still flaked at 5×200 ms budget. |
| v5 | 4/6 | Beacon + ArcFurnace green at 8×500 ms; PrecisionLaserEtcher resists even 4 s budget; new flake shapes surfaced (Centrifuge recipe-order, SolarPanel, MixinHook fGravity, Wireless secondary). |

**Acceptance partial:** [✅] Phase 1 retry implemented and proven not to
regress anything across 60+ runs. [✅] Tick-timing-race tests no longer
race on their original assertion. [❌] 10 consecutive PASS not achieved
— see TASK-28 for the residual flakes that need deeper fixes than
budget tuning can deliver.

**No production code touched** (the probe budget bumps are probe-only,
not gameplay). Pyramid counter unchanged (237 / 80 / 339 / 41 = 697).
Bug ledger unchanged.

## Followups → TASK-28

The 10× verification surfaced flakes outside the original TASK-27
scope that defy further budget tuning. They are split out into
[TASK-28](./TASK-28-residual-test-flakes.md):

- `PrecisionLaserEtcherRecipeEndToEndTest.precisionLaserEtcherFixtureValidates`
  — `attempted:false` survives 8 × 500 ms (4 s); needs a chunk-load
  forcing strategy in the fixture probe, not a longer wait.
- `ForceFieldProjectionSmokeTest.poweredProjectorProjectsAndUnpoweredCollapses`
  — `extensionRange=0` survives 6 s under parallel load; needs a
  different driving mechanism (forceful tick of the projector tile
  on the server thread, bypassing the natural % 5 gate).
- `CentrifugeRecipeEndToEndTest.centrifugeRunsFirstRegisteredRecipe`
  — recipe-order non-determinism (production picks a recipe at
  runtime that differs from probe's `recipe-info 0`). Test design
  issue, not a race — needs name-pinned recipe selection.
- `MixinHookBehaviourPinsTest.fGravityMixinAffectsFallingBlockInOverworld`
  — falling block dies in 1 tick under load (probe response shows
  `isDead:true, motionY:0.0`); test design issue with fall-clearance
  too tight relative to mixin-accelerated gravity.
- `MachineDomainSmokeSuite.solarPanelAccumulatesEnergyOverTicks`
  — new shape spotted in v5 run 9, single sighting; needs a second
  occurrence to characterise.
- `WirelessTransceiverContractTest.pairingBothUnpairedAssignsFreshSharedIdRegisteredOnNetwork`
  — Wireless secondary; `tile:null` persists past 1 s wait-for-tile
  budget; needs larger budget or chunk-load force.
- `WorldgenDeterminismAndSamplingTest.differentChunksReturnIndependentlyAddressableData`
  — TASK-16 shape #4 (worldgen sampling race) — now observed 3×
  total (once in TASK-16 close-out, once each in v3/v5). Pattern
  confirmed; moved out of TASK-16 watching status into TASK-28.
