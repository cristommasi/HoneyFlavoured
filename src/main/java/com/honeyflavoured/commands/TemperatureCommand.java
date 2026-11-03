package com.honeyflavoured.commands;

import java.util.Collection;

import com.honeyflavoured.temperature.TemperatureManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class TemperatureCommand {
    private TemperatureCommand() {}

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        CommandNode<CommandSourceStack> effect =
            dispatcher.getRoot().getChild("effect");

        if (effect == null) {
            throw new IllegalStateException("Vanilla /effect command not found");
        }

        CommandNode<CommandSourceStack> give = effect.getChild("give");

        if (give == null) {
            throw new IllegalStateException("Vanilla /effect give command not found");
        }

        CommandNode<CommandSourceStack> targets = give.getChild("targets");

        if (targets == null) {
            throw new IllegalStateException("Could not find /effect give targets argument");
        }

        // /effect give <targets> honeyflavoured:freeze
        targets.addChild(
            Commands.literal("honeyflavoured:freeze")
                .executes(ctx -> apply(ctx, true))
                .build()
        );

        // /effect give <targets> honeyflavoured:burn
        targets.addChild(
            Commands.literal("honeyflavoured:burn")
                .executes(ctx -> apply(ctx, false))
                .build()
        );
    }

    private static int apply(
        com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
        boolean freeze
    ) throws CommandSyntaxException {

        Collection<? extends Entity> targets =
            EntityArgument.getEntities(ctx, "targets");

        int affected = 0;

        for (Entity entity : targets) {
            if (entity instanceof ServerPlayer player) {
                if (freeze) {
                    TemperatureManager.setExposure(player, 1.0F, 0.0F);
                } else {
                    TemperatureManager.setExposure(player, 0.0F, 1.0F);
                }

                affected++;
            }
        }

        String effect = freeze ? "freeze" : "burn";
        final int count = affected;

        ctx.getSource().sendSuccess(
            () -> Component.literal(
                "Applied " + effect + " temperature to " + count + " player(s)"
            ),
            true
        );

        return affected;
    }
}