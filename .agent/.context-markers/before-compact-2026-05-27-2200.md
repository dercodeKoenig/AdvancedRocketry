# Context marker — pre-compact 2026-05-27 22:00

**Slug**: before-compact-2026-05-27-2200
**Branch**: `feature/tests`
**Trigger**: `/navigator:nav-compact` after full audit + batch
TASK-37/38/39 shipped.

## Session arc — what got done

This session ran a **full coverage audit** (per user request)
followed by implementation of the first three new gaps it
surfaced. Two distinct outputs:

### 1. Audit doc — `.agent/audits/2026-05-27-full-coverage-audit.md`

Comprehensive coverage matrix of ~559 production classes vs ~840
tests, organised by 14 subsystems (rocket flight, multiblocks,
satellites, atmosphere, items, station, missions, cables,
worldcommand, dimension, persistence, mixin, decoration). Per
SOP litmus — contracts not impl pins.

**Two-pass methodology**:
- First pass: surfaced 15 contract-shaped gaps (A-N + O cables)
- User dropped O (cables out of scope)
- Second pass (3 parallel Explore agents, deep-grep of every
  Tile/Block/Item/Entity/Satellite/Mission/Packet/Mixin/Capability
  /Atmosphere/Recipe/Worldgen/GUI/Event-handler/Command class):
  - Surfaced ~80 candidates
  - ~70 rejected after SOP litmus (catalogued in §8 so future
    agents don't re-propose them — includes ItemJetpack /
    PressureTank / Atmosphere subtypes / Mixins / Capabilities /
    Enchantment / packets / decoration blocks / fuel-tank variants
    / per-biome pins / per-MapGen pins / Recipe class unit-tests /
    GUI Module classes — all either already COVERED via grep
    verification or IMPL-only per SOP)
  - 4 new contracts survived: P (nuclear engines), Q (BlockMiningDrill),
    R (TileSatelliteTerminal), S (AreaBlob max-radius)
- Final actionable: **17 gaps total** (A-N from first pass + P/Q/R/S
  from second), ~50 h estimated

User reaction to second pass: "ты прям уверен?" → triggered the
verification. Confirmation came back affirmative with the §8
rejection catalogue.

### 2. Batch P+Q+R shipped — 7 server tests, all green

User: "давай-ка делай P, Q и R одним батчем"

**Commits NOT yet made** — diff sits on working tree. User
explicitly never authorised commits. CLAUDE.md rule: no autonomous
commits, no production logic changes.

**Files touched**:

Production probe surface (test infra, not gameplay logic):
- `src/main/java/zmaster587/advancedRocketry/command/test/TestProbeCommand.java`:
  - New dispatch `case "satellite-terminal":` (line ~108-110)
  - Extended `rocket info` with `drillingPower` field (~line 815)
  - 3 new rocket-fixture variants: `with-nuclear-stack`,
    `with-nuclear-misplaced`, `with-mining-drill` (~line 5760-5910)
  - New handler `handleSatelliteTerminal` with subcommands
    `info`/`load-chip`/`press-erase` (~3000 lines after the
    satellite-builder handler)
  - `satellite-terminal` added to tab-completion list

New test files (server tier):
- `src/test/java/.../server/NuclearEngineRocketAssemblyTest.java` (2)
- `src/test/java/.../server/RocketAssemblerMiningDrillStatTest.java` (1)
- `src/test/java/.../server/SatelliteTerminalChipRecognitionTest.java` (4)

New task docs:
- `.agent/tasks/TASK-37-nuclear-engine-rocket-assembly.md`
- `.agent/tasks/TASK-38-mining-drill-rocket-assembly.md`
- `.agent/tasks/TASK-39-satellite-terminal-chip-recognition.md`

Updated docs:
- `.agent/tasks/README.md` — Done table + counter (839 → 843)
- `.agent/audits/2026-05-27-full-coverage-audit.md` — Gap P/Q/R
  marked ✅ Shipped

## Pyramid

**839 → 843** total. testUnit 288 / testIntegration 81 /
testServer **414** (+4 observed; +7 added but pre-counter
appears to have been off-by-3 — see counter-regen comment in
README). testClient 60.

## Phase-0 reframe

**Gap Q** was originally framed as "BlockMiningDrill placeable
single-block drill" but Phase-0 read showed it has no
TileEntity / no `update()`. It's a cargo-component block consumed
by rocket assembly via IMiningDrill aggregation. Contract
reframed accordingly in TASK-38 doc.

## Debug arc

First testServer run of Nuclear batch: 2 fails out of 7.
- `nuclearCoreAboveMotorContributesNuclearThrust` → NOFUEL
  (mixing nuclear motors with monopropellant fuel tanks fails
  COMBINEDTHRUST gate — the simple fixture's BlockFuelTank is
  monopropellant, not nuclear).
- `misplacedNuclearCoreContributesZeroThrust` → NOENGINES
  (correct contract — misplaced core → reactor=0 → thrust=0 →
  scan rejects).

Fix: routed nuclear variants' fuel tanks through
`BlockNuclearFuelTank` (advancedrocketry:nuclearfueltank).
Rewrote misplaced test to pin the scan-status:NOENGINES (player-
visible chat error) instead of post-assembled rocket info.

All 7 green after fix. Regression on
RocketAssemblySmokeTest/UvAssembler*/TerraformingTerminal — all
green.

## Build flag note

testClient + compileTestJava require `-PuseLocalFramework=true`
until the ForgeTestFramework `RealClientHarness.start(server,
username)` overload is published to mavenLocal. (Carried over
from previous session's `2e16dea` framework commit.)

## What's next

**Backlog from audit (14 gaps left, ~40 h)**:
- Suggested next batch: (1)+(2)+(4) = rocket loader item active
  transfer + railgun firing + planet analyser scan output. Probe
  additions overlap (force-tick at the multiblock + storage
  fixtures).
- testClient batch: (7)+(10) = GasChargePad pressure tank fill +
  AreaGravityController player effect.
- Phase-0-heavy batch: (8)+(9)+(11)+(12)+(13)+(15)+(16) =
  TilePump / ForceFieldProjector / ItemUpgrade / CO2 Scrubber /
  AtmosphereDetector / SatelliteHatch / BlockIntake. Several may
  collapse to IMPL-only after Phase 0.
- New audit gap S (AreaBlob max-radius enforcement, ~4h) not yet
  picked up.

**Watch-list (Backlog table, not active TASK)**:
- (17) Asteroid dimension density
- TASK-15 visual regression
- TASK-16 flake journal

## Open commits to make

User has NOT authorised a commit. Diff includes:
- TestProbeCommand.java (probe surface additions)
- 3 new test files
- 3 new TASK files
- README + audit doc updates

Suggested commit message (when user authorises):

```
test: TASK-37/38/39 batch — nuclear engine + mining drill + satellite terminal

- 7 server tests across 3 classes pinning audit Gaps P/Q/R
- 3 new /artest fixture rocket variants (nuclear-stack /
  nuclear-misplaced / mining-drill)
- rocket info exposes drillingPower
- new /artest satellite-terminal subcommand group
  (info / load-chip / press-erase)
- pyramid 839 → 843
```

## Bug ledger

No new live bugs surfaced this session. Count stays at **3**.

## Flake watch

No new shapes observed. Shape #6
(`InventoryBypassRedirectE2ETest.mixinRedirectKeepsContainerOpenAcrossDistance`)
still at 1 sighting (from previous session).
