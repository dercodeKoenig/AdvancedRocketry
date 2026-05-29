# Test-coverage tasks — roadmap and dependency graph

## Source of truth

**Status of every task lives in its own `TASK-NN-*.md` file.** This
README is a derived index — Done and Backlog tables below mirror
each task file's header, nothing more. When in doubt, the
individual task file wins.

Lifecycle rules and the closure checklist are in
[`../sops/development/task-lifecycle.md`](../sops/development/task-lifecycle.md).
Bug-ledger history lives in
[`../history/known-bugs-ledger.md`](../history/known-bugs-ledger.md).

## Current state

- **Pyramid**: 846 (testUnit **288** / testIntegration 81 /
  testServer **417** / testClient **60**). +3 on 2026-05-29 from
  TASK-40 Batch 1 (audit Gaps E/A/D from 2026-05-27 audit):
  `RocketItemUnloaderActiveTransferTest` (1) +
  `RailgunCargoReceiveContractTest` (1) +
  `PlanetAnalyserResearchContractTest` (1) + 7 new probe verbs.
  +7 on 2026-05-27 from
  the TASK-37/38/39 batch (audit Gaps P/Q/R from
  `.agent/audits/2026-05-27-full-coverage-audit.md`):
  TASK-37 nuclear engine rocket-assembly (2 server —
  `NuclearEngineRocketAssemblyTest`: core-above-motor → thrust > 0;
  misplaced core → scan rejects with NOENGINES). TASK-38 IMiningDrill
  stat aggregation (1 server — `RocketAssemblerMiningDrillStatTest`:
  simple → drillingPower=0, with-mining-drill → drillingPower>0).
  TASK-39 TileSatelliteTerminal chip recognition (4 server —
  `SatelliteTerminalChipRecognitionTest`: status 0/1/3 ladder +
  destructive erase button removes satellite from dim + blanks NBT).
  Probe additions: 3 new fixture rocket variants
  (`with-nuclear-stack`, `with-nuclear-misplaced`, `with-mining-drill`),
  `drillingPower` field on `/artest rocket info`, new
  `/artest satellite-terminal {info|load-chip|press-erase}` subcommand
  group. Reused TASK-36a TerraformingTerminal probe pattern. Counter
  regenerated via
  `grep -rc '@Test$' src/test/java/.../{unit,integration,server,client}/`.
  +1 earlier on 2026-05-27 from
  TASK-36b deep — full repair cycle with formed PrecisionAssembler
  multiblock (`ServiceStationFullRepairCycleTest`): phase 1
  (consumePartToRepair moves part to partsProcessing), phase 2
  (processAssemblerResult clears slot + restores part at stage 0
  to rocket storage). Reuses TASK-26 `/artest fixture machine
  precision-assembler` wildcard-overlay probe (was already in
  place — `MachineRecipeEndToEndKit`'s "wildcard machines out of
  scope" caveat misled prior deferral). New `/artest infra
  service-perform-function` reflection-bypass probe (calls public
  performFunction direct, sidesteps canPerformFunction's
  `worldTime % 20 == 0` gate); `service-state` extended with
  `partsProcessingCount`. Earlier same-day batch: +3 from
  TASK-36b extension + multi-client moderator-fetch batch:
  TASK-36b extension `ServiceStationAssemblerScanTest` (2 server:
  scanForAssemblers picks up nearby PrecisionAssembler block,
  no-assembler-no-progress invariant); new `/artest infra
  service-scan-assemblers` reflection probe bypasses the
  canPerformFunction (worldTime % 20 == 0) gate that force-tick
  can't satisfy. Multi-client moderator-fetch:
  `WorldCommandFetchModeratorTest` (1 testClient — bot1 op fetches
  bot2 across positions). Required ForgeTestFramework changes:
  new `RealClientHarness.start(server, username)` overload + moved
  `--username` + `--uuid` out of the `legacyArgs` block so FG6
  `legacydev.MainClient` honours them (without that, FG6 generates
  random "Player###" names breaking name-resolution probes). AR
  probes added: `player exec-as-named <name> <cmd>`,
  `player position-of <name>`, `player op-named <name>` — multi-
  client variants of the existing single-bot verbs that hardcode
  `players.get(0)`. testClient now requires
  `-PuseLocalFramework=true` if the modified framework hasn't been
  published to mavenLocal. Earlier same-day batch: +2 from
  TASK-35 — `/ar fetch` coverage without a second player:
  `WorldCommandFetchTest` (2 testClient — self-fetch positive
  pinning resolve→transfer→setPosition, unknown-name negative
  pinning `getPlayerByName==null` branch). Reframes the original
  Phase 0 plan (heavy NetworkManager-stub real-EntityPlayerMP probe)
  as unnecessary: self-fetch + bot username from `artest player
  health` cover the verb's contract surface. Multi-player "moderator
  fetch" still out of scope. Earlier same-day batch: +5 from
  TASK-33 + TASK-36a batch: TASK-33 satellite-builder press-build
  contract (2 server: optical-happy-path + per-type chip rejection
  for weatherController), new `/artest satellite-builder press-build
  <dim> <x> <y> <z> <typeId>` probe (drives REAL `onInventoryButtonPressed`
  path on a placed tile, not the pre-existing fast-path that
  bypasses TileSatelliteBuilder); TASK-36a terraforming terminal
  chip-recognition (3 server: valid chip + redstone → enabled,
  valid chip no redstone → idle, empty slot rejects), new
  `/artest terraforming terminal-info` + `terminal-load-chip` probes.
  Earlier same-day batch: +3 from
  TASK-36b partial — service-station broken-part scan contract
  (`ServiceStationBrokenPartScanContractTest` 3 server: positive
  link-time scan + multi-part scan + post-link-injection-needs-rescan).
  Probe surface: `/artest infra inject-broken-part <entityId> <stage>`
  marks a TileBrokenPart in rocket storage as worn (mirrors
  production wear-on-use, without needing PrecisionAssembler
  recipe wiring); `/artest infra service-relink <dim> <x> <y> <z>`
  invokes private `updateRepairList()` so tests can mutate
  rocket storage AFTER linking. Sister Phase 0 audits for
  TASK-33 / TASK-35 / TASK-36a documented in their task files
  (still backlogged; recommended landing order 33+36a → 35).
  Earlier same-day batches: +5 from
  TASK-34 + TASK-30 batch: TASK-34 fluid loader/unloader active
  transfer (2 server — loaderTransfersOxygenIntoRocketStorage +
  unloaderDrainsRocketStorageIntoOwnTank, with `rocket
  storage-fluid-fill` probe addition); TASK-30 station controller
  tick contracts (3 server — altitude/gravity/orientation walk
  target, with `station controller-set-target` probe and station
  info extension for gravity/rotation/targetGravity/targetRPH).
  Bug #3 logged to ledger (gravity controller redstone-default
  bug — workaround test pins end-state walk).
  Earlier same-day batch: +15 from
  TASK-29/31/32 batch: TASK-29 scanning satellite tick contracts
  (6 server — per-type DataType pins for Optical/Density/Mass/Composition,
  oreScanner non-SatelliteData pin, SpyTelescope no-op-tick pin),
  TASK-31 rocket lifecycle event payloads (3 server — Landed +
  DeOrbiting + ReachesOrbit entity-id + dim payload pins, extending
  RocketEventPayloadContractTest to cover the full 6-event surface),
  TASK-32 Tier 3 misc (2 unit + 2 server — ItemPackedStructure
  null-gate + hasSubtypes, custom AtmosphereType registry+NBT
  round-trip, MonitoringStation comparator-override unlinked=0 +
  monotonic-with-posY). Probe surface: `satellite data` emits
  `dataType.name()` (stable enum identifier, not the localization
  key), `infra monitor-info` exposes `comparatorOverride`.
  Earlier same-day batches: +35 from the second audit batch:
  Gap 3 PlanetaryTravelHelper (11 unit),
  Gap 1 RocketLoader polarity (6 unit), Gap 7 GravityHandler (6 unit),
  Gap 4 SatelliteWeatherController NBT (2 unit), Gap 8 SatelliteMicrowave
  teir NBT (2 unit), Gap 6 FluidTank stacked-fill (2 server),
  Gap 5 TileDockingPort NBT+packet (4 server), Gap 2 MonitoringStation
  redstone trigger (2 server). Earlier same-day batch: +7 from
  TASK-30 Gap 3 elevator capsule (5 server + 2 client).
  +66 on 2026-05-25 from TASK-19 (11) + TASK-23 (2) + TASK-22 (4) +
  TASK-24 (3) + Tier 1 audit gaps (10) + Tier 2/3 audit gaps (27) +
  TASK-20 hovercraft (4) + TASK-21 /ar player-equipped (5).
  Counter regenerated via
  `grep -rc '@Test$' src/test/java/.../{unit,integration,server,client}/`.
- **testServer wall time**: 8m 27s (50 % faster than pre-B2).
- **Bug ledger**: 3 live bugs (Batch #2 opened 2026-05-25).
  Batch #1 fully drained by TASK-12 on 2026-05-23. Entries:
  (1) `SatelliteRegistry.getNewSatellite` returns `null` for unknown
  types instead of the documented `SatelliteDefunct` fallback —
  pinned by `SatelliteRegistryFallbackTest._documentsKnownBug` pair.
  Found during coverage-audit Gap 4.
  (2) `EntityElevatorCapsule.setStandTime(int)` ignores its
  argument and writes the `standTime` field — masked today because
  the single caller passes the field value. Ledger-only.
  Found during TASK-30 Gap 3 authoring (2026-05-26).
  (3) `TileStationGravityController` constructor does NOT call
  `redstoneControl.setRedstoneState(OFF)` (its altitude sibling
  does, line 43). `ModuleRedstoneOutputButton`'s default is `ON`,
  so freshly-placed gravity controllers enter `update()` with
  `redstoneControl.getState() == ON`, overwriting the station's
  `targetGravity` to `(strongPower * 6) + 10 = 10` on every tick
  with no redstone wiring around it. Player-visible: a placed
  gravity controller pulls station gravity to 0.1 by default
  until the player explicitly toggles the redstone control via
  GUI. Worked around by `StationControllersTickContractTest`'s
  gravity test (pins end-state walk, not target identity). No
  `_documentsKnownBug` test — the workaround test already
  inherits the contract polarity. Found during TASK-30
  authoring (2026-05-26).
  See `.agent/history/known-bugs-ledger.md` Batch #2.

## Done

| ID | Title | Status |
|---|---|---|
| [TASK-01](TASK-01-smart-depth-coverage.md) | SMART per-scenario depth coverage | ✅ |
| [TASK-02](TASK-02-functional-coverage-expansion.md) | Functional coverage expansion (Phases 0–8, 11; Phase 9 → TASK-14, Phase 10 → TASK-15) | ✅ |
| [TASK-03](TASK-03-test-depth-and-harness-consolidation.md) | Test depth deepening + harness consolidation (A1/A2/A4/A5/A6/A7 + B1/B2/B4/C); A2 tail + B3 → TASK-10; A3 → TASK-10b | ✅ |
| [TASK-04](TASK-04-multiblock-machine-depth.md) | Multiblock machine depth (Warp / Laser Drill / Elevator / Black Hole / 12 multiblocks) | ✅ |
| [TASK-05](TASK-05-item-behaviour-suite.md) | Item-behaviour suite — unit-tier for 12 of 21 classes + SealDetector dispatch; player-tier → TASK-10b Phase 7 | ✅ partial |
| [TASK-06](TASK-06-mission-system-depth.md) | Mission-system depth — 20 tests + 9 mission probe verbs, rocket-side relink shipped | ✅ |
| [TASK-07](TASK-07-rocket-flight-cycle-beyond-launch.md) | Rocket flight cycle beyond launch (orbit / dim-transition / descent / landing / dismantle / failure) | ✅ |
| [TASK-08](TASK-08-asm-coremod-safety-net.md) | ASM coremod safety net | ❌ Obsolete (superseded by TASK-08-mixin) |
| [TASK-08-mixin](TASK-08-mixin-rewrite.md) | Rewrite ASM coremod (`ClassTransformer.java` + vendored HookLib) to Mixin | ✅ |
| [TASK-09](TASK-09-satellite-type-depth.md) | Per-satellite-type behavioural depth (3 suites / 14 pins + 15 satellite probe verbs) | ✅ |
| [TASK-10](TASK-10-fakeplayer-and-task03-tail.md) | TASK-03 deferred tail — A2 remainder (4 deep-tile tests) + B3 single-method-smoke suite-grouping | ✅ |
| [TASK-10b](TASK-10b-testclient-player-events.md) | testClient e2e player-event coverage — Phases 1-7 (5 suites + 15 pins + 9 probe verbs + Phase 7 player-tier item closures) | ✅ |
| [TASK-11](TASK-11-world-command-coverage.md) | `/ar` (WorldCommand) coverage — 23 tests across 4 classes (planet / star / misc / console-sender) | ✅ |
| [TASK-12](TASK-12-bug-fix-pass.md) | Production bug-fix sweep — 8 ledgered bugs fixed across 4 phases; pins flipped from `_documentsKnownBug` to positive contracts | ✅ |
| [TASK-13](TASK-13-wireless-transceiver-coverage.md) | Wireless transceiver E2E coverage (pivoted from pipe E2E — upstream deprecated pipes in commit 48610953) — 11 server-tier pins + 4 new probe verbs | ✅ |
| [TASK-14](TASK-14-companion-mod-integration-coverage.md) | Companion-mod integration coverage (JEI / GC / MO) — closed as Obsolete: mod-absent paths already pinned implicitly by 441 boot-the-server tests + TASK-11's JEI null-guard pin | ❌ Obsolete |
| [TASK-17](TASK-17-ssot-integrity-followups.md) | SSOT integrity follow-ups — `task-lifecycle.md` step 2.5 (counter regen) shipped; Phase 2a already done in `b97ddf0b`; Phase 2b premise wrong (no exact-120-RF assertion existed) → doc-comment cleanup only | ✅ |
| [TASK-18](TASK-18-industrial-machine-powered-cycle.md) | Industrial machine powered-cycle coverage — 7 of 9 multiblock machines shipped (14 server-tier tests + 3 probe extensions + shared `MachineRecipeEndToEndKit` with input-drain pin); ArcFurnace + PrecisionAssembler → TASK-26 (wildcard structure shape) | ✅ partial |
| [TASK-25](TASK-25-plate-press-coverage.md) | PlatePress (single-block redstone-triggered) recipe coverage — 1 class × 2 tests + 3 probe verbs (`fixture machine plate-press`, `recipe-info-block`, `entity scan-items`) | ✅ |
| [TASK-26](TASK-26-wildcard-based-machine-coverage.md) | Wildcard-structure machine coverage — ArcFurnace + PrecisionAssembler (2 classes × 2 tests = 4 server-tier tests + 1 generic-helper refactor with hatch-overlay + structure-block-filler for `'*'` cells + 1 kit hook for adaptive force-tick budget) | ✅ |
| [TASK-27](TASK-27-flake-fix-port-and-tick-races.md) | Flake fix — port-bind retry in `RealDedicatedServerHarness` + per-test polling for tick races + shape-#3 `tryCompleteWithRetry` kit helper (Beacon + cuttingMachine migrated) + `wireless-info` wait-for-tile probe + `field info` budget bump. **Acceptance partial**: 10× metric not achieved — residual flake shapes outside original scope → TASK-28. | ✅ partial |
| [TASK-28](TASK-28-residual-test-flakes.md) | Residual flake shapes from TASK-27 — chunk-force probe helper (F1/F6/F7), ForceField direct-tick refactor (F2), Centrifuge permissive output (F3), Observatory + Wireless migrations. **9/10 PASS in v10**; v11 F8 watch sweep (2026-05-25) confirmed **0/10 Beacon recurrence** — F8 in watching mode (1/5 clean reruns toward Obsolete), new F9 (MissionGasCompletion fluidEntries:0) at 1/5. TASK-29 not opened — triggers not met. | ✅ partial |
| [TASK-19](TASK-19-multiblock-powered-cycle-trio.md) | Multiblock powered-cycle (Terraformer / BHG / Beacon) — 11 server-tier tests across 4 classes: Phase 1a AR-native terraformer (3), Phase 1b non-AR config flip (2), Phase 2 BHG on station orbiting black-hole star (3), Phase 3 Beacon enable/disable/break (3). 5 new probe verbs (`machine controller-state`, `machine clear-batteries`, `config get/set` whitelisted, `star get/set-blackhole`). | ✅ |
| [TASK-22](TASK-22-uv-assembler-full-delta.md) | UV-assembler full behavioural delta from rocket assembler — 4 server-tier tests across 2 classes: Phase 1 bounds-constants delta via reflection (2), Phase 2 output entity class delta (rocket → EntityRocket, UV → EntityStationDeployedRocket) via new `uv-rocket` fixture probe (2). Phase 3 mount eligibility deferred — implicitly covered by Phase 2's entity-class pin. | ✅ partial |
| [TASK-23](TASK-23-sealdetector-remaining-branches.md) | SealDetector remaining branches — 2 of 3 deferred branches pinned: `notsealblock` via probe-driven `blockBanList` mutation, `fluid` via AR's `oxygenFluid` (IFluidBlock). Third branch `notfullblock` documented as unreachable (no vanilla/AR block satisfies the required full-collision-bbox + liquid/IFluidBlock combination). Phase 4 client mirror skipped — server-tier probe replicates dispatch 1:1. | ✅ partial |
| [TASK-24](TASK-24-spacearmor-chest-route.md) | SpaceArmor CHEST sub-inventory drain (testClient) — 3 testClient tests pinning vacuum-drain through `ItemSpaceChest.decrementAir` (component-walking + FluidStack drain in embedded pressure tank). 2 new probes (`player equip-space-chest`, `player held-air-component-route`). testClient harness requires `xvfb-run` wrapper on headless dev boxes. Phase 2 (Suit Workstation drive-through) deferred. | ✅ |
| [TASK-20](TASK-20-hovercraft-ride-coverage.md) | Hovercraft ride / mount / throttle / motion (testClient) — 4 client tests: mount via startRiding probe, dismount, throttle-via-drive-ridden-entity probe (composite that re-applies moveForward inline to defeat CPacketInput reset), unmounted hovercraft doesn't drift. Phase 3 fuel reframed as documentation — production has zero fuel logic; documented so future addition forces a contract pin. 5 new probes. | ✅ partial |
| [TASK-21](TASK-21-ar-player-equipped-positives.md) | `/ar` player-equipped positive paths (testClient) — 5 client tests: goto dim, goto station, giveStation chip, addTorch, addSolidBlockOverride. New `player exec-as-player` probe (bot-as-sender via commandManager) + op-self/deop-self + inventory-contains + give-held probes. `/ar fetch` deferred (needs two-bot harness); `/ar fillData` covered transitively by satellite-construction flow. | ✅ partial |
| [TASK-29](TASK-29-scanning-satellite-tick-contracts.md) | Scanning satellite tick contracts — 6 server-tier tests pinning per-type DataType identity (Optical→DISTANCE, Density→ATMOSPHEREDENSITY, Mass→MASS, Composition→COMPOSITION), oreScanner non-SatelliteData + battery-only accrual, SpyTelescope no-op-tick defense-in-depth. Probe `satellite data` updated to emit `dataType.name()` (stable enum, not localization key). | ✅ |
| [TASK-31](TASK-31-rocket-event-payload-contracts.md) | Rocket lifecycle event payloads — 3 server-tier tests extending RocketEventPayloadContractTest: RocketLandedEvent (real-tick descent), RocketDeOrbitingEvent (`ticksExisted == 20` branch), RocketReachesOrbitEvent (via `force-orbit-reached` probe). Together with the pre-existing Dismantle + PreLaunch pins, all 6 RocketEvent subtypes now have entity-id + dim payload coverage. | ✅ |
| [TASK-32](TASK-32-tier3-misc-coverage.md) | Tier 3 misc — 4 tests across testUnit + testServer. 3a ItemPackedStructure unit pins (null-gate + hasSubtypes flag — full setStructure round-trip requires runtime profiler, deferred to existing server-tier coverage). 3b custom AtmosphereType registry + NBT round-trip (2 unit tests). 3c MonitoringStation comparator override (2 server: unlinked-returns-0 + monotonic-with-posY); new `infra monitor-info comparatorOverride` field on the probe. | ✅ |
| [TASK-34](TASK-34-fuel-loader-active-transfer.md) | Fluid loader / unloader active transfer — 2 server tests using the existing `with-fluid-cargo` fixture variant (loader oxygen → rocket liquidTanks via real-tick natural transfer, unloader pulls oxygen back into its own tank). 1 new probe verb `rocket storage-fluid-fill` (writes via `FLUID_HANDLER_CAPABILITY` on storage TEs). Phase 0 outcome: NOT Obsolete — capability survives storage chunk round-trip when using `liquidTank` (TileFluidTank) blocks, already proven by MissionGasCompletionTest. | ✅ |
| [TASK-30](TASK-30-station-controller-tick-contracts.md) | Station controller tick contracts (altitude / gravity / orientation) — 3 server tests. New `station controller-set-target <dim> <x> <y> <z> <id> <value>` probe (calls `ISliderBar.setProgress` directly), `station info` extended with `gravity`, `targetGravity`, `rotationEast/Up/North`, `targetRPH0..2`, `targetOrbitalDistance`. Gravity controller has a redstone-default-state production bug — logged in ledger as Batch #2 entry #3, test workaround pins end-state walk under the broken default. | ✅ |
| [TASK-36b](TASK-36-terraforming-and-service-station-depth.md) | Service-station broken-part scan contract — 3 server tests (`ServiceStationBrokenPartScanContractTest`: inject + link → scan finds it, multi-part scan, post-link injection needs explicit re-scan). New `/artest infra inject-broken-part <entityId> <stage>` probe (uses pre-existing TileBrokenPart instances copied into rocket storage by `cutWorldBB`, calls setStage — no allocation). New `/artest infra service-relink` probe exposes private `updateRepairList()` for post-link injection scenarios. Repair-cycle WITH PrecisionAssembler still deferred (recipe-surface dependency). TASK-36a (BiomeChanger) still in backlog. | ✅ partial |
| [TASK-33](TASK-33-satellitebuilder-real-construction.md) | SatelliteBuilder real-construction path — 2 server tests (`SatelliteBuilderPressBuildContractTest`: optical happy-path pinning chassis-consumed + holding slot carries ItemSatellite + chip slot has matching satelliteId; weatherController negative pin for per-type chip override). New `/artest satellite-builder press-build <dim> <x> <y> <z> <typeId>` probe loads slots and invokes `onInventoryButtonPressed(0)` (REAL GUI path, not the fast-path bypass). | ✅ |
| [TASK-36a](TASK-36-terraforming-and-service-station-depth.md) | TerraformingTerminal chip-recognition + redstone gate — 3 server tests (`TerraformingTerminalChipRecognitionTest`: chip+redstone → wasEnabledLastTick=true + block STATE=true, chip alone idles, empty slot rejects). New `/artest terraforming terminal-info` + `terminal-load-chip <dim> <x> <y> <z> <satId>` probes. Out of scope: biome-mutation inner loop (battery/TerraformingHelper dependencies). | ✅ |
| [TASK-35](TASK-35-ar-fetch-two-bot-harness.md) | `/ar fetch` coverage — 2 testClient tests (`WorldCommandFetchTest`: self-fetch positive resolve→transfer→setPosition path, unknown-name negative `getPlayerByName==null` branch). Phase 0 plan reframed — no NetworkManager-stub real-EntityPlayerMP probe needed; self-fetch (bot fetching itself, name discovered via `artest player health`) closes the resolvable contract surface with no second-player infrastructure. Multi-player "moderator fetch" still out of scope (single-bot harness limit). | ✅ |
| [TASK-36b ext](TASK-36-terraforming-and-service-station-depth.md) | Service-station assembler-scan + no-progress-without-assembler — 2 server tests (`ServiceStationAssemblerScanTest`). New `/artest infra service-scan-assemblers` reflection probe (bypasses canPerformFunction's `worldTime % 20 == 0` gate that `tile force-tick` can't satisfy). Full repair-cycle with FORMED PrecisionAssembler multiblock still deferred (requires recipe-fixture infrastructure for wildcard machines — TASK-26 territory). | ✅ partial |
| [TASK-36b deep](TASK-36-terraforming-and-service-station-depth.md) | Full repair cycle with FORMED PrecisionAssembler multiblock — 1 server test (`ServiceStationFullRepairCycleTest`). Phase 1 pins consumePartToRepair (part moves from partsToRepair to partsProcessing on first powered performFunction); Phase 2 pins processAssemblerResult (with a "rocket"-named item injected into the assembler output port, the part is cleared from partsProcessing and restored at stage 0 in rocket storage). Reuses TASK-26 `/artest fixture machine precision-assembler` wildcard-overlay probe + new `/artest infra service-perform-function` probe; `service-state` extended with `partsProcessingCount`. | ✅ |
| [TASK-35 ext](TASK-35-ar-fetch-two-bot-harness.md) | Multi-client testClient harness + moderator-fetch — 1 testClient test (`WorldCommandFetchModeratorTest`). ForgeTestFramework `RealClientHarness.start(server, username)` overload + per-username `--username`/`--uuid` propagation (also fixes FG6 legacydev that previously generated random `Player###` names). New AR probes: `player exec-as-named`, `player position-of`, `player op-named`. testClient runs require `-PuseLocalFramework=true` until the framework change is published. | ✅ |
| [TASK-37](TASK-37-nuclear-engine-rocket-assembly.md) | Nuclear engine rocket-assembly thrust aggregation — 2 server tests (`NuclearEngineRocketAssemblyTest`) pinning IRocketNuclearCore cohesion check (core-above-motor → thrust>0; misplaced → NOENGINES). 2 new `/artest fixture rocket` variants. From audit Gap P. | ✅ |
| [TASK-38](TASK-38-mining-drill-rocket-assembly.md) | IMiningDrill rocket-assembly stat aggregation — 1 server test (`RocketAssemblerMiningDrillStatTest`) pinning placed drill → `stats.drillingPower > 0` chain. `with-mining-drill` fixture variant + `drillingPower` field on `rocket info`. From audit Gap Q. | ✅ |
| [TASK-39](TASK-39-satellite-terminal-chip-recognition.md) | TileSatelliteTerminal chip recognition + erase button — 4 server tests (`SatelliteTerminalChipRecognitionTest`) pinning status 0/1/3 ladder + destructive erase removes sat from dim properties + blanks chip NBT. New `/artest satellite-terminal {info\|load-chip\|press-erase}` subcommand group. From audit Gap R. | ✅ |
| [TASK-40](TASK-40-batch1-rocket-loader-railgun-analyser.md) | Batch 1 of 2026-05-27 audit close-out: Gap E (rocket item unloader active transfer — 1 server) + Gap A (railgun receiver-side cargo contract via `onReceiveCargo` — 1 server) + Gap D (TileAstrobodyDataProcessor chip-data increment from DataBus — 1 server). 7 new probe verbs (`rocket storage-item-fill`, `infra unloader-debug`, `infra railgun-receive-cargo`, `infra astrobody-{set-research\|load-chip\|chip-data}`, `infra databus-set-data`). Reshapes for D (asteroid chip not planet chip) and A (cargo transport not weapon firing) documented in task doc. | ✅ |
| [TASK-40b](TASK-40b-batch2-gascharge-areagravity.md) | Batch 2 of 2026-05-27 audit close-out: Gap F.2 (TileGasChargePad refills suit air — 1 testClient) + Gap C (TileAreaGravityController resets in-radius player fallDistance — 1 testClient). 2 new probe verbs (`player set-fall-distance`, `player get-fall-distance`). Reshape for Gap C: pin the unconditional fallDistance-reset (player-visible fall-damage immunity) instead of the audit's band-quality motion pin — strictly cheaper, pins the same gate. **testClient runs blocked in dev env** by Xvfb/GL context mismatch (existing testClient tests fail identically); code compiles, follows established pattern. | ✅ authored, harness-blocked |

## Backlog

Backlog promoted 2026-05-23 from full-repo audit findings. Each
entry is an actionable TASK with a defined plan + acceptance.

| ID | Title | Status | Blocker / trigger |
|---|---|---|---|
| [TASK-15](TASK-15-visual-regression.md) | Visual regression infrastructure for Minecraft client | ❌ Not planned | Closed 2026-05-29 — speculative infra with no live trigger and high build cost. Original 4 promotion triggers retained in task file; re-open via a new TASK if any fires. |
| [TASK-16](TASK-16-test-stability-flake-watch.md) | Test-stability flake watch — investigation deliverable. Three flake shapes root-caused; shape #3 mitigated in TASK-26 via kit retry; #1+#2 split into TASK-27; #4 (worldgen sampling) confirmed across 3 sightings, promoted to TASK-28 F7. | 🟡 Investigation complete | Investigation done 2026-05-23. |

## Conscious non-goals

Two audit findings are explicit **non-goals**, not gaps. They do
NOT get TASK files because they're deliberate decisions, not
deferred work:

- **Cross-session worldgen determinism** — same-seed-across-reboot
  histogram pins. Within-session determinism is covered by
  `WorldgenDeterminismAndSamplingTest`; cross-session adds
  significant fixture cost for a contract that's already
  implicitly preserved by Forge's chunk cache. Reopen only if a
  chunkgen change introduces a real cross-session divergence.
- **Rocket out-of-fuel mid-flight auto-explosion** —
  `RocketFlightFailureModesTest` deliberately pins the **current
  contract** ("no auto-explosion"). If production adds an
  explosion branch, the test flips polarity — no new task needed
  until then.

TASK-29 through TASK-36 promoted 2026-05-26 from the 2026-05-25
Tier 1/2/3 audit deferrals + 2026-05-26 audit out-of-scope list —
each prior free-form bullet is now an actionable TASK with
defined plan + blocker per `task-lifecycle.md`. Future deferrals
must land here as TASK files; free-form bullet lists in this
README are forbidden.

## Dependency graph

```
TASK-03 ──┬─► TASK-04  (multiblock)
          ├─► TASK-05  (items)        ─┐
          ├─► TASK-06  (missions)     ─┤── EntityPlayer paths
          ├─► TASK-07  (rocket cycle) ─┤   live in testClient e2e
          ├─► TASK-08  (ASM)           │   (TASK-10b)
          ├─► TASK-09  (satellite types)
          └─► TASK-10  (A2 tail + B3 grouping)

TASK-13 ✅ — independent (closed 2026-05-23)
TASK-14 ❌ — independent (closed Obsolete 2026-05-23)
TASK-15 ❌ — independent, closed Not planned 2026-05-29
TASK-16 👁 — independent, watches flake pattern from TASK-12 close-out

Audit-2026-05-23 backlog (all independent of each other):
TASK-17 — SSOT integrity (touches TASK-09 satellite tests)
TASK-18 — industrial machine powered-cycle (touches TASK-04 multiblocks)
TASK-19 — multiblock trio (touches TASK-04 + TASK-06 surfaces)
TASK-20 — hovercraft testClient (touches TASK-10b layer)
TASK-21 — /ar positives (extends TASK-11)
TASK-22 — UV-assembler depth (extends TASK-07 / TASK-06)
TASK-23 — sealdetector branches (extends TASK-10b Phase 7)
TASK-24 — SpaceArmor chest route (extends TASK-10b Phase 7)
```

## Conventions

All TASK-NN docs share a structure:

- **Ticket**: source, status, creation date.
- **Context**: what's currently uncovered + why it matters.
- **Implementation Plan** (or **Approach options** for Backlog tasks
  with multiple paths): phased; each phase ~2-5 h.
- **Technical Decisions** (for In Progress / Completed tasks): same
  `no production logic changes` rule as TASK-01 §15. New probe verbs
  documented inline.
- **Dependencies**: explicit `requires` / `does NOT block` calls.
- **Completion Checklist** (Completed tasks): per
  [`task-lifecycle.md`](../sops/development/task-lifecycle.md).
- **EOD marker**: in `.agent/.context-markers/` with the date and a
  short slug. The `.active` file points at the most recent marker
  for `/nav:start` to pick up.

## Bug-tracking pointer

Live bug tracking is OFF the README. Per
[`CLAUDE.md`](../../CLAUDE.md#bug-tracking--every-discovered-production-bug-must-be-logged):

- New bugs are pinned in the test suite (positive contract or
  `_documentsKnownBug`-style pinning of wrong behaviour) AND
  recorded in `.agent/history/known-bugs-ledger.md` under a new
  batch heading.
- The historical Batch #1 is drained; future bugs open Batch #2.
- The `_documentsKnownBug` suffix is no longer used in test method
  names (three javadoc breadcrumbs remain — see history file).
