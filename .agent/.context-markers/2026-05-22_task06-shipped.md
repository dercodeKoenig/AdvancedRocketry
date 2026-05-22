# Context marker — 2026-05-22 (TASK-06 closed)

**Slug**: task06-shipped
**Branch**: `feature/tests` (uncommitted: shows diff + new files for review)
**Session focus**: TASK-06 follow-up close-out — 5 additional tests.

## Session arc

Continued the same-day Phase 1-4 session (`task06-phases-1-4-shipped`)
and shipped the three deferred follow-ups in one pass:

1. **Fluid-cargo fixture + strong 64000 mB oxygen pin**
   New `with-fluid-cargo` variant on `/artest fixture rocket`: swaps
   2 of 6 BlockFuelTank positions for `advancedrocketry:liquidTank`
   (BlockPressurizedFluidTank → TileFluidTank → exposes
   CapabilityFluidHandler.FLUID_HANDLER). With this fixture, the
   strong production-literal pin `64000 mB oxygen per fluid tile`
   becomes observable. Test added to `MissionGasCompletionTest`.

2. **Infrastructure lifecycle tests** (Phase 5)
   2 server tests in new `MissionInfrastructureLifecycleTest`:
   - start + link-infra → tile.mission set to mission ref
   - complete-now → tile.mission cleared (production unlinkMission)
   Uses `advancedrocketry:monitoringStation` as the fixture infra tile
   (one of the IInfrastructure implementors that actually stores the
   mission ref — TileGuidanceComputerAccessHatch is a counter-example,
   it always returns false from linkMission).
   Two new probe verbs added: `link-infra` and `infra-state`.

3. **Multi-boot persistence** (gas + ore)
   2 server tests in new `MissionPersistenceRestartTest`:
   - boot1 starts mission, close + reboot, boot2 finds same missionId
     with same duration/type/isDead=false
   - separate test for gas (type=gas, oxygen fluid) and ore
   Extends `PersistenceRestartSmokeTest` harness pattern.

## Commits this session

This session's work is uncommitted at marker time. The user reviews
the diff before commit per CLAUDE.md rule.

Files changed:
- `src/main/java/.../command/test/TestProbeCommand.java`
  - Added `with-fluid-cargo` rocket-fixture variant
  - Added `link-infra` and `infra-state` mission probe verbs
  - Extended `snapshotCargoJson` with `infraEntries` + `infrastructure`
    keys (forward-compat: existing tests' contains() assertions
    still pass — only new keys)
  - Added `readObjectFieldOrNull` reflection helper

- `src/test/java/.../server/MissionGasCompletionTest.java` (+1 test,
  +1 helper overload)
- `src/test/java/.../server/MissionInfrastructureLifecycleTest.java`
  (new, 2 tests)
- `src/test/java/.../server/MissionPersistenceRestartTest.java`
  (new, 2 tests)

- `.agent/tasks/TASK-06-mission-system-depth.md` — closed; one
  narrow follow-up documented
- `.agent/tasks/README.md` — counter 398→403; Done row rewritten;
  P2 backlog cleared

## Test status

Mission suite: 19/19 green (3 unit + 16 server).
- MissionNbtRoundTripTest: 3/3
- MissionLifecyclePyramidTest: 5/5
- MissionGasCompletionTest: 4/4 (new strong fluid-fill pin included)
- MissionOreCompletionTest: 3/3
- MissionInfrastructureLifecycleTest: 2/2 (new)
- MissionPersistenceRestartTest: 2/2 (new)

## Discoveries (worth carrying forward)

### `forwardDirection` defaults via readMissionPersistentNBT

`EntityStationDeployedRocket.forwardDirection` is a non-final field
that starts null. `MissionGasCollection.onMissionComplete` accesses
`rocket.forwardDirection.getFrontOffsetX()` at line 71 — naively a
NPE risk. But line 69's
`rocket.readMissionPersistentNBT(missionPersistantNBT)` calls into
`EntityStationDeployedRocket.readMissionPersistentNBT` which does
`forwardDirection = EnumFacing.values()[nbt.getInteger("fwd")]`.
Empty NBT returns 0 → EnumFacing.DOWN (default). So in practice
forwardDirection is always DOWN at mission completion when the
original rocket's `writeMissionPersistentNBT` is a no-op (vanilla
EntityRocket). Result: new rocket spawns at SAME coords as launch
(`dir.X=0, dir.Z=0`).

### Rocket-side relink assertion isn't observable yet

A snapshot scan of the 128-cube around launch coords returns only
ONE EntityRocket after completion, vs the expected two (original
EntityRocket + new EntityStationDeployedRocket). That one rocket's
`infrastructureCoords` is observed empty even though
`MissionGasCollection.onMissionComplete` clearly calls
`rocket.linkInfrastructure(tile)` at line 84. Hypotheses:
- Original rocket marked dead by some hook during completion
- StationDeployed spawn-with-overlap suppressed by entity-collision
  logic (both rockets spawn at same coords because dir=DOWN)
- Snapshot bbox wrong somehow
Investigation deferred — the tile-side unlinking IS pinned, which is
the player-facing contract.

### Hook config error (environmental, not project)

`PostToolUse` hooks fail with `python3: can't open file
'/hooks/monitor-tokens.py'` — `${CLAUDE_PLUGIN_DIR}` env var unset
or wrongly substituted in the user's settings. Non-blocking (tool
calls succeed despite the hook's stderr), but warrants a one-line
fix in `~/.claude/settings.json` or similar.

## Open backlog (post-TASK-06)

**P1**:
- TASK-10b Phase 7 follow-ups (SpaceArmor useFluid; WeatherController
  right-click — both gated on production / framework changes)

**P2**:
- TASK-06 rocket-side relink investigation (~1-2 h, mostly
  diagnostic — drop a logging probe to determine which rocket the
  snapshot is seeing)

No P0 work remaining for gameplay-contract coverage.
