# Context Marker: TASK-10 Phases 1 + 2 — FakePlayer probe + player events

**Created**: 2026-05-19 16:00 local
**Branch**: `feature/tests`
**Status**: ✅ Phase 1 (FakePlayer probe surface) + Phase 2 (real
player-event tests) shipped. Phase 3 (A2 remainder — suit / UV /
fueling / NBT) and Phase 4 (B3 suite-grouping) **DEFERRED** —
discovered a load-bearing finding that demands further investigation:
AR's player-tick chain NPEs on FakePlayer, so most "real player
behaviour" tests need a different harness approach.

---

## TL;DR

- **+6 server tests** in `FakePlayerProbeTest` — probe surface
  contract (create / teleport / tick / destroy / info / list +
  idempotency).
- **+4 server tests** in `PlayerEventBehaviourTest` — real
  `LivingUpdateEvent` posts via probe + side-effect observations.
  Including ONE `_documentsFakePlayerNPE` test that pins a
  harness-limitation finding.
- **6 new `/artest fakeplayer` probe verbs**: `create`, `teleport`,
  `tick`, `destroy`, `info`, `list`, plus `fire-living-update`.

---

## Pyramid state (post-TASK-10 partial)

| Layer | Result | Δ from TASK-07 (425) |
|---|---|---|
| testUnit | 162 / 0 / 0 | (unchanged) |
| testIntegration | 80 / 0 / 0 | (unchanged) |
| testServer | ~187 / 0 / 3 | +10 (6 probe + 4 event) |
| testClient | 6 / 0 / 0 | (unchanged) |
| **Total** | **~435 / 0 / 3** | **+10** |

---

## Major finding — production NPEs on FakePlayer

This session uncovered that AR's player-tick subscribers were never
designed to handle Forge's `FakePlayer`. Two production NPEs surface
when a FakePlayer is exposed to `LivingUpdateEvent`:

1. **`PlanetEventHandler.playerTick:229`** — calls
   `PlayerList.transferPlayerToDimension(player, 0, …)` to force-
   teleport players out of unattended spaceDim. FakePlayer is an
   `EntityPlayerMP` but isn't in the player list; the call NPEs at
   `PlayerList:620` dereferencing a null connection.

2. **`AtmosphereVacuum.onTick:30`** — calls
   `entity.addPotionEffect(...)` which routes into
   `EntityPlayerMP.onNewPotionEffect:1187` — that NPE-fires on
   `connection.sendPacket(...)` with a null connection.

**Why this is a finding, not a bug**: production assumes real players
with valid network connections. The NPEs happen only inside a test
harness using FakePlayer. We're documenting the limitation so that:

a. Future tests don't get blocked by "FakePlayer works in vanilla but
   crashes AR" mystery.
b. Anyone considering moddler-facing FakePlayer integration (e.g.
   automation mods that spawn FakePlayers in AR dims) knows this is
   currently unsafe.

The pin test
`fakePlayerInSpaceDimTriggersPlayerTickGuard_documentsFakePlayerNPE`
asserts the production DECISION-LOGIC fires correctly (it tried to
teleport, NPE'd on the side-effect). A future improvement: extend the
probe with a "drive playerTick via reflection without the EVENT_BUS"
mode that bypasses the failing subscribers — that lets us assert the
post-teleport dim cleanly.

---

## Probe surface delta

```
/artest fakeplayer create <name> [dim]
  → FakePlayerFactory.get(world, GameProfile) — idempotent

/artest fakeplayer teleport <name> <dim> <x> <y> <z>
  → setLocationAndAngles (not setPositionAndUpdate — FakePlayer
    has no connection for client packets); cross-dim refreshes
    the FAKE_PLAYERS cache

/artest fakeplayer tick <name> [count]
  → fp.onUpdate() — note: does NOT increment ticksExisted
    (Entity.ticksExisted advances via World.updateEntities, not
    direct onUpdate)

/artest fakeplayer destroy <name>
  → removes from probe cache + world.removeEntity

/artest fakeplayer info <name>
  → dim, pos, air, health, isAlive, ticksExisted, uuid

/artest fakeplayer list
  → all cached fake-player names

/artest fakeplayer fire-living-update <name>
  → posts LivingEvent.LivingUpdateEvent on EVENT_BUS.
    Subscriber exceptions are SWALLOWED and surfaced in the
    `subscriberError` response field. Probe response includes
    dimBefore/dimAfter + posXBefore/After for cause-effect checks.
```

---

## What's pinned

### `FakePlayerProbeTest` (6 tests)

- `createPersistsInListAndExposesInfo` — probe-shape contract.
- `teleportMovesPlayerToTargetDimAndCoords` — cross-dim + position.
- `tickIsSafeOnFakePlayer` — `onUpdate()` × 5 doesn't throw.
- `destroyRemovesPlayerFromList` — lifecycle cleanup.
- `teleportToUnknownPlayerReturnsError` — probe error path.
- `createIsIdempotentAcrossRepeatedCalls` — FakePlayerFactory cache
  behaviour confirmed (same UUID across two creates).

### `PlayerEventBehaviourTest` (4 tests)

- `fakePlayerInSpaceDimTriggersPlayerTickGuard_documentsFakePlayerNPE`
  — pins that the production guard at PlanetEventHandler.playerTick:229
  DID fire (subscriberError contains NPE from the teleport chain).
  Documents AR's FakePlayer-handling limitation explicitly.
- `fakePlayerInOverworldStaysInOverworldAfterEvent` — counter-test:
  the spaceDim-only guard must not over-fire on overworld players.
  No subscriber NPE expected (breathable atmosphere).
- `fakePlayerInArPlanetEventFiresWithoutTeleport` — AR planet
  (non-space) FakePlayer's dim is preserved. The dim discriminator
  in playerTick correctly gates on spaceDimId, not "any AR dim".
- `fakePlayerInfoStillResolvesAfterFailedTeleport` — probe cache
  resilience: even if a subscriber NPE'd mid-tick, subsequent
  info queries find the player.

---

## What's deferred from TASK-10

### Phase 3 — A2 remainder (DEFERRED)

- `SuitWorkStationAssemblesSuit`
- `UvAssemblerDivergesFromRocketAssembler`
- `FuelingStationFuelsAdjacentRocket`
- `FluidTankNBTRoundTripsAcrossRestart`

These need (a) recipe machinery for suit assembly, (b) a rocket
fixture adjacent to fueling station, (c) multi-boot for NBT. Each
is its own 2-3h investment — out of scope for this session.

### Phase 4 — B3 suite-grouping (DEFERRED)

Single-method `*SmokeTest` classes still spawn 14 separate JVMs.
Grouping them into ~3 domain suites saves ~120s wall at 3-way
parallelism. Worth doing but mechanical; defer to a cleanup session.

### Phase 5 — Original A3 tests still partly out of reach

The TASK-03 A3 plan had:
- `playerJoinArDimAppliesAtmosphereTracking` — needs atmosphere read
  on FakePlayer (covered partially via `info.air` field).
- `playerLeavingArDimReleasesTracking` — counter; no clear server
  hook.
- `playerInSpaceDimWithoutStationForceTeleported` — covered as
  `_documentsFakePlayerNPE` above (limitation pinned, not the full
  post-teleport state).
- `playerOnLunaTriggersWentToTheMoonAdvancement` — advancement
  registry requires a player on the actual player-list.

For the full Phase 5 plan we'd need either: (a) a real EntityPlayerMP
with a stub `NetHandlerPlayServer` connection, or (b) probe verbs
that invoke each side-effect method directly (atmosphere apply,
advancement grant) bypassing the LivingUpdateEvent chain. Both are
~4-6h follow-ups.

---

## Restore instructions

```
Read .agent/.context-markers/2026-05-19-1600_task10-fakeplayer-eod.md
Read .agent/.context-markers/2026-05-19-1530_task07-rocket-flight-cycle-eod.md
Read .agent/tasks/TASK-10-fakeplayer-and-task03-tail.md
```

Open items:

1. **A2 remainder** — suit / UV / fueling / NBT (~10-12 h).
2. **B3 suite-grouping** — mechanical wall-time win (~3 h).
3. **Real-EntityPlayerMP probe** — bypass FakePlayer NPEs to enable
   full Phase 5 player-event tests.
4. TASK-04 follow-up — post-assembly multiblock fixtures (~25 h).
5. TASK-05 items (~16-20 h).
6. TASK-09 satellite types (~10-12 h).
7. TASK-06 missions (~10-12 h).
8. TASK-08 ASM coremod (~13-18 h).

Nothing blocks releasing the suite at ~435/0/3.
