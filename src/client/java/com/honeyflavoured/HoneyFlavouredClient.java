package com.honeyflavoured;

import com.honeyflavoured.enchanting.EnchantingClient;
import com.honeyflavoured.fletching.FletchingClient;

import net.fabricmc.api.ClientModInitializer;


public class HoneyFlavouredClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {

		EnchantingClient.register();
		FletchingClient.register(); 
	}
}
