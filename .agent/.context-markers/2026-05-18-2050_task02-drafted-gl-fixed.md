# Context Marker: task02-drafted-gl-fixed

**Created**: 2026-05-18 20:50
**Branch**: `feature/tests` (was at `70410da4` from earlier today)
**Status**: ✅ TASK-02 drafted, GL renderer fix found + SOP'd, full
pyramid green on this Linux sandbox (191/0/3 — counts identical to
fix/weather's Windows box result of 191/0/0 minus the 3 pre-existing
PipeNetworkSmokeTest skips).

---

## TL;DR

- **TASK-02 drafted**: `.agent/tasks/TASK-02-functional-coverage-expansion.md`
  — 11 phases covering event handlers, worldgen, armor, tile machines,
  recipes, missions, network handlers, stations, mod compat, plus a
  capped client/rendering phase. ~50-65 h estimated effort, organized
  by risk × effort with explicit P0/P1/P2 tiers.
- **GL renderer for `testClient` fixed on this Linux sandbox.** Root
  cause was the wrong `DISPLAY` (`:99` had no connected output → LWJGL
  `LinuxDisplay.getAvailableDisplayModes` NPE'd on the empty XRandR
  mode list). Switch to `:77` + `LIBGL_ALWAYS_SOFTWARE=1` and all 6
  client tests pass. SOP at
  `.agent/sops/development/client-tests-on-linux.md`.
- **Local pyramid (post-fix)**:
  - testUnit 87 / 0 / 0
  - testIntegration 80 / 0 / 0
  - testServer 90 / 0 / 3 (pre-existing PipeNetworkSmokeTest skips)
  - testClient **6 / 0 / 0** when run fresh; one known soft-GL flake
    in `RocketBuilderGuiE2ETest` that re-passes in isolation (~1 in N).

---

## What changed in this session

### Phase 5 closeout (committed in `70410da4`, earlier today)

- `/artest planet info <dim>` extended from 15 to 20 fields.
- `WEATHER_MODE_SHARED` retired across `AdvancedRocketryTestConstants`,
  `WeatherBaselineTest`, `build.gradle.kts`, `src/test/README.md`.
- TASK-01 Phase 5 closed.

### TASK-02 drafted (this commit)

`.agent/tasks/TASK-02-functional-coverage-expansion.md` — full plan.
Phases:

| # | Phase                                  | Tier | Est.    |
|---|----------------------------------------|------|---------|
| 0 | Probe gap audit + `case "help"`        | —    | ~1 h    |
| 1 | Event handlers (Planet/Rocket/Cable…)  | P0   | 6-8 h   |
| 2 | World generation (ChunkProvider*)      | P0   | 8-10 h  |
| 3 | Armor / suit / breathing               | P0   | 4-5 h   |
| 4 | Tile machines depth (≥10 tiles)        | P1   | 10-12 h |
| 5 | Recipes (10 Recipe* classes)           | P1   | 3-4 h   |
| 6 | Missions (3 mission classes)           | P1   | 3-4 h   |
| 7 | Pipe network handlers + un-skip 3      | P1   | 4-5 h   |
| 8 | Stations (docking/fuel/multi-station)  | P1   | 3 h     |
| 9 | Integration compat (GC, MO, JEI)       | P2   | 4-6 h   |
| 10| Client rendering (capped scope)        | P2   | deferred|
| 11| Final pyramid validation + report      | —    | 2 h     |

Audit summary (Explore agent, recorded in the task doc): ~480 source
files; existing 191 tests touch ~30-35 % of subsystem breadth. **Zero
coverage** on `event/` (1 261 LoC), `world/` worldgen (61 files),
`recipe/` (10 classes), `mission/` (3 classes, 423 LoC), `integration/`
(51 files), `client/` (6 files, 1 282 LoC).

### GL fix for `testClient` (SOP)

`.agent/sops/development/client-tests-on-linux.md`. Key steps:

1. Use `DISPLAY=:77` (or any Xvfb display that reports a connected
   output via `xrandr`). `:99` on this sandbox has none.
2. Export `LIBGL_ALWAYS_SOFTWARE=1` to suppress Mesa loader spam and
   route through `llvmpipe`.
3. Run normally:
   ```bash
   DISPLAY=:77 LIBGL_ALWAYS_SOFTWARE=1 \
     ./gradlew testClient \
       -Dnet.minecraftforge.gradle.check.certs=false \
       --no-daemon --console=plain
   ```

Known soft-GL flake noted: GUI right-click → `openGui` → `displayGui`
round-trip occasionally drops on the first attempt; isolated re-run
passes. Documented in SOP under "Known flakes on software GL".

---

## Git state (target after this marker is committed)

```
On branch feature/tests
$ git log --oneline -3
<this-commit>  docs: TASK-02 draft + GL SOP + session marker
70410da4       test: close TASK-01 Phase 5 (planet info + weather audit + shared retire)
0bb704c4       docs: add marker for fix/weather → feature/tests merge
```

`origin/feature/tests` will be pushed after commit.

---

## Restore instructions

```
Read .agent/.context-markers/2026-05-18-2050_task02-drafted-gl-fixed.md
Read .agent/tasks/TASK-02-functional-coverage-expansion.md
Read .agent/sops/development/client-tests-on-linux.md
```

Then pick a TASK-02 phase to start. Recommended order per the task doc:
Phase 0 (probe audit, ~1 h) before any of the per-subsystem phases.
