# TASK-15: Visual regression infrastructure for Minecraft client

**Status: ❌ Not planned (closed 2026-05-29)**

## Ticket

- Source: TASK-02 Phase 10 deferral (own proposal at the time),
  promoted into a tracked task on 2026-05-23 during the SSOT cleanup.
- Status: **❌ Not planned (closed 2026-05-29)**. Speculative
  infrastructure with no live trigger and high build cost — the
  user explicitly closed it during the 2026-05-29 audit-delta pass
  rather than continue carrying it as `👁 Backlog (watching)`.
  State-tier tests continue to catch the functional half of any
  regression that manifests both visually AND functionally; the
  visual-only half remains implicitly uncovered. Re-open via a new
  TASK file if any of the original promotion triggers fires
  (see below — kept for future reference).
- Created: 2026-05-23. Closed: 2026-05-29.

## Promotion triggers

Promote out of "watching" into `In Progress` when ANY of these
fires. Until then this task does NOT get scheduled — coverage of
visual regressions stays implicitly zero (state-tier tests
continue to catch the functional half of any regression that
manifests both visually AND functionally).

1. **Planned GUI refactor lands in scope**. Mass before/after
   coverage of touched GUIs is the canonical reason to invest
   in visual regression. Trigger fires when a TASK opens that
   touches `gui/`, `inventory/modules/`, or libVulpes
   `GuiModular` paths.
2. **Modpack-side visual regression report**. Player or modpack
   maintainer files an issue describing a visual-only bug
   (texture binding, layout shift, HUD overlay misrender,
   skybox glitch). The new TASK opened in response cites this
   one as its dependency.
3. **JEI / GUI rework that breaks the IAdvancedGuiHandler
   integration**. The `ARPlugin.register` block (lines 101-118)
   has been silently fragile in modpack contexts before;
   visual diff would catch the class of "JEI overlay extra
   areas wrong" regression early.
4. **Texture-pipeline change in libVulpes or Forge 1.12.2 dep
   bump**. Texture-binding regressions across a Forge / libVulpes
   version bump are the worst class of silent break — surface
   silently, ship without anyone noticing.

If none of these has fired in 6+ months, revisit and consider
closing as Obsolete during the next SSOT cleanup pass.

## Context

testClient e2e currently asserts on **state** (chat lines, NBT,
registry, world-state probes) but not on **pixels**. Three classes
of regression are therefore invisible to the suite:

1. GUI layout regressions (slot positions, button widths, tab
   ordering in JEI integration).
2. World-render regressions (broken texture binds, missing block
   textures, atmosphere rendering, planet-skybox).
3. HUD regressions (sealability overlay, suit overlay, mission
   tracker overlay).

A modpack-side regression in any of these silently breaks player
experience without breaking a single test.

## Why this is hard for Minecraft 1.12.2

Standard tools (Storybook, Chromatic, Percy, BackstopJS) all
assume a web rendering surface. Minecraft 1.12.2 renders via LWJGL
+ OpenGL into a native GL context. Screenshot-capture infrastructure
exists at the Minecraft level (`/screenshot` command,
`ScreenShotHelper.saveScreenshot`), but:

- It produces full-window PNGs, not isolated component crops.
- It runs only on the integrated client, which means it has to live
  in the testClient profile (same one with the LWJGL flake history;
  see marker note about `DISPLAY=:77`).
- Pixel-perfect diff is unreliable across drivers / GPU
  manufacturers / mesa versions. Need perceptual-diff (SSIM /
  pixelmatch threshold) not strict byte-equality.

## Approach sketch

### Phase 1 — Capture infrastructure (~3 h)

`/artest screenshot capture <slug>` verb that:

1. Defers a single client tick.
2. Calls `ScreenShotHelper.saveScreenshot` with a deterministic
   filename `expected-<slug>.png` (write) or `actual-<slug>.png`
   (compare).
3. Returns the file path via probe-response.

Add a thin `VisualBaselineFixtures` helper for the testClient suite
to call the verb and load both files for diff.

### Phase 2 — Diff infrastructure (~2 h)

Vendor a small perceptual-diff library (or write ~50 lines around
SSIM). Bundle as a test-classpath dep. Define a per-image
tolerance ceiling (e.g. 0.5% pixel delta).

### Phase 3 — First baseline suites (~4 h)

Three minimum suites to prove the shape:

- `MainMenuVisualRegressionTest` (golden screenshot of the main
  menu — pure smoke).
- `JeiRecipeCategoryVisualRegressionTest` (open a known AR recipe
  category in JEI).
- `RocketAssemblyGuiVisualRegressionTest` (open the rocket assembly
  GUI with a known blueprint loaded).

Baselines committed under `.agent/visual-baselines/`.

### Phase 4 — CI integration (~1 h)

Gate testClient on visual diff. On failure, the harness saves the
diff overlay PNG to `.agent/visual-baselines/diffs/<slug>.png` so a
human can eyeball the change and either approve (commit the new
baseline) or fix the regression.

## Out-of-scope deferrals

- Cross-platform / cross-GPU baseline matrix (start single-platform).
- Animated state (rocket flight cycle frames) — golden screenshots
  are static states only.
- Companion-mod GUIs — TASK-14 was closed as Obsolete on
  2026-05-23 (mod-absent paths implicitly pinned, present-branch
  coverage not justified). Visual regression of companion-mod
  GUIs would therefore need its own scope decision tied to a
  specific reported regression, not pre-emptive.

## Dependencies

- Requires testClient harness to be stable (DISPLAY=:77 known-good
  per session marker 2026-05-22).
- Does NOT block any other task.

## Estimated effort

- Phase 1 + 2 + Phase 3 starter suite: ~10 h
- Full CI integration including diff overlay UX: +4 h
- Ongoing maintenance: ~30 min per accepted baseline change

## Risk notes

This task introduces a **new infrastructure category** (image
diffs) and a **new failure mode** (false positives from GPU
driver drift). It is intentionally "watching" not "Backlog" —
the cost/benefit only works when paired with a specific trigger
(see Promotion triggers above). Pre-emptive infrastructure that
nobody is consuming protects nothing and tends to rot.
