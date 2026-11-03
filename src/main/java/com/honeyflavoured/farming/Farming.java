package com.honeyflavoured.farming;

import java.util.List;

import com.honeyflavoured.commands.HoneyFlavouredGameRules;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Crop drops for wheat, carrots, potatoes and beetroots (fully grown crops only).
 *
 * With a hoe (same numbers as vanilla carrots / potatoes, Java Edition):
 *   crop item  = 2 + binomial(3 + Fortune, 4/7)   -> 2 to 5 without Fortune
 *   seeds      = 1 + binomial(3 + Fortune, 4/7)   -> 1 to 4 without Fortune
 * Without a hoe (minimum drops):
 *   carrots / potatoes: 1 or 2
 *   wheat / beetroot: 1 seed always, plus the crop item 50% of the time
 * Young crops are left alone, so they drop what vanilla gives.
 */
public final class Farming {

    private Farming() {}

    public static void register() {

        LootTableEvents.MODIFY_DROPS.register(Farming::modifyDrops);
    }

    private static void modifyDrops(Holder<LootTable> holder, LootContext context, List<ItemStack> drops) {

        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);

        if (state == null || drops.isEmpty()) {
            return;
        }

        // Gamerule honeyflavoured:farming_changes false = vanilla crop drops.
        if (!HoneyFlavouredGameRules.enabled(context.getLevel(), HoneyFlavouredGameRules.FARMING_CHANGES)) {
            return;
        }

        boolean wheat = state.is(Blocks.WHEAT);
        boolean carrots = state.is(Blocks.CARROTS);
        boolean potatoes = state.is(Blocks.POTATOES);
        boolean beetroot = state.is(Blocks.BEETROOTS);

        if (!(wheat || carrots || potatoes || beetroot)) {
            return;
        }

        // Crops that are not fully grown keep their vanilla drops.
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
            return;
        }

        var tool = context.getOptional(LootContextParams.TOOL);

        boolean hasHoe = tool != null && (tool.is(Items.WOODEN_HOE) || tool.is(Items.STONE_HOE) || tool.is(Items.COPPER_HOE) || tool.is(Items.IRON_HOE)
                                        || tool.is(Items.GOLDEN_HOE) || tool.is(Items.DIAMOND_HOE) || tool.is(Items.NETHERITE_HOE));

        RandomSource random = context.getRandom();
        int fortuneLevel = 0;

        if (hasHoe) {

            var enchantments = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var fortune = enchantments.getOrThrow(Enchantments.FORTUNE);
            fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(fortune, tool);
        }

        drops.clear();

        if (carrots || potatoes) {

            Item item = carrots ? Items.CARROT : Items.POTATO;

            if (!hasHoe) {
                drops.add(new ItemStack(item, random.nextInt(2) + 1));
            }
            else {
                drops.add(new ItemStack(item, 2 + binomial(random, 3 + fortuneLevel)));
            }
            return;
        }

        // Wheat and beetroot.
        Item cropItem = wheat ? Items.WHEAT : Items.BEETROOT;
        Item seeds = wheat ? Items.WHEAT_SEEDS : Items.BEETROOT_SEEDS;

        if (!hasHoe) {
            drops.add(new ItemStack(seeds, 1));

            int amount = random.nextInt(2);
            if (amount > 0) {
                drops.add(new ItemStack(cropItem, amount));
            }
        }
        else {
            drops.add(new ItemStack(cropItem, 2 + binomial(random, 3 + fortuneLevel)));
            drops.add(new ItemStack(seeds, 1 + binomial(random, 3 + fortuneLevel)));
        }
    }

    /** Number of successes in n tries with a 4 in 7 chance each (vanilla's crop distribution). */
    private static int binomial(RandomSource random, int tries) {

        int successes = 0;

        for (int i = 0; i < tries; i++) {
            if (random.nextInt(7) < 4) {
                successes++;
            }
        }

        return successes;
    }
}