package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.TestContext;
import com.github.stannismod.forge.testing.TestStatus;
import com.github.stannismod.forge.testing.server.TestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMART §7.16 — energy systems.
 *
 * <p>Drives a real {@link zmaster587.advancedRocketry.tile.TileSolarPanel} cycle:</p>
 *
 * <ol>
 *   <li>Set world time to noon (so {@code world.isDaytime()} is true).</li>
 *   <li>Place {@code advancedrocketry:solarGenerator} at a high y with sky
 *       access at chunk (0,0).</li>
 *   <li>Confirm initial {@code energyStored=0}.</li>
 *   <li>Force-tick the tile 100 times via {@code /artest tile force-tick}
 *       (synchronous, bypasses world ticker).</li>
 *   <li>Assert {@code energyStored} increased — production
 *       {@code TileSolarPanel.update()} ⇒ {@code energy.acceptEnergy(...)}.</li>
 * </ol>
 *
 * <p>Additionally validates the energy-probe round-trip on a
 * {@code libvulpes:battery} (capability-provider sanity).</p>
 */
public class EnergySystemsSmokeTest extends HarnessBoundScenario {

    private static final Pattern STORED = Pattern.compile("\"energyStored\":(\\d+)");
    private static final Pattern MAX = Pattern.compile("\"energyMax\":(\\d+)");

    @Override public String id() { return "ar.scenario.energy_systems_smoke"; }
    @Override public String category() { return "P2/energy"; }
    @Override public boolean required() { return false; }

    @Override
    protected TestStatus runScenario(TestContext context, TestClient client) throws Exception {
        // 1. Empty-pos NPE guard.
        String empty = String.join("\n", client.execute("artest energy stored 0 1000 64 1000"));
        if (!empty.contains("\"no tile entity\"")) {
            context.note("expected 'no tile entity' on empty pos: " + empty);
            return TestStatus.FAILED;
        }

        // 2. libVulpes creative battery — Forge-energy capability presence.
        String placeBattery = String.join("\n",
                client.execute("artest place 0 1000 64 1000 libvulpes:creativepowerbattery"));
        if (placeBattery.contains("\"placed\":true")) {
            String bat = String.join("\n", client.execute("artest energy stored 0 1000 64 1000"));
            if (!bat.contains("\"hasEnergy\":true")) {
                context.note("libvulpes:creativepowerbattery has no IEnergyStorage capability: " + bat);
                return TestStatus.FAILED;
            }
            long max = parseLong(MAX, bat);
            if (max <= 0L) {
                context.note("creative battery has zero capacity: " + bat);
                return TestStatus.FAILED;
            }
        } else {
            context.note("libvulpes:creativepowerbattery not placeable — skipping battery probe");
        }

        // 3. Solar panel real generation cycle.
        // Place at y=100 to clear all surface biome heightmap (oceans + mountains).
        client.execute("time set day");
        client.execute("weather clear 100000");

        String placeSolar = String.join("\n", client.execute(
                "artest place 0 1100 100 1100 advancedrocketry:solarGenerator"));
        if (!placeSolar.contains("\"placed\":true")) {
            context.note("could not place solarGenerator: " + placeSolar);
            return TestStatus.FAILED;
        }

        // Initial: 0 energy.
        String s0 = String.join("\n", client.execute("artest energy stored 0 1100 100 1100"));
        if (!s0.contains("\"hasEnergy\":true")) {
            context.note("solarGenerator missing IEnergyStorage: " + s0);
            return TestStatus.FAILED;
        }
        long initial = parseLong(STORED, s0);
        if (initial < 0L) {
            context.note("could not read initial energyStored: " + s0);
            return TestStatus.FAILED;
        }

        // Force-tick the tile 100 times.
        String tick = String.join("\n",
                client.execute("artest tile force-tick 0 1100 100 1100 100"));
        if (!tick.contains("\"ok\":true")) {
            context.note("force-tick failed: " + tick);
            return TestStatus.FAILED;
        }

        String s1 = String.join("\n", client.execute("artest energy stored 0 1100 100 1100"));
        long after = parseLong(STORED, s1);
        if (after <= initial) {
            context.note("solarGenerator did not accumulate energy: initial=" + initial
                    + " after-100-ticks=" + after + " response=" + s1);
            return TestStatus.FAILED;
        }

        context.note("solarGenerator: " + initial + " → " + after + " RF over 100 ticks");
        return TestStatus.PASSED;
    }

    private static long parseLong(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Long.parseLong(m.group(1)) : -1L;
    }
}
