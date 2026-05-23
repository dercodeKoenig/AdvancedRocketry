package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

/**
 * TASK-18 — Chemical Reactor end-to-end recipe contract.
 */
public class ChemicalReactorRecipeEndToEndTest extends AbstractSharedServerTest {

    private static final String FIXTURE_KEY = "chemical-reactor";
    private static final String TILE_SHORT  = "TileChemicalReactor";

    @Test
    public void chemicalReactorFixtureValidates() throws Exception {
        MachineRecipeEndToEndKit.runFixtureValidates(client(), FIXTURE_KEY, 400, 70, 400);
    }

    @Test
    public void chemicalReactorRunsFirstRegisteredRecipe() throws Exception {
        MachineRecipeEndToEndKit.runFirstRecipeEndToEnd(client(),
                FIXTURE_KEY, TILE_SHORT, 500, 70, 400);
    }
}
