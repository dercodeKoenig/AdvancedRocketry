# Marker — 2026-05-25 Tier 1 coverage-audit gaps complete

**Branch**: `feature/tests`
**Mode**: autonomous batch, 4 Tier-1 gaps from the post-TASK-26 audit.

## What shipped

| Gap | Class | Tests | New probes | Tier rationale |
|---|---|---|---|---|
| 4 | `SatelliteRegistryFallbackTest` (unit) | 3 | — | save-compat for unregistered mod-satellite types |
| 1 | `RocketPreLaunchEventCancellationTest` (server) | 2 | `rocket arm/disarm-prelaunch-cancel` + `prelaunch-cancel-counts` + `launchCounter` in rocket info | public @Cancelable API contract |
| 5 | `OxygenVentRequiresFuelAndPowerTest` (server) | 3 | — (reused existing vent probes) | base-gameplay oxygen-vent counter-branches |
| 2 | `RocketServiceStationLinkAndStateTest` (server) | 2 | `infra service-state` (reflective) | service-station observability (full repair cycle deferred) |

**Total**: 10 new tests (3 unit + 7 server) across 4 new classes,
4 new probe verbs + 1 new field in `rocket info`.

## Pyramid

240 / 80 / **363** / 44 = **727** (+10 from 717).

## Bug ledger

**1 new live bug logged** (Batch #2 opened):

`SatelliteRegistry.getNewSatellite` returns `null` for unknown types
contradicting its javadoc which promises `SatelliteDefunct` fallback.
Downstream `createFromNBT` NPEs immediately. The shipping save-load
path catches the NPE; packet-handler and item paths don't. Pinned by
two `_documentsKnownBug` tests in `SatelliteRegistryFallbackTest`.

Fix candidates (for future TASK):
- `SatelliteRegistry.java:97` — return `new SatelliteDefunct()` instead of `null`.
- OR null-guard every caller of `getNewSatellite` / `createFromNBT`.

## Decisions made autonomously

1. **Gap 4 turned into a bug-ledger entry, not just a gap pin.** The
   audit hypothesised `SatelliteDefunct` was a working fallback; the
   code reads `return null`. Two `_documentsKnownBug` tests pin the
   buggy contract.

2. **Gap 5 (`notfullblock`-style edge case discovered)**: the audit
   plan assumed "no fluid → vent un-seals". Production reality: vent
   stays `isSealed=true` but flips `hasFluid=false` and reverts the
   atmosphere type to dim baseline. Test assertion rewritten to pin
   `hasFluid:false` + atmosphere-reverts contract instead.

3. **Gap 2 scope reduction**: full repair cycle (inject worn parts +
   adjacent PrecisionAssembler + run cycle) requires ~6-8 h fixture
   infra to inject `TileBrokenPart` instances with stage>0 into a
   rocket's StorageChunk. Deferred. Shipped lighter scope: link/state
   observability via new `infra service-state` reflective probe
   + fresh-rocket invariant (zero worn parts on assembly).

4. **Gap 1 probe design**: the `RocketPreLaunchEvent` listener
   approach uses a static `volatile` toggle + lazy event-bus
   registration. Tests MUST disarm in `@After` — a leaked-armed
   canceller would break every subsequent rocket-launch test in the
   shared harness. Explicit defensive `disarm` in `@After`.

## Audit gaps NOT shipped this batch (Tier 2/3 backlog)

- Tier 1 gap 3 (EntityElevatorCapsule ride cycle) — deferred per
  audit recommendation (high fixture cost; methodologically follows
  TASK-20 Hovercraft).
- Tier 2: 5 untested scanning satellite types (OreMapping, Density,
  Composition, MassScanner, Optical, SpyTelescope tick behaviour).
- Tier 2: 3 station controllers (Altitude / Gravity / Orientation).
- Tier 2: RocketLandedEvent / RocketDismantleEvent / RocketDeOrbiting
  payload contract for external subscribers.
- Tier 3: ItemPackedStructure deploy contract.
- Tier 3: custom atmosphereType NBT round-trip.

These are documented in the audit and live as candidates for the
next coverage batch.

## Commits

| SHA (pending) | Subject |
|---|---|
| TBD | test: Tier 1 audit gaps — service station + oxygen vent + prelaunch cancel + satellite registry (10 tests, 4 probes, 1 bug logged) |

## Resumption

Next-session paths:

1. **Tier 2 backlog**: 5 satellite scanning types is the biggest
   single coverage chunk; same shape as TASK-09. ~6 h.
2. **Gap 3 (elevator capsule)**: after TASK-20 Hovercraft as
   methodological prereq.
3. **Bug-fix pass for Batch #2**: actually fix
   `SatelliteRegistry.getNewSatellite` to return SatelliteDefunct,
   flip the `_documentsKnownBug` test pair to positive contracts,
   drain the ledger entry. Trivial (~1 h).
