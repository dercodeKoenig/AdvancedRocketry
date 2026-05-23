package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

/**
 * TASK-18 — Centrifuge end-to-end recipe contract.
 */
public class CentrifugeRecipeEndToEndTest extends AbstractSharedServerTest {

    private static final String FIXTURE_KEY = "centrifuge";
    private static final String TILE_SHORT  = "TileCentrifuge";

    @Test
    public void centrifugeFixtureValidates() throws Exception {
        MachineRecipeEndToEndKit.runFixtureValidates(client(), FIXTURE_KEY, 400, 70, 400);
    }

    @Test
    public void centrifugeRunsFirstRegisteredRecipe() throws Exception {
        MachineRecipeEndToEndKit.runFirstRecipeEndToEnd(client(),
                FIXTURE_KEY, TILE_SHORT, 500, 70, 400);
    }
}
