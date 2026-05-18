package zmaster587.advancedRocketry.test.server;

import com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SMART §7.13 — atmosphere / oxygen gameplay.
 *
 * <p>Earth breathable by default → set density 0 → vacuum → restore. Plus
 * depth coverage for the atmosphere detector, CO2 scrubber, gas charge pad,
 * the spacebreathing-enchant air-suit acceptance gate, and the torch-extinguish
 * config path.</p>
 */
public class AtmosphereOxygenSmokeTest extends AbstractHeadlessServerTest {

    @Test
    public void earthDensityZeroFlipsAtmosphereToVacuum() throws Exception {
        String baseline = String.join("\n", client().execute("artest atmosphere get 0 0 70 0"));
        assertTrue("baseline atmosphere probe errored: " + baseline,
                !baseline.contains("\"error\""));
        assertTrue("baseline Earth not breathable — env contamination? " + baseline,
                baseline.contains("\"breathable\":true"));

        String planet = String.join("\n", client().execute("artest planet info 0"));
        int originalDensity = extractInt(planet, "\"atmosphereDensity\":(-?\\d+)");
        assertTrue("could not read Earth atmosphereDensity: " + planet, originalDensity >= 0);

        try {
            String setResp = String.join("\n", client().execute("artest atmosphere set-density 0 0"));
            assertTrue("set-density failed: " + setResp, setResp.contains("\"ok\":true"));
            assertTrue("set-density did not stick: " + setResp,
                    setResp.contains("\"newDensity\":0"));

            String vacResp = String.join("\n", client().execute("artest atmosphere get 0 0 70 0"));
            assertTrue("density=0 should yield non-breathable, got: " + vacResp,
                    vacResp.contains("\"breathable\":false"));
        } finally {
            client().execute("artest atmosphere set-density 0 " + originalDensity);
        }
    }

    /**
     * Place an atmosphere detector on overworld. Its default {@code
     * atmosphereToDetect} is AIR, and overworld has no per-dim atmosphere
     * handler, so {@link zmaster587.advancedRocketry.tile.atmosphere.TileAtmosphereDetector#update()}
     * falls into the no-handler branch where {@code detectedAtm = atmosphereToDetect == AIR}
     * → {@code true} → the block flips to POWERED on the first valid tick.
     * Then re-target the detector to a non-AIR atmosphere (vacuum) and confirm
     * it unpowers — exercises both branches of the update loop.
     */
    @Test
    public void atmosphereDetectorReportsCurrentAtmosphereOnRedstone() throws Exception {
        int bx = 1700, by = 70, bz = 1500;

        // Clear neighbours so the detector's sample loop sees AIR (any opaque
        // block on any face would suppress the AIR branch). 3×3×3 air around
        // the target pos is enough.
        ok(client().execute("artest fill 0 " + (bx - 1) + " " + (by - 1) + " " + (bz - 1)
                + " " + (bx + 1) + " " + (by + 1) + " " + (bz + 1) + " minecraft:air"));

        String place = String.join("\n", client().execute(
                "artest place 0 " + bx + " " + by + " " + bz + " advancedrocketry:oxygenDetection"));
        assertTrue("detector did not place: " + place, place.contains("\"placed\":true"));

        // Snapshot pre-tick — defaults to unpowered.
        String pre = String.join("\n", client().execute(
                "artest atmosphere detector-output 0 " + bx + " " + by + " " + bz));
        assertTrue("pre-tick probe failed: " + pre, pre.contains("\"isDetector\":true"));
        assertEquals("detector should default to AIR mode: " + pre,
                "air", matchOrFail(Pattern.compile("\"detectorMode\":\"([^\"]+)\""), pre));

        // Drive the sample loop directly via probe — TileAtmosphereDetector.update()
        // is gated by world.getWorldTime() % 10 == 0, which force-tick doesn't
        // advance, so the headless harness can't observe a flip without the
        // dedicated detector-force-sample probe.
        String sample1 = String.join("\n", client().execute(
                "artest atmosphere detector-force-sample 0 " + bx + " " + by + " " + bz));
        assertTrue("force-sample failed: " + sample1, sample1.contains("\"ok\":true"));
        assertTrue("AIR target on overworld must report detected=true: " + sample1,
                sample1.contains("\"detected\":true"));

        String postAir = String.join("\n", client().execute(
                "artest atmosphere detector-output 0 " + bx + " " + by + " " + bz));
        assertTrue("detector should be POWERED after detecting AIR: " + postAir,
                postAir.contains("\"powered\":true"));
        assertEquals("strongPower should be 15 when POWERED: " + postAir,
                "15", matchOrFail(Pattern.compile("\"strongPower\":(\\d+)"), postAir));

        // Re-target detector to vacuum — there's no vacuum near here, so the
        // sample loop should report non-detect and the block should unpower.
        String setMode = String.join("\n", client().execute(
                "artest atmosphere detector-set-mode 0 " + bx + " " + by + " " + bz + " vacuum"));
        assertTrue("detector-set-mode failed: " + setMode, setMode.contains("\"ok\":true"));

        String sample2 = String.join("\n", client().execute(
                "artest atmosphere detector-force-sample 0 " + bx + " " + by + " " + bz));
        assertTrue("force-sample (vacuum target) failed: " + sample2,
                sample2.contains("\"ok\":true"));
        assertTrue("vacuum target on overworld must report detected=false: " + sample2,
                sample2.contains("\"detected\":false"));

        String postVacuum = String.join("\n", client().execute(
                "artest atmosphere detector-output 0 " + bx + " " + by + " " + bz));
        assertTrue("detector should be UNPOWERED when looking for vacuum on Earth: "
                + postVacuum, postVacuum.contains("\"powered\":false"));
        assertEquals("strongPower should be 0 when UNPOWERED: " + postVacuum,
                "0", matchOrFail(Pattern.compile("\"strongPower\":(\\d+)"), postVacuum));
    }

    /**
     * Loads a scrubber with a fresh carbonScrubberCartridge and confirms each
     * {@code useCharge()} call increments the cartridge's item damage by
     * exactly one (the production contract that {@link
     * zmaster587.advancedRocketry.tile.atmosphere.TileOxygenVent} relies on
     * when draining scrubbers every 200 ticks in a sealed room with a CO2
     * atmosphere). An exhausted cartridge must report {@code consumed=false}.
     */
    @Test
    public void co2ScrubberRemovesCo2InSealedRoom() throws Exception {
        int bx = 1700, by = 70, bz = 1600;

        // Clear neighbours so the place doesn't replace an arbitrary block.
        ok(client().execute("artest fill 0 " + (bx - 1) + " " + (by - 1) + " " + (bz - 1)
                + " " + (bx + 1) + " " + (by + 1) + " " + (bz + 1) + " minecraft:air"));

        String place = String.join("\n", client().execute(
                "artest place 0 " + bx + " " + by + " " + bz + " advancedrocketry:oxygenScrubber"));
        assertTrue("scrubber did not place: " + place, place.contains("\"placed\":true"));

        // Empty scrubber — useCharge must report consumed=false.
        String emptyConsume = String.join("\n", client().execute(
                "artest scrubber consume 0 " + bx + " " + by + " " + bz));
        assertTrue("empty scrubber must reject useCharge: " + emptyConsume,
                emptyConsume.contains("\"consumed\":false"));

        // Load a fresh cartridge into slot 0.
        String fill = String.join("\n", client().execute(
                "artest hatch fill 0 " + bx + " " + by + " " + bz
                        + " 0 advancedrocketry:carbonScrubberCartridge 1 0"));
        assertTrue("hatch fill failed: " + fill, fill.contains("\"ok\":true"));

        // First consume — should succeed, damage goes 0 → 1.
        String firstConsume = String.join("\n", client().execute(
                "artest scrubber consume 0 " + bx + " " + by + " " + bz));
        assertTrue("first consume should succeed: " + firstConsume,
                firstConsume.contains("\"consumed\":true"));
        int damageBefore = extractInt(firstConsume, "\"damageBefore\":(-?\\d+)");
        int damageAfter = extractInt(firstConsume, "\"damageAfter\":(-?\\d+)");
        assertEquals("damage must increment by exactly 1 per consume — got "
                + damageBefore + " → " + damageAfter,
                damageBefore + 1, damageAfter);

        // Second consume — same contract, damage 1 → 2.
        String secondConsume = String.join("\n", client().execute(
                "artest scrubber consume 0 " + bx + " " + by + " " + bz));
        int secondAfter = extractInt(secondConsume, "\"damageAfter\":(-?\\d+)");
        assertEquals("repeated consume must continue to increment by 1",
                damageAfter + 1, secondAfter);

        // Comparator override drops in 2185-damage brackets — verify the
        // probe surfaces a non-negative override for an in-use cartridge.
        int comp = extractInt(secondConsume, "\"comparatorOverride\":(\\d+)");
        assertTrue("comparator override must be >= 0 when cartridge loaded: " + comp,
                comp >= 0);
    }

    /**
     * Inject 4 000 mB of oxygen into a gas charge pad's tank, then run the
     * pad's player-facing fill code path against a synthetic empty
     * spaceChestplate stack. Asserts that:
     * <ul>
     *   <li>the suit's air rose by exactly the drained amount;</li>
     *   <li>the pad's tank dropped by the same amount.</li>
     * </ul>
     */
    @Test
    public void gasChargePadFillsSuitTank() throws Exception {
        int bx = 1700, by = 70, bz = 1700;

        ok(client().execute("artest fill 0 " + (bx - 1) + " " + (by - 1) + " " + (bz - 1)
                + " " + (bx + 1) + " " + (by + 1) + " " + (bz + 1) + " minecraft:air"));

        String place = String.join("\n", client().execute(
                "artest place 0 " + bx + " " + by + " " + bz + " advancedrocketry:oxygenCharger"));
        assertTrue("charge pad did not place: " + place, place.contains("\"placed\":true"));

        // Pad's tank caps at 16 000 mB; 4 000 leaves headroom for the test
        // either way. We deliberately use less than the chestplate's max-air
        // so the fluid is the limiting factor — the test then verifies that
        // exactly the drained amount lands in the suit.
        String inject = String.join("\n", client().execute(
                "artest fluid inject 0 " + bx + " " + by + " " + bz + " oxygen 4000"));
        assertTrue("oxygen inject into pad failed: " + inject,
                inject.contains("\"ok\":true"));
        int injected = extractInt(inject, "\"filled\":(\\d+)");
        assertTrue("tank should accept some oxygen: " + inject, injected > 0);

        String resp = String.join("\n", client().execute(
                "artest gascharge fill-suit 0 " + bx + " " + by + " " + bz));
        assertTrue("gascharge fill-suit failed: " + resp, resp.contains("\"ok\":true"));
        int filled = extractInt(resp, "\"filled\":(\\d+)");
        int airBefore = extractInt(resp, "\"airBefore\":(\\d+)");
        int airAfter = extractInt(resp, "\"airAfter\":(\\d+)");
        int tankBefore = extractInt(resp, "\"tankBefore\":(\\d+)");
        int tankAfter = extractInt(resp, "\"tankAfter\":(\\d+)");

        assertEquals("airBefore must be 0 — probe starts with empty suit", 0, airBefore);
        assertTrue("filled must be > 0 when tank has oxygen and suit is empty: " + resp,
                filled > 0);
        assertEquals("airAfter must equal filled when suit started empty",
                filled, airAfter);
        assertEquals("tank delta must equal suit fill amount",
                tankBefore - filled, tankAfter);
    }

    /**
     * The space-protection enchant ({@code spacebreathing}) is the synonym
     * the production damage path uses to recognise <em>any</em> armor piece
     * as a valid air container — see {@link zmaster587.advancedRocketry.util.ItemAirUtils#isStackValidAirContainer}.
     * A vanilla diamond chestplate is rejected; the same chestplate with
     * the enchant applied is accepted. That is the bypass branch that lets
     * {@link zmaster587.advancedRocketry.atmosphere.AtmosphereNeedsSuit#isImmune}
     * skip vacuum damage for the wearer.
     */
    @Test
    public void spaceBreathingEnchantBypassesVacuumDamage() throws Exception {
        // Baseline: vanilla armor must NOT register as an air container.
        String bare = String.join("\n", client().execute(
                "artest enchant validates-as-airsuit minecraft:diamond_chestplate false"));
        assertTrue("baseline probe failed: " + bare, bare.contains("\"registered\":true"));
        assertTrue("vanilla diamond chestplate must NOT be an air container: " + bare,
                bare.contains("\"isAirContainer\":false"));

        // With the spacebreathing enchant: same stack now passes the gate.
        String enchanted = String.join("\n", client().execute(
                "artest enchant validates-as-airsuit minecraft:diamond_chestplate true"));
        assertTrue("enchanted probe failed: " + enchanted,
                enchanted.contains("\"registered\":true"));
        assertTrue("spacebreathing-enchanted armor must register as air container: "
                + enchanted, enchanted.contains("\"isAirContainer\":true"));

        // Sanity: the enchant itself is registered (defence in depth — if the
        // registration broke, the probe would still synthesise an enchant
        // entry but isStackValidAirContainer would silently fail).
        String reg = String.join("\n", client().execute(
                "artest enchant check advancedrocketry:spacebreathing"));
        assertTrue("spacebreathing enchant missing: " + reg,
                reg.contains("\"registered\":true"));
    }

    /**
     * SMART §7.13 — config-gated torch extinguish. Drives
     * {@link zmaster587.advancedRocketry.util.AtmosphereBlob}'s per-block
     * effect loop on a single coordinate via probe; verifies both branches:
     * <ol>
     *   <li>vanilla {@code minecraft:torch} → replaced with
     *       {@code advancedrocketry:unlitTorch} (always-on, no config gate);</li>
     *   <li>arbitrary block added to {@code torchBlocks} config → dropped
     *       as item, position cleared to air.</li>
     * </ol>
     */
    @Test
    public void torchExtinguishesInLowOxygenConfig() throws Exception {
        int bx = 1700, by = 70, bz = 1800;

        // Clear neighbourhood so torch placement isn't refused for lack of a
        // valid floor block.
        ok(client().execute("artest fill 0 " + (bx - 2) + " " + (by - 1) + " " + (bz - 2)
                + " " + (bx + 2) + " " + (by + 1) + " " + (bz + 2) + " minecraft:air"));
        // Provide a stone floor for the torch (vanilla torch needs a solid
        // support face).
        ok(client().execute("artest fill 0 " + (bx - 1) + " " + (by - 1) + " " + (bz - 1)
                + " " + (bx + 1) + " " + (by - 1) + " " + (bz + 1) + " minecraft:stone"));

        // ----- Branch 1: vanilla TORCH → blockUnlitTorch -------------------
        String placeTorch = String.join("\n", client().execute(
                "artest place 0 " + bx + " " + by + " " + bz + " minecraft:torch"));
        assertTrue("torch did not place: " + placeTorch,
                placeTorch.contains("\"placed\":true"));
        String preTorch = String.join("\n", client().execute(
                "artest block at 0 " + bx + " " + by + " " + bz));
        assertTrue("pre-extinguish must be minecraft:torch: " + preTorch,
                preTorch.contains("\"block\":\"minecraft:torch\""));

        String exTorch = String.join("\n", client().execute(
                "artest atmosphere extinguish-at 0 " + bx + " " + by + " " + bz));
        assertTrue("extinguish-at failed for torch: " + exTorch,
                exTorch.contains("\"ok\":true"));
        assertTrue("torch must extinguish to unlitTorch — action: " + exTorch,
                exTorch.contains("\"action\":\"extinguished\""));

        String postTorch = String.join("\n", client().execute(
                "artest block at 0 " + bx + " " + by + " " + bz));
        // Forge normalises registry names to lower-case ("unlitTorch" → "unlittorch").
        assertTrue("post-extinguish must be advancedrocketry:unlittorch: " + postTorch,
                postTorch.contains("\"block\":\"advancedrocketry:unlittorch\""));

        // ----- Branch 2: config-listed block → dropped as item -------------
        // Use stone — already on the floor, but we add it to torchBlocks then
        // run the probe on a fresh stone pillar.
        int sx = bx + 2;
        ok(client().execute("artest place 0 " + sx + " " + by + " " + bz + " minecraft:stone"));

        // Clear any prior contents from previous test runs in the same JVM.
        client().execute("artest atmosphere torch-block-clear");

        String addList = String.join("\n", client().execute(
                "artest atmosphere torch-block-add minecraft:stone"));
        assertTrue("torch-block-add failed: " + addList,
                addList.contains("\"ok\":true"));

        String exStone = String.join("\n", client().execute(
                "artest atmosphere extinguish-at 0 " + sx + " " + by + " " + bz));
        assertTrue("extinguish-at on torchBlocks-listed block must drop — "
                + exStone, exStone.contains("\"action\":\"dropped\""));

        String postStone = String.join("\n", client().execute(
                "artest block at 0 " + sx + " " + by + " " + bz));
        assertTrue("post-drop position must be air: " + postStone,
                postStone.contains("\"isAir\":true"));

        // Clean up the torchBlocks list so other tests don't see polluted
        // config state.
        client().execute("artest atmosphere torch-block-clear");
    }

    private void ok(java.util.List<String> response) {
        String joined = String.join("\n", response);
        assertTrue("probe call failed: " + joined, joined.contains("\"ok\":true"));
    }

    private static String matchOrFail(Pattern p, String s) {
        Matcher m = p.matcher(s);
        assertTrue("pattern " + p + " did not match in: " + s, m.find());
        return m.group(1);
    }

    private static int extractInt(String haystack, String regex) {
        Matcher m = Pattern.compile(regex).matcher(haystack);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
}
