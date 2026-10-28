package com.example.fletching;

import net.minecraft.client.gui.screens.MenuScreens;

/** Call FletchingClient.register() once from onInitializeClient. */
public final class FletchingClient {
	private FletchingClient() {}

	public static void register() {
		MenuScreens.register(Fletching.MENU_TYPE, FletchingScreen::new);
	}
}