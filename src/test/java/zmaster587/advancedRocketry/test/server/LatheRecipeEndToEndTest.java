package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

/**
 * TASK-18 — Lathe end-to-end recipe contract.
 */
public class LatheRecipeEndToEndTest extends AbstractSharedServerTest {

    private static final String FIXTURE_KEY = "lathe";
    private static final String TILE_SHORT  = "TileLathe";

    @Test
    public void latheFixtureValidates() throws Exception {
        MachineRecipeEndToEndKit.runFixtureValidates(client(), FIXTURE_KEY, 400, 70, 400);
    }

    @Test
    public void latheRunsFirstRegisteredRecipe() throws Exception {
        MachineRecipeEndToEndKit.runFirstRecipeEndToEnd(client(),
                FIXTURE_KEY, TILE_SHORT, 500, 70, 400);
    }
}
