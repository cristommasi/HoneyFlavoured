package com.example.fletching;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;

/**
 * Fletching table menu: arrows + one lingering potion = the same number of tipped arrows.
 *
 * Menu slots: 0 = arrows, 1 = lingering potion, 2 = result, 3..29 = player inventory, 30..38 = hotbar.
 */
public class FletchingMenu extends AbstractContainerMenu {
	/** Slot positions (relative to the GUI's top-left). Adjust these to match your fletching_table.png. */
	public static final int ARROW_X = 44;
	public static final int ARROW_Y = 23;
	public static final int POTION_X = 44;
	public static final int POTION_Y = 44;
	public static final int RESULT_X = 116;
	public static final int RESULT_Y = 34;

	private static final int ARROW_SLOT = 0;
	private static final int POTION_SLOT = 1;
	private static final int RESULT_SLOT = 2;
	private static final int INV_START = 3;
	private static final int INV_END = 39;       // exclusive

	private final ContainerLevelAccess access;
	private final SimpleContainer inputs = new SimpleContainer(2) {
		@Override
		public void setChanged() {
			super.setChanged();
			FletchingMenu.this.slotsChanged(this);
		}
	};
	private final SimpleContainer resultContainer = new SimpleContainer(1);

	/** Client-side constructor (used by the MenuType). */
	public FletchingMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	/** Server-side constructor. */
	public FletchingMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(Fletching.MENU_TYPE, containerId);
		this.access = access;

		this.addSlot(new ArrowSlot(this.inputs, 0, ARROW_X, ARROW_Y));
		this.addSlot(new PotionSlot(this.inputs, 1, POTION_X, POTION_Y));
		this.addSlot(new ResultSlot(this.resultContainer, 0, RESULT_X, RESULT_Y));

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
		}
	}

	// ---------------------------------------------------------------- crafting logic

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);
		if (container == this.inputs) {
			this.updateResult();
		}
	}

	/** Runs on the server only (ContainerLevelAccess.NULL does nothing on the client). */
	private void updateResult() {
		this.access.execute((level, pos) -> {
			ItemStack out = ItemStack.EMPTY;
			ItemStack arrows = this.inputs.getItem(0);
			ItemStack potion = this.inputs.getItem(1);

			if (arrows.is(Items.ARROW) && potion.is(Items.LINGERING_POTION)) {
				PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
				if (contents != null && contents.hasEffects()) {
					out = new ItemStack(Items.TIPPED_ARROW, arrows.getCount());
					out.set(DataComponents.POTION_CONTENTS, contents);
				}
			}
			this.resultContainer.setItem(0, out);
		});
	}

	/** Called after the player takes the result: all the arrows and one lingering potion are used up. */
	private void consume() {
		this.inputs.setItem(ARROW_SLOT, ItemStack.EMPTY);
		this.inputs.removeItem(POTION_SLOT, 1);
		this.updateResult();
	}

	// ---------------------------------------------------------------- slots

	private static class ArrowSlot extends Slot {
		ArrowSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.is(Items.ARROW);
		}
	}

	private static class PotionSlot extends Slot {
		PotionSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.is(Items.LINGERING_POTION);
		}
	}

	private class ResultSlot extends Slot {
		ResultSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public void onTake(Player player, ItemStack stack) {
			consume();
		}
	}

	// ---------------------------------------------------------------- shift-click

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (index == RESULT_SLOT) {
			if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack, original);
			slot.setChanged();
			consume();
			return original;
		} else if (index < INV_START) {
			if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.is(Items.ARROW)) {
			if (!this.moveItemStackTo(stack, ARROW_SLOT, ARROW_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.is(Items.LINGERING_POTION)) {
			if (!this.moveItemStackTo(stack, POTION_SLOT, POTION_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	// ---------------------------------------------------------------- lifecycle

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, Blocks.FLETCHING_TABLE);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.access.execute((level, pos) -> this.clearContainer(player, this.inputs));
	}
}