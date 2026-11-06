package com.honeyflavoured.enchantments.enchantments;

import com.honeyflavoured.enchantments.NewEnchantments;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.Enchantments;

/** Blast Protection also reduces: Warden sonic boom, Ender Dragon attacks, mob ramming and Breeze attacks. */
public final class BlastProtection {
    private BlastProtection() {}

    private static final ThreadLocal<Boolean> reducing = ThreadLocal.withInitial(() -> false);

    private static boolean isBlastLike(DamageSource source) {
        if (source.is(DamageTypes.SONIC_BOOM)) {
            return true;
        }

        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();

        return isBlastEntity(attacker) || isBlastEntity(direct);
    }

    private static boolean isBlastEntity(Entity entity) {
        if (entity == null) {
            return false;
        }

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();

        return switch (id) {
            case "ender_dragon",
                "hoglin",
                "goat",
                "ravager",
                "breeze",
                "breeze_wind_charge" -> true;
            default -> false;
        };
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {

            if (reducing.get() || !NewEnchantments.enabled
                || !(entity.level() instanceof ServerLevel level)
                || !isBlastLike(source)) {
                return true;
            }

            int levels = NewEnchantments.armorLevelSum(entity, Enchantments.BLAST_PROTECTION);

            if (levels <= 0) {
                return true;
            }

            // Same formula as vanilla Blast Protection.
            float reduced = amount * (1.0F - Math.min(20, levels * 2) / 25.0F);

            reducing.set(true);

            try {
                entity.hurtServer(level, source, reduced);
            }
            finally {
                reducing.remove();
            }

            return false;
        });
    }
}