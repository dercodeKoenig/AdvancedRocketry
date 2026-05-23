# TASK-13: Wireless transceiver E2E coverage

## Ticket

- Source: pivot from the original TASK-13 "Pipe end-to-end" scope.
  Upstream commit `48610953` (`deprecating pipes, added wireless
  transciever, closes #1075 #1034 #771 #757`) intentionally
  retired placed-block pipes in favour of `BlockTransciever` (the
  wireless replacement). The original TODO comment at
  `AdvancedRocketry.java:782-787` was misleading — the blocker was
  a deliberate product decision, not a fixable bug. Discovered
  during the 2026-05-23 SSOT cleanup session.
- Status: **✅ Completed 2026-05-23**.
- Created: 2026-05-23.

## Context — what shipped vs what's open

`TileWirelessTransciever` is the active replacement for the three
deprecated pipe blocks. Registration is at
`AdvancedRocketry.java:635` (tile-entity at line 424; block at 635;
block-registry at 809). It is the **only** live data-network
endpoint the player can place; the pipe-block tile entities at
lines 401-403 are kept registered for save-compatibility only.

### What's already covered

1. `PipeNetworkSmokeTest.wirelessTransceiverPairsAndTransmits` —
   single happy-path pin: place two transceivers, pair, assert
   shared `networkID` ≠ -1 and matches across both endpoints.
2. `wireless-pair` + `wireless-info` probe verbs in `TestProbeCommand`
   (lines 2455-2574) — mirror the `onLinkComplete` four-branch
   network-merge logic without needing an `ItemLinker` item.
3. `PipeNetworkHandlerDeepTest` (unit-tier) — pins the
   `CableNetwork` / `EnergyNetwork` / `HandlerCableNetwork` merge
   contracts that the transceiver routes through (via
   `NetworkRegistry.dataNetwork`).

### Contract gaps this task closes

Per [`testing-principles`](../sops/development/testing-principles.md),
count contract-coverage. The following player-visible contracts are
unpinned:

| # | Contract | Why it matters |
|---|---|---|
| 1 | Pairing branch: both unpaired → fresh ID assigned | Single-test happy-path only — covers this case, but assert is "id ≠ -1", not "id is freshly minted" |
| 2 | Pairing branch: only A paired → B inherits A's id | Untested |
| 3 | Pairing branch: only B paired → A inherits B's id | Untested |
| 4 | Pairing branch: both paired, different ids → merged | Untested |
| 5 | Pairing branch: both paired, same id → no-op | Untested |
| 6 | NBT round-trip: `mode`, `enabled`, `networkID`, MultiData survive server restart | Save-compat regression class |
| 7 | Mode toggle: `extractMode=true` registers tile as a **source** on the data network; `extractMode=false` registers as **sink** | Player flips toggle button — silent regression if removeFromAll / addSource / addSink polarity is wrong |
| 8 | Enabled gate: when `enabled=false`, `extractData`/`addData` return 0 regardless of buffer state | Player turns OFF the transceiver — must stop transmission |
| 9 | onChunkUnload removes from network | Cross-chunk loaded/unloaded state coherence |
| 10 | onLoad re-registers as the configured role (source vs sink) | Restart preserves player-visible role |

10 contracts. The first 5 (pairing branches) can run from a single
test class via the existing `wireless-pair` probe (its branch logic
mirrors `onLinkComplete` exactly — see `TestProbeCommand:2503-2522`).
Contracts 6-10 need a small extension of the existing probe
surface (read mode/enabled, write mode/enabled, optional buffer
seeding) and one persistence-restart test.

### Out of scope for this task (future work)

- **Adjacent-tile data flow** (transceiver pushes data INTO / pulls
  FROM an adjacent `IDataHandler` via `update()`). Requires a
  placed `IDataHandler` partner tile in the test world; that's a
  meaningful infrastructure jump and the contract being tested is
  primarily the `IDataHandler` interface, not the transceiver
  itself. Defer to a future TASK-13b if a regression class
  surfaces.
- **GUI toggle round-trip via packets** (`PacketMachine` →
  `useNetworkData` → state mutation). The state-mutation side is
  pinned by contracts 7-8; the packet plumbing is libVulpes
  responsibility.
- **`ItemLinker` flow under a real player** — the testClient e2e
  layer would cover this; not adding here. The
  `onLinkStart`/`onLinkComplete` server-side logic is already
  mirrored by the `wireless-pair` probe.

## Implementation plan

### Phase 1 — Probe surface extension (~30 min)

Extend `TestProbeCommand.handlePipe` to surface and mutate the
remaining fields. All extensions use the same reflection idiom
already established at lines 2497-2542.

**Extended `wireless-info`** — add `mode`, `enabled` to the JSON
response. New fields: `"mode":"extract"|"inject"`, `"enabled":true|false`.

**New verb `wireless-set-mode`**:
`/artest pipe wireless-set-mode <dim> <x> <y> <z> <extract|inject>`
Mirrors what the GUI toggle button does at
`TileWirelessTransciever.useNetworkData` lines 196-205: writes
`extractMode`, calls `removeFromAll`, calls `addSource` or
`addSink`. Returns `ok` + new `mode`.

**New verb `wireless-set-enabled`**:
`/artest pipe wireless-set-enabled <dim> <x> <y> <z> <true|false>`
Mirrors `useNetworkData` line 207: sets `enabled` field, calls
`markDirty`. Returns `ok` + new `enabled`.

**New verb `wireless-role-on-network`**:
`/artest pipe wireless-role-on-network <dim> <x> <y> <z>` — reads
back the **observed** role the tile has on the network it belongs
to. Returns `{"ok":true,"isSource":..., "isSink":...}`. Required
to pin contract #7 — the tile's `extractMode` field is one thing,
its actual registration with the network is the contract.

### Phase 2 — Contract tests (~90 min)

Single test class: `WirelessTransceiverContractTest extends
AbstractSharedServerTest`. Position-isolated per the
`AbstractSharedServerTest` contract — each test picks a unique
`BASE_X` offset.

Test method list:

1. `pairingBothUnpairedAssignsSharedFreshId` (contract 1) —
   tighten the existing happy-path: verify both endpoints register
   in `dataNetwork.doesNetworkExist(sharedId)` (i.e. shared id is
   not just non-sentinel, it's an actual live network).
2. `pairingOnlyFirstPairedSpreadsIdToSecond` (contract 2).
3. `pairingOnlySecondPairedSpreadsIdToFirst` (contract 3).
4. `pairingBothPairedDifferentIdsMergesIntoOne` (contract 4) —
   uses two `wireless-pair` calls to set up the precondition, then
   a third to trigger the merge case.
5. `pairingBothPairedSameIdIsNoOp` (contract 5) — pair A↔B, then
   pair A↔B again, assert id unchanged.
6. `nbtRoundTripPreservesModeEnabledNetworkId` (contract 6) — set
   non-default state, restart server, reload, read back via
   `wireless-info`.
7. `extractModeRegistersTileAsNetworkSource` (contract 7a).
8. `injectModeRegistersTileAsNetworkSink` (contract 7b).
9. `disabledTransceiverRefusesDataExtraction` (contract 8a).
10. `disabledTransceiverRefusesDataInjection` (contract 8b).
11. `onChunkUnloadRemovesFromNetwork` (contract 9) — place
    transceiver, force chunk unload, assert role on network is
    null/absent.
12. `onLoadReRegistersConfiguredRole` (contract 10) — round-trip
    via server restart, observe the reloaded tile is in the same
    role on the same network.

~12 tests, ~80 LoC. Reuses `AbstractSharedServerTest` so the cold
server start is amortised across all 12.

### Phase 3 — Stale-claim sweep on the pre-existing @Ignore'd tests (~10 min)

`PipeNetworkSmokeTest.java:185-198` has three `@Ignore`d tests
whose ignore-reason text references the misleading "TODO add back
after fixing the cable network" comment. Update the reasons to
reflect the upstream deprecation reality:

- `dataPipeRoutesPacketsBetweenEndpoints` → reason:
  `"blockDataPipe deprecated upstream (commit 48610953) — replaced by wireless transceiver; see WirelessTransceiverContractTest"`
- `liquidPipeTransfersFluidAcrossChunkBoundary` → same reason
  shape for `blockFluidPipe`.
- `dataBusBridgesAdjacentInventories` → leave as-is (the reason is
  accurate — `TileDataBus` has no placeable block in AR, a
  separate root cause from the pipe deprecation).

Also update the javadoc at `PipeNetworkHandlerDeepTest.java:31-43`
which claims "future end-to-end pipe tests will depend on" — that
future is moot. Reframe as "save-compat invariants the
already-placed pipe networks depend on" since the tile entities
remain registered for legacy worlds.

### Phase 4 — Close-out per SOP (~15 min)

Run [`task-lifecycle.md`](../sops/development/task-lifecycle.md)
closure checklist:

1. TASK-13 header → `✅ Completed 2026-05-23`.
2. README Done table — move row, drop Backlog row.
3. Stale-claim sweep already done in Phase 3 for the tests; do the
   same for `tasks/README.md` Backlog (TASK-13 row out).
4. EOD marker.
5. Commit + push.

## Technical decisions

- **Server-tier only.** No unit-tier additions. The
  `TileWirelessTransciever` constructor instantiates
  `ModuleToggleSwitch` which calls
  `LibVulpes.proxy.getLocalizedString` and references
  `TextureResources` — both classloader-bind to GUI-side code that
  doesn't load on a dedicated-server JVM. Unit-tier is therefore
  not viable for the tile entity itself; existing unit-tier
  coverage in `PipeNetworkHandlerDeepTest` already pins the
  network-handler contracts the transceiver routes through.
- **Reflection in probes, not in tests.** The probe surface owns
  the reflection idiom (already established at lines 2497-2542
  for `wireless-pair`). Tests assert on JSON-shape probe responses
  only.
- **One test class, shared harness.** Per
  `AbstractSharedServerTest` contract: every method
  position-isolated, no cross-method state leak. The 12 tests
  amortise one cold-start (~10-15 s) instead of 12 × cold-start.
- **No production logic changes.** Per the CLAUDE.md rule. If a
  bug surfaces, ledger it under a new Batch in
  `.agent/history/known-bugs-ledger.md` and pin the wrong
  behaviour.

## Dependencies

- Does NOT block any other task.
- Touches `TestProbeCommand` (probe surface extension) — same
  file every TASK-NN extends.

## Estimated effort

- Phase 1 probe surface: 30 min
- Phase 2 contract tests: 90 min
- Phase 3 stale-claim sweep on existing tests: 10 min
- Phase 4 closure: 15 min
- **Total**: ~2.5 h

## Result

Shipped 2026-05-23 in a single session.

**Probe surface extensions** (`TestProbeCommand.handlePipe`):
- Extended `wireless-info` to surface `mode` (extract/inject) and
  `enabled` alongside `networkID`.
- New `wireless-set-mode <dim> <x> <y> <z> <extract|inject>` —
  mirrors the GUI toggle's `removeFromAll` + `addSource`/`addSink`
  side.
- New `wireless-set-enabled <dim> <x> <y> <z> <true|false>` —
  writes `enabled` field + `markDirty`.
- New `wireless-role-on-network <dim> <x> <y> <z>` — reads the
  observed source/sink registration on the live `dataNetwork`.
  Returns `networkExists`, `isSource`, `isSink`.

**Tests shipped** — 11 server-tier pins across 2 classes:
- `WirelessTransceiverContractTest` (10 tests, shared harness):
  4 pairing branches (both-unpaired, only-A, only-B, both-paired-
  different-merge, both-paired-same-no-op), mode toggle source
  registration, inject toggle sink registration, mode flip swaps
  source/sink, enabled round-trip, mode/enabled independence.
- `WirelessTransceiverRestartTest` (1 test, per-method harness):
  NBT round-trip of `mode`/`enabled`/`networkID` AND `onLoad`
  re-registering the saved role on the live network across server
  restart.

**Stale-claim sweep** (Phase 3 of plan):
- `AdvancedRocketry.java:781` — TODO "add back after fixing the
  cable network" replaced with an honest "deprecated upstream in
  commit 48610953; do NOT uncomment without product decision".
- `PipeNetworkSmokeTest.java:185-192` — `@Ignore` reasons for
  `dataPipeRoutesPacketsBetweenEndpoints` /
  `liquidPipeTransfersFluidAcrossChunkBoundary` updated from "fix
  cable network" to "deprecated upstream, see
  WirelessTransceiverContractTest".
- `PipeNetworkHandlerDeepTest.java:31-50` — class javadoc rewritten
  from "future end-to-end pipe tests will depend on this" to
  "save-compat invariants + merge contracts the wireless transceiver
  exercises".
- `PipeNetworkHandlerDeepTest.java:239-251` — stacked obsolete
  "DOCUMENTS KNOWN PRODUCTION BUG" javadoc consolidated into the
  single accurate TASK-12 fix description.

**No production logic changes** beyond the misleading comment fix
at `AdvancedRocketry.java:781`. No bugs surfaced — ledger remains
drained.

**Follow-ups deferred** (not in scope this session):
- Adjacent-tile data-flow contract — would need a placed
  `IDataHandler` partner; deferred to a hypothetical TASK-13b if
  the contract ever regresses.
- `ItemLinker` flow under a real player — testClient e2e layer,
  separate scope.
