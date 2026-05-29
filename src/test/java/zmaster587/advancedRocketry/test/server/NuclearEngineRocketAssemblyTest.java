package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-37 (Gap P) — nuclear-engine rocket assembly thrust aggregation.
 *
 * <p>The nuclear engine family — {@link
 * zmaster587.advancedRocketry.block.BlockNuclearRocketMotor},
 * {@link zmaster587.advancedRocketry.block.BlockNuclearCore},
 * {@link zmaster587.advancedRocketry.block.BlockNuclearFuelTank},
 * and the {@link zmaster587.advancedRocketry.api.IRocketNuclearCore}
 * interface — is wired into rocket assembly via two distinct scan paths:
 *
 * <ul>
 *   <li>{@link zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine}
 *       lines 386-395 — initial scan during the player's "Build" click.</li>
 *   <li>{@link zmaster587.advancedRocketry.util.StorageChunk#recalculateStats}
 *       lines 222-224 — re-scan from the storage chunk's own snapshot.</li>
 * </ul>
 *
 * <p>Both paths apply a cohesion check: a placed {@code IRocketNuclearCore}
 * contributes its {@code getMaxThrust()} to {@code thrustNuclearReactorLimit}
 * <em>only if</em> the block directly below is either another
 * {@code IRocketNuclearCore} OR an {@code IRocketEngine}. The final rocket
 * thrust is {@code max(monopropellant, bipropellant, nuclearTotal)} where
 * {@code nuclearTotal = min(nozzleLimit, reactorLimit)} — so a nuclear
 * motor with NO contributing core gates the nuclear branch to zero, and
 * with no chemical engines either the final {@code stats.thrust} stays at
 * 0 (the player-visible "rocket cannot launch" state).</p>
 *
 * <p>Contracts pinned (two paired tests share one fixture chassis so the
 * delta isolates the cohesion check):</p>
 *
 * <ul>
 *   <li><b>Core stacked above nuclear motor → thrust &gt; 0.</b> The
 *       {@code with-nuclear-stack} fixture places 2 nuclear motors with
 *       cores directly above; the assembled rocket reports a positive
 *       {@code stats.thrust}, proving the nuclear chain energises the
 *       launch-readiness gate.</li>
 *   <li><b>Core misplaced (no engine/core below) → thrust = 0.</b> The
 *       {@code with-nuclear-misplaced} fixture places the same 2 nuclear
 *       motors but the core sits at the center column where below is air;
 *       {@code reactorLimit} stays 0 → {@code nuclearTotal=min(N,0)=0} →
 *       {@code thrust=max(0,0,0)=0}. Pins that the cohesion check is the
 *       difference, not just "presence of any nuclear block".</li>
 * </ul>
 *
 * <p>Rejected sub-pins: exact thrust magnitude (= 35 per motor × ratio)
 * is impl per SOP — the player-visible contract is "rocket has thrust"
 * vs "rocket has none", not the specific numbers. The
 * {@code nuclearCoreThrustRatio} config flows through {@link
 * zmaster587.advancedRocketry.test.unit.ARConfigurationTest} and is
 * impl on this surface.</p>
 */
public class NuclearEngineRocketAssemblyTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS = Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern THRUST = Pattern.compile("\"thrust\":(-?\\d+)");
    private static final Pattern ENGINE_COUNT = Pattern.compile("\"engineCount\":(-?\\d+)");

    @Test
    public void nuclearCoreAboveMotorContributesNuclearThrust() throws Exception {
        int entityId = buildAndAssemble(1700, 64, 500, "with-nuclear-stack");
        String info = String.join("\n",
                client().execute("artest rocket info " + entityId));
        // Both nuclear motors must register in engineCount via the
        // IRocketEngine + air-below scan branch (BlockNuclearRocketMotor
        // extends BlockRocketMotor; with-nuclear-stack overrides BOTH
        // engine positions with nuclear motors).
        assertEquals("with-nuclear-stack must register both nuclear motors: " + info,
                2, extractInt(info, ENGINE_COUNT));
        // Positive-thrust contract — the cohesion check found cores above
        // motors, so nuclearReactorLimit > 0 and nuclearTotal > 0.
        int thrust = extractInt(info, THRUST);
        assertTrue("nuclear stack with cohesion must yield thrust > 0: "
                + info, thrust > 0);
    }

    @Test
    public void misplacedNuclearCoreFailsAssemblyWithNoEngines() throws Exception {
        // No buildAndAssemble — the contract here is that scanRocket
        // REJECTS the rocket entirely. The probe surfaces the scan status
        // when not SUCCESS, mirroring the chat / GUI error the player
        // sees when they hit "Build" without proper engine wiring.
        int baseX = 1800, baseY = 64, baseZ = 500;
        String assemble = setupAndAttemptAssemble(baseX, baseY, baseZ, "with-nuclear-misplaced");
        // Player-visible contract — nuclear motor with core misplaced
        // (no IRocketEngine or IRocketNuclearCore below) leaves
        // thrustNuclearReactorLimit=0 → nuclearTotalLimit=0 →
        // stats.thrust=max(0,0,0)=0 → scan gate at
        // TileRocketAssemblingMachine line 457 (getThrust() <=
        // getNeededThrust()) fires → status NOENGINES.
        assertTrue("misplaced-core assemble must NOT succeed: " + assemble,
                assemble.contains("\"error\""));
        assertTrue("misplaced-core scan must surface NOENGINES status: " + assemble,
                assemble.contains("\"status\":\"NOENGINES\""));
    }

    /** Run fixture + assemble but DON'T assert SUCCESS — returns the raw
     *  assemble response so the caller can pin a specific error status
     *  (e.g. NOENGINES) on the failure path. */
    private String setupAndAttemptAssemble(int baseX, int baseY, int baseZ, String variant) throws Exception {
        int cx1 = (baseX - 2) >> 4, cz1 = (baseZ - 2) >> 4;
        int cx2 = (baseX + 7) >> 4, cz2 = (baseZ + 7) >> 4;
        client().execute("artest chunk warmup 0 " + cx1 + " " + cz1 + " " + cx2 + " " + cz2);
        client().execute("artest fill 0 " + (baseX - 2) + " " + (baseY + 1) + " " + (baseZ - 2)
                + " " + (baseX + 7) + " " + (baseY + 10) + " " + (baseZ + 7) + " minecraft:air");
        String fixture = String.join("\n", client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ + " " + variant));
        assertTrue("fixture (" + variant + ") failed: " + fixture, fixture.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture (" + variant + ") missing builderPos: " + fixture, bp.find());
        int bx = Integer.parseInt(bp.group(1)),
                by = Integer.parseInt(bp.group(2)),
                bz = Integer.parseInt(bp.group(3));
        return String.join("\n", client().execute(
                "artest rocket assemble 0 " + bx + " " + by + " " + bz));
    }

    /** Mirror of RocketAssemblySmokeTest#buildAndAssemble (chunk warmup,
     *  air pre-clear, fixture, assemble, return last spawned rocket id). */
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

    private static int extractInt(String haystack, Pattern pattern) {
        Matcher m = pattern.matcher(haystack);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
}
