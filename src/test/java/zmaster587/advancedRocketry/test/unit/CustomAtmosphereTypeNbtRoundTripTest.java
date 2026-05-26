package zmaster587.advancedRocketry.test.unit;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.atmosphere.AtmosphereRegister;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * TASK-32 3b — custom {@link AtmosphereType} NBT round-trip contract.
 *
 * <p>Companion mods extend the atmosphere system by constructing a fresh
 * {@link AtmosphereType} (or subclass) and registering it via
 * {@link AtmosphereRegister#registerAtmosphere}. Tiles that persist a
 * reference to an atmosphere — most prominently
 * {@link zmaster587.advancedRocketry.tile.atmosphere.TileAtmosphereDetector}
 * which writes/reads the {@code atmName} NBT key — depend on the
 * unlocalized-name + registry-lookup loop being lossless: write
 * {@code atmosphere.getUnlocalizedName()} to NBT, restart, read the
 * string back, query {@link AtmosphereRegister#getAtmosphere}, get the
 * SAME registered instance back.</p>
 *
 * <p>This test pins that loop end-to-end against a freshly-registered
 * <em>custom</em> atmosphere type (mirroring the companion-mod use
 * case) — not just against the stock {@code AIR} / {@code VACUUM} /
 * etc. listed in the {@code AtmosphereType} static init block.</p>
 *
 * <p>Pyramid layer: testUnit. No world / server needed; the registry is
 * a process-wide singleton.</p>
 */
public class CustomAtmosphereTypeNbtRoundTripTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    /**
     * Pin: registering a custom {@link AtmosphereType} makes it
     * resolvable via {@link AtmosphereRegister#getAtmosphere}, AND the
     * registry returns the SAME instance (not a copy). The instance-
     * identity pin matters because consumers compare atmospheres with
     * {@code ==} or {@code instanceof} in some branches (e.g.
     * {@code AtmosphereType.LOWOXYGEN}).
     */
    @Test
    public void customAtmosphereResolvesByUnlocalizedNameViaRegistry() {
        // Unique name — avoid collisions with stock types or with
        // other test classes that may also register custom types in the
        // same harness.
        String name = "task32CustomTestAtmosphere";
        AtmosphereType custom = new AtmosphereType(false, true, name);
        AtmosphereRegister.getInstance().registerAtmosphere(custom);

        IAtmosphere resolved = AtmosphereRegister.getInstance().getAtmosphere(name);
        assertNotNull("getAtmosphere on a registered unlocalized-name must "
                        + "resolve (not fall back to AIR) — companion mods "
                        + "depend on this for tile-state read-back",
                resolved);
        assertSame("registry must return the SAME instance that was "
                        + "registered (not a copy) — consumers compare "
                        + "atmospheres with == in some branches",
                custom, resolved);
        assertEquals("resolved atmosphere must report the same "
                        + "unlocalized name it was registered under",
                name, resolved.getUnlocalizedName());
    }

    /**
     * Pin: write {@code atmosphere.getUnlocalizedName()} to NBT, read it
     * back, query the registry → get the SAME registered instance.
     *
     * <p>Mirrors the production
     * {@link zmaster587.advancedRocketry.tile.atmosphere.TileAtmosphereDetector}
     * persistence loop (lines 136 + 144) but against a custom-registered
     * type, to verify companion-mod-registered atmospheres survive the
     * save → load cycle.</p>
     */
    @Test
    public void customAtmosphereSurvivesNbtNameRoundTripThroughRegistry() {
        String name = "task32CustomTestAtmosphereForNbt";
        AtmosphereType custom = new AtmosphereType(true, false, false, name);
        AtmosphereRegister.getInstance().registerAtmosphere(custom);

        // Mirror TileAtmosphereDetector.writeToNBT.
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("atmName", custom.getUnlocalizedName());

        // Mirror TileAtmosphereDetector.readFromNBT.
        String readbackName = nbt.getString("atmName");
        IAtmosphere readback = AtmosphereRegister.getInstance()
                .getAtmosphere(readbackName);
        assertSame("custom AtmosphereType must round-trip through the NBT "
                        + "unlocalized-name + registry-lookup loop intact — "
                        + "this is the save-compat contract for any tile "
                        + "that persists an atmosphere reference (e.g. "
                        + "TileAtmosphereDetector)",
                custom, readback);
    }
}
