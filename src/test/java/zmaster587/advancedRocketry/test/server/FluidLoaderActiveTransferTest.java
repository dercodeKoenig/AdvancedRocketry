package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-34 — fluid loader / unloader active transfer contract.
 *
 * <p>The pre-existing {@link RocketInfrastructureSmokeTest#fluidLoaderTransfersFluidAfterLanding}
 * pins only tile lifecycle (placement → link → 30 ticks survive). It
 * documents why active transfer was deferred: fuel-tank tiles in the
 * fixture rocket's storage chunk lose {@code FLUID_HANDLER_CAPABILITY}
 * when re-instantiated in the detached storage world.</p>
 *
 * <p>TASK-34 Phase 0 (2026-05-26) found the blocker is bypassed by the
 * {@code with-fluid-cargo} fixture variant which replaces 2 of the 6
 * fuel-tank slots with {@code advancedrocketry:liquidTank} (TileFluidTank)
 * blocks — those TEs DO survive the storage-chunk round-trip with their
 * Forge fluid capability intact (already exercised by
 * {@link MissionGasCompletionTest#gasCompletionFillsRocketFluidTilesWithConfiguredFluid}).</p>
 *
 * <p>Pins:</p>
 * <ul>
 *   <li><b>Loader → rocket transfer</b> ({@link
 *       zmaster587.advancedRocketry.tile.infrastructure.TileRocketFluidLoader#update}):
 *       fluid pre-loaded into the loader's own tank ends up in the
 *       linked rocket's storage liquidTanks after a tick budget.</li>
 *   <li><b>Unloader → rocket-drain</b> ({@link
 *       zmaster587.advancedRocketry.tile.infrastructure.TileRocketFluidUnloader#update}):
 *       fluid pre-filled into the linked rocket's storage liquidTanks
 *       gets pulled out into the unloader's tank.</li>
 * </ul>
 *
 * <p>Loose-bound assertions: "at least 1 mB moved" (the contract is the
 * direction, not exact mB/tick); both tests use a generous 60-tick
 * budget which is well above any plausible per-tick transfer cost.</p>
 */
public class FluidLoaderActiveTransferTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENT_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern TOTAL_AMOUNT =
            Pattern.compile("\"totalAmount\":(\\d+)");
    private static final Pattern TILES_WITH_CAP =
            Pattern.compile("\"tilesWithCapability\":(\\d+)");
    private static final Pattern TOTAL_FILLED =
            Pattern.compile("\"totalFilled\":(\\d+)");
    private static final Pattern LOADER_TANK_AMOUNT =
            Pattern.compile("\"fluid\":\"oxygen\",\"amount\":(\\d+)");

    /**
     * TASK-34 — loader pre-loaded with oxygen actively transfers it into
     * the linked rocket's storage liquidTanks across 60 production
     * ticks. Asserts both legs of the transfer:
     *
     * <ol>
     *   <li>Loader's own tank drained by &gt;0 mB.</li>
     *   <li>Rocket's storage gained &gt;0 mB total across its
     *       liquidTanks.</li>
     * </ol>
     *
     * <p>Doesn't pin exact mB-per-tick — production's transfer rate is
     * impl (depends on tank capacity, handler fill behaviour, etc.).</p>
     */
    @Test
    public void loaderTransfersOxygenIntoRocketStorageLiquidTanks() throws Exception {
        int lx = 1300, ly = 65, lz = 1300;
        ok("artest place 0 " + lx + " " + ly + " " + lz
                + " advancedrocketry:loader 5");

        int rocketId = assembleFixture(lx + 20, 64, lz, "with-fluid-cargo");

        // Pre-load loader's tank with oxygen. The loader IS a
        // TileFluidHatch, so `fluid inject` works against its world pos.
        String inj = exec("artest fluid inject 0 " + lx + " " + ly + " " + lz
                + " oxygen 32000");
        assertTrue("loader fluid inject must succeed: " + inj,
                inj.contains("\"ok\":true"));
        int loaderFilled = extract(inj, Pattern.compile("\"filled\":(\\d+)"));
        assertTrue("loader pre-fill must accept > 0 mB: " + inj,
                loaderFilled > 0);

        // Link rocket to loader. From this moment forward the real
        // server tick loop will fire TileRocketFluidLoader.update()
        // every server tick (~50ms) — that means by the time the test
        // thread issues its next probe command, natural ticks have
        // already done the transfer. The contract pin is therefore on
        // the END STATE, not on the delta around a synthetic force-tick
        // window: after linking + ticking, the rocket's storage
        // liquidTanks hold oxygen and the loader's own tank has
        // drained.
        ok("artest infra link 0 " + lx + " " + ly + " " + lz + " " + rocketId);

        // Force at least 60 additional ticks of the loader's update()
        // to ensure the transfer completes even on slow harnesses.
        ok("artest tile force-tick 0 " + lx + " " + ly + " " + lz + " 60");

        // Loader's tank must be drained (production transferred
        // fluid out). After full drain the tank reads
        // {"fluid":null} (no amount field) — parse defensively.
        String loaderAfter = exec("artest fluid stored 0 " + lx + " " + ly + " " + lz);
        int loaderTankAfter = parseOxygenAmountOrZero(loaderAfter);
        assertTrue("loader's own tank must have drained from "
                        + loaderFilled + " mB toward 0 after ticks; "
                        + "after=" + loaderTankAfter
                        + " loaderJson=" + loaderAfter,
                loaderTankAfter < loaderFilled);

        // Rocket storage must hold oxygen. The exact amount depends on
        // tank capacities + how many ticks fired between commands; the
        // contract pin is "rocket gained the loader's fluid", not a
        // specific mB count.
        String postStorage = exec("artest rocket storage-fluid " + rocketId);
        int storageAfter = extract(postStorage, TOTAL_AMOUNT);
        assertTrue("rocket storage liquidTanks must contain oxygen "
                        + "after loader ticks (the player-visible "
                        + "'re-fuel automation' contract); storageAfter="
                        + storageAfter + " storageJson=" + postStorage,
                storageAfter > 0);
        assertTrue("rocket storage post-state must contain the loader's "
                        + "fluid type (oxygen) specifically — guards "
                        + "against an off-target transfer; storageJson="
                        + postStorage,
                postStorage.contains("\"fluid\":\"oxygen\""));
    }

    /**
     * TASK-34 — unloader pre-linked to a rocket actively drains the
     * rocket's storage liquidTanks into its own tank across 60 ticks.
     * Inverse direction of the loader test.
     *
     * <p>Pre-fill the rocket's storage liquidTanks via the
     * {@code rocket storage-fluid-fill} probe (which iterates
     * {@code storage.getFluidTiles()} and fills each one via the
     * FLUID_HANDLER capability — same surface the loader writes
     * against, but driven directly from the test).</p>
     */
    @Test
    public void unloaderDrainsRocketStorageLiquidTanksIntoOwnTank() throws Exception {
        int ux = 1400, uy = 65, uz = 1400;
        ok("artest place 0 " + ux + " " + uy + " " + uz
                + " advancedrocketry:loader 4");

        int rocketId = assembleFixture(ux + 20, 64, uz, "with-fluid-cargo");

        // Pre-fill rocket's storage liquidTanks with oxygen via the
        // dedicated probe.
        String fillResp = exec("artest rocket storage-fluid-fill " + rocketId
                + " oxygen 16000");
        assertTrue("storage-fluid-fill must succeed: " + fillResp,
                fillResp.contains("\"ok\":true"));
        int tilesWithCap = extract(fillResp, TILES_WITH_CAP);
        int totalFilled = extract(fillResp, TOTAL_FILLED);
        assertTrue("with-fluid-cargo fixture must produce at least one "
                        + "TE with FLUID_HANDLER capability inside storage: "
                        + fillResp,
                tilesWithCap >= 1);
        assertTrue("pre-fill must succeed with > 0 mB total: " + fillResp,
                totalFilled > 0);

        // Sanity: storage-fluid probe agrees with fill result.
        String preStorage = exec("artest rocket storage-fluid " + rocketId);
        int storageBefore = extract(preStorage, TOTAL_AMOUNT);
        assertTrue("rocket storage must show the pre-filled amount "
                        + "(storage-fluid probe sanity gate): " + preStorage,
                storageBefore > 0);

        // Pre-condition: unloader's own tank starts empty (or with any
        // residual from previous test runs — only the delta matters).
        String preUnloader = exec("artest fluid stored 0 " + ux + " " + uy + " " + uz);
        int unloaderTankBefore = parseOxygenAmountOrZero(preUnloader);

        // Link rocket to unloader.
        String link = exec("artest infra link 0 " + ux + " " + uy + " " + uz
                + " " + rocketId);
        assertTrue("infra link must succeed: " + link,
                link.contains("\"linked\":true"));

        // Run the unloader's production update() for 60 ticks.
        ok("artest tile force-tick 0 " + ux + " " + uy + " " + uz + " 60");

        // Unloader's tank must have gained fluid.
        String postUnloader = exec("artest fluid stored 0 " + ux + " " + uy + " " + uz);
        int unloaderTankAfter = parseOxygenAmountOrZero(postUnloader);
        assertTrue("unloader's own tank must have gained oxygen after "
                        + "60 ticks (the player-visible 'drain returning "
                        + "rocket' contract); before=" + unloaderTankBefore
                        + " after=" + unloaderTankAfter
                        + " postUnloader=" + postUnloader,
                unloaderTankAfter > unloaderTankBefore);

        // Rocket storage must have drained.
        String postStorage = exec("artest rocket storage-fluid " + rocketId);
        int storageAfter = extract(postStorage, TOTAL_AMOUNT);
        assertTrue("rocket storage liquidTanks must have drained after "
                        + "60 unloader ticks; before=" + storageBefore
                        + " after=" + storageAfter,
                storageAfter < storageBefore);
    }

    // -- helpers ----------------------------------------------------------

    private static String exec(String cmd) throws Exception {
        return String.join("\n", client().execute(cmd));
    }

    private void ok(String cmd) throws Exception {
        String resp = exec(cmd);
        assertTrue("probe must succeed: cmd='" + cmd + "' resp=" + resp,
                resp.contains("\"ok\":true"));
    }

    private int assembleFixture(int baseX, int baseY, int baseZ, String variant)
            throws Exception {
        ok("artest fill 0 " + (baseX - 2) + " " + (baseY + 1) + " " + (baseZ - 2)
                + " " + (baseX + 7) + " " + (baseY + 10) + " " + (baseZ + 7)
                + " minecraft:air");
        String fx = exec("artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ
                + " " + variant);
        assertTrue("fixture rocket (" + variant + ") failed: " + fx,
                fx.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fx);
        assertTrue("could not parse builderPos: " + fx, bp.find());
        String assemble = exec("artest rocket assemble 0 "
                + bp.group(1) + " " + bp.group(2) + " " + bp.group(3));
        assertTrue("rocket assemble failed: " + assemble,
                assemble.contains("\"ok\":true"));
        Matcher em = ENT_ID.matcher(assemble);
        assertTrue("rocket entityId missing: " + assemble, em.find());
        return Integer.parseInt(em.group(1));
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }

    /**
     * Parse the oxygen amount from a {@code fluid stored} probe response.
     * Returns 0 when the tank has no oxygen ({@code "fluid":null} or
     * missing) — that's a valid drained-tank state, not a parse error.
     */
    private static int parseOxygenAmountOrZero(String src) {
        Matcher m = LOADER_TANK_AMOUNT.matcher(src);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
