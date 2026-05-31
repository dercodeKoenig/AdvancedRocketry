# Bug ledger — `_documentsKnownBug` history + live Batch #2

**Status**: Batch #1 is frozen/historical (drained by TASK-12 on
2026-05-23). Batch #2 below is **live** and is kept in sync with the
summary in [`../tasks/README.md`](../tasks/README.md) bug-ledger section.

**Live bug count (as of 2026-05-31)**: 4 live — Batch #2 entries
#1, #3, #5, #7. Entry #2 dropped as impl-trivia, #4 fixed by TASK-41,
#6 fixed by TASK-43 Phase 3 (see per-entry notes below).
When a future production bug is uncovered, follow the rule in
[`CLAUDE.md`](../../CLAUDE.md#bug-tracking--every-discovered-production-bug-must-be-logged)
and append it to Batch #2 here AND to the README summary.

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

2. ❌ **DROPPED 2026-05-31 — impl-trivia, not a contract bug.** Per
   CLAUDE.md, a bug whose consequence is "nothing observable today"
   is impl trivia and not loggable; this entry's own consequence note
   reads "invisible today". Retained struck-through to keep #3-#7
   numbering stable. Original description follows.
   **`EntityElevatorCapsule.setStandTime(int time)` ignores its
   parameter and writes the `standTime` field instead.**
   File: `src/main/java/zmaster587/advancedRocketry/entity/EntityElevatorCapsule.java:83-85`.
   The body reads
   `this.dataManager.set(standTimeCounter, standTime);` — the
   `time` argument is never consulted; the dataManager always
   receives the value of the field by the same name.
   **Consequence**: invisible today because the only caller
   ({@code onEntityUpdate} line 399) invokes
   `setStandTime(standTime)`, passing the field value, which is
   exactly what the buggy body reads. Any future caller (e.g.
   external mod, a refactor that resets via `setStandTime(0)`,
   a sibling tile-entity hook) will silently lose the requested
   value and overwrite with the stale field. The dataManager
   would then desynchronize from the field on the next read
   path.
   **Pinned by**: ledger-only (deferred — the bug sits behind a
   single safe caller; a `_documentsKnownBug` test would cost
   more in fixture wiring than the ledger entry buys today). Fix
   candidates: change body to
   `this.dataManager.set(standTimeCounter, time); this.standTime = time;`
   so both the field and the dataManager update from the
   argument.
   **Found**: 2026-05-26 during TASK-30 Gap 3 elevator-capsule
   coverage authoring.

3. **`TileStationGravityController` constructor omits the
   `redstoneControl.setRedstoneState(OFF)` call its altitude sibling
   makes.**
   File: `src/main/java/zmaster587/advancedRocketry/tile/station/TileStationGravityController.java:38-47`
   (constructor) — compare to
   `src/main/java/zmaster587/advancedRocketry/tile/station/TileStationAltitudeController.java:42-43`
   which explicitly does `redstoneControl.setRedstoneState(RedstoneState.OFF)`
   right after constructing the module.
   `zmaster587.libVulpes.inventory.modules.ModuleRedstoneOutputButton`
   defaults to `RedstoneState.ON` (line 22 of that class). The
   gravity controller therefore enters its first `update()` tick
   with `redstoneControl.getState() == ON`, which triggers the
   branch at line 114:
   `((SpaceStationObject) spaceObject).targetGravity = (world.getStrongPower(pos) * 6) + 10`.
   With no redstone wiring around a freshly-placed controller
   the right-hand side evaluates to `0 * 6 + 10 = 10`, so the
   tile silently overwrites `targetGravity` to 10 on every tick.
   **Consequence**: player-visible. A player who places the
   gravity controller and walks away (without opening the GUI
   to toggle the redstone-output button) sees their station's
   gravity drift down to `0.1` (`targetGravity / 100 = 10/100`)
   instead of staying at the placed default 1.0. The GUI input
   path (`setProgressByUser` → `setProgress` → writes the
   intended `targetGravity = progress + minGravity`) is also
   immediately reverted on the next tick if the player hasn't
   first toggled `redstoneControl` to OFF via the GUI.
   **Pinned by**: ledger-only — the workaround test
   `StationControllersTickContractTest.gravityControllerWalksStationGravityTowardTarget`
   pins the end-state walk (gravity moves measurably below
   1.0) under the broken default, which would also pass if the
   bug were fixed (because in that case the slider's
   `setProgress(0, 50)` write would stick and the walk would
   approach `0.6` instead of `0.1` — still distinctly below
   the 0.9 threshold). A separate `_documentsKnownBug` test
   would cost more fixture wiring (probe to inject specific
   `redstoneControl.state` value) than the ledger entry buys
   today. Fix candidate: append
   `redstoneControl.setRedstoneState(RedstoneState.OFF);` to
   the constructor at line 45.
   **Found**: 2026-05-26 during TASK-30 station-controller
   tick-contract authoring.

4. ✅ **FIXED 2026-05-29 by TASK-41.**
   `mixins.advancedrocketry.json:AccessorWorld` mixin apply failed
   during `./gradlew runClient` with `InvalidAccessorException: No
   candidates were found matching field_72986_A`. Root cause: the
   AP-generated refmap was jar-only (not staged into
   `build/resources/main/`), and even staged the SRG-name lookup is
   wrong for the MCP-named dev classloader. **Fixed**: swapped
   `@Accessor` for an access transformer
   (`public net.minecraft.world.World field_72986_A`);
   `PlanetWeatherManager` sets `world.worldInfo = wrapped` directly;
   `AccessorWorld` mixin deleted. Added `stageMixinRefmapForRun` to
   stage the refmap for future @Inject mixins.
   **Found**: 2026-05-29 during TASK-41.

5. **5 pre-existing test failures on `feature/tests` HEAD** (tracker
   entry, not a single production bug). 3 testServer recipe tests
   (`Electrolyser`/`PrecisionAssembler`/`PrecisionLaserEtcher` —
   parallel-fork contention, pass in isolation) + 2 testClient
   (`InventoryBypassRedirectE2ETest` broken-since-inception;
   `WorldCommandFetchModeratorTest` stable-fail-in-isolation). Triaged
   by TASK-42, the 4 residuals promoted to TASK-43 (Shape A recipe
   flakes / Shape B fetch-moderator). Stays open as a tracker for the
   deferred TASK-43 work.
   **Found**: 2026-05-29 during TASK-41 validation sweep.

6. ✅ **FIXED 2026-05-30 by TASK-43 Phase 3** (resolved fully by
   TASK-44 2026-05-31). `MixinEntityPlayer*InventoryAccess` `@Redirect`
   + `MixinWorldSetBlockState` `@Inject` silently no-op'd in the dev
   classloader because the refmap translates targets to SRG names the
   MCP-named dev runtime doesn't have. Because
   `mixins.advancedrocketry.json` is `"required": true`, the first
   PREINJECT failure aborted ALL 6 mixins in dev (silent — @Inject
   FATALs don't crash the JVM). **Fixed**: `mixin.env.disableRefMap=true`
   added to `runs.client` + `runs.server` FG6 maps. Player-visible
   (dev only): AR's "keep rocket inventory open across distance"
   feature didn't work in `runClient` (works in reobf installs).
   TASK-44 then un-`@Ignore`'d `InventoryBypassRedirectE2ETest` via a
   server-side `player open-chest` probe (4/4 reruns green).
   **Found**: 2026-05-30 during TASK-42/43 InventoryBypass diagnostic.

7. **`TilePump.performFunction` only drains `instanceof IFluidBlock`
   blocks (lines 102 / 120 / 158).** Vanilla `Blocks.WATER` is a
   `BlockLiquid`, not Forge's `IFluidBlock`, so a pump over a vanilla
   water source pumps nothing — only Forge/AR fluids
   (`BlockFluidClassic` subclasses) are drainable.
   File: `src/main/java/zmaster587/advancedRocketry/tile/multiblock/machine/TilePump.java:102,120,158`.
   **Consequence**: player-visible — players expecting the pump to lift
   vanilla water (as most tech-mod pumps do) get an empty tank with no
   error. May be intended (AR pump is a mod-fluid network device) or a
   limitation; recorded because the 2026-05-27 audit's Gap F.4 framing
   assumed water would work.
   **Pinned by**: ledger-only — no `_documentsKnownBug` test;
   `TilePumpFillsFromAdjacentWaterSourceTest` pins the real contract
   (drains an AR Forge-fluid source) and documents this in its docstring.
   **Found**: 2026-05-31 during TASK-44 Gap F.4 un-ignore.
