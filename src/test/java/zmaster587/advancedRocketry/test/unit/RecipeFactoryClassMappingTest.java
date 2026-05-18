package zmaster587.advancedRocketry.test.unit;

import org.junit.Test;
import zmaster587.advancedRocketry.recipe.RecipeCentrifuge;
import zmaster587.advancedRocketry.recipe.RecipeChemicalReactor;
import zmaster587.advancedRocketry.recipe.RecipeCrystallizer;
import zmaster587.advancedRocketry.recipe.RecipeCuttingMachine;
import zmaster587.advancedRocketry.recipe.RecipeElectricArcFurnace;
import zmaster587.advancedRocketry.recipe.RecipeElectrolyser;
import zmaster587.advancedRocketry.recipe.RecipeLathe;
import zmaster587.advancedRocketry.recipe.RecipePrecisionAssembler;
import zmaster587.advancedRocketry.recipe.RecipePrecisionLaserEtcher;
import zmaster587.advancedRocketry.recipe.RecipeRollingMachine;
import zmaster587.advancedRocketry.tile.multiblock.machine.TileCentrifuge;
import zmaster587.advancedRocketry.tile.multiblock.machine.TileChemicalReactor;
import zmaster587.advancedRocketry.tile.multiblock.machine.TileCrystallizer;
import zmaster587.advancedRocketry.tile.multiblock.machine.TileCuttingMachine;
import zmaster587.advancedRocketry.tile.multiblock.machine.TileElectricArcFurnace;
import zmaster587.advancedRocketry.tile.multiblock.machine.TileElectrolyser;
import zmaster587.advancedRocketry.tile.multiblock.machine.TileLathe;
import zmaster587.advancedRocketry.tile.multiblock.machine.TilePrecisionAssembler;
import zmaster587.advancedRocketry.tile.multiblock.machine.TilePrecisionLaserEtcher;
import zmaster587.advancedRocketry.tile.multiblock.machine.TileRollingMachine;
import zmaster587.libVulpes.recipe.RecipeMachineFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7 — TASK-02 Phase 5.
 *
 * Each {@code Recipe*} class is a thin {@code RecipeMachineFactory} subclass
 * whose only job is to bind the parsed recipe JSON to a specific tile
 * machine via {@link RecipeMachineFactory#getMachine()}. A typo (wrong tile
 * class) would silently route recipes to the wrong machine — recipes "stop
 * working" with no error. Pin the mapping here so any future rename surfaces
 * immediately.
 */
public class RecipeFactoryClassMappingTest {

    @Test
    public void recipeLatheBindsToTileLathe() {
        assertBinding(new RecipeLathe(), TileLathe.class);
    }

    @Test
    public void recipeCentrifugeBindsToTileCentrifuge() {
        assertBinding(new RecipeCentrifuge(), TileCentrifuge.class);
    }

    @Test
    public void recipeCrystallizerBindsToTileCrystallizer() {
        assertBinding(new RecipeCrystallizer(), TileCrystallizer.class);
    }

    @Test
    public void recipeCuttingMachineBindsToTileCuttingMachine() {
        assertBinding(new RecipeCuttingMachine(), TileCuttingMachine.class);
    }

    @Test
    public void recipeElectricArcFurnaceBindsToTileElectricArcFurnace() {
        assertBinding(new RecipeElectricArcFurnace(), TileElectricArcFurnace.class);
    }

    @Test
    public void recipeElectrolyserBindsToTileElectrolyser() {
        assertBinding(new RecipeElectrolyser(), TileElectrolyser.class);
    }

    @Test
    public void recipeChemicalReactorBindsToTileChemicalReactor() {
        assertBinding(new RecipeChemicalReactor(), TileChemicalReactor.class);
    }

    @Test
    public void recipePrecisionAssemblerBindsToTilePrecisionAssembler() {
        assertBinding(new RecipePrecisionAssembler(), TilePrecisionAssembler.class);
    }

    @Test
    public void recipePrecisionLaserEtcherBindsToTilePrecisionLaserEtcher() {
        assertBinding(new RecipePrecisionLaserEtcher(), TilePrecisionLaserEtcher.class);
    }

    @Test
    public void recipeRollingMachineBindsToTileRollingMachine() {
        assertBinding(new RecipeRollingMachine(), TileRollingMachine.class);
    }

    private static void assertBinding(RecipeMachineFactory factory, Class<?> expected) {
        Class<?> bound = factory.getMachine();
        assertNotNull("getMachine() returned null for " + factory.getClass().getSimpleName(), bound);
        assertEquals(
                "Recipe factory " + factory.getClass().getSimpleName()
                        + " bound to unexpected tile class — recipes would silently route to the wrong machine",
                expected, bound);
        // Each Recipe* extends RecipeMachineFactory directly; preserve the inheritance shape
        // so the recipe-loader's instanceof check keeps working.
        assertTrue("Recipe factory " + factory.getClass().getSimpleName()
                        + " no longer extends RecipeMachineFactory",
                RecipeMachineFactory.class.isAssignableFrom(factory.getClass()));
    }
}
