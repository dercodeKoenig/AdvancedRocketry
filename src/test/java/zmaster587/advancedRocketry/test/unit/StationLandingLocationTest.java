package zmaster587.advancedRocketry.test.unit;

import org.junit.Test;
import zmaster587.advancedRocketry.util.StationLandingLocation;
import zmaster587.libVulpes.util.HashedBlockPosition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 8 (continuation, unit slice).
 *
 * {@link StationLandingLocation} is a value-ish data carrier used as the
 * key for station docking pads. Its {@code equals} is asymmetric on
 * purpose (equals to a bare {@code HashedBlockPosition} so registries
 * can be keyed by position alone). Pin the contract — the equality
 * polarity is non-obvious and a "clean up" refactor that symmetrised it
 * would silently break docking.
 */
public class StationLandingLocationTest {

    private static HashedBlockPosition at(int x, int y, int z) {
        return new HashedBlockPosition(x, y, z);
    }

    @Test
    public void getPosAndNameRoundTrip() {
        HashedBlockPosition pos = at(1, 64, 2);
        StationLandingLocation loc = new StationLandingLocation(pos, "Pad-A");
        assertEquals(pos, loc.getPos());
        assertEquals("Pad-A", loc.getName());
    }

    @Test
    public void noArgNameDefaultsToEmpty() {
        StationLandingLocation loc = new StationLandingLocation(at(0, 0, 0));
        assertEquals("", loc.getName());
    }

    @Test
    public void occupiedAndAutoLandFlagsRoundTrip() {
        StationLandingLocation loc = new StationLandingLocation(at(0, 0, 0));
        assertFalse("freshly constructed must be unoccupied", loc.getOccupied());
        assertFalse("freshly constructed must default to no auto-land", loc.getAllowedForAutoLand());

        loc.setOccupied(true);
        loc.setAllowedForAutoLand(true);
        assertTrue(loc.getOccupied());
        assertTrue(loc.getAllowedForAutoLand());
    }

    @Test
    public void equalsBetweenTwoLocationsOnlyComparesPosition() {
        // Two locations on the same pos are equal even when their names
        // differ — the equality contract is position-based so multiple
        // labels can race for the same pad without splitting the registry.
        StationLandingLocation a = new StationLandingLocation(at(10, 64, 10), "Pad-A");
        StationLandingLocation b = new StationLandingLocation(at(10, 64, 10), "Pad-B");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalsAsymmetricallyAcceptsBareHashedBlockPosition() {
        // Intentional asymmetry — see class javadoc. A registry can look
        // up "is there a pad at this pos?" by passing the bare position.
        // The reverse (HashedBlockPosition.equals(StationLandingLocation))
        // returns false because HashedBlockPosition has no reciprocal hook.
        StationLandingLocation loc = new StationLandingLocation(at(5, 64, 5), "pad");
        HashedBlockPosition pos = at(5, 64, 5);
        assertEquals("pad must equal a bare pos at the same coords", loc, pos);
    }

    @Test
    public void differentPositionsAreNotEqual() {
        StationLandingLocation a = new StationLandingLocation(at(0, 0, 0), "A");
        StationLandingLocation b = new StationLandingLocation(at(1, 0, 0), "B");
        assertNotEquals(a, b);
    }

    @Test
    public void toStringFavorsNameButFallsBackToPos() {
        StationLandingLocation named = new StationLandingLocation(at(3, 4, 5), "Bay-7");
        assertEquals("Bay-7", named.toString());

        StationLandingLocation unnamed = new StationLandingLocation(at(3, 4, 5));
        assertEquals(at(3, 4, 5).toString(), unnamed.toString());
    }
}
