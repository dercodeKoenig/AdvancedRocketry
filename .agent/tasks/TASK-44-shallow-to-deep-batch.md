# TASK-44: Convert all shallow subsystems to deep (one batch)

**Branch**: `feature/tests`
**Opened**: 2026-05-31
**Driver**: user directive after the TASK-41/42/43 mixin verification —
"turn all shallow into deep in one batch".
**Parent audits**:
[`2026-05-27-full-coverage-audit.md`](../audits/2026-05-27-full-coverage-audit.md) §3 (gap litmus),
[`2026-05-29-coverage-delta.md`](../audits/2026-05-29-coverage-delta.md) (subsystem matrix),
[`2026-05-31-mixin-coverage-nuance.md`](../audits/2026-05-31-mixin-coverage-nuance.md) (gaps T, U).

**Governing SOP**: `.agent/sops/development/testing-principles.md` —
pin CONTRACTS, never impl details (magic numbers, loop bounds,
internal fields). Litmus per test: "this test fails if production
breaks the contract that ___".

---

## Phase 0 outcome (2026-05-31) — scope pruned

Six collapse-risk gaps read before authoring. Result:

| Gap | Verdict | Action |
|---|---|---|
| H — TileSatelliteHatch | impl-only (read-only item→satellite projection; covered by ItemPackedStructureNbtRoundTrip + SatelliteProperties) | **DROP** |
| K — ItemBasicLaserGun | unwired (registered creative-tab, NO recipe → unreachable in survival) | **DROP** |
| M — BlockIntake/IIntake | `getIntakeAmt()` hardcoded 10; pinning ==10 = magic-number anti-pattern; launch-eligibility already at launch tests | **DROP/defer** |
| F.1 — TileCO2Scrubber | no independent atmosphere effect (scrubbing lives in TileOxygenVent); only a cartridge-holder + comparator override | **Reframe**: pin OxygenVent↔scrubber cartridge-consumption interaction (the real observable), not the holder |
| J — ItemUpgrade | slot-eligibility already pinned; `onTick` sprint walkSpeed boost player-visible + unpinned | **KEEP** (testClient) |
| F.3 — TileAtmosphereDetector | `update()` emits redstone power when adjacent atmosphere matches selected | **KEEP** (server) |

Pruned ~12 h of false work.

---

## RECONCILIATION (2026-05-31) — the 2026-05-29 delta audit was stale

The delta audit was written the morning of 2026-05-29 against HEAD
`c3cf8cc7` and listed gaps A–N as open. But TASK-40a–e batches
(`18ab6106`, `1cfc968e`, `f66d6da8`, `7b423a12`, …) landed LATER the
same day and closed most of them. Ground truth re-derived from the
actual test tree + TASK-40 close-outs:

**Already shipped (do NOT redo):** A (→`RailgunCargoReceiveContractTest`),
D (→`PlanetAnalyserResearchContractTest`), E-loader, F.1
(`CO2ScrubberComparatorOutputTest`), F.2 (`GasChargePadFillsPressureTankE2ETest`),
J (`ItemUpgradeSlotEligibilityTest`), L (`ForceFieldProjectorProjectsAndRetractsTest`).

**Stay DROPPED — SOP forbids a test (NOT real gaps):**
- G GuidanceComputer — no chip→comparator contract exists (audit framing wrong; GUI already pinned).
- H Hatches — impl-only.
- I HolographicPlanetSelector — no chip slot; GUI-display already covered (audit framing wrong).
- M BlockIntake — impl-only constant.
- K ItemBasicLaserGun — **unwired (no recipe)**; unreachable in survival.

These 5 are shallow *by design*. Forcing tests would be magic-number /
impl-detail pins — the exact anti-patterns CLAUDE.md forbids. User
confirmed 2026-05-31: leave dropped with rationale.

## Actual actionable set — 7 contracts (user-approved 2026-05-31)

| Gap | Contract (litmus) | Test | Layer |
|---|---|---|---|
| **F.4** | Powered Pump adjacent to Forge-fluid source fills internal tank >0 mB/tick | un-`@Ignore` `TilePumpFillsFromAdjacentWaterSourceTest` | server | ✅ DONE 2026-05-31 — was misdiagnosed (pump needs IFluidBlock, vanilla water isn't one). Ledger #7 added. |
| ~~**T**~~ | ~~MixinWorldServerMulti~~ | — | — | ❌ DROPPED — impl-only. Weather isolation already pinned by `WeatherClientSyncE2ETest`; mixin-vs-fallback attribution is impl-detail. See mixin-coverage audit. |
| ~~**F.3**~~ | ~~AtmosphereDetector emits redstone~~ | — | — | ❌ ALREADY COVERED — `AtmosphereOxygenSmokeTest` (lines 49-110) pins both states: mode=AIR on overworld → POWERED; re-target vacuum → detected=false → not powered. Dedicated test = duplicate. The `detector-set-mode/force-sample/output` probes exist for it. |
| **B** | MINING-mode drill removes target column block + yields its drop | `OrbitalLaserDrillModeDispatchTest` (new) + `infra laserdrill-mine` probe | server | ✅ DONE 2026-05-31. Audit's "EntityItemAbducted" framing was off (spawns EntityLaserNode visual; observable = block-removal + drop). Terraforming-mode sister-pin **deferred-as-duplicate-risk**: `terraformingdrill` delegates to `BiomeHandler.terraform` (already covered by TASK-36 terraformer tests) and needs a heavy ChunkManagerPlanet planet-dim fixture. |
| **N** | Asteroid worldprovider dim generates fill-block asteroids (not void) | `AsteroidDimensionContainsAsteroidsTest` (new) + `worldgen create-asteroid-dim` probe | server | ✅ DONE 2026-05-31. Probe clones an existing planet's DimensionProperties → new id + genType=ASTEROID + explicit Forge registerDimension(AsteroidDimensionType) (registerDim's internal guard skipped it). Load + ore-stats stone > 0 (band-pin). 2/2 reruns green. |
| **C** | AreaGravityController resets fallDistance of IN-radius entities only | NEW server `AreaGravityControllerFallDistanceResetTest` (deleted @Ignore'd client one) + probes `entity set-fall-distance`/`set-no-gravity`/fallDistance-in-info | server | ✅ DONE 2026-05-31. Re-designed: two no-gravity armor stands (in/out radius) + machine-enable; in→0, out→7.5. Moved client→server (contract is server-side). **Finding**: controller isn't enabled by default; old grounded-bot test was non-discriminating (vanilla reset masked whether controller ran at all). |
| **U** | Inv-bypass mixin keeps container open across distance | un-`@Ignore` `InventoryBypassRedirectE2ETest` + new `player open-chest` probe | testClient | ✅ DONE 2026-05-31. Replaced flaky `bot.rightClickBlock` with server-side `displayGUIChest` (direct TileEntity IInventory, bypassing vanilla isBlocked which flaked on chunk-populate terrain above the chest). 4/4 reruns green. Ledger #6 InventoryBypass line resolved. |

Order: ~~F.4~~✅, ~~T~~❌drop, ~~F.3~~❌covered, then B, N (server), C, U (client).
Net actionable after reconciliation: **4** (B, N, C, U). The "shallow"
backlog was mostly already deep — the 2026-05-29 audit was stale and
under-credited the TASK-40 sweep.

---

## Rules for this batch
- Each test reuses existing probe verbs where possible; new `/artest`
  verbs are test-only (gated by `advancedrocketry.tests`).
- NO production logic changes (this is a coverage batch). Bugs found →
  ledger per CLAUDE.md, do not fix in scope.
- Band-pins / end-state pins, never magic-number pins.
- Run affected layer green before moving to next gap.

## Status — ✅ COMPLETE (2026-05-31)

Final tally of the original "all shallow → deep" directive:
- **Shipped this batch (4 real contracts)**: F.4 (pump fluid drain),
  B (laser-drill mining dispatch), C (area-gravity fallDistance reset),
  N (asteroid worldgen). All green + reruns clean.
- **Already covered (no work needed)**: A, D, E, F.1, F.2, J, L (TASK-40
  sweep), F.3 (AtmosphereOxygenSmokeTest).
- **Correctly dropped per SOP** (impl-only / unwired / wrong-framing):
  G, H, I, K, M, and T (impl-only — weather isolation already pinned).
- **Bug ledger**: #7 added (pump can't drain vanilla water). #6
  InventoryBypass line resolved (U un-ignored).

New probe verbs added (all test-only, gated by `advancedrocketry.tests`):
`infra laserdrill-mine`, `entity set-fall-distance`, `entity set-no-gravity`
(+ fallDistance in `entity info`), `player open-chest`,
`worldgen create-asteroid-dim`.

Net new behavioural tests: 4 (+1 deleted superseded @Ignore'd client test).
Pyramid net: +3 server, +0 client (U was already counted as @Ignore'd;
C moved client→server).

Meta-lesson: the 2026-05-29 delta audit was stale (written before the
same-day TASK-40 sweep), inflating "17 gaps / 8 shallow subsystems" to a
phantom. Ground-truth reconciliation against the test tree + TASK-40
close-outs reduced it to 4 real contracts. Always reconcile a frozen
audit against current code before planning from it.
