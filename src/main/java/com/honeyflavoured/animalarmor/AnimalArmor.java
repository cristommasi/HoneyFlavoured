package com.honeyflavoured.animalarmor;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class AnimalArmor {
    private AnimalArmor() {}

    public static final int HORSE_DURABILITY = 1000;
    public static final int WOLF_DURABILITY = 700;
    public static final int NAUTILUS_DURABILITY = 500;

    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    public static boolean isAnimalArmor(Item item) {
        return durabilityOf(item) > 0;
    }

    /** 0 if this is not animal armour. */
    public static int durabilityOf(Item item) {
        String id = id(item);

        if (id.endsWith("_horse_armor")) {
            return HORSE_DURABILITY;
        }
        if (id.equals("wolf_armor")) {
            return WOLF_DURABILITY;
        }
        if (id.endsWith("_nautilus_armor")) {
            return NAUTILUS_DURABILITY;
        }
        return 0;
    }

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context ->
            context.modify(AnimalArmor::isAnimalArmor, (builder, item) -> builder
                .set(DataComponents.MAX_DAMAGE, durabilityOf(item))
                .set(DataComponents.DAMAGE, 0)
                .set(DataComponents.MAX_STACK_SIZE, 1)));

   
    }

    /** True if this armour carries Binding Curse. */
    public static boolean isBound(ItemStack stack) {
        return EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }
}