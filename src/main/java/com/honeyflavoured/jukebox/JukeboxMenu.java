package com.honeyflavoured.jukebox;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class JukeboxMenu extends AbstractContainerMenu {

    public static final int ROWS = 4;
    public static final int DISC_SLOTS = ROWS * 9;
    public static final int NOW_PLAYING_SLOT = DISC_SLOTS;
    public static final int BUTTON_PREVIOUS = 0;
    public static final int BUTTON_PLAY = 1;
    public static final int BUTTON_NEXT = 2;
    public static final int GRID_X = 8;    // slot frames start at 7,35 -> items at 8,36
    public static final int GRID_Y = 36;
    public static final int NOW_X = 7;     // display slot frame 7,14 -> item at 8,15
    public static final int NOW_Y = 15;
    public static final int PLAYER_Y = 139; // top of your player inventory; change if your PNG differs

    private final Container discs;
    private final SimpleContainer nowPlaying = new SimpleContainer(1);
    private final ContainerData data;
    private final JukeboxAccess jukebox; // null on the client

    /** Client side. */
    public JukeboxMenu(int id, Inventory inventory) {
        this(id, inventory, new JukeboxDiscs(), new SimpleContainerData(2), null);
    }

    /** Server side. */
    public JukeboxMenu(int id, Inventory inventory, JukeboxAccess jukebox) {
        this(id, inventory, jukebox.honeyflavoured$discs(), jukebox.honeyflavoured$data(), jukebox);
    }

    private JukeboxMenu(int id, Inventory inventory, Container discs, ContainerData data, JukeboxAccess jukebox) {
        super(Jukebox.MENU_TYPE, id);
        checkContainerSize(discs, DISC_SLOTS);
        this.discs = discs;
        this.data = data;
        this.jukebox = jukebox;
        discs.startOpen(inventory.player);

        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new Slot(discs, x + y * 9, GRID_X + x * 18, GRID_Y + y * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return JukeboxDiscs.isDisc(stack);
                    }
                });
            }
        }

        // read-only "ghost" slot above the play button
        this.addSlot(new Slot(nowPlaying, 0, NOW_X, NOW_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        // same height as a double chest: 6 rows of space
        this.addStandardInventorySlots(inventory, 8, PLAYER_Y);
        this.addDataSlots(data);

        if (jukebox != null) {
            nowPlaying.setItem(0, jukebox.honeyflavoured$displayDisc());
        }
    }

    public int getPlayingIndex() {
        return data.get(0);
    }

    public boolean isPlaying() {
        return data.get(1) != 0;
    }

    public ItemStack getNowPlaying() {
        return nowPlaying.getItem(0);
    }

    @Override
    public void broadcastChanges() {
        if (jukebox != null) {
            nowPlaying.setItem(0, jukebox.honeyflavoured$displayDisc());
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (jukebox == null) {
            return false;
        }
        jukebox.honeyflavoured$press(id);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slotIndex == NOW_PLAYING_SLOT) {
            return ItemStack.EMPTY;
        }
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            int playerStart = DISC_SLOTS + 1;
            if (slotIndex < DISC_SLOTS) {
                if (!this.moveItemStackTo(stack, playerStart, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, DISC_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return jukebox == null || Container.stillValidBlockEntity(jukebox.honeyflavoured$entity(), player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        discs.stopOpen(player);
    }
}