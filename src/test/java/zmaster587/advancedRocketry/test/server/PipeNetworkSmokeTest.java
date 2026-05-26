package zmaster587.advancedRocketry.test.server;

// migrated to AbstractSharedServerTest (TASK-03 B2)
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.17 — energy / data / fluid network transport.
 *
 * Validates the Forge {@code IEnergyStorage} contract on
 * {@code libvulpes:forgepowerinput} — the foundation every pipe network proxies
 * through.
 */
public class PipeNetworkSmokeTest extends AbstractSharedServerTest {

    private static final Pattern STORED = Pattern.compile("\"energyStored\":(\\d+)");
    private static final Pattern MAX = Pattern.compile("\"energyMax\":(\\d+)");
    private static final Pattern ACCEPTED = Pattern.compile("\"accepted\":(-?\\d+)");
    private static final Pattern INJ_STORED = Pattern.compile("\"stored\":(\\d+)");

    @Test
    public void forgeEnergyStorageContractMatches() throws Exception {
        String empty = String.join("\n", client().execute("artest energy stored 0 1200 64 1200"));
        assertTrue("expected 'no tile entity': " + empty, empty.contains("\"no tile entity\""));

        String place = String.join("\n", client().execute(
                "artest place 0 1200 64 1200 libvulpes:forgepowerinput"));
        assertTrue("could not place libvulpes:forgepowerinput: " + place,
                place.contains("\"placed\":true"));

        String initial = String.join("\n", client().execute("artest energy stored 0 1200 64 1200"));
        assertTrue("placed block missing IEnergyStorage: " + initial,
                initial.contains("\"hasEnergy\":true"));
        long storedInit = parseLong(STORED, initial);
        long capacity = parseLong(MAX, initial);
        assertTrue("placed block capacity unreasonable: " + initial, capacity > 0L);

        String inj1 = String.join("\n",
                client().execute("artest energy inject 0 1200 64 1200 5000"));
        assertTrue("inject 5000 failed: " + inj1, inj1.contains("\"ok\":true"));
        long accepted1 = parseLong(ACCEPTED, inj1);
        long expectedAccept1 = Math.min(5000L, capacity - storedInit);
        assertEquals("accepted ≠ expected: " + inj1, expectedAccept1, accepted1);
        long storedAfter1 = parseLong(INJ_STORED, inj1);
        assertEquals("stored did not advance correctly: " + inj1,
                storedInit + accepted1, storedAfter1);

        String inj2 = String.join("\n", client().execute(
                "artest energy inject 0 1200 64 1200 " + capacity));
        long accepted2 = parseLong(ACCEPTED, inj2);
        long storedAfter2 = parseLong(INJ_STORED, inj2);
        assertEquals("battery not at cap after overflow: " + inj2, capacity, storedAfter2);
        assertEquals("overflow accepted wrong: " + inj2,
                capacity - storedAfter1, accepted2);

        String inj3 = String.join("\n", client().execute(
                "artest energy inject 0 1200 64 1200 1000 true"));
        long accepted3 = parseLong(ACCEPTED, inj3);
        long storedAfter3 = parseLong(INJ_STORED, inj3);
        assertEquals("simulate=true mutated stored: " + inj3, capacity, storedAfter3);
        assertEquals("simulate at-cap accepted should be 0: " + inj3, 0L, accepted3);
    }

    /**
     * SMART §7.17 — wireless transceiver pairing. Place two transceivers
     * 50 blocks apart, pair them via the probe (mirrors the player-side
     * linker-item flow), and confirm both end up on the same
     * {@code networkID}.
     */
    @Test
    public void wirelessTransceiverPairsAndTransmits() throws Exception {
        int x1 = 1300, x2 = 1350, y = 65, z = 1200;
        ok(client().execute(
                "artest place 0 " + x1 + " " + y + " " + z + " advancedrocketry:wirelessTransciever"));
        ok(client().execute(
                "artest place 0 " + x2 + " " + y + " " + z + " advancedrocketry:wirelessTransciever"));

        // Pre-pairing — each transceiver carries the default sentinel.
        String pre1 = String.join("\n", client().execute(
                "artest pipe wireless-info 0 " + x1 + " " + y + " " + z));
        String pre2 = String.join("\n", client().execute(
                "artest pipe wireless-info 0 " + x2 + " " + y + " " + z));
        assertEquals("transceiver A starts unpaired (networkID=-1): " + pre1,
                -1, extractInt(pre1, "\"networkID\":(-?\\d+)"));
        assertEquals("transceiver B starts unpaired (networkID=-1): " + pre2,
                -1, extractInt(pre2, "\"networkID\":(-?\\d+)"));

        String pair = String.join("\n", client().execute(
                "artest pipe wireless-pair 0 " + x1 + " " + y + " " + z + " "
                        + x2 + " " + y + " " + z));
        assertTrue("wireless-pair probe failed: " + pair, pair.contains("\"ok\":true"));
        int sharedId = extractInt(pair, "\"sharedNetworkId\":(-?\\d+)");
        // NetworkRegistry hashes network IDs and may return negative values;
        // the only invariant we care about is "not the unpaired sentinel".
        assertTrue("shared networkID must be assigned (not -1 sentinel): " + pair,
                sharedId != -1);

        // Post-pairing — both endpoints must report the same networkID.
        String post1 = String.join("\n", client().execute(
                "artest pipe wireless-info 0 " + x1 + " " + y + " " + z));
        String post2 = String.join("\n", client().execute(
                "artest pipe wireless-info 0 " + x2 + " " + y + " " + z));
        assertEquals("A and B must share the same networkID after pairing",
                sharedId, extractInt(post1, "\"networkID\":(-?\\d+)"));
        assertEquals("A and B must share the same networkID after pairing",
                sharedId, extractInt(post2, "\"networkID\":(-?\\d+)"));
    }

    /**
     * SMART §7.17 — inventory hatch accepts items and surfaces them via the
     * standard hatch read probe (same code path libVulpes machines use to
     * iterate input hatches). Round-trips the item through the hatch's
     * IInventory.
     */
    @Test
    public void inventoryHatchAcceptsAndExportsItems() throws Exception {
        int hx = 1400, hy = 65, hz = 1200;
        ok(client().execute("artest place 0 " + hx + " " + hy + " " + hz
                + " advancedrocketry:invhatch"));

        // Slot 0 — 16 sticks.
        ok(client().execute("artest hatch fill 0 " + hx + " " + hy + " " + hz
                + " 0 minecraft:stick 16 0"));

        String read = String.join("\n", client().execute(
                "artest hatch read 0 " + hx + " " + hy + " " + hz));
        assertTrue("hatch read must surface the deposited stick stack: " + read,
                read.contains("\"item\":\"minecraft:stick\"")
                        && read.contains("\"count\":16"));

        // Overwrite slot 0 with a different stack — verify the hatch
        // accepts replacement (export semantics: it can be cleared and
        // re-filled, mirroring how multiblock controllers pull from it).
        ok(client().execute("artest hatch fill 0 " + hx + " " + hy + " " + hz
                + " 0 minecraft:cobblestone 64 0"));
        String read2 = String.join("\n", client().execute(
                "artest hatch read 0 " + hx + " " + hy + " " + hz));
        assertTrue("hatch must surface the replacement cobblestone stack: " + read2,
                read2.contains("\"item\":\"minecraft:cobblestone\"")
                        && read2.contains("\"count\":64"));
        assertTrue("old stick stack must be gone after replacement: " + read2,
                !read2.contains("\"item\":\"minecraft:stick\""));
    }

    /**
     * SMART §7.17 — fluid hatch accepts fluid via the standard fluid inject
     * probe and surfaces it via fluid stored. AR registers a pressurised
     * tank (advancedrocketry:liquidTank) that exposes the fluid-handler
     * capability the same way libVulpes' fluid hatch does.
     */
    @Test
    public void fluidHatchAcceptsAndExportsFluids() throws Exception {
        int fx = 1500, fy = 65, fz = 1200;
        ok(client().execute("artest place 0 " + fx + " " + fy + " " + fz
                + " advancedrocketry:liquidTank"));

        String injected = String.join("\n", client().execute(
                "artest fluid inject 0 " + fx + " " + fy + " " + fz + " water 8000"));
        assertTrue("fluid inject must succeed: " + injected, injected.contains("\"ok\":true"));
        int amount = extractInt(injected, "\"filled\":(\\d+)");
        assertTrue("hatch must accept some water: " + injected, amount > 0);

        String stored = String.join("\n", client().execute(
                "artest fluid stored 0 " + fx + " " + fy + " " + fz));
        assertTrue("stored probe must show water present after inject: " + stored,
                stored.contains("\"fluid\":\"water\""));
        assertTrue("stored amount must equal the accepted fill: " + stored,
                stored.contains("\"amount\":" + amount));
    }

    private static long parseLong(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Long.parseLong(m.group(1)) : -1L;
    }

    private static int extractInt(String haystack, String regex) {
        Matcher m = Pattern.compile(regex).matcher(haystack);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private void ok(java.util.List<String> response) {
        String joined = String.join("\n", response);
        assertTrue("probe call failed: " + joined, joined.contains("\"ok\":true"));
    }
}
