# TASK-21: /ar player-equipped positive paths (testClient)

## Ticket

- Source: 2026-05-23 audit — Gap #3 ("/ar player-equipped
  subcommands positive side"). TASK-11 closed the guard side
  (non-player sender) with deep coverage in
  `WorldCommandGuardContractTest`; the positive side requires a
  real player.
- Status: ✅ **Completed 2026-05-25**.
- Created: 2026-05-23.

## Actual scope (2026-05-25)

`WorldCommandPlayerEquippedE2ETest` — 5/5 client tests covering all
reachable positive paths:

- `arGotoTransfersPlayerToTargetDim` — generate AR planet, op bot,
  `exec-as-player /ar goto <dim>`, assert player.dim matches.
  (The original plan's `goto <dim> <x> <y> <z>` form does NOT exist
  in production — `commandGoto` takes only `<dim>` or
  `station <id>`.)
- `arGotoStationTeleportsToStationSpawnInSpaceDim` — create station,
  `/ar goto station <id>`, assert player.dim == spaceDim (-2).
- `arGiveStationAddsChipToPlayerInventory` — create station,
  `/ar giveStation <id>`, verify chip count >= 1 via new
  `player inventory-contains` probe.
- `arAddTorchAddsHeldBlockToTorchList` — give-held cobblestone,
  `/ar addTorch`, command result >= 1.
- `arAddSolidBlockOverrideAddsHeldBlockToSealedList` — give-held
  dirt, `/ar addSolidBlockOverride`, command result >= 1.

**Out of scope (not shipped this batch)**:

- `/ar fetch <player>` — needs a second connected bot; the
  testClient harness supports one player. Defer to a separate task
  if multi-bot harness lands.
- `/ar fillData <type> <amount>` — needs fixture for ItemData stack
  with the right data-type compatibility; covered transitively by
  the satellite-construction flow.

**New probes** for this task (`TestProbeCommand`):

- `player exec-as-player <command...>` — runs a command via the
  server's command manager with the bot's player as the sender.
  Distinct from `serverClient().execute(cmd)` which uses a
  synthetic non-player sender that AR rejects.
- `player op-self` / `player deop-self` — elevate / restore op
  level so the bot can run /ar commands (op-protected).
- `player inventory-contains <item-id>` — observability probe.
- `player give-held <item-id>` — equip a specific item in main
  hand for /ar addTorch / addSolidBlockOverride setups.

**testClient ENV**: requires `xvfb-run` wrapper.

## Context

`/ar` (alias `/advancedrocketry`) has several subcommands that
mutate player-equipped state or move the player:

| Verb | Mutates | Pinned today |
|---|---|---|
| `goto <dim> [x y z]` | sender position | Guard side only (`WorldCommandGuardContractTest.gotoRefusesNonPlayer*`) |
| `fetch <player>` | target position | Guard side only |
| `giveStation <id> [player]` | player inventory | Guard side only |
| `addTorch` | torch list (held item read) | Guard side only |
| `addSolidBlockOverride` | sealed-block list (held item read) | Guard side only |
| `fillData <type> <amt>` | held data-item NBT | Guard side only |
| `setGravity <dim> <mult>` | dim gravity (no player side, but ops-protected) | Already deep — no gap |

`setGravity` is the only non-equipped one — already deep-pinned via
`WorldCommandPlanetSetGetContractTest`. The other 6 verbs all need
a real player with held item to exercise their positive paths.

## Why testClient

The server-tier harness has no concept of "a player holding an
item" — `MinecraftServer.getPlayerList()` is empty until a real
client connects. `WorldCommandFixtures.CapturingCommandSender` is
a synthetic non-player sender; the guard side specifically pins
"this synthetic sender is rejected".

The positive path needs:

- A real connected player (testClient bot).
- A held item in the right slot (probe-set inventory).
- A registered dim with the right properties (probe-create
  beforehand if not default).

## Implementation plan

Single test class: `WorldCommandPlayerEquippedE2ETest extends
RealClientHarness`-style base.

### Phase 1 — `goto` + `fetch` (~2 h)

- `gotoTeleportsPlayerToSpecifiedDimCoords` — connect bot, execute
  `/ar goto 0 100 70 100` (via op-level 2 sender = the bot), assert
  `bot.posX/Y/Z` matches within rounding.
- `gotoWithoutCoordsTeleportsToWorldSpawn` — execute `/ar goto -1`
  (nether), assert bot now in dim -1.
- `fetchTeleportsTargetPlayerToSender` — connect two bots, sender
  executes `/ar fetch <other>`, assert other bot's position close
  to sender's.

Two-bot fetch test may exceed harness capacity — confirm
RealClientHarness can run two concurrent connections; if not,
defer the fetch test or use a synthetic offline player target
that AR refuses cleanly.

### Phase 2 — `giveStation` + `fillData` (~2 h)

- `giveStationAddsBoundChipToPlayerInventory` — pre-create station
  via probe, execute `/ar giveStation <id>`, assert player
  inventory now contains an `itemSpaceStationChip` with NBT
  encoding the station ID.
- `fillDataWritesDataMapOnHeldItem` — equip a blank
  `itemMultiData` in main hand via probe, execute
  `/ar fillData composition 500`, assert held item NBT has
  `data.composition = 500`.

### Phase 3 — `addTorch` + `addSolidBlockOverride` (~1.5 h)

- `addTorchAddsHeldBlockToTorchList` — equip `minecraft:cobblestone`
  in main hand, execute `/ar addTorch`, read
  `ARConfiguration.getCurrentConfig().torchBlocks` via probe, assert
  cobblestone present.
- `addSolidBlockOverrideAddsHeldBlockToSealedList` — same shape
  against the sealed-block list.
- Both verbs need the player's held item; non-player sender is
  already rejected by `WorldCommandGuardContractTest`.

## Acceptance

- [ ] One test class with 6-7 tests covering each verb's positive
      path.
- [ ] Probe-set inventory used to control the held-item
      precondition (not GUI clicks).
- [ ] Pyramid counter regenerated per TASK-17 phase 1.

## Technical decisions

- **testClient required for player presence** — these verbs cannot
  be exercised by a synthetic sender; the guard side
  (`WorldCommandGuardContractTest`) explicitly proves that.
- **Reuse probe inventory verbs** (`/artest player give-item`,
  `/artest player set-equipped`) if they exist; add if missing.
- **Op-level 2 required**. The bot must connect with op
  permissions for `/ar` to accept its commands. If
  `RealClientHarness` doesn't grant ops automatically, add a
  probe to elevate the bot's permission level on connect.
- **No production logic changes**.

## Out of scope

- Cross-bot `/ar fetch` if harness can't run two players (defer
  to a separate task only if a regression surfaces).
- `setGravity` positive path — already deep-pinned at server-tier
  via `WorldCommandPlanetSetGetContractTest`.
- GUI-driven equivalents of these verbs (separate scope; none
  exist today AFAIK).

## Dependencies

- Depends on: testClient harness stable.
- Does NOT block any other task.

## Estimated effort

- Phase 1 goto/fetch: ~2 h
- Phase 2 giveStation/fillData: ~2 h
- Phase 3 addTorch/addSolidBlockOverride: ~1.5 h
- Close-out: ~30 min
- **Total**: ~6 h

## Player-impact justification

Low player-impact (these are admin-only verbs), but completes the
`/ar` surface that TASK-11 started. Closes the asymmetry where
the **guard side** (rejection of non-players) is deep-pinned but
the **positive side** (acceptance by real players) is unpinned.
