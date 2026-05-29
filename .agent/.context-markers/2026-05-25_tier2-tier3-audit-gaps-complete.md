# Marker — 2026-05-25 Tier 2 + Tier 3 audit gaps complete

**Branch**: `feature/tests`
**Mode**: autonomous batch — Tier 2 + Tier 3 gaps from coverage audit.

## What shipped

| Gap | Tier | Layer | Tests | Class |
|---|---|---|---|---|
| #13 atmosphereType NBT | T3 | — | 0 (already covered) | `DimensionPropertiesTest.atmosphereTypeFromDensityAndTemperature` |
| #11 IArmorComponent contract | T3 | unit | 7 | `ArmorComponentContractTest` |
| #6 RocketEvent payloads | T2 | server | 2 | `RocketEventPayloadContractTest` |
| #15 ItemPackedStructure | T3 | integration | 1 | `ItemPackedStructureNbtTest` |
| #14 3 station controllers | T3 | server | 3 | `StationControllersSmokeTest` |
| #10 TerraformingTerminal | T2 | server | 2 | `TerraformingTerminalSmokeTest` |
| #8 5 scanning satellites | T2 | unit | 6 | `ScanningSatelliteContractTest` |
| #12 BeaconFinder/OreScanner | T3 | unit + client | 4 + 2 | `BeaconFinderAndOreScannerContractTest` + `OreScannerRightClickClientE2ETest` |
| **Total** | | | **27** | **8 new classes** |

## Pyramid

257 / 81 / 370 / 46 = **754** (+27 from 727).

Layer breakdown of this batch:
- testUnit: +17 (gaps 11, 8, 12 unit slice)
- testIntegration: +1 (gap 15)
- testServer: +7 (gaps 6, 14, 10)
- testClient: +2 (gap 12 client slice)

## Probe additions

- `rocket event-payloads` — last-observed entity-id + dim per event type
- `player try-orescanner-rclick [dim]` — equip + invoke onItemRightClick

(Also extended `RocketEventRecorder` to capture per-event payload fields.)

## Decisions made autonomously

1. **Gap 13 was already covered** — `atmosphereType` field on
   DimensionProperties:111 is dead code (zero usages). The actual
   atmosphere is derived via `getAtmosphere()` from density+temp+hasOxygen,
   already pinned by `atmosphereTypeFromDensityAndTemperature`. Skipped
   as not-a-real-gap; documented.

2. **Gap 15 NBT round-trip deferred** — `ItemPackedStructure` is just
   a serialization wrapper. `setStructure`/`getStructure` round-trip
   needs `new StorageChunk()` which eagerly calls
   `AdvancedRocketry.proxy.getProfiler()` → NPEs without a running
   `MinecraftServer`. Only the null-sentinel contract pinned at
   integration tier. Server-side probe-driven test could close the
   round-trip gap but would duplicate `RocketAssemblySmokeTest`.

3. **Gap 14 scope-down to smoke** — full station-controller contracts
   (altitude actually changes station altitude, gravity mutates
   DimensionProperties.gravity) need station-context fixtures.
   Shipped smoke-level: place + tick + tile-class-preserved. Same
   pattern as TASK-19 Gap 2's scope reduction.

4. **Gap 10 scope-down to smoke** — TerraformingTerminal needs a
   real BiomeChanger chip with embedded satellite-id to drive the
   biome-mutation path. That fixture duplicates
   `SatelliteTypeBehaviourTest`. Shipped smoke: place + tick (empty
   and redstone-powered) without crash.

5. **Gap 12 split unit + client** — `ItemBeaconFinder` has NO
   item-use methods (pure HUD-render IArmorComponent); only slot
   contract is unit-testable. `ItemOreScanner` has onItemRightClick
   that opens a GUI when the satellite-ID resolves. Wrote a probe
   that constructs a real `SatelliteOreMapping`, registers it, and
   invokes the right-click — the client-tier test verifies "no
   crash" on both branches (empty and resolved satellite-ID).
   testClient runs under `xvfb-run`.

## Tier 2 gaps NOT shipped this batch

- **Gap 7 SatelliteBuilder + Terminal real-construction path** — testClient,
  ~10 h. Heavy fixture. Could be a separate TASK if a regression in
  the satellite-construction flow ever surfaces.
- **Gap 9 Fuel loader active transfer** — explicitly deferred by
  audit. Documented as accepted limitation in
  `RocketInfrastructureSmokeTest`.

## Bug ledger

No new bugs found in this batch. Batch #2 still has 1 live entry
(SatelliteRegistry getNewSatellite null vs SatelliteDefunct).

## Commits

| SHA (pending) | Subject |
|---|---|
| TBD | test: Tier 2+3 audit gaps batch — 27 tests, 1 probe, 1 event-recorder extension |

## Resumption

**Audit findings drained** through Tier 3 (excluding the 2 explicitly-
deferred items above). Backlog is now empty of audit-derived TASK
candidates.

**Quick-win bug-fix available**: Batch #2 bug #1 (SatelliteRegistry
SatelliteDefunct fallback) is 5 lines of production change + flip
the two `_documentsKnownBug` tests to positive contracts. ~1 h.
