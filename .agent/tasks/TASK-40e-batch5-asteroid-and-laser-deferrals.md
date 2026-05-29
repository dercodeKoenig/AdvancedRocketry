# TASK-40e — Batch 5: Gap N deferral + Gap K deferral

**Status: ❌ Deferred 2026-05-29 (both gaps need standalone batches)**

## Ticket

- Source: 2026-05-27 coverage audit Gap N (WorldProviderAsteroid +
  ChunkProviderAsteroids) + Gap K (ItemBasicLaserGun firing) — both
  remained un-shipped after the TASK-40 / 40b / 40c / 40d sweeps.
- Status: ❌ Deferred to a possible TASK-41.

## Why deferred (not dropped)

Both contracts are real and player-visible per SOP litmus. The
**reason for deferral is fixture cost** — each gap exceeds the
budget for a tail-end batch but is a self-contained piece of work
for a follow-up TASK.

### Gap N — Asteroid worldgen

**Contract** (per audit): "Loading the Asteroid worldprovider
dimension and walking N chunks produces > K asteroid stems."

**Fixture cost**:

- Asteroid dim must be registered + loaded (the
  `DimensionManager.AsteroidDimensionType` is registered via
  `registerDimension` from inside satellite-construction flows; no
  ready-to-load dim ID exists at server boot).
- Need to walk N chunks (e.g. 16×16 region), invoking
  `chunkProvider.generateChunk(x, z)` per cell.
- Need to count asteroid `fillblock` (the configured material) in
  each chunk's primer.
- Counter must compare against a config-derived "expected at least K"
  baseline.

**Estimate**: ~4 h with probe + fixture authoring. Audit estimate
matches at ~3 h.

**Defer rationale**: independent of all other Batch 1–4 gaps; not a
blocker per the 2026-05-29 delta-audit's rewrite-safety classification
(asteroid worldgen belongs to ⚠ "pre-rewrite pin recommended" cluster,
not the empty ❌ "rewrite-blocked" cluster).

### Gap K — ItemBasicLaserGun firing

**Contract** (per Phase-0-reshaped audit framing): "Right-clicking
with an ItemBasicLaserGun on an entity in ray-trace range damages
the entity" (the audit's "spawn EntityLaserNode" framing was
client-side visual; the server-side contract is the damage tick).

**Phase-0 verified wired**: `AdvancedRocketry.itemBasicLaserGun` is
registered (`setRegistryName("basicLaserGun")`), set into the AR
creative tab, and has a client-side model registered. Recipe path
not verified but the audit's drop-trigger was "no creative tab
entry"; that doesn't fire.

**Fixture cost**:

- testClient harness (needed for real EntityPlayer / item-use
  interaction; bot tooling exists per TASK-24).
- New probe: `player laser-fire-at <targetEntityId>` — equips the
  laser gun, calls `onItemRightClick`, returns hit result.
- Bot rotation must face target before fire — `try-hovercraft` probe
  has a yaw/pitch pattern that can be lifted.
- Spawn target entity (zombie / cow) via existing `entity spawn`.
- Read target HP via existing `entity info` or extend.

**Estimate**: ~3 h. Audit estimated ~4 h.

**Defer rationale**: same as Gap N — independent of the Batch 1–4
sweep and not a blocker. Belongs to the same ⚠ cluster as
ForceFieldProjector before TASK-40d shipped it.

## What ships in Batch 5 — nothing

Batch 5 is empty by design: closing the deferral docs is the only
deliverable.

## TASK-41 candidates summary

Promote a TASK-41 if depth-coverage on these subsystems becomes
priority. Candidates:

| Gap | Subsystem | Est. effort |
|---|---|---|
| K | ItemBasicLaserGun firing (testClient) | ~3 h |
| N | Asteroid worldgen | ~4 h |
| B (TASK-40c) | Orbital Laser Drill mode dispatch | ~5 h |
| S (TASK-40c) | AreaBlob max-radius enforcement | ~4 h |
| F.3 (TASK-40c) | AtmosphereDetector with custom dim setup | ~3 h |
| F.4 un-ignore (TASK-40c) | TilePump — source-water probe | ~0.5 h |
| C un-ignore (TASK-40b) | AreaGravityController via EntityItem | ~2 h |

Total TASK-41 backlog if all promoted: ~22 h. Per 2026-05-29 delta
audit, none are blockers.

## Dependencies

None.
