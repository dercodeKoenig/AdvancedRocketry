package zmaster587.advancedRocketry.test.server;

import org.junit.Assume;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-08-mixin Phase 3 — behavioural pins for the surviving hooks after the
 * ASM-coremod → Mixin rewrite.
 *
 * <h2>Why this file is short</h2>
 *
 * <p>The Mixin annotation processor statically resolves every target at
 * compile time, and the runtime fails-loud (with {@code required: true}
 * in {@code mixins.advancedrocketry.json}) when any target can't be applied
 * — so the silent-no-op regression mode the original {@code IClassTransformer}
 * allowed is structurally impossible.</p>
 *
 * <p>The existing testServer suite already covers every surviving hook
 * surface indirectly:</p>
 *
 * <ul>
 *   <li>{@link AtmosphereOxygenSmokeTest},
 *       {@link zmaster587.advancedRocketry.test.server.OxygenVentSmokeTest}
 *       — sealed-volume cycle through
 *       {@link zmaster587.advancedRocketry.mixin.MixinWorldSetBlockState}.</li>
 *   <li>{@link RocketDescentLandingTest},
 *       {@link RocketFlightFailureModesTest} — rocket and base
 *       {@code Entity.onUpdate} tick path through
 *       {@link zmaster587.advancedRocketry.mixin.MixinEntityGravity}'s
 *       {@code Entity.class} target.</li>
 *   <li>Various multiblock placement tests
 *       ({@link OrbitalLaserDrillMultiblockTest}, etc.) —
 *       3-arg {@code World.setBlockState} through the same mixin.</li>
 * </ul>
 *
 * <p>What's left to pin explicitly:</p>
 *
 * <ul>
 *   <li>The {@code @Redirect} on {@code Container.canInteractWith} inside
 *       {@code EntityPlayer(MP).onUpdate}
 *       ({@link zmaster587.advancedRocketry.mixin.MixinEntityPlayerInventoryAccess},
 *       {@link zmaster587.advancedRocketry.mixin.MixinEntityPlayerMPInventoryAccess})
 *       — needs a real {@code EntityPlayer} GUI session and lives in
 *       TASK-10b (testClient e2e).</li>
 *   <li>This file: a focused single-frame check that the
 *       {@code setBlockState} hook completes without throwing, so any
 *       mapping-snapshot drift that breaks the
 *       {@code AtmosphereHandler.onBlockChange} dispatch surfaces here as a
 *       cheap fast-failing test rather than as a cascade across the full
 *       atmosphere suite.</li>
 * </ul>
 *
 * <p>The multi-target {@code @Mixin} list on
 * {@link zmaster587.advancedRocketry.mixin.MixinEntityGravity}
 * ({@code Entity, EntityFallingBlock, EntityMinecart, EntityTNTPrimed}) is
 * caught by Mixin AP at compile time if any of the four targets is
 * unresolvable.</p>
 */
public class MixinHookBehaviourPinsTest extends AbstractSharedServerTest {

    private static final Pattern AR_DIMS_ARRAY =
            Pattern.compile("\"arDimensions\":\\[([^]]*)]");

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private int firstNonOverworldArDimOrSkip() throws Exception {
        String joined = ok(client().execute("artest dim list"));
        Assume.assumeFalse("No AR dimensions registered",
                joined.contains("\"arDimensions\":[]"));
        Matcher m = AR_DIMS_ARRAY.matcher(joined);
        assertTrue("could not parse arDimensions array: " + joined, m.find());
        for (String part : m.group(1).split(",")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            int dim = Integer.parseInt(t);
            if (dim != 0) return dim;
        }
        Assume.assumeTrue("Only overworld is registered as AR planet", false);
        return -1;
    }

    /**
     * Phase 3 pin for
     * {@link zmaster587.advancedRocketry.mixin.MixinWorldSetBlockState}.
     *
     * <p>Pokes a single block in an AR dim (which routes through the
     * 3-arg {@code World.setBlockState} that the mixin hooks at
     * {@code @At("RETURN")}) and re-queries the atmosphere subsystem.
     * Failure mode being ruled out: the mixin's
     * {@code AtmosphereHandler.onBlockChange} dispatch fails or throws —
     * which would corrupt {@code setBlockState} on every tick of every
     * world. A hard mixin-application failure manifests at server startup
     * (covered by {@code required: true}); a subtler runtime
     * NPE/dispatch-misalignment would surface here as the atmosphere query
     * returning an error or the {@code place} probe failing.</p>
     *
     * <p>The pass criterion is deliberately weak — the existing
     * {@link AtmosphereOxygenSmokeTest} suite covers the rich sealed-volume
     * semantics. This pin's only job is "does the hook fire without
     * blowing up the world?" so it stays fast and stable under future
     * mapping drift.</p>
     */
    @Test
    public void setBlockStateMixinHookCompletesWithoutThrowing() throws Exception {
        int dim = firstNonOverworldArDimOrSkip();
        // Each /artest place call invokes 2-arg World.setBlockState which
        // delegates to the 3-arg overload the mixin hooks. We do four
        // distinct placements at a single column to ensure the mixin path
        // runs both on air→solid AND solid→air transitions (atmosphere
        // recomputes both ways).
        String r1 = ok(client().execute(
                "artest place " + dim + " 12000 100 0 minecraft:stone"));
        assertFalse("place 1 must succeed: " + r1, r1.contains("\"error\""));
        String r2 = ok(client().execute(
                "artest place " + dim + " 12000 100 0 minecraft:air"));
        assertFalse("place 2 must succeed: " + r2, r2.contains("\"error\""));
        String r3 = ok(client().execute(
                "artest place " + dim + " 12000 101 0 minecraft:glass"));
        assertFalse("place 3 must succeed: " + r3, r3.contains("\"error\""));
        String r4 = ok(client().execute(
                "artest place " + dim + " 12000 101 0 minecraft:air"));
        assertFalse("place 4 must succeed: " + r4, r4.contains("\"error\""));

        // Re-query atmosphere subsystem at the touched position. The mixin
        // notified AtmosphereHandler on each setBlockState; if that path
        // misaligned (mapping drift, wrong descriptor), the atmosphere
        // handler would be in an inconsistent state — the get probe would
        // surface an error or unhandled exception.
        String atmoInfo = ok(client().execute(
                "artest atmosphere get " + dim + " 12000 100 0"));
        assertFalse("atmosphere get must succeed (mixin hook hot path): "
                + atmoInfo, atmoInfo.contains("\"error\""));
    }

    // Note: a runtime "gravity hook ticks clean" pin was prototyped here and
    // dropped: a freshly-started dedicated server holds the overworld tick
    // loop idle for several seconds while it spools planet configs +
    // unloads vanilla nether/end, and forcing a chunk anchor wasn't enough
    // to drive natural ticking from a cold-start. The runtime gravity hook
    // is covered transitively by:
    //
    //   • RocketDescentLandingTest — descent/landing requires real
    //     server-thread ticks on EntityRocket.onUpdate, whose AR gravity
    //     branch hits the same applyGravity() path the mixin invokes.
    //   • PerDimensionWeatherIsolationTest — depends on AR-dim natural
    //     ticking, which would stall if applyGravity NPE'd per-entity.
    //
    // Adding a from-scratch behavioural pin for the gravity hook is
    // tracked under TASK-10/10b as part of the next testServer harness
    // cycle if those existing tests ever regress.
}
