package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

/**
 * TASK-18 — Precision Laser Etcher end-to-end recipe contract.
 */
public class PrecisionLaserEtcherRecipeEndToEndTest extends AbstractSharedServerTest {

    private static final String FIXTURE_KEY = "precision-laser-etcher";
    private static final String TILE_SHORT  = "TilePrecisionLaserEtcher";

    @Test
    public void precisionLaserEtcherFixtureValidates() throws Exception {
        MachineRecipeEndToEndKit.runFixtureValidates(client(), FIXTURE_KEY, 400, 70, 400);
    }

    @Test
    public void precisionLaserEtcherRunsFirstRegisteredRecipe() throws Exception {
        MachineRecipeEndToEndKit.runFirstRecipeEndToEnd(client(),
                FIXTURE_KEY, TILE_SHORT, 500, 70, 400);
    }
}
