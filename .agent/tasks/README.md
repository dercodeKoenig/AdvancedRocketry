# Test-coverage tasks — roadmap and dependency graph

## Current state (post-TASK-03)

Pyramid: **398 / 0 / 3** (testUnit 162 / testIntegration 80 /
testServer 150 / testClient 6).
testServer wall time: **8m 27s** (50 % faster than pre-B2).
`_documentsKnownBug` count: **4** real production bugs pinned.

## Done

| ID | Title | Status |
|---|---|---|
| TASK-01 | SMART per-scenario depth coverage | ✅ |
| TASK-02 | Functional coverage expansion (Phases 0–8, 11) | ✅ |
| TASK-03 | Test depth deepening + harness consolidation (A1/A2/A4/A5/A6/A7 + B1/B2/B4/C) | ✅ partial — A2 tail + B3 deferred to TASK-10; A3 reframed as testClient e2e (TASK-10b) |
| TASK-04 | Multiblock machine depth (Warp / Laser Drill / Elevator / Black Hole / 12 multiblocks) | ✅ |
| TASK-07 | Rocket flight cycle beyond launch (orbit / dim-transition / descent / landing / dismantle / failure modes) | ✅ |
| TASK-08-mixin | Rewrite ASM coremod (`ClassTransformer.java` + vendored HookLib) to Mixin; behavioural pin for `setBlockState` hook; existing 239-test suite implicitly pins gravity + atmosphere hooks | ✅ |
| TASK-10 | TASK-03 deferred tail — A2 remainder (4 deep-tile tests: FluidTank NBT round-trip, UV-vs-Rocket assembler class identity, SuitWorkStation assembly, FuelingStation matched accounting) + B3 single-method-smoke suite-grouping (MachineDomainSmokeSuite, ServerBootSmokeSuite) | ✅ |
| TASK-10b | testClient e2e player-event coverage (atmosphere bookkeeping, space-dim guard, advancements, sleep/flint vacuum guards, low-gravity fall) — 5 e2e suites, 15 pins, 9 new `/artest` verbs | ✅ |
| TASK-09 | Per-satellite-type behavioural depth — `SatelliteTickBehaviourTest` (4 pins: base power accrual + cap + SatelliteData accumulation + maxData cap) + `SatelliteTypeBehaviourTest` (3 pins: IUniversalEnergyTransmitter marker + BiomeChanger terraforms + WeatherController setBlockState) + 9 new `/artest satellite` / `block biome-at` verbs | ✅ |

## Backlog — prioritised

Priority is by **gameplay impact × regression-blast-radius**. P0 items
ship silent gameplay breakage if untouched code regresses.

### 🔴 P0 — schedule next

*(TASK-04 multiblock depth, TASK-07 flight cycle, TASK-08-mixin coremod
rewrite: closed — see Done table.)*

### 🟡 P1 — broad surface, medium impact

4. **TASK-05** — Item-behaviour suite. ~16-20 h. ~25 % of mod
   surface; 0 % isolated coverage today. EntityPlayer-touching items
   belong in the **testClient** e2e layer, not testServer.

### 🟢 P2 — narrower, lower urgency

5. **TASK-06** — Mission system depth. ~10-12 h. Reward-grant tests
   that need a real EntityPlayer go into **testClient** e2e.

### Already-known deferred (no task doc yet — surface in a future plan)

- Phase 9 — JEI / GalacticCraft / MatterOverdrive integration tests
  (needs companion mods in classpath; TASK-02 deferred).
- Phase 10 — Visual regression (Storybook + Chromatic equivalent for
  Minecraft client; TASK-02 deferred — own proposal).
- Pipe end-to-end (placed pipe blocks) — blocked by commented-out
  block registrations at `AdvancedRocketry.java:782-787`. Reinstate
  before any of these can be tested.
- Production-side `WorldCommand` (`/ar`) — 991 LoC, 0 coverage. Worth
  its own task when prioritised.
- Production bug fixes for the 4 currently-pinned
  `_documentsKnownBug` tests — separate ticket; flip assertions
  afterwards.

## Dependency graph

```
TASK-03 ──┬─► TASK-04  (multiblock)
          ├─► TASK-05  (items)        ─┐
          ├─► TASK-06  (missions)     ─┤── EntityPlayer paths
          ├─► TASK-07  (rocket cycle) ─┤   live in testClient e2e
          ├─► TASK-08  (ASM)           │   (TASK-10b proposal)
          ├─► TASK-09  (satellite types)
          └─► TASK-10  (A2 tail + B3 grouping)
```

All P0/P1 tasks are independent of each other. TASK-05 / TASK-06 are
NOT blocked by TASK-10 — their EntityPlayer-touching coverage is the
responsibility of testClient e2e (planned as TASK-10b), not of a
FakePlayer injection.

## Suggested session ordering

If picking the next session:

1. **If team wants the biggest player-visible coverage win**:
   start TASK-10b (testClient e2e player-event coverage — also unlocks
   the deferred inventory-distance-bypass pin from TASK-08-mixin).
2. **If team wants quick wall-time win**: start TASK-10 Phase 2 (B3
   suite-grouping — mechanical, ~3 h).
3. **If team wants the most "items checked off"**: start TASK-09
   (smallest task; ~3-4 sessions).

## Conventions

All TASK-NN docs share a structure:

- **Context**: what's currently uncovered + why it matters.
- **Implementation Plan**: phased; each phase ~2-5 h.
- **Technical Decisions**: same `no production logic changes` rule
  as TASK-01 §15. New probe verbs documented inline.
- **Dependencies**: explicit `requires` / `does NOT block` calls.
- **Completion Checklist**: phase-by-phase; flipping to ✅ requires
  a green pyramid run + an EOD marker.
- **EOD marker**: in `.agent/.context-markers/` with the date and a
  short slug. The `.active` file points at the most recent marker
  for `/nav:start` to pick up.

## Notes on `_documentsKnownBug`

The TASK-02 / TASK-03 audit surfaced 4 real production bugs we chose
NOT to fix in-scope (per the "no production logic changes" rule).
Tests pin the **current** behaviour as expected so a future fix has to
update them:

1. `HandlerCableNetwork:67` assertion polarity inverted.
2. `CableNetwork.merge` addAll-before-dedupe ordering.
3. `EnergyNetwork.merge` battery-migration cascade from (2).
4. `SpaceStationObject:801` writes `"autoLand"`, reads `"occupied"`.

A separate **bug-fix ticket** should address all four; once fixed,
the corresponding `_documentsKnownBug` tests flip to expected-passing
semantics.
