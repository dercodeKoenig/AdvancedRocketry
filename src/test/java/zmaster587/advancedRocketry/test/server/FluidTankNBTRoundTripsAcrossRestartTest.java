package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import com.github.stannismod.forge.testing.server.RealDedicatedServerHarness;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10 Phase 1 (A2 remainder) — fluid-tank NBT round-trip across restart.
 *
 * <p>Boot 1: place an {@code advancedrocketry:liquidTank} ({@code TileFluidTank}),
 * inject 7 500 mB of oxygen, close the harness WITHOUT cleanup. Boot 2 on the
 * same workDir: probe the same coordinates, assert the tile still reports
 * 7 500 mB of oxygen.</p>
 *
 * <p>This pins the {@code FluidTank.writeToNBT} / {@code readFromNBT} chain
 * end-to-end through the real save/load path (chunk save → region file →
 * chunk load → tile NBT). A regression in libVulpes' fluid-tank NBT format,
 * or in AR's {@code writeToNBTHelper} override
 * ({@link zmaster587.advancedRocketry.tile.TileFluidTank}) would lose the
 * fluid on world reload — a silent gameplay break.</p>
 *
 * <p>Uses the same two-boot pattern as {@link PersistenceRestartSmokeTest}.</p>
 */
public class FluidTankNBTRoundTripsAcrossRestartTest {

    private static final Pattern FLUID_NAME = Pattern.compile("\"fluid\":\"([^\"]+)\"");
    private static final Pattern FLUID_AMOUNT = Pattern.compile("\"amount\":(\\d+)");

    /** Tank position — far enough from spawn that no other tile collides. */
    private static final int TX = 2400;
    private static final int TY = 64;
    private static final int TZ = 2400;
    /** Amount injected; below libVulpes' default tank capacity (16 000 mB)
     *  so {@code fluid inject} doesn't clamp and we can read it back exactly. */
    private static final int INJECT_AMOUNT = 7500;

    private Path workDir;
    private RealDedicatedServerHarness firstBoot;
    private RealDedicatedServerHarness secondBoot;

    @Before
    public void prepareWorkDir() throws Exception {
        Assume.assumeTrue(
                "Server harness disabled — set -D"
                        + AbstractHeadlessServerTest.PROP_HARNESS_ENABLED + "=true",
                Boolean.parseBoolean(System.getProperty(
                        AbstractHeadlessServerTest.PROP_HARNESS_ENABLED, "false")));
        workDir = Files.createTempDirectory("forge-server-fluidtank-nbt-restart-");
    }

    @After
    public void closeAll() throws Exception {
        if (firstBoot != null) firstBoot.close();
        if (secondBoot != null) secondBoot.close();
    }

    @Test
    public void liquidTankRetainsOxygenContentAcrossRestart() throws Exception {
        // ─────── Boot 1: place tank, inject oxygen, save & shut down ───────
        firstBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/false);

        String place = String.join("\n", firstBoot.client().execute(
                "artest place 0 " + TX + " " + TY + " " + TZ + " advancedrocketry:liquidTank"));
        assertTrue("liquidTank place failed: " + place,
                place.contains("\"placed\":true"));

        String preInject = String.join("\n", firstBoot.client().execute(
                "artest fluid stored 0 " + TX + " " + TY + " " + TZ));
        assertTrue("liquidTank must expose IFluidHandler capability: " + preInject,
                preInject.contains("\"hasFluid\":true"));

        String inject = String.join("\n", firstBoot.client().execute(
                "artest fluid inject 0 " + TX + " " + TY + " " + TZ + " oxygen " + INJECT_AMOUNT));
        assertTrue("fluid inject failed: " + inject, inject.contains("\"ok\":true"));

        // Verify the inject landed in-memory before we save the world.
        String storedBefore = String.join("\n", firstBoot.client().execute(
                "artest fluid stored 0 " + TX + " " + TY + " " + TZ));
        String fluidBefore = matchOrFail(FLUID_NAME, storedBefore, "fluidName (boot 1)");
        int amountBefore = Integer.parseInt(
                matchOrFail(FLUID_AMOUNT, storedBefore, "amount (boot 1)"));
        assertTrue("expected non-empty oxygen tank after inject — fluid=" + fluidBefore
                        + " amount=" + amountBefore + " response=" + storedBefore,
                fluidBefore.toLowerCase().contains("oxygen") && amountBefore > 0);

        // The dedicated-server shutdown path drives a full chunk-save before
        // the JVM exits, so closing the harness here is the genuine save path
        // that a player /stop would invoke.
        firstBoot.close();
        firstBoot = null;

        // ─────── Boot 2: reload the same workDir, re-probe the tank ────────
        secondBoot = RealDedicatedServerHarness.startWith(workDir, /*cleanupOnClose=*/true);

        // The chunk at (TX>>4, TZ>>4) may not be force-loaded after restart;
        // a /artest probe at the tank's exact coords pulls the chunk into
        // memory before reading.
        String storedAfter = String.join("\n", secondBoot.client().execute(
                "artest fluid stored 0 " + TX + " " + TY + " " + TZ));
        assertTrue("liquidTank must still expose IFluidHandler after restart: " + storedAfter,
                storedAfter.contains("\"hasFluid\":true"));

        String fluidAfter = matchOrFail(FLUID_NAME, storedAfter, "fluidName (boot 2)");
        int amountAfter = Integer.parseInt(
                matchOrFail(FLUID_AMOUNT, storedAfter, "amount (boot 2)"));

        // Exact-match: NBT format must round-trip lossless.
        assertEquals("fluid name lost across restart (was " + fluidBefore + "): " + storedAfter,
                fluidBefore, fluidAfter);
        assertEquals("fluid amount lost across restart (was " + amountBefore + "): " + storedAfter,
                amountBefore, amountAfter);
    }

    private static String matchOrFail(Pattern p, String s, String label) {
        Matcher m = p.matcher(s);
        assertTrue("could not parse " + label + " from response: " + s, m.find());
        return m.group(1);
    }
}
