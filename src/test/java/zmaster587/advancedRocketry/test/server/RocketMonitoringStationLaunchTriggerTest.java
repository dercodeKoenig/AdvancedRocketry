package zmaster587.advancedRocketry.test.server;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Coverage-audit gap (2026-05-26 Tier 1 #2) — TileRocketMonitoringStation
 * redstone-triggered launch.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation}'s
 * {@code update()} method (production lines 96-115) implements a
 * rising-edge redstone trigger that calls
 * {@code linkedRocket.prepareLaunch()} exactly once per
 * redstone-power transition. The gate is the {@code was_powered}
 * boolean field — set on first tick after power rises, cleared on
 * first tick after power drops.</p>
 *
 * <p>This is the <b>only non-GUI launch path</b> in the mod. Without
 * it, players can't automate launches via observer/redstone-clock
 * logic. Existing {@link RocketInfrastructureSmokeTest} pins
 * link/unlink and the {@code linkedRocket} reference but does not
 * touch the redstone gate at all.</p>
 *
 * <h2>Contract pinned</h2>
 *
 * <ol>
 *   <li><b>Rising-edge fires {@code prepareLaunch}.</b> Redstone
 *       power rises from absent → present, one tick later
 *       {@code preLaunchCount} increments by 1.</li>
 *   <li><b>Sustained-high does not re-fire.</b> Subsequent ticks with
 *       the same power level do not call prepareLaunch (the
 *       {@code !was_powered} check on production line 103 guards
 *       it).</li>
 *   <li><b>Falling-edge clears the gate.</b> When power drops, the
 *       {@code was_powered} flag resets (production line 112)
 *       allowing the next rising edge to fire again.</li>
 *   <li><b>Second rising edge re-fires.</b> After a power cycle, a
 *       fresh rise triggers prepareLaunch again.</li>
 * </ol>
 *
 * <p>Position-isolated at x=9500. Uses
 * {@link AbstractSharedServerTest} for one cold-start per class.</p>
 */
public class RocketMonitoringStationLaunchTriggerTest extends AbstractSharedServerTest {

    private static final Pattern BUILDER_POS =
            Pattern.compile("\"builderPos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern ENT_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern OBSERVED =
            Pattern.compile("\"observed\":(\\d+)");
    private static final Pattern WAS_POWERED =
            Pattern.compile("\"wasPowered\":(true|false)");
    private static final Pattern EQUIVALENT_POWER =
            Pattern.compile("\"equivalentPower\":(true|false)");

    @Before
    public void armPreLaunchCanceller() throws Exception {
        // arm-prelaunch-cancel installs a Forge subscriber that
        // cancels every RocketPreLaunchEvent. With cancel, production
        // skips the {@code LAUNCH_COUNTER = 200} branch, so subsequent
        // prepareLaunch() calls don't enter the abort-existing-launch
        // path on line 1693-1697 of EntityRocket. Net effect: each
        // rising edge fires the event cleanly, observable via the
        // prelaunch-cancel-counts.observed counter (which arm-... also
        // resets to 0).
        ok(client().execute("artest rocket arm-prelaunch-cancel"));
    }

    @AfterClass
    public static void disarmPreLaunchCanceller() throws Exception {
        // Don't leak the canceller subscription into sibling test
        // classes — any test that legitimately needs LAUNCH_COUNTER to
        // be set to 200 would observe phantom cancellations.
        ok(client().execute("artest rocket disarm-prelaunch-cancel"));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private static void ok(java.util.List<String> resp) {
        String joined = join(resp);
        assertTrue("probe call failed: " + joined, joined.contains("\"ok\":true"));
    }

    /** Number of RocketPreLaunchEvent fires observed since
     *  {@code arm-prelaunch-cancel} reset the counter. Each
     *  prepareLaunch() call that reaches line 1706 of EntityRocket
     *  bumps this. */
    private static int observedPreLaunchEvents() throws Exception {
        String resp = join(client().execute(
                "artest rocket prelaunch-cancel-counts"));
        Matcher m = OBSERVED.matcher(resp);
        assertTrue("observed count must be present: " + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    /** Run a single update() tick on the monitoring station tile. */
    private static void tickMonitor(int x, int y, int z) throws Exception {
        ok(client().execute("artest tile force-tick 0 " + x + " " + y + " " + z + " 1"));
    }

    private static boolean monitorWasPowered(int x, int y, int z) throws Exception {
        String resp = join(client().execute(
                "artest infra monitor-info 0 " + x + " " + y + " " + z));
        Matcher m = WAS_POWERED.matcher(resp);
        assertTrue("monitor-info must include wasPowered: " + resp, m.find());
        return Boolean.parseBoolean(m.group(1));
    }

    private static boolean monitorEquivalentPower(int x, int y, int z) throws Exception {
        String resp = join(client().execute(
                "artest infra monitor-info 0 " + x + " " + y + " " + z));
        Matcher m = EQUIVALENT_POWER.matcher(resp);
        assertTrue("monitor-info must include equivalentPower: " + resp, m.find());
        return Boolean.parseBoolean(m.group(1));
    }

    /** Place a redstone block adjacent (east) to the monitor — this
     *  raises {@code world.isBlockIndirectlyGettingPowered(monitor.pos)}
     *  to a non-zero level. */
    private static void powerOn(int x, int y, int z) throws Exception {
        ok(client().execute("artest place 0 " + (x + 1) + " " + y + " " + z
                + " minecraft:redstone_block"));
    }

    /** Replace the adjacent redstone block with air, dropping power. */
    private static void powerOff(int x, int y, int z) throws Exception {
        ok(client().execute("artest place 0 " + (x + 1) + " " + y + " " + z
                + " minecraft:air"));
    }

    /** Assembles a rocket via the standard fixture; returns its entity id. */
    private static int assembleFixture(int baseX, int baseY, int baseZ) throws Exception {
        ok(client().execute("artest fill 0 " + (baseX - 2) + " " + (baseY + 1)
                + " " + (baseZ - 2) + " " + (baseX + 7) + " " + (baseY + 10)
                + " " + (baseZ + 7) + " minecraft:air"));
        String fx = join(client().execute("artest fixture rocket 0 " + baseX
                + " " + baseY + " " + baseZ + " simple"));
        assertTrue("fixture rocket failed: " + fx, fx.contains("\"ok\":true"));
        Matcher bp = BUILDER_POS.matcher(fx);
        assertTrue("builderPos missing: " + fx, bp.find());
        int bx = Integer.parseInt(bp.group(1));
        int by = Integer.parseInt(bp.group(2));
        int bz = Integer.parseInt(bp.group(3));
        String assemble = join(client().execute("artest rocket assemble 0 "
                + bx + " " + by + " " + bz));
        assertTrue("rocket assemble failed: " + assemble, assemble.contains("\"ok\":true"));
        Matcher em = ENT_ID.matcher(assemble);
        assertTrue("entityId missing: " + assemble, em.find());
        return Integer.parseInt(em.group(1));
    }

    @Test
    public void risingRedstoneEdgeFiresPrepareLaunchExactlyOnce_andSustainedDoesNotRefire()
            throws Exception {
        int mx = 9500, my = 65, mz = 9500;
        ok(client().execute("artest place 0 " + mx + " " + my + " " + mz
                + " advancedrocketry:monitoringStation"));
        int rocketId = assembleFixture(mx + 20, 64, mz);
        ok(client().execute("artest infra link 0 " + mx + " " + my + " " + mz
                + " " + rocketId));

        // Sanity baseline: no redstone, no wasPowered.
        assertFalse("baseline: monitor must not be powered without a "
                + "redstone source", monitorEquivalentPower(mx, my, mz));
        assertFalse("baseline: was_powered must be false initially",
                monitorWasPowered(mx, my, mz));

        int before = observedPreLaunchEvents();

        // Rising edge — power on, then tick once. Production line
        // 102-108 fires prepareLaunch exactly once because !was_powered.
        powerOn(mx, my, mz);
        assertTrue("after powerOn, monitor must observe redstone",
                monitorEquivalentPower(mx, my, mz));
        tickMonitor(mx, my, mz);

        int afterFirstTick = observedPreLaunchEvents();
        assertEquals("rising-edge tick must fire prepareLaunch exactly once "
                        + "(observed preLaunchCount " + before + " → "
                        + afterFirstTick + ")",
                1, afterFirstTick - before);
        assertTrue("was_powered must be true after rising-edge tick "
                + "(gate is now armed against re-fire)",
                monitorWasPowered(mx, my, mz));

        // Sustained high: 3 more ticks with the same redstone block in
        // place. The !was_powered guard must keep prepareLaunch from
        // being re-invoked.
        tickMonitor(mx, my, mz);
        tickMonitor(mx, my, mz);
        tickMonitor(mx, my, mz);

        int afterSustained = observedPreLaunchEvents();
        assertEquals("sustained-high ticks must NOT re-fire prepareLaunch — "
                        + "the !was_powered gate is the entire point of the "
                        + "rising-edge contract (delta from rising-edge: "
                        + (afterSustained - afterFirstTick) + ")",
                0, afterSustained - afterFirstTick);
    }

    @Test
    public void fallingRedstoneEdgeResetsTheGate_andSecondRisingEdgeRefires()
            throws Exception {
        // Distinct column from the first test (position isolation).
        int mx = 9520, my = 65, mz = 9500;
        ok(client().execute("artest place 0 " + mx + " " + my + " " + mz
                + " advancedrocketry:monitoringStation"));
        int rocketId = assembleFixture(mx + 20, 64, mz);
        ok(client().execute("artest infra link 0 " + mx + " " + my + " " + mz
                + " " + rocketId));

        int before = observedPreLaunchEvents();

        // First rising edge — should fire prepareLaunch.
        powerOn(mx, my, mz);
        tickMonitor(mx, my, mz);
        assertEquals("first rising-edge fires prepareLaunch once",
                1, observedPreLaunchEvents() - before);
        assertTrue("was_powered armed after first rising edge",
                monitorWasPowered(mx, my, mz));

        // Falling edge — power off + one tick. Production line 111-113
        // clears was_powered when getEquivalentPower returns false.
        powerOff(mx, my, mz);
        assertFalse("after powerOff, monitor must observe no redstone",
                monitorEquivalentPower(mx, my, mz));
        tickMonitor(mx, my, mz);
        assertFalse("was_powered must reset to false on falling edge — "
                        + "without this, a powered-then-unpowered cycle could "
                        + "never re-trigger",
                monitorWasPowered(mx, my, mz));
        int afterFallingEdge = observedPreLaunchEvents();

        // Second rising edge — power back on, tick, expect another
        // prepareLaunch fire because the gate is reset.
        powerOn(mx, my, mz);
        tickMonitor(mx, my, mz);
        assertEquals("second rising-edge after a falling-edge reset must "
                        + "fire prepareLaunch a second time",
                1, observedPreLaunchEvents() - afterFallingEdge);
    }
}
