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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.resources.ResourceKey;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.honeyflavoured.HoneyFlavoured;
import net.fabricmc.loader.api.FabricLoader;

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

	public static final Map<Item, ResourceKey<Enchantment>> BY_INGREDIENT;
	/** All recipes in a fixed order (by ingredient id). Index = bit used for "unlocked" syncing. */
	public static final List<Map.Entry<Item, ResourceKey<Enchantment>>> ORDERED;
	private static final Map<ResourceKey<Enchantment>, String> DESCRIPTIONS = new HashMap<>();

	// ---- flowers: the second ingredient (two of them, bottom-left and bottom-right slots)
	/** Grid indices of the two flower slots. */
	public static final int[] FLOWER_SLOTS = {6, 8};

	private static final Map<ResourceKey<Enchantment>, List<Item>> FLOWERS = new HashMap<>();
	private static final Set<Item> ALL_FLOWERS = new HashSet<>();

	static {
		Map<Item, ResourceKey<Enchantment>> byIngredient = new HashMap<>();
		try {
			Path dir = FabricLoader.getInstance().getModContainer(HoneyFlavoured.MOD_ID).orElseThrow()
				.findPath("data/" + HoneyFlavoured.MOD_ID + "/enchanting_recipes").orElseThrow();
			try (Stream<Path> files = Files.list(dir)) {
				for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
					readRecipe(file, byIngredient);
				}
			}
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not read the enchanting recipes", e);
		}
		BY_INGREDIENT = Map.copyOf(byIngredient);
		ORDERED = BY_INGREDIENT.entrySet().stream()
			.sorted(Comparator.comparing((Map.Entry<Item, ResourceKey<Enchantment>> e) -> BuiltInRegistries.ITEM.getKey(e.getKey()).getPath()))
			.toList();
	}

	private static Item itemOf(Path file, String id) {
		Identifier ident = Identifier.parse(id);
		if (!BuiltInRegistries.ITEM.containsKey(ident)) {
			throw new IllegalStateException(file.getFileName() + ": unknown item " + id);
		}
		return BuiltInRegistries.ITEM.getValue(ident);
	}

	private static void readRecipe(Path file, Map<Item, ResourceKey<Enchantment>> out) throws IOException {
		JsonObject json;
		try (Reader reader = Files.newBufferedReader(file)) {
			json = JsonParser.parseReader(reader).getAsJsonObject();
		}

		ResourceKey<Enchantment> enchantment = ResourceKey.create(Registries.ENCHANTMENT,
			Identifier.parse(json.get("enchantment").getAsString()));
		Item ingredient = itemOf(file, json.get("ingredient").getAsString());

		if (out.put(ingredient, enchantment) != null) {
			throw new IllegalStateException(file.getFileName() + ": that ingredient is already used by another recipe");
		}

		List<Item> flowers = new ArrayList<>();
		if (json.has("flowers")) {
			for (JsonElement e : json.getAsJsonArray("flowers")) {
				flowers.add(itemOf(file, e.getAsString()));
			}
		}
		FLOWERS.put(enchantment, flowers);
		ALL_FLOWERS.addAll(flowers);
		DESCRIPTIONS.put(enchantment, json.has("description") ? json.get("description").getAsString() : "");
	}

	public static String descriptionFor(ResourceKey<Enchantment> key) {
		return DESCRIPTIONS.getOrDefault(key, "");
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