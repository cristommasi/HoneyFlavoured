package com.honeyflavoured.temperature;

import java.util.Set;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

/**
 * Grace period: a status effect that stops all weather (cold / hot) damage.
 * Every player gets it once, 5 minutes, the first time they join a world.
 * Commands: /honeyflavoured temperature grace set|remove|toggle <targets>
 * Icon: assets/honeyflavoured/textures/mob_effect/grace_period.png (18x18)
 */
public final class TemperatureGrace {
    private TemperatureGrace() {}

    /** Default length, in ticks (20 ticks = 1 second).*/
    public static final int DEFAULT_TICKS = 10 * 60 * 20;

    private static final String GIVEN_TAG = "honeyflavoured_grace_given";

    public static Holder<MobEffect> EFFECT;

    public static void register() {
        EFFECT = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath("honeyflavoured", "temperature/grace_period"),
            new MobEffect(MobEffectCategory.BENEFICIAL, 0xF2C94C) {}
        );

        // First time a player ever joins this world: give the grace period once.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();

            if (tagsOf(player).contains(GIVEN_TAG)) return;

            addTag(player, GIVEN_TAG);
            give(player, DEFAULT_TICKS);
        });
    }

    public static boolean has(ServerPlayer player) {
        return player.hasEffect(EFFECT);
    }

    public static void give(ServerPlayer player, int ticks) {
        player.addEffect(new MobEffectInstance(EFFECT, ticks, 0, false, false, true));
    }

    public static void remove(ServerPlayer player) {
        player.removeEffect(EFFECT);
    }

    // Entity tag methods were renamed between versions, so they are looked up by name at runtime.
    @SuppressWarnings("unchecked")
    private static Set<String> tagsOf(Entity entity) {
        for (String name : new String[] {"entityTags", "getTags", "tags"}) {
            try {
                Object r = Entity.class.getMethod(name).invoke(entity);
                if (r instanceof Set<?>) {
                    return (Set<String>) r;
                }
            } catch (ReflectiveOperationException ignored) {
                // try the next name
            }
        }
        throw new IllegalStateException("No tag getter found on Entity");
    }

    private static void addTag(Entity entity, String tag) {
        try {
            Entity.class.getMethod("addTag", String.class).invoke(entity, tag);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No addTag(String) found on Entity", e);
        }
    }
}

