# Historical ledger — `_documentsKnownBug` (frozen)

**Status**: frozen historical document. Do not edit except to add a
pointer to a new bug-batch ledger if a future task ever needs one.

**Live source of truth for current bugs**: there is none today — the
last ledger was drained by TASK-12 on 2026-05-23 and the
`_documentsKnownBug` suffix is no longer used in test-method names.
If a future production bug is uncovered, follow the rule in
[`CLAUDE.md`](../../CLAUDE.md#bug-tracking--every-discovered-production-bug-must-be-logged)
and start a new ledger here under a new "Batch #2" heading.

---

## Batch #1 (2026-05-22 → 2026-05-23, closed by TASK-12)

All 8 bugs surfaced as side-effects of the test-coverage build-up
(TASK-02 / TASK-03 / TASK-05 / TASK-10b / TASK-11). The original
ledger entries recorded the bug shape and the tests pinned the
**wrong** behaviour as expected. **TASK-12 (2026-05-23) fixed all 8
in production and flipped every pin to assert the corrected
contract.**

1. `HandlerCableNetwork:67` — assertion polarity inverted.
   **Fixed**: assertion now requires both networks non-null
   (was: requires either side null). Pin flipped to
   `mergeNetworksProducesLowerIdSurvivor`.
2. `CableNetwork.merge` — addAll-before-dedupe ordering causes
   duplicate node retention.
   **Fixed**: per-entry dedupe restored (matches the commented-out
   `canMerge` blocks that suggested original intent). Pin flipped
   to `cableNetworkMergeReturnsTrueAndAbsorbsDisjointSinks`.
3. `EnergyNetwork.merge` — battery-migration cascade from (2).
   **Fixed**: cascades naturally from #2. Pin flipped to
   `energyNetworkMergeMigratesBatteryFromMergedSource`.
4. `SpaceStationObject:801` — writes NBT key `"autoLand"`, reads
   key `"occupied"`. The autoLand flag is silently dropped across
   save/load.
   **Fixed**: read now uses the `"autoLand"` key on both sides;
   default-true fallback preserves legacy-save compatibility. Pin
   flipped to `autoLandFlagWithoutDockSurvivesRestart`.
5. `ItemSpaceElevatorChip:42` — calls `removeTag("positions")` to
   clear the chip's stored positions, but `NBTStorableListList`
   actually stores entries under the key `"list"`. Setting an empty
   position list is a no-op; clearing the chip from the GUI doesn't
   work.
   **Fixed**: changed `removeTag` key to `"list"`. Pin flipped to
   `elevatorChipSetEmptyAfterNonEmptyClearsList`.
6. `ItemSatelliteIdentificationChip.setSatellite(stack, SatelliteBase)`
   (lines 54-64) — else-branch built fresh NBT but never called
   `stack.setTagCompound(nbt)`. Player-visible: programming a fresh
   blank chip produced a still-blank chip.
   **Fixed**: added the missing `stack.setTagCompound(nbt);` mirroring
   the sibling overload at line 87. **Pin added in TASK-12**:
   `satelliteChipSetSatelliteAttachesNbtToFreshStack` (was originally
   ledger-only).
7. `WorldCommand.commandReloadRecipes` (line 256-258) — included
   `createAutoGennedRecipes` which calls `ForgeRegistry.register_impl`
   on the frozen recipe registry. Crashed with
   `IllegalStateException("is being added too late")` and emitted the
   `"Serious error has occurred"` message. Cascading bug: the
   JEI-integration call (`CompatibilityMgr.reloadRecipes` →
   `ARPlugin.reload` → `jeiHelpers.reload()`) NPE-d on a dedicated
   server because `jeiHelpers` is null off the client.
   **Fixed (compound)**:
   (a) removed `createAutoGennedRecipes` from the runtime reload —
       it's an init-only registration (the init-time call at
       `AdvancedRocketry.java:1044` is sufficient; auto-genned
       recipes are static once `modProducts` is set);
   (b) added null-guard on `jeiHelpers` in `ARPlugin.reload` so the
       JEI cascade is a no-op when JEI isn't initialised (correct
       for dedicated server). Pin flipped to
       `reloadRecipesEmitsSuccessConfirmationMessage`.
8. `ItemPlanetIdentificationChip.setDimensionId(stack, INVALID_PLANET)`
   (lines 73-77) — same shape as #6 but in a different class. The
   INVALID_PLANET branch built fresh NBT, wrote `dimId`, and returned
   without `stack.setTagCompound(nbt);`. The sentinel was silently
   dropped.
   **Fixed**: added the missing `stack.setTagCompound(nbt);`. Pin
   flipped to
   `planetChipSetDimensionIdWithInvalidPlanetAttachesNbtSentinel`.

### Residual references

The `_documentsKnownBug` suffix no longer appears in any test method
name. Three test files still contain javadoc / comment references
to the practice (kept intentionally as breadcrumbs explaining why
some pins look the way they do):

- `src/test/.../unit/ItemDataCarrierNBTRoundTripTest.java:43`
- `src/test/.../unit/ChipNBTRoundTripTest.java:35,70`
- `src/test/.../unit/PipeNetworkHandlerDeepTest.java:194`

If those files ever get a refactor pass, the comments can be
modernised (the bugs they describe are fixed); they are not
load-bearing.

---

## Batch #2 (2026-05-25, open)

Live entries — bugs discovered during coverage audits or test
authoring that have not yet been fixed.

1. **`SatelliteRegistry.getNewSatellite` returns `null` for unknown
   types instead of the documented `SatelliteDefunct` fallback.**
   File: `src/main/java/zmaster587/advancedRocketry/api/SatelliteRegistry.java:97`.
   The javadoc promises "SatelliteDefunct otherwise" but the code
   returns `null`. Downstream `createFromNBT` (line 84) immediately
   calls `satellite.readFromNBT(nbt)` → `NullPointerException`.
   **Consequence**: a save containing a satellite of a type that was
   registered by a companion mod no longer in the modpack:
   - On dim load: `DimensionProperties.readFromNBT` catches the NPE
     in a try/catch around `createFromNBT` and silently drops the
     satellite — save loads OK with the satellite missing.
   - On packet handling: `PacketSatellite.readClient` only catches
     `IOException` — an NPE propagates, potentially crashing the
     client packet handler / disconnecting the player.
   - Other callers (`EntityRocket.readEntityFromNBT:2038`,
     `ItemSatellite:43`, `TileSatelliteBuilder:89`, etc.) also lack
     null-guards.
   **Pinned by**: `SatelliteRegistryFallbackTest.unknownSatelliteTypeReturnsNullInsteadOfDefunct_documentsKnownBug`
   and `…createFromNBTWithUnknownTypeThrowsNPE_documentsKnownBug`
   (both pass against the current buggy behaviour). Fix candidates:
   either return `new SatelliteDefunct()` from
   `getNewSatellite:97`, or null-guard at every caller.
   **Found**: 2026-05-25 during coverage-audit (Gap 4).
