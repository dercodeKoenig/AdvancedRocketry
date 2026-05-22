# Context marker — 2026-05-21

**Slug**: task05-closed-task10b-phase7-reopened
**Branch**: `feature/tests` (5 commits ahead of origin pushed; clean)
**Session focus**: TASK-05 unit-tier closure + backlog actualization

## Session arc

1. Audit of test suite vs `testing-principles` SOP — found one HIGH
   violation (`SatelliteTickBehaviourTest.baseSatelliteTickAccruesPowerGenMinusOnePerTick`
   pinning exact `990L = 10×(powerGen-1)`). Relaxed to window.
2. Backlog actualization — discovered DEVELOPMENT-README was stale.
   Real state: TASK-04/07/08-mixin/09/10/10b all done; TASK-08
   obsolete (ASM removed by TASK-08-mixin); only TASK-05, TASK-06,
   TASK-08-mixin (yes that one too) open.
3. TASK-05 work: shipped unit-tier surface for 12 of 21 item classes
   across 5 test files + 1 new `/artest` probe verb.
4. Closed TASK-05 formally; reopened TASK-10b with new Phase 7
   absorbing the player-tier item remainder.

## Commits this session (all pushed)

| SHA | Title |
|---|---|
| `b97ddf0b` | test: loosen satellite power accrual pin to contract shape |
| `2518f166` | test: TASK-05 Phase 1/5 - data-carrier item NBT round-trip |
| `d291a1b4` | test: TASK-05 Phase 3/5 - scanner/detector + special-purpose |
| `ff1b68ef` | test: TASK-05 Phase 3/4 - JackHammer + SealDetector dispatch |
| `a62f71a5` | docs: close TASK-05 unit-tier, move player-tier to TASK-10b Phase 7 |

Origin head: `a62f71a5` on `feature/tests`.

## Files created this session

**Tests** (+48 contract pins, all green):

- `src/test/java/zmaster587/advancedRocketry/test/unit/ItemDataCarrierNBTRoundTripTest.java`
  (17 tests; ItemSpaceElevatorChip + ItemData + ItemMultiData;
  includes 5th `_documentsKnownBug` pin for
  `ItemSpaceElevatorChip:42` wrong removeTag key)
- `src/test/java/zmaster587/advancedRocketry/test/unit/ScannerDetectorItemContractTest.java`
  (8 tests; ItemBeaconFinder slot gate + ItemOreScanner NBT/GUI)
- `src/test/java/zmaster587/advancedRocketry/test/unit/SpecialPurposeItemContractTest.java`
  (9 tests; ItemThermite burn-time + ItemBiomeChanger /
  ItemWeatherController metadata + wire→NBT round-trip)
- `src/test/java/zmaster587/advancedRocketry/test/unit/JackHammerContractTest.java`
  (6 tests; getDestroySpeed elevated for ROCK/IRON, fall-through
  for WOOD/GROUND, canHarvestBlock unconditional)
- `src/test/java/zmaster587/advancedRocketry/test/server/SealDetectorDispatchTest.java`
  (8 server tests via new `/artest seal-detector check` probe;
  sealed/notsealmat/other branches pinned across 6 fixtures)

**Production**:

- `src/main/java/zmaster587/advancedRocketry/command/test/TestProbeCommand.java`
  — new `handleSealDetector` (`/artest seal-detector check <dim> <x> <y> <z>`)
  re-uses real `SealableBlockHandler` predicates, mirrors
  `ItemSealDetector.onItemUse:34-50` dispatch ordering.

**Modified**:

- `src/test/java/zmaster587/advancedRocketry/test/server/SatelliteTickBehaviourTest.java`
  — relaxed window pin, renamed method to drop `MinusOne` impl detail.
- `CLAUDE.md` + `.agent/.nav-config.json` — Navigator version sync to v6.15.4.
- `.agent/DEVELOPMENT-README.md` + `.agent/tasks/README.md` +
  `.agent/tasks/TASK-05-item-behaviour-suite.md` +
  `.agent/tasks/TASK-10b-testclient-player-events.md` — backlog sync.

## Discoveries / decisions

- **TASK-08 is OBSOLETED by TASK-08-mixin** — ASM ClassTransformer
  and gloomyfolken/hooklib repack are gone. Flagged in
  DEVELOPMENT-README under "Obsolete". A future Mixin-snapshot
  safety net would be TASK-08b, not P0.
- **5th `_documentsKnownBug`**: `ItemSpaceElevatorChip.setBlockPositions`
  (line 42) calls `removeTag("positions")` but `NBTStorableListList`
  stores entries under key `"list"`. Setting empty list is a no-op.
  Documented in `.agent/tasks/README.md` bug ledger.
- **`MultiData` non-bug**: initial test iterated all `DataType.values()`
  and crashed on UNDEFINED. But UNDEFINED is a sentinel — both
  `MultiData.reset()` and `ItemMultiData.addInformation` explicitly
  skip it. Test fixed to skip UNDEFINED; no production bug.
- **`ItemAtmosphereAnalzer` not unit-testable** — static `<clinit>`
  dereferences `LibVulpes.proxy.getLocalizedString(...)` before
  proxy is injected. Tests for it moved to TASK-10b Phase 7.
- **`ItemData.getItemStackLimit`**: the `data==0 ? super : 1` ternary
  is functionally dead because ctor `setMaxStackSize(1)` makes super
  return 1. Real contract: data sticks never stack past 1.
- **Stone slab works for "other" branch in SealDetector** because
  it's solid ROCK material with half-block bounds. Torch first tried
  but fails because vanilla torch needs an attachment block; without
  attachment the placement decays to air → "notsealmat" branch.

## Open backlog (post-actualization)

**P1**: TASK-10b Phase 7 — ~10-14 h, 7 e2e suites for player-tier
item behaviour (Hovercraft spawn / SpaceArmor useFluid / SpaceChest
death-persist / BiomeChanger + WeatherController right-click satellite
action / SealDetector player messages / AtmosphereAnalzer readout).
Phase 7 is fully scoped in
`.agent/tasks/TASK-10b-testclient-player-events.md`.

**P2**: TASK-06 — Mission system depth, ~10-12 h.
Needs `/artest mission ...` probe infrastructure first (~2-3 h).

## Stale infrastructure note

The `PostToolUse:Bash` hook `monitor-tokens.py` in `.claude/settings.json`
is broken — `${CLAUDE_PLUGIN_DIR}` does not resolve to the plugin path,
so every tool call ends with a blocking error message. Plugin v6.15.4
ships hooks via plugin manifest but requires **Claude Code restart**
to activate. Restart will fix this and also enable PreCompact +
PostCompact + SessionStart fast-path hooks.

## Next session entry point

1. `nav-start` will detect this marker via `.active` and offer
   restoration.
2. Likely next task: TASK-10b Phase 7 (player-tier item behaviour).
   Recommended starting sub-suite: `ItemSealDetectorPlayerMessagesE2ETest`
   — extends existing `SealDetectorDispatchTest` server pins, reuses
   the same `/artest seal-detector check` probe surface for
   cross-validation.
3. Alternative: TASK-06 if scope demands tackling missions before
   more e2e player work.
