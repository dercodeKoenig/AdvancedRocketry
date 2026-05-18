# AdvancedRocketry - Claude Code Configuration

## Context

Fork of Advanced Rocketry — a Minecraft 1.12.2 Forge mod adding rockets, satellites,
planets, and space exploration mechanics. Reworked/maintained for the modern
"Towards Rocket Science" modpack.

**Tech Stack**: Java 8, Minecraft Forge 1.12.2, Kotlin DSL Gradle (FancyGradle), libVulpes,
JEI integration, ASM coremod (`AdvancedRocketryPlugin`)

**Core Principle**: Maintain compatibility with vanilla 1.12.2 Forge ecosystem; favor
small targeted bugfixes over large refactors; preserve existing public API/registry IDs
to avoid breaking saves and dependent mods.

**Last Updated**: 2026-05-11
**Navigator Version**: 5.5.0

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
- ASM transformers live in `zmaster587.advancedRocketry.asm` (coremod entry point: `AdvancedRocketryPlugin`)
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
