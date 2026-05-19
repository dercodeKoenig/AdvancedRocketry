# Context Marker: TASK-04 multiblock fixtures — BHG + Beacon shipped

**Created**: 2026-05-19 20:30 local
**Branch**: `feature/tests`
**Session type**: continuation of autonomous batch (multiblock-fixture
unblock specifically requested by user)

---

## Foundation unlocked

`libVulpes` source cloned at `/workspace/libVulpes` — char-mapping for
the production `Object[][][] structure` arrays is now visible without
reverse-engineering the deobf JAR. Key documentation in
`TileMultiBlock.java`:

- `'c'` — controller (skip validation; this is the block from which
  the structure is anchored)
- `'*'` — per-multiblock wildcard via `getAllowableWildCardBlocks()`
- `'I'` / `'O'` — item input / output hatches (libvulpes:hatch
  meta 0|8 / meta 1|9)
- `'P'` / `'p'` — power input / output plugs (forge / IC2 / GT
  variants registered in `LibVulpes.java` lines 370-410)
- `'L'` / `'l'` — liquid input / output hatches
- `'D'` — data hatch
- Block reference — exact block, meta wildcard
- `Blocks.AIR` — must be `world.isAirBlock(globalPos)`
- `null` — no constraint

Position formula (from `TileMultiBlock.completeStructure` line 336-338):
```
globalX = pos.X + (x - off.x) * front.frontZ - (z - off.z) * front.frontX
globalY = pos.Y - y + off.y
globalZ = pos.Z - (x - off.x) * front.frontX - (z - off.z) * front.frontZ
```

`off = getControllerOffset(structure)` = the `(x, y, z)` of the `'c'`
character in the structure array.

For a NORTH-facing controller (frontX=0, frontZ=-1) this simplifies
to:
- `globalX = pos.X - (x - off.x)`
- `globalY = pos.Y - y + off.y`
- `globalZ = pos.Z + (z - off.z)`

---

## What landed this session

| Multiblock | Fixture probe | Tests | Status |
|---|---|---|---|
| BlackHoleGenerator | `/artest fixture multiblock blackhole-gen` | 4 | ✅ |
| Beacon | `/artest fixture multiblock beacon` | 3 | ✅ |

**Pyramid delta**: +7 testServer tests.

### BHG details

- 5×3×3 structure, 10 non-null cells (the load-bearing surprise: layer
  y=2 has TWO advStructure blocks at z=0 AND z=1, not one — the
  first iteration of the fixture missed the z=0 cell and the validator
  refused).
- 4 tests: validates / invalidates on column-break / power-output-plug
  exposes IEnergyStorage capacity / formed-BHG-in-overworld stays idle
  (counter-test for the `isAroundBlackHole` production guard).

### Beacon details

- 5×3×3 structure, 9 non-air cells. Footprint is mostly Blocks.AIR
  → fixture pre-clears the whole bounding box to air before placing
  the 9 non-air blocks.
- 3 tests: validates / invalidates on redstone-tip-removal / invalidates
  on shaft-break.

---

## Commits on `feature/tests`

```
9644 6e9  test: TASK-04 — BHG behavioural depth + Beacon fixture/validation
57d3 58d  test: TASK-04 — fixture-builder probe for BHG + multiblock validation pin
```

(Plus the earlier TASK-04 doc note about libVulpes registry names —
that's `2aabbed` from the prior autonomous session.)

---

## Recipe for adding the next multiblock

1. Read `TileX.structure` from production (typically in
   `src/main/java/.../tile/multiblock/...`).
2. Find the controller offset (`'c'` cell coordinates).
3. Check `TileX.getAllowableWildCardBlocks` override (if any) to
   determine what `'*'` accepts for that specific multiblock.
4. Add `handleFixtureX` method in `TestProbeCommand` modelled on
   `handleFixtureBeacon` (small structure) or `handleFixtureBlackHoleGenerator`
   (medium structure with hatches). Add the dispatch in `handleFixture`.
5. For Blocks.AIR cells, pre-clear the bounding box to air (Beacon
   pattern).
6. Test class `XMultiblockTest extends AbstractSharedServerTest` with
   3-4 methods: validates, invalidates-on-break-1, invalidates-on-
   break-2, optionally a behavioural depth test like
   `isAroundBlackHole_documentsContract` for guards.

The Beacon implementation is ~80 LOC fixture + ~100 LOC test. Each
new multiblock should land in ~1 hour with this template.

---

## Realistic per-multiblock sizing for next sessions

| Tile | Layer × Z × X | Difficulty |
|---|---|---|
| TileBeacon | 5×3×3 | ✅ done |
| TileBlackHoleGenerator | 5×3×3 | ✅ done |
| TileRailgun | 9×9×9 (sparse) | small/medium — mostly null |
| TileObservatory | 5×5×5 | medium — ~30 non-null + lens Block[] |
| TileMicrowaveReciever | 1×5×5 | already covered as smoke |
| TileWarpCore | ? | unknown — needs survey |
| TileAreaGravityController | ? | unknown |
| TileAstrobodyDataProcessor | ? | unknown |
| TileAtmosphereTerraformer | 17×17×?? | massive — own session |
| TileOrbitalLaserDrill | 9×11×11 (~500 cells) | massive — own session |

Recommend tackling Railgun + Observatory next (~2-3 h combined),
defer Terraformer + OrbitalLaserDrill as standalone sessions.

---

## Open followups

1. **Multiblock fixtures continuation** — Railgun + Observatory
   (+ optional Warp/AreaGravity/AstrobodyData if simple).
2. **TASK-06 mission probes** — still blocked behind ~2 h probe-
   builder work (same shape as the multiblock fixture problem, but
   for missions). The libVulpes clone doesn't help directly here.
3. **TASK-10b** — testClient e2e player-event plan still pre-doc.

---

## Restore instructions

```
Read .agent/.context-markers/2026-05-19-2030_multiblock-fixtures-bhg-beacon.md
Read .agent/tasks/TASK-04-multiblock-machine-depth.md
Read src/main/java/zmaster587/advancedRocketry/command/test/TestProbeCommand.java  # handleFixtureBlackHoleGenerator + handleFixtureBeacon
Read /workspace/libVulpes/src/main/java/zmaster587/libVulpes/tile/multiblock/TileMultiBlock.java  # completeStructure + charMapping
```
