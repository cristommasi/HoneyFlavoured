package com.honeyflavoured.fletching;

import net.minecraft.client.gui.screens.MenuScreens;


public final class FletchingClient {

	private FletchingClient() {}

	public static void register() {

		MenuScreens.register(Fletching.MENU_TYPE, FletchingScreen::new);
	}
}