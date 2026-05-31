# SOP — Bash exit codes that look like failures but aren't

When the harness reports a non-zero exit from a Bash tool call, the
default reaction is "stop and investigate". That instinct is correct
for build/test commands but **wrong** for a handful of POSIX utilities
whose exit code is part of their query semantics. Treat the codes
below as informational, not as a blocker — keep going.

## The "1 means empty result" family

| Command   | Exit 1 means                                | What to do |
|-----------|---------------------------------------------|------------|
| `pgrep P` | no process matched pattern                  | Means already-clean. Continue. |
| `pkill P` | no process to kill                          | Means already-clean. Continue. |
| `grep`    | zero matches                                | Continue — absence is a real result, not an error. |
| `diff`    | files differ                                | Often the answer you wanted. Continue. |
| `cmp`     | files differ                                | Same as diff. Continue. |
| `test` / `[ ]` | condition false                        | Branching, not failure. Continue. |
| `find ... -quit` after a hit | varies by version            | Inspect stdout, not exit code. |

**Rule of thumb**: if the command's purpose is to *answer a yes/no
question*, exit 1 is the "no" answer, not an error. Only exit 2+ on
these tools is a true failure (bad regex, permission denied, etc.).

## How to avoid stopping on these

Two safe idioms — pick one and use it consistently in this repo:

1. **Trailing `|| true`** when you genuinely don't care:
   ```bash
   pkill -f "GradleStart" || true
   ```
2. **Guard chain** when you want the next step to fire only on the
   "found something" branch:
   ```bash
   pgrep -af GradleStart && echo "still running" || echo "clean"
   ```

For tool-call chains where you grep AND want a follow-up, prefer:
```bash
grep -q PATTERN file && do_something  # never stops the chain on no-match
```

## Cleanup pattern for AR test/run JVMs

The single recurring case in this project: tearing down a stuck
`runClient` / `runServer` / harness fork. Use this exact line:

```bash
pkill -9 -f "GradleStart" 2>/dev/null
pkill -f "GradleWrapperMain (runClient|runServer)" 2>/dev/null
pkill -f "RealDedicatedServerHarness\|RealClientHarness" 2>/dev/null
sleep 2
pgrep -af "java.*GradleStart\|java.*Gradle.*run" || echo "✓ all clear"
```

The final `|| echo` swallows the exit-1 from a clean pgrep so the
calling agent doesn't pause.

## Spurious exit 1 from a broken PostToolUse hook

If EVERY Bash result reports `Exit code 1` regardless of what the
command did, and a system-reminder mentions
`nav_commit_reminder.py: No such file or directory`, the cause is a
stale Navigator hook entry pointing at a path that no longer exists
in the installed plugin.

**Where it actually lives** (verified 2026-05-31): NOT in
`.claude/settings.json` — it's the plugin's own
`.claude-plugin/plugin.json`, under `hooks.PostToolUse`, the entry
with `"matcher": "Bash"`. `nav_commit_reminder.py` was a v6.12.1
probe (characterised PostToolUse output channels — see plugin
`mem-035`/OQ-3); the file was later deleted but its registration was
left behind. The plugin is installed in two copies that must BOTH be
fixed:
- `~/.claude/plugins/marketplaces/navigator-marketplace/.claude-plugin/plugin.json`
- `~/.claude/plugins/cache/navigator-marketplace/navigator/<ver>/.claude-plugin/plugin.json`

The hook fires AFTER your command and crashes; its non-zero exit
propagates back to the tool harness, masking your command's actual
exit code. **Your command still ran correctly.** Read the actual
stdout/stderr to judge success — ignore the harness exit code in
this mode.

Fix (when the user OKs the plugin-config change): delete the orphaned
`Bash → nav_commit_reminder.py` block from `hooks.PostToolUse` in both
`plugin.json` copies. Editing the plugin config is gated by the
auto-mode self-modification classifier, so it needs explicit user
approval. **The fix only takes effect after a session restart** —
Claude Code caches the hook config at session start. Until restart,
treat all "Exit code 1" reports as informational.

## Reason this SOP exists

Without it, the agent stops mid-task every time `pkill` is used as a
"if anything's there, kill it" idempotent step — because the harness
treats exit 1 as a hard failure signal. Documented here so future
agents (and humans grepping for "pkill" in SOPs) get the disambiguation
in one place instead of re-learning it per session.
