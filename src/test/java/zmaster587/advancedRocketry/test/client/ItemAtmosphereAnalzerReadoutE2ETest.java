package zmaster587.advancedRocketry.test.client;

import com.github.stannismod.forge.testing.junit.AbstractClientE2ETest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10b Phase 7 — player-visible side of
 * {@link zmaster587.advancedRocketry.item.ItemAtmosphereAnalzer#onItemRightClick}.
 *
 * <p>Production dispatches TWO chat lines on right-click:</p>
 * <ul>
 *   <li>line 1: {@code "%s %s %s"} wrapping
 *       ({@code msg.atmanal.atmtype}, atmType name, pressure string)</li>
 *   <li>line 2: {@code "%s %s"} wrapping
 *       ({@code msg.atmanal.canbreathe}, {@code msg.yes} or {@code msg.no})</li>
 * </ul>
 *
 * <p>On dim 0 there's typically no {@code AtmosphereHandler} registered,
 * so {@code getOxygenHandler} returns null and
 * {@code getAtmosphereReadout} substitutes {@code AtmosphereType.AIR}
 * — the i18n suffix is the literal {@code "air"} (from
 * {@code AtmosphereType.AIR.getUnlocalizedName()}) and breathable=yes.
 * That is the contract pinned here: a vanilla-dim right-click reports
 * AIR + breathable, regardless of whether an oxygen handler exists.</p>
 *
 * <p>The chat-tap captures translation keys by joining the outer key
 * with every nested translation key (DFS) separated by {@code |}; tests
 * assert on substring presence so they don't depend on i18n output.</p>
 *
 * <p>Gated by {@code forge.test.client.enabled=true}; auto-skips on
 * headless CI.</p>
 */
public class ItemAtmosphereAnalzerReadoutE2ETest extends AbstractClientE2ETest {

    /** Dim 0 has no AtmosphereHandler → production falls back to
     *  AtmosphereType.AIR. Both lines must reach the player: line 1
     *  carries msg.atmanal.atmtype + the AIR i18n suffix ("air"), line 2
     *  carries msg.atmanal.canbreathe + msg.yes (AIR is breathable). */
    @Test
    public void rightClickInVanillaDimDispatchesAirReadoutToPlayer() throws Exception {
        serverClient().execute("artest player chat-clear");
        String resp = String.join("\n", serverClient().execute(
                "artest player try-atm-analyze 0"));
        assertFalse("try-atm-analyze must not error; resp=" + resp,
                resp.contains("\"error\""));
        // Exactly two messages must have been dispatched.
        assertTrue("expected messageCount=2; resp=" + resp,
                resp.contains("\"messageCount\":2"));

        // Line 1 (atmType): outer format + msg.atmanal.atmtype + AIR
        // i18n key "air". All three must be present in the captured key.
        assertTrue("line 1 must include msg.atmanal.atmtype; resp=" + resp,
                resp.contains("msg.atmanal.atmtype"));
        assertTrue("line 1 must include the AIR atm-name key (\"air\"); resp=" + resp,
                resp.contains("|air"));

        // Line 2 (canbreathe): outer format + msg.atmanal.canbreathe + msg.yes
        assertTrue("line 2 must include msg.atmanal.canbreathe; resp=" + resp,
                resp.contains("msg.atmanal.canbreathe"));
        assertTrue("line 2 must include msg.yes (AIR is breathable); resp=" + resp,
                resp.contains("msg.yes"));
        // And must NOT report msg.no (no false negatives on a breathable atm).
        assertFalse("line 2 must NOT report msg.no for breathable AIR; resp=" + resp,
                resp.contains("msg.no"));
    }

    /** Probe must surface an error JSON when the dim arg is missing,
     *  matching the rest of the /artest player error envelope. Catches
     *  accidental signature changes that would silently no-op. */
    @Test
    public void tryAtmAnalyzeErrorsWithoutDim() throws Exception {
        String resp = String.join("\n", serverClient().execute(
                "artest player try-atm-analyze"));
        assertTrue("missing args must surface an error; resp=" + resp,
                resp.contains("\"error\""));
    }

    /** Probe must surface a clear error for an unloaded dim rather than
     *  silently emitting no messages — catches typo'd dim ids. */
    @Test
    public void tryAtmAnalyzeErrorsForUnloadedDim() throws Exception {
        String resp = String.join("\n", serverClient().execute(
                "artest player try-atm-analyze 999999"));
        assertTrue("unloaded dim must surface an error; resp=" + resp,
                resp.contains("\"error\""));
        assertTrue("error must identify the dim id; resp=" + resp,
                resp.contains("\"dim\":999999"));
    }
}
