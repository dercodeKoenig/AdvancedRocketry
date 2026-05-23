# TASK-27: Flake fix — port-bind retry + tick-timing-race per-test polling

## Ticket

- Source: TASK-16 investigation 2026-05-23 — three flake shapes
  identified; shape #3 (post-fixture validate race) mitigated in
  TASK-26, but #1 (port contention) and #2 (tick-timing race) still
  open.
- Status: 🟢 **Backlog**.
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
