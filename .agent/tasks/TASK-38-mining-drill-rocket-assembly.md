# TASK-38: IMiningDrill rocket-assembly stat aggregation

**Status**: ✅ Completed 2026-05-27
**Created**: 2026-05-27
**Source**: Gap Q from `.agent/audits/2026-05-27-full-coverage-audit.md`

## Context

`BlockMiningDrill` (registry `advancedrocketry:drill`,
`AdvancedRocketryBlocks.blockDrill`) is a placeable but TileEntity-
less block implementing `IMiningDrill`. It is wired into rocket
assembly via two scan paths
(`TileRocketAssemblingMachine.scanRocket` line 394;
`StorageChunk.recalculateStats` line 230) which sum
`IMiningDrill.getMiningSpeed(world, pos)` over every drill block in
the rocket's storage chunk and stash the total in
`stats.setDrillingPower(sum)`. The stat then feeds
`EntityRocket.getMissionFromInfrastructure` (line 1434) and
`MissionOreMining` — a non-zero `drillingPower` is the
player-visible "this rocket can mine asteroid ore" flag.

No test referenced `BlockMiningDrill` or `IMiningDrill` directly —
the chain `placed drill → drillPower stat → mission duration` was
untested end-to-end.

## Contract pinned

One server test pins both polarities of the IMiningDrill scan
branch:

- **Simple rocket → drillingPower = 0**: baseline, proves no other
  latent source contributes to the stat.
- **`with-mining-drill` variant rocket → drillingPower &gt; 0**:
  positive branch, proves the scan loop's `IMiningDrill` aggregation
  reaches the published `stats.getDrillingPower()` surface.

## Litmus

> "This test fails if production breaks the contract that **a
> rocket assembled with a placed `BlockMiningDrill` in its cargo
> column has a non-zero `stats.drillingPower`.**"

Reads as API-visible (MissionOreMining queries this stat) — passes
the SOP litmus.

## Result

- 1 server test in `RocketAssemblerMiningDrillStatTest`
- 1 new fixture variant `with-mining-drill` in
  `/artest fixture rocket` (drops a single drill block at
  `(rocketX+1, rocketY+3, rocketZ)` where columns above stay air →
  `getMiningSpeed` returns the sky-exposed 0.02f branch)
- `rocket info` probe extended with `drillingPower` field
- No production logic changed

## Out of scope

- Exact drillingPower magnitude (= 0.02f for one sky-exposed drill)
  — impl per SOP.
- The mission-duration formula
  `(360 / drillingPower) × asteroidDrillingMult × asteroidMiningTimeMult`
  — impl-side magnitude algebra inside `EntityRocket`. The fact
  that a non-zero drillingPower exists IS the contract; the formula
  is impl detail.

## Dependencies

- Requires `/artest fixture rocket` + `/artest rocket assemble` +
  `/artest rocket info`
- Does NOT block TASK-37 / TASK-39 (parallel batch).
