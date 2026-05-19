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
| TASK-03 | Test depth deepening + harness consolidation (A1/A2/A4/A5/A6/A7 + B1/B2/B4/C) | ✅ partial — A3 and B3 deferred to TASK-10 |

## Backlog — prioritised

Priority is by **gameplay impact × regression-blast-radius**. P0 items
ship silent gameplay breakage if untouched code regresses.

### 🔴 P0 — schedule next

1. **TASK-04** — Multiblock machine depth (Warp / Laser Drill /
   Elevator / Black Hole / Space Laser). ~18-22 h, 5-6 sessions.
   These are late-game tiles, all currently smoke-only. Highest
   gameplay impact per hour invested.
2. **TASK-07** — Rocket flight cycle beyond launch (orbit / descent /
   landing / dismantle). ~14-17 h. Closes the main gameplay loop;
   TASK-03 A1 only got us to `isInFlight=true`.
3. **TASK-08** — ASM coremod safety net. ~13-18 h. Hardest to write,
   highest single-point-of-failure risk. Schedule when team has
   bandwidth for the bytecode work.

### 🟡 P1 — broad surface, medium impact

4. **TASK-10** — FakePlayer probe + TASK-03 tail (A3 / A2 remainder /
   B3). ~14-18 h. **Unblocks** TASK-05 and TASK-06 reward tests.
5. **TASK-05** — Item-behaviour suite. ~16-20 h. ~25 % of mod
   surface; 0 % isolated coverage today. Soft-requires TASK-10.
6. **TASK-09** — Per-satellite-type behavioural depth. ~10-12 h.
   Player-facing passive-production layer.

### 🟢 P2 — narrower, lower urgency

7. **TASK-06** — Mission system depth. ~10-12 h. Soft-requires TASK-10
   for reward tests. Player-facing but small surface.

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
          ├─► TASK-07  (rocket cycle)
          ├─► TASK-08  (ASM)
          ├─► TASK-09  (satellite types)
          └─► TASK-10  (FakePlayer + A3/A2tail/B3)
                  │
                  ├─► TASK-05  (items — soft dep)
                  └─► TASK-06  (missions — soft dep for rewards)
```

TASK-04, TASK-07, TASK-08, TASK-09 are independent of each other and of
TASK-10. TASK-05 and TASK-06 work without TASK-10 but with lower
coverage (reward / EntityPlayer paths skipped).

## Suggested session ordering

If picking the next session:

1. **If team wants the biggest player-visible coverage win**:
   start TASK-04 Phase 1 (Warp Controller depth).
2. **If team wants the biggest risk-reduction win**: start TASK-08
   Phase 1 (ASM golden-snapshot infrastructure).
3. **If team wants to unblock the most other tasks**: start TASK-10
   Phase 1 (FakePlayer probe).
4. **If team wants the most "items checked off"**: start TASK-09
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
