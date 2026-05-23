# SOP: Task lifecycle — single source of truth discipline

## Why this SOP exists

`.agent/` is the **single source of truth** for project state.
Status of any task lives in exactly one place: the corresponding
`TASK-NN-*.md` file. Everything else — `.agent/tasks/README.md`,
EOD markers, conversation summaries — is a **derived view** of
that truth.

This SOP exists because past drift has happened:

- "Already-known deferred" bullets in `README.md` outlived the
  tasks they were waiting on (e.g. `WorldCommand 0 coverage`
  listed as deferred after TASK-11 actually shipped it).
- Free-form claims like "_documentsKnownBug suffix is no longer in
  use" went stale when a comment-level reference remained.
- Done-table entries lagged actual file-header status by days.

The rule below makes each of those impossible by construction.

## The rule

**Status of a task lives in exactly one place: the TASK file
header**. Every other location is derived and must be regenerated
at close-out, not maintained in parallel.

Valid status values:

| Status | Meaning |
|---|---|
| `Backlog` | Real task, not yet started, no blockers |
| `Backlog (watching)` | Real task, deferred until a trigger fires (e.g. a flake recurring). Trigger MUST be documented in the task. |
| `Blocked` | Cannot start until a documented external prerequisite clears. Prerequisite MUST be documented in the task. |
| `In Progress` | Active work; an agent is currently in the task. |
| `Completed` | Shipped. Pyramid green. EOD marker saved. |
| `Obsolete` | No longer relevant. The reason MUST be documented. |

Free-form bullet lists describing "things we should eventually
do" are **forbidden** outside a TASK file. If it is worth
remembering, it is worth a TASK file (even a one-paragraph one).

## Closure checklist — apply on every TASK status change to `Completed`

When closing a task, the agent MUST work through this checklist in
order. Each item is a hard gate — do not move on until done.

### 1. Update the TASK file header

- `Status:` line set to `✅ Completed <YYYY-MM-DD>`.
- `Created:` line untouched (that is original creation date).
- Add a `## Result` section at the bottom summarising shipped
  artefacts: test counts, probe verbs, production touches,
  follow-ups. One paragraph is enough; this is the part future
  sessions will scan.

### 2. Sync the Done table in `tasks/README.md`

- Move the row from Backlog table to Done table.
- Done row format: `| TASK-NN | Title — one-line result | ✅ |`
- Top-of-file pyramid counter and bug-ledger counter updated if
  this task changed them.

### 2.5. Regenerate pyramid counter — REQUIRED if the closed TASK added or removed any test methods

The free-form stale-claim sweep in step 3 has historically **missed**
the pyramid counter line in `tasks/README.md` — it reads like a
labelled fact, not a free-form claim, so agents (and humans) skip it.
The 2026-05-23 audit found the counter stale by 236 tests because
every recent TASK closure trusted "+N added" arithmetic from commit
messages, and drafts have been off by 5+ per session. Regenerate
from the source of truth instead:

```
for tier in unit integration server client; do
  echo -n "$tier: "
  grep -rc '^    @Test$\|^	@Test$' \
    src/test/java/zmaster587/advancedRocketry/test/$tier/ \
    2>/dev/null | awk -F: '{s+=$2} END {print s}'
done
```

Sum the four tier counts and update the **Pyramid** line in the
`## Current state` section of `tasks/README.md` (format
`testUnit X / testIntegration Y / testServer Z / testClient W`).
Bump the "Counter verified <YYYY-MM-DD>" date on the same line.

Skip this step only if you can certify zero `@Test` methods were
added or removed by the closed TASK (rare — most closures move the
counter).

### 3. Stale-claim sweep — REQUIRED, NOT OPTIONAL

This is the step that has historically been skipped. Skipping it
is what caused every drift incident.

Run all four scans:

#### 3a. Scan the Backlog table for items this task just closed

Open `tasks/README.md`. Read every Backlog row. For each row, ask:
"did the TASK I just closed deliver any of what this row is
asking for?"

If yes:
- Move the closed row to Done (with reference to closing TASK).
- Or, if partially closed, edit the Backlog row's description to
  narrow the remaining scope.

#### 3b. Scan other TASK files' "Dependencies" / "Blocked on" sections

`grep -l "TASK-NN" .agent/tasks/` (substitute the closed task ID).

For each TASK file that references the closed task:
- If it was `Blocked` on this task → promote to `Backlog`.
- If its plan references something this task changed → edit the
  reference to reflect new state.

#### 3c. Scan `DEVELOPMENT-README.md` for stale pointers

Search for any reference to the closed task. Update or remove.

#### 3d. Scan for claims that may have gone false

Specifically search the closed TASK's diff for any production /
test file the TASK touched. Then `grep` for the file name in the
rest of `.agent/`. Any claim about that file's state in another
doc may now be stale — re-read and update.

### 4. EOD context marker

Save a marker in `.agent/.context-markers/` with the date,
short slug, and a 1-paragraph result summary. Update
`.agent/.context-markers/.active` if this is the final task of
the session.

### 5. Commit

Single commit, message format:

```
<type>: TASK-NN — <short result>

- key shipped artefact 1
- key shipped artefact 2
- README Done row added; <N> stale Backlog entries cleared
```

Where `<type>` follows the CLAUDE.md commit-prompt template
(`feat` / `fix` / `refactor` / `chore` / `docs` / `test` /
`style` / `perf`).

## Closure checklist — apply on every TASK status change to `Obsolete`

Same as Completed except:

- Step 1: status `❌ Obsolete <YYYY-MM-DD>` plus a `## Why obsolete`
  section explaining the supersession.
- Step 2: move to Done table with the obsolete marker.
- Steps 3-5 unchanged.

## Closure checklist — apply on every TASK status change to `Blocked`

When promoting an existing task INTO Blocked:

- Step 1: status `Blocked` + a `## Blocker` section naming the
  prerequisite. The blocker MUST be a concrete, verifiable
  condition (a file path + line, a configuration setting, a
  task ID dependency) — not "we should talk about it first".
- Steps 2-3 unchanged but the Backlog-row's "Status" column flips
  to `Blocked`.
- Step 4 (marker) optional unless the block was a surprise.
- Step 5 commit.

## What this SOP does NOT cover

- **Creation of a new TASK file**: see existing patterns in
  `TASK-13` through `TASK-16` for shape. A new task should have a
  Ticket section, Context, Plan or Approach options, Out-of-scope,
  Dependencies, and Estimated effort.
- **Mid-task progress tracking**: use the in-session task tool, not
  `.agent/`. `.agent/` records outcomes, not iterations.
- **Bug ledger maintenance**: see `CLAUDE.md` "Bug tracking" rule
  and `.agent/history/known-bugs-ledger.md`.

## Anti-patterns to avoid

- ❌ "Just a one-liner in README to remember this for later" → write
  a TASK file. Future-you will not remember the context.
- ❌ Marking a task Completed without the stale-claim sweep
  (step 3). This is how every prior drift happened.
- ❌ "Status: ✅ Completed partial" with no clear scope on what
  shipped vs deferred. Either close the task fully and open a
  successor TASK for the remainder, or mark `In Progress` and
  finish the remainder.
- ❌ Putting status anywhere outside the TASK file header. The
  README Done table is a row pointer with a one-line description,
  not an independent source of truth.
- ❌ Free-form claims about code state ("0 coverage", "no longer in
  use") in `.agent/tasks/README.md`. They go stale invisibly. If
  the claim matters, pin it with an assertion in the test suite.

## Tooling note

The `nav-task` skill is the natural place to enforce the closure
checklist — invoke it at task close-out and it should walk you
through steps 1-5. Until that skill is updated to mirror this
SOP exactly, the agent is responsible for running through this
file by hand.
