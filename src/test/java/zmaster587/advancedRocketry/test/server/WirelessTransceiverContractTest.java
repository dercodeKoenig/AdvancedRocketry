package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-13 — wireless transceiver contracts (server-tier).
 *
 * <p>Pins the player-visible contracts of {@code TileWirelessTransciever} —
 * the live replacement for the upstream-deprecated pipe blocks
 * (commit {@code 48610953}). The transceiver is the only data-network
 * endpoint a player can place today.</p>
 *
 * <p>Covered contracts (see TASK-13 doc table):</p>
 * <ol>
 *   <li>Pairing branch — both unpaired: fresh id assigned + network exists.</li>
 *   <li>Pairing branch — only A paired: B inherits A's id.</li>
 *   <li>Pairing branch — only B paired: A inherits B's id.</li>
 *   <li>Pairing branch — both paired, different: ids merge to one.</li>
 *   <li>Pairing branch — both paired, same: re-pair is a no-op.</li>
 *   <li>Mode toggle: extract → tile is source on network.</li>
 *   <li>Mode toggle: inject → tile is sink on network.</li>
 *   <li>Enabled toggle round-trip surfaces via wireless-info.</li>
 *   <li>Mode flip swaps source ↔ sink registration on the live network.</li>
 * </ol>
 *
 * <p>Out of scope here (see TASK-13 doc): NBT round-trip across server
 * restart + onLoad role re-registration — those live in
 * {@code WirelessTransceiverRestartTest} which manages its own
 * harness lifecycle. Adjacent-tile {@code IDataHandler} data flow is
 * deferred to a future TASK-13b.</p>
 */
public class WirelessTransceiverContractTest extends AbstractSharedServerTest {

    private static final Pattern NET_ID = Pattern.compile("\"networkID\":(-?\\d+)");
    private static final Pattern SHARED_ID = Pattern.compile("\"sharedNetworkId\":(-?\\d+)");
    private static final Pattern MODE = Pattern.compile("\"mode\":\"(extract|inject)\"");
    private static final Pattern ENABLED = Pattern.compile("\"enabled\":(true|false)");
    private static final Pattern IS_SOURCE = Pattern.compile("\"isSource\":(true|false)");
    private static final Pattern IS_SINK = Pattern.compile("\"isSink\":(true|false)");
    private static final Pattern NETWORK_EXISTS = Pattern.compile("\"networkExists\":(true|false)");

    // Each test method picks a unique BASE_X offset per the
    // AbstractSharedServerTest position-isolation contract. 50 blocks of
    // headroom per method covers up to 4 transceivers per scenario.

    @Test
    public void pairingBothUnpairedAssignsFreshSharedIdRegisteredOnNetwork() throws Exception {
        int baseX = 2000;
        placeAt(baseX, baseX + 25);
        int idA = netIdAt(baseX);
        int idB = netIdAt(baseX + 25);
        assertEquals("A starts unpaired", -1, idA);
        assertEquals("B starts unpaired", -1, idB);

        int shared = pair(baseX, baseX + 25);
        assertNotEquals("freshly-paired id must not be sentinel", -1, shared);
        assertEquals("both endpoints share the new id", shared, netIdAt(baseX));
        assertEquals("both endpoints share the new id", shared, netIdAt(baseX + 25));

        // wireless-role-on-network surfaces networkExists=true once paired
        // (each tile defaults to inject mode → sink on the live network).
        String role = roleAt(baseX);
        assertTrue("network must exist after pairing: " + role,
                extractBool(NETWORK_EXISTS, role));
    }

    @Test
    public void pairingOnlyFirstPairedSpreadsIdToSecond() throws Exception {
        int baseX = 2100;
        placeAt(baseX, baseX + 25, baseX + 50);
        // Pair A↔B to give A a non-sentinel id; C remains unpaired.
        int sharedAB = pair(baseX, baseX + 25);
        assertEquals(-1, netIdAt(baseX + 50));

        // Pair A↔C: only A is paired → C inherits A's id (no new id).
        int sharedAC = pair(baseX, baseX + 50);
        assertEquals("C must inherit A's id, not get a new one",
                sharedAB, sharedAC);
        assertEquals(sharedAB, netIdAt(baseX + 50));
    }

    @Test
    public void pairingOnlySecondPairedSpreadsIdToFirst() throws Exception {
        int baseX = 2200;
        placeAt(baseX, baseX + 25, baseX + 50);
        // Pair B↔C first → B has an id, A is unpaired.
        int sharedBC = pair(baseX + 25, baseX + 50);
        assertEquals(-1, netIdAt(baseX));

        int sharedAB = pair(baseX, baseX + 25);
        assertEquals("A must inherit B's id when only B was paired",
                sharedBC, sharedAB);
        assertEquals(sharedBC, netIdAt(baseX));
    }

    @Test
    public void pairingBothPairedDifferentIdsMergesIntoOne() throws Exception {
        int baseX = 2300;
        placeAt(baseX, baseX + 25, baseX + 50, baseX + 75);
        // Two disjoint networks: A↔B and C↔D.
        int idAB = pair(baseX, baseX + 25);
        int idCD = pair(baseX + 50, baseX + 75);
        assertNotEquals("the two networks must start distinct", idAB, idCD);

        // Bridge them via pairing B↔C: both already-paired with different
        // ids → mergeNetworks fires. The post-merge id must replace at
        // least one of (idAB, idCD); both tiles end up on the merged net.
        int merged = pair(baseX + 25, baseX + 50);
        int postIdB = netIdAt(baseX + 25);
        int postIdC = netIdAt(baseX + 50);
        assertEquals("B and C must share the merged id", postIdB, postIdC);
        assertEquals("pair response merged-id must match the live state",
                merged, postIdB);
    }

    @Test
    public void pairingBothPairedSameIdIsNoOp() throws Exception {
        int baseX = 2400;
        placeAt(baseX, baseX + 25);
        int firstPair = pair(baseX, baseX + 25);
        // Pair again — same two tiles, same id. The probe's branch logic
        // (and onLinkComplete's) treat (id1 == id2 && id1 != -1) as no-op.
        int secondPair = pair(baseX, baseX + 25);
        assertEquals("re-pairing same network is a no-op",
                firstPair, secondPair);
        assertEquals(firstPair, netIdAt(baseX));
        assertEquals(firstPair, netIdAt(baseX + 25));
    }

    @Test
    public void extractModeRegistersTileAsNetworkSource() throws Exception {
        int baseX = 2500;
        placeAt(baseX, baseX + 25);
        pair(baseX, baseX + 25);

        // Default mode is inject (extractMode=false → sink).
        String preInfo = info(baseX);
        assertEquals("default mode is inject (sink)", "inject", extractMode(preInfo));

        // Flip to extract → tile becomes a source on its network.
        setMode(baseX, "extract");
        String postInfo = info(baseX);
        assertEquals("mode field flipped to extract", "extract", extractMode(postInfo));

        String role = roleAt(baseX);
        assertTrue("extract-mode tile must register as source: " + role,
                extractBool(IS_SOURCE, role));
        assertFalse("extract-mode tile must not register as sink: " + role,
                extractBool(IS_SINK, role));
    }

    @Test
    public void injectModeRegistersTileAsNetworkSink() throws Exception {
        int baseX = 2600;
        placeAt(baseX, baseX + 25);
        pair(baseX, baseX + 25);

        // Flip to extract first, then back to inject — exercises the
        // remove+re-add path in both directions.
        setMode(baseX, "extract");
        setMode(baseX, "inject");

        String info = info(baseX);
        assertEquals("inject", extractMode(info));

        String role = roleAt(baseX);
        assertTrue("inject-mode tile must register as sink: " + role,
                extractBool(IS_SINK, role));
        assertFalse("inject-mode tile must not register as source: " + role,
                extractBool(IS_SOURCE, role));
    }

    @Test
    public void modeFlipSwapsSourceAndSinkRegistration() throws Exception {
        int baseX = 2700;
        placeAt(baseX, baseX + 25);
        pair(baseX, baseX + 25);

        // Start inject (default) — sink.
        String r0 = roleAt(baseX);
        assertTrue("default registration is sink", extractBool(IS_SINK, r0));
        assertFalse(extractBool(IS_SOURCE, r0));

        setMode(baseX, "extract");
        String r1 = roleAt(baseX);
        assertTrue("flip to extract → source", extractBool(IS_SOURCE, r1));
        assertFalse("must clear sink registration on flip",
                extractBool(IS_SINK, r1));

        setMode(baseX, "inject");
        String r2 = roleAt(baseX);
        assertTrue("flip back to inject → sink",
                extractBool(IS_SINK, r2));
        assertFalse("must clear source registration on flip back",
                extractBool(IS_SOURCE, r2));
    }

    @Test
    public void enabledToggleRoundTripsViaInfoProbe() throws Exception {
        int baseX = 2800;
        placeAt(baseX);
        String pre = info(baseX);
        assertFalse("default enabled is false", extractBool(ENABLED, pre));

        setEnabled(baseX, true);
        assertTrue(extractBool(ENABLED, info(baseX)));

        setEnabled(baseX, false);
        assertFalse(extractBool(ENABLED, info(baseX)));
    }

    @Test
    public void modeAndEnabledAreIndependent() throws Exception {
        // Player-visible: flipping enabled must not perturb mode, and
        // vice versa. The two are separate GUI toggles.
        int baseX = 2900;
        placeAt(baseX);

        setMode(baseX, "extract");
        setEnabled(baseX, true);
        String s1 = info(baseX);
        assertEquals("extract", extractMode(s1));
        assertTrue(extractBool(ENABLED, s1));

        setEnabled(baseX, false);
        String s2 = info(baseX);
        assertEquals("disabling must not flip mode", "extract", extractMode(s2));
        assertFalse(extractBool(ENABLED, s2));

        setMode(baseX, "inject");
        String s3 = info(baseX);
        assertEquals("inject", extractMode(s3));
        assertFalse("flipping mode must not re-enable", extractBool(ENABLED, s3));
    }

    // --- helpers -----------------------------------------------------------

    private static final int Y = 65;
    private static final int Z = 2000;
    private static final int DIM = 0;

    private void placeAt(int... xs) throws Exception {
        for (int x : xs) {
            String r = String.join("\n", client().execute(
                    "artest place " + DIM + " " + x + " " + Y + " " + Z
                            + " advancedrocketry:wirelessTransciever"));
            assertTrue("place failed at x=" + x + ": " + r,
                    r.contains("\"placed\":true"));
            // Under parallel-fork load the tile entity can lag the block
            // setBlockState (or the chunk holding it can unload between
            // commands). wireless-pair then sees tile=null and flakes.
            // Poll wireless-info until the probe signals it found the tile
            // (response carries `"ok":true`; tile-missing responses carry
            // `"error":...`). Budget 20 × 500 ms — happy path costs one
            // round-trip; non-happy 10 s ceiling absorbs the worst case
            // observed across TASK-27 v5 + TASK-28 v6/v7 reruns.
            String last = "n/a";
            boolean ready = false;
            for (int attempt = 0; attempt < 20; attempt++) {
                last = info(x);
                if (last.contains("\"ok\":true")) {
                    ready = true;
                    break;
                }
                Thread.sleep(500);
            }
            assertTrue("tile entity never materialized at x=" + x + ": " + last, ready);
        }
    }

    private String info(int x) throws Exception {
        return String.join("\n", client().execute(
                "artest pipe wireless-info " + DIM + " " + x + " " + Y + " " + Z));
    }

    private int netIdAt(int x) throws Exception {
        return extractInt(NET_ID, info(x));
    }

    private String roleAt(int x) throws Exception {
        return String.join("\n", client().execute(
                "artest pipe wireless-role-on-network "
                        + DIM + " " + x + " " + Y + " " + Z));
    }

    private int pair(int x1, int x2) throws Exception {
        String r = String.join("\n", client().execute(
                "artest pipe wireless-pair " + DIM + " "
                        + x1 + " " + Y + " " + Z + " "
                        + x2 + " " + Y + " " + Z));
        assertTrue("pair probe failed: " + r, r.contains("\"ok\":true"));
        return extractInt(SHARED_ID, r);
    }

    private void setMode(int x, String mode) throws Exception {
        String r = String.join("\n", client().execute(
                "artest pipe wireless-set-mode " + DIM + " "
                        + x + " " + Y + " " + Z + " " + mode));
        assertTrue("set-mode failed: " + r, r.contains("\"ok\":true"));
    }

    private void setEnabled(int x, boolean enabled) throws Exception {
        String r = String.join("\n", client().execute(
                "artest pipe wireless-set-enabled " + DIM + " "
                        + x + " " + Y + " " + Z + " " + enabled));
        assertTrue("set-enabled failed: " + r, r.contains("\"ok\":true"));
    }

    private static String extractMode(String haystack) {
        Matcher m = MODE.matcher(haystack);
        if (!m.find()) throw new AssertionError("no mode in: " + haystack);
        return m.group(1);
    }

    private static int extractInt(Pattern p, String haystack) {
        Matcher m = p.matcher(haystack);
        if (!m.find()) throw new AssertionError("pattern " + p + " did not match: " + haystack);
        return Integer.parseInt(m.group(1));
    }

    private static boolean extractBool(Pattern p, String haystack) {
        Matcher m = p.matcher(haystack);
        if (!m.find()) throw new AssertionError("pattern " + p + " did not match: " + haystack);
        return Boolean.parseBoolean(m.group(1));
    }
}
