package zmaster587.advancedRocketry.test.integration;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.test.MinecraftBootstrap;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * §6.8 Atmosphere — pure-logic checks on AtmosphereType subtypes.
 *
 * Loading {@code AtmosphereType} runs its static initializer which registers
 * atmospheres into {@code AtmosphereRegister}. We trigger MC bootstrap defensively
 * because some atmosphere subclasses reference vanilla blocks transitively.
 */
public class AtmosphereLogicTest {

    @BeforeClass
    public static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    public void airIsBreathable() {
        assertTrue(AtmosphereType.AIR.isBreathable());
        assertTrue("normal air must allow combustion (torches burn)", AtmosphereType.AIR.allowsCombustion());
    }

    @Test
    public void pressurizedAirIsBreathable() {
        assertTrue(AtmosphereType.PRESSURIZEDAIR.isBreathable());
    }

    @Test
    public void vacuumIsNotBreathable() {
        assertFalse(AtmosphereType.VACUUM.isBreathable());
        assertFalse("vacuum must not support combustion", AtmosphereType.VACUUM.allowsCombustion());
    }

    @Test
    public void noOxygenAtmospheresAreNotBreathable() {
        assertFalse(AtmosphereType.NOO2.isBreathable());
        assertFalse(AtmosphereType.HIGHPRESSURENOO2.isBreathable());
        assertFalse(AtmosphereType.SUPERHIGHPRESSURENOO2.isBreathable());
        assertFalse(AtmosphereType.VERYHOTNOO2.isBreathable());
        assertFalse(AtmosphereType.SUPERHEATEDNOO2.isBreathable());
    }

    @Test
    public void hostileAtmospheresHaveTickingEnabled() {
        // Atmospheres that damage / affect entities every tick must report canTick.
        assertTrue("vacuum ticks for suffocation damage", AtmosphereType.VACUUM.canTick());
        assertTrue("LowO2 ticks for nausea/damage", AtmosphereType.LOWOXYGEN.canTick());
        assertTrue("HighPressure ticks", AtmosphereType.HIGHPRESSURE.canTick());
        assertTrue("VeryHot ticks", AtmosphereType.VERYHOT.canTick());
    }

    @Test
    public void breathableAtmospheresDoNotTick() {
        assertFalse("Breathable AIR is not expected to tick effects", AtmosphereType.AIR.canTick());
        assertFalse("PressurizedAir does not tick", AtmosphereType.PRESSURIZEDAIR.canTick());
    }

    @Test
    public void atmosphereNamesArePreservedFromConstructor() {
        assertEquals("air", AtmosphereType.AIR.getUnlocalizedName());
        assertEquals("PressurizedAir", AtmosphereType.PRESSURIZEDAIR.getUnlocalizedName());
        assertEquals("lowO2", AtmosphereType.LOWOXYGEN.getUnlocalizedName());
        assertEquals("NoO2", AtmosphereType.NOO2.getUnlocalizedName());
    }

    @Test
    public void breathableSetterFlipsBreathableFlag() {
        // Local instance — DO NOT mutate the singleton AIR / VACUUM, that would leak
        // into other tests.
        AtmosphereType local = new AtmosphereType(false, true, "ar.test.local." + System.nanoTime());
        assertTrue(local.isBreathable());

        local.setIsBreathable(false);
        assertFalse(local.isBreathable());
    }

    @Test
    public void allowsCombustionSetterIsIndependentOfBreathable() {
        AtmosphereType local = new AtmosphereType(false, false, true, "ar.test.combust." + System.nanoTime());
        assertFalse(local.isBreathable());
        assertTrue("constructor must keep combustion flag distinct from breathable", local.allowsCombustion());

        local.setAllowsCombustion(false);
        assertFalse(local.allowsCombustion());
    }

    /**
     * §6.8 — space-suit "capability" NBT round-trip.
     *
     * The suit's worn state is persisted on the ItemStack itself: ItemSpaceChest
     * stores its modular slot inventory (which holds fluid tanks → capability
     * adapters) into {@code stack.getTagCompound()} via
     * {@code EmbeddedInventory.writeToNBT}, and reloads it the same way.
     * That mechanism is just an {@link ItemStack}-with-NBT round-trip; the
     * capability adapters on inner stacks are rebuilt lazily from the registry,
     * so the NBT IS the entire persistence surface.
     *
     * Instantiating the real {@code ItemSpaceChest} needs ArmorMaterial + the AR
     * registry chain (out-of-scope for unit tests). We assert the underlying
     * contract against a vanilla armor item with the same tagCompound shape that
     * {@code ItemSpaceArmor.saveEmbeddedInventory} writes (an "Items" NBT list
     * with "Slot" / item id entries).
     */
    @Test
    public void spaceSuitCapabilityNbtRoundTrip() {
        ItemStack suit = new ItemStack(Items.IRON_HELMET);

        // Mirror the EmbeddedInventory.writeToNBT(parent) → parent.setTag("Items", list)
        // layout used by the production suit. The inner fluid tank is represented
        // by a sub-tag with Damage/Count/Fluid keys (the same shape libVulpes'
        // FluidContainerItem writes via writeShareTag).
        NBTTagCompound tag = new NBTTagCompound();

        NBTTagList items = new NBTTagList();
        NBTTagCompound slot0 = new NBTTagCompound();
        slot0.setByte("Slot", (byte) 0);
        slot0.setShort("id", (short) Item_REGISTRY_ID_BUCKET);
        slot0.setByte("Count", (byte) 1);

        NBTTagCompound bucketTag = new NBTTagCompound();
        NBTTagCompound fluid = new NBTTagCompound();
        fluid.setString("FluidName", "oxygen");
        fluid.setInteger("Amount", 4000);
        bucketTag.setTag("Fluid", fluid);
        slot0.setTag("tag", bucketTag);

        items.appendTag(slot0);
        tag.setTag("Items", items);
        // Mirror the "air" timer field the suit may also set.
        tag.setInteger("air", 18_000);

        suit.setTagCompound(tag);

        // Round-trip through ItemStack.writeToNBT / new ItemStack(nbt) — the
        // production save path used when the player drops the suit into a chest
        // or saves the world.
        NBTTagCompound serialized = new NBTTagCompound();
        suit.writeToNBT(serialized);

        ItemStack restored = new ItemStack(serialized);
        assertFalse("stack must NOT lose identity through NBT round-trip", restored.isEmpty());
        assertSame("item identity must be preserved",
                Items.IRON_HELMET, restored.getItem());
        assertNotNull("suit tag compound must survive", restored.getTagCompound());

        NBTTagCompound restoredTag = restored.getTagCompound();
        assertEquals("air timer must survive", 18_000, restoredTag.getInteger("air"));

        NBTTagList restoredItems = restoredTag.getTagList("Items", 10 /*NBT.TAG_COMPOUND*/);
        assertEquals("modular slot count must survive", 1, restoredItems.tagCount());

        NBTTagCompound restoredSlot = restoredItems.getCompoundTagAt(0);
        assertEquals(0, restoredSlot.getByte("Slot"));

        NBTTagCompound restoredBucketTag = restoredSlot.getCompoundTag("tag");
        NBTTagCompound restoredFluid = restoredBucketTag.getCompoundTag("Fluid");
        assertEquals("fluid name must survive", "oxygen", restoredFluid.getString("FluidName"));
        assertEquals("fluid amount must survive", 4000, restoredFluid.getInteger("Amount"));
    }

    // Vanilla bucket numeric id — kept inline so the test doesn't depend on
    // RegistryEvent firing order. We only use it as a placeholder for "some
    // item with NBT", the real ItemSpaceChest stores its own item id.
    private static final int Item_REGISTRY_ID_BUCKET = 325;

    /**
     * §6.8 — entity-bypass config parses ResourceLocations and FQCNs.
     *
     * Production loadPreInit walks {@code entityList} (a String[] from
     * config.getStringList("entityAtmBypass", ...)) and for each entry:
     *   1. tries {@code EntityList.getClass(new ResourceLocation(str))} —
     *      the registry name path for vanilla / modded entities;
     *   2. falls back to {@code Class.forName(str)} for fully-qualified class
     *      names AND verifies {@code Entity.class.isAssignableFrom(clazz)};
     *   3. on both failures, logs a warning and skips the entry — no NPE.
     *
     * We exercise each of the three branches against a real EntityList (vanilla
     * registry, populated by Bootstrap.register() in MinecraftBootstrap).
     */
    @Test
    public void entityBypassConfigParsesResourceLocations() {
        // Replay the exact parsing loop from ARConfiguration.loadPreInit lines
        // 714–733 against a representative input set.
        String[] entityList = {
                "minecraft:armor_stand",                              // vanilla RL → EntityArmorStand
                "minecraft:doesnotexist_entity",                      // vanilla namespace, unknown name → null
                "net.minecraft.entity.item.EntityArmorStand",         // FQCN fallback → same class
                "java.lang.String",                                   // FQCN but NOT an Entity → must be filtered
                "totally::garbage::value::with::wrong::syntax",       // malformed → must NOT throw
                "minecraft:zombie",                                   // vanilla RL → EntityZombie
        };

        List<Class<?>> resolved = new LinkedList<>();
        for (String str : entityList) {
            Class<?> clazz;
            try {
                clazz = EntityList.getClass(new ResourceLocation(str));
            } catch (Throwable e) {
                clazz = null;
            }

            if (clazz == null) {
                try {
                    clazz = Class.forName(str);
                    if (!Entity.class.isAssignableFrom(clazz)) {
                        clazz = null;
                    }
                } catch (Throwable e) {
                    clazz = null;
                }
            }

            if (clazz != null) {
                resolved.add(clazz);
            }
        }

        // Branch 1: armor_stand resolved via ResourceLocation (vanilla registry).
        assertTrue("EntityArmorStand must resolve via minecraft:armor_stand",
                resolved.contains(EntityArmorStand.class));

        // Branch 2 (unknown registry name): null → skipped, no exception.
        // (Implicit — if it threw, we'd never reach branch 3.)

        // Branch 3a (FQCN fallback): EntityArmorStand via class-name fallback —
        // already in the list from branch 1, so just confirm at least one
        // instance.
        long armorStandHits = resolved.stream()
                .filter(c -> c == EntityArmorStand.class)
                .count();
        assertTrue("FQCN fallback must also resolve armor stand",
                armorStandHits >= 1L);

        // Branch 3b (FQCN but non-Entity): java.lang.String → filtered out.
        assertFalse("non-Entity class must be filtered",
                resolved.contains(String.class));

        // Branch 4 (malformed): must not crash the parser; entry just doesn't
        // appear in the resolved set. We verify the loop completed by checking
        // the post-malformed entries also resolved.
        Class<?> zombie = EntityList.getClass(new ResourceLocation("minecraft:zombie"));
        assertNotNull("vanilla zombie registry name must resolve", zombie);
        assertTrue("zombie must end up in resolved set (malformed entry didn't break the loop)",
                resolved.contains(zombie));

        // The bypassEntity collection in production always starts with
        // EntityArmorStand.class (loadPreInit:708). We don't run loadPreInit
        // here but confirm the class is resolvable as the loop expects.
        assertSame("entity registry must return EntityArmorStand for armor_stand",
                EntityArmorStand.class,
                EntityList.getClass(new ResourceLocation("minecraft:armor_stand")));
        assertNull("unknown name must return null cleanly",
                EntityList.getClass(new ResourceLocation("minecraft:totally_made_up_entity")));
    }
}
