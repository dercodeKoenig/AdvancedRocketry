# Context Marker: task02-round2-tile-rocket-eod

**Created**: 2026-05-19 06:00 local
**Branch**: `feature/tests`
**Status**: ✅ TASK-02 round 2 complete — Phase 4 (tile machines) and
Phase 1 deep (rocket launch event chain) landed. TASK-02's main
checklist is now ✅ across all P0/P1 phases except the two explicitly
deferred P2 items (mod compat + client rendering).

---

## TL;DR

- **+12 server tests** across `TileMachineDepthTest` (8) and
  `RocketLaunchEventTest` (4).
- **testServer** went 103 → **115** (+12), **0 failures**.
- Full local pyramid: testUnit 142 / testIntegration 80 /
  testServer 115 / testClient 6 = **343 / 0 / 3** passing.
- TASK-02 P0/P1 phases all marked done in the task doc; P2 phases
  9 & 10 explicitly deferred with rationale.

---

## What was added in round 2

### Phase 4 — Tile machines (`TileMachineDepthTest`, 8 server tests)

Each test uses the existing `/artest place` + `/artest energy stored` /
`/artest tile force-tick` / `/artest hatch read` / `/artest fluid stored`
probes. Tile placement helper does an `air` pre-clear so overwriting
terrain doesn't trip the setBlockState gate.

Tiles covered:
- `solarGenerator` → exposes CapabilityEnergy + survives force-tick
  (registry-name surprise: the plain "solarPanel" block is decorative
  and has NO tile entity; the machine is registered as "solarGenerator"
  internally. Pinned in a class-level comment).
- `liquidTank` → place succeeds, tile is a FluidTank/FluidHatch family.
- `forceFieldProjector` → must be ITickable (probe refuses to tick a
  non-ITickable).
- `guidanceComputer` → hatch-read probe finds it as IInventory; size > 0.
- `oxygenVent` → exposes CapabilityEnergy (RF consumer) +
  force-tickable.
- `blockPump` → place succeeds.
- `satelliteBuilder` → tileClass reports SatelliteBuilder.
- Virgin counter-test: a fresh position must report "no tile entity"
  (sanity for the test harness state).

### Phase 1 deep — Rocket launch events (`RocketLaunchEventTest`, 4)

Builds an assembled rocket via the existing
`/artest fixture rocket simple` + `/artest rocket assemble` chain, then
drives the launch probe through every supported mode:
- `launch <id> false force` — `setInFlight(true)` bypass; pin flag
  flips + persists across a separate `info` probe call.
- `launch <id> true instant` — production `rocket.launch()` path; pin
  the response wiring (ok / mode / fuelFilled echo). The fixture rocket
  has no real launchpad context so isInFlight may stay false — this is
  documented behaviour, not a bug.
- `launch 9999999 false force` — counter-test: unknown id must NOT
  silently succeed.
- Double-launch idempotency: a second `force` call doesn't flip the
  flag back off, no crash.

Player dim-change event side effects (PlanetEventHandler.onPlayerChange
…) still **deferred** — requires new probe verbs (`/artest event …`)
or harness-level player injection.

---

## Bugs / surprises captured as tests

- **"solarPanel" is the decorative cube**, "solarGenerator" is the
  machine. A future test author following intuition would write
  `place(advancedrocketry:solarPanel, …)` and get a confusing
  "no tile entity" — the new comment in `TileMachineDepthTest` heads
  off that landmine.
- **vanilla `setBlockState(air, currently-air)` returns false** — air
  pre-clear at a virgin position can't assert `placed=true`. The
  test helper swallows the pre-clear result; the dedicated
  `virginAirPositionHasNoTileEntity` test sidesteps the helper entirely.

---

## Full pyramid state (this branch, post-round-2)

| Layer            | Result        | Δ from 2026-05-18 EOD |
|------------------|---------------|-----------------------|
| testUnit         | 142 / 0 / 0   | (unchanged)           |
| testIntegration  | 80 / 0 / 0    | (unchanged)           |
| testServer       | 115 / 0 / 3   | +12                   |
| testClient       | 6 / 0 / 0     | (unchanged)           |
| **Total**        | **343 / 0 / 3** | +12 net             |

testClient still requires `DISPLAY=:77 LIBGL_ALWAYS_SOFTWARE=1` per
the GL SOP.

---

## TASK-02 status

All P0/P1 phases now checked in
`.agent/tasks/TASK-02-functional-coverage-expansion.md`:

- ✅ Phase 0 — probe gaps
- ✅ Phase 1 — events (shallow + deep rocket launch)
- ✅ Phase 2 — worldgen
- ✅ Phase 3 — armor / breathing
- ✅ Phase 4 — tile machines (7 representative tiles + counter-test)
- ✅ Phase 5 — recipes
- ✅ Phase 6 — missions
- ✅ Phase 7 — networks (unit slice; end-to-end deferred)
- ✅ Phase 8 — stations depth (dock/undock deferred)
- ⏸ Phase 9 — mod compat (companion mods absent)
- ⏸ Phase 10 — client rendering (wrong tool)
- ✅ Phase 11 — round-2 validation + this marker

Tests went from **263 baseline** at the start of TASK-02 to **343 now**
(+80 net), 0 failures, 3 pre-existing SKIPs.

---

## Restore instructions

```
Read .agent/.context-markers/2026-05-19-0600_task02-round2-tile-rocket-eod.md
Read .agent/.context-markers/2026-05-18-2300_task02-autonomous-execution-eod.md
Read .agent/tasks/TASK-02-functional-coverage-expansion.md
```

Open items for a future session (in priority order — none of these
blocks releasing the SMART pyramid as-is):

1. **Phase 4 round 2**: extend tile coverage to the remaining
   ~5 tile families (suit workstation, unmanned vehicle assembler,
   landing pad isolated, fueling station isolated, terraformer).
2. **Phase 1 player events**: probe + tests for
   `/artest event playerJoinPlanet`, dim-change side effects.
3. **Phase 7 end-to-end**: real pipe multiblock with merge/split.
4. **Phase 8 dock/undock**: needs new probe + multi-boot harness.
5. **Phase 9 mod compat** when GC / MO are in classpath.
6. **Phase 10 visual regression** as a separate proposal.
