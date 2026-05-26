package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-33 — TileSatelliteBuilder real-construction path.
 *
 * <p>The pre-existing {@code /artest satellite-builder build <dim> <typeId>}
 * probe constructs and registers a satellite via reflection, bypassing
 * TileSatelliteBuilder entirely. This test exercises the REAL player-
 * facing path: place a TileSatelliteBuilder, populate the four critical
 * slots, press the "Build" button (modules[0] in getModules) — the same
 * code path a player triggers through the GUI — and pin the slot-state
 * delta {@code assembleSatellite()} produces.</p>
 *
 * <p>Contract pinned (per slot map at
 * {@code TileSatelliteBuilder.java:37-50,98-142}):</p>
 *
 * <ul>
 *   <li><b>canAssembleSatellite gates assembly.</b> With the standard
 *       four-item set (chassis / primary-function / power source / id
 *       chip), production must return true. Pinned by the press-build
 *       probe's pre-flight check.</li>
 *   <li><b>Chassis slot is consumed.</b> After build, slot 11 (chassis)
 *       is empty — the empty ItemSatellite shell has moved to holding.</li>
 *   <li><b>Holding slot carries the assembled satellite.</b> Slot 10
 *       (holding) contains an ItemSatellite whose NBT has a fresh
 *       {@code satelliteId} from
 *       {@code DimensionManager.getNextSatelliteId()}. The output slot
 *       (7) stays empty — processComplete moves holding→output only
 *       once libVulpes' completionTime countdown finishes.</li>
 *   <li><b>ID chip carries matching satelliteId.</b> Slot 8 (chipSlot)
 *       gets rewritten via {@code sat.getControllerItemStack(...)} which
 *       stamps the satelliteId into the chip's NBT. Chip and holding
 *       MUST share the same satelliteId — that's the wiring that lets a
 *       player later place the chip in a SatelliteControlCenter to point
 *       it at the same satellite the chassis represents.</li>
 *   <li><b>Per-type primary meta resolution.</b> The press-build probe
 *       resolves the primary-function meta from {@code typeId} via the
 *       SatelliteRegistry property scan; this pins that "optical" maps
 *       to a registered meta (defensive against a registry regression
 *       that loses the optical type).</li>
 * </ul>
 *
 * <p><b>Out of scope</b>: the libVulpes-side completionTime countdown
 * (holding → output transition). That's libVulpes plumbing, not
 * TileSatelliteBuilder's contract, and is implicitly exercised by every
 * other multiblock machine test in the suite.</p>
 */
public class SatelliteBuilderPressBuildContractTest extends AbstractSharedServerTest {

    private static final Pattern CHASSIS_EMPTY = Pattern.compile("\"chassisEmpty\":(true|false)");
    private static final Pattern OUTPUT_EMPTY = Pattern.compile("\"outputEmpty\":(true|false)");
    private static final Pattern HOLDING_ITEM = Pattern.compile("\"holdingItem\":\"([^\"]*)\"");
    private static final Pattern CHIP_ITEM = Pattern.compile("\"chipItem\":\"([^\"]*)\"");
    private static final Pattern HOLDING_SAT_ID = Pattern.compile("\"holdingSatId\":(-?\\d+)");
    private static final Pattern CHIP_SAT_ID = Pattern.compile("\"chipSatId\":(-?\\d+)");
    private static final Pattern PRIMARY_META = Pattern.compile("\"primaryMeta\":(-?\\d+)");

    private static final int CY = 64;
    private static final int CZ = 9700;
    private static final int CX_OPTICAL = 10100;
    private static final int CX_WEATHER = 10500;

    /** Standard happy-path: place builder, press build with "optical"
     *  primary, observe slot transitions match {@code assembleSatellite}. */
    @Test
    public void pressBuildAssemblesOpticalSatellite() throws Exception {
        int x = CX_OPTICAL, y = CY, z = CZ;
        exec("artest chunk warmup 0 " + (x >> 4) + " " + (z >> 4)
                + " " + (x >> 4) + " " + (z >> 4));
        String place = exec("artest place 0 " + x + " " + y + " " + z
                + " advancedrocketry:satelliteBuilder");
        assertTrue("satellite builder place failed: " + place,
                place.contains("\"placed\":true"));

        String resp = exec("artest satellite-builder press-build 0 "
                + x + " " + y + " " + z + " optical");
        assertTrue("press-build must succeed: " + resp, resp.contains("\"ok\":true"));

        // Per-type resolution: optical must map to a primary-function meta.
        assertNotEquals("optical must resolve to a valid primary-function meta",
                -1, extractInt(resp, PRIMARY_META));

        // Chassis consumed.
        assertEquals("chassis slot must be empty after build: " + resp,
                "true", extract(resp, CHASSIS_EMPTY));

        // Output stays empty (processComplete countdown not yet run).
        assertEquals("output slot stays empty post-button (completionTime pending): "
                        + resp, "true", extract(resp, OUTPUT_EMPTY));

        // Holding slot carries the satellite item.
        assertEquals("holding slot must carry advancedrocketry:satellite: " + resp,
                "advancedrocketry:satellite", extract(resp, HOLDING_ITEM));

        // Chip slot was rewritten with the controller stack.
        assertEquals("chip slot must carry advancedrocketry:satelliteidchip after build: "
                        + resp, "advancedrocketry:satelliteidchip",
                extract(resp, CHIP_ITEM));

        // Both NBTs share the same fresh satelliteId. Note the two stacks
        // use DIFFERENT NBT keys for the id: the chip uses "satelliteId"
        // (ItemSatelliteIdentificationChip.setSatellite), the chassis uses
        // "satId" (SatelliteProperties.writeToNBT). The probe reads both
        // into a unified field so this test pins that the same id is
        // stamped into both stacks during one assembleSatellite call.
        long holdingId = extractLong(resp, HOLDING_SAT_ID);
        long chipId = extractLong(resp, CHIP_SAT_ID);
        assertNotEquals("holdingSatId must be present, not the -1 sentinel: " + resp,
                -1L, holdingId);
        assertEquals("chip + chassis must share the same satelliteId — that's the "
                        + "wiring that lets a player route the chip back to the chassis: "
                        + resp, holdingId, chipId);
    }

    /** Per-type id-chip enforcement: weatherController overrides
     *  {@code SatelliteBase.isAcceptableControllerItemStack} to require
     *  its own dedicated chip (not the default {@code itemSatelliteIdChip}).
     *  The press-build probe loads the default chip into chipSlot, so
     *  {@code canAssembleSatellite()} must return FALSE for
     *  weatherController. Pin protects the per-type-chip contract — a
     *  regression that loses an override would let the wrong chip
     *  silently accept any type. */
    @Test
    public void pressBuildRejectsDefaultChipForChipOverridingType() throws Exception {
        int x = CX_WEATHER, y = CY, z = CZ;
        exec("artest chunk warmup 0 " + (x >> 4) + " " + (z >> 4)
                + " " + (x >> 4) + " " + (z >> 4));
        String place = exec("artest place 0 " + x + " " + y + " " + z
                + " advancedrocketry:satelliteBuilder");
        assertTrue("satellite builder place failed: " + place,
                place.contains("\"placed\":true"));

        String resp = exec("artest satellite-builder press-build 0 "
                + x + " " + y + " " + z + " weatherController");
        // Probe must surface the canAssemble-false branch as an error
        // (not crash, not silently succeed).
        assertTrue("expected canAssembleSatellite=false error for default chip + "
                        + "weatherController: " + resp,
                resp.contains("canAssembleSatellite returned false"));
        // Primary-meta resolution must still succeed — the rejection is
        // about the chip slot, not the registry scan.
        assertNotEquals("weatherController must still resolve a primary meta even "
                        + "though canAssemble fails: " + resp,
                -1, extractInt(resp, PRIMARY_META));
    }

    private static String extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return m.group(1);
    }

    private static int extractInt(String src, Pattern pattern) {
        return Integer.parseInt(extract(src, pattern));
    }

    private static long extractLong(String src, Pattern pattern) {
        return Long.parseLong(extract(src, pattern));
    }
}
