package com.honeyflavoured.enchantments;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;

/** Fire Protection V makes you immune to lava: no lava damage and no burning. One piece at level V is enough. */
public final class FireProtection {
    private FireProtection() {}

    private static final int LEVEL_NEEDED = 5;

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (NewEnchantments.enabled
                && entity instanceof Player player
                && source.is(DamageTypes.LAVA)
                && NewEnchantments.armorLevel(player, Enchantments.FIRE_PROTECTION) >= LEVEL_NEEDED) {
                return false;
            }
            return true;
        });

        // Lava sets you on fire every tick; put it out again so you are not burning when you climb out.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!NewEnchantments.enabled) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isInLava() && NewEnchantments.armorLevel(player, Enchantments.FIRE_PROTECTION) >= LEVEL_NEEDED) {
                    player.clearFire();
                }
            }
        });
    }
}