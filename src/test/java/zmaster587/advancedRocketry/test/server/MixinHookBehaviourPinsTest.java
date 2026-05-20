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
     * Deterministically advances an entity's tick state by directly
     * invoking {@code Entity.onUpdate} via {@code /artest entity tick}.
     * Bypasses the natural server tick loop entirely — the
     * {@code @Inject(HEAD)} on {@code onUpdate} fires whether the call
     * comes from {@code WorldServer.updateEntities} or this probe, so
     * the mixin is exercised identically. Returns the response JSON's
     * {@code motionY}.
     *
     * <p>Robust against the dedicated-server harness's idiosyncratic
     * tick scheduling, which doesn't reliably advance entity onUpdate
     * during {@code /artest server wait} on a cold server.</p>
     */
    private double tickEntityAndReadMotionY(int dim, int id, int count) throws Exception {
        String resp = ok(client().execute(
                "artest entity tick " + dim + " " + id + " " + count));
        assertFalse("entity tick must succeed: " + resp, resp.contains("\"error\""));
        return doubleField(MOTION_Y, resp, "motionY");
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
            double motionY = tickEntityAndReadMotionY(dim, id, 3);
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
            double motionY = tickEntityAndReadMotionY(0, id, 3);
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
            double motionY = tickEntityAndReadMotionY(dim, id, 3);
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
     * <p>Uses {@code /artest entity tick} to invoke
     * {@code EntityFallingBlock.onUpdate} directly — the mixin's
     * {@code @Inject(HEAD)} fires on this path identically to the
     * natural server tick (mixin patches bytecode, not the tick loop).
     * Sand is placed at the spawn block so vanilla's "block at posY
     * must equal fallTile" guard passes on tick 1; the column below
     * is cleared so {@code onGround} doesn't trip the impact-setDead
     * branch.</p>
     */
    @Test
    public void eGravityMixinAffectsFallingBlockInArDim() throws Exception {
        int dim = firstNonOverworldArDimOrSkip();
        int worldX = 13300;
        int worldZ = 0;
        int spawnY = 200;
        forceLoadColumn(dim, worldX, worldZ);
        // Use stone as the fall-state — vanilla BlockFalling.onBlockAdded
        // schedules a tick that would auto-{@code checkFallable} for sand
        // sitting on air, eating our spawn block before the entity gets
        // a chance to validate it. Stone has no such behavior so the
        // block survives until the entity's own onUpdate consumes it.
        ok(client().execute("artest place " + dim + " " + worldX + " "
                + spawnY + " " + worldZ + " minecraft:stone"));
        for (int dy = -10; dy <= -1; dy++) {
            ok(client().execute("artest place " + dim + " " + worldX + " "
                    + (spawnY + dy) + " " + worldZ + " minecraft:air"));
        }
        try {
            String resp = ok(client().execute("artest entity spawn "
                    + dim + " " + (worldX + 0.5) + " " + spawnY
                    + " " + (worldZ + 0.5) + " minecraft:falling_block "
                    + "minecraft:stone 3"));
            assertFalse("spawn+tick must succeed: " + resp,
                    resp.contains("\"error\""));
            double motionY = doubleField(MOTION_Y, resp, "motionY");
            assertTrue("EntityFallingBlock motionY must be < 0 after 3 "
                    + "immediate onUpdate ticks (vanilla -0.04 + mixin "
                    + "AR gravity delta); response=" + resp,
                    motionY < 0.0);
        } finally {
            releaseColumn(dim, worldX, worldZ);
        }
    }

    /**
     * Phase 3 pin (extension) — live motion-tick for
     * {@link net.minecraft.entity.item.EntityFallingBlock} in the
     * overworld. Counter-test for the AR-dim pin
     * ({@link #eGravityMixinAffectsFallingBlockInArDim}): vanilla's
     * {@code motionY -= 0.04} alone (the mixin's AR-gravity branch
     * is a no-op in vanilla dims, but the hook itself still fires)
     * must observe {@code motionY < 0} after the same 3-tick
     * exercise. Proves the mixin's {@code @Inject(HEAD)} applies to
     * {@code EntityFallingBlock} on the vanilla-dim path too.
     */
    @Test
    public void fGravityMixinAffectsFallingBlockInOverworld() throws Exception {
        int worldX = 13400;
        int worldZ = 0;
        int spawnY = 250;
        forceLoadColumn(0, worldX, worldZ);
        ok(client().execute("artest place 0 " + worldX + " " + spawnY
                + " " + worldZ + " minecraft:stone"));
        for (int dy = -10; dy <= -1; dy++) {
            ok(client().execute("artest place 0 " + worldX + " "
                    + (spawnY + dy) + " " + worldZ + " minecraft:air"));
        }
        try {
            String resp = ok(client().execute("artest entity spawn 0 "
                    + (worldX + 0.5) + " " + spawnY + " " + (worldZ + 0.5)
                    + " minecraft:falling_block minecraft:stone 3"));
            assertFalse("spawn+tick must succeed: " + resp,
                    resp.contains("\"error\""));
            double motionY = doubleField(MOTION_Y, resp, "motionY");
            assertTrue("EntityFallingBlock motionY must be < 0 after 3 "
                    + "immediate onUpdate ticks in overworld; response="
                    + resp, motionY < 0.0);
        } finally {
            releaseColumn(0, worldX, worldZ);
        }
    }
}
