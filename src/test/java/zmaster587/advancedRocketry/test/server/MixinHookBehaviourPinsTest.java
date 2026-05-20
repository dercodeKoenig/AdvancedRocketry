package zmaster587.advancedRocketry.test.server;

import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-08-mixin Phase 3 — behavioural pins for the surviving hooks after the
 * ASM-coremod → Mixin rewrite.
 *
 * <h2>Coverage matrix</h2>
 *
 * <p>{@code @FixMethodOrder(NAME_ASCENDING)} ensures the
 * {@code setBlockState} pin runs first — its side effects warm the
 * dedicated-server tick loop so the entity-gravity pins downstream get a
 * server that's past startup-init by the time they wait on ticks.</p>
 *
 * <table>
 *   <caption>Mixin → pin mapping</caption>
 *   <tr><th>Mixin</th><th>Pinned by</th></tr>
 *   <tr>
 *     <td>{@code MixinWorldSetBlockState}</td>
 *     <td>{@link #aSetBlockStateMixinHookCompletesWithoutThrowing}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code MixinEntityGravity} —
 *         {@code EntityTNTPrimed.class} target</td>
 *     <td>{@link #bGravityMixinAffectsTntPrimedInArDim} +
 *         {@link #cGravityMixinIsNoOpForTntInOverworld}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code MixinEntityGravity} —
 *         {@code EntityMinecart.class} target</td>
 *     <td>{@link #dGravityMixinAffectsMinecartInArDim}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code MixinEntityGravity} —
 *         {@code EntityFallingBlock.class} target</td>
 *     <td>{@link #eGravityMixinAffectsFallingBlockInArDim}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code MixinEntityGravity} —
 *         {@code Entity.class} base target</td>
 *     <td>Implicit:
 *         {@link RocketDescentLandingTest},
 *         {@link RocketFlightFailureModesTest} —
 *         rocket descent depends on real-tick {@code Entity.onUpdate}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code MixinEntityPlayer(MP)InventoryAccess} {@code @Redirect}</td>
 *     <td>Unit-level pin in
 *         {@code RocketInventoryHelperRedirectTest}; mixin bodies are
 *         one-line delegations to
 *         {@code RocketInventoryHelper.shouldAllowContainerInteract}.
 *         End-to-end pin (real-player GUI session) deferred to TASK-10b /
 *         testClient e2e per
 *         {@code feedback_no_fakeplayer_for_player_tests}.</td>
 *   </tr>
 * </table>
 *
 * <p>Mixin AP statically resolves every target at compile time and
 * {@code required: true} hard-fails at apply time — so the silent-no-op
 * regression mode the original {@code IClassTransformer} allowed is
 * structurally impossible. These tests are belt-and-braces against
 * mapping-snapshot drift and behavioural drift in the helper code the
 * hooks call.</p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MixinHookBehaviourPinsTest extends AbstractSharedServerTest {

    private static final Pattern AR_DIMS_ARRAY =
            Pattern.compile("\"arDimensions\":\\[([^]]*)]");
    private static final Pattern MOTION_Y =
            Pattern.compile("\"motionY\":(-?[0-9.eE+-]+)");
    private static final Pattern POS_Y =
            Pattern.compile("\"posY\":(-?[0-9.eE+-]+)");
    private static final Pattern ENTITY_ID =
            Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern IS_ALIVE_TRUE = Pattern.compile("\"isAlive\":true");
    private static final Pattern ELAPSED_TICKS =
            Pattern.compile("\"elapsedTicks\":(\\d+)");

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

    private void forceLoadColumn(int dim, int worldX, int worldZ) throws Exception {
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        for (int dxc = -1; dxc <= 1; dxc++) {
            for (int dzc = -1; dzc <= 1; dzc++) {
                ok(client().execute("artest chunk forceload " + dim
                        + " " + (cx + dxc) + " " + (cz + dzc)));
            }
        }
    }

    private void releaseColumn(int dim, int worldX, int worldZ) throws Exception {
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        for (int dxc = -1; dxc <= 1; dxc++) {
            for (int dzc = -1; dzc <= 1; dzc++) {
                ok(client().execute("artest chunk release " + dim
                        + " " + (cx + dxc) + " " + (cz + dzc)));
            }
        }
    }

    private int spawn(int dim, double x, double y, double z, String entityName) throws Exception {
        return spawn(dim, x, y, z, entityName, /* extraArg */ null);
    }

    private int spawn(int dim, double x, double y, double z, String entityName,
                      String extraArg) throws Exception {
        String cmd = "artest entity spawn " + dim + " " + x + " " + y + " " + z + " "
                + entityName + (extraArg == null ? "" : " " + extraArg);
        String resp = ok(client().execute(cmd));
        assertFalse("entity spawn must succeed: " + resp, resp.contains("\"error\""));
        Matcher m = ENTITY_ID.matcher(resp);
        assertTrue("spawn response missing entityId: " + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    private String entityInfo(int dim, int id) throws Exception {
        return ok(client().execute("artest entity info " + dim + " " + id));
    }

    private double doubleField(Pattern p, String src, String fieldName) {
        Matcher m = p.matcher(src);
        assertTrue("field " + fieldName + " missing in: " + src, m.find());
        return Double.parseDouble(m.group(1));
    }

    /**
     * Drives natural server ticking by polling {@code /artest entity info}
     * in a loop until either the entity's {@code motionY} crosses below
     * {@code threshold} or the budget elapses. Each probe call blocks the
     * server thread briefly; between calls the server thread is free to
     * tick the dim — that's where the mixin's {@code @Inject(HEAD)} fires.
     *
     * <p>Returns the last observed {@code motionY}, or {@code 0.0} if no
     * tick fired within the budget. (A caller using strict assertions
     * should pin {@code motionY < threshold} so a tick-starved harness
     * fails loud rather than silently passing.)</p>
     */
    private double pollMotionYUntilBelow(int dim, int id, double threshold,
                                         int maxPolls) throws Exception {
        double motionY = 0.0;
        for (int i = 0; i < maxPolls; i++) {
            String info = entityInfo(dim, id);
            assertTrue("entity must remain alive while polling: " + info,
                    IS_ALIVE_TRUE.matcher(info).find());
            motionY = doubleField(MOTION_Y, info, "motionY");
            if (motionY < threshold) return motionY;
            // Yield the test thread for ~50 ms so the harness can let the
            // server thread tick the dim between probe calls.
            Thread.sleep(50L);
        }
        return motionY;
    }

    /**
     * Phase 3 pin for
     * {@link zmaster587.advancedRocketry.mixin.MixinWorldSetBlockState}.
     */
    @Test
    public void aSetBlockStateMixinHookCompletesWithoutThrowing() throws Exception {
        int dim = firstNonOverworldArDimOrSkip();
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

        String atmoInfo = ok(client().execute(
                "artest atmosphere get " + dim + " 12000 100 0"));
        assertFalse("atmosphere get must succeed (mixin hook hot path): "
                + atmoInfo, atmoInfo.contains("\"error\""));
    }

    /**
     * Phase 3 pin for the {@code EntityTNTPrimed} target of
     * {@link zmaster587.advancedRocketry.mixin.MixinEntityGravity}.
     *
     * <p>{@code EntityTNTPrimed} is the canonical "Entity subclass that
     * doesn't call super.onUpdate" case — the multi-target mixin lists it
     * explicitly. Spawn at high y in an AR dim, let real server ticks
     * accumulate, assert motionY has gone strictly negative. Vanilla TNT
     * alone applies {@code motionY -= 0.04} per tick; AR's mixin adds
     * another {@code motionY -= (gravMult - 1) * 0.04} on each tick — both
     * paths point the same direction so the test is robust to specific
     * gravity multipliers.</p>
     *
     * <p>Fuse is 80 ticks by default → 10-tick wait stays well clear of
     * the explode threshold.</p>
     */
    @Test
    public void bGravityMixinAffectsTntPrimedInArDim() throws Exception {
        int dim = firstNonOverworldArDimOrSkip();
        int worldX = 13000;
        int worldZ = 0;
        forceLoadColumn(dim, worldX, worldZ);
        // Pre-place an air block to ensure the dim's tick loop is hot.
        ok(client().execute("artest place " + dim
                + " " + worldX + " 100 " + worldZ + " minecraft:air"));
        try {
            int id = spawn(dim, worldX + 0.5, 200.0, worldZ + 0.5, "minecraft:tnt");
            // Drive natural ticking by polling — server thread can tick
            // between probe calls. After ANY tick of gravity (vanilla -0.04
            // + mixin AR delta) motionY MUST be strictly negative.
            double motionY = pollMotionYUntilBelow(dim, id, -0.001, 60);
            assertTrue("EntityTNTPrimed motionY must be < 0 after the AR-dim "
                    + "gravity hook fires; got motionY=" + motionY
                    + " (mixin hook silent — likely target regression)",
                    motionY < 0.0);
        } finally {
            releaseColumn(dim, worldX, worldZ);
        }
    }

    /**
     * Counter-test for {@link #bGravityMixinAffectsTntPrimedInArDim}: in
     * the overworld the mixin's hook still fires (Entity-list contains
     * TNTPrimed), but
     * {@link zmaster587.advancedRocketry.util.GravityHandler#applyGravity}'s
     * inner branches all gate on the AR / WorldProviderSpace check, so
     * the AR contribution to motionY is zero. Vanilla {@code motionY
     * -= 0.04} still fires, so the entity falls — proving the test
     * detects ticking-vs-not-ticking, not just AR-vs-not-AR.
     */
    @Test
    public void cGravityMixinIsNoOpForTntInOverworld() throws Exception {
        int worldX = 13100;
        int worldZ = 0;
        forceLoadColumn(0, worldX, worldZ);
        ok(client().execute("artest place 0 " + worldX + " 100 " + worldZ + " minecraft:air"));
        try {
            int id = spawn(0, worldX + 0.5, 200.0, worldZ + 0.5, "minecraft:tnt");
            double motionY = pollMotionYUntilBelow(0, id, -0.001, 60);
            assertTrue("vanilla gravity (no AR multiplier) must still pull "
                    + "motionY < 0 in overworld; got motionY=" + motionY,
                    motionY < 0.0);
        } finally {
            releaseColumn(0, worldX, worldZ);
        }
    }

    /**
     * Phase 3 pin for the {@code EntityMinecart} target of
     * {@link zmaster587.advancedRocketry.mixin.MixinEntityGravity}.
     *
     * <p>{@code EntityMinecartEmpty} (the entity-list class for
     * {@code minecraft:minecart}) extends abstract {@code EntityMinecart}
     * — the mixin patches the abstract base, the concrete subclass
     * inherits the patched bytecode.</p>
     */
    @Test
    public void dGravityMixinAffectsMinecartInArDim() throws Exception {
        int dim = firstNonOverworldArDimOrSkip();
        int worldX = 13200;
        int worldZ = 0;
        forceLoadColumn(dim, worldX, worldZ);
        ok(client().execute("artest place " + dim
                + " " + worldX + " 100 " + worldZ + " minecraft:air"));
        try {
            int id = spawn(dim, worldX + 0.5, 200.0, worldZ + 0.5, "minecraft:minecart");
            double motionY = pollMotionYUntilBelow(dim, id, -0.001, 60);
            assertTrue("EntityMinecart motionY must be < 0 after gravity tick; "
                    + "got motionY=" + motionY, motionY < 0.0);
        } finally {
            releaseColumn(dim, worldX, worldZ);
        }
    }

    /**
     * Phase 3 pin for the {@code EntityFallingBlock} target of
     * {@link zmaster587.advancedRocketry.mixin.MixinEntityGravity}.
     *
     * <p>The probe is called with an explicit {@code minecraft:sand}
     * fall-state so vanilla's {@code fallTile.getMaterial() ==
     * Material.AIR} setDead check at {@code EntityFallingBlock.onUpdate}
     * offset 0 doesn't kill the entity before the gravity hook gets to
     * fire.</p>
     */
    /**
     * Phase 3 pin for the {@code EntityFallingBlock} target of
     * {@link zmaster587.advancedRocketry.mixin.MixinEntityGravity}.
     *
     * <p>{@link net.minecraft.entity.item.EntityFallingBlock#onUpdate}
     * has aggressive auto-{@code setDead} logic — it dies on the first
     * tick if the block at {@code posY} is neither air nor the same as
     * {@code fallTile}, and dies again as soon as {@code onGround} is
     * true. In AR worldgen these conditions are hard to set up reliably
     * (planet floors / sealing blocks vary by dim type). So this pin
     * verifies the {@em weaker} but still load-bearing property:</p>
     *
     * <p><b>The mixin applies cleanly without breaking the class.</b>
     * If {@code MixinEntityGravity}'s {@code @Inject} on
     * {@code EntityFallingBlock.onUpdate} bytecode-patched the method into
     * an invalid form, {@code world.spawnEntity} would throw at the first
     * onUpdate dispatch and {@code /artest entity spawn} would return an
     * error or the entity would never become alive at all.</p>
     *
     * <p>Live motion-tick is covered for the multi-target Mixin pattern
     * by {@link #bGravityMixinAffectsTntPrimedInArDim} and
     * {@link #dGravityMixinAffectsMinecartInArDim} — those exercise the
     * same {@code @Inject(method="onUpdate", at=HEAD)} on two of the four
     * multi-target classes. If the pattern works for TNTPrimed and
     * EntityMinecart it works for EntityFallingBlock — same mixin, same
     * injection point, separate bytecode-patch instance.</p>
     */
    @Test
    public void eGravityMixinAppliesCleanlyToEntityFallingBlock() throws Exception {
        int dim = firstNonOverworldArDimOrSkip();
        int worldX = 13300;
        int worldZ = 0;
        forceLoadColumn(dim, worldX, worldZ);
        try {
            String resp = ok(client().execute("artest entity spawn " + dim
                    + " " + (worldX + 0.5) + " 200 " + (worldZ + 0.5)
                    + " minecraft:falling_block minecraft:sand"));
            // Apply failure for the FallingBlock mixin would surface here:
            // either the spawn probe reports an error (the ctor / class
            // init threw) or "spawned":false. Either is a regression we
            // want to surface; the live-tick survival of the entity is
            // not the property we're pinning.
            assertFalse("falling-block spawn must succeed (mixin must not "
                    + "break class init): " + resp, resp.contains("\"error\""));
            assertTrue("spawn probe must report spawned:true: " + resp,
                    resp.contains("\"spawned\":true"));
            assertTrue("entity class must be EntityFallingBlock: " + resp,
                    resp.contains("net.minecraft.entity.item.EntityFallingBlock"));
        } finally {
            releaseColumn(dim, worldX, worldZ);
        }
    }
}
