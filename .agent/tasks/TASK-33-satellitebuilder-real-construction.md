# TASK-33: SatelliteBuilder real-construction coverage

## Ticket

- Source: 2026-05-25 Tier 2 audit, deferred ("heavy testClient
  cost"). Confirmed still deferred in 2026-05-26 audit.
- Status: **Blocked** — see Blocker section.
- Created: 2026-05-26.

## Context

`TileSatelliteBuilder` is the player-facing GUI for assembling a
satellite from chips + components. Existing coverage:

- `SatelliteBuilderE2ETest` and friends pin smoke-tier construction
  (place builder, place chip, validate that chip-recognition
  works).
- Component-acceptance logic (`acceptsItemInConstruction`) is
  pinned per-satellite-type at unit tier (TASK-09).

What's NOT pinned: **real end-to-end construction** — drop a full
set of compatible chips into the builder, complete the assembly,
verify the produced `ItemSatellite` carries the right `id` /
properties / battery / per-type metadata. The full UI flow with a
real player.

## Why it matters

Satellite construction is the canonical "play loop" for the mod's
mid-game progression. A regression in chip-routing or
multi-chip-resolution silently breaks every satellite-based
gameplay loop. The current tests confirm individual pieces work;
they do not confirm a full assemble cycle produces a working
satellite.

## Blocker

Needs a testClient harness that can:

1. Open a `GuiSatelliteBuilder` instance bound to a real
   `TileSatelliteBuilder`.
2. Place items in the builder's input slots (existing
   `hatch fill` probe partially covers).
3. Click the "build" button via the existing testClient
   `bot().click(...)` infrastructure — OR via a new
   `gui press-button <buttonId>` probe.
4. Observe the resulting `ItemSatellite` in the output slot,
   inspect its NBT.

Item 3 is the real blocker — the testClient bot's GUI button
support exists for some GUIs (see GuidanceComputerGuiE2ETest)
but the SatelliteBuilder GUI uses a custom module layout that
may need an additional `gui press-build-button` probe.

## Implementation plan

| Phase | Effort | Result |
|---|---|---|
| 0 | ~2 h | Audit `bot().click(...)` surface. Identify whether the existing button-press mechanism can target a `ModuleBuildButton` instance, or if a new probe is needed. |
| 1 | ~3 h | `SatelliteBuilderFullConstructionE2ETest` — for each of the 3-4 "main" satellite types (solar, microwave, biomechanger, weather): place chip set in builder, press build, assert output `ItemSatellite` carries correct registry name + matches expected satellite class. |
| 2 | ~2 h | NBT depth pin: produced ItemSatellite's NBT carries `satelliteProperties` + battery capacity + per-type config. |

## Acceptance

- [ ] 3-4 testClient e2e tests, one per main satellite type.
- [ ] Each test exercises the full GUI flow: place chip set →
      press build → output slot has correct satellite item.
- [ ] No production logic changes (per CLAUDE.md rule).
- [ ] Pyramid counter regenerated.

## Out of scope

- Per-type item-acceptance permutations (covered at unit tier).
- Satellite-deploy flow (separate scope — what happens after the
  satellite is loaded into a rocket and launched).
- Edge cases: missing required chip, conflicting chips, full
  output slot. These can land in a Phase 3 if motivated.

## Dependencies

- Does NOT block any other task.
- Once unblocked (Phase 0 probe lands), Phases 1-2 are
  straightforward.

## Estimated effort

- Phase 0: 2 h
- Phase 1: 3 h
- Phase 2: 2 h
- **Total**: ~7 h

## Risk

Medium-high. testClient bot stability + xvfb harness (per the
recurring DISPLAY=:77 / LWJGL flake history) increases the chance
of intermittent failures.

## Phase 0 audit findings (2026-05-26)

**Verdict: FEASIBLE without xvfb dependency.**

- `TileSatelliteBuilder.onInventoryButtonPressed(int buttonId)` at
  `:208-219`: `buttonId=0 → assembleSatellite()`, `buttonId=1 →
  copyChip()`. Client side sends via `PacketHandler.sendToServer(new
  PacketMachine(this, (byte)(buttonId + 100)))` — server packet
  dispatch.
- `bot().clickButtonById()` is proven working in
  `RocketBuilderGuiE2ETest:70,78` (paired with
  `ClientGuiTestSupport.java:38-55`).

**Cleanest probe (avoids xvfb):**
`/artest satellite-builder build <dim> <x> <y> <z>` — server-side
subcommand that directly calls
`((TileSatelliteBuilder) tile).onInventoryButtonPressed(0)` (or
equivalently sends the equivalent PacketMachine). Avoids client-bot
flake history. Mirrors `bot().clickButtonById()` test-client path
on the server side.

Tests become **testServer**, not testClient — cuts xvfb risk
entirely.
