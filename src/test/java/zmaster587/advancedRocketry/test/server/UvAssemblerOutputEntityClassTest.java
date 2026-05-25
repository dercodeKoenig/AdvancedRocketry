package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-22 Phase 2 — output entity-class delta between the two assemblers.
 *
 * <p>The two assemblers spawn different entity types from
 * {@code assembleRocket()}:</p>
 *
 * <ul>
 *   <li>{@link zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine#assembleRocket}
 *       → {@code new EntityRocket(...)} (ascending, crewed, orbit-capable).</li>
 *   <li>{@link zmaster587.advancedRocketry.tile.TileUnmannedVehicleAssembler#assembleRocket}
 *       → {@code new EntityStationDeployedRocket(...)} (descending, station-
 *       deployed, cargo-only).</li>
 * </ul>
 *
 * <p>Pinning the entity-class delta is the most player-visible UV contract:
 * the spawned entity's behaviour (initial launch direction, flight model,
 * passenger eligibility surface, completion path) is entirely determined by
 * which subclass instance the assembler creates. A regression that swaps
 * the {@code new} expressions — or that consolidates the two assemblers
 * onto a single {@code assembleRocket} — would either disable UV
 * altogether or break the rocket-assembler's crewed launch path.</p>
 *
 * <p>Test uses the new {@code /artest fixture uv-rocket} probe (which
 * builds a minimal UV-compatible geometry) and the existing
 * {@code /artest fixture rocket simple} probe in two distinct positions
 * (same dim, X-isolated). Both assemble paths run through
 * {@code /artest rocket assemble} which is polymorphic on the controller
 * tile's class.</p>
 */
public class UvAssemblerOutputEntityClassTest extends AbstractSharedServerTest {

    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern ENTITY_CLASS = Pattern.compile("\"entityClass\":\"([^\"]+)\"");
    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");

    /** Rocket-assembler fixture at x=5500; UV-assembler fixture at x=5700.
     *  Far enough apart to avoid scan-volume overlap (rocket bb ~6 wide × 8
     *  tall; UV bb 5×6×4). */
    private static final int CY = 64;
    private static final int CZ = 5500;
    private static final int CX_ROCKET = 5500;
    private static final int CX_UV     = 5700;

    @Test
    public void rocketAssemblerProducesEntityRocketNotStationDeployed() throws Exception {
        // Pre-clear above the launchpad — the existing rocket fixture's
        // buildAndAssemble helper does this; replicate inline because we
        // don't want that helper's package coupling here.
        exec("artest chunk warmup 0 " + ((CX_ROCKET - 2) >> 4) + " " + ((CZ - 2) >> 4)
                + " " + ((CX_ROCKET + 7) >> 4) + " " + ((CZ + 7) >> 4));
        exec("artest fill 0 " + (CX_ROCKET - 2) + " " + (CY + 1) + " " + (CZ - 2)
                + " " + (CX_ROCKET + 7) + " " + (CY + 10) + " " + (CZ + 7) + " minecraft:air");

        String fixture = exec("artest fixture rocket 0 " + CX_ROCKET + " " + CY + " " + CZ
                + " simple");
        assertTrue("rocket fixture must build: " + fixture, fixture.contains("\"ok\":true"));
        int[] builder = parseBuilder(fixture);

        String assemble = exec("artest rocket assemble 0 " + builder[0] + " "
                + builder[1] + " " + builder[2]);
        assertTrue("rocket assemble must succeed: " + assemble,
                assemble.contains("\"ok\":true"));

        int entityId = lastRocketId();
        String info = exec("artest rocket info " + entityId);
        Matcher m = ENTITY_CLASS.matcher(info);
        assertTrue("info must surface entityClass: " + info, m.find());
        String entityClass = m.group(1);
        assertTrue("rocket assembler must spawn EntityRocket "
                        + "(not EntityStationDeployedRocket); got " + entityClass,
                entityClass.endsWith(".EntityRocket"));
        assertFalse("rocket assembler must NOT collapse to UV's output class; got "
                        + entityClass,
                entityClass.contains("StationDeployedRocket"));
    }

    @Test
    public void uvAssemblerProducesEntityStationDeployedRocket() throws Exception {
        String fixture = exec("artest fixture uv-rocket 0 " + CX_UV + " " + CY + " " + CZ);
        assertTrue("uv-rocket fixture must build: " + fixture,
                fixture.contains("\"ok\":true"));
        int[] builder = parseBuilder(fixture);

        String assemble = exec("artest rocket assemble 0 " + builder[0] + " "
                + builder[1] + " " + builder[2]);
        assertTrue("UV assemble must succeed: " + assemble,
                assemble.contains("\"ok\":true"));

        int entityId = lastRocketId();
        String info = exec("artest rocket info " + entityId);
        Matcher m = ENTITY_CLASS.matcher(info);
        assertTrue("info must surface entityClass: " + info, m.find());
        String entityClass = m.group(1);
        assertTrue("UV assembler must spawn EntityStationDeployedRocket; got "
                        + entityClass,
                entityClass.endsWith(".EntityStationDeployedRocket"));
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private static int[] parseBuilder(String fixture) {
        Matcher m = BUILDER_POS.matcher(fixture);
        assertTrue("fixture missing builderPos: " + fixture, m.find());
        return new int[]{
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))};
    }

    private static int lastRocketId() throws Exception {
        String list = exec("artest rocket list 0");
        Matcher m = ROCKET_LIST_ID.matcher(list);
        int last = -1;
        while (m.find()) last = Integer.parseInt(m.group(1));
        assertTrue("no rocket ids in list: " + list, last >= 0);
        return last;
    }
}
