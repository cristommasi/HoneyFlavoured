package com.honeyflavoured.mounts;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

public class MountSwimming {

	private static final double BASE_WATER_EFFICIENCY = 0.5D;

	public static void register() {

		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {

			if (entity instanceof AbstractHorse horse) {

				AttributeInstance efficiency = horse.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);

				if (efficiency != null) {

					efficiency.setBaseValue(BASE_WATER_EFFICIENCY);
				}
			}
		});
	}
}