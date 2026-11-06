package com.honeyflavoured.enchantments.enchantments;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.network.chat.Component;

public class SilkTouch {

	public static void register() {

		PlayerBlockBreakEvents.AFTER.register(SilkTouch::onBlockBreak);
	}

	public static boolean hasSilkTouch(Level level, ItemStack tool) {

		var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

		return (EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.SILK_TOUCH), tool) > 0);
	}

	private static void onBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {

		ItemStack tool = player.getMainHandItem();
		ItemStack drop;
		TagValueOutput output;
		CompoundTag data;

		if (level.isClientSide() || player.isCreative() || !state.is(Blocks.SPAWNER) || blockEntity == null) {

			return;
		}
		if (!tool.isCorrectToolForDrops(state) || !hasSilkTouch(level, tool)) {

			return;
		}
		output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		blockEntity.saveCustomOnly(output);
		data = output.buildResult();
		drop = new ItemStack(Items.SPAWNER);
		BlockItem.setBlockEntityData(drop, blockEntity.getType(), output);
		drop.set(DataComponents.RARITY, Rarity.UNCOMMON);
		drop.set(DataComponents.ITEM_NAME, getSpawnerName(data));
		Block.popResource(level, pos, drop);
	}

	private static Component getSpawnerName(CompoundTag data) {

		String id = data.getCompound("SpawnData").flatMap(spawn -> spawn.getCompound("entity")).flatMap(entity -> entity.getString("id")).orElse("");
		String key;

		if (id.isEmpty()) {

			return (Component.translatable("block.minecraft.spawner"));
		}
		key = "entity." + id.replace(":", ".");
		return (Component.translatable("item.honeyflavoured.spawner", Component.translatable(key)));
	}
}