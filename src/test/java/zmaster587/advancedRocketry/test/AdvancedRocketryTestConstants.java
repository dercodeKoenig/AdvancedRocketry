package zmaster587.advancedRocketry.test;

/**
 * Shared constants for AR test fixtures. Keep values stable across runs so
 * snapshot/round-trip assertions stay deterministic.
 *
 * Naming follows §4 of the SMART test plan
 * ({@code advanced_rocketry_full_test_suite_smart.md}).
 */
public final class AdvancedRocketryTestConstants {

    /** Test-only system property gating /artest probe commands and other test hooks. */
    public static final String TEST_MODE_PROPERTY = "advancedrocketry.tests";

    /** Selectable expected mode for {@code WeatherBaselineTest} (§7.5). */
    public static final String WEATHER_MODE_PROPERTY = "advancedrocketry.tests.expectedWeatherMode";
    public static final String WEATHER_MODE_SHARED = "shared";
    public static final String WEATHER_MODE_PER_DIMENSION = "per_dimension";

    /** Deterministic world seed for any worldgen scenario (§9.3). */
    public static final long DETERMINISTIC_WORLD_SEED = 0x4151544553544CL; // "AQTESTL"

    /** Stable dimension ids the test fixtures assume. */
    public static final int TEST_PLANET_EARTHLIKE_DIM = 9001;
    public static final int TEST_PLANET_VACUUM_DIM = 9002;
    public static final int TEST_PLANET_MOON_DIM = 9003;
    public static final int TEST_PLANET_RINGED_DIM = 9004;

    private AdvancedRocketryTestConstants() {}

    public static boolean isTestMode() {
        return Boolean.getBoolean(TEST_MODE_PROPERTY);
    }

    public static String expectedWeatherMode() {
        // Default per_dimension matches current production: AR planets get
        // isolated weather through CustomDerivedWorldInfo, so rain on the
        // overworld does not leak to AR dims. The SMART scaffolding originally
        // assumed "pre-B1 = shared, post-B1 = per_dimension", but that premise
        // is stale in this fork. Override with -Dadvancedrocketry.tests
        // .expectedWeatherMode=shared when verifying a regression back to
        // vanilla weather behaviour.
        String value = System.getProperty(WEATHER_MODE_PROPERTY, WEATHER_MODE_PER_DIMENSION);
        if (!WEATHER_MODE_SHARED.equals(value) && !WEATHER_MODE_PER_DIMENSION.equals(value)) {
            throw new IllegalArgumentException(
                    "Invalid -D" + WEATHER_MODE_PROPERTY + "=" + value
                            + ". Expected '" + WEATHER_MODE_SHARED + "' or '" + WEATHER_MODE_PER_DIMENSION + "'.");
        }
        return value;
    }
}
