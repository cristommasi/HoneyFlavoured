
package com.honeyflavoured.commands;

import java.util.Collection;

import com.honeyflavoured.enchanting.Recipes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.brigadier.tree.CommandNode;

public final class EnchantingCommand {
    private EnchantingCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> recipe =
            dispatcher.getRoot().getChild("recipe");

        if (recipe == null) {
            throw new IllegalStateException("Vanilla /recipe command was not registered");
        }

        registerBranch(recipe, "give", true);
        registerBranch(recipe, "take", false);
    }

    private static void registerBranch(
        CommandNode<CommandSourceStack> recipe,
        String action,
        boolean unlock
    ) {
        CommandNode<CommandSourceStack> branch = recipe.getChild(action);

        if (branch == null) {
            throw new IllegalStateException(
                "Vanilla /recipe " + action + " command was not registered"
            );
        }

        CommandNode<CommandSourceStack> targets = branch.getChild("targets");

        if (targets == null) {
            throw new IllegalStateException(
                "Could not find the targets argument in vanilla /recipe"
            );
        }

        for (int i = 0; i < Recipes.ORDERED.size(); i++) {
            final int index = i;

            String id = Recipes.ORDERED.get(i)
                .getValue()
                .identifier()
                .toString();

            targets.addChild(
                Commands.literal(id)
                    .executes(ctx -> run(ctx, unlock, index))
                    .build()
            );
        }

        targets.addChild(
            Commands.literal("honeyflavoured:*")
                .executes(ctx -> run(ctx, unlock, -1))
                .build()
        );
    }

    private static int run(
        CommandContext<CommandSourceStack> ctx,
        boolean unlock,
        int only
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players =
            EntityArgument.getPlayers(ctx, "targets");

        int changed = 0;

        for (ServerPlayer player : players) {
            if (only >= 0) {
                if (Recipes.setUnlocked(player, only, unlock, true)) {
                    changed++;
                }
            } else {
                for (int i = 0; i < Recipes.ORDERED.size(); i++) {
                    if (Recipes.setUnlocked(player, i, unlock, false)) {
                        changed++;
                    }
                }
            }
        }

        final int total = changed;

        ctx.getSource().sendSuccess(
            () -> Component.literal(
                (unlock ? "Unlocked " : "Locked ")
                    + total + " enchanting recipe(s)"
            ),
            true
        );

        return total;
    }
}
