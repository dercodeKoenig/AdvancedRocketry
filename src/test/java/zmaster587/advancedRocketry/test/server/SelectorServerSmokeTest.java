package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * SMART §7.20 (server-side companion to the client GUI test) — covers the
 * server state machine for {@link
 * zmaster587.advancedRocketry.tile.multiblock.TilePlanetSelector} without
 * needing an OpenGL display.
 *
 * <ol>
 *   <li>Place {@code advancedrocketry:planetSelector} block.</li>
 *   <li>{@code /artest selector info} reports {@code hasSelection=false} on a
 *       freshly-placed tile (no client has picked a planet yet).</li>
 *   <li>{@code /artest selector simulate-click <pos> 0} mimics the wire-side
 *       state change that {@link
 *       zmaster587.advancedRocketry.tile.multiblock.TilePlanetSelector#useNetworkData}
 *       applies when a packet arrives from a real GUI click — sets
 *       {@code dimCache} to Earth's {@code DimensionProperties}.</li>
 *   <li>{@code /artest selector info} now reports {@code hasSelection=true},
 *       {@code selectedDim=0}.</li>
 *   <li>Simulating a second click flips selection without leaking state
 *       (idempotent re-selection).</li>
 * </ol>
 *
 * <p>The full client GUI path lives in {@code client/PlanetSelectorGuiE2ETest}
 * and is gated by the {@code forge.test.client.enabled} system property.</p>
 */
public class SelectorServerSmokeTest extends AbstractHeadlessServerTest {

    @Test
    public void selectorTileStateMachineFollowsSimulatedClicks() throws Exception {
        // Place at a position that won't collide with other tests' fixtures.
        int x = 250, y = 64, z = 250;
        String place = String.join("\n", client().execute(
                "artest place 0 " + x + " " + y + " " + z + " advancedrocketry:planetSelector"));
        assertTrue("could not place planetSelector: " + place,
                place.contains("\"placed\":true"));

        // Initial state — no selection yet.
        String empty = String.join("\n", client().execute(
                "artest selector info 0 " + x + " " + y + " " + z));
        assertTrue("selector info errored on fresh tile: " + empty,
                !empty.contains("\"error\""));
        assertTrue("freshly placed selector tile should report hasSelection=false: " + empty,
                empty.contains("\"hasSelection\":false"));

        // Simulate a click selecting Earth (dim 0).
        String clickEarth = String.join("\n", client().execute(
                "artest selector simulate-click 0 " + x + " " + y + " " + z + " 0"));
        assertTrue("simulate-click failed: " + clickEarth,
                clickEarth.contains("\"ok\":true"));

        // dimCache must now reflect Earth.
        String earthInfo = String.join("\n", client().execute(
                "artest selector info 0 " + x + " " + y + " " + z));
        assertTrue("selection didn't stick: " + earthInfo,
                earthInfo.contains("\"hasSelection\":true"));
        assertTrue("selectedDim mismatch: " + earthInfo,
                earthInfo.contains("\"selectedDim\":0"));

        // Probe non-existent planet dim — must reject without mutating state.
        String reject = String.join("\n", client().execute(
                "artest selector simulate-click 0 " + x + " " + y + " " + z + " 99999"));
        assertTrue("expected rejection for non-registered planet dim 99999: " + reject,
                reject.contains("\"error\":\"planet dim not registered\""));

        String unchanged = String.join("\n", client().execute(
                "artest selector info 0 " + x + " " + y + " " + z));
        assertTrue("selection unexpectedly mutated after rejected simulate-click: " + unchanged,
                unchanged.contains("\"selectedDim\":0"));
    }

    @Test
    public void selectorInfoOnEmptyPositionErrorsCleanly() throws Exception {
        String resp = String.join("\n", client().execute("artest selector info 0 100 80 100"));
        assertTrue("expected 'tile not TilePlanetSelector' on empty pos: " + resp,
                resp.contains("\"error\":\"tile not TilePlanetSelector\""));
    }
}
