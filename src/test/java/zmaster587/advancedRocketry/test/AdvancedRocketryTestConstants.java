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
}
