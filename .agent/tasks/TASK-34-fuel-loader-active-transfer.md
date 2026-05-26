# TASK-34: Fuel loader active fluid transfer

## Ticket

- Source: 2026-05-25 Tier 2 audit, gap #9. Explicitly deferred by
  the audit itself with the rationale that the fixture path was
  unclear. Carried forward into 2026-05-26 audit.
- Status: **Blocked** — see Blocker section.
- Created: 2026-05-26.

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
