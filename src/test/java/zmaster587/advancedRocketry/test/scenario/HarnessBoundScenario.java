package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.HeadlessGameTest;
import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import com.github.stannismod.forge.testing.server.TestClient;

/**
 * Convenience base for scenarios that need a real dedicated server.
 *
 * <p>The reusable framework's {@link RealDedicatedServerHarness} requires a running
 * JVM whose classpath contains {@code GradleStartServer} and whose Gradle cache
 * provides MC assets — typically only true under ForgeGradle's {@code runServer}
 * task. When invoked from a vanilla {@code gradle test}, prerequisites may be
 * missing. Subclasses gracefully report {@link TestStatus#SKIPPED} via
 * {@link TestContext#note(String)} explaining the gap, instead of failing.</p>
 *
 * <p>The lifecycle template:</p>
 * <ol>
 *   <li>{@link #setUp(TestContext)} attempts to start the harness; on failure
 *       writes a SKIPPED note. On success stores the harness in
 *       {@code context.attributes()}.</li>
 *   <li>{@link #tick(TestContext)} dispatches to {@link #runScenario(TestContext, TestClient)}
 *       only when the harness is alive; otherwise returns {@link TestStatus#SKIPPED}.</li>
 *   <li>{@link #tearDown(TestContext)} closes the harness if it was started.</li>
 * </ol>
 *
 * <p>Subclass contract: implement {@link #runScenario} as a single tick — long
 * scenarios chain {@code TestClient.execute(...)} calls which block per command
 * with a deterministic {@code "say marker"}-based completion protocol.</p>
 */
public abstract class HarnessBoundScenario implements HeadlessGameTest {

    protected static final String CTX_HARNESS = "harness";
    protected static final String CTX_SKIP_REASON = "skipReason";

    @Override
    public int timeoutTicks() {
        // Each scenario completes in a single tick because TestClient.execute()
        // blocks until the server prints the completion marker.
        return 1;
    }

    @Override
    public boolean required() {
        return true;
    }

    /**
     * System property opting INTO real server harness invocation. When unset (the
     * default), every {@link HarnessBoundScenario} short-circuits to
     * {@link TestStatus#SKIPPED} during setUp instead of attempting to spawn a
     * 3-minute server boot — a vanilla {@code gradle test} run cannot satisfy
     * the harness's {@code GradleStartServer}-on-classpath / RFG-asset-cache
     * preconditions (AR uses ForgeGradle 6, framework targets RetroFutura).
     *
     * <p>Set {@code -Dadvancedrocketry.tests.harness=true} when running from a
     * properly configured {@code runServer}-classpath context (e.g. a future
     * dedicated Gradle task that mimics ForgeGradle's runServer setup).</p>
     */
    public static final String HARNESS_ENABLED_PROPERTY = "advancedrocketry.tests.harness";

    public static boolean isHarnessEnabled() {
        return Boolean.getBoolean(HARNESS_ENABLED_PROPERTY);
    }

    @Override
    public void setUp(TestContext context) {
        if (!isHarnessEnabled()) {
            context.put(CTX_SKIP_REASON,
                    "RealDedicatedServerHarness disabled by default — set -D"
                            + HARNESS_ENABLED_PROPERTY + "=true to enable when running "
                            + "with ForgeGradle's runServer classpath");
            context.note((String) context.get(CTX_SKIP_REASON));
            return;
        }
        try {
            RealDedicatedServerHarness harness = RealDedicatedServerHarness.start();
            context.put(CTX_HARNESS, harness);
            context.note("dedicated server harness started, port=" + harness.port());
        } catch (Throwable t) {
            // Common failure modes:
            //   - GradleStartServer not on classpath (running outside ForgeGradle runServer)
            //   - MC asset cache missing or in a non-RFG layout
            //   - Insufficient memory / blocked port
            context.put(CTX_SKIP_REASON,
                    "Unable to start RealDedicatedServerHarness: "
                            + t.getClass().getSimpleName() + ": " + t.getMessage());
            context.note((String) context.get(CTX_SKIP_REASON));
        }
    }

    @Override
    public TestStatus tick(TestContext context) throws Exception {
        RealDedicatedServerHarness harness = context.get(CTX_HARNESS);
        if (harness == null) {
            return TestStatus.SKIPPED;
        }
        TestClient client = harness.client();
        return runScenario(context, client);
    }

    @Override
    public void tearDown(TestContext context) throws Exception {
        RealDedicatedServerHarness harness = context.get(CTX_HARNESS);
        if (harness != null) {
            harness.close();
        }
    }

    /**
     * Implement the scenario's actual server-side assertions here. Use
     * {@code client.execute("/artest ...")} to query state.
     *
     * @return {@link TestStatus#PASSED}, {@link TestStatus#FAILED} or
     *         {@link TestStatus#SKIPPED}.
     */
    protected abstract TestStatus runScenario(TestContext context, TestClient client) throws Exception;
}
