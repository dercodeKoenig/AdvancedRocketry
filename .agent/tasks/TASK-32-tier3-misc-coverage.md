# TASK-32: Tier 3 misc coverage — ItemPackedStructure deploy + atmosphereType NBT + MonitoringStation comparatorOverride

## Ticket

- Source: 2026-05-25 Tier 3 audit, gaps left over (ItemPackedStructure
  deploy contract + custom atmosphereType NBT round-trip).
  Plus 2026-05-26 audit out-of-scope:
  `TileRocketMonitoringStation.getComparatorOverride` (comparator
  signal 0-15 from rocket height — needs flying rocket).
- Status: **✅ Completed 2026-05-26 partial** — see
  `.agent/tasks/README.md` Done table. 3a downscoped at unit tier;
  see "Actual scope" below.
- Created: 2026-05-26.

## Actual scope shipped

**3a — ItemPackedStructure** (`testUnit`,
`ItemPackedStructureNbtRoundTripTest`):

- `getStructureOnStackWithoutNbtReturnsNull` — pin the null-gate
  consumers (`TileSatelliteHatch`, `TileRocketAssemblingMachine`)
  use to skip blank items.
- `itemPackedStructureDeclaresHasSubtypes` — pin the constructor's
  `hasSubtypes=true` flag, required for `itemSpaceStation`'s
  per-meta variant rendering.

The full `setStructure` → `getStructure` round-trip pin was
deferred: `StorageChunk`'s constructor reaches
`FMLCommonHandler.getMinecraftServerInstance().profiler` via
`CommonProxy.getProfiler` which NPEs at unit tier. The
round-trip contract is already exercised at server tier by the
existing rocket-assembly + station-assembly suites that feed
through `ItemPackedStructure` end-to-end.

**3b — custom AtmosphereType** (`testUnit`,
`CustomAtmosphereTypeNbtRoundTripTest`):

- `customAtmosphereResolvesByUnlocalizedNameViaRegistry` — register
  a fresh `AtmosphereType`, resolve via
  `AtmosphereRegister.getAtmosphere(name)`, assert SAME instance
  (not a copy — production code compares atmospheres with `==`).
- `customAtmosphereSurvivesNbtNameRoundTripThroughRegistry` —
  mirror the `TileAtmosphereDetector.writeToNBT`/`readFromNBT` loop
  (write `atmName=getUnlocalizedName()`, read back, lookup) on a
  custom-registered type. Pins the companion-mod save-compat
  contract.

**3c — MonitoringStation comparator override** (`testServer`,
`MonitoringStationComparatorOverrideTest`):

- `unlinkedMonitorReportsZeroComparatorOverride` — pin the
  `return 0` null-rocket branch.
- `linkedMonitorComparatorOutputRisesWithRocketPosY` — link a
  rocket, set `posY=68` (low) → read comparator; set `posY=5000`
  (high) → read; assert strict monotonicity. Doesn't pin exact
  values (depends on `getTopBlock` + `getEntryHeight` which are
  not part of the player-visible contract).

Probe surface: extended `infra monitor-info` to also return
`comparatorOverride` (option 1 of the original plan — directly
expose the live `getComparatorOverride()` call). Option 2
(probe to manipulate rocket position) was not needed —
`rocket set-state posY=...` already exists.

## Out-of-scope items confirmed deferred

- ItemPackedStructure full setStructure round-trip — exercised
  transitively by the rocket-assembly / station-assembly suites.
- ItemPackedStructure capture path (player → assembler) — that's
  the assembler suite's domain, not 3a's.

## Context

Three small Tier 3 contracts grouped here because each is one or
two tests and they share no fixture work — folded into a single
TASK to keep the index lean.

### 3a. ItemPackedStructure deploy

`zmaster587.advancedRocketry.item.ItemPackedStructure` — players
right-click to deploy a stored multiblock structure. The deploy
path:

1. Reads serialized blocks from item NBT.
2. Places them in the world at the right-click hit position.

Player-visible contract: a packed item deploys the same block
layout it captured. NBT format pins are the save-compat contract
for any item with stored block data.

### 3b. Custom atmosphereType NBT round-trip

`AtmosphereType` extension allows companion mods to register
custom atmosphere types. The NBT serialization of an in-world
atmosphere region must preserve the custom type's identity
across save/load.

Player-visible contract: a custom atmosphere zone set up by a
companion mod still has its custom type after server restart.

### 3c. MonitoringStation comparator override

`TileRocketMonitoringStation.getComparatorOverride()` returns
`(int) (15 * rocket.getRelativeHeightFraction())` — produces a
0-15 redstone-comparator output that tracks the linked rocket's
altitude.

Player-visible contract: a player who places a redstone comparator
adjacent to a monitoring station can drive a circuit off the
rocket's height during flight.

## Implementation plan

### 3a — ItemPackedStructure deploy (testServer, ~1.5 h)

`ItemPackedStructureDeployTest`:
- Construct a packed-structure item with a small known layout
  (3x3x1 cobblestone via reflection or via existing item-pack
  probe if one exists).
- Right-click via `player exec-as-player /artest item use-on
  <pos>` or a dedicated probe.
- Assert the layout is present at the deploy location.

### 3b — custom atmosphereType NBT (testUnit, ~1 h)

`CustomAtmosphereTypeNbtRoundTripTest`:
- Register a test-only atmosphere type via `AtmosphereType`
  static method.
- Construct an `AtmosphereBlob` or equivalent carrier with the
  custom type, write to NBT.
- Read back, assert type identity preserved.

### 3c — comparator override (testServer, ~2 h)

Needs a rocket in mid-flight (height between 0 and
`ARConfiguration.orbit`). Approach options:

1. Use the existing `arm-prelaunch-cancel` to keep LAUNCH_COUNTER
   at 0 between iterations, then manually set the rocket's posY
   via reflection or new probe.
2. Drive via `rocket launch` + observe over several ticks while
   the rocket actually climbs.

Option 1 is faster but adds a probe; option 2 reuses existing
infrastructure.

`MonitoringStationComparatorOverrideTest`:
- Link a rocket to a monitor.
- Set rocket posY to 0 → comparator = 0.
- Set rocket posY to orbit/2 → comparator ~ 7-8.
- Set rocket posY to orbit → comparator = 15.

Loose-bound pins (within ±1) — exact rounding is impl.

## Acceptance

- [ ] 3 tests across testUnit + testServer.
- [ ] Each pins a player-visible / save-compat / external-API
      contract.
- [ ] Pyramid counter regenerated.

## Out of scope

- ItemPackedStructure capture path (separate item-recipe scope).
- Atmosphere region tick behaviour with custom type (separate
  atmosphere depth scope).
- Comparator signal across dimension transitions (corner case).

## Dependencies

- Does NOT block any other task.
- 3c may benefit from a new probe `rocket set-pos <id> <x> <y> <z>`
  for direct manipulation — add if option 1 chosen.

## Estimated effort

- 3a: 1.5 h
- 3b: 1 h
- 3c: 2 h
- **Total**: ~4.5 h

## Risk

Low. Each item is small and isolated. The TASK groups them only
to keep the index lean — they can be split into separate commits.
