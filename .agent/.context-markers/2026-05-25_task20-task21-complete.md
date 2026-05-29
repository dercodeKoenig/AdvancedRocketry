# Marker — 2026-05-25 TASK-20 + TASK-21 complete

**Branch**: `feature/tests`
**Mode**: autonomous batch — both testClient tasks from original audit backlog.

## What shipped

| Task | Class | Tests | Layer |
|---|---|---|---|
| TASK-20 Hovercraft ride | `HovercraftRideE2ETest` | 4 | testClient |
| TASK-21 /ar player-equipped | `WorldCommandPlayerEquippedE2ETest` | 5 | testClient |
| **Total** | | **9** | |

## Pyramid

257 / 81 / 370 / **55** = **763** (+9 from 754).

## Probe additions (9 new verbs)

For TASK-20 hovercraft:
- `player mount-entity <entityId>` — startRiding bridge.
- `player dismount` — dismountRidingEntity bridge.
- `player riding-entity` — observability.
- `player set-move-forward <value>` — direct field set (racy alone).
- `player drive-ridden-entity <moveForward> <ticks>` — composite that
  re-applies moveForward inline before each onUpdate (defeats
  CPacketInput reset between probe round-trips).

For TASK-21 /ar player-equipped:
- `player exec-as-player <cmd...>` — command manager run with bot as
  sender (vs synthetic non-player serverClient sender).
- `player op-self` / `player deop-self` — op level toggle.
- `player inventory-contains <item-id>` — observability.
- `player give-held <item-id>` — equip in main hand.

## Decisions made autonomously

1. **TASK-20 Phase 3 fuel reframed as documentation**: reading
   `EntityHoverCraft.java` revealed ZERO fuel/energy logic in
   production. The audit's "fuel drain" gap was based on assumed
   mechanics. Documented in class javadoc so a future fuel addition
   forces a contract pin.

2. **TASK-20 input bridge — server-side probes vs bot input**:
   ClientBot doesn't support right-click-on-entity, sneak, or
   forward movement. Drove mount/dismount/throttle via new
   server-side probes. The observable result is identical because
   `getPassengerMovingForward` reads the SAME `player.moveForward`
   field whether set by client input or by server-side reflection.

3. **TASK-20 throttle race**: standalone `set-move-forward` probe
   failed because the bot's CPacketInput stream resets the field
   between probe round-trips. Solved by composite
   `drive-ridden-entity` probe that re-applies the field inline
   before each onUpdate call.

4. **TASK-21 verb shape was wrong in audit**: original plan had
   `/ar goto <dim> <x> <y> <z>` but `commandGoto` only takes
   `<dim>` or `station <id>`. Tests adjusted to match actual
   production grammar. Both forms (regular dim, station) pinned.

5. **TASK-21 `/ar fetch` deferred** — needs two connected bots;
   testClient harness supports one. Logged in TASK-21 file.

6. **TASK-21 `/ar fillData` deferred** — covered transitively by
   satellite-construction flow (TASK-09); the verb alone needs an
   ItemData fixture that duplicates that coverage.

## Commits

| SHA (pending) | Subject |
|---|---|
| TBD | test: TASK-20 + TASK-21 batch — hovercraft ride + /ar player-equipped (9 tests, 9 probes) |

## Resumption

Audit backlog effectively drained. Remaining items (TASK-15 visual
regression watching, TASK-16 investigation complete) are non-actionable.

**Quick-win still available**: Batch #2 bug #1 (SatelliteRegistry
SatelliteDefunct fallback). User opted to keep documenting bugs, not
fixing them — so this stays in ledger until explicitly requested.

**Next coverage batch needs new audit input** — current 763-test
suite covers all surfaces identified by the 2026-05-25 audit
sweep. Future audits could re-walk newly-modified production code.
