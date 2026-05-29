package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-38 (Gap Q) — IMiningDrill stat aggregation during rocket assembly.
 *
 * <p>{@link zmaster587.advancedRocketry.block.BlockMiningDrill} is a
 * cargo-component block (no TileEntity, no tick) consumed by the rocket
 * assembler's scan loop. Both {@code TileRocketAssemblingMachine.scanRocket}
 * (line 394) and the production-side {@code StorageChunk.recalculateStats}
 * (line 230) walk the storage chunk, sum every {@code
 * IMiningDrill.getMiningSpeed(world, pos)}, and stash the total in
 * {@code stats.setDrillingPower(sum)}.</p>
 *
 * <p>The stat then feeds {@link
 * zmaster587.advancedRocketry.entity.EntityRocket#getMissionFromInfrastructure}
 * (line 1434) and {@link
 * zmaster587.advancedRocketry.mission.MissionOreMining} — a non-zero
 * drillingPower is the player-visible "this rocket can mine ore" flag, and
 * the magnitude shapes the mission's duration formula.</p>
 *
 * <p>Contract pinned: a rocket assembled with one
 * {@code advancedrocketry:drill} block in its cargo column shows
 * {@code drillingPower > 0} on the resulting EntityRocket's StatsRocket;
 * a rocket assembled from the same fixture WITHOUT the drill block shows
 * {@code drillingPower = 0}. Both polarities pinned in one test so the
 * delta isolates the IMiningDrill scan branch.</p>
 *
 * <p>Rejected sub-pins: exact drillingPower magnitude (= 0.02f for one
 * sky-exposed drill) is impl per SOP — the contract is the polarity
 * (zero vs positive). The mission-duration formula in
 * {@code EntityRocket} is impl-side magnitude algebra, not a separate
 * contract here.</p>
 */
public class RocketAssemblerMiningDrillStatTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS = Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    /** drillingPower is serialised as a float — accept "drillingPower":0.0,
     *  "drillingPower":0.02, etc. */
    private static final Pattern DRILLING_POWER =
            Pattern.compile("\"drillingPower\":(-?\\d+(?:\\.\\d+)?(?:E-?\\d+)?)");

    @Test
    public void rocketWithMiningDrillBlockAccumulatesDrillingPower() throws Exception {
        // Baseline — same fixture geometry minus the drill block. Pin
        // drillingPower == 0 so the with-drill assertion below isn't
        // attributable to some other latent stat source on the chassis.
        int baselineId = buildAndAssemble(1500, 64, 500, "simple");
        String baselineInfo = String.join("\n",
                client().execute("artest rocket info " + baselineId));
        double baselineDp = extractDouble(baselineInfo, DRILLING_POWER);
        assertEquals("simple fixture must produce drillingPower=0: " + baselineInfo,
                0.0, baselineDp, 0.0);

        // With drill — should flip to > 0.
        int withDrillId = buildAndAssemble(1600, 64, 500, "with-mining-drill");
        String drillInfo = String.join("\n",
                client().execute("artest rocket info " + withDrillId));
        double drillDp = extractDouble(drillInfo, DRILLING_POWER);
        assertTrue("with-mining-drill fixture must produce drillingPower > 0: "
                        + drillInfo, drillDp > 0.0);
    }

    /** Mirror of RocketAssemblySmokeTest#buildAndAssemble — warmup chunks,
     *  pre-clear the bbCache volume with air, run fixture + assemble,
     *  return the spawned entity id. */
    private int buildAndAssemble(int baseX, int baseY, int baseZ, String variant) throws Exception {
        int cx1 = (baseX - 2) >> 4, cz1 = (baseZ - 2) >> 4;
        int cx2 = (baseX + 7) >> 4, cz2 = (baseZ + 7) >> 4;
        String warmup = String.join("\n", client().execute(
                "artest chunk warmup 0 " + cx1 + " " + cz1 + " " + cx2 + " " + cz2));
        assertTrue("chunk warmup failed: " + warmup, warmup.contains("\"ok\":true"));

        String fillAir = String.join("\n", client().execute(
                "artest fill 0 " + (baseX - 2) + " " + (baseY + 1) + " " + (baseZ - 2)
                        + " " + (baseX + 7) + " " + (baseY + 10) + " " + (baseZ + 7)
                        + " minecraft:air"));
        assertTrue("pre-clear failed: " + fillAir, fillAir.contains("\"ok\":true"));

        String fixture = String.join("\n", client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ + " " + variant));
        assertTrue("fixture (" + variant + ") failed: " + fixture, fixture.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture (" + variant + ") missing builderPos: " + fixture, bp.find());
        int bx = Integer.parseInt(bp.group(1)),
                by = Integer.parseInt(bp.group(2)),
                bz = Integer.parseInt(bp.group(3));

        String assemble = String.join("\n", client().execute(
                "artest rocket assemble 0 " + bx + " " + by + " " + bz));
        assertTrue("assemble (" + variant + ") failed: " + assemble,
                assemble.contains("\"ok\":true"));

        String rocketList = String.join("\n", client().execute("artest rocket list 0"));
        Matcher rim = ROCKET_LIST_ID.matcher(rocketList);
        int lastId = -1;
        while (rim.find()) lastId = Integer.parseInt(rim.group(1));
        assertTrue("rocket list yielded no ids after assemble: " + rocketList, lastId >= 0);
        return lastId;
    }

    private static double extractDouble(String haystack, Pattern pattern) {
        Matcher m = pattern.matcher(haystack);
        assertTrue("pattern not found in: " + haystack, m.find());
        return Double.parseDouble(m.group(1));
    }
}
