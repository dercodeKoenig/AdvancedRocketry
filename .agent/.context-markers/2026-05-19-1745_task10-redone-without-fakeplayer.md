# Context Marker: TASK-10 re-done without FakePlayer (Phase 2 ✅, Phase 1 partial)

**Created**: 2026-05-19 17:45 local
**Branch**: `feature/tests`
**Status**: Phase 2 (B3) complete. Phase 1 (A2 remainder) 2 of 4
shipped; 2 deferred behind documented probe-surface additions.

---

## What happened this session

1. **Reverted** the earlier FakePlayer-direction commit (`d0c3cba`).
   The TASK-10 draft used `FakePlayer` injection in `testServer` to
   cover player-event behaviour; the user clarified that
   player-behaviour tests belong in the existing `testClient` (§2.4
   real GL client + dedicated server) source set, not `testServer`.
   The two "production NPEs" pinned by the original
   `_documentsFakePlayerNPE` test were harness artefacts (FakePlayer
   has a null `connection`), not real bugs. Revert commit: `df2b927`.

2. **Rewrote TASK-10 scope** in the doc: A3 (player-event tests) is
   out of scope here, becomes proposed **TASK-10b** (testClient e2e).
   `TASK-05` / `TASK-06` / `TASK-07` cross-links updated — they no
   longer "soft-require" FakePlayer.

3. **Phase 2 (B3) shipped**: 11 single-method `*SmokeTest` classes
   merged into 2 shared-harness suites (one server boot each).
   - `MachineDomainSmokeSuite` — 8 methods (Microwave, BlackHole,
     Energy, SealedRoom, SuitVacuum, SpecialInfra, MultiMachine,
     Multiblock). Originally tried 9; **ForceField extracted** after
     it flaked in shared harness — chunk eviction stalls the natural
     tick loop the projector depends on. Lives in its own JVM as
     `ForceFieldProjectionSmokeTest`.
   - `ServerBootSmokeSuite` — 2 methods (ServerStartup, Registry).
   - `RocketDomainSmokeSuite` dropped (only RocketLaunchSmokeTest is
     single-method; wrapping 1 class saves zero JVM-boots).

4. **Phase 1 (A2 remainder) — 2 of 4 shipped**:
   - ✅ `FluidTankNBTRoundTripsAcrossRestartTest` — two-boot pin for
     libVulpes FluidTank NBT format on AR's TileFluidTank. Boot 1
     injects 7 500 mB oxygen, closes harness; Boot 2 reads back
     exactly.
   - ✅ `UvAssemblerDivergesFromRocketAssemblerTest` — class-identity
     pin: `rocketBuilder` → TileRocketAssemblingMachine vs
     `deployableRocketBuilder` → TileUnmannedVehicleAssembler.
   - ⏸️ `SuitWorkStationAssemblesSuit` — needs an NBT-dump option on
     `/artest hatch read` to verify the chestplate's components list
     mutated. ~1.5 h follow-up. The original spec ("fill component
     slots, tick, assert assembled suit in output") was based on a
     misunderstanding — the tile is a passive container that mutates
     the armor item's NBT via `addArmorComponent` at write-time, not
     a ticked machine.
   - ⏸️ `FuelingStationFuelsAdjacentRocket` — needs a new
     `/artest rocket fuel <entityId>` verb to expose
     `stats.getFuelAmount(FuelType)`. Without it, we can only assert
     "station tank drained" not "rocket received fuel". ~2 h
     follow-up.

---

## Final pyramid

| Layer | Tests | Failures | Skipped | Wall |
|---|---|---|---|---|
| testServer | 179 | 0 | 3 | ~8m 30s |

(Previous baseline: ~187 testServer at 8m 27s pre-revert. Net delta:
−8 tests (smoke consolidation collapses 11 single-method classes into
2 multi-method suite classes; +2 from new tests). Wall +3s — within
noise.)

TASK-10 deliverables:

| Test/Class | Result | Time |
|---|---|---|
| MachineDomainSmokeSuite (8 methods) | 8/0/0 | 6.6 s |
| ServerBootSmokeSuite (2 methods)    | 2/0/0 | 0.9 s |
| ForceFieldProjectionSmokeTest       | 1/0/0 | 30.5 s |
| FluidTankNBTRoundTripsAcrossRestart | 1/0/0 | 29.1 s |
| UvAssemblerDivergesFromRocketAssembler | 1/0/0 | 22.5 s |

---

## Commits on `feature/tests`

```
286afff  test: TASK-10 — extract ForceFieldProjection from suite + finalize doc
9140b8c  test: TASK-10 Phase 1 — pin UV/rocket-assembler tile-class divergence
4684d97  test: TASK-10 Phase 1 — FluidTank NBT round-trip across restart
ed1c6a9  test: TASK-10 Phase 2 — ServerBootSmokeSuite + finalize B3
b692677  test: TASK-10 Phase 2 (B3) — consolidate 9 machine smokes into MachineDomainSmokeSuite
3812954  docs: redirect TASK-10 scope — A2 tail + B3 only; player tests → testClient e2e
df2b927  Revert "test: TASK-10 Phases 1+2 — FakePlayer probe + real player-event tests"
```

---

## Open follow-ups

1. **Phase 1 deferred tests** — need two small TestProbeCommand
   additions (NBT-dump in `hatch read`; new `rocket fuel <id>` verb).
   Total ~3.5 h. Once shipped, both Suit and Fueling tests become
   straightforward. Consider as TASK-10 Phase 3 OR a dedicated
   TASK-10a.
2. **TASK-10b proposal** — testClient e2e player-event coverage
   (atmosphere apply on AR-dim join, space-dim teleport guard,
   advancements). Replaces the rejected FakePlayer direction. Doc
   stub in `.agent/tasks/README.md`; no implementation plan yet.
3. **`.agent/.nav-config.json`** has an uncommitted bump of
   `read_guard_hook.escalate_threshold` to 20 (from default 5) — let
   the user decide whether to keep that change.
4. Untracked: `.agent/.nav-read-counter.json`,
   `.agent/.nav-workflow-state.json` (Navigator runtime files).

---

## Restore instructions

```
Read .agent/.context-markers/2026-05-19-1745_task10-redone-without-fakeplayer.md
Read .agent/tasks/TASK-10-fakeplayer-and-task03-tail.md
Read .agent/tasks/README.md
```
