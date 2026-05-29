# SOP: Using `mcp__intellij__*` tools in this project

## Context

The IntelliJ IDEA MCP server exposes IDE capabilities (symbol
lookup, find-usages, refactor/rename, library-aware search,
project model) to the agent. In this project the MCP server is a
strict win for a narrow set of operations — and a strict loss for
others. This SOP captures which is which, plus the path-prefix
trap that bites every agent that hasn't read this file.

Applies to any session where `mcp__intellij__*` tools are
available (visible in the deferred-tool list, fetched via
`ToolSearch` with `select:mcp__intellij__<name>`).

## The path-prefix rule (most important)

**IDE root ≠ agent CWD.**

- The IntelliJ project is opened at **`/workspace`** (the
  super-directory that contains `AdvancedRocketry/`, the
  `libVulpes/` composite if present, and possibly other modules).
- Agents typically start at **`/workspace/AdvancedRocketry`** or
  deeper (worktrees, subdirs after `cd`).
- All `mcp__intellij__*` tools that take a `path` argument
  resolve it **relative to the IDE root**, NOT the agent CWD.

**Consequences:**

| Tool | Wrong | Right |
|---|---|---|
| `get_file_text_by_path` | `src/main/java/...` | `AdvancedRocketry/src/main/java/...` |
| `open_file_in_editor` | `src/...` | `AdvancedRocketry/src/...` |
| `replace_text_in_file` | `src/...` | `AdvancedRocketry/src/...` |
| `reformat_file` | `src/...` | `AdvancedRocketry/src/...` |
| `create_new_file` | `src/...` | `AdvancedRocketry/src/...` |
| `get_file_problems` | `src/...` | `AdvancedRocketry/src/...` |
| `find_files_by_glob` | `src/**/*.java` (matches all modules) | `AdvancedRocketry/src/**/*.java` |
| `list_directory_tree` (no arg) | dumps all of `/workspace` | pass `AdvancedRocketry` explicitly |
| `search_in_files_by_*` | matches across every module in `/workspace` | scope via `path` arg or filter results |

**`execute_terminal_command`** runs in the IDE's terminal whose
CWD is the IDE root (`/workspace`), NOT the agent CWD. If you
need to run gradle for AR, either prefix `cd AdvancedRocketry &&`
or — preferred — use the regular `Bash` tool, which inherits the
agent's CWD and gives controlled output streaming.

**Sanity check at session start**: if you plan to use MCP for
non-trivial work, call `get_project_modules` or
`get_repositories` once to confirm the IDE sees `AdvancedRocketry`
(and `libVulpes` as a composite, if expected). Stale or
not-yet-indexed projects silently return empty results from
search/symbol tools.

## When MCP wins (use it)

These are the cases where the MCP tool is strictly better than
the built-in equivalent. The win comes from **two** capabilities
the built-ins lack: (a) the IDE's symbol/type index, (b) coverage
of `.class` files from libraries (Minecraft, Forge, libVulpes)
that aren't on disk as `.java`.

- **`get_symbol_info`** ≫ `Grep` for symbol lookup. Returns
  definition, signature, Javadoc; resolves through interfaces,
  overrides, generics. **Works on decompiled
  `net.minecraft.*` / `net.minecraftforge.*` / `zmaster587.libVulpes.*`
  classes that have no source in this repo** — `Grep` cannot find
  them at all. This is the single largest reason MCP exists in
  this project.
- **Find Usages** (via `get_symbol_info` references) ≫ `Grep` by
  method name. Honours polymorphism, interface dispatch, and
  ignores same-named methods in unrelated classes. Especially
  valuable for common names: `update()`, `read()`, `write()`,
  `onBlockActivated()` — `Grep` returns hundreds of false
  positives.
- **`rename_refactoring`** ≫ manual `Edit` + `replace_all`.
  Updates all usages, imports, and Mixin targets (where the IDE
  resolves them). **Never** use it on:
  - `setRegistryName("...")` string literals — breaks saves
    (CLAUDE.md forbids this).
  - NBT keys in string literals — breaks save persistence.
  - Lang keys / JSON resource paths — IDE refactor won't find
    them, you'll get half-renamed code.
- **`get_file_problems`** for a single file post-edit. Faster
  than `./gradlew compileJava`; surfaces IDE warnings (unused
  imports, raw types, unchecked casts) that the compiler stays
  silent on. Good cheap sanity check after a series of edits.
- **`get_project_modules` / `get_project_dependencies`** — only
  fast way to see the resolved classpath with versions
  (libVulpes commit, Forge build, JEI version) without parsing
  `build.gradle.kts` by hand.
- **`search_in_files_by_regex` / `search_in_files_by_text` —
  but only when scoped to libraries**. Searching vanilla
  Minecraft / Forge / libVulpes bytecode for a string is
  impossible with `Grep` (those `.java` files don't exist on
  disk). For our own sources, `Grep` is faster and lighter.

## When MCP is neutral or worse (prefer built-ins)

- **Reading a file in this repo**: `Read` beats
  `get_file_text_by_path`. Same content, no path-prefix trap,
  no IDE-index dependency.
- **Glob/search across our sources**: `Grep` / `Glob` with
  `glob: "src/**"` is faster, cheaper, and immune to indexing
  state. Use MCP search **only** when the target may live in a
  jar dependency.
- **Editing a file**: `Edit` is safer than `replace_text_in_file`
  — `Edit` requires `old_string` to be unique and verifies the
  Read-first invariant. Use MCP-replace only for files outside
  the repo (IDE config, scratch files).
- **Creating a file**: `Write` beats `create_new_file` — no IDE
  round-trip.

## When NOT to use MCP at all

- **`execute_run_configuration`** for tests / runs. Heavy, stream
  goes to IDE console (not back to the agent), and bypasses the
  cache-bust + per-run-`PASSED` grep discipline required by
  [`flake-diagnosis.md`](./flake-diagnosis.md). Always use `Bash`
  + `./gradlew testServer …` for AR test runs.
- **`open_file_in_editor`** unless the user explicitly asked you
  to open a file in their IDE. It returns nothing to the agent;
  it's a pure UI side-effect.
- **`reformat_file`** by default. If the project lacks
  enforced IDE code-style settings (we don't ship `.editorconfig`
  for Java), it may introduce diff noise unrelated to your
  change. Only run when the user asks.
- **`get_all_open_file_paths`** as a source of truth. It's a
  hint about what the user is currently looking at, useful for
  aligning context — not for deciding what to edit.

## Quick reference table

| Task | Tool |
|---|---|
| Read a file in this repo | `Read` |
| Read a class from Minecraft / Forge / libVulpes | `get_symbol_info` or `get_file_text_by_path` |
| Look up where a symbol is defined | `get_symbol_info` |
| Find all usages of a method or class | MCP find-usages (`get_symbol_info` → references) |
| Search our sources (text/regex) | `Grep` with `glob: "src/**"` |
| Search Minecraft / Forge / libVulpes for a string | `search_in_files_by_regex` (MCP) |
| Rename a symbol | `rename_refactoring` (NEVER for registry IDs / NBT keys / lang keys) |
| Point-edit a file | `Edit` |
| Quick compile-check after edits | `get_file_problems` |
| Run tests / gradle | `Bash` + `./gradlew …` (NOT `execute_run_configuration`) |
| List modules / resolved deps | `get_project_modules` / `get_project_dependencies` |
| Create a new file in this repo | `Write` |

## Litmus before reaching for an MCP tool

Ask: **"Does this operation need either (a) the IDE symbol index,
or (b) access to a library `.class` file that has no source on
disk?"**

- Yes → MCP is probably the right choice.
- No → built-in is cheaper and more predictable.

## Related SOPs

- [`testing-principles.md`](./testing-principles.md) — contracts vs impl details
- [`flake-diagnosis.md`](./flake-diagnosis.md) — why `execute_run_configuration` is forbidden for tests
- [`task-lifecycle.md`](./task-lifecycle.md) — closure checklist
