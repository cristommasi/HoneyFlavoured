package com.honeyflavoured.enchantments;

import com.honeyflavoured.commands.HoneyFlavouredCommands;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The mod's own enchantments. Their stats are in data/honeyflavoured/enchantment/*.json.
 * Code behind them:
 *   Tunneling        -> Tunneling.java
 *   Igneous Strider  -> mixin/LivingEntityMixin.java
 *   Fire Protection V lava immunity -> LavaImmunity.java
 * Switched with /gamerule honeyflavoured:use_new_enchantments <true|false>.
 * (Melee Protection, the level 5 protections, Bane of Polypods and Porcine Grace are plain data and are always on.)
 */
public final class NewEnchantments {
    private NewEnchantments() {}

    public static final ResourceKey<Enchantment> TUNNELING = key("tunneling");
    public static final ResourceKey<Enchantment> IGNEOUS_STRIDER = key("igneous_strider");
    public static final ResourceKey<Enchantment> MELEE_PROTECTION = key("melee_protection");
    public static final ResourceKey<Enchantment> PORCINE_GRACE = key("porcine_grace");
    public static final ResourceKey<Enchantment> BANE_OF_POLYPODS = key("bane_of_polypods");

    /** An armour piece can carry at most this many different protection enchantments. */
    public static final int MAX_PROTECTIONS = 2;

    /** Read by the code above. Set by the server tick, and on the client by the packet. */
    public static volatile boolean enabled = true;

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("honeyflavoured", name));
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(NewEnchantmentsPayload.TYPE, NewEnchantmentsPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            boolean now = HoneyFlavouredCommands.enabled(server.overworld(), HoneyFlavouredCommands.USE_NEW_ENCHANTMENTS);
            enabled = now;
            ServerPlayNetworking.send(handler.getPlayer(), new NewEnchantmentsPayload(now));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean now = HoneyFlavouredCommands.enabled(server.overworld(), HoneyFlavouredCommands.USE_NEW_ENCHANTMENTS);

            if (now != enabled) {
                enabled = now;

                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, new NewEnchantmentsPayload(now));
                }
            }
        });

        Tunneling.register();
        LavaImmunity.register();
    }

    public static Holder<Enchantment> holder(RegistryAccess access, ResourceKey<Enchantment> key) {
        return access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static boolean isProtection(Holder<Enchantment> h) {
        return h.is(Enchantments.PROTECTION) || h.is(Enchantments.FIRE_PROTECTION)
            || h.is(Enchantments.BLAST_PROTECTION) || h.is(Enchantments.PROJECTILE_PROTECTION)
            || h.is(MELEE_PROTECTION);
    }

    /**
     * True when adding this enchantment would put a 3rd kind of protection on the item.
     * Levelling up one it already has is always fine. Used by the enchanting menu.
     */
    public static boolean tooManyProtections(ItemEnchantments existing, Holder<Enchantment> adding) {
        if (!isProtection(adding)) {
            return false;
        }

        int others = 0;

        for (Holder<Enchantment> h : existing.keySet()) {
            if (h.equals(adding)) {
                return false;
            }
            if (isProtection(h)) {
                others++;
            }
        }

        return others >= MAX_PROTECTIONS;
    }

    /** Highest level of an enchantment on any worn armour piece. */
    public static int armorLevel(LivingEntity entity, ResourceKey<Enchantment> key) {
        Holder<Enchantment> holder = holder(entity.level().registryAccess(), key);
        int best = 0;

        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            best = Math.max(best, EnchantmentHelper.getItemEnchantmentLevel(holder, entity.getItemBySlot(slot)));
        }

        return best;
    }
}