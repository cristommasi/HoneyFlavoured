package com.honeyflavoured.enchanting;

import com.honeyflavoured.commands.HoneyFlavouredCommands;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;

public final class Enchanting {

	private Enchanting() {}

	public static MenuType<EnchantingMenu> MENU_TYPE;

	
	public static void register() {

		MENU_TYPE = Registry.register( 
			BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath( com.honeyflavoured.HoneyFlavoured.MOD_ID, "enchanting"), new MenuType<>(EnchantingMenu::new, FeatureFlagSet.of())
		);

		// Packet used for the "new enchanting recipe unlocked" toast.
		registerUnlockPacket();

		// Lapis lazuli enchantment glint.
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(Items.LAPIS_LAZULI, builder -> builder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

		// Knowledge book: unlock recipes when the player picks up their ingredient (checked once a second).
		ServerTickEvents.END_SERVER_TICK.register(server -> {

			if (server.getTickCount() % 20 != 0) {

				return;
			}
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {

				Recipes.unlockFromInventory(p);
			}
		});

		// Replace the vanilla enchanting table screen with our crafting menu.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {

			BlockPos pos = hit.getBlockPos();
			if (!world.getBlockState(pos).is(Blocks.ENCHANTING_TABLE)) {

				return InteractionResult.PASS;
			}
			if (player.isSpectator()) {

				return InteractionResult.PASS;
			}
			// Gamerule honeyflavoured:modded_enchanting false = vanilla enchanting table.
			if (!HoneyFlavouredCommands.enabled(world, HoneyFlavouredCommands.USE_ENCHANTING_TABLE)) {

				return InteractionResult.PASS;
			}
			// Same as vanilla: sneaking with an item in hand lets you place blocks instead.
			if (player.isShiftKeyDown() && !player.getMainHandItem().isEmpty()) {

				return InteractionResult.PASS;
			}
			if (!world.isClientSide()) {

				player.openMenu(new SimpleMenuProvider(
					(id, inv, p) -> new EnchantingMenu(id, inv, ContainerLevelAccess.create(world, pos)),
					Component.translatable("container.enchanting.title")
				));
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** Fabric renamed playS2C() to clientboundPlay(); try both so this builds on either. */
	private static void registerUnlockPacket() {

		for (String name : new String[] {"clientboundPlay", "playS2C"}) {

			try {

				Object registry = PayloadTypeRegistry.class.getMethod(name).invoke(null);
				PayloadTypeRegistry.class
					.getMethod("register", CustomPacketPayload.Type.class, StreamCodec.class)
					.invoke(registry, UnlockPayload.TYPE, UnlockPayload.CODEC);
				return;
			}
			catch (ReflectiveOperationException ignored) {
				// try the next name
			}
		}
		throw new IllegalStateException("Could not register the recipe-unlocked packet");
	}
}