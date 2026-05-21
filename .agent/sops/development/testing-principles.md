# SOP: Testing Principles — what tests should verify

## Context

Applies to ALL test layers: testUnit, testIntegration, testServer,
testClient. The point of this SOP is to keep tests focused on the
right thing — and to push back when the agent (or anyone) starts
adding "tighter pins" that aren't pinning what matters.

## The core rule

**Tests verify contracts. They do NOT verify implementation details.**

A contract is something a **caller** depends on:

- the player (player-facing behaviour),
- another piece of code (public API),
- the modpack ecosystem (registry IDs, NBT format, network packets),
- another mod (cross-mod hooks),
- the save file (persistence format).

An implementation detail is something the production code happens
to do today and could change tomorrow without anyone outside the
class noticing.

## What counts as a contract

**Player-visible behaviour**:

- "Sleep is refused in vacuum"
- "Fall damage is reduced on low-gravity dims"
- "WENT_TO_THE_MOON fires when player visits Luna near the lander coords"
- "A BiomeChanger satellite, given power and a queued position,
  eventually changes the biome there"
- "A satellite marked dead stops ticking"
- "Microwave receivers can accept energy from solar satellites"
  (the IUniversalEnergyTransmitter marker)

**Public API surface** (anything in `api/`, anything subclasses or
other modules call):

- `SatelliteBase.tickEntity()` is invoked once per dim tick on
  canTick=true satellites
- `AtmosphereHandler.onPlayerChangeDim` clears the per-player cache
- NBT round-trip preserves observable state across server restart

**Integration / wire / save format**:

- Registry names of blocks, items, entities, satellites, advancements
- NBT keys and shape (because saves must load on the next boot)
- Network packet IDs and field order
- Achievement / advancement IDs
- `OreDict` entries

## What does NOT count as a contract

**Specific magic numbers**:

- "exactly 120 RF per processed position" — implementation choice
- "loop bound = 10 per tick" — implementation choice
- "collectionTime = floor(200/sqrt(0.1·powerGen))" — implementation
  formula
- "powerGen - 1 per tick accrual" — the `-1` is impl; the contract
  is "battery accrues at approximately powerGen rate while the
  satellite has work to do"

**Internal data structures**:

- `LinkedList` vs `ArrayList` for `toChangeList` — impl
- Whether mode 2 uses a separate code branch from mode 0 — impl;
  the contract is "each mode produces the right visible effect"
- Internal field names — impl (saves DO pin field names indirectly
  through NBT; those go into the NBT-format contract bucket
  separately)

**Internal helpers**:

- private gate functions, private predicates — impl
- order of operations within a single method — impl (unless that
  order has an observable side effect)

## The litmus test

Before adding an assertion, ask:

1. **If I rewrite the implementation to preserve user-visible
   behaviour, does this assertion still pass?** If no, the
   assertion is over-tight.
2. **Does the thing this assertion pins appear anywhere a caller
   can observe?** Wiki, public Javadoc, network packet, NBT key,
   chat message, achievement ID, registry name, save format. If
   no, it's impl.
3. **Is this assertion the difference between "the feature works"
   and "the feature is broken"?** If passing/failing only signals a
   refactor not a regression, redesign.

## When tighter pins ARE warranted

Tight pins exist for things where exact details ARE the contract:

- **NBT format pins** — save compatibility. Worth asserting exact
  keys and shapes.
- **Registry name pins** — breaks `/give`, saves, recipes, JEI.
  Worth pinning the exact string.
- **Network packet schema** — wire compat with the client.
- **Achievement / advancement IDs** — externally referenced.
- **OreDict membership** — cross-mod compat.

Even here: pin the schema, not the values that flow through it.
"NBT has a key called `dataType`" is a contract; "the value in
`dataType` is the literal string `COMPOSITION`" depends — if other
code switches on that string, it's a contract; if only one place
reads/writes it, it's impl.

## Loose end-state pins are good

A pin like "after a tick with battery and a queued position, the
biome at pos is no longer the original biome" is the right shape:
it asserts the **observable outcome** (biome changed) without
asserting the **mechanism** (cost per position, loop bound, exact
RF debited).

A pin like "battery dropped by exactly 120 RF" is the wrong shape.
The player doesn't see 120; they see "satellite ran out of power
faster when it had more work".

## Anti-patterns from past audits

These show up when someone (the agent, a contributor) does a
"depth audit" and starts proposing tightenings:

- **"Doesn't pin exact cost = N RF"** — usually noise. If the
  visible effect is pinned, the cost is impl.
- **"Doesn't pin upper bound of loop = 10"** — almost always noise.
  The contract is "the queue drains over time"; bound is impl.
- **"Doesn't pin specific private field updated"** — noise.
- **"Mode X uses same code path as mode Y"** — only matters if X
  and Y have different visible effects.
- **"Doesn't pin exact tick where gate fires"** — pin "fires
  within reasonable window", not exact tick.

## Applying this to test design

**Before writing a test**, complete this sentence in one line:

> "This test fails if production breaks the contract that
> __________________________."

If the blank reads like an impl detail ("exact RF cost",
"specific loop bound", "specific field name"), redesign the test
to assert the contract instead.

**Before adding a new pin to an existing test**, run the litmus
above. Be willing to delete proposed assertions that don't pass
the litmus.

**During depth audits**, count contract-coverage, not pin-count.
"How many user-visible behaviours are pinned?" beats "how many
asserts in the file".

## Required reading

Anyone writing or reviewing tests in this repo should re-read
this SOP at the start of the task. The Navigator
(`.agent/DEVELOPMENT-README.md`) and `CLAUDE.md` link to this
SOP for that reason.

## Related

- `.agent/DEVELOPMENT-README.md` — Navigator entrypoint, includes
  a one-paragraph summary + link back here.
- `CLAUDE.md` — top-level project instructions, requires reading
  this SOP before authoring tests.
