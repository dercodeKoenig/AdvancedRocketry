package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-06 Phase 2 — MissionResourceCollection lifecycle contract.
 *
 * <p>Pins the cause-effect of:
 * <ul>
 *   <li>{@code getProgress} reading {@code (now - startWorldTime) / duration}
 *       linearly, unbounded above 1.0, clamped at 0 below.</li>
 *   <li>{@code tickEntity} firing {@code onMissionComplete} + {@code setDead}
 *       at the tick where progress crosses 1.0.</li>
 *   <li>Natural {@code DimensionProperties.tick} loop prunes a completed
 *       mission from the satellite registry (cleanup contract).</li>
 * </ul>
 *
 * <p>Uses MissionGasCollection as the test vehicle — the simpler of the
 * two concrete subclasses (no asteroid chip required). The lifecycle
 * contract being pinned is in the abstract parent, so the choice of
 * concrete vehicle is an impl detail of the test, not the contract.</p>
 *
 * <p>Important: assertions read fields from the probe response of the
 * mutating call itself (advance / complete-now) rather than a follow-up
 * `state` call. Reason: the natural server tick prunes dead satellites
 * from the registry between commands, so a state lookup post-complete
 * races the natural tick. The mutating probe call computes its own
 * post-state snapshot atomically on the server thread — race-free.</p>
 */
public class MissionLifecyclePyramidTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern MISSION_ID = Pattern.compile("\"missionId\":(-?\\d+)");
    private static final Pattern PROGRESS = Pattern.compile("\"progress\":(-?\\d+\\.?\\d*(?:[eE]-?\\d+)?)");

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private long buildRocketAndStartGasMission(int baseX, long duration) throws Exception {
        int baseY = 64;
        int baseZ = 500;
        // Clear airspace so the assembler can scan a clean pad column.
        ok(client().execute(
                "artest fill 0 " + (baseX - 2) + " " + (baseY + 1) + " " + (baseZ - 2)
                        + " " + (baseX + 7) + " " + (baseY + 10) + " " + (baseZ + 7)
                        + " minecraft:air"));
        String fixture = ok(client().execute(
                "artest fixture rocket 0 " + baseX + " " + baseY + " " + baseZ + " simple"));
        Matcher bp = BUILDER_POS.matcher(fixture);
        assertTrue("fixture missing builderPos: " + fixture, bp.find());
        int bx = Integer.parseInt(bp.group(1));
        int by = Integer.parseInt(bp.group(2));
        int bz = Integer.parseInt(bp.group(3));
        ok(client().execute("artest rocket assemble 0 " + bx + " " + by + " " + bz));

        String list = ok(client().execute("artest rocket list 0"));
        Matcher rim = ROCKET_LIST_ID.matcher(list);
        int lastId = -1;
        while (rim.find()) lastId = Integer.parseInt(rim.group(1));
        assertTrue("no rocket after assemble: " + list, lastId >= 0);

        String start = ok(client().execute(
                "artest mission start-gas 0 " + lastId + " " + duration + " water"));
        assertFalse("start-gas must not error: " + start, start.contains("\"error\""));
        Matcher mm = MISSION_ID.matcher(start);
        assertTrue("missing missionId in start response: " + start, mm.find());
        return Long.parseLong(mm.group(1));
    }

    private double progressFromAdvance(long missionId, long ticks) throws Exception {
        String r = ok(client().execute("artest mission advance " + missionId + " " + ticks));
        assertFalse("advance must not error: " + r, r.contains("\"error\""));
        Matcher pm = PROGRESS.matcher(r);
        assertTrue("missing progress in advance response: " + r, pm.find());
        return Double.parseDouble(pm.group(1));
    }

    /** Progress fraction matches the (now - start) / duration ratio at
     *  the moment of the probe call. Window allowed for natural tick
     *  drift between commands (~ few ms / 50ms per tick). */
    @Test
    public void progressAdvancesLinearlyWithWorldTime() throws Exception {
        long mid = buildRocketAndStartGasMission(7000, 1000);
        double p1 = progressFromAdvance(mid, 250);
        assertTrue("after advance 250 / duration 1000, progress must be in [0.25, 0.35); got " + p1,
                p1 >= 0.25 && p1 < 0.35);
        double p2 = progressFromAdvance(mid, 250);
        assertTrue("after cumulative advance 500, progress must be in [0.5, 0.6); got " + p2,
                p2 >= 0.5 && p2 < 0.6);
    }

    /** Production's {@code getProgress} has no upper cap on the
     *  fraction it returns — pin the unbounded behaviour so a future
     *  cap surfaces here intentionally rather than silently. Use the
     *  advance response's progress field (atomic snapshot) so the
     *  natural-tick prune of the dead mission doesn't race the
     *  assertion. */
    @Test
    public void progressIsUnboundedAboveOne() throws Exception {
        long mid = buildRocketAndStartGasMission(7100, 1000);
        double p = progressFromAdvance(mid, 2500);
        assertTrue("after advance 2500 / duration 1000, progress must be ≥ 2.0; got " + p,
                p >= 2.0);
    }

    /** Below progress=1.0 the mission is not yet completable — verify
     *  via advance response's progress field. */
    @Test
    public void missionStaysAliveBelowProgressOne() throws Exception {
        long mid = buildRocketAndStartGasMission(7200, 1000);
        double p = progressFromAdvance(mid, 500);
        assertTrue("progress at 500/1000 must be < 1.0; got " + p, p < 1.0);
    }

    /** complete-now backdates + drives tickEntity once → the probe's
     *  atomic post-state report must show isDeadAfter=true AND
     *  completed=true (transition from alive→dead happened in this
     *  call). */
    @Test
    public void completionFiresAtProgressOne() throws Exception {
        long mid = buildRocketAndStartGasMission(7300, 1000);
        String resp = ok(client().execute("artest mission complete-now " + mid));
        assertFalse("complete-now must not error: " + resp, resp.contains("\"error\""));
        assertTrue("complete-now must report transition (wasDeadBefore=false): " + resp,
                resp.contains("\"wasDeadBefore\":false"));
        assertTrue("complete-now must mark mission dead: " + resp,
                resp.contains("\"isDeadAfter\":true"));
        assertTrue("complete-now must report completion fired: " + resp,
                resp.contains("\"completed\":true"));
    }

    /** After completion, the DimensionProperties.tick loop removes the
     *  mission from the satellite registry — cleanup contract. Probes
     *  registry-cleanup as a player-visible contract: stale mission
     *  entries would leak the satellite map.
     *
     *  Drives the prune deterministically via {@code satellite
     *  force-tick-dim} rather than waiting on the natural tick, then
     *  polls {@code mission state} for the not-found response. */
    @Test
    public void completionPrunesMissionFromSatelliteRegistry() throws Exception {
        long mid = buildRocketAndStartGasMission(7400, 1000);
        String complete = ok(client().execute("artest mission complete-now " + mid));
        assertTrue("complete-now must succeed: " + complete,
                complete.contains("\"completed\":true"));

        String state = "n/a";
        boolean pruned = false;
        for (int attempt = 0; attempt < 30; attempt++) {
            ok(client().execute("artest satellite force-tick-dim 0"));
            state = ok(client().execute("artest mission state " + mid));
            if (state.contains("\"error\":\"mission not found\"")) {
                pruned = true;
                break;
            }
        }
        assertTrue("post-completion state lookup must report mission not-found "
                        + "(after 30 dim-ticks): " + state, pruned);
    }
}
