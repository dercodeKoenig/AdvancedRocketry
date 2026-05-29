package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-39 (Gap R) — TileSatelliteTerminal chip recognition + power gate +
 * destructive erase button.
 *
 * <p>The Satellite Control Center (registry name
 * {@code advancedrocketry:satelliteControlCenter}) is the GUI a player uses
 * to query satellite state remotely. Its server-side
 * {@code writeDataToNetwork(packetId 22)} ladders four mutually-exclusive
 * status codes that the client GUI then displays as the player-visible
 * "satellite info" text (see {@link
 * zmaster587.advancedRocketry.tile.satellite.TileSatelliteTerminal} lines
 * 84-104, 134-167):
 *
 * <ol>
 *   <li>{@code status=0} — no link. Slot 0 empty OR loaded chip's satellite
 *       isn't a SatelliteData subclass (the only kind with the
 *       data/powerPerTick fields the terminal surfaces).</li>
 *   <li>{@code status=1} — no power. Energy buffer below {@code
 *       getPowerPerOperation() = 1 RF}.</li>
 *   <li>{@code status=2} — out of range. Chip's satellite dim not in the
 *       same planetary system as the terminal's dim (per
 *       PlanetaryTravelHelper.isTravelAnywhereInPlanetarySystem).</li>
 *   <li>{@code status=3} — connected. Surfaces powerPerTick + data/maxData.</li>
 * </ol>
 *
 * <p>The {@code onInventoryButtonPressed(1)} branch wires the destructive
 * "kill satellite" button: it removes the linked satellite from its dim's
 * {@code DimensionProperties} AND erases the chip's NBT (chip stays in
 * slot 0, but is now blank — {@code getSatelliteFromSlot(0) == null}).
 *
 * <p>Contracts pinned (player-/API-visible per SOP):
 * <ul>
 *   <li><b>Chip + power → status 3.</b> A SatelliteOptical (a SatelliteData
 *       subclass) chip in slot 0 of a powered terminal on its dim reaches
 *       status 3 and surfaces non-negative powerPerTick + maxData fields.</li>
 *   <li><b>No chip → status 0.</b> Empty terminal reports no link even with
 *       power.</li>
 *   <li><b>Chip, no power → status 1.</b> Chip recognised but energy
 *       starvation gates the surface — pins that the GUI doesn't show
 *       stale data on an unpowered terminal.</li>
 *   <li><b>Erase button → satellite removed from dim + chip blank.</b>
 *       After pressing button 1, the linked satellite is no longer in
 *       {@code DimensionProperties.getSatellite(id)}, and the chip's NBT
 *       compound is cleared.</li>
 * </ul>
 *
 * <p>Out of scope: status 2 (out-of-range). Pinning this branch requires a
 * second dim that's in a different planetary system, which the shared
 * harness doesn't provide as a pre-registered fixture. The branch is
 * defended by {@link
 * zmaster587.advancedRocketry.test.unit.PlanetaryTravelHelperTest} at the
 * helper level; chaining that into the terminal's dispatch is impl, not a
 * contract divergence.
 */
public class SatelliteTerminalChipRecognitionTest extends AbstractSharedServerTest {

    private static final Pattern STATUS = Pattern.compile("\"status\":(-?\\d+)");
    private static final Pattern POWER_PER_TICK = Pattern.compile("\"powerPerTick\":(-?\\d+)");
    private static final Pattern MAX_DATA = Pattern.compile("\"maxData\":(-?\\d+)");
    private static final Pattern SAT_ID = Pattern.compile("\"id\":(-?\\d+)");
    private static final Pattern PRE_REGISTERED = Pattern.compile("\"preSatRegistered\":(true|false)");
    private static final Pattern POST_REGISTERED = Pattern.compile("\"postSatRegistered\":(true|false)");
    private static final Pattern POST_NBT_NULL = Pattern.compile("\"postNbtNull\":(true|false)");

    private static final int CY = 64;
    private static final int CZ = 13000;
    private static final int CX_STATUS3 = 13500;
    private static final int CX_NO_CHIP = 14000;
    private static final int CX_NO_POWER = 14500;
    private static final int CX_ERASE = 15000;

    /** Happy path — chip in slot + energy injected → status 3. */
    @Test
    public void chippedTerminalWithPowerReachesStatus3() throws Exception {
        int x = CX_STATUS3, y = CY, z = CZ;
        long satId = placeTerminalAndLoadChip(x, y, z, /*injectPower*/ true);
        assertNotEquals(-1L, satId);

        String info = exec("artest satellite-terminal info 0 " + x + " " + y + " " + z);
        assertEquals("chip+power must reach status 3: " + info,
                "3", extract(info, STATUS));
        // PowerPerTick reflects the satellite's generator wattage — the
        // satellite-builder fixture installs a non-trivial power source,
        // so this is strictly > 0. The exact number is impl; we only
        // assert positivity (the contract is "GUI shows generation, not
        // zero").
        int ppt = Integer.parseInt(extract(info, POWER_PER_TICK));
        assertTrue("powerPerTick must be > 0 with installed power source: " + info,
                ppt > 0);
        // maxData is the satellite's total data-storage capacity. The
        // chip flows through SatelliteData.data so this surface must be
        // non-negative (negative would indicate uninitialised storage).
        int maxData = Integer.parseInt(extract(info, MAX_DATA));
        assertTrue("maxData must be non-negative: " + info, maxData >= 0);
    }

    /** Empty slot → status 0 even with power present. */
    @Test
    public void unchippedTerminalReportsNoLink() throws Exception {
        int x = CX_NO_CHIP, y = CY, z = CZ;
        placeTerminal(x, y, z);
        // Inject power so we PIN that the no-link branch wins over no-power.
        injectPower(x, y, z, 1000);

        String info = exec("artest satellite-terminal info 0 " + x + " " + y + " " + z);
        assertEquals("empty slot must report status 0: " + info,
                "0", extract(info, STATUS));
    }

    /** Chip loaded but zero energy → status 1. */
    @Test
    public void chippedTerminalWithoutPowerReportsNoPower() throws Exception {
        int x = CX_NO_POWER, y = CY, z = CZ;
        long satId = placeTerminalAndLoadChip(x, y, z, /*injectPower*/ false);
        assertNotEquals(-1L, satId);

        String info = exec("artest satellite-terminal info 0 " + x + " " + y + " " + z);
        assertEquals("chip without power must report status 1: " + info,
                "1", extract(info, STATUS));
    }

    /** Erase button — destructive contract: removes linked satellite from
     *  its dim's DimensionProperties AND blanks the chip NBT. */
    @Test
    public void pressEraseRemovesSatelliteFromDimAndBlanksChip() throws Exception {
        int x = CX_ERASE, y = CY, z = CZ;
        long satId = placeTerminalAndLoadChip(x, y, z, /*injectPower*/ false);
        assertNotEquals(-1L, satId);

        String result = exec("artest satellite-terminal press-erase 0 " + x + " " + y + " " + z);
        assertEquals("linked satellite must be registered on dim BEFORE erase: "
                        + result, "true", extract(result, PRE_REGISTERED));
        assertEquals("linked satellite must be GONE from dim AFTER erase: "
                        + result, "false", extract(result, POST_REGISTERED));
        assertEquals("chip NBT must be null AFTER erase: " + result,
                "true", extract(result, POST_NBT_NULL));
    }

    // --- fixture helpers --------------------------------------------------

    /** Place a satelliteControlCenter at the given position. */
    private void placeTerminal(int x, int y, int z) throws Exception {
        exec("artest chunk warmup 0 " + (x >> 4) + " " + (z >> 4)
                + " " + (x >> 4) + " " + (z >> 4));
        String place = exec("artest place 0 " + x + " " + y + " " + z
                + " advancedrocketry:satelliteControlCenter");
        assertTrue("satelliteControlCenter place failed: " + place,
                place.contains("\"placed\":true"));
    }

    /** Place terminal, build optical satellite, load chip; optionally
     *  inject 1000 RF for the powered-path tests. Returns the satellite id. */
    private long placeTerminalAndLoadChip(int x, int y, int z, boolean injectPower) throws Exception {
        placeTerminal(x, y, z);
        String build = exec("artest satellite-builder build 0 optical");
        assertTrue("optical satellite build failed: " + build,
                build.contains("\"ok\":true"));
        Matcher m = SAT_ID.matcher(build);
        if (!m.find()) {
            return -1L;
        }
        long satId = Long.parseLong(m.group(1));
        String load = exec("artest satellite-terminal load-chip 0 " + x + " " + y + " " + z
                + " " + satId);
        assertTrue("chip load failed: " + load, load.contains("\"ok\":true"));
        if (injectPower) {
            injectPower(x, y, z, 1000);
        }
        return satId;
    }

    private void injectPower(int x, int y, int z, int amount) throws Exception {
        String result = exec("artest energy inject 0 " + x + " " + y + " " + z
                + " " + amount);
        assertTrue("energy inject must succeed: " + result, result.contains("\"ok\":true"));
    }

    private static String extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return m.group(1);
    }
}
