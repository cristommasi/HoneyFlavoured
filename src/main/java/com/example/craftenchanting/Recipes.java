package com.example.craftenchanting;

import java.lang.reflect.Method;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.resources.ResourceKey;

/**
 * The enchanting recipes: ONE ingredient item surrounding a book in a 3x3 grid.
 * The 7 ingredient slots (everything except centre = book and bottom middle = lapis) must hold the same item.
 *
 * Map.ofEntries throws on duplicate keys at class load, so no item can ever
 * be mapped to two enchantments by accident.
 */
public final class Recipes {
	private Recipes() {}

	private static Map.Entry<Item, ResourceKey<Enchantment>> r(Item item, ResourceKey<Enchantment> ench) {
		return Map.entry(item, ench);
	}

	public static final Map<Item, ResourceKey<Enchantment>> BY_INGREDIENT = Map.ofEntries(
		// All purpose
		r(Items.NETHER_STAR, Enchantments.MENDING),
		r(Items.OBSIDIAN, Enchantments.UNBREAKING),
		r(Items.TOTEM_OF_UNDYING, Enchantments.VANISHING_CURSE),

		// Armor
		r(Items.NAUTILUS_SHELL, Enchantments.AQUA_AFFINITY),
		r(Items.GUNPOWDER, Enchantments.BLAST_PROTECTION),
		r(Items.SLIME_BALL, Enchantments.BINDING_CURSE),
		r(Items.PRISMARINE_SHARD, Enchantments.DEPTH_STRIDER),
		r(Items.FEATHER, Enchantments.FEATHER_FALLING),
		r(Items.FIRE_CHARGE, Enchantments.FIRE_PROTECTION),
		r(Items.PACKED_ICE, Enchantments.FROST_WALKER),
		r(Items.ARMADILLO_SCUTE, Enchantments.PROTECTION),
		r(Items.TURTLE_SCUTE, Enchantments.RESPIRATION),
		r(Items.SOUL_LANTERN, Enchantments.SOUL_SPEED),
		r(Items.CACTUS, Enchantments.THORNS),
		r(Items.ECHO_SHARD, Enchantments.SWIFT_SNEAK),
		r(Items.ARROW, Enchantments.PROJECTILE_PROTECTION),       // added (not in your list)

		// Melee weapons
		r(Items.STRING, Enchantments.BANE_OF_ARTHROPODS),
		r(Items.SHEARS, Enchantments.BREACH),
		r(Items.ANCIENT_DEBRIS, Enchantments.DENSITY),
		r(Items.HONEYCOMB, Enchantments.EFFICIENCY),
		r(Items.RABBIT_FOOT, Enchantments.LOOTING),
		r(Items.SWEET_BERRIES, Enchantments.LUNGE),
		r(Items.AMETHYST_SHARD, Enchantments.IMPALING),
		r(Items.CACTUS_FLOWER, Enchantments.KNOCKBACK),
		r(Items.DIAMOND, Enchantments.SHARPNESS),
		r(Items.ROTTEN_FLESH, Enchantments.SMITE),
		r(Items.BONE, Enchantments.SWEEPING_EDGE),
		r(Items.WIND_CHARGE, Enchantments.WIND_BURST),
		r(Items.MAGMA_CREAM, Enchantments.FIRE_ASPECT),          // added (not in your list)

		// Ranged weapons
		// Lightning rods are a copper "collection" in 26.x, so look the plain one up by id.
		r(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("lightning_rod")), Enchantments.CHANNELING),
		r(Items.BLAZE_POWDER, Enchantments.FLAME),
		r(Items.RESIN_CLUMP, Enchantments.INFINITY),
		r(Items.LEAD, Enchantments.LOYALTY),
		r(Items.TNT, Enchantments.RIPTIDE),
		r(Items.QUARTZ, Enchantments.MULTISHOT),
		r(Items.POINTED_DRIPSTONE, Enchantments.PIERCING),
		r(Items.GOLDEN_APPLE, Enchantments.POWER),
		r(Items.ANVIL, Enchantments.PUNCH),
		r(Items.SALMON, Enchantments.QUICK_CHARGE),

		// Tools (Efficiency is honeycomb only, per "first listing only")
		r(Items.ENCHANTED_GOLDEN_APPLE, Enchantments.FORTUNE),
		r(Items.HEART_OF_THE_SEA, Enchantments.LUCK_OF_THE_SEA),
		r(Items.CLOCK, Enchantments.LURE),
		r(Items.COBWEB, Enchantments.SILK_TOUCH)
	);

	/** All recipes in a fixed order (by ingredient id). Index = bit used for "unlocked" syncing. */
	public static final List<Map.Entry<Item, ResourceKey<Enchantment>>> ORDERED = BY_INGREDIENT.entrySet().stream()
		.sorted(Comparator.comparing((Map.Entry<Item, ResourceKey<Enchantment>> e) ->
			BuiltInRegistries.ITEM.getKey(e.getKey()).getPath()))
		.toList();

	// ---- knowledge book: unlocked recipes are stored as player tags (saved with the player, survive death)
	private static String tagFor(Item item) {
		return "craftenchanting_unlocked_" + BuiltInRegistries.ITEM.getKey(item).getPath();
	}

	// Entity tag methods were renamed between versions, so they are looked up by name at runtime.
	@SuppressWarnings("unchecked")
	private static Set<String> tagsOf(Entity entity) {
		for (String name : new String[] {"entityTags", "getTags", "tags"}) {
			try {
				Object r = Entity.class.getMethod(name).invoke(entity);
				if (r instanceof Set<?>) {
					return (Set<String>) r;
				}
			} catch (ReflectiveOperationException ignored) {
				// try the next name
			}
		}
		throw new IllegalStateException("No tag getter found on Entity");
	}

	private static void addTag(Entity entity, String tag) {
		try {
			Method m = Entity.class.getMethod("addTag", String.class);
			m.invoke(entity, tag);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("No addTag(String) found on Entity", e);
		}
	}

	public static boolean isUnlocked(ServerPlayer player, int index) {
		return tagsOf(player).contains(tagFor(ORDERED.get(index).getKey()));
	}

	/** Unlocks every recipe whose ingredient item the player is carrying right now. */
	public static void unlockFromInventory(ServerPlayer player) {
		Inventory inv = player.getInventory();
		HashSet<Item> held = new HashSet<>();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty()) {
				held.add(s.getItem());
			}
		}
		for (int i = 0; i < ORDERED.size(); i++) {
			Item item = ORDERED.get(i).getKey();
			if (held.contains(item) && !tagsOf(player).contains(tagFor(item))) {
				addTag(player, tagFor(item));
				// Tell the client so it can show the "new recipe unlocked" toast.
				ServerPlayNetworking.send(player, new UnlockPayload(i));
			}
		}
	}

	/** Unlocked recipes as three 16-bit masks (container data is sent as 16-bit values). */
	public static int[] unlockedMask(ServerPlayer player) {
		int[] mask = new int[3];
		for (int i = 0; i < ORDERED.size(); i++) {
			if (isUnlocked(player, i)) {
				mask[i / 16] |= 1 << (i % 16);
			}
		}
		return mask;
	}

	/**
	 * Grid indices of the 7 ingredient slots. Index 4 (centre) is the book and
	 * index 7 (bottom middle) is the lapis slot, so neither is part of the recipe.
	 */
	public static final int[] RING = {0, 1, 2, 3, 5, 6, 8};

	/** XP level cost by resulting enchantment level I..V. Matches level_1/2/3/6/9 icons. */
	private static final int[] COSTS = {1, 2, 3, 6, 9};
	/** Flat cost for treasure enchantments. Matches the level_12 icon. */
	private static final int TREASURE_COST = 12;

	public static int costFor(ResourceKey<Enchantment> key, int level) {
		if (key.equals(Enchantments.MENDING) || key.equals(Enchantments.FROST_WALKER)
			|| key.equals(Enchantments.SOUL_SPEED) || key.equals(Enchantments.SWIFT_SNEAK)) {
			return TREASURE_COST;
		}
		return COSTS[Math.max(0, Math.min(level, COSTS.length) - 1)];
	}

	/** True if at least one of our enchantments can go on this item (used for the centre slot). */
	public static boolean supportsAny(RegistryAccess access, ItemStack stack) {
		var lookup = access.lookupOrThrow(Registries.ENCHANTMENT);
		for (ResourceKey<Enchantment> k : BY_INGREDIENT.values()) {
			if (lookup.getOrThrow(k).value().isSupportedItem(stack)) {
				return true;
			}
		}
		return false;
	}

	/** Looks at the 3x3 grid and returns the enchantment whose ring recipe it matches. */
	public static Optional<ResourceKey<Enchantment>> find(Container grid) {
		ItemStack first = grid.getItem(RING[0]);
		if (first.isEmpty()) {
			return Optional.empty();
		}
		Item item = first.getItem();
		for (int i : RING) {
			ItemStack s = grid.getItem(i);
			if (s.isEmpty() || s.getItem() != item) {
				return Optional.empty();
			}
		}
		return Optional.ofNullable(BY_INGREDIENT.get(item));
	}
}