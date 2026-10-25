package com.example.craftenchanting;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;

/**
 * Enchanting table menu that works like a crafting table.
 *
 * Grid layout (grid index):      Menu slots:
 *   0 1 2                         result = 0
 *   3 4 5      4 = book           grid   = 1..9   (menu slot = grid index + 1)
 *   6 7 8      7 = lapis          player = 10..36, hotbar = 37..45
 *
 * The 7 remaining grid slots (0,1,2,3,5,6,8) hold the recipe ingredient.
 */
public class CraftEnchantMenu extends AbstractContainerMenu {
	/** Slot positions (relative to the GUI's top-left). Your enchanting_table.png must match these. */
	public static final int GRID_X = 62;     // top-left slot of the 3x3 grid, 18px spacing
	public static final int GRID_Y = 17;
	public static final int RESULT_X = 140;  // result slot
	public static final int RESULT_Y = 35;

	/** Lapis lazuli used up per craft. */
	public static final int LAPIS_COST = 1;

	private static final int RESULT_SLOT = 0;
	private static final int GRID_START = 1;
	private static final int GRID_END = 10;      // exclusive
	private static final int INV_START = 10;
	private static final int INV_END = 46;       // exclusive
	private static final int BOOK = 4;           // grid index, centre
	private static final int LAPIS = 7;          // grid index, bottom middle
	private static final int BOOK_SLOT = GRID_START + BOOK;
	private static final int LAPIS_SLOT = GRID_START + LAPIS;

	private final ContainerLevelAccess access;
	private final RegistryAccess registries;
	private final Player player;
	private final DataSlot[] unlocked = {DataSlot.standalone(), DataSlot.standalone(), DataSlot.standalone()};
	private int unlockTimer;
	private final SimpleContainer grid = new SimpleContainer(9) {
		@Override
		public void setChanged() {
			super.setChanged();
			CraftEnchantMenu.this.slotsChanged(this);
		}
	};
	private final SimpleContainer resultContainer = new SimpleContainer(1);
	private final DataSlot cost = DataSlot.standalone();

	/** Client-side constructor (used by the MenuType). */
	public CraftEnchantMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	/** Server-side constructor. */
	public CraftEnchantMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(CraftEnchanting.MENU_TYPE, containerId);
		this.access = access;
		this.registries = inventory.player.level().registryAccess();
		this.player = inventory.player;

		// Result slot
		this.addSlot(new ResultSlot(this.resultContainer, 0, RESULT_X, RESULT_Y));

		// 3x3 grid
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int gi = col + row * 3;
				int gx = GRID_X + col * 18;
				int gy = GRID_Y + row * 18;
				if (gi == BOOK) {
					this.addSlot(new CentreSlot(this.grid, gi, gx, gy));
				} else if (gi == LAPIS) {
					this.addSlot(new LapisSlot(this.grid, gi, gx, gy));
				} else {
					this.addSlot(new Slot(this.grid, gi, gx, gy));
				}
			}
		}

		// Player inventory + hotbar
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
		}

		this.addDataSlot(this.cost);
		for (DataSlot d : this.unlocked) {
			this.addDataSlot(d);
		}
	}

	// ---------------------------------------------------------------- queries used by the screen

	/** XP levels the current result costs (0 = no result). Synced to the client. */
	public int getCost() {
		return this.cost.get();
	}

	// ---------------------------------------------------------------- knowledge book

	public RegistryAccess registries() {
		return this.registries;
	}

	/** Client + server: is recipe #idx (Recipes.ORDERED) unlocked for the player who opened the table? */
	public boolean isUnlocked(int idx) {
		return (((this.unlocked[idx / 16].get() & 0xFFFF) >> (idx % 16)) & 1) == 1;
	}

	public Component enchantName(int idx) {
		ResourceKey<Enchantment> key = Recipes.ORDERED.get(idx).getValue();
		return this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key).value().description();
	}

	@Override
	public void broadcastChanges() {
		if (this.player instanceof ServerPlayer sp && this.unlockTimer++ % 10 == 0) {
			Recipes.unlockFromInventory(sp);
			int[] mask = Recipes.unlockedMask(sp);
			for (int i = 0; i < 3; i++) {
				this.unlocked[i].set(mask[i]);
			}
		}
		super.broadcastChanges();
	}

	private static int count(Inventory inv, Item item) {
		int n = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(item)) {
				n += s.getCount();
			}
		}
		return n;
	}

	private static ItemStack takeOne(Inventory inv, Item item) {
		for (int i = 0; i < 36; i++) {
			if (inv.getItem(i).is(item)) {
				return inv.removeItem(i, 1);
			}
		}
		return ItemStack.EMPTY;
	}

	/** Clicking a recipe in the knowledge book: fill the 7 ingredient slots + lapis from the inventory. */
	@Override
	public boolean clickMenuButton(Player clicker, int id) {
		if (id < 0 || id >= Recipes.ORDERED.size()
			|| !(clicker instanceof ServerPlayer sp) || !Recipes.isUnlocked(sp, id)) {
			return false;
		}
		Item ingredient = Recipes.ORDERED.get(id).getKey();
		Inventory inv = clicker.getInventory();

		// Put the ingredient slots and the lapis slot back into the inventory first.
		// The centre slot (book / pickaxe / ...) is never touched.
		int[] fillSlots = {0, 1, 2, 3, 5, 6, 8, LAPIS};
		for (int i : fillSlots) {
			ItemStack s = this.grid.getItem(i);
			if (s.isEmpty()) {
				continue;
			}
			ItemStack moving = s.copy();
			if (inv.add(moving)) {
				this.grid.setItem(i, ItemStack.EMPTY);
			} else {
				this.grid.setItem(i, moving);   // inventory full: keep what didn't fit
				return false;
			}
		}

		if (count(inv, ingredient) < Recipes.RING.length
			|| count(inv, Items.LAPIS_LAZULI) < LAPIS_COST) {
			return false;   // not enough materials: slots stay empty
		}

		for (int i : Recipes.RING) {
			this.grid.setItem(i, takeOne(inv, ingredient));
		}
		for (int k = 0; k < LAPIS_COST; k++) {
			takeOne(inv, Items.LAPIS_LAZULI);
		}
		this.grid.setItem(LAPIS, new ItemStack(Items.LAPIS_LAZULI, LAPIS_COST));
		return true;
	}

	/** Item currently in the centre slot (used to animate the vanilla book on the client). */
	public ItemStack getCentreItem() {
		return this.grid.getItem(BOOK);
	}

	/** True when a book / enchanted book sits in the centre slot. */
	public boolean hasBook() {
		return isBook(this.grid.getItem(BOOK));
	}

	/** True when a craft is ready in the result slot. */
	public boolean hasResult() {
		return !this.resultContainer.getItem(0).isEmpty();
	}

	private static boolean isBook(ItemStack s) {
		return s.is(Items.BOOK) || s.is(Items.ENCHANTED_BOOK);
	}

	// ---------------------------------------------------------------- crafting logic

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);
		if (container == this.grid) {
			this.updateResult();
		}
	}

	/** Runs on the server only (ContainerLevelAccess.NULL does nothing on the client). */
	private void updateResult() {
		this.access.execute((level, pos) -> {
			ItemStack out = ItemStack.EMPTY;
			int newCost = 0;

			ItemStack centre = this.grid.getItem(BOOK);
			ItemStack lapis = this.grid.getItem(LAPIS);
			boolean hasLapis = lapis.is(Items.LAPIS_LAZULI) && lapis.getCount() >= LAPIS_COST;
			boolean book = isBook(centre);

			Optional<ResourceKey<Enchantment>> key = Recipes.find(this.grid);
			if (key.isPresent() && hasLapis && !centre.isEmpty()) {
				Holder<Enchantment> holder = level.registryAccess()
					.lookupOrThrow(Registries.ENCHANTMENT)
					.getOrThrow(key.get());

				ItemEnchantments existing;
				if (centre.is(Items.ENCHANTED_BOOK)) {
					existing = centre.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
				} else if (book) {
					existing = ItemEnchantments.EMPTY;
				} else {
					existing = centre.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
				}

				boolean allowed = book || holder.value().isSupportedItem(centre);
				if (allowed && !book) {
					// Items can't carry enchantments that exclude each other (Sharpness + Smite...).
					for (Holder<Enchantment> other : existing.keySet()) {
						if (!other.equals(holder) && !Enchantment.areCompatible(holder, other)) {
							allowed = false;
							break;
						}
					}
				}

				int current = existing.getLevel(holder);
				int max = holder.value().getMaxLevel();
				// Already has this enchant -> one level higher. Otherwise the default (level I).
				int newLevel = current > 0 ? current + 1 : 1;

				if (allowed && newLevel <= max) {
					ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(existing);
					mutable.set(holder, newLevel);

					if (book) {
						out = new ItemStack(Items.ENCHANTED_BOOK);
						out.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
					} else {
						out = centre.copyWithCount(1);
						out.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
					}
					newCost = Recipes.costFor(key.get(), newLevel);
				}
			}

			this.resultContainer.setItem(0, out);
			this.cost.set(newCost);
		});
	}

	private boolean canAfford(Player player) {
		return player.hasInfiniteMaterials() || player.experienceLevel >= this.cost.get();
	}

	/** Called after the player takes the result: charge XP, use up one of each input and the lapis. */
	private void consume(Player player) {
		if (!player.hasInfiniteMaterials()) {
			player.giveExperienceLevels(-this.cost.get());
		}
		for (int i = 0; i < 9; i++) {
			this.grid.removeItem(i, i == LAPIS ? LAPIS_COST : 1);
		}
		// Same sound vanilla plays when you take an enchanted item from the table.
		this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
			SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F));
		this.updateResult();
	}

	// ---------------------------------------------------------------- slots

	/** Books, enchanted books, or anything one of our enchantments can go on. */
	private boolean isValidCentre(ItemStack stack) {
		return isBook(stack) || stack.isEnchantable() || Recipes.supportsAny(this.registries, stack);
	}

	/** Centre slot: books and enchantable items only. */
	private class CentreSlot extends Slot {
		CentreSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return isValidCentre(stack);
		}
	}

	/** Bottom-middle slot: only accepts lapis lazuli. */
	private static class LapisSlot extends Slot {
		LapisSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.is(Items.LAPIS_LAZULI);
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
		public boolean mayPickup(Player player) {
			if (!this.hasItem()) {
				return false;
			}
			return canAfford(player);
		}

		@Override
		public void onTake(Player player, ItemStack stack) {
			consume(player);
		}
	}

	// ---------------------------------------------------------------- shift-click

	/** Moves a stack into the 7 ingredient slots only (never the book or lapis slot). */
	private boolean moveToIngredients(ItemStack stack) {
		boolean moved = this.moveItemStackTo(stack, GRID_START, BOOK_SLOT, false);          // grid 0-3
		if (!stack.isEmpty()) {
			moved |= this.moveItemStackTo(stack, BOOK_SLOT + 1, LAPIS_SLOT, false);         // grid 5-6
		}
		if (!stack.isEmpty()) {
			moved |= this.moveItemStackTo(stack, LAPIS_SLOT + 1, GRID_END, false);          // grid 8
		}
		return moved;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (index == RESULT_SLOT) {
			if (!canAfford(player)) {
				return ItemStack.EMPTY;
			}
			if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack, original);
			slot.setChanged();
			consume(player);
			return original;
		} else if (index >= GRID_START && index < GRID_END) {
			if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			// From inventory into the grid: books -> centre only, lapis -> lapis slot only,
			// everything else -> the 7 ingredient slots.
			boolean moved;
			if (stack.is(Items.LAPIS_LAZULI)) {
				moved = this.moveItemStackTo(stack, LAPIS_SLOT, LAPIS_SLOT + 1, false);
			} else if (!Recipes.BY_INGREDIENT.containsKey(stack.getItem()) && isValidCentre(stack)) {
				// Books and enchantable items -> centre only. (Recipe ingredients that are also
				// enchantable, like shears, go to the ingredient slots; place those by hand.)
				moved = this.moveItemStackTo(stack, BOOK_SLOT, BOOK_SLOT + 1, false);
			} else {
				moved = this.moveToIngredients(stack);
			}
			if (!moved) {
				return ItemStack.EMPTY;
			}
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
		return stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.access.execute((level, pos) -> this.clearContainer(player, this.grid));
	}
}