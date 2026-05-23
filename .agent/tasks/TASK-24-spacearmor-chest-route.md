# TASK-24: SpaceArmor chest sub-inventory drain route

## Ticket

- Source: 2026-05-23 audit — Gap #10. TASK-10b Phase 7's
  `ItemSpaceArmorUseFluidE2ETest` covered the cheaper enchanted-
  vanilla-armor route for suit oxygen drain. The suit-family
  CHEST route (multi-component sub-inventory) is deferred.
- Status: **Backlog**.
- Created: 2026-05-23.

## Context

`ItemSpaceArmor` (the suit-family chestplate) accepts upgrade
components via a sub-inventory: oxygen tank, pressure tank,
jetpack, etc. The oxygen tank is the relevant component for the
breathing contract — its NBT under the chestplate's NBT carries
the actual `air` value that drains in vacuum.

What's already pinned:

- ✅ Enchanted vanilla armor with the Space Breathing enchant —
  `ItemAirUtils.setAirRemaining(stack, n)` writes directly to the
  armor stack's NBT. Drain pinned by
  `ItemSpaceArmorUseFluidE2ETest`.
- ✅ Empty-stack defaults, capability dispatch, protectsFromSubstance
  matrix — all pinned by `SpaceArmorContractTest` and
  `SpaceArmorProtectionContractTest`.
- ✅ Component item NBT round-trip (single-item) — pinned via
  `ItemDataCarrierNBTRoundTripTest`.

What's NOT pinned:

- ❌ Drain through the CHEST route: vacuum → AtmosphereHandler →
  AtmosphereNeedsSuit.isImmune → finds chest's oxygen tank
  component → decrements its NBT-stored air. The full chain has
  multiple hops where a regression could silently zero out the
  drain effect.

## Why "heavy fixture"

The cheaper route uses a single-stack ItemStack with a top-level
`air` NBT key. The CHEST route requires:

1. A `ItemSpaceChest` stack with a populated sub-inventory NBT.
2. An oxygen tank component item INSIDE that sub-inventory.
3. The component's own air value updates on drain (not the chest's).

Building this state requires either:

- Probe-set: `/artest player set-chest-components <player>
  <tank-air-amount>` — needs a new verb.
- Test fixture: pre-construct an `ItemStack` with the right NBT
  tree, hand it to the bot via existing item-give probe.

The pre-constructed-NBT approach is cheaper; the player-set-via-
GUI route is the high-fidelity option but needs a working Suit
Workstation interaction.

## Implementation plan

### Phase 1 — Pre-constructed NBT fixture (~2 h)

Add helper to `WorldCommandFixtures` (or a new fixture class):

```java
static ItemStack spaceChestWithChargedOxygenTank(int airAmount);
```

The helper assembles the NBT tree:

```
{
  components: [
    {
      id: "advancedrocketry:itemPressureTank",
      ItemAirUtils.air: 1000
    }
  ]
}
```

Test: `SpaceArmorChestRouteDrainE2ETest` (testClient):

- `vacuumDrainsOxygenFromChestSubInventoryTank` — equip bot with
  helper-constructed chest, teleport to vacuum dim, force atmosphere
  ticks, assert sub-inventory tank's air decreased.
- `fullyDrainedChestNoLongerProtects` — set tank air to 0,
  vacuum-tick, assert bot starts taking suffocation damage.
- `rechargedChestResumesProtection` — drain to near-zero,
  recharge via probe-set, assert no damage.

### Phase 2 — Suit Workstation positive path (~3 h, optional)

Existing `SuitWorkStationAssemblesSuitTest` pins component-consumed-
into-NBT for the assembly side. Phase 2 would add the **drain**
side via the Suit Workstation:

- `suitAssembledInWorkstationDrainsCorrectlyInVacuum` — drive the
  workstation through GUI clicks (testClient), assemble a chest +
  tank, equip, vacuum-tick, assert drain.

Optional because Phase 1's pre-constructed-NBT route already
exercises the **drain contract**; Phase 2 would close the
*assembly → drain* chain. Defer to a Phase 2 ticket if the
assembly side ever regresses.

## Acceptance

- [ ] Phase 1: test class with ≥3 tests covering chest sub-inventory
      drain shape.
- [ ] `WorldCommandFixtures` (or sibling) has the NBT-constructor
      helper.
- [ ] Pyramid counter regenerated per TASK-17 phase 1.

## Technical decisions

- **NBT-constructor helper, not real workstation assembly** — Phase 1
  isolates the *drain* contract from the *assembly* contract. They
  have separate regression surfaces.
- **testClient required** — vacuum drain needs a real player whose
  inventory ticks; testServer can't simulate player atmosphere ticks.
- **No production logic changes**.

## Out of scope

- Phase 2 (Suit Workstation drive-through) — optional. Splits to
  a separate ticket if needed.
- Other suit components (jetpack fuel, pressure tank thermal) —
  each has its own contract; this task scopes only oxygen
  drain via chest sub-inventory.

## Dependencies

- Depends on: testClient harness stable.
- Does NOT block any other task.
- Pattern source: `ItemSpaceArmorUseFluidE2ETest` (the cheaper
  enchanted-armor route).

## Estimated effort

- Phase 1: ~2 h
- Close-out: ~30 min
- **Total**: ~2.5 h (Phase 1 only)
- Phase 2 if added: +3 h
