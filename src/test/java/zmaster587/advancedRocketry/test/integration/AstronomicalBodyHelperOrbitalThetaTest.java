package zmaster587.advancedRocketry.test.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.common.CommonProxy;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;
import zmaster587.advancedRocketry.util.AstronomicalBodyHelper;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * §6.7 #4 — {@code orbitalAngleWrapsCorrectly}.
 *
 * <p>Lives in the integration layer because {@link AstronomicalBodyHelper#getOrbitalTheta}
 * dereferences {@code AdvancedRocketry.proxy}, and merely loading
 * {@code AdvancedRocketry.class} triggers {@code FluidRegistry.enableUniversalBucket()}
 * which only succeeds after Forge bootstrap. {@link MinecraftBootstrap#ensure()}
 * sets that up. The rest of the §6.7 pure-math tests (no proxy dereference)
 * live in {@code unit/AstronomicalBodyHelperTest}.</p>
 *
 * <p>The production formula is</p>
 * <pre>
 * theta = ((worldTime % (24000 * period)) / (24000 * period)) * 2π
 * </pre>
 * <p>The modulo is the wrap. This test pins the wrap by probing the four
 * cardinal phases plus a multi-orbit, multi-cycle case that would catch a
 * missing modulo or a long-arithmetic overflow.</p>
 */
public class AstronomicalBodyHelperOrbitalThetaTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    private Object originalProxy;
    private final ControllableProxy stub = new ControllableProxy();

    @Before
    public void installControllableProxy() throws Exception {
        Field proxyField = AdvancedRocketry.class.getDeclaredField("proxy");
        proxyField.setAccessible(true);
        originalProxy = proxyField.get(null);
        stub.fakeTime = 0L;
        proxyField.set(null, stub);
    }

    @After
    public void restoreOriginalProxy() throws Exception {
        Field proxyField = AdvancedRocketry.class.getDeclaredField("proxy");
        proxyField.setAccessible(true);
        proxyField.set(null, originalProxy);
    }

    @Test
    public void orbitalAngleWrapsCorrectly() {
        // Earth-baseline orbit: distance=100, solarSize=1.0 → period=48 (per
        // AstronomicalBodyHelper.getOrbitalPeriod docs). One full orbit takes
        // 24000 * 48 = 1_152_000 world ticks.
        final int distance = 100;
        final float solarSize = 1.0f;
        final double period = AstronomicalBodyHelper.getOrbitalPeriod(distance, solarSize);
        final long oneOrbitTicks = (long) (24000d * period);

        // Phase 0: t=0 → θ=0.
        stub.fakeTime = 0L;
        assertEquals(0.0, AstronomicalBodyHelper.getOrbitalTheta(distance, solarSize), 1e-9);

        // Phase π/2: quarter orbit.
        stub.fakeTime = oneOrbitTicks / 4;
        assertEquals(Math.PI / 2,
                AstronomicalBodyHelper.getOrbitalTheta(distance, solarSize), 1e-6);

        // Phase π: half orbit.
        stub.fakeTime = oneOrbitTicks / 2;
        assertEquals(Math.PI,
                AstronomicalBodyHelper.getOrbitalTheta(distance, solarSize), 1e-6);

        // Wrap: a full orbit → back to θ=0 (modulo collapses to 0).
        stub.fakeTime = oneOrbitTicks;
        assertEquals(0.0,
                AstronomicalBodyHelper.getOrbitalTheta(distance, solarSize), 1e-6);

        // Wrap across many orbits: 7 full + a quarter → θ should still be π/2.
        stub.fakeTime = oneOrbitTicks * 7L + oneOrbitTicks / 4L;
        assertEquals("multiple wraps must collapse to the same cardinal phase",
                Math.PI / 2,
                AstronomicalBodyHelper.getOrbitalTheta(distance, solarSize), 1e-6);

        // Stress: a world time near the long-arithmetic safe ceiling. The
        // result must still fit cleanly in [0, 2π) — no NaN, no Infinity.
        stub.fakeTime = Long.MAX_VALUE / 1024L;
        double huge = AstronomicalBodyHelper.getOrbitalTheta(distance, solarSize);
        assertTrue("θ must be a real number even at huge world times: " + huge,
                !Double.isNaN(huge) && !Double.isInfinite(huge));
        assertTrue("θ must remain in [0, 2π): " + huge,
                huge >= 0.0 && huge < 2.0 * Math.PI);
    }

    /**
     * Overrides only {@code getWorldTimeUniversal}; everything else inherits
     * from {@link CommonProxy} (and is unused by this test).
     */
    private static final class ControllableProxy extends CommonProxy {
        long fakeTime;

        @Override
        public long getWorldTimeUniversal(int id) {
            return fakeTime;
        }
    }
}
