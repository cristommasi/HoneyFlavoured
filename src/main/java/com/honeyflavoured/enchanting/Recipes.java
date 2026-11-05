package com.honeyflavoured.enchanting;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import com.honeyflavoured.enchantments.NewEnchantments;

/**
 * The enchanting recipes. 3x3 grid:
 *   - centre = item / book to enchant, bottom middle = lapis
 *   - bottom left + bottom right = a flower belonging to the recipe (see FLOWERS)
 *   - the 5 remaining slots (RING) must hold the recipe's main ingredient.
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
		r(Items.EMERALD, Enchantments.UNBREAKING),
		r(Items.POISONOUS_POTATO, Enchantments.VANISHING_CURSE),

		// Armor
		r(Items.NAUTILUS_SHELL, Enchantments.AQUA_AFFINITY),
		r(Items.BLAZE_POWDER, NewEnchantments.LAVA_AFFINITY),
		r(Items.GUNPOWDER, Enchantments.BLAST_PROTECTION),
		r(Items.SLIME_BALL, Enchantments.BINDING_CURSE),
		r(Items.SPONGE, Enchantments.DEPTH_STRIDER),
		r(Items.FEATHER, Enchantments.FEATHER_FALLING),
		r(Items.ARMADILLO_SCUTE, Enchantments.FIRE_PROTECTION),
		r(Items.SNOWBALL, Enchantments.FROST_WALKER),
		r(Items.TUFF, NewEnchantments.MELEE_PROTECTION),
		r(Items.TURTLE_SCUTE, Enchantments.RESPIRATION),
		r(Items.SOUL_SOIL, Enchantments.SOUL_SPEED),
		r(Items.CACTUS, Enchantments.THORNS),
		r(Items.ECHO_SHARD, Enchantments.SWIFT_SNEAK),
		r(Items.ARROW, Enchantments.PROJECTILE_PROTECTION),
		r(Items.OBSIDIAN, NewEnchantments.IGNEOUS_STRIDER),

		// Melee weapons
		r(Items.PORKCHOP, NewEnchantments.SWINENESS),
		r(Items.STRING, NewEnchantments.BANE_OF_POLYPODS),
		r(Items.GOAT_HORN, Enchantments.BREACH),
		r(Items.ANCIENT_DEBRIS, Enchantments.DENSITY),
		r(Items.AMETHYST_SHARD, Enchantments.EFFICIENCY),
		r(Items.RABBIT_FOOT, Enchantments.LOOTING),
		r(Items.SUGAR, Enchantments.LUNGE),
		r(Items.PRISMARINE_SHARD, Enchantments.IMPALING),
		r(Items.TNT, Enchantments.KNOCKBACK),
		r(Items.DIAMOND, Enchantments.SHARPNESS),
		r(Items.ROTTEN_FLESH, Enchantments.SMITE),
		r(Items.BONE, Enchantments.SWEEPING_EDGE),
		r(Items.WIND_CHARGE, Enchantments.WIND_BURST),
		r(Items.MAGMA_CREAM, Enchantments.FIRE_ASPECT),     

		// Ranged weapons
		// Lightning rods are a copper "collection" in 26.x, so look the plain one up by id.
		r(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("lightning_rod")), Enchantments.CHANNELING),
		r(Items.FIRE_CHARGE, Enchantments.FLAME),
		r(Items.RESIN_CLUMP, Enchantments.INFINITY),
		r(Items.LEAD, Enchantments.LOYALTY),
		r(Items.FIREWORK_ROCKET, Enchantments.RIPTIDE),
		r(Items.QUARTZ, Enchantments.MULTISHOT),
		r(Items.POINTED_DRIPSTONE, Enchantments.PIERCING),
		r(Items.GOLDEN_APPLE, Enchantments.POWER),
		r(Items.SWEET_BERRIES, Enchantments.PUNCH),
		r(Items.HONEYCOMB, Enchantments.QUICK_CHARGE),

		// Tools (Efficiency is honeycomb only, per "first listing only")
		r(Items.ENCHANTED_GOLDEN_APPLE, Enchantments.FORTUNE),
		r(Items.HEART_OF_THE_SEA, Enchantments.LUCK_OF_THE_SEA),
		r(Items.CLOCK, Enchantments.LURE),
		r(Items.COBWEB, Enchantments.SILK_TOUCH),
		r(Items.SULFUR_SPIKE, NewEnchantments.TUNNELING)
	);

	/** All recipes in a fixed order (by ingredient id). Index = bit used for "unlocked" syncing. */
	public static final List<Map.Entry<Item, ResourceKey<Enchantment>>> ORDERED = BY_INGREDIENT.entrySet().stream()
		.sorted(Comparator.comparing((Map.Entry<Item, ResourceKey<Enchantment>> e) -> BuiltInRegistries.ITEM.getKey(e.getKey()).getPath())).toList();

	// ---- flowers: the second ingredient (two of them, bottom-left and bottom-right slots)
	/** Grid indices of the two flower slots. */
	public static final int[] FLOWER_SLOTS = {6, 8};

	private static final Map<ResourceKey<Enchantment>, List<Item>> FLOWERS = new HashMap<>();
	private static final Set<Item> ALL_FLOWERS = new HashSet<>();

	/** Registers the flower(s) for an enchantment by item id. Unknown ids are skipped with a warning. */
	private static void f(ResourceKey<Enchantment> ench, String... ids) {

		List<Item> list = new ArrayList<>();
		for (String id : ids) {

			Identifier ident = Identifier.withDefaultNamespace(id);
			if (BuiltInRegistries.ITEM.containsKey(ident)) {

				list.add(BuiltInRegistries.ITEM.getValue(ident));
			}
			else {

				System.err.println("[enchanting] Unknown flower item id: minecraft:" + id);
			}
		}
		FLOWERS.put(ench, list);
		ALL_FLOWERS.addAll(list);
	}

	static {
		f(Enchantments.MENDING, "wither_rose");
		f(NewEnchantments.SWINENESS, "open_eyeblossom");
		f(Enchantments.UNBREAKING, "vine");
		f(Enchantments.INFINITY, "firefly_bush");
		f(Enchantments.LOOTING, "golden_dandelion");
		f(NewEnchantments.MELEE_PROTECTION, "chorus_flower");
		f(Enchantments.POWER, "sunflower");
		f(Enchantments.EFFICIENCY, "allium");
		f(Enchantments.SILK_TOUCH, "oxeye_daisy");
		f(NewEnchantments.TUNNELING, "flowering_azalea");
		f(Enchantments.FORTUNE, "wildflowers");
		f(Enchantments.SMITE, "poppy");
		f(Enchantments.QUICK_CHARGE, "peony");
		f(Enchantments.KNOCKBACK, "cactus_flower");
		f(Enchantments.FEATHER_FALLING, "blue_orchid");
		f(Enchantments.BLAST_PROTECTION, "red_tulip", "orange_tulip", "white_tulip", "pink_tulip");
		f(Enchantments.PROJECTILE_PROTECTION, "dandelion");
		f(NewEnchantments.BANE_OF_POLYPODS, "azure_bluet");
		f(Enchantments.SWEEPING_EDGE, "lily_of_the_valley");
		f(Enchantments.AQUA_AFFINITY, "fire_coral");
		f(NewEnchantments.LAVA_AFFINITY, "magma", "warped_fungus");
		f(Enchantments.RESPIRATION, "tube_coral");
		f(Enchantments.CHANNELING, "horn_coral");
		f(Enchantments.DEPTH_STRIDER, "brain_coral");
		f(NewEnchantments.IGNEOUS_STRIDER, "dead_brain_coral");
		f(Enchantments.LOYALTY, "kelp");
		f(Enchantments.RIPTIDE, "mangrove_propagule");
		f(Enchantments.LURE, "lily_pad");
		f(Enchantments.LUCK_OF_THE_SEA, "sea_pickle");
		f(Enchantments.FROST_WALKER, "fern");
		f(Enchantments.SWIFT_SNEAK, "pitcher_plant");
		f(Enchantments.IMPALING, "brown_mushroom");
		f(Enchantments.FLAME, "twisting_vines");
		f(Enchantments.FIRE_PROTECTION, "rose_bush");
		f(Enchantments.FIRE_ASPECT, "weeping_vines");
		f(Enchantments.THORNS, "short_dry_grass", "tall_dry_grass");
		f(Enchantments.SOUL_SPEED, "torchflower");
		f(Enchantments.VANISHING_CURSE, "dead_bush");
		f(Enchantments.BINDING_CURSE, "dead_bush");
		f(Enchantments.LUNGE, "cornflower");
		f(Enchantments.DENSITY, "red_shrub");
		f(Enchantments.BREACH, "bush");
		f(Enchantments.MULTISHOT, "pink_petals");
		f(Enchantments.WIND_BURST, "hanging_roots");
		f(Enchantments.PIERCING, "spore_blossom");
		f(Enchantments.PUNCH, "lilac");
	}

	/** The flower items accepted for this enchantment's recipe. */
	public static List<Item> flowersFor(ResourceKey<Enchantment> key) {

		return FLOWERS.getOrDefault(key, List.of());
	}

	/** Is this item a flower of any recipe? (what the two flower slots accept) */
	public static boolean isFlower(Item item) {

		return ALL_FLOWERS.contains(item);
	}

	// ---- knowledge book: unlocked recipes are stored as player tags (saved with the player, survive death)
	private static String tagFor(Item item) {

		return "enchanting_unlocked_" + BuiltInRegistries.ITEM.getKey(item).getPath();
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
			}
			catch (ReflectiveOperationException ignored) {
				// try the next name
			}
		}
		throw new IllegalStateException("No tag getter found on Entity");
	}

	private static void addTag(Entity entity, String tag) {

		try {

			Method m = Entity.class.getMethod("addTag", String.class);
			m.invoke(entity, tag);
		}
		catch (ReflectiveOperationException e) {

			throw new IllegalStateException("No addTag(String) found on Entity", e);
		}
	}

	private static void removeTag(Entity entity, String tag) {
		try {
			Entity.class.getMethod("removeTag", String.class).invoke(entity, tag);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("No removeTag(String) found on Entity", e);
		}
	}

	/** Used by the commands. Returns true if something changed. */
	public static boolean setUnlocked(ServerPlayer player, int index, boolean unlocked, boolean toast) {
		String tag = tagFor(ORDERED.get(index).getKey());
		boolean has = tagsOf(player).contains(tag);
		if (has == unlocked) return false;
		if (unlocked) {
			addTag(player, tag);
			if (toast) ServerPlayNetworking.send(player, new UnlockPayload(index));
		} else {
			removeTag(player, tag);
		}
		return true;
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
			boolean has = held.contains(item);

			for (Item fl : flowersFor(ORDERED.get(i).getValue())) {

				has |= held.contains(fl);
			}
			if (has && !tagsOf(player).contains(tagFor(item))) {
				addTag(player, tagFor(item));
				// Tell the client so it can show the "new recipe unlocked" toast.
				ServerPlayNetworking.send(player, new UnlockPayload(i));
			}
		}
	}

	/** Unlocked recipes as three 16-bit masks (container data is sent as 16-bit values). */
	public static int[] unlockedMask(ServerPlayer player) {

		int[] mask = new int[4];

		for (int i = 0; i < ORDERED.size(); i++) {

			if (isUnlocked(player, i)) {
				
				mask[i / 16] |= 1 << (i % 16);
			}
		}
		return mask;
	}

	/**
	 * Grid indices of the 5 main-ingredient slots. Index 4 (centre) is the book / item, index 7 (bottom middle)
	 * is the lapis slot, and 6 + 8 (bottom left / right) are the flower slots.
	 *  level cost by resulting enchantment level I..V. Matches level_1/2/3/6/9 icons.
	 * Flat cost for treasure enchantments. Matches the level_12 icon. 
	 */
	public static final int[] RING = {0, 1, 2, 3, 5};
	private static final int[] COSTS = {1, 3, 6, 9, 12};
	private static final int TREASURE_COST = 12;

	public static int costFor(ResourceKey<Enchantment> key, int level) {

		if (key.equals(Enchantments.MENDING) || key.equals(Enchantments.FROST_WALKER) || key.equals(Enchantments.SOUL_SPEED) || key.equals(Enchantments.SWIFT_SNEAK)) {

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
		ResourceKey<Enchantment> key = BY_INGREDIENT.get(item);

		if (key == null) {

			return Optional.empty();
		}

		List<Item> flowers = flowersFor(key);

		for (int i : FLOWER_SLOTS) {

			ItemStack s = grid.getItem(i);

			if (s.isEmpty() || !flowers.contains(s.getItem())) {
				
				return Optional.empty();
			}
		}
		return Optional.of(key);
	}
}