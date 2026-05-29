# Context marker — pre-compact 2026-05-26

**Slug**: before-compact-2026-05-26-0000
**Branch**: `feature/tests`
**Trigger**: `/navigator:nav-compact` after long session (~86 % context,
833k / 1M tokens, mostly in messages). Compacting at a clean boundary —
all batches shipped + pushed + zero in-flight work.

## Session arc — what got done

This was a multi-batch coverage build-out + audit:

### Phase A: F8 watch sweep + TASK-19 (start of day)
- **F8 watch v11 sweep** — 10× testServer, 9/10 PASS. F8 (Beacon
  `attempted:false`) 0/10 recurrence → no TASK-29. New F9
  (`MissionGasCompletion fluidEntries:0`) at 1/5 toward Obsolete.
  Commit `44009db9`.
- **TASK-19 multiblock powered-cycle trio** — split into 4 phases:
  - 1a Terraformer on AR-planet (3 tests, commits `c074d494` WIP +
    `5d43df08`).
  - 1b Terraformer on overworld w/ config flip (2).
  - 2 BHG on station orbiting black-hole star (3).
  - 3 Beacon enable cycle (3).
  - 11 tests total, 5 new probe verbs. Commit `6667b684`.

### Phase B: Original-backlog cleanup batch (TASK-22, 23, 24)
- **TASK-23** SealDetector remaining branches: 2 reachable pinned,
  `notfullblock` documented as unreachable.
- **TASK-22** UV-assembler delta: constants reflection + entity-class
  delta via new `fixture uv-rocket` probe.
- **TASK-24** SpaceArmor CHEST drain (testClient under xvfb-run).
- 9 new tests, 6 probes. Commit `3ec15dbd`.

### Phase C: Coverage-audit (Tier 1)
- Used `navigator:navigator-research` agent to identify gaps.
- Found bug #1: `SatelliteRegistry.getNewSatellite` returns null
  instead of documented `SatelliteDefunct`. Logged to ledger Batch #2
  + pinned by `_documentsKnownBug` pair.
- Gap 4 (registry), Gap 1 (PreLaunch cancel), Gap 5 (OxygenVent),
  Gap 2 (ServiceStation): 10 tests, 4 probes. Commit `f8be8656`.

### Phase D: Coverage-audit (Tier 2+3)
- 8 gaps shipped: 27 tests across 8 classes + 1 new probe
  (`rocket event-payloads`) + extended RocketEventRecorder.
- Includes testClient gap 12 (OreScanner right-click) per user
  request. Commit `1eaface3`.

### Phase E: Original backlog cleanup (TASK-20, 21)
- **TASK-20** Hovercraft ride coverage: 4 client tests, 5 new
  probes including composite `drive-ridden-entity` (defeats
  CPacketInput race). Phase 3 fuel reframed as documentation
  (no fuel logic in production code).
- **TASK-21** /ar player-equipped: 5 client tests (goto dim,
  goto station, giveStation, addTorch, addSolidBlockOverride).
  4 new probes including `exec-as-player`. /ar fetch deferred
  (needs two-bot harness), /ar fillData deferred (covered
  transitively). Commit `1dc3e8a3`.

## Pyramid

**697 → 763** over the day (+66 tests).
257 / 81 / 370 / 55 layer split.

Per-batch deltas (rough):
- TASK-19: +11 (server)
- TASK-22/23/24: +9 (6 server + 3 client)
- Tier 1 audit: +10 (3 unit + 7 server)
- Tier 2/3 audit: +27 (17 unit + 1 integration + 7 server + 2 client)
- TASK-20/21: +9 (client)

## Probes added during session (~20+ new verbs)

- `machine controller-state` / `clear-batteries` (TASK-19)
- `config get/set` whitelisted (TASK-19)
- `star get/set-blackhole` (TASK-19)
- `seal-detector add/remove-block-ban` (TASK-23)
- `assembler max-y` / `pad-bounds` (TASK-22)
- `fixture uv-rocket` (TASK-22)
- `player equip-space-chest` / `held-air-component-route` (TASK-24)
- `rocket event-payloads` (Tier 2 #6)
- `rocket arm/disarm-prelaunch-cancel` / `prelaunch-cancel-counts` (Tier 1)
- `machine controller-state` extended (Tier 1)
- `infra service-state` (Tier 1)
- `player try-orescanner-rclick` (Tier 3 #12)
- `player mount-entity` / `dismount` / `riding-entity` /
  `set-move-forward` / `drive-ridden-entity` (TASK-20)
- `player exec-as-player` / `op-self` / `deop-self` /
  `inventory-contains` / `give-held` (TASK-21)

## Backlog status — fully drained

**Done table** in `.agent/tasks/README.md` covers TASK-01..28 + TASK-19,
20, 21, 22, 23, 24.

**Backlog table** now only has watching/investigation-complete items:
- TASK-15 visual regression (watching for triggers).
- TASK-16 flake watch (investigation complete).

**Deferred** with documented reasons:
- Gap 3 elevator capsule (needs hovercraft pattern from TASK-20 —
  could now be picked up).
- Gap 7 SatelliteBuilder real-construction (heavy testClient).
- Gap 9 Fuel loader active transfer (explicitly deferred by audit).
- /ar fetch (needs two-bot harness).
- /ar fillData (covered transitively by TASK-09).
- TerraformingTerminal deeper biome-mutation (needs BiomeChanger chip
  fixture).
- ServiceStation repair cycle (needs TileBrokenPart injection).
- Station controllers deeper contracts (needs station context).

## Bug ledger

**Batch #2 has 1 LIVE bug**:
`SatelliteRegistry.getNewSatellite` (line 97) returns `null` for
unknown types — javadoc promises `SatelliteDefunct`. Downstream
`createFromNBT` NPEs. Pinned by 2 `_documentsKnownBug` tests in
`SatelliteRegistryFallbackTest`.

**User decision**: "we document bugs, not fix them in this session" —
ledger stays open until explicitly requested.

Fix candidate when ready: change `return null` to
`return new SatelliteDefunct()` in `SatelliteRegistry.java:97`.
Then flip `unknownSatelliteTypeReturnsNullInsteadOfDefunct_documentsKnownBug`
and `createFromNBTWithUnknownTypeThrowsNPE_documentsKnownBug` to
positive contracts. ~30 min including verification.

## Commits on `feature/tests` (today)

| SHA | Subject |
|---|---|
| `44009db9` | docs: TASK-28 — F8 watch v11 sweep results |
| `c074d494` | test: TASK-19 Phase 1a WIP — terraformer on AR planet |
| `5d43df08` | test: TASK-19 Phase 1 — terraformer powered cycle |
| `6667b684` | test: TASK-19 complete — BHG (Phase 2) + Beacon (Phase 3) |
| `3ec15dbd` | test: batch TASK-22 + 23 + 24 |
| `f8be8656` | test: Tier 1 audit gaps |
| `1eaface3` | test: Tier 2+3 audit gaps batch |
| `1dc3e8a3` | test: TASK-20 + TASK-21 batch |

All pushed to `origin/feature/tests`.

## Files that exist as untracked (state to be aware of)

```
.agent/.context-markers/2026-05-25_f8-watch-v11-sweep.md
.agent/.context-markers/2026-05-25_task19-phase1a-wip.md
.agent/.context-markers/2026-05-25_task19-complete.md
.agent/.context-markers/2026-05-25_batch-task22-23-24-complete.md
.agent/.context-markers/2026-05-25_tier1-audit-gaps-complete.md
.agent/.context-markers/2026-05-25_tier2-tier3-audit-gaps-complete.md
.agent/.context-markers/2026-05-25_task20-task21-complete.md
.agent/.context-markers/before-compact-2026-05-23-1230.md
.agent/.context-markers/before-compact-2026-05-25-0000.md
```

These follow the pre-existing convention of "markers stay untracked"
that I saw at session start. NOT committed by me — they're working
notes.

## Resumption tips for next session

1. Run `/nav:start` — `.active` marker (this file) will offer
   restoration.

2. **First decision**: what kind of work?
   - **More tests**: backlog drained, audit drained. Need a fresh
     audit pass (look at production code modified since last audit
     sweep) or watch for player-bug-reports surfacing new contracts.
   - **Fix bug #1**: ~30 min, clean win. User has said "document not
     fix" so wait for explicit go-ahead before touching production.
   - **Defer-list closure**: pick from Gap 3 elevator, Gap 7
     SatelliteBuilder, ServiceStation repair cycle, etc.
   - **Pause**: legitimate option — pyramid is healthy at 763.

3. **If 10× sweep flake**: re-read
   `.agent/sops/development/flake-diagnosis.md`. F8 (Beacon) and F9
   (MissionGasCompletion) are still in watching mode (1/5 toward
   Obsolete each).

4. **testClient gotcha**: requires `xvfb-run` wrapper on this
   headless dev box. `xvfb-run -a ./gradlew testClient ...`. Same
   as TASK-24, TASK-20, TASK-21 used.

5. **gradle.properties**: `forks=5` set machine-globally; project
   default 3 still applies for AR.

## Why compacting here

Context at 86 %, mostly messages. All in-flight work committed +
pushed. Marker history for the day's 4 distinct sessions is
preserved as 7 untracked marker files. Clean inflection.

## Open follow-up items

- **F8 / F9 flake watch**: 1/5 toward Obsolete each. Need 4 more
  clean 10× sweeps to retire.
- **Batch #2 bug #1**: live, awaiting user decision on fix vs keep
  documented.
- **Backlog**: empty except watching/investigation-complete.
