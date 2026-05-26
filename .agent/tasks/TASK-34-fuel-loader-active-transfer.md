# TASK-34: Fuel loader active fluid transfer

## Ticket

- Source: 2026-05-25 Tier 2 audit, gap #9. Explicitly deferred by
  the audit itself with the rationale that the fixture path was
  unclear. Carried forward into 2026-05-26 audit.
- Status: **✅ Completed 2026-05-26** — see `.agent/tasks/README.md`
  Done table.
- Created: 2026-05-26.

## Actual scope shipped

**Phase 0 — investigation outcome**: NOT Obsolete. The blocker
description's "fixture rocket's fuel tanks lose
FLUID_HANDLER_CAPABILITY" referred specifically to the rocket's
`BlockFuelTank` tiles. The `with-fluid-cargo` fixture variant
(already in `TestProbeCommand` at the time of this TASK) replaces
2 fuel-tank positions with `advancedrocketry:liquidTank`
(TileFluidTank) blocks, and TileFluidTank's capability IS
preserved across the storage-chunk round-trip — already proven by
`MissionGasCompletionTest.gasCompletionFillsRocketFluidTilesWithConfiguredFluid`
which depends on the same path.

**Phase 1 — fixture variant**: pre-existing (no work). The
`with-fluid-cargo` variant ships in `TestProbeCommand` at line
5246+.

**Phase 2 — transfer tests** (`FluidLoaderActiveTransferTest`, 2
tests):

1. `loaderTransfersOxygenIntoRocketStorageLiquidTanks` —
   pre-fill loader's own tank with oxygen, link rocket
   (with-fluid-cargo), force-tick. End-state contract: rocket
   storage holds oxygen AND loader's tank has drained. Pinned
   end-state rather than synthetic delta because natural server
   ticks between probe commands already transfer fluid — the
   contract is direction-of-transfer, not exact tick budget.
2. `unloaderDrainsRocketStorageLiquidTanksIntoOwnTank` —
   pre-fill rocket storage via new `rocket storage-fluid-fill`
   probe, link unloader, force-tick. End-state contract:
   unloader's tank gained oxygen AND rocket storage drained.

**Probe addition**: `rocket storage-fluid-fill <entityId>
<fluidName> <amount>` — iterates `rocket.storage.getFluidTiles()`
and fills each via `FLUID_HANDLER_CAPABILITY`. Used by the
unloader test to pre-fill rocket tanks (which live in the
detached `WorldDummy`, not addressable via world coords).

## Context

`RocketInfrastructureSmokeTest.fluidLoaderTransfersFluidAfterLanding`
(see existing class) pins **placement + tick-stability** for the
fluid loader (`TileRocketFluidLoader`, loader meta=5) and unloader
(`TileRocketFluidUnloader`, loader meta=4) — but its javadoc
explicitly notes:

> Production loader transfer therefore depends on a CARGO-style
> fluid tank placed by the player after launch — out of headless
> scope.

What's pinned: the tile lifecycle survives 30 ticks without
crashing. What's NOT pinned: actual fluid actually moves from
loader's tank into the rocket's fluid-handling tiles, and vice
versa for the unloader.

## Why it matters

Fuel automation is a core mod-pack-tier feature. Players build
landing pads with fuel loaders so a returning rocket can be
re-fueled without manual hand-pumping. A regression that stops
the transfer silently breaks every multi-flight automation.

## Blocker

The fixture-rocket's fuel tanks lose their `FLUID_HANDLER_
CAPABILITY` when re-instantiated in the rocket's detached storage
chunk (per the existing test's javadoc). Two paths around this:

1. **Storage chunk capability re-attachment** — investigate
   whether the loss is structural (storage chunk is genuinely
   read-only at the capability layer) or fixable (the
   capability provider doesn't propagate, but could be patched).
   If structural: this gap is **Obsolete** — production loader
   simply doesn't have a way to operate on storage-chunk tanks,
   so testing it would test impossible behaviour.
2. **CARGO fluid tank fixture** — extend `fixture rocket` to
   place an `advancedrocketry:liquidTank` (TileFluidTank) as
   cargo inside the rocket's seat area, similar to how
   `with-cargo` variant places a chest. Production loader can
   then transfer into this tank.

Option 2 is the actionable blocker — needs a `fixture rocket
with-fluid-cargo` variant.

## Implementation plan

| Phase | Effort | Result |
|---|---|---|
| 0 | ~2 h | Investigate storage chunk capability loss. If structural: close as Obsolete. If fixture-fixable: design the `with-fluid-cargo` variant. |
| 1 | ~3 h | Extend `fixture rocket` to support `with-fluid-cargo` — places a liquidTank in the cargo bay. Update `RocketInfrastructureSmokeTest` to use it. |
| 2 | ~3 h | `FluidLoaderActiveTransferTest` — 2 tests: (a) loader transfers oxygen from its tank into rocket's liquidTank (b) unloader drains rocket's liquidTank into its own tank. |

## Acceptance

- [ ] 2 tests pinning active transfer in both directions.
- [ ] Loose-bound: "amount in destination > 0 after N ticks",
      not exact mB/tick.
- [ ] Pyramid counter regenerated.

## Out of scope

- Fluid type mismatch handling (loader's tank holds oxygen,
  rocket holds fuel). Separate gate.
- Multi-cargo permutations.

## Dependencies

- Does NOT block any other task.
- Phase 0 may flip this task to Obsolete.

## Estimated effort

- Phase 0: 2 h
- Phase 1: 3 h
- Phase 2: 3 h
- **Total**: ~8 h (if not Obsolete after Phase 0)

## Risk

Medium. Phase 0 outcome determines whether the rest is
achievable at all.
