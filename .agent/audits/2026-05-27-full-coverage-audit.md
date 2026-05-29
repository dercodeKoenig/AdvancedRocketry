# Full coverage audit — 2026-05-27

**Branch**: `feature/tests`
**Snapshot**: pyramid 839 (testUnit 288 / testIntegration 81 /
testServer 410 / testClient 60); backlog drained (only TASK-15
watching + TASK-16 flake journal remain).
**Methodology**: per `.agent/sops/development/testing-principles.md`
— contracts not impl pins. Every gap below is framed as a
**user-visible / API-visible** contract the test would assert, or
explicitly flagged as **impl-only ⇒ do not test**.

The litmus for every proposed pin in §3:

> "This test fails if production breaks the contract that ____."

If the blank reads like an impl detail, the proposal is rejected
inside this document, not deferred to the future agent.

---

## 1. Coverage status legend

| Status | Meaning |
|---|---|
| **Deep** | All player-visible / API-visible contracts of this mechanic have at least one positive pin; negative branches pinned where they materially differ. |
| **Partial** | Primary contract pinned; one or more secondary contracts (additional modes, error branches, persistence) unpinned. |
| **Shallow** | Only smoke / boot / registry-level coverage. The mechanic's behaviour under normal use is not pinned. |
| **None** | No test references this class / package. |
| **Impl-only** | Class exists but its public contract is exhausted by an aggregate (registry presence, NBT round-trip via a parent, etc.) — no separate test is warranted. |

---

## 2. Coverage matrix by subsystem

### 2.1 Rocket flight cycle

| Mechanic | Status | Where pinned |
|---|---|---|
| Pre-launch event cancellation | Deep | `RocketPreLaunchEventCancellationTest`, `RocketLaunchDepthTest` |
| Launch → flight transition | Deep | `RocketLaunchSmokeTest`, `RocketLaunchEventTest` |
| Orbit reached event | Deep | `RocketEventPayloadContractTest`, `RocketFlightCycleIntegrationTest` |
| De-orbiting event (`ticksExisted == 20` branch) | Deep | `RocketEventPayloadContractTest` (TASK-31) |
| Descent + ground-impact event | Deep | `RocketDescentLandingTest`, `RocketEventPayloadContractTest` |
| Dimension transition | Deep | `RocketDimensionTransitionTest` |
| Dismantle event | Deep | `RocketFlightCycleDepthTest`, `RocketEventPayloadContractTest` |
| Failure modes (no fuel / no destination) | Deep | `RocketFlightFailureModesTest` (also locks the "no auto-explosion" non-goal) |
| Station-deployed rocket (UV) class divergence | Deep | `UvAssemblerDivergesFromRocketAssemblerTest`, `UvAssemblerOutputEntityClassTest`, `UvAssemblerBoundsConstantsTest` (TASK-22) |
| Pad ↔ rocket linking persistence | Deep | `RocketInfrastructureLinkPersistenceTest`, `RocketInfrastructureSmokeTest` |

**Verdict**: nothing actionable. Six RocketEvent subtypes, two
rocket entity classes, all launch / descent / dimension /
dismantle paths pinned. Out-of-fuel-auto-explosion is a conscious
non-goal (README §"Conscious non-goals").

---

### 2.2 Multiblock machines

#### 2.2.1 Industrial recipe machines

| Machine | Assembly | Powered cycle | Recipe end-to-end |
|---|---|---|---|
| Arc Furnace | ✅ (TASK-26) | ✅ | `ArcFurnaceRecipeEndToEndTest` |
| Precision Assembler | ✅ (TASK-26) | ✅ | `PrecisionAssemblerRecipeEndToEndTest` |
| Crystallizer | ✅ | ✅ | `CrystallizerRecipeEndToEndTest` |
| Lathe | ✅ | ✅ | `LatheRecipeEndToEndTest` |
| Precision Laser Etcher | ✅ | ✅ | `PrecisionLaserEtcherRecipeEndToEndTest` |
| Rolling Machine | ✅ | ✅ | `RollingMachineRecipeEndToEndTest` |
| Cutting Machine | ✅ | ✅ | `CuttingMachineRecipeEndToEndTest` (via Machine domain suite) |
| Centrifuge | ✅ | ✅ | `CentrifugeRecipeEndToEndTest` |
| Electrolyser | ✅ | ✅ | `ElectrolyserRecipeEndToEndTest` |
| Chemical Reactor | ✅ | ✅ | `ChemicalReactorRecipeEndToEndTest` |

**Verdict**: Deep. Every TASK-18 / TASK-19 / TASK-26 machine has
assembly + at least one recipe end-to-end + power-drain pin via
`MachineRecipeEndToEndKit`.

#### 2.2.2 Heavy / exotic multiblocks

| Multiblock | Assembly | Powered cycle / behavioural |
|---|---|---|
| Atmosphere Terraformer | ✅ | ✅ (`TerraformerPoweredCycleOnArPlanetTest`, `…OnOverworldTest` — TASK-19) |
| Black Hole Generator | ✅ | ✅ (`BlackHoleGeneratorPoweredCycleTest` — TASK-19) |
| Beacon | ✅ | ✅ (`BeaconEnableCycleTest`, `BeaconLocationProbeSmokeTest` — TASK-19) |
| Warp Core / Warp Controller | ✅ | ✅ (`WarpControllerDepthTest`) |
| Orbital Laser Drill | ✅ | Partial (`OrbitalLaserDrillMultiblockTest` pins assembly + drill power; mining-mode dispatch impl-only) |
| Railgun | Partial | None pin firing — see §3 |
| Solar Array | ✅ | ✅ (`SolarArrayMultiblockTest`, `SolarPanelInsolationTest`) |
| Microwave Receiver | ✅ | ✅ (links to SatelliteMicrowaveEnergy) |
| Planet Analyser | ✅ | Shallow — see §3 |
| Area Gravity Controller | ✅ | Partial — see §3 + bug #3 (StationGravityController redstone default) |
| Space Elevator | ✅ | ✅ via `ElevatorCapsuleRideE2ETest` + `ElevatorCapsuleStateAndNbtTest` (TASK-30 Gap 3) |
| Observatory | ✅ | ✅ (`ObservatoryMultiblockTest`) |

#### 2.2.3 Single-block machines

| Machine | Status |
|---|---|
| Plate Press | Deep (TASK-25) |
| Fueling Station | Deep (`FuelingStationFuelsAdjacentRocketTest`) |
| Rocket Fluid Loader / Unloader | Deep (TASK-34 `FluidLoaderActiveTransferTest`) |
| Rocket Loader / Unloader (item) | Partial — redstone polarity pinned (`RocketLoaderRedstonePolarityTest`); active item transfer not pinned (see §3) |
| Satellite Builder | Deep (TASK-33) |
| Suit Workstation | Deep (`SuitWorkStationAssemblesSuitTest`); SpaceArmor CHEST route via TASK-24 |
| Terraforming Terminal | Deep (TASK-36a) |
| Rocket Service Station | Deep (TASK-36b base + ext + deep) |
| Rocket Monitoring Station | Deep (`MonitoringStationComparatorOverrideTest`, `RocketMonitoringStationLaunchTriggerTest`) |

---

### 2.3 Satellites

| Type | Lifecycle | Tick contract | Behavioural action |
|---|---|---|---|
| SatelliteOptical | ✅ | ✅ (DISTANCE dataType — TASK-29) | n/a (data-only) |
| SatelliteDensity | ✅ | ✅ (ATMOSPHEREDENSITY) | n/a |
| SatelliteMassScanner | ✅ | ✅ (MASS) | n/a |
| SatelliteComposition | ✅ | ✅ (COMPOSITION) | n/a |
| SatelliteOreMapping | ✅ | ✅ (non-SatelliteData accrual) | (battery-only) |
| SatelliteSpyTelescope | ✅ | ✅ (no-op tick defense) | n/a |
| SatelliteWeatherController | ✅ | ✅ (NBT TASK-09) | `ItemBiomeChangerSatelliteActionE2ETest` covers sibling action |
| SatelliteBiomeChanger | ✅ | ✅ | ✅ (`SatelliteTypeBehaviourTest`, `ItemBiomeChangerSatelliteActionE2ETest`) |
| SatelliteMicrowaveEnergy | ✅ | ✅ | ✅ (MicrowaveReceiver link) |
| SatelliteDefunct | ✅ (fallback pin) | n/a | `SatelliteRegistryFallbackTest` |

**Verdict**: Deep across the board. Bug #1 (registry returns
`null` instead of fallback) is ledgered + pinned.

---

### 2.4 Atmosphere / sealing / oxygen

| Mechanic | Status |
|---|---|
| 14 AtmosphereType subtypes (oxygen × pressure × heat) | Deep (`AtmosphereLogicTest` 11 + `AtmosphereOxygenSmokeTest` 6) |
| Custom AtmosphereType registry round-trip | Deep (`CustomAtmosphereTypeNbtRoundTripTest` — TASK-32) |
| AtmosphereHandler dim-change cache | Deep (`AtmospherePlayerEventE2ETest`) |
| SealableBlockHandler allow/ban list mutation | Deep (`SealableBlockHandlerTest`) |
| SealDetector dispatch (5 branches) | Deep (`SealDetectorDispatchTest` 10 + TASK-23) |
| Oxygen vent powered consumption | Deep (`OxygenVentRequiresFuelAndPowerTest`) |
| ItemSealDetector player message branches | Deep (`ItemSealDetectorPlayerMessagesE2ETest`) |
| TileCO2Scrubber, TileGasChargePad, TileAtmosphereDetector | **Shallow** — boot only — see §3 |

---

### 2.5 Items / armor / wearables

| Item class | Status |
|---|---|
| ItemSpaceArmor (helmet/leggings/boots) | Deep (TASK-05, TASK-10b, `SpaceArmorContractTest`, `SpaceArmorProtectionContractTest`) |
| ItemSpaceChest (CHEST route) | Deep (TASK-24) |
| ItemJetpack | Deep (`ArmorComponentContractTest`, `OxygenSuitClientStateE2ETest`) |
| ItemPressureTank | Deep (`ArmorComponentContractTest`, `ItemSpaceArmorUseFluidE2ETest`) |
| ItemUpgrade | **None** — see §3 |
| EnchantmentSpaceBreathing | Deep (`SpaceBreathingEnchantmentContractTest`) |
| ItemJackHammer | Deep (`JackHammerContractTest`) |
| ItemBeaconFinder | Deep (`ScannerDetectorItemContractTest`, `BeaconLocationProbeSmokeTest`) |
| ItemOreScanner | Deep (`ScannerDetectorItemContractTest`, `OreScannerRightClickClientE2ETest`) |
| ItemAtmosphereAnalzer | Deep (`ItemAtmosphereAnalzerReadoutE2ETest`) |
| ItemSealDetector | Deep (`ItemSealDetectorPlayerMessagesE2ETest`) |
| ItemBiomeChanger / ItemWeatherController | Deep (`SpecialPurposeItemContractTest` + satellite-action E2E) |
| ItemThermite | Deep (`SpecialPurposeItemContractTest` — burn-time) |
| ItemAsteroidChip / ItemPlanetIdentificationChip / ItemStationChip / ItemSatelliteIdentificationChip | Deep (`ChipNBTRoundTripTest`) |
| ItemSpaceElevatorChip | Deep (`ItemDataCarrierNBTRoundTripTest`) |
| ItemData / ItemMultiData | Deep (`ItemDataCarrierNBTRoundTripTest`) |
| ItemSatellite | Deep (TASK-33 press-build covers full constructor surface) |
| ItemPackedStructure | Partial (`ItemPackedStructureNbtRoundTripTest` — null-gate + hasSubtypes; full setStructure runtime requires profiler, deferred per TASK-32 3a) |
| ItemBasicLaserGun / PacketLaserGun | **None** — see §3 |
| ItemBlockFluidTank | Impl-only — covered transitively by `FluidTankNBTRoundTripsAcrossRestartTest` + `FluidTankStackedFillTest` |

---

### 2.6 Vehicle entities

| Entity | Status |
|---|---|
| EntityRocket | Deep (TASK-07 family) |
| EntityStationDeployedRocket | Deep (TASK-22) |
| EntityHoverCraft | Deep (TASK-20 — mount, dismount, throttle, idle-drift) |
| EntityElevatorCapsule | Deep (TASK-30 Gap 3 — 5 server + 2 client; bug #2 ledgered) |
| EntityLaserNode / EntityItemAbducted | **None** — see §3 (Orbital Laser Drill mining-mode dispatch) |
| EntityUIButton / EntityUIPlanet / EntityUIStar | Impl-only — visual-only UI entities; covered by GUI E2E (PlanetSelector, RocketBuilder, Guidance) |
| EntityDummy | Impl-only — test util |

---

### 2.7 Space stations

| Mechanic | Status |
|---|---|
| Station create / register / persist | Deep (`SpaceStationLifecycleSmokeTest`, `SpaceStationDepthTest`) |
| Dock / undock + pad persistence | Deep (`SpaceStationDockUndockTest`, `SpaceStationPadPersistenceTest`) |
| Altitude controller tick | Deep (TASK-30 `StationControllersTickContractTest`) |
| Gravity controller tick | Deep — workaround pin (bug #3) |
| Orientation controller tick | Deep (TASK-30) |
| Station-deployed rocket | Deep (TASK-22) |
| Monitoring station comparator | Deep (TASK-32) |
| Holographic planet selector tile | **Shallow** — boot only — see §3 |

---

### 2.8 Missions

| Mission | Status |
|---|---|
| MissionGasCollection | Deep (`MissionGasCompletionTest`, `MissionNbtRoundTripTest`) |
| MissionOreMining | Deep (`MissionOreCompletionTest`) |
| MissionResourceCollection | Deep (`MissionResourceCollectionContractTest`) |
| Mission infrastructure linking | Deep (`MissionInfrastructureLifecycleTest`) |
| Persistence across restart | Deep (`MissionPersistenceRestartTest`) |
| Mission pyramid (lifecycle) | Deep (`MissionLifecyclePyramidTest`) |

---

### 2.9 Cables / wireless / pipes

| Network | Status |
|---|---|
| EnergyNetwork (RF distribution) | Deep (`PipeNetworkHandlerDeepTest`, `PipeNetworkSmokeTest`) |
| LiquidNetwork | Deep (idem) |
| DataNetwork | Deep (idem) |
| WaterPipe | Impl-only — same handler class as LiquidPipe |
| CableNetwork id generation / merge / consolidate | Deep (`CableNetworkHandlerContractTest`, `PipeNetworkHandlerDeepTest` 20) |
| WirelessTransceiver | Deep (TASK-13 — 10 tests + persistence) |
| Power-flow routing across interconnected segments (split/merge under live load) | **Partial** — see §3 |

---

### 2.10 World commands `/ar` `/advancedrocketry`

| Surface | Status |
|---|---|
| Guard predicates | Deep (TASK-11 `WorldCommandGuardContractTest`) |
| Planet lifecycle verbs | Deep (`WorldCommandPlanetLifecycleContractTest`) |
| Planet set/get verbs | Deep (`WorldCommandPlanetSetGetContractTest`) |
| Star + misc verbs | Deep (`WorldCommandStarMiscContractTest`) |
| Player-equipped positives | Deep (TASK-21 `WorldCommandPlayerEquippedE2ETest`) |
| `/ar fetch` single-bot | Deep (TASK-35) |
| `/ar fetch` multi-client (moderator-fetch) | Deep (TASK-35 ext) |
| `/ar fillData` | Impl-only — covered transitively via satellite-construction probes |

---

### 2.11 Dimension / planet / weather / worldgen

| Mechanic | Status |
|---|---|
| DimensionProperties NBT + defaults + hierarchy | Deep (`DimensionPropertiesTest`, integration tier) |
| XMLPlanetLoader | Deep (unit + integration + `PlanetXmlConfigIntegrationTest`) |
| Per-dimension weather isolation | Deep (TASK-09 `PerDimensionWeatherIsolationTest`) |
| Non-AR dimension exclusion | Deep (`NonARDimensionIsolationTest`) |
| Weather persistence | Deep (`WeatherPersistenceTest`, `PlanetWeatherSavedDataTest`) |
| Weather sync to client | Deep (`WeatherClientSyncE2ETest`, `ARWeatherWorldInfoTest`) |
| Worldgen determinism (within-session) | Deep (`WorldgenDeterminismAndSamplingTest`) |
| Worldgen cross-session reboot determinism | **Non-goal** (README §"Conscious non-goals") |
| OreGen properties registry | Deep (`OreGenPropertiesTest`) |
| WorldProviderAsteroid + ChunkProviderAsteroid | **Shallow** — see §3 |
| MapGenSpaceVillage / Lander / Geode / Volcano / Ravine / InvertedPillar | **Shallow** (sampled by `WorldgenDeterminismAndSamplingTest`, not pinned individually) |
| 14 Biome subtypes | Impl-only — biome registry presence + worldgen sampling sufficient |
| PlanetaryTravelHelper (geostationary + transbody) | Deep (unit-tier — TASK-09 Gap 3) |
| AstronomicalBodyHelper orbital theta | Deep (integration + unit) |
| Stellar body / IGalaxy | Deep — covered via `/ar star` commands + `PacketStellarInfo` serialization |

---

### 2.12 Persistence / NBT / wire

| Mechanic | Status |
|---|---|
| Full server restart persistence | Deep (`PersistenceRestartSmokeTest`) |
| FluidTank NBT across restart | Deep (`FluidTankNBTRoundTripsAcrossRestartTest`) |
| SatelliteId chip persistence | Deep (`SatelliteIdChipPersistenceTest`) |
| All 18 PacketXxx round-trips | Deep (unit `PacketSerializationTest` 33 + integration 14) |
| DockingPort NBT + packet | Deep (`TileDockingPort` Gap 5 — 4 server) |

---

### 2.13 Event handlers / mixin / advancements

| Mechanic | Status |
|---|---|
| Event-handler wiring (registration sanity) | Deep (`EventHandlerWiringTest`, `PlayerEventHandlerWiringTest`) |
| MixinPlayerList / MixinWorldServerMulti / MixinEntityGravity / MixinWorldSetBlockState / MixinEntityPlayer(MP)InventoryAccess | Deep (`MixinHookBehaviourPinsTest` 6 + `InventoryBypassRedirectE2ETest` + `LowGravFallDamageE2ETest` + `RocketInventoryHelperRedirectTest`) |
| Advancement triggers (CustomTrigger) | Deep (`AdvancementsE2ETest`) |

---

### 2.14 Static / decoration blocks

These exist but have no behavioural surface beyond registry +
worldgen placement. Per SOP they are **impl-only**: pinning
"block exists in registry" duplicates the registry test; pinning
"breaks with pickaxe" duplicates vanilla Forge behaviour.

- `BlockCharcoalLog` / `BlockLightwoodLeaves` / `BlockLightwoodPlanks` /
  `BlockLightwoodWood` / `BlockLightwoodSapling` / `BlockRegolith` /
  `BlockTorchUnlit` / `BlockElectricMushroom` / `BlockLightSource` /
  `BlockLens` / `BlockThermiteTorch` / `BlockSeat` / `BlockDoor2` /
  `BlockCrystal` / `BlockQuartzCrucible`
- `WavefrontObject` + `Vertex` + `Face` + `TextureCoordinate` + `GroupObject` + `ModelFormatException` — backward-compat OBJ loader; rendering-only, no contract a caller depends on beyond vanilla Forge model pipeline.

These are explicitly excluded from §3 — adding pins would violate
the SOP's "tests verify contracts" rule (the only "caller" is the
vanilla renderer, and Forge tests that itself).

---

## 3. Identified gaps and proposed coverage

Every entry below: gap, **contract** (litmus completed), proposed
test shape, prerequisite probe extensions (if any), rough effort.
Anything failing the litmus is rejected inside this section — not
silently deferred.

---

### Gap A — Railgun firing contract

**Status today**: assembly pinned (`RailgunMultiblockTest`); the
firing surface — which produces the orbital projectile and debits
energy — is not pinned.

**Contract candidate**: "A formed + fully-powered Railgun, given a
target dimension token, emits the orbital firing event and
deducts > 0 RF from its battery."

**Litmus**: passes — orbital firing is a player-visible side
effect (chat message + target dimension state) and the energy
debit is a player-visible (GUI bar).

**Test shape**: 1 server test
`RailgunFiringContractTest.firedRailgunDeductsEnergyAndEmitsEvent` —
assemble railgun via existing `multiblock assemble railgun` probe,
preload battery, force-tick the firing path, assert battery
strictly decreased + target-dim flagged.

**Probe extension**: `/artest infra railgun-fire <dim> <x> <y> <z>
<targetDim>` reflection-bypass for the firing method (likely
gated by a `worldTime % N == 0` ticker as in `service-station`).

**Effort**: ~3 h. Single test, single probe verb.

**Rejected sub-pin**: "exact RF cost N per shot" — impl. The
contract is "battery strictly decreased", not the number.

---

### Gap B — Orbital Laser Drill mining-mode dispatch

**Status today**: assembly pinned. The three drill modes
(`MiningDrill`, `terraformingdrill`, `VoidDrill`) each dispatch via
`IMiningDrill` and produce `EntityItemAbducted` projectiles.

**Contract candidate**: "Setting drill mode = MINING and tick-firing
the drill produces an `EntityItemAbducted` entity over the target
column whose drop-table item matches the ore at that column."

**Litmus**: passes — the dropped item is what the player sees; mode
selection is user-driven via GUI.

**Test shape**: 2 server tests —
- `OrbitalLaserDrillModeDispatchTest.miningModeProducesAbductedItem`
- `OrbitalLaserDrillModeDispatchTest.terraformingModeReplacesBlock`
  (VoidDrill is a player-deletable-block sub-mode, sister contract
  to `MiningDrill` — second test gives mode-divergence pin without
  multiplying tests).

**Probe extension**: `/artest infra laserdrill-set-mode <dim> <x>
<y> <z> <mode>` + `/artest infra laserdrill-fire <dim> <x> <y>
<z>` (or extend the existing `multiblock force-tick` with a budget
big enough to satisfy the drill cooldown).

**Effort**: ~5 h. Drill is a fixture-heavy multiblock; probe writing
+ 2 tests + assemble harness reuse.

**Rejected sub-pin**: "VoidDrill mode shares branch X with
MiningDrill" — impl. Both modes are observable via different
outputs; that suffices.

---

### Gap C — Area Gravity Controller player effect

**Status today**: multiblock assembly + station-gravity controller
target-walk pinned. The player-effect side — that a player inside
the projected area receives modified gravity — is not pinned.

**Contract candidate**: "A formed AreaGravityController with
target = 0.5 applied to a player inside its projection radius
causes the player's fall-step distance over N ticks to fall
within the 0.5-gravity band, distinct from the 1.0-gravity
baseline."

**Litmus**: passes — fall speed is player-visible; LowGravFallDamage
E2E pins the sibling contract on dimension-level gravity, this is
the same shape on area-projected gravity.

**Test shape**: 1 testClient test
`AreaGravityControllerPlayerEffectE2ETest.playerInsideAreaFallsAtTargetGravity`
— mirror of `LowGravFallDamageE2ETest`, fallback to band-pin not
exact-distance.

**Probe**: existing — `multiblock assemble area-gravity-controller`,
`player position`, `player velocity-sample`.

**Effort**: ~4 h (testClient harness contention).

**Rejected sub-pin**: "exact pixel-distance per tick = X" — impl;
band-pin (0.4 < distance < 0.6) is the contract.

---

### Gap D — Planet Analyser scan output

**Status today**: assembly pinned (`PlanetAnalyserMultiblockTest`).
The output — a `SatelliteData` with the analysed planet's
properties — is not pinned end-to-end on the analyser side
(satellite-side scanning is pinned by TASK-29 instead).

**Contract candidate**: "A formed + powered PlanetAnalyser tick-fed
with a planet-id chip produces a `SatelliteData` slot output
matching the chip's planet's properties."

**Litmus**: passes — the player retrieves the output slot
contents.

**Test shape**: 1 server test
`PlanetAnalyserScanOutputContractTest.scanProducesSatelliteDataForChippedPlanet`.

**Probe extension**: `/artest infra planet-analyser-load-chip
<dim> <x> <y> <z> <planetId>` + reuse existing
`multiblock force-tick`.

**Effort**: ~3 h.

---

### Gap E — Rocket Loader / Unloader item active transfer

**Status today**: redstone polarity pinned (TASK-09 Gap 1
`RocketLoaderRedstonePolarityTest`). The actual item-transfer
side — loader pushes inventory into adjacent rocket, unloader
pulls — is not pinned. (Compare TASK-34: equivalent fluid surface
**is** pinned.)

**Contract candidate**: "An armed RocketLoader adjacent to a placed
rocket transfers > 0 items from its inventory into the rocket's
storage chunk under a real server tick; an armed RocketUnloader
drains > 0 items back into its own inventory."

**Litmus**: passes — player-visible cargo manifest.

**Test shape**: 2 server tests
`RocketItemLoaderActiveTransferTest.loaderPushesItemsIntoRocketStorage`
+ `…unloaderPullsItemsFromRocketStorage`.

**Probe**: `/artest rocket storage-item-fill <entityId> <slot>
<itemId> <count>` (mirror of TASK-34's `storage-fluid-fill`).

**Effort**: ~3 h.

**Rejected sub-pin**: "transfers exactly N items per tick" — impl.
Contract is "> 0 transferred over T ticks".

---

### Gap F — TileCO2Scrubber + TileGasChargePad + TileAtmosphereDetector + TilePump

**Status today**: Boot-only. These tiles exist in the
`tile/atmosphere/` package, register, and load, but none of their
behavioural verbs are pinned.

#### F.1 TileCO2Scrubber

**Contract candidate**: "A powered CO2 Scrubber inside a sealed
volume with an AtmosphereHighPressureNoOxygen reading converts the
volume's atmosphere to its breathable peer (or "lowers CO2",
depending on AR's actual semantic — needs production-code read
before phrasing the litmus blank)."

**Litmus**: passes IF the scrubber has an observable effect on the
atmosphere reading of an adjacent sealed volume. Needs a 10-min
prod read before authoring.

**Test shape**: 1 server test, gated by Phase 0 read of
`TileCO2Scrubber.update()`.

**Effort**: ~4 h including Phase 0.

#### F.2 TileGasChargePad

**Contract candidate**: "A player standing on a powered + filled
GasChargePad has their ItemPressureTank fluid amount increase tick
over tick."

**Litmus**: passes — tank fill is the player-visible
suit-readiness contract.

**Test shape**: 1 testClient test
`GasChargePadFillsPressureTankE2ETest.standingOnPadRefillsTank`.

**Probe**: existing — `player held-air-component-route` from
TASK-24 already exposes the FluidStack drain side; mirror to
`player held-pressure-tank-fluid`.

**Effort**: ~4 h (testClient harness).

#### F.3 TileAtmosphereDetector

**Contract candidate**: "A powered AtmosphereDetector with a chip
in its slot outputs the correct atmosphere reading on its display
GUI / comparator output."

**Litmus**: passes IF the detector has a comparator output (needs
prod read; if not, this is impl-only and drops).

**Effort**: ~3 h including Phase 0.

#### F.4 TilePump

**Contract candidate**: "A powered Pump adjacent to a water source
block fills its internal tank by > 0 mB per tick."

**Litmus**: passes — tank fill is player-visible.

**Test shape**: 1 server test
`TilePumpFillsAdjacentWaterSourceTest`.

**Effort**: ~2 h.

---

### Gap G — TileGuidanceComputer behavioural

**Status today**: GUI surface pinned (`GuidanceComputerGuiE2ETest`).
The non-GUI tile behaviour — guidance-target chip slot + redstone
output to monitoring station — is unpinned.

**Contract candidate**: "Loading a planet-id chip into a placed
GuidanceComputer makes the adjacent MonitoringStation comparator
output mirror the chip's planet's accessibility flag."

**Litmus**: passes — comparator output is observable.

**Test shape**: 1 server test
`GuidanceComputerChipDrivesMonitoringStationComparatorTest`.

**Probe**: reuse `infra monitor-info comparatorOverride` from
TASK-32; add `/artest infra guidance-load-chip`.

**Effort**: ~3 h.

**Rejection note**: do NOT add "GuidanceComputer GUI shows planet
X" — already covered by `GuidanceComputerGuiE2ETest`.

---

### Gap H — Hatches (TileInvHatch / TileDataBus / TileSatelliteHatch)

**Status today**: Boot-only.

**Verdict**: **impl-only** for InvHatch and DataBus — they are
generic I/O bus adapters whose contract is exhausted by the parent
multiblock recipe-end-to-end tests that already feed items / data
**through** them (Arc Furnace inputs/outputs go through InvHatch;
PrecisionAssembler wildcard fixture overlay places a hatch — see
TASK-26).

**Exception** — `TileSatelliteHatch` *might* warrant 1 pin if its
contract diverges from generic InvHatch. Needs Phase 0 prod read
(~30 min). If it's plain InvHatch + a satellite ID slot type, it's
impl-only (the slot validator is the contract; testable as a
unit-tier predicate pin).

**Effort if pursued**: ~2 h.

---

### Gap I — TileHolographicPlanetSelector

**Status today**: Boot-only.

**Contract candidate**: "Right-clicking a HolographicPlanetSelector
with a planet-id chip imprints the chip's planet onto the
selector's display target NBT, persisted across restart."

**Litmus**: passes — display target is what the player sees on the
holo.

**Test shape**: 1 server test
`HolographicPlanetSelectorChipImprintTest` + 1 line of NBT
persistence pin.

**Effort**: ~3 h.

---

### Gap J — ItemUpgrade

**Status today**: No tests. Class exists in
`item/components/ItemUpgrade.java`.

**Verdict pending Phase 0 (~20 min read)**: if ItemUpgrade is a
data-only carrier (NBT + slot eligibility), 1 unit test
mirroring `ArmorComponentContractTest`. If it has a behavioural
verb (applies bonus to host item), 1 server test pinning the
bonus.

**Effort**: ~2 h with Phase 0.

---

### Gap K — ItemBasicLaserGun + PacketLaserGun + EntityLaserNode + FxSkyLaser

**Status today**: No tests on the laser-gun mechanic. Packet
serialization is covered by `PacketSerializationTest` (round-trip
only).

**Contract candidate**: "Right-clicking with a charged ItemBasicLaserGun
spawns an EntityLaserNode entity at the player's eye position with
the correct direction vector and emits PacketLaserGun to nearby
clients."

**Litmus**: passes — the laser beam is player-visible.

**Test shape**: 1 testClient test
`LaserGunFireSpawnsLaserNodeE2ETest`.

**Probe**: `player held-laser-gun-fire`.

**Effort**: ~4 h.

**Caveat**: if ItemBasicLaserGun is an unfinished / unwired
feature (no recipe, no creative tab entry), this drops — verify
in Phase 0.

---

### Gap L — Force-field projector behavioural

**Status today**: `ForceFieldProjectionSmokeTest` — 1 smoke
boot only.

**Contract candidate**: "A powered ForceFieldProjector facing
direction D projects N BlockForceField blocks along D until an
obstacle or the configured range, and unpowers → removes them."

**Litmus**: passes — the field is player-visible (collision +
render).

**Test shape**: 2 server tests
- `ForceFieldProjectorProjectsAndRetractsTest.poweredProjectsField`
- `…unpoweringClearsField`

**Probe**: existing `multiblock force-tick` + `block-at` scan.

**Effort**: ~4 h.

**Rejected sub-pin**: "exact range = N blocks" — impl. Contract is
"projects > 0 + retracts on unpower".

---

### Gap M — BlockIntake / IIntake

**Status today**: No tests on intake mechanic. (Distinct from
TilePump.)

**Contract candidate**: depends on prod semantic — likely an
atmosphere-aware fluid intake (pulls atmospheric gas when over a
matching atmosphere).

**Verdict**: Phase 0 required. If it's a pure rocket-engine helper
(IRocketEngine reads `IIntake.canIntake(atmosphere)`), then it's
**impl-only** — the contract is already pinned via rocket
launch-on-atmosphere tests. If it's a placeable functional tile,
1 server test.

**Effort**: ~3 h with Phase 0.

---

### Gap N — WorldProviderAsteroid + ChunkProviderAsteroid

**Status today**: Worldgen sampling pins MoonDim only. Asteroid
dimension load is implicitly tested by `PlanetDimensionLoadTest`
but the chunk-provider's asteroid-cluster spawn density is not
pinned.

**Contract candidate**: "Loading the Asteroid worldprovider
dimension and walking N chunks produces > K asteroid stems"
(loose end-state pin per SOP).

**Litmus**: passes if and only if "asteroids exist" is the
player-visible contract (the dimension's defining feature). It is.

**Test shape**: 1 server test
`AsteroidDimensionContainsAsteroidsTest`.

**Effort**: ~3 h.

**Rejected sub-pin**: "exactly N asteroids per chunk" — impl
(chunkgen RNG); band-pin instead.

---

### Gap O — Cross-network routing under split / merge

**Dropped 2026-05-27 second-pass**: user explicitly removed cable
coverage from scope. Network split/merge routing is not pursued.

---

## 3a. Additional gaps surfaced by 2026-05-27 second-pass verification

Three parallel deep-grep agents re-cross-walked every Tile / Block /
Item / Entity / Satellite / Mission / Network / Mixin / Capability /
Atmosphere / Worldgen / API class against the test suite. Most of
their flagged "uncovered" classes failed the SOP litmus on
verification — they're catalogued in §8 with the rejection
rationale, so no future agent re-proposes them. Four new gaps
survived.

---

### Gap P — Nuclear rocket engine family — ✅ Shipped 2026-05-27 (TASK-37)


**Status today**: `BlockNuclearRocketMotor`, `BlockNuclearFuelTank`,
`BlockNuclearCore` are all registered in `AdvancedRocketry.java`
(creative tab + recipe-eligible) and consumed by
`TileRocketAssemblingMachine` + `TileUnmannedVehicleAssembler`
during rocket synthesis. The `IRocketNuclearCore` interface marks
a stat-differentiating component. None of the existing
rocket-assembly / launch / flight tests use any nuclear part.

**Contract candidate**: "A rocket assembled with a nuclear engine
stack produces a measurably different StatsRocket profile than the
same chassis built with chemical engines (thrust × fuel-mass
trade-off observable in pad/launch readouts)."

**Litmus**: passes — StatsRocket fields drive launch readiness,
ascent speed, and the pad-side GUI readout the player sees.

**Test shape**: 2 server tests
- `NuclearEngineRocketAssemblyTest.nuclearStackProducesDistinctStatsProfile`
- `NuclearEngineRocketAssemblyTest.nuclearRocketLaunchesAndReachesOrbit`
  (end-state pin: orbit reached, not specific tick count).

**Probe**: existing — `multiblock assemble rocket-assembler`,
`fixture rocket nuclear-stack` (new probe verb seeds the
nuclear-engine fixture variant). `rocket stats <entityId>` already
exists from TASK-07.

**Effort**: ~5 h (Phase 0 confirms wired nuclear recipe exists,
which initial grep already evidenced).

**Rejected sub-pins**: "exact thrust multiplier = N" — impl; the
contract is "profile differs", not the magnitudes. "Nuclear core
uses code branch X" — impl per SOP anti-patterns list.

---

### Gap Q — BlockMiningDrill (placeable single-block drill) — ✅ Shipped 2026-05-27 (TASK-38)

**Phase-0 outcome**: not a placeable functional drill — it's a
cargo-component block (no TileEntity) consumed by rocket assembly
via `IMiningDrill.getMiningSpeed` aggregation. Contract reframed
accordingly. See `.agent/tasks/TASK-38-mining-drill-rocket-assembly.md`.


**Status today**: registered as `AdvancedRocketryBlocks.blockDrill`
(creative-tab + non-zero hardness). Implements `IMiningDrill` and
is a `BlockFullyRotatable`. No test references the class. Distinct
from `TileOrbitalLaserDrill` (multiblock) — this is a placeable
single block.

**Contract candidate (pending Phase 0 read)**: "A placed
BlockMiningDrill facing direction D, powered, breaks the block
immediately in front of it within N ticks and emits the broken
block as a drop."

**Litmus**: passes IF the drill has the block-break behaviour
suggested by the IMiningDrill marker. A 10-minute Phase 0 read of
`BlockMiningDrill.java` confirms the verb shape before authoring.

**Test shape**: 1 server test
`BlockMiningDrillBreaksFrontBlockTest`.

**Probe**: existing `tile force-tick` + `block-at` scan, plus
maybe `/artest infra drill-power-on <dim> <x> <y> <z>` if the drill
gates on redstone signal.

**Effort**: ~3 h with Phase 0.

**Drop trigger**: if Phase 0 shows BlockMiningDrill is purely a
marker block (no `update()` / `breakBlock` logic), the contract
collapses to "block exists in registry" which is impl-only — drop
the gap.

---

### Gap R — TileSatelliteTerminal — ✅ Shipped 2026-05-27 (TASK-39)


**Status today**: lives in `tile/satellite/TileSatelliteTerminal.java`,
distinct from `TileTerraformingTerminal` (covered by TASK-36a) and
`TileSatelliteBuilder` (covered by TASK-33). No test references it.

**Contract candidate (pending Phase 0 read)**: most likely "a
SatelliteTerminal with a satellite-id chip loaded surfaces that
satellite's data/properties on its inventory/GUI module" — mirror
of how TerraformingTerminal works with the biome-changer chip.

**Litmus**: passes IF the terminal has a chip-recognition surface
analogous to TerraformingTerminal.

**Test shape**: 1 server test
`SatelliteTerminalChipRecognitionTest` (sister to TASK-36a's test).

**Probe**: extend existing `/artest satellite-builder` family with
`/artest satellite-terminal load-chip` or similar.

**Effort**: ~3 h with Phase 0.

**Drop trigger**: if the class is dead code (registered but
unreachable via gameplay) or its only contract overlaps 100% with
TerraformingTerminal, drop.

---

### Gap S — AreaBlob radius / max-blob enforcement

**Status today**: `api/AreaBlob.java` is the base for
`util/AtmosphereBlob.java` (oxygen sealing volumes). The blob's
`addBlock` / `removeBlock` surface is exercised transitively by
`AtmosphereOxygenSmokeTest` + `SealableBlockHandlerTest` +
`SealDetectorDispatchTest`. The **enforcement** of
`getBlobMaxRadius()` and `getMaxBlobs()` limits — i.e. that a
player who tries to seal a too-large volume gets a documented
failure mode — is not pinned.

**Contract candidate**: "An OxygenVent attempting to fill a
sealed volume that exceeds the configured max-radius does NOT
expand the blob beyond the cap, and the un-covered region remains
in vacuum/non-oxygen state."

**Litmus**: passes — sealed-volume size is a player-visible
config-level invariant (modpack tuning relies on it).

**Test shape**: 1 server test
`OxygenBlobMaxRadiusEnforcementTest.tooLargeVolumeLeavesUncoveredCellsInVacuum`.

**Probe**: existing — `infra oxygen-vent fill <dim> <x> <y> <z>`
+ existing `infra atmosphere-at <dim> <x> <y> <z>` for the
out-of-radius cell assertion.

**Effort**: ~4 h.

**Rejected sub-pin**: "exact cap N = configured constant" — impl;
the contract is "uncovered cell is uncovered", not the cap value.
The config value flows through `ARConfiguration` which is
separately pinned.

---

## 4. Out of scope (do not test)

Captured here so the next agent doesn't propose any of these.

- **Cables / pipe networks** — user explicitly removed from scope
  on 2026-05-27 second pass. Existing `PipeNetworkHandlerDeepTest`
  + `PipeNetworkSmokeTest` + `CableNetworkHandlerContractTest`
  remain in place; no further depth.
- **Rocket out-of-fuel auto-explosion** — README non-goal; current
  contract pinned is "no explosion".
- **Cross-session worldgen reboot determinism** — README non-goal.
- **Block-on-block decoration variants** (lightwood, regolith,
  torch-unlit, electric mushroom, etc.) — no caller-observable
  contract beyond registry.
- **OBJ model loader** (`backwardCompat/`) — vanilla Forge
  rendering pipeline owns the contract.
- **Particle Fx classes** (FxLaser, FxLaserHeat, FxLaserSpark,
  TrailFx, RocketFx, etc.) — visual-only; SOP §"What does NOT count
  as a contract" applies.
- **UI helper entities** (EntityUIButton, EntityUIPlanet,
  EntityUIStar) — covered transitively by GUI E2E tests.
- **EntityDummy** — test util.
- **Exact RF / mB / item-count magic numbers** in any of the gaps
  above — impl per SOP. Use band / non-zero pins.
- **Internal loop bounds, internal helper dispatch, private field
  names** — impl per SOP.

---

## 5. Effort summary and recommended landing order

Total proposed: **17 actionable gaps** (A–N from first pass minus
dropped Gap O cables, plus P–S from second pass) = roughly
**50 h** of work, all contract-shaped per litmus. Second-pass
verification added 4 contracts (P/Q/R/S) and explicitly rejected
~70 second-pass false positives (catalogued in §8).

| Order | Gap | Effort | Justification for ordering |
|---|---|---|---|
| 1 | E — Rocket loader item active transfer | 3 h | Closes a TASK-09/TASK-34 asymmetry (fluid covered, items not). Highest contract value. |
| 2 | A — Railgun firing | 3 h | Closes the only multiblock with assembly-but-not-behaviour pin. |
| 3 | B — Orbital Laser Drill mode dispatch | 5 h | Closes the largest remaining behavioural gap on a primary mechanic. |
| 4 | D — Planet Analyser scan output | 3 h | Cheap follow-up to (B); same probe infra. |
| 5 | G — GuidanceComputer chip drives comparator | 3 h | Reuses TASK-32 `monitor-info` probe. |
| 6 | I — HolographicPlanetSelector chip imprint | 3 h | Cheap NBT pin; high test/effort ratio. |
| 7 | F.2 — GasChargePad refills pressure tank | 4 h | Player-visible suit-readiness contract; reuses TASK-24 probes. |
| 8 | F.4 — TilePump fills from water source | 2 h | Cheap; trivial contract surface. |
| 9 | L — ForceFieldProjector projects + retracts | 4 h | First behavioural pin on force-field family. |
| 10 | C — AreaGravityController player effect | 4 h | testClient — bundle with other client-tier work. |
| 11 | J — ItemUpgrade | 2 h | Cheap once Phase 0 confirms shape. |
| 12 | F.1 — CO2 Scrubber | 4 h | Needs Phase 0 read. |
| 13 | F.3 — AtmosphereDetector | 3 h | Needs Phase 0 read. |
| 14 | K — Laser gun firing | 4 h | Verify wired feature in Phase 0 before authoring. |
| 15 | H — Satellite hatch divergence | 2 h | Drop after Phase 0 if it's plain InvHatch. |
| 16 | M — BlockIntake | 3 h | Phase 0 may collapse this to impl-only. |
| 17 | N — Asteroid dimension density | 3 h | Worldgen — lower priority. |
| — | ~~O — Cable live-split routing~~ | ~~5 h~~ | **Dropped 2026-05-27** — user removed cables from scope. |
| 19 | P — Nuclear engine rocket assembly | 5 h | Surfaces a fully-wired engine family with zero test coverage. Reuses TASK-07 launch infra. |
| 20 | Q — BlockMiningDrill placeable single-block | 3 h | Phase 0 may collapse to impl-only. |
| 21 | R — TileSatelliteTerminal chip recognition | 3 h | Mirror of TASK-36a TerraformingTerminal; trivial after Phase 0. |
| 22 | S — Oxygen blob max-radius enforcement | 4 h | Closes the only AreaBlob/IBlobHandler contract not pinned transitively. |

A reasonable batching strategy mirrors the TASK-18 / TASK-19 /
TASK-26 batches the team has already run: **(1)+(2)+(D)** as one
batch (probe additions overlap), **(F.2)+(C)** as a testClient
batch, **(F.1)+(F.3)+(F.4)+(J)+(H)+(M)** as a Phase-0-heavy
single-batch "atmosphere/items follow-up", **(L)+(K)** as a
visual-effect / firing pair, **(N)+(O)** as low-priority watch-
list entries (could land in Backlog table rather than as TASKs).

---

## 6. Bug-ledger pointer

No new live bugs surfaced during this audit. The 3 ledgered bugs
(README §"Current state") all have either positive-contract pins
or workaround pins:

1. `SatelliteRegistry.getNewSatellite` null-instead-of-fallback —
   pinned by `SatelliteRegistryFallbackTest._documentsKnownBug`
2. `EntityElevatorCapsule.setStandTime(int)` ignores argument —
   ledger-only (masked at single call site)
3. `TileStationGravityController` constructor missing redstone-OFF
   default — workaround pin in `StationControllersTickContractTest`
   gravity branch

---

## 7. Methodology notes for next agent

- Every proposed test above has the litmus blank completed in
  prose. Before authoring any of them, re-read
  `.agent/sops/development/testing-principles.md` and confirm the
  blank still reads as a contract on a fresh reading. If it now
  reads like an impl detail, drop or reshape — don't write the test.
- Phase 0 prod reads (Gaps F.1, F.3, H, J, K, M) MUST happen
  before authoring. Multiple proposed gaps may collapse to
  impl-only after a 20-min read.
- TASK files for any of these should follow `task-lifecycle.md`
  (creation date + status + dependencies + non-goals). Bundle by
  the batching strategy in §5 rather than one-task-per-gap.
- This document is a snapshot at HEAD `c3cf8cc7`. Re-run the
  audit (or extend this file with a `## Re-audit YYYY-MM-DD`
  section) after any of the bundles above land.

---

## 8. Second-pass rejection catalogue

The 2026-05-27 verification spawned three parallel Explore agents
that re-grepped every production class against the test suite.
They surfaced ~80 candidates flagged as "uncovered" or "partial".
After applying the SOP litmus, **only 4 survived** (Gaps P–S in
§3a). The rest are recorded here so a future agent does not
re-propose them.

### 8.1 Agents-claimed-uncovered, actually COVERED

The agents missed existing pins. Verified via grep:

- **`ItemJetpack`** / **`ItemPressureTank`** — covered by
  `ArmorComponentContractTest` (slot eligibility + onComponentAdded)
  and `OxygenSuitClientStateE2ETest` /
  `ItemSpaceArmorUseFluidE2ETest` (fluid drain).
- **14 `Atmosphere*` subtypes** — covered by `AtmosphereLogicTest`
  (11 tests pinning oxygen / pressure / heat damage branches) +
  `AtmosphereOxygenSmokeTest` (6 tests) +
  `SpaceArmorProtectionContractTest`.
- **All 7 Mixins** (`MixinPlayerList` / `MixinWorldServerMulti` /
  `MixinEntityGravity` / `MixinWorldSetBlockState` /
  `MixinEntityPlayer(MP)InventoryAccess` / `AccessorWorld`) —
  covered by `MixinHookBehaviourPinsTest` (6) +
  `InventoryBypassRedirectE2ETest` + `LowGravFallDamageE2ETest` +
  `RocketInventoryHelperRedirectTest`.
- **`CapabilitySpaceArmor`** — covered by
  `SpaceArmorProtectionContractTest`.
- **`TankCapabilityItemStack`** — covered by
  `ItemSpaceArmorUseFluidE2ETest` +
  `ItemSpaceChestSubInventoryDrainE2ETest`.
- **`EnchantmentSpaceBreathing`** — covered by
  `SpaceBreathingEnchantmentContractTest` (7).
- **`CustomTrigger`** + **`ARAdvancements`** — covered by
  `AdvancementsE2ETest` (4) end-to-end.
- **`PacketStorageTileUpdate`** + all 17 other Packet classes —
  covered by `PacketSerializationTest` (33 unit + 14 integration).
- **`BlockLandingPad`** — covered transitively by
  `RocketInfrastructureSmokeTest` + `RocketInfrastructureLinkPersistenceTest`
  (pad↔rocket linking IS the pad's contract).
- **`BlockStationModuleDockingPort`** — covered by
  `SpaceStationDockUndockTest` + `SpaceStationPadPersistenceTest`.
- **`BlockSeal`** / **`TileSeal`** — covered by
  `SealableBlockHandlerTest` + `SealDetectorDispatchTest` +
  `AtmosphereOxygenSmokeTest`.
- **`BlockFuelTank`** / **`BlockBipropellantFuelTank`** /
  **`BlockOxidizerFuelTank`** / **`BlockPressurizedFluidTank`** —
  the fluid-storage contract is covered by
  `FluidTankNBTRoundTripsAcrossRestartTest` +
  `FluidTankStackedFillTest`. Per-variant capacity numbers are
  impl per SOP.
- **`BlockSolarGenerator`** — `TileSolarPanel` powers it; covered
  by `SolarPanelInsolationTest`.
- **`BlockForceField`** / **`BlockForceFieldProjector`** — covered
  by `ForceFieldProjectionSmokeTest` (deeper projection contract
  is Gap L in §3, not a new gap).
- **`BlockWarpController`** / **`BlockWarpCore`** — covered by
  `WarpControllerDepthTest` (10).
- **`BlockBeacon`** — covered by `BeaconEnableCycleTest` +
  `BeaconMultiblockTest` + `BeaconLocationProbeSmokeTest`.
- **`BlockOrbitalLaserDrill`** — covered by
  `OrbitalLaserDrillMultiblockTest` (deeper mode dispatch is Gap B
  in §3).
- **`BlockAtmosphereTerraformer`** / **`BlockTileTerraformer`** —
  covered by `TerraformerMultiblockTest` +
  `TerraformerPoweredCycleOnArPlanetTest` +
  `TerraformerPoweredCycleOnOverworldTest`.
- **All `Block*RocketMotor` variants** (basic / advanced /
  bipropellant / advanced-bipropellant) — covered transitively via
  `StatsRocketTest` (12 unit) + `RocketLaunchEventTest` (4) —
  per-block thrust constants are impl; the rocket-stat contract is
  pinned.
- **`TileRocketAssemblingMachine`** / **`TileUnmannedVehicleAssembler`** /
  **`TileStationAssembler`** — covered by `RocketAssemblySmokeTest`
  (9) + `UvAssemblerDivergesFromRocketAssemblerTest` +
  `UvAssemblerBoundsConstantsTest` + `UvAssemblerOutputEntityClassTest`
  (TASK-22).
- **`TileRocketFluidLoader`** / **`TileRocketFluidUnloader`** —
  covered by `FluidLoaderActiveTransferTest` (TASK-34).
- **`TileSuitWorkStation`** — covered by
  `SuitWorkStationAssemblesSuitTest` + TASK-24 chest route.
- **`TileBrokenPart`** — covered by
  `ServiceStationBrokenPartScanContractTest` (TASK-36b) and
  `ServiceStationFullRepairCycleTest` (TASK-36b deep).

### 8.2 Agents-claimed-gap, actually IMPL-only per SOP

These would violate the SOP "tests verify contracts" rule if
authored:

- **`TileEntitySyncable`** (base class) — sync mechanism is the
  parent of every Tile in the codebase. The contract is pinned
  every time any descendant tile NBT round-trips through a server
  restart — adding a base-class isolation test duplicates without
  contract.
- **All 14 `BiomeGen*` per-biome individual pins** — per SOP
  "Anti-patterns from past audits", the contract is "sampling is
  deterministic + the biomes register". Pinning each biome's
  top-block / spawn-list is impl-pin parade; per-biome contracts
  would only matter if any one biome's *player-visible feature*
  diverges, and that's already implicit in
  `WorldgenDeterminismAndSamplingTest`.
- **All 15 `MapGen*` per-structure individual pins** — same
  reasoning. Sampled by `WorldgenDeterminismAndSamplingTest`. A
  per-structure shape-pin asserts impl.
- **5 `WorldGen*` tree generators** — invoked by biomes. The
  player-visible contract ("trees appear in the alien forest
  biome") is already sampled. Per-class shape pins are impl.
- **All 10 `Recipe*` classes as unit tests** — each is covered
  end-to-end by the matching `*RecipeEndToEndTest`. A unit-level
  "Recipe matches given inputs/outputs" test asserts the very
  thing the end-to-end test runs through; the redundancy is
  noise.
- **All GUI / Container / Module classes** (`GuiHandler`,
  `GuiPlanetButton`, `GuiOrbitalLaserDrill`, `GuiSpySatellite`,
  `GuiOreMappingSatellite`, `GuiProgressBarContainer`,
  `ContainerOrbitalLaserDrill`, `ContainerOreMappingSatellite`,
  `ContainerTypes`, `ModuleButtonPlanet`, `ModulePlanetImage`,
  `ModuleStellarBackground`, `ModuleOreMapper`, `ModuleData`,
  `ModuleAutoData`, `ModuleBrokenPart`, `SlotData`) — render
  layer. SOP §"What does NOT count as a contract" applies. The
  GUI behavioural contracts are covered by the testClient e2e
  family (`RocketBuilderGuiE2ETest`, `PlanetSelectorGuiE2ETest`,
  `GuidanceComputerGuiE2ETest`, etc.) which assert
  player-observable outputs, not module internal state.
- **`Constants`** (api/) — registry constants.
- **`MaterialGeode`** — material assigned to geode blocks; the
  contract is "geode block has correct material" = registry.
- **`ChunkManagerPlanet`** — impl detail of WorldProvider.
- **`DimensionCompat`** — single static helper that returns a
  config value (`getDefaultSpawnDimension()`). The config value
  itself is pinned by `ARConfigurationTest`.
- **`EntityDummy`** — test util.
- **`EntityLaserNode`** in isolation — covered by Gap B
  (Orbital Laser Drill mining-mode dispatch) where the laser node
  is the player-visible side effect.
- **`EntityItemAbducted`** — same as EntityLaserNode, covered as
  Gap B's output entity.
- **`BlockRedstoneEmitter`** / **`BlockTileRedstoneEmitter`** —
  Forge `IBlockState` redstone API. The contract is "comparator
  reads the override value" — covered by
  `MonitoringStationComparatorOverrideTest` (TASK-32).
- **`BlockTransciever`** — wrapper around
  `TileWirelessTransciever`; covered by TASK-13 (11 tests).
- **`BlockRocketFire`** — visual-only flame block. No
  player-visible contract beyond render.
- **`BlockIntake`** — assessed in Gap M; Phase 0 likely confirms
  impl-only (interface marker for rocket engine fuel-air mix).
- **`BlockSmallPlatePress`** — covered by TASK-25.
- **`BlockHalfTile`** / **`BlockLinkedHorizontalTexture`** /
  **`BlockTileNeighborUpdate`** / **`BlockTileWithMultitooltip`** —
  base / mixin block classes. Behaviour is in concrete
  descendants.
- **All decoration blocks** (`BlockSeat`, `BlockDoor2`,
  `BlockQuartzCrucible`, `BlockLens`, `BlockLightSource`,
  `BlockThermiteTorch`, `BlockCrystal`, `BlockElectricMushroom`,
  `BlockCharcoalLog`, `BlockRegolith`, `BlockEnrichedLava`,
  `BlockFluid`, `BlockTorchUnlit`, lightwood family) — already
  catalogued in §2.14.
- **All event handler classes in isolation**
  (`PlanetEventHandler`, `RocketEventHandler`, `WorldEvents`,
  `EntityEventHandler`, `CableTickHandler`, `BlockBreakEvent`) —
  event dispatch logic IS the wiring + the downstream effect.
  Wiring pinned by `EventHandlerWiringTest` +
  `PlayerEventHandlerWiringTest`. Downstream effects pinned by
  the dozens of behavioural tests that fire the events. A
  per-handler isolation test asserts the wiring twice.
- **`DataStorage`** — covered transitively by
  `ItemDataCarrierNBTRoundTripTest` +
  `ScanningSatelliteTickContractTest` + `ItemData` /
  `ItemMultiData` flows that round-trip through it.
- **`WorldTypeSpace`** / **`WorldTypePlanetGen`** — covered by
  every server boot test that loads AR dimensions + the `/ar`
  planet lifecycle suite.
- **`CrystalColorizer`** (mentioned by agent) — render utility.
- **`watersourcelocked`** — covered by `DimensionPropertiesTest`
  + `XMLPlanetLoaderTest` (XML-loaded property).
- **`AdvancedRocketryPlugin`** / **`asm/ModContainer`** — mixin
  bootstrap. Covered by the very act of mixin-pin tests passing
  (if the plugin failed to load, all mixin pin tests would fail
  loudly).
- **`TileInvHatch`** / **`TileDataBus`** — already addressed as
  Gap H IMPL-ONLY rationale (covered transitively as inputs to
  multiblock recipe-end-to-end tests).
- **`Capability` factory holders** (`CapabilityProtectiveArmor`,
  `CapabilitySpaceArmor` registration sides) — capability
  registration is covered by the very fact that capability
  consumers work; isolating the holder asserts impl.

### 8.3 Net result of second pass

- **First pass found**: 15 actionable gaps.
- **Dropped by user**: 1 (Gap O cables).
- **Second pass added**: 4 (Gaps P, Q, R, S).
- **Second pass rejected**: ~70 false positives (catalogued
  above so they don't get re-proposed).
- **Final actionable count**: 17 gaps (A–N + P + Q + R + S).
- **Confidence statement**: after the second pass, every Tile /
  Block / Item / Entity / Satellite / Mission / Network packet /
  Mixin / Capability / Atmosphere subtype / Mission / Recipe /
  Worldgen / GUI / Event handler / Command class in the
  production tree has been individually grepped against the test
  suite and assigned one of {COVERED, PARTIAL→listed in §3/§3a,
  IMPL-only→rejected here, or out-of-scope per §4 / §2.14}.

**End of audit.**
