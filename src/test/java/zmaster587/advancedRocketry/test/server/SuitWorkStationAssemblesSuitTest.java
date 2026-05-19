package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TASK-10 Phase 1 (A2 remainder) — suit-workstation component-assembly pin.
 *
 * <p>{@link zmaster587.advancedRocketry.tile.TileSuitWorkStation} is a 5-slot
 * passive container — slot 0 holds the armor piece (any {@code IModularArmor}),
 * slots 1-4 hold the per-armor components. Assembly is NOT ticked: when
 * {@code setInventorySlotContents(slot &gt;= 1, IArmorComponent)} fires, the
 * tile calls {@code addArmorComponent(world, armor, component, slot-1)} on
 * the armor item in slot 0, which mutates the armor's NBT to record the new
 * component. The component is NOT placed in the underlying inventory slot —
 * it's "consumed into" the armor.</p>
 *
 * <p>This test pins the assembly contract end-to-end:</p>
 * <ol>
 *   <li>Place a {@code suitWorkStation} block.</li>
 *   <li>Insert a {@code spaceChestplate} into slot 0 via
 *       {@code /artest hatch fill}. The chestplate starts with no
 *       components — its NBT must NOT contain the jetpack token.</li>
 *   <li>Insert a {@code jetPack} into slot 1. The tile dispatches the
 *       component into the chestplate's NBT via {@code addArmorComponent}.</li>
 *   <li>Re-read the inventory with {@code /artest hatch read ... nbt}:
 *       the chestplate's NBT in slot 0 now contains the {@code jetPack}
 *       token, and slot 1 is still empty (the component was consumed).</li>
 * </ol>
 *
 * <p>A regression that drops the {@code addArmorComponent} dispatch in
 * {@code TileSuitWorkStation.setInventorySlotContents} would leave the
 * chestplate's NBT unchanged — the assertion fires.</p>
 */
public class SuitWorkStationAssemblesSuitTest extends AbstractHeadlessServerTest {

    /** Isolated patch — no collision with MachineDomainSmokeSuite
     *  (highest x ~2200) or other restart/UV tests (2400, 2500). */
    private static final int X = 2700;
    private static final int Y = 64;
    private static final int Z = 2700;

    @Test
    public void chestplateGainsJetpackComponentWhenJetpackPlacedInComponentSlot() throws Exception {
        // 1. Place the suit work station.
        String place = join(client().execute(
                "artest place 0 " + X + " " + Y + " " + Z + " advancedrocketry:suitWorkStation"));
        assertTrue("suitWorkStation place failed: " + place,
                place.contains("\"placed\":true"));

        // Sanity: tile is the expected class + IInventory.
        String info0 = join(client().execute("artest machine info 0 " + X + " " + Y + " " + Z));
        assertTrue("expected TileSuitWorkStation tile: " + info0,
                info0.contains("TileSuitWorkStation"));

        // Init-modules: TileSuitWorkStation.slotArray is populated only when
        // the GUI-open path calls getModules(). On a freshly-placed server
        // tile slotArray is an array of nulls, so setInventorySlotContents(0)
        // NPEs while iterating it. /artest tile init-modules invokes
        // getModules(0, null) on the tile, populating slotArray as a side
        // effect (the player-using ModuleSlotArmor at the end of the method
        // NPEs on null player but by then slotArray is already set; the
        // probe swallows that exception).
        String initMods = join(client().execute(
                "artest tile init-modules 0 " + X + " " + Y + " " + Z));
        assertTrue("init-modules probe failed: " + initMods,
                initMods.contains("\"ok\":true"));

        // 2. Put a fresh spaceChestplate into slot 0.
        String fillArmor = join(client().execute(
                "artest hatch fill 0 " + X + " " + Y + " " + Z + " 0 advancedrocketry:spaceChestplate 1"));
        assertTrue("chestplate fill failed: " + fillArmor,
                fillArmor.contains("\"ok\":true"));

        // 3. Read with NBT — pin baseline. The chestplate must NOT yet have a
        // jetpack component in its NBT.
        String pre = join(client().execute(
                "artest hatch read 0 " + X + " " + Y + " " + Z + " nbt"));
        assertTrue("slot 0 must contain spaceChestplate: " + pre,
                pre.contains("\"item\":\"advancedrocketry:spacechestplate\""));
        assertTrue("fresh chestplate must not contain jetPack token yet — "
                        + "either the component slot pre-populated unexpectedly "
                        + "or a previous test leaked. Response: " + pre,
                !pre.toLowerCase().contains("jetpack"));

        // 4. Put a jetpack into slot 1. Suit work station calls
        // addArmorComponent → mutates the chestplate's NBT.
        String fillJet = join(client().execute(
                "artest hatch fill 0 " + X + " " + Y + " " + Z + " 1 advancedrocketry:jetPack 1"));
        assertTrue("jetPack fill failed: " + fillJet,
                fillJet.contains("\"ok\":true"));

        // 5. Re-read inventory with NBT. Two things must now be observable:
        //    (a) The chestplate's NBT in slot 0 now contains the jetpack
        //        registry name (the component was written into the armor's
        //        outputItems list by addArmorComponent).
        //    (b) Slot 1 reports the jetpack — but NOT because the underlying
        //        EmbeddedInventory stores it there. Production
        //        TileSuitWorkStation.getStackInSlot(slot>=1) read-throughs to
        //        the armor: it returns ((IModularArmor) armor).getComponentInSlot(
        //        armor, slot-1). So slot 1 reporting the jetpack is the
        //        contract: "armor component at index 0 is jetpack".
        String post = join(client().execute(
                "artest hatch read 0 " + X + " " + Y + " " + Z + " nbt"));
        assertTrue("slot 0 must still contain spaceChestplate after component dispatch: " + post,
                post.contains("\"item\":\"advancedrocketry:spacechestplate\""));
        // (a) Chestplate's NBT must now contain the jetpack registry id.
        // Coupling to lower-cased token (Forge normalises resource paths).
        assertTrue("chestplate NBT must contain jetpack reference after addArmorComponent: " + post,
                post.toLowerCase().contains("jetpack"));
        // (b) Slot 1 must read-through to the armor's component 0 (jetpack).
        assertTrue("slot 1 must report the jetpack via getComponentInSlot read-through: " + post,
                post.contains("\"slot\":1") && post.contains("\"item\":\"advancedrocketry:jetpack\""));
    }

    private static String join(java.util.List<String> resp) {
        return String.join("\n", resp);
    }
}
