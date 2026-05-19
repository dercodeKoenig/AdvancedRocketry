package zmaster587.advancedRocketry.command.test;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import zmaster587.advancedRocketry.AdvancedRocketry;

/**
 * Conditional registration entry-point for the test-only {@code /artest} command
 * tree (SMART §5).
 *
 * <p>Call from {@code AdvancedRocketry.serverStarting} (or any FMLServerStartingEvent
 * handler):</p>
 * <pre>{@code
 *   TestProbeCommandRegistration.registerIfTestMode(event);
 * }</pre>
 *
 * <p>The command is registered ONLY when the JVM was launched with
 * {@code -Dadvancedrocketry.tests=true}. In normal gameplay the helper is a no-op
 * and the command is never visible.</p>
 */
public final class TestProbeCommandRegistration {

    private static final String FLAG = "advancedrocketry.tests";

    /**
     * Framework-set flag on dedicated server JVMs spawned by
     * {@code RealDedicatedServerHarness}. AR doesn't need to forward
     * {@link #FLAG} explicitly — being in a harness-spawned server is a
     * sufficient signal to register the probes.
     */
    private static final String HARNESS_FLAG = "forge.test.server";

    private TestProbeCommandRegistration() {}

    public static boolean isTestMode() {
        return Boolean.getBoolean(FLAG) || Boolean.getBoolean(HARNESS_FLAG);
    }

    public static void registerIfTestMode(FMLServerStartingEvent event) {
        if (!isTestMode()) {
            return;
        }
        event.registerServerCommand(new TestProbeCommand());
        // TASK-07: register the rocket-event recorder at server start so
        // counters are accurate from the first rocket lifecycle event.
        TestProbeCommand.RocketEventRecorder.ensureRegistered();
        AdvancedRocketry.logger.info("Registered /artest test-only probe commands (-D" + FLAG + "=true)");
    }
}
