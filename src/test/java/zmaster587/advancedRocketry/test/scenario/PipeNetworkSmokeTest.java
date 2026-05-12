package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMART §7.17 — energy / data / fluid network transport.
 *
 * <p>Validates Forge {@code IEnergyStorage} contract on a placed
 * {@code libvulpes:forgepowerinput}, which is the transport-layer foundation every AR
 * pipe / hatch chains through:</p>
 *
 * <ol>
 *   <li>Empty position → "no tile entity" (no NPE).</li>
 *   <li>Fresh battery: {@code energyStored=0}, {@code energyMax>0}.</li>
 *   <li>{@code /artest energy inject … 5000} returns
 *       {@code accepted=min(5000,max)}, stored advances.</li>
 *   <li>Second inject of {@code max} pushes battery to capacity and reports
 *       {@code accepted=max-prevStored} (overflow rejected, not double-counted).</li>
 *   <li>{@code /artest energy inject … 1 simulate=true} returns
 *       {@code accepted=1} (or 0 if at cap) without mutating stored —
 *       proves the simulate flag round-trips through receiveEnergy.</li>
 * </ol>
 *
 * <p>Drives the same capability path that {@code TilePowerPipe.transferPower}
 * uses each tick to propagate RF — if this regresses, every pipe network
 * regresses with it.</p>
 */
public class PipeNetworkSmokeTest extends HarnessBoundScenario {

    private static final Pattern STORED = Pattern.compile("\"energyStored\":(\\d+)");
    private static final Pattern MAX = Pattern.compile("\"energyMax\":(\\d+)");
    private static final Pattern ACCEPTED = Pattern.compile("\"accepted\":(-?\\d+)");
    // Inject probe reports current state as {"stored":N,"max":M} (uses short names
    // to distinguish from the queried tile's {"energyStored":...}).
    private static final Pattern INJ_STORED = Pattern.compile("\"stored\":(\\d+)");

    @Override public String id() { return "ar.scenario.pipe_network_smoke"; }
    @Override public String category() { return "P2/networks"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // 1. Empty-pos NPE guard.
        String empty = String.join("\n", client.execute("artest energy stored 0 1200 64 1200"));
        if (!empty.contains("\"no tile entity\"")) {
            context.note("expected 'no tile entity': " + empty);
            return TestStatus.FAILED;
        }

        // 2. Place a libvulpes battery (Forge-energy capability provider).
        String place = String.join("\n",
                client.execute("artest place 0 1200 64 1200 libvulpes:forgepowerinput"));
        if (!place.contains("\"placed\":true")) {
            context.note("could not place libvulpes:forgepowerinput: " + place);
            return TestStatus.FAILED;
        }
        String initial = String.join("\n", client.execute("artest energy stored 0 1200 64 1200"));
        if (!initial.contains("\"hasEnergy\":true")) {
            context.note("battery missing IEnergyStorage: " + initial);
            return TestStatus.FAILED;
        }
        long storedInit = parseLong(STORED, initial);
        long capacity = parseLong(MAX, initial);
        if (capacity <= 0L) {
            context.note("battery capacity unreasonable: " + initial);
            return TestStatus.FAILED;
        }

        // 3. Inject some energy.
        String inj1 = String.join("\n",
                client.execute("artest energy inject 0 1200 64 1200 5000"));
        if (!inj1.contains("\"ok\":true")) {
            context.note("inject 5000 failed: " + inj1);
            return TestStatus.FAILED;
        }
        long accepted1 = parseLong(ACCEPTED, inj1);
        long expectedAccept1 = Math.min(5000L, capacity - storedInit);
        if (accepted1 != expectedAccept1) {
            context.note("accepted=" + accepted1 + " ≠ expected=" + expectedAccept1 + " — " + inj1);
            return TestStatus.FAILED;
        }
        long storedAfter1 = parseLong(INJ_STORED, inj1);
        if (storedAfter1 != storedInit + accepted1) {
            context.note("stored did not advance correctly: init=" + storedInit
                    + " accepted=" + accepted1 + " after=" + storedAfter1);
            return TestStatus.FAILED;
        }

        // 4. Inject capacity — must cap at max, accepted = remaining.
        String inj2 = String.join("\n", client.execute(
                "artest energy inject 0 1200 64 1200 " + capacity));
        long accepted2 = parseLong(ACCEPTED, inj2);
        long storedAfter2 = parseLong(INJ_STORED, inj2);
        if (storedAfter2 != capacity) {
            context.note("battery not at capacity after overflow inject: " + inj2);
            return TestStatus.FAILED;
        }
        if (accepted2 != capacity - storedAfter1) {
            context.note("overflow accepted=" + accepted2 + " ≠ " + (capacity - storedAfter1)
                    + ": " + inj2);
            return TestStatus.FAILED;
        }

        // 5. simulate=true must not mutate. Battery is now at cap; accepted=0.
        String inj3 = String.join("\n", client.execute(
                "artest energy inject 0 1200 64 1200 1000 true"));
        long accepted3 = parseLong(ACCEPTED, inj3);
        long storedAfter3 = parseLong(INJ_STORED, inj3);
        if (storedAfter3 != capacity) {
            context.note("simulate=true mutated stored: " + inj3);
            return TestStatus.FAILED;
        }
        if (accepted3 != 0L) {
            context.note("simulate at-cap accepted=" + accepted3 + " (expected 0): " + inj3);
            return TestStatus.FAILED;
        }

        context.note("battery cap=" + capacity + ", round-trip stored " + storedInit
                + " → " + storedAfter2 + " via energy inject");
        return TestStatus.PASSED;
    }

    private static long parseLong(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Long.parseLong(m.group(1)) : -1L;
    }
}
