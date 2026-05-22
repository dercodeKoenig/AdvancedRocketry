# Test-coverage tasks — roadmap and dependency graph

## Current state (post-TASK-03)

Pyramid: **404 / 0 / 3** (testUnit 162 / testIntegration 80 /
testServer 156 / testClient 6).
testServer wall time: **8m 27s** (50 % faster than pre-B2).
Bug ledger: **6** real production bugs recorded (5 pinned by
`_documentsKnownBug` tests, 1 ledger-only — see bottom of file).

## Done

| ID | Title | Status |
|---|---|---|
| TASK-01 | SMART per-scenario depth coverage | ✅ |
| TASK-02 | Functional coverage expansion (Phases 0–8, 11) | ✅ |
| TASK-03 | Test depth deepening + harness consolidation (A1/A2/A4/A5/A6/A7 + B1/B2/B4/C) | ✅ partial — A2 tail + B3 deferred to TASK-10; A3 reframed as testClient e2e (TASK-10b) |
| TASK-04 | Multiblock machine depth (Warp / Laser Drill / Elevator / Black Hole / 12 multiblocks) | ✅ |
| TASK-07 | Rocket flight cycle beyond launch (orbit / dim-transition / descent / landing / dismantle / failure modes) | ✅ |
| TASK-08-mixin | Rewrite ASM coremod (`ClassTransformer.java` + vendored HookLib) to Mixin; behavioural pin for `setBlockState` hook; existing 239-test suite implicitly pins gravity + atmosphere hooks | ✅ |
| TASK-10 | TASK-03 deferred tail — A2 remainder (4 deep-tile tests: FluidTank NBT round-trip, UV-vs-Rocket assembler class identity, SuitWorkStation assembly, FuelingStation matched accounting) + B3 single-method-smoke suite-grouping (MachineDomainSmokeSuite, ServerBootSmokeSuite) | ✅ |
| TASK-10b | testClient e2e player-event coverage — Phases 1-6 ✅ (5 e2e suites, 15 pins, 9 new `/artest` verbs). **Phase 7 reopened 2026-05-21** to absorb TASK-05 player-tier remainder (Hovercraft / SpaceArmor useFluid / SpaceChest death-persist / BiomeChanger + WeatherController right-click / SealDetector messages / AtmosphereAnalzer readout). | ✅ partial |
| TASK-09 | Per-satellite-type behavioural depth — 3 suites / 14 pins (`SatelliteTickBehaviourTest` 4: base power + cap + SatelliteData accumulation/cap; `SatelliteTypeBehaviourTest` 3: IUniversalEnergyTransmitter marker + BiomeChanger terraform + WeatherController mode-0; `SatelliteCoverageGapsTest` 7: weather modes 1/2 + mode-change clear + biome batch-10 + biome null-guard + canTick gating + isDead removal) + 15 new `/artest` verbs | ✅ |
| TASK-05 | Item-behaviour suite — unit-tier surface for 12 of 21 item classes (5 chips, 2 data-carriers, BeaconFinder, OreScanner, Thermite, BiomeChanger / WeatherController metadata+wire, JackHammer pure-fn) via `ChipNBTRoundTripTest`, `ItemDataCarrierNBTRoundTripTest`, `ScannerDetectorItemContractTest`, `SpecialPurposeItemContractTest`, `JackHammerContractTest`, plus SealDetector dispatch via new `/artest seal-detector check` probe (`SealDetectorDispatchTest` 8 server tests). ~48 contract pins, +1 production bug (`ItemSpaceElevatorChip` wrong removeTag key). Player-tier surface moved to TASK-10b Phase 7. | ✅ partial |
| TASK-06 | Mission-system depth — 20 tests (3 unit + 17 server) covering lifecycle progress/completion/registry-prune, gas + ore completion, gas fluid-fill (strong 64000 mB pin via new `with-fluid-cargo` fixture), 3 NBT round-trips, infra-tile link/unlink lifecycle with rocket-side relink, and gas+ore multi-boot persistence. 9 `/artest mission` probe verbs (incl. link-infra, infra-state, rocket-relink-state). | ✅ |

## Backlog — prioritised

Priority is by **gameplay impact × regression-blast-radius**. P0 items
ship silent gameplay breakage if untouched code regresses.

### 🔴 P0 — schedule next

*(TASK-04 multiblock depth, TASK-07 flight cycle, TASK-08-mixin coremod
rewrite: closed — see Done table.)*

### 🟡 P1 — broad surface, medium impact

4. **TASK-10b Phase 7** — TASK-05 player-tier item behaviour
   (~10-14 h). Hovercraft spawn, SpaceArmor useFluid + damage
   absorption, SpaceChest death-persist, BiomeChanger /
   WeatherController right-click satellite action, SealDetector
   per-branch player message, AtmosphereAnalzer readout. The
   unit-tier surface for items closed in TASK-05 (12 of 21 classes);
   what remains genuinely needs a real EntityPlayer, so per the
   no-FakePlayer rule it lives in testClient e2e under TASK-10b's
   harness.

### 🟢 P2 — narrower, lower urgency

*(TASK-06 mission system depth: closed — see Done table. Rocket-side
relink follow-up closed 2026-05-22.)*

### Already-known deferred (no task doc yet — surface in a future plan)

- Phase 9 — JEI / GalacticCraft / MatterOverdrive integration tests
  (needs companion mods in classpath; TASK-02 deferred).
- Phase 10 — Visual regression (Storybook + Chromatic equivalent for
  Minecraft client; TASK-02 deferred — own proposal).
- Pipe end-to-end (placed pipe blocks) — blocked by commented-out
  block registrations at `AdvancedRocketry.java:782-787`. Reinstate
  before any of these can be tested.
- Production-side `WorldCommand` (`/ar`) — 991 LoC, 0 coverage. Worth
  its own task when prioritised.
- Production bug fixes for the 4 currently-pinned
  `_documentsKnownBug` tests — separate ticket; flip assertions
  afterwards.

## Dependency graph

```
TASK-03 ──┬─► TASK-04  (multiblock)
          ├─► TASK-05  (items)        ─┐
          ├─► TASK-06  (missions)     ─┤── EntityPlayer paths
          ├─► TASK-07  (rocket cycle) ─┤   live in testClient e2e
          ├─► TASK-08  (ASM)           │   (TASK-10b proposal)
          ├─► TASK-09  (satellite types)
          └─► TASK-10  (A2 tail + B3 grouping)
```

All P0/P1 tasks are independent of each other. TASK-05 / TASK-06 are
NOT blocked by TASK-10 — their EntityPlayer-touching coverage is the
responsibility of testClient e2e (planned as TASK-10b), not of a
FakePlayer injection.

## Suggested session ordering

If picking the next session:

1. **Player-visible coverage win**: start TASK-10b Phase 7 (TASK-05
   player-tier remainder). Each item sub-suite is independently
   shippable in 1-2 h. Start with `ItemSpaceArmorUseFluidE2ETest` or
   `ItemSealDetectorPlayerMessagesE2ETest` — they extend existing
   testClient suites and reuse the seal-detector probe surface.
2. **Smallest-but-coherent**: start TASK-06 (mission system depth).
   ~2-3 h infra (`/artest mission` probes) then 3 phases of ~2-3 h.

## Conventions

All TASK-NN docs share a structure:

- **Context**: what's currently uncovered + why it matters.
- **Implementation Plan**: phased; each phase ~2-5 h.
- **Technical Decisions**: same `no production logic changes` rule
  as TASK-01 §15. New probe verbs documented inline.
- **Dependencies**: explicit `requires` / `does NOT block` calls.
- **Completion Checklist**: phase-by-phase; flipping to ✅ requires
  a green pyramid run + an EOD marker.
- **EOD marker**: in `.agent/.context-markers/` with the date and a
  short slug. The `.active` file points at the most recent marker
  for `/nav:start` to pick up.

## Notes on `_documentsKnownBug`

Recorded as we found them across TASK-02 / TASK-03 / TASK-05 /
TASK-10b audits. Entries 1-5 are pinned by `_documentsKnownBug`
tests that assert the **current (wrong) behaviour** as expected —
the day someone fixes production, the test fails and forces an
update. Entry 6 is ledgered-only (see "Pin status" notes).

1. `HandlerCableNetwork:67` — assertion polarity inverted.
   **Pin**: `_documentsKnownBug` in CableNetworkSuite (TASK-02).
2. `CableNetwork.merge` — addAll-before-dedupe ordering causes
   duplicate node retention.
   **Pin**: `_documentsKnownBug` in CableNetworkSuite (TASK-02).
3. `EnergyNetwork.merge` — battery-migration cascade from (2).
   **Pin**: `_documentsKnownBug` in EnergyNetworkSuite (TASK-02).
4. `SpaceStationObject:801` — writes NBT key `"autoLand"`, reads
   key `"occupied"`. The autoLand flag is silently dropped across
   save/load.
   **Pin**: `_documentsKnownBug` in SpaceObjectPersistenceTest (TASK-03).
5. `ItemSpaceElevatorChip:42` — calls `removeTag("positions")` to
   clear the chip's stored positions, but `NBTStorableListList`
   actually stores entries under the key `"list"`. Setting an empty
   position list is a no-op; clearing the chip from the GUI doesn't
   work.
   **Pin**: `_documentsKnownBug` in ItemDataCarrierNBTRoundTripTest (TASK-05).
6. `ItemSatelliteIdentificationChip.setSatellite(stack, SatelliteBase)`
   (lines 54-64) — in the `else`-branch (stack has no existing
   NBTTagCompound) the method constructs a new local NBT, writes
   `satelliteName`/`dimId`/`satelliteId` into it, but never calls
   `stack.setTagCompound(nbt)`. Result: the NBT is silently dropped
   for any item that didn't already have a tag. Cross-reference:
   the sibling overload `setSatellite(stack, SatelliteProperties)`
   (line 72-89) DOES call `stack.setTagCompound(nbt)` at line 87 —
   confirming the omission in the SatelliteBase overload is an
   oversight, not deliberate. Player-visible consequence: programming
   a fresh blank chip with a satellite reference via this code path
   produces a still-blank chip; right-clicking it does nothing.
   **Pin**: **none yet — ledger only.** Discovered during TASK-10b
   Phase 7 BiomeChanger probe work where we worked around it by
   writing the NBT directly in the probe. Worth a `_documentsKnownBug`
   when a future test exercises the chip-programming path; for now,
   the ledger entry is enough to ensure a bug-fix ticket sweeps it in.

A separate **bug-fix ticket** should address all six; once fixed,
the pinned tests (#1-5) flip to expected-passing semantics and #6
becomes "verified safe" (no test to flip).
