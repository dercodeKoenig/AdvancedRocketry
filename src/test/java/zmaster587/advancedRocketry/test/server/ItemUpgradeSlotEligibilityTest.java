package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-40c (audit Gap J) — {@link
 * zmaster587.advancedRocketry.item.components.ItemUpgrade} slot
 * eligibility dispatch by meta.
 *
 * <p>Production (lines 92-98 of {@code ItemUpgrade.isAllowedInSlot}):
 * dispatches strictly on {@code componentStack.getItemDamage()}:</p>
 *
 * <ul>
 *   <li>meta = {@code legUpgradeDamage} (2) or
 *       {@code speedUpgradeDamage} (1) → LEGS only.</li>
 *   <li>meta = {@code bootsUpgradeDamage} (3) → FEET only.</li>
 *   <li>any other meta (0, 4, 5, ...) → HEAD only.</li>
 * </ul>
 *
 * <p>Player-visible: armor crafting / module-slot acceptance —
 * placing a leg upgrade into the helmet module slot is rejected by
 * the GUI. Pinning slot eligibility per meta guards against any
 * regression that mixes the slot dispatch (e.g. a bootsUpgrade
 * landing in LEGS).</p>
 *
 * <p>NOT pinned (impl per SOP): the specific magic numbers (2, 3, 1)
 * — only the slot-dispatch outcome matters. If a future refactor
 * renames metas, this test continues to check the outcome.</p>
 */
public class ItemUpgradeSlotEligibilityTest extends AbstractSharedServerTest {

    private static final String ID = "advancedrocketry:itemUpgrade";

    @Test
    public void hoverUpgradeMeta0OnlyFitsHead() throws Exception {
        assertSlots(0, true, false, false, false);
    }

    @Test
    public void flightSpeedMeta1OnlyFitsLegs() throws Exception {
        assertSlots(1, false, false, true, false);
    }

    @Test
    public void bionicLegsMeta2OnlyFitsLegs() throws Exception {
        assertSlots(2, false, false, true, false);
    }

    @Test
    public void landingBootsMeta3OnlyFitsFeet() throws Exception {
        assertSlots(3, false, false, false, true);
    }

    @Test
    public void antiFogVisorMeta4OnlyFitsHead() throws Exception {
        assertSlots(4, true, false, false, false);
    }

    @Test
    public void earthbrightVisorMeta5OnlyFitsHead() throws Exception {
        assertSlots(5, true, false, false, false);
    }

    private void assertSlots(int meta, boolean head, boolean chest, boolean legs, boolean feet)
            throws Exception {
        String resp = exec("artest infra item-armor-slot " + ID + " " + meta + " 1");
        assertTrue("item-armor-slot must succeed: " + resp,
                resp.contains("\"ok\":true"));
        assertTrue("meta=" + meta + " head expected=" + head + "; resp=" + resp,
                resp.contains("\"head\":" + head));
        assertTrue("meta=" + meta + " chest expected=" + chest + "; resp=" + resp,
                resp.contains("\"chest\":" + chest));
        assertTrue("meta=" + meta + " legs expected=" + legs + "; resp=" + resp,
                resp.contains("\"legs\":" + legs));
        assertTrue("meta=" + meta + " feet expected=" + feet + "; resp=" + resp,
                resp.contains("\"feet\":" + feet));
    }

    private String exec(String cmd) throws Exception {
        return String.join("\n", client().execute(cmd));
    }
}
