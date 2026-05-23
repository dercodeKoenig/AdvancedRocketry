# TASK-05: Item-behaviour suite (hovercraft / jackhammer / suit / scanners / chips)

## Ticket

- Source: TASK-03 EOD audit (2026-05-19) — `item/` has 21 production
  classes, ~0 isolated test files (only `ItemAirUtilsTest` for a static
  utility). `SpaceArmorProtectionContractTest` covers part of suit
  armor logic at unit tier but nothing else.
- Status: ✅ Completed partial — unit-tier surface for 12 of 21 classes shipped; player-tier remainder absorbed by TASK-10b Phase 7. See `.agent/tasks/README.md` Done table.
- Created: 2026-05-19
- Predecessor: `.agent/.context-markers/2026-05-19-1230_task03-A-and-B-mostly-done-eod.md`

## Context

Items are ~25 % of mod surface and 0 % of isolated coverage today.
Specifically untested:

| Item class | What's untested |
|---|---|
| `ItemHovercraft` | Entity spawn from item-use, ride mechanics |
| `ItemJackHammer` | Mining speed multiplier, durability decrement, recipe |
| `ItemSpaceArmor` (289 LoC) | Air-tank consumption rate, IFillableArmor compliance, component-slot install |
| `ItemSpaceChest` (287 LoC) | Capacity per slot, NBT preserve-on-death |
| `ItemAtmosphereAnalzer` | Tick scan of current dim → produces correct atmosphere descriptor |
| `ItemBeaconFinder` | Beacon lookup + arrow display |
| `ItemSealDetector` | Sealed-room walker correctness |
| `ItemPlanetIdentificationChip` | Dim id round-trip (partly covered by TASK-03 A1 indirectly) |
| `ItemStationChip` | Station UUID round-trip, landing-pad coords |
| `ItemAsteroidChip` | Asteroid mission state |
| `ItemSatelliteIdentificationChip` | Satellite id round-trip |
| `ItemSpaceElevatorChip` | Station-elevator binding |
| `ItemThermite` | Block-melt mechanic |
| `ItemBiomeChanger` | Biome paint on right-click |
| `ItemOreScanner` | Ore scan output format |
| `ItemWeatherController` | Per-dim weather override (intersects B1 weather chain) |
| `ItemData` / `ItemMultiData` | Data-stick NBT carrier |
| `ItemPackedStructure` | Structure-paste mechanic |
| `ItemBlockCrystal` / `ItemBlockFluidTank` | Item-form of block-with-NBT |

Out of scope: GUI rendering tests for these items.

**No production logic changes** (same rule as TASK-01 §15).

## Implementation Plan

### Phase 1: Critical-path chip items (~4-5 h)

`ItemPlanetIdentificationChip`, `ItemStationChip`, `ItemAsteroidChip`,
`ItemSatelliteIdentificationChip` — all carry NBT that production reads
in launch/landing paths. Tests should round-trip via direct NBT
manipulation (no in-world placement needed).

- [ ] Unit tests for each chip's NBT round-trip:
  - `chipDimIdWriteReadCycle`
  - `chipDimIdInvalidPlanetSentinelHandled`
  - `chipPersistsAcrossItemStackCopy` (covers vanilla item-stack
    duplication paths).
- [ ] `ItemStationChip.getUUID` / `setUUID` round-trip with edge
  cases (negative UUID, max long).
- [ ] `ItemSatelliteIdentificationChip` with the `SatelliteRegistry`
  integration.

### Phase 2: Suit / armor / chest (~4-5 h)

- [ ] `ItemSpaceArmor`:
  - Tank capacity matches the NBT-stored capacity tag.
  - Tank decrement per `IFillableArmor.useFluid` call.
  - Component slot install/uninstall round-trip.
  - Damage absorption matches `damageReductionAmount`.
- [ ] `ItemSpaceChest`:
  - Capacity slots; reject overflow.
  - Death-persist (the NBT branch that survives respawn).
  - Component-mount mirror of `ItemSpaceArmor`.

### Phase 3: Scanner / detector items (~3-4 h)

- [ ] `ItemAtmosphereAnalzer` — tick a held item in a vacuum dim vs
  Earth → assert different output strings / output ItemStack tags.
- [ ] `ItemBeaconFinder` — place beacon, hold finder → finder NBT
  records beacon pos.
- [ ] `ItemSealDetector` — place sealed room (closed door + walls) vs
  un-sealed → detector reports correctly.
- [ ] `ItemOreScanner` — scan area with known fixture ore distribution
  → assert output histogram.

### Phase 4: Entity-spawning items (~2-3 h)

- [ ] `ItemHovercraft` — right-click on grass spawns `EntityHovercraft`
  (already partially tested via `HovercraftEntitySmokeTest` placement;
  this extends to item-use path).
- [ ] `ItemJackHammer` — break a block via the production
  `Item.onBlockStartBreak` chain; assert correct mining speed.
- [ ] `ItemThermite` — right-click melts target block per
  `meltableBlocks` config.

### Phase 5: Special-purpose items (~2-3 h)

- [ ] `ItemBiomeChanger` — right-click on grass changes biome (production
  `BiomeHandler.changeBiome` integration).
- [ ] `ItemWeatherController` — uses the existing `/artest weather`
  probe surface; verify item-action mirrors set-weather.
- [ ] `ItemData` / `ItemMultiData` — generic NBT carrier round-trip.

### Phase 6: Validation + EOD (~1 h)

- [ ] Full pyramid PASS.
- [ ] EOD marker with per-item coverage map.

## Technical Decisions

- **Most chip / data items can be unit-tested** — they're pure NBT
  carriers without world dependency. Use `@BeforeClass MinecraftBootstrap.ensure()`
  pattern (mirrors `XMLPlanetLoaderTest`).
- **World-interaction items** need server-tier with `AbstractSharedServerTest`:
  Hovercraft, JackHammer, BiomeChanger, scanners.
- **Item NBT tests use** `new ItemStack(...)` directly — no player
  needed. Items that need `EntityPlayer.getHeldItem()` / on-use paths
  belong in the **testClient** e2e harness (proposed TASK-10b), not
  here. Do NOT introduce a FakePlayer.

## Dependencies

**Requires**: TASK-03 base.
**Cross-cuts**: items with EntityPlayer interaction live in testClient
e2e (TASK-10b); this task covers the NBT / world-interaction surface
only.
**Does NOT block**: feature work.

## Estimated effort

~16-20 hours across 5-6 sessions.

## Completion Checklist

- [x] Chips (5 classes via `ChipNBTRoundTripTest` + 2 data-carriers via
      `ItemDataCarrierNBTRoundTripTest`): Planet/Station/Asteroid/Satellite/
      SpaceElevator chips + ItemData + ItemMultiData — 24 unit tests, plus
      `SatelliteIdChipPersistenceTest` at server tier.
- [x] Suit / chest (2 classes via `SpaceArmorContractTest` +
      `SpaceArmorProtectionContractTest`): unit-tier surface covered —
      slot gate, protectsFromSubstance matrix, empty-stack contracts,
      airRemaining default, module-slot accept/reject. Player-tier
      (useFluid decrement on damage, damage absorption, death-persist) →
      [[TASK-10b]] Phase 7.
- [x] Scanners / detectors (2 unit-feasible classes via
      `ScannerDetectorItemContractTest` + 1 server-tier via
      `SealDetectorDispatchTest`): BeaconFinder slot-gate, OreScanner
      satellite-id NBT round-trip + GUI metadata, SealDetector dispatch
      matrix via new `/artest seal-detector check` probe (8 server
      tests). AtmosphereAnalzer + SealDetector onItemUse player paths →
      [[TASK-10b]] Phase 7.
- [x] Entity-/tool-items: JackHammer pure-fn (6 unit tests in
      `JackHammerContractTest`) + Thermite burn-time (2 in
      `SpecialPurposeItemContractTest`). Hovercraft item-use entity-spawn
      → [[TASK-10b]] Phase 7.
- [x] Special-purpose items (3 classes via
      `SpecialPurposeItemContractTest`): BiomeChanger / WeatherController
      i18n inventory name + container openable + wire→NBT round-trip;
      Thermite burn-time. Right-click → satellite.performAction paths →
      [[TASK-10b]] Phase 7.
- [x] Items requiring real EntityPlayer cross-linked to [[TASK-10b]]
      Phase 7.
- [x] Full pyramid PASS — testUnit ALL GREEN; new server suite
      green.

## Status (2026-05-21)

**✅ Completed — unit-tier scope.** 12 of 21 item classes have isolated
contract coverage at the unit / server tier without FakePlayer
scaffolding. ~48 new contract pins shipped, +1 production bug pinned
as `_documentsKnownBug` (ItemSpaceElevatorChip.setBlockPositions
wrong-key removal — see `tasks/README.md`).

**Deferred to [[TASK-10b]] Phase 7 (player-tier remainder):**

| Item | Player-tier surface |
|---|---|
| `ItemAtmosphereAnalzer` | static `<clinit>` via LibVulpes proxy + onItemRightClick atmosphere readout |
| `ItemSealDetector` | full onItemUse player.sendMessage dispatch |
| `ItemHovercraft` | item-use entity-spawn path |
| `ItemSpaceArmor` | useFluid decrement, damage absorption per LivingDamageEvent |
| `ItemSpaceChest` | death-persist (player respawn cycle) |
| `ItemBiomeChanger` | right-click → satellite.performAction |
| `ItemWeatherController` | right-click → satellite.performAction |
| `ItemBlockCrystal` / `ItemBlockFluidTank` | not yet assessed |
| `ItemPackedStructure` | structure-paste mechanic |

Per the `no-FakePlayer` rule from
`.agent/sops/development/testing-principles.md` (and the
`feedback_no_fakeplayer_for_player_tests` memory), these MUST land in
the testClient e2e harness, not in testServer with FakePlayer.

EOD marker: not separately filed — coverage delta documented inline +
in commits `2518f166` / `d291a1b4` / `ff1b68ef` on `feature/tests`.
