package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.planetFloatField;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.planetIntField;

/**
 * TASK-11 Phase 1 — {@code /ar planet set | get | list} contract pins.
 *
 * <p>Each test mutates one DimensionProperties field on the overworld
 * via {@code /ar planet set 0 <field> <val>}, asserts the change is
 * observable through the independent {@code /artest planet info 0}
 * JSON reader, then restores the pre-test value in a finally block.
 * Pinning the result (the field IS the new value) rather than the
 * dispatch chain (which reflective branch fired) keeps each test ≤ 6
 * lines of body.</p>
 *
 * <p>{@code planet set} has two write paths in production: a hardcoded
 * branch for {@code atmosphereDensity} (calls
 * {@code setAtmosphereDensityDirect}) and a generic reflective branch
 * for the rest. Both paths are exercised here.</p>
 */
public class WorldCommandPlanetSetGetContractTest extends AbstractSharedServerTest {

    private static final int DIM = 0;

    @Test
    public void planetSetAtmosphereDensityIsObservableViaProbe() throws Exception {
        int before = planetIntField(DIM, "atmosphereDensity");
        try {
            exec("ar planet set " + DIM + " atmosphereDensity 73");
            assertEquals(73, planetIntField(DIM, "atmosphereDensity"));
        } finally {
            exec("ar planet set " + DIM + " atmosphereDensity " + before);
        }
    }

    @Test
    public void planetSetGravitationalMultiplierIsObservableViaProbe() throws Exception {
        double before = planetFloatField(DIM, "gravity");
        try {
            exec("ar planet set " + DIM + " gravitationalMultiplier 0.42");
            assertEquals(0.42, planetFloatField(DIM, "gravity"), 1e-4);
        } finally {
            exec("ar planet set " + DIM + " gravitationalMultiplier " + before);
        }
    }

    // averageTemperature is intentionally NOT pinned via planet set: it is
    // a derived field — DimensionProperties.getAverageTemp() recomputes it
    // from star + orbital + atmosphereDensity on every read
    // (DimensionProperties.java:2002). Pinning a write to a derived field
    // would test impl detail (whether the write-then-immediate-read window
    // is observable) rather than contract. The three real settable ints
    // (atmosphereDensity, gravitationalMultiplier, rotationalPeriod) cover
    // the same reflective branch.

    @Test
    public void planetSetRotationalPeriodIsObservableViaProbe() throws Exception {
        int before = planetIntField(DIM, "rotationalPeriod");
        try {
            exec("ar planet set " + DIM + " rotationalPeriod 17000");
            assertEquals(17000, planetIntField(DIM, "rotationalPeriod"));
        } finally {
            exec("ar planet set " + DIM + " rotationalPeriod " + before);
        }
    }

    /** {@code /ar planet get <dim> <field>} echoes the field's current
     *  value via chat. Pinning that the value text matches the probe
     *  read — cross-checks the two read paths agree, which guards
     *  against the "set-and-get both buggy in the same way" scenario. */
    @Test
    public void planetGetEchoesCurrentAtmosphereDensity() throws Exception {
        int probeValue = planetIntField(DIM, "atmosphereDensity");
        String getResp = exec("ar planet get " + DIM + " atmosphereDensity");
        assertTrue("planet get response must contain current density "
                        + probeValue + " — got: " + getResp,
                getResp.contains(String.valueOf(probeValue)));
    }

    /** {@code /ar planet list} prints one chat line per registered dim
     *  in {@code DimensionManager.getInstance()}. Overworld is always
     *  registered (AR adds it at boot — confirmed by every existing
     *  test that calls {@code /artest planet info 0}). Pin the presence
     *  of {@code DIM0} substring without pinning exact line wording. */
    @Test
    public void planetListIncludesOverworldDim() throws Exception {
        String resp = exec("ar planet list");
        assertTrue("planet list must include DIM0 — got: " + resp,
                resp.contains("DIM0"));
    }
}
