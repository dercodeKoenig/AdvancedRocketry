package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Coverage-audit gap (2026-05-26 Tier 2 #5) — TileDockingPort
 * persistence + network packet schema.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.station.TileDockingPort}
 * holds two strings ({@code myIdStr}, {@code targetIdStr}) that
 * uniquely identify a station's docking port plus its pairing
 * target. Both strings drive the cross-station docking lookup; if
 * either is lost on save/load or scrambled by the network packet
 * layer, the docking pair silently breaks and players can't dock.
 * No test in the suite touches this tile pre-this-class.</p>
 *
 * <h2>Contracts pinned</h2>
 *
 * <ol>
 *   <li><b>NBT save format</b> — non-empty {@code myIdStr} /
 *       {@code targetIdStr} round-trip through
 *       {@code writeToNBT → readFromNBT}.</li>
 *   <li><b>NBT empty-string handling</b> — empty strings are NOT
 *       written ({@code if (!.isEmpty())} gates in production lines
 *       110-113), but a peer reading the partial NBT recovers
 *       {@code ""} for the missing keys (vanilla
 *       {@code NBTTagCompound.getString} default).</li>
 *   <li><b>Network packet schema</b> — packet id 0 ships
 *       {@code myIdStr}, packet id 1 ships {@code targetIdStr}; the
 *       wire format writes a length-prefixed string and the reader
 *       expects exactly that schema.</li>
 * </ol>
 *
 * <p>Position-isolated at x=9000. Uses
 * {@link AbstractSharedServerTest} for one cold-start per class.</p>
 */
public class DockingPortNbtAndPacketTest extends AbstractSharedServerTest {

    private static final int BASE_X = 9000;
    private static final int BASE_Y = 64;
    private static final int BASE_Z = 9000;

    private static final Pattern MY_ID = Pattern.compile("\"myId\":\"([^\"]*)\"");
    private static final Pattern TARGET_ID = Pattern.compile("\"targetId\":\"([^\"]*)\"");
    private static final Pattern PEER_MY_ID = Pattern.compile("\"peerMyId\":\"([^\"]*)\"");
    private static final Pattern PEER_TARGET_ID = Pattern.compile("\"peerTargetId\":\"([^\"]*)\"");
    private static final Pattern HAS_MY_ID_KEY = Pattern.compile("\"hasMyIdKey\":(true|false)");
    private static final Pattern HAS_TARGET_ID_KEY = Pattern.compile("\"hasTargetIdKey\":(true|false)");
    private static final Pattern DECODED_ID = Pattern.compile("\"decodedId\":\"([^\"]*)\"");
    private static final Pattern PACKET_BYTES = Pattern.compile("\"bytes\":(\\d+)");

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }

    private static void warmup(int blockX, int blockZ) throws Exception {
        int cx = blockX >> 4;
        int cz = blockZ >> 4;
        String resp = join(client().execute(
                "artest chunk warmup 0 " + (cx - 1) + " " + (cz - 1)
                        + " " + (cx + 1) + " " + (cz + 1)));
        assertTrue("chunk warmup failed: " + resp,
                resp.contains("\"ok\":true"));
    }

    /** Place a TileDockingPort at the given coords. The block is
     *  registered as {@code stationMarker} (per AR's AdvancedRocketry
     *  init), not {@code dockingPort} — the registry name and the
     *  tile-entity class name don't have to match in Forge. */
    private static void placeDockingPort(int x, int y, int z) throws Exception {
        String resp = join(client().execute(
                "artest place 0 " + x + " " + y + " " + z
                        + " advancedrocketry:stationMarker"));
        assertTrue("stationMarker place failed at (" + x + "," + y + "," + z
                        + "): " + resp,
                resp.contains("\"placed\":true"));
    }

    private static String extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern " + pattern + " not found in: " + src, m.find());
        return m.group(1);
    }

    private static boolean extractBool(String src, Pattern pattern) {
        return Boolean.parseBoolean(extract(src, pattern));
    }

    @Test
    public void nbtRoundTripPreservesNonEmptyMyIdAndTargetId() throws Exception {
        int x = BASE_X;
        int y = BASE_Y;
        int z = BASE_Z;
        warmup(x, z);
        placeDockingPort(x, y, z);

        String setIds = join(client().execute(
                "artest docking-port set-ids 0 " + x + " " + y + " " + z
                        + " portA stationB"));
        assertTrue("set-ids must succeed: " + setIds,
                setIds.contains("\"ok\":true"));

        String rt = join(client().execute(
                "artest docking-port nbt-roundtrip 0 " + x + " " + y + " " + z));
        assertTrue("nbt-roundtrip must succeed: " + rt,
                rt.contains("\"ok\":true"));

        assertTrue("non-empty myIdStr must serialize a 'myId' NBT key: "
                + rt, extractBool(rt, HAS_MY_ID_KEY));
        assertTrue("non-empty targetIdStr must serialize a 'targetId' NBT key: "
                + rt, extractBool(rt, HAS_TARGET_ID_KEY));
        assertEquals("peer must round-trip myId",
                "portA", extract(rt, PEER_MY_ID));
        assertEquals("peer must round-trip targetId",
                "stationB", extract(rt, PEER_TARGET_ID));
    }

    @Test
    public void freshDockingPortOmitsEmptyStringKeysFromNbt() throws Exception {
        // A freshly-placed tile has myIdStr="" and targetIdStr="" (ctor
        // defaults). Production lines 110-113 gate the NBT writes on
        // !isEmpty, so the keys must NOT appear. The peer reads back
        // "" via vanilla getString-on-missing-key behaviour — no NPE.
        int x = BASE_X + 20;
        int y = BASE_Y;
        int z = BASE_Z;
        warmup(x, z);
        placeDockingPort(x, y, z);

        String rt = join(client().execute(
                "artest docking-port nbt-roundtrip 0 " + x + " " + y + " " + z));
        assertTrue("nbt-roundtrip must succeed: " + rt,
                rt.contains("\"ok\":true"));

        assertEquals("empty myIdStr must NOT be written to NBT",
                false, extractBool(rt, HAS_MY_ID_KEY));
        assertEquals("empty targetIdStr must NOT be written to NBT",
                false, extractBool(rt, HAS_TARGET_ID_KEY));
        assertEquals("peer recovers empty myId on missing key (no NPE)",
                "", extract(rt, PEER_MY_ID));
        assertEquals("peer recovers empty targetId on missing key (no NPE)",
                "", extract(rt, PEER_TARGET_ID));
    }

    @Test
    public void networkPacketIdZeroShipsMyIdString() throws Exception {
        int x = BASE_X + 40;
        int y = BASE_Y;
        int z = BASE_Z;
        warmup(x, z);
        placeDockingPort(x, y, z);

        // Set myId so the packet has something to encode.
        assertTrue(join(client().execute(
                "artest docking-port set-ids 0 " + x + " " + y + " " + z
                        + " gamma omega")).contains("\"ok\":true"));

        String rt = join(client().execute(
                "artest docking-port packet-roundtrip 0 " + x + " " + y + " "
                        + z + " 0"));
        assertTrue("packet-roundtrip id=0 must succeed: " + rt,
                rt.contains("\"ok\":true"));
        assertEquals("packet id=0 must carry myIdStr",
                "gamma", extract(rt, DECODED_ID));
        // The wire is length-prefixed: int (4 bytes) + utf8 bytes for "gamma" (5).
        // Pin "more than 4 bytes consumed" so we know the length prefix +
        // payload actually flowed.
        assertTrue("packet id=0 must consume > 4 bytes (length prefix + chars): "
                + rt, Integer.parseInt(extract(rt, PACKET_BYTES)) > 4);
    }

    @Test
    public void networkPacketIdOneShipsTargetIdString() throws Exception {
        int x = BASE_X + 60;
        int y = BASE_Y;
        int z = BASE_Z;
        warmup(x, z);
        placeDockingPort(x, y, z);

        assertTrue(join(client().execute(
                "artest docking-port set-ids 0 " + x + " " + y + " " + z
                        + " alpha beta")).contains("\"ok\":true"));

        String rt = join(client().execute(
                "artest docking-port packet-roundtrip 0 " + x + " " + y + " "
                        + z + " 1"));
        assertTrue("packet-roundtrip id=1 must succeed: " + rt,
                rt.contains("\"ok\":true"));
        assertEquals("packet id=1 must carry targetIdStr (not myIdStr)",
                "beta", extract(rt, DECODED_ID));
    }
}
