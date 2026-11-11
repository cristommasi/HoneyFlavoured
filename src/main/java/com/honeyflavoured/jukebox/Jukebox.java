package com.honeyflavoured.jukebox;

import com.honeyflavoured.HoneyFlavoured;
import com.honeyflavoured.commands.HoneyFlavouredCommands;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

public final class Jukebox {
    private Jukebox() {}

    public static MenuType<JukeboxMenu> MENU_TYPE;

    public static boolean enabled(ServerLevel level) {
        return HoneyFlavouredCommands.enabled(level, HoneyFlavouredCommands.USE_CUSTOM_JUKEBOX);
    }

    public static void register() {
        MENU_TYPE = Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(HoneyFlavoured.MOD_ID, "jukebox"),
                new MenuType<>(JukeboxMenu::new, FeatureFlagSet.of()));

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            BlockPos pos = hit.getBlockPos();
            if (!world.getBlockState(pos).is(Blocks.JUKEBOX) || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            // sneaking with an item in hand still places blocks, like vanilla
            if (player.isShiftKeyDown() && !player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            if (world.isClientSide()) {
                return InteractionResult.SUCCESS; // the server decides; if the rule is off it returns PASS and vanilla runs
            }
            if (!(world instanceof ServerLevel level) || !enabled(level)) {
                return InteractionResult.PASS;
            }
            if (!(level.getBlockEntity(pos) instanceof JukeboxBlockEntity be)
                    || !((Object) be instanceof JukeboxAccess access)) {
                return InteractionResult.PASS;
            }
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new JukeboxMenu(id, inv, access),
                    Component.translatable("block.minecraft.jukebox")));
            return InteractionResult.SUCCESS;
        });
    }
}