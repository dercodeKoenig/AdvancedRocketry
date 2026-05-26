package zmaster587.advancedRocketry.test.unit;

import org.junit.BeforeClass;
import org.junit.Test;
import sun.misc.Unsafe;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;
import zmaster587.advancedRocketry.tile.infrastructure.TileRocketLoader;
import zmaster587.libVulpes.util.ZUtils.RedstoneState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Coverage-audit gap (2026-05-26 Tier 1 #1) — TileRocketLoader's
 * redstone-output polarity logic.
 *
 * <p>The loader emits a redstone signal indicating "rocket fully
 * loaded". The signal polarity is configurable via the
 * {@link RedstoneState} enum on the {@code state} field:</p>
 *
 * <ul>
 *   <li>{@code ON} — emit while the loader's condition is true
 *       (default — "rocket fully loaded").</li>
 *   <li>{@code INVERTED} — flip; emit while condition is false
 *       ("rocket needs loading").</li>
 *   <li>{@code OFF} — never emit.</li>
 * </ul>
 *
 * <p>Players wire the loader into hopper/comparator automation that
 * keys on this signal. A regression that swaps the polarity silently
 * breaks every such automation. The protected helper
 * {@code isStateActive(RedstoneState, boolean)} (production lines
 * 224-230) is the gate that converts the raw "loader condition" into
 * the world redstone signal — pinning its truth table catches any
 * polarity flip.</p>
 *
 * <p><b>Tier choice</b>: testUnit. The helper is a pure boolean
 * function — no world, no rocket, no item handling. Reflection lets
 * us reach the {@code protected} surface without a real
 * {@code update()} cycle.</p>
 */
public class RocketLoaderRedstonePolarityTest {

    private static Unsafe UNSAFE;
    private static Method isStateActive;
    private static TileRocketLoader fakeLoader;

    @BeforeClass
    public static void bootstrap() throws Exception {
        // SatelliteBase / LibVulpes localisation paths touched during
        // ctor need MC's Bootstrap registries.
        MinecraftBootstrap.ensure();
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        UNSAFE = (Unsafe) theUnsafe.get(null);

        // Allocate a TileRocketLoader without running its ctor — the
        // ctor builds libVulpes UI modules + sideSelectorModule that
        // pull localization keys, which is fine at testUnit tier but
        // we don't need them here. isStateActive is a pure boolean
        // function of (RedstoneState, boolean) so a bare instance is
        // enough as the dispatch target.
        fakeLoader = (TileRocketLoader) UNSAFE.allocateInstance(TileRocketLoader.class);

        // Cache the protected helper. Located on the loader class
        // itself (not inherited).
        isStateActive = TileRocketLoader.class.getDeclaredMethod(
                "isStateActive", RedstoneState.class, boolean.class);
        isStateActive.setAccessible(true);
    }

    private static boolean invoke(RedstoneState state, boolean condition) {
        try {
            return (Boolean) isStateActive.invoke(fakeLoader, state, condition);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void onStateEmitsRedstoneWhenConditionTrue() {
        assertTrue("state=ON + condition=true must emit redstone "
                + "(player-visible: 'rocket fully loaded' signal)",
                invoke(RedstoneState.ON, true));
    }

    @Test
    public void onStateStaysOffWhenConditionFalse() {
        assertFalse("state=ON + condition=false must NOT emit "
                + "(rocket not yet fully loaded)",
                invoke(RedstoneState.ON, false));
    }

    @Test
    public void invertedStateFlipsTruthOutput() {
        assertFalse("state=INVERTED + condition=true must NOT emit "
                + "(polarity flipped)",
                invoke(RedstoneState.INVERTED, true));
    }

    @Test
    public void invertedStateFlipsFalseOutput() {
        assertTrue("state=INVERTED + condition=false MUST emit "
                + "(player-visible: 'rocket needs loading' signal)",
                invoke(RedstoneState.INVERTED, false));
    }

    @Test
    public void offStateNeverEmits() {
        assertFalse("state=OFF + condition=true must NOT emit "
                + "(OFF disables the polarity logic outright)",
                invoke(RedstoneState.OFF, true));
        assertFalse("state=OFF + condition=false must NOT emit",
                invoke(RedstoneState.OFF, false));
    }

    @Test
    public void onIsTheLogicalNegationOfInvertedForBothInputs() {
        // Relational pin: ON and INVERTED produce opposite results
        // for the SAME condition input. If a regression accidentally
        // makes them equal, this fires before any single-cell test
        // hits its specific polarity.
        assertEquals("ON and INVERTED must produce opposite signals "
                        + "for condition=true",
                !invoke(RedstoneState.ON, true),
                invoke(RedstoneState.INVERTED, true));
        assertEquals("ON and INVERTED must produce opposite signals "
                        + "for condition=false",
                !invoke(RedstoneState.ON, false),
                invoke(RedstoneState.INVERTED, false));
    }
}
