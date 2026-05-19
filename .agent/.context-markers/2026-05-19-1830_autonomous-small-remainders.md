# Context Marker: autonomous small-remainders batch

**Created**: 2026-05-19 18:30 local
**Branch**: `feature/tests`
**Session type**: autonomous (per user direction "делай task-04, 05,
06 и 07 в автономном режиме")

---

## What landed

| Task | Delta | Status |
|---|---|---|
| TASK-05 Phase 1 | +14 unit tests in `ChipNBTRoundTripTest` | ✅ shipped |
| TASK-05 Phase 2 | +11 unit tests in `SpaceArmorContractTest` | ✅ shipped |
| TASK-04 Phase 2-5 | research note: libVulpes registry names | doc-only |
| TASK-06 | status note: blocker is the same fixture-builder problem | doc-only |
| TASK-07 | already closed — remaining items belong to TASK-10b | no changes |

**Pyramid delta**: testUnit + 25 tests, testServer unchanged from
TASK-10 close (181 / 0 / 3). All unit tests run in <12 s combined.

## Commits on `feature/tests`

```
2aabbed  docs: TASK-04 + TASK-06 status notes
90efec1  test: TASK-05 Phase 1+2 — chip NBT + space-armor unit contracts
```

Stacked on top of the previous TASK-10 close (`b01fa55`).

## Production findings pinned

1. **`ItemPlanetIdentificationChip.setDimensionId(INVALID_PLANET)`**
   silently drops the NBT compound (lines 73-77 — creates a fresh
   compound and returns without `stack.setTagCompound(nbt)`). Tests
   pin this as `_documentsKnownBug`. Future production fix: attach
   the NBT before the early return.

## Blockers identified for next sessions

Both TASK-04 (multiblock depth) and TASK-06 (mission depth) have the
**same shape of blocker**: each needs ~2-3 h of fixture-builder probe
infrastructure before the first behavioural test lands.

For TASK-04:
- libVulpes structure-block registry names recovered:
  `libvulpes:structureMachine`, `libvulpes:advStructureMachine`,
  `libvulpes:advancedMotor`.
- Next step: `/artest fixture multiblock <type> <dim> <x> <y> <z>`
  per multiblock type — verbatim from `Tile*Generator.structure[][][]`.
- BlackHoleGenerator-specific note: its `writeToNBT` is a pass-
  through, so even a fixture-free NBT round-trip test buys nothing
  over the base class.

For TASK-06:
- Mission data-carrying ctors need `EntityRocket` +
  `LinkedList<IInfrastructure>` + Fluid. Two viable approaches:
  (a) `/artest mission ...` probe verbs that wire a mission from a
  fixture-built rocket; (b) reflection-based instantiation at server
  tier. Either is the long pole before per-tick or persistence tests.

## TASK-10 / TASK-10b reminders

- TASK-10 itself is fully closed (Phase 2 + Phase 1 4/4 — see prior
  marker `2026-05-19-1745_task10-redone-without-fakeplayer.md`).
- TASK-10b (proposed) for testClient e2e player-event coverage is
  still pre-plan — no doc, no implementation. Pre-requisite for the
  deferred items in TASK-05 (EntityPlayer items), TASK-06 (reward
  grants), TASK-07 (descent / landing / fuel-out-of-flight).

## Open follow-ups (priority for next session)

1. **TASK-04 fixture-builder probe** (~2 h) — implement
   `/artest fixture multiblock <type>` for at least BlackHoleGenerator
   + SpaceLaser using the recovered libVulpes names. Then ship
   per-multiblock depth tests.
2. **TASK-06 mission probe** (~2 h) — choose probe-verb vs
   reflection-instantiation path; ship Phase 1+2.
3. **TASK-10b draft** — write the doc for testClient e2e
   player-event coverage so the cross-links from TASK-05/06/07
   point at a real plan.

## Restore instructions

```
Read .agent/.context-markers/2026-05-19-1830_autonomous-small-remainders.md
Read .agent/tasks/README.md
Read .agent/tasks/TASK-04-multiblock-machine-depth.md
Read .agent/tasks/TASK-06-mission-system-depth.md
```
