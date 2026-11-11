package com.honeyflavoured.jukebox;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The 36 disc slots. Only music discs fit. */
public class JukeboxDiscs extends SimpleContainer {
    private BlockEntity owner;

    public JukeboxDiscs() {
        super(JukeboxMenu.DISC_SLOTS);
    }

    public void setOwner(BlockEntity owner) {
        this.owner = owner;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (owner != null) {
            owner.setChanged();
        }
    }
}