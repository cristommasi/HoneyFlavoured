package com.honeyflavoured.enchantments;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.Enchantments;


/** Blast Protection also reduces: Warden sonic boom, Elder Guardian laser, Ender Dragon melee hit. */
public final class BlastProtection {
    private BlastProtection() {}

    private static boolean reducing;

    private static boolean isBlastLike(DamageSource source) {
        if (source.is(DamageTypes.SONIC_BOOM)) {
            return true;
        }

        Entity attacker = source.getEntity();

        if (attacker == null) {
            return false;
        }

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).getPath();

        if (id.equals("ender_dragon")) {
            return true;
        }
        return false;
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {

            if (reducing || !NewEnchantments.enabled || !(entity.level() instanceof ServerLevel level) || !isBlastLike(source)) {
                return true;
            }

            int levels = NewEnchantments.armorLevelSum(entity, Enchantments.BLAST_PROTECTION);

            if (levels <= 0) {
                return true;
            }

            // Same formula as vanilla: 2 points per level across all pieces, capped at 20, each point = 4%.
            float reduced = amount * (1.0F - Math.min(20, levels * 2) / 25.0F);

            reducing = true;

            try {
                entity.hurtServer(level, source, reduced);
            }
            finally {
                reducing = false;
            }
            return false;
        });
    }
}