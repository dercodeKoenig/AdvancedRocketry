package zmaster587.advancedRocketry.test.unit;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
import zmaster587.advancedRocketry.util.SpacePosition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * §6.7 Space position and astronomy helpers.
 *
 * Covers SpacePosition NBT round-trip and pure-math helpers (distance, normal vector,
 * spherical projection). Star/world references go through DimensionManager which
 * requires full AR init — those branches are exercised in scenario tests, not here.
 */
public class SpacePositionTest {

    private static final double EPS = 1e-9;

    @Test
    public void spacePositionNbtRoundTrip() {
        SpacePosition position = new SpacePosition();
        position.x = 100.0;
        position.y = -42.5;
        position.z = 9001.0;
        position.yaw = Math.PI / 4;
        position.pitch = -0.25;
        position.roll = 1.5;
        position.isInInterplanetarySpace = true;

        NBTTagCompound nbt = new NBTTagCompound();
        position.writeToNBT(nbt);

        SpacePosition restored = new SpacePosition();
        restored.readFromNBT(nbt);

        assertEquals(position.x, restored.x, EPS);
        assertEquals(position.y, restored.y, EPS);
        assertEquals(position.z, restored.z, EPS);
        assertEquals(position.yaw, restored.yaw, EPS);
        assertEquals(position.pitch, restored.pitch, EPS);
        assertEquals(position.roll, restored.roll, EPS);
        assertTrue(restored.isInInterplanetarySpace);
        assertNull("star reference must not be reconstructed from NBT without a writer-side star", restored.star);
        assertNull("world reference must not be reconstructed from NBT without a writer-side world", restored.world);
    }

    @Test
    public void spacePositionNbtRoundTripDefaults() {
        SpacePosition position = new SpacePosition();
        NBTTagCompound nbt = new NBTTagCompound();
        position.writeToNBT(nbt);

        SpacePosition restored = new SpacePosition();
        restored.readFromNBT(nbt);

        assertEquals(0.0, restored.x, EPS);
        assertEquals(0.0, restored.y, EPS);
        assertEquals(0.0, restored.z, EPS);
        assertFalse(restored.isInInterplanetarySpace);
    }

    @Test
    public void readFromNbtWithoutSpacePositionTagIsNoOp() {
        SpacePosition position = new SpacePosition();
        position.x = 5.0;
        position.readFromNBT(new NBTTagCompound());

        // Position must keep its previous in-memory state when the NBT lacks the tag.
        assertEquals(5.0, position.x, EPS);
    }

    @Test
    public void distanceSquaredMatchesEuclideanDefinition() {
        SpacePosition a = new SpacePosition();
        a.x = 0; a.y = 0; a.z = 0;
        SpacePosition b = new SpacePosition();
        b.x = 3; b.y = 4; b.z = 12;

        double squared = a.distanceToSpacePosition2(b);

        // 3-4-12 Pythagorean → 13² = 169.
        assertEquals(169.0, squared, EPS);
    }

    @Test
    public void distanceSquaredIsSymmetric() {
        SpacePosition a = new SpacePosition();
        a.x = -7; a.y = 11; a.z = 3;
        SpacePosition b = new SpacePosition();
        b.x = 2;  b.y = -5; b.z = 8;

        assertEquals(a.distanceToSpacePosition2(b), b.distanceToSpacePosition2(a), EPS);
    }

    @Test
    public void normalVectorHasUnitLength() {
        SpacePosition a = new SpacePosition();
        SpacePosition b = new SpacePosition();
        b.x = 10; b.y = 0; b.z = 0;

        Vec3d normal = a.getNormalVectorTo(b);

        assertEquals(1.0, normal.x, 1e-12);
        assertEquals(0.0, normal.y, 1e-12);
        assertEquals(0.0, normal.z, 1e-12);
        assertEquals(1.0, Math.sqrt(normal.x * normal.x + normal.y * normal.y + normal.z * normal.z), 1e-12);
    }

    @Test
    public void normalVectorPointsTowardsTarget() {
        SpacePosition a = new SpacePosition();
        a.x = 1; a.y = 1; a.z = 1;
        SpacePosition b = new SpacePosition();
        b.x = 4; b.y = 5; b.z = 13;

        Vec3d normal = a.getNormalVectorTo(b);
        double length = Math.sqrt(normal.x * normal.x + normal.y * normal.y + normal.z * normal.z);
        assertEquals(1.0, length, 1e-12);
        // Component-wise, the normal must have the same sign as (b - a).
        assertTrue(normal.x > 0);
        assertTrue(normal.y > 0);
        assertTrue(normal.z > 0);
    }

    @Test
    public void getFromSphericalReturnsPointAtRequestedRadius() {
        SpacePosition origin = new SpacePosition();
        origin.x = 0; origin.y = 100; origin.z = 0;

        SpacePosition projected = origin.getFromSpherical(50.0, 0.0);

        // theta=0 ⇒ x = origin.x + cos(0)*r = 50, z = origin.z + sin(0)*r = 0.
        assertEquals(50.0, projected.x, EPS);
        assertEquals(100.0, projected.y, EPS); // y is preserved
        assertEquals(0.0, projected.z, EPS);
    }

    @Test
    public void getFromSphericalThetaPiOverTwoLandsOnZAxis() {
        SpacePosition origin = new SpacePosition();
        SpacePosition projected = origin.getFromSpherical(10.0, Math.PI / 2);

        assertEquals(0.0, projected.x, 1e-12);
        assertEquals(10.0, projected.z, 1e-12);
    }

    @Test
    public void getFromSphericalCarriesContextFields() {
        SpacePosition origin = new SpacePosition();
        origin.isInInterplanetarySpace = true;

        SpacePosition projected = origin.getFromSpherical(1.0, 0.0);

        assertTrue("interplanetary flag must propagate", projected.isInInterplanetarySpace);
    }
}
