package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

/**
 * TASK-18 — Crystallizer end-to-end recipe contract.
 */
public class CrystallizerRecipeEndToEndTest extends AbstractSharedServerTest {

    private static final String FIXTURE_KEY = "crystallizer";
    private static final String TILE_SHORT  = "TileCrystallizer";

    @Test
    public void crystallizerFixtureValidates() throws Exception {
        MachineRecipeEndToEndKit.runFixtureValidates(client(), FIXTURE_KEY, 400, 70, 400);
    }

    @Test
    public void crystallizerRunsFirstRegisteredRecipe() throws Exception {
        MachineRecipeEndToEndKit.runFirstRecipeEndToEnd(client(),
                FIXTURE_KEY, TILE_SHORT, 500, 70, 400);
    }
}
