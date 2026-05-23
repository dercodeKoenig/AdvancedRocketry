package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

/**
 * TASK-26 — Electric Arc Furnace end-to-end recipe contract.
 *
 * <p>Wildcard-structure machine: structure declares hatch slots via
 * {@code '*'} wildcards, so the test relies on
 * {@code lookupWildcardMachineOverrides} (TASK-26 probe extension) to
 * overlay concrete libVulpes I/O hatches at the chosen wildcard cells.
 * 'P' is already present as an explicit char in the structure top layer
 * and is picked up by the generic structure scan.</p>
 *
 * <p>Shape mirrors the 7 TASK-18 machines via {@link MachineRecipeEndToEndKit}.</p>
 */
public class ArcFurnaceRecipeEndToEndTest extends AbstractSharedServerTest {

    private static final String FIXTURE_KEY = "arc-furnace";
    private static final String TILE_SHORT  = "TileElectricArcFurnace";

    @Test
    public void arcFurnaceFixtureValidates() throws Exception {
        MachineRecipeEndToEndKit.runFixtureValidates(client(), FIXTURE_KEY, 400, 70, 400);
    }

    @Test
    public void arcFurnaceRunsFirstRegisteredRecipe() throws Exception {
        MachineRecipeEndToEndKit.runFirstRecipeEndToEnd(client(),
                FIXTURE_KEY, TILE_SHORT, 500, 70, 400);
    }
}
