# TASK-13: Pipe end-to-end coverage (placed-block pipes)

## Ticket

- Source: TASK-02 deferred bullet ("Pipe end-to-end"), promoted into
  a tracked task on 2026-05-23 during the SSOT cleanup.
- Status: **Blocked** — production prerequisite missing.
- Created: 2026-05-23.

## Context

The fluid / data / energy pipe **block** entities are part of the
mod's wire ecosystem (`BlockLiquidPipe`, `BlockDataCable`,
`BlockEnergyCable` + their tiles). Coverage today exists for the
network classes (`CableNetwork`, `EnergyNetwork`, `HandlerCableNetwork`,
`PipeNetworkHandlerDeepTest`) but **not** for an end-to-end shape
where blocks are actually placed in a world and the network forms
from neighbour-notify events.

The end-to-end shape is the only one that would catch regressions in:

- placement / break events feeding the handler
- `IBlockState` ↔ tile association round-trip
- cross-chunk merge on chunk load
- save / load of placed pipe networks

## Blocker — production prerequisite

`src/main/java/zmaster587/advancedRocketry/AdvancedRocketry.java`
lines **782-787** have the three pipe-block constructions and
registrations commented out:

```java
//Cables
//TODO: add back after fixing the cable network
//AdvancedRocketryBlocks.blockFluidPipe = new BlockLiquidPipe(...)
//AdvancedRocketryBlocks.blockDataPipe  = new BlockDataCable(...)
//AdvancedRocketryBlocks.blockEnergyPipe = new BlockEnergyCable(...)
//LibVulpesBlocks.registerBlock(... blockDataPipe ...);
//LibVulpesBlocks.registerBlock(... blockEnergyPipe ...);
//LibVulpesBlocks.registerBlock(... blockFluidPipe ...);
```

While these registrations stay commented out:

- the blocks are not in the registry
- placing them via `setBlockState` no-ops to air
- there is nothing to test

**Unblocking step**: a separate production-side task to "fix the
cable network" (whatever the original commenter meant) and reinstate
the three registrations. That task is out of scope here — TASK-13
explicitly does NOT take it on. The TASK-12 fixes for #1/#2/#3 in
the historical ledger (`CableNetwork.merge`, `HandlerCableNetwork`)
may already cover what the original TODO was waiting for, but
verifying that requires a production-side reviewer who knows the
intended network semantics. Defer to a maintainer call.

## What this task is NOT

- Not network-internal coverage. Already done in
  `PipeNetworkHandlerDeepTest` (testUnit) and the cable/energy
  network suites flipped by TASK-12.
- Not pipe-item / pipe-recipe coverage. Items are registered;
  separate scope if ever needed.

## Out-of-scope deferrals (when unblocked)

When the production blocker clears, the following sub-suites become
shippable. Listed for reference, NOT for present scoping.

- `PipePlacementE2ETest` (server-tier) — place 3 pipes in a line,
  break the middle, assert network splits into two.
- `PipeChunkBoundaryE2ETest` — place pipe pair across chunk boundary,
  unload + reload the far chunk, assert network preserves.
- `PipeNbtPersistenceE2ETest` — place a network, save world, restart
  server (existing `restart-server` probe), assert network restored.
- `PipeFluidFlowE2ETest` — fluid pipe between two tanks, source has
  fluid → sink fills.
- `PipeEnergyFlowE2ETest` — energy pipe between source + sink RF
  consumers, sink accrues.

Estimated effort once unblocked: ~6 h across 5 server-tier classes,
~25 tests total. Reuses `AbstractSharedServerTest` + existing
`/artest` probes (`network-summary` if added).

## Resume conditions

Promote out of Blocked when:

1. Lines 782-787 of `AdvancedRocketry.java` are uncommented and the
   mod boots without crash, **and**
2. A maintainer confirms the original "fix the cable network" TODO
   is closed (likely subsumed by TASK-12 fixes #1/#2/#3, but
   confirmation required).

Until then: keep this task in **Blocked**, do not touch.

## Dependencies

- Requires: production-side TODO resolution at
  `AdvancedRocketry.java:782-787`.
- Does NOT block: anything currently planned. The placeholder is
  pure forward-coverage.
