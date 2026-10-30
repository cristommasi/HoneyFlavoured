package com.honeyflavoured.temperature;

import com.honeyflavoured.HoneyFlavoured;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityTypes;
/**
 * Resistance attributes. Anything that grants these (armor, enchantments, effects)
 * automatically counts towards temperature protection.
 * To add a new one, add a field here and read it in TemperatureManager.
 */
public final class TemperatureAttributes {

    private TemperatureAttributes() {}

    // Cold resistance ("Calefaction").
    public static final Holder<Attribute> CALEFACTION = register("calefaction");

    // Heat resistance ("Sweltering").
    public static final Holder<Attribute> SWELTERING = register("sweltering");

    private static Holder<Attribute> register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(HoneyFlavoured.MOD_ID, name);
        Attribute attribute = new RangedAttribute(
            "attribute.name." + HoneyFlavoured.MOD_ID + "." + name,
            0.0D,
            0.0D,
            1024.0D
        ).setSyncable(true);

        return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, id, attribute);
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(
            EntityTypes.PLAYER,
            Player.createAttributes()
                .add(CALEFACTION)
                .add(SWELTERING)
        );
    }
}