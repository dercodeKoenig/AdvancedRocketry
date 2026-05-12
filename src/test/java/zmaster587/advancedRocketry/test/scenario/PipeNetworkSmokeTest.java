package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
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
public class PipeNetworkSmokeTest extends AbstractHeadlessServerTest {

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

    private static long parseLong(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Long.parseLong(m.group(1)) : -1L;
    }
}
