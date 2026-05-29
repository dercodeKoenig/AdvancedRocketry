# Marker — 2026-05-25 TASK-19 Phase 1a WIP

**Branch**: `feature/tests`
**Commits pushed**: `44009db9` (F8 watch v11), `c074d494` (Phase 1a WIP)

## Session arc

1. **F8 watch v11 sweep** — 10× `testServer -Pforks=3`, 9/10 PASS.
   F8 (Beacon) 0/10 recurrence. New F9 (`MissionGasCompletion`
   `fluidEntries:0`) at 1/10 — watching. TASK-29 not opened.

2. **TASK-19 Phase 1a opened** — Multiblock powered-cycle trio.
   Per-user scope split:
   - Phase 1a: AR-native planet (this session).
   - Phase 1b: overworld + `allowTerraformNonAR=true` config flip.
   - Phase 2: BHG with generated black hole in test setup.
   - Phase 3: Beacon enable cycle (unchanged).

3. **Phase 1a recon + code** — 2 of 3 tests passing. Happy-path
   blocked on libVulpes battery integration issue (see below).

## Where Phase 1a stopped

`TerraformerPoweredCycleOnArPlanetTest`:

| Test | Status | Notes |
|---|---|---|
| `nativePlanetTerraformerWithoutFuelDoesNotStep` | ✅ PASS | OOF gate works as designed |
| `nativePlanetTerraformerWithoutPowerDoesNotStep` | ✅ PASS | No power → no progress |
| `nativePlanetTerraformerWithFuelAndPowerStepsDensity` | ⏸ `@Ignore` | Diagnostic showed `progress:0` after 24000 force-ticks despite `isComplete:true, isRunning:true, getMachineEnabled:true` |

## The blocker

`onRunningPoweredTick` never fires because libVulpes'
`batteries.getUniversalEnergyStored()` reads 0 even after
`artest energy inject` reports `accepted:>0` on the 'P' plug.

**Root cause (suspected)**: the fixture places `blockCreativeInputPlug`
(mapping index 0 for 'P'). The plug's `TileCreativePowerInput`
implements `IUniversalEnergy` (libVulpes) AND exposes Forge
`IEnergyStorage` capability. The `artest energy inject` probe writes
via the Forge capability; libVulpes' controller-side
`batteries.addBattery((IUniversalEnergy) tile)` expects to aggregate
`IUniversalEnergy.getUniversalEnergyStored()` which may not bridge
to the Forge capability's stored value on this tile class.

Open question: maybe `integrateTile` ISN'T running for the 'P'
position at all (controller doesn't aggregate). Test
`/artest energy stored <P-pos>` in next session to disambiguate:

- If stored > 0 after inject → integration is the gap.
- If stored == 0 → injection isn't even landing (creative plug
  refuses external energy because `receiveEnergy() return 0`).

## Next-session work

1. Run `artest energy stored` at the 'P' plug position right after
   `artest energy inject` to confirm whether injection actually
   stuck. Hypothesis: it didn't — see `TileCreativePowerInput.java:75`:
   ```java
   public int receiveEnergy(int amt, boolean simulate) { return 0; }
   ```

2. If injection isn't landing: two options.
   - **Option A**: override fixture placement to use
     `blockForgeInputPlug` (mapping index 1) instead of creative.
     Needs a new `wildcardConfig`-like hatch override OR a
     terraformer-specific fixture variant.
   - **Option B**: add new probe verb `/artest machine
     inject-controller-energy <pos> <amount>` that reflects into
     the controller's `batteries` field via
     `batteries.acceptEnergy(amount, false)` directly. Faster, but
     skirts the public capability surface — pure test-helper.

3. Once happy-path passes: lift `@Ignore`, re-run, confirm all 3
   pass.

4. **Phase 1b** (overworld + config flip): also needs a new probe
   verb `/artest config set <category> <key> <value>` (whitelisted
   to terraformer keys) to flip `allowTerraformNonAR=true` at
   runtime. The fixture build pattern is identical to Phase 1a;
   only the dim differs.

5. **Phase 2 (BHG)** — separate recon needed for the
   `isAroundBlackHole()` precondition. Likely also a probe-verb
   addition OR per-planet flag toggle.

6. **Phase 3 (Beacon)** — cleanest of the four, can be done
   in parallel with Phase 1 fixes.

## Open follow-ups (carried over)

- **F8** Beacon `try-complete attempted:false` — 1/5 toward Obsolete.
- **F9** `MissionGasCompletion.fluidEntries:0` — 1/5 toward Obsolete.
- **TASK-29 not opened** — both shapes single-occurrence; promote
  on 2nd sighting per SOP.

## Pyramid

237 / 80 / **340** / 41 = **698**. +1 from TerraformerPoweredCycle
(2 active + 1 `@Ignore`d test methods count toward the file but
the test-counter convention treats `@Ignore`'d as the file's
contract).

**Regen counter on next session** per
`.agent/sops/development/task-lifecycle.md` step 2.5 — the +1
above is my estimate; the script may give a different exact
breakdown.

## Bug ledger

Drained. No live bugs.

## Resumption

1. `./gradlew testServer --tests "*TerraformerPoweredCycleOnArPlanetTest*" -Pforks=1`
   reproduces the 2/3 pass + 1 SKIPPED state in ~75 s.
2. Lift the `@Ignore` to put the happy-path back in red, then
   diagnose per the "Next-session work" steps above.
