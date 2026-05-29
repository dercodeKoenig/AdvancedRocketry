package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-40 (Gap E) — rocket item unloader active transfer contract.
 *
 * <p>The pre-existing {@link RocketInfrastructureSmokeTest#rocketUnloaderRemovesItemsAfterLanding}
 * pins only tile lifecycle (placement → link → 5 ticks survive); it
 * documents why the transfer was deferred ("once a chest-pre-populate
 * probe lands"). TASK-40 introduces
 * {@code rocket storage-item-fill} (mirror of TASK-34's
 * {@code storage-fluid-fill}), unblocking the active-transfer pin.</p>
 *
 * <p>Contract pinned: {@link
 * zmaster587.advancedRocketry.tile.infrastructure.TileRocketUnloader#update}
 * — items pre-injected into the rocket's storage chunk inventory tiles
 * land in the unloader's own inventory after force-tick.</p>
 *
 * <p>Reuses the {@code with-cargo} fixture variant (vanilla chest above
 * the seat in storage chunk; documented at TestProbeCommand fixture
 * dispatcher).</p>
 *
 * <p>Loose-bound: "at least 1 item moved" — the contract is the
 * direction, not exact items/tick. Production iterates the storage
 * chunk's inventory tiles each {@code update()} and moves at most one
 * stack per tick, so a 5-tick budget pinned the loader side; 10 here for
 * safety margin.</p>
 */
public class RocketItemUnloaderActiveTransferTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENT_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern TOTAL_PLACED =
            Pattern.compile("\"totalPlaced\":(\\d+)");
    private static final Pattern TILES_WITH_CAP =
            Pattern.compile("\"tilesWithCapability\":(\\d+)");

    /**
     * TASK-40 Gap E — unloader pre-linked to a rocket actively drains the
     * rocket's storage inventory tiles into its own inventory across 10
     * ticks. Inverse direction of
     * {@link RocketInfrastructureSmokeTest#rocketLoaderTransfersItemsAfterLanding}.
     *
     * <p>Pre-fill the rocket's cargo chest via the
     * {@code rocket storage-item-fill} probe (which iterates
     * {@code storage.getInventoryTiles()} and inserts items via the
     * ITEM_HANDLER capability or IInventory — same surface the loader writes
     * against, but driven directly from the test).</p>
     */
    @Test
    public void unloaderPullsItemsFromRocketStorage() throws Exception {
        int ux = 1450, uy = 65, uz = 1450;
        // Loader meta=2 → TileRocketUnloader (item unloader).
        ok("artest place 0 " + ux + " " + uy + " " + uz
                + " advancedrocketry:loader 2");

        int rocketId = assembleFixture(ux + 20, 64, uz, "with-cargo");

        // Pre-fill rocket's storage inventory tiles (the with-cargo
        // chest) with cobblestone via the dedicated probe.
        String fillResp = exec("artest rocket storage-item-fill " + rocketId
                + " minecraft:cobblestone 32");
        assertTrue("storage-item-fill must succeed: " + fillResp,
                fillResp.contains("\"ok\":true"));
        int tilesWithCap = extract(fillResp, TILES_WITH_CAP);
        int totalPlaced = extract(fillResp, TOTAL_PLACED);
        assertTrue("with-cargo fixture must produce at least one IInventory "
                        + "tile inside storage: " + fillResp,
                tilesWithCap >= 1);
        assertTrue("pre-fill must succeed with > 0 items placed: " + fillResp,
                totalPlaced > 0);

        // Sanity: storage-inventory probe agrees with fill result.
        String preStorage = exec("artest rocket storage-inventory " + rocketId);
        assertTrue("rocket storage must show the pre-filled cobblestone "
                        + "(storage-inventory probe sanity gate): " + preStorage,
                preStorage.contains("\"item\":\"minecraft:cobblestone\""));

        // Link rocket to unloader.
        String link = exec("artest infra link 0 " + ux + " " + uy + " " + uz
                + " " + rocketId);
        assertTrue("infra link must succeed: " + link,
                link.contains("\"linked\":true"));

        // Run the unloader's production update() for 60 ticks. Storage
        // chunk may contain multiple inventory tiles (engine TEs etc.)
        // that the unloader iterates first; 60 ticks comfortably cover
        // the first-empty-slot scan even on the longest TE list.
        ok("artest tile force-tick 0 " + ux + " " + uy + " " + uz + " 60");

        // Unloader's own inventory must contain at least one cobblestone
        // — that's the player-visible "drain returning rocket" contract.
        String postUnloader = exec("artest hatch read 0 " + ux + " " + uy + " " + uz);
        String postStorage = exec("artest rocket storage-inventory " + rocketId);
        assertTrue("unloader's own inventory must contain cobblestone "
                        + "after 60 ticks of update(); unloader read="
                        + postUnloader + "\n storage=" + postStorage,
                postUnloader.contains("\"item\":\"minecraft:cobblestone\""));
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
}
