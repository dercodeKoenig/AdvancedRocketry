# AdvancedRocketry - Development Documentation Navigator

**Project**: Fork of Advanced Rocketry — a Minecraft 1.12.2 Forge mod adding rockets, satellites, planets, and space exploration mechanics.
**Tech Stack**: Java 8, Minecraft Forge 1.12.2, Kotlin DSL Gradle, FancyGradle, JEI integration, libVulpes
**Updated**: 2026-05-23

---

## ⚠️ Required reading before any non-trivial work

### Before writing or auditing tests

**[SOP: Testing Principles](./sops/development/testing-principles.md)** —
must be re-read every time you touch the test suite.

**TL;DR**: tests verify *contracts* (player-visible behaviour, public
API, registry/NBT/wire formats), NOT implementation details (exact RF
costs, exact loop bounds, exact internal field shapes). If a refactor
that preserves user-visible behaviour breaks your test, the test is
over-tight — fix the test, not the refactor.

**Litmus for every assertion**: "this test fails if production breaks
the contract that ____" — if the blank is an impl detail, redesign.

When auditing test depth, count **contract-coverage**, not pin-count.
Resist the temptation to "tighten" with magic-number assertions —
that's the wrong shape of pin.

### Before tuning retry budgets / chasing test flakes

**[SOP: Flake diagnosis](./sops/development/flake-diagnosis.md)** —
must be read before reaching for the retry-budget knob OR running a
10× verification sweep.

**TL;DR**: failure DISTRIBUTION tells you the mode. Same N tests
every run → regression (revert your recent diff). Sparse
non-deterministic set → race (find the non-time variable: chunk
load, populate, tick-gate, recipe order). Alternating outputs on
same test → test-design (loosen, don't tighten).

10× verification loops MUST cache-bust between iterations (delete
`build/{reports,test-results,tmp}/testServer`) AND grep per-run
PASSED count — Gradle's `:testServer UP-TO-DATE` will report PASS
on every run after the first if you don't.

If your retry budget exceeds 5 s and the failure rate is still
> 5 %, the fix is structural, not timed.

### Before using `mcp__intellij__*` tools

**[SOP: MCP IntelliJ usage](./sops/development/mcp-intellij-usage.md)** —
read once per session that plans to use the IntelliJ MCP server.

**TL;DR**: the IDE is opened at **`/workspace`**, not at
`/workspace/AdvancedRocketry` — every MCP `path` argument resolves
from the IDE root, so AR files need the `AdvancedRocketry/`
prefix. MCP wins for symbol lookup, find-usages, and searching
decompiled Minecraft/Forge/libVulpes classes (no `.java` on
disk → `Grep` can't see them). Built-ins (`Read`, `Edit`, `Grep`,
`Glob`) win for our own sources. **Never** use
`execute_run_configuration` for tests (bypasses
flake-diagnosis cache-bust); never `rename_refactoring` registry
IDs / NBT keys / lang keys (breaks saves).

### Before closing a TASK (status → Completed / Obsolete / Blocked)

**[SOP: Task lifecycle](./sops/development/task-lifecycle.md)** —
must be followed when changing any task's status.

**TL;DR**: status of a task lives in exactly one place — the
`TASK-NN-*.md` file header. Everything else (`tasks/README.md`,
markers, this navigator) is a derived view. The closure checklist
(steps 1-5, including the mandatory **pyramid counter regen
(step 2.5)** and **stale-claim sweep (step 3)**) prevents the
drift that caused every prior SSOT incident. Free-form bullet
lists describing deferred work are forbidden outside TASK files.

---

## 🚀 Quick Start for Development

### New to This Project?
**Read in this order:**
1. [Project Architecture](./system/project-architecture.md) - Tech stack, structure, patterns
2. [Tech Stack Patterns](./system/tech-stack-patterns.md) - Framework-specific patterns
3. [Workflow Guide](./system/workflow.md) - Development workflow

### Starting a New Feature?
1. Check if similar task exists in [`tasks/`](#implementation-plans-tasks)
2. Read relevant system docs from [`system/`](#system-architecture-system)
3. Check for integration SOPs in [`sops/`](#standard-operating-procedures-sops)
4. Create ticket in your project management tool
5. Generate implementation plan with `/nav:update-doc feature TASK-XX`

### Fixing a Bug?
1. Check [`sops/debugging/`](#debugging) for known issues
2. Review relevant system docs for context
3. After fixing, create SOP: `/nav:update-doc sop debugging [issue-name]`

---

## 📂 Documentation Structure

```
.agent/
├── DEVELOPMENT-README.md     ← You are here (navigator)
│
├── tasks/                    ← Implementation plans from tickets
│   └── TASK-XX-feature.md
│
├── system/                   ← Living architecture documentation
│   ├── project-architecture.md
│   └── tech-stack-patterns.md
│
└── sops/                     ← Standard Operating Procedures
    ├── integrations/         # Third-party service integration guides
    ├── debugging/            # Common issues and solutions
    ├── development/          # Development workflows
    └── deployment/           # Deployment procedures
```

---

## 📖 Documentation Index

### System Architecture (`system/`)

#### [Project Architecture](./system/project-architecture.md)
**When to read**: Starting work on project, understanding overall structure

**Contains**:
- Technology stack
- Project folder structure
- Component architecture patterns
- Routing setup
- Performance targets
- Development workflow
- Code quality standards

**Updated**: Every major architecture change

#### [Tech Stack Patterns](./system/tech-stack-patterns.md)
**When to read**: Implementing new components/features

**Contains**:
- Framework-specific best practices
- Design patterns for your stack
- Common mistakes to avoid
- Performance optimization techniques

**Updated**: When adding new patterns or major components

---

### Implementation Plans (`tasks/`)

**Single source of truth for task status**:
[`tasks/README.md`](./tasks/README.md). Do not mirror that list
here — this navigator only points at it.

That file maintains:
- Done table (all completed + obsolete tasks)
- Backlog table (with explicit Blocker / trigger column per row)
- Dependency graph
- Pyramid + bug-ledger counters at the top

**Lifecycle discipline**: see
[`sops/development/task-lifecycle.md`](./sops/development/task-lifecycle.md)
for the closure checklist (status transitions, stale-claim sweep,
commit format). The checklist is mandatory when flipping a task to
`Completed`, `Obsolete`, or `Blocked`.

**Bug ledger**: live tracking is in the test suite (pinned
assertions). Historical batch lives in
[`history/known-bugs-ledger.md`](./history/known-bugs-ledger.md).

**Format**: `TASK-XX-feature-slug.md`

**When created**:
- Via `/nav:update-doc feature TASK-XX` after completing feature
- OR manually when starting major feature (planning phase)

**Template structure**:
```markdown
# TASK-XX: [Feature Name]

## Ticket
- Ticket: [URL]
- Status: In Progress / Completed
- Sprint/Milestone: [Name]

## Context
[Why building this]

## Implementation Plan
### Phase 1: [Name]
- [ ] Sub-task 1
- [ ] Sub-task 2

## Technical Decisions
[Framework choices, patterns used]

## Dependencies
[What's required, what this blocks]

## Completion Checklist
- [ ] All sub-tasks completed
- [ ] System docs updated
- [ ] Tests written
- [ ] Deployed
```

---

### Standard Operating Procedures (`sops/`)

**Purpose**: Process knowledge, integration guides, debugging solutions

#### Integrations (`sops/integrations/`)
**When to create**: After integrating third-party service or new pattern

**Example SOPs**:
- JEI (Just Enough Items) integration
- libVulpes integration
- Galacticraft compatibility
- CurseForge upload pipeline

#### Debugging (`sops/debugging/`)
**When to create**: After solving non-obvious bug or recurring issue

**Example SOPs**:
- Forge runtime crashes
- Build/deobf jar issues
- Coremod/ASM transformer issues
- Common runtime issues

#### Development (`sops/development/`)
**When to create**: Establishing development patterns and workflows

**Example SOPs**:
- Local Forge dev environment setup
- Running client/server in IntelliJ
- Mappings refresh
- Git workflow

#### Deployment (`sops/deployment/`)
**When to create**: After setting up deployment processes

**Example SOPs**:
- CurseForge release checklist
- Maven publish process
- Changelog generation

**SOP Template**:
```markdown
# SOP: [Process Name]

## Context
[When/why you need this]

## Problem
[What went wrong or needs to be done]

## Solution
### Step-by-step
1. [Action 1]
2. [Action 2]

### Code Example
\`\`\`
// Example implementation
\`\`\`

## Prevention
- [ ] Checklist item to avoid future issues
- [ ] Validation step to add

## Related Documents
- See also: system/[doc].md
- Ticket: TASK-XX
```

---

## 🔄 When to Read What

### Scenario: Starting New Feature

**Read order**:
1. Ticket via project management → Get requirements
2. Check `tasks/` for similar previous work
3. Review `system/project-architecture.md` → Understand where this fits
4. Review `system/tech-stack-patterns.md` → Patterns needed
5. Check `sops/integrations/` → Any relevant integration guides
6. Generate implementation plan → `/nav:update-doc feature TASK-XX`

**Load into context**: Only relevant docs, not entire .agent/

### Scenario: Adding Third-Party Integration

**Read order**:
1. Check `sops/integrations/` → Similar integration exists?
2. `system/project-architecture.md` → Where integration fits
3. Implement integration
4. Create new SOP → `/nav:update-doc sop integrations [service-name]`
5. Update `system/project-architecture.md` if architecture changed

### Scenario: Debugging Issue

**Read order**:
1. Check `sops/debugging/` → Known issue?
2. Review relevant system doc for context
3. Check project management for related tickets
4. Solve issue
5. If novel pattern → Create SOP: `/nav:update-doc sop debugging [issue-name]`

### Scenario: Context Optimization (Running Low on Tokens)

**Do this**:
1. Read ONLY `DEVELOPMENT-README.md` (this file) → ~2,000 tokens
2. Load ONLY current feature's task doc → ~3,000 tokens
3. Load ONLY needed system doc → ~5,000 tokens
4. Reference SOPs on-demand → ~2,000 each

**Total**: ~12,000 tokens vs ~150,000 if loading everything

**After isolated tasks**: Run `/compact` to clear conversation history

---

## 🛠️ Slash Commands Reference

### `/nav:update-doc` Command

**Purpose**: Maintain documentation system

**Modes**:

#### 1. Initialize Structure
```bash
/nav:update-doc init
```
Creates folders, generates initial system docs, sets up README

#### 2. Archive Feature Implementation
```bash
/nav:update-doc feature TASK-XX
```
After completing feature, archives implementation plan and updates system docs

#### 3. Create SOP
```bash
/nav:update-doc sop <category> <name>

# Examples:
/nav:update-doc sop integrations jei
/nav:update-doc sop debugging coremod-loader
/nav:update-doc sop development forge-setup
```

#### 4. Update System Doc
```bash
/nav:update-doc system <doc-name>

# Examples:
/nav:update-doc system architecture
/nav:update-doc system patterns
```

---

## 📊 Token Optimization Strategy

### On-Demand Documentation Loading

**Instead of loading everything** (~150,000 tokens):

1. **Always load**: `DEVELOPMENT-README.md` (~2,000 tokens)
2. **Load for current work**: Specific task doc (~3,000 tokens)
3. **Load as needed**: Relevant system doc (~5,000 tokens)
4. **Load if required**: Specific SOP (~2,000 tokens)

**Total**: ~12,000 tokens vs ~150,000 (92% savings)

### When to Run `/compact`

**Run after**:
- Completing isolated sub-task
- Finishing documentation update
- Creating SOP
- Research phase before implementation
- Resolving blocker (separate from main work)

**Don't run when**:
- In middle of feature implementation
- Context needed for next sub-task
- Debugging complex issue

---

## ✅ Documentation Quality Checklist

### When Creating Task Doc
- [ ] Ticket linked with URL
- [ ] Context explains WHY building this
- [ ] Implementation broken into phases
- [ ] Technical decisions documented
- [ ] Dependencies mapped (requires, blocks)
- [ ] Completion checklist comprehensive

### When Creating SOP
- [ ] Clear context (when/why needed)
- [ ] Problem statement specific
- [ ] Step-by-step solution provided
- [ ] Code examples included
- [ ] Prevention checklist added
- [ ] Related documents linked
- [ ] Ticket referenced if applicable

### When Updating System Doc
- [ ] Reflects current codebase state
- [ ] Code examples are accurate
- [ ] Timestamp updated
- [ ] README.md index updated
- [ ] Breaking changes noted
- [ ] Related SOPs created if needed

---

## 🚦 Success Metrics

### Documentation Coverage
- [ ] 100% of completed features have task docs
- [ ] 90%+ of integrations have SOPs
- [ ] System docs updated within 24h of changes
- [ ] Zero repeated mistakes (SOPs working)

### Context Efficiency
- [ ] <70% token usage for typical tasks
- [ ] <12,000 tokens loaded per session (documentation)
- [ ] Context optimization rules followed
- [ ] /compact used appropriately

---

**This documentation system transforms your tickets into living knowledge while keeping AI context efficient.**

**Last Updated**: 2026-05-11
**Powered By**: Navigator
