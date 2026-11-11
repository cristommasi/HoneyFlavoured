package com.honeyflavoured.jukebox;

import net.minecraft.world.inventory.ContainerData;

/** Syncs [0] = index of the disc being played (-1 none), [1] = 1 when playing. */
public class JukeboxData implements ContainerData {
    private final JukeboxAccess jukebox;

    public JukeboxData(JukeboxAccess jukebox) {
        this.jukebox = jukebox;
    }

    @Override
    public int get(int index) {
        return index == 0 ? jukebox.honeyflavoured$index() : (jukebox.honeyflavoured$playing() ? 1 : 0);
    }

    @Override
    public void set(int index, int value) {}

    @Override
    public int getCount() {
        return 2;
    }
}