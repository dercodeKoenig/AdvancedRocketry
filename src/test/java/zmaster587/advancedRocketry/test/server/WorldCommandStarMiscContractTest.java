package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-11 Phase 3 — {@code /ar star *}, {@code /ar dumpBiomes},
 * {@code /ar reloadRecipes}.
 *
 * <p>Stars are registered in AR's
 * {@code DimensionManager.getInstance()} alongside planets; the
 * default star is Sol with id=0, name="Sol", temperature=100 (set in
 * {@code DimensionManager} ctor lines 78-81). Tests assert against
 * that baseline.</p>
 */
public class WorldCommandStarMiscContractTest extends AbstractSharedServerTest {

    private static final Pattern TEMP_LINE = Pattern.compile("Temp:\\s*(-?\\d+)");

    @Test
    public void starListIncludesSolAsId0() throws Exception {
        String list = exec("ar star list");
        assertTrue("star list must include Sol — got: " + list,
                list.contains("Star ID: 0") && list.contains("Sol"));
    }

    @Test
    public void starGetTempEchoesSolBaselineTemperature() throws Exception {
        String resp = exec("ar star get temp 0");
        Matcher m = TEMP_LINE.matcher(resp);
        assertTrue("must include a Temp: line — got: " + resp, m.find());
        assertEquals("Sol baseline temperature per DimensionManager ctor",
                100, Integer.parseInt(m.group(1)));
    }

    @Test
    public void starSetTempUpdatesStellarBodyTemperature() throws Exception {
        try {
            exec("ar star set temp 0 4242");
            String resp = exec("ar star get temp 0");
            Matcher m = TEMP_LINE.matcher(resp);
            assertTrue("must include a Temp: line — got: " + resp, m.find());
            assertEquals(4242, Integer.parseInt(m.group(1)));
        } finally {
            exec("ar star set temp 0 100");
        }
    }

    @Test
    public void starGenerateRegistersNewStarObservableInList() throws Exception {
        String beforeList = exec("ar star list");
        assertTrue("baseline list must NOT yet contain the test star name",
                !beforeList.contains("GenStarA"));
        exec("ar star generate GenStarA 8000 50 50");
        String afterList = exec("ar star list");
        // No `star delete` exists in production — the new star persists for
        // the rest of the shared harness. That's fine because (a) the name
        // is unique to this test and (b) subsequent tests don't enumerate
        // by count, only by-name presence.
        assertTrue("star list must include the generated star name — got: "
                        + afterList,
                afterList.contains("GenStarA"));
    }

    /** {@code /ar dumpBiomes} writes {@code ./BiomeDump.txt} relative to
     *  the server JVM's CWD, which is the harness workdir. The file's
     *  first column is the vanilla biome id; pin its presence + the
     *  known {@code minecraft:plains} biome name (id=1 in vanilla 1.12.2). */
    @Test
    public void dumpBiomesWritesBiomeDumpFileWithVanillaPlainsBiome() throws Exception {
        Path root = harness().root();
        Path dump = root.resolve("BiomeDump.txt");
        Files.deleteIfExists(dump);
        exec("ar dumpBiomes");
        assertTrue("BiomeDump.txt must exist after the command",
                Files.exists(dump));
        String body = new String(Files.readAllBytes(dump));
        assertTrue("dump must contain minecraft:plains — got: " + body,
                body.contains("minecraft:plains"));
    }

    /** {@code /ar reloadRecipes} is broken post-init in Forge 1.12.2:
     *  Forge locks recipe registries after init, and the production path
     *  ({@code WorldCommand.java:258} → {@code RecipeHandler
     *  .createAutoGennedRecipes:122}) calls
     *  {@code ForgeRegistry.add(...)} which throws
     *  {@code IllegalStateException("The object ... is being added too
     *  late")}. The catch branch at {@code WorldCommand.java:264} fires
     *  the user-visible "Serious error has occurred! Possible recipe
     *  corruption" message.
     *
     *  <p>This test pins the CURRENT (broken) post-condition. The day
     *  production grows a pre-unfreeze step (or moves the reload to
     *  an event handler that runs while the registry is still mutable),
     *  this assertion flips and the test must be updated.</p>
     *
     *  <p>Logged in {@code .agent/tasks/README.md} bug ledger as the
     *  {@code reloadRecipes} freezing bug.</p> */
    @Test
    public void reloadRecipesEmitsErrorEnvelopeDueToFrozenRegistry_documentsKnownBug()
            throws Exception {
        String resp = exec("ar reloadRecipes");
        assertTrue("reloadRecipes currently fails post-init — expected the "
                        + "catch-branch error envelope — got: " + resp,
                resp.contains("Serious error has occurred"));
        assertTrue("must NOT emit the success confirmation while the bug "
                        + "stands — got: " + resp,
                !resp.contains("Recipes reloaded"));
    }
}
