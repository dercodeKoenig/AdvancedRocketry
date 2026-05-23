package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.planetExists;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.planetIntField;

/**
 * TASK-11 Phase 2 — {@code /ar planet generate | delete | reset} lifecycle.
 *
 * <p>Pins the registry-mutation contract: generate produces a new
 * {@code DimensionProperties} entry; delete removes it; reset restores
 * a dim's properties to its baseline. Each test rolls back any
 * registry changes it makes so the shared harness is left as found.</p>
 *
 * <p><b>Args note</b>: the three randomness factors must be positive —
 * production at {@code DimensionManager.java:281} calls
 * {@code random.nextInt(atmosphereFactor)} which throws
 * {@code IllegalArgumentException("bound must be positive")} on a
 * zero factor. Tests pass {@code 10 10 10}.</p>
 */
public class WorldCommandPlanetLifecycleContractTest extends AbstractSharedServerTest {

    private static final Pattern DIM_LINE = Pattern.compile("DIM(\\d+):");

    private static Set<Integer> dimIds() throws Exception {
        String list = exec("ar planet list");
        Set<Integer> ids = new HashSet<>();
        Matcher m = DIM_LINE.matcher(list);
        while (m.find()) ids.add(Integer.parseInt(m.group(1)));
        return ids;
    }

    @Test
    public void planetGenerateAddsExactlyOneEntryToRegistry() throws Exception {
        Set<Integer> before = dimIds();
        exec("ar planet generate 0 GenTestA 10 10 10");
        Set<Integer> after = dimIds();
        try {
            after.removeAll(before);
            assertEquals("planet generate must add exactly one dim — diff was " + after,
                    1, after.size());
        } finally {
            for (Integer id : after) exec("ar planet delete " + id);
        }
    }

    @Test
    public void planetGenerateNamesNewDimensionFromArg() throws Exception {
        Set<Integer> before = dimIds();
        exec("ar planet generate 0 GenTestNamed 10 10 10");
        Set<Integer> diff = dimIds();
        diff.removeAll(before);
        try {
            assertEquals(1, diff.size());
            String list = exec("ar planet list");
            assertTrue("list must include the supplied name — got: " + list,
                    list.contains("GenTestNamed"));
        } finally {
            for (Integer id : diff) exec("ar planet delete " + id);
        }
    }

    @Test
    public void planetDeleteRemovesEntryFromRegistry() throws Exception {
        Set<Integer> before = dimIds();
        exec("ar planet generate 0 GenTestDel 10 10 10");
        Set<Integer> diff = dimIds();
        diff.removeAll(before);
        assertEquals(1, diff.size());
        int newId = diff.iterator().next();
        assertTrue("precondition: planetExists must be true after generate",
                planetExists(newId));

        exec("ar planet delete " + newId);

        assertFalse("planetExists must be false after delete",
                planetExists(newId));
        assertFalse("planet list must no longer include the dim",
                dimIds().contains(newId));
    }

    /** {@code planet reset <dimId>} calls
     *  {@code DimensionProperties.resetProperties} which on the overworld
     *  baseline restores {@code atmosphereDensity = 100} (set by
     *  {@code DimensionManager} ctor line 84). Mutate to a non-default
     *  value, reset, observe baseline restored. */
    @Test
    public void planetResetRestoresAtmosphereDensityBaselineForOverworld() throws Exception {
        int original = planetIntField(0, "atmosphereDensity");
        try {
            exec("ar planet set 0 atmosphereDensity 37");
            assertEquals(37, planetIntField(0, "atmosphereDensity"));
            exec("ar planet reset 0");
            assertEquals("after reset the field must equal the AR-init baseline",
                    100, planetIntField(0, "atmosphereDensity"));
        } finally {
            // Restore to whatever the harness had before — defends against
            // a future test ordering that depends on the pre-test value.
            exec("ar planet set 0 atmosphereDensity " + original);
        }
    }
}
