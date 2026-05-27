# TASK-16: Test-stability flake watch — parallel-fork contention

## Ticket

- Source: TASK-12 close-out marker 2026-05-23 ("flag these two for a
  future test-stability ticket if pattern recurs"), promoted into a
  tracked task on 2026-05-23 during the SSOT cleanup.
- Status: 🟡 **Investigation complete 2026-05-23**. Shape #3 mitigated
  in-task via a test-side kit-retry; shapes #1 + #2 root-caused with
  fix-shapes documented but deferred to follow-up TASK-27 (port
  contention requires ForgeTestFramework changes; tick-timing
  mitigations are per-test). See "Investigation findings" below.
- Created: 2026-05-23.

## Context

The first full `./gradlew testServer` run after TASK-12 had **2
test failures** that passed in isolation AND on the immediate rerun
with no source changes between:

1. `BeaconMultiblockTest.beaconMultiblockValidatesWhenFixtureIsBuilt`
2. `MachineRecipeIntegrationTest.cuttingMachineRunsFirstRegisteredRecipe`

Both diagnosed as **parallel-forks resource contention** — the test
runner's worker forks tripped over each other on shared world /
fixture state. The flakes pre-date TASK-12 and were not introduced
by its production fixes (verified — the two failures pass when run
serially or one-at-a-time).

## Status: watching, not actively fixing

Rationale:

- One occurrence is not a pattern. A flake that fired exactly once
  during a single full-pyramid run is below the threshold to
  invest in.
- Both tests are real contract pins (TASK-04 multiblock + TASK-02
  machine-recipe shape). They are NOT spurious assertions —
  disabling them would actually lose coverage.
- The parallel-fork count is a global gradle setting; tuning it
  trades wall-time for stability across the **entire** test suite,
  not just these two.

**Promotion trigger**: bump out of "watching" if the flake reoccurs
on a clean run within the next ~5 testServer runs, OR if any third
test joins the flake list.

## Investigation sketch (when promoted)

### Step 1 — Identify the shared resource

Both flakes are server-tier. Candidates for the contention:

- Shared `World` / `WorldServer` instance across forks (unlikely
  — gradle forks are JVM-isolated).
- Shared file-system fixture (`/run-server` working dir,
  `.agent/visual-baselines/` if TASK-15 lands).
- Shared port binding (the testServer harness binds a debug port;
  if forked workers reuse the same number they'll collide).
- Shared registry state in static init that depends on the order
  forks reach `serverStart`.

Run with `--max-workers=1` to confirm contention is the cause (if
flakes disappear in serial, it's confirmed). Then re-introduce
parallelism with explicit per-test exclusion / serial grouping.

### Step 2 — Fix shape (one of)

- Per-fork file-system sandbox (assign `run-server/<fork-id>/` per
  worker).
- Mark these two specific tests with a serialisation hint that
  gradle honours.
- Convert one or both to testUnit if the world dependency is
  shallow.

### Step 3 — Pin the contract

The investigation must NOT delete the offending assertions. They
pin real contracts. Per
[`testing-principles`](../sops/development/testing-principles.md),
"flaky tests get fixed, they do not get deleted".

## Recurrence log

| Date | Test | Trigger | Run number that day | Resolution |
|---|---|---|---|---|
| 2026-05-23 | both (BeaconMultiblock + MachineRecipeIntegration) | `./gradlew testServer` post-TASK-12 | run 1 of 1 | passed on rerun, no investigation yet |
| 2026-05-23 | `WarpControllerDepthTest` (classMethod) | TASK-18 close-out testServer run | run 2 | passed in isolation; **port contention**: `BindException: Address already in use` — classic parallel-fork harness collision. Directly matches this task's diagnostic hypothesis. |
| 2026-05-23 | `MissionLifecyclePyramidTest.completionPrunesMissionFromSatelliteRegistry` | TASK-18 close-out testServer run | run 2 | passed in isolation; **timing race**: mission reached `progress=1.0` + `isDead=true` but had not been pruned from registry yet. Within-fork ordering, distinct from port contention. |
| 2026-05-23 | `ArcFurnaceRecipeEndToEndTest.arcFurnaceFixtureValidates` AND `RollingMachineRecipeEndToEndTest.rollingMachineFixtureValidates` | TASK-26 close-out: 9-class RecipeEndToEnd group run together | run 3 | both passed in isolation; **`attempted:false` from `attemptCompleteStructure`** — fixture builder placed all blocks (placed=90 / placed=23, unresolved=0), then immediate try-complete returned attempted=false. Suggests chunk-load / world-state visibility race when fixture builder + try-complete fire back-to-back under shared-harness pressure. New flake **shape #3** — distinct from port contention (shape #1) and tick-timing (shape #2). |
| 2026-05-23 | `RollingMachineRecipeEndToEndTest.rollingMachineFixtureValidates` (again) | TASK-16 close-out: full testServer pyramid | run 4 | failed after 3×75ms kit-retry budget — retry was undersized for full-pyramid pressure. Retry budget bumped to 5×200ms in same run, re-run **passed** in the 10-class RecipeEndToEnd group AND in the full pyramid. Confirms shape #3 is the same race + the budget needs to scale with class-count pressure. |
| 2026-05-23 | `WorldgenDeterminismAndSamplingTest.<some method>` | TASK-16 close-out: full testServer pyramid | run 5 | passed in isolation. Failure message: "three spaced chunks reported identical (topY,biome) — probe likely caching" with topY=72/72/72 + biome=moondark/moondark/moondark. **Shape #4 — within-chunk caching / sampling race** (probe seems to return the same chunk for three different requests when under full-pyramid pressure). Distinct from #1-3 — touches worldgen sampling, not multiblock validation. First sighting; promotion trigger NOT fired yet (need a 2nd occurrence to confirm pattern). |
| 2026-05-26 | `WarpControllerDepthTest.warpTriggerWithFuelAndWarpCoreMovesStationToTransit` | TASK-30+34 close-out: full testServer pyramid (run after TASK-30+34 +5 tests landed) | run 1 | passed in isolation + on 2nd full-suite rerun. Failure message: `warp-trigger-debug` returned `{"error":"tile not TileWarpController"}` — the warp monitor block placed earlier in the SAME test had vanished by the time the next probe queried it (~3 probe-commands later). **New flake shape #5 — placed-tile disappearance in spaceDim under shared-harness pressure**. Distinct from shape #1 (port contention) — server is up. Likely a spaceDim chunk-unload race: the test does `dim load -2` but no `chunk forceload`, and the new TASK-30 StationControllersTickContractTest also exercises spaceDim, increasing chunk-load churn during this test class's run. Need a 2nd occurrence to confirm pattern + decide mitigation (probably `chunk forceload` in `placeAndReadWarpState`). |
| 2026-05-26 | `InventoryBypassRedirectE2ETest.mixinRedirectKeepsContainerOpenAcrossDistance` | TASK-36b ext + multi-client moderator-fetch close-out: full testClient pyramid | run 1 | reproduced on isolated rerun (with AND without my framework changes — confirms pre-existing). Failure message: `chest GUI must open on right-click expected:<GuiChest> but was:<>`. **Flake shape #6 — right-click-doesn't-open-GUI under client-harness GL/CPU contention**. Matches the testClient javadoc warning: "Running several of those concurrently makes the right-click → openGui → displayGuiScreen round-trip unreliable (the GUI silently fails to open under GL/CPU contention)". Pre-existing — independent of my changes (verified by reverting framework + removing my testClient test and reproducing the failure). `clientForks=1` is already set; the contention may instead come from running ~60 testClient tests sequentially against an aging GL context that doesn't reset cleanly between scenarios. First sighting in this class — need a 2nd occurrence + investigation to decide between (a) per-test GL teardown probe, (b) retry on empty-screen result, (c) production-side investigation if the right-click handler is actually dropping events. |

**Promotion trigger fired**: a third (and fourth) test joined the
flake list, both during the TASK-18 close-out. Two distinct
flake shapes are now visible — port contention (Beacon, Warp) and
a tick-timing race (MachineRecipeIntegration, MissionLifecycle).
TASK-26 close-out surfaced a third shape — `attempted:false` from
`attemptCompleteStructure` immediately after fixture build,
suggesting chunk-load / world-state visibility ordering. Three
distinct flake shapes are now tracked; investigation should treat
them as related-but-separable.

When opening an implementation phase, add subsequent occurrences
here.

## Dependencies

- Does NOT block any other task.
- Touches the gradle build configuration, which is on the
  protected list (`gradle.properties` cannot be changed without
  explicit ask — `CLAUDE.md` rule).

## Estimated effort

- Investigation: ~2 h. ✅ Done 2026-05-23.
- Fix (if it's the file-system sandbox approach): ~1-2 h.
- Re-run + confirm flake-free across 10 testServer runs: ~30 min.

## Investigation findings (2026-05-23)

### Shape #1 — port contention (root cause identified)

`com.github.stannismod.forge.testing.server.RealDedicatedServerHarness.reservePort()`
is a classic TOCTOU race:

```java
private static int reservePort() throws IOException {
    try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
        socket.setReuseAddress(true);
        return socket.getLocalPort();
    }
}
```

The parent JVM binds to port 0 (OS picks a free port), reads
`getLocalPort()`, then **closes the socket** (try-with-resources).
The dedicated-server child JVM is then launched and binds the port
itself. Between the parent's close and the child's bind (~100 ms
of `launchServer` + JVM warm-up + Forge boot) any other parallel
fork running `reservePort()` can win the same port — both child
JVMs then race to bind, the loser throws `BindException`.

`setReuseAddress(true)` does NOT help here — `SO_REUSEADDR` lets a
TIME_WAIT socket be reused; it does not prevent two distinct
LISTEN attempts from colliding when the port is free at the
intermediate moment.

**Fix-shape options for follow-up TASK-27**:

1. **Retry-with-fresh-port in `RealDedicatedServerHarness.start()`** —
   watch the child's stdout for `BindException` (Netty logs it as
   `io.netty.channel.unix.Errors$NativeIoException: bind(..) failed`
   or `java.net.BindException: Address already in use`). On detect,
   kill the child, allocate a new port, retry up to 3 times. Cheap
   to implement; preserves the existing API.
2. **Keep-socket-open and pass-via-env** — keep the parent's
   `ServerSocket` open and pass the port via env var; close the
   parent socket only after the child's bind succeeds (detected via
   stdout). More robust but adds Process-lifecycle complexity.
3. **OS-managed port range allocation** — pre-allocate a port-range
   per gradle fork (`-Pforks=N` → fork i uses ports
   `25000 + i*1000..25999 + i*1000`). Eliminates inter-fork
   collision but adds a build-config dependency.

Option (1) is the lowest-risk fix and is recommended for TASK-27.

### Shape #2 — tick-timing race (per-test mitigation)

`MachineRecipeIntegrationTest.cuttingMachineRunsFirstRegisteredRecipe`
and `MissionLifecyclePyramidTest.completionPrunesMissionFromSatelliteRegistry`
both assert on state that "eventually becomes true" but is read
once, synchronously, right after the triggering action. When a
single-tick delay pushes the state-update past the read, the
assertion fires before the registry has been updated.

**Fix-shape**: convert affected tests to use the existing
`/artest machine tick-until <condition>` polling probe (or a kit
helper that wraps the same pattern) instead of `force-tick N`
followed by an immediate read. The polling probe already exists in
`TestProbeCommand.java:3353+` and is what other tests use for the
same pattern.

This is per-test, not framework-wide — each affected test gets a
small targeted edit. Deferred to TASK-27.

### Shape #3 — post-fixture-validate race (mitigated in TASK-26)

Two failures observed during the TASK-26 close-out (ArcFurnace +
RollingMachine fixture-validates): `attemptCompleteStructure`
returns `attempted:false` on the first call immediately after the
fixture builder finishes. Both passed in isolation and on retry —
strongly suggesting a race between the fixture's `setBlockState`
chain finalizing and the validator's per-cell `getBlockState` reads.

**Mitigation shipped in TASK-26 (later bumped under TASK-16)**:
`MachineRecipeEndToEndKit.assertFixtureValidates` retries the
`try-complete` probe up to **5 times with a 200 ms gap** (started
at 3×75 ms in TASK-26; bumped in the same session after a full-
pyramid run still failed `RollingMachineRecipeEndToEndTest` —
the race window widens under full-suite JVM pressure). The
non-flaky path remains a single call (0 ms added). Verified: the
full testServer pyramid passes 0 / 22 RecipeEndToEnd failures
after the bump, where the same pyramid had 1-2 RecipeEndToEnd
failures with the 3×75 ms budget.

Hypothesis for the root race (unconfirmed): vanilla 1.12.2
`World.setBlockState` calls `markAndNotifyBlock` which fires
`Block.neighborChanged` reentrantly. For a multi-block fixture
where each setBlockState triggers neighborChanged on the previous
cells, there's an ordering where the validator can observe an
in-progress state. The retry is a pragmatic shim; the deep fix
would be to flush the change-set before calling try-complete.

## Outcome

| Shape | Status |
|---|---|
| #1 port contention | Root-caused. Fix recommended for TASK-27. |
| #2 tick-timing | Root-caused. Per-test fix shape documented; deferred to TASK-27. |
| #3 post-fixture | Mitigated test-side via kit-retry. Deep fix deferred to TASK-27. |

Follow-up: open TASK-27 for the actual fix work. This task closes
with the investigation deliverable as planned.
