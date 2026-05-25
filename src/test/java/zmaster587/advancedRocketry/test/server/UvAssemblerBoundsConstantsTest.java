package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-22 Phase 1 — bounds-constants delta between
 * {@link zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine} and
 * {@link zmaster587.advancedRocketry.tile.TileUnmannedVehicleAssembler}.
 *
 * <p>The two assemblers cap rocket-pad-bounds-scan differently:</p>
 *
 * <ul>
 *   <li>{@code TileRocketAssemblingMachine}: {@code MAX_SIZE=16, MAX_SIZE_Y=128}.
 *       Lets the player build tall ascending rockets that carry crew to orbit.</li>
 *   <li>{@code TileUnmannedVehicleAssembler}: {@code MAX_SIZE=17, MAX_SIZE_Y=17}.
 *       Lets the player build compact station-deployed drones; height is
 *       capped because they're meant to fit station bays, not crew Saturn V.</li>
 * </ul>
 *
 * <p><b>Why this is a contract, not an impl-detail</b>: a modpack expects
 * UV builders to be small (the GUI hint, the in-world structure-tower
 * hint, the documentation). A regression that swaps the two caps —
 * or unifies them — silently lets players build a 128-tall UV (which would
 * land catastrophically) or a 17-capped regular rocket (which can no longer
 * reach orbit-tier altitudes for some configs). Both are player-visible
 * regressions that no production assertion currently guards against.</p>
 *
 * <p>This test reads the private static final constants via the new
 * {@code /artest assembler max-y} reflective probe — a single round-trip
 * for both classes. The probe is read-only so it doesn't mutate global
 * state (no @After cleanup needed).</p>
 */
public class UvAssemblerBoundsConstantsTest extends AbstractSharedServerTest {

    private static final Pattern ROCKET_MAX_Y =
            Pattern.compile("\"rocketAssemblerMaxY\":(-?\\d+)");
    private static final Pattern UV_MAX_Y =
            Pattern.compile("\"uvAssemblerMaxY\":(-?\\d+)");

    @Test
    public void rocketAssemblerAllowsTallerStructureThanUvAssembler() throws Exception {
        String resp = exec("artest assembler max-y");
        Matcher rm = ROCKET_MAX_Y.matcher(resp);
        Matcher um = UV_MAX_Y.matcher(resp);
        assertTrue("probe must surface rocketAssemblerMaxY: " + resp, rm.find());
        assertTrue("probe must surface uvAssemblerMaxY: " + resp, um.find());
        int rocketMaxY = Integer.parseInt(rm.group(1));
        int uvMaxY = Integer.parseInt(um.group(1));
        assertTrue("rocket assembler MAX_SIZE_Y must exceed UV's so the two "
                        + "assemblers serve their distinct rocket-class roles "
                        + "(rocket=" + rocketMaxY + ", uv=" + uvMaxY + ")",
                rocketMaxY > uvMaxY);
        // Both must be positive — a 0 cap would mean the assembler is unusable.
        assertTrue("rocket MAX_SIZE_Y must be a usable positive cap (got "
                + rocketMaxY + ")", rocketMaxY > 0);
        assertTrue("uv MAX_SIZE_Y must be a usable positive cap (got "
                + uvMaxY + ")", uvMaxY > 0);
    }

    @Test
    public void uvAssemblerHeightCapMatchesItsWidthCap() throws Exception {
        // UV is a compact cube — its height cap equals its width cap by
        // design (both 17). A regression that decouples them (e.g. height
        // unbumped from a default 17 while width changed) would let
        // partial-cube UV rockets be assembled, which would mess with
        // station-bay docking. Pin the cube invariant explicitly.
        String resp = exec("artest assembler max-y");
        Matcher uy = UV_MAX_Y.matcher(resp);
        Matcher ux = Pattern.compile("\"uvAssemblerMaxXZ\":(-?\\d+)").matcher(resp);
        assertTrue("probe must surface uvAssemblerMaxY: " + resp, uy.find());
        assertTrue("probe must surface uvAssemblerMaxXZ: " + resp, ux.find());
        int uvMaxY = Integer.parseInt(uy.group(1));
        int uvMaxXZ = Integer.parseInt(ux.group(1));
        assertTrue("UV's height cap must match its width cap "
                        + "(MAX_SIZE_Y=" + uvMaxY + ", MAX_SIZE=" + uvMaxXZ + ")",
                uvMaxY == uvMaxXZ);
    }
}
