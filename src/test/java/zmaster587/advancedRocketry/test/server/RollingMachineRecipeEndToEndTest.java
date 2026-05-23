package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

/**
 * TASK-18 — Rolling Machine end-to-end recipe contract.
 *
 * <p>Three contract pins via the shared {@link MachineRecipeEndToEndKit}
 * protocol — see kit Javadoc for shape.</p>
 */
public class RollingMachineRecipeEndToEndTest extends AbstractSharedServerTest {

    private static final String FIXTURE_KEY = "rolling-machine";
    private static final String TILE_SHORT  = "TileRollingMachine";

    @Test
    public void rollingMachineFixtureValidates() throws Exception {
        MachineRecipeEndToEndKit.runFixtureValidates(client(), FIXTURE_KEY, 400, 70, 400);
    }

    @Test
    public void rollingMachineRunsFirstRegisteredRecipe() throws Exception {
        MachineRecipeEndToEndKit.runFirstRecipeEndToEnd(client(),
                FIXTURE_KEY, TILE_SHORT, 500, 70, 400);
    }
}
