package com.honeyflavoured.temperature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.honeyflavoured.HoneyFlavoured;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Table of which armor grants which temperature resistance.
 * Adding another armor type = one more addSet(...) call in register().
 */
public final class TemperatureArmor {

    private TemperatureArmor() {}

    private record Entry(EquipmentSlot slot, Holder<Attribute> attribute, double amount) {}

    private static final Map<Item, List<Entry>> ENTRIES = new LinkedHashMap<>();

    public static void addPiece(Item item, EquipmentSlot slot, Holder<Attribute> attribute, double amount) {
        ENTRIES.computeIfAbsent(item, i -> new ArrayList<>()).add(new Entry(slot, attribute, amount));
    }

    public static void addSet(Holder<Attribute> attribute, double amountPerPiece,
                              Item helmet, Item chestplate, Item leggings, Item boots) {
        addPiece(helmet, EquipmentSlot.HEAD, attribute, amountPerPiece);
        addPiece(chestplate, EquipmentSlot.CHEST, attribute, amountPerPiece);
        addPiece(leggings, EquipmentSlot.LEGS, attribute, amountPerPiece);
        addPiece(boots, EquipmentSlot.FEET, attribute, amountPerPiece);
    }

    public static void register() {
        // Leather = cold resistance.
        addSet(
            TemperatureAttributes.CALEFACTION,
            TemperatureConfig.RESISTANCE_PER_ARMOR_PIECE,
            Items.LEATHER_HELMET,
            Items.LEATHER_CHESTPLATE,
            Items.LEATHER_LEGGINGS,
            Items.LEATHER_BOOTS
        );

        // Gold = heat resistance.
        addSet(
            TemperatureAttributes.SWELTERING,
            TemperatureConfig.RESISTANCE_PER_ARMOR_PIECE,
            Items.GOLDEN_HELMET,
            Items.GOLDEN_CHESTPLATE,
            Items.GOLDEN_LEGGINGS,
            Items.GOLDEN_BOOTS
        );

        DefaultItemComponentEvents.MODIFY.register(context -> {
            for (Map.Entry<Item, List<Entry>> mapEntry : ENTRIES.entrySet()) {
                Item item = mapEntry.getKey();
                List<Entry> entries = mapEntry.getValue();

                context.modify(item, builder -> {
                    // Keep the armor's existing modifiers (armor, toughness...), then add ours.
                    ItemAttributeModifiers modifiers = item.components()
                        .getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

                    for (Entry entry : entries) {
                        Identifier id = Identifier.fromNamespaceAndPath(
                            HoneyFlavoured.MOD_ID,
                            "temperature_" + entry.attribute().unwrapKey().orElseThrow().identifier().getPath()
                                + "_" + entry.slot().getName()
                        );

                        modifiers = modifiers.withModifierAdded(
                            entry.attribute(),
                            new AttributeModifier(id, entry.amount(), AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.bySlot(entry.slot())
                        );
                    }

                    builder.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
                });
            }
        });
    }
}