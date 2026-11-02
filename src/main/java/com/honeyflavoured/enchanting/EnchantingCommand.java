package com.honeyflavoured.enchanting;

import java.util.Collection;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /honeyflavoured enchanting unlock <targets> all | <enchantment>
 * /honeyflavoured enchanting lock <targets> all | <enchantment>
 * <enchantment> is the enchantment name, for example mending or sharpness (tab completion works).
 */
public final class EnchantingCommand {
    private EnchantingCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("enchanting")
            .then(Commands.literal("unlock").then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.literal("all").executes(ctx -> run(ctx, true, null)))
                .then(Commands.argument("recipe", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(names(), builder))
                    .executes(ctx -> run(ctx, true, StringArgumentType.getString(ctx, "recipe"))))))
            .then(Commands.literal("lock").then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.literal("all").executes(ctx -> run(ctx, false, null)))
                .then(Commands.argument("recipe", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(names(), builder))
                    .executes(ctx -> run(ctx, false, StringArgumentType.getString(ctx, "recipe"))))));
    }

    private static Iterable<String> names() {
        return Recipes.ORDERED.stream().map(e -> e.getValue().identifier().getPath()).toList();
    }

    private static int run(CommandContext<CommandSourceStack> ctx, boolean unlock, String recipe) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");

        int only = -1;
        if (recipe != null) {
            for (int i = 0; i < Recipes.ORDERED.size(); i++) {
                if (Recipes.ORDERED.get(i).getValue().identifier().getPath().equals(recipe)) only = i;
            }
            if (only < 0) {
                throw new SimpleCommandExceptionType(Component.literal("Unknown enchanting recipe: " + recipe)).create();
            }
        }

        int changed = 0;
        for (ServerPlayer player : players) {
            if (only >= 0) {
                if (Recipes.setUnlocked(player, only, unlock, true)) changed++;
            }
            else {
                for (int i = 0; i < Recipes.ORDERED.size(); i++) {
                    if (Recipes.setUnlocked(player, i, unlock, false)) changed++;
                }
            }
        }

        final int total = changed;
        ctx.getSource().sendSuccess(() -> Component.literal((unlock ? "Unlocked " : "Locked ") + total + " recipe(s)"), true);
        return total;
    }
}