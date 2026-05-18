# SOP: Run testClient on a headless Linux sandbox

## Context

The AR `testClient` Gradle task launches a real, GL-rendering Minecraft
client per scenario via `RealClientHarness`. On developer Windows boxes
it "just works"; on a headless Linux sandbox (no GPU, no compositor) it
crashes during LWJGL `Display.<clinit>` even though Mesa/llvmpipe is
installed.

## Problem

Symptom (from `/tmp/forge-test-client-last.log`):

```
java.lang.ExceptionInInitializerError
    at net.minecraft.client.Minecraft.setWindowIcon(Minecraft.java:680)
    at net.minecraft.client.Minecraft.init(Minecraft.java:456)
    ...
Caused by: java.lang.NullPointerException
    at org.lwjgl.opengl.LinuxDisplay.getAvailableDisplayModes(LinuxDisplay.java:947)
    at org.lwjgl.opengl.LinuxDisplay.init(LinuxDisplay.java:738)
    at org.lwjgl.opengl.Display.<clinit>(Display.java:138)
```

Every `testClient` scenario fails as `Failed to start real client
harness`. The build itself reports `BUILD FAILED in 14m`.

### Why

LWJGL 2.9.4's `LinuxDisplay.getAvailableDisplayModes` calls
`XRRGetScreenInfo` and dereferences the returned mode list. On Xvfb
servers with **no connected output** (`xrandr` reports
`HDMI-A-0 disconnected`, `DisplayPort-0 disconnected`, …), XRandR has no
modes to enumerate and the dereference NPEs. A second contributing issue
is Mesa's loader trying to open a driver with an empty name
(`/usr/lib/dri/_dri.so`); that error is non-fatal because Mesa falls
back to `llvmpipe`, but it spams the log and made the root cause hard to
find.

## Solution

### Step-by-step

1. **Pick an Xvfb display that has a connected output.** On this sandbox
   the long-running Xvfb at `:77` was started with `-screen 0
   1920x1080x24 +extension GLX +extension RANDR` and presents a single
   connected output; `:99` does not. Verify:

   ```bash
   ps aux | grep -i xvfb
   DISPLAY=:77 xrandr | head -3
   #  Screen 0: minimum 1 x 1, current 1920 x 1080, maximum 1920 x 1080
   #  screen connected 1920x1080+0+0 0mm x 0mm
   #     1920x1080      0.00*
   ```

   If no working display exists, start one:

   ```bash
   Xvfb :77 -screen 0 1920x1080x24 +extension GLX +extension RANDR -noreset &
   ```

2. **Force LWJGL through Mesa's software path** (suppresses the
   `_dri.so` loader spam and avoids any future direct-rendering oddity
   the sandbox might add):

   ```bash
   LIBGL_ALWAYS_SOFTWARE=1
   ```

3. **Run the task** with both env vars in scope:

   ```bash
   DISPLAY=:77 LIBGL_ALWAYS_SOFTWARE=1 \
     ./gradlew testClient \
       -Dnet.minecraftforge.gradle.check.certs=false \
       --no-daemon --console=plain
   ```

4. **Confirm**: the harness now boots Minecraft to the main menu, and
   each scenario completes. A single `ClientConnectSmokeTest` should
   pass in ~45 s.

### Code example (CI / scripted run)

```bash
#!/usr/bin/env bash
set -euo pipefail

# Ensure Xvfb display is up and has a connected output.
if ! DISPLAY=:77 xrandr 2>/dev/null | grep -q '^screen connected'; then
  Xvfb :77 -screen 0 1920x1080x24 +extension GLX +extension RANDR -noreset &
  sleep 1
fi

export DISPLAY=:77
export LIBGL_ALWAYS_SOFTWARE=1

./gradlew testClient \
  -Dnet.minecraftforge.gradle.check.certs=false \
  --no-daemon --console=plain "$@"
```

## Known flakes on software GL

Under `LIBGL_ALWAYS_SOFTWARE=1` the GUI right-click → `openGui` →
`displayGuiScreen` round-trip is slower than on real GPUs. The
`build.gradle.kts` comment already calls this out as the reason
`clientForks = 1` (serialised). Even serialised, a single E2E like
`RocketBuilderGuiE2ETest.clickingScanThenBuildAssemblesRocket` may flake
once per full-pyramid run on this kind of host. **Re-running the single
failing scenario almost always passes** — example from 2026-05-18:
batch run failed on Scan→Build, isolated `--tests "*RocketBuilder*"`
re-run passed in 1 min. Treat as a known flake until a host-detected
timing tuning lands in the harness.

## Prevention

- [ ] Document the two env vars at the top of the testClient invocation
      in CI scripts; do NOT rely on the developer to remember.
- [ ] If a future test starts a fresh Xvfb itself, pass
      `+extension RANDR` AND ensure the screen has at least one
      connected output (Xvfb's default in current Debian/Ubuntu builds
      already provides one; older configs sometimes omit it).
- [ ] If LWJGL is ever upgraded past 2.9.4, re-evaluate — newer
      versions may probe XRandR more defensively.

## Related Documents

- Task: TASK-02 (`Technical Decisions → GL availability for testClient`)
- Marker:
  `.agent/.context-markers/2026-05-18-1900_merge-fix-weather-into-feature-tests.md`
  (originally noted the failure as environment, not a regression).
