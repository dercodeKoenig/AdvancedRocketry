# TASK-36: Deeper contracts — TerraformingTerminal biome-mutation + ServiceStation repair cycle

## Ticket

- Source: 2026-05-25 Tier 1+2 audits. Both deferred at the time
  because of fixture work; carried forward into 2026-05-26 audit
  out-of-scope.
- Status: **Blocked** — see Blocker section below; each subscope
  has its own concrete prerequisite.
- Created: 2026-05-26.

## Context

Two tiles with shallow coverage today, grouped because both need
new fixture/probe surfaces and they share the "depth tile contracts
behind specific item requirements" character.

### 36a. TerraformingTerminal biome-mutation

`TileTerraformingTerminal` plus its companion satellite
(BiomeChanger) implements a player loop:

1. Player programs a BiomeChanger chip with target biome.
2. Player feeds the chip + satellite to a TerraformingTerminal.
3. Terminal queues a biome change at the satellite's coords.

Current coverage:
- `SatelliteTypeBehaviourTest` covers BiomeChanger satellite
  `tickEntity()` with a pre-set queue.
- `ItemBiomeChangerSatelliteActionE2ETest` covers the chip's
  right-click action surface.

What's NOT pinned: the **terminal-to-satellite** wiring — feeding
the terminal a programmed chip results in the satellite getting
the right queue. That's the player-visible mid-game gate.

### 36b. ServiceStation repair cycle

`TileServiceStation` accepts a rocket and repairs damaged parts
(broken from prior re-entry / explosion damage). Current coverage:
- TASK-18 audit pinned placement smoke.
- No test pins the actual repair (broken part in → repaired part
  out).

What's NOT pinned: the full "broken rocket part in, intact part
out after N ticks" loop.

## Blockers

### 36a blocker

Needs a probe to construct a programmed BiomeChanger chip with
known target coords. Concretely:
- `item make-biomechanger-chip <stack-slot> <biome-id> <x> <y> <z>`
  — sets the BiomeChanger chip's NBT to a target biome at known
  coordinates.

Without this, the test can't differentiate "chip wired correctly"
from "chip wasn't programmed".

### 36b blocker

Needs a probe to inject a `TileBrokenPart` into the service
station's input. Concretely:
- `service-station inject-broken-part <dim> <x> <y> <z> <partType>`
  — places a broken part into the station's input slot.

Without this, the test can't set up the "rocket arrives with
broken part" precondition. Production's broken-part injection
happens during launch failures, which is heavy to drive in a
test.

## Implementation plan

### 36a (~3 h)

1. Add `item make-biomechanger-chip` probe (~1 h).
2. `TerraformingTerminalBiomeMutationTest` (~2 h) — feed the
   terminal a programmed chip + a BiomeChanger satellite, force-
   tick, assert the satellite's `viable_positions` (or equivalent
   queue field) contains the target coords.

### 36b (~3 h)

1. Add `service-station inject-broken-part` probe (~1 h).
2. `ServiceStationRepairCycleTest` (~2 h) — inject broken part,
   force-tick station, assert input slot empty + adjacent output
   has intact part.

## Acceptance

- [ ] 1-2 tests per subscope (2-4 total).
- [ ] Probe verbs documented.
- [ ] Pyramid counter regenerated.

## Out of scope

- Per-biome enumeration (test 1-2 representative biomes, not all).
- Concurrent terminal usage (multi-player).
- Specific repair part types (test 1 representative type).

## Dependencies

- 36a and 36b are independent of each other but share this TASK
  for index efficiency. Either can ship first.
- Does NOT block any other task.

## Estimated effort

- 36a: 3 h
- 36b: 3 h
- **Total**: ~6 h

## Risk

Low-medium. Both blockers are probe additions — once probes land,
the tests are mechanical.
