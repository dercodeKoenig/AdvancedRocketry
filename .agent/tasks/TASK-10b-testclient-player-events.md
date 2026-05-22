# TASK-10b: testClient e2e player-event coverage

## Ticket

- Source: TASK-08-mixin close-out follow-up (2026-05-20). Replaces the
  rejected FakePlayer direction from the original TASK-10 draft per
  `feedback_no_fakeplayer_for_player_tests` — EntityPlayer-touching
  behaviour lives in the testClient e2e layer, not in testServer with
  FakePlayer scaffolding.
- Status: Phases 1-6 ✅ Completed (2026-05-20). Phase 7 ✅ Completed
  (2026-05-22) — SpaceArmor drain pin closed the last workable
  follow-up; remaining items (WeatherController, SpaceChest,
  ItemBlock* trio) are rescoped/dropped per SOP litmus.
- Created: 2026-05-20

## Context

Several production player-event paths are wiring-tested but not
behaviour-tested:

| Hook | Code | Existing coverage |
|---|---|---|
| `PlanetEventHandler.playerTick` Y&lt;0 space-dim guard | line 210-232 | wiring only |
| `PlanetEventHandler.sleepEvent` no-atmosphere sleep block | line 237-249 | wiring only |
| `PlanetEventHandler.fallEvent` low-gravity fall damage | line 612-618 | none |
| `PlanetEventHandler.blockRightClicked` flint-and-steel in vacuum | line 281-299 | none |
| `AtmosphereHandler.onTick` damage + suit drain in vacuum | line 212-230 | none |
| `AtmosphereHandler.onPlayerChangeDim` cache invalidation | line 232-236 | none |
| `ARAdvancements` triggers (MOON_LANDING, etc.) | advancements/ | none |
| `SpaceObjectManager.onPlayerTick` station boundary reflect | line 248-293 | none |

The
[`feedback_no_fakeplayer_for_player_tests`](MEMORY.md) memory pins
that these MUST live in testClient e2e, because each touches a real
`EntityPlayer` lifecycle (capabilities, openContainer, fall state,
inventory) that FakePlayer can't faithfully reproduce.

`MixinHookBehaviourPinsTest` and `InventoryBypassRedirectE2ETest`
demonstrated the testClient infra is ready: real client bridge,
`/artest` probe surface, `serverClient().execute` + `bot()` API. This
task ports that pattern to the player-event surface above.

## Implementation Plan

### Phase 1 — Atmosphere effects on dim join + tick (~3 h)

**Behavioural pins:**

- `playerSuffersDamageInVacuumWithoutOxygenSuit` — teleport to a
  no-atmosphere AR dim, wait N ticks, assert player health dropped.
- `oxygenSuitDrainsWhileBreathingInVacuum` — equip suit, teleport to
  vacuum dim, wait N ticks, assert suit's `ItemAirUtils` air NBT
  decreased.
- `dimChangeClearsAtmosphereCacheForPlayer` — chain two dim teleports
  with different atmospheres, observe that the second dim's atmosphere
  applies (not the first's cached one). Needs a probe that exposes
  `AtmosphereHandler.lastAtmosphereForPlayer` or equivalent.

**New probe verbs:**

- `/artest player health` — report player.getHealth(), maxHealth.
- `/artest player held-air` — report `ItemAirUtils.getAirRemaining()`
  of held-item suit.
- `/artest atmosphere cached-for-player <name>` — reflective read of
  AtmosphereHandler.lastSavedAtmosphere or whatever the map is.

### Phase 2 — Space-dim Y&lt;0 teleport guard (~2 h)

Production: `PlanetEventHandler.playerTick` lines 210-232 — when a
player in a space dim has `posY < 0`, the handler teleports them to
the nearest station or back to the overworld.

**Behavioural pins:**

- `playerFallingBelowY0InSpaceTeleportsToStation` — create a station,
  put a player at Y=-10 in the space dim, wait one tick, assert player
  is now at station coords (or in overworld if no station).
- `playerFallingBelowY0InSpaceFallsBackToOverworld` — counter-test: no
  station registered, Y<0 → player lands in overworld.

Existing `SpaceStationLifecycleSmokeTest` already covers station
registration so we can reuse `/artest station create`.

### Phase 3 — Advancements triggered by gameplay events (~3 h)

Production: `ARAdvancements` defines 8 custom triggers; `playerTick`
fires them when the player enters certain dims.

**Behavioural pins:**

- `firstArrivalToMoonFiresMoonLandingAdvancement` — teleport to moon
  dim, wait, assert
  `EntityPlayerMP.getAdvancements().getProgress(MOON_LANDING).isDone()`
  is true.
- `wentToTheMoonAdvancementGrantsOnReturn` — full round-trip (moon →
  overworld), assert WENT_TO_THE_MOON unlocks.
- One advancement-doesn't-fire counter-test (visiting a non-moon AR
  dim should not flag MOON_LANDING).

**New probe verb:**

- `/artest player advancement <id>` — query
  `EntityPlayerMP.getAdvancements().getProgress(rl).isDone()`.

### Phase 4 — Sleep + flint-in-vacuum guards (~2 h)

**Behavioural pins:**

- `sleepOnPlanetWithoutAtmosphereIsRefused` — place bed, right-click,
  assert sleep didn't start (or chat-error fires).
- `flintAndSteelInVacuumDoesNotIgnite` — give player flint+steel,
  right-click in vacuum dim, assert no fire block placed.
- Counter-test for both in a breathable dim (sleep allowed, fire
  ignites).

### Phase 5 — Low-gravity fall damage adjustment (~2 h)

Production: `fallEvent` (LivingFallEvent line 612-618) scales damage
by gravity multiplier.

**Behavioural pin:**

- `lowGravityDimReducesFallDamage` — drop player from y=100 in a
  low-grav dim vs overworld, compare resulting health loss. The mixin
  doesn't change this path — pure event-handler test — but it
  closes the dim-aware fall coverage gap.

### Phase 6 — Docs + EOD (~1 h)

- `.agent/tasks/README.md` — flip TASK-10b to ✅.
- `.agent/system/project-architecture.md` — add Player-event handler
  section if missing.
- EOD marker.

## Technical Decisions

- **All tests testClient e2e, never FakePlayer in testServer** —
  per `feedback_no_fakeplayer_for_player_tests`. Real EntityPlayerMP
  on a real connection; testClient bot drives the client side via
  the existing FG6 bridge.
- **`@FixMethodOrder(NAME_ASCENDING)`** for any class with state
  carry-over between tests, so the order is reproducible.
- **One test class per phase** — keeps a phase's failure localized
  and the test class JVM-shared (shared-harness saves cold-start
  cost like `AbstractSharedServerTest`).
- **Reuse `InventoryBypassRedirectE2ETest` patterns** — explicit
  `clear @a`, force-load chunks before placing, stand-above pose
  with pitch=90 for right-clicks.
- **Probe additions are minimal and tagged** — every new verb in
  `TestProbeCommand` carries a TASK-10b reference and stays gated
  by `-Dadvancedrocketry.tests=true`.

## Dependencies

**Requires:** existing testClient harness + `forge.test.client.enabled`
gating + `DISPLAY=:77` headless X server. Already proven by 7 prior
testClient suites.

**Does NOT block:** further server-only work — production code is
untouched (this is pure new test coverage).

## Risks

1. **Flakiness on player-state polluted by earlier tests in the
   testClient class run.** Mitigation: per-test `clear @a` + bypass
   reset (same pattern that fixed the inventory-bypass e2e).
2. **Advancement state persists across server restarts** in the work
   dir. Mitigation: query via probe rather than asserting on
   filesystem; if needed, add `/artest player advancement reset`.
3. **Multi-dim teleport tests may interact with WeatherClientSyncE2ETest's
   dim setup** — those teleport to dims 2/3. Pick higher-id dims for
   TASK-10b tests or run sequentially.

## Estimated effort

~13 h across 5-6 sessions:
- Phase 1: 3 h
- Phase 2: 2 h
- Phase 3: 3 h
- Phase 4: 2 h
- Phase 5: 2 h
- Phase 6: 1 h

## Completion Checklist

- [x] Phase 1: 3 atmosphere-effect pins green (`AtmospherePlayerEventE2ETest`);
      `/artest player health|set-health|held-air|give-suit-chest` +
      `atmosphere cached-for-player` probes wired. Scope rewritten:
      vacuum-damage application lives in libVulpes binary so the AR pin
      surface is per-player cache + sync, not damage numbers.
- [x] Phase 2: 2 space-dim guard pins green (`SpaceDimGuardE2ETest`)
      — no-station fallback to overworld + registered-station teleport
      to spawn.
- [x] Phase 3: 4 advancement pins green (`AdvancementsE2ETest`) —
      baseline + Luna positive + non-Luna AR dim counter-test +
      far-from-coords counter-test; `/artest player advancement <id>`
      + `advancement reset` probes wired. MOON_LANDING intentionally
      dropped — lives in EntityRocket (TASK-07 domain), not in
      PlanetEventHandler.
- [x] Phase 4: 4 sleep+flint guard pins green (`VacuumGuardsE2ETest`)
      — sleep refused/allowed + flint canceled/allowed in vacuum vs
      breathable AR dims; `/artest player try-sleep|try-ignite` probes
      wired.
- [x] Phase 5: 2 low-gravity fall pins green (`LowGravFallDamageE2ETest`)
      — overworld no-op + 0.17-grav AR dim scales distance by gravity;
      `/artest player try-fall <distance>` probe wired.
- [x] Phase 6: docs flipped, EOD marker shipped.

### Phase 7 — TASK-05 player-tier item behaviour (reopened 2026-05-21, ~10-14 h)

Moved here from [[TASK-05]] per the no-FakePlayer rule. The unit-tier
surface for these items is already shipped (12 of 21 item classes
covered in `ChipNBTRoundTripTest`, `ItemDataCarrierNBTRoundTripTest`,
`ScannerDetectorItemContractTest`, `SpecialPurposeItemContractTest`,
`JackHammerContractTest`, `SealDetectorDispatchTest`,
`SpaceArmorContractTest`, `SpaceArmorProtectionContractTest`). What
remains needs a real EntityPlayerMP and lives in testClient e2e.

**Behavioural pins (one suite per logical cluster):**

- `ItemHovercraftSpawnE2ETest`
  - `hovercraftItemRightClickOnGroundSpawnsRideableEntity` — give item,
    right-click on grass, assert `EntityHovercraft` exists at cursor pos.
  - `hovercraftRefusesToSpawnInVacuumOrLava` — counter-test per
    production gate (if any; otherwise document the contract as
    "spawns regardless of dim").

- `ItemSpaceArmorUseFluidE2ETest`
  - `suitFluidDrainsOnVacuumTickWhenWorn` — equip suit, teleport to
    vacuum dim, wait N ticks, assert `ItemAirUtils.getAirRemaining`
    decreased.
  - `suitDamageAbsorptionReducesPlayerDamage` — equip suit, apply
    fixed damage source, assert delta less than naked-player baseline.

- `ItemSpaceChestDeathPersistE2ETest`
  - `chestStaysEquippedAcrossDeathAndRespawn` — equip chest with NBT,
    kill player, respawn, assert chest still in equipment slot with
    same NBT.
  - `chestComponentSlotsSurviveDeath` — install a component module
    into chest, die/respawn, assert component still in slot.

- `ItemBiomeChangerActionE2ETest`
  - `biomeChangerRightClickOnGrassChangesTargetBiome` — program the
    chip with a BiomeChanger satellite, right-click in same dim,
    assert `world.getBiome(playerPos)` changed.
  - `biomeChangerRightClickInWrongDimIsNoOp` — sat bound to dim A,
    player in dim B → biome unchanged.

- `ItemWeatherControllerActionE2ETest`
  - `weatherControllerRightClickFiresPerformAction` — bind sat,
    right-click, assert satellite-driven weather change occurred
    (rain started / dry-flooded the area per mode_id).

- `ItemSealDetectorPlayerMessagesE2ETest`
  - For each of the 6 dispatch branches (sealed / notsealmat /
    notsealblock / notfullblock / fluid / other), place the
    appropriate fixture, right-click with detector, assert player
    received the matching `msg.sealdetector.<branch>` chat message
    (probe via `/artest player last-chat` if needed).
  - Cross-pin against the existing `SealDetectorDispatchTest` server
    dispatch — both must agree on which branch fires per fixture.

- `ItemAtmosphereAnalzerPlayerReadoutE2ETest`
  - `atmosphereAnalyzerRightClickInVacuumReportsCorrectAtmType` —
    equip analyzer in head slot, right-click in vacuum dim, assert
    player received "vacuum" atm-type chat message. Works around
    the unit-tier static-`<clinit>` LibVulpes.proxy issue because the
    testClient harness has a fully-booted proxy.

- `ItemBlockCrystal` / `ItemBlockFluidTank` / `ItemPackedStructure`
  — research scope first; may move to a separate ticket if surface
  is large.

**New probe verbs (likely):**

- `/artest player give-item <item-id> [count]` — populate hotbar.
- `/artest player swap-armor <slot> <item-id>` — equip armor slot.
- `/artest player kill` — for death-persist tests.
- `/artest player last-chat` — read most-recent received chat line
  (for asserting i18n-message dispatch).
- `/artest player equipment <slot>` — JSON of slot contents.
- `/artest entity find <entity-id> <dim>` — for hovercraft spawn check.

**Acceptance:**

- [x] `ItemSealDetectorPlayerMessagesE2ETest` (8 tests, 6 fixtures
      + chat-clear scaffold + error envelope) — `6184f3e7`
- [x] `ItemAtmosphereAnalzerPlayerReadoutE2ETest` (3 tests, AIR
      readout on vanilla dim + 2 error envelopes) — `5f88b777`
- [x] `ItemHovercraftSpawnE2ETest` (3 tests, target-block spawn
      + empty-ray PASS + error envelope) — `6282334a`
- [x] `ItemBiomeChangerActionE2ETest` (2 tests, posList save-format
      pin + error envelope) — `23e9aadd`
- [~] `ItemWeatherControllerActionE2ETest` — **rescoped/dropped.**
      The right-click effect on a WeatherController-bound chip is
      `performAction` populating the private `viable_positions`
      list. That list is NOT persisted to NBT (writeToNBT only emits
      mode_id / last_mode_id / floodlevel), so there is no
      save-format observable to pin against. Reading
      `viable_positions` via reflection would be impl-field testing
      — anti-pattern per testing-principles SOP. Real player-visible
      contract (eventual rain/dry change) needs full battery + tick
      cycle, out of unit/probe scope. Leave for a future ticket that
      adds either an NBT pin in production or a tick-loop driver.
- [x] `ItemSpaceArmorUseFluidE2ETest` (3 tests) — suited vacuum drain
      pin + breathable-dim counter + bare-skin damage cross-check.
      Uses enchanted-vanilla-armor fixture (Path 1 of
      `AtmosphereNeedsSuit.protectsFrom` — `ItemAirUtils.ItemAirWrapper`
      drain into the static "air" NBT). New probes: `equip-airsuit`
      + `clear-armor`. Reuses `OxygenSuitClientStateE2ETest`'s
      in-place `set-density 0 0` pattern, dropping the XML-planet
      scaffolding originally estimated at 3-4h.
- [~] `ItemSpaceChestDeathPersistE2ETest` — **dropped, not a mod
      contract.** Production has no custom PlayerEvent.Clone /
      death-keep / drop handler for SpaceChest. Pin would test
      vanilla Minecraft ItemStack NBT survival through entity
      drops — not the mod's contract. SOP litmus fails.
- [ ] ItemBlockCrystal / ItemBlockFluidTank / ItemPackedStructure
      — still deferred, separate ticket if needed.
- [x] Phase 7 pyramid PASS (19/19 across the 5 shipped suites)
- [x] EOD marker (`.agent/.context-markers/2026-05-22_task10b-phase7-closeout.md`)
