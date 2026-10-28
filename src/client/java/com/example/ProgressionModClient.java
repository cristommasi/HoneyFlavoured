package com.example;

import com.example.craftenchanting.client.CraftEnchantingClient;
import com.example.fletching.FletchingClient;

import net.fabricmc.api.ClientModInitializer;

/** The only client entrypoint. One line per feature. */
public class ProgressionModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		new CraftEnchantingClient().onInitializeClient();   // enchanting table screen + toast
		FletchingClient.register();                          // fletching table screen
	}
}