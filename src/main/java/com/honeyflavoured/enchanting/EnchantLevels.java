package com.honeyflavoured.enchanting;

import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Enchanting rules for levels.
 *  - requiredLevel: the player level you must have reached, depending on the item being enchanted.
 *  - xpCost: the levels actually taken, 1 to 3, depending on how high the enchantment goes.
 */
public final class EnchantLevels {
    private EnchantLevels() {}

    /** Level needed for enchantable items that are not in the lists below (shears, compass...). */
    public static final int DEFAULT_LEVEL = 3;
    public static final int BOOK_LEVEL = 15;

    /** Single items, by item id (checked first). */
    private static final Map<String, Integer> BY_ITEM = Map.ofEntries(
        Map.entry("carved_pumpkin", 3),
        Map.entry("turtle_helmet", 9),
        Map.entry("elytra", 15),
        Map.entry("carrot_on_a_stick", 3),
        Map.entry("warped_fungus_on_a_stick", 3),
        Map.entry("brush", 3),
        Map.entry("flint_and_steel", 3),
        Map.entry("fishing_rod", 6),
        Map.entry("crossbow", 6),
        Map.entry("bow", 6),
        Map.entry("shield", 6),
        Map.entry("trident", 9),
        Map.entry("mace", 12)
    );

    /** Whole families of items, by the start of the item id: every tool, spear and armour piece of that material. */
    private static final Map<String, Integer> BY_MATERIAL = Map.ofEntries(
        Map.entry("leather_", 3),
        Map.entry("wooden_", 3),
        Map.entry("stone_", 3),
        Map.entry("copper_", 6),
        Map.entry("golden_", 9),
        Map.entry("chainmail_", 12),
        Map.entry("iron_", 12),
        Map.entry("diamond_", 15),
        Map.entry("netherite_", 18)
    );

    public static int requiredLevel(ItemStack stack) {
        if (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) {
            return BOOK_LEVEL;
        }

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

        Integer single = BY_ITEM.get(id);
        if (single != null) {
            return single;
        }

        // Mob heads (skeleton, wither skeleton, zombie, creeper, piglin, dragon, player).
        if (id.endsWith("_head") || id.endsWith("_skull")) {
            return 3;
        }

        for (Map.Entry<String, Integer> e : BY_MATERIAL.entrySet()) {
            if (id.startsWith(e.getKey())) {
                return e.getValue();
            }
        }

        return DEFAULT_LEVEL;
    }

    /** Levels taken per step, by how high the enchantment goes. The last level always costs 3. */
    private static final int[][] COSTS = {
        {},
        {3},
        {1, 3},
        {1, 2, 3},
        {1, 2, 2, 3},
        {1, 2, 2, 3, 3}
    };

    /** Levels taken when raising an enchantment (max level maxLevel) to newLevel. */
    public static int xpCost(int maxLevel, int newLevel) {
        int[] row = COSTS[Math.max(1, Math.min(maxLevel, 5))];
        return row[Math.max(1, Math.min(newLevel, row.length)) - 1];
    }
}