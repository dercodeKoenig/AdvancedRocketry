package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static zmaster587.advancedRocketry.test.server.WorldCommandFixtures.exec;

/**
 * TASK-36a — TileTerraformingTerminal chip-recognition + redstone gate.
 *
 * <p>The terraforming terminal is the player-facing tile that wires a
 * programmed BiomeChanger chip to its satellite for the planet-wide
 * biome-mutation loop. Production gates the loop on two distinct
 * conditions (TileTerraformingTerminal.java:135 + :225-234):</p>
 *
 * <ol>
 *   <li>{@code hasValidBiomeChanger()} — the chip in slot 0 must be an
 *       {@code ItemBiomeChanger} whose satellite is registered on the
 *       terminal's dim and is an instance of {@code SatelliteBiomeChanger}.</li>
 *   <li>The block must be receiving indirect redstone power.</li>
 * </ol>
 *
 * <p>If BOTH hold on a server tick, {@code was_enabled_last_tick} flips
 * to true and the block-model STATE property switches on — that's the
 * player-visible "is this terminal actually running" signal driving the
 * progress text and the block texture. If either gate fails, the
 * terminal idles.</p>
 *
 * <p>Contract pinned:</p>
 *
 * <ul>
 *   <li><b>Valid chip + redstone → enabled.</b> After force-ticking
 *       a terminal loaded with a properly-programmed chip and powered
 *       by an adjacent redstone block, {@code was_enabled_last_tick}
 *       and the block STATE property are both true.</li>
 *   <li><b>Valid chip, no redstone → idle.</b> Same chip without
 *       redstone keeps {@code was_enabled_last_tick} false. Pins that
 *       the chip-recognition gate doesn't auto-enable the loop.</li>
 *   <li><b>Empty slot → invalid chip.</b> An unloaded terminal reports
 *       {@code hasValidBiomeChanger() == false}. Pins the early-out
 *       guard so a tick with no chip is safely a no-op.</li>
 * </ul>
 *
 * <p><b>Out of scope</b>: the biome-mutation inner loop (battery
 * extraction, TerraformingHelper get_next_position iteration, actual
 * BiomeHandler.terraform_biomes mutation). The fresh satellite battery
 * starts at 0 energy so the loop's energy-gate breaks immediately;
 * exercising the loop would need a battery-precharge probe plus a
 * working TerraformingHelper fixture, which sits at the boundary of
 * production's chunk-management subsystem. The chip-recognition + power-
 * gate pin here is the minimal contract that protects against the
 * regression "player wires chip + redstone and nothing happens".</p>
 */
public class TerraformingTerminalChipRecognitionTest extends AbstractSharedServerTest {

    private static final Pattern WAS_ENABLED = Pattern.compile("\"wasEnabledLastTick\":(true|false)");
    private static final Pattern BLOCK_ON = Pattern.compile("\"blockStateOn\":(true|false)");
    private static final Pattern HAS_VALID = Pattern.compile("\"hasValidBiomeChanger\":(true|false)");
    private static final Pattern REDSTONE = Pattern.compile("\"redstonePower\":(true|false)");
    private static final Pattern SAT_ID = Pattern.compile("\"id\":(-?\\d+)");

    private static final int CY = 64;
    private static final int CZ = 11000;
    private static final int CX_VALID = 11500;
    private static final int CX_NO_RS = 12000;
    private static final int CX_EMPTY = 12500;

    /** Happy path — chip loaded + redstone applied → enabled. */
    @Test
    public void validChipPlusRedstoneEnablesTheTerminal() throws Exception {
        int x = CX_VALID, y = CY, z = CZ;
        long satId = setupTerminalWithValidChip(x, y, z);
        assertNotEquals("satId must be non-negative after satellite build",
                -1L, satId);

        // Apply redstone via an adjacent redstone_block on the east face.
        String redstone = exec("artest place 0 " + (x + 1) + " " + y + " " + z
                + " minecraft:redstone_block");
        assertTrue("redstone_block place failed: " + redstone,
                redstone.contains("\"placed\":true"));

        // One force-tick is enough — update() reads redstone + slot 0
        // then mutates was_enabled_last_tick and the block state in the
        // same call.
        exec("artest tile force-tick 0 " + x + " " + y + " " + z + " 1");

        String info = exec("artest terraforming terminal-info 0 " + x + " " + y + " " + z);
        assertEquals("hasValidBiomeChanger must be true after chip load: " + info,
                "true", extract(info, HAS_VALID));
        assertEquals("redstone reach must be true after redstone_block placed: " + info,
                "true", extract(info, REDSTONE));
        assertEquals("was_enabled_last_tick must flip to true: " + info,
                "true", extract(info, WAS_ENABLED));
        assertEquals("block STATE property must reflect enabled: " + info,
                "true", extract(info, BLOCK_ON));
    }

    /** Chip valid but no redstone → recognition passes but power gate
     *  keeps the terminal idle. */
    @Test
    public void validChipWithoutRedstoneStaysIdle() throws Exception {
        int x = CX_NO_RS, y = CY, z = CZ;
        long satId = setupTerminalWithValidChip(x, y, z);
        assertNotEquals(-1L, satId);

        // No redstone source placed.
        exec("artest tile force-tick 0 " + x + " " + y + " " + z + " 1");

        String info = exec("artest terraforming terminal-info 0 " + x + " " + y + " " + z);
        assertEquals("chip is still valid — recognition is independent of power: "
                        + info, "true", extract(info, HAS_VALID));
        assertEquals("redstone power must be reported as off: " + info,
                "false", extract(info, REDSTONE));
        assertEquals("was_enabled_last_tick must stay false without redstone: "
                        + info, "false", extract(info, WAS_ENABLED));
        assertEquals("block STATE must stay off without redstone: " + info,
                "false", extract(info, BLOCK_ON));
    }

    /** Empty slot → chip-recognition rejects, terminal idles even with
     *  redstone. Pins the safe early-out branch. */
    @Test
    public void emptySlotReportsInvalidChipAndIdle() throws Exception {
        int x = CX_EMPTY, y = CY, z = CZ;
        exec("artest chunk warmup 0 " + (x >> 4) + " " + (z >> 4)
                + " " + (x >> 4) + " " + (z >> 4));
        String place = exec("artest place 0 " + x + " " + y + " " + z
                + " advancedrocketry:terraformingTerminal");
        assertTrue("terraformingTerminal place failed: " + place,
                place.contains("\"placed\":true"));
        // Apply redstone — proves the gate is on the chip side, not on
        // power side.
        exec("artest place 0 " + (x + 1) + " " + y + " " + z + " minecraft:redstone_block");
        exec("artest tile force-tick 0 " + x + " " + y + " " + z + " 1");

        String info = exec("artest terraforming terminal-info 0 " + x + " " + y + " " + z);
        assertEquals("empty slot must report hasValidBiomeChanger=false: " + info,
                "false", extract(info, HAS_VALID));
        assertEquals("redstone is present but doesn't help without a chip: " + info,
                "true", extract(info, REDSTONE));
        assertEquals("was_enabled_last_tick must stay false on empty slot: " + info,
                "false", extract(info, WAS_ENABLED));
    }

    // --- fixture helpers --------------------------------------------------

    /** Place a terminal, build a SatelliteBiomeChanger on dim 0, load a
     *  programmed chip into slot 0. Returns the satellite id (or -1 on
     *  failure — caller asserts). */
    private long setupTerminalWithValidChip(int x, int y, int z) throws Exception {
        exec("artest chunk warmup 0 " + (x >> 4) + " " + (z >> 4)
                + " " + (x >> 4) + " " + (z >> 4));
        String place = exec("artest place 0 " + x + " " + y + " " + z
                + " advancedrocketry:terraformingTerminal");
        assertTrue("terraformingTerminal place failed: " + place,
                place.contains("\"placed\":true"));

        // Build + register a SatelliteBiomeChanger on dim 0.
        String build = exec("artest satellite-builder build 0 biomeChanger");
        assertTrue("biomeChanger satellite build failed: " + build,
                build.contains("\"ok\":true"));
        Matcher m = SAT_ID.matcher(build);
        if (!m.find()) {
            return -1L;
        }
        long satId = Long.parseLong(m.group(1));

        String load = exec("artest terraforming terminal-load-chip 0 " + x + " " + y + " " + z
                + " " + satId);
        assertTrue("terminal-load-chip failed: " + load, load.contains("\"ok\":true"));
        return satId;
    }

    private static String extract(String src, Pattern pattern) {
        Matcher m = pattern.matcher(src);
        assertTrue("pattern not found in: " + src, m.find());
        return m.group(1);
    }
}
