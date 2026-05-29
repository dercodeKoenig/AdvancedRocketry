# Context marker — pre-compact 2026-05-27 00:30

**Slug**: before-compact-2026-05-27-0030
**Branch**: `feature/tests`
**Trigger**: `/navigator:nav-compact` after long autonomous session
across two days (2026-05-26 → 2026-05-27).

## Session arc — what got done

Six commits, all pushed. One commit in sibling
`/workspace/ForgeTestFramework` repo (master), five in
AdvancedRocketry `feature/tests`.

### Commit chain (AR feature/tests)

1. `7d2f1991` **TASK-36b** — service-station broken-part scan
   contract (3 server tests). New `/artest infra inject-broken-part`
   + `service-relink` probes. Insight: `TileBrokenPart` instances
   pre-exist in `rocket.storage.tileEntities` (every IBrokenPartBlock
   returns a TileBrokenPart from createTileEntity, copied into
   StorageChunk by cutWorldBB on assemble) — probe just calls
   `setStage(stage)` on first stage==0 entry, no allocation needed.

2. `3b14c96d` **TASK-33 + TASK-36a** batch — satellite builder
   press-build path (2 server) + terraforming terminal chip-
   recognition (3 server). New probes: `satellite-builder
   press-build`, `terraforming terminal-info`, `terminal-load-chip`.
   Discovery: `weatherController` satellite overrides
   `isAcceptableControllerItemStack` to reject default chip — pinned
   as negative branch in test 2.

3. `6c055940` **TASK-35** — `/ar fetch` self-fetch + unknown-name
   pins (2 testClient). Reframed Phase 0 plan: no NetworkManager
   stub real-EntityPlayerMP probe needed. Bot username discovered
   via `artest player health`. Self-fetch covers full
   resolve→transfer→setPosition path without a second player.

4. `b8d13958` **TASK-36b ext + multi-client moderator-fetch** (2
   server + 1 testClient). TASK-36b ext: scanForAssemblers picks up
   nearby PrecisionAssembler + no-progress-without-assembler. New
   `service-scan-assemblers` reflection probe (bypasses
   `worldTime % 20 == 0` gate that force-tick can't satisfy).
   Multi-client: `WorldCommandFetchModeratorTest` — bot1 (op)
   fetches bot2 cross-position. Required ForgeTestFramework
   modification (separate commit `2e16dea` pushed to framework
   master).

5. `c3cf8cc7` **TASK-36b deep** — full repair cycle with FORMED
   PrecisionAssembler multiblock (1 server). Reuses TASK-26
   `/artest fixture machine precision-assembler` (was already in
   place — `MachineRecipeEndToEndKit`'s "wildcard machines out of
   scope" caveat misled prior deferral; it referred to the kit's
   recipe-helper, not the underlying fixture probe). Phase 1 pins
   consumePartToRepair, Phase 2 pins processAssemblerResult. New
   `service-perform-function` probe; `service-state` extended with
   `partsProcessingCount`.

### Framework change (ForgeTestFramework master)

`2e16dea` **feat: multi-client support in RealClientHarness**:
- New `start(server, username)` overload — was hardcoded
  `CLIENT_USERNAME = "ForgeTestClient"`.
- Moved `--username` + `--uuid` OUT of legacyArgs block. AR's test
  setup uses FG6 `legacydev.MainClient` (legacyArgs=false) which
  was skipping the username arg → FG6's `MainClient.getDefaultArguments`
  seeded username=null → random `Player###` names broke PlayerList
  name resolution.

**Important**: testClient now requires `-PuseLocalFramework=true`
until framework is published to mavenLocal. User explicitly
declined the publishToMavenLocal step ("не надо").

## Pyramid

**825 → 839** across the session. Final:
- testUnit **288**
- testIntegration 81
- testServer **410** (+11)
- testClient **60** (+3)

Distribution by commit:
- `7d2f1991`: +3 server (TASK-36b)
- `3b14c96d`: +5 server (TASK-33 + TASK-36a)
- `6c055940`: +2 client (TASK-35)
- `b8d13958`: +2 server + 1 client (TASK-36b ext + moderator-fetch)
- `c3cf8cc7`: +1 server (TASK-36b deep)

## Bug ledger updates

No new live bugs this session. Count stays at **3** (from prior
sessions).

## Flake watch updates

**+1 shape #6** — `InventoryBypassRedirectE2ETest.mixinRedirectKeepsContainerOpenAcrossDistance`
right-click→GUI race under client-harness GL/CPU contention.
Pre-existing (verified by reverting framework + removing my
testClient test — same failure reproduces). Matches testClient
javadoc warning about "right-click → openGui → displayGuiScreen
round-trip unreliable". First sighting; need 2nd to promote.

## Backlog status — drained again

**Done table additions (5 tasks)**: TASK-36b, TASK-33, TASK-36a,
TASK-35, TASK-36b ext, TASK-35 ext (moderator-fetch), TASK-36b
deep.

**Backlog table** (2 entries, both watch-only):
- TASK-15 visual regression — 👁 Watching
- TASK-16 flake watch — 🟡 journal (now contains shape #6)

No ship-able formal backlog work remaining.

## Probe additions this session (recap)

To `TestProbeCommand.java`:
- `infra inject-broken-part <entityId> <stage>`
- `infra service-relink <dim> <x> <y> <z>`
- `infra service-scan-assemblers <dim> <x> <y> <z>`
- `infra service-perform-function <dim> <x> <y> <z>`
- `infra service-state` extended with `partsProcessingCount`
- `satellite-builder press-build <dim> <x> <y> <z> <typeId>`
- `terraforming terminal-info <dim> <x> <y> <z>`
- `terraforming terminal-load-chip <dim> <x> <y> <z> <satId>`
- `player exec-as-named <name> <cmd>`
- `player position-of <name>`
- `player op-named <name>`

## Build flag — REMEMBER

`testClient` now needs `-PuseLocalFramework=true` until
`ForgeTestFramework` is published to mavenLocal. The
`WorldCommandFetchModeratorTest` uses
`RealClientHarness.start(server, username)` which doesn't exist
in the maven-published jar yet. Without the flag testClient
compile fails on this test class.

Workaround alternatives:
- Pass `-PuseLocalFramework=true` always (current state).
- `cd /workspace/ForgeTestFramework && ./gradlew publishToMavenLocal`
  → makes the flag unnecessary on this machine (CI still needs
  framework artifact published somewhere accessible). User
  declined this step.

## Hook noise

`PostToolUse:Bash` hook spam on every Bash call —
`nav_commit_reminder.py` missing from
`/root/.claude/plugins/marketplaces/navigator-marketplace/hooks/`.
Harmless, ignored throughout session.

## What's next (when restored)

Backlog is genuinely empty for ship-able work. Options at restore:
- Wait for new feature/bug request from user.
- Fresh tier audit (low ROI — last audit only 2 days old + ~15 new
  tests since).
- Multi-client framework extensions (no real use case yet — single
  moderator-fetch test exercises the path).
- Multiblock fixture extensions to other wildcard machines (most
  already covered by TASK-26).

User most recently asked "что дальше по бэклогу?" — answer was
"нет formal ship-able работы". User then requested
TASK-36b deep, which was shipped (commit `c3cf8cc7`).
