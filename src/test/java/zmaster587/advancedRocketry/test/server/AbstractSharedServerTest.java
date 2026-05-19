package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import com.github.stannismod.forge.testing.server.TestClient;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;

/**
 * SMART §7 — TASK-03 B1 — class-scoped harness lifecycle base class.
 *
 * <p>{@link AbstractHeadlessServerTest} starts a fresh dedicated-server JVM
 * per {@code @Test} method (its {@code @Before}/{@code @After} lifecycle).
 * For a class with N independent test methods, that's N × ~10-15 s of
 * server cold-start cost. With 136 server tests today, the total wall
 * time at {@code -Pforks=3} is ~17 min.</p>
 *
 * <p>This base class is the opt-in alternative: <strong>one</strong>
 * server JVM is started in {@code @BeforeClass} and closed in
 * {@code @AfterClass}. All {@code @Test} methods in the subclass share
 * that harness. For a 6-method class, this saves 5 × ~12 s ≈ 60 s of
 * wall time per class.</p>
 *
 * <h2>Contract for subclasses</h2>
 *
 * Every {@code @Test} method MUST be:
 *
 * <ol>
 *   <li><b>Position-isolated</b>: if the test places blocks, the
 *       positions must not collide with any other method in the same
 *       class. Convention: each method picks a unique {@code BASE_X}
 *       offset (e.g. method 1 at x=100, method 2 at x=200, etc.) or
 *       includes a hash of its method name in the position.</li>
 *   <li><b>Id-fresh</b>: stations / satellites / rockets created via
 *       probes get auto-allocated ids; subclasses must read the new id
 *       from each create response and not assume a specific id range.</li>
 *   <li><b>No state-leak between methods</b>: a method MUST NOT mutate
 *       state that another method reads as a precondition (e.g. setting
 *       atmosphere density to 0 leaks to all subsequent methods —
 *       {@link AtmosphereOxygenSmokeTest} stays on the per-method base).
 *       JUnit 4 does not guarantee method execution order.</li>
 *   <li><b>Probe-only mutations</b>: any direct world-state mutation must
 *       go through the {@code /artest} probe surface, never through
 *       Bukkit/Forge APIs reflected into the test JVM.</li>
 * </ol>
 *
 * <h2>When NOT to use this base</h2>
 *
 * <ul>
 *   <li>Persistence-restart tests (need a fresh workDir / multi-boot
 *       sequence): stay on the per-method {@link AbstractHeadlessServerTest}
 *       or manage the harness manually.</li>
 *   <li>Tests with global mutations (atmosphere density, weather state)
 *       that are hard to clean up between methods.</li>
 *   <li>Tests that depend on the server's initial registry being pristine
 *       (e.g. counting fresh registry entries).</li>
 * </ul>
 *
 * <h2>Failure isolation</h2>
 *
 * One method's hard crash (e.g. NPE in the server JVM) brings the shared
 * server down. JUnit will report ALL remaining methods in the class as
 * failed against the same root cause. This is the trade-off — keep the
 * shared base only for classes whose methods are stable AND fast.
 */
public abstract class AbstractSharedServerTest {

    private static volatile RealDedicatedServerHarness shared;

    @BeforeClass
    public static void startSharedHarness() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -D"
                        + AbstractHeadlessServerTest.PROP_HARNESS_ENABLED + "=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        // Cold-start once for the whole class.
        shared = RealDedicatedServerHarness.start();
    }

    @AfterClass
    public static void stopSharedHarness() throws Exception {
        if (shared != null) {
            try {
                shared.close();
            } finally {
                shared = null;
            }
        }
    }

    /** The shared server's command client. Safe to call from any
     *  {@code @Test} method; null between @AfterClass and the next class's
     *  @BeforeClass. */
    protected static TestClient client() {
        if (shared == null) {
            throw new IllegalStateException(
                    "Shared harness not started — @BeforeClass setup failed "
                            + "or test called from outside a JUnit lifecycle.");
        }
        return shared.client();
    }

    /** The shared harness. Available for the few cases that need the
     *  RealDedicatedServerHarness API beyond `client()`. */
    protected static RealDedicatedServerHarness harness() {
        return shared;
    }
}
