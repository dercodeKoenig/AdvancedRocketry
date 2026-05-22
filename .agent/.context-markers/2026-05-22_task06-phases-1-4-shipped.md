# Context marker — 2026-05-22 (later)

**Slug**: task06-phases-1-4-shipped
**Branch**: `feature/tests` (clean, all pushed)
**Session focus**: TASK-06 mission-system depth — replan + ship Phases 1-4.

## Session arc

Continuation of the 2026-05-22 morning session that closed TASK-10b
Phase 7. Two sub-arcs:

1. **Replan TASK-06** against a production audit — found 4 sub-tests
   from the 2026-05-19 draft that don't map to real contracts (gas
   satellite-type gate, gas rate-by-fluid, player reward grant,
   reward-capacity-clamp). Rewrote the plan; committed at `0ed605e8`.
2. **Implement Phases 1-4 in one session** — 5 probe verbs + 14 tests
   (3 unit + 11 server) all green. Shipped at `3da0ae0a`.
   (Note: the Phase 5 infra lifecycle, multi-boot persistence, and
   the strong "64000 mB oxygen fill" assertion are deferred; details
   below.)

## Probe verbs added (under `/artest mission ...`)

| Verb | Purpose |
|---|---|
| `start-gas <dim> <rocketId> <duration> <fluid> [intakePower]` | Construct + register MissionGasCollection on rocket; optional intakePower default 0 |
| `start-ore <dim> <rocketId> <duration> <drillingPower>` | Same for MissionOreMining; injects ItemAsteroidChip into guidance computer with mid-range data values |
| `state <missionId>` | Reflection-based JSON of progress / startWorldTime / duration / isDead / type |
| `advance <missionId> <ticks>` | Backdate startWorldTime — deterministic + cheap vs scheduling real ticks |
| `complete-now <missionId>` | Atomic: backdate to progress=1, tickEntity once, embed rocket-cargo readback in same response. Critical: the natural DimensionProperties.tick prunes dead missions from the satellite registry between commands — embedding cargo-readback in the same probe call avoids the prune race |
| `rocket-cargo <missionId>` | Standalone cargo readback (only safe to call before the prune fires — use complete-now for tests that need both) |

## Commits this session (all pushed, branch `feature/tests`)

| SHA | Title | Tests |
|---|---|---|
| `0ed605e8` | docs: rewrite TASK-06 mission-system plan against production audit | — |
| `3da0ae0a` | test: TASK-06 Phases 1-4 — mission system depth (+14 pins) | 14 |
| `<this>` | docs: TASK-06 phases 1-4 close-out + marker | — |

## Files touched

**Production / probe**:
- `src/main/java/.../command/test/TestProbeCommand.java`
  - Added `case "mission"` dispatch + `handleMission` with 6 verbs
  - Added `snapshotCargoJson` helper used by both `rocket-cargo` and
    `complete-now` (latter embeds cargo to escape the prune race)
  - Added field-reflection helpers (`readLongField` etc.) that walk
    the class hierarchy — missions store their fields as
    package-private in `MissionResourceCollection` and only the
    `gasFluid` field lives on the concrete subclass

**Tests** (4 new files):
- `src/test/java/.../test/unit/MissionNbtRoundTripTest.java` (3 tests)
- `src/test/java/.../test/server/MissionLifecyclePyramidTest.java` (5)
- `src/test/java/.../test/server/MissionGasCompletionTest.java` (3)
- `src/test/java/.../test/server/MissionOreCompletionTest.java` (3)

**Docs**:
- `.agent/tasks/TASK-06-mission-system-depth.md` — replanned + closed
  Phases 1-4 with deferred follow-ups documented
- `.agent/tasks/README.md` — Done table entry for TASK-06
- `.agent/DEVELOPMENT-README.md` — Pending list refreshed

## Discoveries (worth carrying forward)

### Race condition: natural tick prunes dead satellites

`DimensionProperties.tick()` iterates `tickingSatellites`, calls
`tickEntity()`, and on `isDead()` removes from BOTH satellite maps.
Tests that do `complete-now` then `state` race against this — the
mission is gone before the follow-up state call lands. Fix pattern:
make the mutating probe call return the post-state atomically rather
than relying on follow-up reads. Same pattern as the chat-tap fix
from the prior session (atomic capture + emit).

### Fixture rocket has no fluid TileEntities

`BlockFuelTank` is a pure block — no `createNewTileEntity`. So a
fixture rocket's `StorageChunk.liquidTiles` is empty (the population
filter is `tile.hasCapability(FLUID_HANDLER_CAPABILITY, null)` which
never fires for fuel tanks). Gas mission's
`for (TileEntity tile : rocketStorage.getFluidTiles()) { fill(...) }`
iterates zero times → no observable fluid amount. The strong
"64000 mB oxygen fill" assertion needs a fluid-cargo rocket fixture
variant that doesn't exist yet — recorded in `TASK-06.md` deferred
follow-ups.

### NBT round-trip can't unit-test through full readFromNBT

`MissionResourceCollection.readFromNBT` expects non-null
`rocketStats` + `rocketStorage` compounds. A unit test that
constructs an empty mission then calls writeToNBT → readFromNBT NPEs
unless those compounds are pre-built. Worked around by testing the
gas-key path in isolation (write/read just the `"gas"` key on a
synthetic NBT) and the infrastructure-list shape via direct tag-list
iteration. A heavier server-tier persistence test would be the
honest end-to-end pin.

## Deferred follow-ups (see TASK-06.md for details)

| # | Item | Effort | Why deferred |
|---|---|---|---|
| 1 | Fluid-cargo rocket fixture + restore strong 64000 mB pin | ~1 h | Needs a `with-fluid-cargo` variant on `/artest fixture rocket` (probe scope creep) |
| 2 | Multi-boot persistence tests (gas + ore) | ~2-3 h | Heavier infra; not blocking the headline contracts |
| 3 | Phase 5 infrastructure lifecycle tests | ~2-3 h | Needs `/artest mission infra-state` verb + a fixture infrastructure tile (TileGuidanceComputerAccessHatch-flavoured) |

Total deferred: ~5-7 h. None blocking for the core mission contract
coverage.

## Open backlog (post-TASK-06 partial)

**P2**:
- TASK-06 deferred follow-ups above (5-7 h total)
- TASK-10b Phase 7 follow-ups (SpaceArmor useFluid; WeatherController
  right-click — both gated on production / framework changes)

No P1/P0 work remaining for gameplay-contract coverage. The mod's
main loops (rocket build / launch / dim transition / satellites /
missions / player atmosphere effects / item right-click behaviours /
sealed-room detection / atmosphere readouts / biome-changer) all
have at least baseline contract coverage now.

## Infra notes (still relevant)

- `PostToolUse:Bash` hook still spams blocking errors about a
  missing `monitor-tokens.py`. User declined to touch settings.json;
  the spam is non-blocking.
- `DISPLAY=:77` for testClient (default `:99` has no Xvfb).
- Server-tier mission tests run in ~30-45s wall clock; fast feedback
  loop compared to testClient.

## Next session entry point

If resuming:
- `nav-start` will detect this marker via `.active`.
- Largest gameplay-contract gaps are now in the **known-bug-fix**
  bucket (6 bugs in `.agent/tasks/README.md` ledger, 5 with
  `_documentsKnownBug` pins waiting to be flipped). A "fix +
  flip pins" ticket would touch real production logic and so was
  out of scope for the test-authoring sessions.
- Otherwise: TASK-06 follow-ups (~5-7 h) or TASK-10b Phase 7
  follow-ups complete the long tail.
