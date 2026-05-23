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

    /** Fixed in TASK-12 (bug #7). The {@code createAutoGennedRecipes}
     *  call that hit Forge's frozen recipe registry was removed from
     *  the runtime reload path; init-time registration handles it
     *  once. XML hot-reload now succeeds. */
    @Test
    public void reloadRecipesEmitsSuccessConfirmationMessage() throws Exception {
        String resp = exec("ar reloadRecipes");
        assertTrue("reloadRecipes must emit success confirmation — got: " + resp,
                resp.contains("Recipes reloaded"));
        assertTrue("must NOT emit the catch-branch error envelope — got: " + resp,
                !resp.contains("Serious error has occurred"));
    }

    private static final Pattern CUTTING_COUNT =
            Pattern.compile("\"TileCuttingMachine\":(\\d+)");

    private int cuttingMachineRecipeCount() throws Exception {
        String summary = exec("artest machine recipes-summary");
        Matcher m = CUTTING_COUNT.matcher(summary);
        assertTrue("recipes-summary must include TileCuttingMachine count: "
                + summary, m.find());
        return Integer.parseInt(m.group(1));
    }

    /** Stronger pin for bug #7: not just "chat envelope says success" but
     *  "the recipe registry actually has recipes afterwards". The reload
     *  pipeline is {@code clearAllMachineRecipes} →
     *  {@code registerAllMachineRecipes} (re-adds programmatic recipes)
     *  → {@code registerXMLRecipes} (re-loads XML from
     *  {@code config/<machine>.xml}). The {@code TileCuttingMachine} has
     *  several recipes registered at init via both paths; if reload
     *  silently drops them (e.g. clear without successful re-register),
     *  this assertion fires.
     *
     *  <p>Pin shape: post-reload count must be {@code >= pre-reload count}
     *  AND {@code > 0}. The "==" form would be stricter but is fragile
     *  against future additions that register recipes lazily before the
     *  reload but not after — the "no recipes lost" semantic is the
     *  actual contract.</p> */
    @Test
    public void reloadRecipesPreservesProgrammaticAndXmlRecipesForCuttingMachine()
            throws Exception {
        int before = cuttingMachineRecipeCount();
        assertTrue("pre-condition: TileCuttingMachine must have recipes "
                + "registered at init (got " + before + ")", before > 0);

        String resp = exec("ar reloadRecipes");
        assertTrue("reload must succeed: " + resp,
                resp.contains("Recipes reloaded"));

        int after = cuttingMachineRecipeCount();
        assertTrue("post-reload count " + after + " must be >= pre-reload "
                        + "count " + before + " — no recipes silently dropped",
                after >= before);
        assertTrue("post-reload count must remain > 0 (reload must actually "
                        + "re-register, not just clear)",
                after > 0);
    }
}
