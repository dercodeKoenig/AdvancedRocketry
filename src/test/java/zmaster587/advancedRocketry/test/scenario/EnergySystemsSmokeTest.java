package zmaster587.advancedRocketry.test.scenario;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.16 — energy systems.
 *
 * Drives a real {@link zmaster587.advancedRocketry.tile.TileSolarPanel} cycle:
 * place at high y (sky access), force-tick, assert energy accumulated.
 * Additionally validates {@code libvulpes:creativepowerbattery} exposes an
 * {@code IEnergyStorage} capability.
 */
public class EnergySystemsSmokeTest extends AbstractHeadlessServerTest {

    private static final Pattern STORED = Pattern.compile("\"energyStored\":(\\d+)");
    private static final Pattern MAX = Pattern.compile("\"energyMax\":(\\d+)");

    @Test
    public void solarPanelAccumulatesEnergyOverTicks() throws Exception {
        // 1. Empty-pos NPE guard.
        String empty = String.join("\n", client().execute("artest energy stored 0 1000 64 1000"));
        assertTrue("expected 'no tile entity' on empty pos: " + empty,
                empty.contains("\"no tile entity\""));

        // 2. libVulpes creative battery — Forge-energy capability presence (optional).
        String placeBattery = String.join("\n", client().execute(
                "artest place 0 1000 64 1000 libvulpes:creativepowerbattery"));
        if (placeBattery.contains("\"placed\":true")) {
            String bat = String.join("\n", client().execute("artest energy stored 0 1000 64 1000"));
            assertTrue("creative battery missing IEnergyStorage: " + bat,
                    bat.contains("\"hasEnergy\":true"));
            assertTrue("creative battery has zero capacity: " + bat,
                    parseLong(MAX, bat) > 0L);
        }

        // 3. Solar panel real generation.
        client().execute("time set day");
        client().execute("weather clear 100000");
        String placeSolar = String.join("\n", client().execute(
                "artest place 0 1100 100 1100 advancedrocketry:solarGenerator"));
        assertTrue("could not place solarGenerator: " + placeSolar,
                placeSolar.contains("\"placed\":true"));

        String s0 = String.join("\n", client().execute("artest energy stored 0 1100 100 1100"));
        assertTrue("solarGenerator missing IEnergyStorage: " + s0,
                s0.contains("\"hasEnergy\":true"));
        long initial = parseLong(STORED, s0);
        assertTrue("could not read initial energyStored: " + s0, initial >= 0L);

        String tick = String.join("\n", client().execute(
                "artest tile force-tick 0 1100 100 1100 100"));
        assertTrue("force-tick failed: " + tick, tick.contains("\"ok\":true"));

        String s1 = String.join("\n", client().execute("artest energy stored 0 1100 100 1100"));
        long after = parseLong(STORED, s1);
        assertTrue("solarGenerator did not accumulate energy: initial=" + initial
                        + " after-100-ticks=" + after + " response=" + s1,
                after > initial);
    }

    private static long parseLong(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Long.parseLong(m.group(1)) : -1L;
    }
}
