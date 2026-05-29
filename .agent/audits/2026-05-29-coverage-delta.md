# Coverage delta — 2026-05-29

**Branch**: `feature/tests`
**Parent audit**: [`2026-05-27-full-coverage-audit.md`](./2026-05-27-full-coverage-audit.md)
**Purpose**: answer the question "can we start bug-fixes / core
rewrites now, or is more test coverage required first?". Reads
the parent audit + sweeps churn since `c3cf8cc7` (HEAD at audit)
instead of regenerating the full ~840-class matrix.

---

## 1. Churn since 2026-05-27 audit

| Commits since audit HEAD | Files touched | Production code changed? |
|---|---|---|
| `c3cf8cc7` test: TASK-36b deep | 1 test file added | No (test only) |
| `1ed946f0` test: TASK-37 + 38 + 39 batch | 3 test files added + `TestProbeCommand` extended | **Probe-only** — `TestProbeCommand` is a `/artest` debug surface, not gameplay logic. No gameplay code changed. |
| `b7e60b5e` docs: SOP MCP IntelliJ | 2 docs | No |
| `261931cb` chore: navigator state | 24 navigator files | No |

**Verdict**: zero production-gameplay churn. The parent audit's
class inventory (~559 prod classes, ~840 tests at the time)
still applies. Pyramid moved 839 → 843 (+4 net — TASK-36b deep
added 1, TASK-37/38/39 batch added 7, counter regen).

---

## 2. Gap delta vs parent audit

Parent audit catalogued **17 actionable contract gaps** (A–N from
first pass minus dropped Gap O cables, plus P–S from second pass)
≈ 50 h total.

| Gap | Title | Status at 2026-05-29 |
|---|---|---|
| A | Railgun firing contract | 🟥 Open |
| B | Orbital Laser Drill mode dispatch | 🟥 Open |
| C | Area Gravity Controller player effect | 🟥 Open (testClient) |
| D | Planet Analyser scan output | 🟥 Open |
| E | Rocket Loader / Unloader item active transfer | 🟥 Open |
| F.1 | TileCO2Scrubber | 🟥 Open (Phase 0 read pending) |
| F.2 | TileGasChargePad refills suit tank | 🟥 Open (testClient) |
| F.3 | TileAtmosphereDetector | 🟥 Open (Phase 0 read pending) |
| F.4 | TilePump fills from water source | 🟥 Open |
| G | TileGuidanceComputer chip → comparator | 🟥 Open |
| H | TileInvHatch / TileDataBus / TileSatelliteHatch divergence | 🟥 Open (Phase 0 may collapse) |
| I | TileHolographicPlanetSelector chip imprint | 🟥 Open |
| J | ItemUpgrade | 🟥 Open (Phase 0 may collapse) |
| K | ItemBasicLaserGun firing + packet + entity | 🟥 Open (Phase 0: verify wired) |
| L | ForceFieldProjector projects + retracts | 🟥 Open |
| M | BlockIntake / IIntake | 🟥 Open (Phase 0 may collapse to impl-only) |
| N | WorldProviderAsteroid + ChunkProviderAsteroid | 🟥 Open (worldgen, low prio) |
| O | Cable live-split routing | ⛔ Dropped (user removed cables from scope) |
| P | Nuclear engine rocket assembly | ✅ Shipped (TASK-37, `1ed946f0`) |
| Q | BlockMiningDrill placeable single-block | ✅ Shipped (TASK-38, `1ed946f0`) |
| R | TileSatelliteTerminal chip recognition | ✅ Shipped (TASK-39, `1ed946f0`) |
| S | Oxygen blob max-radius enforcement | 🟥 Open |

**Remaining gap count**: **15** (A–N + S, with H/J/K/M flagged
Phase-0-may-collapse → realistic landed count likely 11–13).
Estimated effort: **~40 h** unchanged from parent (subtract ~11 h
shipped P+Q+R = ~39 h remaining).

---

## 3. Coverage by subsystem — bug-fix / core-rewrite readiness

The question is whether the existing pyramid catches regressions
when production logic changes. Mapped per the parent audit §2
matrix, with **rewrite-safety** appended:

| Subsystem | Coverage depth | Rewrite-safe? | Notes |
|---|---|---|---|
| Rocket flight cycle (launch / dim / descent / dismantle / failure) | **Deep** | ✅ | All 6 RocketEvent payloads pinned; UV / crewed entity divergence pinned. |
| Rocket assembly (engines, drills, cargo, fuel, nuclear) | **Deep** | ✅ | TASK-37/38 close last asymmetries. |
| Industrial multiblocks (10 machines) | **Deep** | ✅ | Assembly + recipe e2e + power-drain via `MachineRecipeEndToEndKit`. |
| Heavy multiblocks (Warp / Laser Drill / Elevator / BHG / Beacon / Terraformer / Service Station / Space Laser / PrecisionAssembler) | **Deep** | ✅ | TASK-04 / 19 / 26 / 36 closed all assembly + powered-cycle paths. |
| Satellites (8 types) | **Deep** | ✅ | TASK-09 / 29 cover per-type DataType + tick contracts. |
| Atmosphere — sealed-room O₂ vent / suit vacuum | **Deep** | ✅ | Existing sealing / suit-drain tests. |
| Atmosphere — sub-blocks (CO2Scrubber / Pump / GasChargePad / AtmosphereDetector) | **Shallow** | ⚠️ | Gaps F.1–F.4. Rewrite risk if touching these tiles. |
| Mission system | **Deep** | ✅ | TASK-06 + persistence + rocket-side relink. |
| Station + controllers (altitude / gravity / orientation / monitor) | **Deep** | ✅ | TASK-30 + monitor comparator (TASK-32 3c). |
| `/ar` WorldCommand (planet / star / misc / console / player-equipped) | **Deep** | ✅ | TASK-11 + 21 + 35. |
| Items — generic suite (12 of 21 classes) | **Deep** | ✅ | TASK-05 + 10b Phase 7. |
| Items — Loader/Unloader, Upgrade, Laser gun, ItemPackedStructure | **Shallow / None** | ⚠️ | Gaps E, J, K. Rewrite risk if touching these. |
| Recipes (10 machines via end-to-end) | **Deep** | ✅ | Each recipe pinned through real production code path. |
| Persistence (NBT / save-load / wireless) | **Deep** | ✅ | TASK-09 + TASK-10 FluidTank + TASK-13. |
| Mixin layer (was ASM coremod) | **Deep** | ✅ | TASK-08-mixin. |
| Force-field / Railgun / Orbital Laser Drill | **Shallow** | ⚠️ | Gaps A, B, L. Rewrite risk if touching these tiles. |
| Hatches (Inv / DataBus / Satellite) | **Shallow** | ⚠️ | Gap H — may collapse to impl-only after Phase 0. |
| HolographicPlanetSelector, GuidanceComputer, PlanetAnalyser | **Shallow / None** | ⚠️ | Gaps D, G, I. |
| Worldgen (asteroid + within-session determinism) | **Partial** | ⚠️ | Gap N (cross-session is a conscious non-goal). |
| AreaBlob / IBlobHandler max-radius | **Shallow** | ⚠️ | Gap S. |
| BlockIntake, decoration blocks | **Shallow / Impl-only** | ⚠️ | Gap M may collapse. |

---

## 4. Bug-ledger state

3 live ledgered bugs, all with either pin or workaround
(unchanged since 2026-05-26):

1. `SatelliteRegistry.getNewSatellite` null-instead-of-fallback
   — pinned by `SatelliteRegistryFallbackTest._documentsKnownBug`.
   Fix-ready: flip pin polarity in the same commit.
2. `EntityElevatorCapsule.setStandTime(int)` ignores argument —
   ledger-only (no test). Fix-ready: add positive pin + flip.
3. `TileStationGravityController` constructor missing
   `redstoneControl.setRedstoneState(OFF)` — workaround pin in
   `StationControllersTickContractTest` gravity branch. Fix-ready:
   adjust workaround pin to positive contract.

A TASK-12-style sweep would close these in one batch (~3 h).

---

## 5. Bottom line — can you start now?

### 5a. Bug-fix pass — **GO**

Safe to flip the 3 ledgered bugs immediately:
- Pyramid (843 tests, deep coverage on the 16 ✅ subsystems above)
  catches collateral regressions.
- Each ledgered bug already has either a pin or a documented
  workaround, so the fix path is mechanical.
- Recommend TASK-40 (bug-fix sweep, ~3 h) for batched landing,
  mirroring TASK-12's 8-bug pass.

### 5b. Core rewrites — **CONDITIONAL GO**

Safe **if** the rewrite touches any of the ✅ subsystems above.
The 16 deep-covered areas catch behaviour regressions
end-to-end through real production code.

Add a **pre-rewrite contract pin** if the rewrite touches any
of the ⚠️ areas — specifically these subsystem clusters:

- Atmosphere sub-blocks (F.1–4) — touching `TileCO2Scrubber`,
  `TileGasChargePad`, `TileAtmosphereDetector`, `TilePump`
- Force-field / railgun / orbital laser drill (A, B, L) —
  touching `TileForceFieldProjector`, `TileRailgun`,
  `TileOrbitalLaserDrill`
- Player-effect tiles (C) — `TileAreaGravityController`
- Holographic / analyser / guidance (D, G, I)
- Loader / unloader / upgrade (E, J)
- Worldgen-asteroid (N)
- AreaBlob max-radius (S)
- Hatches divergence (H), Intake (M) — only if Phase 0 confirms
  the contract is non-trivial

For each ⚠️ cluster touched, pin one contract first (per parent
audit §3 — each gap already has the litmus blank filled), then
proceed with the rewrite. Probe infra is already in place for
most clusters (`/artest tile ...`, `/artest infra ...`).

### 5c. What does NOT need to close first

- TASK-15 (visual regression) — closed Not planned 2026-05-29.
- TASK-16 (flake watch) — investigation deliverable, complete.
- Gap O (cable routing) — out of scope per 2026-05-27 user call.
- Gap N (worldgen-asteroid) — low priority per parent audit.

---

## 6. Recommended next actions

1. **If next session is bug-fixes**: open TASK-40 (bug-fix sweep,
   3 ledgered bugs, ~3 h, mirrors TASK-12 structure).
2. **If next session is a core rewrite**: identify which
   subsystem cluster (§3) the rewrite touches. For ✅ — proceed.
   For ⚠️ — open a TASK that pins one parent-audit gap from that
   cluster before the rewrite, then ship the rewrite as a
   separate TASK.
3. **Optional cleanup** before either: TASK-40 + Gap S as a
   warm-up batch (~7 h) drains the small-scope tail. Not required.
