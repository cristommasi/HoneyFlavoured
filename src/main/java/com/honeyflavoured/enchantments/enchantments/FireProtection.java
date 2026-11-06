package com.honeyflavoured.enchantments.enchantments;

import com.honeyflavoured.enchantments.NewEnchantments;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;


/** Fire Protection V makes you immune to lava: no lava damage and no burning. One piece at level V is enough. */
public final class FireProtection {
    private FireProtection() {}


    public static void register() {
        
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {

            if (NewEnchantments.enabled && source.is(DamageTypes.LAVA) && NewEnchantments.hasLavaImmunity(entity)) {
                return false;
            }
            return true;
        });

        // Lava sets you on fire every tick; put it out again so you are not burning when you climb out.
        ServerTickEvents.END_SERVER_TICK.register(server -> {

            if (!NewEnchantments.enabled) return;

            for (LivingEntity entity : server.getPlayerList().getPlayers()) {

                if (entity.isInLava() && NewEnchantments.hasLavaImmunity(entity)) {
                    entity.clearFire();
                }
            }
        });
    }
}