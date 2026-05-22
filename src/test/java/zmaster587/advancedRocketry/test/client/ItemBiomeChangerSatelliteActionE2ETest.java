package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10b Phase 7 — player-visible side of
 * {@link zmaster587.advancedRocketry.item.ItemBiomeChanger#onItemRightClick}.
 *
 * <p>Contract: right-clicking a programmed BiomeChanger chip in the same
 * dimension as its registered SatelliteBiomeChanger calls
 * {@code SatelliteBiomeChanger.performAction(player, world, pos)}, which
 * queues a radius-12 + noise field of positions into the satellite's
 * save-format {@code posList} (int-array NBT key). The queue is then
 * drained over server ticks to actually mutate biomes; this test pins
 * only the queue-population step because the i/o-bound drain is a
 * separate behavioural slice (rate-of-drain is impl-detail per SOP).</p>
 *
 * <p>Pin shape: {@code posList} NBT key (declared in
 * {@link zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger#writeToNBT}).
 * Tests a save-format contract — if production stops writing posList,
 * existing-world saves with queued biome changes silently drop them
 * on the next boot (player-visible regression). If production stops
 * populating the queue on right-click, the chip becomes a no-op.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true}; auto-skips on
 * headless CI.</p>
 */
public class ItemBiomeChangerSatelliteActionE2ETest extends AbstractClientE2ETest {

    private static final Pattern DELTA = Pattern.compile("\"posListDelta\":(-?\\d+)");

    /** Same-dim right-click on a programmed chip must enqueue at least
     *  one position into posList (production radius=12 + noise field
     *  guarantees many entries; the loose lower bound of >= 1 stays
     *  contract-faithful instead of pinning the magic radius/noise
     *  constants).
     *
     *  <p>Each posList entry is a 3-int triple (x, y, z) so the int-array
     *  length must be divisible by 3 — pin that too, as the
     *  {@link zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger#readFromNBT}
     *  reader splits by stride-3 and would crash on a non-multiple. */
    @Test
    public void rightClickInSameDimEnqueuesPositionsIntoSatellitePosList() throws Exception {
        String resp = String.join("\n", serverClient().execute(
                "artest player try-biomechanger-rclick 0"));
        assertFalse("try-biomechanger-rclick must not error; resp=" + resp,
                resp.contains("\"error\""));

        Matcher m = DELTA.matcher(resp);
        assertTrue("expected posListDelta field in: " + resp, m.find());
        int delta = Integer.parseInt(m.group(1));

        assertTrue("right-click on a programmed BiomeChanger in same dim "
                        + "must enqueue >= 1 position triple (delta in ints "
                        + ">= 3); got delta=" + delta + "; resp=" + resp,
                delta >= 3);
        assertTrue("posList entries are (x,y,z) triples — int-array length "
                        + "delta must be a multiple of 3; got delta=" + delta,
                delta % 3 == 0);
    }

    /** Probe must surface an error JSON when the dim arg is missing. */
    @Test
    public void tryBiomeChangerRclickErrorsWithoutDim() throws Exception {
        String resp = String.join("\n", serverClient().execute(
                "artest player try-biomechanger-rclick"));
        assertTrue("missing args must surface an error; resp=" + resp,
                resp.contains("\"error\""));
    }
}
