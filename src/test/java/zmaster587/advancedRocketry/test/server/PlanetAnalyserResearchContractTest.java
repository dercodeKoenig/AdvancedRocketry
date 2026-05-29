package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-40 (Gap D) — Planet Analyser (TileAstrobodyDataProcessor) research
 * increment contract.
 *
 * <p>Phase-0 reshape: the audit framed this as "planet-id chip → SatelliteData
 * scan output". Production
 * ({@link zmaster587.advancedRocketry.tile.multiblock.TileAstrobodyDataProcessor})
 * is actually an ASTEROID research pipeline: when a {@link
 * zmaster587.advancedRocketry.item.ItemAsteroidChip} with non-null UUID sits
 * in slot 0, the {@code researching{Atmosphere,Distance,Mass}} private flag
 * is true, and a connected {@link
 * zmaster587.advancedRocketry.tile.hatch.TileDataBus} has &ge; 1 unit of the
 * matching {@link zmaster587.advancedRocketry.api.DataStorage.DataType},
 * each {@code update()} cycle (10 ticks per data type — see {@code maxResearchTime})
 * increments the chip's stored data field by 1.</p>
 *
 * <p>Contract pinned: <i>"powered + AsteroidChip in slot 0 + DataBus
 * with COMPOSITION data + researchingAtmosphere=true → after a research
 * cycle the chip's COMPOSITION value rises by &ge; 1."</i> Litmus passes
 * — the chip's data fields are what the player sees on the chip's
 * tooltip and what feeds onward into the rest of the data flow.</p>
 *
 * <p>The pre-existing {@link PlanetAnalyserMultiblockTest} pins assembly
 * + the {@code 'D'} char mapping; nothing covered the research-increment
 * surface before this test.</p>
 *
 * <p>Position-isolated at x=6200 (no collision with
 * PlanetAnalyserMultiblockTest's x=6000 + 30 + 60 = x=6060 fixtures).</p>
 */
public class PlanetAnalyserResearchContractTest extends AbstractSharedServerTest {

    private static final int CX = 6200;
    private static final int CY = 64;
    private static final int CZ = 6200;

    private static final Pattern COMPOSITION_PAT =
            Pattern.compile("\"composition\":(\\d+)");

    /**
     * TASK-40 Gap D — assembled analyser increments the chip's COMPOSITION
     * counter when (1) powered, (2) chip with UUID in slot 0, (3) DataBus
     * pre-loaded with COMPOSITION, (4) researchingAtmosphere flag set.
     */
    @Test
    public void poweredAnalyserIncrementsChipCompositionFromDataBus() throws Exception {
        // 1) Assemble the analyser via fixture.
        String fixture = exec("artest fixture multiblock planet-analyser 0 "
                + CX + " " + CY + " " + CZ);
        assertTrue("fixture failed: " + fixture, fixture.contains("\"ok\":true"));

        // Validate structure — required so libVulpes' integrateTile populates
        // dataCables[] (the field the analyser reads from).
        String tryComplete = exec("artest machine try-complete 0 "
                + CX + " " + CY + " " + CZ);
        assertTrue("analyser must validate (precondition for dataCables[] "
                        + "to be populated): " + tryComplete,
                tryComplete.contains("\"isComplete\":true"));

        // 2) Pre-fill all 3 data hatches with COMPOSITION data via the
        // databus-set-data probe. Which physical hatch maps to
        // dataCables[0]=COMPOSITION depends on libVulpes' integrateTile
        // iteration order — feeding all 3 sidesteps the question.
        for (int dx = -1; dx <= 1; dx++) {
            String seed = exec("artest infra databus-set-data 0 "
                    + (CX + dx) + " " + (CY - 1) + " " + (CZ + 1)
                    + " COMPOSITION 30");
            assertTrue("databus-set-data must succeed at dx=" + dx
                            + ": " + seed,
                    seed.contains("\"ok\":true"));
        }

        // 3) Inject 100k RF into the power-input plug at (cx+1, cy-1, cz).
        // requiredPowerPerTick = 100 → 100k buys 1000 ticks of headroom
        // (~50 research cycles, well above the 1 we need).
        String energy = exec("artest energy inject 0 "
                + (CX + 1) + " " + (CY - 1) + " " + CZ + " 100000");
        assertTrue("energy inject must succeed: " + energy,
                energy.contains("\"ok\":true"));

        // 4) Drop an AsteroidChip with UUID=1L into slot 0 of the controller.
        String load = exec("artest infra astrobody-load-chip 0 "
                + CX + " " + CY + " " + CZ);
        assertTrue("astrobody-load-chip must succeed: " + load,
                load.contains("\"ok\":true"));

        // Baseline: chip should report composition=0 before research.
        String pre = exec("artest infra astrobody-chip-data 0 "
                + CX + " " + CY + " " + CZ);
        assertTrue("chip-data must succeed pre-research: " + pre,
                pre.contains("\"ok\":true"));
        int compositionBefore = extract(pre, COMPOSITION_PAT);
        assertTrue("chip must start at composition=0 (fresh chip): "
                        + " before=" + compositionBefore + " pre=" + pre,
                compositionBefore == 0);

        // 5) Set researchingAtmosphere=true (bit 1) — this also calls
        // attemptAllResearchStart inside the probe, which arms
        // atmosphereProgress=0 (consuming 1 COMPOSITION from the DataBus).
        String setR = exec("artest infra astrobody-set-research 0 "
                + CX + " " + CY + " " + CZ + " 1");
        assertTrue("astrobody-set-research must succeed: " + setR,
                setR.contains("\"ok\":true"));

        // 6) Force-tick the controller 30 times. Per maxResearchTime=10
        // each research cycle is 10 ticks of ramp + 1 increment;
        // 30 ticks comfortably covers 2 cycles.
        String tick = exec("artest tile force-tick 0 "
                + CX + " " + CY + " " + CZ + " 30");
        assertTrue("force-tick must succeed: " + tick,
                tick.contains("\"ok\":true"));

        // 7) Read chip data, assert composition grew by >= 1.
        String post = exec("artest infra astrobody-chip-data 0 "
                + CX + " " + CY + " " + CZ);
        assertTrue("chip-data must succeed post-research: " + post,
                post.contains("\"ok\":true"));
        int compositionAfter = extract(post, COMPOSITION_PAT);
        assertTrue("chip composition must have incremented from 0 after "
                        + "30 ticks of analyser research (the player-visible "
                        + "'data field grows' contract); before="
                        + compositionBefore + " after=" + compositionAfter
                        + " post=" + post,
                compositionAfter >= 1);
    }

    // -- helpers ----------------------------------------------------------

    private static String exec(String cmd) throws Exception {
        return String.join("\n", client().execute(cmd));
    }

    private static int extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return Integer.parseInt(m.group(1));
    }
}
