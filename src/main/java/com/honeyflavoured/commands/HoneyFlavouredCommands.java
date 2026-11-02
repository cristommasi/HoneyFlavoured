package com.honeyflavoured.commands;

import com.honeyflavoured.enchanting.EnchantingCommand;
import com.honeyflavoured.temperature.TemperatureCommand;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;

/**
 * Root of every command of the mod: /honeyflavoured <feature> ...
 * Each feature has its own XCommand class in its own folder with a build() method.
 * To add a command for a new feature, create its class and add one .then(...) line below.
 */
public final class HoneyFlavouredCommands {
    private HoneyFlavouredCommands() {}

    public static void register() {
        HoneyFlavouredGameRules.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("honeyflavoured")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(EnchantingCommand.build())
                .then(TemperatureCommand.build())
                // .then(FletchingCommand.build())   <- future features go here
            )
        );
    }
}