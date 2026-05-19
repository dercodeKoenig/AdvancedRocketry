package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * TASK-10 Phase 1 (A2 remainder) — fueling-station ⇒ rocket fuel transfer.
 *
 * <p>Pins the {@link zmaster587.advancedRocketry.tile.infrastructure.TileFuelingStation}
 * {@code performFunction} cause-effect: with a fueling station linked to an
 * assembled {@link zmaster587.advancedRocketry.entity.EntityRocket}, containing
 * rocketFuel in its tank and enough power, force-ticking the station must
 * drain the tank AND increase the rocket's {@code LIQUID_MONOPROPELLANT}
 * fuel amount (matched accounting).</p>
 *
 * <p>Steps:</p>
 * <ol>
 *   <li>Build a rocket fixture at (X_ROCKET, ...) via {@code /artest fixture rocket}
 *       and assemble it into an {@code EntityRocket}.</li>
 *   <li>Place a {@code fuelingStation} adjacent to the rocket pad area.</li>
 *   <li>Link the station → rocket via {@code /artest infra link}.</li>
 *   <li>Inject {@code rocketFuel} into the station's tank and feed it
 *       RF via {@code /artest energy inject} so {@code canPerformFunction}
 *       returns true.</li>
 *   <li>Force-tick the station; assert station tank dropped and the rocket's
 *       primary-fuel amount rose (matched accounting, modulo capacity clamp).</li>
 * </ol>
 *
 * <p>A regression that breaks the {@code addFuelAmount} dispatch, the
 * fuel-fluid matching in {@code performFunction}, or the
 * {@code canPerformFunction} guard would fail this test.</p>
 */
public class FuelingStationFuelsAdjacentRocketTest extends AbstractHeadlessServerTest {

    /** Rocket pad center coords — isolated patch (no collisions). */
    private static final int RX = 2800;
    private static final int RY = 64;
    private static final int RZ = 2800;
    /** Fueling station placed 8 blocks away — within max link distance. */
    private static final int FX = RX - 8;
    private static final int FY = RY + 1;
    private static final int FZ = RZ;

    private static final Pattern ENTITY_ID = Pattern.compile("\"entityId\":(-?\\d+)");
    private static final Pattern FUEL_AMOUNT_MONO =
            Pattern.compile("\"LIQUID_MONOPROPELLANT\":\\{\"amount\":(\\d+),\"capacity\":(\\d+)\\}");
    private static final Pattern TANK_AMOUNT = Pattern.compile("\"amount\":(\\d+)");

    @Test
    public void stationDrainsTankAndRocketFuelRisesAfterLinkAndTick() throws Exception {
        // ─── 1. Build + assemble rocket fixture ────────────────────────
        String fixture = join(client().execute(
                "artest fixture rocket 0 " + RX + " " + RY + " " + RZ));
        assertTrue("rocket fixture failed: " + fixture,
                fixture.contains("\"ok\":true"));

        // The fixture places the rocket builder at (RX+2, RY+1, RZ-1).
        int builderX = RX + 2;
        int builderY = RY + 1;
        int builderZ = RZ - 1;
        // /artest fixture rocket already assembles internally. A re-assemble
        // here is idempotent: status comes back as ALREADY_ASSEMBLED with
        // the existing rocket's entityId. We accept either SUCCESS or
        // ALREADY_ASSEMBLED — only the entityId matters downstream.
        String assemble = join(client().execute(
                "artest rocket assemble 0 " + builderX + " " + builderY + " " + builderZ));
        assertTrue("rocket assemble probe errored: " + assemble,
                assemble.contains("\"ok\":true")
                        && (assemble.contains("\"status\":\"SUCCESS\"")
                                || assemble.contains("\"status\":\"ALREADY_ASSEMBLED\"")));
        Matcher em = ENTITY_ID.matcher(assemble);
        assertTrue("could not parse entityId: " + assemble, em.find());
        int rocketId = Integer.parseInt(em.group(1));

        // ─── 2. Place fueling station + read initial rocket fuel ───────
        String placeFs = join(client().execute(
                "artest place 0 " + FX + " " + FY + " " + FZ + " advancedrocketry:fuelingStation"));
        assertTrue("fuelingStation place failed: " + placeFs,
                placeFs.contains("\"placed\":true"));

        String preFuel = join(client().execute("artest rocket fuel " + rocketId));
        Matcher pfm = FUEL_AMOUNT_MONO.matcher(preFuel);
        assertTrue("rocket fuel probe missing LIQUID_MONOPROPELLANT entry: " + preFuel, pfm.find());
        int initialFuel = Integer.parseInt(pfm.group(1));
        int fuelCapacity = Integer.parseInt(pfm.group(2));
        assertTrue("fresh rocket should have ample mono-propellant capacity: cap=" + fuelCapacity
                        + " response=" + preFuel,
                fuelCapacity > 1000);

        // ─── 3. Link station → rocket ──────────────────────────────────
        String link = join(client().execute(
                "artest infra link 0 " + FX + " " + FY + " " + FZ + " " + rocketId));
        assertTrue("infra link failed: " + link, link.contains("\"ok\":true"));

        // ─── 4. Fluid + power into station ─────────────────────────────
        // rocketFuel is the canonical LIQUID_MONOPROPELLANT in
        // ARConfiguration.registerFuel. Inject 8 000 mB (large enough that
        // the per-tick drain consumes only a fraction).
        // Forge's FluidRegistry stores names case-sensitively as registered;
        // AR registers the fluid under "rocketFuel". If the lookup misses
        // (different Forge variant or test profile), retry with the lower-
        // cased form before declaring the inject broken.
        String inject = join(client().execute(
                "artest fluid inject 0 " + FX + " " + FY + " " + FZ + " rocketFuel 8000"));
        if (inject.contains("\"fluid not registered\"")) {
            inject = join(client().execute(
                    "artest fluid inject 0 " + FX + " " + FY + " " + FZ + " rocketfuel 8000"));
        }
        assertTrue("fluid inject failed: " + inject, inject.contains("\"ok\":true"));

        // Charge RF — fueling station consumes 30 RF per operation; 100 000
        // RF is enough for many ticks.
        String energy = join(client().execute(
                "artest energy inject 0 " + FX + " " + FY + " " + FZ + " 100000"));
        assertTrue("energy inject failed: " + energy, energy.contains("\"ok\":true"));

        // Read tank before tick — pin the baseline.
        String preTank = join(client().execute(
                "artest fluid stored 0 " + FX + " " + FY + " " + FZ));
        assertTrue("station must report fluid present: " + preTank,
                preTank.contains("\"hasFluid\":true"));
        Matcher ptm = TANK_AMOUNT.matcher(preTank);
        assertTrue("could not parse tank amount: " + preTank, ptm.find());
        int initialTank = Integer.parseInt(ptm.group(1));
        assertTrue("station tank must be at least 1 000 mB before tick: " + initialTank
                        + " response=" + preTank,
                initialTank >= 1000);

        // ─── 5. Force-tick station → drains tank + fills rocket ────────
        // 200 ticks: enough to traverse many performFunction calls
        // (libVulpes machines fire performFunction once their internal
        // progress timer rolls over — getProgressBarValueDelta=10 means
        // every 10 ticks of update()).
        String tick = join(client().execute(
                "artest tile force-tick 0 " + FX + " " + FY + " " + FZ + " 200"));
        assertTrue("station force-tick errored: " + tick,
                tick.contains("\"ok\":true"));

        // ─── 6. Verify both endpoints of the matched-accounting claim ───
        String postTank = join(client().execute(
                "artest fluid stored 0 " + FX + " " + FY + " " + FZ));
        Matcher post = TANK_AMOUNT.matcher(postTank);
        assertTrue("could not parse post-tick tank amount: " + postTank, post.find());
        int finalTank = Integer.parseInt(post.group(1));
        int tankDrop = initialTank - finalTank;
        assertTrue("station tank must drop after fueling-station tick burst "
                        + "(initial=" + initialTank + " final=" + finalTank
                        + " response=" + postTank + ")",
                tankDrop > 0);

        String postFuel = join(client().execute("artest rocket fuel " + rocketId));
        Matcher pfp = FUEL_AMOUNT_MONO.matcher(postFuel);
        assertTrue("post-tick rocket fuel probe missing MONO entry: " + postFuel, pfp.find());
        int finalFuel = Integer.parseInt(pfp.group(1));
        int fuelGain = finalFuel - initialFuel;
        assertTrue("rocket LIQUID_MONOPROPELLANT must increase after station tick "
                        + "(initial=" + initialFuel + " final=" + finalFuel
                        + " response=" + postFuel + ")",
                fuelGain > 0);
    }

    private static String join(List<String> resp) {
        return String.join("\n", resp);
    }
}
