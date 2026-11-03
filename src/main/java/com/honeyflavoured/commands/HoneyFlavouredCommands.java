package com.honeyflavoured.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public final class HoneyFlavouredCommands {
    private HoneyFlavouredCommands() {}

    // Gamerules: all enabled by default.
    public static final GameRule<Boolean> DO_FREEZE_DAMAGE =
        registerRule("do_freeze_damage");

    public static final GameRule<Boolean> DO_BURN_DAMAGE =
        registerRule("do_burn_damage");

    public static final GameRule<Boolean> USE_SLOW_FARMING_SPEED =
        registerRule("use_slow_farming_speed");

    public static final GameRule<Boolean> USE_HOE_FORTUNE_DROPS =
        registerRule("use_hoe_fortune_drops");

    public static final GameRule<Boolean> USE_FLETCHING_TABLE =
        registerRule("use_fletching_table");

    public static final GameRule<Boolean> USE_ENCHANTING_TABLE =
        registerRule("use_enchanting_table");

    public static final GameRule<Boolean> USE_NEW_XP_CURVE =
        registerRule("use_new_xp_curve");

    public static final GameRule<Boolean> USE_BUNDLES =
        registerRule("use_bundles");

    private static GameRule<Boolean> registerRule(String name) {
        return GameRuleBuilder.forBoolean(true)
            .category(GameRuleCategory.MISC)
            .buildAndRegister(
                Identifier.fromNamespaceAndPath("honeyflavoured", name)
            );
    }

    public static void init() {}

    // Client-side levels are treated as enabled.
    public static boolean enabled(Level level, GameRule<Boolean> rule) {
        return !(level instanceof ServerLevel server)
            || server.getGameRules().get(rule);
    }

    public static void register() {
        init();

        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> {

                // /honeyflavoured help
                dispatcher.register(
                    Commands.literal("honeyflavoured")
                        .requires(source ->
                            source.permissions().hasPermission(
                                Permissions.COMMANDS_GAMEMASTER
                            )
                        )
                        .then(Commands.literal("help")
                            .executes(ctx -> {
                                ctx.getSource().sendSuccess(
                                    () -> Component.literal(
                                        "HoneyFlavoured commands:\n\n"
                                        + "Temperature:\n"
                                        + "/effect give <targets> honeyflavoured:freeze\n"
                                        + "/effect give <targets> honeyflavoured:burn\n"
                                        + "/effect give <targets> honeyflavoured:grace_period 255 true\n"

                                        + "Enchanting recipes:\n"
                                        + "/recipe give <targets> honeyflavoured:<recipe>\n"
                                        + "/recipe take <targets> honeyflavoured:<recipe>\n"
                                        + "/recipe give <targets> honeyflavoured:*\n"
                                        + "/recipe take <targets> honeyflavoured:*\n\n"

                                        + "Gamerules:\n"
                                        + "/gamerule honeyflavoured:do_freeze_damage <true|false>\n"
                                        + "/gamerule honeyflavoured:do_burn_damage <true|false>\n"
                                        + "/gamerule honeyflavoured:use_slow_farming_speed <true|false>\n"
                                        + "/gamerule honeyflavoured:use_hoe_fortune_drops <true|false>\n"
                                        + "/gamerule honeyflavoured:use_fletching_table <true|false>\n"
                                        + "/gamerule honeyflavoured:use_enchanting_table <true|false>\n"
                                        + "/gamerule honeyflavoured:use_new_xp_curve <true|false>\n"
                                        + "/gamerule honeyflavoured:use_bundles <true|false>"
                                    ),
                                    false
                                );
                                return 1;
                            })
                        )
                );

                // Register temperature commands and extend vanilla /effect.
                TemperatureCommand.register(dispatcher);

                // Extend vanilla /recipe with HoneyFlavoured recipes.
                EnchantingCommand.register(dispatcher);
            }
        );
    }
}