# TASK-35: /ar fetch positive coverage

## Ticket

- Source: 2026-05-25 Tier 1 audit. Deferred at the time because of
  the harness requirement; carried forward into 2026-05-26 audit.
- Status: **Blocked** — see Blocker section.
- Created: 2026-05-26.

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
