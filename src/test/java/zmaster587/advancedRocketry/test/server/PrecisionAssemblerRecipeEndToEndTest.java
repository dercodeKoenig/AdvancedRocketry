package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

/**
 * TASK-26 — Precision Assembler end-to-end recipe contract.
 *
 * <p>Wildcard-structure machine: the structure array has NO explicit
 * hatch chars at all — every hatch slot is a {@code '*'} wildcard. The
 * test relies on {@code lookupWildcardMachineOverrides} (TASK-26 probe
 * extension) to overlay all three role hatches (I, O, P) onto the
 * front-row wildcards on the bottom layer.</p>
 *
 * <p>Shape mirrors the 7 TASK-18 machines via {@link MachineRecipeEndToEndKit}.</p>
 */
public class PrecisionAssemblerRecipeEndToEndTest extends AbstractSharedServerTest {

    private static final String FIXTURE_KEY = "precision-assembler";
    private static final String TILE_SHORT  = "TilePrecisionAssembler";

    @Test
    public void precisionAssemblerFixtureValidates() throws Exception {
        MachineRecipeEndToEndKit.runFixtureValidates(client(), FIXTURE_KEY, 400, 70, 400);
    }

    @Test
    public void precisionAssemblerRunsFirstRegisteredRecipe() throws Exception {
        MachineRecipeEndToEndKit.runFirstRecipeEndToEnd(client(),
                FIXTURE_KEY, TILE_SHORT, 500, 70, 400);
    }
}
