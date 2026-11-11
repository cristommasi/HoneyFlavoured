package com.honeyflavoured.jukebox;

import net.minecraft.client.gui.screens.MenuScreens;

public final class JukeboxClient {
    private JukeboxClient() {}

    public static void register() {
        MenuScreens.register(Jukebox.MENU_TYPE, JukeboxScreen::new);
    }
}