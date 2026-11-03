package com.honeyflavoured.fletching;

import com.honeyflavoured.HoneyFlavoured;
import com.honeyflavoured.commands.HoneyFlavouredCommands;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;


/** Fletching table feature: arrows + lingering potion = tipped arrows. Call Fletching.register() once from onInitialize. */
public final class Fletching {
	private Fletching() {}

	public static MenuType<FletchingMenu> MENU_TYPE;

	public static void register() {
		MENU_TYPE = Registry.register(
			BuiltInRegistries.MENU,
			Identifier.fromNamespaceAndPath(HoneyFlavoured.MOD_ID, "fletching"),
			new MenuType<>(FletchingMenu::new, FeatureFlagSet.of())
		);

		// Right-click a fletching table to open our menu.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			BlockPos pos = hit.getBlockPos();
			if (!world.getBlockState(pos).is(Blocks.FLETCHING_TABLE) || player.isSpectator()) {
				return InteractionResult.PASS;
			}
			// Gamerule honeyflavoured:fletching_table false = vanilla fletching table.
			if (!HoneyFlavouredCommands.enabled(world, HoneyFlavouredCommands.USE_FLETCHING_TABLE)) {
				return InteractionResult.PASS;
			}
			// Same as vanilla: sneaking with an item in hand lets you place blocks instead.
			if (player.isShiftKeyDown() && !player.getMainHandItem().isEmpty()) {
				return InteractionResult.PASS;
			}
			if (!world.isClientSide()) {
				player.openMenu(new SimpleMenuProvider(
					(id, inv, p) -> new FletchingMenu(id, inv, ContainerLevelAccess.create(world, pos)),
					Component.translatable("container.fletching.title")
				));
			}
			return InteractionResult.SUCCESS;
		});
	}
}