package com.honeyflavoured.commands;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.Level;

/**
 * All gamerules of the mod. Use in game:  /gamerule honeyflavoured:cold_damage false
 * Every rule defaults to true (feature on).
 */
public final class HoneyFlavouredGameRules {
    private HoneyFlavouredGameRules() {}

    public static final GameRule<Boolean> COLD_DAMAGE = register("cold_damage");
    public static final GameRule<Boolean> HOT_DAMAGE = register("hot_damage");
    public static final GameRule<Boolean> FARMING_CHANGES = register("farming_changes");
    public static final GameRule<Boolean> MODDED_ENCHANTING = register("modded_enchanting");
    public static final GameRule<Boolean> MODDED_XP = register("modded_xp");
    public static final GameRule<Boolean> FLETCHING_TABLE = register("fletching_table");

    private static GameRule<Boolean> register(String name) {
        return GameRuleBuilder.forBoolean(true)
            .category(GameRuleCategory.MISC)
            .buildAndRegister(Identifier.fromNamespaceAndPath("honeyflavoured", name));
    }

    /** Call once from the main entrypoint so the rules get registered at startup. */
    public static void init() {}

    /** Reads a rule. On the client (no server level) the feature is treated as on. */
    public static boolean enabled(Level level, GameRule<Boolean> rule) {
        return !(level instanceof ServerLevel server) || server.getGameRules().get(rule);
    }
}

/*
/damage x 0 honeyflavoured:freeze
/damage x 0 honeyflavoured:burn
/effect give x honeyflavoured:grace_period 255 true
/gamerule honeyflavoured:doFreezeDamage true
/gamerule honeyflavoured:doBurnDamage true
/gamerule honeyflavoured:useSlowFarmingSpeed true
/gamerule honeyflavoured:useHoeFortuneDrops true
/gamerule honeyflavoured:useFletchingTable true
/gamerule honeyflavoured:useEnchantingTable true
/gamerule honeyflavoured:useNewXpCurve true
*/