# SOP: Sharing the client harness across test methods

## Context

The testClient layer is the slowest tier in the pyramid: each `@Test`
method spawns BOTH a dedicated-server JVM AND a real client JVM (LWJGL +
OpenGL + Minecraft client). Cold-start cost ≈ 30-45 s per method, vs the
testServer tier's ~10-15 s. With 6 client tests (when `DISPLAY=:77` is
set), this is ~3-4 min wall time even at `-PclientForks=3`.

The B2 phase of TASK-03 added `AbstractSharedServerTest` for the server
tier, cutting per-class lifetime to a single server boot. This SOP
investigates whether the same pattern can apply to `AbstractClientE2ETest`,
and inventories the risks.

## Problem

A naive `AbstractSharedClientE2ETest` (BeforeClass spawns server + client,
AfterClass closes both) would in principle save ~30 s × (N-1) per class.
But the client carries state that the server doesn't:

| State source | Impact of cross-method bleed |
|---|---|
| **Server packet inbox** | If method A leaves packets unread (e.g. weather sync packet queued by the server during teardown), method B sees stale packets in its observation window. Hard to debug. |
| **GUI back-stack** | One method opens a GUI screen; if it doesn't close before the test ends, method B starts with a stale screen open. Tests that probe "is `RocketBuilderGui` showing" see a false positive. |
| **Texture / model cache** | Cumulative — won't reset across methods. Probably harmless for current assertions but a regression that mutates the cache silently leaks. |
| **`Minecraft.gameSettings`** | Mutations to render distance, particle settings, etc. persist. Tests that assert against defaults silently use the previous method's overrides. |
| **`Keyboard.areCreatedKeyEvents`** | LWJGL keyboard event buffer state. If method A simulated a keypress and didn't drain, method B reads it. |
| **World render state** | Last-rendered chunks, dimension transition queue, etc. Cross-method semantics not documented. |

The risk profile is significantly higher than the server tier where
"unique positions / fresh ids" is a clean isolation contract.

## Candidates that COULD share

After auditing the six current client tests (`src/test/java/.../client/`):

| Class | Methods | Sharing verdict |
|---|---|---|
| `ClientConnectSmokeTest` | 1 | Single method — no within-class sharing benefit. Could share with another single-method class IF combined into a suite. |
| `GuidanceComputerGuiE2ETest` | 1 | Opens a GUI; closing GUI between methods is fragile. KEEP per-method. |
| `OxygenSuitClientStateE2ETest` | 1 | Mutates player inventory / suit state. Hard to reset. KEEP per-method. |
| `PlanetSelectorGuiE2ETest` | 1 | Opens GUI + mutates planet selection. KEEP per-method. |
| `RocketBuilderGuiE2ETest` | 1 | Opens GUI + builds rocket. KEEP per-method. |
| `WeatherClientSyncE2ETest` | 1 | Mutates weather state. KEEP per-method. |

**All six are single-method classes**. The within-class sharing pattern
saves nothing today. The only win available is suite-grouping — combine
multiple classes' methods into ONE class with one shared client/server
JVM. That requires the methods to be inter-method-safe (see risks above),
which they are NOT given GUI / inventory state coupling.

## Conditional sharing — when adding NEW client tests

If a NEW client test class adds multiple methods, evaluate:

1. **Read-only methods only**: e.g. "GUI X renders without crashing for
   parameter set Y, Z, W". These can share — none mutates state.
2. **Mutations restricted to instance-local fixtures**: e.g. each method
   places its own block in a unique position and reads it back. Even
   these are higher-risk than server tier because of accumulated render
   state.
3. **Anything touching `Minecraft.player` or `Minecraft.world`**: keep
   per-method. The implicit-state surface is too broad.

## Solution sketch (NOT implemented)

If the team decides to pay the risk, the pattern would mirror
`AbstractSharedServerTest`:

```java
public abstract class AbstractSharedClientE2ETest {
    private static RealDedicatedServerHarness sharedServer;
    private static RealClientHarness sharedClient;

    @BeforeClass
    public static void start() throws Exception {
        Assume.assumeTrue(/* DISPLAY + harness props */);
        sharedServer = RealDedicatedServerHarness.start();
        sharedClient = RealClientHarness.connectTo(sharedServer);
    }

    @AfterClass
    public static void stop() throws Exception {
        if (sharedClient != null) sharedClient.close();
        if (sharedServer != null) sharedServer.close();
    }

    // Subclasses MUST:
    //   - Close every GUI they opened in their @Test method (consider
    //     @After per-method that calls "press Escape" via the keyboard
    //     event injector).
    //   - Reset Minecraft.gameSettings values they changed.
    //   - Drain the server packet inbox between methods.
}
```

The @After cleanup discipline is the load-bearing part — without it the
shared harness silently corrupts subsequent tests.

## Prevention

- [ ] Before adding a multi-method client test, document the state
      mutations per method.
- [ ] Default to extending the per-method base
      (`AbstractClientE2ETest`). Switching to a shared base requires a
      written justification in the test class's javadoc.
- [ ] If a regression surfaces in a shared-base client test, the FIRST
      hypothesis is method-order coupling — re-run the failing method in
      isolation to confirm.

## Related Documents

- [`AbstractSharedServerTest`](../../../src/test/java/zmaster587/advancedRocketry/test/server/AbstractSharedServerTest.java) — server-tier sibling pattern.
- [`client-tests-on-linux.md`](./client-tests-on-linux.md) — DISPLAY / GL setup for headless client harness.
- TASK-03 Phase B4 — original proposal (`/workspace/AdvancedRocketry/.agent/tasks/TASK-03-test-depth-and-harness-consolidation.md`).
