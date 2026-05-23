package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

/**
 * TASK-18 — Electrolyser end-to-end recipe contract.
 */
public class ElectrolyserRecipeEndToEndTest extends AbstractSharedServerTest {

    private static final String FIXTURE_KEY = "electrolyser";
    private static final String TILE_SHORT  = "TileElectrolyser";

    @Test
    public void electrolyserFixtureValidates() throws Exception {
        MachineRecipeEndToEndKit.runFixtureValidates(client(), FIXTURE_KEY, 400, 70, 400);
    }

    @Test
    public void electrolyserRunsFirstRegisteredRecipe() throws Exception {
        MachineRecipeEndToEndKit.runFirstRecipeEndToEnd(client(),
                FIXTURE_KEY, TILE_SHORT, 500, 70, 400);
    }
}
