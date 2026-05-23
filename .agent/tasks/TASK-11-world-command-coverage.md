# TASK-11: `/advancedrocketry` (`/ar`) WorldCommand coverage

## Ticket

- Source: TASK-10b Phase 7 close-out (2026-05-22 EOD). `WorldCommand`
  is the largest single uncovered surface in the repo — 991 LoC, 0
  dedicated tests, 12 top-level subcommands + two large families
  (planet / star).
- Status: ✅ Completed 2026-05-23.
- Created: 2026-05-23.

## Context

`/ar` (aliases `/advancedrocketry`, `/advrocketry`) is the in-game
admin/debug surface. It's the only player-accessible path to several
production code paths that bypass the GUI entirely:

- Per-dim planet field mutation (`planet set <id> <field> <val>`)
- Planet creation / deletion at runtime
- Star registry mutation (`star generate`, `star set …`)
- Granting a space-station chip pre-bound to a station ID
- Setting per-dim gravitational multiplier
- Teleport (`goto`, `fetch`) with AR's `TeleporterNoPortal`
- Mutating runtime AR config (`addTorch`, `addSolidBlockOverride`)
- Dumping biome registry to disk

Regressions in this command silently break server-admin workflows
that the in-game GUI does not cover. Worth its own task by raw
exposed surface area even though it's not on a critical gameplay
path.

**Out of scope**: client-side autocompletion order (`getTabCompletions`)
beyond a smoke test; the in-tree `beginTest` orchestrator (it's a
test runner itself — wrapping it in a test would be a category
error).

## Subcommand map

Source: `src/main/java/zmaster587/advancedRocketry/command/WorldCommand.java`.

| # | Subcommand | Production effect | Observable result |
|---|---|---|---|
| 1 | `addTorch` | adds held block to `ARConfiguration.torchBlocks` | `torchBlocks.contains(block)` |
| 2 | `addSolidBlockOverride` | adds held block to `sealedBlocks` | `sealedBlocks.contains(block)` |
| 3 | `giveStation <id> [player]` | adds `itemSpaceStationChip` w/ UUID=id to inv | player inventory contains chip; `ItemStationChip.getUUID(stack) == id` |
| 4 | `fillData <type> <amount>` | writes data to held `ItemMultiData` | item NBT `data` map matches |
| 5 | `reloadRecipes` | re-fires AR recipe registry | recipe count stable / known-AR recipes present |
| 6 | `setGravity <dim> <mult>` | mutates `DimensionProperties.gravitationalMultiplier` | reflective probe of dim field |
| 7 | `goto <dim> [x y z]` | teleports sender to dim/coord | sender.world.dim + posX/Y/Z |
| 8 | `fetch <player>` | teleports named player to sender | target.posX/Y/Z near sender |
| 9 | `planet new <…>` | calls `DimensionManager.createDimensionProperties` | `DimensionManager.getInstance().getDimensionProperties(id) != null` |
| 10 | `planet delete <id>` | removes dim from registry | `getDimensionProperties(id) == null` |
| 11 | `planet reset <id>` | reloads from defaults | dim fields equal config defaults |
| 12 | `planet get <id> <field>` | sends chat with current value | captured chat output contains field value |
| 13 | `planet set <id> <field> <val>` | writes via reflection | reflective read of same field |
| 14 | `planet list` | sends chat with all dim ids | chat contains every registered dim id |
| 15 | `star get/set/list/generate` | star registry mutation/read | `DimensionManager.getStar(id)` + field reads |
| 16 | `dumpBiomes` | writes file in run dir | file exists, contains a known vanilla biome name |

Skipped: `beginTest` (delegates to `IngameTestOrchestrator`).

## Test design — result-focused, low boilerplate

**Anti-pattern to avoid** (per
[`feedback_tests_verify_contracts`](feedback_tests_verify_contracts.md)):
pinning chat-message exact wording, exact reflection field names,
or which helper got invoked. The contract is the **side effect on
world / registry / inventory**, not the dispatch chain.

**Pattern** — one server-tier test class with a small invocation
helper, one assertion per test, ≤ 6 lines of test body. Shared
fixture (server + planet registry seed) carries via
`AbstractSharedServerTest`.

### Reuse the existing harness

The repo already runs `WorldCommand` against the live registry at
boot (it's an `ICommand` registered in `AdvancedRocketry.serverStart`).
No new probe surface is needed for invocation — the testServer
harness already executes raw Minecraft commands via
`client().execute(...)`. The aliases `ar` / `advrocketry` work
identically.

So per-test shape becomes:

```java
client().execute("ar setGravity 0 0.5");
assertEquals(0.5f, gravityOf(0), 1e-4f);
```

Two lines. The full test class is `~12 tests × 3-4 lines each` →
~50 LoC, vs. ~200+ if each test stood up its own server probe.

### Helpers — package-private statics

Put query helpers in `WorldCommandFixtures` (package-private), used
by both this suite and any future ones:

- `static float gravityOf(int dim)` — reads `gravitationalMultiplier`
- `static boolean torchListContains(Block b)` — reads
  `ARConfiguration.getCurrentConfig().torchBlocks`
- `static boolean planetExists(int dim)` — `DimensionManager.getInstance()
  .getDimensionProperties(dim) != null`
- `static int planetField(int dim, String key)` / `floatField(...)` —
  reflective read mirroring what `planet set` writes
- `static List<String> captureChatLines(Runnable invoke)` — installs
  a temporary chat sink on a synthetic `ICommandSender` and returns
  the captured lines; used by the few `planet get` / `planet list` /
  `star list` cases where the chat output IS the contract

### Why server-tier, not client e2e

`/ar` is server-side dispatch. No EntityPlayer needs to actually
*see* the chat — assertions go against world/registry state, with
chat-capture only for the `get`/`list` text-output cases. Server-tier
is ~10× faster per test than testClient and avoids the harness
flakiness.

### Why direct command execution, not a new probe

The repo already has a 8000-LoC `TestProbeCommand`. Adding mirror
verbs (e.g. `/artest planet set …`) for things `/ar` already does
would be pure duplication. The whole point of this suite is to
pin `/ar` itself — invoking it directly is the contract.

## Implementation Plan

### Phase 1 — Fixture + Misc subcommands (~2 h)

`WorldCommandFixtures` package-private helper class.

`WorldCommandMiscContractTest` (server-tier):

- `addTorchPutsHeldBlockInTorchList`
- `addSolidBlockOverridePutsHeldBlockInSealedList`
- `giveStationAddsStationChipWithBoundUUIDToInventory`
- `setGravityWritesDimensionPropertiesGravity`
- `setGravityRefusesNegativeMultiplier_documentsContract` (look
  at production — if no guard exists, write as
  `_documentsKnownBug` per `CLAUDE.md` ledger rule)
- `fillDataWritesNBTDataMapOnHeldItem`

~6 tests, ~30 LoC.

### Phase 2 — Planet family (~2.5 h)

`WorldCommandPlanetContractTest`:

- `planetSetWritesAtmosphereDensity`
- `planetSetWritesGravitationalMultiplier`
- `planetSetWritesAverageTemperature`
- `planetGetEchoesCurrentAtmosphereDensity` (chat-capture)
- `planetListIncludesAllRegisteredDims` (chat-capture, contains-check)
- `planetNewCreatesDimensionEntry`
- `planetDeleteRemovesDimensionEntry`
- `planetResetRestoresDefaultsAfterMutation`

~8 tests. The chat-capture cases use a single helper; everything
else uses reflective reads through `WorldCommandFixtures`.

### Phase 3 — Star family + dumpBiomes + goto (~1.5 h)

`WorldCommandStarMiscContractTest`:

- `starGenerateRegistersNewStarWithSuppliedTemp`
- `starSetTempUpdatesStellarBodyTemperature`
- `starListIncludesAllRegisteredStars` (chat-capture)
- `starGetTempEchoesCurrentTemperature` (chat-capture)
- `dumpBiomesWritesFileWithKnownVanillaBiomeName` (file-system check
  against the test-run workdir)
- `gotoTeleportsSenderToTargetDim` — uses a synthetic player
  sender or runs as `@p` once a player is connected
- `fetchPullsNamedPlayerToSender` — same caveat; may need a
  second harness player

~6 tests.

### Phase 4 — Edge cases + close-out (~1 h)

- `unknownSubcommandIsNoOp` — no chat error spam, no world mutation
- `helpSubcommandPrintsExpectedTopLevelEntries` (chat-capture)
- `reloadRecipesDoesNotCorruptRecipeRegistry` — count stable, one
  known-AR recipe present before+after
- EOD marker
- README counter bump + Done row

~3 tests.

**Total**: ~23 tests, ~150 LoC across 3 test classes.

## Technical decisions

- **Server-tier only**. Per `feedback_no_fakeplayer_for_player_tests`,
  player-touching tests live in testClient — but `/ar` itself runs
  on the server thread and observably mutates server state. The
  one or two cases needing a player (`goto`/`fetch`) can use the
  harness's existing fakeplayer-substitute (an `ICommandSender`
  implementation that points at a `WorldServer` and a `BlockPos`).
  If that proves insufficient, those two tests get hoisted to
  testClient — but the rest stay server-tier.

- **Reflective reads**. `DimensionProperties` fields are public
  for `planet set` to write via `Field.set`; tests use the same
  `Field.get` path. This is the same shape used in `/artest planet
  info` probes — no new mechanism.

- **Chat capture via `ICommandSender` impl**. Implement a tiny
  `CapturingCommandSender` in `WorldCommandFixtures` that records
  every `sendMessage(ITextComponent)` call into a `List<String>`.
  Used for the 5-6 tests where text IS the contract. Production's
  `sendMessage` is the contract API — pinning ON the captured list
  is testing the sender interface, not impl.

- **No fixture rocket / no probe addition**. The whole point is
  to pin `/ar` end-to-end. New probe verbs would dilute the test;
  use raw `Field.get` reads to keep the proof chain short.

- **No production logic changes** — record any bug found in
  `.agent/tasks/README.md` ledger per CLAUDE.md.

## Dependencies

- **Requires**: existing `AbstractSharedServerTest` + the
  reflective-read helpers used by current `TestProbeCommand`
  planet info paths.
- **Does NOT block**: anything in the current backlog.

## Risks

1. **`commandPlanetGenerate` mutates persistent state** — leaving
   dims registered across tests pollutes downstream. Mitigation:
   `@After` deletes any test-created dims by id range; use
   `dim >= 9500` for this suite (above the 9401/9402 used by
   `AtmospherePlayerEventE2ETest`).

2. **`addTorch` / `addSolidBlockOverride` mutate
   `ARConfiguration.getCurrentConfig()`** — leaks between tests.
   Mitigation: capture pre-state lists in `@Before`, restore in
   `@After`.

3. **`dumpBiomes` writes to the server work dir** — the path may
   differ between harness forks. Mitigation: read `run-server/`
   or the harness's actual workDir via the same env the harness
   uses; if not exposed, assert "file with prefix `biomes_dump`
   exists in workDir" without pinning the exact name.

4. **`reloadRecipes` is slow** — full recipe re-registration.
   Run it as the last test in its class to amortize the cost.

## Estimated effort

~7 hours across 2-3 sessions:
- Phase 1: 2 h
- Phase 2: 2.5 h
- Phase 3: 1.5 h
- Phase 4: 1 h

## Completion Checklist

- [x] Phase 1: `WorldCommandPlanetSetGetContractTest` — 5 tests
      (atmosphereDensity / gravitationalMultiplier / rotationalPeriod
      set + get echo + list-contains-overworld).
- [x] Phase 2: `WorldCommandPlanetLifecycleContractTest` — 4 tests
      (generate-adds / generate-names / delete-removes / reset-baseline).
- [x] Phase 3: `WorldCommandStarMiscContractTest` — 6 tests
      (star list + get-temp + set-temp + generate + dumpBiomes file +
      reloadRecipes `_documentsKnownBug`).
- [x] Phase 4: `WorldCommandGuardContractTest` — 8 tests
      (addTorch / addSolidBlockOverride / setGravity / fillData / goto
      / fetch / giveStation refuse-console + unknown-sub quiet).
- [x] Bug logged in `.agent/tasks/README.md` ledger (entry #7,
      `commandReloadRecipes` frozen-registry crash).

**Tests landed**: 23 (server-tier). All green.

**Scope cuts from plan**:
- `planetSetAverageTemperature` dropped — production
  `DimensionProperties.getAverageTemp()` (line 2002) recomputes the
  field from star + orbital + atmosphereDensity on read, so it's a
  derived quantity. Pinning a write to a derived field would test
  impl rather than contract.
- `goto`/`fetch`/`addTorch`/`addSolidBlockOverride`/`setGravity`/
  `fillData` positive (player-equipped) cases NOT covered — they need
  a real EntityPlayerMP per `feedback_no_fakeplayer_for_player_tests`.
  Negative (guard-pinning) cases captured here; positive cases live
  in testClient e2e if/when a future ticket needs them.

**Helper LoC**: `WorldCommandFixtures` 80 LoC; test classes 60 / 90
/ 100 / 80 LoC respectively. Average test body ≤ 6 lines per the
plan budget. Result-focused: each pin asserts an observable state
change (registry, JSON probe field, file existence, chat envelope)
rather than dispatch-chain internals.
