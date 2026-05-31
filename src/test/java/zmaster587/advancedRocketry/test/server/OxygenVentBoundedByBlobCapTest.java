package zmaster587.advancedRocketry.test.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * Coverage-audit Gap S (2026-05-27 audit, deferred by TASK-40c, closed
 * by the 2026-05-31 final audit) — the {@code TileOxygenVent} blob is
 * <b>bounded</b>: a vent cannot pressurise an arbitrarily large sealed
 * space.
 *
 * <p>{@link OxygenVentRequiresFuelAndPowerTest} pins the fuel/power
 * gating but never exercises the size cap. Production enforces the cap
 * in {@code AtmosphereBlob.run} (the seal flood-fill): when the BFS
 * reaches an open cell beyond the cap it does NOT partial-fill — it
 * {@code clearBlob()}s and voids the whole blob (lines 142-146). So the
 * player-visible contract is binary:</p>
 *
 * <ul>
 *   <li>a sealed space <b>within</b> the cap → pressurised
 *       ({@code PRESSURIZEDAIR});</li>
 *   <li>a sealed space <b>larger than</b> the cap → not sealed at all,
 *       stays at the dim baseline ({@code air}).</li>
 * </ul>
 *
 * <p><b>Contract, not impl-pin</b>: we don't assert the exact cap value.
 * We pin the fill mode to the deterministic synchronous, radius-based
 * algorithm ({@code atmosphereHandleBitMask = 0} — a real production
 * config option) and a small radius, then build two corridors that
 * differ <em>only</em> in length — one inside the cap, one past it — and
 * assert the cap is the discriminator. Pinning the mode removes the
 * default threaded-volume fill's timing from the assertion.</p>
 *
 * <p>Failure semantics: goes red if production ever drops the cap check
 * and lets the blob flood an oversized sealed volume — the oversized
 * room's root cell would turn {@code PRESSURIZEDAIR}.</p>
 */
public class OxygenVentBoundedByBlobCapTest extends AbstractSharedServerTest {

    private static final Pattern CFG_VALUE = Pattern.compile("\"value\":(-?\\d+)");
    private static final Pattern ATM_TYPE  = Pattern.compile("\"type\":\"([^\"]*)\"");

    private static final int DIM = 0;
    private static final int CY = 64;
    private static final int CZ = 2800;
    /** Two patches, X-spread far apart so the two blobs never interact. */
    private static final int CX_WITHIN = 2800;
    private static final int CX_OVER   = 3000;

    /** Radius cap (in blocks) for the duration of this test. With the
     *  Euclidean distance check, a corridor whose far cell sits beyond
     *  this radius from the vent root voids the whole blob. */
    private static final int CAP = 8;
    /** Within-cap corridor: max cell distance ~sqrt(4^2+1) ≈ 4.1 < 8. */
    private static final int LEN_WITHIN = 4;
    /** Oversized corridor: a cell at dx=12 sits at ~12 > 8 → voids. */
    private static final int LEN_OVER = 16;

    private int originalVentSize;
    private int originalBitMask;

    @Before
    public void pinDeterministicSmallCap() throws Exception {
        originalVentSize = readConfigInt("oxygenVentSize");
        originalBitMask  = readConfigInt("atmosphereHandleBitMask");
        // bitMask 0 = synchronous, radius-based fill (no threading, no volume
        // cap) — deterministic for the cap-boundary assertion.
        setConfig("atmosphereHandleBitMask", 0);
        setConfig("oxygenVentSize", CAP);
    }

    @After
    public void restoreConfig() throws Exception {
        // Restore so other tests (and the whitelist contract) aren't left
        // with the pinned mode / shrunk cap.
        exec("artest config set oxygenVentSize " + originalVentSize);
        exec("artest config set atmosphereHandleBitMask " + originalBitMask);
    }

    @Test
    public void ventSealsWithinCapButNotBeyondIt() throws Exception {
        // Control: a corridor entirely inside the cap → must pressurise.
        buildSealedCorridor(CX_WITHIN, LEN_WITHIN);
        sealVent(CX_WITHIN);
        String within = atmosphereTypeAt(CX_WITHIN, CY + 1, CZ);

        // Subject: a corridor longer than the cap → blob voids → stays air.
        buildSealedCorridor(CX_OVER, LEN_OVER);
        sealVent(CX_OVER);
        String over = atmosphereTypeAt(CX_OVER, CY + 1, CZ);

        assertTrue("baseline: a sealed corridor within the " + CAP + "-block cap "
                        + "must pressurise — else the vent harness is broken and the "
                        + "oversized assertion proves nothing (within=" + within + ")",
                within.equalsIgnoreCase("PressurizedAir"));
        assertFalse("a sealed corridor longer than the " + CAP + "-block cap must "
                        + "NOT pressurise — the blob voids rather than flooding an "
                        + "oversized volume; the room stays at the dim baseline "
                        + "(over=" + over + ")",
                over.equalsIgnoreCase("PressurizedAir"));
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private int readConfigInt(String key) throws Exception {
        String resp = exec("artest config get " + key);
        Matcher m = CFG_VALUE.matcher(resp);
        assertTrue("could not read config " + key + ": " + resp, m.find());
        return Integer.parseInt(m.group(1));
    }

    private void setConfig(String key, int value) throws Exception {
        String resp = exec("artest config set " + key + " " + value);
        assertTrue("could not set config " + key + ": " + resp, resp.contains("\"ok\":true"));
    }

    /** A fully enclosed 1×1×len air tube running +X from the vent, wrapped
     *  in a solid stone shell. */
    private void buildSealedCorridor(int cx, int len) throws Exception {
        exec("artest fill " + DIM + " " + (cx - 1) + " " + (CY - 1) + " " + (CZ - 1)
                + " " + (cx + len + 1) + " " + (CY + 2) + " " + (CZ + 1) + " minecraft:stone");
        exec("artest fill " + DIM + " " + cx + " " + (CY + 1) + " " + CZ
                + " " + (cx + len) + " " + (CY + 1) + " " + CZ + " minecraft:air");
    }

    private void sealVent(int cx) throws Exception {
        String resp = exec("artest place " + DIM + " " + cx + " " + CY + " " + CZ
                + " advancedrocketry:oxygenVent");
        assertTrue("vent place failed: " + resp, resp.contains("\"placed\":true"));
        String e = exec("artest energy inject " + DIM + " " + cx + " " + CY + " " + CZ + " 1000000");
        assertTrue("energy inject failed: " + e, e.contains("\"ok\":true"));
        String o = exec("artest fluid inject " + DIM + " " + cx + " " + CY + " " + CZ + " oxygen 16000");
        assertTrue("oxygen inject failed: " + o, o.contains("\"ok\":true"));
        exec("artest tile force-tick " + DIM + " " + cx + " " + CY + " " + CZ + " 1");
        exec("artest vent reseal " + DIM + " " + cx + " " + CY + " " + CZ);
        exec("artest tile force-tick " + DIM + " " + cx + " " + CY + " " + CZ + " 5");
    }

    private String atmosphereTypeAt(int x, int y, int z) throws Exception {
        String info = exec("artest atmosphere get " + DIM + " " + x + " " + y + " " + z);
        Matcher m = ATM_TYPE.matcher(info);
        assertTrue("atmosphere type not found in: " + info, m.find());
        return m.group(1);
    }
}
