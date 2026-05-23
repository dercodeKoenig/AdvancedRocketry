# Test-coverage tasks — roadmap and dependency graph

## Current state (post-TASK-03)

Pyramid: **430 / 0 / 3** (testUnit 162 / testIntegration 80 /
testServer 179 / testClient 9).
testServer wall time: **8m 27s** (50 % faster than pre-B2).
Bug ledger: **8 bugs found, all 8 fixed in TASK-12** (2026-05-23).
Pins flipped from `_documentsKnownBug` to positive contract
assertions. See bottom of file for the history.

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
| TASK-10b | testClient e2e player-event coverage — Phases 1-7 ✅. Phases 1-6: 5 e2e suites, 15 pins, 9 new `/artest` verbs. Phase 7 (TASK-05 player-tier remainder): 5 suites / 19 pins (`ItemSealDetectorPlayerMessagesE2ETest` 8, `ItemAtmosphereAnalzerPlayerReadoutE2ETest` 3, `ItemHovercraftSpawnE2ETest` 3, `ItemBiomeChangerActionE2ETest` 2, `ItemSpaceArmorUseFluidE2ETest` 3) — drain-via-enchant fixture closed the originally-3-4h deferral. WeatherController + SpaceChest + ItemBlock\* trio rescoped/dropped per SOP litmus. | ✅ |
| TASK-11 | `/ar` (WorldCommand) coverage — 23 server-tier tests across 4 classes (planet set/get/list, planet generate/delete/reset lifecycle, star + dumpBiomes + reloadRecipes, console-sender guard contracts). Result-focused pins (registry state, JSON probe readback, file existence, chat envelope). Found + ledgered production bug #7 (`commandReloadRecipes` frozen-registry crash, pinned via `_documentsKnownBug`). | ✅ |
| TASK-12 | Production bug-fix sweep — 8 ledgered bugs fixed across 4 phases (NBT-attach pair, wrong-key bugs, cable/energy network merge, recipe reload). All `_documentsKnownBug` pins flipped to positive contract assertions; the suffix no longer in use anywhere. Full pyramid PASS post-fix. | ✅ |
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

*(TASK-10b Phase 7 closed 2026-05-22 — see Done table.)*

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

## Notes on `_documentsKnownBug` — historical ledger

All 8 bugs surfaced during the test-coverage build-up (TASK-02 /
TASK-03 / TASK-05 / TASK-10b / TASK-11). The original ledger
recorded the bug shape; tests pinned the wrong behaviour as
expected. **TASK-12 (2026-05-23) fixed all 8 in production and
flipped every pin to assert the corrected contract.**

1. `HandlerCableNetwork:67` — assertion polarity inverted.
   **Fixed**: assertion now requires both networks non-null
   (was: requires either side null). Pin flipped to
   `mergeNetworksProducesLowerIdSurvivor`.
2. `CableNetwork.merge` — addAll-before-dedupe ordering causes
   duplicate node retention.
   **Fixed**: per-entry dedupe restored (matches the commented-out
   `canMerge` blocks that suggested original intent). Pin flipped
   to `cableNetworkMergeReturnsTrueAndAbsorbsDisjointSinks`.
3. `EnergyNetwork.merge` — battery-migration cascade from (2).
   **Fixed**: cascades naturally from #2. Pin flipped to
   `energyNetworkMergeMigratesBatteryFromMergedSource`.
4. `SpaceStationObject:801` — writes NBT key `"autoLand"`, reads
   key `"occupied"`. The autoLand flag is silently dropped across
   save/load.
   **Fixed**: read now uses the `"autoLand"` key on both sides;
   default-true fallback preserves legacy-save compatibility. Pin
   flipped to `autoLandFlagWithoutDockSurvivesRestart`.
5. `ItemSpaceElevatorChip:42` — calls `removeTag("positions")` to
   clear the chip's stored positions, but `NBTStorableListList`
   actually stores entries under the key `"list"`. Setting an empty
   position list is a no-op; clearing the chip from the GUI doesn't
   work.
   **Fixed**: changed `removeTag` key to `"list"`. Pin flipped to
   `elevatorChipSetEmptyAfterNonEmptyClearsList`.
6. `ItemSatelliteIdentificationChip.setSatellite(stack, SatelliteBase)`
   (lines 54-64) — else-branch built fresh NBT but never called
   `stack.setTagCompound(nbt)`. Player-visible: programming a fresh
   blank chip produced a still-blank chip.
   **Fixed**: added the missing `stack.setTagCompound(nbt);` mirroring
   the sibling overload at line 87. **Pin added in TASK-12**:
   `satelliteChipSetSatelliteAttachesNbtToFreshStack` (was originally
   ledger-only).
7. `WorldCommand.commandReloadRecipes` (line 256-258) — included
   `createAutoGennedRecipes` which calls `ForgeRegistry.register_impl`
   on the frozen recipe registry. Crashed with
   `IllegalStateException("is being added too late")` and emitted the
   `"Serious error has occurred"` message. Cascading bug: the
   JEI-integration call (`CompatibilityMgr.reloadRecipes` →
   `ARPlugin.reload` → `jeiHelpers.reload()`) NPE-d on a dedicated
   server because `jeiHelpers` is null off the client.
   **Fixed (compound)**:
   (a) removed `createAutoGennedRecipes` from the runtime reload —
       it's an init-only registration (the init-time call at
       `AdvancedRocketry.java:1044` is sufficient; auto-genned
       recipes are static once `modProducts` is set);
   (b) added null-guard on `jeiHelpers` in `ARPlugin.reload` so the
       JEI cascade is a no-op when JEI isn't initialised (correct
       for dedicated server). Pin flipped to
       `reloadRecipesEmitsSuccessConfirmationMessage`.
8. `ItemPlanetIdentificationChip.setDimensionId(stack, INVALID_PLANET)`
   (lines 73-77) — same shape as #6 but in a different class. The
   INVALID_PLANET branch built fresh NBT, wrote `dimId`, and returned
   without `stack.setTagCompound(nbt);`. The sentinel was silently
   dropped.
   **Fixed**: added the missing `stack.setTagCompound(nbt);`. Pin
   flipped to
   `planetChipSetDimensionIdWithInvalidPlanetAttachesNbtSentinel`.

Status as of 2026-05-23: ledger drained. The `_documentsKnownBug`
suffix is no longer in use in this repo — if a future bug is found,
add it here and pin the wrong behaviour as before.
