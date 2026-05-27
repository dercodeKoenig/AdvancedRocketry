# TASK-35: /ar fetch positive coverage

## Ticket

- Source: 2026-05-25 Tier 1 audit. Deferred at the time because of
  the harness requirement; carried forward into 2026-05-26 audit.
- Status: **✅ Completed 2026-05-26 (reframed scope, see Outcome).**
- Created: 2026-05-26.

## Outcome (2026-05-26)

Shipped `WorldCommandFetchTest` (2 testClient tests). Original
Phase 0 plan (heavy NetworkManager-stub real-EntityPlayerMP probe)
was reframed during implementation:

- **Self-fetch positive pin** — bot runs
  `/ar fetch <bot-own-username>` against itself; production
  resolves the name, transfers to the same dim (no-op), and
  sets the bot's position to the sender's own coords. Pins
  the full resolve → transferPlayerToDimension → setPosition
  path with sender == target — no second player needed. Bot
  username is discovered via the existing
  `/artest player health` probe (returns `player.getName()` in
  its JSON).
- **Unknown-name negative pin** — `/ar fetch <bogus>`
  exercises the `getPlayerByName == null` branch. Pins
  "command runs cleanly + reaches the negative branch".

Key insight: positive coverage doesn't actually need a SECOND
player. The original framing assumed "different player as
target"; self-fetch covers the verb's contract surface (resolve
+ transfer + setPosition) without that infrastructure cost.

Still out of scope (intentional): true moderator-fetch where
target is a different connected player. Needs multi-client
testClient harness expansion (separate scope).

## Outcome — Multi-client moderator-fetch (2026-05-26)

Shipped `WorldCommandFetchModeratorTest` (1 testClient test) +
multi-client harness support in ForgeTestFramework.

### Framework changes
`RealClientHarness.start(server, username)` new overload — was
hardcoded `CLIENT_USERNAME = "ForgeTestClient"`. Additionally
moved `--username` and `--uuid` arg propagation OUT of the
`legacyArgs` block, since AR's test setup uses FG6's
`net.minecraftforge.legacydev.MainClient` (legacyArgs=false) which
WAS skipping the username arg → FG6's `MainClient.getDefaultArguments`
seeded username=null → random `Player###` names that broke
PlayerList name resolution.

### AR probes added
- `/artest player exec-as-named <name> <cmd>` — runs command with
  the named player as sender (the existing `exec-as-player` hard-
  codes `players.get(0)`).
- `/artest player position-of <name>` — read named player's
  dim/coords.
- `/artest player op-named <name>` — op a specific named player.

### Test design
- Two bots `ModBot1` (op) + `ModBot2` started sequentially (~60-90s
  each). Both connect to the same dedicated server.
- Bots `/tp`'d to (100,80,100) and (200,80,200) respectively.
- `/ar fetch ModBot2` issued as ModBot1 → ModBot2's post-fetch
  position must equal ModBot1's pre-fetch position (±1.5 blocks for
  same-dim transferPlayerToDimension nudging).
- Wallclock ~3-4 min, ~7 GB RAM.

testClient now requires `-PuseLocalFramework=true` until the
framework change is published.

## Context

`WorldCommand.commandFetch` (`/ar fetch <player>`) teleports a
target player to the sender's location. TASK-11 / TASK-21 covered
the rest of the player-equipped `/ar` subcommand surface but
`/ar fetch` was deferred because positive coverage requires **two
players online** — the sender (who runs the command) and the
target (who gets teleported).

Existing negative coverage:
- `WorldCommandAtConsoleSenderTest` covers the "console can't
  fetch — needs Entity sender" branch.
- `WorldCommandPlayerEquippedE2ETest` does NOT cover this verb.

What's NOT pinned: the actual teleport happens — sender's
position becomes target's position (or vice-versa, depending on
the production semantics).

## Why it matters

`/ar fetch` is a moderator/admin tool for unsticking players.
Regression possibilities:
- Verb syntax breaks → admin can't recover stuck players.
- Position arithmetic wrong → fetches to wrong coords.
- Permission check broken → unauthorized players fetch.

## Blocker

The testClient harness today supports ONE bot client. The
two-player verbs (fetch, goto-player if it existed, etc.) need
a second bot connected to the same server simultaneously.

Concrete blocker: `testing.client.RealMinecraftClientHarness`
does not expose multi-client startup. Either:

1. Extend the harness with `RealMinecraftClientHarness.startSecond
   Client()` that joins the same world as the first bot.
2. Drive the second player via a FakePlayer construction probe —
   server-side fake player that ConsoleSender treats as a real
   Entity. Production's permission gate checks
   `sender instanceof Entity`; a FakePlayer is.

Option 2 is lighter — no second JVM, just a server-side fake.

## Implementation plan

| Phase | Effort | Result |
|---|---|---|
| 0 | ~3 h | Add probe: `player spawn-fake-player <name> <dim> <x> <y> <z>` — creates a `FakePlayer` (or `EntityPlayerMP` via reflection) named `name`, registered with the server's player list so `/ar fetch <name>` can resolve it. |
| 1 | ~2 h | `WorldCommandFetchTest` — 3 tests: (a) bot fetches the fake player to bot's pos, assert fake-player.pos == bot.pos. (b) `/ar fetch` with unknown player name reports error. (c) `/ar fetch` from console sender refuses (negative parity with `WorldCommandAtConsoleSenderTest`). |

## Acceptance

- [ ] 3 tests pinning the positive + 2 negative branches.
- [ ] Probe verb documented in `TestProbeCommand` javadoc.
- [ ] Pyramid counter regenerated.

## Out of scope

- Permission depth (op vs non-op). Separate scope.
- Cross-dim fetch. The verb supports it; this is an extension if
  motivated.

## Dependencies

- Does NOT block any other task.
- Once unblocked (Phase 0 probe lands), Phase 1 is mechanical.

## Estimated effort

- Phase 0: 3 h
- Phase 1: 2 h
- **Total**: ~5 h

## Risk

Medium. FakePlayer registration with the PlayerList has historical
gotchas (Forge's `FakePlayerFactory` is the standard tool but its
return value isn't trivially treated as a real online player by
all server systems).

## Phase 0 audit findings (2026-05-26)

**Verdict: FakePlayer path BLOCKED — must use real EntityPlayerMP.**

- `WorldCommand.commandFetch:361` calls
  `getPlayerByName(:992)` which iterates each world's
  `world.getPlayerEntityByName(name)` — only real `EntityPlayerMP`
  instances in the world entity list. FakePlayer is NOT registered
  there.
- No existing `FakePlayerFactory` usage in the AR codebase
  (verified by grep).
- `TestProbeCommand.java:8755+` `handlePlayer` already exists but
  only operates on already-connected EntityPlayerMPs.

**Path chosen (user decision 2026-05-26): spawn real EntityPlayerMP.**

Probe shape:
`/artest player spawn-real <name> <dim> <x> <y> <z>` —
constructs a minimal EntityPlayerMP with synthesised GameProfile
+ stub NetHandlerPlayServer, registers in
`server.getPlayerList().getPlayers()` so
`WorldServer.getPlayerEntityByName(name)` resolves.

Risk: stub NetHandlerPlayServer construction is historically flake-
prone (NetworkManager requires Channel; can be no-op stub but every
packet send must be guarded). Budget +1h flake-investigation if
the test goes intermittent.

Alternate considered + rejected:
- Patch `commandFetch` to accept entity-id fallback — violates
  CLAUDE.md "no production logic changes" rule.
