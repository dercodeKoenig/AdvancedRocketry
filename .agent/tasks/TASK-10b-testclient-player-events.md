# TASK-10b: testClient e2e player-event coverage

## Ticket

- Source: TASK-08-mixin close-out follow-up (2026-05-20). Replaces the
  rejected FakePlayer direction from the original TASK-10 draft per
  `feedback_no_fakeplayer_for_player_tests` — EntityPlayer-touching
  behaviour lives in the testClient e2e layer, not in testServer with
  FakePlayer scaffolding.
- Status: Pending
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

- [ ] Phase 1: 3 atmosphere-effect pins green; new
      `/artest player health|held-air` + `atmosphere cached-for-player`
      probes wired.
- [ ] Phase 2: 2 space-dim guard pins green.
- [ ] Phase 3: 3 advancement pins green; `/artest player advancement`
      probe wired.
- [ ] Phase 4: 4 sleep+fire guard pins green.
- [ ] Phase 5: low-gravity fall damage pin green.
- [ ] Phase 6: docs flipped, EOD marker shipped, pyramid green.
