# TASK-20: Hovercraft ride / throttle / fuel-drain coverage

## Ticket

- Source: 2026-05-23 audit — Gap #2 ("Hovercraft riding / fuel-burn
  / fan-physics").
- Status: ✅ **Completed 2026-05-25** (partial — Phase 1+2 shipped; Phase 3 reframed as documentation).
- Created: 2026-05-23.

## Actual scope (2026-05-25)

**Phase 1 (mount/dismount) + Phase 2 (throttle) — ✅ shipped.**

`HovercraftRideE2ETest` — 4/4 client tests:

- `playerMountsHovercraftViaStartRiding` — spawn craft via probe,
  `mount-entity` probe drives `startRiding`, assert
  `riding-entity` probe reports the craft's id + class.
- `playerDismountClearsRidingEntity` — mount + dismount via probe,
  assert riding cleared.
- `forwardThrottleMovesHovercraftLaterally` — mount, `drive-ridden-
  entity 1 40` (combined probe that re-applies moveForward inline
  before each onUpdate), assert craft lateral position changed.
- `unmountedHovercraftDoesNotMoveLaterally` — counter-test:
  unmounted craft ticks but doesn't drift laterally.

**Phase 3 (fuel drain) — reframed as documentation.**

Reading `EntityHoverCraft.java` revealed the audit's "fuel drain"
gap was based on assumed mechanics that don't exist: the production
class has ZERO fuel/energy logic. `onUpdate` only reads
`player.moveForward` and applies acceleration; no fuel field, no
drain. Documented in `HovercraftRideE2ETest`'s javadoc so a future
addition of fuel mechanics MUST add the corresponding contract pin.

**Phase 4 (persistence) — not shipped this batch.**

Chunk-unload/reload persistence is testServer-tier (server-driven
chunk lifecycle), not testClient. Could be added later as a
sibling smoke test if a regression motivates.

**New probes** for this task (`TestProbeCommand`):

- `player mount-entity <entityId>` — bridges ClientBot's missing
  "right-click on entity" by calling `startRiding` server-side.
- `player dismount` — clears `getRidingEntity` via
  `dismountRidingEntity`.
- `player riding-entity` — observability probe.
- `player set-move-forward <value>` — set `player.moveForward`
  field; standalone, racy in client harness because CPacketInput
  resets between probe round-trips.
- `player drive-ridden-entity <moveForward> <ticks>` — composite
  probe; re-applies moveForward inline before each `onUpdate` call
  on the ridden entity. The reliable throttle driver.

**testClient ENV**: requires `xvfb-run` wrapper (LWJGL on headless
Linux), same as TASK-24.

## Context

Hovercraft coverage today:

- ✅ `HovercraftEntitySmokeTest` (1 server test, shallow by
  design) — spawn + tick alive, no riding/physics assertions.
- ✅ `ItemHovercraftSpawnE2ETest` (3 client tests, deep) —
  right-click ground spawns entity at ray-trace hit, item
  consumed in survival.

What's NOT pinned:

| Contract | Why it matters |
|---|---|
| Player mounts hovercraft via right-click on entity | Heartbeat UX — player can't ride if this breaks |
| Throttle (W key) accelerates forward | Player-visible motion |
| Fuel drains while in use | Energy economy gate |
| Fuel-empty drops player + craft | Player must not get stuck in air |
| Hover height tracks ground unevenness | Player would clip terrain otherwise |
| NBT save/load preserves energy + upgrades | Save-compat |
| Dismount returns control to vanilla movement | Player not stuck riding |

## Approach — testClient e2e

Hovercraft's contract is fundamentally player-driven (input keys,
movement, dismount). testServer can't drive `EntityPlayerSP.input`
state in a meaningful way. Goes in testClient territory.

The existing `ItemHovercraftSpawnE2ETest` is the right harness
shape — extend with ride methods, OR add a sibling
`HovercraftRideE2ETest` for failure isolation.

Recommend: sibling class, since spawn and ride fail for distinct
reasons.

## Implementation plan

### Phase 1 — Mount + dismount (~2 h)

Test: `HovercraftRideMountDismountE2ETest`:

- `playerMountsHovercraftOnRightClick` — spawn craft via probe,
  client bot right-clicks the entity, assert `player.ridingEntity
  == hovercraft`.
- `playerDismountsOnShift` — start mounted, send sneak input,
  assert `player.ridingEntity == null`.
- `dismountReturnsControlToPlayer` — after dismount, send forward
  movement input, assert player position changes (i.e. vanilla
  player movement reattached).

### Phase 2 — Throttle + motion (~3 h)

Test: `HovercraftRideThrottleE2ETest`:

- `forwardThrottleMovesHovercraftForward` — mount, send forward
  input for N ticks, assert craft `posX` (or `posZ` depending on
  yaw) changed in expected direction. Loose bound on distance.
- `noInputLeavesHovercraftHovering` — mount, no input, force ticks,
  assert craft `posY` stayed at hover height (within 0.1 tolerance)
  and lateral position unchanged.

### Phase 3 — Fuel drain + empty (~3 h)

Test: `HovercraftFuelDrainE2ETest`:

- `throttleConsumesFuel` — mount with known starting fuel, throttle
  for N ticks, assert remaining < starting.
- `emptyFuelDropsCraftAndPlayer` — set fuel to 1, force ticks past
  drain threshold, assert craft `posY` decreased + player no longer
  `ridingEntity`.
- `fuelAccrualPersistsAcrossDismount` — drain to 50%, dismount,
  re-mount, assert fuel still ~50% (saved on craft entity NBT).

### Phase 4 — Persistence (~1 h)

Test: `HovercraftPersistenceE2ETest` (or fold into
`MachineDomainSmokeSuite`):

- `hovercraftSurvivesChunkUnloadReload` — spawn at known coords,
  force chunk unload + reload, assert entity still present with
  same fuel/upgrades NBT.

## Acceptance

- [ ] Four testClient test classes (or three + folded).
- [ ] All assertions on player-visible state (`ridingEntity`,
      `posX/Y/Z`, fuel field, NBT key presence) — no impl pins.
- [ ] Pyramid counter regenerated per TASK-17 phase 1.

## Technical decisions

- **testClient required** — server-side `EntityHoverCraft.update`
  doesn't run player-input simulation; need a real client bot.
- **Loose-bound on motion** — exact velocity is impl; "moved
  forward by ≥1 block in 20 ticks" is the contract.
- **Probe-driven fuel set** — extend `/artest entity set-nbt` or
  add hovercraft-specific verb for setting initial fuel state.
- **No production logic changes**.

## Out of scope

- Fan-particle FX / sound effects.
- Upgrade-install permutations (separate item-contract scope).
- Multi-passenger rides (hovercraft is single-rider).

## Dependencies

- Depends on: testClient harness stable (DISPLAY=:77 per
  2026-05-22 marker).
- Does NOT block any other task.

## Estimated effort

- Phase 1 mount/dismount: ~2 h
- Phase 2 throttle/motion: ~3 h
- Phase 3 fuel: ~3 h
- Phase 4 persistence: ~1 h
- **Total**: ~9 h (largest single testClient task in the backlog)

## Risk

testClient flake exposure. The `BeaconMultiblockTest` /
`MachineRecipeIntegrationTest` flakes from TASK-12 close-out (see
TASK-16) are server-tier; adding more client-tier surface area
increases chance of similar contention. Plan to run the suite
serially (`--max-workers=1`) for the first 5 runs before letting
it go fully parallel.
