# TASK-37: Nuclear engine rocket-assembly thrust aggregation

**Status**: ✅ Completed 2026-05-27
**Created**: 2026-05-27
**Source**: Gap P from `.agent/audits/2026-05-27-full-coverage-audit.md`

## Context

The nuclear-engine block family — `BlockNuclearRocketMotor`,
`BlockNuclearCore`, `BlockNuclearFuelTank`, and the
`IRocketNuclearCore` marker interface — was fully wired into rocket
assembly (registered in `AdvancedRocketry.java`, scanned by both
`TileRocketAssemblingMachine.scanRocket` lines 386-395 and
`StorageChunk.recalculateStats` lines 222-224) but **no test
referenced any of these classes**. A regression that broke the
nuclear thrust aggregation, the `IRocketNuclearCore` cohesion check
(line 386: core's `belowPos` must be `IRocketEngine` or
`IRocketNuclearCore`), or the final `stats.thrust = max(mono, bi,
nuclearTotal)` arithmetic would have shipped undetected.

## Contract pinned

Two paired server tests share one chassis layout (2× nuclear motors
+ 6 nuclear fuel tanks for the COMBINEDTHRUST gate) and differ only
in core placement, so the resulting `stats.thrust` delta isolates
the cohesion check:

1. **Cores stacked above motors → thrust &gt; 0**
   (`nuclearCoreAboveMotorContributesNuclearThrust`) — 2 cores
   placed directly above 2 nuclear motors → `reactorLimit > 0` →
   `nuclearTotal > 0` → assembly succeeds with positive thrust.
2. **Core misplaced (no engine/core below) → scan rejects with
   NOENGINES** (`misplacedNuclearCoreFailsAssemblyWithNoEngines`) —
   single core at center column where below=air → cohesion fails →
   `reactorLimit=0` → `nuclearTotal=0` → `stats.thrust=0` →
   `getThrust() <= getNeededThrust()` gate at
   `TileRocketAssemblingMachine` line 457 fires → status NOENGINES.

## Litmus

> "This test fails if production breaks the contract that **a
> rocket built with a chemical-thrust-free nuclear engine stack
> only succeeds when each nuclear core sits directly above an
> IRocketEngine or another IRocketNuclearCore.**"

Reads as player-visible (assemble GUI status / chat error message)
— passes the SOP litmus.

## Result

- 2 server tests in `NuclearEngineRocketAssemblyTest`
- 2 new fixture variants in `/artest fixture rocket`:
  - `with-nuclear-stack` — 2 nuclear motors + 2 cores above + 4
    nuclear fuel tanks
  - `with-nuclear-misplaced` — 2 nuclear motors + 1 center-column
    core (uncohered) + 5 nuclear fuel tanks
- All existing rocket-fixture consumers
  (`RocketAssemblySmokeTest`, `UvAssembler*Test`) regression-green
- No production logic changed

## Out of scope

- Exact thrust magnitudes (= 35/motor × `nuclearCoreThrustRatio`)
  — impl per SOP. The `nuclearCoreThrustRatio` config flows through
  `ARConfigurationTest`.
- Nuclear core stack chaining (core above core above motor) — same
  cohesion code path, already exercised by the 2-core stack.

## Dependencies

- Requires `/artest fixture rocket` (TASK-07 era)
- Requires `/artest rocket assemble` + `/artest rocket info`
  (TASK-07 era)
- Does NOT block TASK-38 / TASK-39 (parallel batch).
