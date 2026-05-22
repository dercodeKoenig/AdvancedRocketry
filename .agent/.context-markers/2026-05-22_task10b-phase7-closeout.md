# Context marker — 2026-05-22

**Slug**: task10b-phase7-closeout
**Branch**: `feature/tests` (clean, all pushed)
**Session focus**: TASK-10b Phase 7 — shipped 4 of 7 candidate suites,
rescoped 3 with documented justification.

## Session arc

Resumed from the 2026-05-21 marker (TASK-10b Phase 7 reopened after
TASK-05 unit-tier closed). Three sub-arcs:

1. Fixed three real bugs in the chat-tap probe shipped the prior
   session (it was committed but never actually exercised end-to-end
   because the `DISPLAY` env var was wrong — when finally run, the
   tap was found broken).
2. Shipped 4 player-tier e2e suites against the now-working tap +
   new probe verbs (`try-atm-analyze`, `try-hovercraft`,
   `try-biomechanger-rclick`).
3. Audited the remaining 3 candidate suites (Weather, SpaceArmor,
   SpaceChest) against the testing-principles SOP; rescoped or
   dropped each with explicit justification.

## Chat-tap fixes (root cause for all SealDetector regressions)

| Bug | Cause | Fix |
|---|---|---|
| Tap never fired | `pipeline().addFirst(...)` puts handler at outbound-tail (after PacketEncoder); `msg` was a ByteBuf by then | `addLast` so we're at outbound-head, before encode |
| `NoSuchMethodError: SPacketChat.getChatComponent()` | testClient runtime ships SRG-named classes; deobf transformer not applied to test classpath | Reflective lookup trying MCP name → SRG name → field-access fallback, cached |
| Command-echo flooded deque | Every `/artest` invocation broadcasts `chat.type.announcement` as the player who ran it; that drowned the player-visible keys tests pin | Filter `chat.type.announcement*` in the tap |

Last fix required the prior `componentKey` to recurse into
`TextComponentTranslation.getFormatArgs()` and join nested keys with
`|` — needed for AtmAnalzer's `"%s %s %s"` wrapper format anyway.

## Commits this session (all pushed)

| SHA | Title | Tests |
|---|---|---|
| `be480a52` | test: TASK-10b Phase 7 — SealDetector player-msg e2e (+8 pins) | 8 |
| `6184f3e7` | docs: context marker — TASK-10b Phase 7 SealDetector shipped | — |
| `5f88b777` | test: TASK-10b Phase 7 — AtmosphereAnalzer readout e2e (+3 pins) | 3 |
| `6282334a` | test: TASK-10b Phase 7 — Hovercraft spawn e2e (+3 pins) | 3 |
| `23e9aadd` | test: TASK-10b Phase 7 — BiomeChanger right-click e2e (+2 pins) | 2 |

**Total this session: +16 new e2e pins, all green** under
`DISPLAY=:77 ./gradlew testClient --tests <suite>` (~10min wall-clock
per re-run).

## Files touched

**Production / probe**:
- `src/main/java/.../command/test/TestProbeCommand.java`
  - Fixed: chat-tap addLast + reflective SPacketChat read + announcement filter
  - Enhanced: `componentKey` recursively joins nested translation keys
  - Added: `try-atm-analyze`, `try-hovercraft`, `try-biomechanger-rclick`
  - Updated help text in `handlePlayer` final fallback

**Tests** (all new, all under `src/test/java/.../test/client/`):
- `ItemSealDetectorPlayerMessagesE2ETest.java` (8 tests)
- `ItemAtmosphereAnalzerReadoutE2ETest.java` (3 tests)
- `ItemHovercraftSpawnE2ETest.java` (3 tests)
- `ItemBiomeChangerSatelliteActionE2ETest.java` (2 tests)

**Docs**:
- `.agent/tasks/TASK-10b-testclient-player-events.md` — Phase 7
  acceptance checklist updated with rescope justifications

## Rescope decisions (with SOP justification)

Each call applied the testing-principles litmus before writing or
skipping the suite.

### ItemWeatherControllerActionE2ETest — DROPPED

- Right-click effect = `performAction` populates private
  `viable_positions` list (floodfill of rain targets).
- That list is **NOT** in `writeToNBT` — only mode_id, last_mode_id,
  floodlevel are persisted.
- → no save-format observable to pin against.
- Reflective read of `viable_positions` would be impl-field testing,
  an anti-pattern called out in the SOP ("internal field names —
  impl").
- True player-visible contract (actual rain/dry weather change)
  requires battery + tick cycle to drain the list — substantial
  probe infra.
- **Outcome**: leave for a future ticket. Either production adds
  an NBT pin for viable_positions (then the e2e becomes trivial),
  or the test framework grows a tick-loop driver (separate scope).

### ItemSpaceArmorUseFluidE2ETest — DEFERRED

- Real drain contract: suit air-buffer decremented per tick while
  player is in a vacuum-suitable atmosphere.
- Drain happens inside `AtmosphereNeedsSuit.isImmune` →
  `ItemAirWrapper.protectsFromSubstance(atm, stack, true)`.
- Needs: planetary dim fixture + `AtmosphereHandler.runEffectsOnPlayer`
  loop + suit equipped with space-protection enchantment.
- **~3-4h** probe-infra cost to set up properly.
- **Outcome**: out of scope for Phase 7 close-out. Follow-up ticket
  recommended — the contract is real and worth pinning.

### ItemSpaceChestDeathPersistE2ETest — DROPPED (not a mod contract)

- Greppe d entire production for `PlayerEvent.Clone`, `keepInventory`,
  custom drop/death handlers — **nothing**.
- The mod has no special death handling for SpaceChest.
- A "death-persist" test would pin vanilla Minecraft NBT survival
  through entity-drop serialization — that's a vanilla contract,
  not a mod one. SOP litmus blank reads "vanilla preserves
  ItemStack NBT" — fail.
- **Outcome**: not a real contract. Remove from backlog.

## Discoveries

- **Chat-tap test was effectively zero-coverage before this session**.
  It compiled and shipped in commit `ff1b68ef` but had three
  independent bugs (addFirst, NoSuchMethodError, announcement
  flooding) that prevented it from ever observing a chat message
  end-to-end. The 8 SealDetector pins were green for the first
  time today, not regression-protected.
- **`ItemSatelliteIdentificationChip.setSatellite(stack, SatelliteBase)`
  has a likely pre-existing bug**: it constructs a new NBT compound
  in the `else` branch but never calls `stack.setTagCompound(nbt)`,
  so the NBT is silently dropped for items with no existing tag.
  The BiomeChanger probe works around this by building+writing
  the tag directly. Worth documenting in the bug ledger if it
  hasn't been called out yet — not fixed this session.

## Open backlog (post-Phase 7)

**P1**:
- `TASK-06` — Mission-system depth, ~10-12 h. Needs `/artest
  mission ...` probe scaffolding first (~2-3 h).

**P2**:
- Follow-up ticket: `ItemSpaceArmorUseFluidE2ETest` (deferred above,
  needs vacuum-dim + atmosphere-tick fixture).
- Follow-up ticket: WeatherController e2e — gated on either
  production adding `viable_positions` NBT pin or test framework
  growing a tick-loop driver.

**Infra notes** (still relevant from prior marker):
- `PostToolUse:Bash` hook still broken (monitor-tokens.py absent on
  disk; settings.json change blocked by auto-mode). User opted to
  leave as-is — hook spam is non-blocking.
- `DISPLAY=:77` is the right value for testClient (default `:99`
  has no Xvfb backing it on this box).

## Next session entry point

If resuming:
- `nav-start` will detect this marker via `.active`.
- Most natural next task: TASK-06 mission system, or the deferred
  SpaceArmor follow-up.
- Hook-noise tolerance: every Bash/Edit/Write call ends with a
  blocking-error reminder about monitor-tokens.py — ignore it,
  the actual operations succeed.
