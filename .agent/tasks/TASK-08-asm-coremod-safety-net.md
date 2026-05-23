# TASK-08: ASM coremod safety net (transformer regression coverage)

## Ticket

- Source: TASK-03 EOD audit (2026-05-19) — `asm/` has 3 production
  files (`ClassTransformer` 835 LoC, `AdvancedRocketryPlugin`,
  helpers) and 0 test coverage. This is the **highest-risk gap** in
  the mod: a bad transformer = full game crash at boot.
- Status: ❌ Obsolete — superseded by TASK-08-mixin (the ASM coremod was rewritten to Mixin, so a safety net for the now-deleted code is moot). Kept for historical context only.
- Created: 2026-05-19
- Predecessor: `.agent/.context-markers/2026-05-19-1230_task03-A-and-B-mostly-done-eod.md`

## Context

`AdvancedRocketryPlugin` (the coremod entry point) loads
`ClassTransformer`, which intercepts vanilla / Forge class loads and
rewrites bytecode. Patches include rendering hooks, gravity overrides,
fluid handling tweaks, and miscellaneous hooks via the bundled
`gloomyfolken.hooklib.asm` framework (`repack/`, 24 files).

A regression in:

- The transformer's class-name match logic (silently skips a target
  class) — gameplay feature breaks invisibly.
- The bytecode rewrite itself (corrupted constant pool / wrong opcode)
  — `VerifyError` at load → game crash.
- Patch ordering when multiple transformers compete — undefined
  behaviour.

The mod doesn't currently have a single test that exercises any of
this. **A class-rewrite regression here is the most dangerous failure
mode in the entire codebase** because it surfaces only when end-users
launch the game; CI passes cleanly with the production-side tests
because they don't go through the launchwrapper that triggers the
transformers.

Out of scope: hot-reload of transformers (not supported by Forge
classloader anyway).

**No production logic changes** (same rule as TASK-01 §15). Tests are
read-only; they snapshot transformer outputs against golden bytecode.

## Implementation Plan

### Phase 1: Inventory + golden-snapshot infrastructure (~3-4 h)

- [ ] Enumerate every `ClassTransformer.transform(...)` branch by
  target class name. Document each branch's intent in a comment table
  inside the test file.
- [ ] Set up a `TransformerGoldenSnapshotTest` that:
  - Reads the original (pre-transform) `.class` resource from the
    test classpath.
  - Feeds it through `ClassTransformer.transform(name, name, bytes)`.
  - Runs the output through ASM's `CheckClassAdapter` to validate
    structural correctness (no broken stack maps, no orphan labels).
  - Hashes the output (SHA-256) and compares against a frozen golden
    hash stored in `src/test/resources/asm-goldens/{class}.sha256`.

### Phase 2: Per-branch deep tests (~5-6 h)

For each transformer branch, write a test that:

- [ ] Verifies the transformer DID modify the class (output hash !=
  input hash).
- [ ] Asserts the specific bytecode change is present (target method
  has an inserted invokestatic, an inserted ldc, etc. — use ASM's
  `ClassReader` + custom visitor to scan for the hook).
- [ ] Counter-test: transformer with a name that DOESN'T match must
  pass-through unmodified (out == in).

### Phase 3: Classloader-isolation smokes (~2 h)

- [ ] `AdvancedRocketryPlugin.getASMTransformerClass` returns the
  expected FQN (must NOT silently regress to null on a refactor).
- [ ] `getModContainerClass`, `getSetupClass`, `getAccessTransformerClass`
  contracts.
- [ ] `injectData` is called by the launchwrapper with expected key
  set (smoke for the @IFMLLoadingPlugin contract).

### Phase 4: VerifyError canary (~2-3 h)

Hardest but most valuable: load the transformed class into a JVM and
attempt to instantiate / call a method on it. If the bytecode is
corrupt, the JVM emits VerifyError. Approach:

- [ ] Use a child-classloader pattern: load the original Minecraft
  class file as a resource, transform it, define it in a fresh
  `URLClassLoader` (or custom anonymous classloader), call
  `Class.forName(name, true, loader)` to trigger verification.
- [ ] Catch `VerifyError` → fail the test with the verbose dump from
  ASM's `Textifier`.

### Phase 5: Hook-lib smoke (~1-2 h)

`repack/gloomyfolken/hooklib/` is third-party code AR ships
internally. We don't own the source but we DO own:

- [ ] A smoke test that `AsmHook.create()`-style API calls don't
  crash on construction.
- [ ] The hook-targets AR registers (search `addHookToBuild` /
  `AsmHook` instantiations in main code) — each construction
  succeeds and produces an installable hook.

### Phase 6: Validation + EOD (~1 h)

- [ ] Full pyramid PASS — testUnit will gain ~15-20 new tests.
- [ ] EOD marker.

## Technical Decisions

- **All ASM tests are unit-tier** — no Forge boot, no Minecraft
  classloader. We use ASM's own classfile reader/writer + a custom
  classloader for the VerifyError canary. No `MinecraftBootstrap`
  required.
- **Golden snapshots are by hash, not by full bytecode dump** — a
  full dump in the repo would balloon git history. The hash test fails
  with a clear message ("transformer output diverged from frozen
  golden — update `asm-goldens/<class>.sha256` AND describe the change
  in the commit message"); the test author then runs an ASM Textifier
  on both versions to confirm the diff is intended.
- **Goldens are regenerated when production transformer changes** — a
  small Gradle task `regenerateAsmGoldens` runs the transformer in
  no-assert mode and writes the new hashes. CI does NOT run this task.
- **External hook lib** (`repack/gloomyfolken`) treated as opaque —
  no per-line coverage attempted.

## Dependencies

**Requires**: nothing — ASM tests are self-contained.
**Does NOT block**: feature work.
**Soft-blocks**: any future ASM transformer refactor — those changes
must regenerate goldens AND document the diff.

## Estimated effort

~13-18 hours across 4-5 sessions.

## Completion Checklist

- [ ] Transformer branch inventory documented inline
- [ ] Golden-snapshot harness (`TransformerGoldenSnapshotTest`)
- [ ] Per-branch deep tests (one per transform target)
- [ ] Classloader-isolation smoke tests
- [ ] VerifyError canary tests passing on every transformer target
- [ ] Hook-lib smoke
- [ ] `regenerateAsmGoldens` Gradle task documented
- [ ] Full pyramid PASS
- [ ] EOD marker
