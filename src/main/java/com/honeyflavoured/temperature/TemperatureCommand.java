package com.honeyflavoured.temperature;

import java.util.Collection;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /honeyflavoured temperature cold <targets>   fills all cold hearts
 * /honeyflavoured temperature hot <targets>    fills all hot hearts
 * /honeyflavoured temperature clear <targets>  empties both
 */
public final class TemperatureCommand {
    private TemperatureCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("temperature")
            .then(Commands.literal("cold").then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> set(ctx, 1.0F, 0.0F, "full cold hearts"))))
            .then(Commands.literal("hot").then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> set(ctx, 0.0F, 1.0F, "full hot hearts"))))
            .then(Commands.literal("clear").then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> set(ctx, 0.0F, 0.0F, "no temperature hearts"))));
    }

    private static int set(CommandContext<CommandSourceStack> ctx, float cold, float hot, String what) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");

        for (ServerPlayer player : players) {
            TemperatureManager.setExposure(player, cold, hot);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("Gave " + what + " to " + players.size() + " player(s)"), true);
        return players.size();
    }
}