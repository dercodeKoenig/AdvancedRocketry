package zmaster587.advancedRocketry.test.server;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TASK-10 Phase 2 (B3) — machine-domain smoke suite.
 *
 * <p>Consolidates 8 single-method smoke classes that each previously spawned
 * their own dedicated-server JVM (boot cost ~12 s × 8 ≈ 96 s wall) into a
 * single class scoped under {@link AbstractSharedServerTest} (one boot for
 * the whole suite).</p>
 *
 * <p>Method names are preserved verbatim from the original classes so failure
 * messages remain grep-able against historical CI output. Each method's
 * preamble comment names its source class.</p>
 *
 * <h2>Consolidated from</h2>
 * <ul>
 *   <li>{@code MultiMachineControllerSmokeTest} → {@link #allMachineControllersPlaceTickAndHaveRecipes()}</li>
 *   <li>{@code MultiblockValidationSmokeTest}  → {@link #cuttingMachineMultiblockValidatesAndInvalidates()}</li>
 *   <li>{@code EnergySystemsSmokeTest}         → {@link #solarPanelAccumulatesEnergyOverTicks()}</li>
 *   <li>{@code SealedRoomOxygenVentTest}       → {@link #sealedRoomBecomesBreathableThenLeaks()}</li>
 *   <li>{@code SuitVacuumSubsystemSmokeTest}   → {@link #suitItemsAndEnchantAreWiredUp()}</li>
 *   <li>{@code SpecialInfrastructureSmokeTest} → {@link #allSpecialBlocksPlaceAndTickWithoutException()}</li>
 *   <li>{@code MicrowaveReceiverSmokeTest}     → {@link #multiblockValidatesAndTicksWithoutCrash()}</li>
 *   <li>{@code BlackHoleGeneratorSmokeTest}    → {@link #controllerWithoutStructureTicksWithoutCrash()}</li>
 * </ul>
 *
 * <h2>NOT consolidated: {@code ForceFieldProjectionSmokeTest}</h2>
 *
 * <p>The force-field projector relies on the server's natural tick loop to
 * advance {@code world.getTotalWorldTime()} past the {@code % 5 == 0} gate
 * that drives extension range. In the shared harness, by the time
 * {@code poweredProjectorProjectsAndUnpoweredCollapses} runs, the projector's
 * chunk may have been unloaded by chunk eviction from the prior 7 tests,
 * stalling extension at range=0 despite the redstone being detected. Keeps
 * the test isolated in its original {@code ForceFieldProjectionSmokeTest}
 * class extending {@link com.github.stannismod.forge.testing.junit.AbstractHeadlessServerTest}
 * — one extra JVM-boot, deterministic behaviour.</p>
 *
 * <h2>State-leak audit</h2>
 *
 * <p>The shared-harness contract (see {@link AbstractSharedServerTest})
 * forbids state leaks between methods. Audit per method:</p>
 * <ul>
 *   <li><b>Position isolation</b>: each method uses a unique base-coordinate
 *       patch (see method-level comments). Patches do not overlap.</li>
 *   <li><b>Atmosphere density</b>: only
 *       {@link #suitItemsAndEnchantAreWiredUp()} mutates it, and restores
 *       in {@code finally}.</li>
 *   <li><b>Time / weather</b>: {@link #solarPanelAccumulatesEnergyOverTicks()}
 *       sets {@code day} + {@code clear} (intentional, doesn't restore — both
 *       are friendly state for every other method in this suite).</li>
 *   <li><b>Force-field projector</b>: placed by
 *       {@link #allSpecialBlocksPlaceAndTickWithoutException()} (at 720,64,700)
 *       but never powered, so no field blocks are projected. The powered/
 *       collapse cycle is tested separately in
 *       {@code ForceFieldProjectionSmokeTest} (see "NOT consolidated" note
 *       above).</li>
 * </ul>
 */
public class MachineDomainSmokeSuite extends AbstractSharedServerTest {

    // ── Shared regex patterns ─────────────────────────────────────────────

    private static final Pattern ENERGY_STORED = Pattern.compile("\"energyStored\":(\\d+)");
    private static final Pattern ENERGY_MAX = Pattern.compile("\"energyMax\":(\\d+)");
    private static final Pattern TICKED = Pattern.compile("\"ticked\":(\\d+)");
    private static final Pattern MULTIBLOCK_SAWBLADE_POS =
            Pattern.compile("\"sawBladePos\":\\[(-?\\d+),(-?\\d+),(-?\\d+)]");
    private static final Pattern VENT_SEALED = Pattern.compile("\"isSealed\":(true|false)");
    private static final Pattern VENT_BLOB_SIZE = Pattern.compile("\"blobSize\":(-?\\d+)");
    private static final Pattern VENT_FLUID_AMT = Pattern.compile("\"fluidAmount\":(\\d+)");
    private static final Pattern VENT_BREATHABLE = Pattern.compile("\"breathable\":(true|false)");
    private static final Pattern PLANET_DENSITY = Pattern.compile("\"atmosphereDensity\":(-?\\d+)");

    // ── Machine block-id → expected Tile* short class name (TASK-03 reused) ─

    /** Machine block id → expected Tile* class short name. */
    private static final Map<String, String> MACHINES = new LinkedHashMap<>();
    static {
        MACHINES.put("advancedrocketry:rollingMachine",             "TileRollingMachine");
        MACHINES.put("advancedrocketry:lathe",                      "TileLathe");
        MACHINES.put("advancedrocketry:crystallizer",               "TileCrystallizer");
        MACHINES.put("advancedrocketry:electrolyser",               "TileElectrolyser");
        MACHINES.put("advancedrocketry:chemicalReactor",            "TileChemicalReactor");
        MACHINES.put("advancedrocketry:centrifuge",                 "TileCentrifuge");
        MACHINES.put("advancedrocketry:arcfurnace",                 "TileElectricArcFurnace");
        MACHINES.put("advancedrocketry:precisionassemblingmachine", "TilePrecisionAssembler");
        MACHINES.put("advancedrocketry:precisionlaseretcher",       "TilePrecisionLaserEtcher");
    }

    // ─────────────────────────────────────────────────────────────────────
    // From MultiMachineControllerSmokeTest — SMART §7.7
    // Position patch: x=2100..2140 step 5, y=64, z=2100
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Controller-level recipe-machine smoke for all 9 non-cutting AR
     * multiblock controllers: place + tile-class assert + bare try-complete
     * (false) + force-tick + recipe-summary presence.
     */
    @Test
    public void allMachineControllersPlaceTickAndHaveRecipes() throws Exception {
        // recipes-summary baseline.
        String summary = join(client().execute("artest machine recipes-summary"));
        assertTrue("recipes-summary errored: " + summary,
                !summary.contains("\"error\""));

        // Layout: row of machines on flat stone at y=64, x=2100..2140 step 5.
        int y = 64;
        int z = 2100;
        int xOff = 2100;

        StringBuilder failures = new StringBuilder();
        int idx = 0;
        for (Map.Entry<String, String> e : MACHINES.entrySet()) {
            String blockId = e.getKey();
            String tileClass = e.getValue();
            int x = xOff + idx * 5;
            idx++;

            String place = join(client().execute(
                    "artest place 0 " + x + " " + y + " " + z + " " + blockId));
            if (!place.contains("\"placed\":true")) {
                failures.append(blockId).append("=PLACE_FAILED(").append(place).append(");\n");
                continue;
            }

            String info = join(client().execute(
                    "artest machine info 0 " + x + " " + y + " " + z));
            if (!info.contains(tileClass)) {
                failures.append(blockId).append("=WRONG_TILE_CLASS(expected ")
                        .append(tileClass).append("; got: ").append(info).append(");\n");
                continue;
            }

            String tryComplete = join(client().execute(
                    "artest machine try-complete 0 " + x + " " + y + " " + z));
            if (!tryComplete.contains("\"isComplete\":false")) {
                failures.append(blockId).append("=BARE_TRY_COMPLETE_NOT_FALSE(")
                        .append(tryComplete).append(");\n");
                continue;
            }

            String tick = join(client().execute(
                    "artest tile force-tick 0 " + x + " " + y + " " + z + " 20"));
            if (!tick.contains("\"ok\":true")) {
                failures.append(blockId).append("=TICK_FAILED(").append(tick).append(");\n");
                continue;
            }
            Matcher tm = TICKED.matcher(tick);
            if (!tm.find() || Integer.parseInt(tm.group(1)) != 20) {
                failures.append(blockId).append("=INCOMPLETE_TICK(").append(tick).append(");\n");
                continue;
            }

            String postInfo = join(client().execute(
                    "artest machine info 0 " + x + " " + y + " " + z));
            if (!postInfo.contains(tileClass)) {
                failures.append(blockId).append("=POST_TICK_TILE_LOST(")
                        .append(postInfo).append(");\n");
                continue;
            }

            Pattern p = Pattern.compile("\"" + tileClass + "\":(-?\\d+|\"[^\"]+\")");
            if (!p.matcher(summary).find()) {
                failures.append(blockId).append("=NOT_IN_RECIPE_SUMMARY;\n");
                continue;
            }
        }
        assertEquals("machine smoke failures:\n" + failures, "", failures.toString());
    }

    // ─────────────────────────────────────────────────────────────────────
    // From MultiblockValidationSmokeTest — SMART §7.8
    // Position patch: cutting fixture at (300,64,300); probe sanity at (200..212, 100..102, 200..212)
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void cuttingMachineMultiblockValidatesAndInvalidates() throws Exception {
        // Step 0 — fixture-builder primitives still healthy.
        String emptyInfo = join(client().execute("artest machine info 0 200 100 200"));
        assertTrue("empty position machine info wrong: " + emptyInfo,
                emptyInfo.contains("\"error\":\"no tile entity\""));
        String fill = join(client().execute(
                "artest fill 0 210 100 210 212 102 212 minecraft:stone"));
        assertTrue("fill 3x3x3 stone failed: " + fill,
                fill.contains("\"ok\":true") && fill.contains("\"volume\":27"));

        // Step 1 — build the multiblock fixture.
        int cx = 300, cy = 64, cz = 300;
        String fixture = join(client().execute(
                "artest fixture machine cutting 0 " + cx + " " + cy + " " + cz));
        assertTrue("fixture machine cutting failed: " + fixture,
                fixture.contains("\"ok\":true"));

        Matcher m = MULTIBLOCK_SAWBLADE_POS.matcher(fixture);
        assertTrue("could not parse sawBladePos: " + fixture, m.find());
        int sx = Integer.parseInt(m.group(1)),
                sy = Integer.parseInt(m.group(2)),
                sz = Integer.parseInt(m.group(3));

        // Step 2 — try-complete on the controller → isComplete=true.
        String complete = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("try-complete errored: " + complete, complete.contains("\"ok\":true"));
        assertTrue("structure didn't validate (isComplete=false): " + complete,
                complete.contains("\"isComplete\":true"));

        // Step 3 — break the sawblade → re-validate → isComplete=false.
        String breakBlock = join(client().execute(
                "artest place 0 " + sx + " " + sy + " " + sz + " minecraft:air"));
        assertTrue("could not replace sawBlade with air: " + breakBlock,
                breakBlock.contains("\"ok\":true"));

        String broken = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("try-complete errored after break: " + broken, broken.contains("\"ok\":true"));
        assertTrue("structure stayed complete after sawBlade removal — validator broken: " + broken,
                broken.contains("\"isComplete\":false"));

        // Step 4 — restore the sawblade → re-validate → isComplete=true again.
        String restore = join(client().execute(
                "artest place 0 " + sx + " " + sy + " " + sz + " advancedrocketry:sawBlade"));
        assertTrue("could not restore sawBlade: " + restore,
                restore.contains("\"placed\":true"));

        String recomplete = join(client().execute(
                "artest machine try-complete 0 " + cx + " " + cy + " " + cz));
        assertTrue("validator failed to re-detect a restored structure: " + recomplete,
                recomplete.contains("\"isComplete\":true"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // From EnergySystemsSmokeTest — SMART §7.16
    // Position patch: battery at (1000,64,1000), solar panel at (1100,100,1100).
    // Friendly globals: time=day, weather=clear. Not restored (no test in this
    // suite depends on natural time/weather).
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void solarPanelAccumulatesEnergyOverTicks() throws Exception {
        // 1. Empty-pos NPE guard.
        String empty = join(client().execute("artest energy stored 0 1000 64 1000"));
        assertTrue("expected 'no tile entity' on empty pos: " + empty,
                empty.contains("\"no tile entity\""));

        // 2. libVulpes creative battery — Forge-energy capability presence (optional).
        String placeBattery = join(client().execute(
                "artest place 0 1000 64 1000 libvulpes:creativepowerbattery"));
        if (placeBattery.contains("\"placed\":true")) {
            String bat = join(client().execute("artest energy stored 0 1000 64 1000"));
            assertTrue("creative battery missing IEnergyStorage: " + bat,
                    bat.contains("\"hasEnergy\":true"));
            assertTrue("creative battery has zero capacity: " + bat,
                    parseLong(ENERGY_MAX, bat) > 0L);
        }

        // 3. Solar panel real generation.
        client().execute("time set day");
        client().execute("weather clear 100000");
        String placeSolar = join(client().execute(
                "artest place 0 1100 100 1100 advancedrocketry:solarGenerator"));
        assertTrue("could not place solarGenerator: " + placeSolar,
                placeSolar.contains("\"placed\":true"));

        String s0 = join(client().execute("artest energy stored 0 1100 100 1100"));
        assertTrue("solarGenerator missing IEnergyStorage: " + s0,
                s0.contains("\"hasEnergy\":true"));
        long initial = parseLong(ENERGY_STORED, s0);
        assertTrue("could not read initial energyStored: " + s0, initial >= 0L);

        String tick = join(client().execute(
                "artest tile force-tick 0 1100 100 1100 100"));
        assertTrue("force-tick failed: " + tick, tick.contains("\"ok\":true"));

        String s1 = join(client().execute("artest energy stored 0 1100 100 1100"));
        long after = parseLong(ENERGY_STORED, s1);
        assertTrue("solarGenerator did not accumulate energy: initial=" + initial
                        + " after-100-ticks=" + after + " response=" + s1,
                after > initial);
    }

    // ─────────────────────────────────────────────────────────────────────
    // From SealedRoomOxygenVentTest — SMART §7.13
    // Position patch: 5×5×4 room centred at (1500,64,1500). Vent at floor.
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void sealedRoomBecomesBreathableThenLeaks() throws Exception {
        int bx = 1500, by = 64, bz = 1500;

        ok(client().execute("artest fill 0 " + (bx - 2) + " " + (by - 1) + " " + (bz - 2)
                + " " + (bx + 2) + " " + by + " " + (bz + 2) + " minecraft:stone"));

        for (int yy = by + 1; yy <= by + 2; yy++) {
            ok(client().execute("artest fill 0 " + (bx - 2) + " " + yy + " " + (bz - 2)
                    + " " + (bx + 2) + " " + yy + " " + (bz + 2) + " minecraft:stone"));
            ok(client().execute("artest fill 0 " + (bx - 1) + " " + yy + " " + (bz - 1)
                    + " " + (bx + 1) + " " + yy + " " + (bz + 1) + " minecraft:air"));
        }

        ok(client().execute("artest fill 0 " + (bx - 2) + " " + (by + 3) + " " + (bz - 2)
                + " " + (bx + 2) + " " + (by + 3) + " " + (bz + 2) + " minecraft:stone"));

        String place = join(client().execute(
                "artest place 0 " + bx + " " + by + " " + bz + " advancedrocketry:oxygenVent"));
        assertTrue("vent did not place: " + place, place.contains("\"placed\":true"));

        String preTick = join(client().execute(
                "artest vent info 0 " + bx + " " + by + " " + bz));
        assertTrue("probe must recognise the vent tile: " + preTick,
                preTick.contains("\"isVent\":true"));

        String fluidFill = join(client().execute(
                "artest fluid inject 0 " + bx + " " + by + " " + bz + " oxygen 16000"));
        assertTrue("oxygen fill failed: " + fluidFill, fluidFill.contains("\"ok\":true"));

        String energyFill = join(client().execute(
                "artest energy inject 0 " + bx + " " + by + " " + bz + " 1000000"));
        assertTrue("energy fill failed: " + energyFill, energyFill.contains("\"ok\":true"));

        String fueled = join(client().execute(
                "artest vent info 0 " + bx + " " + by + " " + bz));
        assertTrue("vent should report fluid after inject: " + fueled,
                VENT_FLUID_AMT.matcher(fueled).find()
                        && Integer.parseInt(matchOrFail(VENT_FLUID_AMT, fueled)) > 0);

        client().execute("artest tile force-tick 0 " + bx + " " + by + " " + bz + " 1");

        String reseal = join(client().execute(
                "artest vent reseal 0 " + bx + " " + by + " " + bz));
        assertTrue("vent reseal probe failed: " + reseal,
                reseal.contains("\"ok\":true"));

        client().execute("artest tile force-tick 0 " + bx + " " + by + " " + bz + " 5");

        String sealed = join(client().execute(
                "artest vent info 0 " + bx + " " + by + " " + bz));
        assertEquals("vent must be sealed after reseal+tick: " + sealed,
                "true", matchOrFail(VENT_SEALED, sealed));
        int sealedBlobSize = Integer.parseInt(matchOrFail(VENT_BLOB_SIZE, sealed));
        assertTrue("vent blob must include the interior (>=18): " + sealed,
                sealedBlobSize >= 18);

        String atm = join(client().execute(
                "artest atmosphere get 0 " + bx + " " + (by + 1) + " " + bz));
        assertEquals("interior must be breathable when sealed: " + atm,
                "true", matchOrFail(VENT_BREATHABLE, atm));

        // Break one wall — blob must react.
        ok(client().execute("artest place 0 " + (bx + 2) + " " + (by + 1) + " " + bz
                + " minecraft:air"));

        String reseal2 = join(client().execute(
                "artest vent reseal 0 " + bx + " " + by + " " + bz));
        assertTrue("second reseal probe failed: " + reseal2,
                reseal2.contains("\"ok\":true"));

        String leaked = join(client().execute(
                "artest vent info 0 " + bx + " " + by + " " + bz));
        int leakedBlobSize = Integer.parseInt(matchOrFail(VENT_BLOB_SIZE, leaked));
        boolean leakDetected =
                leakedBlobSize > sealedBlobSize
                || (leakedBlobSize == 0 && matchOrFail(VENT_SEALED, leaked).equals("false"));
        assertTrue("blob must react to wall break — either grow or void"
                        + " (was " + sealedBlobSize + ", now " + leakedBlobSize
                        + "): " + leaked, leakDetected);
    }

    // ─────────────────────────────────────────────────────────────────────
    // From SuitVacuumSubsystemSmokeTest — SMART §7.13 deepen
    // No world placement. Atmosphere density mutation restored in finally.
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void suitItemsAndEnchantAreWiredUp() throws Exception {
        // 1. All four suit pieces registered + expose IProtectiveArmor capability.
        for (String id : new String[]{
                "advancedrocketry:spaceHelmet",
                "advancedrocketry:spaceChestplate",
                "advancedrocketry:spaceLeggings",
                "advancedrocketry:spaceBoots"
        }) {
            String resp = join(client().execute(
                    "artest item check " + id + " protective-armor"));
            assertTrue(id + " not registered: " + resp,
                    resp.contains("\"registered\":true"));
            assertTrue(id + " missing IProtectiveArmor capability: " + resp,
                    resp.contains("\"hasCapability\":true"));
        }

        // 2. SpaceBreathing enchantment registered.
        String ench = join(client().execute(
                "artest enchant check advancedrocketry:spacebreathing"));
        assertTrue("spacebreathing enchant missing: " + ench,
                ench.contains("\"registered\":true"));

        // 3. Vacuum precondition: Earth → density 0 → non-breathable.
        // Snapshot original so we restore it after.
        String planet = join(client().execute("artest planet info 0"));
        Matcher dm = PLANET_DENSITY.matcher(planet);
        int originalDensity = dm.find() ? Integer.parseInt(dm.group(1)) : 100;

        try {
            String setVac = join(client().execute(
                    "artest atmosphere set-density 0 0"));
            assertTrue("set-density 0 failed: " + setVac,
                    setVac.contains("\"ok\":true"));

            String atm = join(client().execute(
                    "artest atmosphere get 0 0 70 0"));
            assertTrue("density=0 must yield non-breathable atmosphere: " + atm,
                    atm.contains("\"breathable\":false"));
        } finally {
            client().execute("artest atmosphere set-density 0 " + originalDensity);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // From SpecialInfrastructureSmokeTest — SMART §7.18
    // Position patch: 5 devices at x=700,710,720,730,740, y=64, z=700.
    // Note: forceFieldProjector at (720,64,700) is never powered (no redstone)
    // → no field blocks projected. Powered/collapse cycle for the projector
    // lives in ForceFieldProjectionSmokeTest (own JVM).
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void allSpecialBlocksPlaceAndTickWithoutException() throws Exception {
        int y = 64;
        int baseX = 700, baseZ = 700;

        Map<String, Integer> devices = new LinkedHashMap<>();
        devices.put("advancedrocketry:railgun", 0);
        devices.put("advancedrocketry:beacon", 10);
        devices.put("advancedrocketry:forceFieldProjector", 20);
        devices.put("advancedrocketry:spaceLaser", 30);
        devices.put("advancedrocketry:spaceElevatorController", 40);

        StringBuilder failures = new StringBuilder();
        int errors = 0;
        for (Map.Entry<String, Integer> e : devices.entrySet()) {
            String blockId = e.getKey();
            int x = baseX + e.getValue();
            String place = join(client().execute(
                    "artest place 0 " + x + " " + y + " " + baseZ + " " + blockId));
            if (!place.contains("\"placed\":true")) {
                failures.append(blockId).append("=PLACE_FAILED;");
                errors++;
                continue;
            }
            String info = join(client().execute(
                    "artest machine info 0 " + x + " " + y + " " + baseZ));
            if (info.contains("Exception")
                    || (!info.contains("\"tileClass\"") && !info.contains("\"no tile entity\""))) {
                failures.append(blockId).append("=INFO_BAD;");
                errors++;
                continue;
            }
            if (info.contains("\"tileClass\"")) {
                String tick = join(client().execute(
                        "artest tile force-tick 0 " + x + " " + y + " " + baseZ + " 5"));
                if (tick.contains("Exception") || tick.contains("\"error\":\"tile.update")) {
                    failures.append(blockId).append("=TICK_THREW(").append(tick).append(");");
                    errors++;
                }
            }
        }

        assertEquals("special infrastructure failures: " + failures, 0, errors);
    }

    // ─────────────────────────────────────────────────────────────────────
    // From MicrowaveReceiverSmokeTest — SMART §7.16
    // Position patch: 5×5 multiblock at (1700..1704, 64, 1700..1704). Controller (xC,yC,zC)=(1702,64,1702).
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void multiblockValidatesAndTicksWithoutCrash() throws Exception {
        int x0 = 1700, y = 64, z0 = 1700;
        int xC = x0 + 2, zC = z0 + 2;

        String fill = join(client().execute(
                "artest fill 0 " + x0 + " " + y + " " + z0 + " "
                        + (x0 + 4) + " " + y + " " + (z0 + 4)
                        + " advancedrocketry:solarPanel"));
        assertTrue("solar fill failed: " + fill, fill.contains("\"ok\":true"));

        int[][] airPositions = new int[][]{
                {x0, z0},     {x0 + 4, z0},     {x0, z0 + 4},     {x0 + 4, z0 + 4},
                {x0 + 1, z0}, {x0 + 2, z0},     {x0 + 3, z0},
                {x0 + 1, z0 + 4}, {x0 + 2, z0 + 4}, {x0 + 3, z0 + 4},
                {x0, z0 + 1}, {x0, z0 + 2}, {x0, z0 + 3},
                {x0 + 4, z0 + 1}, {x0 + 4, z0 + 2}, {x0 + 4, z0 + 3}
        };
        for (int[] p : airPositions) {
            client().execute("artest place 0 " + p[0] + " " + y + " " + p[1] + " minecraft:air");
        }

        String place = join(client().execute(
                "artest place 0 " + xC + " " + y + " " + zC
                        + " advancedrocketry:microwaveReciever"));
        assertTrue("controller place failed: " + place,
                place.contains("\"placed\":true"));

        String info = join(client().execute(
                "artest machine info 0 " + xC + " " + y + " " + zC));
        assertTrue("expected microwave-receiver tile: " + info,
                info.contains("TileMicrowaveReciever"));

        String tick = join(client().execute(
                "artest tile force-tick 0 " + xC + " " + y + " " + zC + " 40"));
        assertTrue("force-tick errored: " + tick, tick.contains("\"ok\":true"));
        assertEquals("must tick all 40 iterations",
                "40", extract(tick, "\"ticked\":(\\d+)"));

        String postInfo = join(client().execute(
                "artest machine info 0 " + xC + " " + y + " " + zC));
        assertTrue("tile must survive tick burst: " + postInfo,
                postInfo.contains("TileMicrowaveReciever"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // From BlackHoleGeneratorSmokeTest — SMART §7.16
    // Position patch: controller at (1800,64,1800), no multiblock structure.
    // ─────────────────────────────────────────────────────────────────────

    @Test
    public void controllerWithoutStructureTicksWithoutCrash() throws Exception {
        int x = 1800, y = 64, z = 1800;

        String place = join(client().execute(
                "artest place 0 " + x + " " + y + " " + z
                        + " advancedrocketry:blackholegenerator"));
        assertTrue("controller place failed: " + place,
                place.contains("\"placed\":true"));

        String info = join(client().execute(
                "artest machine info 0 " + x + " " + y + " " + z));
        assertTrue("expected black-hole-generator tile: " + info,
                info.contains("TileBlackHoleGenerator"));

        String tick = join(client().execute(
                "artest tile force-tick 0 " + x + " " + y + " " + z + " 50"));
        assertTrue("force-tick errored: " + tick, tick.contains("\"ok\":true"));
        assertEquals("must tick all 50 iterations",
                "50", extract(tick, "\"ticked\":(\\d+)"));

        String postInfo = join(client().execute(
                "artest machine info 0 " + x + " " + y + " " + z));
        assertTrue("tile must survive tick burst: " + postInfo,
                postInfo.contains("TileBlackHoleGenerator"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static String join(List<String> resp) {
        return String.join("\n", resp);
    }

    /** Asserts the probe response contains {@code "ok":true} and returns nothing. */
    private static void ok(List<String> response) {
        String joined = join(response);
        assertTrue("probe call failed: " + joined, joined.contains("\"ok\":true"));
    }

    private static long parseLong(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Long.parseLong(m.group(1)) : -1L;
    }

    private static String matchOrFail(Pattern p, String s) {
        Matcher m = p.matcher(s);
        assertTrue("pattern " + p + " did not match in: " + s, m.find());
        return m.group(1);
    }

    private static String extract(String s, String regex) {
        Matcher m = Pattern.compile(regex).matcher(s);
        return m.find() ? m.group(1) : "";
    }
}
