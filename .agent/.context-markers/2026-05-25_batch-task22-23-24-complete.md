# Marker — 2026-05-25 batch: TASK-22 + TASK-23 + TASK-24 complete

**Branch**: `feature/tests`
**Mode**: autonomous batch run, three tasks back-to-back.

## What shipped

| Task | Tests | New probes | Notes |
|---|---|---|---|
| TASK-23 SealDetector branches | 2 server | `seal-detector add-block-ban / remove-block-ban` | `notfullblock` documented as unreachable; Phase 4 client mirror skipped (probe replicates dispatch 1:1) |
| TASK-22 UV-assembler delta | 4 server (2 + 2) | `assembler max-y` + `assembler pad-bounds` + `entityClass` in rocket info + `fixture uv-rocket` | Phase 3 mount-eligibility deferred (entity-class pin covers transitively) |
| TASK-24 SpaceArmor CHEST drain | 3 client | `player equip-space-chest` + `player held-air-component-route` | testClient requires `xvfb-run` wrapper on this headless dev box |

**Total**: 9 new tests (6 server + 3 client) across 4 new test classes,
6 new probe verbs / probe response fields.

## Pyramid

237 / 80 / **356** / **44** = **717** (+9 from 708 at TASK-19 close).
Counter regenerated via `grep -rc '@Test$' src/test/.../{unit,integration,server,client}/`.

## Decisions made autonomously

1. **TASK-23 `notfullblock` branch is unreachable for vanilla+AR's
   block set** — analysis recorded in the test-file javadoc.
   Documented in the TASK file as well, NOT logged to the bug ledger
   per the CLAUDE.md "nothing observable = impl trivia, not bug"
   rule.

2. **TASK-22 bounds delta via reflection over fixture-observation** —
   the original plan suggested building a tall-tower fixture to
   observe bounds truncation. Switched to reflective constants probe
   (`assembler max-y`) — same player-visible contract pinned at
   fraction of the wall-time. Phase 3 mount eligibility skipped:
   `EntityStationDeployedRocket extends EntityRocket` and inherits
   `processInitialInteract`, so the entity-class pin from Phase 2
   already establishes "different entity class → different
   downstream behaviour".

3. **TASK-24 testClient probe correctness discovered mid-run** —
   first attempt used the existing `held-air` probe, which
   delegates to `ItemAirUtils.INSTANCE.getAirRemaining` →
   reads only the static `"air"` NBT key. `ItemSpaceChest` stores
   its O2 in embedded components (capability route), so that probe
   returned 0. Added `held-air-component-route` probe that calls
   `chest.getItem().getAirRemaining(stack)` directly (which
   dispatches into ItemSpaceChest's component-walking override).
   Test passed once probe was correct.

4. **testClient harness needs `xvfb-run`** — LWJGL's
   `LinuxDisplay.init` NPEs without a display. Xvfb is available
   on this box (`/usr/bin/xvfb-run`) — used `xvfb-run -a ./gradlew
   testClient ...` for validation. CI / dev-onboarding docs should
   surface this. The existing TASK-10b Phase 7 / OxygenSuitClient
   tests share the same requirement; they presumably ran in an
   env that already had X11.

## Commits

| SHA (pending push) | Subject |
|---|---|
| TBD | test: TASK-22 + 23 + 24 autonomous batch (9 tests, 6 probes) |

## Open follow-ups

- **F8 / F9** flake watch — still 1/5 toward Obsolete.
- **Backlog drained** except watching tasks (TASK-15) and
  investigation-complete (TASK-16). Next coverage batch needs new
  audit input.

## Bug ledger

Drained. No new bugs found during this batch. The TASK-23
`notfullblock` finding is a code smell (dead branch), not a bug
(no observable consequence — partial blocks already hit "other"
which has a sensible player message).

## Resumption

Backlog is essentially empty for autonomous TDD work. Next-session
options:

1. **Fresh audit**: review `.agent/sops/development/testing-principles.md`-
   style coverage of newly-added or recently-modified code areas.
2. **Flake watch**: another 10× sweep if F8/F9 want characterisation.
3. **User-directed**: wait for next feature/bug request.
