package com.example.craftenchanting.client;

import com.example.craftenchanting.CraftEnchanting;
import com.example.craftenchanting.Recipes;
import com.example.craftenchanting.UnlockPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;

public class CraftEnchantingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(CraftEnchanting.MENU_TYPE, CraftEnchantScreen::new);

		// "New enchanting recipe unlocked" toast (top right, like vanilla's recipe toast).
		ClientPlayNetworking.registerGlobalReceiver(UnlockPayload.TYPE, (payload, context) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || payload.index() < 0 || payload.index() >= Recipes.ORDERED.size()) {
				return;
			}
			Component name = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Recipes.ORDERED.get(payload.index()).getValue()).value().description();
			SystemToast.add(mc.gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
				Component.literal("New enchanting recipe unlocked"), name);
		});
	}
}