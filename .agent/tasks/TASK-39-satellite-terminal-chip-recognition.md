# TASK-39: TileSatelliteTerminal chip recognition + erase button

**Status**: ✅ Completed 2026-05-27
**Created**: 2026-05-27
**Source**: Gap R from `.agent/audits/2026-05-27-full-coverage-audit.md`

## Context

The Satellite Control Center
(`advancedrocketry:satelliteControlCenter` → `TileSatelliteTerminal`)
is the player-facing GUI for querying a satellite's data remotely.
Its server-side `writeDataToNetwork(packetId 22)` ladders four
mutually-exclusive status codes (`TileSatelliteTerminal.java` lines
84-104) that the client translates into the visible "no link / no
power / out of range / connected" GUI text. The destructive
`onInventoryButtonPressed(1)` branch erases the chip AND removes
the linked satellite from `DimensionProperties`.

Before this task, `TileSatelliteTerminal` had zero test coverage —
no class reference anywhere in the test suite. Sister of TASK-36a
(TerraformingTerminal) which was similarly untested before TASK-36.

## Contract pinned

Four server tests in `SatelliteTerminalChipRecognitionTest`:

1. **Chip + power → status 3**
   (`chippedTerminalWithPowerReachesStatus3`) — optical satellite
   chip in slot 0 + 1000 RF injected → status 3, non-zero
   powerPerTick, non-negative maxData.
2. **Empty slot → status 0** (`unchippedTerminalReportsNoLink`) —
   empty terminal with power → status 0. Pins that the no-chip
   branch wins regardless of power state.
3. **Chip without power → status 1**
   (`chippedTerminalWithoutPowerReportsNoPower`) — chip loaded but
   energy=0 → status 1. Pins that energy gating is independent of
   chip recognition.
4. **Erase button → satellite removed + chip blanked**
   (`pressEraseRemovesSatelliteFromDimAndBlanksChip`) — calling
   `onInventoryButtonPressed(1)` removes the linked satellite from
   its dim's `DimensionProperties` AND blanks the chip NBT
   (via `ItemSatelliteIdentificationChip.erase`).

## Litmus

> "This test fails if production breaks the contract that **the
> Satellite Control Center surfaces the correct status code per
> (chip × power) combination AND the destructive erase button
> deregisters the satellite globally.**"

Reads as player-visible (GUI text + dim properties) — passes the
SOP litmus.

## Result

- 4 server tests in `SatelliteTerminalChipRecognitionTest`
- 3 new probe subcommands under `/artest satellite-terminal`:
  - `info <dim> <x> <y> <z>` — server-side replica of the GUI
    status logic + slotSatId / energy / powerPerTick / data /
    maxData
  - `load-chip <dim> <x> <y> <z> <satId>` — programs a fresh
    `ItemSatelliteIdentificationChip` via `chip.setSatellite(stack,
    sat)` and places it in slot 0 (sister of
    `/artest terraforming terminal-load-chip`)
  - `press-erase <dim> <x> <y> <z>` — invokes the production
    `onInventoryButtonPressed(1)` server-side; reports pre/post
    chip NBT state + dim-side satellite registration
- No production logic changed

## Out of scope

- Status 2 (out-of-range, `PlanetaryTravelHelper.isTravelAnywhere
  InPlanetarySystem == false`) — requires a second dim in a
  different planetary system, which the shared harness doesn't
  pre-register. The branch is defended at the helper level by
  `PlanetaryTravelHelperTest` (TASK-09 Gap 3); chaining that into
  the terminal's dispatch is impl, not a contract divergence.
- Inner data accrual loop (the data field actually filling as the
  satellite ticks) — covered by TASK-29 scanning satellite tick
  contracts. This task pins the terminal's *read* surface, not the
  satellite's *write* surface.

## Dependencies

- Requires `/artest satellite-builder build` (TASK-33 surface) to
  produce the optical satellite the chip points at.
- Requires `/artest energy inject` (existing pre-TASK-37 probe).
- Does NOT block TASK-37 / TASK-38 (parallel batch).
