# Context marker — 2026-05-23 (TASK-11 closed)

**Slug**: task11-world-command-shipped
**Branch**: `feature/tests` (2 commits ahead from prior sessions + this
TASK-11 close-out pending review/push).
**Session focus**: TASK-11 — `/ar` (WorldCommand) coverage, all 4 phases.

## Session arc

Followed the all-phases-then-commit-then-push pattern.

Started with full testClient regression (DISPLAY=:77, 25 min, BUILD
SUCCESSFUL) to retire the leftover from prior session. Then wrote
TASK-11 task doc to the user's constraints (terse, result-focused,
no duplication), then implemented all 4 phases.

## Tests landed this session (23)

- **Phase 1** `WorldCommandPlanetSetGetContractTest` (5) — set
  atmosphereDensity / gravitationalMultiplier / rotationalPeriod via
  `/ar planet set 0 <field> <val>`, assert via independent
  `/artest planet info 0` JSON readback. Plus `planet get` echo
  cross-check and `planet list` overworld-presence pin.

- **Phase 2** `WorldCommandPlanetLifecycleContractTest` (4) — generate
  adds exactly one dim / generated name appears in list / delete
  removes the dim / reset restores overworld baseline density (100).

- **Phase 3** `WorldCommandStarMiscContractTest` (6) — star list /
  get temp / set temp / generate registers a new star /
  `dumpBiomes` writes a file containing `minecraft:plains` /
  `reloadRecipes` `_documentsKnownBug`.

- **Phase 4** `WorldCommandGuardContractTest` (8) — addTorch /
  addSolidBlockOverride / setGravity / fillData / goto guard
  console-sender; fetch + giveStation report invalid-player; unknown
  top-level subcommand does NOT print help envelope.

Helper class: `WorldCommandFixtures` — `exec(cmd)` thin wrapper plus
`planetIntField` / `planetFloatField` (regex matchers over
`/artest planet info <dim>` JSON) and `planetExists(dim)` (scan
`/ar planet list` for `DIM<N>:`).

## Discoveries (worth carrying forward)

### Bug #7 — `commandReloadRecipes` crashes post-init

`/ar reloadRecipes` is broken at runtime: Forge 1.12.2 freezes the
recipe registry after init, and the production path
(`WorldCommand:258` → `RecipeHandler.createAutoGennedRecipes:122` →
`ForgeRegistry.add`) throws `IllegalStateException("The object …
is being added too late")`. Catch branch fires the user-visible
"Serious error has occurred! Possible recipe corruption" message.
Logged as bug ledger entry #7; pinned by
`reloadRecipesEmitsErrorEnvelopeDueToFrozenRegistry_documentsKnownBug`.

### `getDimensionProperties` falls back to overworldProperties

`DimensionManager.getDimensionProperties(dimId)` returns
`overworldProperties` for any unknown dim (line 539). This means
`/artest planet info <dim>` NEVER returns the `"unknown planet"`
error envelope for AR dims — only the spaceDim or STAR_ID_OFFSET
branches can yield non-defaults. The probe is not a reliable
"does this dim exist" oracle. Use `/ar planet list` regex scan
instead (which iterates `dimensionList.keySet()` directly).

### `random.nextInt(0)` crashes silent in `planet generate`

`DimensionManager.generateRandom:281` calls
`random.nextInt(atmosphereFactor)`. With factor=0 (the natural
"deterministic" args to a test), this throws
`IllegalArgumentException("bound must be positive")` and the
catch in `commandPlanetGenerate` only catches `NumberFormatException`,
so the IAE bubbles up to the server thread and the command
silently no-ops. Tests use `10 10 10`.

### `averageTemperature` is derived, not a settable contract

`DimensionProperties.getAverageTemp()` (line 2002) recomputes
the field from star + orbital + atmosphereDensity on every read.
Pinning a `/ar planet set 0 averageTemperature 412` would test
the write-then-immediate-read window — an impl detail, not a
contract. Dropped from the suite.

## Files changed

- `src/test/java/.../server/WorldCommandFixtures.java` — new helper.
- `src/test/java/.../server/WorldCommandPlanetSetGetContractTest.java`
- `src/test/java/.../server/WorldCommandPlanetLifecycleContractTest.java`
- `src/test/java/.../server/WorldCommandStarMiscContractTest.java`
- `src/test/java/.../server/WorldCommandGuardContractTest.java`
- `.agent/tasks/TASK-11-world-command-coverage.md` — plan + close-out.
- `.agent/tasks/README.md` — counter 407→430, +TASK-11 Done row,
  bug ledger entry #7.

## Test status

`./gradlew testServer --tests "...WorldCommand*"` → **23/23 PASSED**.
Full testClient suite (run earlier this session) → BUILD SUCCESSFUL
in 25:29 with all classes green.

## Open backlog (post-TASK-11)

**P0**: empty.
**P1**: empty.
**P2**: empty.

**Deferred / no task yet**:
- Phase 9 (companion-mod integration tests)
- Phase 10 (visual regression for MC client)
- Pipe end-to-end (blocked on uncommented registrations)
- Production-bug fixes for the 6 pinned `_documentsKnownBug` entries
  (+ ledger-only #6) — separate ticket; flip pins after the fix.

No P0/P1/P2 work remaining for gameplay-contract coverage.
