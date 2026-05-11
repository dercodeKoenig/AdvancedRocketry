package zmaster587.advancedRocketry.test.unit;

import org.junit.Test;
import zmaster587.advancedRocketry.api.ARConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * §6.3 Configuration default-value stability.
 *
 * Pure construction tests — not exercising loadPreInit (that depends on Forge
 * Configuration files and the mod loader). Verifies invariants of a freshly
 * constructed configuration so accidental field-removal or default-flip is caught.
 */
public class ARConfigurationTest {

    @Test
    public void defaultConfigLoadsWithoutNulls() {
        ARConfiguration cfg = new ARConfiguration();

        // Required collections must be eager-initialized so nothing NPE-s before loadPreInit.
        assertNotNull(cfg.bypassEntity);
        assertNotNull(cfg.torchBlocks);
        assertNotNull(cfg.blackListRocketBlocks);
        assertNotNull(cfg.standardGeodeOres);
        assertNotNull(cfg.standardLaserDrillOres);
        assertNotNull(cfg.laserBlackListDims);
        assertNotNull(cfg.initiallyKnownPlanets);
        assertNotNull(cfg.asteroidTypes);
    }

    @Test
    public void rocketConfigDefaultsStable() {
        ARConfiguration cfg = new ARConfiguration();

        // Stability snapshot — anyone changing these defaults must update this test
        // intentionally so save/balance regressions are visible in a PR.
        assertEquals(1000, cfg.orbit);
        assertEquals(true, cfg.rocketRequireFuel);
        assertEquals(true, cfg.canBeFueledByHand);
        assertEquals(10, cfg.fuelPointsPer10Mb);
    }

    @Test
    public void stationConfigDefaultsStable() {
        ARConfiguration cfg = new ARConfiguration();

        assertEquals(1024, cfg.stationSize);
        assertEquals(1000, cfg.stationClearanceHeight);
        assertEquals(-2, cfg.spaceDimId);
    }

    @Test
    public void oxygenConfigDefaultsStable() {
        ARConfiguration cfg = new ARConfiguration();

        assertEquals(true, cfg.enableOxygen);
        assertEquals(true, cfg.enableNausea);
    }

    @Test
    public void planetConfigDefaultsStable() {
        ARConfiguration cfg = new ARConfiguration();

        // The Moon's dimension id starts unset (Constants.INVALID_PLANET) until config
        // assigns it. Assertion is an "invalid" sentinel, not a number.
        assertTrue("MoonId must default to a sentinel, not a real dim id", cfg.MoonId < 0 || cfg.MoonId == 0 || cfg.MoonId == Integer.MIN_VALUE);
    }

    @Test
    public void getCurrentConfigReturnsSingleton() {
        ARConfiguration first = ARConfiguration.getCurrentConfig();
        ARConfiguration second = ARConfiguration.getCurrentConfig();
        assertTrue("getCurrentConfig must return the same singleton", first == second);
    }

    @Test
    public void cloneConstructorCopiesFields() {
        ARConfiguration src = new ARConfiguration();
        src.orbit = 4242;
        src.stationSize = 256;

        ARConfiguration copy = new ARConfiguration(src);
        assertEquals(4242, copy.orbit);
        assertEquals(256, copy.stationSize);
    }
}
