
package com.honeyflavoured.farming;

import java.util.List;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.util.RandomSource;

public final class Farming {

    private Farming() {}

    public static void register() {

        LootTableEvents.MODIFY_DROPS.register(Farming::modifyDrops);
    }

    private static void modifyDrops( Holder<LootTable> holder, LootContext context, List<ItemStack> drops) {

        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);

        if (state == null || drops.isEmpty()) {
            return;
        }

        boolean wheat = state.is(Blocks.WHEAT);
        boolean carrots = state.is(Blocks.CARROTS);
        boolean potatoes = state.is(Blocks.POTATOES);
        boolean beetroot = state.is(Blocks.BEETROOTS);


        if (!(wheat || carrots || potatoes || beetroot)) {
            return;
        }


        var tool = context.getOptional(LootContextParams.TOOL);

        boolean hasHoe = tool != null && (tool.is(Items.WOODEN_HOE) || tool.is(Items.STONE_HOE) || tool.is(Items.COPPER_HOE) || tool.is(Items.IRON_HOE)
                                        || tool.is(Items.GOLDEN_HOE) || tool.is(Items.DIAMOND_HOE) || tool.is(Items.NETHERITE_HOE));

        RandomSource random = context.getRandom();
        int fortuneLevel = 0;
            
        if (hasHoe && tool != null) {
        
            var enchantments = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var fortune = enchantments.getOrThrow(Enchantments.FORTUNE);
            fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(fortune, tool);
        }
        
        drops.clear();
        
        if (wheat) {
        
            if (!hasHoe) {
            
                drops.add(new ItemStack(Items.WHEAT, 1));
                drops.add(new ItemStack(Items.WHEAT_SEEDS, 1));
            }
            else if (fortuneLevel > 0) {
            
                drops.add(new ItemStack(Items.WHEAT, random.nextInt(4) + 2 + fortuneLevel - 1));
                drops.add(new ItemStack(Items.WHEAT_SEEDS, random.nextInt(2) + fortuneLevel));
            }
            else {
            
                drops.add(new ItemStack(Items.WHEAT, random.nextInt(2) + 1));
                drops.add(new ItemStack(Items.WHEAT_SEEDS, random.nextInt(2) + 1));
            }
        
        }
        else if (carrots) {
        
            if (!hasHoe) {
            
                drops.add(new ItemStack(Items.CARROT, random.nextInt(2) + 1));
            }
            else if (fortuneLevel > 0) {
            
                drops.add(new ItemStack(Items.CARROT, random.nextInt(fortuneLevel + 1) + 3));
            }
            else {
            
                drops.add(new ItemStack(Items.CARROT, random.nextInt(2) + 2));
            }
        
        }
        else if (potatoes) {
        
            if (!hasHoe) {
            
                drops.add(new ItemStack(Items.POTATO, random.nextInt(2) + 1));
            }
            else if (fortuneLevel > 0) {
            
                drops.add(new ItemStack(Items.POTATO, random.nextInt(fortuneLevel + 1) + 3));
            }
            else {
            
                drops.add(new ItemStack(Items.POTATO, random.nextInt(2) + 2));
            }
        
        }
        else if (beetroot) {
        
            if (!hasHoe) {
            
                drops.add(new ItemStack(Items.BEETROOT, 1));
                drops.add(new ItemStack(Items.BEETROOT_SEEDS, 1));
            }
            else if (fortuneLevel > 0) {
            
                drops.add(new ItemStack(Items.BEETROOT, random.nextInt(4) + 2 + fortuneLevel - 1));
                drops.add(new ItemStack(Items.BEETROOT_SEEDS, random.nextInt(2) + fortuneLevel));
            }
            else {
            
                drops.add(new ItemStack(Items.BEETROOT, random.nextInt(2) + 1));
                drops.add(new ItemStack(Items.BEETROOT_SEEDS, random.nextInt(2) + 1));
            }
        }
    }
}
