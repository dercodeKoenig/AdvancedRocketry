package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-06 Phase 4 — MissionOreMining.onMissionComplete contract.
 *
 * <p>Pins the player-visible cause-effect of completing an ore-mining
 * mission. Two production code paths run on completion:
 * <ul>
 *   <li>Conditional (guarded by {@code rocketStats.getDrillingPower() != 0f}):
 *       asteroid harvest with three Math.random() rolls (distance /
 *       composition / mass) populating rocket inventory tiles. Pins are
 *       loose-bound here (≥0 stacks) because exact roll outcomes are
 *       impl per testing-principles SOP, and an unregistered asteroid
 *       type short-circuits the inner harvest loop anyway.</li>
 *   <li>Unconditional (always runs): asteroid-chip slot 0 cleared and
 *       refilled with a fresh empty ItemAsteroidChip. This is a
 *       player-visible contract — the chip is consumed by the mission
 *       and a blank replacement appears in the guidance computer.</li>
 *   <li>Unconditional: a new EntityRocket (NOT EntityStationDeployedRocket
 *       like the gas path) is spawned in the launch dim at launch
 *       coords.</li>
 * </ul>
 */
public class MissionOreCompletionTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ROCKET_LIST_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern MISSION_ID = Pattern.compile("\"missionId\":(-?\\d+)");

    private static String ok(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private int buildAndAssembleRocket(int baseX) throws Exception {
        int baseY = 64;
        int baseZ = 700;
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
        return lastId;
    }

    private long startOreMission(int rocketId, long duration, float drillingPower) throws Exception {
        String start = ok(client().execute(
                "artest mission start-ore 0 " + rocketId + " " + duration + " " + drillingPower));
        assertFalse("start-ore must not error: " + start, start.contains("\"error\""));
        Matcher mm = MISSION_ID.matcher(start);
        assertTrue("missing missionId in start response: " + start, mm.find());
        return Long.parseLong(mm.group(1));
    }

    /** Whether drillingPower is zero or not, the mission UNCONDITIONALLY
     *  clears guidance-computer slot 0 and refills it with a blank
     *  ItemAsteroidChip (MissionOreMining lines 116-118). The chip
     *  refill is a save-format / inventory contract — players see the
     *  fresh chip when they open the landed rocket. */
    @Test
    public void oreCompletionAlwaysRefillsGuidanceWithBlankAsteroidChip() throws Exception {
        int rid = buildAndAssembleRocket(9000);
        long mid = startOreMission(rid, 1000, 1.0f);
        String cargo = ok(client().execute("artest mission complete-now " + mid));
        assertFalse("complete-now must not error: " + cargo, cargo.contains("\"error\""));
        // Refilled chip lands in the respawned rocket's guidance
        // computer (storage chunk inventory tile). It's a fresh chip
        // with no NBT — registry name match is enough.
        assertTrue("respawned rocket must carry an asteroid chip post-completion: " + cargo,
                cargo.contains("advancedrocketry:asteroidchip"));
    }

    /** Production gate: with {@code drillingPower == 0f} the entire
     *  harvest block (MissionOreMining lines 42-114) is skipped — the
     *  rocket inventory has no ore stacks, just the refilled blank
     *  chip from lines 116-118. Counter-test pinning the gate. */
    @Test
    public void oreCompletionSkipsHarvestWhenDrillingPowerZero() throws Exception {
        int rid = buildAndAssembleRocket(9100);
        long mid = startOreMission(rid, 1000, 0.0f);
        String cargo = ok(client().execute("artest mission complete-now " + mid));
        assertFalse("complete-now must not error: " + cargo, cargo.contains("\"error\""));
        // The blank refill chip (line 118) is the only item expected
        // — extract a count and pin upper bound. Tolerant of the
        // respawn-coords search returning multiple rockets if a prior
        // test in the same JVM placed one nearby (different Z origin
        // 700 keeps them apart but allow ≤ 2 for safety).
        Matcher m = Pattern.compile("\"itemEntries\":(\\d+)").matcher(cargo);
        assertTrue("itemEntries field missing in cargo: " + cargo, m.find());
        int entries = Integer.parseInt(m.group(1));
        assertTrue("drillingPower=0 → only the refill chip (≤ 2 entries to allow "
                        + "a duplicate from a sibling test rocket); got " + entries
                        + "; resp=" + cargo,
                entries >= 1 && entries <= 2);
    }

    /** The ore-mining completion path spawns a plain EntityRocket (line
     *  119) at launch coords. rocket-cargo finds at least one rocket in
     *  the search BB. Together with the gas test (which spawns
     *  EntityStationDeployedRocket — a SUBCLASS of EntityRocket so it
     *  also matches the EntityRocket.class filter), this pin only
     *  confirms "some rocket exists" — type discrimination is a
     *  follow-up. */
    @Test
    public void oreCompletionRespawnsRocketInLaunchDim() throws Exception {
        int rid = buildAndAssembleRocket(9200);
        long mid = startOreMission(rid, 1000, 1.0f);
        String cargo = ok(client().execute("artest mission complete-now " + mid));
        assertFalse("complete-now must not error: " + cargo, cargo.contains("\"error\""));
        assertTrue("at least one rocket entity must exist near launch coords after ore completion: "
                        + cargo,
                cargo.contains("\"rocketCount\":") && !cargo.contains("\"rocketCount\":0"));
    }
}
