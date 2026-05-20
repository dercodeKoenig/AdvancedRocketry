# Context Marker: before-compact 2026-05-20 14:24 — TASK-07 shipped, TASK-08-mixin planned

**Created**: 2026-05-20 14:24 local
**Branch**: `feature/tests` (synced with `origin/feature/tests`)
**Status**: working tree clean; all session work committed & pushed
**Purpose**: Auto-created before compact. Captures the TASK-07 close-out
+ flake-fix + TASK-08-mixin plan filing.

---

## What shipped this session (4 commits, all pushed)

```
1f65e0c1  docs: file TASK-08-mixin — rewrite ASM coremod to Mixin
79d69efd  chore: persist Navigator config + ignore per-session runtime state
56ebf6d9  test: /artest chunk warmup eliminates cross-chunk populate flake
d0608c24  test: TASK-07 close-out — dim transition + descent/landing + failure modes (+18 tests)
```

### `d0608c24` — TASK-07 close-out

- **+18 server tests** across 3 new classes:
  - `RocketDimensionTransitionTest` (6): cross-dim transition via
    `reachSpaceManned → changeDimension`. UUID-based rocket lookup with
    live-preferred-over-isDead filter.
  - `RocketDescentLandingTest` (7): REAL server-tick descent/landing
    via ForgeChunkManager ticket + `/artest server wait`.
  - `RocketFlightFailureModesTest` (5): explode contract, out-of-fuel
    pin (no auto-explode), zero-fuel launch pin.
- **New probes** in `TestProbeCommand`: rocket {find-by-uuid,
  force-dest-dim, tick, set-state, explode, drain-fuel,
  event-counts-full}, chunk {forceload, release, release-all, list},
  server wait. `RocketEventRecorder` extended with landed +
  deOrbiting counters.

### `56ebf6d9` — chunk warmup flake-fix

- New probe `/artest chunk warmup <dim> <cx1> <cz1> <cx2> <cz2>` —
  synchronous `provideChunk` over rectangle + 1-chunk halo. Halo
  guarantees `populate()` fires for every inner chunk (vanilla
  triggers populate only when all 4 neighbours are loaded).
- Wired into the two flaky tests:
  - `RocketAssemblySmokeTest.buildAndAssemble` (covers fill rect).
  - `SpaceElevatorMultiblockTest` × 3 methods via private `warmup()`
    helper.
- **Root cause of the flake** (pinned in commit message): cross-chunk
  populate dropped tree/leaf decorations on top of fixture cells
  AFTER `fill` returned, making scanRocket's "passable above seat"
  check fail and SpaceElevator's `try-complete` refuse to attempt.

### `79d69efd` — chore

- `.agent/.nav-config.json` got `read_guard_hook.escalate_threshold=20`
  persisted.
- `.gitignore` excludes `.agent/.nav-{read-counter,workflow-state}.json`
  (per-session JSONs with `session_id` / `turn_count`).

### `1f65e0c1` — TASK-08-mixin doc

- New file `.agent/tasks/TASK-08-mixin-rewrite.md` — 5-phase plan to
  rewrite ASM coremod to Mixin instead of testing it in place.
- `tasks/README.md` P0 entry updated.

---

## Pyramid state

| Layer | Result |
|---|---|
| testUnit | 187 / 0 / 0 |
| testIntegration | 80 / 0 / 0 |
| testServer | 239 / 0 / 3 |
| testClient | passes when `DISPLAY=:77` (Xvfb at :77, env defaults to :99 mismatch — not from this work) |

testServer flakes (`RocketAssemblySmokeTest.seatCountMatchesFixturePlacement`
and `SpaceElevatorMultiblockTest.spaceElevatorMultiblockValidatesWhenFixtureIsBuilt`)
that intermittently fired during full-pyramid runs are now resolved
by the chunk warmup probe.

---

## Architectural threads worth carrying forward

1. **Real server ticking > synthetic `onUpdate` calls** (user feedback
   mid-session). The `/artest rocket tick <id> <n>` probe is retained
   for failure-mode single-step control, but Phase 4 descent/landing
   tests use chunk-anchor + `server wait` so `EntityRocket.onUpdate`
   runs in production context (real chunk neighbours, real
   collision data, real packet dispatch).

2. **Cross-chunk populate race** is a generalised problem: any test
   that does `fill` then `fixture` near a chunk border without
   warming up neighbours is exposed. The `/artest chunk warmup`
   probe is the cure. Most existing tests still don't use it — they
   passed by luck of chunk boundaries. Future flakes likely come
   from the same root cause.

3. **`MEMORY.md` user feedback `feedback_no_fakeplayer_for_player_tests`**
   still authoritative: EntityPlayer-touching tests belong in
   testClient e2e (TASK-10b), not FakePlayer in testServer.

---

## Next planned task: TASK-08-mixin

See `.agent/tasks/TASK-08-mixin-rewrite.md` for the full plan.
Summary of intent:

| | Now | After TASK-08-mixin |
|---|---|---|
| `asm/ClassTransformer.java` | 835 LoC, 5 active transforms | DELETED |
| `repack/gloomyfolken/hooklib/` | 24 vendored files | DELETED |
| `methods.bin` | exists | DELETED |
| `asm/AdvancedRocketryPlugin.java` | 61 LoC | ~25 LoC (no HookLoader) |
| Mixins in `mixin/` | 3 | 7-8 |
| Test coverage of bytecode patches | 0% (was the original TASK-08 goal) | 4-5 behavioural integration tests |

5-phase plan: (1) write 5 mixins, (2) delete old ASM + HookLib,
(3) behavioural pins, (4) runClient/runServer smoke, (5) docs + EOD.
Estimated ~14h.

**Phase 1.3 spike to start with**: disassemble vanilla
`EntityPlayer.onUpdate` (`javap -c` on deobf) to identify the exact
call to `@Redirect` for the inventory-distance-bypass mixin. ASM
version uses `IFEQ` jump after a specific INVOKEVIRTUAL — need to
name it in Mixin form (`Container.canInteractWith` is the leading
candidate).

---

## Restore instructions

```
Read .agent/.context-markers/before-compact-2026-05-20-1424.md
Read .agent/tasks/TASK-08-mixin-rewrite.md
Read .agent/tasks/README.md
git log --oneline -5    # confirm: 1f65e0c1 at head, pushed
git status              # confirm: clean
```

When ready to start TASK-08-mixin Phase 1, the entry point is:
- Read `src/main/java/zmaster587/advancedRocketry/asm/ClassTransformer.java`
  lines 638-756 (the 4 active transforms that survive — RenderGlobal
  one at 583+ is dead).
- Read `src/main/resources/mixins.advancedrocketry.json` (will add 5
  new entries).
- Read the existing mixin templates `mixin/MixinPlayerList.java` and
  `mixin/MixinWorldServerMulti.java` for project conventions.

User preference: respond in Russian (see `feedback_respond_in_russian`
auto-memory + `CLAUDE.md` "Language" section).
