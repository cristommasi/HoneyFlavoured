package com.honeyflavoured.jukebox;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface JukeboxAccess {
    Container honeyflavoured$discs();
    ContainerData honeyflavoured$data();
    BlockEntity honeyflavoured$entity();
    ItemStack honeyflavoured$displayDisc();
    int honeyflavoured$index();
    boolean honeyflavoured$playing();
    void honeyflavoured$press(int button);
    void honeyflavoured$serverTick(ServerLevel level);
}