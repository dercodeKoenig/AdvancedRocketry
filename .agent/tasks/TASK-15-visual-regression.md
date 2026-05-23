# TASK-15: Visual regression infrastructure for Minecraft client

## Ticket

- Source: TASK-02 Phase 10 deferral (own proposal at the time),
  promoted into a tracked task on 2026-05-23 during the SSOT cleanup.
- Status: **Backlog**.
- Created: 2026-05-23.

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

This is the only task in the backlog that introduces a **new
infrastructure category** (image diffs) and a **new failure mode**
(false positives from GPU driver drift). Worth doing only if the
modpack-side reports start surfacing visual regressions, OR if a
single planned change (e.g. a GUI refactor) would benefit from
mass before/after coverage. Until then it sits in Backlog with no
P-tier — it is a "build the thing when we have a reason" task, not
a "build it preemptively" task.
