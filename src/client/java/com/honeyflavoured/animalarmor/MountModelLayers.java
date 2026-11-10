
package com.honeyflavoured.animalarmor;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public class MountModelLayers {

    public static final ModelLayerLocation MULE_ARMOR = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath("honeyflavoured", "mule_armor"),
        "main"
    );


    public static void register() {

        ModelLayerRegistry.registerModelLayer(MULE_ARMOR, MuleArmorModel::createLayer);
    }
}
