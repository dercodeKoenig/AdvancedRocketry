# AdvancedRocketry - Claude Code Configuration

## Context

Fork of Advanced Rocketry — a Minecraft 1.12.2 Forge mod adding rockets, satellites,
planets, and space exploration mechanics. Reworked/maintained for the modern
"Towards Rocket Science" modpack.

**Tech Stack**: Java 8, Minecraft Forge 1.12.2, Kotlin DSL Gradle (FancyGradle), libVulpes,
JEI integration, MixinBooter (`AdvancedRocketryPlugin` registers
`mixins.advancedrocketry.json`; legacy ASM `IClassTransformer` coremod removed
in TASK-08-mixin)

**Core Principle**: Maintain compatibility with vanilla 1.12.2 Forge ecosystem; favor
small targeted bugfixes over large refactors; preserve existing public API/registry IDs
to avoid breaking saves and dependent mods.

**Last Updated**: 2026-05-21
**Navigator Version**: 6.15.4

---

## Language

- **Always respond to the user in Russian** regardless of the conversation language. Code, identifiers, commit messages, and inline code comments stay in English. User-facing prose (chat replies, EOD markers, summaries) is in Russian.

---

## Navigator Quick Start

**Every session begins with**:
```
"Start my Navigator session"
```

This loads `.agent/DEVELOPMENT-README.md` (your project navigator) which provides:
- Documentation index and "when to read what" guide
- Current task context from PM tool (if configured)
- Quick start guides and integration status

**Core workflow**:
1. **Start session** → Loads navigator automatically
2. **Load task docs** → Only what's needed for current work
3. **Implement** → Follow project patterns below
4. **Document** → "Archive TASK-XX documentation" when complete
5. **Compact** → "Clear context and preserve markers" after isolated tasks

**Natural language commands**:
- "Start my Navigator session" (begin work)
- "Archive TASK-XX documentation" (after completion)
- "Create an SOP for debugging [issue]" (document solution)
- "Clear context and preserve markers" (after sub-tasks)

---

## Testing — REQUIRED reading before writing or auditing tests

Before authoring or modifying ANY test in this repo, re-read
[`.agent/sops/development/testing-principles.md`](./.agent/sops/development/testing-principles.md).
Re-read every session — even if the file feels familiar — because
the agent (and humans) drift back to over-tight impl-detail pins
under the guise of "depth audits".

**The core rule**: tests verify *contracts* (player-visible
behaviour, public API surface, registry / NBT / wire formats),
NOT implementation details (exact RF costs, exact loop bounds,
internal field shapes, specific code branches).

**Litmus before every assertion**: complete the sentence
"this test fails if production breaks the contract that __."
If the blank reads like an impl detail, redesign.

**During depth audits**: count contract-coverage, not pin-count.
Do NOT propose tightenings whose only purpose is to pin a magic
number, a loop bound, an internal data-structure choice, or an
internal helper — these are anti-patterns called out explicitly
in the SOP.

---

## Flake diagnosis — REQUIRED reading before tuning retry budgets

When a test fails intermittently (or your 10× rerun comes back red),
re-read [`.agent/sops/development/flake-diagnosis.md`](./.agent/sops/development/flake-diagnosis.md)
before reaching for the retry-budget knob. The SOP distinguishes
three failure modes — real race, self-introduced regression,
test-design bug — and gives the diagnosis checklist. Skipping it
costs 150-minute reruns chasing the wrong variable.

**The core rule**: failure DISTRIBUTION across runs tells you which
mode. Same N tests every run → regression. Sparse non-deterministic
set → race. Alternating outputs on same test → test-design.

**Cache-bust sanity**: every 10×-rerun loop MUST delete
`build/{reports,test-results,tmp}/testServer` between iterations
AND grep per-run `PASSED` count, or you'll discover ten "PASS" runs
where only run 1 actually executed.

This rule overrides the agent's instinct to "make assertions
tighter". Tighter is not always better.

---

## Bug tracking — every discovered production bug must be logged

When you uncover a real production bug during any activity (test
authoring, depth audits, probe work, code review, debugging an
unrelated issue), **log it immediately** in the bug ledger at
[`.agent/tasks/README.md`](./.agent/tasks/README.md) under the
"Notes on `_documentsKnownBug`" section, before moving on.

A ledger entry is a one-paragraph record:

- File + line where the bug lives.
- One-sentence description of the wrong behaviour.
- One-sentence description of the player-visible / API-visible
  consequence — if the consequence is "nothing observable" then it
  is not a bug worth logging, it is impl trivia.
- Whether the bug is pinned by a `_documentsKnownBug` test (and
  where), or merely ledgered (no test yet).

Optional: a `_documentsKnownBug` test that pins the **current
(wrong) behaviour** as expected — so the day someone fixes the bug
in production, the test fails and forces an update. Write one when
the bug sits on a code path the test suite already exercises;
defer when adding a test costs more than ledgering does.

**Reason**: bugs surface most often as a side-effect of unrelated
work. If they aren't recorded the moment they are found, they
evaporate from working memory and re-surface months later as
"mystery" regressions. The ledger is the single point of truth so
a future bug-fix ticket can sweep them in batch.

**Per-session scope**: this is a logging rule, not a fix-now rule.
The "no production logic changes" rule from each TASK still applies
— record the bug, do not silently fix it in scope.

Update the running counter at the top of `.agent/tasks/README.md`
when you add or remove a ledger entry so it stays accurate.

---

## Project-Specific Code Standards

### General Standards
- **Architecture**: Mirror vanilla Forge 1.12.2 patterns; KISS over abstraction
- **Java**: Source/target 1.8 — no Java 9+ APIs (`var`, records, switch expressions, etc.)
- **Mappings**: MCP snapshot `20171003-1.12` — use SRG/MCP names consistently
- **Encoding**: UTF-8 for source and javadoc
- **No Kotlin in mod source** — Kotlin is build-script only (`build.gradle.kts`)

### Forge Patterns
- Register blocks/items/entities via Forge `@Mod.EventBusSubscriber` registry events
- Tile entities: keep NBT save/load symmetric, version legacy NBT carefully (saves matter)
- Bytecode patching uses Mixin (MixinBooter); mixins live in
  `zmaster587.advancedRocketry.mixin` and are registered via
  `mixins.advancedrocketry.json`. The `AdvancedRocketryPlugin` coremod entry
  point only bootstraps Mixin — no `IClassTransformer` left.
- Network packets: use `IMessage`/`IMessageHandler` SimpleImpl pattern
- Side checks: `@SideOnly(Side.CLIENT)` for rendering / GUI / sound code only
- Don't break public APIs in `zmaster587.advancedRocketry.api.*` without strong reason

### Build / Run
- `./gradlew build` — produces main + deobf jar
- `./gradlew runClient` / `runServer` — launch test environment (working dirs `run/`, `run-server/`)
- JEI is required at runtime (compileOnly + implementation)
- libVulpes can be a composite build via `settings.gradle.kts` if `libVulpes/` exists locally

---

## Forbidden Actions

### Navigator Violations (HIGHEST PRIORITY)
- ❌ NEVER load all `.agent/` docs at once (defeats token optimization)
- ❌ NEVER skip reading DEVELOPMENT-README.md navigator
- ❌ NEVER skip documentation after non-trivial features

### General Violations
- ❌ Never run `git commit` autonomously (per global rules — always show diff first)
- ❌ No Claude Code mentions in commits/code
- ❌ No `gradle.properties` modifications without approval (version pins)
- ❌ Never commit secrets/API keys (`thecursedkey`, etc.)
- ❌ Don't bump `mcVersion`, `forgeVersion`, or mappings snapshot without explicit ask
- ❌ Don't introduce Java 9+ language features
- ❌ Don't change registry names of existing blocks/items (breaks saves)

---

## Documentation Structure

```
.agent/
├── DEVELOPMENT-README.md      # Navigator (always load first)
├── tasks/                     # Implementation plans
├── system/                    # Architecture docs
└── sops/                      # Standard Operating Procedures
    ├── integrations/
    ├── debugging/
    ├── development/
    └── deployment/
```

**Token-efficient loading**:
- Navigator: ~2k tokens (always)
- Current task: ~3k tokens (as needed)
- System docs: ~5k tokens (when relevant)
- SOPs: ~2k tokens (if required)
- **Total**: ~12k vs ~150k loading everything

---

## Project Management Integration

**Configured Tool**: None (issues tracked in upstream GitHub repo when applicable)

**Workflow**:
1. Identify bug/feature (commit history, user report, modpack feedback)
2. Generate implementation plan → `.agent/tasks/`
3. Implement on a topic branch / worktree
4. Update system docs if architecture changes
5. Show diff for review → human runs commit
6. Update changelog if releasing

---

## Configuration

Navigator config in `.agent/.nav-config.json`:

```json
{
  "version": "5.5.0",
  "project_management": "none",
  "task_prefix": "TASK",
  "team_chat": "none",
  "auto_load_navigator": true,
  "compact_strategy": "conservative"
}
```

---

## Commit Guidelines

- **Format**: short imperative summary matching existing history (e.g. `fix crash when weight config is wrong`)
- Reference upstream issue/PR if applicable
- No Claude Code mentions in commits
- Concise and descriptive
- **Never auto-commit** — always show the diff and wait for explicit approval

### Commit message prompt

When the user asks for a commit message, generate it with this template:

```
Write a git commit message based on the following changes.

Rules:
- Header: max 72 chars, imperative mood, no trailing period
  (e.g. "Add user authentication", "Fix null pointer in payment flow")
- Body: bullet list with dashes, each bullet a single complete thought,
  max 10 words per bullet
- Blank line between header and body
- No filler, no explanations, no preamble

Output format:
<type>: <header>

- <change 1>
- <change 2>
- <change 3>

Types: feat, fix, refactor, chore, docs, test, style, perf

Changes:
[diff or change description]
```

Commit messages stay in English regardless of conversation language.

---

## Success Metrics

### Context Efficiency
- <70% token usage for typical tasks
- <12k tokens loaded per session
- 10+ exchanges without compact

### Documentation Coverage
- 100% completed features have task docs
- 90%+ integrations have SOPs
- System docs updated within 24h
- Zero repeated mistakes

---

**For complete Navigator documentation**:
- `.agent/DEVELOPMENT-README.md` (project navigator)
- Plugin's root CLAUDE.md (full workflow reference)
